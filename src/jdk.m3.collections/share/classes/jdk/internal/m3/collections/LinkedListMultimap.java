/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSequentialList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import jdk.m3.collections.ObjectIntHashMap;

/**
 * Object multimap with live views and per-key and global encounter order.
 *
 * <p>Nulls and duplicate pairs are supported. The existing object-to-int owner
 * indexes key heads; payloads, two linked orders, free slots and state bits live
 * in this owner's arrays. The existing {@link PackedSlotLinkAtoms} owns shared
 * link mechanics. No collection, node or entry object is retained per key or
 * mapping. Removing a mapping clears both payload references and recycles its
 * physical slot.
 *
 * <p>A per-key list supports positional insertion, replacement and removal.
 * Positional insertion precedes the next same-key mapping in global order;
 * appending to a key appends globally. Global values support replacement and
 * removal; global entries support removal and entry-value replacement. The map
 * and key-set views support removal, with first-current-occurrence key order.
 * Iterators detect external structural changes, including same-key changes.
 *
 * <p>Entries are fresh caller-owned projections. A projection follows value
 * updates while its slot generation is live. After removal or clear its key
 * remains available, its value is null, and setValue throws IllegalStateException.
 * It never retargets a recycled slot. This follows the existing M3 slot-map
 * lifetime policy; it does not claim Guava's detached-node lifetime or identity.
 *
 * <p>User key/value callbacks may read the owner. Mutation during indexing or
 * equality callbacks is rejected before changing the owner. An indexing callback
 * failure discards only the rebuildable index; canonical lanes remain intact.
 * This existing internal M3 API is not a Guava interface implementation and is
 * not thread-safe.
 */
public final class LinkedListMultimap<K, V> {
    static final long STATE_OCCUPIED = 1L;
    static final long STATE_DELETED = 2L;
    private static final int NONE = -1;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private final ObjectIntHashMap<Object> headByKey = new ObjectIntHashMap<>(0);
    private Object[] keyLane = new Object[0];
    private Object[] valueLane = new Object[0];
    private int[] globalPrevLane = new int[0];
    private int[] globalNextLane = new int[0];
    private int[] keyPrevLane = new int[0];
    private int[] keyNextLane = new int[0];
    /** At a key head: its tail. At a free slot: the next free slot. */
    private int[] hashIndexLane = new int[0];
    /** Counts live only at key heads; no separate per-key object or payload store. */
    private int[] keyCountLane = new int[0];
    private long[] generationLane = new long[0];
    private long[] stateMaskWords = new long[0];
    private int size;
    private int distinctKeys;
    private int used;
    private int first = NONE;
    private int last = NONE;
    private int free = NONE;
    private int modCount;
    private int callbackDepth;
    private long nextGeneration;
    private boolean indexComplete = true;

    public LinkedListMultimap() { }

    /** Retained compatibility descriptor; it does not describe admission status. */
    public static String scaffoldDescriptor() {
        return "M3LinkedListMultimap|LINKED_LIST_MULTIMAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] globalPrevLane;int[] globalNextLane;int[] keyPrevLane;int[] keyNextLane;int[] hashIndexLane";
    }

    public boolean put(K key, V value) {
        mutable();
        insert(key, value, findHead(key), NONE);
        return true;
    }

    /** A live list, including when the key is currently absent. */
    public List<V> get(K key) { return new KeyList(key); }

    /** Removes a whole key run and returns an immutable detached list. */
    public List<V> removeAll(Object key) {
        mutable();
        int head = findHead(key);
        List<V> old = snapshot(head);
        eraseKey(key, head);
        return old;
    }

