/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 * Modified 2026 by Hitesh Soliwal and contributors: publish grown planes
 * only after every allocation succeeds.
 *
 * Proposed M3JDK21 internal implementation; not copied from donor source.
 */
package jdk.internal.m3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Array-and-mask slot storage. No object is allocated for individual entries.
 *
 * <p>Slots have generation-qualified handles so a removed slot cannot be
 * mistaken for the next occupant. The dense logical-order array is distinct
 * from physical slots; removing a slot shifts integer indexes only.
 *
 * <p>The explicit lease counter is a count of cooperative M3 leases, never a
 * count of arbitrary Java references. Mutation and lease operations are
 * serialized on this instance; callbacks must not mutate the same store.
 */
public final class M3IndexedArray<E> {
    public static final int LIVE = 1;
    public static final int LEASED = 2;
    public static final int SHARED = 4;
    public static final int RETIRED = 8;

    private static final int DEFAULT_CAPACITY = 8;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private Object[] values = new Object[DEFAULT_CAPACITY];
    private int[] states = new int[DEFAULT_CAPACITY];
    private int[] generations = new int[DEFAULT_CAPACITY];
    private int[] leaseCounts = new int[DEFAULT_CAPACITY];
    private int[] freeLinks = new int[DEFAULT_CAPACITY];
    private int[] order = new int[DEFAULT_CAPACITY];
    private int[] positions = new int[DEFAULT_CAPACITY];

    private int allocated;
    private int size;
    private int freeHead = -1;
    private long version;
    // Value and state epochs differ: cooperative leases do not change values.
    private long stateVersion;

    /** Adds a logical element and returns a stable, generation-qualified slot handle. */
    public synchronized long append(E value) {
        if (size == MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError("M3 logical index limit");
        }
        final int slot = allocateSlot();
        values[slot] = value;
        states[slot] = LIVE;
        leaseCounts[slot] = 0;
        positions[slot] = size;
        order[size++] = slot;
        version++;
        stateVersion++;
        return pack(slot, generations[slot]);
    }

    public synchronized int size() {
        return size;
    }

    public synchronized long version() {
        return version;
    }

    /** Returns the slot handle at a logical list index. */
    public synchronized long handleAt(int index) {
        Objects.checkIndex(index, size);
        final int slot = order[index];
        return pack(slot, generations[slot]);
    }

    public synchronized E at(int index) {
        Objects.checkIndex(index, size);
        return valueAtSlot(order[index]);
    }

    public synchronized E get(long handle) {
        return valueAtSlot(validSlot(handle));
    }

    /** Replaces an unleased value without changing its handle. */
    public synchronized boolean replace(long handle, E value) {
        final int slot = validSlot(handle);
        if (leaseCounts[slot] != 0) {
            return false;
        }
        values[slot] = value;
        version++; // Filtering and sorted indexing may now change.
        stateVersion++;
        return true;
    }

    /**
     * Removes an unleased slot. Returns false while an M3 lease is outstanding.
     * Removed handles are invalidated even if this slot is subsequently reused.
     */
    public synchronized boolean remove(long handle) {
        final int slot = validSlot(handle);
        if (leaseCounts[slot] != 0) {
            return false;
        }
        final int position = positions[slot];
        final int moved = size - position - 1;
        if (moved != 0) {
            System.arraycopy(order, position + 1, order, position, moved);
        }
        size--;
        order[size] = 0;
        for (int i = position; i < size; i++) {
            positions[order[i]] = i;
        }
        values[slot] = null;
        positions[slot] = -1;
        leaseCounts[slot] = 0;
        if (generations[slot] == Integer.MAX_VALUE) {
            // Never permit generation wrap to resurrect an old handle.
            states[slot] = RETIRED;
        } else {
            generations[slot]++;
            states[slot] = 0;
            freeLinks[slot] = freeHead;
            freeHead = slot;
        }
        version++;
        stateVersion++;
        return true;
    }

