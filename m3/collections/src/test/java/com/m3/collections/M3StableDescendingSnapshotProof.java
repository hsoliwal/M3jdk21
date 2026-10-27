/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
/*
 * Source-sealed Synexia recipe: M3JDK21 indexed stable descending groups.
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Differential oracle for stable comparator groups and 32-bit mask-first descending projections. */
public final class M3StableDescendingSnapshotProof {
    private static final Comparator<Integer> ORDER =
            Comparator.nullsFirst(Comparator.comparingInt(v -> Math.floorMod(v, 4)));
    private static long checks;
    private M3StableDescendingSnapshotProof() { }

    private static void yes(boolean pass, String reason) {
        checks++;
        if (!pass) throw new AssertionError(reason + " check=" + checks);
    }

    private static void same(Object want, Object got, String reason) {
        checks++;
        if (!Objects.equals(want, got)) {
            throw new AssertionError(reason + " expected=" + want + " actual=" + got);
        }
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable runnable, String label) {
        checks++;
        try {
            runnable.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError(label + " wrong exception " + thrown, thrown);
        }
        throw new AssertionError(label + " should have refused");
    }

    private static boolean selected(int[][] planes, int[] needs, int[] excludes, int physical) {
        for (int stage = 0; stage < planes.length; stage++) {
            int flags = planes[stage][physical];
            if ((flags & needs[stage]) != needs[stage]
                    || (flags & excludes[stage]) != 0) return false;
        }
        return true;
    }

    private static void verify(Integer[] input, Random random,
            M3IndexPathMasks.Backend backend) {
        M3IndexedListSnapshot<Integer> origin = M3IndexedListSnapshot.copyOf(input);
        int start = random.nextInt(input.length + 1);
        int count = random.nextInt(input.length - start + 1);
        M3IndexedListSnapshot<Integer> view = origin.slice(start, count);
        switch (random.nextInt(3)) {
            case 0 -> view = view.sorted(ORDER);
            case 1 -> view = view.sorted(ORDER.reversed());
            default -> { }
        }
        int[] retainedPositions = view.physicalIndices();
        Object[] retainedValues = view.toArray();
        int[][] planes = {new int[input.length], new int[input.length]};
        for (int[] lane : planes) for (int i = 0; i < lane.length; i++) lane[i] = random.nextInt();
        int[] needs = {random.nextBoolean() ? Integer.MIN_VALUE : 0,
                random.nextBoolean() ? 1 : 0};
        int[] excludes = {0, random.nextBoolean() ? 2 : 0};

        List<Integer> expectedOrdinals = new ArrayList<>();
        for (int i = 0; i < view.size(); i++) expectedOrdinals.add(view.physicalIndex(i));
        List<Integer> expectedDescending = new ArrayList<>(expectedOrdinals);
        // Reference JDK TimSort is stable for comparator ties, even when input is already sorted.
        expectedDescending.sort((a, b) -> ORDER.compare(input[b], input[a]));
        M3IndexedListSnapshot<Integer> descending = view.sortedDescending(ORDER);
        same(expectedDescending.size(), descending.size(), "descending length");
        for (int i = 0; i < expectedDescending.size(); i++) {
            same(expectedDescending.get(i), descending.physicalIndex(i), "descending ordinal");
        }

        List<Integer> admitted = new ArrayList<>();
        for (int physical : expectedOrdinals) {
            if (selected(planes, needs, excludes, physical)) admitted.add(physical);
        }
        admitted.sort((a, b) -> ORDER.compare(input[b], input[a]));
        AtomicInteger callbacks = new AtomicInteger();
        Comparator<Integer> observed = (a, b) -> {
            callbacks.incrementAndGet();
            return ORDER.compare(a, b);
        };
        M3IndexedListSnapshot<Integer> fused = view.sortedMaskedByPhysicalStatesDescending(
                planes, needs, excludes, observed, backend);
        same(admitted.size(), fused.size(), "mask-first descending length");
        for (int i = 0; i < admitted.size(); i++) {
            same(admitted.get(i), fused.physicalIndex(i), "mask-first descending ordinal");
        }
        if (admitted.size() < 2) same(0, callbacks.get(), "no comparison for fewer than two survivors");
        yes(Arrays.equals(retainedPositions, view.physicalIndices()), "input permutation unchanged");
        yes(Arrays.equals(retainedValues, view.toArray()), "input object lane unchanged");
        Arrays.fill(planes[0], 0);
        Arrays.fill(planes[1], -1);
        for (int i = 0; i < fused.size(); i++) {
            same(admitted.get(i), fused.physicalIndex(i), "no mask lane retained");
        }
        throwsType(UnsupportedOperationException.class, fused::clear, "immutable clear");
        throwsType(UnsupportedOperationException.class, () -> fused.addAll(List.of()),
                "immutable no-op addAll");
    }

