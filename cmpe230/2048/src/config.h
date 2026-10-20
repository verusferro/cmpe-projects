#pragma once

// Game config constants
namespace Config {

constexpr int N = 4;        // grid rows
constexpr int M = 4;        // grid columns
constexpr int K = 2048;     // winning tile value
constexpr int P = 90;       // % chance a newly spawned tile is 2
constexpr int Q = 10;       // % chance a newly spawned tile is 4

static_assert(N >= 2 && M >= 2, "Grid must be at least 2x2");
static_assert(P + Q == 100,     "Spawn probabilities P and Q must sum to 100");

}

