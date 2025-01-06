package core;

public class Player {
    private String username;
    private int points;

    public Player(String username) {
        this.username = username;
        this.points = 0; // Start with 0 points
    }

    public String getUsername() {
        return username;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public int calculateDifficulty() {
        if (points < 50) {
            return 1; // Easy
        } else if (points < 150) {
            return 2; // Medium
        } else {
            return 3; // Hard
        }
    }
}
