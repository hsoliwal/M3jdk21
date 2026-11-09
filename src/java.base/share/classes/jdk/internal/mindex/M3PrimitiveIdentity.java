/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package jdk.internal.mindex;

/**
 * Allocation-free typed raw-bit identities for the eight Java primitives.
 *
 * <p>Identities are represented by (kind, bits), never by a boxed reference or
 * an address. A 64-bit long/double value cannot be injectively encoded together
 * with its type in one 64-bit word. Raw NaN payloads and signed zeros are
 * preserved. Java primitive equality is deliberately a separate operation.</p>
 */
public final class M3PrimitiveIdentity {
    public static final int BOOLEAN = 1;
    public static final int BYTE = 2;
    public static final int SHORT = 3;
    public static final int CHAR = 4;
    public static final int INT = 5;
    public static final int LONG = 6;
    public static final int FLOAT = 7;
    public static final int DOUBLE = 8;

    private M3PrimitiveIdentity() {}

    public static long bits(boolean v) { return v ? 1L : 0L; }
    public static long bits(byte v) { return v & 0xffL; }
    public static long bits(short v) { return v & 0xffffL; }
    public static long bits(char v) { return v; }
    public static long bits(int v) { return Integer.toUnsignedLong(v); }
    public static long bits(long v) { return v; }
    public static long bits(float v) { return Integer.toUnsignedLong(Float.floatToRawIntBits(v)); }
    public static long bits(double v) { return Double.doubleToRawLongBits(v); }

    /** Rejects aliasing encodings and out-of-domain values, without allocation. */
    public static boolean valid(int kind, long bits) {
        return switch (kind) {
            case BOOLEAN -> bits == 0L || bits == 1L;
            case BYTE -> (bits & ~0xffL) == 0L;
            case SHORT, CHAR -> (bits & ~0xffffL) == 0L;
            case INT, FLOAT -> (bits & ~0xffff_ffffL) == 0L;
            case LONG, DOUBLE -> true;
            default -> false;
        };
    }

    /** Exact content identity. NaN payloads and +0/-0 remain different. */
    public static boolean sameRaw(int leftKind, long leftBits, int rightKind, long rightBits) {
        return leftKind == rightKind && valid(leftKind, leftBits)
                && valid(rightKind, rightBits) && leftBits == rightBits;
    }

    /** Java's float ==, not boxed Float.equals and not raw-bit identity. */
    public static boolean floatEquals(long leftBits, long rightBits) {
        if (!valid(FLOAT, leftBits) || !valid(FLOAT, rightBits)) {
            throw new IllegalArgumentException("invalid float bits");
        }
        return Float.intBitsToFloat((int) leftBits) == Float.intBitsToFloat((int) rightBits);
    }

    /** Java's double ==, not boxed Double.equals and not raw-bit identity. */
    public static boolean doubleEquals(long leftBits, long rightBits) {
        return Double.longBitsToDouble(leftBits) == Double.longBitsToDouble(rightBits);
    }

    /** Order used by the compiler image; this is NOT numeric Java comparison. */
    public static int compareRaw(int leftKind, long leftBits, int rightKind, long rightBits) {
        int kindOrder = Integer.compare(leftKind, rightKind);
        return kindOrder != 0 ? kindOrder : Long.compareUnsigned(leftBits, rightBits);
    }
}
