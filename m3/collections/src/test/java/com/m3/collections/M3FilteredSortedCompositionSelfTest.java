/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.LongPredicate;

/**
 * No-Entry primitive filtered / sorted compositions against independent JDK
 * test oracles. Test-only TreeSet/boxed values never enter production storage.
 */
public final class M3FilteredSortedCompositionSelfTest {
    private static long checks;

    private M3FilteredSortedCompositionSelfTest() { }

    public static void main(String[] args) throws Exception {
        for (int seed = 0; seed < 96; seed++) {
            liveFilteredComposition(seed);
            primitiveSortedSnapshots(seed);
        }
        sortedSetWrapperContracts();
        invalidAndCallbackContracts();
        System.out.println("PASS M3FilteredSortedCompositionSelfTest checks=" + checks);
    }

    private static void liveFilteredComposition(int seed) throws Exception {
        Random random = new Random(0x4d3346494c544552L ^ seed);
        int count = random.nextInt(256);
        long[] values = new long[count];
        for (int i = 0; i < count; i++) values[i] = value(random, i);
        M3LongArrayList owner = M3LongArrayList.copyOf(values);
        M3LongFilteredView first = M3LongSortedViews.filter(
                (M3LongCollection) owner, v -> v >= -8);
        M3LongFilteredView nested = M3LongSortedViews.filter(
                (M3LongCollection) first, v -> (v & 1L) == 0L);
        long[] expected = Arrays.stream(values)
                .filter(v -> v >= -8 && (v & 1L) == 0L).toArray();
        eq(expected, nested.toArray());
        check(nested.size() == expected.length, "filtered live size");
        check(nested.isEmpty() == (expected.length == 0), "filtered live empty");
        Field backing = M3LongFilteredView.class.getDeclaredField("source");
        backing.setAccessible(true);
        check(backing.get(nested) == owner, "nested filters must flatten");
        fails(UnsupportedOperationException.class, nested::clear);
        M3LongCollection concatenated = M3LongCompositeViews.concat(nested, nested);
        long[] twice = new long[expected.length * 2];
        System.arraycopy(expected, 0, twice, 0, expected.length);
        System.arraycopy(expected, 0, twice, expected.length, expected.length);
        eq(twice, concatenated.toArray());

        owner.add(24L);
        long[] later = Arrays.copyOf(expected, expected.length + 1);
        later[later.length - 1] = 24L;
        eq(later, nested.toArray());
        check(nested.contains(24L), "live insertion visible");
        owner.remove(24L);
        eq(expected, nested.toArray());
    }

    private static void primitiveSortedSnapshots(int seed) throws Exception {
        Random random = new Random(0x4d33534f52544544L + seed);
        int length = random.nextInt(275);
        long[] input = new long[length];
        byte[] states = new byte[length];
        int requiredMask = seed & 255;
        for (int i = 0; i < length; i++) {
            input[i] = value(random, i);
            states[i] = (byte) random.nextInt(256);
        }
        M3LongArrayList owner = M3LongArrayList.copyOf(input);
        M3LongCollection sorted = M3LongSortedViews.sortedSnapshot(owner);
        long[] expectedSorted = input.clone();
        Arrays.sort(expectedSorted);
        eq(expectedSorted, sorted.toArray());
        eq(expectedSorted, sorted.parallelLongStream().toArray());
        check(sorted.size() == input.length, "snapshot preserves occurrences");
        check(sorted.longSpliterator().hasCharacteristics(Spliterator.ORDERED
                | Spliterator.SORTED | Spliterator.SIZED | Spliterator.SUBSIZED),
                "sorted primitive traversal");
        check(!sorted.longSpliterator().hasCharacteristics(Spliterator.DISTINCT),
                "duplicate-preserving sorted snapshot is not DISTINCT");
        check(sorted.longSpliterator().getComparator() == null, "natural comparator");
        fails(UnsupportedOperationException.class, sorted::clear);

        long[] direct = sorted.toArray();
        if (direct.length != 0) {
            direct[0] ^= 0x551L;
            eq(expectedSorted, sorted.toArray());
        }

        int[] calls = {0};
        LongPredicate predicate = v -> {
            calls[0]++;
            return v >= 0 && (v & 1L) == 0L;
        };
        M3LongCollection filtered = M3LongSortedViews.filteredSortedSnapshot(
                owner, predicate);
        check(calls[0] == input.length, "exactly one user-predicate call per value");
        long[] expectedFiltered = Arrays.stream(input)
                .filter(v -> v >= 0 && (v & 1L) == 0L).toArray();
        Arrays.sort(expectedFiltered);
        eq(expectedFiltered, filtered.toArray());
        eq(expectedFiltered, filtered.longStream().toArray());

        M3LongCollection masked = M3LongSortedViews.maskedSortedSnapshot(
                input, states, requiredMask);
        long[] maskedOracle = new long[input.length];
        int kept = 0;
        for (int i = 0; i < input.length; i++) {
            if (((states[i] & 255) & requiredMask) == requiredMask) {
                maskedOracle[kept++] = input[i];
            }
        }
        maskedOracle = Arrays.copyOf(maskedOracle, kept);
        Arrays.sort(maskedOracle);
        eq(maskedOracle, masked.toArray());
        if (input.length != 0) { input[0] = 9_999L; states[0] ^= 1; }
        owner.add(-9911L);
        eq(expectedSorted, sorted.toArray());
        eq(expectedFiltered, filtered.toArray());
        eq(maskedOracle, masked.toArray());

        for (Field field : sorted.getClass().getDeclaredFields()) {
            Class<?> type = field.getType();
            check(type.isArray() && type.getComponentType().isPrimitive(),
                    "only a primitive result lane may be retained");
        }
    }

