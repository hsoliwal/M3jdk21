package jdk.m3.graphics;


// SkPathTypes.h - SkPathDirection
public enum M3PathDirection {
    /** Clockwise direction for adding closed contours. */
    CLOCKWISE,

    /** Counter-clockwise direction for adding closed contours. */
    COUNTER_CLOCKWISE;

    public static final M3PathDirection[] _values = values();
}
