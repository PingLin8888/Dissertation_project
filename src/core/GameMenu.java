package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;
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
    private long CHASER_MOVE_INTERVAL = 500; // Reduced interval for faster chaser movement

    private Player player = null;
    private Language currentLanguage = Language.ENGLISH; // Default language
    private TranslationManager translationManager;
    private List<Notification> notifications = new ArrayList<>();

    // Add this class to cache HUD information
    private static class HUDInfo {
        final String tileDescription;
        final String playerInfo;
        final String pointsInfo;

        HUDInfo(String tileDescription, String playerInfo, String pointsInfo) {
            this.tileDescription = tileDescription;
            this.playerInfo = playerInfo;
            this.pointsInfo = pointsInfo;
        }
    }

    // Add these fields to GameMenu class
    private HUDInfo hudCache;
    private boolean hudNeedsUpdate = true;

    private enum GameState {
        LANGUAGE_SELECT,
        LOGIN,
        MAIN_MENU,
        IN_GAME
    }

    private GameState currentState = GameState.LANGUAGE_SELECT;

    private boolean hasSavedGame = false; // Add this field to track if saved game exists

    private int currentLevel = 1;
    private static final int MAX_LEVEL = 5;
    private static final int POINTS_PER_LEVEL = 100;

    // Add these fields to GameMenu class
    private char lastDirection = 's'; // Default facing down

    public GameMenu() {
        initializeTranslations();
    }

    private void initializeTranslations() {
        translationManager = new TranslationManager(currentLanguage);
    }

    public void createGameMenu() throws InterruptedException {
        setupCanvas();
        ter = new TERenderer();
        StdDraw.enableDoubleBuffering();

        // Set initial state
        currentState = GameState.LANGUAGE_SELECT;

        while (true) {
            // Handle input
            boolean inputHandled = handleInput();
            boolean mouseMoved = detectMouseMove();
            boolean chaserMoved = false;

            // Update game state
            if (currentState == GameState.IN_GAME) {
                chaserMoved = updateChaser();
            }

            // Render if needed
            if (redraw || inputHandled || mouseMoved || chaserMoved) {
                render();
            }

            Thread.sleep(16); // Cap at ~60 FPS
        }
    }

    private boolean updateChaser() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastChaserMoveTime >= CHASER_MOVE_INTERVAL) {
            world.moveChaser();
            checkChaserEncounter();
            lastChaserMoveTime = currentTime;
            return true;
        }
        return false;
    }

    private void render() {
        StdDraw.clear(StdDraw.BLACK);

        switch (currentState) {
            case LANGUAGE_SELECT:
                renderLanguageSelect();
                break;
            case LOGIN:
                drawLoginMenu();
                break;
            case MAIN_MENU:
                drawPostLoginMenu(player);
                break;
            case IN_GAME:
                renderGameScreen();
                break;
        }

        StdDraw.show();
        redraw = false;
    }

    private void renderLanguageSelect() {
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(0.5, 0.6, "Select Language");
        StdDraw.text(0.5, 0.5, "Press E for English");
        StdDraw.text(0.5, 0.4, "按 'C' 选择中文");
    }

    private void renderGameScreen() {
        ter.renderFrame(world.getVisibleMap());
        updateHUD();
        if (world.isShowPath() && world.getPathToAvatar() != null) {
            drawPath();
        }
        renderNotifications();
    }

    private boolean handleInput() throws InterruptedException {
        if (!StdDraw.hasNextKeyTyped()) {
            return false;
        }

        char key = Character.toLowerCase(StdDraw.nextKeyTyped());
        redraw = true;

        switch (currentState) {
            case LANGUAGE_SELECT:
                handleLanguageSelection(key);
                break;
            case LOGIN:
                handleLoginInput(key);
                break;
            case MAIN_MENU:
                handleMainMenuInput(key);
                break;
            case IN_GAME:
                handleGameInput(key);
                break;
        }
        return true;
    }

    private void setupCanvas() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        StdDraw.setCanvasSize(screenSize.width, screenSize.height);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
    }

    private void drawLoginMenu() {
        // Load a font that supports Chinese characters
        Font font = new Font("SimSun", Font.PLAIN, 24);
        StdDraw.setFont(font);

        // Draw everything to the back buffer
        StdDraw.setPenColor(Color.WHITE);
        String loginText = translationManager.getTranslation("login");
        String quitText = translationManager.getTranslation("quit");
        StdDraw.text(0.5, 0.65, loginText);
        StdDraw.text(0.5, 0.5, quitText);

        // Show the back buffer
        StdDraw.show();
    }

    private void drawPostLoginMenu(Player player) {
        StdDraw.clear(StdDraw.BLACK);

        // Load a font that supports Chinese characters
        Font font = new Font("SimSun", Font.PLAIN, 24);
        StdDraw.setFont(font);

        // Check if saved game exists for this player
        hasSavedGame = checkSavedGameExists(player.getUsername());

        // Draw menu items
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(0.5, 0.8, translationManager.getTranslation("welcome", player.getUsername()));
        StdDraw.text(0.5, 0.7, translationManager.getTranslation("points", player.getPoints()));
        StdDraw.text(0.5, 0.6, translationManager.getTranslation("new_game"));

        // Set color based on whether saved game exists
        if (hasSavedGame) {
            StdDraw.setPenColor(Color.WHITE);
        } else {
            StdDraw.setPenColor(Color.GRAY);
        }
        StdDraw.text(0.5, 0.5, translationManager.getTranslation("load_game"));

        // Reset color to white for quit option
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(0.5, 0.4, translationManager.getTranslation("quit"));
        StdDraw.show();
    }

    private boolean checkSavedGameExists(String username) {
        String fileName = "game_data.txt";
        try {
            String contents = FileUtils.readFile(fileName);
            String[] lines = contents.split("\n");
            return lines[0].equals(username);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void drawPath() {
        StdDraw.setPenColor(StdDraw.BOOK_RED);
        for (Point p : world.getPathToAvatar()) {
            StdDraw.filledSquare(p.x + 0.5, p.y + 0.5, 0.5);
        }
    }

    private boolean hasMouseMoved(double currentMouseX, double currentMouseY) {
        // Add a small threshold to prevent tiny movements from triggering updates
        double threshold = 0.001;
        return Math.abs(currentMouseX - prevMouseX) > threshold ||
                Math.abs(currentMouseY - prevMouseY) > threshold;
    }

    private void updateHUD() {
        // Get the current tile at the avatar's position from World
        TETile currentTile = world.getMap()[world.getAvatarX()][world.getAvatarY()];

        // Get the description of the current tile
        String tileDescription = getTileDescription(currentTile);

        // Update the HUD information
        hudCache = new HUDInfo(tileDescription,
                "Player: " + player.getUsername(),
                "Points: " + player.getPoints());

        // Render the HUD
        renderHUD();
    }

    private String getTileDescription(TETile tile) {
        // Get the tile in front of the avatar based on last movement direction
        Point facingTile = getFacingTilePosition();
        if (facingTile != null) {
            tile = world.getMap()[facingTile.x][facingTile.y];
        }

        // Check for obstacles first
        for (ObstacleType obstacle : ObstacleType.values()) {
            if (tile == obstacle.getTile()) {
                switch (obstacle) {
                    case SPIKES -> {
                        return "Danger ahead! Spikes will hurt you and reduce points!";
                    }
                    case TELEPORTER -> {
                        return "A mysterious portal ahead. Where will it take you?";
                    }
                    case ICE -> {
                        return "Careful! Slippery ice ahead!";
                    }
                }
            }
        }

        // Check for consumables
        for (Consumable consumable : world.getConsumables()) {
            if (tile == consumable.getTile()) {
                return "Ahead: " + consumable.getName() + " worth " + consumable.getPointValue() + " points!";
            }
        }

        // Check basic world tiles
        if (tile == world.getFloorTile()) {
            return "Clear path ahead.";
        } else if (tile == world.getWallTile()) {
            return "A wall blocks your path.";
        } else if (tile == Tileset.CHASER) {
            return "DANGER! The chaser is right in front of you!";
        } else if (tile == Tileset.LOCKED_DOOR || tile == Tileset.UNLOCKED_DOOR) {
            return "The exit door is right ahead!";
        }

        return "Can't see what's ahead.";
    }

    private void renderHUD() {
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.textLeft(0.01, 42, hudCache.tileDescription);
        StdDraw.textLeft(0.01, 44, hudCache.playerInfo);
        StdDraw.textLeft(0.01, 43, hudCache.pointsInfo);
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
                } else if (key == '\b' || key == 127) { // Backspace and Delete keys
                    // Remove the last character if there is one
                    if (usernameBuilder.length() > 0) {
                        usernameBuilder.setLength(usernameBuilder.length() - 1);
                    }
                } else {
                    // Add the character if it's not a control character
                    if (!Character.isISOControl(key)) {
                        usernameBuilder.append(key);
                    }
                }
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
        lastDirection = key; // Update last direction before moving
        if (world.moveAvatar(key)) {
            AudioManager.getInstance().playSound("walk");
            hudNeedsUpdate = true;
        }
        checkObjectiveCompletion();
    }

    private void checkObjectiveCompletion() {
        if (world.getAvatarX() == world.getDoorX() && world.getAvatarY() == world.getDoorY()) {
            AudioManager.getInstance().playSound("gamePass");

            // Award points based on current level
            int levelPoints = POINTS_PER_LEVEL * currentLevel;
            player.addPoints(levelPoints);
            hudNeedsUpdate = true;

            if (currentLevel < MAX_LEVEL) {
                // Show level completion message
                showLevelCompleteMessage(levelPoints);

                // Advance to next level
                currentLevel++;
                createNextLevel();
            } else {
                // Show game completion message
                showGameCompleteMessage();

                // Return to main menu
                gameStarted = false;
                currentState = GameState.MAIN_MENU;
            }
        }
    }

    private void createNextLevel() {
        // Increase difficulty with each level
        long newSeed = world.getSeed() + currentLevel; // Generate new seed for variety

        // Adjust game parameters based on level
        CHASER_MOVE_INTERVAL = Math.max(1000, CHASER_MOVE_INTERVAL - (500 * currentLevel));
        int numConsumables = 3 + currentLevel; // More consumables in higher levels
        int numObstacles = 10 + (currentLevel * 9); // More obstacles in higher levels

        // Create new world with increased difficulty
        world = new World(player, newSeed, numConsumables, numObstacles);
        world.getEventDispatcher().addListener(this);

        // Show new level message
        showNewLevelMessage();

        drawWorld();
    }

    private void showLevelCompleteMessage(int pointsEarned) {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Level " + currentLevel + " Complete!");
        StdDraw.text(40, 23, "Points earned: " + pointsEarned);
        StdDraw.text(40, 26, "Total points: " + player.getPoints());
        StdDraw.show();
        StdDraw.pause(3000);
    }

    private void showNewLevelMessage() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Level " + currentLevel);
        StdDraw.text(40, 23, "Get ready!");
        StdDraw.text(40, 26, "Chaser is faster now!");
        StdDraw.show();
        StdDraw.pause(3000);
    }

    private void showGameCompleteMessage() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Congratulations!");
        StdDraw.text(40, 23, "You've completed all " + MAX_LEVEL + " levels!");
        StdDraw.text(40, 26, "Final Score: " + player.getPoints());
        StdDraw.show();
        StdDraw.pause(3000);
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
            hudNeedsUpdate = true; // Update HUD when points change
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

    private void handleRestart() throws InterruptedException {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setXscale(0, 1);
        StdDraw.setYscale(0, 1);
        StdDraw.text(0.5, 0.6, translationManager.getTranslation("restart_confirm"));
        StdDraw.text(0.5, 0.5, translationManager.getTranslation("restart_options"));
        StdDraw.show();

        // Wait for user confirmation
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char response = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (response == 'y') {
                    AudioManager.getInstance().playSound("menu");
                    // Restart the game
                    // gameStarted = false;
                    // createGameMenu();
                    currentState = GameState.MAIN_MENU;
                    break;
                } else if (response == 'n') {
                    AudioManager.getInstance().playSound("menu");
                    // Return to the current game
                    redraw = true;
                    drawWorld(); // Redraw the current world
                    break;
                }
            }
            StdDraw.pause(10);
        }
    }

    private void handleLanguageSelection(char key) {
        if (key == 'e') {
            AudioManager.getInstance().playSound("menu");
            currentLanguage = Language.ENGLISH;
            currentState = GameState.LOGIN;
            initializeTranslations();
        } else if (key == 'c') {
            AudioManager.getInstance().playSound("menu");
            currentLanguage = Language.CHINESE;
            currentState = GameState.LOGIN;
            initializeTranslations();
        }
    }

    private void handleLoginInput(char key) {
        switch (key) {
            case 'p':
                AudioManager.getInstance().playSound("menu");
                player = loginOrCreateProfile();
                currentState = GameState.MAIN_MENU;
                break;
            case 'q':
                AudioManager.getInstance().playSound("menu");
                System.exit(0);
                break;
        }
    }

    private void handleMainMenuInput(char key) {
        switch (key) {
            case 'n':
                AudioManager.getInstance().playSound("menu");
                createNewGame();
                AudioManager.getInstance().playSound("gamestart");
                currentState = GameState.IN_GAME;
                break;
            case 'l':
                if (hasSavedGame) {
                    AudioManager.getInstance().playSound("menu");
                    loadGame(player);
                    AudioManager.getInstance().playSound("gamestart");
                    currentState = GameState.IN_GAME;
                } else {
                    // Show no saved game message
                    StdDraw.clear(StdDraw.BLACK);
                    StdDraw.setPenColor(StdDraw.WHITE);
                    StdDraw.text(0.5, 0.5, translationManager.getTranslation("no_saved_game"));
                    StdDraw.show();
                    StdDraw.pause(2000);

                    // Return to main menu
                    redraw = true;
                    currentState = GameState.MAIN_MENU;
                }
                break;
            case 'q':
                AudioManager.getInstance().playSound("menu");
                saveGame(player);
                System.exit(0);
                break;
        }
    }

    private void handleGameInput(char key) throws InterruptedException {
        if (key == ':') {
            quitSignBuilder.setLength(0);
            quitSignBuilder.append(key);
        } else if (key == 'q' && quitSignBuilder.toString().equals(":")) {
            AudioManager.getInstance().playSound("menu");
            saveGame(player);
            System.exit(0);
        } else if (key == 'p') {
            AudioManager.getInstance().playSound("menu");
            world.togglePathDisplay();
        } else if (key == 'n') {
            handleRestart();
        } else if (key == 'w' || key == 'a' || key == 's' || key == 'd') {
            handleMovement(key);
        }
    }

    private boolean detectMouseMove() {
        double currentMouseX = StdDraw.mouseX();
        double currentMouseY = StdDraw.mouseY();

        if (hasMouseMoved(currentMouseX, currentMouseY)) {
            prevMouseX = currentMouseX;
            prevMouseY = currentMouseY;
            return true;
        }
        return false;
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
            currentState = GameState.MAIN_MENU; // Update the game state
            redraw = true;
            System.out.println("Game state reset to post-login menu.");
        }
    }

    // Add this method to get the position of the tile the avatar is facing
    private Point getFacingTilePosition() {
        int x = world.getAvatarX();
        int y = world.getAvatarY();

        switch (lastDirection) {
            case 'w' -> y += 1;
            case 's' -> y -= 1;
            case 'a' -> x -= 1;
            case 'd' -> x += 1;
            default -> {
                return null;
            }
        }

        // Check if the position is within bounds
        if (x >= 0 && x < world.getMap().length && y >= 0 && y < world.getMap()[0].length) {
            return new Point(x, y);
        }
        return null;
    }
}
