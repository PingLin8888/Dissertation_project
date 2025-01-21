package core;

import java.io.IOException;
import java.io.InputStream;
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
        if (language == Language.ENGLISH) {
            fileName = "messages_en.properties";
        } else {
            fileName = "messages_zh.properties";
        }

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + fileName);
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            for (String key : prop.stringPropertyNames()) {
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