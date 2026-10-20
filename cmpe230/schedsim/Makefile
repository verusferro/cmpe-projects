.PHONY: all clean

all: schedsim

schedsim: src/schedsim.s
	as -o src/schedsim.o src/schedsim.s
	ld -o schedsim src/schedsim.o

clean:
	rm -f schedsim src/schedsim.o
