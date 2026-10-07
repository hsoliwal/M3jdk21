// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bounded or growable blocking ring deque. Elements and iterator identities are arrays.
 * The lock/conditions may allocate JVM wait records under contention, never element nodes.
 * Iterators snapshot elements and primitive identities; removal cannot delete a reused slot.
 */
public final class M3BlockingDeque<E> extends AbstractQueue<E> implements BlockingDeque<E> {
    private Object[] elements;
    private long[] identities;
    private int head, size;
    private long sequence, revision;
    private final int capacity;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    /** Growable deque, limited by Java array capacity. */
    public M3BlockingDeque() { this(Integer.MAX_VALUE - 8, false); }
    /** Bounded deque with non-fair waiting. */
    public M3BlockingDeque(int capacity) { this(capacity, false); }
    /** Bounded deque with the requested lock fairness. */
    public M3BlockingDeque(int capacity, boolean fair) {
        if (capacity <= 0) { throw new IllegalArgumentException("capacity must be positive"); }
        this.capacity = capacity; elements = new Object[Math.min(capacity, 8)]; identities = new long[elements.length];
        lock = new ReentrantLock(fair); notEmpty = lock.newCondition(); notFull = lock.newCondition();
    }
    private int at(int offset) { int right = elements.length - head; return offset < right ? head + offset : offset - right; }
    private E value(int offset) { return M3SlotArrays.get(elements, at(offset)); }
    private void grow() {
        if (size < elements.length) { return; }
        int n = Math.min(capacity, M3SlotArrays.grow(elements.length)); Object[] e = new Object[n]; long[] ids = new long[n];
        for (int i = 0; i < size; i++) { int s = at(i); e[i] = elements[s]; ids[i] = identities[s]; }
        elements = e; identities = ids; head = 0;
    }
    private void insert(E value, boolean first) {
        if (sequence == Long.MAX_VALUE) { throw new IllegalStateException("iterator identity space exhausted"); }
        grow(); if (first) { head = head == 0 ? elements.length - 1 : head - 1; }
        int s = first ? head : at(size); elements[s] = value; identities[s] = ++sequence;
        size++; revision++; notEmpty.signal();
    }
    private E erase(int offset) {
        E old = value(offset);
        if (offset < size / 2) {
            for (int i = offset; i > 0; i--) { int a = at(i), b = at(i - 1); elements[a] = elements[b]; identities[a] = identities[b]; }
            elements[head] = null; identities[head] = 0; head = at(1);
        } else {
            for (int i = offset; i < size - 1; i++) { int a = at(i), b = at(i + 1); elements[a] = elements[b]; identities[a] = identities[b]; }
            int s = at(size - 1); elements[s] = null; identities[s] = 0;
        }
        size--; revision++; notFull.signal(); return old;
    }
    private boolean offerEnd(E value, boolean first) {
        Objects.requireNonNull(value); lock.lock();
        try { if (size == capacity) { return false; } insert(value, first); return true; } finally { lock.unlock(); }
    }
    private void putEnd(E value, boolean first) throws InterruptedException {
        Objects.requireNonNull(value); lock.lockInterruptibly();
        try { while (size == capacity) { notFull.await(); } insert(value, first); } finally { lock.unlock(); }
    }
    private boolean offerEnd(E value, boolean first, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(value); long nanos = unit.toNanos(timeout); lock.lockInterruptibly();
        try {
            while (size == capacity) { if (nanos <= 0) { return false; } nanos = notFull.awaitNanos(nanos); }
            insert(value, first); return true;
        } finally { lock.unlock(); }
    }
    private E pollEnd(boolean first) {
        lock.lock(); try { return size == 0 ? null : erase(first ? 0 : size - 1); } finally { lock.unlock(); }
    }
    private E takeEnd(boolean first) throws InterruptedException {
        lock.lockInterruptibly();
        try { while (size == 0) { notEmpty.await(); } return erase(first ? 0 : size - 1); } finally { lock.unlock(); }
    }
    private E pollEnd(boolean first, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout); lock.lockInterruptibly();
        try {
            while (size == 0) { if (nanos <= 0) { return null; } nanos = notEmpty.awaitNanos(nanos); }
            return erase(first ? 0 : size - 1);
        } finally { lock.unlock(); }
    }
    private E peekEnd(boolean first) { lock.lock(); try { return size == 0 ? null : value(first ? 0 : size - 1); } finally { lock.unlock(); } }
    @Override public int size() { lock.lock(); try { return size; } finally { lock.unlock(); } }
    @Override public int remainingCapacity() { lock.lock(); try { return capacity - size; } finally { lock.unlock(); } }
    @Override public boolean offerFirst(E value) { return offerEnd(value, true); }
    @Override public boolean offerLast(E value) { return offerEnd(value, false); }
    @Override public void addFirst(E value) { if (!offerFirst(value)) { throw new IllegalStateException("deque full"); } }
    @Override public void addLast(E value) { if (!offerLast(value)) { throw new IllegalStateException("deque full"); } }
    @Override public void putFirst(E value) throws InterruptedException { putEnd(value, true); }
    @Override public void putLast(E value) throws InterruptedException { putEnd(value, false); }
    @Override public boolean offerFirst(E value, long timeout, TimeUnit unit) throws InterruptedException { return offerEnd(value, true, timeout, unit); }
    @Override public boolean offerLast(E value, long timeout, TimeUnit unit) throws InterruptedException { return offerEnd(value, false, timeout, unit); }
    @Override public E pollFirst() { return pollEnd(true); }
    @Override public E pollLast() { return pollEnd(false); }
    @Override public E takeFirst() throws InterruptedException { return takeEnd(true); }
    @Override public E takeLast() throws InterruptedException { return takeEnd(false); }
    @Override public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException { return pollEnd(true, timeout, unit); }
    @Override public E pollLast(long timeout, TimeUnit unit) throws InterruptedException { return pollEnd(false, timeout, unit); }
    @Override public E peekFirst() { return peekEnd(true); }
    @Override public E peekLast() { return peekEnd(false); }
    private E required(E value) { if (value == null) { throw new NoSuchElementException(); } return value; }
    @Override public E removeFirst() { return required(pollFirst()); }
    @Override public E removeLast() { return required(pollLast()); }
    @Override public E getFirst() { return required(peekFirst()); }
    @Override public E getLast() { return required(peekLast()); }
    @Override public boolean offer(E value) { return offerLast(value); }
    @Override public void put(E value) throws InterruptedException { putLast(value); }
    @Override public boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException { return offerLast(value, timeout, unit); }
    @Override public E poll() { return pollFirst(); }
    @Override public E peek() { return peekFirst(); }
    @Override public E take() throws InterruptedException { return takeFirst(); }
    @Override public E poll(long timeout, TimeUnit unit) throws InterruptedException { return pollFirst(timeout, unit); }
    @Override public void push(E value) { addFirst(value); }
    @Override public E pop() { return removeFirst(); }
    @Override public boolean remove(Object value) { return removeFirstOccurrence(value); }
    @Override public boolean removeFirstOccurrence(Object value) { return removeOccurrence(value, false); }
    @Override public boolean removeLastOccurrence(Object value) { return removeOccurrence(value, true); }
    private boolean removeOccurrence(Object value, boolean last) {
        if (value == null) { return false; } lock.lock();
        try {
            for (int i = last ? size - 1 : 0; i >= 0 && i < size; i += last ? -1 : 1) {
                if (value.equals(value(i))) { erase(i); return true; }
            }
            return false;
        } finally { lock.unlock(); }
    }
    @Override public boolean contains(Object value) {
        if (value == null) { return false; } lock.lock();
        try { for (int i = 0; i < size; i++) { if (value.equals(value(i))) { return true; } } return false; }
        finally { lock.unlock(); }
    }
    @Override public void clear() {
        lock.lock(); try { Arrays.fill(elements, null); Arrays.fill(identities, 0); size = 0; head = 0; revision++; notFull.signalAll(); }
        finally { lock.unlock(); }
    }
    @Override public int drainTo(Collection<? super E> target) { return drainTo(target, Integer.MAX_VALUE); }
    @Override public int drainTo(Collection<? super E> target, int maximum) {
        Objects.requireNonNull(target); if (target == this) { throw new IllegalArgumentException("self drain"); }
        lock.lock();
        try {
            int n = Math.min(size, Math.max(0, maximum));
            for (int i = 0; i < n; i++) {
                long expected = revision; target.add(value(0));
                if (revision != expected) { throw new ConcurrentModificationException("drain callback changed source"); } erase(0);
            }
            return n;
        } finally { lock.unlock(); }
    }
    @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.concurrent(iterator(), true, false); }
    @Override public Iterator<E> iterator() { return snapshot(false); }
    @Override public Iterator<E> descendingIterator() { return snapshot(true); }
    private Iterator<E> snapshot(boolean reverse) {
        Object[] payload; long[] ids; lock.lock();
        try {
            payload = new Object[size]; ids = new long[size];
            for (int i = 0; i < size; i++) { int s = at(reverse ? size - 1 - i : i); payload[i] = elements[s]; ids[i] = identities[s]; }
        } finally { lock.unlock(); }
        return new Iterator<>() {
            private int cursor, last = -1;
            @Override public boolean hasNext() { return cursor < payload.length; }
            @Override public E next() { if (!hasNext()) { throw new NoSuchElementException(); } last = cursor++; return M3SlotArrays.get(payload, last); }
            @Override public void remove() {
                if (last < 0) { throw new IllegalStateException(); } lock.lock();
                try { for (int i = 0; i < size; i++) { if (identities[at(i)] == ids[last]) { erase(i); break; } } }
                finally { last = -1; lock.unlock(); }
            }
        };
    }
}
