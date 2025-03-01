package core;

public class InGameInputHandler implements InputHandler {
    private final GameMenu gameMenu;

    public InGameInputHandler(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public boolean handleInput(char key) throws InterruptedException {
        if (gameMenu.settingsMenu.isVisible()) {
            gameMenu.settingsMenu.handleInput(key);
            return true;
        }

        if (key == ':') {
            gameMenu.quitSignBuilder.setLength(0);
            gameMenu.quitSignBuilder.append(key);
        } else if (key == 'q' && gameMenu.quitSignBuilder.toString().equals(":")) {
            AudioManager.getInstance().playSound("menu");
            gameMenu.saveGame(gameMenu.player);
            AudioManager.getInstance().stopAllSoundsExcept("menu");
            gameMenu.currentState = GameMenu.GameState.MAIN_MENU;
            gameMenu.menuItems.clear(); // Clear menu items to force refresh
            gameMenu.redraw = true;
            gameMenu.quitSignBuilder.setLength(0);
        } else if (key == 'p') {
            gameMenu.handlePause();
        } else if (key == 'o') { // New key for settings
            gameMenu.settingsMenu.show();
        } else if (key == 'n') {
            gameMenu.handleRestart();
        } else if (!gameMenu.isPaused) {
            if (key == 'v') {
                if (gameMenu.player.purchaseInvisibilityCure()) {
                    gameMenu.world.updateAvatarTile();
                    AudioManager.getInstance().setWalkVolume(0.1f);
                    gameMenu.notifications
                            .add(new Notification("Invisibility activated!", System.currentTimeMillis() + 2000));
                } else {
                    gameMenu.notifications
                            .add(new Notification("Cannot activate invisibility!", System.currentTimeMillis() + 2000));
                }
                gameMenu.redraw = true;
            } else if (key == 'w' || key == 'a' || key == 's' || key == 'd') {
                gameMenu.handleMovement(key);
            }
        }
        return true;
    }


}