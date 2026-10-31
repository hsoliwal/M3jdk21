package jdk.m3.graphics;


// SkPaint.h - SkPaint::Cap
/**
 * Cap draws at the beginning and end of an open path contour.
 */
public enum M3PaintStrokeCap {
    /**
     * No stroke extension
     */
    BUTT,

    /**
     * adds circle
     */
    ROUND,

    /**
     * adds square
     */
    SQUARE;

    public static final M3PaintStrokeCap[] _values = values();
}
