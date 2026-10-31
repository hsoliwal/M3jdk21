// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Mathematical oracles use division, multiplication and arbitrary precision. */
final class VIntTest {
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger BASE = BigInteger.valueOf(128);
    private static final BigInteger TWO_TO_63 = TWO.pow(63);
    private static final BigInteger TWO_TO_64 = TWO.pow(64);
    private static final HexFormat HEX = HexFormat.ofDelimiter(" ");

    @Test
    void unsignedLiteralBytesCoverEveryCarrierSign() {
        assertLiteral(0L, "00", false);
        assertLiteral(1L, "01", false);
        assertLiteral(127L, "7f", false);
        assertLiteral(128L, "80 01", false);
        assertLiteral(255L, "ff 01", false);
        assertLiteral(16_383L, "ff 7f", false);
        assertLiteral(16_384L, "80 80 01", false);
        assertLiteral(2_097_151L, "ff ff 7f", false);
        assertLiteral(2_097_152L, "80 80 80 01", false);
        assertLiteral(4_294_967_295L, "ff ff ff ff 0f", false);
        assertLiteral(Long.MAX_VALUE, "ff ff ff ff ff ff ff ff 7f", false);
        assertLiteral(Long.MIN_VALUE, "80 80 80 80 80 80 80 80 80 01", false);
        assertLiteral(-1L, "ff ff ff ff ff ff ff ff ff 01", false);
    }

    @Test
    void zigZagLiteralBytesCoverSignedExtrema() {
        assertLiteral(0L, "00", true);
        assertLiteral(-1L, "01", true);
        assertLiteral(1L, "02", true);
        assertLiteral(-2L, "03", true);
        assertLiteral(63L, "7e", true);
        assertLiteral(-64L, "7f", true);
        assertLiteral(64L, "80 01", true);
        assertLiteral(-65L, "81 01", true);
        assertLiteral(Long.MAX_VALUE, "fe ff ff ff ff ff ff ff ff 01", true);
        assertLiteral(Long.MIN_VALUE, "ff ff ff ff ff ff ff ff ff 01", true);
    }

    @Test
    void powersAndNeighborsAgreeWithIndependentBigIntegerOracles() {
        List<Long> boundaries = new ArrayList<>();
        boundaries.add(0L);
        boundaries.add(-1L);
        boundaries.add(Long.MIN_VALUE);
        boundaries.add(Long.MAX_VALUE);
        for (int bit = 0; bit < 64; bit++) {
            BigInteger power = TWO.pow(bit);
            for (int offset = -1; offset <= 1; offset++) {
                BigInteger unsigned = power.add(BigInteger.valueOf(offset));
                boundaries.add(unsigned.longValue());
                BigInteger signed = unsigned.negate();
                if (signed.compareTo(TWO_TO_63.negate()) >= 0) {
                    boundaries.add(signed.longValueExact());
                }
            }
        }
        long[] values = toArray(boundaries);
        assertBothFormats(values);
        for (long value : values) {
            assertBothFormats(new long[] {value});
        }
    }

    @Test
    void allSingleOctetsAreEitherValuesOrTruncatedContinuations() {
        for (int octet = 0; octet < 256; octet++) {
            byte[] bytes = {(byte) octet};
            if (octet < 128) {
                assertArrayEquals(new long[] {octet}, VInt.decodeUnsigned64(bytes));
                assertArrayEquals(referenceDecode(bytes, true), VInt.decodeZigZag64(bytes));
            } else {
                assertMalformed(bytes);
            }
        }
    }

    @Test
    void everyPossibleTenthOctetIsCheckedForLowAndHighPayloads() {
        for (int preceding : new int[] {0x80, 0xff}) {
            for (int tenth = 0; tenth < 256; tenth++) {
                byte[] bytes = new byte[10];
                Arrays.fill(bytes, 0, 9, (byte) preceding);
                bytes[9] = (byte) tenth;
                if (tenth == 1) {
                    long expected = preceding == 0x80 ? Long.MIN_VALUE : -1L;
                    assertArrayEquals(new long[] {expected}, VInt.decodeUnsigned64(bytes));
                    assertArrayEquals(referenceDecode(bytes, true), VInt.decodeZigZag64(bytes));
                    byte[] followedByZero = Arrays.copyOf(bytes, 11);
                    assertArrayEquals(new long[] {expected, 0L},
                            VInt.decodeUnsigned64(followedByZero));
                    assertArrayEquals(referenceDecode(followedByZero, true),
                            VInt.decodeZigZag64(followedByZero));
                } else {
                    assertMalformed(bytes);
                    assertMalformed(Arrays.copyOf(bytes, 11));
                }
            }
        }
    }

