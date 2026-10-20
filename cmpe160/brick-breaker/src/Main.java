import java.awt.*;

public class Main {
    public static void main(String[] args) {

        StdDraw.enableDoubleBuffering();
        // Canvas properties, scale and set the canvas with the given parameters
        double xScale = 800.0, yScale = 400.0;
        StdDraw.setCanvasSize(800, 400);
        StdDraw.setXscale(0.0, xScale);
        StdDraw.setYscale(0.0, yScale);
        // Color array for bricks (first import java.awt.Color )
        Color[] colors = { new Color(255, 0, 0), new Color(220, 20, 60),
                new Color(178, 34, 34), new Color(139, 0, 0),
                new Color(255, 69, 0), new Color(165, 42, 42)
        };
        // Game Components (These can be changed for custom scenarios)
        double ballRadius = 8; // Ball radius
        double initialBallVelocity = 5; // Magnitude of the initial ball velocity
        Color ballColor = new Color(15, 82, 186); // Color of the ball
        double[] ballPos = {400,18}; //Initial position of the ball in the format {x, y}
        double[] paddlePos = {400, 5}; // Initial position of the center of the paddle
        double paddleHalfwidth = 60; // Paddle half width
        double paddleHalfheight = 5; // Paddle half height
        double paddleSpeed = 20; // Paddle speed
        Color paddleColor = new Color(128, 128, 128); // Paddle color
        double brickHalfwidth = 50; // Brick half width
        double brickHalfheight = 10; // Brick half height
        // 2D array to store center coordinates of bricks in the format {x, y}
        double[][] brickCoordinates = new double[][]{
                                     {250, 320},{350, 320},{450, 320},{550, 320},
                          {150, 300},{250, 300},{350, 300},{450, 300},{550, 300},{650, 300},
                {50, 280},{150, 280},{250, 280},{350, 280},{450, 280},{550, 280},{650, 280},{750, 280},
                {50, 260},{150, 260},{250, 260},{350, 260},{450, 260},{550, 260},{650, 260},{750, 260},
                {50, 240},{150, 240},{250, 240},{350, 240},{450, 240},{550, 240},{650, 240},{750, 240},
                          {150, 220},{250, 220},{350, 220},{450, 220},{550, 220},{650, 220},
                                     {250, 200},{350, 200},{450, 200},{550, 200}};
        // Brick colors
        Color[] brickColors = new Color[] {
                colors[0], colors[1], colors[2], colors[3],
                colors[2], colors[4], colors[3], colors[0], colors[4], colors[5],
                colors[5], colors[0], colors[1], colors[5], colors[2], colors[3], colors[0], colors[4],
                colors[1], colors[3], colors[2], colors[4], colors[0], colors[5], colors[2], colors[1],
                colors[4], colors[0], colors[5], colors[1], colors[2], colors[3], colors[0], colors[5],
                colors[1], colors[4], colors[0], colors[5], colors[1], colors[2],
                colors[3], colors[2], colors[3], colors[0]};

        boolean[] destroyedBricks = new boolean[brickCoordinates.length];

        boolean isPaused = false;
        // Initial values for physics calculations
        double ballXVelocity = 0.0;
        double ballYVelocity = 0.0;
        double nextXVelocity = 0.0;
        double nextYVelocity = 0.0;

        // Changeable variables
        int shootingLineLength = 30;
        double shootingAngle = Math.PI / 2;
        int fps = 60;
        int score = 0;
        int brickPoints = 10;

        while (true) {

            StdDraw.pause(1000 / fps);
            
            // Pause function
            if (StdDraw.hasNextKeyTyped()) {
                char keyTyped = StdDraw.nextKeyTyped();
                if (keyTyped == ' ')
                    isPaused = !isPaused;
            }

            // Skip game logic if paused
            if (isPaused) {
                StdDraw.setFont(new Font("Default", Font.BOLD, 20));
                StdDraw.text(xScale / 2, yScale - 20, "PAUSED");
                StdDraw.show();
                StdDraw.setFont(); // Reset to default font
                continue;
            }

            StdDraw.clear();

            // Draw objects
            StdDraw.setPenColor(ballColor);
            StdDraw.filledCircle(ballPos[0], ballPos[1], ballRadius);
            StdDraw.setPenColor(paddleColor);
            StdDraw.filledRectangle(paddlePos[0], paddlePos[1], paddleHalfwidth, paddleHalfheight);

            for (int i = 0; i < brickCoordinates.length; i++) {
                if (!destroyedBricks[i]) {
                    StdDraw.setPenColor(brickColors[i]);
                    StdDraw.filledRectangle(brickCoordinates[i][0], brickCoordinates[i][1], brickHalfwidth, brickHalfheight);
                }
            }

            // Win or lose condition
            if (ballPos[1] + ballYVelocity <= 0 || score == brickPoints * brickCoordinates.length) {
                break;
            }

            // Initial start, show shooting angle and line to launch the ball
            if (ballXVelocity == 0 && ballYVelocity == 0) {
                StdDraw.setPenRadius(0.005);
                StdDraw.setPenColor(StdDraw.RED);
                StdDraw.line(ballPos[0], ballPos[1],
                        ballPos[0] + Math.cos(shootingAngle) * shootingLineLength,
                        ballPos[1] + Math.sin(shootingAngle) * shootingLineLength);
                // Left arrow key
                if (StdDraw.isKeyPressed(37) && shootingAngle < Math.PI - Math.PI / 180)
                    shootingAngle += Math.PI / 180;
                // Right arrow key
                if (StdDraw.isKeyPressed(39) && shootingAngle > 0)
                    shootingAngle -= Math.PI / 180;
                // Up arrow key
                if (StdDraw.isKeyPressed(38)) {
                    nextXVelocity = Math.cos(shootingAngle) * initialBallVelocity;
                    nextYVelocity = Math.sin(shootingAngle) * initialBallVelocity;
                    ballXVelocity = nextXVelocity;
                    ballYVelocity = nextYVelocity;
                }
                // Display angle in degrees in upper left corner
                StdDraw.setPenColor();
                StdDraw.text(50, yScale - 20, String.format("Angle: %.1f°", Math.toDegrees(shootingAngle)));
                StdDraw.show();
                continue;
            }

            StdDraw.setPenColor();

            // Handle keyboard input for paddle movement
            if (StdDraw.isKeyPressed(37)) { // Left arrow key
                if (paddlePos[0] - paddleSpeed >= paddleHalfwidth) {
                    paddlePos[0] -= paddleSpeed;
                }
            }
            if (StdDraw.isKeyPressed(39)) { // Right arrow key
                if (paddlePos[0] + paddleSpeed <= xScale - paddleHalfwidth) {
                    paddlePos[0] += paddleSpeed;
                }
            }

            // Set next velocities to be used in case of no collision
            double nextX = ballPos[0] + ballXVelocity;
            double nextY = ballPos[1] + ballYVelocity;
            // Variables for collision calculations
            double nx = 0;
            double ny = 0;

            // Paddle collision calculations
            // These are same for both paddle and bricks
            if (nextY <= paddlePos[1] + paddleHalfheight + ballRadius) {
                // Collision checks for calculations later on
                boolean horizontalCollision = 
                    nextX + ballRadius >= paddlePos[0] - paddleHalfwidth &&
                    nextX - ballRadius <= paddlePos[0] + paddleHalfwidth &&
                    ballPos[1] >= paddlePos[1] - paddleHalfheight &&
                    ballPos[1] <= paddlePos[1] + paddleHalfheight;
                
                boolean verticalCollision = 
                    nextY + ballRadius >= paddlePos[1] - paddleHalfheight &&
                    nextY - ballRadius <= paddlePos[1] + paddleHalfheight &&
                    ballPos[0] >= paddlePos[0] - paddleHalfwidth &&
                    ballPos[0] <= paddlePos[0] + paddleHalfwidth;

                boolean cornerCollision = false;

                // Check all four corners of the paddle
                if (ballRadius >= Math.sqrt(Math.pow(nextX - paddlePos[0] - paddleHalfwidth, 2) +
                    Math.pow(nextY - paddlePos[1] - paddleHalfheight, 2))) {
                    cornerCollision = true;
                    nx = nextX - paddlePos[0] - paddleHalfwidth;
                    ny = nextY - paddlePos[1] - paddleHalfheight;
                }
                else if (ballRadius >= Math.sqrt(Math.pow(nextX - paddlePos[0] + paddleHalfwidth, 2) +
                         Math.pow(nextY - paddlePos[1] - paddleHalfheight, 2))) {
                    cornerCollision = true;
                    nx = nextX - paddlePos[0] + paddleHalfwidth;
                    ny = nextY - paddlePos[1] - paddleHalfheight;
                }
                else if (ballRadius >= Math.sqrt(Math.pow(nextX - paddlePos[0] - paddleHalfwidth, 2) +
                         Math.pow(nextY - paddlePos[1] + paddleHalfheight, 2))) {
                    cornerCollision = true;
                    nx = nextX - paddlePos[0] - paddleHalfwidth;
                    ny = nextY - paddlePos[1] + paddleHalfheight;
                }
                else if (ballRadius >= Math.sqrt(Math.pow(nextX - paddlePos[0] + paddleHalfwidth, 2) +
                         Math.pow(nextY - paddlePos[1] + paddleHalfheight, 2))) {
                    cornerCollision = true;
                    nx = nextX - paddlePos[0] + paddleHalfwidth;
                    ny = nextY - paddlePos[1] + paddleHalfheight;
                }

                if (cornerCollision || horizontalCollision || verticalCollision) {
                    if (cornerCollision) {
                        // Use vector reflection formula to calculate new velocity
                        // Normalize the normal vector
                        double length = Math.sqrt(nx * nx + ny * ny);
                        if (length != 0) { // Avoid division by zero
                            nx /= length;
                            ny /= length;
                            double dot = ballXVelocity * nx + ballYVelocity * ny;
                            // Reflect the ball's velocity vector
                            nextXVelocity = ballXVelocity - 2 * dot * nx;
                            nextYVelocity = ballYVelocity - 2 * dot * ny;
                        }
                    // Don't need to move ball outside paddle as it doesn't glitch into it..
                    // ..usually
                    } else if (horizontalCollision) {
                        nextXVelocity = -ballXVelocity;
                    } else if (verticalCollision) {
                        nextYVelocity = -ballYVelocity;
                    }
                }
            }

            // Boundary checks for the ball
            if (ballPos[0] + ballXVelocity > xScale - ballRadius || ballPos[0] + ballXVelocity < ballRadius)
                nextXVelocity = -ballXVelocity;
            if (ballPos[1] + ballYVelocity > yScale - ballRadius)
                nextYVelocity = -ballYVelocity;

            // Collision calculations with bricks
            for (int i = 0; i < brickCoordinates.length; i++) {
                if (!destroyedBricks[i]) {
                    nextX = ballPos[0] + ballXVelocity;
                    nextY = ballPos[1] + ballYVelocity;
                    nx = 0;
                    ny = 0;

                    // Collision checks for calculations later on
                    boolean horizontalCollision = 
                    nextX + ballRadius >= brickCoordinates[i][0] - brickHalfwidth &&
                    nextX - ballRadius <= brickCoordinates[i][0] + brickHalfwidth &&
                    ballPos[1] >= brickCoordinates[i][1] - brickHalfheight &&
                    ballPos[1] <= brickCoordinates[i][1] + brickHalfheight;
                    boolean verticalCollision = 
                    nextY + ballRadius >= brickCoordinates[i][1] - brickHalfheight &&
                    nextY - ballRadius <= brickCoordinates[i][1] + brickHalfheight &&
                    ballPos[0] >= brickCoordinates[i][0] - brickHalfwidth &&
                    ballPos[0] <= brickCoordinates[i][0] + brickHalfwidth;

                    boolean cornerCollision = false;
                    // Check all four corners of the brick
                    if (ballRadius >= Math.sqrt(Math.pow(nextX - brickCoordinates[i][0] - brickHalfwidth, 2) +
                                                Math.pow(nextY - brickCoordinates[i][1] - brickHalfheight, 2))) {
                        cornerCollision = true;
                        nx = nextX - brickCoordinates[i][0] - brickHalfwidth;
                        ny = nextY - brickCoordinates[i][1] - brickHalfheight;
                    }
                    else if (ballRadius >= Math.sqrt(Math.pow(nextX - brickCoordinates[i][0] + brickHalfwidth, 2) +
                                                    Math.pow(nextY - brickCoordinates[i][1] - brickHalfheight, 2))) {
                        cornerCollision = true;
                        nx = nextX - brickCoordinates[i][0] + brickHalfwidth;
                        ny = nextY - brickCoordinates[i][1] - brickHalfheight;
                    }
                    else if (ballRadius >= Math.sqrt(Math.pow(nextX - brickCoordinates[i][0] - brickHalfwidth, 2) +
                                                    Math.pow(nextY - brickCoordinates[i][1] + brickHalfheight, 2))) {
                        cornerCollision = true;
                        nx = nextX - brickCoordinates[i][0] - brickHalfwidth;
                        ny = nextY - brickCoordinates[i][1] + brickHalfheight;
                    }
                    else if (ballRadius >= Math.sqrt(Math.pow(nextX - brickCoordinates[i][0] + brickHalfwidth, 2) +
                                                    Math.pow(nextY - brickCoordinates[i][1] + brickHalfheight, 2))) {
                        cornerCollision = true;
                        nx = nextX - brickCoordinates[i][0] + brickHalfwidth;
                        ny = nextY - brickCoordinates[i][1] + brickHalfheight;
                    }

                    if (cornerCollision || horizontalCollision || verticalCollision) {
                        destroyedBricks[i] = true;
                        if (cornerCollision) {
                            // Use vector reflection formula to calculate new velocity

                            // Normalize the normal vector
                            double length = Math.sqrt(nx * nx + ny * ny);
                            if (length == 0) continue; // Avoid division by zero
                            nx /= length;
                            ny /= length;
                            
                            double dot = ballXVelocity * nx + ballYVelocity * ny;
                            
                            // Reflect the ball's velocity vector
                            nextXVelocity = ballXVelocity - 2 * dot * nx;
                            nextYVelocity = ballYVelocity - 2 * dot * ny;
                            
                            score += brickPoints;
                            continue;
                        }
                        
                        if (horizontalCollision) {
                            // Move ball outside brick before changing velocity
                            if (ballXVelocity > 0) {
                                ballPos[0] = brickCoordinates[i][0] - brickHalfwidth - ballRadius;
                            } else {
                                ballPos[0] = brickCoordinates[i][0] + brickHalfwidth + ballRadius;
                            }
                            nextXVelocity = -ballXVelocity;
                        }
                        if (verticalCollision) {
                            // Move ball outside brick before changing velocity
                            if (ballYVelocity > 0) {
                                ballPos[1] = brickCoordinates[i][1] - brickHalfheight - ballRadius;
                            } else {
                                ballPos[1] = brickCoordinates[i][1] + brickHalfheight + ballRadius;
                            }
                            nextYVelocity = -ballYVelocity;
                        }
                        score += brickPoints;
                    }
                }
            }

            

            // Update ball velocity and position
            if (nextXVelocity != 0.0) {
                ballXVelocity = nextXVelocity;
                nextXVelocity = 0.0;
            }
            if (nextYVelocity != 0.0) {
                ballYVelocity = nextYVelocity;
                nextYVelocity = 0.0;
            }
            ballPos[0] += ballXVelocity;
            ballPos[1] += ballYVelocity;

            StdDraw.text(xScale - 50, yScale - 20, "Score: " + score);

            StdDraw.show();
        }

        // Game end screen
        StdDraw.setPenColor();
        // If score is max possible score, the player won
        if (score == brickPoints * brickCoordinates.length)  {
            StdDraw.setFont(new Font("Default", Font.BOLD, 40));
            StdDraw.text(xScale / 2, yScale / 4, "VICTORY!");
        } else {
            StdDraw.setFont(new Font("Default", Font.BOLD, 40));
            StdDraw.text(xScale / 2, yScale / 4, "GAME OVER!");
        }
        StdDraw.setFont();
        StdDraw.text(xScale / 2, yScale / 4 - 30, "Score: " + score);
        StdDraw.show();
    }
}