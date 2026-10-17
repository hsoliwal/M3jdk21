/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.LongPredicate;

/** Independent boxed-value observations for the existing primitive snapshot recipe. */
public final class M3PrimitiveSortedSnapshotOracleSelfTest {
    private static final int[] LENGTHS = {0, 1, 2, 7, 255, 256, 257, 511, 512, 513, 1024};
    private static int checks;

    private M3PrimitiveSortedSnapshotOracleSelfTest() { }

    public static void main(String[] args) {
        boolean nativeRequired = args.length == 1 && args[0].equals("native");
        if (args.length != 0 && !nativeRequired) throw new IllegalArgumentException("expected native or no argument");
        for (int seed = 0; seed < 33; seed++) snapshotOracle(seed);
        exhaustiveMasks(nativeRequired);
        failureAndSizeDriftContracts();
        System.out.println("M3_PRIMITIVE_SORTED_ORACLE_PASS backend="
                + (nativeRequired ? "JAVA_AND_NATIVE_INDEX_PARITY" : "JAVA") + " checks=" + checks);
    }

    private static void snapshotOracle(int seed) {
        int length = LENGTHS[seed % LENGTHS.length];
        Random random = new Random(0x4d33534e41505348L ^ seed);
        long[] input = new long[length];
        for (int i = 0; i < length; i++) input[i] = switch (i % 9) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2, 3 -> 0;
            case 4 -> -1;
            case 5 -> 1;
            default -> random.nextLong();
        };
        long[] inputCopy = input.clone();
        M3LongArrayList backing = M3LongArrayList.copyOf(input);
        M3LongCollection sorted = M3LongSortedViews.sortedSnapshot(backing);
        List<Long> expected = boxedSorted(input, ignored -> true);
        assertSnapshot(expected, sorted);

        int[] callbacks = {0};
        LongPredicate keep = value -> (value & 7L) != 0L;
        M3LongCollection filtered = M3LongSortedViews.filteredSortedSnapshot(backing, value -> {
            callbacks[0]++;
            return keep.test(value);
        });
        check(callbacks[0] == length, "exactly one supplied predicate call per occurrence");
        List<Long> selected = boxedSorted(input, keep);
        assertSnapshot(selected, filtered);
        check(Arrays.equals(input, inputCopy), "input array unchanged by creation");
        check(Arrays.equals(backing.toArray(), inputCopy), "backing collection unchanged by creation");