    @Test
    void everyProperPrefixAtEachEncodingWidthIsTruncated() {
        for (int width = 2; width <= 10; width++) {
            BigInteger minimum = BASE.pow(width - 1);
            BigInteger maximum = BASE.pow(width).min(TWO_TO_64).subtract(BigInteger.ONE);
            for (BigInteger value : List.of(minimum, maximum)) {
                byte[] full = referenceEncode(new long[] {value.longValue()}, false);
                assertEquals(width, full.length);
                for (int retained = 1; retained < full.length; retained++) {
                    assertMalformed(Arrays.copyOf(full, retained));
                }
            }
        }
    }

    @Test
    void terminalZeroNeverMakesAMultiOctetEncodingCanonical() {
        for (int width = 2; width <= 10; width++) {
            for (int lowPayload = 0; lowPayload < 128; lowPayload++) {
                byte[] nonminimal = new byte[width];
                Arrays.fill(nonminimal, 0, width - 1, (byte) 0x80);
                nonminimal[0] = (byte) (0x80 | lowPayload);
                assertMalformed(nonminimal);
            }
            byte[] nonminimalHighPayload = new byte[width];
            Arrays.fill(nonminimalHighPayload, 0, width - 1, (byte) 0xff);
            assertMalformed(nonminimalHighPayload);
        }
    }

    @Test
    void malformedValuesAfterAValidPrefixCannotReturnAPartialResult() {
        byte[] prefix = referenceEncode(new long[] {0L, 128L, -1L}, false);
        for (String malformed : List.of(
                "80", "80 00", "ff 00", "ff ff 00",
                "80 80 80 80 80 80 80 80 80 00",
                "ff ff ff ff ff ff ff ff ff 02",
                "80 80 80 80 80 80 80 80 80 81 00")) {
            assertMalformed(concat(prefix, HEX.parseHex(malformed)));
        }
        byte[] middle = concat(prefix, HEX.parseHex("80 00"));
        assertMalformed(concat(middle, referenceEncode(new long[] {1L, 0L}, false)));
    }

    @Test
    void completedValuesResetWidthAndAccumulator() {
        long[] values = {Long.MIN_VALUE, 0L, -1L, 1L, Long.MAX_VALUE, 128L, 0L, -1L};
        assertBothFormats(values);
        for (boolean zigZag : new boolean[] {false, true}) {
            ByteArrayOutputStream concatenated = new ByteArrayOutputStream();
            for (long value : values) {
                concatenated.writeBytes(encode(new long[] {value}, zigZag));
            }
            assertArrayEquals(referenceEncode(values, zigZag), concatenated.toByteArray());
            assertArrayEquals(values, decode(concatenated.toByteArray(), zigZag));
        }
    }

    @Test
    void emptyInputsProduceFreshEmptyOutputsAndNullsAreRejected() {
        assertArrayEquals(new byte[0], VInt.encodeUnsigned64(new long[0]));
        assertArrayEquals(new byte[0], VInt.encodeZigZag64(new long[0]));
        assertArrayEquals(new long[0], VInt.decodeUnsigned64(new byte[0]));
        assertArrayEquals(new long[0], VInt.decodeZigZag64(new byte[0]));
        assertNotSame(VInt.encodeUnsigned64(new long[0]), VInt.encodeUnsigned64(new long[0]));
        assertNotSame(VInt.encodeZigZag64(new long[0]), VInt.encodeZigZag64(new long[0]));
        assertNotSame(VInt.decodeUnsigned64(new byte[0]), VInt.decodeUnsigned64(new byte[0]));
        assertNotSame(VInt.decodeZigZag64(new byte[0]), VInt.decodeZigZag64(new byte[0]));
        assertThrows(NullPointerException.class, () -> VInt.encodeUnsigned64(null));
        assertThrows(NullPointerException.class, () -> VInt.encodeZigZag64(null));
        assertThrows(NullPointerException.class, () -> VInt.decodeUnsigned64(null));
        assertThrows(NullPointerException.class, () -> VInt.decodeZigZag64(null));
    }

