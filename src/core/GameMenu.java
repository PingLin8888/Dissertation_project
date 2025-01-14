package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import utils.FileUtils;

import java.awt.*;

/**
 * Inspired by GPT.
 */
public class GameMenu {
    private static World world;
    private static TERenderer ter;
    // private static StringBuilder seedBuilder = new StringBuilder();
    private static StringBuilder quitSignBuilder = new StringBuilder();
    // private static boolean enteringSeed = false;
    private static boolean gameStarted = false;
    private static boolean redraw = true;
    private double prevMouseX = 0;
    private double prevMouseY = 0;
    private long lastChaserMoveTime = 0; // Variable to track the last time the chaser moved
    private static final long CHASER_MOVE_INTERVAL = 500; // Interval in milliseconds between chaser movements

    private static Player player = null;

    public void createGameMenu() {
        StdDraw.setCanvasSize(800, 600);
        ter = new TERenderer();

        while (true) {
            if (redraw) {
                StdDraw.clear(StdDraw.BLACK);

                if (player == null) {
                    drawLoginMenu(); // Initial menu
                } else if (!gameStarted) {
                    drawPostLoginMenu(player); // Menu after login
                } else {
                    ter.renderFrame(world.getMap());
                    updateHUD();
                    if (world.isShowPath() && world.getPathToAvatar() != null) {
                        drawPath();
                    }
                }

                StdDraw.show();
                redraw = false;
            }

            handleInput();
            detectMouseMove();
            StdDraw.pause(20);

            if (gameStarted) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastChaserMoveTime >= CHASER_MOVE_INTERVAL) {
                    world.moveChaser();
                    lastChaserMoveTime = currentTime; // Update the last move time
                    redraw = true;
                }
            }
        }
    }

    private static void drawLoginMenu() {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(0.5, 0.65, "Log In / Create Profile (P)");
        StdDraw.text(0.5, 0.5, "Quit (Q)");
        StdDraw.show();

    }

    private static void drawPostLoginMenu(Player player) {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(0.5, 0.8, "Welcome, " + player.getUsername());
        StdDraw.text(0.5, 0.7, "Points: " + player.getPoints());
        StdDraw.text(0.5, 0.6, "New Game (N)");
        StdDraw.text(0.5, 0.5, "Load Game (L)");
        StdDraw.text(0.5, 0.4, "Quit (Q)");
        StdDraw.show();

    }

    private void drawPath() {
        StdDraw.setPenColor(StdDraw.BOOK_RED);
        for (Point p : world.getPathToAvatar()) {
            StdDraw.filledSquare(p.x + 0.5, p.y + 0.5, 0.5);
        }
    }

    private void detectMouseMove() {
        double currentMouseX = StdDraw.mouseX();
        double currentMouseY = StdDraw.mouseY();

        if (hasMouseMoved(currentMouseX, currentMouseY)) {

            // Update previous mouse position
            prevMouseX = currentMouseX;
            prevMouseY = currentMouseY;
            redraw = true;
        }
    }

    private boolean hasMouseMoved(double currentMouseX, double currentMouseY) {
        return currentMouseX != prevMouseX || currentMouseY != prevMouseY;
    }

    private void updateHUD() {
        // Example HUD update method
        String description;
        int mouseX = (int) Math.floor(prevMouseX);
        int mouseY = (int) Math.floor(prevMouseY);

        if (mouseX >= 0 && mouseX < world.getMap().length && mouseY >= 0 && mouseY < world.getMap()[0].length) {
            TETile tile = world.getMap()[mouseX][mouseY];
            description = tile.description();
        } else {
            description = "out side of map";
        }
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.textLeft(0.01, 0.99, description);
    }

    private static void handleInput() {
        if (StdDraw.hasNextKeyTyped()) {
            char key = Character.toLowerCase(StdDraw.nextKeyTyped());
            redraw = true;

            if (player == null) {
                // Initial menu for login or quit
                switch (key) {
                    case 'p': // Login or create a player
                        player = loginOrCreateProfile();
                        redraw = true;
                        break;
                    case 'q':
                        System.exit(0);
                        break;
                }
            } else if (!gameStarted) {
                // Post-login menu options
                switch (key) {
                    case 'n':
                        createNewGame(player);
                        break;
                    case 'l':
                        loadGame(player);
                        break;
                    case 'q':
                        System.exit(0);
                        break;
                }
            } else {
                // Game started: Handle in-game inputs
                if (key == ':') {
                    quitSignBuilder.setLength(0);
                    quitSignBuilder.append(key);
                } else if (key == 'q' && quitSignBuilder.toString().equals(":")) {
                    saveGame(player);
                    System.exit(0);
                } else if (key == 'p') {
                    world.togglePathDisplay();
                }
                handleMovement(key);
            }
        }
    }

    private static Player loginOrCreateProfile() {
        StdDraw.clear();
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.text(0.5, 0.6, "Enter Username: ");
        StdDraw.show();
        StringBuilder usernameBuilder = new StringBuilder();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == '\n' || key == '\r') {
                    break;
                }
                usernameBuilder.append(key);
            }

            // Clear and redraw the screen with the current username input
            StdDraw.clear();
            StdDraw.setPenColor(StdDraw.BLACK);
            StdDraw.text(0.5, 0.6, "Enter Username: " + usernameBuilder);
            StdDraw.show();

            // Add a small pause to prevent excessive CPU usage
            StdDraw.pause(20);
        }

        String username = usernameBuilder.toString().trim();
        Player loadedPlayer = PlayerStorage.loadPlayer(username);

        StdDraw.clear();
        StdDraw.setPenColor(StdDraw.BLACK);
        if (loadedPlayer == null) {
            StdDraw.text(0.5, 0.5, "Creating new profile for: " + username);
            StdDraw.show();
            return new Player(username);
        } else {
            StdDraw.text(0.5, 0.5, "Welcome back, " + username + "! Points: " + loadedPlayer.getPoints());
            StdDraw.show();
            return loadedPlayer;
        }
    }

    private static void createNewGame(Player player) {
        StdDraw.clear();
        StdDraw.text(0.5, 0.6, "Enter seed for world generation or press R for a random world: ");
        StdDraw.show();

        StringBuilder seedInput = new StringBuilder();
        boolean randomSeed = false;

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == 'r' || key == 'R') {// reaches
                    randomSeed = true;
                    break;
                } else if (Character.isDigit(key)) {
                    seedInput.append(key);
                    StdDraw.clear();
                    StdDraw.text(0.5, 0.6, "Enter seed: " + seedInput);
                    StdDraw.show();
                } else if (key == '\n' || key == '\r') {
                    break;
                }
            }
        }

        long seed = randomSeed
                ? System.currentTimeMillis() // Generate a random seed if player skips
                : Long.parseLong(seedInput.toString());

        world = new World(player, seed); // Pass seed and player
        gameStarted = true;
        drawWorld();
    }

    private static void drawWorld() {
        // StdDraw.clear();
        int width = world.getMap().length;
        int height = world.getMap()[0].length;
        ter.initialize(width, height);
        ter.renderFrame(world.getMap());
    }

    private static void handleMovement(char key) {
        switch (Character.toLowerCase(key)) {
            case 'w':
            case 'a':
            case 's':
            case 'd':
                world.moveAvatar(key);
                break;
        }
    }

    public static void saveGame(Player player) {
        String fileName = "save-file.txt";
        try {
            String contents = world.getSeed() + "\n" + world.getAvatarX() + "\n" +
                    world.getAvatarY() + "\n" + world.getChaseX() + "\n" +
                    world.getChaseY() + "\n" + player.getUsername() + "\n" +
                    player.getPoints();
            FileUtils.writeFile(fileName, contents);
            PlayerStorage.savePlayer(player); // Save player data
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void loadGame(Player player) {
        String fileName = "save-file-" + player.getUsername() + ".txt";
        try {
            String contents = FileUtils.readFile(fileName);
            String[] lines = contents.split("\n");
            world = new World(player, Long.parseLong(lines[0])); // Pass the player
            world.setAvatarToNewPosition(Integer.parseInt(lines[1]), Integer.parseInt(lines[2]));
            world.setChaserToNewPosition(Integer.parseInt(lines[3]), Integer.parseInt(lines[4]));
            gameStarted = true;
            drawWorld();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

}
