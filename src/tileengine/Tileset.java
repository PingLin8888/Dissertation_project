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
        public static final TETile AVATAR = new TETile(' ', Color.white, Color.black, "you",
                        "assets/images/character_femaleAdventurer_walk0.png");
        public static final TETile CHASER = new TETile(' ', Color.white, Color.black, "Monster",
                        "assets/images/character_zombie_idle.png");
        public static final TETile WALL0 = new TETile('#', new Color(216, 128, 128), Color.darkGray,
                        "wall");
        public static final TETile WALL = new TETile('#', new Color(216, 128, 128), Color.darkGray,
                        "wall", "assets/images/platformPack_tile014.png");
        public static final TETile FLOOR = new TETile('·', new Color(128, 192, 128), Color.black,
                        "floor");
        public static final TETile NOTHING = new TETile(' ', Color.black, Color.black, "nothing");
        public static final TETile GRASS0 = new TETile('"', Color.green, Color.black, "grass");
        // public static final TETile WATER = new TETile('≈', Color.blue, Color.black,
        // "water");
        public static final TETile FLOWER = new TETile('❀', Color.magenta, Color.pink, "flower");
        public static final TETile LOCKED_DOOR0 = new TETile('█', Color.orange, Color.black,
                        "locked door");
        public static final TETile LOCKED_DOOR = new TETile('█', Color.orange, Color.black,
                        "locked door", "assets/images/platformPack_tile050.png");
        public static final TETile UNLOCKED_DOOR = new TETile('▢', Color.orange, Color.black,
                        "unlocked door");
        public static final TETile SAND = new TETile('▒', Color.yellow, Color.black, "sand");
        public static final TETile MOUNTAIN = new TETile('▲', Color.gray, Color.black, "mountain");
        public static final TETile TREE = new TETile('♠', Color.green, Color.black, "tree");
        public static final String APPLE_SYMBOL = "\uD83C\uDF4E"; // Apple emoji as a String
        public static final TETile APPLE = new TETile('a', Color.red, Color.black, "apple", "assets/images/apple.png");
        public static final TETile BANANA = new TETile('b', Color.yellow, Color.black, "banana",
                        "assets/images/banana.png");
        public static final TETile SMILEY_FACE_green_body_rhombus = new TETile('·', new Color(128, 192, 128),
                        Color.black,
                        "yum", "assets/images/SMILEY_FACE_green_body_rhombus.png");
        public static final TETile SMILEY_FACE_green_body_circle = new TETile('·', new Color(128, 192, 128),
                        Color.black,
                        "yum", "assets/images/smiley_green_body_circle.png");
        public static final TETile GRASS = new TETile(' ', Color.green, Color.black, "grass",
                        "assets/images/platformPack_tile045.png");
        public static final TETile WATER = new TETile(' ', Color.green, Color.black, "grass",
                        "assets/images/platformPack_tile005.png");
        public static final TETile SPIKES = new TETile(' ', Color.green, Color.black, "spikes",
                        "assets/images/explosion01.png");
        public static final TETile MUD = new TETile(' ', Color.green, Color.black, "mud",
                        "assets/images/bushOrange4.png");
        public static final TETile TELEPORTER = new TETile(' ', Color.green, Color.black, "PORTAL",
                        "assets/images/houseAlt2.png");
        public static final TETile ICE = new TETile(' ', Color.green, Color.black, "ICE",
                        "assets/images/treeLongFrozen.png" +
                                        "");

        public static final TETile DARK_MODE = new TETile('▒', Color.blue, Color.black,
                        "dark mode", "assets/images/darkPortal.png");

        public static final TETile TORCH = new TETile('*', Color.yellow, Color.orange,
                        "torch", "assets/images/torch.png");

        public static final TETile PATH = new TETile('·', Color.red, Color.black,
                        "danger path", "assets/images/path.png");
}
