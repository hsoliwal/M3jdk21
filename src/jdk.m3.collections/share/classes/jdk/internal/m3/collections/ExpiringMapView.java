/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Passive-expiry object map stored as parallel key, value, deadline and hash
 * arrays. The existing {@link SlotIndex} owns primitive ordinal indexing; no
 * retained Entry, Node, wrapper map or per-key expiration objects are needed.
 *
 * <p>Negative TTL never expires; zero TTL expires on the next expiry-sensitive
 * access; a positive TTL is measured in milliseconds from each put. A clock
 * supplied to the constructor enables deterministic testing. An optional
 * policy computes absolute deadlines per inserted key/value pair. Reads expire only
 * the requested key; size and isEmpty sweep all expired rows. As in Commons
 * PassiveExpiringMap, remove does not first expire its requested key.
 *
 * <p>Not thread-safe. Keys must not change their hash or equality while stored.
 */
public final class ExpiringMapView<K, V> implements Map<K, V> {
    static final long STATE_OCCUPIED = 1L << 0;
    static final long STATE_DELETED = 1L << 1;
    static final long STATE_EXPIRED = 1L << 2;
    private static final int SLOTS_PER_MASK_WORD = 21;
    private static final long STATE_BITS = 7L;
    private static final int MAX_ENTRIES = 1 << 28;

    private Object[] keyLane = new Object[0];
    private Object[] valueLane = new Object[0];
    private long[] deadlineLane = new long[0];
    private int[] hashLane = new int[0];
    private int[] hashIndexLane = new int[0];
    private long[] stateMaskWords = new long[0];
    private int size;
    private int epoch;
    // Conservative minimum live deadline; stale-low hints only cause extra sweeps.
    private long nextDeadline = Long.MAX_VALUE;
    private final long ttlMillis;
    private final LongSupplier clock;
    private final ExpirationPolicy<? super K, ? super V> policy;

    /** Matches Commons' policy contract: a negative deadline never expires. */
    @FunctionalInterface
    public interface ExpirationPolicy<K, V> {
        long expirationTime(K key, V value);
    }

    /** The original no-argument constructor now creates a never-expiring map. */
    public ExpiringMapView() {
        this(-1L, System::currentTimeMillis);
    }

    /** Constant TTL in milliseconds, negative for no expiry. */
    public ExpiringMapView(long ttlMillis) {
        this(ttlMillis, System::currentTimeMillis);
    }

    /** Clock-injected constant TTL, without per-entry callback objects. */
    public ExpiringMapView(long ttlMillis, LongSupplier clock) {
        this.ttlMillis = ttlMillis;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policy = null;
    }

    /** A Commons-style per-entry deadline policy, with the system clock. */
    public ExpiringMapView(ExpirationPolicy<? super K, ? super V> policy) {
        this(policy, System::currentTimeMillis);
    }

