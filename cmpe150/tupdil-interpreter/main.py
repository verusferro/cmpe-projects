handle = open('input.tup', encoding='utf-8')
code = handle.read()
handle.close()
output = ""
import copy #This should be allowed. We learnt this in the lesson.

#Global variables
err = False
lines = code.strip('\n').split("\n")
variables = dict()
key_words = ['artı','eksi','çarp','bölü','metin','tam-sayı','reel-sayı']
operators = ['artı','eksi','çarp','bölü']
first_run = True
defined_variables = dict()
rte = False

def compile_error(num):
    global err, output, comp_output
    err = True
    output = f"Compile error at line {num+1}.\n"

def runtime_error(num):
    global err, output, rte
    err = True
    if not rte: #Prevents double rte in output
        output += f"Runtime error at line {num+1}.\n"
    rte = True

#Initial check
if lines[0] != "Programı başlat.":
    compile_error(0)

#Boolean check functions for valid types, chars, seperators, etc.
allowed_chars = set("abcçdefgğhıijklmnoöprsştuüvyzABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ0123456789.,:; \n")
allowed_var_chars = set("abcçdefgğhıijklmnoöprsştuüvyzABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ")
def is_allowed_chars(text):
    return set(text).issubset(allowed_chars)

def is_allowed_var_chars(text):
    return set(text).issubset(allowed_var_chars)

def valid_thousands_separator(num_str):
    if '.' in num_str:
        integer_part, decimal_part = num_str.split('.', 1)
        if '_' in decimal_part:
            return False
    else:
        integer_part = num_str
    if len(integer_part) <= 3:
        return True
    parts = integer_part.split('_')
    if len(parts) == 1:
        return False
    if len(parts[0]) > 3:
        return False
    if not all(len(part) == 3 for part in parts[1:]):
        return False
    return True

#Without thousand funcs have a single specific use case in the main function
def valid_int_without_thousand(num):
    num = num.replace('.','_')
    try:
        if int(num) < -10000 or int(num) > 10000:
            return False
    except:
        return False
    return True

def valid_float_without_thousand(num):
    num = num.replace('.','_').replace(',','.')
    try:
        if float(num) < -10000 or float(num) > 10000 or len(num.split('.')[1]) > 3 or not num.split('.')[1]:
            return False
    except:
        return False
    return True

#I now know that this is bad practice, unfortunately I learned this too late
def valid_int(num):
    num = num.replace('.','_')
    try:
        if valid_thousands_separator(num):
            if int(num) < -10000 or int(num) > 10000:
                return False
        else:
            return False
    except:
        return False
    return True

def valid_float(num):
    num = num.replace('.','_').replace(',','.')
    try:
        if valid_thousands_separator(num):
            if float(num) < -10000 or float(num) > 10000 or len(num.split('.')[1]) > 3 or not num.split('.')[1]:
                return False
        else:
            return False
    except:
        return False
    return True

def valid_metin(text):
    try:
        if not is_allowed_chars(text[1:-1].replace('|',' ')) or len(text) > 52 or text[0] != '!' or text[-1] != '!':
            return False
    except:
        return False
    return True

#To determine rte/ce
using_variables = False
used_variables = False

def calc_expr(expr, num):
    global using_variables, err, used_variables, rte
    for k in range(int((len(expr)-1)/2)):
        using_variables = False
        #Check if vars are defined later in the code but not currently. Defined_variables is assigned at the end of the first run (at the bottom)
        if (t2e(expr[0]) in defined_variables and t2e(expr[0]) not in variables) and (t2e(expr[2]) in defined_variables and t2e(expr[2]) not in variables):
            runtime_error(num)
            expr = ['0'] + expr[3:]
            continue
        if (t2e(expr[0]) in defined_variables and t2e(expr[0]) not in variables) and (valid_int(expr[2]) or valid_float(expr[2]) or valid_metin(expr[2])):
            runtime_error(num)
            expr = ['0'] + expr[3:]
            continue
        if (t2e(expr[2]) in defined_variables and t2e(expr[2]) not in variables) and (valid_int(expr[0]) or valid_float(expr[0]) or valid_metin(expr[0])):
            runtime_error(num)
            expr = ['0'] + expr[3:]
            continue
        if not (valid_int(expr[0]) or valid_float(expr[0]) or valid_metin(expr[0]) or t2e(expr[0]) in variables) or not (valid_int(expr[2]) or valid_float(expr[2]) or valid_metin(expr[2]) or t2e(expr[2]) in variables):
            if not first_run:
                compile_error(num)
            return
        if t2e(expr[0]) in variables:
            expr[0] = variables[t2e(expr[0])]['value']
            using_variables, used_variables = True, True
        if t2e(expr[2]) in variables:
            expr[2] = variables[t2e(expr[2])]['value']
            using_variables, used_variables = True, True
        if expr[1] in operators:
            if expr[1] == 'artı':
                expr = [add(expr[0], expr[2], num)] + expr[3:]
            elif expr[1] == 'eksi':
                expr = [substract(expr[0], expr[2], num)] + expr[3:]
            elif expr[1] == 'çarp':
                expr = [multiply(expr[0], expr[2], num)] + expr[3:]
            elif expr[1] == 'bölü':
                expr = [divide(expr[0], expr[2], num)] + expr[3:]
        else:
            compile_error(num)
            return
        if err:
            if first_run and rte and not using_variables:
                err = False
                rte = False
                continue
            return
    return str(expr[0]) if not err else ''

