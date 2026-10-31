// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-primitives/src/main/java/com/synexia/primitives/PrimitiveDoubleEndedPriorityQueue.java
// Source-Git-blob: d1507aac2d65b16f84559caa6d3a02c908bbf1ef
package com.m3.util;

import java.util.Objects;

/**
 * Double-ended priority queue retaining one exact-width, logically sorted primitive ring.
 * Duplicates are permitted; ordering and carrier conversion are defined by {@link M3PrimitiveKind}.
 *
 * <p>Both extrema can be examined and removed in constant time. Search/count uses binary
 * search in O(log n); single insertion/removal moves the shorter side in bulk. Sorted export
 * remains O(n). Growth is O(n); no per-entry node or wrapper is retained.
 *
 * <p>Bulk insertion sorts only an unsorted incoming run and merges it with the retained run.
 * For n existing and m incoming values, this takes O(n + m log m), or O(n + m) for already
 * sorted input, with O(m) temporary primitive storage for nonempty merges. Empty imports use
 * the retained array directly. The retained run is never re-sorted.
 * NaN payload order among comparator-equal values is not a stable-order guarantee.
 *
 * <p>This class is mutable, not thread-safe, and not a {@link java.util.Queue}. Its legacy
 * peek/poll methods throw on empty rather than returning a sentinel. Their get/remove aliases
 * make the throwing contract explicit. Returned arrays are independent sorted copies.
 */
public final class M3PrimitiveMinMaxQueue {
    private final M3PrimitiveKind kind;
    private Object values;
    private int size;
    private int head;

    /**
     * Creates an empty queue with capacity eight.
     * @param kind retained primitive kind
     * @throws NullPointerException if kind is null
     */
    public M3PrimitiveMinMaxQueue(M3PrimitiveKind kind) { this(kind, 8); }

    /**
     * Creates an empty queue with at least the requested capacity.
     * @param kind retained primitive kind
     * @param initialCapacity nonnegative reservation; zero reserves one lane
     * @throws NullPointerException if kind is null
     * @throws IllegalArgumentException if initialCapacity is negative
     * @throws OutOfMemoryError if the requested array cannot be allocated
     */
    public M3PrimitiveMinMaxQueue(M3PrimitiveKind kind, int initialCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        values = kind.newArray(Math.max(1, initialCapacity));
    }

    /**
     * Returns the retained primitive kind.
     * @return the retained primitive kind
     */
    public M3PrimitiveKind kind() { return kind; }
    /**
     * Returns the number of retained values.
     * @return the number of retained values
     */
    public int size() { return size; }
    /**
     * Returns whether this queue contains no values.
     * @return whether this queue contains no values
     */
    public boolean isEmpty() { return size == 0; }
    /**
     * Returns the number of physical lanes reserved.
     * @return the number of physical lanes reserved
     */
    public int capacity() { return java.lang.reflect.Array.getLength(values); }
    /**
     * Returns reserved lane bytes, excluding headers, alignment and temporary merge storage.
     * @return {@code kind().payloadBytes(capacity())}
     */
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    /**
     * Inserts one carrier after comparator-equal values. Moves at most the shorter side.
     * @param bits value carrier as defined by kind
     * @throws OutOfMemoryError if size overflows or growth allocation fails
     */
    public void offerBits(long bits) {
        int insertion = upperBound(bits);
        ensure(M3PrimitiveKind.checkedSize(size, 1));
        int capacity = capacity();
        if (insertion < size - insertion) {
            head = head == 0 ? capacity - 1 : head - 1;
            M3PrimitiveKind.moveRing(values, head, capacity, 1, 0, insertion);
        } else {
            M3PrimitiveKind.moveRing(values, head, capacity, insertion, insertion + 1, size - insertion);
        }
        kind.writeBits(values, physical(insertion), bits);
        size++;
    }

