package tileengine;

import java.awt.Color;

public class AvatarTileset {
    // Small tiles for world rendering (16x16)
    public static final TETile FEMALE_FRONT = new TETile(' ', Color.white, Color.black, "Female Avatar",
            "assets/images/female_front_small.png");
    public static final TETile FEMALE_BACK = new TETile(' ', Color.white, Color.black, "Female Avatar",
            "assets/images/female_back_small.png");
    public static final TETile FEMALE_LEFT = new TETile(' ', Color.white, Color.black, "Female Avatar",
            "assets/images/female_left_small.png");
    public static final TETile FEMALE_RIGHT = new TETile(' ', Color.white, Color.black, "Female Avatar",
            "assets/images/female_right_small.png");

    public static final TETile MALE_FRONT = new TETile(' ', Color.white, Color.black, "Male Avatar",
            "assets/images/male_front.png");
    public static final TETile MALE_BACK = new TETile(' ', Color.white, Color.black, "Male Avatar",
            "assets/images/male_back.png");
    public static final TETile MALE_LEFT = new TETile(' ', Color.white, Color.black, "Male Avatar",
            "assets/images/male_left.png");
    public static final TETile MALE_RIGHT = new TETile(' ', Color.white, Color.black, "Male Avatar",
            "assets/images/male_right.png");

    // Large preview images for menu (64x64 or larger)
    public static final String FEMALE_PREVIEW = "assets/images/female_preview.png";
    public static final String MALE_PREVIEW = "assets/images/male_preview.png";

    // Directional sets for each avatar type
    public static final TETile[][] DIRECTIONAL_SETS = {
            { FEMALE_FRONT, FEMALE_BACK, FEMALE_LEFT, FEMALE_RIGHT },
            { MALE_FRONT, MALE_BACK, MALE_LEFT, MALE_RIGHT }
    };

    // Avatar options for selection menu
    public static final AvatarOption[] AVATAR_OPTIONS = {
            new AvatarOption("Female Adventurer", FEMALE_PREVIEW, 0),
            new AvatarOption("Male Adventurer", MALE_PREVIEW, 1)
    };
}

