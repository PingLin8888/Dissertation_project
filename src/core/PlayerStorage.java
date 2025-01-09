package core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerStorage {
    private static final String FILE_PATH = "players.dat";

    public static void savePlayer(Player player) {
        Map<String, Integer> players = loadAllPlayers();
        players.put(player.getUsername(), player.getPoints());
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(players);
        } catch (IOException e) {
            System.err.println("Failed to save player data: " + e.getMessage());
        }
    }

    public static Player loadPlayer(String username) {
        Map<String, Integer> players = loadAllPlayers();
        if (players.containsKey(username)) {
            return new Player(username, players.get(username));
        }
        return null;
    }

    private static Map<String, Integer> loadAllPlayers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (Map<String, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>(); // Return an empty map if no file exists
        }
    }
}