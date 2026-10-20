# SchedSim

A CPU scheduling simulator written entirely in x86-64 assembly, with no C library. You give it a list of processes, and it prints which process runs at each clock tick.

```
$ echo "FCFS A-2-0 B-1-4" | ./schedsim
AAXXB
```

Here A runs for 2 ticks, the CPU is idle (`X`) until B arrives at tick 4, then B runs.

**Course:** CMPE 230 Systems Programming, Spring 2026 · x86-64 Assembly · Pair project

## Highlights

- Supports 5 scheduling methods: first come first served, shortest job first, shortest remaining time, priority and round robin
- Reads the input and writes the output with direct system calls

## Input format

The first word picks the method. Each process is a letter followed by its numbers.

| Method | Example | Process format |
|---|---|---|
| First come first served | `FCFS A-3-0 B-2-1` | name-run time-arrival |
| Shortest job first | `SJF A-4 B-2` | name-run time |
| Shortest remaining time | `SRTF A-4-0 B-1-1` | name-run time-arrival |
| Priority | `PF A-6-0-3 B-2-1-1` | name-run time-arrival-priority (lower runs first) |
| Round robin | `RR A-10 B-3 4` | name-run time, then the time slice |

## Run

Needs Linux on x86-64.

```sh
make
echo "SRTF A-4-0 B-1-1" | ./schedsim
```

## Tests

`tests/input` has the course's test inputs, and `tests/expected` has the output each one should give. To run a test:

```sh
./schedsim < tests/input/test0.txt
```

Then compare the result with `tests/expected/test0.txt`.
