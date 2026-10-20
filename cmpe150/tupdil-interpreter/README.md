# TÜPDİL Interpreter

An interpreter for TÜPDİL, a small programming language written in Turkish. It checks a program for errors and then runs it.

```
Programı başlat.
a bir tam-sayı olsun.
a değeri 1 artı 5 olsun.
a yazdır.
Programı bitir.
```

Output: `6`

**Course:** CMPE 150 Introduction to Computing, Fall 2024 · Python

## Highlights

- Integer, decimal and text variables, math operators, parentheses and jumps to other lines
- Finds compile errors before the program runs, such as bad syntax or invalid names
- Stops with an error when the program goes wrong while running, such as an unknown variable or a type mismatch
- Prints numbers in Turkish format, for example `1.250,5`

## Run

```sh
python3 main.py
```

Reads `input.tup` and writes the result to `output.txt`.

## Tests

`tests/input` has 75 test programs, and `tests/expected` has the output each one should give. The program always reads `input.tup`, so copy a test there first:

```sh
cp tests/input/test21.tup input.tup
python3 main.py
```

Then compare `output.txt` with `tests/expected/test21.txt`.
