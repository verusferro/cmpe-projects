# GigMatch Pro

A simulator for a freelancing platform. Customers request services, and the platform picks the best freelancer for each job.

**Course:** CMPE 250 Data Structures and Algorithms, Fall 2025 · Java

## Highlights

- Ranks freelancers by skill match, ratings, reliability and burnout
- Handles jobs from start to finish: hiring, cancellations, ratings and blacklists
- Freelancer skills grow with good work and drop after cancellations
- A monthly update handles burnout, recovery and customer loyalty tiers
- Uses its own hash map, hash set and max-heap written from scratch

## Example

Input (`tests/input/t4.txt`). A freelancer line has the name, service, price and 5 skill scores:

```
register_freelancer free1 tutoring 400 88 81 60 63 81
register_freelancer free2 tutoring 458 57 60 87 61 51
register_freelancer free3 tutoring 415 51 59 48 58 56
register_customer cust1
request_job cust1 tutoring 5
```

Output:

```
registered freelancer free1
registered freelancer free2
registered freelancer free3
registered customer cust1
available freelancers for tutoring (top 3):
free1 - composite: 8615, price: 400, rating: 5.0
free2 - composite: 7942, price: 458, rating: 5.0
free3 - composite: 7513, price: 415, rating: 5.0
auto-employed best freelancer: free1 for customer cust1
```

## Run

```sh
javac -d out src/*.java
java -cp out Main input.txt output.txt
```

## Tests

`tests/input` has 3 of the course's test inputs, and `tests/expected` has the output each one should give. To run a test:

```sh
java -cp out Main tests/input/t1_small_1a.txt output.txt
```

Then compare `output.txt` with `tests/expected/t1_small_1a.txt`.
