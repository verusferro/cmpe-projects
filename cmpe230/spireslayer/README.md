# SpireSlayer

An interpreter for a text-based card game inspired by Slay the Spire. You type plain-English commands and questions, and the program updates the game and answers.

```
» Ironclad gains 7 gold
Gold obtained
» Total gold ?
7
```

**Course:** CMPE 230 Systems Programming, Spring 2026 · C · Pair project

## Highlights

- Runs every line through 3 stages: tokenizer, parser and executor
- Rejects any command that does not follow the game's grammar
- Tracks gold, health, deck, relics, potions and floors
- Uses generic dynamic arrays built with C macros, with manual memory management

## Run

```sh
make
./spireslayer
```

Type `Exit` to quit.
