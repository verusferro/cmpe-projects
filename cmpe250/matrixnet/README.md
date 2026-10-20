# MatrixNet

A simulator for a secret network of hosts and tunnels, themed after The Matrix. It builds the network from commands, finds the best route between two hosts, and checks which failures would split the network.

**Course:** CMPE 250 Data Structures and Algorithms, Fall 2025 · Java

## Highlights

- Adds and removes hosts and the tunnels between them
- Finds the fastest route that respects bandwidth and security limits
- Simulates a lost host or tunnel and reports if the network falls apart
- Writes a summary report of the whole network
- Uses its own hash map, hash set and min-heap written from scratch

## Example

Input:

```
spawn_host ZION_CORE 5
spawn_host NEO_HUB 3
spawn_host TRINITY_KERNEL 4
link_backdoor ZION_CORE NEO_HUB 10 100 2
link_backdoor NEO_HUB TRINITY_KERNEL 5 80 1
trace_route ZION_CORE TRINITY_KERNEL 50 0
scan_connectivity
simulate_breach NEO_HUB
oracle_report
```

Output:

```
Spawned host ZION_CORE with clearance level 5.
Spawned host NEO_HUB with clearance level 3.
Spawned host TRINITY_KERNEL with clearance level 4.
Linked ZION_CORE <-> NEO_HUB with latency 10ms, bandwidth 100Mbps, firewall 2.
Linked NEO_HUB <-> TRINITY_KERNEL with latency 5ms, bandwidth 80Mbps, firewall 1.
Optimal route ZION_CORE -> TRINITY_KERNEL: ZION_CORE -> NEO_HUB -> TRINITY_KERNEL (Latency = 15ms)
Network is fully connected.
Host NEO_HUB IS an articulation point.
Failure results in 2 disconnected components.
--- Resistance Network Report ---
Total Hosts: 3
Total Unsealed Backdoors: 2
Network Connectivity: Connected
Connected Components: 1
Contains Cycles: No
Average Bandwidth: 90.0Mbps
Average Clearance Level: 4.0
```

## Run

```sh
javac -d out src/*.java
java -cp out Main input.txt output.txt
```

## Tests

`tests/input` has 2 of the course's test inputs, and `tests/expected` has the output each one should give. To run a test:

```sh
java -cp out Main tests/input/type3_1_small.txt output.txt
```

Then compare `output.txt` with `tests/expected/type3_1_small.txt`.
