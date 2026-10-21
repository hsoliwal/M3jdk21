// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** Frozen real-owner oracle: stable physical-index selection, ownership and callback effects. */
public final class M3MaskedSortContract {
    private static long checks;
    private static long digest = 1;
    private static volatile Object escaped;

    private M3MaskedSortContract() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(int[] expected, int[] actual, String message) {
        check(Arrays.equals(expected, actual), message);
        for (int value : actual) digest = digest * 31 + value;
    }

    private static void failure(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); throw new AssertionError("missing " + type.getName()); }
        catch (Throwable error) { check(type.isInstance(error), "wrong exception: " + error); }
    }

    private static int[] oracle(M3IndexedListSnapshot<Integer> view, int[] states,
            int required, int forbidden, Comparator<Integer> comparator) {
        List<Integer> logical = new ArrayList<>();
        for (int i = 0; i < view.size(); i++) {
            int word = states[view.physicalIndex(i)];
            if ((word & required) == required && (word & forbidden) == 0) logical.add(i);
        }
        logical.sort((a, b) -> comparator.compare(view.get(a), view.get(b)));
        return logical.stream().mapToInt(view::physicalIndex).toArray();
    }

    private static void spectrum(M3IndexPathMasks.Backend backend) {
        Comparator<Integer> order = Comparator.nullsLast(Comparator.naturalOrder());
        int[] masks = {0, 1, 2, 3, 0x80, 0x8000_0000, -1};
        for (int size : new int[]{0, 1, 2, 3, 31, 32, 33, 63, 64, 65, 127, 128, 129, 257}) {
            Integer[] data = new Integer[size];
            int[] states = new int[size];
            for (int i = 0; i < size; i++) {
                data[i] = i % 13 == 0 ? null : Integer.valueOf((i * 37) % 17 - 8);
                states[i] = i * 0x9e37_79b9;
            }
            M3IndexedListSnapshot<Integer> owner = M3IndexedListSnapshot.copyOf(data);
            List<M3IndexedListSnapshot<Integer>> views = List.of(owner, owner.sorted(order.reversed()),
                    owner.slice(size / 3, size - size / 3).slice(0, size - size / 3));
            for (M3IndexedListSnapshot<Integer> view : views) {
                int[] original = view.physicalIndices();
                Object[] originalValues = view.toArray();
                for (int required : masks) for (int forbidden : masks) {
                    List<String> tapeA = new ArrayList<>();
                    List<String> tapeB = new ArrayList<>();
                    Comparator<Integer> a = (left, right) -> {
                        tapeA.add(left + ":" + right); return order.compare(left, right);
                    };
                    Comparator<Integer> b = (left, right) -> {
                        tapeB.add(left + ":" + right); return order.compare(left, right);
                    };
                    var control = view.maskedByPhysicalState(states, required, forbidden, backend).sorted(a);
                    var actual = view.sortedMaskedByPhysicalState(states, required, forbidden, b, backend);
                    equal(oracle(view, states, required, forbidden, order), actual.physicalIndices(), "oracle");
                    equal(control.physicalIndices(), actual.physicalIndices(), "composition");
                    check(tapeA.equals(tapeB), "comparator tape");
                    equal(original, view.physicalIndices(), "source indices");
                    check(Arrays.equals(originalValues, view.toArray()), "source values");
                    check(actual != view, "result is detached facade");
                    for (int i = 0; i < actual.size(); i++) {
                        check(actual.get(i) == data[actual.physicalIndex(i)], "canonical payload identity");
                    }
                    int[] exported = actual.physicalIndices();
                    if (exported.length != 0) {
                        int first = actual.physicalIndex(0); exported[0] = -1;
                        check(actual.physicalIndex(0) == first, "defensive export");
                    }
                    failure(UnsupportedOperationException.class, () -> actual.removeIf(value -> false));
                    failure(UnsupportedOperationException.class, () -> actual.reversed().clear());
                }
            }
        }
        // Each state bit, including the sign bit, is a real full-width physical mask.
        var allBits = M3IndexedListSnapshot.copyOf(new Integer[]{3, 1, 1, 2});
        for (int bit = 0; bit < 32; bit++) {
            int mask = 1 << bit;
            equal(new int[]{1, 2, 0}, allBits.sortedMaskedByPhysicalState(
                    new int[]{mask, -1, mask, 0}, mask, 0, null, backend).physicalIndices(), "full-width mask");
        }
    }

    private static void effects(M3IndexPathMasks.Backend backend) {
        var owner = M3IndexedListSnapshot.copyOf(new Integer[]{3, 1, 2, 1});
        int[] before = owner.physicalIndices();
        int[] state = {1, 1, 1, 1};
        RuntimeException sentinel = new IllegalArgumentException("comparator identity");
        try {
            owner.sortedMaskedByPhysicalState(state, 1, 0, (a, b) -> { throw sentinel; }, backend);
            throw new AssertionError("missing comparator exception");
        } catch (RuntimeException observed) { check(observed == sentinel, "exception identity"); }
        equal(before, owner.physicalIndices(), "owner after exception");
        var sorted = owner.sortedMaskedByPhysicalState(state, 1, 0, (a, b) -> {
            Arrays.fill(state, 0); return Integer.compare(a, b);
        }, backend);
        equal(new int[]{1, 3, 2, 0}, sorted.physicalIndices(), "selection precedes comparator");
        equal(before, owner.physicalIndices(), "owner after callback writes");
        equal(new int[0], owner.sortedMaskedByPhysicalState(state, 1, 0,
                (a, b) -> { throw new AssertionError("empty comparison"); }, backend).physicalIndices(), "fresh states");
        failure(NullPointerException.class,
                () -> owner.sortedMaskedByPhysicalState(null, 0, 0, null, backend));
        failure(IllegalArgumentException.class,
                () -> owner.sortedMaskedByPhysicalState(new int[3], 0, 0, null, backend));
        failure(NullPointerException.class,
                () -> owner.sortedMaskedByPhysicalState(state, 0, 0, null, null));
        equal(new int[0], owner.sortedMaskedByPhysicalState(state, 1, 1, null, backend).physicalIndices(), "overlap");
        var nulls = M3IndexedListSnapshot.copyOf(new Integer[]{null, 1});
        failure(NullPointerException.class,
                () -> nulls.sortedMaskedByPhysicalState(new int[2], 0, 0, null, backend));
        var singletonNull = M3IndexedListSnapshot.copyOf(new Integer[]{null});
        equal(new int[]{0}, singletonNull.sortedMaskedByPhysicalState(new int[1], 0, 0,
                null, backend).physicalIndices(), "singleton does not compare");
    }

    private static void allocation() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new IllegalStateException("allocation counter unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        for (int size : new int[]{0, 1, 64, 256, 4096}) {
            Integer[] values = new Integer[size]; int[] states = new int[size];
            for (int i = 0; i < size; i++) { values[i] = i % 31; states[i] = i & 1; }
            var source = M3IndexedListSnapshot.copyOf(values);
            Supplier<Object> run = () -> source.sortedMaskedByPhysicalState(states, 1, 0,
                    Comparator.naturalOrder(), M3IndexPathMasks.Backend.JAVA);
            for (int i = 0; i < 3000; i++) escaped = run.get();
            long start = bean.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 3000; i++) escaped = run.get();
            long used = bean.getThreadAllocatedBytes(thread) - start;
            System.out.println("MASKED_SORT_ALLOCATION\tsize=" + size + "\tbytesPerOperation=" + used / 3000.0);
        }
        escaped = null;
    }

    public static void main(String[] args) {
        checks = 0; digest = 1;
        boolean nativeMode = args.length != 0 && args[0].equals("native");
        var backend = nativeMode ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
        if (nativeMode) check(M3IndexPathMasks.nativeAvailable(), "real JNI required");
        spectrum(backend); effects(backend);
        System.out.println("MASKED_SORT_CHECKS=" + checks + " DIGEST=" + digest);
        if (args.length != 0 && args[0].equals("measure")) allocation();
    }
}
