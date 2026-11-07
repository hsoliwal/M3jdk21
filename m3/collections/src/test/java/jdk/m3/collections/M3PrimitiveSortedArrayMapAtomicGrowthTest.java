// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Regression proof for two-phase exact-width key/value array growth. */
final class M3PrimitiveSortedArrayMapAtomicGrowthTest {
    @Test
    void sixtyFourKindPairsRefusePartialGrowthAndRetainOriginalEntries() {
        for (PrimitiveKind keyKind : PrimitiveKind.values()) {
            for (PrimitiveKind valueKind : PrimitiveKind.values()) {
                M3PrimitiveSortedArrayMap map = new M3PrimitiveSortedArrayMap(keyKind, valueKind, 1);
                map.putBits(1, 2, -1);
                Object oldKeys = map.keyArray();
                Object oldValues = map.valueArray();
                long payload = map.payloadBytes();
                AtomicInteger attempts = new AtomicInteger();
                assertThrows(OutOfMemoryError.class, () -> map.ensure(2, (kind, source, capacity) -> {
                    if (attempts.incrementAndGet() == 2) throw new OutOfMemoryError("value lane");
                    return kind.copyOf(source, capacity);
                }));
                assertEquals(1, map.size());
                assertEquals(1, map.capacity());
                assertEquals(2, attempts.get());
                assertEquals(payload, map.payloadBytes());
                assertSame(oldKeys, map.keyArray());
                assertSame(oldValues, map.valueArray());
                assertTrue(map.containsKeyBits(1));
                assertEquals(valueKind.storageBits(2), map.getOrDefaultBits(1, -1));
                map.ensure(2, PrimitiveKind::copyOf);
                assertEquals(2, map.capacity());
                assertEquals(valueKind.storageBits(2), map.getOrDefaultBits(1, -1));
            }
        }
    }

    @Test
    void incorrectlyTypedAndShortCopiesCannotCorruptSortedMap() {
        M3PrimitiveSortedArrayMap map =
                new M3PrimitiveSortedArrayMap(PrimitiveKind.INT, PrimitiveKind.LONG, 1);
        map.putBits(8, 80, -1);
        Object keys = map.keyArray();
        Object values = map.valueArray();
        assertThrows(IllegalStateException.class, () -> map.ensure(2,
                (kind, source, capacity) -> new double[capacity]));
        assertThrows(IllegalStateException.class, () -> map.ensure(2,
                (kind, source, capacity) -> kind.copyOf(source, capacity - 1)));
        assertThrows(NullPointerException.class, () -> map.ensure(2,
                (kind, source, capacity) -> null));
        assertSame(keys, map.keyArray());
        assertSame(values, map.valueArray());
        assertEquals(80, map.getOrDefaultBits(8, -1));
        map.putBits(9, 90, -1);
        assertEquals(90, map.getOrDefaultBits(9, -1));
        assertEquals(2, map.size());
    }

    @Test
    void sortedInsertAndRemoveRemainEquivalentToTreeMap() {
        M3PrimitiveSortedArrayMap map =
                new M3PrimitiveSortedArrayMap(PrimitiveKind.LONG, PrimitiveKind.LONG, 1);
        TreeMap<Long, Long> oracle = new TreeMap<>();
        for (int step = 0; step < 1_024; step++) {
            long key = (step * 73L + 19L) % 251L - 125L;
            if (step % 3 == 0) {
                Long previous = oracle.remove(key);
                long expected = previous == null ? -777L : previous;
                assertEquals(expected, map.removeOrDefaultBits(key, -777L));
            } else {
                long value = step * 17L - 4L;
                Long previous = oracle.put(key, value);
                assertEquals(previous == null ? -777L : previous,
                        map.putBits(key, value, -777L));
            }
            assertEquals(oracle.size(), map.size());
            int index = 0;
            for (var entry : oracle.entrySet()) {
                assertEquals(index, map.binarySearchKeyBits(entry.getKey()));
                assertEquals(entry.getValue().longValue(),
                        map.getOrDefaultBits(entry.getKey(), -777));
                index++;
            }
        }
    }
}