    @Test
    void inputsAndReturnedArraysHaveIndependentOwnership() {
        for (boolean zigZag : new boolean[] {false, true}) {
            long[] values = {0L, 127L, 128L, -1L, Long.MIN_VALUE, Long.MAX_VALUE};
            long[] before = values.clone();
            byte[] bytes = encode(values, zigZag);
            byte[] encodedSnapshot = bytes.clone();
            assertArrayEquals(before, values);
            byte[] anotherEncoding = encode(values, zigZag);
            assertNotSame(bytes, anotherEncoding);
            values[0] = 123L;
            assertArrayEquals(encodedSnapshot, bytes);

            long[] decoded = decode(bytes, zigZag);
            assertArrayEquals(before, decoded);
            assertArrayEquals(encodedSnapshot, bytes);
            long[] anotherDecoding = decode(bytes, zigZag);
            assertNotSame(decoded, anotherDecoding);
            decoded[0] = 456L;
            assertArrayEquals(before, anotherDecoding);
            assertArrayEquals(encodedSnapshot, bytes);
            bytes[0] = 99;
            assertArrayEquals(encodedSnapshot, anotherEncoding);
            assertArrayEquals(before, anotherDecoding);
            assertEquals(123L, values[0]);
        }
    }

    @Test
    void portableArraysAreNotRestrictedToTheNativeBatchCeiling() {
        long[] values = new long[10_003];
        for (int index = 0; index < values.length; index++) {
            values[index] = Long.MIN_VALUE + index;
        }
        for (boolean zigZag : new boolean[] {false, true}) {
            byte[] encoded = encode(values, zigZag);
            assertTrue(encoded.length > 4096);
            assertArrayEquals(referenceEncode(values, zigZag), encoded);
            assertArrayEquals(values, decode(encoded, zigZag));
        }
    }

    @Test
    void deterministicFullWidthValuesMatchMathematicalEncodingAndDecoding() {
        for (long seed : new long[] {0L, 0x53594e45584941L, -1L}) {
            Random random = new Random(seed);
            long[] values = new long[4097];
            for (int index = 0; index < values.length; index++) {
                values[index] = random.nextLong();
            }
            assertBothFormats(values);
        }
    }

    @Test
    void deterministicByteStringsAgreeWithTheIndependentFormatOracle() {
        Random random = new Random(0x56494e543634L);
        for (int trial = 0; trial < 4096; trial++) {
            byte[] bytes = new byte[random.nextInt(41)];
            random.nextBytes(bytes);
            long[] expected;
            try {
                expected = referenceDecode(bytes, false);
            } catch (IllegalArgumentException malformed) {
                assertMalformed(bytes);
                continue;
            }
            byte[] snapshot = bytes.clone();
            assertArrayEquals(expected, VInt.decodeUnsigned64(bytes));
            assertArrayEquals(referenceDecode(bytes, true), VInt.decodeZigZag64(bytes));
            assertArrayEquals(bytes, VInt.encodeUnsigned64(expected));
            assertArrayEquals(bytes, VInt.encodeZigZag64(referenceDecode(bytes, true)));
            assertArrayEquals(snapshot, bytes);
        }
    }

    private static void assertLiteral(long value, String hex, boolean zigZag) {
        byte[] expected = HEX.parseHex(hex);
        assertArrayEquals(expected, encode(new long[] {value}, zigZag));
        assertArrayEquals(new long[] {value}, decode(expected, zigZag));
        assertArrayEquals(expected, referenceEncode(new long[] {value}, zigZag));
        assertArrayEquals(new long[] {value}, referenceDecode(expected, zigZag));
    }

