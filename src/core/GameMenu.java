package core;

import edu.princeton.cs.algs4.StdDraw;
import tileengine.TERenderer;
import tileengine.TETile;
import tileengine.Tileset;
import utils.FileUtils;
import tileengine.AvatarTileset;
import tileengine.AvatarOption;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

/**
 * Inspired by GPT.
 */

// Enum to represent supported languages
enum Language {
    ENGLISH, CHINESE // Add more languages as needed
}

public class GameMenu implements EventListener {
    World world;
    private TERenderer ter;
    StringBuilder quitSignBuilder = new StringBuilder();
    private boolean gameStarted = false;
    boolean redraw = true;
    private double prevMouseX = 0;
    private double prevMouseY = 0;
    private long lastChaserMoveTime = 0; // Variable to track the last time the chaser moved
    private long CHASER_MOVE_INTERVAL = 500; // Reduced interval for faster chaser movement

    Player player = null;
    public Language currentLanguage = Language.ENGLISH; // Default language
    public TranslationManager translationManager;
    List<Notification> notifications = new ArrayList<>();

    // Add this class to cache HUD information
    private static class HUDInfo {
        final String tileDescription;
        final String playerInfo;
        final String pointsInfo;
        final String instructions;

        HUDInfo(String tileDescription, String playerInfo, String pointsInfo, String instructions) {
            this.tileDescription = tileDescription;
            this.playerInfo = playerInfo;
            this.pointsInfo = pointsInfo;
            this.instructions = instructions;
        }
    }

    // Add these fields to GameMenu class
    private HUDInfo hudCache;
    private boolean hudNeedsUpdate = true;

    enum GameState {
        LANGUAGE_SELECT,
        LOGIN,
        MAIN_MENU,
        IN_GAME
    }

    GameState currentState = GameState.LANGUAGE_SELECT;

    public boolean hasSavedGame = false; // Add this field to track if saved game exists

    private int currentLevel = 1;
    private static final int MAX_LEVEL = 5;
    private static final int POINTS_PER_LEVEL = 100;

    // Add these fields to GameMenu class
    private char lastDirection = 's'; // Default facing down

    // Add new field
    boolean isPaused = false;

    // Add these fields at the top of GameMenu class
    List<AnimatedMenuItem> menuItems = new ArrayList<>();
    private boolean animationInProgress = false;

    // Replace both AnimatedText and MenuText with this unified class
    private class AnimatedMenuItem {
        String text;
        double y;
        double targetY;
        double alpha = 0;
        boolean isLoadGame;
        private long startTime;

        AnimatedMenuItem(String text, double targetY, boolean isLoadGame, long delayMs) {
            this.text = text;
            this.targetY = targetY;
            this.y = targetY - 20; // Start below target
            this.isLoadGame = isLoadGame;
            this.startTime = System.currentTimeMillis() + delayMs;
        }

        void update() {
            if (System.currentTimeMillis() < startTime) {
                return;
            }

            double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
            double progress = Math.min(1.0, elapsed / 0.5);
            double ease = 1 - Math.pow(1 - progress, 3);

            y = targetY - (20 * (1 - ease));
            alpha = ease;
        }

        void draw() {
            // Use consistent font across all menus
            // Font menuFont = new Font("SimSun", Font.PLAIN, 24);
            // StdDraw.setFont(menuFont);

            // Draw shadow with current transparency
            Color shadowColor = new Color(0, 0, 0, (float) alpha);
            StdDraw.setPenColor(shadowColor);
            StdDraw.text(40.2, y - 0.2, text);

            // Draw main text with current transparency
            Color textColor;
            if (isLoadGame && !hasSavedGame) {
                textColor = new Color(0.5f, 0.5f, 0.5f, (float) alpha);
            } else {
                textColor = new Color(1f, 1f, 1f, (float) alpha);
            }
            StdDraw.setPenColor(textColor);
            StdDraw.text(40, y, text);
        }
    }

    // Update the menu item lists to use the new class
    public List<AnimatedMenuItem> languageMenuItems = new ArrayList<>();
    private List<AnimatedMenuItem> loginMenuItems = new ArrayList<>();

    // Add these constants at the top of GameMenu class
    private static final String SAVES_DIR = "saves";
    private static final String AUTO_SAVE_PREFIX = "auto_";
    private static final String SAVE_FILE_SUFFIX = "_save.txt";
    private static final int MAX_AUTO_SAVES = 3;
    private static final long AUTO_SAVE_INTERVAL = 300000; // 5 minutes in milliseconds
    private long lastAutoSaveTime = 0;

