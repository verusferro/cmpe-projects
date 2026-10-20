# 2048

The sliding tile puzzle 2048 as a desktop app. Slide the tiles with the arrow keys or WASD, merge equal tiles, and reach 2048.

**Course:** CMPE 230 Systems Programming, Spring 2026 · C++ with Qt · Pair project

## Highlights

- 3 game modes: normal, unlimited (keep playing after you win) and hard (the game moves for you if you wait more than 5 seconds)
- Grid size, target tile and tile spawn chances are set in `src/config.h`
- Game logic is kept apart from the board drawing and the window

## Run

Needs Qt 6 and CMake.

```sh
cmake -B build
cmake --build build
./build/2048
```