    /** Retains a cooperative lease. It does not count ordinary Java aliases. */
    public synchronized void retain(long handle) {
        final int slot = validSlot(handle);
        if (leaseCounts[slot] == Integer.MAX_VALUE) {
            throw new IllegalStateException("M3 lease counter overflow");
        }
        leaseCounts[slot]++;
        updateLeaseMask(slot);
    }

    /** Releases a previously acquired cooperative lease. */
    public synchronized void release(long handle) {
        final int slot = validSlot(handle);
        if (leaseCounts[slot] == 0) {
            throw new IllegalStateException("unbalanced M3 lease release");
        }
        leaseCounts[slot]--;
        updateLeaseMask(slot);
    }

    public synchronized int ownerCount(long handle) {
        return leaseCounts[validSlot(handle)];
    }

    public synchronized int stateMask(long handle) {
        return states[validSlot(handle)];
    }

    /** Live, read-only, dynamically filtered view, with indexes stored in int[]. */
    public List<E> filtered(Predicate<? super E> predicate) {
        return new M3IndexedView<>(this, Objects.requireNonNull(predicate), null);
    }

    /** Live, read-only stable sorted view; does not change physical slot order. */
    public List<E> sorted(Comparator<? super E> comparator) {
        return new M3IndexedView<>(this, null, Objects.requireNonNull(comparator));
    }

    /** Live, read-only, filtered and stably sorted view. */
    public List<E> filteredSorted(Predicate<? super E> predicate,
                                  Comparator<? super E> comparator) {
        return new M3IndexedView<>(this, Objects.requireNonNull(predicate),
                                   Objects.requireNonNull(comparator));
    }

    /**
     * Live read-only view selected by the physical slot state bits.
     * Required bits must all be present; forbidden bits must all be absent.
     * No payload references or state planes are copied into this view.
     */
    public List<E> maskedStates(int requiredMask, int forbiddenMask) {
        checkMasks(requiredMask, forbiddenMask);
        return new M3IndexedView<>(this, null, null,
                requiredMask, forbiddenMask, true);
    }

    /** Stable state-selected order, using only a primitive slot directory. */
    public List<E> maskedSortedStates(int requiredMask, int forbiddenMask,
                                      Comparator<? super E> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        checkMasks(requiredMask, forbiddenMask);
        return new M3IndexedView<>(this, null, comparator,
                requiredMask, forbiddenMask, true);
    }

    private static void checkMasks(int requiredMask, int forbiddenMask) {
        if ((requiredMask & forbiddenMask) != 0) {
            throw new IllegalArgumentException("overlapping required/forbidden M3 state bits");
        }
    }

    synchronized long stateVersion() {
        return stateVersion;
    }

    synchronized int[] select(Predicate<? super E> predicate,
                              Comparator<? super E> comparator) {
        return select(predicate, comparator, 0, 0, false);
    }

    synchronized int[] select(Predicate<? super E> predicate,
                              Comparator<? super E> comparator,
                              int requiredMask, int forbiddenMask,
                              boolean stateAware) {
        final long initialVersion = stateAware ? stateVersion : version;
        final int[] selected = new int[size];
        int used = 0;
        for (int i = 0; i < size; i++) {
            final int slot = order[i];
            final int state = states[slot];
            if ((!stateAware || ((state & requiredMask) == requiredMask
                                 && (state & forbiddenMask) == 0))
                    && (predicate == null || predicate.test(valueAtSlot(slot)))) {
                selected[used++] = slot;
            }
            rejectReentrantMutation(initialVersion, stateAware);
        }
        final int[] result = used == selected.length
                ? selected : Arrays.copyOf(selected, used);
        if (comparator != null) {
            stableSort(result, comparator, initialVersion, stateAware);
        }
        return result;
    }

    synchronized E viewValue(long expectedVersion, int slot) {
        return viewValue(expectedVersion, slot, false);
    }

    synchronized E viewValue(long expectedVersion, int slot, boolean stateAware) {
        rejectReentrantMutation(expectedVersion, stateAware);
        return valueAtSlot(slot);
    }

