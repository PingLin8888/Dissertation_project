package core;

public class LoginInputHandler implements InputHandler {
    private final GameMenu gameMenu;

    public LoginInputHandler(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public boolean handleInput(char key) {
        AudioManager.getInstance().playSound("menu");
        switch (key) {
            case 'p':
                gameMenu.player = gameMenu.loginOrCreateProfile();
                gameMenu.currentState = GameMenu.GameState.MAIN_MENU;
                break;
            case 'q':
                System.exit(0);
                break;
        }
        return true;
    }


}