        M3LongFilteredView live = M3LongSortedViews.filter((M3LongCollection) backing, value -> value == 12345L);
        backing.clear();
        backing.add(12345L);
        if (input.length != 0) input[0] ^= 77L;
        check(live.contains(12345L), "existing live view observes external mutation");
        compare(expected, sorted.toArray());
        compare(selected, filtered.toArray());
        long[] export = sorted.toArray();
        if (export.length != 0) export[0] ^= 1;
        compare(expected, sorted.toArray());
    }

    private static List<Long> boxedSorted(long[] source, LongPredicate keep) {
        List<Long> expected = new ArrayList<>();
        for (long value : source) if (keep.test(value)) expected.add(value);
        Collections.sort(expected);
        return expected;
    }

    private static void assertSnapshot(List<Long> expected, M3LongCollection actual) {
        check(actual.size() == expected.size(), "occurrence count");
        compare(expected, actual.toArray());
        compare(expected, actual.longStream().toArray());
        compare(expected, actual.parallelLongStream().toArray());
        List<Long> traversed = new ArrayList<>();
        actual.forEachLong(traversed::add);
        check(traversed.equals(expected), "primitive forEach order");
        for (long probe : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, 0, -1, 1, 12345L}) {
            check(actual.contains(probe) == expected.contains(probe), "boxed membership parity");
        }
        Spliterator.OfLong cursor = actual.longSpliterator();
        check(cursor.hasCharacteristics(Spliterator.ORDERED | Spliterator.SORTED
                | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL
                | Spliterator.IMMUTABLE), "truthful detached sorted traversal flags");
        check(!cursor.hasCharacteristics(Spliterator.DISTINCT), "duplicates are allowed");
        check(cursor.getComparator() == null, "natural signed-long comparator");
        check(cursor.getExactSizeIfKnown() == expected.size(), "exact snapshot size");
        fails(UnsupportedOperationException.class, actual::clear);
        if (expected.isEmpty()) fails(NoSuchElementException.class, () -> actual.iterator().nextLong());
    }

    private static void exhaustiveMasks(boolean nativeRequired) {
        if (nativeRequired) check(M3IndexMaskCompactor.nativeAvailable(), "required existing JNI bridge loaded");
        for (int length : LENGTHS) {
            long[] input = new long[length];
            byte[] states = new byte[length];
            int[] rows = new int[length];
            for (int i = 0; i < length; i++) {
                input[i] = switch (i % 7) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> -1L;
                    case 3 -> 0L;
                    default -> i % 17 - 8L;
                };
                states[i] = (byte) ((i * 37) & 255);
                rows[i] = i;
            }
            long[] originalInput = input.clone();
            byte[] originalStates = states.clone();
            for (int mask = 0; mask <= 255; mask++) {
                List<Long> expected = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    if (((Byte.toUnsignedInt(states[i]) & mask) == mask)) expected.add(input[i]);
                }
                Collections.sort(expected);
                M3LongCollection result = M3LongSortedViews.maskedSortedSnapshot(input, states, mask);
                compare(expected, result.toArray());
                if (nativeRequired) {
                    int[] selected = M3IndexMaskCompactor.select(rows, states, mask,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                    List<Long> nativeSelected = new ArrayList<>();
                    for (int row : selected) nativeSelected.add(input[row]);
                    Collections.sort(nativeSelected);
                    check(nativeSelected.equals(expected), "native index selection and Java long snapshot agree");
                }
                check(Arrays.equals(input, originalInput), "mask input values unchanged");
                check(Arrays.equals(states, originalStates), "mask input states unchanged");
                if (length != 0) {
                    long oldValue = input[0];
                    byte oldState = states[0];
                    input[0] = ~oldValue;
                    states[0] ^= (byte) 255;
                    compare(expected, result.toArray());
                    input[0] = oldValue;
                    states[0] = oldState;
                }
            }
        }
    }

    private static void failureAndSizeDriftContracts() {
        M3LongArrayList backing = M3LongArrayList.copyOf(new long[] {3, -1, 3, Long.MIN_VALUE});
        long[] before = backing.toArray();
        fails(NullPointerException.class, () -> M3LongSortedViews.sortedSnapshot(null));
        fails(NullPointerException.class, () -> M3LongSortedViews.filteredSortedSnapshot(backing, null));
        fails(NullPointerException.class, () -> M3LongSortedViews.maskedSortedSnapshot(null, new byte[0], 0));
        fails(NullPointerException.class, () -> M3LongSortedViews.maskedSortedSnapshot(new long[0], null, 0));
        fails(IllegalArgumentException.class, () -> M3LongSortedViews.maskedSortedSnapshot(before, new byte[3], 0));
        for (int bad : new int[] {-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            fails(IllegalArgumentException.class, () -> M3LongSortedViews.maskedSortedSnapshot(before, new byte[4], bad));
        }
        fails(IllegalStateException.class, () -> M3LongSortedViews.filteredSortedSnapshot(backing, value -> {
            if (value == -1) throw new IllegalStateException("supplied predicate failure");
            return true;
        }));
        check(Arrays.equals(before, backing.toArray()), "failing predicate does not itself modify the owner");
        fails(ConcurrentModificationException.class,
                () -> M3LongSortedViews.sortedSnapshot(new MisSizedCollection(backing, 3)));
        fails(ConcurrentModificationException.class,
                () -> M3LongSortedViews.sortedSnapshot(new MisSizedCollection(backing, 5)));
        fails(ConcurrentModificationException.class, () -> M3LongSortedViews.filteredSortedSnapshot(backing, value -> {
            if (value == -1) backing.add(42);
            return true;
        }));
        check(backing.contains(42), "caller callback effects remain observable on refusal");
    }

    private static void compare(List<Long> expected, long[] actual) {
        check(expected.size() == actual.length, "boxed oracle size");
        for (int i = 0; i < actual.length; i++) check(expected.get(i).longValue() == actual[i], "boxed oracle order/value");
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void fails(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type.getName() + " got " + error, error);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static final class MisSizedCollection implements M3LongCollection {
        private final M3LongCollection source;
        private final int reportedSize;

        private MisSizedCollection(M3LongCollection source, int reportedSize) {
            this.source = source;
            this.reportedSize = reportedSize;
        }

        @Override public int size() { return reportedSize; }
        @Override public boolean contains(long value) { return source.contains(value); }
        @Override public M3LongIterator iterator() { return source.iterator(); }
        @Override public long[] toArray() { return source.toArray(); }
        @Override public void clear() { throw new UnsupportedOperationException("oracle"); }
    }
}
