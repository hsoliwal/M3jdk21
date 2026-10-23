/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the accompanying NOTICE and LICENSE.
 * The ASF licenses this work under the Apache License, Version 2.0:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, this work is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 */
/*
 * Copyright (C) 2012 The Guava Authors. Licensed under Apache License, Version 2.0.
 * Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Modified: owned dynamic array ring; Guava eviction/suffix policy; first-party
 * stable-mask compaction. No provider implementation or current GPL body copied.
 * Attribution and before-code selection: FOSS_IMPLEMENTATION_DECISION.tsv.
 */
package com.m3.collections.queue;

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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Owned FIFO queue that evicts its oldest element when its logical limit is reached.
 * A zero limit accepts nonnull insertions without retaining them. This class owns
 * its element array and primitive ring positions, and is not thread-safe.
 *
 * <p>The JDK Collection/Iterable defaults operate through this owned cursor. In
 * particular removeIf commits each cursor removal; removeAll/retainAll instead
 * select a primitive removal mask before stable compaction.
 *
 * <p>This is the vendor-neutral M3 owner with its own serialization class identity.
 * The earlier package spelling remains a separate historical compatibility class.
 *
 * @param <E> element type
 */
public final class M3EvictingQueue<E> extends AbstractCollection<E>
        implements Queue<E>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int FULL = 1;
    private static final int ARRAY_LIMIT = Integer.MAX_VALUE - 8;

    private final int maxSize;
    private transient Object[] elements;
    private transient int head;
    private transient int tail;
    private transient int state;

    private M3EvictingQueue(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize (" + maxSize + ") must >= 0");
        }
        this.maxSize = maxSize;
        int slots = maxSize == Integer.MAX_VALUE ? maxSize : maxSize + 1;
        elements = new Object[slots];
        refreshState();
    }

    /** Creates an owned queue with a nonnegative logical maximum. */
    public static <E> M3EvictingQueue<E> create(int maxSize) {
        return new M3EvictingQueue<>(maxSize);
    }

    /** Number of insertions possible without logical eviction. */
    public int remainingCapacity() { return maxSize - size(); }

    private static int next(int position, int slots) {
        return position + 1 == slots ? 0 : position + 1;
    }

    private static int previous(int position, int slots) {
        return position == 0 ? slots - 1 : position - 1;
    }

    private static int distance(int begin, int end, int slots) {
        int difference = end - begin;
        return difference < 0 ? difference + slots : difference;
    }

    private void refreshState() {
        state = (state & ~FULL) | (size() == maxSize ? FULL : 0);
    }

    @Override public int size() {
        return distance(head, tail, elements.length);
    }

    @Override public boolean isEmpty() { return head == tail; }

    @Override public boolean add(E element) {
        Objects.requireNonNull(element);
        if (maxSize == 0) return true;
        refreshState();
        if ((state & FULL) != 0) remove();
        elements[tail] = element;
        tail = next(tail, elements.length);
        try {
            if (tail == head) grow();
            return true;
        } finally {
            refreshState();
        }
    }

    @Override public boolean offer(E element) { return add(element); }

    private int expandedLength(int oldLength) {
        long jump = oldLength < 64 ? (long) oldLength + 2 : oldLength / 2L;
        long target = oldLength + jump;
        if (target <= ARRAY_LIMIT) return (int) target;
        long minimum = (long) oldLength + 1;
        if (minimum > Integer.MAX_VALUE) {
            throw new IllegalStateException("Sorry, deque too big");
        }
        return minimum > ARRAY_LIMIT ? Integer.MAX_VALUE : ARRAY_LIMIT;
    }

    /** Array growth preserves physical ring coordinates and moves only the wrapped first leg. */
    private void grow() {
        Object[] original = elements;
        int oldLength = original.length;
        Object[] expanded = Arrays.copyOf(original, expandedLength(oldLength));
        if (head > tail || (head == tail && expanded[head] != null)) {
            int oldHead = head;
            int additional = expanded.length - oldLength;
            System.arraycopy(expanded, oldHead, expanded, oldHead + additional, oldLength - oldHead);
            Arrays.fill(expanded, oldHead, oldHead + additional, null);
            head = oldHead + additional;
        }
        elements = expanded;
    }

    @SuppressWarnings("unchecked")
    @Override public E peek() { return (E) elements[head]; }

    @Override public E element() {
        E value = peek();
        if (value == null) throw new NoSuchElementException();
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override public E poll() {
        E value = (E) elements[head];
        if (value != null) {
            elements[head] = null;
            head = next(head, elements.length);
            refreshState();
        }
        return value;
    }

    @Override public E remove() {
        E value = poll();
        if (value == null) throw new NoSuchElementException();
        return value;
    }

    @Override public void clear() {
        Object[] ring = elements;
        int position = head;
        int stop = tail;
        while (position != stop) {
            ring[position] = null;
            position = next(position, ring.length);
        }
        head = 0;
        tail = 0;
        refreshState();
    }

    @Override public boolean contains(Object query) {
        if (query == null) return false;
        Object[] ring = elements;
        int position = head;
        int stop = tail;
        while (position != stop) {
            if (query.equals(ring[position])) return true;
            position = next(position, ring.length);
        }
        return false;
    }

    @Override public boolean remove(Object query) {
        if (query == null) return false;
        Object[] ring = elements;
        int position = head;
        int stop = tail;
        while (position != stop) {
            if (query.equals(ring[position])) {
                try { erase(position); }
                finally { refreshState(); }
                return true;
            }
            position = next(position, ring.length);
        }
        return false;
    }

    /** Erases one physical slot, moving the shorter side; true means the suffix moved. */
    private boolean erase(int position) {
        Object[] ring = elements;
        int slots = ring.length;
        int front = distance(head, position, slots);
        int back = distance(position, tail, slots) - 1;
        if (front < back) {
            if (head <= position) {
                System.arraycopy(ring, head, ring, head + 1, front);
            } else {
                int destination = position;
                for (int count = front; count > 0; count--) {
                    int source = previous(destination, slots);
                    ring[destination] = ring[source];
                    destination = source;
                }
            }
            ring[head] = null;
            head = next(head, slots);
            return false;
        }
        int oldTail = tail;
        tail = previous(tail, slots);
        if (position <= tail) {
            System.arraycopy(ring, position + 1, ring, position, back);
        } else {
            int firstLeg = slots - position - 1;
            System.arraycopy(ring, position + 1, ring, position, firstLeg);
            ring[slots - 1] = ring[0];
            System.arraycopy(ring, 1, ring, 0, oldTail - 1);
        }
        ring[tail] = null;
        return true;
    }

    @Override public boolean addAll(Collection<? extends E> collection) {
        int supplied = collection.size();
        Iterator<? extends E> iterator;
        if (supplied >= maxSize) {
            clear();
            int skipped = supplied - maxSize;
            if (collection instanceof List<? extends E> list) {
                int first = Math.min(list.size(), skipped);
                iterator = list.subList(first, list.size()).iterator();
            } else {
                iterator = Objects.requireNonNull(collection.iterator());
                int consumed = 0;
                while (consumed < skipped && iterator.hasNext()) {
                    iterator.next();
                    consumed++;
                }
            }
        } else {
            iterator = collection.iterator();
        }
        Objects.requireNonNull(iterator);
        boolean changed = false;
        while (iterator.hasNext()) changed |= add(iterator.next());
        return changed;
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private int position = head;
            private int remaining = size();
            private int delivered = -1;

            @Override public boolean hasNext() { return remaining > 0; }

            @SuppressWarnings("unchecked")
            @Override public E next() {
                if (remaining <= 0) throw new NoSuchElementException();
                Object value = elements[position];
                if (value == null) throw new ConcurrentModificationException();
                delivered = position;
                position = M3EvictingQueue.next(position, elements.length);
                remaining--;
                return (E) value;
            }

            @Override public void remove() {
                if (delivered < 0) throw new IllegalStateException();
                boolean suffixMoved;
                try { suffixMoved = erase(delivered); }
                finally { refreshState(); }
                if (suffixMoved) position = previous(position, elements.length);
                delivered = -1;
            }

            @SuppressWarnings("unchecked")
            @Override public void forEachRemaining(Consumer<? super E> action) {
                Objects.requireNonNull(action);
                int pending = remaining;
                if (pending <= 0) return;
                remaining = 0;
                Object[] ring = elements;
                int stop = tail;
                if (ring[position] == null || distance(position, stop, ring.length) != pending) {
                    throw new ConcurrentModificationException();
                }
                int cursor = position;
                for (int count = 0; count < pending; count++) {
                    action.accept((E) ring[cursor]);
                    cursor = M3EvictingQueue.next(cursor, ring.length);
                }
                if (tail != stop) throw new ConcurrentModificationException();
                delivered = previous(stop, ring.length);
            }
        };
    }

    @Override public boolean removeAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        return selectAndCompact(value -> collection.contains(value));
    }

    @Override public boolean retainAll(Collection<?> collection) {
        Objects.requireNonNull(collection);
        return selectAndCompact(value -> !collection.contains(value));
    }

    @SuppressWarnings("unchecked")
    private boolean selectAndCompact(Predicate<? super E> rejected) {
        Object[] ring = elements;
        int position = head;
        int stop = tail;
        while (position != stop) {
            if (rejected.test((E) ring[position])) {
                try { return compactFrom(position, rejected); }
                finally { refreshState(); }
            }
            position = next(position, ring.length);
        }
        if (stop != tail) throw new ConcurrentModificationException();
        return false;
    }

    /** First-party stable-mask selection, composed with circular offsets and callback lifecycle. */
    @SuppressWarnings("unchecked")
    private boolean compactFrom(int first, Predicate<? super E> rejected) {
        Object[] ring = elements;
        int slots = ring.length;
        int stop = tail;
        int span = distance(first, stop, slots);
        long[] removed = new long[(int) ((span + 63L) / 64)];
        removed[0] = 1L;
        int read = next(first, slots);
        int logical = 1;
        while (read != stop) {
            if (rejected.test((E) ring[read])) removed[logical >>> 6] |= 1L << (logical & 63);
            read = next(read, slots);
            logical++;
        }
        int write = first;
        read = next(first, slots);
        logical = 1;
        while (read != stop) {
            if ((removed[logical >>> 6] & (1L << (logical & 63))) == 0) {
                ring[write] = ring[read];
                write = next(write, slots);
            }
            read = next(read, slots);
            logical++;
        }
        // Predicate writers are checked after physical expunge, matching selected lifecycle.
        if (stop != tail) throw new ConcurrentModificationException();
        tail = write;
        while (write != stop) {
            ring[write] = null;
            write = next(write, slots);
        }
        return true;
    }

    private void copyRingInto(Object[] output, int count) {
        Object[] ring = elements;
        int first = Math.min(count, ring.length - head);
        System.arraycopy(ring, head, output, 0, first);
        if (count > first) System.arraycopy(ring, 0, output, first, count - first);
    }

    @Override public Object[] toArray() {
        Object[] result = new Object[size()];
        copyRingInto(result, result.length);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T[] toArray(T[] a) {
        int count = size();
        T[] result = a;
        if (count > a.length) {
            result = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
        }
        copyRingInto(result, count);
        if (count < result.length) result[count] = null;
        return result;
    }

    @Override public String toString() {
        Iterator<E> cursor = iterator();
        if (!cursor.hasNext()) return "[]";
        StringBuilder text = new StringBuilder("[");
        while (true) {
            // The provider renders through a distinct internal owner: outer self is not special.
            text.append(cursor.next());
            if (!cursor.hasNext()) return text.append(']').toString();
            text.append(", ");
        }
    }

    @Serial private void writeObject(ObjectOutputStream output) throws IOException {
        int count = size();
        Object[] image = new Object[count + 1];
        copyRingInto(image, count);
        output.defaultWriteObject();
        output.writeInt(count);
        output.writeObject(image);
    }

    @Serial private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        int count = input.readInt();
        if (maxSize < 0 || count < 0 || count > maxSize || count == Integer.MAX_VALUE) {
            throw new InvalidObjectException("invalid queue capacity or element count");
        }
        Object payload = input.readObject();
        if (payload == null || payload.getClass() != Object[].class) {
            throw new InvalidObjectException("queue image must be an Object[]");
        }
        Object[] image = (Object[]) payload;
        if (image.length != count + 1 || image[count] != null) {
            throw new InvalidObjectException("invalid queue image extent or sentinel");
        }
        for (int index = 0; index < count; index++) {
            if (image[index] == null) throw new InvalidObjectException("null queue element");
        }
        // The stream filter admits this exact array extent; separate its external aliases
        // from private storage while preserving identities of the element references.
        elements = image.clone();
        head = 0;
        tail = count;
        refreshState();
    }
}
