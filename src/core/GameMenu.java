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
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        ter = new TERenderer();

        while (true) {
            if (redraw) {
                System.out.println("Redrawing the screen...");
                StdDraw.clear(StdDraw.BLACK);

                if (player == null) {
                    drawLoginMenu(); // Initial menu
                } else if (!gameStarted) {
                    drawPostLoginMenu(player); // Menu after login
                } else {
                    System.out.println("Rendering game world...");
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
        StdDraw.clear(StdDraw.BLACK);
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
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
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
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(0.5, 0.6, "Enter Username: " + usernameBuilder);
            StdDraw.show();

            // Add a small pause to prevent excessive CPU usage
            StdDraw.pause(20);
        }

        String username = usernameBuilder.toString().trim();
        Player loadedPlayer = PlayerStorage.loadPlayer(username);

        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
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
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(0.5, 0.6, "Enter seed for world generation or press R for a random world: ");
        StdDraw.show();

        StringBuilder seedInput = new StringBuilder();
        boolean randomSeed = false;

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == 'r' || key == 'R') {
                    randomSeed = true;
                    System.out.println("Random seed selected.");
                    break;
                } else if (Character.isDigit(key) && seedInput.length() < 18) { // Limit seed length
                    seedInput.append(key);
                    StdDraw.clear(StdDraw.BLACK);
                    StdDraw.setPenColor(StdDraw.WHITE);
                    StdDraw.text(0.5, 0.6, "Enter seed: " + seedInput);
                    StdDraw.show();
                } else if (key == '\n' || key == '\r') {
                    System.out.println("Seed entered: " + seedInput.toString());
                    break;
                }
            }
        }

        long seed;
        try {
            seed = randomSeed
                    ? System.currentTimeMillis() // Generate a random seed if player skips
                    : Long.parseLong(seedInput.toString());
        } catch (NumberFormatException e) {
            System.out.println("Invalid seed entered. Using random seed.");
            seed = System.currentTimeMillis();
        }

        // Reset the game state
        gameStarted = true;
        redraw = true;

        // Initialize a new world with the given seed and player
        world = new World(player, seed);
        System.out.println("New world created with seed: " + seed);
        drawWorld();
        System.out.println("World drawn.");
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
                checkObjectiveCompletion();
                break;
        }
    }

    private static void checkObjectiveCompletion() {
        if (world.getAvatarX() == world.getDoorX() && world.getAvatarY() == world.getDoorY()) {
            player.addPoints(100); // Award points for reaching the door
            System.out.println("Objective completed! Points awarded: 100");

            // Reapply coordinate system before drawing
            StdDraw.setXscale(0, 1);
            StdDraw.setYscale(0, 1);

            // Display the completion message on the screen
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(0.5, 0.5, "Objective completed! You've reached the door.");
            StdDraw.show();
            StdDraw.pause(2000); // Pause for 2 seconds to allow the user to read the message

            // Reset game state to show the post-login menu
            gameStarted = false;
            redraw = true;
            System.out.println("Game state reset to post-login menu.");
        }
    }

    public static void saveGame(Player player) {
        String fileName = "game_data.txt";
        try {
            String contents = player.getUsername() + "\n" + world.getSeed() + "\n" + world.getAvatarX() + "\n" +
                    world.getAvatarY() + "\n" + world.getChaseX() + "\n" +
                    world.getChaseY() + "\n" + player.getPoints();
            FileUtils.writeFile(fileName, contents);
            PlayerStorage.savePlayer(player); // Save player data
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void loadGame(Player player) {
        String fileName = "game_data.txt";
        try {
            String contents = FileUtils.readFile(fileName);
            String[] lines = contents.split("\n");

            // Check if the saved game belongs to the current player
            if (!lines[0].equals(player.getUsername())) {
                // Clear the screen and display the message
                StdDraw.clear(StdDraw.BLACK);
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.text(0.5, 0.5, "No saved gamefound for this player.");
                StdDraw.show();
                StdDraw.pause(2000); // Pause for 2 seconds to allow the user to read the message
                return;
            }

            world = new World(player, Long.parseLong(lines[1])); // Pass the player
            world.setAvatarToNewPosition(Integer.parseInt(lines[2]), Integer.parseInt(lines[3]));
            world.setChaserToNewPosition(Integer.parseInt(lines[4]), Integer.parseInt(lines[5]));
            gameStarted = true;
            drawWorld();
        } catch (RuntimeException e) {
            e.printStackTrace(); // Ensure exceptions are printed
        }
    }

}
