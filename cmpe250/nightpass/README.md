# Nightpass

A survival card game. Each night you duel a stranger, and the program picks the best card from your deck to play against each of his cards.

**Course:** CMPE 250 Data Structures and Algorithms, Fall 2025 · Java

## Highlights

- Picks the best card for each battle by attack and health, following strict priority rules
- Damaged cards stay in play, defeated cards go to a discard pile and can come back later
- Stores cards in self-balancing AVL trees written from scratch, so lookups stay fast with large decks

## Run

```sh
javac -d out src/*.java
java -cp out Main input.txt output.txt
```
