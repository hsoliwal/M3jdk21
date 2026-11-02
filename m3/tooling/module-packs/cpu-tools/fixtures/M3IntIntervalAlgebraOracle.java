// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3IntIntervalAlgebra;
import com.m3.tools.cpu.M3IntIntervalSet;
import java.util.Arrays;
import java.util.Random;

/** Independent bounded truth-table oracle: no production interval sweep reused. */
public final class M3IntIntervalAlgebraOracle {
    private static long checks;
    private static final int BASE = -4;
    private static final int WIDTH = 7;

    private static void check(boolean valid, String context) {
        checks++;
        if (!valid) throw new AssertionError(context + " check=" + checks);
    }

    private static int[] fromTruth(boolean[] truth, int base) {
        int count = 0;
        for (int i = 0; i < truth.length; i++) if (truth[i] && (i == 0 || !truth[i - 1])) count++;
        int[] pairs = new int[count * 2];
        int used = 0;
        for (int i = 0; i < truth.length;) {
            if (!truth[i]) { i++; continue; }
            pairs[used++] = base + i;
            while (i < truth.length && truth[i]) i++;
            pairs[used++] = base + i;
        }
        return pairs;
    }

    private static boolean[] maskTruth(int mask) {
        boolean[] truth = new boolean[WIDTH];
        for (int k = 0; k < truth.length; k++) truth[k] = (mask & (1 << k)) != 0;
        return truth;
    }

    private static boolean[] truthFromRaw(int[] pairs, int base, int width) {
        boolean[] truth = new boolean[width];
        for (int i = 0; i < pairs.length; i += 2) {
            for (int x = 0; x < width; x++) {
                int coordinate = base + x;
                if (pairs[i] <= coordinate && coordinate < pairs[i + 1]) truth[x] = true;
            }
        }
        return truth;
    }

    private static int[] expectedPairResult(boolean[] left, boolean[] right, int base, int op) {
        boolean[] answer = new boolean[left.length];
        for (int k = 0; k < left.length; k++) {
            answer[k] = switch (op) {
                case 0 -> left[k] || right[k];
                case 1 -> left[k] && right[k];
                case 2 -> left[k] && !right[k];
                default -> throw new AssertionError("invalid op");
            };
        }
        return fromTruth(answer, base);
    }

    private static void verify(M3IntIntervalSet got, int[] expected, String label) {
        int[] actual = got.toPairs();
        check(Arrays.equals(actual, expected), label + " endpoints expected="
                + Arrays.toString(expected) + " got=" + Arrays.toString(actual));
        long length = 0;
        for (int i = 0; i < expected.length; i += 2) length += (long) expected[i+1] - expected[i];
        check(got.coveredLength() == length, label + " cardinality");
        check(got.intervalCount() == expected.length / 2, label + " pair count");
        check(actual != got.toPairs(), label + " detached export");
        if (actual.length > 0) {
            actual[0] = Integer.MAX_VALUE;
            check(Arrays.equals(got.toPairs(), expected), label + " no alias");
        }
    }

    private static void exhaustive() {
        M3IntIntervalSet[] cache = new M3IntIntervalSet[1 << WIDTH];
        for (int mask = 0; mask < cache.length; mask++) {
            cache[mask] = M3IntIntervalSet.copyOf(fromTruth(maskTruth(mask), BASE));
        }
        for (int a = 0; a < cache.length; a++) {
            boolean[] la = maskTruth(a);
            for (int b = 0; b < cache.length; b++) {
                boolean[] lb = maskTruth(b);
                verify(M3IntIntervalAlgebra.union(cache[a], cache[b]), expectedPairResult(la, lb, BASE, 0), "union");
                verify(M3IntIntervalAlgebra.intersection(cache[a], cache[b]), expectedPairResult(la, lb, BASE, 1), "intersection");
                verify(M3IntIntervalAlgebra.difference(cache[a], cache[b]), expectedPairResult(la, lb, BASE, 2), "difference");
                if (((a + b) & 15) == 0) {
                    boolean[] remainder = new boolean[WIDTH];
                    for (int k = 0; k < WIDTH; k++) remainder[k] = k < 6 && !lb[k];
                    verify(M3IntIntervalAlgebra.complementWithin(cache[b], BASE, BASE + 6),
                            fromTruth(remainder, BASE), "bounded complement");
                }
                check(Arrays.equals(cache[a].toPairs(), fromTruth(la, BASE)), "source left retained");
                check(Arrays.equals(cache[b].toPairs(), fromTruth(lb, BASE)), "source right retained");
            }
        }
    }