    private int allocateSlot() {
        if (freeHead >= 0) {
            final int slot = freeHead;
            freeHead = freeLinks[slot];
            return slot;
        }
        ensureCapacity(allocated + 1);
        final int slot = allocated++;
        generations[slot] = 1;
        return slot;
    }

    private void ensureCapacity(int required) {
        if (required <= values.length) {
            return;
        }
        final int oldLength = values.length;
        final long proposed = Math.max((long) oldLength + (oldLength >> 1), required);
        if (proposed > MAX_ARRAY_SIZE) {
            if (required > MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError("M3 slot index limit");
            }
        }
        final int newLength = (int) Math.min(MAX_ARRAY_SIZE, proposed);
        // A later allocation failure must not publish differently sized planes.
        final Object[] newValues = Arrays.copyOf(values, newLength);
        final int[] newStates = Arrays.copyOf(states, newLength);
        final int[] newGenerations = Arrays.copyOf(generations, newLength);
        final int[] newLeaseCounts = Arrays.copyOf(leaseCounts, newLength);
        final int[] newFreeLinks = Arrays.copyOf(freeLinks, newLength);
        final int[] newPositions = Arrays.copyOf(positions, newLength);
        final int[] newOrder = Arrays.copyOf(order, newLength);

        values = newValues;
        states = newStates;
        generations = newGenerations;
        leaseCounts = newLeaseCounts;
        freeLinks = newFreeLinks;
        positions = newPositions;
        order = newOrder;
    }

    private int validSlot(long handle) {
        final int slot = (int) handle;
        final int generation = (int) (handle >>> 32);
        if (slot < 0 || slot >= allocated || generation == 0
                || (states[slot] & LIVE) == 0 || generations[slot] != generation) {
            throw new IllegalArgumentException("stale or invalid M3 handle");
        }
        return slot;
    }

    private void updateLeaseMask(int slot) {
        final int owners = leaseCounts[slot];
        states[slot] = LIVE | (owners > 0 ? LEASED : 0)
                | (owners > 1 ? SHARED : 0);
        stateVersion++;
    }

    @SuppressWarnings("unchecked")
    private E valueAtSlot(int slot) {
        return (E) values[slot];
    }

    private static long pack(int slot, int generation) {
        return ((long) generation << 32) | Integer.toUnsignedLong(slot);
    }

    private void rejectReentrantMutation(long initialVersion) {
        rejectReentrantMutation(initialVersion, false);
    }

    private void rejectReentrantMutation(long initialVersion, boolean stateAware) {
        if ((stateAware ? stateVersion : version) != initialVersion) {
            throw new ConcurrentModificationException("M3 predicate/comparator mutated the backing store");
        }
    }

    /** Bottom-up stable merge sort of primitive slot indexes, never entries. */
    private void stableSort(int[] indexes, Comparator<? super E> comparator,
                            long initialVersion, boolean stateAware) {
        final int count = indexes.length;
        if (count < 2) {
            return;
        }
        int[] source = indexes;
        int[] target = new int[count];
        for (long width = 1; width < count; width *= 2) {
            for (long start = 0; start < count; start += 2 * width) {
                final int leftEnd = (int) Math.min(start + width, count);
                final int rightEnd = (int) Math.min(start + 2 * width, count);
                int l = (int) start;
                int r = leftEnd;
                int output = (int) start;
                while (l < leftEnd && r < rightEnd) {
                    final int ordering = comparator.compare(valueAtSlot(source[l]),
                                                            valueAtSlot(source[r]));
                    rejectReentrantMutation(initialVersion, stateAware);
                    target[output++] = ordering <= 0 ? source[l++] : source[r++];
                }
                while (l < leftEnd) {
                    target[output++] = source[l++];
                }
                while (r < rightEnd) {
                    target[output++] = source[r++];
                }
            }
            final int[] previousSource = source;
            source = target;
            target = previousSource;
        }
        if (source != indexes) {
            System.arraycopy(source, 0, indexes, 0, count);
        }
    }
}
