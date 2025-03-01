package core;

import edu.princeton.cs.algs4.StdDraw;
import java.awt.Color;

public class SettingsMenu {
    private Settings settings;
    private boolean isVisible;
    private int selectedOption;
    private static final int NUM_OPTIONS = 4;
    private TranslationManager translationManager;

    public SettingsMenu(TranslationManager translationManager) {
        this.settings = Settings.getInstance();
        this.translationManager = translationManager;
        this.isVisible = false;
        this.selectedOption = 0;
    }

    public void show() {
        isVisible = true;
        render();
    }

    public void hide() {
        isVisible = false;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void handleInput(char key) {
        boolean needsRender = true;
        switch (key) {
            case 'w' -> moveSelection(-1);
            case 's' -> moveSelection(1);
            case 'a' -> adjustSetting(-1);
            case 'd' -> adjustSetting(1);
            case '\u001B' -> {
                hide(); // ESC to exit
                needsRender = false;
            }
        }
        if (needsRender) {
            render();
        }
    }

    private void moveSelection(int direction) {
        selectedOption = (selectedOption + direction + NUM_OPTIONS) % NUM_OPTIONS;
        AudioManager.getInstance().playSound("menu");
    }

    private void adjustSetting(int direction) {
        AudioManager.getInstance().playSound("menu");
        switch (selectedOption) {
            case 0 -> adjustVolume("masterVolume", direction);
            case 1 -> adjustVolume("musicVolume", direction);
            case 2 -> adjustVolume("sfxVolume", direction);
            case 3 -> adjustDifficulty(direction);
        }
        settings.saveSettings();
    }

    private void adjustVolume(String volumeType, int direction) {
        float currentVolume = (float) settings.getSetting(volumeType);
        float newVolume = Math.max(0.0f, Math.min(1.0f, currentVolume + direction * 0.1f));
        settings.setSetting(volumeType, newVolume);
    }

    private void adjustDifficulty(int direction) {
        int currentDifficulty = (int) settings.getSetting("difficulty");
        int newDifficulty = Math.max(1, Math.min(3, currentDifficulty + direction));
        settings.setSetting("difficulty", newDifficulty);
    }

    public void render() {
        // Draw semi-transparent dark overlay
        StdDraw.setPenColor(new Color(0, 0, 0, 180));
        StdDraw.filledRectangle(40, 25, 30, 20);

        // Draw title box
        StdDraw.setPenColor(new Color(40, 40, 40));
        StdDraw.filledRectangle(40, 40, 25, 2);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(40, 40, translationManager.getTranslation("settings_title"));

        // Draw settings options
        drawSettingOption(35, "Master Volume", "masterVolume", 0);
        drawSettingOption(31, "Music Volume", "musicVolume", 1);
        drawSettingOption(27, "SFX Volume", "sfxVolume", 2);
        drawSettingOption(23, "Difficulty", "difficulty", 3);

        // Draw controls help box
        StdDraw.setPenColor(new Color(40, 40, 40));
        StdDraw.filledRectangle(40, 10, 25, 2);
        StdDraw.setPenColor(new Color(200, 200, 200));
        StdDraw.text(40, 10, "W/S - Navigate | A/D - Adjust | ESC - Back");

        StdDraw.show();
    }

    private void drawSettingOption(double y, String label, String settingKey, int optionIndex) {
        boolean isSelected = selectedOption == optionIndex;

        // Draw selection arrows if selected
        if (isSelected) {
            StdDraw.setPenColor(Color.YELLOW);
            StdDraw.text(15, y, ">");
            StdDraw.text(65, y, "<");
        }

        // Draw label with appropriate color
        StdDraw.setPenColor(isSelected ? Color.YELLOW : Color.WHITE);
        StdDraw.text(25, y, label + ":");

        // Draw value with appropriate color
        Object value = settings.getSetting(settingKey);
        String displayValue = formatSettingValue(settingKey, value);
        StdDraw.setPenColor(isSelected ? Color.YELLOW : new Color(180, 180, 180));
        StdDraw.text(55, y, displayValue);
    }

    private String formatSettingValue(String key, Object value) {
        if (value instanceof Float) {
            float floatValue = (float) value;
            return String.format("%.0f%%", floatValue * 100);
        } else if (key.equals("difficulty")) {
            int diff = (int) value;
            return switch (diff) {
                case 1 -> "Easy";
                case 2 -> "Normal";
                case 3 -> "Hard";
                default -> "Unknown";
            };
        }
        return value.toString();
    }
}