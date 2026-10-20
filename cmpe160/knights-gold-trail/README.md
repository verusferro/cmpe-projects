# Knight's Gold Trail

A knight walks across a map of grass, sand and rock tiles to collect gold coins. The program finds the cheapest path to each coin and animates the walk.

**Course:** CMPE 160 Object-Oriented Programming, Spring 2025 · Java

## Highlights

- Finds the cheapest path between tiles with Dijkstra's algorithm
- Draws the map, the knight and the path step by step with StdDraw
- Bonus mode finds the cheapest order to collect all coins

## Run

Needs Java 21 or newer. Run from this folder so the images load.

```sh
javac -cp lib/stdlib.jar -d out src/*.java
java -cp out:lib/stdlib.jar Main tests/20-20/mapData.txt tests/20-20/travelCosts.txt tests/20-20/objectives.txt -draw
```

Leave out `-draw` to skip the animation. On Windows, use `;` instead of `:` in the class path.

## Tests

Each folder in `tests` is one course test, named after the map size. It has 3 input files and the expected result:

- `mapData.txt`: the map size and the type of each tile
- `travelCosts.txt`: the cost of moving between neighbouring tiles
- `objectives.txt`: where the knight starts and where the coins are
- `output.txt`: the expected path and total cost

The program writes its result to `out/output.txt`. For the bonus test, use `Bonus` instead of `Main` with `tests/20-20-Bonus`. It writes to `out/bonus.txt`.
