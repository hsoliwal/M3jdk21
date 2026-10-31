package jdk.m3.graphics;


// Sk1DPathEffect.h - SkPath1DPathEffect::Style
public enum M3PathEffect1DStyle {
    /** translate the shape to each position */
    TRANSLATE,
    /** rotate the shape about its center */
    ROTATE,
    /** transform each point, and turn lines into curves */
    MORPH;

    public static final M3PathEffect1DStyle[] _values = values();
}
