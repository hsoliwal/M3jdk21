// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Executable JDK Comparator/IEEE754 stable-sort differential oracle. */
public final class M3SignalSortedViewsSelfTest {
    private record Row(String id, double weight) { }
    private M3SignalSortedViewsSelfTest() { }
    public static void main(String[] ignored) {
        var input = new ArrayList<>(List.of(
                new Row("nan", Double.NaN), new Row("plus", 0.0),
                new Row("minus", -0.0), new Row("one", 1.0),
                new Row("one-again", 1.0), new Row("negInf", Double.NEGATIVE_INFINITY),
                new Row("nan2", Double.longBitsToDouble(0x7ff8000000000002L))));
        int[] calls = {0};
        var asc = M3SignalSortedViews.sortedBySignalSnapshot(input, row -> {
            calls[0]++; return row.weight();
        });
        if (calls[0] != input.size()) throw new AssertionError("multiple signal projections");
        if (!asc.stream().map(Row::id).toList().equals(
                List.of("negInf", "minus", "plus", "one", "one-again", "nan", "nan2")))
            throw new AssertionError("IEEE ascending");
        var desc = M3SignalSortedViews.sortedBySignalSnapshot(input, Row::weight, false);
        if (!desc.stream().map(Row::id).toList().equals(
                List.of("nan", "nan2", "one", "one-again", "plus", "minus", "negInf")))
            throw new AssertionError("IEEE descending");
        input.clear();
        if (asc.size() != 7 || desc.size() != 7) throw new AssertionError("not snapshot");
        try { asc.add(new Row("x", 1.0)); throw new AssertionError("mutable"); }
        catch (UnsupportedOperationException expected) { }
        try { M3SignalSortedViews.sortedBySignalSnapshot(
                Arrays.asList(new Row("x", 1.0), null), Row::weight);
            throw new AssertionError("null entry admitted");
        } catch (NullPointerException expected) { }
        if (!M3SignalSortedViews.sortedBySignalSnapshot(List.<Row>of(), Row::weight).isEmpty())
            throw new AssertionError("empty");

        Random random = new Random(20261017L);
        for (int round = 0; round < 320; round++) {
            int n = round * 11;
            var rows = new ArrayList<Row>();
            for (int i = 0; i < n; i++) {
                double key = switch (random.nextInt(8)) {
                    case 0 -> Double.NaN;
                    case 1 -> 0.0;
                    case 2 -> -0.0;
                    case 3 -> Double.POSITIVE_INFINITY;
                    case 4 -> Double.NEGATIVE_INFINITY;
                    default -> random.nextDouble(-1e6, 1e6);
                };
                rows.add(new Row("row" + i, key));
            }
            var expected = new ArrayList<>(rows);
            expected.sort(Comparator.comparingDouble(Row::weight));
            if (!expected.equals(M3SignalSortedViews.sortedBySignalSnapshot(rows, Row::weight)))
                throw new AssertionError("ascending mismatch " + round);
            expected.sort((left, right) -> Double.compare(right.weight(), left.weight()));
            if (!expected.equals(M3SignalSortedViews.sortedBySignalSnapshot(rows, Row::weight, false)))
                throw new AssertionError("descending mismatch " + round);
        }
        System.out.println("M3_SIGNAL_SORT_PASS rounds=320");
    }
}
