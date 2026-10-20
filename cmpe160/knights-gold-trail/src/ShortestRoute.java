import java.util.*;

public class ShortestRoute {

    // Record to hold the results of Dijkstra's algorithm for multiple start tiles
    public record DijkstraArrays(List<HashMap<Tile, Double>> tileCosts, List<HashMap<Tile, Tile>> prevTiles) {}

    // Dijkstra's algorithm for multiple start tiles, no heuristic
    public static DijkstraArrays DijkstraAlgorithm(ArrayList<Tile> startTiles, Tile[][] tiles) {
        List<HashMap<Tile, Double>> tileCosts = new ArrayList<>();
        List<HashMap<Tile, Tile>> prevTiles = new ArrayList<>();
        // Store each tile's costs and path in its own hashmap
        for (int k = 0; k < startTiles.size(); k++) {
            Tile startTile = startTiles.get(k);
            tileCosts.add(new HashMap<>());
            prevTiles.add(new HashMap<>());

            for (int i = 0; i < tiles.length; i++) {
                for (int j = 0; j < tiles[0].length; j++) {
                    Tile tile = tiles[i][j];
                    tile.setTravelCost(Double.POSITIVE_INFINITY);
                    tile.setPreviousTile(null);
                }
            }

            HashSet<Tile> visited = new HashSet<>();
            PriorityQueue<Tile> pq = new PriorityQueue<>((t1, t2) -> Double.compare(t1.getTotalCost(), t2.getTotalCost()));
            startTile.setTravelCost(0);
            pq.add(startTile);

            while (!pq.isEmpty()) {
                Tile current = pq.poll();
                if (visited.contains(current)) {
                    continue;
                }
                visited.add(current);

                for (Tile neighbor : current.getAdjacentTiles().keySet()) {
                    if (!visited.contains(neighbor)) {
                        double neighborCost = current.getTravelCost() + current.getNeighborCost(neighbor);
                        neighborCost = Math.round(neighborCost * 100.0) / 100.0;
                        if (neighborCost < neighbor.getTravelCost()) {
                            neighbor.setTravelCost(neighborCost);
                            neighbor.setPreviousTile(current);
                            pq.offer(neighbor);
                            prevTiles.get(k).put(neighbor, current);
                            tileCosts.get(k).put(neighbor, neighborCost);
                        }
                    }
                }
            }
        }
        return new DijkstraArrays(tileCosts, prevTiles);
    }

    static final double INF = Double.POSITIVE_INFINITY;
    public static void HeldKarp(int n, double[][] cost, double[][] memo, int[][] parent) {
        // The 'memo' table stores the minimum cost of a path starting at a 'current_pos',
        // having visited the set of cities represented by 'mask', and eventually returning to the start city.
        // The 'parent' table stores the next city to visit from 'current_pos' in the optimal path
        // for the subproblem defined by (current_pos, mask).

        // Step 1: Base cases for the dynamic programming.
        // If all cities have been visited (mask is all_visited_mask), the only remaining step
        // is to return to the starting city (index 0).
        int all_visited_mask = (1 << n) - 1; // Mask representing all cities visited.
        for (int pos = 0; pos < n; pos++) {
            // The cost from 'pos' (the last city visited in the tour) back to the start city (index 0).
            memo[pos][all_visited_mask] = cost[pos][0];
            // parent[pos][all_visited_mask] is not used by reconstructPath as it stops before this state.
        }

        // Step 2: Iterate through subproblems of decreasing size.
        // 'num_set_bits' represents the number of cities already visited in the current subproblem's mask.
        // We iterate downwards because the cost of a subproblem with 'k' cities visited
        // depends on the costs of subproblems with 'k+1' cities visited.
        for (int num_set_bits = n - 1; num_set_bits >= 1; num_set_bits--) {
            // Iterate through all possible masks (subsets of visited cities).
            for (int mask = 0; mask < (1 << n); mask++) {
                // Process only masks that have 'num_set_bits' cities visited.
                if (Integer.bitCount(mask) != num_set_bits) {
                    continue;
                }

                // For each city 'current_pos' that could be the end of a path segment
                // for the current 'mask'.
                for (int current_pos = 0; current_pos < n; current_pos++) {
                    // The state (current_pos, mask) is valid only if 'current_pos' is one of the
                    // cities included in the 'mask'.
                    if ((mask & (1 << current_pos)) == 0) {
                        continue;
                    }

                    // Try to transition from 'current_pos' to an unvisited city 'next_city'.
                    // 'memo[current_pos][mask]' will store the minimum cost of completing the tour
                    // starting from 'current_pos', given that cities in 'mask' are already visited.
                    // It is initialized to INF (from outside this function).
                    for (int next_city = 0; next_city < n; next_city++) {
                        // Consider 'next_city' only if it's not already in the current 'mask'.
                        if ((mask & (1 << next_city)) == 0) {

                            // If there's no direct path from 'current_pos' to 'next_city'.
                            if (cost[current_pos][next_city] >= INF) {
                                continue;
                            }

                            // 'next_mask' represents the set of visited cities after moving to 'next_city'.
                            int next_mask = mask | (1 << next_city);
                            // 'cost_of_remaining_path' is the pre-computed optimal cost from 'next_city'
                            // having visited cities in 'next_mask', to complete the tour.
                            double cost_of_remaining_path = memo[next_city][next_mask];

                            // If the remaining path from 'next_city' is impossible.
                            if (cost_of_remaining_path >= INF) {
                                continue;
                            }

                            // Calculate the total cost of the path: current_pos -> next_city -> ... -> start_city.
                            double current_path_candidate_cost = cost[current_pos][next_city] + cost_of_remaining_path;

                            // If this path candidate offers a lower cost than the best found so far
                            // for the subproblem (current_pos, mask).
                            if (current_path_candidate_cost < memo[current_pos][mask]) {
                                memo[current_pos][mask] = current_path_candidate_cost;
                                parent[current_pos][mask] = next_city; // Record 'next_city' as the choice for this subproblem.
                            }
                        }
                    }
                }
            }
        }
    }

    // Reconstruct the optimal path
    public static List<Tile> reconstructPath(List<Tile> indexToTile, int[][] parent, int n) {
        List<Tile> path = new ArrayList<>();
        int visited = 1;  // Start with only the first city visited
        int pos = 0;
        path.add(indexToTile.get(pos));

        // Follow the parent pointers to construct the path
        while (visited != (1 << n) - 1) {
            int next = parent[pos][visited];
            if (next == -1) break;  // Safety check

            path.add(indexToTile.get(next));
            visited |= (1 << next);  // Mark the next city as visited
            pos = next;
        }

        // Add the starting tile to complete the tour
        path.add(indexToTile.getFirst());
        return path;
    }
}