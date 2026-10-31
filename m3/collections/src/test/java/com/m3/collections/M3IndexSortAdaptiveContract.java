// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/** Standalone Java 21 oracle for the explicit trusted prepared-index sorting path. */
public final class M3IndexSortAdaptiveContract {
    private static long checks;
    private M3IndexSortAdaptiveContract() { }

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason);
    }

    private static void same(int[] expected, int[] actual, String reason) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(reason + ": expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable op) {
        checks++;
        try { op.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("Wrong exception " + failure, failure);
        }
        throw new AssertionError("Missing " + type.getSimpleName());
    }

    public static long contracts() {
        checks = 0;
        expect(NullPointerException.class, () -> M3IndexSort.stablePrepared(null, x -> x, null));
        expect(NullPointerException.class, () -> M3IndexSort.stablePrepared(new int[0], null, null));
        expect(ClassCastException.class, () ->
                M3IndexSort.stablePrepared(new int[] {0, 1}, x -> new Object(), null));
        expect(NullPointerException.class, () ->
                M3IndexSort.stablePrepared(new int[] {0, 1}, x -> null, null));
        // The comparator exception is propagated without modifying the index lane.
        int[] exceptional = {2, 1, 0};
        RuntimeException sentinel = new IllegalStateException("source-order-callback");
        try {
            M3IndexSort.stablePrepared(exceptional, x -> x, (a, b) -> { throw sentinel; });
            throw new AssertionError("Comparator exception swallowed");
        } catch (RuntimeException actual) {
            check(actual == sentinel, "exception identity");
        }
        same(new int[] {2, 1, 0}, exceptional, "preflight failure leaves lane untouched");

        Random random = new Random(0x57A61B3L);
        int[] sizes = {0, 1, 2, 3, 7, 31, 32, 33, 64, 127, 128, 257, 512};
        for (int size : sizes) {
            for (int iteration = 0; iteration < 140; iteration++) {
                Map<Integer, Integer> keys = new HashMap<>();
                int[] positions = new int[size];
                // Includes duplicate *physical* coordinates and negative addresses;
                // no payload is copied and stable ties retain encounter order.
                for (int i = 0; i < size; i++) {
                    int address = random.nextInt(Math.max(1, size / 2 + 1)) - size;
                    positions[i] = address;
                    keys.putIfAbsent(address, random.nextInt(11) - 5);
                }
                IntFunction<Integer> valueAt = k -> Objects.requireNonNull(keys.get(k));
                Comparator<Integer> comparator = iteration % 3 == 0
                        ? Comparator.reverseOrder() : Comparator.naturalOrder();
                if (iteration % 3 == 2) comparator = Comparator.comparingInt(Math::abs);
                Comparator<Integer> comp = comparator;
                Integer[] boxed = Arrays.stream(positions).boxed().toArray(Integer[]::new);
                Arrays.sort(boxed, (a, b) -> comp.compare(valueAt.apply(a), valueAt.apply(b)));
                int[] expected = Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
                int[] actual = positions.clone();
                M3IndexSort.stablePrepared(actual, valueAt, comparator);
                same(expected, actual, "random size=" + size + " iteration=" + iteration);
            }
        }

        for (int size : new int[] {2, 3, 32, 33, 64, 512, 4096}) {
            int[] sorted = new int[size];
            for (int i = 0; i < size; i++) sorted[i] = i;
            AtomicInteger comparisons = new AtomicInteger();
            M3IndexSort.stablePrepared(sorted, i -> i, (a, b) -> {
                comparisons.incrementAndGet(); return a.compareTo(b);
            });
            check(comparisons.get() == size - 1, "linear fast-path comparisons n=" + size);
            for (int i = 0; i < size; i++) check(sorted[i] == i, "sorted order untouched");
        }
        // Explicit natural-order overload and physical negative coordinates.
        int[] natural = {3, -4, 1, 3, 0, -4};
        M3IndexSort.stablePrepared(natural, i -> i, null);
        same(new int[] {-4, -4, 0, 1, 3, 3}, natural, "Comparable order");
        System.out.println("PASS M3_INDEX_SORT_ADAPTIVE checks=" + checks);
        return checks;
    }

    public static void main(String[] ignored) { contracts(); }
}
