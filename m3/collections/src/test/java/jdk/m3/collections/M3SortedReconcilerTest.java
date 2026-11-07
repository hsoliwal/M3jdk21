// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class M3SortedReconcilerTest {
    @Test
    void classifiesMatchedMismatchedAndOneSidedRowsInLinearMergeOrder() {
        var result = M3SortedReconciler.reconcile(
                List.of(
                        new M3SortedReconciler.Item<>(1, "A"),
                        new M3SortedReconciler.Item<>(2, "B"),
                        new M3SortedReconciler.Item<>(4, "D-left")),
                List.of(
                        new M3SortedReconciler.Item<>(1, "A"),
                        new M3SortedReconciler.Item<>(3, "C"),
                        new M3SortedReconciler.Item<>(4, "D-right")),
                Comparator.naturalOrder(),
                Objects::equals);

        assertEquals(List.of(2), result.onlyLeft().stream().map(M3SortedReconciler.Item::key).toList());
        assertEquals(List.of(3), result.onlyRight().stream().map(M3SortedReconciler.Item::key).toList());
        assertEquals(List.of(1), result.matched().stream().map(M3SortedReconciler.Item::key).toList());
        assertEquals(List.of(4), result.mismatched().stream().map(M3SortedReconciler.Difference::key).toList());
        assertFalse(result.equivalent());
    }

    @Test
    void rejectsUnsortedOrDuplicateKeysInsteadOfProducingAmbiguousReceipt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3SortedReconciler.reconcile(
                        List.of(
                                new M3SortedReconciler.Item<>(2, "B"),
                                new M3SortedReconciler.Item<>(1, "A")),
                        List.of(),
                        Comparator.<Integer>naturalOrder(),
                        Objects::equals));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3SortedReconciler.reconcile(
                        List.of(
                                new M3SortedReconciler.Item<>(1, "A"),
                                new M3SortedReconciler.Item<>(1, "B")),
                        List.of(),
                        Comparator.<Integer>naturalOrder(),
                        Objects::equals));
    }
}
