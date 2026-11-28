/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
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
public final class ExpiringMapView<K, V> {
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
