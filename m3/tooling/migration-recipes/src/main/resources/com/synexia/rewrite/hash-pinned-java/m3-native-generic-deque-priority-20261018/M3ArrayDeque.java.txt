/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * M3-owned, GC-traced, nonblocking generic ring Deque. A single {@code Object[]}
 * owns values; an aligned {@code byte[]} owns the physical-slot lifecycle flags.
 * All positions are integer offsets, not per-occurrence nodes or vendor adapters.
 * Null elements are prohibited, like {@link java.util.ArrayDeque}.
 *
 * <p>Iterator traversal is live/fail-fast on this owner's structural revision;
 * iterator removal updates the same ring. User callbacks must not mutate the
 * owner while a predicate operation is preparing its decisions. Not concurrent.
 */
public final class M3ArrayDeque<E> extends AbstractCollection<E>
        implements Deque<E>, Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    /** Internal occupied slot flag; application bits occupy the remaining byte. */
    public static final int LIVE = 1;
    public static final int VISIBLE = 2;
    public static final int DIRTY = 4;
    public static final int DEFERRED = 8;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private transient Object[] values;
    private transient byte[] states;
    private transient int head;
    private transient int size;
    private transient long revision;

    public M3ArrayDeque() { values = M3SlotArrays.EMPTY_OBJECTS; states = M3SlotArrays.EMPTY_BYTES; }
    public M3ArrayDeque(int capacity) {
        if (capacity < 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("invalid initial capacity");
        }
        values = capacity == 0 ? M3SlotArrays.EMPTY_OBJECTS : new Object[capacity];
        states = capacity == 0 ? M3SlotArrays.EMPTY_BYTES : new byte[capacity];
    }
    public M3ArrayDeque(Collection<? extends E> values) {
        this(Objects.requireNonNull(values, "values").size());
        addAll(values);
    }

    public int capacity() { return values.length; }
    public long revision() { return revision; }
    @Override public int size() { return size; }
    /** Logical index -> physical slot, using one subtraction instead of modulo. */
    public int physicalIndex(int logical) {
        Objects.checkIndex(logical, size);
        return position(logical);
    }
    private int position(int offset) {
        int remaining = values.length - head;
        return offset < remaining ? head + offset : offset - remaining;
    }
    @SuppressWarnings("unchecked") private E at(int logical) {
        return (E) values[position(logical)];
    }
    private static void userBits(int bits) {
        if ((bits & ~0xfe) != 0) throw new IllegalArgumentException("state flags must use bits 1..7");
    }
    private static void selectionBits(int required, int forbidden) {
        if (((required | forbidden) & ~255) != 0 || (required & forbidden) != 0) {
            throw new IllegalArgumentException("invalid overlapping/oversized selection masks");
        }
    }
    /** Public state belongs to a logical occurrence, not to its object identity. */
    public int stateAt(int logical) { return states[physicalIndex(logical)] & 255; }
    public int updateState(int logical, int setBits, int clearBits) {
        userBits(setBits); userBits(clearBits);
        if ((setBits & clearBits) != 0) throw new IllegalArgumentException("overlapping bits");
        int index = physicalIndex(logical);
        int old = states[index] & 255;
        int next = (old | setBits) & ~clearBits;
        if (next != old) { states[index] = (byte) next; revision++; }
        return old;
    }
    /** Return a detached physical index projection. JNI never receives object values. */
    public int[] selectedPhysicalIndices(int required, int forbidden,
            M3IndexMaskCompactor.Backend backend) {
        selectionBits(required, forbidden);
        Objects.requireNonNull(backend, "backend");
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) {
            int[] order = new int[size];
            byte[] admitted = new byte[size];
            for (int i = 0; i < size; i++) {
                int slot = position(i), bits = states[slot] & 255;
                order[i] = slot;
                admitted[i] = (byte) (((bits & required) == required
                        && (bits & forbidden) == 0) ? 1 : 0);
            }
            return M3IndexMaskCompactor.select(order, admitted, 1, backend);
        }
        int count = 0;
        for (int i = 0; i < size; i++) {
            int flags = states[position(i)] & 255;
            if ((flags & required) == required && (flags & forbidden) == 0) count++;
        }
        int[] result = new int[count];
        for (int i = 0, j = 0; i < size; i++) {
            int slot = position(i), flags = states[slot] & 255;
            if ((flags & required) == required && (flags & forbidden) == 0) result[j++] = slot;
        }
        return result;
    }
    private void grow() {
        if (size < values.length) return;
        if (size >= MAX_CAPACITY) throw new OutOfMemoryError("M3 Deque capacity exhausted");
        int nextCapacity = M3SlotArrays.grow(values.length);
        Object[] nextValues = new Object[nextCapacity];
        byte[] nextStates = new byte[nextCapacity];
        for (int i = 0; i < size; i++) {
            int slot = position(i);
            nextValues[i] = values[slot];
            nextStates[i] = states[slot];
        }
        values = nextValues; states = nextStates; head = 0;
    }
    @Override public void addFirst(E item) {
        Objects.requireNonNull(item, "item");
        grow();
        head = head == 0 ? values.length - 1 : head - 1;
        values[head] = item; states[head] = LIVE;
        size++; revision++;
    }
    @Override public void addLast(E item) {
        Objects.requireNonNull(item, "item");
        grow();
        int slot = position(size);
        values[slot] = item; states[slot] = LIVE;
        size++; revision++;
    }
    @Override public boolean offerFirst(E item) { addFirst(item); return true; }
    @Override public boolean offerLast(E item) { addLast(item); return true; }
    @Override public boolean offer(E item) { return offerLast(item); }
    @Override public boolean add(E item) { return offerLast(item); }
    @Override public E pollFirst() { return size == 0 ? null : erase(0); }
    @Override public E pollLast() { return size == 0 ? null : erase(size - 1); }
    @Override public E removeFirst() { return removeOrThrow(true); }
    @Override public E removeLast() { return removeOrThrow(false); }
    private E removeOrThrow(boolean first) {
        E result = first ? pollFirst() : pollLast();
        if (result == null) throw new NoSuchElementException();
        return result;
    }
    @Override public E poll() { return pollFirst(); }
    @Override public E remove() { return removeFirst(); }
    @Override public E peekFirst() { return size == 0 ? null : at(0); }
    @Override public E peekLast() { return size == 0 ? null : at(size - 1); }
    @Override public E getFirst() { E e = peekFirst(); if (e == null) throw new NoSuchElementException(); return e; }
    @Override public E getLast() { E e = peekLast(); if (e == null) throw new NoSuchElementException(); return e; }
    @Override public E peek() { return peekFirst(); }
    @Override public E element() { return getFirst(); }
    @Override public void push(E e) { addFirst(e); }
    @Override public E pop() { return removeFirst(); }

    /** Shift the shorter physical window, keeping each element's flags aligned. */
    private E erase(int logical) {
        E old = at(logical);
        if (logical < size / 2) {
            for (int i = logical; i > 0; i--) {
                int dest = position(i), source = position(i - 1);
                values[dest] = values[source]; states[dest] = states[source];
            }
            values[head] = null; states[head] = 0;
            head = position(1);
        } else {
            for (int i = logical; i < size - 1; i++) {
                int dest = position(i), source = position(i + 1);
                values[dest] = values[source]; states[dest] = states[source];
            }
            int end = position(size - 1);
            values[end] = null; states[end] = 0;
        }
        size--; if (size == 0) head = 0;
        revision++;
        return old;
    }
    @Override public boolean removeFirstOccurrence(Object wanted) {
        if (wanted == null) return false;
        for (int i = 0; i < size; i++) if (wanted.equals(at(i))) { erase(i); return true; }
        return false;
    }
    @Override public boolean removeLastOccurrence(Object wanted) {
        if (wanted == null) return false;
        for (int i = size - 1; i >= 0; i--) if (wanted.equals(at(i))) { erase(i); return true; }
        return false;
    }
    @Override public boolean remove(Object target) { return removeFirstOccurrence(target); }
    @Override public boolean contains(Object target) {
        if (target == null) return false;
        for (int i = 0; i < size; i++) if (target.equals(at(i))) return true;
        return false;
    }
    @Override public void clear() {
        if (size == 0) return;
        for (int i = 0; i < size; i++) {
            int slot = position(i); values[slot] = null; states[slot] = 0;
        }
        size = 0; head = 0; revision++;
    }
    /** Evaluate callbacks first; if they fail, retain the original circular layout. */
    @Override public boolean removeIf(Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long expected = revision;
        byte[] removed = new byte[size];
        int rejected = 0;
        for (int i = 0; i < removed.length; i++) {
            boolean selected = predicate.test(at(i));
            if (revision != expected) throw new ConcurrentModificationException("reentrant filtered deque mutation");
            if (selected) { removed[i] = 1; rejected++; }
        }
        if (rejected == 0) return false;
        int oldSize = size, write = 0;
        for (int read = 0; read < oldSize; read++) {
            if (removed[read] == 0) {
                int src = position(read), dst = position(write++);
                values[dst] = values[src]; states[dst] = states[src];
            }
        }
        for (int i = write; i < oldSize; i++) {
            int slot = position(i); values[slot] = null; states[slot] = 0;
        }
        size = write; if (size == 0) head = 0;
        revision++;
        return true;
    }

    private final class Cursor implements Iterator<E> {
        private final boolean reverse;
        private int cursor;
        private int last = -1;
        private long expected = revision;
        Cursor(boolean reverse) { this.reverse = reverse; cursor = reverse ? size - 1 : 0; }
        private void live() {
            if (expected != revision) throw new ConcurrentModificationException();
        }
        @Override public boolean hasNext() { live(); return reverse ? cursor >= 0 : cursor < size; }
        @Override public E next() {
            live();
            if (!hasNext()) throw new NoSuchElementException();
            last = cursor;
            E result = at(last);
            cursor += reverse ? -1 : 1;
            return result;
        }
        @Override public void remove() {
            live();
            if (last < 0) throw new IllegalStateException("next() required");
            erase(last);
            if (!reverse && last < cursor) cursor--;
            last = -1; expected = revision;
        }
    }
    @Override public Iterator<E> iterator() { return new Cursor(false); }
    @Override public Iterator<E> descendingIterator() { return new Cursor(true); }
    @Override public Object[] toArray() {
        Object[] copy = new Object[size];
        for (int i = 0; i < size; i++) copy[i] = at(i);
        return copy;
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T[] toArray(T[] target) {
        Objects.requireNonNull(target, "target");
        T[] output = target.length >= size ? target
                : (T[]) java.lang.reflect.Array.newInstance(target.getClass().getComponentType(), size);
        for (int i = 0; i < size; i++) output[i] = (T) at(i);
        if (output.length > size) output[size] = null;
        return output;
    }
    @Override public M3ArrayDeque<E> clone() {
        M3ArrayDeque<E> result = new M3ArrayDeque<>(size);
        for (int i = 0; i < size; i++) {
            int slot = position(i);
            result.values[i] = values[slot]; result.states[i] = states[slot];
        }
        result.size = size;
        return result;
    }
    @Serial private void writeObject(ObjectOutputStream stream) throws IOException {
        long expected = revision;
        stream.defaultWriteObject();
        stream.writeInt(size);
        for (int i = 0; i < size; i++) {
            int slot = position(i);
            stream.writeObject(values[slot]);
            stream.writeByte(states[slot]);
        }
        if (expected != revision) throw new ConcurrentModificationException("mutated during serialization");
    }
    @Serial private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int count = stream.readInt();
        if (count < 0 || count > MAX_CAPACITY) throw new InvalidObjectException("invalid deque size");
        values = count == 0 ? M3SlotArrays.EMPTY_OBJECTS : new Object[count];
        states = count == 0 ? M3SlotArrays.EMPTY_BYTES : new byte[count];
        for (int i = 0; i < count; i++) {
            Object value = stream.readObject();
            byte bits = stream.readByte();
            if (value == null || (bits & LIVE) == 0) throw new InvalidObjectException("invalid deque slot");
            values[i] = value; states[i] = bits;
        }
        size = count; head = 0; revision = 0;
    }
}
