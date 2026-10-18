// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Mutable long-value array plus an aligned full-width int state-mask lane.
 *
 * <p>The existing {@link M3LongArrayList} is the sole mutable value owner. This
 * class owns only parallel metadata: insertion/removal shifts the two logical
 * arrays together and replacement never silently moves the object's flags.
 * State bits are application metadata, not object headers, native addresses
 * or GC roots. Positions are not persistent generation-tagged handles.
 *
 * <p>Detached snapshots reuse {@link M3IndexPathRank} for occurrence rank/select
 * and {@link M3LongIndexProjection} for sorted index permutations. Explicit
 * JNI selection reuses {@link M3IndexPathMasks}'s established primitive ABI
 * with the Java rank index as the semantic oracle. No element Entry objects,
 * user callbacks or object references cross JNI. This owner is single-threaded.
 */
public final class M3LongStateArray {
    private static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private final M3LongArrayList values;
    private int[] states;
    private long generation;

    public M3LongStateArray() { this(0); }

    public M3LongStateArray(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("negative capacity");
        values = new M3LongArrayList(initialCapacity);
        states = new int[initialCapacity];
    }

    public int size() { return values.size(); }
    public boolean isEmpty() { return size() == 0; }
    /** Usable aligned capacity; backing lanes may grow independently. */
    public int capacity() { return Math.min(values.capacity(), states.length); }
    public long generation() { return generation; }
    public long get(int index) { return values.get(index); }
    public int stateAt(int index) { return states[Objects.checkIndex(index, size())]; }

    public void ensureCapacity(int minimum) {
        if (minimum < 0) throw new OutOfMemoryError("required array size too large");
        if (minimum > states.length) {
            long grown = states.length == 0 ? Math.max(DEFAULT_CAPACITY, minimum)
                    : (long) states.length + (states.length >> 1);
            int target = grown > MAX_ARRAY_SIZE
                    ? (minimum > MAX_ARRAY_SIZE ? Integer.MAX_VALUE : MAX_ARRAY_SIZE)
                    : (int) Math.max(grown, minimum);
            states = Arrays.copyOf(states, target);
        }
        values.ensureCapacity(minimum);
    }

    public void add(long value, int flags) {
        int at = size();
        ensureCapacity(Math.addExact(at, 1));
        values.add(value);
        states[at] = flags;
        generation++;
    }

    /** JDK-shaped positional insert, keeping flags aligned with moved values. */
    public void insert(int index, long value, int flags) {
        int before = size();
        if (index < 0 || index > before) throw new IndexOutOfBoundsException(index);
        ensureCapacity(Math.addExact(before, 1));
        values.add(index, value);
        System.arraycopy(states, index, states, index + 1, before - index);
        states[index] = flags;
        generation++;
    }

    public long set(int index, long value) {
        long previous = values.set(index, value);
        if (previous != value) generation++;
        return previous;
    }

    public int setState(int index, int flags) {
        int checked = Objects.checkIndex(index, size());
        int previous = states[checked];
        if (previous != flags) {
            states[checked] = flags;
            generation++;
        }
        return previous;
    }

    /** Clear wins when the caller supplies the same bit to set and clear. */
    public int updateState(int index, int setMask, int clearMask) {
        int checked = Objects.checkIndex(index, size());
        int previous = states[checked];
        int updated = (previous | setMask) & ~clearMask;
        if (updated != previous) {
            states[checked] = updated;
            generation++;
        }
        return updated;
    }

    /** Removes one occurrence; physical slot identities are not persistent. */
    public long removeAt(int index) {
        int before = size();
        Objects.checkIndex(index, before);
        long previous = values.removeAt(index);
        System.arraycopy(states, index + 1, states, index, before - index - 1);
        states[before - 1] = 0;
        generation++;
        return previous;
    }

    public void clear() {
        int before = size();
        if (before == 0) return;
        values.clear();
        Arrays.fill(states, 0, before, 0);
        generation++;
    }

    /**
     * Stable in-place LeetCode-style array compaction under pure full-width
     * required/forbidden mask bits. No per-element or result-array allocation.
     * It first counts survivors to preserve the no-op iterator/generation
     * contract when every value passes. Tail deletions are performed only
     * after survivors have been shifted into their final slots.
     *
     * @return number of removed occurrences
     */
    public int retainByStateMask(int requiredMask, int forbiddenMask) {
        int before = size();
        int kept = 0;
        for (int read = 0; read < before; read++) {
            int flags = states[read];
            if ((flags & requiredMask) == requiredMask && (flags & forbiddenMask) == 0) {
                kept++;
            }
        }
        if (kept == before) return 0;
        if (kept == 0) {
            clear();
            return before;
        }
        int write = 0;
        for (int read = 0; read < before; read++) {
            int flags = states[read];
            if ((flags & requiredMask) == requiredMask && (flags & forbiddenMask) == 0) {
                if (write != read) {
                    values.set(write, values.get(read));
                    states[write] = flags;
                }
                write++;
            }
        }
        for (int index = before - 1; index >= write; index--) {
            values.removeAt(index);
            states[index] = 0;
        }
        generation++;
        return before - write;
    }

    public long[] toArray() { return values.toArray(); }
    public int[] statesCopy() { return Arrays.copyOf(states, size()); }

