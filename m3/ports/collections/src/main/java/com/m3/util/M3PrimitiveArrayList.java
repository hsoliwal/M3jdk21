// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveArrayList.java
// Source-Git-blob: 2b2f7d73fbcdc1d8ea6b65c0520ca526d7306f4d
package com.m3.util;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Resizable exact-width primitive list backed by one primitive array, without per-entry wrappers.
 * Carrier narrowing, floating equality and BOOLEAN byte storage follow {@link M3PrimitiveKind}.
 * This class is not a {@link java.util.List}; it retains primitive rather than boxed values.
 *
 * <p>Indexed access/replacement is O(1); append is amortized O(1); insertion/removal inside
 * the list moves a suffix in O(n). Bulk insertion and range removal use bulk array copies.
 * Predicate removal evaluates once per element before mutation, using a lazy temporary
 * one-bit-per-element decision mask only when a match exists. Callback failure leaves the
 * list unchanged unless the callback itself mutates it, which is prohibited.
 *
 * <p>This mutable class is not thread-safe. Traversal callbacks must not mutate the list.
 * Snapshots are independent; clearing retains capacity. No iterator/fail-fast or serialization
 * contract is implied. payloadBytes reports capacity times lane width, not total memory use.
 */
public final class M3PrimitiveArrayList {
    private final M3PrimitiveKind kind;
    private Object elements;
    private int size;

    /**
     * Creates an empty list with capacity eight.
     * @param kind retained primitive kind
     * @throws NullPointerException if kind is null
     */
    public M3PrimitiveArrayList(M3PrimitiveKind kind) {
        this(kind, 8);
    }

    /**
     * Creates an empty list with a nonnegative reservation.
     * @param kind retained primitive kind
     * @param initialCapacity capacity, with zero reserving one lane
     * @throws NullPointerException if kind is null
     * @throws IllegalArgumentException if capacity is negative
     * @throws OutOfMemoryError if allocation fails
     */
    public M3PrimitiveArrayList(M3PrimitiveKind kind, int initialCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        elements = kind.newArray(Math.max(1, initialCapacity));
    }

    /**
     * Returns the retained primitive kind.
     * @return the retained primitive kind
     */
    public M3PrimitiveKind kind() { return kind; }
    /**
     * Returns number of retained values.
     * @return number of retained values
     */
    public int size() { return size; }
    /**
     * Returns whether there are no values.
     * @return whether there are no values
     */
    public boolean isEmpty() { return size == 0; }
    /**
     * Returns number of reserved primitive lanes.
     * @return number of reserved primitive lanes
     */
    public int capacity() { return java.lang.reflect.Array.getLength(elements); }
    /**
     * Reports capacity times lane width, excluding headers, alignment and temporary masks/copies.
     * @return reserved primitive payload bytes
     */
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    /**
     * Appends a carrier after narrowing to the retained kind.
     * @param bits value carrier
     * @throws OutOfMemoryError if size overflows or allocation fails
     */
    public void addBits(long bits) {
        ensure(M3PrimitiveKind.checkedSize(size, 1));
        kind.writeBits(elements, size++, bits);
    }

