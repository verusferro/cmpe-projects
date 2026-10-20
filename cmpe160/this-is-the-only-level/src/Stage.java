import java.awt.*;
import java.util.Random;

public class Stage {

    private int stageNumber;
    private double gravity;
    private double velocityX;
    private double velocityY;
    private int rightCode;
    private int leftCode;
    private int upCode;
    private String clue;
    private String help;
    private Color color;

    public Stage(double gravity, double velocityX, double velocityY, int stageNumber,
                 int rightCode, int leftCode, int upCode, String clue, String help) {
        this.gravity = gravity;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.stageNumber = stageNumber;
        this.rightCode = rightCode;
        this.leftCode = leftCode;
        this.upCode = upCode;
        this.clue = clue;
        this.help = help;
        Random rand = new Random();
        // Random color for obstacles
        this.color = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
    }

    public int[] getKeyCodes() {
        return new int[]{rightCode, leftCode, upCode};
    }

    public Color getColor() {
        return color;
    }

    public String getClue() {
        return clue;
    }

    public String getHelp() {
        return help;
    }

    public double getGravity() {
        return gravity;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public int getStageNumber() {
        return stageNumber;
    }
}
