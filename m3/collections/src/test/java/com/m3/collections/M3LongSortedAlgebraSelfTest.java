// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.LongPredicate;

/**
 * Differential test of actual primitive sorted set algebra against JDK TreeSet.
 * Test-only TreeSet and Long boxing are independent oracles, never runtime storage.
 */
public final class M3LongSortedAlgebraSelfTest {
    private static long checks;
    private static final long FALLBACK = 0x6a7b4c3d5e2f1a09L;

    private M3LongSortedAlgebraSelfTest() { }

    public static void main(String[] args) throws Exception {
        for (int seed = 0; seed < 128; seed++) randomized(seed);
        singleSourceAndExtrema();
        liveOwnershipAndRefusals();
        sortedFilteredComposition();
        invalidOrderingRefuses();
        noRetainedPerEntryObjects();
        System.out.println("PASS M3LongSortedAlgebraSelfTest checks=" + checks);
    }

    private static void randomized(int seed) {
        Random random = new Random(0x4d33534554414c47L ^ seed);
        JdkSortedLongSet left = new JdkSortedLongSet(false);
        JdkSortedLongSet right = new JdkSortedLongSet(false);
        for (int i = 0; i < 90; i++) {
            if (random.nextBoolean()) left.add(value(random, i));
            if (random.nextBoolean()) right.add(value(random, i + 7));
        }

        M3LongSortedSet intersection = M3LongCompositeViews.sortedIntersection(left, right);
        M3LongSortedSet difference = M3LongCompositeViews.sortedDifference(left, right);
        M3LongSortedSet xor = M3LongCompositeViews.sortedSymmetricDifference(left, right);

        for (int phase = 0; phase < 8; phase++) {
            TreeSet<Long> expectedIntersection = new TreeSet<>(left.elements);
            expectedIntersection.retainAll(right.elements);
            TreeSet<Long> expectedDifference = new TreeSet<>(left.elements);
            expectedDifference.removeAll(right.elements);
            TreeSet<Long> expectedXor = new TreeSet<>(left.elements);
            expectedXor.addAll(right.elements);
            expectedXor.removeAll(expectedIntersection);

            verify(intersection, expectedIntersection, random);
            verify(difference, expectedDifference, random);
            verify(xor, expectedXor, random);

            // Factories are LIVE views: mutate the original owners after their creation.
            for (int j = 0; j < 9; j++) {
                long key = value(random, phase * 11 + j);
                if (random.nextBoolean()) left.add(key);
                else left.remove(key);
                if (random.nextBoolean()) right.add(key);
                else right.remove(key);
            }
        }
    }

    private static void verify(
            M3LongSortedSet view, NavigableSet<Long> expected, Random random) {
        long[] wanted = asLongs(expected);
        eq(wanted, view.toArray());
        check(view.size() == expected.size(), "size");
        check(view.isEmpty() == expected.isEmpty(), "isEmpty");
        M3LongIterator iter = view.iterator();
        for (long value : wanted) {
            check(iter.hasNext() && iter.hasNext(), "hasNext idempotent");
            check(iter.nextLong() == value, "ordered primitive iteration");
            check(view.contains(value), "present key membership");
        }
        check(!iter.hasNext() && !iter.hasNext(), "cursor exhausted");
        fails(NoSuchElementException.class, iter::nextLong);

        if (wanted.length == 0) {
            fails(NoSuchElementException.class, view::first);
            fails(NoSuchElementException.class, view::last);
        } else {
            check(view.first() == wanted[0], "first");
            check(view.last() == wanted[wanted.length - 1], "last");
            long[] detached = view.toArray();
            detached[0] ^= 0x3579L;
            eq(wanted, view.toArray());
        }

        long[] probes = {
            Long.MIN_VALUE, Long.MIN_VALUE + 1, -65L, -1L, 0L, 1L, 65L,
            Long.MAX_VALUE - 1, Long.MAX_VALUE, value(random, 31), value(random, 73)
        };
        for (long probe : probes) {
            check(view.contains(probe) == expected.contains(probe), "query membership");
            check(view.lower(probe, FALLBACK) == expectedOrFallback(
                    expected.lower(probe)), "lower");
            check(view.floor(probe, FALLBACK) == expectedOrFallback(
                    expected.floor(probe)), "floor");
            check(view.higher(probe, FALLBACK) == expectedOrFallback(
                    expected.higher(probe)), "higher");
            check(view.ceiling(probe, FALLBACK) == expectedOrFallback(
                    expected.ceiling(probe)), "ceiling");
        }

        Spliterator.OfLong split = view.longSpliterator();
        int characteristicFlags = Spliterator.ORDERED | Spliterator.SORTED
                | Spliterator.DISTINCT | Spliterator.NONNULL;
        check(split.hasCharacteristics(characteristicFlags), "sorted set characteristics");
        check(!split.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED),
                "live view cannot promise SIZED/SUBSIZED");
        check(split.getComparator() == null, "natural signed-long comparator");
        check(split.trySplit() == null, "no per-entry split buffers");
        eq(wanted, view.longStream().toArray());

