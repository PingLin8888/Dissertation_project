package core;

public class Notification {
    private final String message;
    private final long expiryTime; // Timestamp when notification should expire

    public Notification(String message, long expiryTime) {
        this.message = message;
        this.expiryTime = expiryTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public String getMessage() {
        return message;
    }
}
