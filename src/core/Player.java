package core;

public class Player {
    private String username;
    private int points;
    private boolean isInvisible = false;

    public Player(String username) {
        this.username = username;
        this.points = 0;
    }

    public Player(String username, int points) {
        this.username = username;
        this.points = points;
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

    public boolean isInvisible() {
        return isInvisible;
    }

    public boolean purchaseInvisibilityCure() {
        int cost = 50; // Cost for the cure
        if (points >= cost && !isInvisible) {
            points -= cost;
            isInvisible = true;
            // Immediately reduce walk volume (the caller in GameMenu already calls it,
            // but in case of programmatic use we ensure it here as well).
            AudioManager.getInstance().setWalkVolume(0.1f);
            // Optionally, schedule a timer to disable invisibility after a fixed duration.
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // invisibility lasts for 5 seconds; adjust as needed
                } catch (InterruptedException e) {
                    // handle interruption
                }
                isInvisible = false;
                // Restore normal walk volume after invisibility expires.
                AudioManager.getInstance().setWalkVolume(0.3f);
            }).start();
            return true;
        }
        return false;
    }
}
