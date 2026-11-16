/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * One-to-one map between the constants of two enum types, stored in two ordinal-indexed int lanes.
 *
 * <p>The key lane holds, per key ordinal, the ordinal of its value; the value lane holds, per value ordinal, the
 * ordinal of its key; -1 marks an unmapped ordinal. A state word per key ordinal carries the {@code PRESENT} bit.
 * Every operation is O(1) except {@link #clear()}, which is linear in the two enum types. No node or entry object is
 * stored, and a lookup allocates nothing. Null keys and null values are refused with {@code NullPointerException}.</p>
 *
 * <p>Provider comparison: Guava {@code EnumBiMap} (Apache-2.0): the same results and the same refusals for the
 * operations declared here, where {@link #inverseGet} stands for {@code inverse().get}. A map made without types
 * learns its two enum types from its first mapping. Guava {@code EnumHashBiMap}, whose values are of any type, is not
 * covered: both lanes are indexed by ordinal. Not thread safe. Not a {@code java.util} replacement.</p>
 */
public final class EnumBiMap<K extends Enum<K>, V extends Enum<V>> {
    /** State-mask bit {@code PRESENT} (per-slot state word, bit 0). */
    static final long STATE_PRESENT = 1L << 0;
    private static final int NONE = -1;

    /** Storage lane {@code keyToValueOrdinalLane}: per key ordinal, the ordinal of its value, or -1. */
    private int[] keyToValueOrdinalLane = new int[0];
    /** Storage lane {@code valueToKeyOrdinalLane}: per value ordinal, the ordinal of its key, or -1. */
    private int[] valueToKeyOrdinalLane = new int[0];
    /** Per-slot state words (PRESENT); one word per key ordinal. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;
    private Class<K> keyType;
    private Class<V> valueType;
    private K[] keyUniverse;
    private V[] valueUniverse;

    /** Creates an empty map that learns its key type and its value type from its first mapping. */
    public EnumBiMap() {
    }

    /**
     * Creates an empty map between two enum types.
     *
     * @param keyType the enum type of the keys
     * @param valueType the enum type of the values
     */
    public EnumBiMap(Class<K> keyType, Class<V> valueType) {
        bind(Objects.requireNonNull(keyType), Objects.requireNonNull(valueType));
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "EnumBiMap|ENUM_BIMAP|OBJECT|PRESENT|int[] keyToValueOrdinalLane;int[] valueToKeyOrdinalLane";
    }

    /**
     * Maps a key to a value. Returns the value the key had before, or null; returns the value itself when the key
     * already has it. A value that belongs to another key is refused with {@code IllegalArgumentException} and the
     * map is unchanged.
     */
    public V put(K key, V value) {
        return store(key, value, false);
    }

    /** Like {@link #put}, but a value that belongs to another key is taken from that key, which is removed. */
    public V forcePut(K key, V value) {
        return store(key, value, true);
    }

    /** The value of a key, or null when the key is not mapped or is not a constant of the key type. */
    public V get(Object key) {
        int slot = slotOf(key, keyType);
        return slot == NONE ? null : valueAt(slot);
    }

    /** The key of a value, or null when the value is not mapped or is not a constant of the value type. */
    public K inverseGet(Object value) {
        int slot = slotOf(value, valueType);
        return slot == NONE || valueToKeyOrdinalLane[slot] == NONE ? null : keyUniverse[valueToKeyOrdinalLane[slot]];
    }

    /** Removes the mapping of a key and returns its value, or null when there was none. */
    public V remove(Object key) {
        int slot = slotOf(key, keyType);
        V old = slot == NONE ? null : valueAt(slot);
        if (old != null) {
            unlink(slot);
        }
        return old;
    }

    /** Tells whether a key is mapped. */
    public boolean containsKey(Object key) {
        int slot = slotOf(key, keyType);
        return slot != NONE && present(slot);
    }

    /** Tells whether a value is mapped. */
    public boolean containsValue(Object value) {
        int slot = slotOf(value, valueType);
        return slot != NONE && valueToKeyOrdinalLane[slot] != NONE;
    }

    /** The number of mappings. */
    public int size() {
        return size;
    }

    /** Removes every mapping. The enum types stay. */
    public void clear() {
        Arrays.fill(keyToValueOrdinalLane, NONE);
        Arrays.fill(valueToKeyOrdinalLane, NONE);
        Arrays.fill(stateMaskWords, 0L);
        size = 0;
    }

    private V store(K key, V value, boolean force) {
        bindOnFirstMapping(key, value);
        int keySlot = slotOrRefuse(key, keyType);
        int valueSlot = slotOrRefuse(value, valueType);
        V old = valueAt(keySlot);
        if (old == value) {
            return value;
        }
        releaseHolder(valueSlot, value, force);
        if (old == null) {
            size++;
        } else {
            valueToKeyOrdinalLane[old.ordinal()] = NONE;
        }
        link(keySlot, valueSlot);
        return old;
    }

    private void releaseHolder(int valueSlot, V value, boolean force) {
        int holder = valueToKeyOrdinalLane[valueSlot];
        if (holder != NONE && !force) {
            throw new IllegalArgumentException("value already present: " + value);
        }
        if (holder != NONE) {
            unlink(holder);
        }
    }

    private void link(int keySlot, int valueSlot) {
        keyToValueOrdinalLane[keySlot] = valueSlot;
        valueToKeyOrdinalLane[valueSlot] = keySlot;
        stateMaskWords[keySlot] = STATE_PRESENT;
    }

    private void unlink(int keySlot) {
        valueToKeyOrdinalLane[keyToValueOrdinalLane[keySlot]] = NONE;
        keyToValueOrdinalLane[keySlot] = NONE;
        stateMaskWords[keySlot] = 0L;
        size--;
    }

    private boolean present(int keySlot) {
        return (stateMaskWords[keySlot] & STATE_PRESENT) != 0L;
    }

    private V valueAt(int keySlot) {
        return present(keySlot) ? valueUniverse[keyToValueOrdinalLane[keySlot]] : null;
    }

    private void bindOnFirstMapping(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (keyType == null) {
            bind(key.getDeclaringClass(), value.getDeclaringClass());
        }
    }

    private void bind(Class<K> keys, Class<V> values) {
        keyUniverse = constants(keys);
        valueUniverse = constants(values);
        keyType = keys;
        valueType = values;
        keyToValueOrdinalLane = unmapped(keyUniverse.length);
        valueToKeyOrdinalLane = unmapped(valueUniverse.length);
        stateMaskWords = new long[keyUniverse.length];
    }

    private static <E extends Enum<E>> E[] constants(Class<E> type) {
        E[] constants = type.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("not an enum type: " + type);
        }
        return constants;
    }

    private static int[] unmapped(int length) {
        int[] lane = new int[length];
        Arrays.fill(lane, NONE);
        return lane;
    }

    private static int slotOf(Object candidate, Class<?> type) {
        return candidate instanceof Enum<?> constant && constant.getDeclaringClass() == type ? constant.ordinal() : NONE;
    }

    private static int slotOrRefuse(Enum<?> constant, Class<?> type) {
        if (constant.getDeclaringClass() != type) {
            throw new ClassCastException(constant.getDeclaringClass() + " != " + type);
        }
        return constant.ordinal();
    }
}
