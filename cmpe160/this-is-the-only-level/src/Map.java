import java.awt.*;

public class Map {

    private Stage stage;
    private Player player;
    private int buttonPressNum;
    private boolean isDoorOpen;
    // keyCodes are KeyEvent.VK_RIGHT, KeyEvent.VK_A etc.
    // Obstacles List (formant is int[] = {xLeftDown , yLeftDown, xRightUp, yRightUp}
    private int[][] obstacles = {
    new int[]{0, 120, 120, 270}, new int[]{0, 270, 168, 330},
    new int[]{0, 330, 30, 480}, new int[]{0, 480, 180, 600},
    new int[]{180, 570, 680, 600}, new int[]{270, 540, 300, 570},
    new int[]{590, 540, 620, 570}, new int[]{680, 510, 800, 600},
    new int[]{710, 450, 800, 510}, new int[]{740, 420, 800, 450},
    new int[]{770, 300, 800, 420}, new int[]{680, 240, 800, 300},
    new int[]{680, 300, 710, 330}, new int[]{770, 180, 800, 240},
    new int[]{0, 120, 800, 150}, new int[]{560, 150, 800, 180},
    new int[]{530, 180, 590, 210}, new int[]{530, 210, 560, 240},
    new int[]{320, 150, 440, 210}, new int[]{350, 210, 440, 270},
    new int[]{220, 270, 310, 300}, new int[]{360, 360, 480, 390},
    new int[]{530, 310, 590, 340}, new int[]{560, 400, 620, 430}};
    // Button Coordinates
    private int[] button = new int[]{400, 390, 470, 410};
    // Button Floor Coordinates
    private int[] buttonFloor = new int[]{400, 390, 470, 400};
    // Start Pipe Coordinates for Drawing
    private int[][] startPipe = {new int[]{115, 450, 145, 480},
    new int[]{110, 430, 150, 450}};
    // Exit Pipe Coordinates for Drawing
    private int[][] exitPipe = {new int[]{720, 175, 740, 215},
    new int[]{740, 180, 770, 210}};
    // Coordinates of spikes[i] areas
    private int[][] spikes = {
    new int[]{30, 333, 50, 423}, new int[]{121, 150, 207, 170},
    new int[]{441, 150, 557, 170}, new int[]{591, 180, 621, 200},
    new int[]{750, 301, 770, 419}, new int[]{680, 490, 710, 510},
    new int[]{401, 550, 521, 570}};
    // Door Coordinates
    private int[] door = new int[]{685, 180, 700, 240};
    private String imageSpike = "misc/Spikes.png";
    private boolean playerDead;
    private boolean buttonPressed;

    public Map(Stage stage, Player player) {
        this.stage = stage;
        this.player = player;
        // Players spawns at start pipe
        player.setX((startPipe[0][0] + startPipe[0][2]) / 2.0);
        player.setY(startPipe[0][1]);
        this.buttonPressNum = 0;
        this.isDoorOpen = false;
        this.playerDead = false;
        this.buttonPressed = false;
    }

