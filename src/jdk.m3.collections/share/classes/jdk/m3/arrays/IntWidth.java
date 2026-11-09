// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

/**
 * Byte-aligned integer widths used by compact primitive storage.
 *
 * <p>Unsigned 64-bit values use the raw bits of a {@code long}; a negative Java value is
 * therefore valid for unsigned {@link #INT64}, but not for a narrower unsigned width.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/165ceb5a020291e99ebeb89783b88e8a9686ae5c/synexia-common/src/main/java/com/synexia/common/collections/IntWidth.java blob 150e2580b487; recipe m3/arrays/recipe/lint-storage-20261107/relocate.py v3
public enum IntWidth {
    INT32(32),
    INT40(40),
    INT48(48),
    INT56(56),
    INT64(64);

    private final int bits;
    private final int bytes;
    private final long mask;
    private final long signedMinimum;
    private final long signedMaximum;

    IntWidth(int bits) {
        this.bits = bits;
        this.bytes = bits / Byte.SIZE;
        this.mask = bits == Long.SIZE ? -1L : (1L << bits) - 1L;
        this.signedMinimum = -1L << (bits - 1);
        this.signedMaximum = ~signedMinimum;
    }

    public int bits() {
        return bits;
    }

    public int bytes() {
        return bytes;
    }

    /** Returns the low-width bit mask; all bits are set for {@link #INT64}. */
    public long mask() {
        return mask;
    }

    public long minValue(boolean signed) {
        return signed ? signedMinimum : 0L;
    }

    /** The unsigned {@link #INT64} maximum is represented by the raw bits {@code -1L}. */
    public long maxValue(boolean signed) {
        return signed ? signedMaximum : mask;
    }

    public boolean fits(long value, boolean signed) {
        if (this == INT64) {
            return true;
        }
        return signed
                ? value >= signedMinimum && value <= signedMaximum
                : value >= 0L && value <= mask;
    }

    /**
     * Returns {@code value} unchanged when representable.
     *
     * @throws ArithmeticException if the value does not fit the selected interpretation
     */
    public long require(long value, boolean signed) {
        if (!fits(value, signed)) {
            throw new ArithmeticException(value + " does not fit "
                    + (signed ? "signed " : "unsigned ") + bits + " bits");
        }
        return value;
    }

    /** Returns the smallest supported width that represents the value without loss. */
    public static IntWidth smallest(long value, boolean signed) {
        if (INT32.fits(value, signed)) return INT32;
        if (INT40.fits(value, signed)) return INT40;
        if (INT48.fits(value, signed)) return INT48;
        if (INT56.fits(value, signed)) return INT56;
        return INT64;
    }
}
