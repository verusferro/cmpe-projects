import java.awt.*;
import java.util.Objects;
import java.util.Random;

public class Point {
    private final int x;
    private final int y;
    private Color color;
    private int number;
    private static final Random rand = new Random();

    public Point(int x, int y) {
        this(x, y, 0);
    }

    Point(int x, int y, int number) {
        this.x = x;
        this.y = y;
        this.number = number;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }

    public int getNumber() {
        return number;
    }

    public static Color getRandColor() {
        return new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point other)) return false;
        return x == other.getX() && y == other.getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
