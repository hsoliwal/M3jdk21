// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;

/** Java 21 executable differential oracle, intentionally no JUnit or JNI dependency. */
public final class M3LongLongIndexedSortedViewProof {
    private M3LongLongIndexedSortedViewProof() { }

    public static void main(String[] arguments) {
        Random random = new Random(0x4D33434F4C4C4543L);
        long evaluations = 0;
        for (int n = 0; n < 70; n++) {
            for (int trial = 0; trial < 50; trial++) {
                long[] keys = new long[n];
                long[] values = new long[n];
                TreeMap<Long, Long> oracle = new TreeMap<>();
                for (int i = 0; i < n; i++) {
                    long key;
                    do {
                        key = random.nextLong(-1000, 1000);
                        if (i == 0) key = Long.MIN_VALUE;
                        if (i == 1) key = Long.MAX_VALUE;
                    } while (oracle.containsKey(key));
                    keys[i] = key;
                    values[i] = random.nextLong();
                    oracle.put(key, values[i]);
                }
                long[] originalKeys = keys.clone();
                long[] originalValues = values.clone();
                M3LongLongIndexedSortedView view = M3LongLongIndexedSortedView.snapshot(keys, values);
                require(view.size() == oracle.size(), "size");
                require(Arrays.equals(originalKeys, keys) && Arrays.equals(originalValues, values),
                        "snapshot mutates inputs");
                int row = 0;
                for (var entry : oracle.entrySet()) {
                    require(view.keyAt(row) == entry.getKey(), "ordered key");
                    require(view.valueAt(row) == entry.getValue(), "corresponding value");
                    require(view.getOrDefault(entry.getKey(), 123) == entry.getValue(), "lookup");
                    require(view.indexOfKey(entry.getKey()) == row, "exact index");
                    require(view.physicalIndex(row) >= 0 && view.physicalIndex(row) < n,
                            "physical slot");
                    require(view.rank(entry.getKey()) == row, "key rank");
                    row++;
                    evaluations += 6;
                }
                if (n == 0) {
                    expect(NoSuchElementException.class, view::firstKey);
                    expect(NoSuchElementException.class, view::lastKey);
                } else {
                    require(view.firstKey() == oracle.firstKey(), "first");
                    require(view.lastKey() == oracle.lastKey(), "last");
                }
                require(view.retainedPrimitiveBytes() == (long) n * (2 * Long.BYTES + Integer.BYTES),
                        "array-only payload accounting");
                keys = keys.clone();
                if (n > 0) {
                    keys[0] = 34902L;
                    require(view.containsKey(originalKeys[0]), "key snapshot identity");
                }
                for (int i = 0; i < 20; i++) {
                    long query = random.nextLong(-1500, 1500);
                    long fallback = 3000L;
                    require(view.floorKey(query, fallback) == or(oracle.floorKey(query), fallback), "floor");
                    require(view.ceilingKey(query, fallback) == or(oracle.ceilingKey(query), fallback), "ceiling");
                    require(view.lowerKey(query, fallback) == or(oracle.lowerKey(query), fallback), "lower");
                    require(view.higherKey(query, fallback) == or(oracle.higherKey(query), fallback), "higher");
                    require(view.rank(query) == oracle.headMap(query, false).size(), "rank arbitrary");
                    evaluations += 5;
                }
                byte[] states = new byte[n];
                for (int i = 0; i < n; i++) states[i] = (byte) ((i % 3 == 0 ? 1 : 0) | (i % 2 == 0 ? 2 : 0));
                M3LongLongIndexedSortedView filtered = view.filtered(states, 3);
                int count = 0;
                for (int i = 0; i < n; i++) {
                    if ((states[i] & 3) == 3) {
                        require(filtered.keyAt(count) == view.keyAt(i), "filtered ordered keys");
                        require(filtered.valueAt(count) == view.valueAt(i), "filtered ordered values");
                        count++;
                    }
                }
                require(filtered.size() == count, "filtered count");
                if (filtered.size() > 0) {
                    byte[] secondStates = new byte[filtered.size()];
                    Arrays.fill(secondStates, (byte) 0xff);
                    require(filtered.filtered(secondStates, 0xff) == filtered,
                            "all-selected immutable index shares identity");
                }
                byte[] emptyStates = new byte[view.size()];
                require(view.filtered(emptyStates, 1).isEmpty(), "zero-state selection");
                require(view.filtered(new byte[n], 0) == view, "empty mask selects all");
                int nSnapshot = n;
                expect(IllegalArgumentException.class,
                        () -> view.filtered(new byte[nSnapshot + 1], 1));
                expect(IllegalArgumentException.class,
                        () -> view.filtered(new byte[nSnapshot], 0x100));
                expect(IllegalArgumentException.class,
                        () -> view.subMap(100, 1));
                if (n > 0) {
                    var split = view.slice(0, Math.min(n, 4));
                    require(split.size() == Math.min(4, n), "slice size");
                    require(view.subMap(view.firstKey(), view.lastKey()).size() == n - 1,
                            "range boundary");
                    require(view.headMap(view.firstKey()).isEmpty(), "head exclusive");
                    require(view.tailMap(view.lastKey()).size() == 1, "tail inclusive");
                }
                if (!M3IndexMaskCompactor.nativeAvailable()) {
                    expect(UnsatisfiedLinkError.class, () -> view.filtered(
                            new byte[nSnapshot], 1, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                } else {
                    M3LongLongIndexedSortedView nativeSelected = view.filtered(
                            states, 3, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    require(Arrays.equals(nativeSelected.keys(), filtered.keys()), "JNI keys parity");
                    require(Arrays.equals(nativeSelected.values(), filtered.values()), "JNI values parity");
                }
                evaluations += n * 2L + 15;
            }
        }
        expect(IllegalArgumentException.class,
                () -> M3LongLongIndexedSortedView.snapshot(new long[]{2, 2}, new long[]{7, 8}));
        expect(IllegalArgumentException.class,
                () -> M3LongLongIndexedSortedView.snapshot(new long[]{2}, new long[0]));
        M3LongLongIndexedSortedView singleton = M3LongLongIndexedSortedView.snapshot(
                new long[]{Long.MAX_VALUE}, new long[]{Long.MIN_VALUE});
        require(singleton.getOrDefault(Long.MAX_VALUE, 3) == Long.MIN_VALUE,
                "all signed-long keys and values allowed");
        System.out.println("M3_INDEXED_SORTED_MAP_PROOF_PASS evaluations=" + evaluations);
    }

    private static long or(Long actual, long fallback) {
        return actual == null ? fallback : actual.longValue();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected failure", failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
