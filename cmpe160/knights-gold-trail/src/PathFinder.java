import java.util.*;

public class PathFinder {

    public static ArrayList<Tile> AstarAlgorithm(Tile startTile, Tile endTile, Tile[][] tiles) {
        // Initialize and reset all tiles
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                Tile tile = tiles[i][j];
                tile.setTravelCost(Double.POSITIVE_INFINITY);
                // Calculate heuristic cost (Manhattan distance)
                tile.setHeuristicCost(Math.abs(endTile.getX() - tile.getX()) + Math.abs(endTile.getY() - tile.getY()));
                tile.setPreviousTile(null);
            }
        }

        HashSet<Tile> visited = new HashSet<>();
        PriorityQueue<Tile> pq = new PriorityQueue<>((t1, t2) -> Double.compare(t1.getTotalCost(), t2.getTotalCost()));
        startTile.setTravelCost(0); // Start tile has 0 cost
        pq.add(startTile); // Start the queue

        while (!pq.isEmpty()) {
            Tile current = pq.poll();

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            if (current.equals(endTile)) {
                break;
            }

            // Since we only deal with neighbors (who aren't walls), we don't need extra wall checks
            for (Tile neighbor : current.getAdjacentTiles().keySet()) {
                if (!visited.contains(neighbor)) {
                    double neighborCost = current.getTravelCost() + current.getNeighborCost(neighbor);
                    neighborCost = Math.round(neighborCost * 100.0) / 100.0;
                    if (neighborCost < neighbor.getTravelCost()) {
                        neighbor.setTravelCost(neighborCost);
                        neighbor.setPreviousTile(current);
                        pq.offer(neighbor);
                    }
                }
            }
        }

        // Reconstruct path
        ArrayList<Tile> path = new ArrayList<>();
        Tile currentTile = endTile;
        while (currentTile != null) {
            path.add(currentTile);
            currentTile = currentTile.getPreviousTile();
        }

        Collections.reverse(path); // reverse to make it start -> end
        return path;
    }
}