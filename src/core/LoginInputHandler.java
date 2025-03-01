package core;

public class LoginInputHandler implements InputHandler {
    private final GameMenu gameMenu;

    public LoginInputHandler(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public boolean handleInput(char key) {
        switch (key) {
            case 'p':
                AudioManager.getInstance().playSound("menu");
                gameMenu.player = gameMenu.loginOrCreateProfile();
                gameMenu.currentState = GameMenu.GameState.MAIN_MENU;
                break;
            case 'q':
                AudioManager.getInstance().playSound("menu");
                System.exit(0);
                break;
        }
        return true;
    }

    @Override
    public int getPriority() {
        return 1; // Priority can be adjusted as needed
    }
}