// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays.packed;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.TreeSet;

/** Deterministic contract tests with byte vectors and an independent BigInteger oracle. */
public final class PackedIntArrayTest {
    private static final BigInteger ONE = BigInteger.ONE;
    private static int cases;
    private static long assertions;
    private static String currentCase = "initialization";

    PackedIntArrayTest() {
    }

    /**
     * Runs all correctness checks and prints a JSON summary only after success.
     *
     * @param args unused command-line arguments
     */
    @org.junit.jupiter.api.Test
    void frozenContract() {
        main(new String[0]);
    }

    public static void main(String[] args) {
        cases = 0;
        assertions = 0;
        currentCase = "initialization";
        runCase("public-constants-and-types", PackedIntArrayTest::publicConstantsAndTypes);
        for (int bits : new int[] {40, 48, 56}) {
            runCase("metadata-" + bits, () -> metadata(bits));
            runCase("explicit-vectors-" + bits, () -> explicitVectors(bits));
            runCase("numeric-boundaries-" + bits, () -> numericBoundaries(bits));
            runCase("single-bits-and-neighbors-" + bits, () -> singleBitsAndNeighbors(bits));
            runCase("deterministic-oracle-" + bits, () -> deterministicOracle(bits));
            runCase("raw-patterns-" + bits, () -> rawPatterns(bits));
            runCase("index-and-length-validation-" + bits, () -> indexAndLengthValidation(bits));
            runCase("value-failure-atomicity-" + bits, () -> valueFailureAtomicity(bits));
            runCase("fill-" + bits, () -> fill(bits));
            runCase("byte-ownership-" + bits, () -> byteOwnership(bits));
            runCase("bulk-operations-" + bits, () -> bulkOperations(bits));
            runCase("bulk-failure-atomicity-" + bits, () -> bulkFailureAtomicity(bits));
            runCase("overlap-and-copy-" + bits, () -> overlapAndCopy(bits));
            runCase("copy-failure-atomicity-" + bits, () -> copyFailureAtomicity(bits));
            runCase("empty-operations-" + bits, () -> emptyOperations(bits));
        }
        System.out.println("{\"status\":\"PASS\",\"testCases\":" + cases
                + ",\"assertions\":" + assertions + ",\"widths\":[40,48,56]}");
    }

    private static void publicConstantsAndTypes() {
        equal(40, Int40Array.BITS, "int40 bits constant");
        equal(5, Int40Array.BYTES, "int40 bytes constant");
        equal(-549_755_813_888L, Int40Array.MIN_VALUE, "int40 minimum constant");
        equal(549_755_813_887L, Int40Array.MAX_VALUE, "int40 maximum constant");
        equal(48, Int48Array.BITS, "int48 bits constant");
        equal(6, Int48Array.BYTES, "int48 bytes constant");
        equal(-140_737_488_355_328L, Int48Array.MIN_VALUE, "int48 minimum constant");
        equal(140_737_488_355_327L, Int48Array.MAX_VALUE, "int48 maximum constant");
        equal(56, Int56Array.BITS, "int56 bits constant");
        equal(7, Int56Array.BYTES, "int56 bytes constant");
        equal(-36_028_797_018_963_968L, Int56Array.MIN_VALUE, "int56 minimum constant");
        equal(36_028_797_018_963_967L, Int56Array.MAX_VALUE, "int56 maximum constant");
        check(PackedIntArray.class.isSealed(), "base class is sealed");
        Set<Class<?>> permitted = Set.of(PackedIntArray.class.getPermittedSubclasses());
        check(permitted.equals(Set.of(Int40Array.class, Int48Array.class, Int56Array.class)),
                "sealed family has exactly the documented widths");
        equal(0, PackedIntArray.class.getConstructors().length, "no public base constructor");
        for (Class<?> type : permitted) {
            check(Modifier.isFinal(type.getModifiers()), "width class is final");
        }
    }

