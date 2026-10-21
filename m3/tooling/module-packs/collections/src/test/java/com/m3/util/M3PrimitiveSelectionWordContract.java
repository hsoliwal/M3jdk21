/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.function.LongPredicate;

/** Independent order oracle for first rejection, word boundaries and composed views. */
public final class M3PrimitiveSelectionWordContract {
    private static long checks;
    private M3PrimitiveSelectionWordContract() { }

    public static void main(String[] args) {
        for (int size : new int[] {0, 1, 63, 64, 65, 127, 128, 129, 257}) {
            var store = new M3PrimitiveSoaStore(Math.max(1, size),
                    M3PrimitiveSoaStore.STATE_BYTES, M3PrimitiveKind.LONG);
            for (int i = 0; i < size; i++) store.appendRow(new long[] {i}, (byte) -1, 0L);
            var identity = M3PrimitiveIndexView.identity(store);
            var views = new M3PrimitiveIndexView[] {identity, identity.reversed(),
                    identity.slice(size / 4, size - size / 4).reversed(),
                    identity.sortedByLane(0, false).topKByLane(0, size / 2, true)};
            for (var view : views) {
                int[] rows = view.toIndexArray();
                for (int firstRejected = 0; firstRejected <= rows.length; firstRejected++) {
                    final int boundary = firstRejected;
                    for (int pattern = 0; pattern < 3; pattern++) {
                        final int mode = pattern;
                        int[] visits = {0};
                        LongPredicate predicate = value -> {
                            int index = visits[0]++;
                            require(value == rows[index], "ordered callback");
                            return index < boundary || index > boundary
                                    && (mode == 0 || mode == 1 && (index & 1) == 0);
                        };
                        int[] expected = new int[rows.length];
                        int count = 0;
                        for (int index = 0; index < rows.length; index++) {
                            if (index < boundary || index > boundary
                                    && (mode == 0 || mode == 1 && (index & 1) == 0)) {
                                expected[count++] = rows[index];
                            }
                        }
                        var selected = view.filterLane(0, predicate);
                        require(visits[0] == rows.length, "one callback per occurrence");
                        require(Arrays.equals(Arrays.copyOf(expected, count), selected.toIndexArray()),
                                "exact independent row order");
                        require(Arrays.equals(rows, view.toIndexArray()), "owner projection unchanged");
                    }
                }
                var all = view.filterStateMask(-1);
                require(all != view, "fresh facade identity");
                int[] exported = all.toIndexArray();
                Arrays.fill(exported, -1);
                require(Arrays.equals(rows, all.toIndexArray()), "defensive export after all selection");
            }
            var selected = identity.reversed().filterLane(0, value -> true);
            selected.applyFullPermutation();
            stale(identity);
            stale(selected);
        }
        System.out.println("M3PrimitiveSelectionWordContract PASS checks=" + checks);
    }

    private static void stale(M3PrimitiveIndexView view) {
        try { view.size(); }
        catch (ConcurrentModificationException expected) { checks++; return; }
        throw new AssertionError("shared generation must become stale");
    }

    private static void require(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
}