    /**
     * Inserts an array slice, sorting only the incoming values when needed.
     * Source values are copied; the source is not modified or retained. Nonzero BOOLEAN bytes
     * become one. Validation and temporary allocation occur before queue mutation.
     * An empty valid array range retains the legacy no-op behavior even for another array kind.
     * @param source matching primitive array (BOOLEAN uses byte[])
     * @param from first source index
     * @param length number of source values
     * @return length
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if source is not an array or a nonempty slice has the wrong kind
     * @throws IndexOutOfBoundsException if the source range is invalid
     * @throws OutOfMemoryError if size overflows or required storage cannot be allocated
     */
    public int offerAllBits(Object source, int from, int length) {
        checkLegacyRange(source, from, length);
        if (length == 0) return 0;
        kind.checkArray(source);
        int required = M3PrimitiveKind.checkedSize(size, length);
        if (length == 1) {
            offerBits(kind.readBits(source, from));
            return 1;
        }
        if (size == 0) {
            ensure(required);
            head = 0;
            kind.copyStorage(source, from, values, 0, length);
            if (!sorted(values, length)) kind.sort(values, 0, length);
            size = length;
            return length;
        }
        Object incoming = kind.newArray(length);
        kind.copyStorage(source, from, incoming, 0, length);
        if (!sorted(incoming, length)) kind.sort(incoming, 0, length);
        merge(incoming, length, required);
        return length;
    }

    /**
     * Inserts a sorted source slice without sorting it or the retained run.
     * The ordering precondition is verified before queue mutation; it is not trusted implicitly.
     * @param source matching primitive array (BOOLEAN uses byte[])
     * @param from first source index
     * @param length number of source values
     * @return length
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if the source kind or ordering is invalid
     * @throws IndexOutOfBoundsException if the source range is invalid
     * @throws OutOfMemoryError if size overflows or required storage cannot be allocated
     */
    public int offerAllSortedBits(Object source, int from, int length) {
        kind.checkRange(source, from, length);
        int required = M3PrimitiveKind.checkedSize(size, length);
        if (length == 0) return 0;
        if (!sortedRange(source, from, length)) throw new IllegalArgumentException("source range is not sorted");
        if (size == 0) {
            ensure(required);
            head = 0;
            kind.copyStorage(source, from, values, 0, length);
            size = length;
            return length;
        }
        if (length == 1) { offerBits(kind.readBits(source, from)); return 1; }
        Object incoming = kind.newArray(length);
        kind.copyStorage(source, from, incoming, 0, length);
        merge(incoming, length, required);
        return length;
    }

    /**
     * Examines the minimum without removal; this legacy method throws on empty.
     * @return minimum carrier
     * @throws java.util.NoSuchElementException if empty
     * @see #getMinBits()
     */
    public long peekMinBits() { requireNotEmpty(); return kind.readBits(values, head); }
    /**
     * Examines the maximum without removal; this legacy method throws on empty.
     * @return maximum carrier
     * @throws java.util.NoSuchElementException if empty
     * @see #getMaxBits()
     */
    public long peekMaxBits() { requireNotEmpty(); return kind.readBits(values, physical(size - 1)); }
    /**
     * Examines the minimum without removal.
     * @return minimum carrier
     * @throws java.util.NoSuchElementException if empty
     */
    public long getMinBits() { return peekMinBits(); }
    /**
     * Examines the maximum without removal.
     * @return maximum carrier
     * @throws java.util.NoSuchElementException if empty
     */
    public long getMaxBits() { return peekMaxBits(); }

    /**
     * Removes the minimum in O(1), clearing one lane without moving other values.
     * This legacy poll method throws on empty; no carrier is reserved as a sentinel.
     * @return removed minimum carrier
     * @throws java.util.NoSuchElementException if empty
     */
    public long pollMinBits() {
        requireNotEmpty();
        long value = kind.readBits(values, head);
        kind.clear(values, head, head + 1);
        head = head == capacity() - 1 ? 0 : head + 1;
        if (--size == 0) head = 0;
        return value;
    }

    /**
     * Removes the maximum in O(1), clearing one lane without moving other values.
     * @return removed maximum carrier
     * @throws java.util.NoSuchElementException if empty
     */
    public long pollMaxBits() {
        requireNotEmpty();
        int tail = physical(size - 1);
        long value = kind.readBits(values, tail);
        kind.clear(values, tail, tail + 1);
        if (--size == 0) head = 0;
        return value;
    }
    /**
     * Removes the minimum; explicitly named throwing alias of pollMinBits.
     * @return removed minimum carrier
     * @throws java.util.NoSuchElementException if empty
     */
    public long removeMinBits() { return pollMinBits(); }
    /**
     * Removes the maximum; explicitly named throwing alias of pollMaxBits.
     * @return removed maximum carrier
     * @throws java.util.NoSuchElementException if empty
     */
    public long removeMaxBits() { return pollMaxBits(); }

