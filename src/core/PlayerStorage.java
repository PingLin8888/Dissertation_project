package core;

import utils.FileUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerStorage {
    private static final String FILE_PATH = "players.dat";

    public static void savePlayer(Player player) {
        String fileName = player.getUsername() + "_profile.txt";
        try {
            StringBuilder data = new StringBuilder();
            data.append(player.getUsername()).append("\n");
            data.append(player.getPoints()).append("\n");
            data.append(player.getAvatarChoice()).append("\n"); // Save avatar choice in profile

            FileUtils.writeFile(fileName, data.toString());
        } catch (Exception e) {
            System.err.println("Error saving player profile: " + e.getMessage());
        }
    }

    public static Player loadPlayer(String username) {
        String fileName = username + "_profile.txt";
        try {
            String contents = FileUtils.readFile(fileName);
            if (contents == null || contents.trim().isEmpty()) {
                return null;
            }

            String[] lines = contents.split("\n");
            if (lines.length < 3) { // Check we have all required data
                return null;
            }

            String savedUsername = lines[0];
            int points = Integer.parseInt(lines[1]);
            int avatarChoice = Integer.parseInt(lines[2]); // Load avatar choice from profile

            Player player = new Player(savedUsername, points);
            player.setAvatarChoice(avatarChoice);
            return player;
        } catch (Exception e) {
            System.err.println("Error loading player profile: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, Integer> loadAllPlayers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (Map<String, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>(); // Return an empty map if no file exists
        }
    }
}