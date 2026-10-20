import java.util.HashMap;
import java.util.Objects;

public class Tile {
    private final Point pos;
    private final int type;
    private final HashMap<Tile, Double> adjacentTiles;

    private double travelCost;
    private double heuristicCost;
    private double totalCost; // travel + heuristic
    private Tile previousTile;

    public Tile(int x, int y, int type) {
        this.pos = new Point(x, y);
        this.type = type;
        this.adjacentTiles = new HashMap<>();
        this.travelCost = Double.POSITIVE_INFINITY;
        this.heuristicCost = 0;
        this.totalCost = travelCost + heuristicCost;
        this.previousTile = null;
    }

    public void setTravelCost(double cost) {
        this.travelCost = cost;
        this.totalCost = travelCost + heuristicCost;
    }

    public void setHeuristicCost(double cost) {
        this.heuristicCost = cost;
        this.totalCost = travelCost + heuristicCost;
    }

    public double getTravelCost() {
        return travelCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public Tile getPreviousTile() {
        return previousTile;
    }

    public void setPreviousTile(Tile previousTile) {
        this.previousTile = previousTile;
    }

    public void addNeighbor(Tile neighbor, double cost) {
        adjacentTiles.put(neighbor, cost);
    }

    public double getNeighborCost(Tile neighbor) {
        return adjacentTiles.get(neighbor);
    }

    public int getType() {
        return type;
    }

    public HashMap<Tile, Double> getAdjacentTiles() {
        return adjacentTiles;
    }

    public Point getPos() {
        return pos;
    }

    public int getX() {
        return pos.getX();
    }

    public int getY() {
        return pos.getY();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tile other)) return false;
        return pos == other.getPos();
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, type);
    }
}
