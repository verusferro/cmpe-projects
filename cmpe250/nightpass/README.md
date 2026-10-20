# Nightpass

A survival card game. Each night you duel a stranger, and the program picks the best card from your deck to play against each of his cards.

**Course:** CMPE 250 Data Structures and Algorithms, Fall 2025 · Java

## Highlights

- Picks the best card for each battle by attack and health, following strict priority rules
- Damaged cards stay in play, defeated cards go to a discard pile and can come back later
- Stores cards in self-balancing AVL trees written from scratch, so lookups stay fast with large decks

## Example

The input is a list of commands, and the program writes one output line for each command.

| Input | Output |
|---|---|
| `draw_card small0000 75 309` | `Added small0000 to the deck` |
| `deck_count` | `Number of cards in the deck: 107` |
| `battle 378 405 0` | `Found with priority 2, Survivor plays small0023, the played card returned to deck, 0 cards revived` |
| `steal_card 1020 169` | `The Stranger stole the card: torture_asc_0022` |
| `find_winning` | `The Survivor, Score: 49` |

These lines come from `tests/input/type2_small.txt`.

## Run

```sh
javac -d out src/*.java
java -cp out Main input.txt output.txt
```

## Tests

`tests/input` has 2 of the course's test inputs, and `tests/expected` has the output each one should give. To run a test:

```sh
java -cp out Main tests/input/type1_small.txt output.txt
```

Then compare `output.txt` with `tests/expected/type1_small.txt`.