    private static void assertBothFormats(long[] values) {
        long[] snapshot = values.clone();
        for (boolean zigZag : new boolean[] {false, true}) {
            byte[] expected = referenceEncode(values, zigZag);
            assertArrayEquals(expected, encode(values, zigZag));
            assertArrayEquals(values, decode(expected, zigZag));
            assertArrayEquals(values, referenceDecode(expected, zigZag));
        }
        assertArrayEquals(snapshot, values);
    }

    private static void assertMalformed(byte[] bytes) {
        byte[] snapshot = bytes.clone();
        assertThrows(IllegalArgumentException.class, () -> referenceDecode(bytes, false));
        assertThrows(IllegalArgumentException.class, () -> VInt.decodeUnsigned64(bytes));
        assertThrows(IllegalArgumentException.class, () -> VInt.decodeZigZag64(bytes));
        assertArrayEquals(snapshot, bytes);
    }

    private static byte[] encode(long[] values, boolean zigZag) {
        return zigZag ? VInt.encodeZigZag64(values) : VInt.encodeUnsigned64(values);
    }

    private static long[] decode(byte[] bytes, boolean zigZag) {
        return zigZag ? VInt.decodeZigZag64(bytes) : VInt.decodeUnsigned64(bytes);
    }

    private static byte[] concat(byte[] left, byte[] right) {
        byte[] joined = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, joined, left.length, right.length);
        return joined;
    }

    private static long[] toArray(List<Long> values) {
        long[] result = new long[values.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    /** Uses arithmetic division and remainders, not production masks or shifts. */
    private static byte[] referenceEncode(long[] values, boolean zigZag) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (long value : values) {
            BigInteger unsigned = BigInteger.valueOf(value);
            if (zigZag) {
                unsigned = unsigned.signum() < 0
                        ? unsigned.negate().multiply(TWO).subtract(BigInteger.ONE)
                        : unsigned.multiply(TWO);
            } else if (unsigned.signum() < 0) {
                unsigned = unsigned.add(TWO_TO_64);
            }
            do {
                BigInteger[] quotientAndRemainder = unsigned.divideAndRemainder(BASE);
                unsigned = quotientAndRemainder[0];
                int digit = quotientAndRemainder[1].intValueExact();
                bytes.write(digit + (unsigned.signum() == 0 ? 0 : 128));
            } while (unsigned.signum() != 0);
        }
        return bytes.toByteArray();
    }

    /** Checks mathematical range and minimal magnitude before converting to long. */
    private static long[] referenceDecode(byte[] bytes, boolean zigZag) {
        List<Long> values = new ArrayList<>();
        int cursor = 0;
        while (cursor < bytes.length) {
            BigInteger unsigned = BigInteger.ZERO;
            BigInteger place = BigInteger.ONE;
            int width = 0;
            while (true) {
                if (cursor == bytes.length) {
                    throw new IllegalArgumentException("Truncated mathematical encoding");
                }
                int octet = Byte.toUnsignedInt(bytes[cursor++]);
                unsigned = unsigned.add(place.multiply(BigInteger.valueOf(octet % 128)));
                width++;
                if (unsigned.compareTo(TWO_TO_64) >= 0) {
                    throw new IllegalArgumentException("Mathematical unsigned overflow");
                }
                if (octet < 128) {
                    if (width > 1 && unsigned.compareTo(BASE.pow(width - 1)) < 0) {
                        throw new IllegalArgumentException("Nonminimal mathematical encoding");
                    }
                    break;
                }
                if (width == 10) {
                    throw new IllegalArgumentException("Excess mathematical continuation");
                }
                place = place.multiply(BASE);
            }
            BigInteger signed;
            if (zigZag) {
                BigInteger[] quotientAndRemainder = unsigned.divideAndRemainder(TWO);
                signed = quotientAndRemainder[1].signum() == 0 ? quotientAndRemainder[0]
                        : quotientAndRemainder[0].add(BigInteger.ONE).negate();
            } else {
                signed = unsigned.compareTo(TWO_TO_63) < 0
                        ? unsigned : unsigned.subtract(TWO_TO_64);
            }
            values.add(signed.longValueExact());
        }
        return toArray(values);
    }
}
