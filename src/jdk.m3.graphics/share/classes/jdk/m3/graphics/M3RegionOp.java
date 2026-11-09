package jdk.m3.graphics;


// SkRegion.h - SkRegion::Op
public enum M3RegionOp {
    DIFFERENCE,
    INTERSECT,
    UNION,
    XOR,
    REVERSE_DIFFERENCE,
    REPLACE;

    public static final M3RegionOp[] _values = values();
}
