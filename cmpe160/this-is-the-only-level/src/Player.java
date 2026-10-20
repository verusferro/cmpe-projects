public class Player {

    private double x;
    private double y;
    private double width;
    private double height;
    private double velocityY;
    private boolean facingRight;
    private String imageRight = "misc/ElephantRight.png";
    private String imageLeft = "misc/ElephantLeft.png";

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
        this.width = 20;
        this.height = 20;
        this.velocityY = 0;
        this.facingRight = true;
    }

    public void respawn(int[] spawnPoint) {
        this.x = spawnPoint[0];
        this.y = spawnPoint[1];
        this.velocityY = 0;
        this.facingRight = true;
    }

    public void draw() {
        // Select the image based on the direction the player is facing
        String currentImage = facingRight ? imageRight : imageLeft;
        StdDraw.picture(x, y, currentImage, width, height);
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void facingRight() {
        this.facingRight = true;
    }

    public void facingLeft() {
        this.facingRight = false;
    }

    public void changeVelocityY(double velocityY) {
        this.velocityY += velocityY;
    }
}
