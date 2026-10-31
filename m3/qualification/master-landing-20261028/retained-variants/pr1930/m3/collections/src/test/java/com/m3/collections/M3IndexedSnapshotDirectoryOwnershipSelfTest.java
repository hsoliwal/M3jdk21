/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Standalone Java21 receiver regression for immutable index-directory sharing. */
public final class M3IndexedSnapshotDirectoryOwnershipSelfTest {
    private static int checks;
    private M3IndexedSnapshotDirectoryOwnershipSelfTest() { }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void equal(Object expected, Object actual, String reason) {
        check(Objects.equals(expected, actual), reason + " expected=" + expected + " actual=" + actual);
    }

    private static void indices(int[] expected, M3IndexedListSnapshot<?> actual, String reason) {
        check(Arrays.equals(expected, actual.physicalIndices()), reason + " expected="
                + Arrays.toString(expected) + " actual=" + Arrays.toString(actual.physicalIndices()));
    }

    private static void unchanged(Object[] values, int[] positions, BitSet membership,
            M3IndexedListSnapshot<?> actual, String reason) {
        Object[] observed = actual.toArray();
        check(values.length == observed.length, reason + " length");
        for (int i = 0; i < values.length; i++) {
            check(values[i] == observed[i], reason + " reference identity at " + i);
        }
        indices(positions, actual, reason + " positions");
        equal(membership, actual.physicalMaskCopy(), reason + " membership");
    }

    private static Object directory(M3IndexedListSnapshot<?> snapshot) throws Exception {
        Field field = M3IndexedListSnapshot.class.getDeclaredField("positions");
        field.setAccessible(true);
        return field.get(snapshot);
    }

    private static void expect(Class<? extends Throwable> type, Runnable task, String reason) {
        checks++;
        try { task.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(reason + " wrong failure", thrown);
        }
        throw new AssertionError(reason + " did not throw");
    }

    private static void allPassParentAndPriorProjections() {
        var owner = M3IndexedListSnapshot.copyOf(new Integer[] {4, 3, 2, 1});
        var priorSlice = owner.slice(0, 2);
        var priorAll = owner.filtered(x -> true).subList(0, 4);
        Object[] before = owner.toArray(), sliceBefore = priorSlice.toArray();
        int[] positions = owner.physicalIndices(), slicePositions = priorSlice.physicalIndices();
        BitSet membership = owner.physicalMaskCopy(), sliceMembership = priorSlice.physicalMaskCopy();
        var result = owner.filteredSorted(x -> true, Comparator.naturalOrder());
        equal(List.of(1, 2, 3, 4), result, "all-pass sorted result");
        // This fails for the naive donor-filter/master-sort union.
        unchanged(before, positions, membership, owner, "all-pass must preserve parent directory");
        unchanged(before, positions, membership, priorAll, "prior complete projection");
        unchanged(sliceBefore, slicePositions, sliceMembership, priorSlice, "prior slice");
        indices(new int[] {3, 2, 1, 0}, result, "sorted physical occurrences");
        unchanged(before, positions, membership, owner,
                "sorting a nested all-pass projection leaves root unchanged");
        var nestedResult = priorAll.filtered(x -> true).filteredSorted(x -> true, Comparator.naturalOrder());
        equal(result, nestedResult, "nested all-pass sorted result");
        unchanged(before, positions, membership, owner, "nested projection preserves root");
        unchanged(before, positions, membership, priorAll, "nested projection preserves prior");
    }

