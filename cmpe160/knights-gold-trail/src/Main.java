import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Locale.setDefault(Locale.US);
        StdDraw.enableDoubleBuffering();

        boolean drawMode = false;
        ArrayList<String> fileNames = new ArrayList<>();
        Tile[][] tiles;
        ArrayList<Point> coins = new ArrayList<>();

        // Check for flags and file names
        for (String arg : args) {
            if (arg.equals("-draw")) {
                drawMode = true;
            } else {
                fileNames.add(arg);
            }
        }

        // Ensure at least 3 file names are provided
        if (fileNames.size() < 3) {
            System.out.println("Enter 3 arguments for file names");
            System.exit(0);
        }

        File mapData = new File(fileNames.get(0));
        File travelCosts = new File(fileNames.get(1));
        File objectives = new File(fileNames.get(2));

        // Read map data, scale canvas, and initialize tiles
        Scanner sc = new Scanner(mapData);
        int[] mapSize = {sc.nextInt(), sc.nextInt()}; // cols, rows
        StdDraw.setCanvasSize(mapSize[0]*30, mapSize[1]*30);
        StdDraw.setXscale(0, mapSize[0]);
        StdDraw.setYscale(mapSize[1], 0);
        tiles = new Tile[mapSize[0]][mapSize[1]];

        // Read tile data
        while (sc.hasNextInt()) {
            int x = sc.nextInt();
            int y = sc.nextInt();
            tiles[x][y] =  new Tile(x, y, sc.nextInt());
        }

        sc.close();
        // Read travel costs, set neighbors
        sc = new Scanner(travelCosts);
        while (sc.hasNextInt() || sc.hasNextDouble()) {
            int x1 = sc.nextInt();
            int y1 = sc.nextInt();
            int x2 = sc.nextInt();
            int y2 = sc.nextInt();
            double cost = sc.nextDouble();
            tiles[x1][y1].addNeighbor(tiles[x2][y2], cost);
            tiles[x2][y2].addNeighbor(tiles[x1][y1], cost);
        }

        sc.close();
        // Read objectives and initialize knight and coins
        sc = new Scanner(objectives);
        Knight knight = new Knight(sc.nextInt(), sc.nextInt(), tiles);
        while (sc.hasNextInt()) {
            coins.add(new Point(sc.nextInt(), sc.nextInt()));
        }
        knight.setCoins(new ArrayList<>(coins));

        sc.close(); // Close scanner

        // Create output file
        PrintWriter writer = new PrintWriter("out/output.txt");
        double totalCost = 0;
        int totalSteps = 0;

        // Process each coin (objective)
        for (int i = 0; i < coins.size(); i++) {
            Point coin = coins.get(i);
            // Run pathfinder
            ArrayList<Tile> path = PathFinder.AstarAlgorithm(knight.getTile(), tiles[coin.getX()][coin.getY()], tiles);
            // If path only contains end tile, it is unreachable
            if (path.size() <= 1) {
                writer.println("Objective " + (i+1) + " cannot be reached!");
                continue;
            }
            // Update totals
            totalCost += path.getLast().getTravelCost();
            totalSteps += path.size()-1;
            writer.println("Starting position: " + knight.getPos());
            for (int j = 1; j < path.size(); j++) {
                Tile tile = path.get(j);
                writer.printf("Step Count: %d, move to %s. Total Cost: %.2f.\n", j, tile.getPos(), tile.getTravelCost());
            }
            writer.println("Objective " + (i+1) + " reached!");
            // Draw path if in draw mode
            if (drawMode) {
                knight.draw(path);
                knight.removeCoin(coin);
            }
            // Update knight's position
            knight.setPos(coin);
        }
        writer.printf("Total Step: %d, Total Cost: %.2f", totalSteps, totalCost);
        writer.close(); // Close writer

        System.exit(0); // Exit program since stddraw never quits
    }
}
