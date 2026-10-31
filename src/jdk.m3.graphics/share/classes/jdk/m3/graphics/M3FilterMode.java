package jdk.m3.graphics;


public enum M3FilterMode {
    /**
     * single sample point (nearest neighbor)
     */
    NEAREST,

    /**
     * interporate between 2x2 sample points (bilinear interpolation)
     */
    LINEAR;

    public static final M3FilterMode[] _values = values();
}
