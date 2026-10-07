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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * FIFO transfer or zero-capacity rendezvous queue using primitive slot states and links.
 * A shared lock protects matching, cancellation and acknowledgement. This implementation
 * is blocking, not lock-free. JVM condition waiting may allocate per-waiter machinery.
 */
public final class M3TransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E> {
    private static final byte DATA = 1, TRANSFER = 2, REQUEST = 3, MATCHED = 4;
    private Object[] payload = new Object[8];
    private int[] before = new int[8], after = new int[8];
    private byte[] state = new byte[8];
    private long[] identities = new long[8];
    private int used, free = -1, dataFirst = -1, dataLast = -1, requestFirst = -1, requestLast = -1, dataCount, requestCount;
    private long sequence;
    private final boolean rendezvous;
    private final ReentrantLock lock;
    private final Condition changed;
    /** Unbounded FIFO transfer queue. */
    public M3TransferQueue() { this(false, false); }
    /** Select zero-capacity rendezvous and lock fairness; matching is FIFO in both modes. */
    public M3TransferQueue(boolean rendezvous, boolean fair) {
        this.rendezvous = rendezvous; lock = new ReentrantLock(fair); changed = lock.newCondition();
    }
    private int allocate(byte kind, E value) {
        if (sequence == Long.MAX_VALUE) { throw new IllegalStateException("iterator identity space exhausted"); }
        if (free < 0 && used == payload.length) {
            int n = M3SlotArrays.grow(payload.length); Object[] p = Arrays.copyOf(payload, n);
            int[] b = Arrays.copyOf(before, n), a = Arrays.copyOf(after, n);
            byte[] s = Arrays.copyOf(state, n); long[] ids = Arrays.copyOf(identities, n);
            payload = p; before = b; after = a; state = s; identities = ids;
        }
        int slot;
        if (free < 0) { slot = used++; } else { slot = free; free = after[slot]; }
        state[slot] = kind; payload[slot] = value; identities[slot] = ++sequence;
        boolean request = kind == REQUEST; int tail = request ? requestLast : dataLast;
        before[slot] = tail; after[slot] = -1;
        if (tail >= 0) { after[tail] = slot; }
        if (request) { if (requestFirst < 0) { requestFirst = slot; } requestLast = slot; requestCount++; }
        else { if (dataFirst < 0) { dataFirst = slot; } dataLast = slot; dataCount++; }
        return slot;
    }
    private void unlink(int s, boolean request) {
        int b = before[s], a = after[s];
        if (b >= 0) { after[b] = a; } else if (request) { requestFirst = a; } else { dataFirst = a; }
        if (a >= 0) { before[a] = b; } else if (request) { requestLast = b; } else { dataLast = b; }
        before[s] = -1; after[s] = -1;
        if (request) { requestCount--; } else { dataCount--; }
    }
    private void release(int s) { payload[s] = null; state[s] = 0; identities[s] = 0; before[s] = -1; after[s] = free; free = s; }
    private boolean deliver(E value) {
        if (requestFirst < 0) { return false; }
        int s = requestFirst; unlink(s, true); payload[s] = value; state[s] = MATCHED; changed.signalAll(); return true;
    }
    private E consume(int s) {
        E value = M3SlotArrays.get(payload, s); boolean waiting = state[s] == TRANSFER;
        unlink(s, false); payload[s] = null;
        if (waiting) { state[s] = MATCHED; changed.signalAll(); } else { release(s); }
        return value;
    }
    private void cancel(int s, boolean request) { unlink(s, request); release(s); }
    private boolean awaitMatch(int s, boolean request, boolean timed, long nanos) throws InterruptedException {
        try {
            while (state[s] != MATCHED) {
                if (timed && nanos <= 0) { cancel(s, request); return false; }
                if (timed) { nanos = changed.awaitNanos(nanos); } else { changed.await(); }
            }
            return true;
        } catch (InterruptedException interrupted) {
            if (state[s] == MATCHED) { Thread.currentThread().interrupt(); return true; }
            cancel(s, request); throw interrupted;
        }
    }
    private boolean send(E value, boolean timed, long nanos) throws InterruptedException {
        Objects.requireNonNull(value); lock.lockInterruptibly();
        try {
            if (deliver(value)) { return true; }
            if (timed && nanos <= 0) { return false; }
            int s = allocate(TRANSFER, value);
            if (!awaitMatch(s, false, timed, nanos)) { return false; }
            release(s); return true;
        } finally { lock.unlock(); }
    }
    private E receive(boolean timed, long nanos) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            if (dataFirst >= 0) { return consume(dataFirst); }
            if (timed && nanos <= 0) { return null; }
            int s = allocate(REQUEST, null);
            if (!awaitMatch(s, true, timed, nanos)) { return null; }
            E value = M3SlotArrays.get(payload, s); release(s); return value;
        } finally { lock.unlock(); }
    }
    @Override public boolean offer(E value) {
        Objects.requireNonNull(value); lock.lock();
        try { if (deliver(value)) { return true; } if (rendezvous) { return false; } allocate(DATA, value); return true; }
        finally { lock.unlock(); }
    }
    @Override public void put(E value) throws InterruptedException { if (rendezvous) { send(value, false, 0); } else { offer(value); } }
    @Override public boolean offer(E value, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit); return rendezvous ? send(value, true, unit.toNanos(timeout)) : offer(value);
    }
    @Override public void transfer(E value) throws InterruptedException { send(value, false, 0); }
    @Override public boolean tryTransfer(E value) {
        Objects.requireNonNull(value); lock.lock(); try { return deliver(value); } finally { lock.unlock(); }
    }
    @Override public boolean tryTransfer(E value, long timeout, TimeUnit unit) throws InterruptedException { return send(value, true, unit.toNanos(timeout)); }
    @Override public E poll() { lock.lock(); try { return dataFirst < 0 ? null : consume(dataFirst); } finally { lock.unlock(); } }
    @Override public E take() throws InterruptedException { return receive(false, 0); }
    @Override public E poll(long timeout, TimeUnit unit) throws InterruptedException { return receive(true, unit.toNanos(timeout)); }
    @Override public E peek() { lock.lock(); try { return rendezvous || dataFirst < 0 ? null : M3SlotArrays.get(payload, dataFirst); } finally { lock.unlock(); } }
    @Override public int size() { lock.lock(); try { return rendezvous ? 0 : dataCount; } finally { lock.unlock(); } }
    @Override public int remainingCapacity() { return rendezvous ? 0 : Integer.MAX_VALUE; }
    @Override public boolean hasWaitingConsumer() { lock.lock(); try { return requestCount != 0; } finally { lock.unlock(); } }
    @Override public int getWaitingConsumerCount() { lock.lock(); try { return requestCount; } finally { lock.unlock(); } }
    @Override public boolean contains(Object value) {
        if (value == null || rendezvous) { return false; } lock.lock();
        try { for (int s = dataFirst; s >= 0; s = after[s]) { if (value.equals(payload[s])) { return true; } } return false; }
        finally { lock.unlock(); }
    }
    @Override public boolean remove(Object value) {
        if (value == null || rendezvous) { return false; } lock.lock();
        try { for (int s = dataFirst; s >= 0; s = after[s]) { if (value.equals(payload[s])) { consume(s); return true; } } return false; }
        finally { lock.unlock(); }
    }
    @Override public void clear() {
        if (rendezvous) { return; } lock.lock();
        try { while (dataFirst >= 0) { consume(dataFirst); } } finally { lock.unlock(); }
    }
    @Override public int drainTo(Collection<? super E> target) { return drainTo(target, Integer.MAX_VALUE); }
    @Override public int drainTo(Collection<? super E> target, int maximum) {
        Objects.requireNonNull(target); if (target == this) { throw new IllegalArgumentException("self drain"); }
        lock.lock();
        try {
            int count = 0;
            while (count < maximum && dataFirst >= 0) {
                int s = dataFirst; long id = identities[s]; target.add(M3SlotArrays.get(payload, s));
                if (s != dataFirst || identities[s] != id) { throw new ConcurrentModificationException("drain callback changed source"); }
                consume(s); count++;
            }
            return count;
        } finally { lock.unlock(); }
    }
    @Override public Spliterator<E> spliterator() { return M3SlotSpliterators.concurrent(iterator(), true, false); }
    @Override public Iterator<E> iterator() {
        Object[] values; long[] ids; lock.lock();
        try {
            int n = rendezvous ? 0 : dataCount; values = new Object[n]; ids = new long[n]; int i = 0;
            for (int s = dataFirst; i < n; s = after[s]) { values[i] = payload[s]; ids[i++] = identities[s]; }
        } finally { lock.unlock(); }
        return new Iterator<>() {
            private int cursor, last = -1;
            @Override public boolean hasNext() { return cursor < values.length; }
            @Override public E next() { if (!hasNext()) { throw new NoSuchElementException(); } last = cursor++; return M3SlotArrays.get(values, last); }
            @Override public void remove() {
                if (last < 0) { throw new IllegalStateException(); } lock.lock();
                try { for (int s = dataFirst; s >= 0; s = after[s]) { if (identities[s] == ids[last]) { consume(s); break; } } }
                finally { last = -1; lock.unlock(); }
            }
        };
    }
    /** Validate live, matched and free slot ownership while holding the queue lock. */
    public void verifyInvariants() {
        lock.lock();
        try {
            boolean[] seen = new boolean[used]; int n = checkChain(dataFirst, dataLast, false, seen);
            if (n != dataCount || checkChain(requestFirst, requestLast, true, seen) != requestCount) { throw new AssertionError("queue counts"); }
            for (int s = free; s >= 0; s = after[s]) {
                if (s >= used || seen[s] || state[s] != 0 || payload[s] != null) { throw new AssertionError("queue free"); } seen[s] = true;
            }
            for (int s = 0; s < used; s++) { if (!seen[s] && state[s] != MATCHED) { throw new AssertionError("queue ownership"); } }
            if (dataCount > 0 && requestCount > 0) { throw new AssertionError("unmatched peers"); }
        } finally { lock.unlock(); }
    }
    private int checkChain(int first, int last, boolean request, boolean[] seen) {
        int count = 0, p = -1;
        for (int s = first; s >= 0; s = after[s]) {
            if (s >= used || seen[s] || before[s] != p || (request ? state[s] != REQUEST : state[s] != DATA && state[s] != TRANSFER)) { throw new AssertionError("queue links"); }
            seen[s] = true; count++; p = s;
        }
        if (p != last) { throw new AssertionError("queue tail"); } return count;
    }
}
