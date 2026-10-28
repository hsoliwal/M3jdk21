/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Actual-owner regressions for callback state and packed occurrence selection. */
public final class M3CatalogueRefinementContract {
    private static int checks;
    private M3CatalogueRefinementContract() { }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void check(boolean value, String label) { equal(true, value, label); }

    private static void reentrant() {
        AtomicReference<Spliterator<Integer>> cursor = new AtomicReference<>();
        List<Integer> seen = new ArrayList<>();
        cursor.set(M3CollectionViews.filtered(List.of(1, 2, 3), value -> {
            if (value == 1) check(cursor.get().tryAdvance(seen::add), "nested predicate advance");
            return true;
        }).spliterator());
        check(cursor.get().tryAdvance(seen::add), "outer predicate advance");
        cursor.get().forEachRemaining(seen::add);
        equal(List.of(2, 1, 3), seen, "separate callback invocations");

        Spliterator<Integer> only = M3CollectionViews.filtered(List.of(9), value -> true).spliterator();
        check(only.tryAdvance(value -> check(!only.tryAdvance(ignored -> {
            throw new AssertionError("exhausted nested callback");
        }), "nested exhaustion")), "delivered outer value remains delivered");

        RuntimeException marker = new IllegalStateException("nested action marker");
        AtomicReference<Spliterator<Integer>> throwing = new AtomicReference<>();
        throwing.set(M3CollectionViews.filtered(List.of(1, 2, 3), value -> {
            if (value == 1) {
                try {
                    throwing.get().tryAdvance(ignored -> { throw marker; });
                    throw new AssertionError("nested action did not throw");
                } catch (RuntimeException actual) {
                    check(actual == marker, "nested exception identity");
                }
            }
            return true;
        }).spliterator());
        List<Integer> afterThrow = new ArrayList<>();
        check(throwing.get().tryAdvance(afterThrow::add), "outer callback restored after exception");
        throwing.get().forEachRemaining(afterThrow::add);
        equal(List.of(1, 3), afterThrow, "nested failure consumes only nested occurrence");
    }

    private static void invalidation() {
        M3LongArrayList source = M3LongArrayList.copyOf(new long[] {1, 2, 3});
        AtomicInteger calls = new AtomicInteger();
        AtomicLong revision = new AtomicLong();
        AtomicBoolean invalidateOnce = new AtomicBoolean(true);
        AtomicBoolean changeRevision = new AtomicBoolean();
        AtomicReference<M3LongBitmapFilteredSnapshot.VersionedView> owner = new AtomicReference<>();
        owner.set(new M3LongBitmapFilteredSnapshot.VersionedView(source, value -> {
            calls.incrementAndGet();
            if (invalidateOnce.getAndSet(false)) owner.get().invalidate();
            if (changeRevision.getAndSet(false)) revision.incrementAndGet();
            return value > 1;
        }, revision::get));
        M3LongBitmapFilteredSnapshot first = owner.get().snapshot();
        equal(3, calls.get(), "invalidation does not retry callbacks");
        M3LongBitmapFilteredSnapshot second = owner.get().snapshot();
        equal(6, calls.get(), "capture invalidation survives publication");
        check(first != second, "invalidated capture is not memoized");
        check(second == owner.get().snapshot(), "stable capture remains memoized");
        equal(6, calls.get(), "warm cache callback count");
        equal("[2, 3]", Arrays.toString(first.toArray()), "captured result remains usable");
        owner.get().invalidate();
        owner.get().snapshot();
        equal(9, calls.get(), "explicit invalidation recaptures");
        changeRevision.set(true);
        owner.get().invalidate();
        try {
            owner.get().snapshot();
            throw new AssertionError("changing revision admitted");
        } catch (ConcurrentModificationException expected) {
            equal(12, calls.get(), "revision failure does not retry callbacks");
        }
        owner.get().snapshot();
        equal(15, calls.get(), "failed revision capture is not cached");
    }

    private record Value(int key) { }

