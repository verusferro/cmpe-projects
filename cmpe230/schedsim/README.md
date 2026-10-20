# SchedSim

A CPU scheduling simulator written entirely in x86-64 assembly, with no C library. You give it a list of processes, and it prints which process runs at each clock tick.

```
$ echo "FCFS A-2-0 B-1-4" | ./schedsim
AAXXB
```

Each process is written as name, run time and arrival time. Here A runs for 2 ticks, the CPU is idle (`X`) until B arrives at tick 4, then B runs.

**Course:** CMPE 230 Systems Programming, Spring 2026 · x86-64 Assembly · Pair project

## Highlights

- Supports 5 scheduling methods: first come first served, shortest job first, shortest remaining time, priority and round robin
- Reads the input and writes the output with direct system calls

## Run

Needs Linux on x86-64.

```sh
make
echo "SRTF A-4-0 B-1-1" | ./schedsim
```