    private static void randomized() {
        Random rng = new Random(0x4d33494e54455256L);
        for (int trial = 0; trial < 5_000; trial++) {
            int[] a = new int[2 * rng.nextInt(8)];
            int[] b = new int[2 * rng.nextInt(8)];
            for (int[] data : new int[][]{a, b}) {
                for (int k = 0; k < data.length; k += 2) {
                    int v = rng.nextInt(91) - 45;
                    int w = rng.nextInt(91) - 45;
                    data[k] = Math.min(v, w); data[k + 1] = Math.max(v, w);
                }
            }
            M3IntIntervalSet x = M3IntIntervalSet.copyOf(a);
            M3IntIntervalSet y = M3IntIntervalSet.copyOf(b);
            boolean[] l = truthFromRaw(a, -50, 101), r = truthFromRaw(b, -50, 101);
            verify(M3IntIntervalAlgebra.union(x, y), expectedPairResult(l, r, -50, 0), "random union");
            verify(M3IntIntervalAlgebra.intersection(x, y), expectedPairResult(l, r, -50, 1), "random intersection");
            verify(M3IntIntervalAlgebra.difference(x, y), expectedPairResult(l, r, -50, 2), "random difference");
        }
    }

    private static void errorChecks() {
        M3IntIntervalSet empty = M3IntIntervalSet.copyOf(new int[0]);
        for (Runnable r : new Runnable[]{
            () -> M3IntIntervalAlgebra.union(null, empty),
            () -> M3IntIntervalAlgebra.union(empty, null),
            () -> M3IntIntervalAlgebra.intersection(null, empty),
            () -> M3IntIntervalAlgebra.intersection(empty, null),
            () -> M3IntIntervalAlgebra.difference(null, empty),
            () -> M3IntIntervalAlgebra.difference(empty, null),
            () -> M3IntIntervalAlgebra.complementWithin(null, 0, 3)
        }) {
            try { r.run(); throw new AssertionError("accepted null"); }
            catch (NullPointerException expected) { checks++; }
        }
        try { M3IntIntervalAlgebra.complementWithin(empty, 5, -5); throw new AssertionError("reversed accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
        verify(M3IntIntervalAlgebra.complementWithin(empty, 3, 3), new int[0], "empty complement");
        int[] extremes = {Integer.MIN_VALUE, 0, 1, Integer.MAX_VALUE};
        M3IntIntervalSet a = M3IntIntervalSet.copyOf(extremes);
        M3IntIntervalSet b = M3IntIntervalSet.copyOf(new int[]{-10, 12});
        verify(M3IntIntervalAlgebra.intersection(a, b), new int[]{-10, 0, 1, 12}, "extreme intersection");
        verify(M3IntIntervalAlgebra.difference(a, b),
                new int[]{Integer.MIN_VALUE, -10, 12, Integer.MAX_VALUE}, "extreme subtraction");
        verify(M3IntIntervalAlgebra.union(a, b), new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}, "extreme union");
        check(a.coveredLength() > Integer.MAX_VALUE, "long coverage");
        verify(M3IntIntervalAlgebra.complementWithin(a, Integer.MIN_VALUE, Integer.MAX_VALUE),
                new int[]{0, 1}, "extreme complement");

        // One incoming interval splits the first of MAX_PAIRS disjoint intervals,
        // creating MAX_PAIRS+1 output intervals: explicit bound refusal.
        int[] many = new int[2 * M3IntIntervalSet.MAX_PAIRS];
        for (int i = 0; i < M3IntIntervalSet.MAX_PAIRS; i++) {
            many[2 * i] = 4 * i;
            many[2 * i + 1] = 4 * i + 3;
        }
        M3IntIntervalSet nearlyFull = M3IntIntervalSet.copyOf(many);
        M3IntIntervalSet divider = M3IntIntervalSet.copyOf(new int[]{1, 2});
        try {
            M3IntIntervalAlgebra.difference(nearlyFull, divider);
            throw new AssertionError("MAX_PAIRS+1 output accepted");
        } catch (IllegalArgumentException expected) {
            check("interval result too large".equals(expected.getMessage()), "output size refusal message");
        }
    }

    public static void main(String[] args) {
        exhaustive();
        randomized();
        errorChecks();
        System.out.println("M3_INTERVAL_ALGEBRA_PASS checks=" + checks);
    }
}