    private static void packed(M3IndexMaskCompactor.Backend backend) {
        Comparator<Value> order = Comparator.nullsFirst(Comparator.comparingInt(Value::key));
        for (int size : new int[] {0, 1, 2, 63, 64, 65, 127, 128, 129, 255, 256, 257, 1023}) {
            Value[] values = new Value[size + 4];
            Value shared = new Value(7);
            for (int i = 0; i < values.length; i++) {
                values[i] = i % 11 == 0 ? null : i % 7 == 0 ? shared : new Value((i * 31) % 19);
            }
            M3IndexedListSnapshot<Value> owner = M3IndexedListSnapshot.copyOf(values);
            M3IndexedListSnapshot<Value> view = owner.slice(2, size);
            Object[] original = owner.toArray();
            for (int mode = 0; mode < 4; mode++) {
                int selection = mode;
                AtomicInteger calls = new AtomicInteger();
                List<Value> expected = new ArrayList<>();
                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < view.size(); i++) {
                    Value value = view.get(i);
                    if (selection == 0 || selection == 2 && i % 3 == 0
                            || selection == 3 && value != null && value.key() % 2 == 0) {
                        expected.add(value);
                        indices.add(view.physicalIndex(i));
                    }
                }
                M3IndexedListSnapshot<Value> result = view.filtered(value -> {
                    int i = calls.getAndIncrement();
                    return selection == 0 || selection == 2 && i % 3 == 0
                            || selection == 3 && value != null && value.key() % 2 == 0;
                }, backend);
                equal(size, calls.get(), "one callback per candidate");
                equal(expected, result, "stable admitted occurrences");
                for (int i = 0; i < result.size(); i++) {
                    equal(indices.get(i), result.physicalIndex(i), "physical index");
                    check(expected.get(i) == result.get(i), "reference identity");
                }
            }
            M3IndexedListSnapshot<Value> sibling = view.filtered(value -> true, backend);
            List<Value> sortedExpected = new ArrayList<>(view);
            sortedExpected.sort(order);
            M3IndexedListSnapshot<Value> sorted = view.filteredSorted(value -> true, order, backend);
            equal(sortedExpected, sorted, "all-pass stable sort");
            check(Arrays.equals(original, owner.toArray()), "sort does not mutate owner directory");
            equal(Arrays.asList(Arrays.copyOfRange(original, 2, size + 2)), sibling,
                    "sort does not mutate sibling directory");
        }
    }

    private static void missingNative() {
        check(!M3IndexMaskCompactor.nativeAvailable(), "missing-library process");
        AtomicInteger calls = new AtomicInteger();
        try {
            M3IndexedListSnapshot.copyOf(List.of(1, 2)).filtered(value -> {
                calls.incrementAndGet(); return true;
            }, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            throw new AssertionError("missing native backend accepted");
        } catch (UnsatisfiedLinkError expected) {
            equal(0, calls.get(), "native refusal precedes callbacks");
        }
    }

    private static void nativeSelection() {
        check(M3IndexMaskCompactor.nativeAvailable(), "required real JNI loaded");
        for (int size : new int[] {0, 1, 255, 256, 257, 1025}) {
            int[] rows = new int[size + 4];
            long[] longs = new long[size + 4];
            byte[] states = new byte[size + 6];
            for (int i = 0; i < rows.length; i++) { rows[i] = i - 100; longs[i] = 1L << 40 | i; }
            for (int mask : new int[] {0, 1, 128, 255}) {
                int[] expected = M3IndexMaskCompactor.selectWindow(rows, 2, states, 3, size, mask,
                        M3IndexMaskCompactor.Backend.JAVA);
                int[] result = M3IndexMaskCompactor.selectWindow(rows, 2, states, 3, size, mask,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(expected, result), "real JNI int empty/identity selection");
                long[] expectedLong = M3IndexMaskCompactor.selectLongWindow(longs, 2, states, 3, size, mask,
                        M3IndexMaskCompactor.Backend.JAVA);
                long[] resultLong = M3IndexMaskCompactor.selectLongWindow(longs, 2, states, 3, size, mask,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(expectedLong, resultLong), "real JNI long empty/identity selection");
            }
            byte[] aligned = new byte[size];
            equal(0, M3IndexMaskCompactor.selectNativeRange(rows, 2, size, aligned, 1).length,
                    "real JNI offset range zero survivors");
        }
        packed(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
    }

    public static void main(String[] args) {
        checks = 0;
        String mode = args.length == 0 ? "java" : args[0];
        switch (mode) {
            case "reentrant" -> reentrant();
            case "invalidation" -> invalidation();
            case "packed" -> packed(M3IndexMaskCompactor.Backend.JAVA);
            case "missing-native" -> missingNative();
            case "native" -> nativeSelection();
            case "java" -> { reentrant(); invalidation(); packed(M3IndexMaskCompactor.Backend.JAVA); }
            default -> throw new IllegalArgumentException(mode);
        }
        System.out.println("PASS M3CatalogueRefinementContract mode=" + mode + " checks=" + checks);
    }
}
