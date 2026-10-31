// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: synexia-primitives/src/main/java/com/synexia/primitives/IntArrayDeque.java
// Source-Git-blob: e5a3dcf87e19d38a2fcf07148320919c958d483a
package com.m3.util;

import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

/**
 * Power-of-two primitive int deque retaining one array and no collection-owned per-entry objects.
 * All int values are permitted. This class is not a {@link java.util.Deque}; legacy peek methods
 * throw on empty rather than returning null. Use getFirst/getLast for explicitly named throwing
 * inspection. Queue/stack aliases use the JDK Deque direction: push/pop operate on the head.
 *
 * <p>End operations are O(1), amortized for insertion; growing, clearing and snapshots are O(n).
 * Bulk append and growth copy at most two contiguous ring spans. Capacity is bounded by 2^30
 * lanes; impossible requests are rejected before shifting or allocating. Negative constructor
 * requests retain the existing behavior of reserving two lanes.
 *
 * <p>This mutable class is not thread-safe. Traversal callbacks must not mutate it and there is
 * no fail-fast guarantee. Snapshots are independent; clear retains capacity. payloadBytes is
 * reserved lane bytes only, excluding array/object headers, alignment and temporary copies.
 */
public final class M3IntArrayDeque {
    private int[] elements;
    private int head;
    private int size;
    /**
     * Creates an empty deque with capacity eight.
     */
    public M3IntArrayDeque() { this(8); }
    /**
     * Creates an empty deque with power-of-two capacity. Negative requests retain the
     * legacy minimum-capacity behavior rather than becoming a new exception.
     * @param expected requested capacity; values below two reserve two lanes
     * @throws IllegalArgumentException if expected exceeds 2^30
     * @throws OutOfMemoryError if allocation fails
     */
    public M3IntArrayDeque(int expected) {
        elements = new int[initialCapacity(expected)];
    }
    /**
     * Returns number of retained values.
     * @return number of retained values
     */
    public int size() { return size; }
    /**
     * Returns whether there are no retained values.
     * @return whether there are no retained values
     */
    public boolean isEmpty() { return size == 0; }
    /**
     * Returns power-of-two array capacity.
     * @return power-of-two array capacity
     */
    public int capacity() { return elements.length; }
    /**
     * Returns reserved int payload bytes, excluding headers/alignment and temporary arrays.
     * @return reserved int payload bytes, excluding headers/alignment and temporary arrays
     */
    public long payloadBytes() { return (long) elements.length * Integer.BYTES; }
    /**
     * Inserts at the head.
     * @param value int value
     * @throws IllegalStateException if the capacity limit is reached
     * @throws OutOfMemoryError if growth allocation fails
     */
    public void addFirst(int value) {
        ensure();
        head = (head - 1) & (elements.length - 1);
        elements[head] = value;
        size++;
    }
    /**
     * Inserts at the tail.
     * @param value int value
     * @throws IllegalStateException if the capacity limit is reached
     * @throws OutOfMemoryError if growth allocation fails
     */
    public void addLast(int value) {
        ensure();
        elements[(head + size) & (elements.length - 1)] = value;
        size++;
    }
    /**
     * Legacy throwing inspection, unlike the null-returning JDK peekFirst.
     * @return first value
     * @throws NoSuchElementException if empty
     * @see #getFirst()
     */
    public int peekFirst() { require(); return elements[head]; }
    /**
     * Legacy throwing inspection, unlike the null-returning JDK peekLast.
     * @return last value
     * @throws NoSuchElementException if empty
     * @see #getLast()
     */
    public int peekLast() { require(); return elements[(head + size - 1) & (elements.length - 1)]; }
    /**
     * Removes and zeroes the head lane.
     * @return removed first value
     * @throws NoSuchElementException if empty
     */
    public int removeFirst() {
        require();
        int v = elements[head];
        elements[head] = 0;
        head = (head + 1) & (elements.length - 1);
        size--;
        return v;
    }
    /**
     * Removes and zeroes the tail lane.
     * @return removed last value
     * @throws NoSuchElementException if empty
     */
    public int removeLast() {
        require();
        int i = (head + size - 1) & (elements.length - 1);
        int v = elements[i];
        elements[i] = 0;
        size--;
        return v;
    }
    /**
     * Clears and removes all retained values, preserving capacity.
     */
    public void clear() {
        int first = Math.min(size, elements.length - head);
        java.util.Arrays.fill(elements, head, head + first, 0);
        java.util.Arrays.fill(elements, 0, size - first, 0);
        size = 0;
        head = 0;
    }
    /**
     * Visits values in encounter order without a snapshot. Callback exceptions propagate
     * and stop traversal. The action must not mutate this deque.
     * @param action consumer; a null consumer retains the legacy no-op behavior when empty
     * @throws NullPointerException if action is null and at least one value is visited
     */
    public void forEach(IntConsumer action) {
        for (int i = 0; i < size; i++) action.accept(elements[(head + i) & (elements.length - 1)]);
    }

