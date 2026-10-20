# Stock Exchange

A simple stock exchange. It reads buy and sell orders and matches them the way a real exchange does.

**Course:** CMPE 150 Introduction to Computing, Fall 2024 · Python

## Highlights

- Matches orders by price, then by time
- Fills orders in parts when the amounts do not match
- Never matches a user with their own order
- Calculates traded and open order volume, in total or for one user

## Run

```sh
python3 main.py
```

Reads orders from `input.txt` and writes the trades to `output.txt`. A small example `input.txt` is included.
