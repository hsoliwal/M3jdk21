// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Counterexample to a blanket preparation guard: finite read-only reentry succeeds. */
public final class M3IndexedLiveReentryContract {
    private M3IndexedLiveReentryContract() { }

    public static void contracts() {
        M3ArrayList<Integer> owner = new M3ArrayList<>(List.of(3, 1, 2));
        AtomicReference<M3IndexedLiveView<Integer>> filteredRef = new AtomicReference<>();
        AtomicInteger predicateCalls = new AtomicInteger();
        M3IndexedLiveView<Integer> filtered = M3IndexedLiveView.filtered(owner, value -> {
            if (predicateCalls.getAndIncrement() == 0 && filteredRef.get().size() != 3) {
                throw new AssertionError("nested read returned wrong size");
            }
            return true;
        });
        filteredRef.set(filtered);
        if (filtered.size() != 3 || !filtered.equals(List.of(3, 1, 2))
                || predicateCalls.get() != 6
                || !Arrays.equals(filtered.physicalIndices(), new int[]{0, 1, 2})) {
            throw new AssertionError("finite predicate reentry changed");
        }

        AtomicReference<M3IndexedLiveView<Integer>> sortedRef = new AtomicReference<>();
        AtomicInteger comparatorCalls = new AtomicInteger();
        M3IndexedLiveView<Integer> sorted = M3IndexedLiveView.sorted(owner, (left, right) -> {
            if (comparatorCalls.getAndIncrement() == 0 && sortedRef.get().size() != 3) {
                throw new AssertionError("nested sorted read returned wrong size");
            }
            return Integer.compare(left, right);
        });
        sortedRef.set(sorted);
        if (sorted.size() != 3 || !sorted.equals(List.of(1, 2, 3))
                || comparatorCalls.get() != 6
                || !Arrays.equals(sorted.physicalIndices(), new int[]{1, 2, 0})) {
            throw new AssertionError("finite comparator reentry changed");
        }
        if (!owner.equals(List.of(3, 1, 2))) {
            throw new AssertionError("read-only callbacks changed parent");
        }
    }

    public static void main(String[] args) {
        contracts();
        System.out.println("M3_INDEXED_LIVE_FINITE_REENTRY_PASS predicate_calls=6 comparator_calls=6");
    }
}
