/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * Primitive multiplicity and state columns attached to existing M3 stable key slots.
 *
 * <p>The key-only {@link M3LinkedHashMap} owns identity, collision resolution, encounter
 * order and recycled slots. Counts and application flags are parallel {@code int[]} columns
 * at those same slots. There are no retained boxed counts, boxed slot handles, or entry
 * objects. A zero count releases the key and clears both columns. Null keys are supported.
 * Positive per-key counts are at most {@link Integer#MAX_VALUE}; totals use {@code long}.
 *
 * <p>This is an explicitly shared, mutable, non-thread-safe storage owner. Provider facades
 * define their own return values, occurrence removal and equality. Distinct-key iterators
 * detect structural key changes on a best-effort basis. Bulk callbacks must not modify the
 * owner. Flag selection returns a detached unmodifiable list, not a live provider view.
 * Optional JNI receives primitive metadata only and reuses {@link M3IndexPathMasks}.
 */
public final class M3ObjectCounts<E> {
    private final M3LinkedHashMap<E, Object> elements = new M3LinkedHashMap<>(false, true);
    private int[] counts = M3SlotArrays.EMPTY_INTS;
    private int[] flags = M3SlotArrays.EMPTY_INTS;
    private int usedSlots;
    private long total;
    private long epoch;
    private long countRevision;

    /** Creates an empty owner; payload columns allocate on first insertion. */
    public M3ObjectCounts() { }

    public int count(Object element) {
        int slot = elements.findSlot(element);
        return slot < 0 ? 0 : counts[slot];
    }

    public int distinctSize() { return elements.size(); }
    public long totalCount() { return total; }

    /** Mutation stamp for shared provider iterators; flag-only changes do not advance it. */
    public long countRevision() { return countRevision; }

    /** Returns the previous count. Invalid counts are rejected before mutation. */
    public int setCount(E element, int count) {
        requireNonnegative(count);
        int slot = elements.findSlot(element);
        int old = slot < 0 ? 0 : counts[slot];
        if (count == old) return old;
        if (slot < 0) insert(element, count);
        else replaceCount(slot, count);
        return old;
    }

    /** Returns the previous count; a per-key overflow is rejected without mutation. */
    public int add(E element, int occurrences) {
        requireNonnegative(occurrences);
        int slot = elements.findSlot(element);
        int old = slot < 0 ? 0 : counts[slot];
        if (occurrences == 0) return old;
        long next = (long) old + occurrences;
        if (next > Integer.MAX_VALUE) throw new IllegalArgumentException("count overflow");
        if (slot < 0) insert(element, (int) next);
        else replaceCount(slot, (int) next);
        return old;
    }

    /** Removes at most the requested count and returns the previous count. */
    public int remove(Object element, int occurrences) {
        requireNonnegative(occurrences);
        int slot = elements.findSlot(element);
        if (slot < 0) return 0;
        int old = counts[slot];
        if (occurrences != 0) replaceCount(slot, Math.max(0, old - occurrences));
        return old;
    }

    /** Returns all 32 application state bits, or zero for an absent element. */
    public int flags(Object element) {
        int slot = elements.findSlot(element);
        return slot < 0 ? 0 : flags[slot];
    }

    /** Sets application state on an existing element, returning its previous flags. */
    public int setFlags(E element, int state) {
        int slot = elements.findSlot(element);
        if (slot < 0) throw new IllegalArgumentException("element is absent");
        int old = flags[slot];
        if (old != state) { flags[slot] = state; epoch++; }
        return old;
    }

    public void clear() {
        if (elements.isEmpty()) return;
        elements.clear();
        Arrays.fill(counts, 0);
        Arrays.fill(flags, 0);
        usedSlots = 0;
        total = 0;
        epoch++;
        countRevision++;
    }

    private static void requireNonnegative(int count) {
        if (count < 0) throw new IllegalArgumentException("negative occurrences");
    }

    private void insert(E element, int count) {
        // Recycled slots already have columns. Reserve only when the key owner must append.
        if (elements.size() == usedSlots) {
            if (usedSlots == Integer.MAX_VALUE) throw new OutOfMemoryError("count slot capacity");
            ensureCapacity(usedSlots + 1);
        }
        elements.put(element, null);
        // Insertion-order owner appends the newly admitted key; do not hash it a third time.
        int slot = elements.lastSlot();
        usedSlots = Math.max(usedSlots, slot + 1);
        counts[slot] = count;
        flags[slot] = 0;
        total += count;
        epoch++;
        countRevision++;
    }

    private void ensureCapacity(int needed) {
        if (needed <= counts.length) return;
        int capacity = M3SlotArrays.grow(counts.length);
        if (capacity < needed) capacity = needed;
        int[] nextCounts = Arrays.copyOf(counts, capacity);
        int[] nextFlags = Arrays.copyOf(flags, capacity);
        counts = nextCounts;
        flags = nextFlags;
    }

    private void replaceCount(int slot, int count) {
        int old = counts[slot];
        if (count == 0) {
            elements.eraseAt(slot);
            flags[slot] = 0;
        }
        counts[slot] = count;
        total += (long) count - old;
        epoch++;
        countRevision++;
    }

    /** Live distinct-element view. Removal, including iterator removal, erases all copies. */
    public Set<E> elementSet() {
        return new AbstractSet<>() {
            @Override public int size() { return elements.size(); }
            @Override public boolean contains(Object value) { return elements.containsKey(value); }
            @Override public void clear() { M3ObjectCounts.this.clear(); }
            @Override public boolean remove(Object value) {
                int slot = elements.findSlot(value);
                if (slot < 0) return false;
                replaceCount(slot, 0);
                return true;
            }
            @Override public Iterator<E> iterator() { return distinctIterator(); }
        };
    }

    private Iterator<E> distinctIterator() {
        return new Iterator<>() {
            private int next = elements.firstSlot();
            private int previous = -1;
            private int expected = elements.revision();
            @Override public boolean hasNext() { elements.check(expected); return next >= 0; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                previous = next;
                next = elements.nextSlot(next);
                return elements.keyAt(previous);
            }
            @Override public void remove() {
                elements.check(expected);
                if (previous < 0) throw new IllegalStateException();
                replaceCount(previous, 0);
                expected = elements.revision();
                previous = -1;
            }
        };
    }

    /** Visits each distinct key and its primitive count once, in insertion order. */
    public void forEachCount(ObjIntConsumer<? super E> action) { forEachCount(action, null); }

    /** A null monitor is the existing no-op monitor; callbacks may not mutate this owner. */
    public void forEachCount(ObjIntConsumer<? super E> action, M3ProgressMonitor monitor) {
        Objects.requireNonNull(action, "action");
        M3ProgressMonitor progress = monitor == null ? M3Progress.NULL : monitor;
        long expected = epoch;
        progress.checkCanceled();
        checkEpoch(expected);
        for (int slot = elements.firstSlot(); slot >= 0; slot = elements.nextSlot(slot)) {
            action.accept(elements.keyAt(slot), counts[slot]);
            checkEpoch(expected);
            progress.worked(1);
            progress.checkCanceled();
            checkEpoch(expected);
        }
    }

    /** Selects all matching distinct elements into an unmodifiable detached snapshot. */
    public List<E> flaggedElements(int requiredMask, int forbiddenMask, M3IndexPathMasks.Backend backend) {
        return flaggedElements(requiredMask, forbiddenMask, 0, distinctSize(), 1, backend, null);
    }

    /**
     * Selects a signed-stride window over distinct encounter positions using the existing
     * Java/C17 mask kernel. All 32 bits are meaningful. Overlapping masks select nothing.
     *
     * <p>Construction is O(distinct keys) primitive metadata plus O(selected keys) result
     * references. Slots and flags remain privately owned. Source changes from a progress
     * callback are detected before a result is published; concurrent access is unsupported.
     */
    public List<E> flaggedElements(int requiredMask, int forbiddenMask, int first, int count,
            int stride, M3IndexPathMasks.Backend backend, M3ProgressMonitor monitor) {
        Objects.requireNonNull(backend, "backend");
        int size = distinctSize();
        if (stride == 0 || count < 0 || first < 0 || first > size)
            throw new IndexOutOfBoundsException("invalid encounter window");
        if (count > 0) {
            long last = (long) first + (long) (count - 1) * stride;
            if (first == size || last < 0 || last >= size)
                throw new IndexOutOfBoundsException("window leaves encounter domain");
        }
        M3ProgressMonitor progress = monitor == null ? M3Progress.NULL : monitor;
        long expected = epoch;
        progress.checkCanceled();
        checkEpoch(expected);
        int[] slots = new int[size];
        int ordinal = 0;
        for (int slot = elements.firstSlot(); slot >= 0; slot = elements.nextSlot(slot)) {
            slots[ordinal++] = slot;
            progress.worked(1);
            progress.checkCanceled();
            checkEpoch(expected);
        }
        int[] selected = M3IndexPathMasks.select(slots, slots, new int[][] {flags},
                new int[] {requiredMask}, new int[] {forbiddenMask}, first, count, stride, backend);
        checkEpoch(expected);
        List<E> result = new M3LinkedList<>();
        for (int slot : selected) result.add(elements.keyAt(slot));
        return Collections.unmodifiableList(result);
    }

    private void checkEpoch(long expected) {
        if (epoch != expected) throw new ConcurrentModificationException("count owner changed during traversal");
    }

    /** Checks key-index membership, primitive column occupancy and the exact long total. */
    public void verifyInvariants() {
        elements.verifyInvariants();
        if (counts.length != flags.length || usedSlots > counts.length)
            throw new AssertionError("count column geometry");
        boolean[] live = new boolean[usedSlots];
        long sum = 0;
        for (int slot = elements.firstSlot(); slot >= 0; slot = elements.nextSlot(slot)) {
            if (slot >= usedSlots || live[slot] || counts[slot] <= 0)
                throw new AssertionError("count slot membership");
            live[slot] = true;
            sum += counts[slot];
        }
        for (int slot = 0; slot < usedSlots; slot++)
            if (!live[slot] && (counts[slot] != 0 || flags[slot] != 0))
                throw new AssertionError("released count slot");
        if (sum != total) throw new AssertionError("count total");
    }
}