    public void movePlayer(char direction) {

        double nextX = player.getX();
        double nextY = player.getY();
        player.changeVelocityY(getStage().getGravity());

        switch (direction) {
            case 'U': // Up
                if (isGrounded(nextX, nextY + player.getVelocityY())) {
                    player.setVelocityY(getStage().getVelocityY());
                }
                break;
            case 'R': // Right
                player.facingRight();
                nextX += getStage().getVelocityX();
                break;
            case 'L': // Left
                player.facingLeft();
                nextX -= getStage().getVelocityX();
                break;
            case 'E': // Up right
                if (isGrounded(nextX, nextY + player.getVelocityY())) {
                    player.setVelocityY(getStage().getVelocityY());
                }
                player.facingRight();
                nextX += getStage().getVelocityX();
                break;
            case 'Q': // Up left
                if (isGrounded(nextX, nextY + player.getVelocityY())) {
                    player.setVelocityY(getStage().getVelocityY());
                }
                player.facingLeft();
                nextX -= getStage().getVelocityX();
                break;
        }

        // Jump constantly
        if (getStage().getStageNumber() == 2 && isGrounded(nextX, nextY + player.getVelocityY())) {
            player.setVelocityY(getStage().getVelocityY());
        }

        player.setVelocityY(Math.max(player.getVelocityY(), -15)); // Terminal velocity
        // Update nextY with new velocity
        nextY += player.getVelocityY();

        // Collision check and "teleportation"
        for (int[] obstacle : obstacles) {
            int collisionSide = checkCollision(nextX, nextY, obstacle);
            // Check Y Velocity direction to prevent hanging off corners
            if (collisionSide == 1  && player.getVelocityY() < 0) {
              nextY = (obstacle[3] + 10);
              player.setVelocityY(0);
            } else if (collisionSide == 3 && player.getVelocityY() > 0) {
                nextY = (obstacle[1] - 10);
                player.setVelocityY(0);
            } else if (collisionSide == 2) {
                nextX = (obstacle[2] + 10);
            } else if (collisionSide == 4) {
                nextX = (obstacle[0] - 10);
            }
        }

        // Door check
        if (!isDoorOpen && checkCollision(nextX, nextY, door) == 4) {
            nextX = (door[0] - 10);
        }

        // Spike and death check
        for (int[] spike : spikes) {
            if (checkCollision(nextX, nextY, spike) != 0) {
                playerDead = true;
                break;
            }
        }

        player.setX(nextX);
        player.setY(nextY);
    }

    private boolean isGrounded(double nextX, double nextY) {
        for (int[] obstacle : obstacles) {
            if (checkCollision(nextX, nextY, obstacle) == 1) {
                return true;
            }
        }
        return false;
    }

    private int checkCollision(double nextX, double nextY, int[] obstacle) {
        // Player is 20x20 square
        double halfWidth = 10;
        double halfHeight = 10;
        // Check if the next position collides with the obstacle
        if (nextX + halfWidth > obstacle[0] && nextX - halfWidth < obstacle[2] &&
            nextY + halfHeight > obstacle[1] && nextY - halfHeight < obstacle[3]) {

            // Calculate overlap in both axes
            double overlapX = Math.min(nextX + halfWidth - obstacle[0], obstacle[2] - (nextX - halfWidth));
            double overlapY = Math.min(nextY + halfHeight - obstacle[1], obstacle[3] - (nextY - halfHeight));

            // 1-Top, 2-Right, 3-Bottom, 4-Left
            if (overlapX < overlapY) {
                if (nextX < (obstacle[0] + obstacle[2]) / 2.0) { // Vertical side
                    return 4; // Left
                } else {
                    return 2; // Right
                }
            } else { // Horizontal side
                if (nextY < (obstacle[1] + obstacle[3]) / 2.0) {
                    return 3; // Bottom
                } else {
                    return 1; // Top
                }
            }
        }
        return 0; // No collision
    }

   public boolean changeStage() {
       // Check if the player is in the exit pipe area
       return player.getX() > exitPipe[0][0] && player.getX() < exitPipe[0][2] &&
               player.getY() > exitPipe[0][1] && player.getY() < exitPipe[0][3];
   }

    public void buttonCheck() {
        if (checkCollision(player.getX(), player.getY(), button) != 0) {
            if (!buttonPressed) pressButton(); // Only counts if button goes from unpressed to pressed
            buttonPressed = true; // To stop drawing button
        } else {
            buttonPressed = false;
        }

        // Handle door state
        if (buttonPressNum > 0) isDoorOpen = true;

        // Require at least 5 presses
        if (getStage().getStageNumber() == 3) {
            isDoorOpen = buttonPressNum >= 5;
        }

        // Door closes on press
        if (getStage().getStageNumber() == 4) {
            isDoorOpen = buttonPressNum == 0;
        }
    }

    public void restartStage() {
        // Respawn player at start and reset everything
        player.respawn(new int[]{(startPipe[0][0] + startPipe[0][2]) / 2, startPipe[0][1]});
        buttonPressNum = 0;
        isDoorOpen = false;
        buttonPressed = false;
    }
    
