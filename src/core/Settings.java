package core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Settings {
    private static Settings instance;
    private Map<String, Object> settings;
    private static final String SETTINGS_FILE = "settings.dat";

    private Settings() {
        settings = new HashMap<>();
        loadDefaultSettings();
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    private void loadDefaultSettings() {
        settings.put("masterVolume", 1.0f);
        settings.put("musicVolume", 0.8f);
        settings.put("sfxVolume", 1.0f);
        settings.put("difficulty", 2); // 1=Easy, 2=Normal, 3=Hard
        loadSettings(); // Try to load saved settings
    }

    public void saveSettings() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SETTINGS_FILE))) {
            out.writeObject(settings);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    void loadSettings() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SETTINGS_FILE))) {
            Map<String, Object> loadedSettings = (Map<String, Object>) in.readObject();
            settings.putAll(loadedSettings); // Merge loaded settings with defaults
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No settings file found, using defaults");
        }
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
        applySettings();
    }

    private void applySettings() {
        // Apply volume settings
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.setMasterVolume((float) settings.get("masterVolume"));

    }
}