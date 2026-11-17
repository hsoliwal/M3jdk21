/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import jdk.m3.collections.eclipse.api.block.HashingStrategy;
import jdk.m3.collections.eclipse.impl.block.factory.HashingStrategies;

/**
 * M3 five-library superset scaffold {@code StrategyHashMap}: API group {@code STRATEGY_HASH_MAP} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-m3-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>FASTUTIL Object2ObjectOpenCustomHashMap/Long2LongOpenCustomHashMap (Hash.Strategy): hashing-strategy maps: caller-supplied equals/hashCode [license Apache-2.0; NEW_OWNER (ring-2: strategy contract absent from ring-1 owners); priority P2; matrix owner PROPOSED:StrategyHashMap (also EC UnifiedMapWithHashingStrategy, Trove TObjectHashingStrategy pattern)]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind OBJECT; lanes {@code Object[] keyLane;Object[] valueLane;int[] hashLane}; offset scheme open-address slot; probe by strategy.hashCode; state-mask bits OCCUPIED,DELETED; stored nodes NONE; API entry objects TRANSIENT_FACADE (transient facades over slots, never the stored representation); matrix note: key+value Object[] lanes + int hash lane; probe offsets via strategy hash / occupancy+deleted masks.
 *
 * <p>Status: RECEIVED (2026-11-17). Equality is a strategy passed in, as ordering is a
 * {@link java.util.Comparator}: the module's one equality strategy, {@link HashingStrategy}
 * (Commons {@code Equator} and Guava {@code Equivalence} convert to it). Open addressing with
 * linear probing over a power-of-two table; the strategy's hash is spread by the golden-ratio
 * multiplier and kept in {@code hashLane}, so a probe compares hashes before it calls the strategy
 * and a resize never calls it. A removed key leaves a {@code DELETED} slot that later insertions
 * reuse; deleted slots count toward the load factor (3/4), and a resize clears them. Keys and
 * values may be null when the strategy accepts null; the default strategy is {@code equals} and
 * {@code hashCode}, null-safe. Not a {@code java.util} replacement: no {@code Map} views other
 * than {@link #entrySet()}.
 */
public final class StrategyHashMap<K, V> {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;

    /** Bits of state per slot in {@code stateMaskWords}. */
    private static final int STATE_BITS = 2;
    /** Slots per state word. */
    private static final int SLOTS_PER_WORD = Long.SIZE / STATE_BITS;
    /** Mask of one slot's state bits. */
    private static final long SLOT_STATE_MASK = (1L << STATE_BITS) - 1;
    /** Capacity of the first allocation. */
    private static final int MIN_CAPACITY = 8;
    /** Largest power-of-two capacity of an array. */
    private static final int MAX_CAPACITY = 1 << 30;
    /** Golden-ratio multiplier (2^32 / phi) that spreads a hash over the table. */
    private static final int GOLDEN_RATIO = 0x9E3779B9;
    /** {@code equals} and {@code hashCode}, null-safe. */
    private static final HashingStrategy<Object> DEFAULT_STRATEGY =
            HashingStrategies.nullSafeHashingStrategy(HashingStrategies.defaultStrategy());

    /** The equality of keys. */
    private final HashingStrategy<? super K> strategy;
    /** Storage lane {@code keyLane}: dynamic primitive/reference array lane. */
    private Object[] keyLane = new Object[0];
    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private Object[] valueLane = new Object[0];
    /** Storage lane {@code hashLane}: dynamic primitive/reference array lane. */
    private int[] hashLane = new int[0];
    /** Per-slot state words (OCCUPIED,DELETED); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;
    /** Slots in state DELETED. */
    private int deleted;
    /** Structural modifications, for the fail-fast iterator of {@link #entrySet()}. */
    private int modCount;

    /** Creates an empty map whose keys are equal by {@code equals} and {@code hashCode}. */
    public StrategyHashMap() {
        this(DEFAULT_STRATEGY);
    }

    /**
     * Creates an empty map whose keys are equal by the strategy.
     *
     * @param strategy the equality of keys
     * @throws NullPointerException if the strategy is null
     */
    public StrategyHashMap(HashingStrategy<? super K> strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3StrategyHashMap|STRATEGY_HASH_MAP|OBJECT|OCCUPIED,DELETED|Object[] keyLane;Object[] valueLane;int[] hashLane";
    }