    public void draw() {

        // Draw obstacles
        StdDraw.setPenColor(getStage().getColor());
        for (int[] obstacle : obstacles) {
            StdDraw.filledRectangle((obstacle[0] + obstacle[2]) / 2.0, (obstacle[1] + obstacle[3]) / 2.0,
                                    (obstacle[2] - obstacle[0]) / 2.0, (obstacle[3] - obstacle[1]) / 2.0);
        }

        // Draw spikes
        for (int i = 0; i < spikes.length; i++) {
            double degree = switch (i) {
                case 5, 6 -> 0;
                case 0 -> 90;
                case 4 -> 270;
                default -> 180;
            };
            double width = spikes[i][2] - spikes[i][0];
            double height = spikes[i][3] - spikes[i][1];
            if (i == 0 || i == 4) {
                double temp = width;
                width = height;
                height = temp;
            }
            StdDraw.picture((spikes[i][0] + spikes[i][2]) / 2.0, (spikes[i][1] + spikes[i][3]) / 2.0,
                            imageSpike, width, height, degree);
        }

        // Draw pipes
        StdDraw.setPenColor(Color.ORANGE);
        StdDraw.filledRectangle((startPipe[0][0] + startPipe[0][2]) / 2.0,
                (startPipe[0][1] + startPipe[0][3]) / 2.0, (startPipe[0][2] - startPipe[0][0]) / 2.0,
                (startPipe[0][3] - startPipe[0][1]) / 2.0);
        StdDraw.filledRectangle((startPipe[1][0] + startPipe[1][2]) / 2.0,
                (startPipe[1][1] + startPipe[1][3]) / 2.0, (startPipe[1][2] - startPipe[1][0]) / 2.0,
                (startPipe[1][3] - startPipe[1][1]) / 2.0);
        StdDraw.filledRectangle((exitPipe[0][0] + exitPipe[0][2]) / 2.0,
                (exitPipe[0][1] + exitPipe[0][3]) / 2.0, (exitPipe[0][2] - exitPipe[0][0]) / 2.0,
                (exitPipe[0][3] - exitPipe[0][1]) / 2.0);
        StdDraw.filledRectangle((exitPipe[1][0] + exitPipe[1][2]) / 2.0,
                (exitPipe[1][1] + exitPipe[1][3]) / 2.0, (exitPipe[1][2] - exitPipe[1][0]) / 2.0,
                (exitPipe[1][3] - exitPipe[1][1]) / 2.0);

        // Draw door
        if (!isDoorOpen) {
            StdDraw.setPenColor(StdDraw.GREEN);
            StdDraw.filledRectangle((door[0] + door[2]) / 2.0, (door[1] + door[3]) / 2.0,
                                    (door[2] - door[0]) / 2.0, (door[3] - door[1]) / 2.0);
        }

        // Draw button
        if (!buttonPressed) {
            StdDraw.setPenColor(StdDraw.RED);
            StdDraw.filledRectangle((button[0] + button[2]) / 2.0, (button[1] + button[3]) / 2.0,
                    (button[2] - button[0]) / 2.0, (button[3] - button[1]) / 2.0);
        }

        // Draw button floor
        StdDraw.setPenColor(StdDraw.DARK_GRAY);
        StdDraw.filledRectangle((buttonFloor[0] + buttonFloor[2]) / 2.0, (buttonFloor[1] + buttonFloor[3]) / 2.0,
                                (buttonFloor[2] - buttonFloor[0]) / 2.0, (buttonFloor[3] - buttonFloor[1]) / 2.0);

        StdDraw.setPenColor(new Color(56, 93, 172)); // Color of the area
        StdDraw.filledRectangle(400, 60, 400, 60); // Drawing timer area
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(250,85,"Help");
        StdDraw.rectangle(250,85,40,15); // Help button
        StdDraw.text(550,85,"Restart");
        StdDraw.rectangle(550,85,40,15); // Restart button
        StdDraw.text(400,20,"RESET THE GAME");
        StdDraw.rectangle(400,20,80,15); // Reset button
        StdDraw.text(100,75, "Level: 1");
    }

    public Player getPlayer() {
        return player;
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isPlayerDead() {
        return playerDead;
    }

    public void revivePlayer() {
        this.playerDead = false;
    }

    public void pressButton() {
        buttonPressNum++;
    }
}
