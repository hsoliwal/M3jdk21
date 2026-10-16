/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Target-owned M3JDK implementation. See the primitive identity receiver receipt.
 */
package jdk.internal.mindex;

/**
 * Typed, immediate primitive identity. A key alone has no universal type tag.
 * All 64 long/double bits remain available; callers carry the kind in their schema.
 * No heap/native cell is allocated by the encoders. No Java reference identity changes.
 */
public final class M3PrimitiveIdentity {
    public static final int SEMANTICS_VERSION = 1;
    public static final int BOOLEAN = 0, BYTE = 1, SHORT = 2, CHAR = 3;
    public static final int INT = 4, LONG = 5, FLOAT = 6, DOUBLE = 7;
    private M3PrimitiveIdentity() { }

    public static long ofBoolean(boolean x) { return x ? 1L : 0L; }
    public static long ofByte(byte x) { return x; }
    public static long ofShort(short x) { return x; }
    public static long ofChar(char x) { return x; }
    public static long ofInt(int x) { return x; }
    public static long ofLong(long x) { return x; }
    public static long ofFloat(float x) { return Integer.toUnsignedLong(Float.floatToRawIntBits(x)); }
    public static long ofDouble(double x) { return Double.doubleToRawLongBits(x); }

    /** Bit ingress avoids converting a signaling NaN through a floating-point register. */
    public static long ofFloatBits(int bits) { return Integer.toUnsignedLong(bits); }
    public static long ofDoubleBits(long bits) { return bits; }

    public static boolean sameBoolean(boolean a, boolean b) { return a == b; }
    public static boolean sameByte(byte a, byte b) { return a == b; }
    public static boolean sameShort(short a, short b) { return a == b; }
    public static boolean sameChar(char a, char b) { return a == b; }
    public static boolean sameInt(int a, int b) { return a == b; }
    public static boolean sameLong(long a, long b) { return a == b; }
    public static boolean sameFloat(float a, float b) { return a == b; }
    public static boolean sameDouble(double a, double b) { return a == b; }

    public static void checkKey(int kind, long key) {
        boolean valid = switch (kind) {
            case BOOLEAN -> key == 0L || key == 1L;
            case BYTE -> key == (byte) key;
            case SHORT -> key == (short) key;
            case CHAR -> key == (char) key;
            case INT -> key == (int) key;
            case LONG, DOUBLE -> true;
            case FLOAT -> (key >>> 32) == 0L;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("invalid typed primitive key");
    }

    public static boolean rawEquals(int aKind, long a, int bKind, long b) {
        checkKey(aKind, a); checkKey(bKind, b);
        return aKind == bKind && a == b;
    }

    /** Boxed value equivalence, NOT wrapper reference identity. NaNs canonicalize; zeros do not. */
    public static long boxedKey(int kind, long raw) {
        checkKey(kind, raw);
        if (kind == FLOAT && (raw & 0x7fffffffL) > 0x7f800000L) return 0x7fc00000L;
        if (kind == DOUBLE && (raw & Long.MAX_VALUE) > 0x7ff0000000000000L)
            return 0x7ff8000000000000L;
        return raw;
    }

    public static boolean boxedEquals(int aKind, long a, int bKind, long b) {
        long left = boxedKey(aKind, a), right = boxedKey(bKind, b);
        return aKind == bKind && left == right;
    }

    /** Java binary numeric promotion followed by ==; boolean/numeric mixing is refused. */
    public static boolean numericEquals(int aKind, long a, int bKind, long b) {
        checkKey(aKind, a); checkKey(bKind, b);
        if (aKind == BOOLEAN || bKind == BOOLEAN) {
            if (aKind != bKind) throw new IllegalArgumentException("boolean/numeric comparison");
            return a == b;
        }
        if (aKind == DOUBLE || bKind == DOUBLE) return asDouble(aKind, a) == asDouble(bKind, b);
        if (aKind == FLOAT || bKind == FLOAT) return asFloat(aKind, a) == asFloat(bKind, b);
        // Integral keys are exact signed values, except char's deliberately unsigned value.
        return a == b;
    }

    private static double asDouble(int kind, long key) {
        return switch (kind) {
            case DOUBLE -> Double.longBitsToDouble(key);
            case FLOAT -> (double) Float.intBitsToFloat((int) key);
            default -> (double) key;
        };
    }

    private static float asFloat(int kind, long key) {
        return kind == FLOAT ? Float.intBitsToFloat((int) key) : (float) key;
    }
}