    private static void metadata(int bits) {
        for (int length : new int[] {0, 1, 2, 17, 257}) {
            PackedIntArray array = create(bits, length);
            equal(length, array.length(), "length");
            equal(bits, array.bitsPerValue(), "bits per value");
            equal(bits / 8, array.bytesPerValue(), "bytes per value");
            equal((long) length * (bits / 8), array.payloadBytes(), "exact payload");
            equal(minimum(bits), array.minValue(), "minimum");
            equal(maximum(bits), array.maxValue(), "maximum");
            equal(array.payloadBytes(), array.toByteArray().length, "export payload length");
            bytesEqual(new byte[length * (bits / 8)], array.toByteArray(), "zero bytes");
            longsEqual(new long[length], array.toLongArray(), "zero decoded values");
            for (int index = 0; index < length; index++) {
                equal(0, array.get(index), "zero initialization");
            }
        }
    }

    private static void explicitVectors(int bits) {
        String[] hex = switch (bits) {
            case 40 -> new String[] {
                "0000000000", "0100000000", "ffffffffff", "0000000080",
                "ffffffff7f", "8967452301", "feffffffff"
            };
            case 48 -> new String[] {
                "000000000000", "010000000000", "ffffffffffff", "000000000080",
                "ffffffffff7f", "896745230100", "feffffffffff"
            };
            case 56 -> new String[] {
                "00000000000000", "01000000000000", "ffffffffffffff", "00000000000080",
                "ffffffffffff7f", "89674523010000", "feffffffffffff"
            };
            default -> throw new AssertionError("unsupported test width");
        };
        long[] values = {0, 1, -1, minimum(bits), maximum(bits), 0x0123456789L, -2};
        PackedIntArray array = create(bits, values.length);
        int width = bits / 8;
        byte[] expected = new byte[values.length * width];
        for (int index = 0; index < values.length; index++) {
            byte[] vector = HexFormat.of().parseHex(hex[index]);
            equal(width, vector.length, "explicit vector width");
            array.set(index, values[index]);
            equal(values[index], array.get(index), "explicit scalar round trip");
            System.arraycopy(vector, 0, expected, index * width, width);
            bytesEqual(vector, oracleEncode(values[index], width), "independent vector oracle");
        }
        bytesEqual(expected, array.toByteArray(), "literal little-endian encoding");
        longsEqual(values, decode(bits, expected).toLongArray(), "literal vector import");
    }

    private static void numericBoundaries(int bits) {
        TreeSet<Long> values = new TreeSet<>();
        long min = minimum(bits);
        long max = maximum(bits);
        values.addAll(Arrays.asList(min, min + 1, -1L, 0L, 1L, max - 1, max));
        for (int position : new int[] {1, 7, 8, 15, 16, 23, 24, 31, 32, 39, 40, 47, 48, 55}) {
            long power = 1L << position;
            for (long center : new long[] {power, -power}) {
                for (long delta = -1; delta <= 1; delta++) {
                    long value = center + delta;
                    if (value >= min && value <= max) {
                        values.add(value);
                    }
                }
            }
        }
        PackedIntArray array = create(bits, values.size());
        int index = 0;
        for (long value : values) {
            array.set(index, value);
            equal(value, array.get(index), "boundary scalar round trip");
            index++;
        }
        byte[] packed = array.toByteArray();
        index = 0;
        for (long value : values) {
            byte[] element = elementBytes(packed, index, bits / 8);
            bytesEqual(oracleEncode(value, bits / 8), element, "boundary bytes");
            equal(value, oracleDecode(element), "boundary oracle decoding");
            index++;
        }
    }

    private static void singleBitsAndNeighbors(int bits) {
        PackedIntArray array = create(bits, 3);
        long min = minimum(bits);
        long max = maximum(bits);
        array.set(0, min);
        array.set(2, max);
        BigInteger mask = ONE.shiftLeft(bits).subtract(ONE);
        for (int position = 0; position < bits; position++) {
            for (BigInteger raw : new BigInteger[] {
                ONE.shiftLeft(position), mask.xor(ONE.shiftLeft(position))
            }) {
                long value = signed(raw, bits);
                array.set(1, value);
                equal(value, array.get(1), "single-bit and complement round trip");
                equal(min, array.get(0), "left neighbor preserved");
                equal(max, array.get(2), "right neighbor preserved");
                bytesEqual(oracleEncode(value, bits / 8),
                        elementBytes(array.toByteArray(), 1, bits / 8), "single-bit bytes");
            }
        }
    }

