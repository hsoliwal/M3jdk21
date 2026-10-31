/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Exact-replay comparator reentrancy contract for native M3 indexed tables.
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

/** Exercises the actual owner and physical slot reuse, without adapter stand-ins. */
public final class M3IndexedTableReentrantSortContract {
    private static int assertions;

    private M3IndexedTableReentrantSortContract() { }

    private static void check(boolean truth, String message) {
        assertions++;
        if (!truth) throw new AssertionError(message);
    }

    private static void throwsExactly(Class<? extends Throwable> expected,
            Runnable runnable, String message) {
        assertions++;
        try {
            runnable.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) return;
            throw new AssertionError(message + ": wrong exception " + actual, actual);
        }
        throw new AssertionError(message + ": did not fail");
    }

    private static List<String> columns(
            M3IndexedTable<String, String, Integer>.IndexView view) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < view.size(); i++) result.add(view.columnAt(i));
        return result;
    }

    private static void unchangedStableSort() {
        var table = new M3IndexedTable<String, String, Integer>();
        table.put("r", "a", 2);
        table.put("r", "b", 1);
        table.put("r", "c", 1);
        var view = table.row("r");
        check(columns(view.sortedByValue(Comparator.naturalOrder()))
                .equals(List.of("b", "c", "a")), "stable ties retain encounter order");
        check(columns(view.slice(1, 3).sortedByValue(Comparator.naturalOrder()))
                .equals(List.of("b", "c")), "slice sorts only its own logical window");
        check(columns(view).equals(List.of("a", "b", "c")),
                "sorting never moves canonical payloads");
        throwsExactly(NullPointerException.class, () -> view.sortedByValue(null),
                "null comparator contract");
    }

    private static void removalAndSlotReuse() {
        var table = new M3IndexedTable<String, String, Integer>();
        table.put("r", "a", 8);
        table.put("r", "b", 4);
        table.put("r", "c", 6);
        var original = table.row("r");
        int beforeCapacity = table.physicalCapacity();
        boolean[] mutated = { false };
        throwsExactly(ConcurrentModificationException.class,
                () -> original.sortedByValue((left, right) -> {
                    if (!mutated[0]) {
                        mutated[0] = true;
                        check(table.removeCell("r", "b"), "removed comparator-era slot");
                        table.put("r", "d", 3);
                    }
                    return Integer.compare(left, right);
                }), "comparator deletion/reuse cannot return stale permutation");
        check(mutated[0], "comparator actually called");
        check(table.physicalCapacity() == beforeCapacity,
                "reused slot instead of expanding payload");
        check(!table.contains("r", "b") && table.contains("r", "d"),
                "mutation itself remains committed");
        check(columns(table.row("r")).equals(List.of("a", "c", "d")),
                "fresh row index contains only live coordinates");
        throwsExactly(ConcurrentModificationException.class, original::physicalIndices,
                "earlier view is still fail-fast");
    }

    private static void replacementDuringComparison() {
        var table = new M3IndexedTable<String, String, Integer>();
        table.put("r", "a", 3);
        table.put("r", "b", 1);
        var original = table.all();
        boolean[] mutated = { false };
        throwsExactly(ConcurrentModificationException.class,
                () -> original.sortedByValue((left, right) -> {
                    if (!mutated[0]) {
                        mutated[0] = true;
                        check(table.put("r", "a", 9).equals(3),
                                "replacement returns previous value");
                    }
                    return Integer.compare(left, right);
                }), "comparator value replacement must invalidate index");
        check(mutated[0] && table.get("r", "a").equals(9),
                "value replacement preserved despite sorted refusal");
    }

    private static void callbackExceptionAndSmallViews() {
        var table = new M3IndexedTable<String, String, Integer>();
        var empty = table.all();
        check(empty.sortedByValue(Integer::compareTo).size() == 0, "empty order");
        table.put("r", "a", 5);
        var singleton = table.all();
        check(singleton.sortedByValue(Integer::compareTo).valueAt(0) == 5,
                "one element never needs comparator callback");
        table.put("r", "b", 6);
        IllegalStateException failure = new IllegalStateException("original callback failure");
        try {
            table.all().sortedByValue((left, right) -> { throw failure; });
            throw new AssertionError("expected original comparator exception");
        } catch (IllegalStateException actual) {
            check(actual == failure, "preserve original callback exception identity");
        }
        check(columns(table.all()).equals(List.of("a", "b")),
                "callback failure never changes owner ordering");
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        unchangedStableSort();
        removalAndSlotReuse();
        replacementDuringComparison();
        callbackExceptionAndSmallViews();
        System.out.println("PASS M3IndexedTableReentrantSortContract checks=" + assertions);
    }
}