    /**
     * Replaces existing values in place, removes surplus mappings and appends
     * additional values globally. Input traversal is staged before changes,
     * including a replacement sourced from this owner's live view.
     */
    public List<V> replaceValues(K key, Iterable<? extends V> values) {
        mutable();
        Objects.requireNonNull(values, "values");
        ArrayList<V> replacement = new ArrayList<>();
        callbackDepth++;
        try {
            for (V value : values) { replacement.add(value); }
        } finally {
            callbackDepth--;
        }
        int head = findHead(key);
        List<V> old = snapshot(head);
        if (replacement.isEmpty()) {
            eraseKey(key, head);
            return old;
        }
        int count = head == NONE ? 0 : keyCountLane[head];
        room(Math.max(0, replacement.size() - count));
        if (head == NONE) {
            head = insert(key, replacement.get(0), NONE, NONE);
            for (int index = 1; index < replacement.size(); index++) {
                insert(key, replacement.get(index), head, NONE);
            }
            return old;
        }
        int row = head;
        int index = 0;
        while (row != NONE && index < replacement.size()) {
            valueLane[row] = replacement.get(index++);
            row = keyNextLane[row];
        }
        while (row != NONE) {
            int next = keyNextLane[row];
            // At least one original mapping remains, so these are non-heads.
            eraseSlot(row, head);
            row = next;
        }
        while (index < replacement.size()) {
            insert(key, replacement.get(index++), head, NONE);
        }
        return old;
    }

    public boolean containsKey(Object key) { return findHead(key) != NONE; }

    public boolean containsEntry(Object key, Object value) {
        for (int row = findHead(key); row != NONE; row = keyNextLane[row]) {
            if (same(value, valueLane[row])) { return true; }
        }
        return false;
    }

