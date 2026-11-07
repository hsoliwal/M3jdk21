/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Independent mathematical and byte-layout oracles; BigInteger is test-only. */
final class M3WideIntegerArrayTest {
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger MASK64 = ONE.shiftLeft(64).subtract(ONE);
    private static final long SEED = 0x65_80_96_128L;
    private static final int RANDOM_PAIRS_PER_FORMAT = 160;

    static Stream<Arguments> formats() {
        return IntStream.rangeClosed(65, 128).boxed()
                .flatMap(width -> Stream.of(Arguments.of(width, false), Arguments.of(width, true)));
    }

    @Test
    void constructorValidatesBeforeAllocationAndEmptyArraysStayEmpty() {
        for (int width : new int[] {Integer.MIN_VALUE, -1, 0, 1, 64, 129, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new M3WideIntegerArray(width, 0, false));
        }
        assertThrows(IllegalArgumentException.class, () -> new M3WideIntegerArray(65, -1, false));
        for (int width : new int[] {65, 80, 96, 128}) {
            final int byteWidth = (width + 7) / Byte.SIZE;
            final int overLimit = (Integer.MAX_VALUE - 8) / byteWidth + 1;
            assertThrows(IllegalArgumentException.class,
                    () -> new M3WideIntegerArray(width, overLimit, false));
            assertThrows(IllegalArgumentException.class,
                    () -> new M3WideIntegerArray(width, Integer.MAX_VALUE, true));
            final M3WideIntegerArray empty = new M3WideIntegerArray(width, 0, true);
            assertEquals(width, empty.bitsPerValue());
            assertEquals(0, empty.size());
            assertTrue(empty.signed());
            assertEquals(0L, empty.payloadBytes());
            assertArrayEquals(new byte[0], empty.toByteArray());
            assertNotSame(empty.toByteArray(), empty.toByteArray());
            assertThrows(IndexOutOfBoundsException.class, () -> empty.getHigh(0));
            assertThrows(IndexOutOfBoundsException.class, () -> empty.set(0, 0, 0));
        }
    }

    @ParameterizedTest(name = "width={0}, signed={1}")
    @MethodSource("formats")
    void everyValidBitAndEveryEntryHaveIndependentCanonicalStorage(int width, boolean signed) {
        final M3WideIntegerArray values = new M3WideIntegerArray(width, 3, signed);
        final int byteWidth = (width + 7) / Byte.SIZE;
        assertEquals(width, values.bitsPerValue());
        assertEquals(signed, values.signed());
        assertEquals(3, values.size());
        assertEquals((long) byteWidth * 3, values.payloadBytes());
        assertArrayEquals(new byte[byteWidth * 3], values.toByteArray());

        put(values, 0, maximum(width, signed));
        put(values, 2, minimum(width, signed));
        for (int bit = 0; bit < width; bit++) {
            final BigInteger value = wrap(ONE.shiftLeft(bit), width, signed);
            put(values, 1, value);
            assertStored(values, 1, value);
            assertStored(values, 0, maximum(width, signed));
            assertStored(values, 2, minimum(width, signed));
            final byte[] snapshot = values.toByteArray();
            for (int byteIndex = 0; byteIndex < byteWidth; byteIndex++) {
                final int expected = byteIndex == bit / Byte.SIZE ? 1 << (bit % Byte.SIZE) : 0;
                assertEquals(expected, snapshot[byteWidth + byteIndex] & 0xff);
            }
            assertUnusedBitsAreZero(values);
        }
        put(values, 1, wrap(ONE.shiftLeft(width).subtract(ONE), width, signed));
        assertUnusedBitsAreZero(values);
    }

