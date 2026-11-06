package jdk.m3.graphics;


/**
 * Level of glyph outline adjustment
 */
public enum M3FontHinting {
    /**
     * glyph outlines unchanged
     */
    NONE,

    /**
     * minimal modification to improve constrast
     */
    SLIGHT,

    /**
     * glyph outlines modified to improve constrast
     */
    NORMAL,

    /**
     * modifies glyph outlines for maximum constrast
     */
    FULL;

    public static final M3FontHinting[] _values = values();
}