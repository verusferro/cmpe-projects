# This Is the Only Level

A remake of the puzzle platformer "This Is the Only Level". You guide an elephant through the same level again and again, and each stage adds a new twist.

**Course:** CMPE 160 Object-Oriented Programming, Spring 2025 · Java

## Highlights

- Player movement, gravity and collisions with walls, spikes and buttons
- A new rule in every stage, with a clue and a help button
- Game timer and death counter
- Built with classes for the game, map, stages and player, and drawn with StdDraw

## Run

Run from this folder so the images load.

```sh
javac -cp lib/stdlib.jar -d out src/*.java
java -cp out:lib/stdlib.jar Main
```

On Windows, use `;` instead of `:` in the class path.
