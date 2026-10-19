/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongPredicate;

/** Plain Java 21 differential/metamorphic suite. No JUnit dependency. */
public final class M3LongBitmapFilteredSnapshotSelfTest {
    private static long checks;
    private M3LongBitmapFilteredSnapshotSelfTest() { }

    private static void check(boolean good, String message) {
        checks++;
        if (!good) throw new AssertionError(message);
    }

    private static void equal(long[] expected, long[] actual, String message) {
        check(Arrays.equals(expected, actual), message + " expected="
                + Arrays.toString(expected) + " actual=" + Arrays.toString(actual));
    }

    private static void fails(Class<? extends Throwable> expected, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError("unexpected " + t + ", expected " + expected, t);
        }
        throw new AssertionError("expected failure: " + expected);
    }

    private static void membershipOracle(M3LongBitmapFilteredSnapshot snapshot) {
        long[] values = snapshot.toArray();
        long[] probes = {Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1,
                Long.MAX_VALUE, -17, -1, 0, 1, 17};
        for (long value : values) {
            membershipProbe(snapshot, values, value);
            if (value > Long.MIN_VALUE) membershipProbe(snapshot, values, value - 1);
            if (value < Long.MAX_VALUE) membershipProbe(snapshot, values, value + 1);
        }
        for (long value : probes) membershipProbe(snapshot, values, value);
    }

    private static void membershipProbe(M3LongBitmapFilteredSnapshot snapshot,
                                         long[] values, long probe) {
        boolean expected = false;
        for (long value : values) if (value == probe) expected = true;
        check(snapshot.contains(probe) == expected, "primitive membership " + probe);
    }

    private static long[] valuesFrom(List<Integer> selected, long[] original) {
        long[] values = new long[selected.size()];
        for (int i = 0; i < selected.size(); i++) values[i] = original[selected.get(i)];
        return values;
    }

    private static void selectedOracle(long[] original, int offset, int size,
                                       boolean descending, int divisor) {
        List<Integer> originalIds = new ArrayList<>();
        for (int i = offset; i < offset + size; i++) originalIds.add(i);
        Comparator<Integer> c = Comparator.comparingLong(i -> original[i]);
        if (descending) c = c.reversed();
        originalIds.sort(c); // Stable JDK reference, duplicates allowed.
        List<Integer> filteredIds = new ArrayList<>();
        for (Integer id : originalIds) {
            if (original[id] % divisor == 0) filteredIds.add(id);
        }
        int[] calls = {0};
        M3LongIndexProjection initial = M3LongIndexProjection.snapshot(original).slice(offset, size);
        M3LongIndexProjection sorted = descending ? initial.sortedDescending() : initial.sortedAscending();
        M3LongBitmapFilteredSnapshot cached = M3LongBitmapFilteredSnapshot.fromProjection(
                sorted, value -> { calls[0]++; return value % divisor == 0; },
                M3IndexMaskCompactor.Backend.JAVA);
        check(calls[0] == size, "exactly one predicate evaluation per candidate");
        check(cached.size() == filteredIds.size(), "bitmap cardinality");
        equal(valuesFrom(filteredIds, original), cached.toArray(), "stable selected output");
        check(cached.physicalMaskCopy().cardinality() == cached.size(), "physical mask count");
        for (int i = 0; i < cached.size(); i++) {
            check(cached.physicalIndex(i) == filteredIds.get(i), "stable original ID");
            check(cached.get(i) == original[filteredIds.get(i)], "ordinal value");
            check(cached.includesPhysicalIndex(filteredIds.get(i)), "physical membership");
        }
        membershipOracle(cached);
        long[] selected = cached.toArray();
        M3LongBitmapFilteredSnapshot ascending = cached.sortedAscending();
        M3LongBitmapFilteredSnapshot descendingView = cached.sortedDescending();
        check(calls[0] == size, "sorting must not re-evaluate predicate");
        long[] ascendingOracle = selected.clone();
        Arrays.sort(ascendingOracle);
        equal(ascendingOracle, ascending.toArray(), "ascending cached selection");
        long[] descendingOracle = ascendingOracle.clone();
        for (int i = 0; i < descendingOracle.length / 2; i++) {
            long temp = descendingOracle[i];
            descendingOracle[i] = descendingOracle[descendingOracle.length - 1 - i];
            descendingOracle[descendingOracle.length - 1 - i] = temp;
        }
        equal(descendingOracle, descendingView.toArray(), "descending cached selection");
        membershipOracle(ascending);
        membershipOracle(descendingView);
        M3LongBitmapFilteredSnapshot sliced = ascending.slice(0, ascending.size());
        check(sliced == ascending, "full range is metadata identity");
        M3LongBitmapFilteredSnapshot tail = ascending.slice(ascending.size(), 0);
        check(tail.size() == 0 && tail.physicalMaskCopy().isEmpty(), "empty tail bitmap");
        membershipOracle(tail);
        if (ascending.size() > 2) {
            M3LongBitmapFilteredSnapshot section = ascending.slice(1, ascending.size() - 2);
            equal(Arrays.copyOfRange(ascendingOracle, 1, ascendingOracle.length - 1),
                    section.toArray(), "offset selection");
            check(section.physicalMaskCopy().cardinality() == section.size(), "range bit set");
            membershipOracle(section);
            membershipOracle(descendingView.slice(1, descendingView.size() - 2));
        }
        BitSet leaked = cached.physicalMaskCopy();
        leaked.clear();
        check(cached.physicalMaskCopy().cardinality() == cached.size(), "defensive bitset");
        int encounter = 0;
        M3LongIterator cursor = cached.iterator();
        while (cursor.hasNext()) check(cursor.nextLong() == selected[encounter++], "cursor");
        check(encounter == selected.length, "iterator exhaustion");
        fails(NoSuchElementException.class, cursor::nextLong);
        check(cached.longStream().count() == selected.length, "stream size");
        equal(selected, cached.parallelLongStream().toArray(), "primitive split stream");
        Spliterator.OfLong split = cached.longSpliterator();
        check((split.characteristics() & (Spliterator.SIZED | Spliterator.SUBSIZED))
                == (Spliterator.SIZED | Spliterator.SUBSIZED), "sized split invariants");
        long[] escaped = cached.toArray();
        if (escaped.length > 0) escaped[0] = Long.MIN_VALUE + 1;
        equal(selected, cached.toArray(), "payload isolation");
        check(calls[0] == size, "all repeated reads must not re-evaluate predicate");
        fails(IndexOutOfBoundsException.class, () -> cached.slice(-1, 0));
        fails(IndexOutOfBoundsException.class, () -> cached.slice(cached.size(), 1));
        fails(IndexOutOfBoundsException.class, () -> cached.get(cached.size()));
        fails(UnsupportedOperationException.class, cached::clear);

        AtomicInteger nextCalls = new AtomicInteger();
        M3LongBitmapFilteredSnapshot refined = cached.filtered(v -> {
            nextCalls.incrementAndGet();
            return (v & 1) == 0;
        });
        check(nextCalls.get() == cached.size(), "only previously accepted values retested");
        check(refined.size() <= cached.size(), "refinement is monotone");
        membershipOracle(refined);
        for (long value : refined.toArray()) check((value & 1) == 0, "refinement retained value");
        check(calls[0] == size, "previous predicate never recomputed by refinement");

        if (M3IndexMaskCompactor.nativeAvailable()) {
            AtomicInteger nativeCalls = new AtomicInteger();
            M3LongBitmapFilteredSnapshot jni = M3LongBitmapFilteredSnapshot.fromProjection(
                    sorted, v -> { nativeCalls.incrementAndGet(); return v % divisor == 0; },
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            equal(cached.toArray(), jni.toArray(), "actual JNI primitive selection parity");
            membershipOracle(jni);
            check(Arrays.equals(cached.physicalMaskCopy().toLongArray(),
                    jni.physicalMaskCopy().toLongArray()), "native bitmap parity");
            check(nativeCalls.get() == size, "JNI cannot call Java predicate again");
        } else {
            AtomicInteger nativeCalls = new AtomicInteger();
            fails(UnsatisfiedLinkError.class, () -> M3LongBitmapFilteredSnapshot.fromProjection(
                    sorted, v -> { nativeCalls.incrementAndGet(); return true; },
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            check(nativeCalls.get() == 0, "native unavailable refuses before predicate callbacks");
        }
    }

    private static final class MutableSource implements M3LongCollection {
        private long[] items;
        MutableSource(long... items) { this.items = items.clone(); }
        void replace(long... value) { items = value.clone(); }
        @Override public int size() { return items.length; }
        @Override public boolean contains(long value) {
            for (long element : items) if (element == value) return true;
            return false;
        }
        @Override public void clear() { items = new long[0]; }
        @Override public long[] toArray() { return items.clone(); }
        @Override public M3LongIterator iterator() {
            long[] capture = items;
            return new M3LongIterator() {
                int index;
                @Override public boolean hasNext() { return index < capture.length; }
                @Override public long nextLong() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return capture[index++];
                }
            };
        }
    }

    private static void verifyEpoch() {
        MutableSource source = new MutableSource(1, 2, 3, 4, 5);
        AtomicLong epoch = new AtomicLong();
        AtomicInteger callbacks = new AtomicInteger();
        long[] bound = {2};
        M3LongBitmapFilteredSnapshot.VersionedView cached =
                new M3LongBitmapFilteredSnapshot.VersionedView(source, v -> {
                    callbacks.incrementAndGet();
                    return v > bound[0];
                }, epoch::get);
        equal(new long[]{3, 4, 5}, cached.toArray(), "initial epoch view");
        for (int i = 0; i < 10; i++) {
            check(cached.size() == 3 && cached.contains(4), "repeat same revision");
            equal(new long[]{3, 4, 5}, cached.toArray(), "cached bitmap snapshot");
        }
        check(callbacks.get() == 5, "same epoch never retests");
        M3LongBitmapFilteredSnapshot asc = cached.sortedAscending();
        check(cached.sortedAscending() == asc, "sorted wrapper reused by revision");
        check(callbacks.get() == 5, "cached sort must not retest");
        M3LongIterator old = cached.iterator();
        check(old.nextLong() == 3L, "iterator begins");
        source.replace(5, 4, 3, 2, 1, 0);
        epoch.incrementAndGet();
        fails(ConcurrentModificationException.class, old::hasNext);
        equal(new long[]{5, 4, 3}, cached.toArray(), "new revision refresh");
        check(callbacks.get() == 11, "fresh epoch only re-tests once");
        check(cached.sortedAscending() != asc, "revision invalidates sorted sidecars");
        bound[0] = 4; // caller must report predicate state changes too.
        epoch.incrementAndGet();
        equal(new long[]{5}, cached.toArray(), "predicate revision refresh");
        check(callbacks.get() == 17, "predicate version refresh once");
        cached.invalidate();
        equal(new long[]{5}, cached.toArray(), "explicit invalidation");
        check(callbacks.get() == 23, "explicit invalidation recomputes exactly once");
        fails(UnsupportedOperationException.class, cached::clear);
        AtomicLong changing = new AtomicLong();
        AtomicInteger attempt = new AtomicInteger();
        M3LongBitmapFilteredSnapshot.VersionedView bad =
                new M3LongBitmapFilteredSnapshot.VersionedView(source, v -> {
                    if (attempt.getAndIncrement() == 0) changing.incrementAndGet();
                    return true;
                }, changing::get);
        fails(ConcurrentModificationException.class, bad::toArray);
        equal(new long[]{5, 4, 3, 2, 1, 0}, bad.toArray(), "retry after rejected capture");
        check(attempt.get() == 12, "failed capture did not publish incomplete bitmap");
    }

    public static void main(String[] args) {
        fails(NullPointerException.class, () -> M3LongBitmapFilteredSnapshot.snapshot(null, v -> true));
        fails(NullPointerException.class, () -> M3LongBitmapFilteredSnapshot.snapshot(new long[0], null));
        fails(NullPointerException.class, () -> M3LongBitmapFilteredSnapshot.fromProjection(
                null, v -> true, M3IndexMaskCompactor.Backend.JAVA));
        M3LongBitmapFilteredSnapshot empty = M3LongBitmapFilteredSnapshot.snapshot(
                new long[0], v -> true);
        check(empty.isEmpty(), "empty snapshot");
        check(empty.physicalMaskCopy().isEmpty(), "empty mask");
        check(!empty.includesPhysicalIndex(-1), "negative physical handle");
        membershipOracle(empty);
        membershipOracle(empty.sortedAscending());
        membershipOracle(empty.sortedDescending());
        M3LongBitmapFilteredSnapshot singleton = M3LongBitmapFilteredSnapshot.snapshot(
                new long[]{Long.MAX_VALUE}, v -> true);
        membershipOracle(singleton);
        membershipOracle(singleton.sortedAscending());
        membershipOracle(singleton.sortedDescending());
        long[] extremes = {Long.MIN_VALUE, Long.MIN_VALUE, 0, 1, 1, Long.MAX_VALUE, -1};
        M3LongBitmapFilteredSnapshot encounter = M3LongBitmapFilteredSnapshot.snapshot(
                extremes, v -> true);
        membershipOracle(encounter);
        membershipOracle(encounter.slice(1, extremes.length - 2));
        membershipOracle(encounter.filtered(v -> v != 0));
        selectedOracle(extremes, 0, extremes.length, false, 1);
        selectedOracle(extremes, 0, extremes.length, true, 1);
        selectedOracle(extremes, 0, extremes.length, false, 2);
        selectedOracle(extremes, 1, 4, true, 3);
        long[] original = {1, 2, 2, 3, 4};
        M3LongBitmapFilteredSnapshot immutable = M3LongBitmapFilteredSnapshot.snapshot(
                original, v -> (v & 1) == 0);
        Arrays.fill(original, 0L);
        equal(new long[]{2, 2, 4}, immutable.toArray(), "caller array isolation");
        membershipOracle(immutable);
        check(immutable.physicalMaskCopy().get(1), "physical slot maps first duplicate");
        check(immutable.physicalMaskCopy().get(2), "physical slot maps second duplicate");
        verifyEpoch();
        Random r = new Random(0x20261017abcdL);
        for (int run = 0; run < 500; run++) {
            int n = r.nextInt(180);
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                int mode = r.nextInt(12);
                values[i] = mode == 0 ? Long.MIN_VALUE : mode == 1 ? Long.MAX_VALUE
                        : r.nextInt(33) - 16L;
            }
            int start = r.nextInt(n + 1);
            int width = r.nextInt(n - start + 1);
            selectedOracle(values, start, width, (run & 1) != 0, 1 + (run % 7));
        }
        System.out.println("PASS M3LongBitmapFilteredSnapshotSelfTest checks=" + checks
                + " native=" + M3IndexMaskCompactor.nativeAvailable());
    }
}