    /** Clock-injected deadline policy; no extra object is stored per mapping. */
    public ExpiringMapView(ExpirationPolicy<? super K, ? super V> policy, LongSupplier clock) {
        this.ttlMillis = -1L;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /** Keep the original descriptor for consumers of the M3 collection census. */
    public static String scaffoldDescriptor() {
        return "M3ExpiringMapView|EXPIRING_MAP|OBJECT|OCCUPIED,DELETED,EXPIRED|Object[] keyLane;Object[] valueLane;long[] deadlineLane;int[] hashIndexLane";
    }


    /**
     * Map.containsValue: a full-view query expires stale rows before comparing
     * values. No retained values facade or per-entry map is required.
     */
    @Override
    public boolean containsValue(Object value) {
        size();
        int observed = epoch;
        for (int row = 0; row < size; row++) {
            if (Objects.equals(value, valueLane[row])) {
                if (observed != epoch) throw new ConcurrentModificationException();
                return true;
            }
            if (observed != epoch) throw new ConcurrentModificationException();
        }
        return false;
    }

    /** Applies the same policy and TTL stamping as repeated Map.put calls. */
    @Override
    public void putAll(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * The caller owns each iterator. It captures the structural epoch after
     * a single passive-expiry sweep and never retains a node per mapping.
     */
    private abstract class LaneCursor<T> implements Iterator<T> {
        int next;
        int last = -1;
        int expected;

        LaneCursor() {
            ExpiringMapView.this.size();
            expected = epoch;
        }

        private void check() {
            if (expected != epoch) throw new ConcurrentModificationException();
        }

        @Override public boolean hasNext() {
            check();
            return next < ExpiringMapView.this.size;
        }

        @Override public T next() {
            check();
            if (next >= ExpiringMapView.this.size) throw new NoSuchElementException();
            last = next++;
            return at(last);
        }

        @Override public void remove() {
            check();
            if (last < 0) throw new IllegalStateException();
            removeAt(last);
            next = last; // last physical row may have swapped into this ordinal
            last = -1;
            expected = epoch;
        }

        abstract T at(int row);
    }

    @SuppressWarnings("unchecked")
    private K keyAt(int row) { return (K) keyLane[row]; }

    @SuppressWarnings("unchecked")
    private V valueAt(int row) { return (V) valueLane[row]; }

    /**
     * Live key projection; the keys themselves remain in keyLane. The cursor
     * is fail-fast on structural removal or an expiry sweep by another call.
     */
    @Override public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override public int size() { return ExpiringMapView.this.size(); }
            @Override public boolean contains(Object key) {
                return ExpiringMapView.this.containsKey(key);
            }
            @Override public Iterator<K> iterator() {
                return new LaneCursor<>() {
                    @Override K at(int row) { return keyAt(row); }
                };
            }
            @Override public boolean remove(Object key) {
                if (!containsKey(key)) return false;
                ExpiringMapView.this.remove(key);
                return true;
            }
            @Override public void clear() { ExpiringMapView.this.clear(); }
        };
    }

    /** Live values projection; iterator.remove mutates the existing lanes. */
    @Override public Collection<V> values() {
        return new AbstractCollection<>() {
            @Override public int size() { return ExpiringMapView.this.size(); }
            @Override public boolean contains(Object value) {
                return ExpiringMapView.this.containsValue(value);
            }
            @Override public Iterator<V> iterator() {
                return new LaneCursor<>() {
                    @Override V at(int row) { return valueAt(row); }
                };
            }
            @Override public void clear() { ExpiringMapView.this.clear(); }
        };
    }

