/* *****************************************************************************
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

import java.util.Objects;

/**
 * Fixed-size array of signed or unsigned integers with widths from 65 to 128 bits.
 *
 * <p>Each value occupies exactly {@code (bitsPerValue + 7) / 8} consecutive
 * little-endian bytes. Unused bits in the last byte are zero. Arithmetic uses
 * primitive lanes; no per-entry object or runtime big integer is retained.
 *
 * <p>The high lane is sign-extended for signed values and zero-extended for
 * unsigned values. The low lane always contains an uninterpreted 64-bit pattern.
 * At width 128 every high-lane bit pattern is valid, including negative Java
 * {@code long} values in unsigned mode.
 *
 * <p>This class is mutable and not thread-safe. Exact-operation failure is
 * atomic with respect to payload changes, not concurrent access.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/WideIntegerArray.java}
 * blob {@code cf6e48d1a8c60018c4cb8f49f94c7c4ac03cf836} at com.synexia develop {@code ded4a4acdffe5a27e1f2cedb02126762816b9850} by
 * RELOCATE_M3_OWNER v2 (identifiers only; bytecode equal to the source modulo names).
 */
public final class WideIntegerArray {
    private static final long MASK32 = 0xffff_ffffL;
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private final int bitsPerValue;
    private final int size;
    private final boolean signed;
    private final int bytesPerValue;
    private final int highBits;
    private final long highMask;
    private final byte[] values;

    /**
     * Creates a zero-filled array.
     *
     * @param bitsPerValue integer width, from 65 through 128
     * @param size number of entries, which must be nonnegative
     * @param signed whether values use signed two's-complement interpretation
     * @throws IllegalArgumentException for invalid width, negative size, or a
     *         payload larger than {@code Integer.MAX_VALUE - 8} bytes
     */
    public WideIntegerArray(int bitsPerValue, int size, boolean signed) {
        if (bitsPerValue < 65 || bitsPerValue > 128) {
            throw new IllegalArgumentException("bitsPerValue must be in [65, 128]");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must be nonnegative");
        }
        final int byteWidth = (bitsPerValue + 7) / Byte.SIZE;
        final long payloadLength = Math.multiplyExact((long) size, byteWidth);
        if (payloadLength > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("packed payload exceeds array limit");
        }
        this.bitsPerValue = bitsPerValue;
        this.size = size;
        this.signed = signed;
        this.bytesPerValue = byteWidth;
        this.highBits = bitsPerValue - Long.SIZE;
        this.highMask = -1L >>> (Long.SIZE - highBits);
        this.values = new byte[(int) payloadLength];
    }

    /** Returns the logical integer width. */
    public int bitsPerValue() {
        return bitsPerValue;
    }

    /** Returns the fixed number of entries. */
    public int size() {
        return size;
    }

    /** Returns whether this array uses signed interpretation. */
    public boolean signed() {
        return signed;
    }

    /** Returns the exact retained payload length, excluding object metadata. */
    public long payloadBytes() {
        return values.length;
    }

    /**
     * Returns the canonical high lane of an entry.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public long getHigh(int index) {
        return canonicalHigh(readHigh(checkedOffset(index)));
    }

    /**
     * Returns the raw low 64-bit lane of an entry.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public long getLow(int index) {
        return readLane(checkedOffset(index), Long.BYTES);
    }

    /**
     * Stores one entry after validating the index and high lane.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the high lane is not canonical
     */
    public void set(int index, long high, long low) {
        final int offset = checkedOffset(index);
        checkHigh(high);
        writeEntry(offset, high, low);
    }

    /**
     * Adds a canonical operand, refusing overflow before changing the payload.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     * @throws ArithmeticException if the result is outside the configured range
     */
    public void addExact(int index, long high, long low) {
        add(index, high, low, true, false);
    }

    /**
     * Adds a canonical operand modulo {@code 2^bitsPerValue}.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     */
    public void addWrapping(int index, long high, long low) {
        add(index, high, low, false, false);
    }

    /**
     * Adds a canonical operand, clamping an overflowing result to the configured
     * minimum or maximum. Unsigned overflow clamps to the unsigned maximum.
     * Exact and wrapping operations keep their independent contracts.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     */
    public void addSaturating(int index, long high, long low) {
        add(index, high, low, false, true);
    }

    /**
     * Subtracts a canonical operand, refusing overflow before changing the payload.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     * @throws ArithmeticException if the result is outside the configured range
     */
    public void subtractExact(int index, long high, long low) {
        subtract(index, high, low, true, false);
    }

    /**
     * Subtracts a canonical operand modulo {@code 2^bitsPerValue}.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     */
    public void subtractWrapping(int index, long high, long low) {
        subtract(index, high, low, false, false);
    }

