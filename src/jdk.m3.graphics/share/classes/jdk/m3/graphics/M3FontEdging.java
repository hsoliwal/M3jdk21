package jdk.m3.graphics;


/**
 * Whether edge pixels draw opaque or with partial transparency.
 */
public enum M3FontEdging {
    /**
     * no transparent pixels on glyph edges
     */
    ALIAS,

    /**
     * may have transparent pixels on glyph edges
     */
    ANTI_ALIAS,

    /**
     * glyph positioned in pixel using transparency
     */
    SUBPIXEL_ANTI_ALIAS;

    public static final M3FontEdging[] _values = values();
}