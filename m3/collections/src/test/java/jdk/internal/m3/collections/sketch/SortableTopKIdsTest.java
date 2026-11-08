/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.sketch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.SortSignalLane;
import jdk.internal.m3.collections.SortableFirstSignalOrder;

/** Maven/JUnit adapter reusing the standalone Java-only differential contract oracle. */
final class SortableTopKIdsTest {
    @Test void randomizedDifferential() { SortableTopKIdsContract.randomDifferential(); }
    @Test void duplicateStableIds() { SortableTopKIdsContract.duplicatesStableId(); }
    @Test void signedComparatorNoNegationOverflow() { SortableTopKIdsContract.reversedAndMinValueComparator(); }
    @Test void unverifiedFirstSignalUsesExactOracle() { SortableTopKIdsContract.unverifiedFirstSignalCannotOverrideExact(); }
    @Test void windowAndInputPreservation() { SortableTopKIdsContract.callerOwnedWindowAndSourceIdentity(); }
    @Test void invalidGeometryIsAtomic() { SortableTopKIdsContract.validatesBeforeWriting(); }
    @Test void zeroWorkIsZeroCompare() { SortableTopKIdsContract.zeroWorkNeverCallsComparator(); }
    @Test void sparseLargeIdRange() { SortableTopKIdsContract.largeSparseIdsAndLargeWorkset(); }

    @Test void integratesWithExistingM3SortableFirstSignalLane() {
        long[] values = {50, 10, 40, 20, 30};
        var signals = new SortSignalLane();
        var order = new SortableFirstSignalOrder(signals,
                id -> -values[id], id -> 0,
                (left, right) -> Long.compare(values[left], values[right]));
        int[] ids = {0, 1, 2, 3, 4};
        int[] result = SortableTopKIds.smallest(ids, 0, ids.length, 3, order);
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
                SortableTopKIds.smallest(ids, 0, ids.length, 3, order));
    }
}
