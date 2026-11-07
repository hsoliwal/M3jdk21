// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Differential oracle: JDK primitive binary search vs the previous per-kind search loop. */
final class M3SortedPrimitiveBinarySearchTest {
    private static final long[] EDGE_BITS = {
        0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE,
        0x8000_0000_0000_0000L, 0x7ff8_0000_0000_0001L,
        0x7ff0_0000_0000_0001L, 0x7ff0_0000_0000_0000L,
        0xfff0_0000_0000_0000L, 0x7fc0_0001L,
        0x7f80_0001L, 0x8000_0000L
    };

    /**
     * Frozen reference algorithm from prior M3PrimitiveSortedArraySet/Map.search.
     * This must not call kind.binarySearch, so a regression cannot compare itself.
     */
    private static int legacySearch(PrimitiveKind kind, Object lanes, int size, long wanted) {
        int low = 0;
        int high = size - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int order = kind.compareBits(kind.readBits(lanes, middle), wanted);
            if (order < 0) low = middle + 1;
            else if (order > 0) high = middle - 1;
            else return middle;
        }
        return -(low + 1);
    }

    @Test
    void allEightPrimitiveKindsHaveExactIndicesAndInsertionPoints() {
        Random random = new Random(0x4d33_5352L);
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            M3PrimitiveSortedArraySet set = new M3PrimitiveSortedArraySet(kind, 1);
            M3PrimitiveSortedArrayMap map =
                    new M3PrimitiveSortedArrayMap(kind, PrimitiveKind.LONG, 1);
            assertEquals(-1, set.binarySearchBits(0L));
            assertEquals(-1, map.binarySearchKeyBits(0L));
            for (int iteration = 0; iteration < 120; iteration++) {
                long offered = (iteration % 3 == 0)
                        ? EDGE_BITS[random.nextInt(EDGE_BITS.length)]
                        : random.nextInt(97) - 48L;
                set.addBits(offered);
                map.putBits(offered, iteration, -1L);
                assertEquals(set.size(), map.size());
                for (int q = 0; q < 80; q++) {
                    long query = (q % 3 == 0)
                            ? EDGE_BITS[random.nextInt(EDGE_BITS.length)]
                            : random.nextInt(197) - 98L;
                    int expected = legacySearch(
                            kind, set.backingArray(), set.size(), query);
                    assertEquals(expected, set.binarySearchBits(query));
                    assertEquals(expected, map.binarySearchKeyBits(query));
                    assertEquals(expected >= 0, set.containsBits(query));
                    assertEquals(expected >= 0, map.containsKeyBits(query));
                }
            }
        }
    }

    @Test
    void allSixtyFourMapPrimitivePairingsPreserveExactValueBits() {
        long[] keys = {-9L, 0L, 1L, 4L, Long.MIN_VALUE, Long.MAX_VALUE,
                       0x7ff8_0000_0000_0001L, 0x8000_0000_0000_0000L};
        for (PrimitiveKind keyKind : PrimitiveKind.values()) {
            for (PrimitiveKind valueKind : PrimitiveKind.values()) {
                M3PrimitiveSortedArrayMap map =
                        new M3PrimitiveSortedArrayMap(keyKind, valueKind, 1);
                for (int i = 0; i < keys.length; i++) {
                    long valueBits = EDGE_BITS[(i + valueKind.ordinal()) % EDGE_BITS.length];
                    map.putBits(keys[i], valueBits, -1L);
                    assertEquals(valueKind.storageBits(valueBits),
                            map.getOrDefaultBits(keys[i], -1L));
                    int expected = legacySearch(
                            keyKind, map.keyArray(), map.size(), keys[i]);
                    assertEquals(expected, map.binarySearchKeyBits(keys[i]));
                }
                for (int i = 0; i < 35; i++) {
                    long missingOrPresent = i - 17L;
                    int expected = legacySearch(keyKind, map.keyArray(),
                            map.size(), missingOrPresent);
                    assertEquals(expected, map.binarySearchKeyBits(missingOrPresent));
                }
            }
        }
    }

    @Test
    void nanPayloadsAndSignedZeroRetainFloatAndDoubleOrdering() {
        for (PrimitiveKind kind : new PrimitiveKind[] {
                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE }) {
            M3PrimitiveSortedArraySet set = new M3PrimitiveSortedArraySet(kind, 1);
            long negativeZero = kind == PrimitiveKind.FLOAT
                    ? 0x8000_0000L : 0x8000_0000_0000_0000L;
            long positiveZero = 0L;
            long nanA = kind == PrimitiveKind.FLOAT
                    ? 0x7fc0_0001L : 0x7ff8_0000_0000_0001L;
            long nanB = kind == PrimitiveKind.FLOAT
                    ? 0x7fc0_0002L : 0x7ff8_0000_0000_0002L;
            assertTrue(set.addBits(negativeZero));
            assertTrue(set.addBits(positiveZero));
            assertTrue(set.addBits(nanA));
            assertFalse(set.addBits(nanB), "distinct NaN payloads compare equal");
            assertEquals(3, set.size());
            assertEquals(0, set.binarySearchBits(negativeZero));
            assertEquals(1, set.binarySearchBits(positiveZero));
            assertEquals(2, set.binarySearchBits(nanA));
            assertEquals(2, set.binarySearchBits(nanB));
        }
    }

    @Test
    void mapAndSetLongMutationsMatchIndependentJdkSortedOwners() {
        Random random = new Random(0x2121_5151L);
        M3PrimitiveSortedArraySet set =
                new M3PrimitiveSortedArraySet(PrimitiveKind.LONG, 1);
        M3PrimitiveSortedArrayMap map =
                new M3PrimitiveSortedArrayMap(PrimitiveKind.LONG, PrimitiveKind.LONG, 1);
        TreeSet<Long> expectedSet = new TreeSet<>();
        TreeMap<Long, Long> expectedMap = new TreeMap<>();
        for (int operation = 0; operation < 3_072; operation++) {
            long key = random.nextInt(283) - 141L;
            if (operation % 4 == 0) {
                assertEquals(expectedSet.remove(key), set.removeBits(key));
                Long old = expectedMap.remove(key);
                assertEquals(old == null ? -1L : old,
                        map.removeOrDefaultBits(key, -1L));
            } else {
                boolean added = expectedSet.add(key);
                assertEquals(added, set.addBits(key));
                long value = operation * 31L - 9L;
                Long old = expectedMap.put(key, value);
                assertEquals(old == null ? -1L : old, map.putBits(key, value, -1L));
            }
            assertEquals(expectedSet.size(), set.size());
            assertEquals(expectedMap.size(), map.size());
            int index = 0;
            for (Long keyAt : expectedSet) {
                assertEquals(keyAt.longValue(), set.getBits(index));
                assertEquals(index, set.binarySearchBits(keyAt));
                assertEquals(index, map.binarySearchKeyBits(keyAt));
                assertEquals(expectedMap.get(keyAt).longValue(),
                        map.getOrDefaultBits(keyAt, -1L));
                index++;
            }
        }
    }
}
