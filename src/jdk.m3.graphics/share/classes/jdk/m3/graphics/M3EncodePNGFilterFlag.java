package jdk.m3.graphics;


public enum M3EncodePNGFilterFlag {
    ZERO(0), NONE(8), SUB(16), UP(32), AVG(64), PAETH(128), ALL(8 | 16 | 32 | 64 | 128);
    public final int _value;

    M3EncodePNGFilterFlag(int value) {
        this._value = value;
    }

    public static final M3EncodePNGFilterFlag[] _values = values();

    @java.lang.SuppressWarnings("all")
    public int getValue() {
        return this._value;
    }
}
