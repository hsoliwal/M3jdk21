/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Standalone JDK21 differential oracle for fused offset/mask selection and stable sort. */
public final class M3IndexedFilterSortFusionSelfTest {
    private static final Comparator<Integer> ORDER =
            Comparator.nullsFirst(Comparator.comparingInt(x -> Math.floorMod(x, 7)));
    private static long checks;
    private M3IndexedFilterSortFusionSelfTest() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " check=" + checks);
    }

    private static void same(Object expected, Object actual, String label) {
        check(Objects.equals(expected, actual),
                label + " expected=" + expected + " actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> type, Runnable work, String label) {
        checks++;
        try { work.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(label + " unexpected exception", failure);
        }
        throw new AssertionError(label + " did not throw");
    }

    private static boolean accepted(Integer value) {
        return value == null || Math.floorMod(value, 5) <= 2;
    }

    private static void exercise(Integer[] payload, Random random, boolean nativeRequired) {
        M3IndexedListSnapshot<Integer> owner = M3IndexedListSnapshot.copyOf(payload);
        Object[] sourceBefore = owner.toArray();
        int from = random.nextInt(owner.size() + 1);
        int to = from + random.nextInt(owner.size() - from + 1);
        M3IndexedListSnapshot<Integer> window = owner.slice(from, to - from);
        if (random.nextBoolean()) window = window.sorted(ORDER.reversed());

        M3IndexedListSnapshot<Integer> prior = window;
        List<Integer> baseline = new ArrayList<>();
        List<Integer> baselinePositions = new ArrayList<>();
        for (int i = 0; i < window.size(); i++) {
            Integer value = window.get(i);
            if (accepted(value)) {
                baseline.add(value);
                baselinePositions.add(window.physicalIndex(i));
            }
        }
        // List.sort is stable: comparator-equal occurrences retain source order.
        baselinePositions.sort((a, b) -> ORDER.compare(payload[a], payload[b]));
        baseline.sort(ORDER);

        M3IndexMaskCompactor.Backend[] backends = nativeRequired
                ? new M3IndexMaskCompactor.Backend[] {
                    M3IndexMaskCompactor.Backend.JAVA,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED}
                : new M3IndexMaskCompactor.Backend[] {
                    M3IndexMaskCompactor.Backend.JAVA};
        for (M3IndexMaskCompactor.Backend backend : backends) {
            AtomicInteger calls = new AtomicInteger();
            M3IndexedListSnapshot<Integer> fused = prior.filteredSorted(x -> {
                calls.incrementAndGet();
                return accepted(x);
            }, ORDER, backend);
            same(prior.size(), calls.get(), "one predicate call per occurrence");
            same(baseline, fused, "JDK sorted value oracle");
            same(baselinePositions.size(), fused.size(), "count");
            for (int i = 0; i < fused.size(); i++) {
                same(baselinePositions.get(i), fused.physicalIndex(i), "stable offset order");
            }
            BitSet expectedMask = new BitSet();
            for (int slot : baselinePositions) expectedMask.set(slot);
            same(expectedMask, fused.physicalMaskCopy(), "physical membership mask");
            same(prior.filtered(M3IndexedFilterSortFusionSelfTest::accepted, backend)
                    .sorted(ORDER), fused, "existing two-stage oracle");
            check(Arrays.equals(sourceBefore, owner.toArray()), "canonical payload not touched");
            same(prior, window, "prior window unchanged");
            expect(UnsupportedOperationException.class,
                    () -> fused.sort(ORDER), "immutable selected projection");
            if (from == 0 && to == payload.length) {
                same(fused, M3GenericCollections.filteredSortedSnapshot(
                        Arrays.asList(payload), M3IndexedFilterSortFusionSelfTest::accepted,
                        ORDER, backend), "factory path");
            }
        }
    }

    private static void validation() {
        M3IndexedListSnapshot<Integer> source =
                M3IndexedListSnapshot.copyOf(new Integer[] {null, 0, -1, 0, 6});
        expect(NullPointerException.class,
                () -> source.filteredSorted(null, ORDER), "null predicate");
        expect(NullPointerException.class,
                () -> source.filteredSorted(x -> true, ORDER, null), "null backend");
        same(List.of(), source.filteredSorted(x -> false, ORDER), "empty match");
        same(source.sorted(ORDER), source.filteredSorted(x -> true, ORDER), "all selected");
        same(List.of(), M3IndexedListSnapshot.copyOf(new Integer[0])
                .filteredSorted(x -> true, ORDER), "zero logical positions");
        expect(IllegalStateException.class,
                () -> source.filteredSorted(x -> {
                    if (Objects.equals(x, 0)) throw new IllegalStateException("filter");
                    return true;
                }, ORDER), "predicate exception");
        expect(IllegalStateException.class,
                () -> source.filteredSorted(x -> true,
                        (a, b) -> { throw new IllegalStateException("compare"); }),
                "comparator exception");
        same(Arrays.asList(null, 0, -1, 0, 6), source, "exception atomicity");
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            AtomicInteger predicateCalls = new AtomicInteger();
            expect(UnsatisfiedLinkError.class,
                    () -> source.filteredSorted(x -> {
                        predicateCalls.incrementAndGet(); return true;
                    }, ORDER, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                    "explicit missing native backend refusal");
            same(0, predicateCalls.get(), "native refusal before callback");
        }
        same(source.filteredSorted(Objects::nonNull, null),
                source.filtered(Objects::nonNull).sorted(),
                "natural comparison");
    }

    public static void main(String[] args) {
        boolean nativeRequired = args.length == 1 && "--native".equals(args[0]);
        if (nativeRequired && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("requested JNI backend unavailable");
        }
        validation();
        Random random = new Random(0x4d33464e44494458L);
        for (Integer[] input : new Integer[][] {
                {}, {null}, {0, 0, 0}, {null, -2, 5, 12, 5, null, 7},
                {21, -7, 0, 7, 14, 0, -14, null, 8}
        }) exercise(input, random, nativeRequired);
        for (int trial = 0; trial < 400; trial++) {
            Integer[] input = new Integer[random.nextInt(210)];
            for (int i = 0; i < input.length; i++) {
                input[i] = random.nextInt(9) == 0 ? null : random.nextInt(100) - 50;
            }
            exercise(input, random, nativeRequired);
        }
        System.out.println("PASS M3IndexedFilterSortFusionSelfTest checks=" + checks
                + " JNI=" + nativeRequired);
    }
}
