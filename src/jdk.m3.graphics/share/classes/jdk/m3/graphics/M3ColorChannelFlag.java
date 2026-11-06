package jdk.m3.graphics;


/**
 * Used to represent the channels available in a color type or texture format as a mask.
 */
public enum M3ColorChannelFlag {
    RED(1), GREEN(2), BLUE(4), ALPHA(8), GRAY(16),
    // Convenience values
    GRAY_ALPHA(16 | 8), RG(1 | 2), RGB(1 | 2 | 4), RGBA(1 | 2 | 4 | 8);
    public final int _value;

    M3ColorChannelFlag(int value) {
        this._value = value;
    }

    public static final M3ColorChannelFlag[] _values = values();

    @java.lang.SuppressWarnings("all")
    public int getValue() {
        return this._value;
    }
}