    private static void edgeCases() {
        Integer[] values = {5, 1, 9, 2, 6, 10, 2};
        M3IndexedListSnapshot<Integer> source = M3IndexedListSnapshot.copyOf(values);
        M3IndexedListSnapshot<Integer> asc = source.sorted(ORDER);
        M3IndexedListSnapshot<Integer> desc = asc.sortedDescending(ORDER);
        int[] expected = asc.physicalIndices();
        Integer[] boxed = Arrays.stream(expected).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, (a, b) -> ORDER.compare(values[b], values[a]));
        int[] expectedIndices = Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
        yes(Arrays.equals(expectedIndices, desc.physicalIndices()), "equal groups preserve original encounter");

        // Comparison may return Integer.MIN_VALUE. Never reverse by negating a compare result.
        Comparator<Integer> extreme = (a, b) ->
                a.equals(b) ? 0 : a < b ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        var x = M3IndexedListSnapshot.copyOf(new Integer[] {3, 1, 2, 1})
                .sortedDescending(extreme);
        same(List.of(3, 2, 1, 1), x, "signed comparator result reversal");

        var all = M3IndexedListSnapshot.copyOf(new Integer[] {3, 2, 1});
        same(List.of(3, 2, 1), all.sortedDescending(), "natural reverse");
        var empty = M3IndexedListSnapshot.copyOf(new Integer[0]);
        same(List.of(), empty.sortedDescending(), "empty reverse");

        if (!M3IndexPathMasks.nativeAvailable()) {
            AtomicInteger callbacks = new AtomicInteger();
            throwsType(UnsatisfiedLinkError.class, () ->
                    all.sortedMaskedByPhysicalStatesDescending(
                        new int[][] {{1, 1, 1}}, new int[] {1}, new int[] {0},
                        (a, b) -> { callbacks.incrementAndGet(); return Integer.compare(a, b); },
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED),
                    "explicit native-required refusal");
            same(0, callbacks.get(), "JNI refusal before comparator callbacks");
        }
    }

    public static void main(String[] args) {
        boolean nativeRequired = args.length == 1 && "--native".equals(args[0]);
        if (nativeRequired && !M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("native library explicitly required for this run");
        }
        edgeCases();
        Random random = new Random(0x4d33535441424c45L);
        var backend = nativeRequired ? M3IndexPathMasks.Backend.NATIVE_REQUIRED
                : M3IndexPathMasks.Backend.JAVA;
        for (Integer[] input : new Integer[][] {
                {}, {null}, {0}, {null, 5, 9, 1, 1, null}, {4, 0, -4, 2, -2, 6}
        }) verify(input, random, backend);
        for (int trial = 0; trial < 2400; trial++) {
            Integer[] input = new Integer[random.nextInt(121)];
            for (int i = 0; i < input.length; i++) {
                input[i] = random.nextInt(8) == 0 ? null : random.nextInt(41) - 20;
            }
            verify(input, random, backend);
        }
        System.out.println("PASS M3StableDescendingSnapshotProof checks=" + checks
                + " backend=" + backend);
    }
}
