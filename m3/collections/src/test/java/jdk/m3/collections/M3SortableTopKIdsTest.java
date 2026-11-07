/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Maven/JUnit adapter reusing the standalone Java-only differential contract oracle. */
final class M3SortableTopKIdsTest {
    @Test void randomizedDifferential() { M3SortableTopKIdsContract.randomDifferential(); }
    @Test void duplicateStableIds() { M3SortableTopKIdsContract.duplicatesStableId(); }
    @Test void signedComparatorNoNegationOverflow() { M3SortableTopKIdsContract.reversedAndMinValueComparator(); }
    @Test void unverifiedFirstSignalUsesExactOracle() { M3SortableTopKIdsContract.unverifiedFirstSignalCannotOverrideExact(); }
    @Test void windowAndInputPreservation() { M3SortableTopKIdsContract.callerOwnedWindowAndSourceIdentity(); }
    @Test void invalidGeometryIsAtomic() { M3SortableTopKIdsContract.validatesBeforeWriting(); }
    @Test void zeroWorkIsZeroCompare() { M3SortableTopKIdsContract.zeroWorkNeverCallsComparator(); }
    @Test void sparseLargeIdRange() { M3SortableTopKIdsContract.largeSparseIdsAndLargeWorkset(); }

    @Test void integratesWithExistingM3SortableFirstSignalLane() {
        long[] values = {50, 10, 40, 20, 30};
        var signals = new M3SortSignalLane();
        var order = new M3SortableFirstSignalOrder(signals,
                id -> -values[id], id -> 0,
                (left, right) -> Long.compare(values[left], values[right]));
        int[] ids = {0, 1, 2, 3, 4};
        int[] result = M3SortableTopKIds.smallest(ids, 0, ids.length, 3, order);
        assertArrayEquals(new int[]{1, 3, 4}, result);
        assertTrue(order.exactComparisons() > 0,
                "unverified provisional signal cannot replace exact order");
        assertTrue(signals.hitCount() > 0,
                "comparison evidence must be retained and reused across heap work");
        assertTrue(signals.computationCount() <= ids.length);
        // A post-mutation dependency revision must invalidate only that ID's signal.
        values[1] = 200;
        signals.invalidate(1);
        assertArrayEquals(new int[]{3, 4, 2},
                M3SortableTopKIds.smallest(ids, 0, ids.length, 3, order));
    }
}
