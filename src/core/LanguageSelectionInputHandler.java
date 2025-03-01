package core;

public class LanguageSelectionInputHandler implements InputHandler {
    private final GameMenu gameMenu;

    public LanguageSelectionInputHandler(GameMenu gameMenu) {
        this.gameMenu = gameMenu;
    }

    @Override
    public boolean handleInput(char key) {
        if (key == 'e') {
            gameMenu.currentLanguage = Language.ENGLISH;
        } else if (key == 'c') {
            gameMenu.currentLanguage = Language.CHINESE;
        }
        AudioManager.getInstance().playSound("menu");
        gameMenu.languageMenuItems.clear(); // Clear animations
        gameMenu.currentState = GameMenu.GameState.LOGIN;
        gameMenu.initializeTranslations();

        return true;
    }


}