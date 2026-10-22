/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Host Java21 deterministic typed-fact, stable-index and missing-value differential. */
public final class M3SignalColumnsSelfTest {
    private M3SignalColumnsSelfTest() { }
    private static long checks;
    private static void check(boolean yes) {
        checks++;
        if (!yes) throw new AssertionError("M3SignalColumns check=" + checks);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("unexpected exception", error);
        }
        throw new AssertionError("missing expected exception " + type.getName());
    }
    public static void main(String[] args) {
        Random random = new Random(0x4d335349474e414cL);
        for (int trial = 0; trial < 1200; trial++) {
            int length = random.nextInt(140);
            long[] values = new long[length];
            int[] minor = new int[length];
            double[] costs = new double[length];
            int[] state = new int[length];
            long[] valid = M3SignalColumns.allValid(length);
            for (int i = 0; i < length; i++) {
                values[i] = random.nextBoolean()
                        ? Long.MAX_VALUE - random.nextInt(4)
                        : Long.MIN_VALUE + random.nextInt(4);
                minor[i] = random.nextInt(9) - 4;
                costs[i] = random.nextDouble();
                state[i] = random.nextInt();
                if (random.nextInt(7) == 0) valid[i >>> 6] &= ~(1L << (i & 63));
            }
            var lane = new M3SignalColumns(length,
                    new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                            M3SignalColumns.Origin.STATIC, 11, false,
                            valid, null, values, null),
                    new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                            M3SignalColumns.Origin.DOMAIN, 12, true,
                            M3SignalColumns.allValid(length), minor, null, null),
                    new M3SignalColumns.Column(M3SignalColumns.Kind.DOUBLE,
                            M3SignalColumns.Origin.PROFILED, 13, false,
                            M3SignalColumns.allValid(length), null, null, costs));
            int[] selected = lane.selectedByState(state, Integer.MIN_VALUE, 1);
            int[] actual = lane.sortSelected(selected);
            Integer[] expected = new Integer[selected.length];
            for (int i = 0; i < selected.length; i++) expected[i] = selected[i];
            Arrays.sort(expected, (a, b) -> {
                boolean left = (valid[a >>> 6] & (1L << (a & 63))) != 0;
                boolean right = (valid[b >>> 6] & (1L << (b & 63))) != 0;
                if (left != right) return left ? -1 : 1;
                if (left) {
                    int comparison = Long.compare(values[a], values[b]);
                    if (comparison != 0) return comparison;
                }
                int comparison = Integer.compare(minor[b], minor[a]);
                return comparison != 0 ? comparison : Double.compare(costs[a], costs[b]);
            });
            for (int i = 0; i < actual.length; i++) check(actual[i] == expected[i]);
            check(Arrays.equals(selected, lane.selectedByState(state, Integer.MIN_VALUE, 1)));
            if (length > 1) {
                int a = random.nextInt(length), b = random.nextInt(length);
                check(lane.compare(a, b).sign() == -lane.compare(b, a).sign());
            }
            long[] defensive = lane.snapshotColumn(0).longs();
            if (defensive.length != 0) {
                long old = defensive[0];
                defensive[0] ^= 1;
                check(lane.snapshotColumn(0).longs()[0] == old);
            }
        }
        var full = new M3SignalColumns(2,
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.STATIC, 42, false,
                        M3SignalColumns.allValid(2), null,
                        new long[]{9007199254740993L, 9007199254740992L}, null));
        check(full.compare(0, 1).sign() > 0);
        check(Double.isNaN(full.compare(0, 1).numericSeparation()));
        refuse(IllegalArgumentException.class, () -> new M3SignalColumns(1,
                new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.STATIC, 0, false,
                        new long[]{2}, new int[]{7}, null, null)));
        System.out.println("PASS M3SignalColumnsSelfTest checks=" + checks);
    }
}
