# variable
input_arguments = [[
    ["Z", "C", "M", "Z", "Z", "B", "A", "P11"],
    ["M", "Z", "Z", "P", "X", "Z", "Z", "Z"],
    ["P", "Z", "Z", "Z", "Z", "Z", "Z", "Z"],
    ["Z", "E", "2", "Z", "0", "P", "Z", "Z"],
    ["Z", "Z", "Z", "Z", "Z", "Z", "I", "Z"],
    ["Z", "Z", "B", "A", "0", "Z", "3", "L"],
    ["Z", "Z", "Z", "Z", "Z", "Z", "Z", "P1"],
    ["Z", "Z", "Z", "Z", "Z", "B", "A", "Z"]
], ['CMPE', 'PILAB']]
# variable

# assign your result to this variable
output_dictionary = None

# DO NOT CHANGE THE CODE ABOVE

def decode_grid(grid, possible_words):
    
    rowNum = len(grid)
    colNum = len(grid[0])
    paths_dict = dict()
    portals = dict()
    
    def find_portals(grid):
        if not grid or not grid[0]:
            return
        current = grid[0][0]
        pos = (rowNum - len(grid), colNum - len(grid[0]))
        # Check for portal
        if len(current) >= 2 and current[0] == 'P':
            if current not in portals:
                portals[current] = []
            portals[current].append(pos)
        grid[0] = grid[0][1:]
        if not grid[0]:
            grid.pop(0)
        find_portals(grid)

    def find_paths(pos, word, path, path_len):
        row, col = pos
        if pos in path:
            return
        if not (0<=row<rowNum and 0<=col<colNum):
            return

        # Portal function
        if grid[row][col] in portals:
            path.append(pos)
            # Teleportation to other portal
            portal_positions = portals[grid[row][col]]
            if len(portal_positions) != 2:
                path.pop()
                return
            if pos == portal_positions[0]:
                pos = portal_positions[1]
            else:
                pos = portal_positions[0]
            path.append(pos)
            row, col = pos
            if len(word) == 0:
                rowS, colS, rowE, colE = path[0] + path[-1]
                distanceToStart = (rowS**2 + colS**2)**0.5
                distanceToEnd = ((rowNum-1-rowE)**2 + (colNum-1-colE)**2)**0.5
                paths.append([path_len,distanceToStart,distanceToEnd] + path.copy())
            find_paths((row+1,col),word,path,path_len+1)
            find_paths((row-1,col),word,path,path_len+1)
            find_paths((row,col+1),word,path,path_len+1)
            find_paths((row,col-1),word,path,path_len+1)
            find_paths((row+1,col+1),word,path,path_len+(2**0.5))
            find_paths((row-1,col+1),word,path,path_len+(2**0.5))
            find_paths((row+1,col-1),word,path,path_len+(2**0.5))
            find_paths((row-1,col-1),word,path,path_len+(2**0.5))
            path.pop()
            path.pop()

        # Star (bridge) function
        if grid[row][col] == '*':
            path.append(pos)
            if len(word) == 0:
                rowS, colS, rowE, colE = path[0] + path[-1]
                distanceToStart = (rowS**2 + colS**2)**0.5
                distanceToEnd = ((rowNum-1-rowE)**2 + (colNum-1-colE)**2)**0.5
                paths.append([path_len,distanceToStart,distanceToEnd] + path.copy())
            find_paths((row+1,col),word,path,path_len+1)
            find_paths((row-1,col),word,path,path_len+1)
            find_paths((row,col+1),word,path,path_len+1)
            find_paths((row,col-1),word,path,path_len+1)
            find_paths((row+1,col+1),word,path,path_len+(2**0.5))
            find_paths((row-1,col+1),word,path,path_len+(2**0.5))
            find_paths((row+1,col-1),word,path,path_len+(2**0.5))
            find_paths((row-1,col-1),word,path,path_len+(2**0.5))
            path.pop()

        # Wall function
        if grid[row][col] == '0':
            if len(word) == 0:
                path.append(pos)
                rowS, colS, rowE, colE = path[0] + path[-1]
                distanceToStart = (rowS**2 + colS**2)**0.5
                distanceToEnd = ((rowNum-1-rowE)**2 + (colNum-1-colE)**2)**0.5
                paths.append([path_len,distanceToStart,distanceToEnd] + path.copy())
                path.pop()
            return

        # Jump function
        if grid[row][col].isdigit():
            i = int(grid[row][col])
            path.append(pos)
            if len(word) == 0:
                rowS, colS, rowE, colE = path[0] + path[-1]
                distanceToStart = (rowS**2 + colS**2)**0.5
                distanceToEnd = ((rowNum-1-rowE)**2 + (colNum-1-colE)**2)**0.5
                paths.append([path_len,distanceToStart,distanceToEnd] + path.copy())
            if not wall_check(row,col,row+i,col):
                find_paths((row+i,col),word,path,path_len+i)
            if not wall_check(row,col,row-i,col):
                find_paths((row-i,col),word,path,path_len+i)
            if not wall_check(row,col,row,col+i):
                find_paths((row,col+i),word,path,path_len+i)
            if not wall_check(row,col,row,col-i):
                find_paths((row,col-i),word,path,path_len+i)
            if not wall_check(row,col,row+i,col+i):
                find_paths((row+i,col+i),word,path,path_len+(2**0.5)*i)
            if not wall_check(row,col,row-i,col+i):
                find_paths((row-i,col+i),word,path,path_len+(2**0.5)*i)
            if not wall_check(row,col,row+i,col-i):
                find_paths((row+i,col-i),word,path,path_len+(2**0.5)*i)
            if not wall_check(row,col,row-i,col-i):
                find_paths((row-i,col-i),word,path,path_len+(2**0.5)*i)
            path.pop()

        if len(word) == 0:
            # rowS, colS, rowE, colE = path[0] + path[-1]
            # distanceToStart = (rowS**2 + colS**2)**0.5
            # distanceToEnd = ((rowNum-1-rowE)**2 + (colNum-1-colE)**2)**0.5
            # paths.append([path_len,distanceToStart,distanceToEnd] + path.copy())
            return

        # Joker and letter function
        if grid[row][col] in [word[0],'#']:
            path.append(pos)
            if len(word) == 1:
                # Path list format: [pathLength,distanceToStart,distanceToEnd,path] for one-shot sorting
                rowS, colS, rowE, colE = path[0] + path[-1]
                distanceToStart = (rowS**2 + colS**2)**0.5
                distanceToEnd = ((rowNum-1-rowE)**2 + (colNum-1-colE)**2)**0.5
                paths.append([path_len,distanceToStart,distanceToEnd] + path.copy())
                # path.pop()
                # return
            find_paths((row+1,col),word[1:],path,path_len+1)
            find_paths((row-1,col),word[1:],path,path_len+1)
            find_paths((row,col+1),word[1:],path,path_len+1)
            find_paths((row,col-1),word[1:],path,path_len+1)
            find_paths((row+1,col+1),word[1:],path,path_len+(2**0.5))
            find_paths((row-1,col+1),word[1:],path,path_len+(2**0.5))
            find_paths((row+1,col-1),word[1:],path,path_len+(2**0.5))
            find_paths((row-1,col-1),word[1:],path,path_len+(2**0.5))
            path.pop()

    # Wall checking for jump function
    def wall_check(start_row, start_col, end_row, end_col):
        if not (0<=end_row<rowNum and 0<=end_col<colNum):
            return True
        if start_row == end_row and start_col == end_col:
            return False
        # Get next cell to check
        next_row = start_row + (1 if end_row > start_row else -1 if end_row < start_row else 0)
        next_col = start_col + (1 if end_col > start_col else -1 if end_col < start_col else 0)
        # Check for wall
        if grid[next_row][next_col] == '0' and (next_row, next_col) != (end_row, end_col):
            return True
        return wall_check(next_row, next_col, end_row, end_col)
    
    # Lambda replacement
    def sort_func(x):
        return (x[0],x[1],x[2])

    import copy
    find_portals(copy.deepcopy(grid))

    for word in possible_words:
        for r in range(rowNum):
            for c in range(colNum):
                paths = []
                find_paths((r,c),word,[],0)
                if paths:
                    paths_dict[word] = paths_dict.get(word, []) + paths
        if word in paths_dict:
            # Path list format: [pathLength,distanceToStart,distanceToEnd,path] for one-shot sorting
            paths_dict[word] = [path[3:] for path in sorted(paths_dict[word], key=sort_func)]

    return paths_dict

print(decode_grid(input_arguments[0], input_arguments[1]))


# DO NOT CHANGE THE CODE BELOW
output_dictionary = decode_grid(input_arguments[0], input_arguments[1])