    /**
     * A per-iteration entry is only a projected API facade. It owns one key
     * reference but never becomes the map's retained physical storage.
     */
    private final class LiveEntry implements Map.Entry<K, V> {
        private final K key;
        LiveEntry(K key) { this.key = key; }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return ExpiringMapView.this.get(key); }
        @Override public V setValue(V value) {
            int row = liveRow(key, checkedHash(key), now());
            if (row < 0) throw new IllegalStateException("mapping no longer present");
            V prior = valueAt(row);
            valueLane[row] = value; // entry mutation does not renew expiration
            return prior;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Map.Entry<?, ?> entry)) return false;
            return Objects.equals(key, entry.getKey())
                    && Objects.equals(getValue(), entry.getValue());
        }
        @Override public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }
        @Override public String toString() { return key + "=" + getValue(); }
    }

    /** Live entry projection, including write-through setValue and removal. */
    @Override public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return ExpiringMapView.this.size(); }
            @Override public Iterator<Map.Entry<K, V>> iterator() {
                return new LaneCursor<>() {
                    @Override Map.Entry<K, V> at(int row) {
                        return new LiveEntry(keyAt(row));
                    }
                };
            }
            @Override public boolean contains(Object candidate) {
                if (!(candidate instanceof Map.Entry<?, ?> entry)) return false;
                Object key = entry.getKey();
                V value = ExpiringMapView.this.get(key);
                return Objects.equals(value, entry.getValue())
                        && (value != null || ExpiringMapView.this.containsKey(key));
            }
            @Override public boolean remove(Object candidate) {
                if (!(candidate instanceof Map.Entry<?, ?> entry)) return false;
                if (!contains(candidate)) return false;
                ExpiringMapView.this.remove(entry.getKey());
                return true;
            }
            @Override public void clear() { ExpiringMapView.this.clear(); }
        };
    }

    /** Standard Map equality, including explicit null mappings. */
    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Map<?, ?> map) || map.size() != size()) return false;
        int expected = epoch;
        for (int i = 0; i < size; i++) {
            Object key = keyLane[i];
            Object value = valueLane[i];
            Object found = map.get(key);
            if (!Objects.equals(value, found)
                    || (value == null && !map.containsKey(key))) return false;
            if (expected != epoch) throw new ConcurrentModificationException();
        }
        return true;
    }

    @Override public int hashCode() {
        size();
        int expected = epoch;
        int hash = 0;
        for (int i = 0; i < size; i++) {
            hash += Objects.hashCode(keyLane[i]) ^ Objects.hashCode(valueLane[i]);
            if (expected != epoch) throw new ConcurrentModificationException();
        }
        return hash;
    }

    @Override public String toString() {
        size();
        StringBuilder text = new StringBuilder("{");
        int expected = epoch;
        for (int i = 0; i < size; i++) {
            if (i != 0) text.append(", ");
            Object key = keyLane[i];
            Object value = valueLane[i];
            text.append(key == this ? "(this Map)" : key);
            text.append('=').append(value == this ? "(this Map)" : value);
            if (expected != epoch) throw new ConcurrentModificationException();
        }
        return text.append('}').toString();
    }

    private long now() {
        return ttlMillis < 0 && policy == null ? 0L : clock.getAsLong();
    }

    private long deadline(K key, V value, long timestamp) {
        if (policy != null) {
            int expected = epoch;
            long deadline = policy.expirationTime(key, value);
            if (epoch != expected) throw new java.util.ConcurrentModificationException();
            return deadline;
        }
        if (ttlMillis < 0) return -1L;
        if (ttlMillis > 0 && timestamp > Long.MAX_VALUE - ttlMillis) {
            return -1L; // donor: saturated TTL means never expire
        }
        return timestamp + ttlMillis;
    }

    private boolean expired(int row, long timestamp) {
        return deadlineLane[row] >= 0 && timestamp >= deadlineLane[row];
    }

    private void markState(int row, int flags) {
        int word = row / SLOTS_PER_MASK_WORD;
        int shift = (row % SLOTS_PER_MASK_WORD) * 3;
        long mask = STATE_BITS << shift;
        stateMaskWords[word] = (stateMaskWords[word] & ~mask)
                | (((long) flags & STATE_BITS) << shift);
    }

    /**
     * Reserves all lanes and builds a replacement index before publishing any
     * reference. If allocation fails, the existing map remains unchanged.
     */
    private void reserve(int required) {
        if (required < 0 || required > MAX_ENTRIES) {
            throw new OutOfMemoryError("M3 expiration map capacity exhausted");
        }
        int indexLength = SlotIndex.length(required);
        if (required <= keyLane.length && indexLength <= hashIndexLane.length) return;

        int target = keyLane.length;
        if (required > target) {
            long grown = target == 0 ? 8L : (long) target + (target >>> 1) + 1;
            target = (int) Math.min(MAX_ENTRIES, Math.max((long) required, grown));
        }
        Object[] keys = target == keyLane.length ? keyLane : Arrays.copyOf(keyLane, target);
        Object[] values = target == valueLane.length ? valueLane : Arrays.copyOf(valueLane, target);
        long[] deadlines = target == deadlineLane.length ? deadlineLane
                : Arrays.copyOf(deadlineLane, target);
        int[] hashes = target == hashLane.length ? hashLane : Arrays.copyOf(hashLane, target);
        long[] masks = target == keyLane.length ? stateMaskWords
                : Arrays.copyOf(stateMaskWords,
                        (int) ((target + (long) SLOTS_PER_MASK_WORD - 1) / SLOTS_PER_MASK_WORD));
        int[] index = indexLength <= hashIndexLane.length ? hashIndexLane
                : SlotIndex.rebuilt(hashes, size, required);

        keyLane = keys;
        valueLane = values;
        deadlineLane = deadlines;
        hashLane = hashes;
        stateMaskWords = masks;
        hashIndexLane = index;
    }

    /** Physically removes one indexed row; a final-row swap preserves density. */
    private void removeAt(int row) {
        int indexed = SlotIndex.positionOf(hashIndexLane, hashLane, row);
        SlotIndex.unindex(hashIndexLane, hashLane, indexed);
        int last = --size;
        if (row != last) {
            int moved = SlotIndex.positionOf(hashIndexLane, hashLane, last);
            hashIndexLane[moved] = row + 1;
            keyLane[row] = keyLane[last];
            valueLane[row] = valueLane[last];
            deadlineLane[row] = deadlineLane[last];
            hashLane[row] = hashLane[last];
            markState(row, (int) STATE_OCCUPIED);
        }
        keyLane[last] = null;
        valueLane[last] = null;
        deadlineLane[last] = 0L;
        hashLane[last] = 0;
        markState(last, 0);
        epoch++;
    }

    /** Re-entrant key callbacks must not leave a stale array-slot reference. */
    private int checkedHash(Object key) {
        int expected = epoch;
        int hash = SlotIndex.hash(key);
        if (epoch != expected) throw new java.util.ConcurrentModificationException();
        return hash;
    }

    /** Lookup and passive expiry; no unrelated rows are scanned. */
    private int liveRow(Object key, int hash, long timestamp) {
        int expected = epoch;
        int row = SlotIndex.find(hashIndexLane, hashLane, keyLane, key, hash);
        if (expected != epoch) throw new java.util.ConcurrentModificationException();
        if (row >= 0 && expired(row, timestamp)) {
            removeAt(row);
            return -1;
        }
        return row;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        long timestamp = now();
        int row = liveRow(key, checkedHash(key), timestamp);
        return row < 0 ? null : (V) valueLane[row];
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        long timestamp = now();
        int hash = checkedHash(key);
        int row = liveRow(key, hash, timestamp);
        long due = deadline(key, value, timestamp);
        if (row >= 0) {
            V prior = (V) valueLane[row];
            valueLane[row] = value;
            deadlineLane[row] = due;
            if (due >= 0 && due < nextDeadline) nextDeadline = due;
            epoch++;
            return prior;
        }
        reserve(Math.addExact(size, 1));
        int next = size;
        keyLane[next] = key;
        valueLane[next] = value;
        deadlineLane[next] = due;
        if (due >= 0 && due < nextDeadline) nextDeadline = due;
        hashLane[next] = hash;
        markState(next, (int) STATE_OCCUPIED);
        SlotIndex.place(hashIndexLane, hashLane, next);
        size++;
        epoch++;
        return null;
    }

    /** Commons remove returns the last mapped value, even if it was expired. */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int hash = checkedHash(key);
        int expected = epoch;
        int row = SlotIndex.find(hashIndexLane, hashLane, keyLane, key, hash);
        if (expected != epoch) throw new java.util.ConcurrentModificationException();
        if (row < 0) return null;
        V old = (V) valueLane[row];
        removeAt(row);
        return old;
    }

    public boolean containsKey(Object key) {
        long timestamp = now();
        return liveRow(key, checkedHash(key), timestamp) >= 0;
    }

    /** One clock sample; skip the full sweep until the earliest possible expiry. */
    public int size() {
        if (ttlMillis >= 0 || policy != null) {
            long timestamp = now();
            if (timestamp >= nextDeadline) {
                long future = Long.MAX_VALUE;
                for (int row = size - 1; row >= 0; row--) {
                    long due = deadlineLane[row];
                    if (due >= 0) {
                        if (timestamp >= due) removeAt(row);
                        else if (due < future) future = due;
                    }
                }
                nextDeadline = future;
            }
        }
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /** Clear retained keys and values, reusing the same packed lanes. */
    public void clear() {
        Arrays.fill(keyLane, 0, size, null);
        Arrays.fill(valueLane, 0, size, null);
        Arrays.fill(deadlineLane, 0, size, 0L);
        Arrays.fill(hashLane, 0, size, 0);
        Arrays.fill(hashIndexLane, 0);
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
        nextDeadline = Long.MAX_VALUE;
        epoch++;
    }
}
