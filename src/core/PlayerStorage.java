package core;

public class PlayerStorage {
    // This class can be removed, or kept minimal for future extensions
    public static void savePlayer(Player player) {
        // No need to save separately - all data is in save file
    }

    public static Player loadPlayer(String username) {
        // This method is no longer needed as loading is handled in GameMenu
        return null;
    }
}