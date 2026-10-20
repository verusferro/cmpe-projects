import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class Game {

    private int stageIndex;
    private ArrayList<Stage> stages;
    private int deathNumber;
    private double gameTime;
    private double resetTime;
    private boolean resetGame;
    private Map map;
    private boolean showHelp;
    private boolean wasMousePressed;

    public Game(ArrayList<Stage> stages) {
        this.stages = stages;
        this.deathNumber = 0;
        this.resetGame = false;
        this.wasMousePressed = false;
    }

    public void play() {

        resetTime = System.currentTimeMillis();
        String timeFormatted = "";
        Player player = new Player(0,0);

        for (stageIndex = 0; stageIndex < stages.size(); stageIndex++) {

            showHelp = false;
            map = new Map(getCurrentStage(), player);

            do {
                StdDraw.pause(20);
                StdDraw.clear();

                handleInput(map);

                if (resetGame) {
                    resetGame = false;
                    continue;
                }

                // Handle player death
                if (map.isPlayerDead()) {
                    deathNumber++;
                    map.restartStage();
                    map.revivePlayer();
                }

                map.buttonCheck();

                player.draw();
                map.draw();

                // Show help or clue
                if (showHelp) {
                    StdDraw.text(400, 85, "Help:");
                    StdDraw.text(400, 55, getCurrentStage().getHelp());
                } else {
                    StdDraw.text(400, 85, "Clue:");
                    StdDraw.text(400, 55, getCurrentStage().getClue());
                }

                // Banner stuff
                StdDraw.text(700, 75, "Deaths: " + deathNumber);
                StdDraw.text(700, 50, "Stage: " + (getStageIndex() + 1));
                gameTime = System.currentTimeMillis() - resetTime;
                // Convert to MM:SS:MsMs
                timeFormatted = String.format("%02d : %02d : %02d", ((int)gameTime / (60 * 1000)),
                        ((int)gameTime / 1000 % 60), ((int)gameTime % 1000) / 10);
                StdDraw.text(100, 50, timeFormatted);
                StdDraw.show();
            } while (!map.changeStage());

            // If game is finished, wait until further input
            if (getStageIndex() == stages.size() - 1) {
                StdDraw.setPenColor(StdDraw.GREEN);
                StdDraw.filledRectangle(400, 300, 400, 75);
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.setFont(new Font("Sans Serif", Font.PLAIN, 30));
                StdDraw.text(400, 340, "CONGRATULATIONS YOU FINISHED THE LEVEL");
                StdDraw.text(400, 300, "PRESS 'A' TO PLAY AGAIN!");
                StdDraw.setFont();
                StdDraw.text(400, 260, "You finished with " + deathNumber +
                        " deaths in " + timeFormatted);
                StdDraw.show();

                while (true) {
                    if (StdDraw.isKeyPressed(KeyEvent.VK_A)) {
                        this.play(); // Restart
                    } else if (StdDraw.isKeyPressed(KeyEvent.VK_Q)) {
                        System.exit(0); // Quit
                    }
                }
            }

            // Stage change banner
            StdDraw.setPenColor(StdDraw.GREEN);
            StdDraw.filledRectangle(400, 300, 400, 75);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.setFont(new Font("Sans Serif", Font.PLAIN, 40));
            StdDraw.text(400, 325, "You passed the stage");
            StdDraw.text(400, 275, "But is the level over?!");
            StdDraw.setFont();
            StdDraw.show();

            // 2 second wait
            long pauseStart = System.currentTimeMillis();
            long pauseDuration;
            do {
                pauseDuration = System.currentTimeMillis() - pauseStart;
            } while (pauseDuration < 2000);
            // Offset the timer by the wait
            resetTime += pauseDuration;

        }
    }

    private void handleInput(Map map) {

        // Track mouse press state to prevent hold click
        if (!StdDraw.isMousePressed()) wasMousePressed = false;

        if (StdDraw.isMousePressed()) {
            // Display help
            if (StdDraw.mouseX() >= 210 && StdDraw.mouseX() <= 290 && StdDraw.mouseY() >= 70 && StdDraw.mouseY() <= 100) {
                showHelp = true;
            }

            // Reset game
            if (StdDraw.mouseX() >= 320 && StdDraw.mouseX() <= 480 && StdDraw.mouseY() >= 5 && StdDraw.mouseY() <= 35) {

                StdDraw.setPenColor(StdDraw.GREEN);
                StdDraw.filledRectangle(400, 300, 400, 75);
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.setFont(new Font("Sans Serif", Font.PLAIN, 50));
                StdDraw.text(400, 300, "RESETTING THE GAME...");
                StdDraw.setFont();
                StdDraw.show();

                // 2 second wait
                long pauseStart = System.currentTimeMillis();
                long pauseDuration;
                do {
                    pauseDuration = System.currentTimeMillis() - pauseStart;
                } while (pauseDuration < 2000);

                stageIndex = 0;
                resetTime = System.currentTimeMillis();
                showHelp = false;
                deathNumber = 0;
                this.map = new Map(getCurrentStage(), map.getPlayer());
                map.restartStage();
                resetGame = true;
            }

            // Restart stage
            if (StdDraw.mouseX() >= 510 && StdDraw.mouseX() <= 590 && StdDraw.mouseY() >= 70 && StdDraw.mouseY() <= 100 && !wasMousePressed) {
                map.restartStage();
                deathNumber++;
                wasMousePressed = true;
            }
        }

        // Player movement
        int[] keyCodes = getCurrentStage().getKeyCodes();
        if (StdDraw.isKeyPressed(keyCodes[0]) && StdDraw.isKeyPressed(keyCodes[2])) {
            map.movePlayer('E'); // Up right
        } else if (StdDraw.isKeyPressed(keyCodes[1]) && StdDraw.isKeyPressed(keyCodes[2])) {
            map.movePlayer('Q'); // Up left
        } else if (StdDraw.isKeyPressed(keyCodes[2])) {
            map.movePlayer('U'); // Up
        } else if (StdDraw.isKeyPressed(keyCodes[0])) {
            map.movePlayer('R'); // Right
        } else if (StdDraw.isKeyPressed(keyCodes[1])) {
            map.movePlayer('L'); // Left
        } else {
            map.movePlayer('0'); // No movement
        }
    }

    public Stage getCurrentStage() {
        return stages.get(stageIndex);
    }

    public int getStageIndex() {
        return stageIndex;
    }
}
