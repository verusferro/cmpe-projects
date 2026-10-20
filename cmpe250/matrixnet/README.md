# MatrixNet

A simulator for a secret network of hosts and tunnels, themed after The Matrix. It builds the network from commands, finds the best route between two hosts, and checks which failures would split the network.

**Course:** CMPE 250 Data Structures and Algorithms, Fall 2025 · Java

## Highlights

- Adds and removes hosts and the tunnels between them
- Finds the fastest route that respects bandwidth and security limits
- Simulates a lost host or tunnel and reports if the network falls apart
- Writes a summary report of the whole network
- Uses its own hash map, hash set and min-heap written from scratch

## Run

```sh
javac -d out src/*.java
java -cp out Main input.txt output.txt
```
