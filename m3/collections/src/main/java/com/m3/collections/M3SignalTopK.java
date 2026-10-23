/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Objects;

/**
 * Caller-compiled, explicit domain-precedence Top-K over immutable M3 primitive columns.
 * Stores only row ordinals and encounter offsets in bounded int[] heaps: no value, Entry,
 * map node or Comparator callback crosses JNI. Equal coordinate tuples preserve original
 * selected occurrence order, including duplicate physical row IDs.
 *
 * <p>Uses a worst-first heap. For k smaller than n this requires O(n log k + k log k)
 * comparisons and O(k) transient primitive slots, without materializing the complete sorted
 * index directory. Source/signals and selection inputs remain immutable to this operation.</p>
 */
public final class M3SignalTopK {
    private final M3SignalColumns signals;
    private final int[] precedence;

    public M3SignalTopK(M3SignalColumns signals, int... precedence) {
        this.signals = Objects.requireNonNull(signals, "signals");
        Objects.requireNonNull(precedence, "precedence");
        if (precedence.length == 0) throw new IllegalArgumentException("no ordered dimensions");
        this.precedence = precedence.clone();
        boolean[] seen = new boolean[signals.columnCount()];
        for (int dimension : this.precedence) {
            Objects.checkIndex(dimension, seen.length);
            if (seen[dimension]) throw new IllegalArgumentException("duplicate domain dimension");
            seen[dimension] = true;
        }
    }

    /** Exactly k best occurrences of the caller's selected physical row IDs, stably ordered. */
    public int[] topK(int[] selected, int k) {
        Objects.requireNonNull(selected, "selected");
        if (k < 0 || k > selected.length) throw new IllegalArgumentException("invalid result limit");
        for (int physicalRow : selected) Objects.checkIndex(physicalRow, signals.rowCount());
        if (k == 0) return new int[0];
        Heap heap = new Heap(k);
        for (int ordinal = 0; ordinal < selected.length; ordinal++) {
            heap.admit(selected[ordinal], ordinal);
        }
        return heap.sorted();
    }

    /**
     * At most k best eligible source rows, in physical encounter order for ties.
     * Unlike selectedByState + full sort, does not allocate an n-length selected row lane.
     */
    public int[] topKByState(int[] states, int requiredMask, int forbiddenMask, int k) {
        Objects.requireNonNull(states, "states");
        if (states.length != signals.rowCount()) {
            throw new IllegalArgumentException("unaligned physical mask lane");
        }
        if (k < 0) throw new IllegalArgumentException("negative result limit");
        if (k == 0) return new int[0];
        Heap heap = new Heap(Math.min(k, states.length));
        int ordinal = 0;
        for (int row = 0; row < states.length; row++) {
            int flags = states[row];
            if ((flags & requiredMask) == requiredMask
                    && (flags & forbiddenMask) == 0) {
                heap.admit(row, ordinal++);
            }
        }
        return heap.sorted();
    }

    private int compare(int a, int b) {
        for (int dimension : precedence) {
            int sign = signals.orderAt(dimension, a, b);
            if (sign != 0) return sign;
        }
        return 0;
    }

    private boolean worse(int rowA, int ordinalA, int rowB, int ordinalB) {
        int sign = compare(rowA, rowB);
        return sign > 0 || (sign == 0 && ordinalA > ordinalB);
    }

    private final class Heap {
        private final int[] rows;
        private final int[] ordinals;
        private int size;

        private Heap(int capacity) {
            rows = new int[capacity];
            ordinals = new int[capacity];
        }

        private void admit(int physicalRow, int encounterOrdinal) {
            if (rows.length == 0) return;
            if (size < rows.length) {
                rows[size] = physicalRow;
                ordinals[size] = encounterOrdinal;
                siftUp(size++);
            } else if (worse(rows[0], ordinals[0], physicalRow, encounterOrdinal)) {
                rows[0] = physicalRow;
                ordinals[0] = encounterOrdinal;
                siftDown(0, size);
            }
        }

        private void swap(int a, int b) {
            int row = rows[a]; rows[a] = rows[b]; rows[b] = row;
            int ordinal = ordinals[a]; ordinals[a] = ordinals[b]; ordinals[b] = ordinal;
        }

        private void siftUp(int at) {
            while (at > 0) {
                int parent = (at - 1) >>> 1;
                if (!worse(rows[at], ordinals[at], rows[parent], ordinals[parent])) return;
                swap(at, parent);
                at = parent;
            }
        }

        private void siftDown(int start, int count) {
            int parent = start;
            while (parent < count / 2) {
                int firstChild = parent * 2 + 1;
                int worstChild = firstChild;
                int secondChild = firstChild + 1;
                if (secondChild < count && worse(rows[secondChild], ordinals[secondChild],
                        rows[firstChild], ordinals[firstChild])) {
                    worstChild = secondChild;
                }
                if (!worse(rows[worstChild], ordinals[worstChild],
                        rows[parent], ordinals[parent])) return;
                swap(parent, worstChild);
                parent = worstChild;
            }
        }

        private int[] sorted() {
            // Worst-first heap extraction sends the worst remaining entry to the end.
            for (int end = size - 1; end > 0; end--) {
                swap(0, end);
                siftDown(0, end);
            }
            return java.util.Arrays.copyOf(rows, size);
        }
    }
}