    @ParameterizedTest(name = "width={0}, signed={1}")
    @MethodSource("formats")
    void boundaryArithmeticMatchesBigIntegerAndRefusalPreservesAllBytes(int width, boolean signed) {
        final M3WideIntegerArray values = new M3WideIntegerArray(width, 3, signed);
        put(values, 0, minimum(width, signed));
        put(values, 2, maximum(width, signed));
        for (BigInteger left : boundaries(width, signed)) {
            for (BigInteger right : boundaries(width, signed)) {
                checkArithmetic(values, left, right, false);
                checkArithmetic(values, left, right, true);
            }
        }
    }

    @ParameterizedTest(name = "width={0}, signed={1}")
    @MethodSource("formats")
    void deterministicRandomArithmeticMatchesIndependentBigInteger(int width, boolean signed) {
        final Random random = new Random(SEED ^ ((long) width << 1) ^ (signed ? 1 : 0));
        final M3WideIntegerArray values = new M3WideIntegerArray(width, 3, signed);
        put(values, 0, minimum(width, signed));
        put(values, 2, maximum(width, signed));
        for (int trial = 0; trial < RANDOM_PAIRS_PER_FORMAT; trial++) {
            final BigInteger left = wrap(new BigInteger(width, random), width, signed);
            final BigInteger right = wrap(new BigInteger(width, random), width, signed);
            checkArithmetic(values, left, right, false);
            checkArithmetic(values, left, right, true);
        }
    }

    @ParameterizedTest(name = "width={0}, signed={1}")
    @MethodSource("formats")
    void comparisonMatchesNumericOrderIncludingUnsignedLowLane(int width, boolean signed) {
        final List<BigInteger> cases = boundaries(width, signed);
        final M3WideIntegerArray left = new M3WideIntegerArray(width, cases.size(), signed);
        final M3WideIntegerArray right = new M3WideIntegerArray(width, cases.size(), signed);
        for (int i = 0; i < cases.size(); i++) {
            put(left, i, cases.get(i));
            put(right, i, cases.get(cases.size() - i - 1));
        }
        for (int i = 0; i < cases.size(); i++) {
            assertEquals(0, left.compare(i, left, i));
            for (int j = 0; j < cases.size(); j++) {
                final int expected = cases.get(i).compareTo(cases.get(cases.size() - j - 1));
                assertEquals(Integer.signum(expected), Integer.signum(left.compare(i, right, j)));
                assertEquals(-Integer.signum(expected), Integer.signum(right.compare(j, left, i)));
            }
        }
    }

    @ParameterizedTest(name = "width={0}, signed={1}")
    @MethodSource("formats")
    void invalidHighOperandsAreRejectedWithoutMutation(int width, boolean signed) {
        final M3WideIntegerArray values = new M3WideIntegerArray(width, 3, signed);
        put(values, 0, minimum(width, signed));
        put(values, 1, ONE);
        put(values, 2, maximum(width, signed));
        if (width == 128) {
            for (long high : new long[] {0, -1, Long.MIN_VALUE, Long.MAX_VALUE}) {
                values.set(1, high, -1L);
                assertEquals(high, values.getHigh(1));
            }
            return;
        }
        final int highBits = width - Long.SIZE;
        final long invalidPositive = 1L << (signed ? highBits - 1 : highBits);
        final long invalidNegative = signed ? -(1L << (highBits - 1)) - 1 : -1L;
        assertInvalidHigh(values, invalidPositive);
        assertInvalidHigh(values, invalidNegative);
    }