    /**
     * Subtracts a canonical operand, clamping an overflowing result to the
     * configured minimum or maximum. Unsigned underflow clamps to zero.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     */
    public void subtractSaturating(int index, long high, long low) {
        subtract(index, high, low, false, true);
    }

    /**
     * Multiplies by a canonical operand, refusing overflow before changing any byte.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     * @throws ArithmeticException if the mathematical product is outside the configured range
     */
    public void multiplyExact(int index, long high, long low) {
        multiply(index, high, low, true, false);
    }

    /**
     * Multiplies by a canonical operand modulo {@code 2^bitsPerValue}.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     */
    public void multiplyWrapping(int index, long high, long low) {
        multiply(index, high, low, false, false);
    }

    /**
     * Multiplies by a canonical operand, clamping the mathematical product to the
     * configured minimum or maximum. Unsigned overflow clamps to the maximum.
     *
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IllegalArgumentException if the operand high lane is not canonical
     */
    public void multiplySaturating(int index, long high, long low) {
        multiply(index, high, low, false, true);
    }

    /**
     * Compares entries with identical width and signedness.
     *
     * @return a negative, zero, or positive value according to numeric order
     * @throws NullPointerException if other is null
     * @throws IllegalArgumentException if widths or signedness differ
     * @throws IndexOutOfBoundsException if either index is invalid
     */
    public int compare(int index, WideIntegerArray other, int otherIndex) {
        Objects.requireNonNull(other, "other");
        if (bitsPerValue != other.bitsPerValue || signed != other.signed) {
            throw new IllegalArgumentException("width and signedness must match");
        }
        final int offset = checkedOffset(index);
        final int otherOffset = other.checkedOffset(otherIndex);
        final long leftHigh = canonicalHigh(readHigh(offset));
        final long rightHigh = other.canonicalHigh(other.readHigh(otherOffset));
        final int highComparison = signed
                ? Long.compare(leftHigh, rightHigh)
                : Long.compareUnsigned(leftHigh, rightHigh);
        return highComparison != 0 ? highComparison : Long.compareUnsigned(
                readLane(offset, Long.BYTES), other.readLane(otherOffset, Long.BYTES));
    }

    /** Returns a fresh canonical little-endian copy of the complete payload. */
    public byte[] toByteArray() {
        return values.clone();
    }

    private void add(int index, long high, long low, boolean exact, boolean saturating) {
        final int offset = checkedOffset(index);
        checkHigh(high);
        final long previousLow = readLane(offset, Long.BYTES);
        final long previousHigh = readHigh(offset);

        // Apache UInt128.add: accumulate zero-extended 32-bit limbs with carry.
        final long lower = (previousLow & MASK32) + (low & MASK32);
        final long upper = (previousLow >>> Integer.SIZE)
                + (low >>> Integer.SIZE) + (lower >>> Integer.SIZE);
        final long resultLow = (upper << Integer.SIZE) | (lower & MASK32);
        final long resultHigh = (previousHigh + (high & highMask)
                + (upper >>> Integer.SIZE)) & highMask;

        if ((exact || saturating)
                && additionOverflows(previousHigh, previousLow, high, resultHigh, resultLow)) {
            if (exact) throw new ArithmeticException("wide integer addition overflow");
            writeSaturated(offset, signed && canonicalHigh(previousHigh) < 0);
            return;
        }
        writeEntry(offset, resultHigh, resultLow);
    }

    private boolean additionOverflows(
            long previousHigh, long previousLow, long operandHigh, long resultHigh, long resultLow) {
        if (signed) {
            final long before = canonicalHigh(previousHigh);
            final long after = canonicalHigh(resultHigh);
            return ((before ^ after) & (operandHigh ^ after)) < 0;
        }
        return compareUnsigned(resultHigh, resultLow, previousHigh, previousLow) < 0;
    }

    private void subtract(int index, long high, long low, boolean exact, boolean saturating) {
        final int offset = checkedOffset(index);
        checkHigh(high);
        final long previousLow = readLane(offset, Long.BYTES);
        final long previousHigh = readHigh(offset);

        // Apache UInt128.subtract: the arithmetic shift carries a zero or -1 borrow.
        final long lower = (previousLow & MASK32) - (low & MASK32);
        final long upper = (previousLow >>> Integer.SIZE)
                - (low >>> Integer.SIZE) + (lower >> Integer.SIZE);
        final long resultLow = (upper << Integer.SIZE) | (lower & MASK32);
        final long resultHigh = (previousHigh - (high & highMask)
                + (upper >> Integer.SIZE)) & highMask;

        if ((exact || saturating)
                && subtractionOverflows(previousHigh, previousLow, high, low, resultHigh)) {
            if (exact) throw new ArithmeticException("wide integer subtraction overflow");
            writeSaturated(offset, !signed || canonicalHigh(previousHigh) < 0);
            return;
        }
        writeEntry(offset, resultHigh, resultLow);
    }

