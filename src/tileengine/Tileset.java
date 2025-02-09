package tileengine;

import java.awt.Color;

/**
 * Contains constant tile objects, to avoid having to remake the same tiles in
 * different parts of
 * the code.
 *
 * You are free to (and encouraged to) create and add your own tiles to this
 * file. This file will
 * be turned in with the rest of your code.
 *
 * Ex:
 * world[x][y] = Tileset.FLOOR;
 *
 * The style checker may crash when you try to style check this file due to use
 * of unicode
 * characters. This is OK.
 */

public class Tileset {
        // World tiles
        public static final TETile WALL = new TETile('#', new Color(216, 128, 128), Color.darkGray,
                        "wall", "assets/images/wall.png");
        public static final TETile FLOOR = new TETile('·', new Color(128, 192, 128), Color.black,
                        "floor", "assets/images/grass.png");
        public static final TETile NOTHING = new TETile(' ', Color.black, Color.black, "nothing");

        // Special tiles
        public static final TETile LOCKED_DOOR = new TETile('█', Color.orange, Color.black,
                        "locked door", "assets/images/locked_door.png");
        public static final TETile UNLOCKED_DOOR = new TETile('▢', Color.orange, Color.black,
                        "unlocked door", "assets/images/unlocked_door.png");
        public static final TETile CHASER = new TETile(' ', Color.white, Color.black, "Monster",
                        "assets/images/chaser.png");

        // Effect tiles
        public static final TETile INVISIBLE = new TETile(' ', Color.white, Color.black, "invisible",
                        "assets/images/invisible.png");
        public static final TETile PATH = new TETile('·', Color.red, Color.black,
                        "danger path", "assets/images/path.png");

        // Obstacles
        public static final TETile SPIKES = new TETile('▲', Color.red, Color.black, "spikes",
                        "assets/images/spikes.png");
        public static final TETile ICE = new TETile('❄', Color.cyan, Color.black, "ice",
                        "assets/images/ice.png");
        public static final TETile TELEPORTER = new TETile('○', Color.magenta, Color.black, "teleporter",
                        "assets/images/teleporter.png");
        public static final TETile DARK_MODE = new TETile(' ', Color.cyan, Color.black, "dark mode",
                "assets/images/DARK_MODE.png");
        // Items
        public static final TETile TORCH = new TETile('*', Color.yellow, Color.orange,
                        "torch", "assets/images/torch.png");
        public static final TETile SMILEY_FACE_green_body_circle = new TETile('*', Color.yellow, Color.orange,
                "torch", "assets/images/smiley_green_body_circle.png");
        public static final TETile SMILEY_FACE_green_body_rhombus = new TETile('*', Color.yellow, Color.orange,
                "torch", "assets/images/SMILEY_FACE_green_body_rhombus.png");


}
