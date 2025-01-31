package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import utils.FileUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspired by GPT.
 */

// Enum to represent supported languages
enum Language {
    ENGLISH, CHINESE // Add more languages as needed
}

public class GameMenu implements EventListener {
    private World world;
    private TERenderer ter;
    private StringBuilder quitSignBuilder = new StringBuilder();
    private boolean gameStarted = false;
    private boolean redraw = true;
    private double prevMouseX = 0;
    private double prevMouseY = 0;
    private long lastChaserMoveTime = 0; // Variable to track the last time the chaser moved
    private static final long CHASER_MOVE_INTERVAL = 5000; // Reduced interval for faster chaser movement

    private Player player = null;
    private Language currentLanguage = Language.ENGLISH; // Default language
    private TranslationManager translationManager;
    private List<Notification> notifications = new ArrayList<>();

    public GameMenu() {
        initializeTranslations();
    }

    private void initializeTranslations() {
        translationManager = new TranslationManager(currentLanguage);
    }

    public void createGameMenu() throws InterruptedException {
        setupCanvas();
        ter = new TERenderer();
        StdDraw.enableDoubleBuffering(); // Enable double buffering

        // Language selection toggle
        toggleLanguageSelection();

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
                    renderNotifications();
                }
                StdDraw.show(); // Show the buffer
                redraw = false; // Reset redraw flag
            }

            handleInput();
            detectMouseMove();
            StdDraw.pause(16); // Approximately 60 FPS

            if (gameStarted) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastChaserMoveTime >= CHASER_MOVE_INTERVAL) {
                    world.moveChaser();
                    checkChaserEncounter();
                    lastChaserMoveTime = currentTime;
                    redraw = true;
                }
            }
        }
    }

    private void checkChaserEncounter() {
        if ((Math.abs(world.getChaserX() - world.getAvatarX()) == 1 &&
                world.getChaserY() == world.getAvatarY()) ||
                (Math.abs(world.getChaserY() - world.getAvatarY()) == 1 &&
                        world.getChaserX() == world.getAvatarX())) {

            // Play game over sound
            AudioManager.getInstance().playSound("gameover");

            // End the game and redirect to the post-login menu
            System.out.println("Chaser is adjacent to the avatar! Ending game.");

            StdDraw.setXscale(0, 1);
            StdDraw.setYscale(0, 1);

            // Clear the screen and display the message
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(0.5, 0.5, translationManager.getTranslation("game_over"));
            StdDraw.show();
            StdDraw.pause(2000); // Pause for 2 seconds to allow the user to read the message

            // Reset game state to show the post-login menu
            gameStarted = false;
            redraw = true;
            System.out.println("Game state reset to post-login menu.");
        }
    }

    private void setupCanvas() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        StdDraw.setCanvasSize(screenSize.width, screenSize.height);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
    }

    private void toggleLanguageSelection() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        // Display language selection prompt
        StdDraw.text(0.5, 0.6, "Select Language");
        StdDraw.text(0.5, 0.5, "Press E for English");
        StdDraw.text(0.5, 0.4, "按 'C' 选择中文");
        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'e') {
                    AudioManager.getInstance().playSound("menu");
                    currentLanguage = Language.ENGLISH;
                    break;
                } else if (key == 'c') {
                    AudioManager.getInstance().playSound("menu");
                    currentLanguage = Language.CHINESE;
                    break;
                }
            }
        }
        initializeTranslations(); // Reinitialize translations after language selection
    }

    private void drawLoginMenu() {

        setupCanvas();
        // Load a font that supports Chinese characters
        Font font = new Font("SimSun", Font.PLAIN, 24); // Ensure this font is available
        StdDraw.setFont(font); // Set the font in StdDraw

        StdDraw.clear(StdDraw.BLACK); // Clear the canvas
        StdDraw.setPenColor(Color.WHITE);

        // Debugging output
        String loginText = translationManager.getTranslation("login");
        String quitText = translationManager.getTranslation("quit");

        // Draw the text
        StdDraw.text(0.5, 0.65, loginText);
        StdDraw.text(0.5, 0.5, quitText);
        StdDraw.show();
    }

    private void drawPostLoginMenu(Player player) {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(Color.WHITE);

        // Load a font that supports Chinese characters
        Font font = new Font("SimSun", Font.PLAIN, 24); // Change "SimSun" to your desired font
        StdDraw.setFont(font); // Set the font in StdDraw
        StdDraw.text(0.5, 0.8, translationManager.getTranslation("welcome", player.getUsername()));
        StdDraw.text(0.5, 0.7, translationManager.getTranslation("points", player.getPoints()));
        StdDraw.text(0.5, 0.6, translationManager.getTranslation("new_game"));
        StdDraw.text(0.5, 0.5, translationManager.getTranslation("load_game"));
        StdDraw.text(0.5, 0.4, translationManager.getTranslation("quit"));
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
        // Get mouse position and convert to tile coordinates
        double mouseX = StdDraw.mouseX();
        double mouseY = StdDraw.mouseY();

        // Only update if mouse has moved
        if (mouseX != prevMouseX || mouseY != prevMouseY) {
            redraw = true;
            prevMouseX = mouseX;
            prevMouseY = mouseY;
        }

        // Get the tile description at mouse position
        String description = getTileDescription(mouseX, mouseY);

        // Draw HUD background (optional - for better readability)
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.filledRectangle(0.5, 0.95, 0.5, 0.05);

        // Set text color
        StdDraw.setPenColor(StdDraw.WHITE);

        // Draw tile description on the left
        StdDraw.textLeft(0.01, 42, description);

        // Draw player name in the center
        StdDraw.textLeft(0.01, 44, "Player: " + player.getUsername());

        // Draw points on the right
        StdDraw.textLeft(0.01, 43, "Points: " + player.getPoints());
    }

    private String getTileDescription(double mouseX, double mouseY) {
        int tileX = (int) Math.floor(mouseX);
        int tileY = (int) Math.floor(mouseY);

        if (tileX >= 0 && tileX < world.getMap().length && tileY >= 0 && tileY < world.getMap()[0].length) {
            TETile tile = world.getMap()[tileX][tileY];
            return tile.description();
        } else {
            return "out side of map";
        }
    }

    private void handleInput() throws InterruptedException {
        if (StdDraw.hasNextKeyTyped()) {
            char key = Character.toLowerCase(StdDraw.nextKeyTyped());
            redraw = true;

            if (player == null) {
                // Initial menu for login or quit
                switch (key) {
                    case 'p': // Login or create a player
                        AudioManager.getInstance().playSound("menu"); // Play sound only for valid menu action
                        player = loginOrCreateProfile();
                        redraw = true;
                        break;
                    case 'q':
                        AudioManager.getInstance().playSound("menu");
                        System.exit(0);
                        break;
                }
            } else if (!gameStarted) {
                // Post-login menu options
                switch (key) {
                    case 'n':
                        AudioManager.getInstance().playSound("menu");
                        createNewGame();
                        AudioManager.getInstance().playSound("gamestart");
                        break;
                    case 'l':
                        AudioManager.getInstance().playSound("menu");
                        loadGame(player);
                        AudioManager.getInstance().playSound("gamestart");
                        break;
                    case 'q':
                        AudioManager.getInstance().playSound("menu");
                        saveGame(player);
                        System.exit(0);
                        break;
                }
            } else {
                // Game started: Handle in-game inputs
                if (key == ':') {
                    quitSignBuilder.setLength(0);
                    quitSignBuilder.append(key);
                } else if (key == 'q' && quitSignBuilder.toString().equals(":")) {
                    AudioManager.getInstance().playSound("menu");
                    saveGame(player);
                    System.exit(0);
                } else if (key == 'z') {
                    AudioManager.getInstance().playSound("menu");
                    world.togglePathDisplay();// Show path
                } else if (key == 'w' || key == 'a' || key == 's' || key == 'd') {
                    handleMovement(key);
                }
            }
        }
    }

    private Player loginOrCreateProfile() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        // Use the translation for "Enter Username:"
        StdDraw.text(0.5, 0.6, translationManager.getTranslation("enter_username"));
        StdDraw.show();

        StringBuilder usernameBuilder = new StringBuilder();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                AudioManager.getInstance().playSound("menu");
                if (key == '\n' || key == '\r') {
                    break;
                }
                usernameBuilder.append(key);
            }

            // Clear and redraw the screen with the current username input
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.text(0.5, 0.6, translationManager.getTranslation("enter_username") + " " + usernameBuilder);
            StdDraw.show();

            // Add a small pause to prevent excessive CPU usage
            StdDraw.pause(80);
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
            return loadedPlayer;
        }
    }

    private void createNewGame() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        // Use the translation for "Enter seed for world generation or press R for a
        // random world:"
        StdDraw.text(0.5, 0.6, translationManager.getTranslation("enter_seed"));
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
                    StdDraw.text(0.5, 0.6, translationManager.getTranslation("enter_seed") + " " + seedInput);
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
            System.out.println("seed is: " + seed);
        } catch (NumberFormatException e) {
            System.out.println("Invalid seed entered. Using random seed.");
            seed = System.currentTimeMillis();
        }

        // Reset the game state
        gameStarted = true;
        System.out.println("before newing the world");
        // Initialize a new world with the given seed and player
        world = new World(player, seed);
        this.world.getEventDispatcher().addListener(this); // Register this GameMenu as a listener
        System.out.println("New world created with seed: " + seed);
        drawWorld();
    }

    private void drawWorld() {
        System.out.println("before drawing world");
        try {
            int width = world.getMap().length;
            int height = world.getMap()[0].length;
            ter.initialize(width, height);
            ter.renderFrame(world.getMap());
        } catch (Exception e) {
            System.err.println("Error drawing world: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMovement(char key) {
        switch (Character.toLowerCase(key)) {
            case 'w':
            case 'a':
            case 's':
            case 'd':
                if (world.moveAvatar(key)) {
                    AudioManager.getInstance().playSound("walkOnGrass");
                }
                checkObjectiveCompletion();
                break;
        }
    }

    private void checkObjectiveCompletion() {

        // Check if the avatar has reached the door
        if (world.getAvatarX() == world.getDoorX() && world.getAvatarY() == world.getDoorY()) {
            AudioManager.getInstance().playSound("gamePass");
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

    public void saveGame(Player player) {
        String fileName = "game_data.txt";
        try {
            StringBuilder contents = new StringBuilder();
            contents.append(player.getUsername()).append("\n");
            contents.append(world.getSeed()).append("\n");
            contents.append(world.getAvatarX()).append("\n");
            contents.append(world.getAvatarY()).append("\n");
            contents.append(world.getChaserX()).append("\n");
            contents.append(world.getChaserY()).append("\n");
            contents.append(player.getPoints()).append("\n");

            // Save door position
            contents.append(world.getDoorX()).append(",").append(world.getDoorY()).append("\n");

            // Save consumables positions
            for (Point consumable : world.getConsumablePositions()) {
                contents.append(consumable.x).append(",").append(consumable.y).append("\n");
            }

            FileUtils.writeFile(fileName, contents.toString());
            PlayerStorage.savePlayer(player); // Save player data
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void loadGame(Player player) {
        String fileName = "game_data.txt";
        try {
            String contents = FileUtils.readFile(fileName);
            String[] lines = contents.split("\n");

            // Check if the saved game belongs to the current player
            if (!lines[0].equals(player.getUsername())) {
                // Clear the screen and display the translated message
                StdDraw.clear(StdDraw.BLACK);
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.text(0.5, 0.5, translationManager.getTranslation("no_saved_game")); // Use the translation
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

    @Override
    public void onEvent(Event event) {
        if (event.getType() == Event.EventType.CONSUMABLE_CONSUMED) {
            // Add notification without any pause
            notifications.add(new Notification(event.getMessage(), System.currentTimeMillis() + 2000));
            redraw = true; // Request a redraw to show the notification
        }
    }

    public void renderNotifications() {
        // Remove expired notifications first
        notifications.removeIf(Notification::isExpired);

        // Only show the most recent notification
        if (!notifications.isEmpty()) {
            Notification latestNotification = notifications.get(notifications.size() - 1);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.textLeft(0.01, 41, latestNotification.getMessage());
        }
    }

    // Add cleanup method to properly close resources
    public void cleanup() {
        AudioManager.getInstance().cleanup();
    }
}