    /**
     * Removes one comparator-equal value, moving the shorter retained side.
     * @param bits value to find using kind equality (canonical NaNs, distinct signed zeros)
     * @return whether a value was removed
     */
    public boolean removeOneBits(long bits) {
        int index = lowerBound(bits);
        if (index >= size || !kind.equalBits(kind.readBits(values, physical(index)), bits)) return false;
        int capacity = capacity();
        int suffix = size - index - 1;
        if (index < suffix) {
            M3PrimitiveKind.moveRing(values, head, capacity, 0, 1, index);
            kind.clear(values, head, head + 1);
            head = head == capacity - 1 ? 0 : head + 1;
        } else {
            M3PrimitiveKind.moveRing(values, head, capacity, index + 1, index, suffix);
            int tail = physical(size - 1);
            kind.clear(values, tail, tail + 1);
        }
        if (--size == 0) head = 0;
        return true;
    }

    /**
     * Counts comparator-equal values using two binary searches, without scanning the run.
     * @param bits value carrier
     * @return number of matching values
     */
    public int countBits(long bits) { return upperBound(bits) - lowerBound(bits); }

    /** Removes and clears all retained lanes in O(n), without reducing capacity. */
    public void clear() { kind.clearRing(values, head, size, capacity()); head = 0; size = 0; }

    /**
     * Returns an independent primitive array in ascending order, with no unused capacity.
     * @return sorted snapshot; BOOLEAN uses byte[], other kinds their named primitive array
     */
    public Object toPrimitiveArray() {
        Object result = kind.newArray(size);
        M3PrimitiveKind.copyRing(values, head, size, capacity(), result, 0);
        return result;
    }

    private int physical(int logical) { return M3PrimitiveKind.ringIndex(head, logical, capacity()); }
    private int lowerBound(long bits) { return bound(bits, false); }
    private int upperBound(long bits) { return bound(bits, true); }
    private int bound(long bits, boolean upper) {
        int low = 0;
        int high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int compared = kind.compareBits(kind.readBits(values, physical(mid)), bits);
            if (compared < 0 || (upper && compared == 0)) low = mid + 1;
            else high = mid;
        }
        return low;
    }
    private boolean sorted(Object array, int length) { return sortedRange(array, 0, length); }
    private boolean sortedRange(Object array, int from, int length) {
        for (int i = 1; i < length; i++) {
            if (kind.compareBits(kind.readBits(array, from + i - 1), kind.readBits(array, from + i)) > 0) return false;
        }
        return true;
    }
    private void merge(Object incoming, int length, int required) {
        ensure(required);
        int left = size - 1;
        int right = length - 1;
        int write = required - 1;
        while (left >= 0 && right >= 0) {
            long retained = kind.readBits(values, physical(left));
            long added = kind.readBits(incoming, right);
            if (kind.compareBits(retained, added) > 0) {
                kind.writeBits(values, physical(write--), retained);
                left--;
            } else {
                kind.writeBits(values, physical(write--), added);
                right--;
            }
        }
        while (right >= 0) kind.writeBits(values, physical(write--), kind.readBits(incoming, right--));
        size = required;
    }
    private void ensure(int required) {
        int oldCapacity = capacity();
        if (required <= oldCapacity) return;
        Object next = kind.newArray(M3PrimitiveKind.grownCapacity(oldCapacity, required));
        M3PrimitiveKind.copyRing(values, head, size, oldCapacity, next, 0);
        values = next;
        head = 0;
    }
    private static void checkLegacyRange(Object source, int from, int length) {
        Objects.requireNonNull(source, "source");
        int sourceLength = java.lang.reflect.Array.getLength(source);
        if (from < 0 || length < 0 || from > sourceLength - length) throw new IndexOutOfBoundsException();
    }
    private void requireNotEmpty() { if (size == 0) throw new java.util.NoSuchElementException(); }
}
