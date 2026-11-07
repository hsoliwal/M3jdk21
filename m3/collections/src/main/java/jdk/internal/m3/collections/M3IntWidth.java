/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

/**
 * Byte-aligned integer widths used by compact primitive storage.
 *
 * <p>Unsigned 64-bit values use the raw bits of a {@code long}; a negative Java value is
 * therefore valid for unsigned {@link #INT64}, but not for a narrower unsigned width.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/IntWidth.java}
 * blob {@code 150e2580b487f97d5ffb6e9d30054e5c35500e7e} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum M3IntWidth {
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

    M3IntWidth(int bits) {
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
    public static M3IntWidth smallest(long value, boolean signed) {
        if (INT32.fits(value, signed)) return INT32;
        if (INT40.fits(value, signed)) return INT40;
        if (INT48.fits(value, signed)) return INT48;
        if (INT56.fits(value, signed)) return INT56;
        return INT64;
    }
}
