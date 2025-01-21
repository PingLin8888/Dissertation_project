package core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class TranslationManager {
    private Map<String, String> translations = new HashMap<>();

    public TranslationManager(Language language) {
        loadTranslations(language);
    }

    private void loadTranslations(Language language) {
        String fileName;
        if (language == Language.CHINESE) {
            fileName = "messages_zh.properties";
        } else {
            fileName = "messages_en.properties";
        }

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
                InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + fileName);
                return;
            }
            Properties prop = new Properties();
            prop.load(reader);
            for (String key : prop.stringPropertyNames()) {
                System.out.println("Loaded key: " + key + " with value: " + prop.getProperty(key));
                translations.put(key, prop.getProperty(key));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getTranslation(String key, Object... args) {
        String translation = translations.get(key);
        if (translation != null && args.length > 0) {
            return String.format(translation, args);
        }
        return translation;
    }
}