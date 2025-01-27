package core;

public class Notification {
    private String message;
    private long displayTime; // Time to display the message
    private long startTime;

    public Notification(String message, long displayTime) {
        this.message = message;
        this.displayTime = displayTime;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - startTime) > displayTime;
    }

    public String getMessage() {
        return message;
    }
}
