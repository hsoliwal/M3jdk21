/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections.packed;

import jdk.m3.collections.LongIterator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class PackedHeapExportContractTest {
    private static long[] oracle(long[] values, boolean min) {
        long[] result = values.clone(); Arrays.sort(result);
        if (!min) {
            for (int left = 0, right = result.length - 1; left < right; left++, right--) {
                long value = result[left]; result[left] = result[right]; result[right] = value;
            }
        }
        return result;
    }

    private static void check(PackedLongHeap heap) {
        long[] before = heap.toArray(); int capacity = heap.capacity();
        LongIterator iterator = heap.iterator();
        long[] expected = oracle(before, heap.isMinHeap()); long[] actual = heap.drainOrdered();
        assertArrayEquals(expected, actual); assertArrayEquals(before, heap.toArray());
        assertEquals(before.length, heap.size()); assertEquals(capacity, heap.capacity());
        for (long value : before) { assertEquals(value, iterator.nextLong()); }
        assertFalse(iterator.hasNext()); assertNotSame(actual, heap.drainOrdered());
        Arrays.fill(actual, 42); assertArrayEquals(expected, heap.drainOrdered());
    }

    @Test void bothPolaritiesCoverEmptySingletonDuplicatesExtremesAndGrowth() {
        Random random = new Random(541902);
        for (boolean min : new boolean[] {true, false}) {
            for (int size : new int[] {0, 1, 2, 3, 7, 8, 9, 31, 32, 33, 255, 1024}) {
                var heap = new PackedLongHeap(min, 0);
                for (int i = 0; i < size; i++) {
                    heap.offer(switch (i % 4) {
                        case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE;
                        case 2 -> 0; default -> random.nextLong();
                    });
                }
                check(heap); long[] expected = heap.drainOrdered();
                for (long value : expected) { assertEquals(value, heap.poll()); }
                check(heap); heap.offer(9); check(heap); heap.clear(); check(heap);
            }
        }
    }

    @Test void exportSurvivesBulkReplacementFilteringAndTopKMutations() {
        for (boolean min : new boolean[] {true, false}) {
            var heap = new PackedLongHeap(min);
            heap.addAll(new long[] {5, 2, 7, 1, 8, -1, Long.MIN_VALUE, Long.MAX_VALUE}); check(heap);
            heap.replaceTop(99); check(heap); heap.poll(); check(heap);
            heap.filterInPlace(value -> value % 2 != 0); check(heap);
            for (long value = -30; value < 30; value++) {
                if (min) { heap.offerTop(value, 12); } else { heap.offerBottom(value, 12); }
                check(heap);
            }
        }
    }
}
