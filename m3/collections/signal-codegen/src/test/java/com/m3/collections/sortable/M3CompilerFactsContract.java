// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.util.Arrays;
import java.util.Random;

/** Independent primitive signal/value/unknown policy contract. */
public final class M3CompilerFactsContract {
    private static long checks;
    private M3CompilerFactsContract() { }

    public static void main(String[] args) {
        int[] counts = new int[M3CompilerFacts.COUNT];
        counts[M3CompilerFacts.FIELDS] = 4;
        counts[M3CompilerFacts.METHODS] = 12;
        counts[M3CompilerFacts.IF_BRANCHES] = 3;
        long[] mask = {(1L << M3CompilerFacts.FIELDS)
                | (1L << M3CompilerFacts.METHODS)
                | (1L << M3CompilerFacts.IF_BRANCHES)};
        var facts = new M3CompilerFacts(counts, mask);
        counts[M3CompilerFacts.FIELDS] = 99;
        mask[0] = 0;
        same(4, facts.count(M3CompilerFacts.FIELDS));
        yes(facts.known(M3CompilerFacts.IF_BRANCHES));
        yes(!facts.known(M3CompilerFacts.RUNTIME_INVOCATIONS));
        yes(facts.origin(M3CompilerFacts.IF_BRANCHES) == M3CompilerFacts.Origin.COMPILER_AST);
        yes(facts.origin(M3CompilerFacts.FIELDS) == M3CompilerFacts.Origin.COMPILER_MODEL);
        yes(facts.origin(M3CompilerFacts.INCOMING_CALLS) == M3CompilerFacts.Origin.UNKNOWN);
        throwsType(IllegalStateException.class, () -> facts.count(M3CompilerFacts.INCOMING_CALLS));
        facts.counts()[M3CompilerFacts.FIELDS] = 1000;
        facts.validityMask()[0] = -1L;
        same(4, facts.count(M3CompilerFacts.FIELDS));
        yes(!facts.known(M3CompilerFacts.RUNTIME_INVOCATIONS));
        double[] weights = new double[M3CompilerFacts.COUNT];
        weights[M3CompilerFacts.METHODS] = 2;
        weights[M3CompilerFacts.FIELDS] = 0.5;
        same(26.0, facts.project(weights));
        weights[M3CompilerFacts.RUNTIME_INVOCATIONS] = 0.5;
        yes(Double.isNaN(facts.project(weights)));
        weights[M3CompilerFacts.RUNTIME_INVOCATIONS] = 0;
        weights[M3CompilerFacts.FIELDS] = Double.NaN;
        throwsType(IllegalArgumentException.class, () -> facts.project(weights));

        int[] rhsCounts = facts.counts();
        rhsCounts[M3CompilerFacts.FIELDS] = 5;
        var rhs = new M3CompilerFacts(rhsCounts, facts.validityMask());
        same(-1, facts.compareBy(rhs, new int[]{M3CompilerFacts.FIELDS}, new boolean[]{false}));
        same(1, facts.compareBy(rhs, new int[]{M3CompilerFacts.FIELDS}, new boolean[]{true}));
        throwsType(IllegalStateException.class,
                () -> facts.compareBy(rhs, new int[]{M3CompilerFacts.RUNTIME_INVOCATIONS}, new boolean[]{false}));
        throwsType(IllegalArgumentException.class,
                () -> facts.compareBy(rhs, new int[]{M3CompilerFacts.FIELDS, M3CompilerFacts.FIELDS}, new boolean[]{false, false}));
        throwsType(IllegalArgumentException.class,
                () -> new M3CompilerFacts(new int[M3CompilerFacts.COUNT], new long[]{1L << 63}));
        throwsType(IllegalArgumentException.class,
                () -> new M3CompilerFacts(new int[1], new long[]{0}));

        var rng = new Random(0x4d335349474e414cL);
        for (int round = 0; round < 10_000; round++) {
            int[] left = new int[M3CompilerFacts.COUNT];
            int[] right = new int[M3CompilerFacts.COUNT];
            long known = 0;
            for (int i = 0; i < M3CompilerFacts.COUNT; i++) {
                left[i] = rng.nextInt(10_000);
                right[i] = rng.nextInt(10_000);
                if (rng.nextBoolean()) known |= 1L << i;
            }
            var a = new M3CompilerFacts(left, new long[]{known});
            var b = new M3CompilerFacts(right, new long[]{known});
            int coordinate = rng.nextInt(M3CompilerFacts.COUNT);
            int selected = coordinate;
            if (a.known(selected)) {
                int c = a.compareBy(b, new int[]{selected}, new boolean[]{false});
                int d = b.compareBy(a, new int[]{selected}, new boolean[]{false});
                same(-Integer.signum(c), Integer.signum(d));
                same(Integer.signum(Integer.compare(left[selected], right[selected])), Integer.signum(c));
            } else {
                throwsType(IllegalStateException.class, () -> a.compareBy(b, new int[]{selected}, new boolean[]{false}));
            }
            yes(Arrays.equals(a.counts(), left));
        }
        System.out.println("PASS M3CompilerFactsContract assertions=" + checks);
    }

    private static void same(int expected, int actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    private static void same(double expected, double actual) {
        checks++;
        if (Double.compare(expected, actual) != 0) throw new AssertionError(expected + " != " + actual);
    }
    private static void yes(boolean value) {
        checks++;
        if (!value) throw new AssertionError("contract failure at " + checks);
    }
    private static void throwsType(Class<? extends Throwable> type, Runnable fn) {
        checks++;
        try { fn.run(); }
        catch (Throwable t) { if (type.isInstance(t)) return; throw new AssertionError(t); }
        throw new AssertionError("expected " + type);
    }
}
