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

    // Add new field
    private boolean isPaused = false;

    public GameMenu() {
        initializeTranslations();
    }

    private void initializeTranslations() {
        translationManager = new TranslationManager(currentLanguage);
    }

    public void createGameMenu() throws InterruptedException {
        setupCanvas();
        ter = new TERenderer();

        // Set initial state
        currentState = GameState.LANGUAGE_SELECT;

        // Reduce frame sleep time for more responsive input
        long lastUpdateTime = System.currentTimeMillis();
        final long FRAME_TIME = 16; // Target ~60 FPS

        while (true) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastUpdateTime;

            // Handle input
            boolean inputHandled = handleInput();
            boolean mouseMoved = detectMouseMove();
            boolean chaserMoved = false;

            // Update game state using deltaTime
            if (currentState == GameState.IN_GAME) {
                chaserMoved = updateChaser();
            }

            // Render if needed
            if (redraw || inputHandled || mouseMoved || chaserMoved) {
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
            checkChaserEncounter();
            lastChaserMoveTime = currentTime;
            return true;
        }
        return false;
    }

    private void render() {
        StdDraw.clear(StdDraw.BLACK);

        setDrawColor(Color.WHITE); // Set default color

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
                if (!isPaused) {
                    renderGameScreen();
                }
                break;
        }

        StdDraw.show();
        redraw = false;
    }

    private void renderLanguageSelect() {
        // No need to setup drawing here, already done in render()
        StdDraw.text(40, 26, "Select Language");
        StdDraw.text(40, 24, "Press E for English");
        StdDraw.text(40, 22, "按 'C' 选择中文");
    }

    private void renderGameScreen() {
        ter.renderFrame(world.getVisibleMap());
        updateHUD();
        if (world.isShowPath() && world.getPathToAvatar() != null) {
            drawPath();
        }
        StdDraw.show();
        renderNotifications();
    }

    private boolean handleInput() throws InterruptedException {
        if (!StdDraw.hasNextKeyTyped()) {
            return false;
        }

        char key = Character.toLowerCase(StdDraw.nextKeyTyped());
        redraw = true;

        // Handle movement keys first for faster response
        if (currentState == GameState.IN_GAME) {
            switch (key) {
                case 'w', 'a', 's', 'd' -> {
                    handleMovement(key);
                    return true;
                }
            }
            handleGameInput(key);
        } else {
            // Handle other states...
            switch (currentState) {
                case LANGUAGE_SELECT -> handleLanguageSelection(key);
                case LOGIN -> handleLoginInput(key);
                case MAIN_MENU -> handleMainMenuInput(key);
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
        // Load a font that supports Chinese characters
        Font font = new Font("SimSun", Font.PLAIN, 24);
        StdDraw.setFont(font);

        // Draw everything to the back buffer
        String loginText = translationManager.getTranslation("login");
        String quitText = translationManager.getTranslation("quit");
        StdDraw.text(40, 24, loginText);
        StdDraw.text(40, 22, quitText);

        // Show the back buffer
        StdDraw.show();
    }

    private void drawPostLoginMenu(Player player) {
        StdDraw.clear(StdDraw.BLACK);
        Font font = new Font("SimSun", Font.PLAIN, 24);
        StdDraw.setFont(font);

        // More spread out menu items with consistent spacing
        StdDraw.text(40, 35, translationManager.getTranslation("main_menu"));
        StdDraw.text(40, 30, translationManager.getTranslation("welcome", player.getUsername()));
        StdDraw.text(40, 25, translationManager.getTranslation("points", player.getPoints()));
        StdDraw.text(40, 20, "N - " + translationManager.getTranslation("new_game"));

        // Set color based on whether saved game exists
        if (hasSavedGame) {
            StdDraw.setPenColor(Color.WHITE);
        } else {
            StdDraw.setPenColor(Color.GRAY);
        }
        StdDraw.text(40, 15, "L - " + translationManager.getTranslation("load_game"));

        // Reset color to white for remaining options
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(40, 10, "C - " + translationManager.getTranslation("change_avatar"));
        StdDraw.text(40, 5, "Q - " + translationManager.getTranslation("quit"));

        StdDraw.show();
    }

    private boolean checkSavedGameExists(String username) {
        String fileName = "game_data.txt";
        try {
            String contents = FileUtils.readFile(fileName);
            if (contents == null || contents.trim().isEmpty()) {
                return false;
            }
            String[] lines = contents.split("\n");
            return lines.length > 0 && lines[0].equals(username);
        } catch (Exception e) {
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
        // Update player invisibility status in case the duration has expired.
        player.updateInvisibility();
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

    private String getInstructions(TETile tile) {
        // Implement the logic to determine instructions based on the tile
        // This is a placeholder and should be replaced with the actual implementation
        return "No instructions available for this tile.";
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
        StdDraw.textLeft(2, 42, hudCache.playerInfo);
        StdDraw.textLeft(2, 40, hudCache.pointsInfo);
        StdDraw.textLeft(2, 38, hudCache.tileDescription);
        StdDraw.textLeft(2, 36, hudCache.instructions);
    }

    private Player loginOrCreateProfile() {
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        // Get username first
        String username = getUsernameInput();

        // Check if player exists
        Player loadedPlayer = PlayerStorage.loadPlayer(username);
        if (loadedPlayer != null) {
            // Check for saved game when loading player
            hasSavedGame = checkSavedGameExists(username);
            return loadedPlayer;
        }

        // If new player, show avatar selection
        int avatarChoice = showAvatarSelection();
        Player newPlayer = new Player(username);
        newPlayer.setAvatarChoice(avatarChoice);
        PlayerStorage.savePlayer(newPlayer); // Save the new player with their avatar choice
        return newPlayer;
    }

    private String getUsernameInput() {
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

    private int showAvatarSelection() {
        StdDraw.clear(StdDraw.BLACK);
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
            String label = (i + 1) + ": " + avatar.getName();
            if (player != null && player.getAvatarChoice() == avatar.getIndex()) {
                label += " (Current)";
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

    private void createNewGame() {
        StdDraw.clear(StdDraw.BLACK);

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
            if (world.getAvatarX() == world.getDoorX() && world.getAvatarY() == world.getDoorY()) {
                exitDoor();
            }
        }
    }

    private void exitDoor() {
        // Stop all ongoing sound effects
        AudioManager.getInstance().stopSound("chaser");
        AudioManager.getInstance().fadeOutSound("eerie", 2000);
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
        cleanupGameSounds();
        StdDraw.clear(StdDraw.BLACK);
        AudioManager.getInstance().stopSound("chaser"); // Stop any lingering chaser sound.
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Level " + currentLevel + " Complete!");
        StdDraw.text(40, 23, "Points earned: " + pointsEarned);
        StdDraw.text(40, 26, "Total points: " + player.getPoints());
        StdDraw.show();
        StdDraw.pause(3000);
    }

    private void showNewLevelMessage() {
        cleanupGameSounds();
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(40, 20, "Level " + currentLevel);
        StdDraw.text(40, 23, "Get ready!");
        StdDraw.text(40, 26, "Chaser is faster now!");
        StdDraw.show();
        StdDraw.pause(3000);
    }

    private void showGameCompleteMessage() {
        cleanupGameSounds();
        StdDraw.clear(StdDraw.BLACK);
        StdDraw.setPenColor(StdDraw.WHITE);

        // Center the completion message with good spacing
        StdDraw.text(40, 30, "Congratulations!");
        StdDraw.text(40, 25, "You've completed all " + MAX_LEVEL + " levels!");
        StdDraw.text(40, 20, "Final Score: " + player.getPoints());
        StdDraw.text(40, 15, "Press any key to continue");

        StdDraw.show();
        StdDraw.pause(3000);
    }

    public void saveGame(Player player) {
        if (player == null || world == null) {
            return;
        }

        String fileName = "game_data.txt";
        try {
            StringBuilder data = new StringBuilder();
            // Basic game state
            data.append(player.getUsername()).append("\n");
            data.append(player.getPoints()).append("\n");
            data.append(player.getAvatarChoice()).append("\n");
            data.append(world.getSeed()).append("\n");
            data.append(world.getAvatarX()).append("\n");
            data.append(world.getAvatarY()).append("\n");
            data.append(world.getChaserX()).append("\n");
            data.append(world.getChaserY()).append("\n");

            // Save door position
            data.append(world.getDoorX()).append(",").append(world.getDoorY()).append("\n");

            // Save only remaining consumables (those that haven't been eaten)
            List<Point> remainingConsumables = new ArrayList<>();
            TETile[][] worldMap = world.getMap();
            for (Point p : world.getConsumablesList()) {
                // Only save if the consumable is still there (not eaten)
                if (worldMap[p.x][p.y] == Tileset.SMILEY_FACE_green_body_circle ||
                        worldMap[p.x][p.y] == Tileset.SMILEY_FACE_green_body_rhombus) {
                    remainingConsumables.add(p);
                }
            }

            // Save number of remaining consumables
            data.append(remainingConsumables.size()).append("\n");
            // Save each remaining consumable with its type
            for (Point p : remainingConsumables) {
                TETile tile = worldMap[p.x][p.y];
                String type = (tile == Tileset.SMILEY_FACE_green_body_circle) ? "Smiley Face" : "Normal Face";
                data.append(p.x).append(",").append(p.y).append(",").append(type).append("\n");
            }

            // Save obstacles
            Map<Point, ObstacleType> obstacles = world.getObstacleMap();
            data.append(obstacles.size()).append("\n");
            for (Map.Entry<Point, ObstacleType> entry : obstacles.entrySet()) {
                Point p = entry.getKey();
                data.append(p.x).append(",")
                        .append(p.y).append(",")
                        .append(entry.getValue().name()).append("\n");
            }

            FileUtils.writeFile(fileName, data.toString());
            PlayerStorage.savePlayer(player);
        } catch (Exception e) {
            System.err.println("Error saving game: " + e.getMessage());
        }
    }

    public void loadGame(Player player) {
        String fileName = "game_data.txt";
        try {
            String contents = FileUtils.readFile(fileName);
            String[] lines = contents.split("\n");
            int currentLine = 0;

            String savedUsername = lines[currentLine++];
            if (!savedUsername.equals(player.getUsername())) {
                return;
            }

            int points = Integer.parseInt(lines[currentLine++]);
            currentLine++; // Skip avatar choice line
            long seed = Long.parseLong(lines[currentLine++]);
            int avatarX = Integer.parseInt(lines[currentLine++]);
            int avatarY = Integer.parseInt(lines[currentLine++]);
            int chaserX = Integer.parseInt(lines[currentLine++]);
            int chaserY = Integer.parseInt(lines[currentLine++]);

            // Create world with seed but don't populate items yet (pass 0,0 to prevent door
            // placement)
            world = new World(player, seed, 0, 0);

            // Set positions
            world.setAvatarToNewPosition(avatarX, avatarY);
            world.setChaserToNewPosition(chaserX, chaserY);

            // Load door position
            String[] doorPos = lines[currentLine++].split(",");
            world.setDoorPosition(Integer.parseInt(doorPos[0]), Integer.parseInt(doorPos[1]));

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

            player.setPoints(points);
            gameStarted = true;
            drawWorld();
        } catch (Exception e) {
            System.err.println("Error loading game: " + e.getMessage());
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

    private void handleRestart() throws InterruptedException {
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
                    StdDraw.text(40, 24, translationManager.getTranslation("no_saved_game"));
                    StdDraw.show();
                    StdDraw.pause(2000);

                    // Return to main menu
                    redraw = true;
                    currentState = GameState.MAIN_MENU;
                }
                break;
            case 'c': // Add case for avatar customization
                AudioManager.getInstance().playSound("menu");
                int newAvatarChoice = showAvatarSelection();
                player.setAvatarChoice(newAvatarChoice);
                if (world != null) {
                    world.updateAvatarTile();
                }
                redraw = true;
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
            cleanupGameSounds();
            currentState = GameState.MAIN_MENU;
            redraw = true;
            quitSignBuilder.setLength(0);
        } else if (key == 'p') {
            // Toggle pause state
            isPaused = !isPaused;
            AudioManager.getInstance().playSound("menu");
            if (isPaused) {
                // Pause all game sounds
                AudioManager.getInstance().stopSound("chaser");
                AudioManager.getInstance().stopSound("eerie");
                drawPauseMenu();
            } else {
                // Resume game, redraw game screen
                redraw = true;
            }
        } else if (!isPaused) { // Only process other inputs if not paused
            if (key == 'n') {
                handleRestart();
            } else if (key == 'v') {
                // When V is pressed, try to purchase invisibility cure.
                if (player.purchaseInvisibilityCure()) {
                    // Update the avatar tile to reflect invisibility.
                    world.updateAvatarTile();
                    // Reduce walk sound volume.
                    AudioManager.getInstance().setWalkVolume(0.1f);
                    notifications.add(new Notification("Invisibility activated!", System.currentTimeMillis() + 2000));
                } else {
                    notifications
                            .add(new Notification("Cannot activate invisibility!", System.currentTimeMillis() + 2000));
                }
                redraw = true;
            } else if (key == 'w' || key == 'a' || key == 's' || key == 'd') {
                handleMovement(key);
            }
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
        if (world.getChaserX() == world.getAvatarX() && world.getChaserY() == world.getAvatarY()) {
            // Play game over sound
            AudioManager.getInstance().playSound("gameover");

            // Clear the screen and display the message
            StdDraw.text(40, 24, translationManager.getTranslation("game_over"));
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

    private void cleanupGameSounds() {
        // Stop all game-related sounds
        AudioManager.getInstance().stopSound("chaser");
        AudioManager.getInstance().fadeOutSound("eerie", 2000);
        AudioManager.getInstance().stopSound("walk");
    }

    // Add new method to draw pause menu
    private void drawPauseMenu() {
        Font font = new Font("SimSun", Font.PLAIN, 24);
        StdDraw.setFont(font);

        // Increased vertical spacing between lines
        StdDraw.text(40, 35, translationManager.getTranslation("game_paused"));
        StdDraw.text(40, 28, translationManager.getTranslation("press_p_resume"));
        StdDraw.text(40, 21, translationManager.getTranslation("press_n_restart"));
        StdDraw.text(40, 14, ":Q - " + translationManager.getTranslation("save_and_quit"));

        StdDraw.show();
    }
}
