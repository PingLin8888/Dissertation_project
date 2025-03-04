package core;

public class Player {
    private String username;
    private int points;
    private boolean isInvisible = false;
    private long invisibilityEndTime = 0;
    private long remainingInvisibilityDuration = 0; // Track remaining duration when paused
    private static final long INVISIBILITY_DURATION = 10000; // 10 seconds in milliseconds
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

    public boolean purchaseInvisibilityCure() {
        int cost = 10; // Cost for the cure
        if (points >= cost) {
            points -= cost;
            isInvisible = true;
            // Set invisibility expiration timestamp
            invisibilityEndTime = System.currentTimeMillis() + INVISIBILITY_DURATION;
            // Immediately reduce walk volume
            AudioManager audioManager = AudioManager.getInstance();
            audioManager.setWalkVolume(0.1f);

            // Only play invisibility sound if master volume is not 0
            if (audioManager.getMasterVolume() > 0.001f) {
                // Start playing invisibility sound effect
                audioManager.playLoopingSound("invisibility");
            }
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

    // Add method to handle game pause
    public void pauseInvisibility() {
        if (isInvisible) {
            // Calculate remaining duration when paused
            remainingInvisibilityDuration = Math.max(0, invisibilityEndTime - System.currentTimeMillis());
        }
    }

    // Add method to handle game unpause
    public void resumeInvisibility() {
        if (isInvisible && remainingInvisibilityDuration > 0) {
            // Reset end time based on remaining duration
            invisibilityEndTime = System.currentTimeMillis() + remainingInvisibilityDuration;
        }
    }

    public boolean isInvisible() {
        return isInvisible;
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

    // Add this method to set invisibility state and remaining duration
    public void setInvisibilityState(boolean isInvisible, long remainingDuration) {
        this.isInvisible = isInvisible;
        if (isInvisible) {
            this.remainingInvisibilityDuration = remainingDuration;
            this.invisibilityEndTime = System.currentTimeMillis() + remainingDuration;
        } else {
            this.remainingInvisibilityDuration = 0;
        }
    }

    // Add a getter for remainingInvisibilityDuration
    public long getRemainingInvisibilityDuration() {
        return remainingInvisibilityDuration;
    }
}