    public SettingsMenu settingsMenu;
    private InGameInputHandler inGameInputHandler;
    private LanguageSelectionInputHandler languageSelectionInputHandler;
    private LoginInputHandler loginInputHandler;
    private MainMenuInputHandler mainMenuInputHandler;

    public GameMenu() {
        initializeTranslations();
        settingsMenu = new SettingsMenu(translationManager);
        Settings.getInstance().loadSettings(); // Load saved settings
        inGameInputHandler = new InGameInputHandler(this);
        languageSelectionInputHandler = new LanguageSelectionInputHandler(this);
        loginInputHandler = new LoginInputHandler(this);
        mainMenuInputHandler = new MainMenuInputHandler(this);
    }

    public void initializeTranslations() {
        translationManager = new TranslationManager(currentLanguage);
        if (settingsMenu != null) {
            settingsMenu.updateTranslationManager(translationManager);
        }
    }

    public void createGameMenu() throws InterruptedException {
        setupCanvas();
        ter = new TERenderer();

        currentState = GameState.LANGUAGE_SELECT;
        long lastUpdateTime = System.currentTimeMillis();
        final long FRAME_TIME = 16; // Target ~60 FPS

        while (true) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastUpdateTime;

            // Add auto-save check
            if (currentState == GameState.IN_GAME && !isPaused) { // Don't auto-save when paused
                checkAndAutoSave();
            }

            // Always update and render when in menu states
            boolean needsRender = currentState == GameState.MAIN_MENU ||
                    currentState == GameState.LANGUAGE_SELECT ||
                    currentState == GameState.LOGIN;

            // Handle input
            boolean inputHandled = handleInput();
            boolean chaserMoved = false;

            // Update game state using deltaTime
            if (currentState == GameState.IN_GAME && !isPaused) { // Don't update chaser when paused
                chaserMoved = updateChaser();
                // needsRender = inputHandled || chaserMoved || detectMouseMove();
                needsRender = inputHandled || chaserMoved;
            }
            if (currentState == GameState.IN_GAME && world.handleChaserCollision()) {
                failGame();
            }

            // Render if needed
            if (needsRender || redraw) {
                render();
            }

            // Calculate sleep time to maintain consistent frame rate
            long sleepTime = Math.max(0, FRAME_TIME - (System.currentTimeMillis() - currentTime));
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }

