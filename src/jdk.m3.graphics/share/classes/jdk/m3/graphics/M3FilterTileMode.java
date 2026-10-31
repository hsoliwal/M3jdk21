package jdk.m3.graphics;


public enum M3FilterTileMode {
    /** Replicate the edge color if the shader draws outside of its original bounds. */
    CLAMP,

    /** Repeat the shader's image horizontally and vertically. */
    REPEAT,

    /** Repeat the shader's image horizontally and vertically, alternating mirror images so that adjacent images always seam. */
    MIRROR,

    /** Only draw within the original domain, return transparent-black everywhere else. */
    DECAL;

    public static final M3FilterTileMode[] _values = values();
}