    private static void readEffectsAndComparatorFailure() {
        for (int failAt : new int[] {0, 1, 4, 5, 6}) {
            var owner = M3IndexedListSnapshot.copyOf(new Integer[] {8, 7, 6, 5, 4, 3, 2, 1});
            var prior = owner.filtered(x -> true);
            Object[] before = owner.toArray();
            int[] positions = owner.physicalIndices();
            BitSet mask = owner.physicalMaskCopy();
            AtomicInteger predicates = new AtomicInteger(), comparisons = new AtomicInteger();
            Runnable task = () -> owner.filteredSorted(value -> {
                equal(before[predicates.getAndIncrement()], value, "predicate encounter order");
                return true;
            }, (left, right) -> {
                unchanged(before, positions, mask, owner, "comparator observes immutable parent");
                unchanged(before, positions, mask, prior, "comparator observes immutable prior projection");
                if (comparisons.incrementAndGet() == failAt) throw new IllegalStateException("compare");
                return Integer.compare(left, right);
            });
            if (failAt == 0) task.run();
            else expect(IllegalStateException.class, task, "comparator failure propagates");
            equal(owner.size(), predicates.get(), "predicate is never repeated by directory detachment");
            unchanged(before, positions, mask, owner, "comparator completion/failure parent");
            unchanged(before, positions, mask, prior, "comparator completion/failure prior projection");
        }
        var owner = M3IndexedListSnapshot.copyOf(new Integer[] {4, 3, 2, 1});
        List<Integer> tape = new ArrayList<>();
        expect(IllegalArgumentException.class, () -> owner.filteredSorted(value -> {
            tape.add(value);
            if (value == 2) throw new IllegalArgumentException("predicate");
            return true;
        }, (left, right) -> { throw new AssertionError("comparator ran after predicate failure"); }),
                "predicate failure propagates");
        equal(List.of(4, 3, 2), tape, "predicate failure stops encounter scan");
        equal(List.of(4, 3, 2, 1), owner, "predicate failure preserves parent");
    }

    private static final class Item {
        final int key;
        final String label;
        Item(int key, String label) { this.key = key; this.label = label; }
        @Override public boolean equals(Object value) { return value instanceof Item item && key == item.key; }
        @Override public int hashCode() { return key; }
        @Override public String toString() { return label; }
    }

    private static void oracleAndStableOccurrences(boolean nativeRequired) {
        Item a = new Item(2, "a"), b = new Item(1, "b"), c = new Item(2, "c"), d = new Item(1, "d");
        Item[] palette = {a, b, null, c, a, d, null};
        Comparator<Item> order = Comparator.nullsFirst(Comparator.comparingInt(item -> item.key));
        Random random = new Random(0x4d33444952454354L);
        for (int length : new int[] {0, 1, 2, 4, 63, 64, 65, 127, 128, 129}) {
            Item[] payload = new Item[length];
            for (int i = 0; i < length; i++) payload[i] = palette[random.nextInt(palette.length)];
            var owner = M3IndexedListSnapshot.copyOf(payload);
            Object[] ownerBefore = owner.toArray();
            int[] ownerPositions = owner.physicalIndices();
            BitSet ownerMask = owner.physicalMaskCopy();
            for (int range = 0; range < 3; range++) {
                int begin = range == 0 ? 0 : length / 3;
                int end = range == 2 ? begin : length;
                var window = owner.subList(begin, end).sorted(order.reversed());
                Object[] before = window.toArray();
                int[] positions = window.physicalIndices();
                BitSet mask = window.physicalMaskCopy();
                for (int mode = 0; mode < 3; mode++) {
                    final int selection = mode;
                    Predicate<Item> predicate = item -> selection == 0
                            || selection == 1 && item != null && item.key == 2;
                    List<Integer> expected = new ArrayList<>();
                    for (int i = 0; i < window.size(); i++) {
                        if (predicate.test(window.get(i))) expected.add(window.physicalIndex(i));
                    }
                    expected.sort((left, right) -> order.compare(payload[left], payload[right]));
                    M3IndexMaskCompactor.Backend[] backends = nativeRequired
                            ? M3IndexMaskCompactor.Backend.values()
                            : new M3IndexMaskCompactor.Backend[] {M3IndexMaskCompactor.Backend.JAVA};
                    for (var backend : backends) {
                        AtomicInteger calls = new AtomicInteger();
                        var result = window.filteredSorted(item -> {
                            check(item == before[calls.getAndIncrement()], "predicate original reference/order");
                            return predicate.test(item);
                        }, order, backend);
                        equal(window.size(), calls.get(), "one predicate call per logical occurrence");
                        indices(expected.stream().mapToInt(Integer::intValue).toArray(), result,
                                "stable comparator/null/duplicate physical-index oracle");
                        for (int i = 0; i < result.size(); i++) {
                            check(result.get(i) == payload[expected.get(i)], "selected original reference identity");
                        }
                        unchanged(before, positions, mask, window, "slice/sorted parent unchanged");
                        unchanged(ownerBefore, ownerPositions, ownerMask, owner, "root unchanged after projection");
                    }
                }
            }
        }
    }

