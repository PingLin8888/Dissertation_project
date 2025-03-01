package core;

import edu.princeton.cs.algs4.StdDraw;

public class MainMenuInputHandler implements InputHandler {
    private final GameMenu gameMenu;

    public MainMenuInputHandler(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public boolean handleInput(char key) {
        // If settings menu is visible, handle its input first
        if (gameMenu.settingsMenu.isVisible()) {
            gameMenu.settingsMenu.handleInput(key);
            gameMenu.redraw = true;
            return true;
        }

        AudioManager.getInstance().playSound("menu");

        switch (key) {
            case '1':
                if (gameMenu.hasSavedGame) {
                    gameMenu.loadGame(gameMenu.player);
                    gameMenu.drawWorld();
                    AudioManager.getInstance().playSound("gamestart");
                    gameMenu.currentState = GameMenu.GameState.IN_GAME;
                } else {
                    // Handle case where no saved game exists
                    StdDraw.clear(StdDraw.BLACK);
                    StdDraw.text(40, 24, gameMenu.translationManager.getTranslation("no_saved_game"));
                    StdDraw.show();
                    StdDraw.pause(2000);
                }
                break;
            case '2':
                if (gameMenu.confirmNewGame()) {
                    gameMenu.createNewGame();
                    AudioManager.getInstance().playSound("gamestart");
                    gameMenu.currentState = GameMenu.GameState.IN_GAME;
                }
                break;
            case '3':
                int newAvatarChoice = gameMenu.showAvatarSelection();
                gameMenu.player.setAvatarChoice(newAvatarChoice);
                if (gameMenu.world != null) {
                    gameMenu.world.updateAvatarTile();
                }
                // Save game after avatar change to persist the choice
                if (gameMenu.hasSavedGame) {
                    gameMenu.saveGame(gameMenu.player);
                }
                gameMenu.redraw = true;
                break;
            case '4':
                gameMenu.saveGame(gameMenu.player);
                AudioManager.getInstance().stopAllSoundsExcept("menu");
                System.exit(0);
                break;
            case '5':
                gameMenu.menuItems.clear(); // Clear menu items
                gameMenu.settingsMenu.show();
                gameMenu.redraw = true;
                break;
        }
        return true;
    }

}