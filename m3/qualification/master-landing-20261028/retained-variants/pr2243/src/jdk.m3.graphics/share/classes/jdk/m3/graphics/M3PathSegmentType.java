package jdk.m3.graphics;


// SkPathTypes.h - SkPathSegmentMask
public enum M3PathSegmentType {
    LINE(1 << 0),
    QUAD(1 << 1),
    CONIC(1 << 2),
    CUBIC(1 << 3);

    public static final M3PathSegmentType[] _values = values();

    public final int _value;

    M3PathSegmentType(int value) {
        this._value = value;
    }
}
