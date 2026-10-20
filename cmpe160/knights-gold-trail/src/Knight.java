import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class Knight {
    private Point pos;
    private ArrayList<Point> coins;
    private final Tile[][] tiles;
    private final HashSet<Point> deletedCoins;

    public Knight(int x, int y, Tile[][] tiles) {
        this.tiles = tiles;
        this.coins = new ArrayList<>();
        this.deletedCoins = new HashSet<>();
        this.pos = new Point(x, y);
    }

    // Draw everything
    public void draw(ArrayList<Tile> path) {
        HashSet<Tile> visited = new HashSet<>();
        for (Tile tile : path) {
            StdDraw.clear();
            drawTiles();
            drawCoins();
            drawDots(visited);
            StdDraw.picture(tile.getX()+0.5, tile.getY()+0.5, "misc/knight.png", 0.6, 0.8);
            StdDraw.show();
            visited.add(tile);
            StdDraw.pause(80);
        }
    }

    public void drawTiles() {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                Tile tile = tiles[i][j];
                String picName = switch (tile.getType()) {
                    case 0 -> "misc/grassTile.jpeg";
                    case 1 -> "misc/sandTile.png";
                    case 2 -> "misc/impassableTile.jpeg";
                    default -> throw new IllegalStateException("Unexpected value: " + tile.getType());
                };
                StdDraw.picture(tile.getX()+0.5, tile.getY()+0.5, picName, 1, 1);

            }
        }
    }

    private void drawCoins() {
        for (Point coin : coins) {
            if (!deletedCoins.contains(coin)) {
                StdDraw.picture(coin.getX()+0.5, coin.getY()+0.5, "misc/coin.png", 0.7, 0.7);
            }
        }
    }

    private void drawDots(HashSet<Tile> visited) {
        StdDraw.setPenColor(Color.RED);
        for (Tile tile : visited) {
            StdDraw.filledCircle(tile.getX()+0.5, tile.getY()+0.5, 0.2);
        }
    }

    // bonus methods below
    private final HashSet<Point> visitedColored = new HashSet<>();
    public void drawColored(ArrayList<Tile> path) {
        Color color = Point.getRandColor();
        for (Tile tile : path) {
            StdDraw.clear();
            drawTiles();
            drawCoins();
            drawDotsColored(visitedColored);
            StdDraw.picture(tile.getX()+0.5, tile.getY()+0.5, "misc/knight.png", 0.6, 0.8);
            tile.getPos().setColor(color);
            visitedColored.add(tile.getPos());
            StdDraw.show();
            StdDraw.pause(80);
        }
    }
    private void drawDotsColored(HashSet<Point> visited) {
        for (Point dot : visited) {
            StdDraw.setPenColor(dot.getColor());
            StdDraw.filledCircle(dot.getX()+0.5, dot.getY()+0.5, 0.2);
        }
    }
    // bonus methods above

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public int getY() {
        return pos.getY();
    }

    public int getX() {
        return pos.getX();
    }

    public void removeCoin(Point coin) {
        deletedCoins.add(coin);
    }

    public void setCoins(ArrayList<Point> coins) {
        this.coins = coins;
    }

    public Tile getTile() {
        return tiles[getX()][getY()];
    }

    public Point getPos() {
        return pos;
    }
}
