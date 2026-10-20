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
java -cp out:lib/stdlib.jar Main sample-input/mapData.txt sample-input/travelCosts.txt sample-input/objectives.txt -draw
```

Replace `Main` with `Bonus` for the bonus mode. On Windows, use `;` instead of `:` in the class path.
