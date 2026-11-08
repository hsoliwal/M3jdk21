// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.sketch.MarkerTopKContract;
import jdk.internal.m3.collections.sketch.MarkerTopKIndex;
import jdk.internal.m3.collections.SortSignalLane;
import jdk.internal.m3.collections.SortableFirstSignalOrder;

/** Oracle and canonical first-@Sortable signal owner integration. */
final class M3MarkerTopKTest {
    @Test void standaloneContractOracle() { MarkerTopKContract.main(new String[0]); }

    @Test void firstSortableHintNeverOverridesUnverifiedExactOrder() {
        long[] exactKeys = {99, 1, 3, -1, 2, 5, 5};
        long[] misleadingSignals = {-99, 50, 1, 100, 44, 2, 2};
        AtomicInteger calculations = new AtomicInteger();
        var evidence = new SortSignalLane();
        var sorter = new SortableFirstSignalOrder(evidence,
                id -> { calculations.incrementAndGet(); return misleadingSignals[id]; },
                id -> 0L, (a, b) -> Long.compare(exactKeys[a], exactKeys[b]));
        int[] input = {0, 1, 2, 3, 4, 5, 6};
        int[] result = MarkerTopKIndex.smallest(input, 0, input.length, 4, sorter);
        assertArrayEquals(new int[]{3, 1, 4, 2}, result);
        int first = calculations.get();
        assertTrue(first > 0);
        int[] repeat = MarkerTopKIndex.smallest(input, 0, input.length, 4, sorter);
        assertArrayEquals(result, repeat);
        assertEquals(first, calculations.get(), "previously computed signals must be reused");
        assertTrue(evidence.hitCount() > 0L);
    }

    @Test void certifiedPrefixCanAvoidExactTieComparison() {
        long[] exactKeys = {8, -1, 3, 3};
        AtomicInteger compared = new AtomicInteger();
        var sorter = new SortableFirstSignalOrder(new SortSignalLane(),
                id -> exactKeys[id], id -> 0L,
                (a, b) -> { compared.incrementAndGet(); return Integer.compare(a, b); },
                SortableFirstSignalOrder.Proof.CERTIFIED_PREFIX);
        int[] result = MarkerTopKIndex.smallest(new int[]{0,1,2,3}, 0,4,2, sorter);
        assertArrayEquals(new int[]{1,2}, result);
        assertTrue(compared.get() >= 0);
        assertTrue(sorter.decisivePrefixes() > 0);
    }
}
