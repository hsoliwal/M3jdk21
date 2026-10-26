/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.runtime;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * DOOP proof-of-runtime: keys and values live in parallel arrays, occupancy in
 * an integer mask array, and hash metadata in a separate integer array.
 * No stored Node, Entry or bucket object is allocated per mapping.
 *
 * This is a standalone experimental backend, not a replacement for java.util.HashMap.
 * It is intentionally single-threaded, does not implement serialization and uses
 * linear probing (not yet collision-tree hardened).
 */
public final class M3ArrayMap<K,V> extends AbstractMap<K,V> {
    private static final int EMPTY = 0, LIVE = 1, DELETED = 2;
    private static final int MIN_CAPACITY = 16;
    private Object[] keys = new Object[MIN_CAPACITY];
    private Object[] values = new Object[MIN_CAPACITY];
    private int[] hashes = new int[MIN_CAPACITY];
    private int[] mask = new int[MIN_CAPACITY];
    private int size, used, modCount;
    private transient Set<Map.Entry<K,V>> entries;

    private static int spread(Object key) {
        int h = key == null ? 0 : key.hashCode();
        return h ^ (h >>> 16);
    }

    private int find(Object key, int hash) {
        int at = hash & (keys.length - 1);
        for (int scanned = 0; scanned < keys.length; scanned++) {
            int state = mask[at];
            if (state == EMPTY) return -1;
            if (state == LIVE && hashes[at] == hash && Objects.equals(keys[at],key))
                return at;
            at = (at + 1) & (keys.length - 1);
        }
        return -1;
    }

    private int insertionSlot(Object key, int hash) {
        int at = hash & (keys.length - 1), deleted = -1;
        for (int scanned = 0; scanned < keys.length; scanned++) {
            int state = mask[at];
            if (state == EMPTY) return deleted >= 0 ? deleted : at;
            if (state == DELETED && deleted < 0) deleted = at;
            if (state == LIVE && hashes[at] == hash && Objects.equals(keys[at],key))
                return at;
            at = (at + 1) & (keys.length - 1);
        }
        if (deleted >= 0) return deleted;
        throw new IllegalStateException("Full M3ArrayMap");
    }

    private void resize(int capacity) {
        Object[] oldKeys = keys, oldValues = values;
        int[] oldHashes = hashes, oldMask = mask;
        keys = new Object[capacity];
        values = new Object[capacity];
        hashes = new int[capacity];
        mask = new int[capacity];
        used = 0;
        for (int i = 0; i < oldMask.length; i++) {
            if (oldMask[i] != LIVE) continue;
            int slot = insertionSlot(oldKeys[i], oldHashes[i]);
            keys[slot] = oldKeys[i];
            values[slot] = oldValues[i];
            hashes[slot] = oldHashes[i];
            mask[slot] = LIVE;
            used++;
        }
    }

    @Override public int size() { return size; }

    @Override public boolean containsKey(Object key) {
        return find(key, spread(key)) >= 0;
    }

    @SuppressWarnings("unchecked")
    @Override public V get(Object key) {
        int slot = find(key, spread(key));
        return slot < 0 ? null : (V) values[slot];
    }

    @SuppressWarnings("unchecked")
    @Override public V put(K key, V value) {
        int hash = spread(key);
        int existing = find(key, hash);
        if (existing >= 0) {
            V previous = (V) values[existing];
            values[existing] = value;
            return previous;
        }
        if ((used + 1L) * 4 >= keys.length * 3L) {
            if (size * 4L < keys.length * 2L) resize(keys.length);
            else resize(keys.length << 1);
        }
        int slot = insertionSlot(key, hash);
        if (mask[slot] == EMPTY) used++;
        mask[slot] = LIVE;
        hashes[slot] = hash;
        keys[slot] = key;
        values[slot] = value;
        size++;
        modCount++;
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override public V remove(Object key) {
        int slot = find(key, spread(key));
        if (slot < 0) return null;
        V previous = (V) values[slot];
        deleteAt(slot);
        return previous;
    }

    private void deleteAt(int slot) {
        mask[slot] = DELETED;
        keys[slot] = null;
        values[slot] = null;
        hashes[slot] = 0;
        size--;
        modCount++;
        if (size == 0) {
            Arrays.fill(mask, EMPTY);
            used = 0;
        }
    }

    @Override public void clear() {
        if (size == 0 && used == 0) return;
        Arrays.fill(keys, null);
        Arrays.fill(values, null);
        Arrays.fill(mask, EMPTY);
        Arrays.fill(hashes, 0);
        size = used = 0;
        modCount++;
    }

    @Override public Set<Map.Entry<K,V>> entrySet() {
        if (entries == null) entries = new EntryView();
        return entries;
    }

    private final class EntryView extends AbstractSet<Map.Entry<K,V>> {
        @Override public int size() { return M3ArrayMap.this.size(); }
        @Override public void clear() { M3ArrayMap.this.clear(); }
        @Override public Iterator<Map.Entry<K,V>> iterator() { return new EntryIterator(); }
        @Override public boolean contains(Object item) {
            if (!(item instanceof Map.Entry<?,?> e)) return false;
            int slot = find(e.getKey(), spread(e.getKey()));
            return slot >= 0 && Objects.equals(values[slot], e.getValue());
        }
        @Override public boolean remove(Object item) {
            if (!contains(item)) return false;
            M3ArrayMap.this.remove(((Map.Entry<?,?>)item).getKey());
            return true;
        }
    }

    private final class EntryIterator implements Iterator<Map.Entry<K,V>> {
        private int cursor, last = -1, expected = modCount;
        private void check() {
            if (expected != modCount) throw new ConcurrentModificationException();
        }
        private int nextSlot() {
            int i = cursor;
            while (i < mask.length && mask[i] != LIVE) i++;
            return i;
        }
        @Override public boolean hasNext() { check(); return nextSlot() < mask.length; }
        @Override public Map.Entry<K,V> next() {
            check();
            int slot = nextSlot();
            if (slot >= mask.length) throw new NoSuchElementException();
            cursor = slot + 1;
            last = slot;
            @SuppressWarnings("unchecked") K key = (K) keys[slot];
            return new LazyEntry(key);
        }
        @Override public void remove() {
            check();
            if (last < 0) throw new IllegalStateException();
            deleteAt(last);
            expected = modCount;
            last = -1;
        }
    }

    /** Entry is materialized only by an API call, never stored as a map record. */
    private final class LazyEntry implements Map.Entry<K,V> {
        private final K key;
        LazyEntry(K key) { this.key = key; }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return M3ArrayMap.this.get(key); }
        @Override public V setValue(V value) {
            int slot = find(key, spread(key));
            if (slot < 0) throw new IllegalStateException("Entry no longer present");
            @SuppressWarnings("unchecked") V old = (V) values[slot];
            values[slot] = value;
            return old;
        }
        @Override public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }
        @Override public boolean equals(Object other) {
            return other instanceof Map.Entry<?,?> e &&
                Objects.equals(key,e.getKey()) && Objects.equals(getValue(),e.getValue());
        }
        @Override public String toString() { return key + "=" + getValue(); }
    }
}
