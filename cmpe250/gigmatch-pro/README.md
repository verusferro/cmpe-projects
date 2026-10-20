# GigMatch Pro

A simulator for a freelancing platform. Customers request services, and the platform picks the best freelancer for each job.

**Course:** CMPE 250 Data Structures and Algorithms, Fall 2025 · Java

## Highlights

- Ranks freelancers by skill match, ratings, reliability and burnout
- Handles jobs from start to finish: hiring, cancellations, ratings and blacklists
- Freelancer skills grow with good work and drop after cancellations
- A monthly update handles burnout, recovery and customer loyalty tiers
- Uses its own hash map, hash set and max-heap written from scratch

## Run

```sh
javac -d out src/*.java
java -cp out Main input.txt output.txt
```