    public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return distinctKeys; }
            @Override public boolean contains(Object key) { return containsKey(key); }
            @Override public void clear() { LinkedListMultimap.this.clear(); }
            @Override public boolean remove(Object key) {
                mutable();
                int head = findHead(key);
                if (head == NONE) { return false; }
                eraseKey(key, head);
                return true;
            }
            @Override public Iterator<K> iterator() {
                return new Iterator<>() {
                    private int expected = modCount;
                    private int next = nextHead(first);
                    private int current = NONE;
                    @Override public boolean hasNext() { check(expected); return next != NONE; }
                    @Override public K next() {
                        if (!hasNext()) { throw new NoSuchElementException(); }
                        current = next;
                        next = nextHead(globalNextLane[current]);
                        return keyAt(current);
                    }
                    @Override public void remove() {
                        check(expected);
                        mutable();
                        if (current == NONE) { throw new IllegalStateException(); }
                        eraseKey(keyLane[current], current);
                        expected = modCount;
                        current = NONE;
                    }
                };
            }
        };
    }

    public List<Map.Entry<K, V>> entries() { return new GlobalList<>(true); }
    public List<V> values() { return new GlobalList<>(false); }

    public Map<K, Collection<V>> asMap() {
        return new AbstractMap<>() {
            @Override public int size() { return distinctKeys; }
            @Override public boolean containsKey(Object key) { return LinkedListMultimap.this.containsKey(key); }
            @Override public Collection<V> get(Object key) {
                if (findHead(key) == NONE) { return null; }
                @SuppressWarnings("unchecked") K typed = (K) key;
                return LinkedListMultimap.this.get(typed);
            }
            @Override public Collection<V> remove(Object key) {
                mutable();
                int head = findHead(key);
                if (head == NONE) { return null; }
                List<V> old = snapshot(head);
                eraseKey(key, head);
                return old;
            }
            @Override public void clear() { LinkedListMultimap.this.clear(); }
            @Override public Set<K> keySet() { return LinkedListMultimap.this.keySet(); }
            @Override public Set<Map.Entry<K, Collection<V>>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return distinctKeys; }
                    @Override public void clear() { LinkedListMultimap.this.clear(); }
                    @Override public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                        Iterator<K> keys = LinkedListMultimap.this.keySet().iterator();
                        return new Iterator<>() {
                            @Override public boolean hasNext() { return keys.hasNext(); }
                            @Override public Map.Entry<K, Collection<V>> next() {
                                K key = keys.next();
                                return new SimpleImmutableEntry<>(key, LinkedListMultimap.this.get(key));
                            }
                            @Override public void remove() { keys.remove(); }
                        };
                    }
                    @Override public boolean contains(Object object) {
                        if (!(object instanceof Map.Entry<?, ?> entry)) { return false; }
                        Collection<V> values = get(entry.getKey());
                        return values != null && values.equals(entry.getValue());
                    }
                    @Override public boolean remove(Object object) {
                        if (!contains(object)) { return false; }
                        LinkedListMultimap.this.keySet().remove(((Map.Entry<?, ?>) object).getKey());
                        return true;
                    }
                };
            }
        };
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        mutable();
        if (used == 0) { return; }
        Arrays.fill(keyLane, 0, used, null);
        Arrays.fill(valueLane, 0, used, null);
        Arrays.fill(globalPrevLane, 0, used, 0);
        Arrays.fill(globalNextLane, 0, used, 0);
        Arrays.fill(keyPrevLane, 0, used, 0);
        Arrays.fill(keyNextLane, 0, used, 0);
        Arrays.fill(hashIndexLane, 0, used, 0);
        Arrays.fill(keyCountLane, 0, used, 0);
        Arrays.fill(stateMaskWords, 0L);
        headByKey.clear();
        indexComplete = true;
        size = distinctKeys = used = 0;
        first = last = free = NONE;
        modCount++;
    }

    private final class KeyList extends AbstractSequentialList<V> {
        private final K key;
        KeyList(K key) { this.key = key; }
        @Override public int size() {
            int head = findHead(key);
            return head == NONE ? 0 : keyCountLane[head];
        }
        @Override public void clear() {
            mutable();
            eraseKey(key, findHead(key));
        }
        @Override public ListIterator<V> listIterator(int index) { return new KeyCursor(key, index); }
    }

    private final class KeyCursor implements ListIterator<V> {
        private final K key;
        private int expected = modCount;
        private int next;
        private int previous;
        private int current = NONE;
        private int index;
        private boolean forward;
        KeyCursor(K key, int index) {
            this.key = key;
            int head = findHead(key);
            int count = head == NONE ? 0 : keyCountLane[head];
            Objects.checkIndex(index, count + 1);
            this.index = index;
            if (index <= count / 2) {
                next = head;
                previous = NONE;
                for (int n = 0; n < index; n++) { previous = next; next = keyNextLane[next]; }
            } else {
                previous = hashIndexLane[head];
                next = NONE;
                for (int n = count; n > index; n--) { next = previous; previous = keyPrevLane[previous]; }
            }
        }
        @Override public boolean hasNext() { check(expected); return next != NONE; }
        @Override public boolean hasPrevious() { check(expected); return previous != NONE; }
        @Override public int nextIndex() { check(expected); return index; }
        @Override public int previousIndex() { check(expected); return index - 1; }
        @Override public V next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            current = next;
            previous = current;
            next = keyNextLane[current];
            index++;
            forward = true;
            return valueAt(current);
        }
        @Override public V previous() {
            if (!hasPrevious()) { throw new NoSuchElementException(); }
            current = previous;
            next = current;
            previous = keyPrevLane[current];
            index--;
            forward = false;
            return valueAt(current);
        }
        @Override public void remove() {
            check(expected);
            mutable();
            if (current == NONE) { throw new IllegalStateException(); }
            int before = keyPrevLane[current];
            int after = keyNextLane[current];
            eraseSlot(current, findHead(key));
            if (forward) { previous = before; index--; } else { next = after; }
            current = NONE;
            expected = modCount;
        }
        @Override public void set(V value) {
            check(expected);
            if (current == NONE) { throw new IllegalStateException(); }
            writeValue(current, value);
        }
        @Override public void add(V value) {
            check(expected);
            mutable();
            previous = insert(key, value, findHead(key), next);
            index++;
            current = NONE;
            expected = modCount;
        }
    }

    private final class GlobalList<T> extends AbstractSequentialList<T> {
        private final boolean entries;
        GlobalList(boolean entries) { this.entries = entries; }
        @Override public int size() { return size; }
        @Override public void clear() { LinkedListMultimap.this.clear(); }
        @Override public ListIterator<T> listIterator(int index) { return new GlobalCursor<>(entries, index); }
    }

    private final class GlobalCursor<T> implements ListIterator<T> {
        private final boolean entries;
        private int expected = modCount;
        private int next;
        private int previous;
        private int current = NONE;
        private int index;
        private boolean forward;
        GlobalCursor(boolean entries, int index) {
            Objects.checkIndex(index, size + 1);
            this.entries = entries;
            this.index = index;
            if (index <= size / 2) {
                next = first;
                previous = NONE;
                for (int n = 0; n < index; n++) { previous = next; next = globalNextLane[next]; }
            } else {
                previous = last;
                next = NONE;
                for (int n = size; n > index; n--) { next = previous; previous = globalPrevLane[previous]; }
            }
        }
        @Override public boolean hasNext() { check(expected); return next != NONE; }
        @Override public boolean hasPrevious() { check(expected); return previous != NONE; }
        @Override public int nextIndex() { check(expected); return index; }
        @Override public int previousIndex() { check(expected); return index - 1; }
        @SuppressWarnings("unchecked") private T project(int row) {
            return (T) (entries ? new EntryView(row) : valueLane[row]);
        }
        @Override public T next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            current = next;
            previous = current;
            next = globalNextLane[current];
            index++;
            forward = true;
            return project(current);
        }
        @Override public T previous() {
            if (!hasPrevious()) { throw new NoSuchElementException(); }
            current = previous;
            next = current;
            previous = globalPrevLane[current];
            index--;
            forward = false;
            return project(current);
        }
        @Override public void remove() {
            check(expected);
            mutable();
            if (current == NONE) { throw new IllegalStateException(); }
            int before = globalPrevLane[current];
            int after = globalNextLane[current];
            eraseSlot(current, findHead(keyLane[current]));
            if (forward) { previous = before; index--; } else { next = after; }
            current = NONE;
            expected = modCount;
        }
        @Override public void set(T value) {
            if (entries) { throw new UnsupportedOperationException("entry replacement"); }
            check(expected);
            if (current == NONE) { throw new IllegalStateException(); }
            writeValue(current, value);
        }
        @Override public void add(T value) { throw new UnsupportedOperationException("global insertion"); }
    }

    private final class EntryView implements Map.Entry<K, V> {
        private final K key;
        private final int row;
        private final long generation;
        EntryView(int row) {
            this.key = keyAt(row);
            this.row = row;
            generation = generationLane[row];
        }
        private boolean present() {
            return generationLane[row] == generation && state(row) == STATE_OCCUPIED;
        }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return present() ? valueAt(row) : null; }
        @Override public V setValue(V value) {
            mutable();
            if (!present()) { throw new IllegalStateException("entry no longer present"); }
            return writeValue(row, value);
        }
        @Override public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> entry
                    && same(key, entry.getKey()) && same(getValue(), entry.getValue());
        }
        @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }
        @Override public String toString() { return key + "=" + getValue(); }
    }

    private int insert(K key, V value, int head, int before) {
        room(1);
        int row = free != NONE ? free : used;
        int tail = head == NONE ? NONE : hashIndexLane[head];
        int count = head == NONE ? 0 : keyCountLane[head];
        // Fallible user hashing and growth finish before topology publication.
        if (head == NONE || before == head) { indexPut(key, row); }
        if (free != NONE) { free = hashIndexLane[row]; } else { used++; }
        keyLane[row] = key;
        valueLane[row] = value;
        generationLane[row] = ++nextGeneration;
        hashIndexLane[row] = NONE;
        keyCountLane[row] = 0;
        if (before == NONE) {
            long endpoints = PackedSlotLinkAtoms.linkLast(globalPrevLane, globalNextLane, first, last, row);
            first = PackedSlotLinkAtoms.first(endpoints);
            last = PackedSlotLinkAtoms.last(endpoints);
            if (head == NONE) {
                keyPrevLane[row] = keyNextLane[row] = NONE;
                head = row;
                distinctKeys++;
            } else {
                PackedSlotLinkAtoms.linkLast(keyPrevLane, keyNextLane, head, tail, row);
            }
            hashIndexLane[head] = row;
        } else {
            int globalBefore = globalPrevLane[before];
            globalPrevLane[row] = globalBefore;
            globalNextLane[row] = before;
            if (globalBefore == NONE) { first = row; } else { globalNextLane[globalBefore] = row; }
            globalPrevLane[before] = row;
            int keyBefore = keyPrevLane[before];
            keyPrevLane[row] = keyBefore;
            keyNextLane[row] = before;
            if (keyBefore == NONE) {
                keyCountLane[head] = 0;
                hashIndexLane[head] = NONE;
                head = row;
                hashIndexLane[head] = tail;
            } else {
                keyNextLane[keyBefore] = row;
            }
            keyPrevLane[before] = row;
        }
        keyCountLane[head] = count + 1;
        setState(row, STATE_OCCUPIED);
        size++;
        modCount++;
        return row;
    }

    private void eraseSlot(int row, int head) {
        int count = keyCountLane[head];
        int tail = hashIndexLane[head];
        int next = keyNextLane[row];
        if (row == head) {
            if (next == NONE) { indexRemove(keyLane[row]); } else { indexPut(keyLane[row], next); }
        }
        long global = PackedSlotLinkAtoms.unlink(globalPrevLane, globalNextLane, first, last, row);
        first = PackedSlotLinkAtoms.first(global);
        last = PackedSlotLinkAtoms.last(global);
        long keyLinks = PackedSlotLinkAtoms.unlink(keyPrevLane, keyNextLane, head, tail, row);
        int newHead = PackedSlotLinkAtoms.first(keyLinks);
        if (newHead == NONE) {
            distinctKeys--;
        } else {
            hashIndexLane[newHead] = PackedSlotLinkAtoms.last(keyLinks);
            keyCountLane[newHead] = count - 1;
        }
        release(row);
        size--;
        modCount++;
    }

    private void eraseKey(Object key, int head) {
        if (head == NONE) { return; }
        int count = keyCountLane[head];
        indexRemove(key);
        for (int row = head; row != NONE; ) {
            int next = keyNextLane[row];
            long endpoints = PackedSlotLinkAtoms.unlink(globalPrevLane, globalNextLane, first, last, row);
            first = PackedSlotLinkAtoms.first(endpoints);
            last = PackedSlotLinkAtoms.last(endpoints);
            release(row);
            row = next;
        }
        size -= count;
        distinctKeys--;
        modCount++;
    }

    private void release(int row) {
        keyLane[row] = valueLane[row] = null;
        globalPrevLane[row] = globalNextLane[row] = NONE;
        keyPrevLane[row] = keyNextLane[row] = NONE;
        keyCountLane[row] = 0;
        hashIndexLane[row] = free;
        free = row;
        setState(row, STATE_DELETED);
    }

    private List<V> snapshot(int head) {
        ArrayList<V> copy = new ArrayList<>(head == NONE ? 0 : keyCountLane[head]);
        for (int row = head; row != NONE; row = keyNextLane[row]) { copy.add(valueAt(row)); }
        return Collections.unmodifiableList(copy);
    }

    private int findHead(Object key) {
        // Re-entrant reads see canonical lanes, never an index being shifted.
        if (callbackDepth == 0) {
            callbackDepth++;
            int indexed;
            try { indexed = headByKey.get(key); } finally { callbackDepth--; }
            if (indexed != 0) { return indexed - 1; }
            if (indexComplete) { return NONE; }
        }
        for (int row = first; row != NONE; row = globalNextLane[row]) {
            if (keyPrevLane[row] == NONE && same(key, keyLane[row])) { return row; }
        }
        return NONE;
    }

    private void indexPut(Object key, int head) {
        callbackDepth++;
        try {
            headByKey.put(key, head + 1);
        } catch (RuntimeException | Error failure) {
            headByKey.clear();
            indexComplete = false;
            throw failure;
        } finally {
            callbackDepth--;
        }
    }

    private void indexRemove(Object key) {
        callbackDepth++;
        try {
            headByKey.remove(key);
        } catch (RuntimeException | Error failure) {
            // Existing hash owner may have started backward shifting before a
            // displaced key's hash callback failed. Clear only that index.
            headByKey.clear();
            indexComplete = false;
            throw failure;
        } finally {
            callbackDepth--;
        }
    }

    private boolean same(Object left, Object right) {
        callbackDepth++;
        try { return Objects.equals(left, right); } finally { callbackDepth--; }
    }

    private int nextHead(int row) {
        while (row != NONE && keyPrevLane[row] != NONE) { row = globalNextLane[row]; }
        return row;
    }

    private void mutable() {
        if (callbackDepth != 0) { throw new ConcurrentModificationException("mutation during callback"); }
    }
    private void check(int expected) {
        if (expected != modCount) { throw new ConcurrentModificationException(); }
    }
    @SuppressWarnings("unchecked") private K keyAt(int row) { return (K) keyLane[row]; }
    @SuppressWarnings("unchecked") private V valueAt(int row) { return (V) valueLane[row]; }
    private V writeValue(int row, Object value) {
        mutable();
        V old = valueAt(row);
        valueLane[row] = value;
        return old;
    }
    private long state(int row) { return stateMaskWords[row >>> 5] >>> ((row & 31) << 1) & 3L; }
    private void setState(int row, long state) {
        int shift = (row & 31) << 1;
        stateMaskWords[row >>> 5] = stateMaskWords[row >>> 5] & ~(3L << shift) | state << shift;
    }

    /** Stage all capacity changes; an allocation failure cannot split lanes. */
    private void room(int additional) {
        if (additional < 0 || (long) size + additional > MAX_CAPACITY) {
            throw new OutOfMemoryError("LinkedListMultimap capacity exhausted");
        }
        if (nextGeneration > Long.MAX_VALUE - additional) {
            throw new IllegalStateException("slot generation exhausted");
        }
        int required = size + additional;
        if (required <= keyLane.length) { return; }
        int capacity = (int) Math.min(MAX_CAPACITY, Math.max(required,
                Math.max(8L, (long) keyLane.length + (keyLane.length >>> 1) + 1)));
        Object[] keys = Arrays.copyOf(keyLane, capacity);
        Object[] values = Arrays.copyOf(valueLane, capacity);
        int[] globalPrevious = Arrays.copyOf(globalPrevLane, capacity);
        int[] globalNext = Arrays.copyOf(globalNextLane, capacity);
        int[] keyPrevious = Arrays.copyOf(keyPrevLane, capacity);
        int[] keyNext = Arrays.copyOf(keyNextLane, capacity);
        int[] indexes = Arrays.copyOf(hashIndexLane, capacity);
        int[] counts = Arrays.copyOf(keyCountLane, capacity);
        long[] generations = Arrays.copyOf(generationLane, capacity);
        long[] states = Arrays.copyOf(stateMaskWords, (int) (((long) capacity + 31) >>> 5));
        keyLane = keys;
        valueLane = values;
        globalPrevLane = globalPrevious;
        globalNextLane = globalNext;
        keyPrevLane = keyPrevious;
        keyNextLane = keyNext;
        hashIndexLane = indexes;
        keyCountLane = counts;
        generationLane = generations;
        stateMaskWords = states;
    }
}
