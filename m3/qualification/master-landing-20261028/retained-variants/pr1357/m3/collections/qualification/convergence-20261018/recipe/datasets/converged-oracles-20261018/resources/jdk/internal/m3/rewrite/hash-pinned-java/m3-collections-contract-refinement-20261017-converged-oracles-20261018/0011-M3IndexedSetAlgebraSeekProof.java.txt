// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;

/** Independent JDK21 signed-long navigation oracle for real M3TreeSet storage. */
public final class M3IndexedSetAlgebraSeekProof {
    private static long checks;
    private static final long[] FALLBACKS = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, -987654321L};
    private M3IndexedSetAlgebraSeekProof() { }

    private static void equal(long want, long got, String label) {
        checks++;
        if (want != got) throw new AssertionError(label + " expected=" + want + " got=" + got);
    }
    private static void equal(long[] want, long[] got) {
        checks++;
        if (!Arrays.equals(want, got)) throw new AssertionError("sorted rows differ");
    }
    private static TreeSet<Long> expected(TreeSet<Long> a, TreeSet<Long> b, int op) {
        TreeSet<Long> output = new TreeSet<>(a);
        switch (op) {
            case 0 -> output.retainAll(b);
            case 1 -> output.removeAll(b);
            case 2 -> {
                TreeSet<Long> common = new TreeSet<>(a);
                common.retainAll(b);
                output.addAll(b);
                output.removeAll(common);
            }
            default -> throw new AssertionError(op);
        }
        return output;
    }
    private static M3LongSortedSet actual(M3LongSortedSet a, M3LongSortedSet b, int op) {
        return switch (op) {
            case 0 -> M3LongCompositeViews.sortedIntersection(a, b);
            case 1 -> M3LongCompositeViews.sortedDifference(a, b);
            case 2 -> M3LongCompositeViews.sortedSymmetricDifference(a, b);
            default -> throw new AssertionError(op);
        };
    }
    private static void compare(M3LongSortedSet got, NavigableSet<Long> want, long[] probes) {
        long[] values = new long[want.size()];
        int i = 0;
        for (Long value : want) values[i++] = value;
        equal(values, got.toArray());
        for (long query : probes) for (long fallback : FALLBACKS) {
            Long x = want.lower(query);
            equal(x == null ? fallback : x, got.lower(query, fallback), "lower");
            x = want.floor(query);
            equal(x == null ? fallback : x, got.floor(query, fallback), "floor");
            x = want.ceiling(query);
            equal(x == null ? fallback : x, got.ceiling(query, fallback), "ceiling");
            x = want.higher(query);
            equal(x == null ? fallback : x, got.higher(query, fallback), "higher");
        }
    }
    private static void run() {
        Random random = new Random(0x4d334c4f4e47494eL);
        for (int seed = 0; seed < 80; seed++) {
            M3LongTreeSet a = new M3LongTreeSet(), b = new M3LongTreeSet();
            TreeSet<Long> jdkA = new TreeSet<>(), jdkB = new TreeSet<>();
            for (int phase = 0; phase < 5; phase++) {
                for (int i = 0; i < 48; i++) {
                    long key = switch (random.nextInt(20)) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> Long.MAX_VALUE;
                        case 2 -> 0L;
                        default -> random.nextInt(161) - 80L;
                    };
                    boolean first = random.nextBoolean();
                    boolean add = random.nextInt(3) != 0;
                    M3LongTreeSet owner = first ? a : b;
                    TreeSet<Long> oracle = first ? jdkA : jdkB;
                    boolean expected = add ? oracle.add(key) : oracle.remove(key);
                    boolean actual = add ? owner.add(key) : owner.remove(key);
                    equal(expected ? 1 : 0, actual ? 1 : 0, "mutation");
                }
                long[] probes = {Long.MIN_VALUE, Long.MIN_VALUE + 1L, -81, -1,
                        0, 1, 81, Long.MAX_VALUE - 1L, Long.MAX_VALUE,
                        random.nextInt(191) - 95L};
                for (int op = 0; op < 3; op++) {
                    compare(actual(a, b, op), expected(jdkA, jdkB, op), probes);
                }
                TreeSet<Long> positive = new TreeSet<>(jdkA);
                positive.removeIf(x -> x < 0L);
                compare(actual(M3LongSortedViews.filter(a, x -> x >= 0L), b, 0),
                        expected(positive, jdkB, 0), probes);
            }
        }
        M3LongTreeSet a = new M3LongTreeSet(), b = new M3LongTreeSet();
        TreeSet<Long> jdkA = new TreeSet<>(), jdkB = new TreeSet<>();
        for (int i = 0; i < 4096; i++) {
            long x = Long.MIN_VALUE + (long) i * 997L;
            a.add(x); jdkA.add(x);
            if (i % 3 == 0) { b.add(x); jdkB.add(x); }
            if (i % 7 == 0) { b.add(x + 1L); jdkB.add(x + 1L); }
        }
        a.add(Long.MAX_VALUE); b.add(Long.MAX_VALUE);
        jdkA.add(Long.MAX_VALUE); jdkB.add(Long.MAX_VALUE);
        long[] probes = {Long.MIN_VALUE, Long.MIN_VALUE + 1L,
                Long.MIN_VALUE + 4095L * 997L, Long.MAX_VALUE - 1L, Long.MAX_VALUE};
        for (int op = 0; op < 3; op++) {
            compare(actual(a, b, op), expected(jdkA, jdkB, op), probes);
        }
    }

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("unexpected arguments");
        run();
        System.out.println("PASS M3IndexedSetAlgebraSeekProof checks=" + checks);
    }
}
