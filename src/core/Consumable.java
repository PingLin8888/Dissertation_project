package core;

import tileengine.TETile;

public class Consumable {
    private String name;
    private int pointValue;
    private TETile tile; // Visual representation of the consumable

    public Consumable(String name, int pointValue, TETile tile) {
        this.name = name;
        this.pointValue = pointValue;
        this.tile = tile;
    }

    public String getName() {
        return name;
    }

    public int getPointValue() {
        return pointValue;
    }

    public TETile getTile() {
        return tile;
    }
}