#This is where we first encounter my horrible decision to store the variable values as they appear in input.tup a.k.a TÜPDİL syntax.
#So each time a var is called, it has to be converted to python syntax and then reconverted back to TÜPDİL syntax.
#This has zero benefits and is utterly idiotic, it makes the code more bloated and harder to read. Also opens way for more mistakes.

#There is not much to explain here and the following 4 functions. They basically check for all possible combinations (int-int,float-float,...).
#Just lots of replacing and replacing and replacing and replac--------...........
def add(word1, word2, num):
    global using_variables
    output_expr = ''
    if valid_int(word1) and valid_int(word2):
        output_expr = str(format(int(word1.replace('.','_'))+int(word2.replace('.','_')),'_')).replace('_','.')
        if valid_int(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0'
    elif valid_float(word1) and valid_float(word2):
        output_expr = str(format(round(float(word1.replace('.','_').replace(',','.'))+float(word2.replace('.','_').replace(',','.')),3),'_')).replace('.',',').replace('_','.')
        if valid_float(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0,0'
    elif (valid_int(word1) and valid_float(word2)) or (valid_float(word1) and valid_int(word2)):
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    elif valid_metin(word1) and valid_metin(word2):
        output_expr = '!'+word1[1:-1]+word2[1:-1]+'!'
        if valid_metin(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '!!'
    elif (valid_metin(word1) and valid_int(word2)) or (valid_metin(word2) and valid_int(word1)):
        int_word = word1 if valid_int(word1) else word2
        metin_word = word1 if valid_metin(word1) else word2
        if valid_int(metin_word[1:-1]):
            output_expr = str(format(int(int_word.replace('.','_'))+int(metin_word[1:-1].replace('.','_')),'_')).replace('_','.')
            if valid_int(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0'
        else:
            runtime_error(num)
            return '0'
    elif (valid_metin(word1) and valid_float(word2)) or (valid_metin(word2) and valid_float(word1)):
        float_word = word1 if valid_float(word1) else word2
        metin_word = word1 if valid_metin(word1) else word2
        if valid_float(metin_word[1:-1]):
            output_expr = str(format(float(float_word.replace('.','_').replace(',','.'))+float(metin_word[1:-1].replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    else:
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    
def substract(word1, word2, num):
    global using_variables
    output_expr = ''
    if valid_int(word1) and valid_int(word2):
        output_expr = str(format(int(word1.replace('.','_'))-int(word2.replace('.','_')),'_')).replace('_','.')
        if valid_int(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0'
    elif valid_float(word1) and valid_float(word2):
        output_expr = str(format(round(float(word1.replace('.','_').replace(',','.'))-float(word2.replace('.','_').replace(',','.')),3),'_')).replace('.',',').replace('_','.')
        if valid_float(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0,0'
    elif (valid_int(word1) and valid_float(word2)) or (valid_float(word1) and valid_int(word2)):
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    elif valid_metin(word1) and valid_metin(word2):
        output_expr = metin_eksi_metin(word1, word2)
        if valid_metin(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '!!'
    elif valid_metin(word1) and valid_int(word2):
        int_word = word2
        metin_word = word1
        if valid_int(metin_word[1:-1]):
            output_expr = str(format(int(metin_word[1:-1].replace('.','_'))-int(int_word.replace('.','_')),'_')).replace('_','.')
            if valid_int(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0'
        else:
            runtime_error(num)
            return '0'
    elif valid_metin(word2) and valid_int(word1):
        int_word = word1
        metin_word = word2
        if valid_int(metin_word[1:-1]):
            output_expr = str(format(int(int_word.replace('.','_'))-int(metin_word[1:-1].replace('.','_')),'_')).replace('_','.')
            if valid_int(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0'
        else:
            runtime_error(num)
            return '0'
    elif valid_metin(word2) and valid_float(word1):
        float_word = word1
        metin_word = word2
        if valid_float(metin_word[1:-1]):
            output_expr = str(format(float(float_word.replace('.','_').replace(',','.'))-float(metin_word[1:-1].replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    elif valid_metin(word1) and valid_float(word2):
        float_word = word2
        metin_word = word1
        if valid_float(metin_word[1:-1]):
            output_expr = str(format(float(metin_word[1:-1].replace('.','_').replace(',','.'))-float(float_word.replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    else:
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)

def multiply(word1, word2, num):
    global using_variables
    output_expr = ''
    if valid_int(word1) and valid_int(word2):
        output_expr = str(format(float(int(word1.replace('.','_'))*int(word2.replace('.','_'))),'_')).replace('.',',').replace('_','.')
        if valid_float(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0,0'
    elif valid_float(word1) and valid_float(word2):
        output_expr = str(format(float(word1.replace('.','_').replace(',','.'))*float(word2.replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
        if valid_float(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0,0'
    elif (valid_int(word1) and valid_float(word2)) or (valid_float(word1) and valid_int(word2)):
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    elif valid_metin(word1) and valid_metin(word2):
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    elif (valid_metin(word1) and valid_int(word2)) or (valid_metin(word2) and valid_int(word1)):
        int_word = word1 if valid_int(word1) else word2
        metin_word = word1 if valid_metin(word1) else word2
        output_expr = '!'+str(int(int_word.replace('.','_'))*metin_word[1:-1])+'!'
        if valid_metin(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '!!'
    elif (valid_metin(word1) and valid_float(word2)) or (valid_metin(word2) and valid_float(word1)):
        float_word = word1 if valid_float(word1) else word2
        metin_word = word1 if valid_metin(word1) else word2
        if valid_float(metin_word[1:-1]):
            output_expr = str(format(float(float_word.replace('.','_').replace(',','.'))*float(metin_word[1:-1].replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    else:
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)

def divide(word1, word2, num):
    global using_variables
    output_expr = ''
    if valid_int(word1) and valid_int(word2):
        output_expr = str(format(int(word1.replace('.','_'))/int(word2.replace('.','_')),'_')).replace('.',',').replace('_','.')
        if valid_float(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0,0'
    elif valid_float(word1) and valid_float(word2):
        output_expr = str(format(float(word1.replace('.','_').replace(',','.'))/float(word2.replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
        if valid_float(output_expr):
            return output_expr
        else:
            runtime_error(num)
            return '0,0'
    elif (valid_int(word1) and valid_float(word2)) or (valid_float(word1) and valid_int(word2)):
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    elif valid_metin(word1) and valid_metin(word2):
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)
    elif valid_metin(word1) and valid_int(word2):
        int_word = word2
        metin_word = word1
        if valid_int(metin_word[1:-1]):
            output_expr = str(format(int(metin_word[1:-1].replace('.','_'))/int(int_word.replace('.','_')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    elif valid_metin(word2) and valid_int(word1):
        int_word = word1
        metin_word = word2
        if valid_int(metin_word[1:-1]):
            output_expr = str(format(int(int_word.replace('.','_'))-int(metin_word[1:-1].replace('.','_')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    elif valid_metin(word2) and valid_float(word1):
        float_word = word1
        metin_word = word2
        if valid_float(metin_word[1:-1]):
            output_expr = str(format(float(float_word.replace('.','_').replace(',','.'))/float(metin_word[1:-1].replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    elif valid_metin(word1) and valid_float(word2):
        float_word = word2
        metin_word = word1
        if valid_float(metin_word[1:-1]):
            output_expr = str(format(float(metin_word[1:-1].replace('.','_').replace(',','.'))/float(float_word.replace('.','_').replace(',','.')),'_')).replace('.',',').replace('_','.')
            if valid_float(output_expr):
                return output_expr
            else:
                runtime_error(num)
                return '0,0'
        else:
            runtime_error(num)
            return '0,0'
    else:
        if using_variables:
            runtime_error(num)
        else:
            compile_error(num)

#Seperate function since it was too long
def metin_eksi_metin(main_str, remove_str):
    main_content = main_str[1:-1]
    remove_content = remove_str[1:-1]
    remove_len = len(remove_content)
    indices = []
    i = 0
    while i <= len(main_content) - remove_len:
        if main_content[i:i + remove_len] == remove_content:
            indices.extend(range(i, i + remove_len))
        i += 1
    result = [char for idx, char in enumerate(main_content) if idx not in indices]
    return f"!{''.join(result)}!"

#I tried to do this in the normal jump function but couldn't no matter what I tried.
#So here's the only proper compile checking function in the entire code. 
def comp_check_jump_to(words, num):
    global lines, err
    expr = words[0][:-1].replace('.','_').replace(',','.')
    if len(words)!=3:
        input_expr = words[:-2]
        input_expr[-1] = input_expr[-1][:-1]
        if calc_expr(input_expr, num):
            words = words[3:]
            words.insert(0,calc_expr(input_expr, num)+'.')
            expr = words[0][:-1].replace('.','_').replace(',','.')
        else:
            if err:
                return 0
            return 0
    if valid_int(words[0][:-1]):
        if 0<int(expr)<=len(lines):
            return int(expr)
    elif valid_float(words[0][:-1]):
        if 0<int(float(expr))<=len(lines) and str(float(expr))[-1]=='0':
            return int(float(expr))
    elif t2e(words[0][:-1]) not in variables and t2e(words[0][:-1]) in defined_variables:
        return 0
    elif t2e(words[0][:-1]) in variables:
        return 0
    if valid_metin(words[0][:-1]):
        expr = words[0][1:-2].replace('.','_').replace(',','.')
        if valid_int(words[0][1:-2]):
            if 0<int(expr)<=len(lines):
                return int(expr)
        elif valid_float(words[0][1:-2]):
            if 0<int(float(expr))<=len(lines) and str(float(expr))[-1]=='0':
                return int(float(expr))
    return 0

#You can see why I couldn't merge the above function with this one, or maybe you can't...
def jump_to(words, num):
    global lines, err
    used_calc = False
    expr = words[0][:-1].replace('.','_').replace(',','.')
    if len(words)!=3:
        input_expr = words[:-2]
        input_expr[-1] = input_expr[-1][:-1]
        if calc_expr(input_expr, num):
            words = words[3:]
            words.insert(0,calc_expr(input_expr, num)+'.')
            expr = words[0][:-1].replace('.','_').replace(',','.')
        else:
            if err:
                return 0
            compile_error(num)
            return 0
        used_calc = True
    if valid_int(words[0][:-1]):
        if 0<int(expr)<=len(lines):
            return int(expr)
    elif valid_float(words[0][:-1]):
        if 0<int(float(expr))<=len(lines) and str(float(expr))[-1]=='0':
            return int(float(expr))
    elif t2e(words[0][:-1]) not in variables and t2e(words[0][:-1]) in defined_variables:
        runtime_error(num)
        return 0
    elif t2e(words[0][:-1]) in variables:
        if not variables[t2e(words[0][:-1])]['value']:
            runtime_error(num)
            return 0
        if variables[t2e(words[0][:-1])]['type'] == 'tam-sayı':
            if 0<int(variables[t2e(words[0][:-1])]['value'].replace('.','_'))<=len(lines):
                return int(variables[t2e(words[0][:-1])]['value'].replace('.','_'))
        elif variables[t2e(words[0][:-1])]['type'] == 'reel-sayı':
            if 0<int(float(variables[t2e(words[0][:-1])]['value'].replace('.','_').replace(',','.')))<=len(lines) and str(float(variables[t2e(expr)]['value'].replace('.','_').replace(',','.')))[-1]=='0':
                return int(float(variables[t2e(words[0][:-1])]['value'].replace('.','_').replace(',','.')))
        elif variables[t2e(words[0][:-1])]['type'] == 'metin':
            words[0] = variables[t2e(words[0][:-1])]['value']+'.'
    if valid_metin(words[0][:-1]):
        expr = words[0][1:-2].replace('.','_').replace(',','.')
        if valid_int(words[0][1:-2]):
            if 0<int(expr)<=len(lines):
                return int(expr)
        elif valid_float(words[0][1:-2]):
            if 0<int(float(expr))<=len(lines) and str(float(expr))[-1]=='0':
                return int(float(expr))
    if used_variables or used_calc:
        runtime_error(num)
    else:
        compile_error(num)
    return 0

#Fix for lower() func not properly lowercasing I and İ. Most often used in var name checking.
def t2e(text):
    return text.replace('I','ı').replace('İ','i').lower()

#Pretty self-explanatory. I don't really know what to say since it's mostly just error checking
def assign_variable_val(words, num):
    global err
    rte_local = False #I don't remember/know why this exists but it's important, I think...
    if len(words) <= 4:
        if not (valid_int(words[-2]) or valid_float(words[-2]) or valid_metin(words[-2]) or t2e(words[-2]) in variables):
            if not first_run:
                if t2e(words[-2]) in defined_variables:
                    rte_local = True
                else:
                    compile_error(num)
                    return
    if t2e(words[0]) not in variables:
        if not first_run:
            if t2e(words[0]) in defined_variables:
                runtime_error(num)
            else:
                compile_error(num)
        return
    if rte_local:
        runtime_error(num)
        return
    if len(words) > 4:
        words[-2] = str(calc_expr(words[2:-1], num))
        if err:
            return
    if t2e(words[-2]) in variables:
        words[-2] = variables[t2e(words[-2])]['value']
    if variables[t2e(words[0])]['type'] == 'metin' and valid_metin(words[-2]):
        variables[t2e(words[0])]['value'] = words[-2]
        return
    if variables[t2e(words[0])]['type'] == 'tam-sayı' and valid_int(words[-2]):
        variables[t2e(words[0])]['value'] = words[-2]
        return
    if variables[t2e(words[0])]['type'] == 'reel-sayı' and valid_float(words[-2]):
        variables[t2e(words[0])]['value'] = words[-2]
        return
    runtime_error(num)

#Again, pretty self-explanatory
def assign_variable_type(words, num):
    if t2e(words[0]) in key_words:
        compile_error(num)
        return
    if not is_allowed_var_chars(words[0]) or len(words[0]) > 20:
        if first_run:
            return
        compile_error(num)
        return
    if t2e(words[0]) not in variables:
        variables[t2e(words[0])] = {'type': '', 'value': ''}
    if words[-2] == 'metin':
        variables[t2e(words[0])]['type'] = 'metin'
        variables[t2e(words[0])]['value'] = ''
    elif words[-2] == 'tam-sayı':
        variables[t2e(words[0])]['type'] = 'tam-sayı'
        variables[t2e(words[0])]['value'] = ''
    elif words[-2] == 'reel-sayı':
        variables[t2e(words[0])]['type'] = 'reel-sayı'
        variables[t2e(words[0])]['value'] = ''
    else:
        compile_error(num)

#Did I mention I store var values as they appear in input? This also applies to strings (metin) too.
#Ex: Var B with value 'bir sayı' is stored as !bir|sayı!
#This is why it prints with word[1:-1]. The '|' will be explained later.
def yazdir(words, num):
    global output
    if len(words) == 2:
        if t2e(words[0]) in variables:
            if variables[t2e(words[0])]['value'] == '':
                if not first_run:
                    runtime_error(num)
                return
            if variables[t2e(words[0])]['type'] == 'metin':
                output += variables[t2e(words[0])]['value'][1:-1].replace('|',' ') + '\n'
            else:
                output += variables[t2e(words[0])]['value'] + '\n'
        elif valid_metin(words[0]):
            output += words[0][1:-1].replace('|',' ') + '\n' #The '!' removal mentioned before
        elif valid_int(words[0]):
            output += words[0].replace('.','_') + '\n'
        elif valid_float(words[0]):
            output += words[0].replace('.','_').replace(',','.') + '\n'
        elif t2e(words[0]) in defined_variables:
            runtime_error(num)
        else:
            if not first_run:
                compile_error(num)
    else:
        if calc_expr(words[:-1], num):
            output += calc_expr(words[:-1], num) + '\n'
        else:
            return

#The MaIn FuNcTiOn
def execute_code(lines):
    global output, err, rte
    line_num = 1
    while line_num < len(lines):
        if err:
            if first_run and rte: #You wouldn't give an rte during first_run a.k.a compile run would you?
                err = False
                rte = False
            else:
                break
        try:
            if lines[line_num][-1] != ".":
                compile_error(line_num)
                break
        except:
            compile_error(line_num)
            break
        if line_num != 0 and lines[line_num] == "Programı başlat.":
            compile_error(line_num)
            break
        elif line_num != len(lines)-1 and lines[line_num] == "Programı bitir.":
            compile_error(line_num)
            break
        elif line_num == len(lines)-1 and lines[line_num] != "Programı bitir.":
            compile_error(line_num)
            break
        elif line_num == len(lines)-1 and lines[line_num] == "Programı bitir.":
            break
        elif line_num == 0 and lines[line_num] == "Programı başlat.":
            line_num+=1
            continue
        if '!' in lines[line_num]: #This one I'm very proud of. Detects strings and replaces the spaces to make them easier to work with.
            if lines[line_num].count('!')%2==1: #Since '!' isn't an allowed char, it doesn't make sense for them to be odd numbered
                compile_error(line_num)
                break
            count=0
            for idx,ch in enumerate(lines[line_num]):
                if count%2==1:
                    if ch=='!':
                        count+=1
                        continue
                    if not ch in allowed_chars:
                        compile_error(line_num)
                        break
                    if ch==' ':
                        lines[line_num] = lines[line_num][:idx]+'|'+lines[line_num][idx+1:]
                if ch=='!':
                    count+=1
                    if count%2==0 and idx<len(lines[line_num])-2: #Checks for !a!!b! which might be interpreted as two adjacent strings
                                                                  #which is impossible/illegal. I think?
                        if lines[line_num][idx+1]!=' ':
                            compile_error(line_num)
                            break                        
                if count%2==0:
                    continue
        words = lines[line_num].split()
        if '\t' in words:
            compile_error(line_num)
            break
        if lines[line_num].count(' ') != len(words)-1:
            compile_error(line_num)
            break
        for i in words: #Check for 9999 without thousand seperator. This is the only use case of these functions
            if valid_int_without_thousand(i) or valid_float_without_thousand(i):
                if valid_thousands_separator(i.replace('.','_').replace(',','.')):
                    continue
                else:
                    compile_error(line_num)
                    break
            try:
                if int(i.replace('.','_')) and not valid_int(i):
                    compile_error(line_num)
                    break
            except:
                try:
                    if float(i.replace('.','_').replace(',','.')) and not valid_float(i):
                        compile_error(line_num)
                        break
                except:
                    continue
        if err:
            break
        if 'satıra zıpla.' == t2e(' '.join(words[-2:])).lower(): #I realized afterwards that the lower() is unnecessary but who cares
            for i in range(len(words)-2):
                if i!=len(words)-3:
                    if words[i][-1]=='.':
                        compile_error(line_num)
                elif not words[i][-1]=='.':
                    compile_error(line_num)
            if err:
                break
            if first_run==False:
                line_num = jump_to(words, line_num)-1
                continue
            comp_check_jump_to(words, line_num)
            line_num+=1
            continue
        elif 'yazdır.' == t2e(words[-1]).lower():
            yazdir(words, line_num)
        elif 'olsun.' == t2e(words[-1]).lower():
            if 'değeri' == t2e(words[1]).lower():
                assign_variable_val(words, line_num)
            elif 'bir' == t2e(words[1]).lower():
                assign_variable_type(words, line_num)
            else:
                compile_error(line_num)
                break
        else:
            compile_error(line_num)
            break
        line_num+=1

#Run the code twice. First for compile errors (without executing jumps and other exclusions), then for runtime errors
#(It actually checks for both errors, both times)
#Some say it's cheating, bad practice, and idiotic (me). I say it's ingenious (also this makes it less than 1000 lines and a pain in the ass)
if not err:
    execute_code(lines)
    first_run = False
if not err or rte:
    lines = code.strip('\n').split("\n")
    defined_variables = copy.deepcopy(variables)
    variables = dict()
    output = ''
    rte = False
    err = False
    execute_code(lines)

handle = open('output.txt', 'w', encoding='utf-8', newline='\n') # newline makes line breaks LF(unix) instead of CRLF(windows) which the diff command checks for
handle.write(output)
handle.close()