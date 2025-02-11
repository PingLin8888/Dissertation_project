package core;

import tileengine.TETile;
import tileengine.Tileset;

public enum ObstacleType {
    SPIKES(-10, Tileset.SPIKES), // Damages player and reduces points
    TELEPORTER(0, Tileset.TELEPORTER), // Randomly teleports player
    ICE(0, Tileset.ICE), // Makes player slide until hitting wall
    DARK_MODE(0, Tileset.DARK_MODE); // Renamed from DARK_ROOM

    private final TETile tile;
    private final int pointPenalty;

    ObstacleType(int pointPenalty, TETile tile) {
        this.tile = tile;
        this.pointPenalty = pointPenalty;
    }

    public TETile getTile() {
        return tile;
    }

    public int getPointPenalty() {
        return pointPenalty;
    }
}