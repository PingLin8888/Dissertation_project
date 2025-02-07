package core;

public class Event {
    public enum EventType {
        CONSUMABLE_CONSUMED,
        OBSTACLE_HIT,
        OBSTACLE_END,
        ITEM_PICKUP,
        GAME_OVER
    }

    private EventType type;
    private String message;

    public Event(EventType type, String message) {
        this.type = type;
        this.message = message;
    }

    public EventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
