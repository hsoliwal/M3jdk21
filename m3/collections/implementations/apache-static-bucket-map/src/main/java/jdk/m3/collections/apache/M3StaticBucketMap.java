/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Original first-party source; supplier APIs are references, not copied code.
 ******************************************************************************/
package jdk.m3.collections.apache;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Parent-owned columnar hash map with primitive open-addressed directory,
 * bit-masked live coordinates and recycling. No retained per-mapping objects.
 *
 * <p>The owner monitor serializes operations, including {@link #atomic}.
 * This deliberately offers safe, coarse-grained synchronization: it is not a
 * claim of the historical vendor's per-bucket concurrency or callback timing.
 * Source-built concurrency/product qualification is required before replacing
 * a vendor-facing runtime implementation.</p>
 *
 * <p>The public surface is the original M3 static-bucket scaffold's 29
 * declarations. The independent algorithm never invokes a model or native code.</p>
 */
public final class M3StaticBucketMap<K, V>
        implements Map<K, V>,
                org.apache.commons.collections4.Get<K, V>,
                org.apache.commons.collections4.IterableGet<K, V>,
                org.apache.commons.collections4.IterableMap<K, V>,
                org.apache.commons.collections4.Put<K, V> {
    private static final int FREE = -1;
    private static final int MIN_CAPACITY = 16;
    private static final int MAX_CAPACITY = 1 << 28;

    // Authoritative, parent-owned structure-of-arrays lanes, retained from
    // the original M3 scaffold's declared storage contract.
    private transient Object[] keys;
    private transient Object[] values;
    private transient int[] hashes;
    private transient int[] index;        // slot + 1; 0 empty; -1 deleted
    private transient long[] liveMask;    // packed live-slot state
    private transient int size;
    private transient int modCount;

    // Physical slot recycler. Entries are *never* allocated as Node objects.
    private transient int[] nextFree;
    private transient int highWater;
    private transient int freeHead = FREE;
    private transient int tombstones;
    private transient Set<K> keyView;
    private transient Set<Map.Entry<K, V>> entryView;
    private transient Collection<V> valueView;

    /** Original zero-argument public constructor. */
    public M3StaticBucketMap() {
        this(255);
    }

    /** Owns a bounded array index sized for the requested expected entries. */
    public M3StaticBucketMap(final int expectedEntries) {
        if (expectedEntries < 0 || expectedEntries > MAX_CAPACITY / 2) {
            throw new IllegalArgumentException("expectedEntries");
        }
        final int directorySize = powerOfTwo(Math.max(MIN_CAPACITY, 2L * expectedEntries));
        index = new int[directorySize];
        final int initial = MIN_CAPACITY;
        keys = new Object[initial];
        values = new Object[initial];
        hashes = new int[initial];
        nextFree = new int[initial];
        liveMask = new long[(initial + 63) >>> 6];
    }

    private static int powerOfTwo(final long requested) {
        if (requested > MAX_CAPACITY) throw new IllegalArgumentException("capacity");
        final int n = (int) requested;
        return n <= 1 ? 1 : Integer.highestOneBit(n - 1) << 1;
    }

    private static int mixed(final int hash) {
        int h = hash;
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        h *= 0x846ca68b;
        return h ^ (h >>> 16);
    }

    private static int keyHash(final Object key) { return key == null ? 0 : key.hashCode(); }

    /** Must run with this owner's monitor held. */
    private int findDirectory(final Object key, final int hash) {
        final int expected = modCount;
        final int mask = index.length - 1;
        int position = mixed(hash) & mask;
        for (int attempts = 0; attempts < index.length; attempts++) {
            final int encoded = index[position];
            if (encoded == 0) return -1;
            if (encoded > 0) {
                final int slot = encoded - 1;
                if (hashes[slot] == hash) {
                    final boolean same = Objects.equals(keys[slot], key);
                    if (expected != modCount) {
                        throw new ConcurrentModificationException("reentrant key equality");
                    }
                    if (same) return position;
                }
            }
            position = (position + 1) & mask;
        }
        return -1;
    }

    private int insertDirectory(final int hash) {
        final int mask = index.length - 1;
        int position = mixed(hash) & mask;
        int deleted = -1;
        for (int attempts = 0; attempts < index.length; attempts++) {
            final int encoded = index[position];
            if (encoded == 0) return deleted >= 0 ? deleted : position;
            if (encoded == -1 && deleted < 0) deleted = position;
            position = (position + 1) & mask;
        }
        if (deleted >= 0) return deleted;
        throw new IllegalStateException("directory exhausted");
    }

    /** Rebuilds only the integer directory, never per-entry objects. */
    private void reindex(final int capacity) {
        final int[] fresh = new int[capacity];
        final int mask = capacity - 1;
        for (int word = 0; word < liveMask.length; word++) {
            long bits = liveMask[word];
            while (bits != 0) {
                final int slot = (word << 6) + Long.numberOfTrailingZeros(bits);
                bits &= bits - 1;
                if (slot >= highWater) continue;
                int position = mixed(hashes[slot]) & mask;
                while (fresh[position] != 0) position = (position + 1) & mask;
                fresh[position] = slot + 1;
            }
        }
        index = fresh;
        tombstones = 0;
    }

    /** Preserve failure atomicity on multi-array growth. */
    private void growSlots() {
        final int capacity = keys.length;
        if (capacity == MAX_CAPACITY) throw new IllegalStateException("slot limit");
        final int nextCapacity = (int) Math.min((long) MAX_CAPACITY,
                (long) capacity + Math.max(8, capacity >>> 1));
        final Object[] grownKeys = java.util.Arrays.copyOf(keys, nextCapacity);
        final Object[] grownValues = java.util.Arrays.copyOf(values, nextCapacity);
        final int[] grownHashes = java.util.Arrays.copyOf(hashes, nextCapacity);
        final int[] grownFree = java.util.Arrays.copyOf(nextFree, nextCapacity);
        final long[] grownLive = java.util.Arrays.copyOf(liveMask, (nextCapacity + 63) >>> 6);
        keys = grownKeys;
        values = grownValues;
        hashes = grownHashes;
        nextFree = grownFree;
        liveMask = grownLive;
    }

    private int allocateSlot() {
        if (freeHead != FREE) {
            final int slot = freeHead;
            freeHead = nextFree[slot];
            return slot;
        }
        if (highWater == keys.length) growSlots();
        return highWater++;
    }

    private void releaseSlot(final int slot) {
        keys[slot] = null;
        values[slot] = null;
        hashes[slot] = 0;
        liveMask[slot >>> 6] &= ~(1L << slot);
        nextFree[slot] = freeHead;
        freeHead = slot;
    }

    private void checkDirectoryGrowth() {
        // Count tombstones to cap unsuccessful probes. Rebuild in-place when
        // there are too many holes rather than growing needlessly.
        if (((long) size + tombstones + 1) * 4 < (long) index.length * 3) return;
        final int needed = (long) (size + 1) * 2 > index.length
                ? Math.min(MAX_CAPACITY, index.length << 1) : index.length;
        if (needed == index.length && tombstones == 0) return;
        reindex(needed);
    }

    private int findSlot(final Object key) {
        final int directory = findDirectory(key, keyHash(key));
        return directory < 0 ? -1 : index[directory] - 1;
    }

    @Override public int size() { synchronized (this) { return size; } }
    @Override public boolean isEmpty() { synchronized (this) { return size == 0; } }
    @Override public boolean containsKey(final Object key) { synchronized (this) { return findSlot(key) >= 0; } }

    @SuppressWarnings("unchecked")
    @Override public V get(final Object key) {
        synchronized (this) {
            final int slot = findSlot(key);
            return slot < 0 ? null : (V) values[slot];
        }
    }

    @Override public V getOrDefault(final Object key, final V fallback) {
        synchronized (this) {
            final int slot = findSlot(key);
            return slot < 0 ? fallback : valueAt(slot);
        }
    }

    @SuppressWarnings("unchecked")
    private V valueAt(final int slot) { return (V) values[slot]; }

    @SuppressWarnings("unchecked")
    private K keyAt(final int slot) { return (K) keys[slot]; }

    @Override public V put(final K key, final V value) {
        synchronized (this) {
            final int hash = keyHash(key);
            final int position = findDirectory(key, hash);
            if (position >= 0) {
                final int slot = index[position] - 1;
                final V previous = valueAt(slot);
                values[slot] = value;
                return previous;
            }
            checkDirectoryGrowth();
            final int destination = insertDirectory(hash);
            final int slot = allocateSlot();
            if (index[destination] == -1) tombstones--;
            keys[slot] = key;
            values[slot] = value;
            hashes[slot] = hash;
            liveMask[slot >>> 6] |= 1L << slot;
            index[destination] = slot + 1;
            size++;
            modCount++;
            return null;
        }
    }

    @Override public V remove(final Object key) {
        synchronized (this) {
            final int directory = findDirectory(key, keyHash(key));
            if (directory < 0) return null;
            final int slot = index[directory] - 1;
            final V old = valueAt(slot);
            index[directory] = -1;
            tombstones++;
            releaseSlot(slot);
            size--;
            modCount++;
            return old;
        }
    }

    @Override public void clear() {
        synchronized (this) {
            if (size == 0) {
                // After individual removals all keys/values are already cleared.
                // Keep allocated slot pages for reuse; reset only tombstoned probes.
                if (tombstones != 0) {
                    java.util.Arrays.fill(index, 0);
                    tombstones = 0;
                }
                modCount++;
                return;
            }
            java.util.Arrays.fill(index, 0);
            java.util.Arrays.fill(keys, 0, highWater, null);
            java.util.Arrays.fill(values, 0, highWater, null);
            java.util.Arrays.fill(hashes, 0, highWater, 0);
            java.util.Arrays.fill(nextFree, 0, highWater, 0);
            java.util.Arrays.fill(liveMask, 0);
            size = 0;
            highWater = 0;
            freeHead = FREE;
            tombstones = 0;
            modCount++;
        }
    }

    @Override public boolean containsValue(final Object value) {
        synchronized (this) {
            final int expected = modCount;
            for (int slot = nextLive(0); slot >= 0; slot = nextLive(slot + 1)) {
                final boolean same = Objects.equals(values[slot], value);
                if (expected != modCount) {
                    throw new ConcurrentModificationException("reentrant value equality");
                }
                if (same) return true;
            }
            return false;
        }
    }

    private int nextLive(final int from) {
        int word = from >>> 6;
        if (word >= liveMask.length) return -1;
        long bits = liveMask[word] & (-1L << from);
        while (true) {
            if (bits != 0) {
                final int slot = (word << 6) + Long.numberOfTrailingZeros(bits);
                return slot < highWater ? slot : -1;
            }
            if (++word == liveMask.length) return -1;
            bits = liveMask[word];
        }
    }

    @Override public void putAll(final Map<? extends K, ? extends V> mappings) {
        synchronized (this) {
            Objects.requireNonNull(mappings, "mappings");
            for (Map.Entry<? extends K, ? extends V> entry : mappings.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Original atomic API: actions run with exclusive ownership of all lanes. */
    public void atomic(final Runnable action) {
        Objects.requireNonNull(action, "action");
        synchronized (this) { action.run(); }
    }

    @Override public boolean remove(final Object key, final Object value) {
        synchronized (this) {
            final int slot = findSlot(key);
            if (slot < 0 || !Objects.equals(values[slot], value)) return false;
            remove(key);
            return true;
        }
    }

    @Override public V putIfAbsent(final K key, final V value) {
        synchronized (this) {
            final V previous = get(key);
            if (previous == null) put(key, value);
            return previous;
        }
    }

    @Override public V replace(final K key, final V value) {
        synchronized (this) {
            final int slot = findSlot(key);
            if (slot < 0) return null;
            final V previous = valueAt(slot);
            values[slot] = value;
            return previous;
        }
    }

    @Override public boolean replace(final K key, final V previous, final V replacement) {
        synchronized (this) {
            final int slot = findSlot(key);
            if (slot < 0 || !Objects.equals(values[slot], previous)) return false;
            values[slot] = replacement;
            return true;
        }
    }

    @Override public V computeIfAbsent(final K key, final Function<? super K, ? extends V> factory) {
        synchronized (this) {
            Objects.requireNonNull(factory, "factory");
            final V present = get(key);
            if (present != null) return present;
            final V result = factory.apply(key);
            if (result != null) put(key, result);
            return result;
        }
    }

    @Override public V computeIfPresent(final K key,
            final BiFunction<? super K, ? super V, ? extends V> factory) {
        synchronized (this) {
            Objects.requireNonNull(factory, "factory");
            final V present = get(key);
            if (present == null) return null;
            final V result = factory.apply(key, present);
            if (result == null) remove(key);
            else put(key, result);
            return result;
        }
    }

    @Override public V compute(final K key,
            final BiFunction<? super K, ? super V, ? extends V> function) {
        synchronized (this) {
            Objects.requireNonNull(function, "function");
            final V previous = get(key);
            final V computed = function.apply(key, previous);
            if (computed == null) {
                if (previous != null || containsKey(key)) remove(key);
            } else put(key, computed);
            return computed;
        }
    }

    @Override public V merge(final K key, final V value,
            final BiFunction<? super V, ? super V, ? extends V> function) {
        synchronized (this) {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(function, "function");
            final V old = get(key);
            final V result = old == null ? value : function.apply(old, value);
            if (result == null) remove(key);
            else put(key, result);
            return result;
        }
    }

    @Override public void forEach(final BiConsumer<? super K, ? super V> action) {
        synchronized (this) {
            Objects.requireNonNull(action, "action");
            final int expected = modCount;
            for (int slot = nextLive(0); slot >= 0; slot = nextLive(slot + 1)) {
                action.accept(keyAt(slot), valueAt(slot));
                if (expected != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    @Override public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        synchronized (this) {
            Objects.requireNonNull(function, "function");
            final int expected = modCount;
            for (int slot = nextLive(0); slot >= 0; slot = nextLive(slot + 1)) {
                values[slot] = function.apply(keyAt(slot), valueAt(slot));
                if (expected != modCount) throw new ConcurrentModificationException();
            }
        }
    }

    @Override public int hashCode() {
        synchronized (this) {
            int result = 0;
            for (int slot = nextLive(0); slot >= 0; slot = nextLive(slot + 1)) {
                result += Objects.hashCode(keys[slot]) ^ Objects.hashCode(values[slot]);
            }
            return result;
        }
    }

    @Override public boolean equals(final Object candidate) {
        synchronized (this) {
            if (candidate == this) return true;
            if (!(candidate instanceof Map<?, ?> other) || other.size() != size) return false;
            for (int slot = nextLive(0); slot >= 0; slot = nextLive(slot + 1)) {
                final Object key = keys[slot];
                final Object value = values[slot];
                final Object theirs = other.get(key);
                if (!Objects.equals(value, theirs) || (value == null && !other.containsKey(key))) {
                    return false;
                }
            }
            return true;
        }
    }

    private abstract class SlotIterator<T> implements Iterator<T> {
        private int cursor;
        private int next = -2;
        private int last = -1;
        private int expectedMod = modCount;

        private void check() {
            if (expectedMod != modCount) throw new ConcurrentModificationException();
        }

        @Override public boolean hasNext() {
            synchronized (M3StaticBucketMap.this) {
                check();
                if (next == -2) next = nextLive(cursor);
                return next != -1;
            }
        }

        final K nextKey() {
            synchronized (M3StaticBucketMap.this) {
                if (!hasNext()) throw new NoSuchElementException();
                last = next;
                cursor = next + 1;
                next = -2;
                return keyAt(last);
            }
        }

        @Override public void remove() {
            synchronized (M3StaticBucketMap.this) {
                check();
                if (last < 0) throw new IllegalStateException();
                M3StaticBucketMap.this.remove(keyAt(last));
                expectedMod = modCount;
                last = -1;
            }
        }

        @Override public T next() {
            return valueFor(nextKey());
        }

        abstract T valueFor(K key);
    }

    private final class EntryView implements Map.Entry<K, V> {
        private final K key;
        EntryView(final K key) { this.key = key; }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return M3StaticBucketMap.this.get(key); }
        @Override public V setValue(final V value) {
            synchronized (M3StaticBucketMap.this) {
                if (!containsKey(key)) throw new IllegalStateException("entry removed");
                return put(key, value);
            }
        }
        @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }
        @Override public boolean equals(final Object other) {
            return other instanceof Map.Entry<?, ?> entry
                    && Objects.equals(key, entry.getKey())
                    && Objects.equals(getValue(), entry.getValue());
        }
    }

    @Override public Set<Map.Entry<K, V>> entrySet() {
        synchronized (this) {
            if (entryView == null) {
                entryView = new AbstractSet<>() {
                    @Override public int size() { return M3StaticBucketMap.this.size(); }
                    @Override public void clear() { M3StaticBucketMap.this.clear(); }
                    @Override public Iterator<Map.Entry<K, V>> iterator() {
                        return new SlotIterator<>() {
                            @Override Map.Entry<K, V> valueFor(final K key) { return new EntryView(key); }
                        };
                    }
                    @Override public boolean contains(final Object target) {
                        if (!(target instanceof Map.Entry<?, ?> entry)) return false;
                        synchronized (M3StaticBucketMap.this) {
                            final int slot = findSlot(entry.getKey());
                            return slot >= 0 && Objects.equals(values[slot], entry.getValue());
                        }
                    }
                    @Override public boolean remove(final Object target) {
                        if (!(target instanceof Map.Entry<?, ?> entry)) return false;
                        return M3StaticBucketMap.this.remove(entry.getKey(), entry.getValue());
                    }
                };
            }
            return entryView;
        }
    }

    @Override public Set<K> keySet() {
        synchronized (this) {
            if (keyView == null) {
                keyView = new AbstractSet<>() {
                    @Override public int size() { return M3StaticBucketMap.this.size(); }
                    @Override public void clear() { M3StaticBucketMap.this.clear(); }
                    @Override public Iterator<K> iterator() {
                        return new SlotIterator<>() {
                            @Override K valueFor(final K key) { return key; }
                        };
                    }
                    @Override public boolean contains(final Object key) { return containsKey(key); }
                    @Override public boolean remove(final Object key) {
                        synchronized (M3StaticBucketMap.this) {
                            if (!containsKey(key)) return false;
                            M3StaticBucketMap.this.remove(key);
                            return true;
                        }
                    }
                };
            }
            return keyView;
        }
    }

    @Override public Collection<V> values() {
        synchronized (this) {
            if (valueView == null) {
                valueView = new AbstractCollection<>() {
                    @Override public int size() { return M3StaticBucketMap.this.size(); }
                    @Override public void clear() { M3StaticBucketMap.this.clear(); }
                    @Override public Iterator<V> iterator() {
                        return new SlotIterator<>() {
                            @Override V valueFor(final K key) { return M3StaticBucketMap.this.get(key); }
                        };
                    }
                    @Override public boolean contains(final Object value) { return containsValue(value); }
                };
            }
            return valueView;
        }
    }

    @Override public org.apache.commons.collections4.MapIterator<K, V> mapIterator() {
        return new org.apache.commons.collections4.MapIterator<>() {
            private final SlotIterator<K> delegate = new SlotIterator<>() {
                @Override K valueFor(final K key) { return key; }
            };
            private K current;
            private boolean valid;
            @Override public boolean hasNext() { return delegate.hasNext(); }
            @Override public K next() {
                current = delegate.next();
                valid = true;
                return current;
            }
            @Override public K getKey() {
                if (!valid) throw new IllegalStateException();
                return current;
            }
            @Override public V getValue() {
                if (!valid) throw new IllegalStateException();
                return M3StaticBucketMap.this.get(current);
            }
            @Override public V setValue(final V value) {
                if (!valid || !M3StaticBucketMap.this.containsKey(current)) {
                    throw new IllegalStateException("mapping removed");
                }
                return M3StaticBucketMap.this.put(current, value);
            }
            @Override public void remove() {
                if (!valid) throw new IllegalStateException();
                delegate.remove();
                valid = false;
            }
        };
    }
}
