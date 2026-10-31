package jdk.m3.graphics;


// SkSamplingOptions.h - SkMipmapMode
public enum M3MipmapMode {
    /**
     * ignore mipmap levels, sample from the "base"
     */
    NONE,

    /**
     * sample from the nearest level
     */
    NEAREST,

    /**
     * interpolate between the two nearest levels
     */
    LINEAR;

    public static final M3MipmapMode[] _values = values();
}