    private static void deterministicOracle(int bits) {
        int length = 4_096;
        PackedIntArray array = create(bits, length);
        long[] expected = new long[length];
        BigInteger modulus = ONE.shiftLeft(bits);
        BigInteger step = new BigInteger("9e3779b97f4a7c15", 16);
        BigInteger raw = new BigInteger("d1b54a32d192ed03", 16).mod(modulus);
        // A fixed additive sequence modulo 2^W provides reproducible broad bit coverage.
        for (int index = 0; index < length; index++) {
            expected[index] = signed(raw, bits);
            array.set(index, expected[index]);
            raw = raw.add(step).mod(modulus);
        }
        byte[] packed = array.toByteArray();
        for (int index = 0; index < length; index++) {
            byte[] element = elementBytes(packed, index, bits / 8);
            equal(expected[index], array.get(index), "sequence scalar read");
            equal(expected[index], oracleDecode(element), "sequence independent decode");
            bytesEqual(oracleEncode(expected[index], bits / 8), element,
                    "sequence independent encode");
        }
        longsEqual(expected, array.toLongArray(), "sequence full decode");
        bytesEqual(packed, decode(bits, packed).toByteArray(), "sequence wire idempotence");
        longsEqual(expected, decode(bits, packed).toLongArray(), "sequence import values");
    }

    private static void rawPatterns(int bits) {
        int width = bits / 8;
        byte[] raw = new byte[1_024 * width];
        for (int index = 0; index < raw.length; index++) {
            raw[index] = (byte) (index * 73 + 19);
        }
        PackedIntArray array = decode(bits, raw);
        equal(1_024, array.length(), "raw pattern count");
        bytesEqual(raw, array.toByteArray(), "every complete raw pattern retained");
        for (int index = 0; index < array.length(); index++) {
            equal(oracleDecode(elementBytes(raw, index, width)), array.get(index),
                    "raw pattern independent decoding");
        }
    }

