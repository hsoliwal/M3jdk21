/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Map from a pair or a triple of keys to a value, stored in parallel array lanes without a key holder object.
 *
 * <p>A mapping lives in one slot of the three key lanes and the value lane; a pair marks its third key slot as
 * absent, so the pair {@code (a, b)} and the triple {@code (a, b, null)} are different keys. The live slots are
 * always the dense prefix {@code 0..size-1}: a removal moves the last slot into the freed one. An open-addressed int
 * lane maps the combined hash of the keys to the slot; a removal closes its gap there, so the lane holds no dead
 * entries. A lookup, an update and a removal are expected O(1); {@link #removeAll} scans the slots. Null keys and
 * null values are allowed.</p>
 *
 * <p>Provider comparison: Apache Commons Collections {@code MultiKeyMap} (Apache-2.0): the same results for the
 * operations declared here. Not thread safe. Not a {@code java.util} replacement.</p>
 *
 * @param <K> the type of every key component
 * @param <V> the value type
 */
public final class MultiKeyMap<K, V> {
    /** State-mask bit {@code OCCUPIED} (per-slot state bit). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED}: declared by the ledger, never set, because removals keep the slots dense. */
    static final long STATE_DELETED = 1L << 1;
    /** Third key of a pair. */
    private static final Object ABSENT = new Object();
    private static final int MIN_SLOTS = 8;
    private static final int MAX_SLOTS = 1 << 29;
    private static final int WORD_SHIFT = 6;
    private static final int FOLD_SHIFT = 16;
    private static final int GOLDEN_RATIO_32 = 0x9E3779B9;

    /** Storage lane {@code key1Lane}: the first key of each slot. */
    private Object[] key1Lane = new Object[0];
    /** Storage lane {@code key2Lane}: the second key of each slot. */
    private Object[] key2Lane = new Object[0];
    /** Storage lane {@code key3Lane}: the third key of each slot, or the absent mark of a pair. */
    private Object[] key3Lane = new Object[0];
    /** Storage lane {@code valueLane}: the value of each slot. */
    private Object[] valueLane = new Object[0];
    /** Storage lane {@code hashIndexLane}: open-addressed table of slot + 1; 0 is empty; at most half full. */
    private int[] hashIndexLane = new int[0];
    /** One OCCUPIED bit per slot, 64 slots per word; exactly the bits below {@code size} are set. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty map. */
    public MultiKeyMap() {
    }

    /**
     * Reports the declared state-mask bits and lanes for ledgers; never allocates per element.
     *
     * @return the descriptor
     */
    public static String scaffoldDescriptor() {
        return "MultiKeyMap|MULTIKEY_MAP|OBJECT|OCCUPIED,DELETED|Object[] key1Lane;Object[] key2Lane;Object[] key3Lane;Object[] valueLane;int[] hashIndexLane";
    }

    /**
     * Returns the value of the pair.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @return the value, or null when there is no mapping or the mapping holds null
     */
    public V get(Object key1, Object key2) {
        int at = find(key1, key2, ABSENT);
        return at < 0 ? null : value(hashIndexLane[at] - 1);
    }

    /**
     * Returns the value of the triple.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @param key3 the third key
     * @return the value, or null when there is no mapping or the mapping holds null
     */
    public V get(Object key1, Object key2, Object key3) {
        int at = find(key1, key2, key3);
        return at < 0 ? null : value(hashIndexLane[at] - 1);
    }

    /**
     * Tests whether the pair has a mapping.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @return whether the pair is mapped
     */
    public boolean containsKey(Object key1, Object key2) {
        return find(key1, key2, ABSENT) >= 0;
    }

    /**
     * Maps the pair to the value.
     *
     * @param key1  the first key
     * @param key2  the second key
     * @param value the value
     * @return the value the pair was mapped to before, or null
     */
    public V put(K key1, K key2, V value) {
        return store(key1, key2, ABSENT, value);
    }

    /**
     * Maps the triple to the value.
     *
     * @param key1  the first key
     * @param key2  the second key
     * @param key3  the third key
     * @param value the value
     * @return the value the triple was mapped to before, or null
     */
    public V put(K key1, K key2, K key3, V value) {
        return store(key1, key2, key3, value);
    }

    /**
     * Removes the mapping of the pair.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @return the value the pair was mapped to, or null
     */
    public V removeMultiKey(Object key1, Object key2) {
        int at = find(key1, key2, ABSENT);
        if (at < 0) {
            return null;
        }
        V old = value(hashIndexLane[at] - 1);
        removeAt(at);
        return old;
    }

    /**
     * Removes every mapping, pair or triple, whose first key equals {@code key1}.
     *
     * @param key1 the first key to match
     * @return whether a mapping was removed
     */
    public boolean removeAll(Object key1) {
        int before = size;
        for (int slot = size - 1; slot >= 0; slot--) {
            if (Objects.equals(key1, key1Lane[slot])) {
                removeAt(find(key1Lane[slot], key2Lane[slot], key3Lane[slot]));
            }
        }
        return size != before;
    }

    /**
     * Returns the number of mappings.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Tests whether the map has no mapping.
     *
     * @return whether the map is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes every mapping and releases the stored references; the lanes keep their capacity. */
    public void clear() {
        Arrays.fill(key1Lane, 0, size, null);
        Arrays.fill(key2Lane, 0, size, null);
        Arrays.fill(key3Lane, 0, size, null);
        Arrays.fill(valueLane, 0, size, null);
        Arrays.fill(hashIndexLane, 0);
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    private V value(int slot) {
        return (V) valueLane[slot];
    }

    private static int hash(Object key1, Object key2, Object key3) {
        int hash = Objects.hashCode(key1) * GOLDEN_RATIO_32 + Objects.hashCode(key2);
        hash = hash * GOLDEN_RATIO_32 + (key3 == ABSENT ? 1 : Objects.hashCode(key3));
        return hash ^ hash >>> FOLD_SHIFT;
    }

    private int home(int slot) {
        return hash(key1Lane[slot], key2Lane[slot], key3Lane[slot]) & hashIndexLane.length - 1;
    }

    /** Returns the position in the hash lane of the slot that holds these keys, or -1. */
    private int find(Object key1, Object key2, Object key3) {
        int mask = hashIndexLane.length - 1;
        if (mask < 0) {
            return -1;
        }
        for (int at = hash(key1, key2, key3) & mask; hashIndexLane[at] != 0; at = at + 1 & mask) {
            int slot = hashIndexLane[at] - 1;
            if (Objects.equals(key1, key1Lane[slot]) && Objects.equals(key2, key2Lane[slot]) && sameThird(key3, slot)) {
                return at;
            }
        }
        return -1;
    }

    private boolean sameThird(Object key3, int slot) {
        Object stored = key3Lane[slot];
        return key3 == ABSENT || stored == ABSENT ? key3 == stored : Objects.equals(key3, stored);
    }

    private V store(Object key1, Object key2, Object key3, Object value) {
        int at = find(key1, key2, key3);
        if (at >= 0) {
            int slot = hashIndexLane[at] - 1;
            V old = value(slot);
            valueLane[slot] = value;
            return old;
        }
        reserve();
        key1Lane[size] = key1;
        key2Lane[size] = key2;
        key3Lane[size] = key3;
        valueLane[size] = value;
        stateMaskWords[size >>> WORD_SHIFT] |= STATE_OCCUPIED << size;
        index(size++);
        return null;
    }

    /** Enters a slot whose keys are absent from the hash lane into the first free position of its probe run. */
    private void index(int slot) {
        int mask = hashIndexLane.length - 1;
        int at = home(slot);
        while (hashIndexLane[at] != 0) {
            at = at + 1 & mask;
        }
        hashIndexLane[at] = slot + 1;
    }

    /** Makes room for one more slot: the slot lanes grow by half, the hash lane doubles and is rebuilt. */
    private void reserve() {
        if (size == MAX_SLOTS) {
            throw new OutOfMemoryError("MultiKeyMap: more than 2^29 mappings");
        }
        if (size == valueLane.length) {
            int slots = Math.min(MAX_SLOTS, Math.max(MIN_SLOTS, size + (size >> 1)));
            key1Lane = Arrays.copyOf(key1Lane, slots);
            key2Lane = Arrays.copyOf(key2Lane, slots);
            key3Lane = Arrays.copyOf(key3Lane, slots);
            valueLane = Arrays.copyOf(valueLane, slots);
            stateMaskWords = Arrays.copyOf(stateMaskWords, slots + (1 << WORD_SHIFT) - 1 >>> WORD_SHIFT);
        }
        if (2 * (size + 1) > hashIndexLane.length) {
            hashIndexLane = new int[Math.max(2 * MIN_SLOTS, 2 * hashIndexLane.length)];
            for (int slot = 0; slot < size; slot++) {
                index(slot);
            }
        }
    }

    /** Removes the mapping at a hash lane position: closes the probe gap, then keeps the slots dense. */
    private void removeAt(int at) {
        int slot = hashIndexLane[at] - 1;
        closeGap(at);
        int last = --size;
        if (slot != last) {
            hashIndexLane[find(key1Lane[last], key2Lane[last], key3Lane[last])] = slot + 1;
            key1Lane[slot] = key1Lane[last];
            key2Lane[slot] = key2Lane[last];
            key3Lane[slot] = key3Lane[last];
            valueLane[slot] = valueLane[last];
        }
        key1Lane[last] = null;
        key2Lane[last] = null;
        key3Lane[last] = null;
        valueLane[last] = null;
        stateMaskWords[last >>> WORD_SHIFT] &= ~(STATE_OCCUPIED << last);
    }

    /** Empties a hash lane position and moves the later entries of its probe run back so that runs stay unbroken. */
    private void closeGap(int at) {
        int mask = hashIndexLane.length - 1;
        int gap = at;
        for (int next = gap + 1 & mask; hashIndexLane[next] != 0; next = next + 1 & mask) {
            if ((next - home(hashIndexLane[next] - 1) & mask) >= (next - gap & mask)) {
                hashIndexLane[gap] = hashIndexLane[next];
                gap = next;
            }
        }
        hashIndexLane[gap] = 0;
    }
}
