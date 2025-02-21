package core;

public class Player {
    private String username;
    private int points;
    private boolean isInvisible = false;
    private long invisibilityEndTime = 0;
    private int avatarChoice = 0; // Default avatar

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
        int cost = 10; // Cost for the cure
        if (points >= cost) {
            points -= cost;
            isInvisible = true;
            // Set invisibility expiration timestamp (5 seconds from now)
            invisibilityEndTime = System.currentTimeMillis() + 10000;
            // Immediately reduce walk volume
            AudioManager.getInstance().setWalkVolume(0.1f);
            // Start playing invisibility sound effect
            AudioManager.getInstance().playLoopingSound("invisibility");
            return true;
        }
        return false;
    }

    // Returns true if invisibility just wore off this update
    public boolean updateInvisibility() {
        if (isInvisible && System.currentTimeMillis() >= invisibilityEndTime) {
            isInvisible = false;
            // Restore normal walk volume when invisibility expires.
            AudioManager.getInstance().setWalkVolume(0.3f);
            // Stop invisibility sound effect
            AudioManager.getInstance().stopLoopingSound("invisibility");
            return true; // Indicate that invisibility just wore off
        }
        return false; // No change in invisibility state
    }

    public void setAvatarChoice(int choice) {
        this.avatarChoice = choice;
    }

    public int getAvatarChoice() {
        return avatarChoice;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
