input_file = 'input.txt'
output_file = 'output.txt'

handle = open(input_file, encoding='utf-8')
code = handle.read()
handle.close()

lines=code.strip('\n').split('\n')
transactions, times, buyers, sellers = [], [], [], []

# Sort to buyers and sellers
for line in lines:
    words = line.split()
    new_line = [words[0],words[3],float(words[-1]),int(words[1]),words[2],words[-3],int(words[-2])]
    if new_line[-2] == 'Buy':
        buyers.append(new_line)
    else:
        sellers.append(new_line)
# Store format: [time, stock, price, userid, name, type, amount]

# Since lambda isn't allowed
def buyer_sort(x):
    return (x[0], x[1], -x[2], x[3])
def buffer_sort(x):
    return x[0]
def order_sort(x):
    return (x[0], x[1])

# Sort buyers and sellers
buyers.sort(key=buyer_sort)
sellers.sort()

# Create a timeline
times = sorted(set([i[0] for i in buyers+sellers]))

# Execute transactions
def main(t=0):
    buy_orders = []
    sell_orders = []
    # Instead of going line-by-line, go time-by-time like a clock (also more realistic)
    for time in times:
        if t and time > t:
            return sell_orders+buy_orders # For volume functions
        
        # Process orders at a given time
        # Pre-sort orders for ease
        orders = sorted([buyer[:] for buyer in buyers if buyer[0] == time],key=buyer_sort) + \
                 sorted([seller[:] for seller in sellers if seller[0] == time])
        orders.sort(key=order_sort)
        buffer = [] # To sort the time of matching orders
        # Buffer store format: [lowest time, transaction, buyer id, seller id]
        # lowest time: lowest time among buyer and seller for sorting
        # buyer and seller id: for use in volume functions
        k=0
        # Process orders in already sorted order
        while k < len(orders):
            popped = False
            if orders[k][-2] == 'Buy':
                buy_order = orders[k]
                j=0
                while j < len(sell_orders):
                    sell_order = sell_orders[j]
                    # Check if buy price is more than sell price and if they're the same stock and if they're not the same user
                    if buy_order[2] >= sell_order[2] and buy_order[1] == sell_order[1] and buy_order[3] != sell_order[3]:
                        amount = min(buy_order[-1], sell_order[-1]) # Amount to be traded
                        buffer.append((min(buy_order[0],sell_order[0]), f'{buy_order[-3]} bought {amount} {buy_order[1]}\
                                       for {sell_order[2]} USD from {sell_order[-3]} on {'-'.join(time.split('-')[:3])}\
                                        at {':'.join(time.split('-')[3:])}'.split(), buy_order[3], sell_order[3]))
                        # Update remaining amounts
                        orders[k][-1] -= amount
                        sell_order[-1] -= amount
                        # Check for empty orders with 0 remaining amount
                        # If buy order is done, no need to check the rest of sell orders
                        if orders[k][-1] == 0 and sell_order[-1] == 0:
                            orders.pop(k)
                            sell_orders.pop(j)
                            popped = True
                            break
                        if orders[k][-1] == 0:
                            orders.pop(k)
                            popped = True
                            break
                        if sell_order[-1] == 0:
                            sell_orders.pop(j)
                            continue
                    j += 1
            else:
                # The same as above but in reverse for a sell order
                sell_order = orders[k]
                j=0
                while j < len(buy_orders):
                    buy_order = buy_orders[j]
                    if buy_order[2] >= sell_order[2] and buy_order[1] == sell_order[1] and buy_order[3] != sell_order[3]:
                        amount = min(buy_order[-1], sell_order[-1])
                        buffer.append((min(buy_order[0],sell_order[0]), f'{buy_order[-3]} bought {amount} {buy_order[1]}\
                                       for {sell_order[2]} USD from {sell_order[-3]} on {'-'.join(time.split('-')[:3])}\
                                        at {':'.join(time.split('-')[3:])}'.split(), buy_order[3], sell_order[3]))
                        orders[k][-1] -= amount
                        buy_order[-1] -= amount
                        if orders[k][-1] == 0 and buy_order[-1] == 0:
                            orders.pop(k)
                            buy_orders.pop(j)
                            popped = True
                            break
                        if orders[k][-1] == 0:
                            orders.pop(k)
                            popped = True
                            break
                        if buy_order[-1] == 0:
                            buy_orders.pop(j)
                            continue
                    j += 1
            # If the order still has remaining amount, add it to the respective list
            if not popped:
                if orders[k][-2] == 'Buy':
                    buy_orders.append(orders[k])
                else:
                    sell_orders.append(orders[k])
                k += 1
        if not t: # Don't need to recalculate transactions if t is given since t is only given in volume functions
            transactions.extend([i[1:] for i in sorted(buffer,key=buffer_sort)])

main()
# The output is just the entire thing
output = '\n'.join([' '.join(transaction[0]) for transaction in transactions])+'\n'

print(output)
handle = open(output_file, 'w', encoding='utf-8', newline='\n')
handle.write(output)
handle.close()

def total_remaining_volume(target_time, userid=0):
    volume = 0
    orders = main(target_time) # Execute until target time
    if not orders:
        orders = []
    volume += sum([order[2]*order[-1] for order in orders if int(userid) in [order[3], 0]]) # 0 for all users (total)
    return round(volume)

def total_executed_volume(target_time, userid=0):
    volume = 0
    for time in times:
        if time > target_time:
            break
        volume += sum([int(order[0][2])*float(order[0][5]) for order in transactions\
                       if order[0][-3] == '-'.join(time.split('-')[:3]) and order[0][-1] == ':'.join(time.split('-')[3:])\
                        and int(userid) in [order[1], order[2], 0]]) # 0 for all users (total)
    return round(volume)

# Didn't really need these but they're here just in case
def remaining_user_volume(userid, target_time):
    return total_remaining_volume(target_time, userid)

def executed_user_volume(userid, target_time):
    return total_executed_volume(target_time, userid)