    /**
     * Inspects the first value without removal.
     * @return first value
     * @throws NoSuchElementException if empty
     */
    public int getFirst() { return peekFirst(); }
    /**
     * Inspects the last value without removal.
     * @return last value
     * @throws NoSuchElementException if empty
     */
    public int getLast() { return peekLast(); }
    /**
     * Inspects the queue head without removal.
     * @return first value
     * @throws NoSuchElementException if empty
     */
    public int element() { return getFirst(); }
    /**
     * Removes the queue head.
     * @return removed first value
     * @throws NoSuchElementException if empty
     */
    public int remove() { return removeFirst(); }
    /**
     * Pushes onto the head, matching JDK Deque stack direction.
     * @param value value to push
     * @throws IllegalStateException if the capacity limit is reached
     * @throws OutOfMemoryError if growth allocation fails
     */
    public void push(int value) { addFirst(value); }
    /**
     * Pops the head, matching JDK Deque stack direction.
     * @return removed first value
     * @throws NoSuchElementException if empty
     */
    public int pop() { return removeFirst(); }
    /**
     * Appends a source slice in encounter order, copying at most two spans.
     * Validation and a required self-alias snapshot precede any mutation.
     * @param source int array, not retained
     * @param from first source index
     * @param length number of values
     * @return length
     * @throws NullPointerException if source is null
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalStateException if the capacity limit would be exceeded
     * @throws OutOfMemoryError if snapshot or growth allocation fails
     */
    public int addAll(int[] source, int from, int length) {
        java.util.Objects.requireNonNull(source, "source");
        java.util.Objects.checkFromIndexSize(from, length, source.length);
        int required = requiredCapacity(size, length);
        if (length == 0) return 0;
        if (source == elements) { source = java.util.Arrays.copyOfRange(source, from, from + length); from = 0; }
        ensure(required);
        int tail = (head + size) & (elements.length - 1);
        int first = Math.min(length, elements.length - tail);
        System.arraycopy(source, from, elements, tail, first);
        if (length > first) System.arraycopy(source, from + first, elements, 0, length - first);
        size = required;
        return length;
    }
    /**
     * Copies values into an independent int array in encounter order.
     * @return a snapshot containing exactly size values
     */
    public int[] toArray() {
        int[] result = new int[size];
        copyInto(result, 0);
        return result;
    }

    private void require() { if (size == 0) throw new NoSuchElementException(); }
    private void ensure() { ensure(requiredCapacity(size, 1)); }
    private void ensure(int required) {
        if (required <= elements.length) return;
        int[] next = new int[initialCapacity(required)];
        copyInto(next, 0);
        elements = next;
        head = 0;
    }
    private void copyInto(int[] target, int offset) {
        int first = Math.min(size, elements.length - head);
        if (first > 0) System.arraycopy(elements, head, target, offset, first);
        if (size > first) System.arraycopy(elements, 0, target, offset + first, size - first);
    }
    static int initialCapacity(int expected) {
        if (expected > (1 << 30)) throw new IllegalArgumentException("capacity exceeds 2^30");
        int needed = Math.max(2, expected);
        return Integer.highestOneBit(needed - 1) << 1;
    }
    static int requiredCapacity(int size, int extra) {
        if (size < 0 || extra < 0 || extra > (1 << 30) - size) {
            throw new IllegalStateException("deque capacity exceeds 2^30");
        }
        return size + extra;
    }
}
