CC := gcc
CFLAGS := -std=c11 -Wall -Wextra -pedantic

.PHONY: default clean

default: spireslayer

spireslayer: src/main.c src/tokenizer.c src/parser.c src/execute.c
	$(CC) $(CFLAGS) -o $@ src/main.c

clean:
	rm -f spireslayer