        // Even a no-op public mutation on an empty live selection must refuse.
        fails(UnsupportedOperationException.class, () -> view.add(123L));
        fails(UnsupportedOperationException.class, () -> view.remove(123L));
        fails(UnsupportedOperationException.class, () -> view.filterInPlace(x -> true));
        fails(UnsupportedOperationException.class, view::clear);
    }

    private static void singleSourceAndExtrema() {
        JdkSortedLongSet owner = new JdkSortedLongSet(false);
        owner.add(Long.MIN_VALUE);
        owner.add(0L);
        owner.add(Long.MAX_VALUE);
        eq(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE},
                M3LongCompositeViews.sortedIntersection(owner, owner).toArray());
        eq(new long[0], M3LongCompositeViews.sortedDifference(owner, owner).toArray());
        eq(new long[0], M3LongCompositeViews.sortedSymmetricDifference(owner, owner).toArray());
        JdkSortedLongSet other = new JdkSortedLongSet(false);
        other.add(Long.MIN_VALUE);
        other.add(Long.MAX_VALUE);
        eq(new long[] {Long.MIN_VALUE, Long.MAX_VALUE},
                M3LongCompositeViews.sortedIntersection(owner, other).toArray());
        eq(new long[] {0L}, M3LongCompositeViews.sortedDifference(owner, other).toArray());
        eq(new long[] {0L}, M3LongCompositeViews.sortedSymmetricDifference(owner, other).toArray());

        JdkSortedLongSet empty = new JdkSortedLongSet(false);
        eq(new long[0], M3LongCompositeViews.sortedIntersection(empty, owner).toArray());
        eq(new long[0], M3LongCompositeViews.sortedDifference(empty, owner).toArray());
        eq(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE},
                M3LongCompositeViews.sortedDifference(owner, empty).toArray());
        eq(new long[] {Long.MIN_VALUE, 0L, Long.MAX_VALUE},
                M3LongCompositeViews.sortedSymmetricDifference(empty, owner).toArray());
    }

    private static void liveOwnershipAndRefusals() {
        JdkSortedLongSet first = new JdkSortedLongSet(false);
        JdkSortedLongSet second = new JdkSortedLongSet(false);
        first.add(1L);
        first.add(5L);
        second.add(5L);
        second.add(9L);
        M3LongSortedSet intersection = M3LongCompositeViews.sortedIntersection(first, second);
        M3LongSortedSet difference = M3LongCompositeViews.sortedDifference(first, second);
        M3LongSortedSet xor = M3LongCompositeViews.sortedSymmetricDifference(first, second);

        eq(new long[] {5L}, intersection.toArray());
        eq(new long[] {1L}, difference.toArray());
        eq(new long[] {1L, 9L}, xor.toArray());

        first.add(9L);
        eq(new long[] {5L, 9L}, intersection.toArray());
        eq(new long[] {1L}, difference.toArray());
        eq(new long[] {1L}, xor.toArray());

        second.remove(5L);
        eq(new long[] {9L}, intersection.toArray());
        eq(new long[] {1L, 5L}, difference.toArray());
        eq(new long[] {1L, 5L}, xor.toArray());

        fails(NullPointerException.class, () ->
                M3LongCompositeViews.sortedIntersection(null, first));
        fails(NullPointerException.class, () ->
                M3LongCompositeViews.sortedDifference(first, null));
        fails(NullPointerException.class, () ->
                M3LongCompositeViews.sortedSymmetricDifference(null, null));
    }

    private static void sortedFilteredComposition() {
        JdkSortedLongSet left = new JdkSortedLongSet(false);
        JdkSortedLongSet right = new JdkSortedLongSet(false);
        for (long k = -12; k <= 12; k++) {
            left.add(k);
            if ((k & 1L) == 0L) right.add(k);
        }
        M3LongSortedSet visible = M3LongSortedViews.filter(left, v -> v >= 0);
        M3LongSortedSet even = M3LongCompositeViews.sortedIntersection(visible, right);
        eq(new long[] {0, 2, 4, 6, 8, 10, 12}, even.toArray());
        eq(new long[] {1, 3, 5, 7, 9, 11},
                M3LongCompositeViews.sortedDifference(visible, right).toArray());
        M3LongSortedSet flipped = M3LongCompositeViews.sortedSymmetricDifference(visible, right);
        eq(new long[] {-12, -10, -8, -6, -4, -2, 1, 3, 5, 7, 9, 11}, flipped.toArray());
        // A detached snapshot stays independent while the live algebra changes.
        long[] snapshot = even.toArray();
        left.add(14);
        right.add(14);
        eq(new long[] {0, 2, 4, 6, 8, 10, 12}, snapshot);
        eq(new long[] {0, 2, 4, 6, 8, 10, 12, 14}, even.toArray());
    }

    private static void invalidOrderingRefuses() {
        JdkSortedLongSet descending = new JdkSortedLongSet(true);
        JdkSortedLongSet ascending = new JdkSortedLongSet(false);
        for (long k : new long[] {1L, 2L, 3L}) {
            descending.add(k);
            ascending.add(k);
        }
        fails(IllegalStateException.class, () ->
                M3LongCompositeViews.sortedIntersection(descending, ascending).toArray());
        fails(IllegalStateException.class, () ->
                M3LongCompositeViews.sortedDifference(descending, ascending).toArray());
        fails(IllegalStateException.class, () ->
                M3LongCompositeViews.sortedSymmetricDifference(descending, ascending).toArray());
    }

    private static void noRetainedPerEntryObjects() throws Exception {
        JdkSortedLongSet first = new JdkSortedLongSet(false);
        JdkSortedLongSet second = new JdkSortedLongSet(false);
        M3LongSortedSet algebra = M3LongCompositeViews.sortedIntersection(first, second);
        for (Class<?> owner : new Class<?>[] {
                algebra.getClass(), algebra.iterator().getClass()}) {
            for (Field field : owner.getDeclaredFields()) {
                check(!field.getType().isArray(), "no retained element or Object[] arrays");
                String type = field.getType().getName();
                check(!type.contains("Entry") && !type.contains("Node"),
                        "no structural entry/node field");
            }
        }
    }

    private static long expectedOrFallback(Long expected) {
        return expected == null ? FALLBACK : expected.longValue();
    }

    private static long[] asLongs(NavigableSet<Long> set) {
        long[] result = new long[set.size()];
        int i = 0;
        for (Long value : set) result[i++] = value.longValue();
        return result;
    }

    private static long value(Random random, int position) {
        return switch (position % 29) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            case 3 -> -1L;
            default -> random.nextInt(131) - 65L;
        };
    }

    private static void eq(long[] expected, long[] observed) {
        checks++;
        if (!Arrays.equals(expected, observed)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " observed " + Arrays.toString(observed));
        }
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("wrong exception", problem);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    /** Independent test oracle; boxed values are confined strictly to tests. */
    private static final class JdkSortedLongSet implements M3LongSortedSet {
        private final TreeSet<Long> elements = new TreeSet<>();
        private final boolean descendingTraversal;

        private JdkSortedLongSet(boolean descendingTraversal) {
            this.descendingTraversal = descendingTraversal;
        }

        @Override public boolean add(long key) { return elements.add(key); }
        @Override public boolean remove(long key) { return elements.remove(key); }
        @Override public void clear() { elements.clear(); }
        @Override public int size() { return elements.size(); }
        @Override public boolean contains(long key) { return elements.contains(key); }
        @Override public long[] toArray() { return asLongs(elements); }
        @Override public int filterInPlace(LongPredicate keep) {
            Objects.requireNonNull(keep, "keep");
            int previous = elements.size();
            elements.removeIf(value -> !keep.test(value));
            return previous - elements.size();
        }

        @Override public long first() { return elements.first(); }
        @Override public long last() { return elements.last(); }
        @Override public long lower(long v, long fallback) {
            Long result = elements.lower(v);
            return result == null ? fallback : result;
        }
        @Override public long floor(long v, long fallback) {
            Long result = elements.floor(v);
            return result == null ? fallback : result;
        }
        @Override public long higher(long v, long fallback) {
            Long result = elements.higher(v);
            return result == null ? fallback : result;
        }
        @Override public long ceiling(long v, long fallback) {
            Long result = elements.ceiling(v);
            return result == null ? fallback : result;
        }
        @Override public M3LongIterator iterator() {
            Iterator<Long> jdk = descendingTraversal
                    ? elements.descendingIterator() : elements.iterator();
            return new M3LongIterator() {
                @Override public boolean hasNext() { return jdk.hasNext(); }
                @Override public long nextLong() { return jdk.next(); }
            };
        }
    }
}
