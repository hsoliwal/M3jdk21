// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import jdk.m3.tools.cpu.M3AbstractRanges;
import jdk.m3.tools.cpu.M3DecisionRows;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independently enumerated signed-int range and decision-row oracles. */
public final class M3AbstractDecisionContract {
    private static long checks;
    private M3AbstractDecisionContract() { }

    private static void check(boolean truth, String label) {
        checks++;
        if (!truth) { throw new AssertionError(label + " check=" + checks); }
    }
    private static void equal(long[] actual, long lo, long hi) {
        check(Arrays.equals(actual, new long[] {lo, hi}), "range=" + Arrays.toString(actual)
                + " expected=" + lo + "," + hi);
    }
    private static void equal(int[] actual, int[] expected) {
        check(Arrays.equals(actual, expected), "decision=" + Arrays.toString(actual)
                + " expected=" + Arrays.toString(expected));
    }
    private static void reject(Runnable action, Class<? extends Throwable> kind) {
        checks++;
        try { action.run(); } catch (Throwable problem) {
            if (kind.isInstance(problem)) { return; }
            throw new AssertionError("unexpected refusal type", problem);
        }
        throw new AssertionError("refusal expected: " + kind);
    }

    private static void intervals() {
        List<long[]> all = new ArrayList<>();
        all.add(M3AbstractRanges.bottom());
        for (int lo = -4; lo <= 4; lo++) {
            for (int hi = lo; hi <= 4; hi++) { all.add(new long[] {lo, hi}); }
        }
        for (long[] a : all) {
            M3AbstractRanges.validate(a);
            long[] neg = M3AbstractRanges.negate(a);
            if (a[0] <= a[1]) { equal(neg, -a[1], -a[0]); }
            else { equal(neg, 1, 0); }
            check(M3AbstractRanges.cardinality(a) == (a[0] <= a[1] ? a[1] - a[0] + 1 : 0),
                    "cardinality");
            for (long[] b : all) {
                boolean ea = a[0] > a[1];
                boolean eb = b[0] > b[1];
                long[] meet = M3AbstractRanges.meet(a, b);
                long[] join = M3AbstractRanges.join(a, b);
                if (ea || eb) {
                    equal(meet, 1, 0);
                    long[] nonempty = ea ? b : a;
                    equal(join, nonempty[0], nonempty[1]);
                    equal(M3AbstractRanges.add(a, b), 1, 0);
                    equal(M3AbstractRanges.subtract(a, b), 1, 0);
                    equal(M3AbstractRanges.multiply(a, b), 1, 0);
                    check(M3AbstractRanges.lessThan(a, b) == M3AbstractRanges.UNKNOWN, "empty lt");
                    continue;
                }
                long mlo = Math.max(a[0], b[0]);
                long mhi = Math.min(a[1], b[1]);
                if (mlo > mhi) { equal(meet, 1, 0); }
                else { equal(meet, mlo, mhi); }
                equal(join, Math.min(a[0], b[0]), Math.max(a[1], b[1]));
                long addMin = Long.MAX_VALUE, addMax = Long.MIN_VALUE;
                long subMin = Long.MAX_VALUE, subMax = Long.MIN_VALUE;
                long mulMin = Long.MAX_VALUE, mulMax = Long.MIN_VALUE;
                boolean allLt = true, noneLt = true;
                for (long x = a[0]; x <= a[1]; x++) {
                    for (long y = b[0]; y <= b[1]; y++) {
                        addMin = Math.min(addMin, x + y);
                        addMax = Math.max(addMax, x + y);
                        subMin = Math.min(subMin, x - y);
                        subMax = Math.max(subMax, x - y);
                        mulMin = Math.min(mulMin, x * y);
                        mulMax = Math.max(mulMax, x * y);
                        allLt &= x < y;
                        noneLt &= x >= y;
                    }
                }
                equal(M3AbstractRanges.add(a, b), addMin, addMax);
                equal(M3AbstractRanges.subtract(a, b), subMin, subMax);
                equal(M3AbstractRanges.multiply(a, b), mulMin, mulMax);
                int expectedLt = allLt ? M3AbstractRanges.TRUE
                        : noneLt ? M3AbstractRanges.FALSE : M3AbstractRanges.UNKNOWN;
                check(M3AbstractRanges.lessThan(a, b) == expectedLt, "lt");
                long[] wide = M3AbstractRanges.widen(a, b);
                check(wide[0] <= a[0] && wide[1] >= a[1] && wide[0] <= b[0]
                        && wide[1] >= b[1], "widen encloses");
            }
        }
        equal(M3AbstractRanges.top(), Integer.MIN_VALUE, Integer.MAX_VALUE);
        check(M3AbstractRanges.cardinality(M3AbstractRanges.top()) == 4294967296L, "full count");
        equal(M3AbstractRanges.add(M3AbstractRanges.constant(Integer.MAX_VALUE),
                M3AbstractRanges.constant(1)), Integer.MIN_VALUE, Integer.MAX_VALUE);
        equal(M3AbstractRanges.subtract(M3AbstractRanges.constant(Integer.MIN_VALUE),
                M3AbstractRanges.constant(1)), Integer.MIN_VALUE, Integer.MAX_VALUE);
        equal(M3AbstractRanges.multiply(M3AbstractRanges.constant(Integer.MIN_VALUE),
                M3AbstractRanges.constant(-1)), Integer.MIN_VALUE, Integer.MAX_VALUE);
        equal(M3AbstractRanges.negate(M3AbstractRanges.constant(Integer.MIN_VALUE)),
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        reject(() -> M3AbstractRanges.validate(null), IllegalArgumentException.class);
        reject(() -> M3AbstractRanges.validate(new long[] {0L}), IllegalArgumentException.class);
        reject(() -> M3AbstractRanges.validate(new long[] {2L, 1L}), IllegalArgumentException.class);
        reject(() -> M3AbstractRanges.validate(new long[] {Integer.MIN_VALUE - 1L, 4L}),
                IllegalArgumentException.class);
        long[] a = {5, 9};
        long[] b = {2, 3};
        long[] result = M3AbstractRanges.join(a, b);
        result[0] = 123;
        check(a[0] == 5 && b[0] == 2, "detached result");
    }

    private static boolean matches(int[] values, int[] rowStart, int[] field,
            int[] lo, int[] hi, int row) {
        for (int i = rowStart[row]; i < rowStart[row + 1]; i++) {
            if (values[field[i]] < lo[i] || values[field[i]] > hi[i]) { return false; }
        }
        return true;
    }

    private static void decisions() {
        Random random = new Random(0x3846B3A7L);
        for (int trial = 0; trial < 24000; trial++) {
            int fields = 1 + random.nextInt(5);
            int rows = random.nextInt(12);
            int[] values = new int[fields];
            for (int i = 0; i < fields; i++) { values[i] = random.nextInt(11) - 5; }
            int[] rowStart = new int[rows + 1];
            int[] priority = new int[rows];
            int[] actions = new int[rows];
            List<Integer> ids = new ArrayList<>(), mins = new ArrayList<>(), maxs = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                rowStart[row] = ids.size();
                priority[row] = random.nextInt(7) - 3;
                actions[row] = random.nextInt(4) - 1;
                int conditions = random.nextInt(4);
                for (int j = 0; j < conditions; j++) {
                    int first = random.nextInt(13) - 6;
                    int second = random.nextInt(13) - 6;
                    ids.add(random.nextInt(fields));
                    mins.add(Math.min(first, second));
                    maxs.add(Math.max(first, second));
                }
            }
            rowStart[rows] = ids.size();
            int[] f = ids.stream().mapToInt(Integer::intValue).toArray();
            int[] lo = mins.stream().mapToInt(Integer::intValue).toArray();
            int[] hi = maxs.stream().mapToInt(Integer::intValue).toArray();
            List<Integer> matching = new ArrayList<>();
            int best = -1;
            boolean conflict = false;
            for (int row = 0; row < rows; row++) {
                if (!matches(values, rowStart, f, lo, hi, row)) { continue; }
                matching.add(row);
                if (best < 0 || priority[row] < priority[best]) {
                    best = row;
                    conflict = false;
                } else if (priority[row] == priority[best] && actions[row] != actions[best]) {
                    conflict = true;
                }
            }
            int[] expected = matching.stream().mapToInt(Integer::intValue).toArray();
            equal(M3DecisionRows.matchingRows(values, rowStart, f, lo, hi, priority, actions, rows),
                    expected);
            if (conflict) {
                reject(() -> M3DecisionRows.decide(values, rowStart, f, lo, hi,
                        priority, actions), IllegalStateException.class);
            } else {
                equal(M3DecisionRows.decide(values, rowStart, f, lo, hi, priority, actions),
                        best < 0 ? new int[] {-1, -1} : new int[] {best, actions[best]});
            }
            if (matching.size() > 0) {
                reject(() -> M3DecisionRows.matchingRows(values, rowStart, f, lo, hi,
                        priority, actions, matching.size() - 1), IllegalStateException.class);
            }
        }
        int[] one = {0}, offsets = {0, 1}, field = {0}, lo = {1}, hi = {2};
        int[] priority = {0}, actions = {5};
        reject(() -> M3DecisionRows.decide(one, offsets, field, lo, hi, priority,
                new int[0]), IllegalArgumentException.class);
        reject(() -> M3DecisionRows.matchingRows(one, offsets, field, lo, hi,
                priority, actions, -1), IllegalArgumentException.class);
        reject(() -> M3DecisionRows.decide(one, offsets, field, new int[] {3}, hi,
                priority, actions), IllegalArgumentException.class);
        reject(() -> M3DecisionRows.decide(one, new int[] {0, 2}, field, lo, hi,
                priority, actions), IllegalArgumentException.class);
        // Earlier ambiguous ties cannot override a later lower-priority winner.
        equal(M3DecisionRows.decide(new int[0], new int[] {0,0,0,0}, new int[0],
                new int[0], new int[0], new int[] {3,3,1}, new int[] {5,6,7}),
                new int[] {2,7});
    }

    public static void main(String[] args) {
        intervals();
        decisions();
        System.out.println("M3_WAVE6_ABSTRACT_DECISION_PASS checks=" + checks);
    }
}
