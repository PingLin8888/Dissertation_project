package core;

import edu.princeton.cs.algs4.StdDraw;

public class MainMenuInputHandler implements InputHandler {
    private final GameMenu gameMenu;

    public MainMenuInputHandler(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public boolean handleInput(char key) {
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
                AudioManager.getInstance().playSound("menu");
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
                AudioManager.getInstance().playSound("menu");
                gameMenu.saveGame(gameMenu.player);
                AudioManager.getInstance().stopAllSoundsExcept("menu");
                System.exit(0);
                break;
        }
        return true;
    }

   
}