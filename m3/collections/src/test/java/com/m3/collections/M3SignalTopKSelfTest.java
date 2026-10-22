/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Independent boxed-JDK stable-order differential; production heap retains only primitive rows. */
public final class M3SignalTopKSelfTest {
    private static long checks;
    private M3SignalTopKSelfTest() { }

    public static void main(String[] args) {
        Random random = new Random(0x4d33544f504b2026L);
        for (int trial = 0; trial < 1800; trial++) {
            int n = random.nextInt(130);
            int dimensionCount = 1 + random.nextInt(4);
            int[] order = new int[dimensionCount];
            M3SignalColumns.Column[] columns = new M3SignalColumns.Column[dimensionCount];
            for (int d = 0; d < dimensionCount; d++) {
                order[d] = d;
                int[] ints = new int[n];
                long[] validity = M3SignalColumns.allValid(n);
                for (int i = 0; i < n; i++) {
                    ints[i] = random.nextInt(11) - 5;
                    if (random.nextInt(9) == 0) validity[i >>> 6] &= ~(1L << (i & 63));
                }
                columns[d] = new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                        M3SignalColumns.Origin.DOMAIN, d + 1, random.nextBoolean(),
                        validity, ints, null, null);
            }
            for (int i = dimensionCount - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swap = order[i]; order[i] = order[j]; order[j] = swap;
            }
            M3SignalColumns facts = new M3SignalColumns(n, columns);
            M3SignalTopK top = new M3SignalTopK(facts, order);
            int selectionSize = n == 0 ? 0 : random.nextInt(2 * n + 1);
            int[] selected = new int[selectionSize];
            for (int i = 0; i < selectionSize; i++) selected[i] = random.nextInt(n);
            int[] snapshot = selected.clone();
            Integer[] positions = new Integer[selectionSize];
            for (int i = 0; i < positions.length; i++) positions[i] = i;
            Arrays.sort(positions, (left, right) -> {
                for (int d : order) {
                    M3SignalColumns.Column column = columns[d];
                    int a = selected[left], b = selected[right];
                    boolean va = (column.validWords()[a >>> 6] & (1L << (a & 63))) != 0;
                    boolean vb = (column.validWords()[b >>> 6] & (1L << (b & 63))) != 0;
                    int sign = va != vb ? (va ? -1 : 1) : va
                            ? Integer.compare(column.integers()[a], column.integers()[b]) : 0;
                    if (va && vb && column.descending()) sign = -sign;
                    if (sign != 0) return sign;
                }
                return Integer.compare(left, right);
            });
            int[] reference = new int[selectionSize];
            for (int i = 0; i < positions.length; i++) reference[i] = selected[positions[i]];
            for (int sample = 0; sample < 7; sample++) {
                int k = selectionSize == 0 ? 0 : random.nextInt(selectionSize + 1);
                require(Arrays.equals(Arrays.copyOf(reference, k), top.topK(selected, k)),
                        "stable occurrence-aware K prefix");
            }
            require(Arrays.equals(selected, snapshot), "source row selection immutable");
            if (n > 1) {
                int a = random.nextInt(n), b = random.nextInt(n);
                require(facts.orderOnly(a, b) == facts.compare(a, b).sign(),
                        "private primitive and public diagnostic comparison parity");
            }
            int[] state = new int[n];
            int count = 0;
            for (int i = 0; i < n; i++) {
                state[i] = random.nextBoolean() ? Integer.MIN_VALUE : 0;
                if (state[i] < 0) count++;
            }
            int[] matching = new int[count];
            for (int i = 0, at = 0; i < n; i++) if (state[i] < 0) matching[at++] = i;
            int[] sorted = facts.sortSelected(matching);
            // The policy is a permutation; use the configured policy's complete K prefix.
            int[] full = top.topK(matching, matching.length);
            require(full.length == sorted.length, "bounded selection cardinality");
            int limit = random.nextInt(n + 3);
            require(Arrays.equals(Arrays.copyOf(full, Math.min(limit, full.length)),
                    top.topKByState(state, Integer.MIN_VALUE, 0, limit)),
                    "streamed signed state-mask selection parity");
        }
        M3SignalColumns one = new M3SignalColumns(1,
                new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                        M3SignalColumns.Origin.STATIC, 42, false,
                        M3SignalColumns.allValid(1), null, new long[]{3}, null));
        M3SignalTopK top = new M3SignalTopK(one, 0);
        rejects(() -> top.topK(new int[]{0}, 2), IllegalArgumentException.class);
        rejects(() -> top.topK(new int[]{4}, 0), IndexOutOfBoundsException.class);
        rejects(() -> new M3SignalTopK(one, 0, 0), IllegalArgumentException.class);
        System.out.println("PASS M3SignalTopKSelfTest checks=" + checks);
    }
    private static void require(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label + " check=" + checks);
    }
    private static void rejects(Runnable action, Class<? extends Throwable> kind) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("expected " + kind.getName());
    }
}
