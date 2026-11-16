/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Map with write hooks: every key and every value passes through its function on the way in and is stored in two
 * parallel array lanes, addressed by an open hash of the stored key.
 *
 * <p>The hooks are the map side of the decorator pattern owned by {@link TransformedCollectionView}: a transforming
 * hook returns what to store (Commons {@code TransformedMap}), a validating hook returns its argument or throws
 * (Commons {@code PredicatedMap}), a guarding hook always throws. A missing hook stores its argument as given. The
 * hooks run before any state changes, and {@link #putAll} runs them on every entry before the first one is stored, so
 * a hook that throws leaves the map as it was. Lookups and {@link #remove} use the key as given, without the hook.
 * A mapping lives in one slot of the key and value lanes; collisions probe linearly and a removal closes its gap, so
 * the lanes hold no dead entries and no node or entry object. Null keys and null values are allowed when the hooks
 * allow them.</p>
 *
 * <p>Provider comparison: Apache Commons Collections {@code TransformedMap} over a {@code HashMap} (Apache-2.0): the
 * same results for the operations declared here. Not thread safe. Not a {@code java.util} replacement.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class TransformedMapView<K, V> {
    /** State-mask bit {@code OCCUPIED} (per-slot state bit). */
    static final long STATE_OCCUPIED = 1L << 0;
    private static final int MIN_SLOTS = 8;
    private static final int MAX_SLOTS = 1 << 30;
    private static final int WORD_SHIFT = 6;
    private static final int FOLD_SHIFT = 16;

    /** Storage lane {@code keyLane}: the stored key of each occupied slot; the length is a power of two or zero. */
    private Object[] keyLane = new Object[0];
    /** Storage lane {@code valueLane}: the stored value of each occupied slot, parallel to {@code keyLane}. */
    private Object[] valueLane = new Object[0];
    /** One OCCUPIED bit per slot, 64 slots per word; at most half of the slots are occupied. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;
    private final Function<? super K, ? extends K> keyHook;
    private final Function<? super V, ? extends V> valueHook;

    /** Creates an empty map that stores every key and value as given. */
    public TransformedMapView() {
        this(null, null);
    }

    /**
     * Creates an empty map that stores {@code keyHook.apply(key)} and {@code valueHook.apply(value)} for every
     * mapping put into it.
     *
     * @param keyHook   the write hook of keys, or null to store keys as given
     * @param valueHook the write hook of values, or null to store values as given
     */
    public TransformedMapView(Function<? super K, ? extends K> keyHook, Function<? super V, ? extends V> valueHook) {
        this.keyHook = keyHook;
        this.valueHook = valueHook;
    }

    /**
     * Reports the declared state-mask bits and lanes for ledgers; never allocates per element.
     *
     * @return the descriptor
     */
    public static String scaffoldDescriptor() {
        return "TransformedMapView|TRANSFORMED_MAP|OBJECT|OCCUPIED|Object[] keyLane;Object[] valueLane";
    }

    /**
     * Maps the key hook's result for {@code key} to the value hook's result for {@code value}.
     *
     * @param key   the key to pass through the key hook
     * @param value the value to pass through the value hook
     * @return the value the stored key was mapped to before, or null
     */
    public V put(K key, V value) {
        Object storedKey = keyHook == null ? key : keyHook.apply(key);
        Object storedValue = valueHook == null ? value : valueHook.apply(value);
        return store(storedKey, storedValue);
    }

    /**
     * Puts all {@code entries}, in their iteration order. The hooks see every entry before the first one is stored.
     *
     * @param entries the mappings to pass through the hooks
     * @throws NullPointerException if {@code entries} is null
     */
    public void putAll(Map<? extends K, ? extends V> entries) {
        Object[] keys = new Object[entries.size()];
        Object[] values = new Object[keys.length];
        int count = 0;
        for (Map.Entry<? extends K, ? extends V> entry : entries.entrySet()) {
            if (count == keys.length) {
                keys = Arrays.copyOf(keys, 2 * count + 1);
                values = Arrays.copyOf(values, keys.length);
            }
            keys[count] = keyHook == null ? entry.getKey() : keyHook.apply(entry.getKey());
            values[count++] = valueHook == null ? entry.getValue() : valueHook.apply(entry.getValue());
        }
        for (int i = 0; i < count; i++) {
            store(keys[i], values[i]);
        }
    }

    /**
     * Returns the value {@code key} is mapped to; the key hook is not applied.
     *
     * @param key the stored key to look up
     * @return the value, or null when there is no mapping or the mapping holds null
     */
    public V get(Object key) {
        int slot = find(key);
        return slot < 0 ? null : value(slot);
    }

    /**
     * Removes the mapping of {@code key}; the key hook is not applied.
     *
     * @param key the stored key to remove
     * @return the value the key was mapped to, or null
     */
    public V remove(Object key) {
        int slot = find(key);
        if (slot < 0) {
            return null;
        }
        V old = value(slot);
        closeGap(slot);
        size--;
        return old;
    }

    /**
     * Returns the number of mappings.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private V value(int slot) {
        return (V) valueLane[slot];
    }

    private static int home(Object key, int mask) {
        int hash = key == null ? 0 : key.hashCode();
        return (hash ^ hash >>> FOLD_SHIFT) & mask;
    }

    private boolean occupied(int slot) {
        return (stateMaskWords[slot >>> WORD_SHIFT] >>> slot & STATE_OCCUPIED) != 0;
    }

    /** Returns the slot of {@code key}, or -1. */
    private int find(Object key) {
        int mask = keyLane.length - 1;
        if (mask < 0) {
            return -1;
        }
        for (int slot = home(key, mask); occupied(slot); slot = slot + 1 & mask) {
            if (Objects.equals(key, keyLane[slot])) {
                return slot;
            }
        }
        return -1;
    }

    private V store(Object key, Object value) {
        int slot = find(key);
        if (slot >= 0) {
            V old = value(slot);
            valueLane[slot] = value;
            return old;
        }
        if (2 * (size + 1) > keyLane.length) {
            grow();
        }
        place(key, value);
        size++;
        return null;
    }

    /** Writes a mapping whose key is absent into the first free slot of its probe run. */
    private void place(Object key, Object value) {
        int mask = keyLane.length - 1;
        int slot = home(key, mask);
        while (occupied(slot)) {
            slot = slot + 1 & mask;
        }
        keyLane[slot] = key;
        valueLane[slot] = value;
        stateMaskWords[slot >>> WORD_SHIFT] |= STATE_OCCUPIED << slot;
    }

    private void grow() {
        if (keyLane.length == MAX_SLOTS) {
            throw new OutOfMemoryError("TransformedMapView: more than 2^29 mappings");
        }
        Object[] keys = keyLane;
        Object[] values = valueLane;
        long[] states = stateMaskWords;
        int slots = Math.max(MIN_SLOTS, 2 * keys.length);
        keyLane = new Object[slots];
        valueLane = new Object[slots];
        stateMaskWords = new long[slots + (1 << WORD_SHIFT) - 1 >>> WORD_SHIFT];
        for (int slot = 0; slot < keys.length; slot++) {
            if ((states[slot >>> WORD_SHIFT] >>> slot & STATE_OCCUPIED) != 0) {
                place(keys[slot], values[slot]);
            }
        }
    }

    /** Empties {@code slot} and moves the later mappings of its probe run back so that every run stays unbroken. */
    private void closeGap(int slot) {
        int mask = keyLane.length - 1;
        int gap = slot;
        for (int next = gap + 1 & mask; occupied(next); next = next + 1 & mask) {
            if ((next - home(keyLane[next], mask) & mask) >= (next - gap & mask)) {
                keyLane[gap] = keyLane[next];
                valueLane[gap] = valueLane[next];
                gap = next;
            }
        }
        keyLane[gap] = null;
        valueLane[gap] = null;
        stateMaskWords[gap >>> WORD_SHIFT] &= ~(STATE_OCCUPIED << gap);
    }
}
