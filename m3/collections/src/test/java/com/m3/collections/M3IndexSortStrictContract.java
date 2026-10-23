// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Regression for the unchanged default sorter: callback transcript and exception state. */
public final class M3IndexSortStrictContract {
    private static long checks;
    private M3IndexSortStrictContract() { }

    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(why + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void sameArray(int[] expected, int[] actual, String why) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(why + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }

    private static void compareTrace(int[] original, int failureAt,
            List<String> expectedCalls, int[] expectedAfter) {
        int[] physical = original.clone();
        List<String> calls = new ArrayList<>();
        IllegalArgumentException sentinel = new IllegalArgumentException("third comparison");
        int[] ordinal = {0};
        Comparator<Integer> recorder = (a, b) -> {
            calls.add(a + ":" + b);
            if (++ordinal[0] == failureAt) throw sentinel;
            return Integer.compare(a, b);
        };
        try {
            M3IndexSort.stable(physical, address -> address, recorder);
            if (failureAt <= expectedCalls.size()) {
                throw new AssertionError("original comparator exception swallowed");
            }
        } catch (IllegalArgumentException thrown) {
            equal(sentinel, thrown, "exact exception identity");
        }
        equal(expectedCalls, calls, "original comparator callback ordering");
        sameArray(expectedAfter, physical, "original exception-time physical coordinates");
    }

    public static long contracts() {
        checks = 0;
        // The original width=1 pass fills scratch. An exception on width=2
        // must not expose the candidate insertion sort's partially shifted lane.
        compareTrace(new int[] {4, 3, 2, 1}, 3,
                List.of("4:3", "2:1", "3:1"), new int[] {4, 3, 2, 1});
        compareTrace(new int[] {4, 3, 2, 1}, 100,
                List.of("4:3", "2:1", "3:1", "3:2"), new int[] {1, 2, 3, 4});
        compareTrace(new int[] {1, 2, 3, 4}, 100,
                List.of("1:2", "3:4", "1:3", "2:3"), new int[] {1, 2, 3, 4});
        // Prepared-only callbacks are explicitly permitted to have fewer calls.
        int[] prepared = {1, 2, 3, 4};
        int[] observed = {0};
        M3IndexSort.stablePrepared(prepared, i -> i, (a, b) -> {
            observed[0]++; return Integer.compare(a, b);
        });
        equal(3, observed[0], "explicit prepared sorted fast path");
        sameArray(new int[] {1, 2, 3, 4}, prepared, "prepared physical indexes stable");
        System.out.println("PASS M3_INDEX_SORT_STRICT checks=" + checks);
        return checks;
    }

    public static void main(String[] ignored) { contracts(); }
}