    /** Writes a configured endpoint while keeping unused high payload bits zero. */
    private void writeSaturated(int offset, boolean minimum) {
        final long high = minimum
                ? (signed ? 1L << (highBits - 1) : 0L)
                : (signed ? highMask >>> 1 : highMask);
        writeEntry(offset, high, minimum ? 0L : -1L);
    }

    private boolean subtractionOverflows(
            long previousHigh, long previousLow, long operandHigh, long operandLow, long resultHigh) {
        if (signed) {
            final long before = canonicalHigh(previousHigh);
            final long after = canonicalHigh(resultHigh);
            return ((before ^ operandHigh) & (before ^ after)) < 0;
        }
        return compareUnsigned(previousHigh, previousLow, operandHigh, operandLow) < 0;
    }

    private void multiply(int index, long high, long low, boolean exact, boolean saturating) {
        final int offset = checkedOffset(index);
        checkHigh(high);
        long leftHigh = canonicalHigh(readHigh(offset));
        long leftLow = readLane(offset, Long.BYTES);
        long rightHigh = high;
        long rightLow = low;
        final boolean leftNegative = signed && leftHigh < 0;
        final boolean rightNegative = signed && rightHigh < 0;
        final boolean negative = leftNegative != rightNegative;

        // Magnitudes use unsigned limbs, including the magnitude of signed minimum.
        if (leftNegative) {
            leftLow = -leftLow;
            leftHigh = ~leftHigh + (leftLow == 0 ? 1L : 0L);
        }
        if (rightNegative) {
            rightLow = -rightLow;
            rightHigh = ~rightHigh + (rightLow == 0 ? 1L : 0L);
        }

        long resultLow = leftLow * rightLow;
        final long lowCarry = Math.unsignedMultiplyHigh(leftLow, rightLow);
        final long firstHigh = lowCarry + leftHigh * rightLow;
        long resultHigh = firstHigh + leftLow * rightHigh;

        if (exact || saturating) {
            // Every omitted term is nonnegative. Any high-high product, high
            // cross limb or carry past resultHigh proves a product above 128 bits.
            final boolean beyond128 = (leftHigh != 0 && rightHigh != 0)
                    || Math.unsignedMultiplyHigh(leftHigh, rightLow) != 0
                    || Math.unsignedMultiplyHigh(leftLow, rightHigh) != 0
                    || Long.compareUnsigned(firstHigh, lowCarry) < 0
                    || Long.compareUnsigned(resultHigh, firstHigh) < 0;
            final long limitHigh = signed
                    ? (negative ? 1L << (highBits - 1) : highMask >>> 1)
                    : highMask;
            final long limitLow = signed && negative ? 0L : -1L;
            if (beyond128 || compareUnsigned(resultHigh, resultLow, limitHigh, limitLow) > 0) {
                if (exact) throw new ArithmeticException("wide integer multiplication overflow");
                writeSaturated(offset, signed && negative);
                return;
            }
        }

        if (negative) {
            resultHigh = ~resultHigh + (resultLow == 0 ? 1L : 0L);
            resultLow = -resultLow;
        }
        writeEntry(offset, resultHigh, resultLow);
    }

    private static int compareUnsigned(long leftHigh, long leftLow, long rightHigh, long rightLow) {
        final int highComparison = Long.compareUnsigned(leftHigh, rightHigh);
        return highComparison != 0 ? highComparison : Long.compareUnsigned(leftLow, rightLow);
    }

    private int checkedOffset(int index) {
        return Objects.checkIndex(index, size) * bytesPerValue;
    }

    private void checkHigh(long high) {
        if (canonicalHigh(high) != high) {
            throw new IllegalArgumentException("high lane is not canonical for this width");
        }
    }

    private long canonicalHigh(long high) {
        final int shift = Long.SIZE - highBits;
        return signed ? (high << shift) >> shift : high & highMask;
    }

    private long readHigh(int offset) {
        return readLane(offset + Long.BYTES, bytesPerValue - Long.BYTES);
    }

    private long readLane(int offset, int byteCount) {
        long result = 0;
        for (int i = 0; i < byteCount; i++) {
            result |= (values[offset + i] & 0xffL) << (i * Byte.SIZE);
        }
        return result;
    }

    private void writeEntry(int offset, long high, long low) {
        writeLane(offset, Long.BYTES, low);
        writeLane(offset + Long.BYTES, bytesPerValue - Long.BYTES, high & highMask);
    }

    private void writeLane(int offset, int byteCount, long value) {
        for (int i = 0; i < byteCount; i++) {
            values[offset + i] = (byte) (value >>> (i * Byte.SIZE));
        }
    }
}