    /**
     * Returns the value of the key equal to {@code key} by the strategy, or null.
     *
     * @param key the key
     * @return the value, or null when no key is equal
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int slot = find(key);
        return slot < 0 ? null : (V) valueLane[slot];
    }

    /**
     * Maps the key to the value; an equal key keeps its slot and gets the value.
     *
     * @param key the key
     * @param value the value
     * @return the previous value of an equal key, or null
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        int hash = strategy.computeHashCode(key);
        int slot = slotOf(key, hash);
        if (slot >= 0) {
            // an update is not a structural change: no room is reserved, nothing moves
            V previous = (V) valueLane[slot];
            valueLane[slot] = value;
            return previous;
        }
        reserveOne();
        occupy(freeSlot(hash), key, value, hash);
        return null;
    }

    /**
     * Removes the key equal to {@code key} by the strategy.
     *
     * @param key the key
     * @return its value, or null when no key is equal
     */
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        int slot = find(key);
        if (slot < 0) {
            return null;
        }
        V previous = (V) valueLane[slot];
        vacate(slot);
        return previous;
    }

    /**
     * Tells whether a key equal to {@code key} by the strategy is mapped.
     *
     * @param key the key
     * @return true when an equal key is mapped
     */
    public boolean containsKey(Object key) {
        return find(key) >= 0;
    }

    /**
     * Returns a view of the mappings. Its entries are transient facades over slots: they read and
     * {@code setValue} write the lanes. The iterator supports {@code remove} and fails fast when
     * the map is changed otherwise.
     *
     * @return the mappings
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySetView();
    }

    /**
     * Returns the number of mappings.
     *
     * @return the number of mappings
     */
    public int size() {
        return size;
    }

    /**
     * Tells whether there is no mapping.
     *
     * @return true when there is no mapping
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes all mappings; the capacity stays. */
    public void clear() {
        if (size == 0 && deleted == 0) {
            return;
        }
        Arrays.fill(keyLane, null);
        Arrays.fill(valueLane, null);
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
        deleted = 0;
        modCount++;
    }

    /** The slot of the key equal to {@code key}, or -1; the strategy sees the key, as in a put. */
    @SuppressWarnings("unchecked")
    private int find(Object key) {
        K probe = (K) key;
        return slotOf(probe, strategy.computeHashCode(probe));
    }

    /** The slot of the key equal to {@code key} whose strategy hash is {@code hash}, or -1. */
    @SuppressWarnings("unchecked")
    private int slotOf(K key, int hash) {
        if (size == 0) {
            return -1;
        }
        int mask = keyLane.length - 1;
        for (int slot = spread(hash) & mask; ; slot = (slot + 1) & mask) {
            long state = state(slot);
            if (state == 0) {
                return -1;
            }
            if (state == STATE_OCCUPIED && hashLane[slot] == hash
                    && strategy.equals((K) keyLane[slot], key)) {
                return slot;
            }
        }
    }

    /** The first deleted or empty slot on the probe sequence of {@code hash}; the key is absent. */
    private int freeSlot(int hash) {
        int mask = keyLane.length - 1;
        int slot = spread(hash) & mask;
        while (state(slot) == STATE_OCCUPIED) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    private static int spread(int hash) {
        int mixed = hash * GOLDEN_RATIO;
        return mixed ^ (mixed >>> 16);
    }

    private long state(int slot) {
        int shift = (slot % SLOTS_PER_WORD) * STATE_BITS;
        return (stateMaskWords[slot / SLOTS_PER_WORD] >>> shift) & SLOT_STATE_MASK;
    }

    private void setState(int slot, long state) {
        int word = slot / SLOTS_PER_WORD;
        int shift = (slot % SLOTS_PER_WORD) * STATE_BITS;
        long cleared = stateMaskWords[word] & ~(SLOT_STATE_MASK << shift);
        stateMaskWords[word] = cleared | (state << shift);
    }

    private void occupy(int slot, Object key, Object value, int hash) {
        if (state(slot) == STATE_DELETED) {
            deleted--;
        }
        keyLane[slot] = key;
        valueLane[slot] = value;
        hashLane[slot] = hash;
        setState(slot, STATE_OCCUPIED);
        size++;
        modCount++;
    }

    private void vacate(int slot) {
        keyLane[slot] = null;
        valueLane[slot] = null;
        setState(slot, STATE_DELETED);
        size--;
        deleted++;
        modCount++;
    }

    /** Makes room for one more mapping: first allocation, larger table, or no deleted slots. */
    private void reserveOne() {
        int capacity = keyLane.length;
        if (capacity == 0) {
            allocate(MIN_CAPACITY);
        } else if ((long) (size + deleted + 1) * 4 > (long) capacity * 3) {
            boolean grow = (long) (size + 1) * 2 > capacity;
            if (grow && capacity == MAX_CAPACITY) {
                throw new IllegalStateException("StrategyHashMap is full: " + size + " mappings");
            }
            rehash(grow ? capacity * 2 : capacity);
        }
    }

    private void allocate(int capacity) {
        keyLane = new Object[capacity];
        valueLane = new Object[capacity];
        hashLane = new int[capacity];
        stateMaskWords = new long[(capacity + SLOTS_PER_WORD - 1) / SLOTS_PER_WORD];
    }

    /** Moves the mappings into fresh lanes of the capacity by their kept hashes. */
    private void rehash(int capacity) {
        Object[] keys = keyLane;
        Object[] values = valueLane;
        int[] hashes = hashLane;
        long[] states = stateMaskWords;
        allocate(capacity);
        int mask = capacity - 1;
        for (int old = 0; old < keys.length; old++) {
            int shift = (old % SLOTS_PER_WORD) * STATE_BITS;
            if (((states[old / SLOTS_PER_WORD] >>> shift) & SLOT_STATE_MASK) != STATE_OCCUPIED) {
                continue;
            }
            int slot = spread(hashes[old]) & mask;
            while (state(slot) != 0) {
                slot = (slot + 1) & mask;
            }
            keyLane[slot] = keys[old];
            valueLane[slot] = values[old];
            hashLane[slot] = hashes[old];
            setState(slot, STATE_OCCUPIED);
        }
        deleted = 0;
        modCount++;
    }

    /** The mappings, read and written through the lanes. */
    private final class EntrySetView extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            StrategyHashMap.this.clear();
        }

        /** A mapping is in the view when its key is equal to a key of the map by the strategy. */
        @Override
        public boolean contains(Object o) {
            return slotOfEntry(o) >= 0;
        }

        @Override
        public boolean remove(Object o) {
            int slot = slotOfEntry(o);
            if (slot < 0) {
                return false;
            }
            vacate(slot);
            return true;
        }

        private int slotOfEntry(Object o) {
            if (!(o instanceof Map.Entry<?, ?> entry)) {
                return -1;
            }
            int slot = find(entry.getKey());
            return slot >= 0 && Objects.equals(valueLane[slot], entry.getValue()) ? slot : -1;
        }
    }

    /** Walks the occupied slots in table order. */
    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {
        private int next = advance(0);
        private int last = -1;
        private int expectedModCount = modCount;

        private int advance(int from) {
            int slot = from;
            while (slot < keyLane.length && state(slot) != STATE_OCCUPIED) {
                slot++;
            }
            return slot;
        }

        @Override
        public boolean hasNext() {
            return next < keyLane.length;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (next >= keyLane.length) {
                throw new NoSuchElementException();
            }
            last = next;
            next = advance(next + 1);
            return new SlotEntry(last);
        }

        @Override
        public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            vacate(last);
            last = -1;
            expectedModCount = modCount;
        }
    }

    /**
     * A transient facade over one slot. While its key holds the slot, it reads and writes the
     * lanes; once the mapping is removed or moved by a resize, it keeps the key and the last value
     * it saw, and {@code setValue} throws {@code IllegalStateException}.
     */
    private final class SlotEntry implements Map.Entry<K, V> {
        private final int slot;
        private final K key;
        private V lastValue;

        @SuppressWarnings("unchecked")
        SlotEntry(int slot) {
            this.slot = slot;
            this.key = (K) keyLane[slot];
            this.lastValue = (V) valueLane[slot];
        }

        /** Whether this entry's key still holds its slot. */
        private boolean live() {
            return slot < keyLane.length && state(slot) == STATE_OCCUPIED && keyLane[slot] == key;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V getValue() {
            if (live()) {
                lastValue = (V) valueLane[slot];
            }
            return lastValue;
        }

        @Override
        public V setValue(V value) {
            if (!live()) {
                throw new IllegalStateException("the mapping of " + key + " left its slot");
            }
            V previous = getValue();
            valueLane[slot] = value;
            lastValue = value;
            return previous;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> entry
                    && Objects.equals(getKey(), entry.getKey())
                    && Objects.equals(getValue(), entry.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
