// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import static com.synexia.mindex.collections.MIndexCollectionMetadata.Role.ELEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class MIndexWeightsTest {
    @Test
    void prefixAndGroupedSelectionSkipZeroWeights() {
        MIndexIdIndex index = lane(new int[] {4, 1, 4, 4, 1});
        MIndexWeights weights = index.weights(new long[] {0, 3, 2, 0, 5});
        assertEquals(10, weights.total());
        assertEquals(8, weights.weightOfId(1));
        assertEquals(2, weights.weightOfId(4));
        assertEquals(0, weights.weightOfId(9));
        assertEquals(5, weights.sum(1, 4));
        assertEquals(1, weights.select(0));
        assertEquals(1, weights.select(2));
        assertEquals(2, weights.select(3));
        assertEquals(4, weights.select(5));
        assertEquals(4, weights.select(9));
        assertEquals(1, weights.selectId(1, 2));
        assertEquals(4, weights.selectId(1, 3));
        assertEquals(2, weights.selectId(4, 1));
    }

    @Test
    void zeroAndEmptyWeightsHaveNoSelectableOffset() {
        for (int size : new int[] {0, 1, 7}) {
            MIndexWeights weights = lane(new int[size]).weights(new long[size]);
            assertEquals(0, weights.total());
            assertEquals(0, weights.sum(0, size));
            assertThrows(IndexOutOfBoundsException.class, () -> weights.select(0));
            assertThrows(IndexOutOfBoundsException.class, () -> weights.selectId(0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> weights.mutableCopy().select(0));
        }
    }

    @Test
    void negativeAndOverflowWeightsFailExplicitly() {
        MIndexIdIndex index = lane(new int[] {1, 2});
        assertThrows(IllegalArgumentException.class, () -> index.weights(new long[] {1}));
        assertThrows(IllegalArgumentException.class, () -> index.weights(new long[] {1, -1}));
        assertThrows(ArithmeticException.class, () -> index.weights(new long[] {Long.MAX_VALUE, 1}));
        MIndexWeights weights = index.weights(new long[] {Long.MAX_VALUE - 1, 1});
        assertEquals(Long.MAX_VALUE, weights.total());
        assertEquals(1, weights.select(Long.MAX_VALUE - 1));
        assertEquals(0, weights.selectId(1, Long.MAX_VALUE - 2));
        assertThrows(IndexOutOfBoundsException.class, () -> weights.select(Long.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> weights.select(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> weights.selectId(99, 0));
    }

    @Test
    void callbacksAndCallerArraysAreNotRetainedAsMutableWeightPolicies() {
        MIndexIdIndex index = lane(new int[] {1, 2, 1});
        AtomicInteger calls = new AtomicInteger();
        MIndexWeights computed = index.weightsById(id -> { calls.incrementAndGet(); return id; });
        for (int i = 0; i < 100; i++) { assertEquals(2, computed.weightOfId(1)); }
        assertEquals(3, calls.get());
        long[] caller = {2, 3, 4};
        MIndexWeights frozen = index.weights(caller);
        caller[0] = 100;
        assertEquals(9, frozen.total());
        MIndexWeightTree tree = frozen.mutableCopy();
        MIndexWeights snapshot = tree.snapshot();
        tree.set(0, 30);
        assertEquals(9, snapshot.total());
        assertEquals(37, tree.total());
        assertEquals(9, frozen.total());
    }

    @Test
    void failedFenwickUpdatesLeaveAllStateUnchanged() {
        MIndexWeightTree tree = lane(new int[] {0, 1, 2, 3})
                .weights(new long[] {Long.MAX_VALUE - 3, 1, 0, 2}).mutableCopy();
        assertThrows(ArithmeticException.class, () -> tree.set(2, 1));
        assertThrows(IllegalArgumentException.class, () -> tree.set(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.set(4, 2));
        assertEquals(Long.MAX_VALUE, tree.total());
        assertEquals(0, tree.weightAt(2));
        assertEquals(3, tree.sum(1, 4));
        assertEquals(3, tree.select(Long.MAX_VALUE - 1));
        tree.set(0, 0);
        assertEquals(3, tree.total());
        tree.set(2, Long.MAX_VALUE - 3);
        assertEquals(Long.MAX_VALUE, tree.total());
        assertEquals(2, tree.select(Long.MAX_VALUE - 3));
    }

    @Test
    void randomizedPrefixGroupedAndFenwickQueriesMatchLinearReference() {
        Random random = new Random(0x574549474854534cL);
        for (int trial = 0; trial < 100; trial++) {
            int size = 1 + random.nextInt(100);
            int[] ids = random.ints(size, 0, 11).toArray();
            long[] values = random.longs(size, 0, 30).toArray();
            MIndexWeights weights = lane(ids).weights(values);
            MIndexWeightTree tree = weights.mutableCopy();
            verifyFrozen(ids, values, weights);
            for (int operation = 0; operation < 200; operation++) {
                int position = random.nextInt(size);
                values[position] = random.nextInt(100);
                tree.set(position, values[position]);
                long total = sum(values, 0, size);
                assertEquals(total, tree.total());
                int from = random.nextInt(size + 1);
                int to = from + random.nextInt(size - from + 1);
                assertEquals(sum(values, from, to), tree.sum(from, to));
                if (total > 0) {
                    long target = random.nextLong(total);
                    assertEquals(select(values, target), tree.select(target));
                }
            }
            verifyFrozen(ids, values, tree.snapshot());
        }
    }

    private static void verifyFrozen(int[] ids, long[] values, MIndexWeights weights) {
        assertEquals(sum(values, 0, values.length), weights.total());
        for (int id = -1; id <= 11; id++) {
            long total = 0;
            for (int position = 0; position < ids.length; position++) {
                if (ids[position] == id) {
                    if (values[position] > 0) {
                        assertEquals(position, weights.selectId(id, total));
                        assertEquals(position, weights.selectId(id, total + values[position] - 1));
                    }
                    total += values[position];
                }
            }
            assertEquals(total, weights.weightOfId(id));
        }
        long prefix = 0;
        for (int position = 0; position < values.length; position++) {
            if (values[position] > 0) {
                assertEquals(position, weights.select(prefix));
                assertEquals(position, weights.select(prefix + values[position] - 1));
            }
            prefix += values[position];
        }
    }

    private static long sum(long[] values, int from, int to) {
        long sum = 0;
        for (int i = from; i < to; i++) { sum += values[i]; }
        return sum;
    }

    private static int select(long[] values, long target) {
        for (int i = 0; i < values.length; i++) {
            if (target < values[i]) { return i; }
            target -= values[i];
        }
        throw new AssertionError("invalid reference offset");
    }

    private static MIndexIdIndex lane(int[] ids) {
        MIndexCompositeIndex store = new MIndexCompositeIndex();
        return MIndexFrozenList.ofIds(new MIndexPrecomputeTest.NumericSpace(), store, ids)
                .precompute(new MIndexCollectionPrecompute(store, 1, 1_000_000)).column(ELEMENT);
    }
}