    /** Stable detached capture. Java callbacks and live state are never cached. */
    public Snapshot snapshot() {
        long[] capturedValues = values.toArray();
        return new Snapshot(capturedValues, Arrays.copyOf(states, capturedValues.length), generation);
    }

    public static final class Snapshot {
        private final long[] values;
        private final int[] states;
        private final int[] ordinalIds;
        private final long sourceGeneration;

        private Snapshot(long[] values, int[] states, long sourceGeneration) {
            this.values = values;
            this.states = states;
            this.sourceGeneration = sourceGeneration;
            ordinalIds = new int[values.length];
            for (int i = 0; i < ordinalIds.length; i++) ordinalIds[i] = i;
        }

        public int size() { return values.length; }
        public long sourceGeneration() { return sourceGeneration; }
        public long get(int ordinal) { return values[Objects.checkIndex(ordinal, size())]; }
        public int stateAt(int ordinal) { return states[Objects.checkIndex(ordinal, size())]; }

        /**
         * Prepare exact full-width required/forbidden mask selection over each
         * source occurrence. Calls to rank, select and window reuse one bitmap.
         * Overlapping masks select an empty set without changing the owner.
         */
        public Selection prepare(int requiredMask, int forbiddenMask,
                                 M3IndexPathMasks.Backend backend) {
            Objects.requireNonNull(backend, "backend");
            int[][] planes = {states};
            int[] required = {requiredMask};
            int[] forbidden = {forbiddenMask};
            M3IndexPathRank rank = M3IndexPathRank.snapshot(
                    ordinalIds, ordinalIds, planes, required, forbidden, 0, size(), 1);
            if (backend == M3IndexPathMasks.Backend.NATIVE_REQUIRED) {
                int[] nativeOrdinals = M3IndexPathMasks.select(
                        ordinalIds, ordinalIds, planes, required, forbidden,
                        0, size(), 1, backend);
                if (!Arrays.equals(rank.toArray(), nativeOrdinals)) {
                    throw new ConcurrentModificationException(
                            "native mask result disagrees with Java rank");
                }
            }
            return new Selection(this, rank);
        }

        public Selection prepare(int requiredMask, int forbiddenMask) {
            return prepare(requiredMask, forbiddenMask, M3IndexPathMasks.Backend.JAVA);
        }
    }

    /** Immutable selected occurrence index; no per-row wrapper objects. */
    public static final class Selection {
        private final Snapshot owner;
        private final M3IndexPathRank rank;

        private Selection(Snapshot owner, M3IndexPathRank rank) {
            this.owner = owner;
            this.rank = rank;
        }

        public int size() { return rank.size(); }
        public int sourceSize() { return rank.sourceSize(); }
        public int sourceOrdinal(int selectedRank) { return rank.selectedSourceOrdinal(selectedRank); }
        public long get(int selectedRank) { return owner.get(sourceOrdinal(selectedRank)); }
        public int stateAt(int selectedRank) { return owner.stateAt(sourceOrdinal(selectedRank)); }
        public int rank(int sourceEndExclusive) { return rank.rank(sourceEndExclusive); }
        public boolean containsSourceOrdinal(int ordinal) {
            return rank.containsSourceOrdinal(ordinal);
        }
        public Window window(int fromInclusive, int toExclusive) {
            return new Window(owner, rank.window(fromInclusive, toExclusive));
        }
        public long[] toArray() {
            long[] result = new long[size()];
            for (int i = 0; i < result.length; i++) result[i] = get(i);
            return result;
        }

        /**
         * Explicit immutable index materialization. Reuses the existing M3
         * sorted-projection owner, retaining original source ordinals. Its
         * detached long[] capture and temporary byte[] bitmap are counted
         * as additional allocations, not asserted to be universally zero-copy.
         */
        public M3LongIndexProjection indexed() {
            byte[] accepted = new byte[owner.size()];
            for (int i = 0; i < rank.size(); i++) {
                accepted[rank.selectedSourceOrdinal(i)] = 1;
            }
            return M3LongIndexProjection.snapshot(owner.values)
                    .filtered(accepted, 1, M3IndexMaskCompactor.Backend.JAVA);
        }

        public M3LongIndexProjection sortedAscending() { return indexed().sortedAscending(); }
        public M3LongIndexProjection sortedDescending() { return indexed().sortedDescending(); }
    }

    /** Half-open source-ordinal rank/select window, sharing immutable bitmap. */
    public static final class Window {
        private final Snapshot owner;
        private final M3IndexPathRank.Window rank;

        private Window(Snapshot owner, M3IndexPathRank.Window rank) {
            this.owner = owner;
            this.rank = rank;
        }
        public int size() { return rank.size(); }
        public int sourceSize() { return rank.sourceSize(); }
        public int rank(int relativeEndExclusive) { return rank.rank(relativeEndExclusive); }
        public int sourceOrdinal(int selectedRank) {
            return rank.selectedSourceOrdinal(selectedRank);
        }
        public long get(int selectedRank) { return owner.get(rank.selectedRowId(selectedRank)); }
        public int stateAt(int selectedRank) {
            return owner.stateAt(rank.selectedRowId(selectedRank));
        }
        public Window window(int fromInclusive, int toExclusive) {
            return new Window(owner, rank.window(fromInclusive, toExclusive));
        }
        public long[] toArray() {
            long[] result = new long[size()];
            for (int i = 0; i < result.length; i++) result[i] = get(i);
            return result;
        }
    }
}
