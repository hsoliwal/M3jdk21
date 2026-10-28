// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

/** Dependency-free deterministic differential regression, including the opt-in native lane. */
public final class M3LongRankIndexSelfTest {
    private M3LongRankIndexSelfTest() { }

    public static void main(String[] args) {
        fixedCases();
        randomizedCases();
        System.out.println("M3LongRankIndexSelfTest PASS native="
                + M3LongRankBatchNative.nativeAvailable());
    }

    private static void fixedCases() {
        M3LongRankIndex empty = M3LongRankIndex.copyOf(new long[0]);
        equal(0, empty.lowerBound(Long.MIN_VALUE));
        equal(0, empty.upperBound(Long.MAX_VALUE));
        equal(0, empty.rangeCountInclusive(Long.MIN_VALUE, Long.MAX_VALUE));
        equal(3, empty.lowerBounds(new long[]{-1, 0, 1}).length);
        long[] input = {Long.MAX_VALUE, Long.MIN_VALUE, -1, 0, 0, Long.MIN_VALUE};
        M3LongRankIndex index = M3LongRankIndex.copyOf(input);
        input[0] = 12345;
        equal(2, index.count(Long.MIN_VALUE));
        equal(2, index.count(0));
        equal(6, index.rangeCountInclusive(Long.MIN_VALUE, Long.MAX_VALUE));
        equal(5, index.rangeCount(Long.MIN_VALUE, Long.MAX_VALUE));
        equal(0, index.rankLessThan(Long.MIN_VALUE));
        equal(6, index.rankLessOrEqual(Long.MAX_VALUE));
        equal(Long.MIN_VALUE, index.select(0));
        equal(Long.MAX_VALUE, index.select(5));
        ArraySource source = new ArraySource(new long[]{100, 100, 50, 40, 40, 20, 10});
        M3LongRankIndex dense = M3LongRankIndex.snapshotFilteredDistinct(source, x -> x > 0);
        source.clear();
        equal(5, dense.size());
        equal(1, dense.denseRankDescending(120));
        equal(1, dense.denseRankDescending(100));
        equal(2, dense.denseRankDescending(50));
        equal(4, dense.denseRankDescending(25));
        equal(6, dense.denseRankDescending(5));
    }

    private static void randomizedCases() {
        final Random random = new Random(0x4D334B21L);
        for (int round = 0; round < 2500; round++) {
            int n = random.nextInt(140);
            long[] source = new long[n];
            ArraySource list = new ArraySource(source);
            TreeSet<Long> distinct = new TreeSet<>();
            for (int i = 0; i < n; i++) {
                long value = switch (random.nextInt(12)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> random.nextLong();
                    default -> random.nextInt(23) - 11L;
                };
                source[i] = value;
                distinct.add(value);
            }
            M3LongRankIndex index = M3LongRankIndex.snapshot(list);
            M3LongRankIndex distinctIndex = M3LongRankIndex.snapshotDistinct(list);
            long[] sorted = source.clone();
            Arrays.sort(sorted);
            arrayEqual(sorted, index.toArray());
            equal(distinct.size(), distinctIndex.size());
            for (int q = 0; q < 25; q++) {
                long value = q == 0 ? Long.MIN_VALUE : q == 1 ? Long.MAX_VALUE
                        : random.nextInt(29) - 14L;
                int lower = 0;
                int upper = 0;
                for (long v : sorted) {
                    if (v < value) lower++;
                    if (v <= value) upper++;
                }
                equal(lower, index.lowerBound(value));
                equal(upper, index.upperBound(value));
                equal(upper - lower, index.count(value));
                equal(upper > lower, index.contains(value));
                equal(1 + distinct.tailSet(value, false).size(),
                        distinctIndex.denseRankDescending(value));
            }
            long[] queries = new long[70 + random.nextInt(75)];
            for (int q = 0; q < queries.length; q++) queries[q] = random.nextInt(101) - 50L;
            int[] batched = index.lowerBounds(queries);
            int[] upper = index.upperBounds(queries);
            int[] upperInto = new int[queries.length + 4];
            Arrays.fill(upperInto, -123);
            index.upperBoundsInto(queries, 0, queries.length, upperInto, 2);
            int[] counts = index.counts(queries);
            int[] into = new int[queries.length + 4];
            Arrays.fill(into, -123);
            index.countInto(queries, 0, queries.length, into, 2);
            for (int q = 0; q < queries.length; q++) {
                equal(index.lowerBound(queries[q]), batched[q]);
                equal(index.upperBound(queries[q]), upper[q]);
                equal(upper[q], upperInto[q + 2]);
                equal(index.count(queries[q]), counts[q]);
                equal(counts[q], into[q + 2]);
            }
            equal(-123, upperInto[0]);
            equal(-123, upperInto[1]);
            equal(-123, upperInto[upperInto.length - 1]);
            equal(-123, upperInto[upperInto.length - 2]);
            // Validation must refuse before mutating any caller-owned output.
            try {
                index.upperBoundsInto(queries, queries.length, 1, upperInto, 2);
                fail("missing invalid query offset exception");
            } catch (IndexOutOfBoundsException expected) {
                equal(-123, upperInto[0]);
            }
            try {
                index.upperBoundsInto(queries, 0, queries.length, upperInto, 5);
                fail("missing invalid output offset exception");
            } catch (IndexOutOfBoundsException expected) {
                equal(-123, upperInto[0]);
            }
            equal(-123, into[0]);
            equal(-123, into[1]);
            equal(-123, into[into.length - 1]);
            equal(-123, into[into.length - 2]);
            M3LongRankIndex filtered = M3LongRankIndex.snapshotFiltered(list, v -> v < 0);
            for (long v : filtered.toArray()) if (!(v < 0)) fail("filter");
        }
    }

    private static final class ArraySource implements M3LongCollection {
        private final long[] values;
        private int length;

        private ArraySource(long[] values) {
            this.values = values;
            this.length = values.length;
        }

        @Override public int size() { return length; }
        @Override public boolean contains(long value) {
            for (int i = 0; i < length; i++) if (values[i] == value) return true;
            return false;
        }
        @Override public void clear() { length = 0; }
        @Override public long[] toArray() { return Arrays.copyOf(values, length); }
        @Override public M3LongIterator iterator() {
            return new M3LongIterator() {
                private int position;
                @Override public boolean hasNext() { return position < length; }
                @Override public long nextLong() {
                    if (!hasNext()) throw new java.util.NoSuchElementException();
                    return values[position++];
                }
            };
        }
    }

    private static void equal(boolean a, boolean b) { if (a != b) fail(a + " != " + b); }
    private static void equal(long a, long b) { if (a != b) fail(a + " != " + b); }
    private static void arrayEqual(long[] a, long[] b) {
        if (!Arrays.equals(a, b)) fail("array mismatch");
    }
    private static void fail(String message) { throw new AssertionError(message); }
}
