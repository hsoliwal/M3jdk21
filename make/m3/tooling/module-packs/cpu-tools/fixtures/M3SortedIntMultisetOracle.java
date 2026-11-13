// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3SortedIntMultiset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.TreeMap;

/** Independent TreeMap/count specification for immutable signed multiset operations. */
public final class M3SortedIntMultisetOracle {
    private static long checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label + " at check " + checks);
    }

    private static TreeMap<Integer, Integer> counts(int[] source) {
        TreeMap<Integer, Integer> map = new TreeMap<>();
        for (int value : source) map.merge(value, 1, Integer::sum);
        return map;
    }

    private static int[] oracle(int[] left, int[] right, int op) {
        TreeMap<Integer, Integer> a = counts(left);
        TreeMap<Integer, Integer> b = counts(right);
        TreeMap<Integer, Integer> domain = new TreeMap<>(a);
        domain.putAll(b);
        List<Integer> expected = new ArrayList<>();
        for (int value : domain.keySet()) {
            int c = a.getOrDefault(value, 0);
            int d = b.getOrDefault(value, 0);
            int n = switch (op) {
                case 0 -> Math.min(c, d);
                case 1 -> Math.max(c - d, 0);
                case 2 -> Math.max(c, d);
                default -> throw new AssertionError("oracle op");
            };
            for (int i = 0; i < n; i++) expected.add(value);
        }
        return expected.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] missing(int[] observed, int[] original) {
        TreeMap<Integer, Integer> a = counts(observed);
        TreeMap<Integer, Integer> b = counts(original);
        return b.entrySet().stream()
                .filter(entry -> entry.getValue() > a.getOrDefault(entry.getKey(), 0))
                .mapToInt(Map.Entry::getKey).toArray();
    }

    private static void test(int[] x, int[] y) {
        int[] xs = x.clone();
        int[] ys = y.clone();
        M3SortedIntMultiset a = M3SortedIntMultiset.copyOfSorted(x);
        M3SortedIntMultiset b = M3SortedIntMultiset.copyOfSorted(y);
        check(a.size() == xs.length && b.size() == ys.length, "sizes");
        check(Arrays.equals(xs, a.toArray()), "input snapshot a");
        check(Arrays.equals(ys, b.toArray()), "input snapshot b");
        check(Arrays.equals(oracle(xs, ys, 0), a.intersection(b)), "multiset intersection");
        check(Arrays.equals(oracle(xs, ys, 1), a.difference(b)), "multiset difference");
        check(Arrays.equals(oracle(xs, ys, 2), a.union(b)), "multiset max union");
        check(Arrays.equals(missing(xs, ys), a.missingDistinctFrom(b)), "missing distinct");
        check(Arrays.equals(missing(ys, xs), b.missingDistinctFrom(a)), "reverse missing distinct");
        for (int value : new int[]{Integer.MIN_VALUE, -14, -1, 0, 1, 14, Integer.MAX_VALUE}) {
            TreeMap<Integer, Integer> reference = counts(xs);
            check(a.countOf(value) == reference.getOrDefault(value, 0), "countOf");
            int expected = 0;
            for (int v : xs) if (v < value) expected++;
            check(a.lowerBound(value) == expected, "lowerBound");
        }
        int[] detached = a.intersection(b);
        if (detached.length > 0) detached[0] ^= 0x317f;
        check(Arrays.equals(oracle(xs, ys, 0), a.intersection(b)), "detached intersection");
        if (x.length > 0) x[0] ^= 0x19;
        if (y.length > 0) y[0] ^= 0x2f;
        check(Arrays.equals(xs, a.toArray()), "no caller input alias a");
        check(Arrays.equals(ys, b.toArray()), "no caller input alias b");
        int[] publicCopy = a.toArray();
        if (publicCopy.length > 0) publicCopy[0] ^= 0x4444;
        check(Arrays.equals(xs, a.toArray()), "detached toArray");
    }

    private static int[] randomSorted(SplittableRandom random, int n) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            int kind = random.nextInt(12);
            values[i] = kind == 0 ? Integer.MIN_VALUE
                    : kind == 1 ? Integer.MAX_VALUE
                    : kind == 2 ? random.nextInt()
                    : random.nextInt(-12, 13);
        }
        Arrays.sort(values);
        return values;
    }

    private static void refuse(Runnable action, Class<? extends RuntimeException> type) {
        RuntimeException caught = null;
        try { action.run(); } catch (RuntimeException e) { caught = e; }
        check(caught != null && type.isInstance(caught), "expected " + type.getSimpleName());
    }

    public static void main(String[] args) {
        test(new int[0], new int[0]);
        test(new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0, 0, Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE, 0, 0, 1, Integer.MAX_VALUE, Integer.MAX_VALUE});
        test(new int[]{2, 2, 2, 2}, new int[]{2, 2});
        test(new int[]{1, 1, 2, 3, 3, 3}, new int[]{1, 2, 2, 3, 4});
        for (int n = 0; n <= 6; n++) for (int m = 0; m <= 6; m++) {
            int[] a = new int[n];
            int[] b = new int[m];
            for (int x = 0; x < 5; x++) {
                Arrays.fill(a, x - 2);
                for (int y = 0; y < 5; y++) {
                    Arrays.fill(b, y - 2);
                    test(a.clone(), b.clone());
                }
            }
        }
        SplittableRandom random = new SplittableRandom(0x31bd02cfc3eL);
        for (int k = 0; k < 16_000; k++) {
            test(randomSorted(random, random.nextInt(80)),
                    randomSorted(random, random.nextInt(80)));
        }
        refuse(() -> M3SortedIntMultiset.copyOfSorted(null), NullPointerException.class);
        refuse(() -> M3SortedIntMultiset.copyOfSorted(new int[]{2, 1}), IllegalArgumentException.class);
        refuse(() -> M3SortedIntMultiset.copyOfSorted(new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}),
                IllegalArgumentException.class);
        refuse(() -> M3SortedIntMultiset.copyOfSorted(
                new int[M3SortedIntMultiset.MAX_ITEMS + 1]), IllegalArgumentException.class);
        M3SortedIntMultiset empty = M3SortedIntMultiset.copyOfSorted(new int[0]);
        refuse(() -> empty.intersection(null), NullPointerException.class);
        refuse(() -> empty.difference(null), NullPointerException.class);
        refuse(() -> empty.union(null), NullPointerException.class);
        refuse(() -> empty.missingDistinctFrom(null), NullPointerException.class);
        int n = M3SortedIntMultiset.MAX_ITEMS / 2 + 1;
        int[] even = new int[n];
        int[] odd = new int[n];
        for (int i = 0; i < n; i++) { even[i] = 2*i; odd[i] = 2*i+1; }
        M3SortedIntMultiset a = M3SortedIntMultiset.copyOfSorted(even);
        M3SortedIntMultiset b = M3SortedIntMultiset.copyOfSorted(odd);
        refuse(() -> a.union(b), IllegalArgumentException.class);
        check(a.intersection(b).length == 0, "large disjoint");
        System.out.println("M3_MULTISET_JAVA_ORACLE_PASS checks=" + checks);
    }
}