    private static void sortedSetWrapperContracts() {
        TreePrimitiveSortedSet backing = new TreePrimitiveSortedSet();
        for (long value = -20; value <= 20; value++) backing.add(value);
        M3LongSortedSet filtered = M3LongSortedViews.filter(
                backing, v -> (v & 1L) == 0L);
        M3LongSortedSet bounded = M3LongSortedViews.subSet(filtered, -10, 13);
        eq(new long[] {-10, -8, -6, -4, -2, 0, 2, 4, 6, 8, 10, 12},
                bounded.toArray());
        check(bounded.first() == -10, "sorted view first");
        check(bounded.last() == 12, "sorted view last");
        check(bounded.lower(2, Long.MIN_VALUE) == 0L, "strict predecessor");
        check(bounded.higher(2, Long.MAX_VALUE) == 4L, "strict successor");
        check(bounded.floor(3, Long.MIN_VALUE) == 2L, "inclusive floor");
        check(bounded.ceiling(3, Long.MAX_VALUE) == 4L, "inclusive ceiling");
        check(!bounded.contains(13), "exclusive upper endpoint");
        M3LongSortedSet frozen = M3LongSortedViews.unmodifiable(bounded);
        fails(UnsupportedOperationException.class, () -> frozen.add(100));
        fails(UnsupportedOperationException.class, () -> frozen.remove(111));
        fails(UnsupportedOperationException.class, frozen::clear);
        fails(UnsupportedOperationException.class,
                () -> frozen.filterInPlace(v -> true));
        check(backing.add(100L), "backing remains writable");
        check(filtered.contains(100L), "generic sorted filter is live");
        check(!bounded.contains(100L), "bounded view excludes external insert");
        check(backing.remove(-10L), "remove backing endpoint");
        check(bounded.first() == -8L, "bounded live endpoint");
        M3LongSortedSet head = M3LongSortedViews.headSet(filtered, -3);
        check(head.last() == -4, "nested head sorted order");
        M3LongSortedSet tail = M3LongSortedViews.tailSet(filtered, 16);
        check(tail.first() == 16, "nested tail sorted order");
        fails(IllegalArgumentException.class,
                () -> M3LongSortedViews.subSet(filtered, 2, -2));
    }

    private static void invalidAndCallbackContracts() {
        M3LongArrayList source = M3LongArrayList.copyOf(new long[] {1, 2, 3});
        long[] before = source.toArray();
        fails(NullPointerException.class, () -> M3LongSortedViews.sortedSnapshot(null));
        fails(NullPointerException.class,
                () -> M3LongSortedViews.filteredSortedSnapshot(source, null));
        fails(NullPointerException.class,
                () -> M3LongSortedViews.maskedSortedSnapshot(null, new byte[0], 0));
        fails(IllegalArgumentException.class,
                () -> M3LongSortedViews.maskedSortedSnapshot(before, new byte[2], 0));
        fails(IllegalArgumentException.class,
                () -> M3LongSortedViews.maskedSortedSnapshot(before, new byte[3], 256));
        fails(IllegalStateException.class, () -> M3LongSortedViews.filteredSortedSnapshot(
                source, value -> {
                    if (value == 2) throw new IllegalStateException("predicate");
                    return true;
                }));
        eq(before, source.toArray());
        fails(ConcurrentModificationException.class,
                () -> M3LongSortedViews.filteredSortedSnapshot(source, value -> {
                    if (value == 2) source.add(99);
                    return true;
                }));
        check(source.contains(99), "caller callback may already have changed source");
        M3LongCollection empty = M3LongSortedViews.sortedSnapshot(new M3LongArrayList());
        check(empty.size() == 0, "empty snapshot");
        fails(NoSuchElementException.class, () -> empty.iterator().nextLong());
        fails(UnsupportedOperationException.class, empty::clear);
    }

    private static long value(Random random, int step) {
        return switch (step % 19) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> random.nextInt(97) - 48L;
        };
    }

    private static void eq(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " got " + Arrays.toString(actual));
        }
    }

    private static void check(boolean pass, String label) {
        checks++;
        if (!pass) throw new AssertionError(label);
    }

    private static void fails(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable thrown) {
            if (kind.isInstance(thrown)) return;
            throw new AssertionError("wrong exception type", thrown);
        }
        throw new AssertionError("expected " + kind.getSimpleName());
    }

    private static final class TreePrimitiveSortedSet implements M3LongSortedSet {
        private final TreeSet<Long> values = new TreeSet<>();
        public boolean add(long v) { return values.add(v); }
        public boolean remove(long v) { return values.remove(v); }
        public boolean contains(long v) { return values.contains(v); }
        public int size() { return values.size(); }
        public void clear() { values.clear(); }
        public long[] toArray() { return values.stream().mapToLong(Long::longValue).toArray(); }
        public long first() { return values.first(); }
        public long last() { return values.last(); }
        public long lower(long v, long fallback) {
            Long got = values.lower(v);
            return got == null ? fallback : got;
        }
        public long higher(long v, long fallback) {
            Long got = values.higher(v);
            return got == null ? fallback : got;
        }
        public long floor(long v, long fallback) {
            Long got = values.floor(v);
            return got == null ? fallback : got;
        }
        public long ceiling(long v, long fallback) {
            Long got = values.ceiling(v);
            return got == null ? fallback : got;
        }
        public int filterInPlace(LongPredicate keep) {
            Objects.requireNonNull(keep, "predicate");
            int before = size();
            values.removeIf(v -> !keep.test(v));
            return before - size();
        }
        public M3LongIterator iterator() {
            var input = values.iterator();
            return new M3LongIterator() {
                public boolean hasNext() { return input.hasNext(); }
                public long nextLong() { return input.next(); }
            };
        }
    }
}
