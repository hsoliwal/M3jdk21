// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Java21 and checked C17 JNI oracle for stable signed-long index sorting. */
public final class M3LongSharedRadixProof {
    private static long checks;
    private M3LongSharedRadixProof() { }

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label + " check=" + checks);
    }

    private static void rejects(Class<? extends Throwable> type, Runnable task) {
        try {
            task.run();
        } catch (Throwable actual) {
            check(type.isInstance(actual), "wrong exception " + actual);
            return;
        }
        throw new AssertionError("missing exception " + type);
    }

    private static void verify(M3LongIndexProjection view, boolean realJni) {
        int n = view.size();
        int[] physical = view.physicalIndices();
        long[] encounter = view.toArray();
        for (boolean descending : new boolean[] {false, true}) {
            Integer[] oracle = new Integer[n];
            for (int i = 0; i < n; i++) oracle[i] = i;
            Comparator<Integer> compare = (a, b) -> descending
                    ? Long.compare(encounter[b], encounter[a])
                    : Long.compare(encounter[a], encounter[b]);
            Arrays.sort(oracle, compare);
            M3LongIndexProjection java = descending
                    ? view.sortedDescending(M3SignalOrder.Backend.JAVA)
                    : view.sortedAscending(M3SignalOrder.Backend.JAVA);
            M3LongIndexProjection original = descending
                    ? view.sortedDescending() : view.sortedAscending();
            check(java.size() == n && original.size() == n, "length");
            check(java.order() == (descending
                    ? M3LongIndexProjection.Order.DESCENDING
                    : M3LongIndexProjection.Order.ASCENDING), "order metadata");
            for (int i = 0; i < n; i++) {
                check(java.physicalIndex(i) == physical[oracle[i]], "stable radix indexes");
                check(original.physicalIndex(i) == physical[oracle[i]], "merge/radix parity");
                check(java.get(i) == encounter[oracle[i]], "exact long payload");
            }
            if (realJni) {
                M3LongIndexProjection nativeOrder = descending
                        ? view.sortedDescending(M3SignalOrder.Backend.NATIVE_REQUIRED)
                        : view.sortedAscending(M3SignalOrder.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(java.physicalIndices(), nativeOrder.physicalIndices()),
                        "real native index parity");
                check(Arrays.equals(java.toArray(), nativeOrder.toArray()),
                        "real native value parity");
            }
        }
    }

    public static void main(String[] arguments) {
        boolean realJni = arguments.length > 0 && arguments[0].equals("jni");
        if (realJni) check(M3SignalOrderNative.available(), "native backend required");
        Random r = new Random(20261017L);
        for (int trial = 0; trial < 2_048; trial++) {
            int n = r.nextInt(160);
            long[] raw = new long[n];
            for (int i = 0; i < n; i++) {
                raw[i] = switch (r.nextInt(8)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    case 3 -> -1L;
                    default -> r.nextInt(9) - 4L;
                };
            }
            M3LongIndexProjection source = M3LongIndexProjection.snapshot(raw);
            M3LongIndexProjection view = trial % 2 == 0 ? source
                    : source.sortedDescending();
            int from = r.nextInt(n + 1);
            int width = r.nextInt(n - from + 1);
            verify(view.slice(from, width), realJni && trial < 64);
            if (n > 0) {
                long retained = source.get(0);
                raw[0] ^= -1L;
                check(source.get(0) == retained, "canonical payload detached");
            }
        }
        for (int trial = 0; trial < 28; trial++) {
            int n = 4096 + r.nextInt(4096);
            long[] raw = new long[n];
            for (int i = 0; i < n; i++) {
                raw[i] = switch (i % 7) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    default -> r.nextLong();
                };
            }
            M3LongIndexProjection source = M3LongIndexProjection.snapshot(raw);
            int from = r.nextInt(20);
            verify(source.slice(from, n - from), realJni && trial < 7);
        }
        M3LongIndexProjection empty = M3LongIndexProjection.snapshot(new long[0]);
        check(empty.sortedAscending(M3SignalOrder.Backend.JAVA).isEmpty(), "empty");
        rejects(NullPointerException.class, () -> empty.sortedDescending(null));
        if (!realJni && !M3SignalOrderNative.available()) {
            rejects(UnsatisfiedLinkError.class, () -> empty.sortedAscending(
                    M3SignalOrder.Backend.NATIVE_REQUIRED));
        }
        check(Arrays.equals(new long[] {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE},
                M3LongIndexProjection.snapshot(
                        new long[] {0, Long.MAX_VALUE, -1, Long.MIN_VALUE})
                        .sortedAscending(M3SignalOrder.Backend.JAVA).toArray()),
                "signed monotone normalized key");
        System.out.println("M3_LONG_SHARED_RADIX_PASS checks=" + checks
                + " real_jni=" + realJni);
    }
}
