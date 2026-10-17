/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone deterministic differential and metamorphic Java/JNI contract oracle. */
public final class M3LongBitmapFilterPlanSelfTest {
    private static long checks;
    private M3LongBitmapFilterPlanSelfTest() { }
    private static void check(boolean holds, String message) {
        checks++;
        if (!holds) throw new AssertionError(message);
    }
    private static void rejects(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("expected " + kind + " got " + problem, problem);
        }
        throw new AssertionError("expected " + kind);
    }
    private static long[] selected(long[] values, BitSet chosen) {
        long[] result = new long[chosen.cardinality()];
        int at = 0;
        for (int pos = chosen.nextSetBit(0); pos >= 0; pos = chosen.nextSetBit(pos + 1)) {
            result[at++] = values[pos];
        }
        return result;
    }
    private static void verify(long[] raw, Random random) {
        long[] retained = raw.clone();
        M3LongIndexProjection root = M3LongIndexProjection.snapshot(raw);
        Arrays.fill(raw, 79L);
        AtomicInteger pRuns = new AtomicInteger();
        AtomicInteger qRuns = new AtomicInteger();
        M3LongBitmapFilterPlan p = M3LongBitmapFilterPlan.where(root, v -> {
            pRuns.incrementAndGet(); return v >= 0;
        });
        M3LongBitmapFilterPlan q = M3LongBitmapFilterPlan.where(root, v -> {
            qRuns.incrementAndGet(); return (v & 1L) == 0;
        });
        check(pRuns.get() == retained.length && qRuns.get() == retained.length, "once evaluation");
        BitSet first = new BitSet(), second = new BitSet();
        for (int i = 0; i < retained.length; i++) {
            if (retained[i] >= 0) first.set(i);
            if ((retained[i] & 1L) == 0) second.set(i);
        }
        BitSet both = (BitSet) first.clone(); both.and(second);
        BitSet either = (BitSet) first.clone(); either.or(second);
        BitSet xor = (BitSet) first.clone(); xor.xor(second);
        BitSet minus = (BitSet) first.clone(); minus.andNot(second);
        BitSet inverse = (BitSet) first.clone(); inverse.flip(0, retained.length);
        M3LongBitmapFilterPlan[] plans = {p, q, p.and(q), p.or(q), p.xor(q), p.minus(q), p.not()};
        BitSet[] reference = {first, second, both, either, xor, minus, inverse};
        for (int caseIndex = 0; caseIndex < plans.length; caseIndex++) {
            M3LongBitmapFilterPlan plan = plans[caseIndex];
            BitSet expected = reference[caseIndex];
            check(plan.size() == expected.cardinality(), "count");
            check(Arrays.equals(plan.toArray(), selected(retained, expected)), "logical values");
            check(plan.projection() == plan.projection(), "cached projection identity");
            check(plan.sortedAscending() == plan.sortedAscending(), "cached ascending");
            check(plan.sortedDescending() == plan.sortedDescending(), "cached descending");
            check(plan.isEmpty() == expected.isEmpty(), "empty");
            long[] sorted = plan.toArray(); Arrays.sort(sorted);
            check(Arrays.equals(plan.sortedAscending().toArray(), sorted), "stable asc values");
            for (int i = 0; i < sorted.length / 2; i++) {
                long temp = sorted[i]; sorted[i] = sorted[sorted.length - 1 - i];
                sorted[sorted.length - 1 - i] = temp;
            }
            check(Arrays.equals(plan.sortedDescending().toArray(), sorted), "descending values");
            check(Arrays.equals(plan.longStream().toArray(), plan.toArray()), "stream");
            check(Arrays.equals(plan.parallelLongStream().toArray(), plan.toArray()), "parallel split");
            int position = 0;
            M3LongIterator it = plan.iterator();
            while (it.hasNext()) {
                check(it.nextLong() == plan.get(position), "ordinal iterator");
                position++;
            }
            check(position == expected.cardinality(), "iterator length");
            for (int i = 0; i < retained.length; i++) {
                check(plan.includesOrdinal(i) == expected.get(i), "ordinal bit");
            }
            BitSet leaked = plan.bitsCopy(); leaked.flip(0, retained.length);
            check(plan.bitsCopy().equals(expected), "bitset defensive copy");
            if (plan.size() > 0) {
                int off = random.nextInt(plan.size() + 1);
                check(Arrays.equals(plan.slice(off, plan.size() - off).toArray(),
                                Arrays.copyOfRange(plan.toArray(), off, plan.size())), "slice");
            }
        }
        check(pRuns.get() == retained.length && qRuns.get() == retained.length,
                "never rerun stored predicates");
        AtomicInteger refinedRuns = new AtomicInteger();
        M3LongBitmapFilterPlan refined = p.refine(v -> {
            refinedRuns.incrementAndGet(); return (v & 1) == 0;
        });
        check(refinedRuns.get() == first.cardinality(), "refinement evaluates admitted only");
        check(Arrays.equals(refined.toArray(), p.and(q).toArray()), "refinement match");
        byte[] states = new byte[retained.length];
        BitSet expectedState = new BitSet();
        for (int i = 0; i < states.length; i++) {
            states[i] = (byte) ((random.nextBoolean() ? 0x81 : 0) | (random.nextBoolean() ? 0x08 : 0));
            if (((states[i] & 0xff) & 0x81) == 0x81 && (states[i] & 0x08) == 0) expectedState.set(i);
        }
        var fromStates = M3LongBitmapFilterPlan.fromStates(root, states, 0x81, 0x08);
        check(Arrays.equals(fromStates.toArray(), selected(retained, expectedState)), "states");
        BitSet imported = (BitSet) first.clone();
        var fromBits = M3LongBitmapFilterPlan.fromBits(root, imported);
        imported.clear();
        check(Arrays.equals(fromBits.toArray(), p.toArray()), "defensive imported bits");
        var empty = M3LongBitmapFilterPlan.none(root);
        check(empty.size() == 0, "none");
        check(Arrays.equals(M3LongBitmapFilterPlan.all(root).toArray(), retained), "all");
        rejects(UnsupportedOperationException.class, p::clear);
        rejects(IllegalArgumentException.class, () -> p.and(M3LongBitmapFilterPlan.all(
                M3LongIndexProjection.snapshot(retained))));
        rejects(IllegalArgumentException.class, () -> M3LongBitmapFilterPlan.fromStates(
                root, new byte[retained.length], 0x01, 0x01));
        rejects(IllegalArgumentException.class, () -> M3LongBitmapFilterPlan.fromStates(
                root, new byte[retained.length + 1], 0, 0));
        BitSet pastEnd = new BitSet(); pastEnd.set(retained.length);
        rejects(IndexOutOfBoundsException.class, () -> M3LongBitmapFilterPlan.fromBits(root, pastEnd));
        if (M3IndexMaskCompactor.nativeAvailable()) {
            check(Arrays.equals(fromStates.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).toArray(),
                    fromStates.toArray()), "JNI parity");
        } else {
            rejects(UnsatisfiedLinkError.class, () -> fromStates.projection(
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }
    public static void main(String[] args) {
        rejects(NullPointerException.class, () -> M3LongBitmapFilterPlan.all(null));
        rejects(NullPointerException.class, () -> M3LongBitmapFilterPlan.where(
                M3LongIndexProjection.snapshot(new long[0]), null));
        Random random = new Random(0x31bb01a52dL);
        verify(new long[0], random);
        verify(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE, 0, -1, -1, 17}, random);
        for (int i = 0; i < 450; i++) {
            int len = random.nextInt(270);
            long[] values = new long[len];
            for (int j = 0; j < len; j++) values[j] = random.nextInt(31) - 15L;
            verify(values, random);
        }
        System.out.println("PASS M3LongBitmapFilterPlanSelfTest checks=" + checks
                + " native=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