    /**
     * Inserts a carrier at a position, shifting the suffix once.
     * @param index insertion position in [0, size]
     * @param bits value carrier
     * @throws IndexOutOfBoundsException if the position is invalid
     * @throws OutOfMemoryError if size overflows or allocation fails
     */
    public void addBits(int index, long bits) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        ensure(M3PrimitiveKind.checkedSize(size, 1));
        System.arraycopy(elements, index, elements, index + 1, size - index);
        kind.writeBits(elements, index, bits);
        size++;
    }

    /**
     * Reads one value without mutation.
     * @param index index in [0, size)
     * @return stored carrier
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public long getBits(int index) {
        return kind.readBits(elements, check(index));
    }

    /**
     * Replaces one value without changing size or capacity.
     * @param index index in [0, size)
     * @param bits replacement carrier
     * @return previous carrier
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public long setBits(int index, long bits) {
        int i = check(index);
        long old = kind.readBits(elements, i);
        kind.writeBits(elements, i, bits);
        return old;
    }

    /**
     * Removes by index, shifts the suffix once and clears the vacated lane.
     * @param index index in [0, size)
     * @return removed carrier
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public long removeAtBits(int index) {
        int i = check(index);
        long old = kind.readBits(elements, i);
        int moved = size - i - 1;
        if (moved > 0) System.arraycopy(elements, i + 1, elements, i, moved);
        size--;
        kind.clear(elements, size, size + 1);
        return old;
    }

    /**
     * Removes the first value equal under kind equality, not an index.
     * @param bits carrier to find
     * @return whether a value was removed
     */
    public boolean removeBits(long bits) {
        int index = indexOfBits(bits);
        if (index < 0) return false;
        removeAtBits(index);
        return true;
    }

    /**
     * Searches from the beginning using kind equality (canonical NaNs, distinct signed zeros).
     * @param bits carrier to find
     * @return first matching index or -1
     */
    public int indexOfBits(long bits) {
        for (int i = 0; i < size; i++) {
            if (kind.equalBits(kind.readBits(elements, i), bits)) return i;
        }
        return -1;
    }

    /**
     * Sorts ascending using the JDK primitive-array algorithms. NaN payload order is not guaranteed.
     */
    public void sort() {
        kind.sort(elements, 0, size);
    }

    /**
     * Searches a list already sorted under kind ordering. Results are undefined if it is unsorted.
     * @param bits carrier to find
     * @return matching index or {@code -(insertionPoint + 1)}, following Arrays.binarySearch
     */
    public int binarySearchBits(long bits) {
        return kind.binarySearch(elements, 0, size, bits);
    }

    /**
     * Clears retained lanes and removes all values, preserving capacity.
     */
    public void clear() {
        kind.clear(elements, 0, size);
        size = 0;
    }

    /**
     * Reserves at least the requested capacity without changing values or size.
     * @param required minimum lane count
     * @throws IllegalArgumentException if required is negative
     * @throws OutOfMemoryError if allocation fails
     */
    public void ensureCapacity(int required) {
        if (required < 0) throw new IllegalArgumentException("required < 0");
        ensure(required);
    }

    /**
     * Shrinks to size, retaining one lane when empty. Existing snapshots remain independent.
     */
    public void trimToSize() {
        int target = Math.max(1, size);
        if (target != capacity()) elements = kind.copyOf(elements, target);
    }

    /**
     * Returns an independent encounter-order array with exactly size lanes.
     * @return matching primitive array; BOOLEAN uses byte[]
     */
    public Object toPrimitiveArray() {
        return kind.copyOf(elements, size);
    }

    /**
     * Visits carriers in order without a snapshot. Exceptions propagate and stop traversal;
     * the action must not mutate the list.
     * @param action consumer, including for an empty list
     * @throws NullPointerException if action is null
     */
    public void forEachBits(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(kind.readBits(elements, i));
    }


    /**
     * Appends a copied source slice. See the indexed overload for validation and alias rules.
     * @param source matching primitive array, BOOLEAN uses byte[]
     * @param from first source index
     * @param length number of values
     * @return length
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if source kind is wrong
     * @throws IndexOutOfBoundsException if the source range is invalid
     * @throws OutOfMemoryError if size overflows or required storage cannot be allocated
     */
    public int addAllBits(Object source, int from, int length) {
        return addAllBits(size, source, from, length);
    }

    /**
     * Inserts a source slice, shifting the suffix once. Self-aliases are snapshotted before
     * moving the suffix or growing. Validation and snapshot allocation precede mutation.
     * BOOLEAN bytes are normalized to zero/one. Source arrays are not retained.
     * @param index insertion position in [0, size]
     * @param source matching primitive array, BOOLEAN uses byte[]
     * @param from first source index
     * @param length number of values
     * @return length
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if source kind is wrong, including empty ranges
     * @throws IndexOutOfBoundsException if index or the source range is invalid
     * @throws OutOfMemoryError if size overflows or required storage cannot be allocated
     */
    public int addAllBits(int index, Object source, int from, int length) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        kind.checkRange(source, from, length);
        int required = M3PrimitiveKind.checkedSize(size, length);
        if (length == 0) return 0;
        if (source == elements) {
            Object snapshot = kind.newArray(length);
            kind.copyStorage(source, from, snapshot, 0, length);
            source = snapshot;
            from = 0;
        }
        ensure(required);
        System.arraycopy(elements, index, elements, index + length, size - index);
        kind.copyStorage(source, from, elements, index, length);
        size = required;
        return length;
    }

    /**
     * Removes the half-open index range, shifting the suffix once and clearing vacated lanes.
     * @param fromIndex inclusive lower bound
     * @param toIndex exclusive upper bound
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void removeRange(int fromIndex, int toIndex) {
        Objects.checkFromToIndex(fromIndex, toIndex, size);
        int removed = toIndex - fromIndex;
        if (removed == 0) return;
        System.arraycopy(elements, toIndex, elements, fromIndex, size - toIndex);
        kind.clear(elements, size - removed, size);
        size -= removed;
    }

    /**
     * Removes matching values, preserving survivor order and stored raw bits.
     * Evaluates each carrier exactly once before mutating retained storage. No-match traversal
     * allocates no decision mask; otherwise temporary storage is ceil(size/64) longs.
     * If the predicate throws, its exception propagates and this method has not changed the list.
     * The predicate must not mutate the list; structural mutation is detected on a best-effort basis.
     * @param filter predicate selecting values to remove
     * @return whether any value was removed
     * @throws NullPointerException if filter is null, even for an empty list
     * @throws java.util.ConcurrentModificationException if detected callback mutation changes size
     * @throws OutOfMemoryError if the temporary decision mask cannot be allocated
     */
    public boolean removeIfBits(java.util.function.LongPredicate filter) {
        Objects.requireNonNull(filter, "filter");
        int originalSize = size;
        long[] removed = null;
        for (int i = 0; i < originalSize; i++) {
            boolean match = filter.test(kind.readBits(elements, i));
            if (size != originalSize) throw new java.util.ConcurrentModificationException();
            if (match) {
                if (removed == null) removed = new long[(int) (((long) originalSize + 63) >>> 6)];
                removed[i >>> 6] |= 1L << (i & 63);
            }
        }
        if (removed == null) return false;
        int write = 0;
        int read = 0;
        while (read < originalSize) {
            while (read < originalSize && (removed[read >>> 6] & (1L << (read & 63))) != 0) read++;
            int start = read;
            while (read < originalSize && (removed[read >>> 6] & (1L << (read & 63))) == 0) read++;
            int length = read - start;
            if (length > 0) {
                if (write != start) System.arraycopy(elements, start, elements, write, length);
                write += length;
            }
        }
        kind.clear(elements, write, originalSize);
        size = write;
        return true;
    }

    Object backingArray() { return elements; }

    private int check(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return index;
    }

    private void ensure(int required) {
        int capacity = capacity();
        if (required <= capacity) return;
        int next = M3PrimitiveKind.grownCapacity(capacity, required);
        elements = kind.copyOf(elements, next);
    }
}
