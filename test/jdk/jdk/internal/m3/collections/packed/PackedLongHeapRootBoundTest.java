// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Differential parity against an independent linear reference after every heap mutation. */
final class PackedLongHeapRootBoundTest {

    @Test void minMaxRootBoundsAndDuplicates() {
        for (boolean min : new boolean[] {true, false}) {
            PackedLongHeap heap = new PackedLongHeap(min, 1);
            assertFalse(heap.contains(Long.MIN_VALUE));
            for (long x : new long[] {10, 10, 20, -20, 0}) heap.offer(x);
            for (long x : new long[] {10, 20, -20, 0}) assertTrue(heap.contains(x));
            assertFalse(heap.contains(11));
            assertFalse(heap.contains(min ? Long.MIN_VALUE : Long.MAX_VALUE));
            heap.clear();
            heap.offer(10);
            assertFalse(heap.contains(min ? 9 : 11));
            assertTrue(heap.contains(10));
        }
    }

    @Test void differentialAcrossOperationsAndSignedExtrema() {
        long checked = 0;
        for (boolean min : new boolean[] {true, false}) {
            PackedLongHeap heap = new PackedLongHeap(min, 1);
            Random random = new Random(min ? 7013L : 7017L);
            for (int step = 0; step < 12_000; step++) {
                long next = step % 31 == 0 ? Long.MIN_VALUE
                    : step % 37 == 0 ? Long.MAX_VALUE : random.nextLong();
                switch (step % 11) {
                    case 0 -> heap.clear();
                    case 1, 2, 3, 4, 5 -> heap.offer(next);
                    case 6 -> { if (heap.size() > 0) heap.poll(); }
                    case 7 -> { if (heap.size() > 0) heap.replaceTop(next); }
                    case 8 -> heap.addAll(new long[] {next, next, random.nextLong()});
                    case 9 -> heap.filterInPlace(value -> (value & 1L) == 0L);
                    default -> { /* retain */ }
                }
                long[] snapshot = heap.toArray();
                for (int i = 0; i < 12; i++) {
                    long probe = switch (i % 6) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> next;
                        case 3 -> snapshot.length == 0 ? 0L
                            : snapshot[random.nextInt(snapshot.length)];
                        case 4 -> random.nextLong();
                        default -> snapshot.length == 0 ? 0L : snapshot[0];
                    };
                    boolean expected = false;
                    for (long item : snapshot) if (item == probe) { expected = true; break; }
                    assertEquals(expected, heap.contains(probe),
                        "min=" + min + ", step=" + step + ", probe=" + i);
                    checked++;
                }
            }
        }
        assertEquals(288_000L, checked);
    }
    @Test void callbackReadsKeepLinearSemanticsUntilHeapOrderIsRestored() {
        for (boolean min : new boolean[] {true, false}) {
            long first = min ? 1L : 9L;
            long otherChild = min ? 2L : 8L;
            long next = min ? 3L : 7L;
            long[] values = min ? new long[] {1, 3, 2, 5, 4}
                : new long[] {9, 7, 8, 5, 6};

            PackedLongHeap heap = new PackedLongHeap(min, 1);
            for (long value : values) heap.offer(value);
            assertEquals(1, heap.filterInPlace(value -> {
                if (value == otherChild) {
                    // The first accepted element overwrote the old root, but an
                    // unread sibling is still in the original visible logical range.
                    assertEquals(next, heap.peek());
                    assertTrue(heap.contains(otherChild));
                    assertFalse(heap.contains(first));
                }
                return value != first;
            }));
            assertTrue(heap.contains(otherChild));
            assertTrue(heap.contains(next));
            assertFalse(heap.contains(first));

            PackedLongHeap exceptional = new PackedLongHeap(min, 1);
            for (long value : values) exceptional.offer(value);
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> exceptional.filterInPlace(value -> {
                    if (value == otherChild) {
                        assertTrue(exceptional.contains(otherChild));
                        throw new IllegalStateException("expected");
                    }
                    return value != first;
                }));
            assertEquals("expected", thrown.getMessage());
            assertTrue(exceptional.contains(otherChild));
            assertFalse(exceptional.contains(first));
        }
    }

}
