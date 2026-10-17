// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.ToDoubleFunction;

/**
 * Stable primitive-coordinate signal ordering based on the original Synexia MatIntOrder.
 * The source is read once, every signal exactly once, with IEEE-754 Double.compare
 * ordering and stable ties. Immutable snapshot, not a backed/live collection.
 */
public final class M3SignalSortedViews {
    private M3SignalSortedViews() { }

    public static <E> List<E> sortedBySignalSnapshot(
            Collection<? extends E> source, ToDoubleFunction<? super E> signal) {
        return sortedBySignalSnapshot(source, signal, true);
    }

    public static <E> List<E> sortedBySignalSnapshot(
            Collection<? extends E> source, ToDoubleFunction<? super E> signal, boolean ascending) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(signal, "signal");
        return new Snapshot<>(source, signal, ascending);
    }

    private static final class Snapshot<E> extends AbstractList<E> implements RandomAccess {
        private final List<E> elements;
        private final int[] rowOrder;

        Snapshot(Collection<? extends E> source, ToDoubleFunction<? super E> signal, boolean ascending) {
            elements = List.copyOf(source);
            int n = elements.size();
            rowOrder = new int[n];
            long[] keys = new long[n];
            for (int i = 0; i < n; i++) {
                long bits = Double.doubleToLongBits(signal.applyAsDouble(elements.get(i)));
                long key = bits ^ ((bits >> 63) | Long.MIN_VALUE);
                keys[i] = ascending ? key : ~key;
                rowOrder[i] = i;
            }
            stableUnsignedLongRadix(keys, rowOrder);
        }
        @Override public E get(int index) { return elements.get(rowOrder[Objects.checkIndex(index, rowOrder.length)]); }
        @Override public int size() { return rowOrder.length; }
    }

    /** Stable unsigned LSD radix; only primitive key and row-index scratch. */
    private static void stableUnsignedLongRadix(long[] keys, int[] rows) {
        int n = keys.length;
        if (n < 2) return;
        long[] scratchKeys = new long[n];
        int[] scratchRows = new int[n];
        int[] counts = new int[256];
        long[] srcKeys = keys, dstKeys = scratchKeys;
        int[] srcRows = rows, dstRows = scratchRows;
        for (int shift = 0; shift < Long.SIZE; shift += 8) {
            java.util.Arrays.fill(counts, 0);
            for (long key : srcKeys) counts[(int) ((key >>> shift) & 255)]++;
            int offset = 0;
            for (int i = 0; i < counts.length; i++) {
                int frequency = counts[i]; counts[i] = offset; offset += frequency;
            }
            for (int i = 0; i < n; i++) {
                int bucket = (int) ((srcKeys[i] >>> shift) & 255);
                int position = counts[bucket]++;
                dstKeys[position] = srcKeys[i];
                dstRows[position] = srcRows[i];
            }
            long[] tmpKeys = srcKeys; srcKeys = dstKeys; dstKeys = tmpKeys;
            int[] tmpRows = srcRows; srcRows = dstRows; dstRows = tmpRows;
        }
        // Eight passes return row indexes to the original array.
    }
}