    private static void immutableAllPassReuse() throws Exception {
        var owner = M3IndexedListSnapshot.copyOf(new Integer[] {3, 2, 1});
        var all = owner.filtered(x -> true);
        check(directory(owner) == directory(all), "complete Java all-pass reuses immutable directory");
        var completeView = owner.subList(0, owner.size()).filtered(x -> true);
        check(directory(owner) == directory(completeView), "complete-span view retains all-pass reuse");
        var properSlice = owner.slice(1, 2).filtered(x -> true);
        check(directory(owner) != directory(properSlice), "proper-slice all-pass detaches exact directory");
        check(((int[]) directory(properSlice)).length == properSlice.size(), "slice does not retain larger directory");
        var sorted = all.filteredSorted(x -> true, Comparator.naturalOrder());
        check(directory(owner) != directory(sorted), "fused sort detaches shared immutable directory");
        equal(List.of(3, 2, 1), all, "all-pass remains unchanged after fused child sort");
    }

    private static void validationAndNativeRefusal(boolean nativeRequired) {
        var owner = M3IndexedListSnapshot.copyOf(new Integer[] {3, null, 2, 1});
        Comparator<Integer> order = Comparator.nullsFirst(Comparator.naturalOrder());
        expect(NullPointerException.class, () -> owner.filteredSorted(null, order), "null predicate");
        expect(NullPointerException.class, () -> owner.filteredSorted(x -> true, order, null), "null backend");
        expect(IndexOutOfBoundsException.class, () -> owner.slice(-1, 1), "negative slice");
        expect(IndexOutOfBoundsException.class, () -> owner.subList(0, 5), "end outside parent");
        equal(Arrays.asList(null, 1, 2, 3), owner.filteredSorted(x -> true, order), "null-aware comparator");
        equal(List.of(1, 2, 3), owner.filteredSorted(Objects::nonNull, null), "natural ordering");
        expect(NullPointerException.class, () -> owner.filteredSorted(x -> true, null), "natural null failure");
        equal(Arrays.asList(3, null, 2, 1), owner, "null comparison failure preserves parent");
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            AtomicInteger calls = new AtomicInteger();
            expect(UnsatisfiedLinkError.class, () -> owner.filteredSorted(x -> {
                calls.incrementAndGet(); return true;
            }, order, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED), "missing native backend refuses");
            equal(0, calls.get(), "native refusal precedes predicate");
        }
        expect(UnsupportedOperationException.class, () -> owner.sort(order), "immutable sort refusal");
    }

    public static void main(String[] args) throws Exception {
        boolean nativeRequired = Arrays.asList(args).contains("--native");
        boolean master = Arrays.asList(args).contains("--master");
        if (nativeRequired) check(M3IndexMaskCompactor.nativeAvailable(), "required real JNI library available");
        allPassParentAndPriorProjections();
        readEffectsAndComparatorFailure();
        oracleAndStableOccurrences(nativeRequired);
        if (!master) immutableAllPassReuse();
        validationAndNativeRefusal(nativeRequired);
        System.out.println("PASS M3IndexedSnapshotDirectoryOwnershipSelfTest checks=" + checks
                + " native=" + nativeRequired + " master=" + master);
    }
}
