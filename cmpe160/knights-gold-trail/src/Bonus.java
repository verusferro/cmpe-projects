import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class Bonus {
    public static void main(String[] args) throws FileNotFoundException {
        Locale.setDefault(Locale.US);
        StdDraw.enableDoubleBuffering();

        boolean drawMode = false;
        ArrayList<String> fileNames = new ArrayList<>();
        Tile[][] tiles;
        ArrayList<Point> coins = new ArrayList<>();

        int n;  // Number of objectives
        double[][] cost;  // Cost matrix
        List<Tile> indexToTile = new ArrayList<>();
        double[][] memo;
        int[][] parent;
        final double INF = Double.POSITIVE_INFINITY;  // More manageable infinity value

        for (String arg : args) {
            if (arg.equals("-draw")) {
                drawMode = true;
            } else {
                fileNames.add(arg);
            }
        }

        if (fileNames.size() < 3) {
            System.out.println("Enter 3 arguments for file names");
            System.exit(0);
        }

        File mapData = new File(fileNames.get(0));
        File travelCosts = new File(fileNames.get(1));
        File objectivesFile = new File(fileNames.get(2));

        Scanner sc = new Scanner(mapData);
        int[] mapSize = {sc.nextInt(), sc.nextInt()}; // cols, rows
        StdDraw.setCanvasSize(mapSize[0]*30, mapSize[1]*30);
        StdDraw.setXscale(0, mapSize[0]);
        StdDraw.setYscale(mapSize[1], 0);
        tiles = new Tile[mapSize[0]][mapSize[1]];

        while (sc.hasNextInt()) {
            int x = sc.nextInt();
            int y = sc.nextInt();
            tiles[x][y] = new Tile(x, y, sc.nextInt());
        }

        sc.close();
        sc = new Scanner(travelCosts);
        while (sc.hasNextInt() || sc.hasNextDouble()) {
            int x1 = sc.nextInt();
            int y1 = sc.nextInt();
            int x2 = sc.nextInt();
            int y2 = sc.nextInt();
            double costVal = sc.nextDouble(); // Renamed from 'cost' to avoid conflict with static field

            tiles[x1][y1].addNeighbor(tiles[x2][y2], costVal);
            tiles[x2][y2].addNeighbor(tiles[x1][y1], costVal);
        }

        sc.close();
        sc = new Scanner(objectivesFile);
        Knight knight = new Knight(sc.nextInt(), sc.nextInt(), tiles);

        int counter = 1;
        while (sc.hasNextInt()) {
            Point coin = new Point(sc.nextInt(), sc.nextInt());
            tiles[coin.getX()][coin.getY()].getPos().setNumber(counter++);
            coins.add(coin);
        }
        knight.setCoins(new ArrayList<>(coins));

        sc.close(); // Close scanner

        ArrayList<Tile> objectives = new ArrayList<>();
        objectives.add(knight.getTile());

        // hacky method to remove unreachable coins
        // since A* ends search if it reaches endTile, we make the endTile
        // impossible to reach to make sure it calculates the costs for every tile
        PathFinder.AstarAlgorithm(knight.getTile(), new Tile(-1, -1, 2), tiles);
        for (Point coin : coins) {
            Tile coinTile = tiles[coin.getX()][coin.getY()];
            if (coinTile.getTravelCost() != INF) { // Use static INF
                objectives.add(coinTile);
            }
        }
        n = objectives.size();

        // Map tiles to indices and build the cost matrix
        for (int i = 0; i < n; i++) {
            indexToTile.add(objectives.get(i));
        }

        cost = new double[n][n];
        // Initialize cost matrix with INF
        for (int i = 0; i < n; i++) {
            Arrays.fill(cost[i], INF);
            cost[i][i] = 0.0; // Cost to self is 0
        }

        // assign cost matrix values
        ShortestRoute.DijkstraArrays arrays = ShortestRoute.DijkstraAlgorithm(objectives, tiles);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    cost[i][j] = arrays.tileCosts().get(i).getOrDefault(objectives.get(j), INF);
                }
            }
        }

        // Initialize memoization table
        memo = new double[n][1 << n];
        parent = new int[n][1 << n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(memo[i], INF);
            Arrays.fill(parent[i], -1);
        }

        // Run the Held-Karp algorithm (starting from the first objective, with only the first one visited)
        ShortestRoute.HeldKarp(n, cost, memo, parent);

        // Reconstruct the optimal path
        List<Tile> optimalPath = ShortestRoute.reconstructPath(indexToTile, parent, n);
        Collections.reverse(optimalPath); // Reverse the path to get start -> end

        // Create output file
        PrintWriter writer = new PrintWriter("out/bonus.txt");
        double totalCost = 0;
        int totalSteps = 0;

        // Process each objective
        for (int i = 1; i < optimalPath.size(); i++) {
            Tile coin = optimalPath.get(i);
            // While we can reuse the existing prevTiles maps from DijkstraArrays record,
            // it's honestly easier to just recalculate since it's so fast anyways.
            ArrayList<Tile> path = PathFinder.AstarAlgorithm(knight.getTile(), tiles[coin.getX()][coin.getY()], tiles);
            double currentCost;
            // The cost doesn't reset for each objective
            for (int j = 1; j < path.size(); j++) {
                Tile tile = path.get(j);
                currentCost = totalCost + tile.getTravelCost();
                totalSteps ++;
                writer.printf("Step Count: %d, move to %s. Total Cost: %.2f.\n", totalSteps, tile.getPos(), currentCost);
            }
            totalCost += path.getLast().getTravelCost();
            // Get the objective number
            if (coin.getPos().getNumber() != 0) {
                writer.println("Objective " + coin.getPos().getNumber() + " reached!");
            }
            // Draw the path if in draw mode
            if (drawMode) {
                knight.drawColored(path);
                knight.removeCoin(coin.getPos());
            }
            // Update knight's position
            knight.setPos(coin.getPos());
        }
        writer.printf("Total Step: %d, Total Cost: %.2f", totalSteps, totalCost); // Original: totalCost
        writer.close(); // Close the writer

        System.exit(0); // Exit program since stddraw never quits
    }
}