            lastUpdateTime = currentTime;
        }
    }

    private boolean updateChaser() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastChaserMoveTime >= CHASER_MOVE_INTERVAL) {
            world.moveChaser();
            lastChaserMoveTime = currentTime;
            return true;
        }
        return false;
    }

    private void render() {
        StdDraw.clear(StdDraw.BLACK);
        setDrawColor(Color.WHITE);

        switch (currentState) {
            case LANGUAGE_SELECT:
                renderLanguageSelect();
                break;
            case LOGIN:
                drawLoginMenu();
                break;
            case MAIN_MENU:
                if (settingsMenu.isVisible()) {
                    settingsMenu.render();
                } else {
                    drawMainMenu(player);
                }
                break;
            case IN_GAME:
                renderInGameScreen();
                if (isPaused) {
                    drawPauseOverlay();
                }
                break;
        }

        StdDraw.show();
        redraw = false;
    }

    private void renderLanguageSelect() {
        if (languageMenuItems.isEmpty()) {
            double startY = 35;
            double spacing = 5;
            long baseDelay = 0;
            long delayIncrement = 200;

            languageMenuItems.add(new AnimatedMenuItem("Select Language", startY, false, baseDelay));
            languageMenuItems.add(new AnimatedMenuItem("Press E for English",
                    startY - spacing, false, baseDelay + delayIncrement));
            languageMenuItems.add(new AnimatedMenuItem("按 'C' 选择中文",
                    startY - spacing * 2, false, baseDelay + delayIncrement * 2));
        }

        StdDraw.clear(new Color(0.1f, 0.1f, 0.1f));

        for (AnimatedMenuItem item : languageMenuItems) {
            item.update();
            item.draw();
        }

        StdDraw.show();
    }

    private void renderInGameScreen() {
        ter.renderFrame(world.getVisibleMap());
        updateHUD();
        renderNotifications();
    }

    private boolean handleInput() throws InterruptedException {
        if (!StdDraw.hasNextKeyTyped()) {
            return false;
        }

        char key = Character.toLowerCase(StdDraw.nextKeyTyped());
        redraw = true;

        // Delegate input handling based on current state
        switch (currentState) {
            case IN_GAME -> {
                return inGameInputHandler.handleInput(key);
            }
            case LANGUAGE_SELECT -> {
                return languageSelectionInputHandler.handleInput(key);
            }
            case LOGIN -> {
                return loginInputHandler.handleInput(key);
            }
            case MAIN_MENU -> {
                return mainMenuInputHandler.handleInput(key);
            }
        }
        return true;
    }

    private void setupCanvas() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        StdDraw.setCanvasSize(screenSize.width, screenSize.height);
        StdDraw.setPenColor(Color.white);
        // Call this once during game initialization
        StdDraw.setXscale(0, world.getWIDTH());
        StdDraw.setYscale(0, world.getHEIGHT());
        StdDraw.enableDoubleBuffering();
    }

    private void setDrawColor(Color color) {
        StdDraw.setPenColor(color);
    }

    private void drawLoginMenu() {
        if (loginMenuItems.isEmpty()) {
            double startY = 35;
            double spacing = 5;
            long baseDelay = 0;
            long delayIncrement = 200;

            loginMenuItems.add(new AnimatedMenuItem(translationManager.getTranslation("login"),
                    startY, false, baseDelay));
            loginMenuItems.add(new AnimatedMenuItem(translationManager.getTranslation("quit"),
                    startY - spacing, false, baseDelay + delayIncrement));
        }

        StdDraw.clear(new Color(0.1f, 0.1f, 0.1f));

        for (AnimatedMenuItem item : loginMenuItems) {
            item.update();
            item.draw();
        }

        StdDraw.show();
    }

    private void drawMainMenu(Player player) {
        if (menuItems.isEmpty()) {
            hasSavedGame = checkSavedGameExists(player.getUsername());

            double startY = 42;
            double spacing = 3; // Reduced from 4 to make spacing narrower
            long baseDelay = 0;
            long delayIncrement = 100;

            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("main_menu"),
                    startY, false, baseDelay));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("welcome", player.getUsername()),
                    startY - spacing, false, baseDelay + delayIncrement));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("points", player.getPoints()),
                    startY - spacing * 2, false, baseDelay + delayIncrement * 2));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("current_level", currentLevel),
                    startY - spacing * 3, false, baseDelay + delayIncrement * 3));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("continue_game"),
                    startY - spacing * 4, true, baseDelay + delayIncrement * 4));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("new_game_scratch"),
                    startY - spacing * 5, false, baseDelay + delayIncrement * 5));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("change_avatar"),
                    startY - spacing * 6, false, baseDelay + delayIncrement * 6));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("settings"),
                    startY - spacing * 7, false, baseDelay + delayIncrement * 7));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("tutorial"),
                    startY - spacing * 8, false, baseDelay + delayIncrement * 8));
            menuItems.add(new AnimatedMenuItem(translationManager.getTranslation("quit_game"),
                    startY - spacing * 9, false, baseDelay + delayIncrement * 9));
        }

        StdDraw.clear(new Color(0.1f, 0.1f, 0.1f));

        // Update and draw all items
        for (AnimatedMenuItem item : menuItems) {
            item.update();
            item.draw();
        }

        StdDraw.show();
    }

    private boolean checkSavedGameExists(String username) {
        String saveFile = SAVES_DIR + "/" + username + SAVE_FILE_SUFFIX;
        return new File(saveFile).exists();
    }

    private boolean hasMouseMoved(double currentMouseX, double currentMouseY) {
        // Add a small threshold to prevent tiny movements from triggering updates
        double threshold = 0.001;
        return Math.abs(currentMouseX - prevMouseX) > threshold ||
                Math.abs(currentMouseY - prevMouseY) > threshold;
    }

    private void updateHUD() {
        // Update player invisibility status in case the duration has expired.
        if (player.updateInvisibility()) {
            // Add notification when invisibility wears off
            AudioManager.getInstance().playSound("menu");
            notifications.add(new Notification("Invisibility has worn off!", System.currentTimeMillis() + 2000));
        }
        // Update the avatar tile based on current invisibility state.
        world.updateAvatarTile();

        // Get the current tile at the avatar's position from World
        TETile currentTile = world.getMap()[world.getAvatarX()][world.getAvatarY()];

        // Get the description of the current tile.
        String tileDescription = getTileDescription(currentTile);

        String instructions = "Press N to restart; Press V for invisibility cure";
        // Update the HUD information with current tile description, player info and
        // instructions.
        hudCache = new HUDInfo(tileDescription,
                "Player: " + player.getUsername(),
                "Points: " + player.getPoints(),
                instructions);

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
        // Spread out HUD elements with better spacing
        StdDraw.setPenColor(Color.white);
        StdDraw.textLeft(0.1, 44, hudCache.playerInfo);
        StdDraw.textLeft(0.1, 43, hudCache.pointsInfo);
        StdDraw.textLeft(0.1, 42, "Level: " + currentLevel);
        StdDraw.textLeft(0.1, 41, hudCache.tileDescription);
        StdDraw.text(40, 44, hudCache.instructions);
    }

    public Player loginOrCreateProfile() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        // Get username first
        String username = getUsernameInput();

        // Check if save file exists
        String saveFile = SAVES_DIR + "/" + username + SAVE_FILE_SUFFIX;
        if (new File(saveFile).exists()) {
            try {
                // Load basic player info from save file
                String contents = FileUtils.readFile(saveFile);
                String[] lines = contents.split("\n");

                // Verify username
                if (!lines[0].equals(username)) {
                    return createNewPlayer(username);
                }

                // Load player data from first few lines of save file
                int points = Integer.parseInt(lines[1]);
                int avatarChoice = Integer.parseInt(lines[2]);

                int currentLevelFromFile = Integer.parseInt(lines[8]);
                currentLevel = currentLevelFromFile;

                Player player = new Player(username, points);
                player.setAvatarChoice(avatarChoice);
                hasSavedGame = true;
                System.out.println("Input Username: " + username);
                System.out.println("Saved Username: " + lines[0]);
                return player;

            } catch (Exception e) {
                System.err.println("Error loading player data: " + e.getMessage());
                return createNewPlayer(username);
            }
        }

        return createNewPlayer(username);
    }

    private Player createNewPlayer(String username) {
        // If new player, show avatar selection
        int avatarChoice = showAvatarSelection();
        Player newPlayer = new Player(username);
        newPlayer.setAvatarChoice(avatarChoice);
        hasSavedGame = false;
        return newPlayer;
    }

    private String getUsernameInput() {
        StdDraw.setPenColor(Color.white);
        StringBuilder usernameBuilder = new StringBuilder();
        StdDraw.text(40, 24, translationManager.getTranslation("enter_username"));
        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                AudioManager.getInstance().playSound("menu");

                if (key == '\n' || key == '\r') {
                    if (usernameBuilder.length() > 0) {
                        break;
                    }
                } else if (key == '\b' || key == 127) {
                    if (usernameBuilder.length() > 0) {
                        usernameBuilder.setLength(usernameBuilder.length() - 1);
                    }
                } else if (!Character.isISOControl(key)) {
                    usernameBuilder.append(key);
                }

                // Redraw
                StdDraw.clear(StdDraw.BLACK);
                StdDraw.text(40, 24, translationManager.getTranslation("enter_username") + " " + usernameBuilder);
                StdDraw.show();
            }
            StdDraw.pause(10);
        }
        return usernameBuilder.toString().trim();
    }

    int showAvatarSelection() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(Color.white);
        StdDraw.text(40, 30, translationManager.getTranslation("choose_avatar"));
        StdDraw.text(40, 25, translationManager.getTranslation("skip_selection"));

        // Calculate positions for avatar display
        // Adjust coordinates to match the 80x45 scale
        double startX = 24; // 30% of 80 ≈ 24
        double spacing = 32; // 40% of 80 = 32
        double previewY = 18; // 40% of 45 = 18
        double textY = 9; // 20% of 45 = 9

        // Display avatar options
        AvatarOption[] options = AvatarTileset.AVATAR_OPTIONS;
        for (int i = 0; i < options.length; i++) {
            AvatarOption avatar = options[i];
            double x = startX + (i * spacing);

            // Draw preview image
            StdDraw.picture(x, previewY, avatar.getPreviewPath());

            // Draw name and highlight current selection if changing avatar
            StdDraw.setPenColor(StdDraw.WHITE);
            String label = translationManager.getTranslation("avatar_label", i + 1, avatar.getName());
            if (player != null && player.getAvatarChoice() == avatar.getIndex()) {
                label += " " + translationManager.getTranslation("avatar_current");
            }
            StdDraw.text(x, textY, label);
        }

        StdDraw.show();

        // Wait for selection
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == '\n' || key == '\r') {
                    return 0; // Default avatar
                }
                int choice = Character.getNumericValue(key);
                AvatarOption[] avatarOptions = AvatarTileset.AVATAR_OPTIONS;
                if (choice >= 1 && choice <= avatarOptions.length) {
                    AudioManager.getInstance().playSound("menu");
                    return avatarOptions[choice - 1].getIndex();
                }
            }
            StdDraw.pause(10);
        }
    }

    public void createNewGame() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(Color.white);

        StdDraw.text(40, 24, translationManager.getTranslation("enter_seed"));
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
                    StdDraw.text(40, 24, translationManager.getTranslation("enter_seed") + " " + seedInput);
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

    public void drawWorld() {
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

    public void handleMovement(char key) {
        lastDirection = key; // Update last direction before moving
        if (world.moveAvatar(key)) {
            AudioManager.getInstance().playSound("walk");
            hudNeedsUpdate = true;
            if (world.getAvatarX() == world.getDoorX() && world.getAvatarY() == world.getDoorY()) {
                exitDoor();
            }
        }
    }

    private void failGame() {
        // Stop all sounds except gameover
        AudioManager.getInstance().stopAllSoundsExcept("gameover");

        // Play game over sound
        AudioManager.getInstance().playSound("gameover");
        StdDraw.clear(StdDraw.BLACK);

        // Clear the screen and display the message
        StdDraw.setPenColor(Color.white);
        StdDraw.text(40, 28, translationManager.getTranslation("game_over"));
        StdDraw.text(40, 24, "1 - " + translationManager.getTranslation("retry_level"));
        StdDraw.text(40, 22, "2 - " + translationManager.getTranslation("return_to_menu"));
        StdDraw.show();

        // Wait for user input
        boolean validInput = false;
        while (!validInput) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == '1') {
                    // Retry current level
                    AudioManager.getInstance().playSound("menu");
                    retryCurrentLevel();
                    validInput = true;
                } else if (key == '2') {
                    // Return to main menu
                    AudioManager.getInstance().playSound("menu");
                    gameStarted = false;
                    currentState = GameState.MAIN_MENU;
                    redraw = true;
                    validInput = true;
                }
            }
            StdDraw.pause(10);
        }
    }

    // Add a new method to retry the current level
    private void retryCurrentLevel() {
        // Create a new world with the same level
        long seed = System.currentTimeMillis();
        int numConsumables = 10 + (currentLevel - 1) * 2; // Scale consumables with level
        int numObstacles = 5 + (currentLevel - 1); // Scale obstacles with level

        // Create a new world with the current level settings
        world = new World(player, seed, numConsumables, numObstacles);
        world.getEventDispatcher().addListener(this);

        // Reset game state
        gameStarted = true;
        currentState = GameState.IN_GAME;
        redraw = true;

        // Play game start sound
        AudioManager.getInstance().playSound("gamestart");

        // Draw the new world
        drawWorld();
    }

    private void exitDoor() {
        // Stop all ongoing sound effects
        AudioManager.getInstance().stopAllSoundsExcept("gamePass");
        AudioManager.getInstance().playSound("gamePass");

        // Award points based on current level
        int levelPoints = POINTS_PER_LEVEL * currentLevel;
        player.addPoints(levelPoints);
        hudNeedsUpdate = true;

        // Show level completion message
        showLevelCompleteMessage(levelPoints);

        // Advance to next level
        currentLevel++;
        createNextLevel();
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

        // Ensure door is locked in new level
        world.resetDoorState();

        drawWorld();
    }

    private void showLevelCompleteMessage(int pointsEarned) {
        AudioManager.getInstance().stopAllSoundsExcept("gamePass");

        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Level " + currentLevel + " Complete!");
        StdDraw.text(40, 23, "Points earned: " + pointsEarned);
        StdDraw.text(40, 26, "Total points: " + player.getPoints());
        StdDraw.text(40, 29, "Press any key to continue...");
        StdDraw.show();

        // Wait for any key press
        while (!StdDraw.hasNextKeyTyped()) {
            StdDraw.pause(10);
        }
        StdDraw.nextKeyTyped(); // Clear the key press
    }

    private void showNewLevelMessage() {
        AudioManager.getInstance().stopAllSoundsExcept("gamestart");

        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Level " + currentLevel);
        StdDraw.text(40, 23, "Get ready!");
        StdDraw.text(40, 26, "Chaser is faster now!");
        StdDraw.show();
        StdDraw.pause(3000);
    }

    public void saveGame(Player player) {
        if (player == null || world == null) {
            return;
        }

        // Create saves directory if it doesn't exist
        new File(SAVES_DIR).mkdirs();

        // Single save file per user
        String fileName = String.format("%s/%s%s", SAVES_DIR, player.getUsername(), SAVE_FILE_SUFFIX);

        try {
            saveGameToFile(fileName);
            PlayerStorage.savePlayer(player);
        } catch (Exception e) {
            System.err.println("Error saving game: " + e.getMessage());
        }
    }

    public void loadGame(Player player) {
        String saveFile = SAVES_DIR + "/" + player.getUsername() + SAVE_FILE_SUFFIX;
        File file = new File(saveFile);

        if (!file.exists()) {
            System.err.println("No save file found for player: " + player.getUsername());
            return;
        }

        try {
            String contents = FileUtils.readFile(saveFile);
            String[] lines = contents.split("\n");
            int currentLine = 0;

            // Verify username
            String savedUsername = lines[currentLine++];
            if (!savedUsername.equals(player.getUsername())) {
                System.err.println("Save file username mismatch");
                return;
            }

            // Load basic game state
            int points = Integer.parseInt(lines[currentLine++]);
            int avatarChoice = Integer.parseInt(lines[currentLine++]);
            long seed = Long.parseLong(lines[currentLine++]);
            int avatarX = Integer.parseInt(lines[currentLine++]);
            int avatarY = Integer.parseInt(lines[currentLine++]);
            int chaserX = Integer.parseInt(lines[currentLine++]);
            int chaserY = Integer.parseInt(lines[currentLine++]);
            currentLevel = Integer.parseInt(lines[currentLine++]); // Load current level

            // Load door position
            String[] doorPos = lines[currentLine++].split(",");
            int doorX = Integer.parseInt(doorPos[0]);
            int doorY = Integer.parseInt(doorPos[1]);

            // Load dark mode state
            String[] darkModeData = lines[currentLine++].split(",");
            boolean isDarkMode = Boolean.parseBoolean(darkModeData[0]);
            int visionRadius = Integer.parseInt(darkModeData[1]);

            // Load invisibility state
            String[] invisibilityData = lines[currentLine++].split(",");
            boolean isInvisible = Boolean.parseBoolean(invisibilityData[0]);
            long remainingDuration = Long.parseLong(invisibilityData[1]);
            if (isInvisible) {
                player.setInvisibilityState(true, remainingDuration);
                player.resumeInvisibility();
                AudioManager.getInstance().playLoopingSound("invisibility");
            }

            // Update player state first (before creating world)
            player.setPoints(points);
            player.setAvatarChoice(avatarChoice);

            // Create new world with seed but don't populate items yet
            world = new World(player, seed, 0, 0);

            // Set positions
            world.setAvatarToNewPosition(avatarX, avatarY);
            world.setChaserToNewPosition(chaserX, chaserY);
            world.setDoorPosition(doorX, doorY);

            // Set dark mode state
            world.setDarkMode(isDarkMode, visionRadius);

            // Load torch positions
            int numTorches = Integer.parseInt(lines[currentLine++]);
            for (int i = 0; i < numTorches; i++) {
                String[] torchPos = lines[currentLine++].split(",");
                int x = Integer.parseInt(torchPos[0]);
                int y = Integer.parseInt(torchPos[1]);
                world.addTorch(x, y);
            }

            // Load consumables
            int numConsumables = Integer.parseInt(lines[currentLine++]);
            for (int i = 0; i < numConsumables; i++) {
                String[] consumableData = lines[currentLine++].split(",");
                int x = Integer.parseInt(consumableData[0]);
                int y = Integer.parseInt(consumableData[1]);
                String type = consumableData[2];
                world.addConsumable(x, y, type);
            }

            // Load obstacles
            int numObstacles = Integer.parseInt(lines[currentLine++]);
            for (int i = 0; i < numObstacles; i++) {
                String[] obstacleData = lines[currentLine++].split(",");
                int x = Integer.parseInt(obstacleData[0]);
                int y = Integer.parseInt(obstacleData[1]);
                ObstacleType type = ObstacleType.valueOf(obstacleData[2]);
                world.addObstacle(x, y, type);
            }

            // Set game state
            gameStarted = true;

            // Add notification about load
            notifications.add(new Notification(
                    "Game loaded successfully",
                    System.currentTimeMillis() + 3000));

            // Check and play chaser sound if nearby
            world.checkChaserProximity();

            // Check and play eerie sound if near dark mode obstacle
            world.checkDarkModeProximity();

        } catch (Exception e) {
            System.err.println("Error loading game: " + e.getMessage());
            e.printStackTrace();
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
            StdDraw.textLeft(0.01, 40, latestNotification.getMessage());
        }
    }

    public void handleRestart() throws InterruptedException {
        // Pause the game if it wasn't already paused
        boolean wasPaused = isPaused;
        if (!wasPaused) {
            handlePause();
        }

        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 24, translationManager.getTranslation("restart_confirm"));
        StdDraw.text(40, 22, translationManager.getTranslation("restart_options"));
        StdDraw.show();

        // Wait for user confirmation
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char response = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (response == 'y') {
                    AudioManager.getInstance().playSound("menu");
                    gameStarted = false;
                    currentState = GameState.MAIN_MENU;
                    break;
                } else if (response == 'n') {
                    AudioManager.getInstance().playSound("menu");
                    // Only resume if game wasn't paused before
                    if (!wasPaused) {
                        handlePause();
                    }
                    redraw = true;
                    drawWorld();
                    break;
                }
            }
            StdDraw.pause(10);
        }
    }

    public void handlePause() {
        // Toggle pause state
        isPaused = !isPaused;

        AudioManager.getInstance().playSound("menu");
        if (isPaused) {
            AudioManager.getInstance().stopAllSounds();
            player.pauseInvisibility(); // Pause invisibility duration
            drawPauseMenu();
        } else {
            // Reset sound flags on unpause
            world.resetSoundFlags();

            // Start relevant sounds based on game state
            if (player.isInvisible()) {
                AudioManager.getInstance().playLoopingSound("invisibility");
            }
            player.resumeInvisibility(); // Resume invisibility duration

            // Check and play chaser sound if nearby
            world.checkChaserProximity();

            // Check and play eerie sound if near dark mode obstacle
            world.checkDarkModeProximity();

            redraw = true;
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

    // Add new method to draw pause menu
    private void drawPauseMenu() {
        StdDraw.setPenColor(Color.white);
        StdDraw.text(40, 35, translationManager.getTranslation("game_paused"));
        StdDraw.text(40, 28, translationManager.getTranslation("press_p_resume"));
        StdDraw.text(40, 21, translationManager.getTranslation("press_n_restart"));
        StdDraw.text(40, 14, ":Q - " + translationManager.getTranslation("save_and_quit"));
        StdDraw.show();
    }

    // Add a new method to draw semi-transparent pause overlay
    private void drawPauseOverlay() {
        // Draw semi-transparent dark overlay
        StdDraw.setPenColor(new Color(0, 0, 0, 0.5f));
        StdDraw.filledRectangle(World.getWIDTH() / 2.0, World.getHEIGHT() / 2.0,
                World.getWIDTH() / 2.0, World.getHEIGHT() / 2.0);

        // Draw pause menu
        StdDraw.setPenColor(Color.WHITE);
        double centerY = World.getHEIGHT() / 2.0;

        StdDraw.text(40, centerY + 5, translationManager.getTranslation("game_paused"));
        StdDraw.text(40, centerY, translationManager.getTranslation("press_p_resume"));
        StdDraw.text(40, centerY - 5, translationManager.getTranslation("press_n_restart"));
        StdDraw.text(40, centerY - 10, ":Q - " + translationManager.getTranslation("save_and_quit"));
    }

    // Add confirmation prompt for new game
    public boolean confirmNewGame() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(40, 24, "Starting a new game will reset your progress.");
        StdDraw.text(40, 22, "Are you sure you want to continue? (Y/N)");
        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = Character.toLowerCase(StdDraw.nextKeyTyped());
                if (key == 'y') {
                    return true; // Confirm new game
                } else if (key == 'n') {
                    return false; // Cancel new game
                }
            }
        }
    }

    // Add this method to handle auto-saves
    private void checkAndAutoSave() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAutoSaveTime >= AUTO_SAVE_INTERVAL) {
            autoSave();
            lastAutoSaveTime = currentTime;
        }
    }

    private void autoSave() {
        if (player == null || world == null || !gameStarted) {
            return;
        }

        // Use the same save file as manual save
        String fileName = String.format("%s/%s%s", SAVES_DIR, player.getUsername(), SAVE_FILE_SUFFIX);

        try {
            saveGameToFile(fileName);
        } catch (Exception e) {
            System.err.println("Error during auto-save: " + e.getMessage());
        }
    }

    // Common save function used by both manual and auto-save
    private void saveGameToFile(String fileName) throws IOException {
        StringBuilder data = new StringBuilder();

        // Basic game state
        data.append(player.getUsername()).append("\n")
                .append(player.getPoints()).append("\n")
                .append(player.getAvatarChoice()).append("\n")
                .append(world.getSeed()).append("\n")
                .append(world.getAvatarX()).append("\n")
                .append(world.getAvatarY()).append("\n")
                .append(world.getChaserX()).append("\n")
                .append(world.getChaserY()).append("\n")
                .append(currentLevel).append("\n"); // Add current level

        // Door position
        data.append(world.getDoorX()).append(",").append(world.getDoorY()).append("\n");

        // Dark mode state
        data.append(world.isDarkMode()).append(",").append(world.getVisionRadius()).append("\n");

        // Use pauseInvisibility to determine remaining duration
        player.pauseInvisibility();
        data.append(player.isInvisible()).append(",").append(player.getRemainingInvisibilityDuration()).append("\n");

        // Save torch positions
        List<Point> torchPositions = world.getTorchPositions();
        data.append(torchPositions.size()).append("\n");
        for (Point p : torchPositions) {
            data.append(p.x).append(",").append(p.y).append("\n");
        }

        // Save consumables and obstacles
        saveConsumables(data);
        saveObstacles(data);

        FileUtils.writeFile(fileName, data.toString());
    }

    private void saveConsumables(StringBuilder data) {
        List<Point> remainingConsumables = new ArrayList<>();
        TETile[][] worldMap = world.getMap();

        for (Point p : world.getConsumablesList()) {
            if (worldMap[p.x][p.y] == Tileset.SMILEY_FACE_green_body_circle ||
                    worldMap[p.x][p.y] == Tileset.SMILEY_FACE_green_body_rhombus) {
                remainingConsumables.add(p);
            }
        }

        data.append(remainingConsumables.size()).append("\n");
        for (Point p : remainingConsumables) {
            TETile tile = worldMap[p.x][p.y];
            String type = (tile == Tileset.SMILEY_FACE_green_body_circle) ? "Smiley Face" : "Normal Face";
            data.append(p.x).append(",").append(p.y).append(",").append(type).append("\n");
        }
    }

    private void saveObstacles(StringBuilder data) {
        Map<Point, ObstacleType> obstacles = world.getObstacleMap();
        data.append(obstacles.size()).append("\n");
        for (Map.Entry<Point, ObstacleType> entry : obstacles.entrySet()) {
            Point p = entry.getKey();
            data.append(p.x).append(",")
                    .append(p.y).append(",")
                    .append(entry.getValue().name()).append("\n");
        }
    }

    public void showTutorial() {
        String[][] tutorialPages = {
                {
                        translationManager.getTranslation("how_to_play"),
                        "",
                        translationManager.getTranslation("tutorial_intro"),
                        translationManager.getTranslation("tutorial_nav")
                },
                {
                        translationManager.getTranslation("movement_controls"),
                        "",
                        translationManager.getTranslation("tutorial_movement_intro"),
                        translationManager.getTranslation("tutorial_move_up"),
                        translationManager.getTranslation("tutorial_move_left"),
                        translationManager.getTranslation("tutorial_move_down"),
                        translationManager.getTranslation("tutorial_move_right")
                },
                {
                        translationManager.getTranslation("game_objective"),
                        "",
                        translationManager.getTranslation("tutorial_goal_line1"),
                        translationManager.getTranslation("tutorial_goal_line2"),
                        translationManager.getTranslation("tutorial_goal_line3")
                },
                {
                        translationManager.getTranslation("special_abilities"),
                        "",
                        translationManager.getTranslation("tutorial_special_line1"),
                        translationManager.getTranslation("tutorial_special_line2"),
                        translationManager.getTranslation("tutorial_special_line3")
                },
                {
                        translationManager.getTranslation("other_controls"),
                        "",
                        translationManager.getTranslation("tutorial_other_line1"),
                        translationManager.getTranslation("tutorial_other_line2"),
                        translationManager.getTranslation("tutorial_other_line3")
                }
        };

        int currentPage = 0;
        boolean inTutorial = true;

        while (inTutorial) {
            // Clear the screen
            StdDraw.clear(StdDraw.BLACK);
            StdDraw.setPenColor(StdDraw.WHITE);

            // Draw the current tutorial page
            String[] currentPageContent = tutorialPages[currentPage];
            double yPosition = 35;
            for (String line : currentPageContent) {
                StdDraw.text(40, yPosition, line);
                yPosition -= 2;
            }

            // Draw navigation instructions
            StdDraw.text(40, 10, String.format(translationManager.getTranslation("tutorial_page"),
                    currentPage + 1, tutorialPages.length));

            // Update navigation instructions based on current page
            if (currentPage == 0) {
                StdDraw.text(40, 8, translationManager.getTranslation("tutorial_next"));
            } else if (currentPage == tutorialPages.length - 1) {
                StdDraw.text(40, 8, translationManager.getTranslation("tutorial_prev"));
            } else {
                StdDraw.text(40, 8, translationManager.getTranslation("tutorial_nav_both"));
            }

            StdDraw.show();

            // Wait for input
            while (!StdDraw.hasNextKeyTyped()) {
                // Just wait for input
                StdDraw.pause(50);
            }

            char key = StdDraw.nextKeyTyped();
            // Handle navigation - space or right arrow (d) for next page
            if (key == ' ' || key == 'd') {
                // Next page
                currentPage++;
                if (currentPage >= tutorialPages.length) {
                    inTutorial = false;
                }
            }
            // Handle navigation - left arrow (a) for previous page
            else if (key == 'a') {
                // Previous page
                currentPage--;
                if (currentPage < 0) {
                    currentPage = 0; // Stay on first page
                }
            }
            // ESC key to exit
            else if (key == 27) { // ESC key
                inTutorial = false;
            }
        }

        // Return to main menu
        redraw = true;
    }
}
