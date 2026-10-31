/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3IndexMaskCompactor;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real-provider JUnit compatibility gate in the optional M3 collections interop module. */
public final class M3FiveFamilyPreparedSelectionTest {
    @Test void deterministicDifferentialCorpus() {
        M3FiveFamilyPreparedSelectionSelfTest.main(new String[0]);
    }

    @Test void duplicateOccurrencesAndFiveSeparateContracts() {
        String[] keys = {"a", "b", "b", "a"};
        Integer[] values = {7, 11, 11, 9};
        int[] ordinals = {0, 1, 1, 2, 3}; // repeated physical slot is a separate occurrence
        byte[] states = {1, 3, 3, 0};
        var source = M3FiveLibraryIndexViews.capture(keys, values, ordinals, states);
        AtomicInteger calls = new AtomicInteger();
        var accepted = M3FiveFamilyPreparedSelection.where(source, (key, value) -> {
            calls.incrementAndGet();
            return value > 8;
        });
        assertEquals(5, calls.get());
        assertEquals(4, accepted.size());
        assertSame(accepted.projection(), accepted.projection());
        var selected = accepted.projection();
        assertEquals(List.of(11, 11, 11, 9), selected.asJdkList());
        assertEquals(4, selected.asGuavaListMultimap().size());
        assertEquals(4, selected.asEclipseListMultimap().size());
        assertEquals(4, selected.asApacheListValuedMap().size());
        assertEquals(2, selected.asSpringMultiValueMap().size());
        assertEquals(List.of(11, 11, 11), selected.asSpringMultiValueMap().get("b"));
        assertEquals(2, selected.asJdkMapLastWins().size());
        assertEquals(5, calls.get(), "preparing other provider results must never rerun predicate");
        assertEquals(List.of(9, 11, 11, 11),
                selected.sortedByKey(String::compareTo).asJdkList());
        assertEquals(List.of(11, 11), selected.slice(0, 2).asJdkList());
    }

    @Test void bitmapDomainDefensiveCopyAndNativeRefusal() {
        Integer[] keys = {1, 2}; Integer[] values = {10, 20};
        var first = M3FiveLibraryIndexViews.capture(keys, values, new int[] {0, 1}, new byte[] {0, 1});
        var second = M3FiveLibraryIndexViews.capture(keys, values, new int[] {0, 1}, new byte[] {0, 1});
        BitSet imported = new BitSet(); imported.set(1);
        var selection = M3FiveFamilyPreparedSelection.fromBits(first, imported);
        imported.clear();
        assertEquals(1, selection.size());
        assertTrue(selection.containsOrdinal(1));
        assertFalse(selection.containsOrdinal(0));
        assertNotSame(selection.projection(), M3FiveFamilyPreparedSelection.all(first).projection());
        assertThrows(IllegalArgumentException.class,
                () -> selection.and(M3FiveFamilyPreparedSelection.all(second)));
        BitSet outside = new BitSet(); outside.set(2);
        assertThrows(IndexOutOfBoundsException.class,
                () -> M3FiveFamilyPreparedSelection.fromBits(first, outside));
        if (M3IndexMaskCompactor.nativeAvailable()) {
            assertEquals(selection.projection().asJdkList(),
                    selection.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).asJdkList());
        } else {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> selection.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }
}
