/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Preserve exact unsigned Bloom positions for power-of-two and arbitrary dimensions.
 * @modules jdk.m3.collections/jdk.internal.m3.collections.bits
 * @run main/othervm --add-opens jdk.m3.collections/jdk.internal.m3.collections.bits=ALL-UNNAMED PackedLongBloomFilterIndexTest
 */
import jdk.internal.m3.collections.bits.PackedLongBloomFilter;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

public class PackedLongBloomFilterIndexTest {
    private static final BigInteger UNSIGNED_LONG_MASK = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
    private static final int[] DIMENSIONS = {1, 2, 3, 7, 8, 31, 32, 63, 64, 65,
            127, 128, 129, 1000, 1024, 4095, 4096, 65535, 65536};
    private static final int[] HASH_COUNTS = {1, 2, 7, 32};

    public static void main(String[] args) throws Exception {
        verifyExactPositions();
        verifyDimensions();
        System.out.println("PASS PackedLongBloomFilter unsigned indexing: 76 configurations");
    }

    public static void verifyExactPositions() throws Exception {
        Field words = PackedLongBloomFilter.class.getDeclaredField("words");
        words.setAccessible(true);
        Random random = new Random(0x4d33424c4f4f4dL);
        for (int dimension : DIMENSIONS) {
            for (int hashCount : HASH_COUNTS) {
                PackedLongBloomFilter filter = new PackedLongBloomFilter(dimension, hashCount);
                boolean[] expectedBits = new boolean[dimension];
                long[] inserted = new long[80];
                for (int i = 0; i < inserted.length; ++i) {
                    inserted[i] = switch (i % 8) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> 0L;
                        case 3 -> -1L;
                        case 4 -> 1L;
                        default -> random.nextLong();
                    };
                    boolean changed = oracleAdd(expectedBits, hashCount, inserted[i]);
                    require(filter.add(inserted[i]) == changed, "changed-bits result");
                    require(!filter.add(inserted[i]), "duplicate addition must not change bits");
                    assertLayout((long[]) words.get(filter), expectedBits);
                    for (int j = 0; j <= i; ++j) {
                        require(filter.mightContain(inserted[j]), "inserted value cannot be absent");
                    }
                    for (int probe = 0; probe < 12; ++probe) {
                        long value = random.nextLong();
                        require(filter.mightContain(value) == oracleContains(expectedBits, hashCount, value),
                                "unsigned modulo membership mismatch");
                    }
                }
                require(filter.bitSize() == dimension, "requested dimensions unchanged");
                require(filter.hashFunctions() == hashCount, "requested hash count unchanged");
                require(filter.arrayPayloadBytes() == 8L * ((dimension + 63L) >>> 6), "packed payload size");
                filter.clear();
                Arrays.fill(expectedBits, false);
                assertLayout((long[]) words.get(filter), expectedBits);
                for (long value : inserted) require(!filter.mightContain(value), "clear resets membership");
                require(filter.add(Long.MIN_VALUE), "reuse after clear");
            }
        }
    }

    public static void verifyDimensions() {
        for (int bits : new int[] {Integer.MIN_VALUE, -1, 0}) {
            expectInvalid(() -> new PackedLongBloomFilter(bits, 1));
        }
        for (int hashes : new int[] {Integer.MIN_VALUE, -1, 0, 33, Integer.MAX_VALUE}) {
            expectInvalid(() -> new PackedLongBloomFilter(64, hashes));
        }
    }

    private static void assertLayout(long[] actual, boolean[] expected) {
        for (int bit = 0; bit < expected.length; ++bit) {
            require(((actual[bit >>> 6] >>> (bit & 63)) & 1L) == (expected[bit] ? 1L : 0L),
                    "exact retained bit layout mismatch");
        }
        int tail = expected.length & 63;
        if (tail != 0) require((actual[actual.length - 1] >>> tail) == 0L, "unused tail bits");
    }

    private static boolean oracleAdd(boolean[] bits, int hashes, long value) {
        long base = mix(value), step = mix(value ^ 0x9e3779b97f4a7c15L) | 1L;
        boolean changed = false;
        for (int i = 0; i < hashes; ++i) {
            int bit = unsignedPosition(base + i * step, bits.length);
            changed |= !bits[bit];
            bits[bit] = true;
        }
        return changed;
    }

    private static boolean oracleContains(boolean[] bits, int hashes, long value) {
        long base = mix(value), step = mix(value ^ 0x9e3779b97f4a7c15L) | 1L;
        for (int i = 0; i < hashes; ++i) {
            if (!bits[unsignedPosition(base + i * step, bits.length)]) return false;
        }
        return true;
    }

    private static int unsignedPosition(long hash, int dimension) {
        return BigInteger.valueOf(hash).and(UNSIGNED_LONG_MASK)
                .mod(BigInteger.valueOf(dimension)).intValueExact();
    }

    private static long mix(long value) {
        value ^= value >>> 33; value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33; value *= 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }

    private static void require(boolean valid, String description) {
        if (!valid) throw new AssertionError(description);
    }

    private static void expectInvalid(Runnable call) {
        try {
            call.run();
            throw new AssertionError("missing invalid-dimension exception");
        } catch (IllegalArgumentException expected) {
            require("Bloom dimensions".equals(expected.getMessage()), "exception message retained");
        }
    }
}
