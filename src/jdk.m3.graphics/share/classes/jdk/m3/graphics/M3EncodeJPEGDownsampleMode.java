package jdk.m3.graphics;


public enum M3EncodeJPEGDownsampleMode {
    /**
     * Reduction by a factor of two in both the horizontal and vertical directions.
     */
    DS_420,

    /**
     * Reduction by a factor of two in the horizontal direction.
     */
    DS_422,

    /**
     * No downsampling.
     */
    DS_444;

    public static final M3EncodeJPEGDownsampleMode[] _values = values();
}
