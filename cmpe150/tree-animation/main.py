#stickman
stick = '  _  \n ( ) \n  Y  \n /|\\ \n| | |\n  |  \n / \\ \n|   |'

#tree
treeWidth=int(input())
tree=''
yamuk=''
emp=' '
emp2=' '
for i in range(int((treeWidth/2))-1):
    if i == (int((treeWidth/2))-2):
        emp = '_'
    yamuk+=' '*int((treeWidth/2)-2-i)+'/'+emp*(2*(i+1))+'\\'+' '*int((treeWidth/2)-2-i)+'\n'
tree = ' '*int((treeWidth-2)/2)+'/\\'+' '*int((treeWidth-2)/2)+'\n'+yamuk*3+(' '*int((treeWidth-2)/2)+'||'+' '*int((treeWidth-2)/2)+'\n')*2
tree = tree.rstrip('\n')

#divide to rows
stick_lines = stick.split('\n')
tree_lines = tree.split('\n')
resultlines = []
maxlines = max(len(stick_lines), len(tree_lines))

#make a stickman row and tree row
for i in range(maxlines):
    if maxlines == len(stick_lines):
        stick_line = stick_lines[i] + ' '*(9+treeWidth)
    elif i < maxlines - len(stick_lines):
        stick_line = ' ' * (14+treeWidth)
    else:
        stick_line = stick_lines[i - (maxlines - len(stick_lines))] + ' '*(9+treeWidth)
    if treeWidth == 4 and i == 0:
        tree_lines.insert(0,' ' * treeWidth)
        tree_lines.insert(0,' ' * treeWidth)
    resultlines.append(stick_line)

#print tree on top of stickman lines
tree_index = 7
for frame in range(treeWidth+10):
    for i in range(maxlines):
        if frame != 0:
            resultlines[i] = ' ' + resultlines[i]
        for j in tree_lines[i]:
            if j == ' ':
                tree_index += 1
            else:
                break
        print(resultlines[i][:tree_index] + tree_lines[i].strip() + resultlines[i][len(tree_lines[i].strip())+tree_index:])
        tree_index = 7