    @ParameterizedTest(name = "width={0}, signed={1}")
    @MethodSource("formats")
    void invalidIndexesAndIncompatibleComparisonsHaveDefinedPrecedence(int width, boolean signed) {
        final M3WideIntegerArray values = new M3WideIntegerArray(width, 2, signed);
        final byte[] before = values.toByteArray();
        for (int index : new int[] {Integer.MIN_VALUE, -1, 2, Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> values.getHigh(index));
            assertThrows(IndexOutOfBoundsException.class, () -> values.getLow(index));
            assertThrows(IndexOutOfBoundsException.class, () -> values.set(index, Long.MAX_VALUE, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> values.addExact(index, Long.MAX_VALUE, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> values.addWrapping(index, Long.MAX_VALUE, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> values.subtractExact(index, Long.MAX_VALUE, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> values.subtractWrapping(index, Long.MAX_VALUE, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> values.compare(index, values, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> values.compare(0, values, index));
            assertArrayEquals(before, values.toByteArray());
        }
        assertThrows(NullPointerException.class, () -> values.compare(-1, null, -1));
        final M3WideIntegerArray differentSign = new M3WideIntegerArray(width, 0, !signed);
        assertThrows(IllegalArgumentException.class, () -> values.compare(-1, differentSign, -1));
        final int otherWidth = width == 128 ? 127 : width + 1;
        final M3WideIntegerArray differentWidth = new M3WideIntegerArray(otherWidth, 0, signed);
        assertThrows(IllegalArgumentException.class, () -> values.compare(-1, differentWidth, -1));
        assertArrayEquals(before, values.toByteArray());
    }

    @Test
    void namedWidthsUseNineTenTwelveAndSixteenBytesAndSnapshotsAreIndependent() {
        final int[] widths = {65, 80, 96, 128};
        final int[] byteWidths = {9, 10, 12, 16};
        for (int i = 0; i < widths.length; i++) {
            final M3WideIntegerArray values = new M3WideIntegerArray(widths[i], 2, true);
            assertEquals(2L * byteWidths[i], values.payloadBytes());
            put(values, 1, BigInteger.valueOf(-1));
            final byte[] first = values.toByteArray();
            final byte[] second = values.toByteArray();
            assertNotSame(first, second);
            first[byteWidths[i]] = 0;
            assertStored(values, 1, BigInteger.valueOf(-1));
            assertEquals(0xff, second[byteWidths[i]] & 0xff);
            put(values, 1, ZERO);
            assertEquals(0xff, second[byteWidths[i]] & 0xff);
            assertStored(values, 1, ZERO);
        }
    }

    private static void checkArithmetic(
            M3WideIntegerArray values, BigInteger left, BigInteger right, boolean subtract) {
        put(values, 1, left);
        final byte[] before = values.toByteArray();
        final long high = right.shiftRight(Long.SIZE).longValue();
        final long low = right.longValue();
        final BigInteger mathematical = subtract ? left.subtract(right) : left.add(right);
        final boolean fits = mathematical.compareTo(minimum(values.bitsPerValue(), values.signed())) >= 0
                && mathematical.compareTo(maximum(values.bitsPerValue(), values.signed())) <= 0;
        if (fits) {
            if (subtract) {
                values.subtractExact(1, high, low);
            } else {
                values.addExact(1, high, low);
            }
            assertStored(values, 1, mathematical);
        } else if (subtract) {
            assertThrows(ArithmeticException.class, () -> values.subtractExact(1, high, low));
            assertArrayEquals(before, values.toByteArray());
        } else {
            assertThrows(ArithmeticException.class, () -> values.addExact(1, high, low));
            assertArrayEquals(before, values.toByteArray());
        }
        assertNeighbors(values);

        put(values, 1, left);
        if (subtract) {
            values.subtractWrapping(1, high, low);
        } else {
            values.addWrapping(1, high, low);
        }
        assertStored(values, 1, wrap(mathematical, values.bitsPerValue(), values.signed()));
        assertUnusedBitsAreZero(values);
        assertNeighbors(values);
    }

    private static void assertInvalidHigh(M3WideIntegerArray values, long high) {
        final byte[] before = values.toByteArray();
        assertThrows(IllegalArgumentException.class, () -> values.set(1, high, 3));
        assertArrayEquals(before, values.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> values.addExact(1, high, 3));
        assertArrayEquals(before, values.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> values.addWrapping(1, high, 3));
        assertArrayEquals(before, values.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> values.subtractExact(1, high, 3));
        assertArrayEquals(before, values.toByteArray());
        assertThrows(IllegalArgumentException.class, () -> values.subtractWrapping(1, high, 3));
        assertArrayEquals(before, values.toByteArray());
    }

    private static void assertNeighbors(M3WideIntegerArray values) {
        assertStored(values, 0, minimum(values.bitsPerValue(), values.signed()));
        assertStored(values, 2, maximum(values.bitsPerValue(), values.signed()));
    }

    private static void assertStored(M3WideIntegerArray values, int index, BigInteger expected) {
        assertEquals(expected.longValue(), values.getLow(index));
        assertEquals(expected.shiftRight(Long.SIZE).longValue(), values.getHigh(index));
        final BigInteger high = values.signed() ? BigInteger.valueOf(values.getHigh(index))
                : BigInteger.valueOf(values.getHigh(index)).and(MASK64);
        final BigInteger low = BigInteger.valueOf(values.getLow(index)).and(MASK64);
        assertEquals(expected, high.shiftLeft(Long.SIZE).add(low));
        assertEquals(expected, decodeSnapshot(values, index));
    }

    private static BigInteger decodeSnapshot(M3WideIntegerArray values, int index) {
        final int byteWidth = (values.bitsPerValue() + 7) / Byte.SIZE;
        final byte[] payload = values.toByteArray();
        BigInteger decoded = ZERO;
        for (int i = byteWidth - 1; i >= 0; i--) {
            decoded = decoded.shiftLeft(Byte.SIZE)
                    .or(BigInteger.valueOf(payload[index * byteWidth + i] & 0xff));
        }
        if (values.signed() && decoded.testBit(values.bitsPerValue() - 1)) {
            return decoded.subtract(ONE.shiftLeft(values.bitsPerValue()));
        }
        return decoded;
    }

    private static void assertUnusedBitsAreZero(M3WideIntegerArray values) {
        final int usedBits = values.bitsPerValue() % Byte.SIZE;
        if (usedBits == 0) {
            return;
        }
        final byte[] payload = values.toByteArray();
        final int byteWidth = (values.bitsPerValue() + 7) / Byte.SIZE;
        final int unusedMask = 0xff << usedBits;
        for (int index = 0; index < values.size(); index++) {
            assertEquals(0, (payload[(index + 1) * byteWidth - 1] & 0xff) & unusedMask);
        }
    }

    private static void put(M3WideIntegerArray values, int index, BigInteger value) {
        values.set(index, value.shiftRight(Long.SIZE).longValue(), value.longValue());
    }

    private static List<BigInteger> boundaries(int width, boolean signed) {
        final Set<BigInteger> values = new LinkedHashSet<>();
        values.add(minimum(width, signed));
        values.add(minimum(width, signed).add(ONE));
        values.add(maximum(width, signed).subtract(ONE));
        values.add(maximum(width, signed));
        values.add(ZERO);
        values.add(ONE);
        values.add(BigInteger.valueOf(-1));
        for (int bit : new int[] {31, 32, 63, 64}) {
            final BigInteger power = ONE.shiftLeft(bit);
            values.add(power.subtract(ONE));
            values.add(power);
            values.add(power.add(ONE));
            values.add(power.negate());
        }
        return values.stream()
                .filter(value -> value.compareTo(minimum(width, signed)) >= 0)
                .filter(value -> value.compareTo(maximum(width, signed)) <= 0)
                .toList();
    }

    private static BigInteger minimum(int width, boolean signed) {
        return signed ? ONE.shiftLeft(width - 1).negate() : ZERO;
    }

    private static BigInteger maximum(int width, boolean signed) {
        return ONE.shiftLeft(signed ? width - 1 : width).subtract(ONE);
    }

    private static BigInteger wrap(BigInteger value, int width, boolean signed) {
        final BigInteger modulus = ONE.shiftLeft(width);
        final BigInteger residue = value.mod(modulus);
        return signed && residue.testBit(width - 1) ? residue.subtract(modulus) : residue;
    }
}