    private static void indexAndLengthValidation(int bits) {
        PackedIntArray array = create(bits, 3);
        array.fill(27);
        byte[] before = array.toByteArray();
        for (int index : new int[] {-1, 3, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            expect(IndexOutOfBoundsException.class, () -> array.get(index));
            expect(IndexOutOfBoundsException.class, () -> array.set(index, 1));
            expect(IndexOutOfBoundsException.class, () -> array.set(index, Long.MAX_VALUE));
            bytesEqual(before, array.toByteArray(), "failed scalar index preserves bytes");
        }
        for (int[] range : new int[][] {
            {-1, 0}, {0, 4}, {2, 1}, {Integer.MIN_VALUE, 1}, {1, Integer.MAX_VALUE}
        }) {
            expect(IndexOutOfBoundsException.class, () -> array.fill(range[0], range[1], 1));
            expect(IndexOutOfBoundsException.class,
                    () -> array.fill(range[0], range[1], Long.MAX_VALUE));
            bytesEqual(before, array.toByteArray(), "failed fill range preserves bytes");
        }
        for (int length : new int[] {
            -1, Integer.MIN_VALUE, Integer.MAX_VALUE / (bits / 8) + 1, Integer.MAX_VALUE
        }) {
            expect(IllegalArgumentException.class, () -> create(bits, length));
        }
    }

    private static void valueFailureAtomicity(int bits) {
        PackedIntArray array = create(bits, 5);
        array.setAll(0, new long[] {-2, -1, 0, 1, 2}, 0, 5);
        byte[] before = array.toByteArray();
        for (long value : new long[] {
            minimum(bits) - 1, maximum(bits) + 1, Long.MIN_VALUE, Long.MAX_VALUE
        }) {
            expect(IllegalArgumentException.class, () -> array.set(2, value));
            bytesEqual(before, array.toByteArray(), "failed set preserves bytes");
            expect(IllegalArgumentException.class, () -> array.fill(value));
            bytesEqual(before, array.toByteArray(), "failed whole fill preserves bytes");
            expect(IllegalArgumentException.class, () -> array.fill(1, 4, value));
            bytesEqual(before, array.toByteArray(), "failed range fill preserves bytes");
            expect(IllegalArgumentException.class, () -> array.fill(3, 3, value));
            bytesEqual(before, array.toByteArray(), "empty fill still validates value");
            for (int position = 0; position < 3; position++) {
                long[] source = {minimum(bits), 0, maximum(bits)};
                source[position] = value;
                expect(IllegalArgumentException.class, () -> array.setAll(1, source, 0, 3));
                bytesEqual(before, array.toByteArray(), "bulk value failure preserves prefix");
            }
        }
    }

    private static void fill(int bits) {
        PackedIntArray array = create(bits, 7);
        array.fill(minimum(bits));
        for (int index = 0; index < array.length(); index++) {
            equal(minimum(bits), array.get(index), "whole fill minimum");
        }
        array.fill(2, 5, maximum(bits));
        for (int index = 0; index < array.length(); index++) {
            long expected = index >= 2 && index < 5 ? maximum(bits) : minimum(bits);
            equal(expected, array.get(index), "partial fill preserves neighbors");
        }
        byte[] before = array.toByteArray();
        array.fill(0, 0, -1);
        array.fill(array.length(), array.length(), 1);
        bytesEqual(before, array.toByteArray(), "empty fill ranges preserve bytes");
        array.fill(-1);
        byte[] allOnes = new byte[(int) array.payloadBytes()];
        Arrays.fill(allOnes, (byte) 0xff);
        bytesEqual(allOnes, array.toByteArray(), "negative whole fill byte pattern");
        array.fill(0);
        bytesEqual(new byte[allOnes.length], array.toByteArray(), "zero whole fill");
        array.fill(-9);
        array.fill(2, 5, 0);
        longsEqual(new long[] {-9, -9, 0, 0, 0, -9, -9}, array.toLongArray(),
                "partial zero fill preserves nonzero neighbors");
    }

    private static void byteOwnership(int bits) {
        expect(NullPointerException.class, () -> decode(bits, null));
        for (int length : new int[] {1, bits / 8 - 1, bits / 8 + 1, bits / 4 + 1}) {
            expect(IllegalArgumentException.class, () -> decode(bits, new byte[length]));
        }
        byte[] source = oracleEncode(-123_456_789L, bits / 8);
        byte[] original = source.clone();
        PackedIntArray array = decode(bits, source);
        Arrays.fill(source, (byte) 0);
        equal(-123_456_789L, array.get(0), "import owns a defensive copy");
        bytesEqual(original, array.toByteArray(), "source mutation cannot change bytes");
        byte[] exported = array.toByteArray();
        Arrays.fill(exported, (byte) 0x55);
        bytesEqual(original, array.toByteArray(), "export mutation cannot change bytes");
        long[] decoded = array.toLongArray();
        decoded[0] = 0;
        equal(-123_456_789L, array.get(0), "decoded export is independent");
        array.set(0, 123_456_789L);
        bytesEqual(new byte[source.length], source, "collection write cannot change source");
        equal(0, decoded[0], "collection write cannot change decoded export");
    }

    private static void bulkOperations(int bits) {
        PackedIntArray array = create(bits, 7);
        array.fill(-9);
        long[] source = {Long.MIN_VALUE, minimum(bits), 5_000_000_000L, maximum(bits),
                Long.MAX_VALUE};
        array.setAll(2, source, 1, 3);
        long[] expected = {-9, -9, minimum(bits), 5_000_000_000L, maximum(bits), -9, -9};
        longsEqual(expected, array.toLongArray(), "bulk import respects source range");
        source[1] = 0;
        equal(minimum(bits), array.get(2), "bulk source is not retained");
        long[] destination = {91, 92, 93, 94, 95, 96};
        array.getAll(2, destination, 1, 3);
        longsEqual(new long[] {91, minimum(bits), 5_000_000_000L, maximum(bits), 95, 96},
                destination, "bulk export preserves outside destination");
        destination[1] = 0;
        equal(minimum(bits), array.get(2), "bulk destination is independent");
        array.setAll(array.length(), source, source.length, 0);
        array.getAll(array.length(), destination, destination.length, 0);
        longsEqual(expected, array.toLongArray(), "zero-count bulk operation preserves data");
        long[] all = new long[array.length()];
        array.getAll(0, all, 0, all.length);
        longsEqual(expected, all, "full bulk export");
    }

    private static void bulkFailureAtomicity(int bits) {
        PackedIntArray array = create(bits, 7);
        array.fill(23);
        byte[] before = array.toByteArray();
        expect(NullPointerException.class, () -> array.setAll(-1, null, -1, -1));
        expect(NullPointerException.class, () -> array.getAll(-1, null, -1, -1));
        long[] source = {1, 2, 3, 4, 5};
        long[] destination = {71, 72, 73, 74, 75};
        long[] destinationBefore = destination.clone();
        for (int[] range : new int[][] {
            {-1, 0, 1}, {7, 0, 1}, {0, -1, 1}, {0, 5, 1}, {0, 0, -1},
            {6, 0, 2}, {0, 4, 2}, {0, 0, Integer.MAX_VALUE},
            {Integer.MAX_VALUE, 0, 0}, {0, Integer.MAX_VALUE, 0}
        }) {
            expect(IndexOutOfBoundsException.class,
                    () -> array.setAll(range[0], source, range[1], range[2]));
            bytesEqual(before, array.toByteArray(), "invalid bulk import preserves bytes");
            expect(IndexOutOfBoundsException.class,
                    () -> array.getAll(range[0], destination, range[1], range[2]));
            longsEqual(destinationBefore, destination, "invalid bulk export preserves target");
        }
        long[] invalid = {1, 2, maximum(bits) + 1};
        expect(IllegalArgumentException.class, () -> array.setAll(1, invalid, 0, 3));
        bytesEqual(before, array.toByteArray(), "invalid final value cannot commit prefix");
        expect(IndexOutOfBoundsException.class, () -> array.setAll(-1, invalid, 0, 3));
        bytesEqual(before, array.toByteArray(), "range validation precedes numeric validation");
    }

    private static void overlapAndCopy(int bits) {
        long[] initial = {1, 2, 3, 4, 5, 6, 7, 8};
        PackedIntArray array = create(bits, initial.length);
        array.setAll(0, initial, 0, initial.length);
        array.copyTo(0, array, 2, 6);
        longsEqual(new long[] {1, 2, 1, 2, 3, 4, 5, 6}, array.toLongArray(),
                "right overlap has memmove semantics");
        array.setAll(0, initial, 0, initial.length);
        array.copyTo(2, array, 0, 6);
        longsEqual(new long[] {3, 4, 5, 6, 7, 8, 7, 8}, array.toLongArray(),
                "left overlap has memmove semantics");
        array.setAll(0, initial, 0, initial.length);
        array.copyTo(0, array, 0, initial.length);
        longsEqual(initial, array.toLongArray(), "identical copy range is stable");
        array.copyTo(array.length(), array, array.length(), 0);
        longsEqual(initial, array.toLongArray(), "zero-count self-copy is stable");
        PackedIntArray destination = create(bits, 8);
        destination.fill(minimum(bits));
        array.copyTo(2, destination, 1, 4);
        longsEqual(new long[] {
            minimum(bits), 3, 4, 5, 6, minimum(bits), minimum(bits), minimum(bits)
        }, destination.toLongArray(), "external copy preserves surrounding values");
        longsEqual(initial, array.toLongArray(), "external copy preserves source");
        destination.set(1, 0);
        equal(3, array.get(2), "copy has independent ownership");
    }

    private static void copyFailureAtomicity(int bits) {
        PackedIntArray source = create(bits, 5);
        PackedIntArray destination = create(bits, 5);
        source.fill(minimum(bits));
        destination.fill(maximum(bits));
        byte[] sourceBefore = source.toByteArray();
        byte[] destinationBefore = destination.toByteArray();
        expect(NullPointerException.class, () -> source.copyTo(-1, null, -1, -1));
        for (int[] range : new int[][] {
            {-1, 0, 1}, {5, 0, 1}, {0, -1, 1}, {0, 5, 1}, {0, 0, -1},
            {4, 0, 2}, {0, 4, 2}, {0, 0, Integer.MAX_VALUE},
            {Integer.MAX_VALUE, 0, 0}, {0, Integer.MAX_VALUE, 0}
        }) {
            expect(IndexOutOfBoundsException.class,
                    () -> source.copyTo(range[0], destination, range[1], range[2]));
            bytesEqual(sourceBefore, source.toByteArray(), "failed copy preserves source");
            bytesEqual(destinationBefore, destination.toByteArray(),
                    "failed copy preserves destination");
        }
        PackedIntArray otherWidth = create(bits == 40 ? 48 : 40, 5);
        otherWidth.fill(-91);
        byte[] otherBefore = otherWidth.toByteArray();
        expect(IllegalArgumentException.class, () -> source.copyTo(0, otherWidth, 0, 5));
        expect(IllegalArgumentException.class, () -> source.copyTo(5, otherWidth, 5, 0));
        bytesEqual(otherBefore, otherWidth.toByteArray(), "cross-width copy preserves target");
        bytesEqual(sourceBefore, source.toByteArray(), "cross-width copy preserves source");
    }

    private static void emptyOperations(int bits) {
        PackedIntArray array = create(bits, 0);
        array.fill(minimum(bits));
        array.fill(0, 0, maximum(bits));
        array.setAll(0, new long[0], 0, 0);
        array.getAll(0, new long[0], 0, 0);
        array.copyTo(0, create(bits, 0), 0, 0);
        equal(0, array.length(), "empty length");
        equal(0, array.payloadBytes(), "empty payload");
        equal(0, array.toByteArray().length, "empty byte export");
        equal(0, array.toLongArray().length, "empty long export");
        equal(0, decode(bits, new byte[0]).length(), "empty import");
        expect(IndexOutOfBoundsException.class, () -> array.get(0));
        expect(IndexOutOfBoundsException.class, () -> array.set(0, 0));
        expect(IllegalArgumentException.class, () -> array.fill(maximum(bits) + 1));
        expect(IllegalArgumentException.class, () -> array.fill(0, 0, minimum(bits) - 1));
        PackedIntArray different = create(bits == 40 ? 48 : 40, 0);
        expect(IllegalArgumentException.class, () -> array.copyTo(0, different, 0, 0));
        equal(0, array.payloadBytes(), "empty failures preserve payload");
    }

    private static PackedIntArray create(int bits, int length) {
        return switch (bits) {
            case 40 -> new Int40Array(length);
            case 48 -> new Int48Array(length);
            case 56 -> new Int56Array(length);
            default -> throw new AssertionError("unsupported test width: " + bits);
        };
    }

    private static PackedIntArray decode(int bits, byte[] packed) {
        return switch (bits) {
            case 40 -> Int40Array.fromByteArray(packed);
            case 48 -> Int48Array.fromByteArray(packed);
            case 56 -> Int56Array.fromByteArray(packed);
            default -> throw new AssertionError("unsupported test width: " + bits);
        };
    }

    private static long minimum(int bits) {
        return ONE.shiftLeft(bits - 1).negate().longValueExact();
    }

    private static long maximum(int bits) {
        return ONE.shiftLeft(bits - 1).subtract(ONE).longValueExact();
    }

    private static long signed(BigInteger raw, int bits) {
        BigInteger value = raw.testBit(bits - 1) ? raw.subtract(ONE.shiftLeft(bits)) : raw;
        return value.longValueExact();
    }

    private static byte[] oracleEncode(long value, int width) {
        BigInteger raw = BigInteger.valueOf(value).mod(ONE.shiftLeft(width * 8));
        byte[] bigEndian = raw.toByteArray();
        byte[] littleEndian = new byte[width];
        for (int index = 0; index < width; index++) {
            int source = bigEndian.length - 1 - index;
            if (source >= 0) {
                littleEndian[index] = bigEndian[source];
            }
        }
        return littleEndian;
    }

    private static long oracleDecode(byte[] littleEndian) {
        byte[] positiveBigEndian = new byte[littleEndian.length + 1];
        for (int index = 0; index < littleEndian.length; index++) {
            positiveBigEndian[positiveBigEndian.length - 1 - index] = littleEndian[index];
        }
        return signed(new BigInteger(positiveBigEndian), littleEndian.length * 8);
    }

    private static byte[] elementBytes(byte[] packed, int index, int width) {
        return Arrays.copyOfRange(packed, index * width, (index + 1) * width);
    }

    private static void runCase(String name, Runnable body) {
        currentCase = name;
        body.run();
        cases++;
    }

    private static void expect(Class<? extends Throwable> expected, Runnable body) {
        assertions++;
        try {
            body.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError(currentCase + ": expected " + expected.getName()
                    + ", received " + actual.getClass().getName(), actual);
        }
        throw new AssertionError(currentCase + ": expected " + expected.getName());
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(currentCase + ": " + message);
        }
    }

    private static void equal(long expected, long actual, String message) {
        check(expected == actual, message + "; expected=" + expected + ", actual=" + actual);
    }

    private static void bytesEqual(byte[] expected, byte[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
    }

    private static void longsEqual(long[] expected, long[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
    }
}
