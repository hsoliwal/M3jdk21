/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Guava 33.4.8-jre com.google.common.collect.EnumHashBiMap.
 * Guava supplies only the BiMap API interface (Apache-2.0); no Guava implementation class, helper,
 * interface default or body is called or copied. The value index reuses the backward-shift lanes of the
 * first-party M3HashMultiset (#1418).
 */
package com.m3.collections.guava;

import com.google.common.collect.BiMap;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Owned enum-keyed bimap over ordinal-indexed lanes and a value index; no per-entry objects.
 *
 * <p>Forward lanes are indexed by key ordinal: {@code Object[] vals} holds each key's value and
 * {@code long[] present} holds one bit per ordinal (null values are legal, so presence is a bit, not a
 * sentinel). The inverse direction is a power-of-two {@code int[] index} (probe position to ordinal+1,
 * {@code length - 1} mask, backward-shift deletion) over the {@code int[] valueHashes} lane. Forward
 * iteration is ordinal order and weakly consistent, as EnumMap is; inverse iteration is fail-fast on
 * {@code valueMod}, which changes exactly when the value set changes, as Guava's backing HashMap does.
 *
 * <p>{@link #inverse()} is a flyweight over the same lanes. Entry objects exist only where a view's
 * iterator returns them; internal traversal ({@code forEach}, {@code equals}, {@code hashCode},
 * {@code toString}) creates none. Inverse iteration follows key ordinal order; Guava's HashMap order is
 * unspecified.
 *
 * @param <K> enum key type
 * @param <V> value type
 */
public final class M3EnumHashBiMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements BiMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int[] NO_INTS = {};
    /** One shared, never-written universe per enum class; getEnumConstants() clones on every call. */
    private static final ClassValue<Object[]> UNIVERSES = new ClassValue<>() {
        @Override protected Object[] computeValue(Class<?> type) {
            return type.getEnumConstants();
        }
    };

    private transient Class<K> keyType;
    private transient K[] universe;
    private transient Object[] vals;
    private transient int[] valueHashes;
    private transient long[] present;
    private transient int[] index;
    private transient int size;
    private transient int valueMod;
    private transient Inverse inverseView;
    private transient Set<K> keySetView;
    private transient Set<V> valuesView;
    private transient Set<Map.Entry<K, V>> entrySetView;

    private M3EnumHashBiMap(Class<K> keyType) {
        init(keyType);
    }

    /** Creates an empty bimap for the given enum key type. */
    public static <K extends Enum<K>, V> M3EnumHashBiMap<K, V> create(Class<K> keyType) {
        return new M3EnumHashBiMap<>(keyType);
    }

    /** Creates a bimap with the mappings of {@code map}; the key type is inferred as Guava does. */
    public static <K extends Enum<K>, V> M3EnumHashBiMap<K, V> create(Map<K, ? extends V> map) {
        M3EnumHashBiMap<K, V> created = create(inferKeyType(map));
        created.putAll(map);
        return created;
    }

    static <K extends Enum<K>> Class<K> inferKeyType(Map<K, ?> map) {
        if (map instanceof M3EnumHashBiMap<K, ?> m3) { return m3.keyType; }
        if (map instanceof com.google.common.collect.EnumHashBiMap<K, ?> guava) { return guava.keyType(); }
        if (map instanceof com.google.common.collect.EnumBiMap<K, ?> guava) { return guava.keyType(); }
        if (map.isEmpty()) { throw new IllegalArgumentException(); }
        return map.keySet().iterator().next().getDeclaringClass();
    }

    private void init(Class<K> type) {
        keyType = Objects.requireNonNull(type);
        universe = cast(UNIVERSES.get(type));
        vals = new Object[universe.length];
        valueHashes = new int[universe.length];
        present = new long[universe.length + 63 >>> 6];
        index = NO_INTS;
    }

    /** Returns the enum key type. */
    public Class<K> keyType() {
        return keyType;
    }

    // ------------------------------------------------------------------ lanes

    private static int spread(Object value) {
        int h = value == null ? 0 : value.hashCode();
        return h ^ h >>> 16;
    }

    private int ord(Object key) {
        if (key == null) { return -1; }
        Class<?> c = key.getClass();
        return c == keyType || c.getSuperclass() == keyType ? ((Enum<?>) key).ordinal() : -1;
    }

    private boolean has(int o) {
        return (present[o >>> 6] & 1L << o) != 0;
    }

    private int nextPresent(int from) {
        for (int o = from; o < universe.length; o++) {
            if (has(o)) { return o; }
        }
        return -1;
    }

    int findValue(Object value) {
        int[] table = index;
        if (table.length == 0) { return -1; }
        int h = spread(value);
        int mask = table.length - 1;
        for (int p = h & mask; table[p] != 0; p = p + 1 & mask) {
            int o = table[p] - 1;
            if (valueHashes[o] == h && Objects.equals(value, vals[o])) { return o; }
        }
        return -1;
    }

    private int positionOf(int o) {
        int mask = index.length - 1;
        int p = valueHashes[o] & mask;
        while (index[p] != o + 1) { p = p + 1 & mask; }
        return p;
    }

    private void linkValue(int o) {
        if (index.length == 0 || (long) (size + 1) * 3 > (long) index.length * 2) { rehash(tableFor(Math.max(size + 1, 4))); }
        valueHashes[o] = spread(vals[o]);
        int mask = index.length - 1;
        int p = valueHashes[o] & mask;
        while (index[p] != 0) { p = p + 1 & mask; }
        index[p] = o + 1;
        valueMod++;
    }

    static int tableFor(int entries) {
        long needed = (long) entries * 3 / 2 + 1;
        return Integer.highestOneBit((int) needed - 1) << 1;
    }

    private void rehash(int capacity) {
        int[] table = new int[capacity];
        int mask = capacity - 1;
        for (int o = 0; o < universe.length; o++) {
            if (has(o) && index.length > 0 && isLinked(o)) {
                int p = valueHashes[o] & mask;
                while (table[p] != 0) { p = p + 1 & mask; }
                table[p] = o + 1;
            }
        }
        index = table;
    }

    private boolean isLinked(int o) {
        int mask = index.length - 1;
        for (int p = valueHashes[o] & mask; index[p] != 0; p = p + 1 & mask) {
            if (index[p] == o + 1) { return true; }
        }
        return false;
    }

    private void unlinkValue(int o) {
        int mask = index.length - 1;
        int gap = positionOf(o);
        for (int q = gap + 1 & mask; index[q] != 0; q = q + 1 & mask) {
            int home = valueHashes[index[q] - 1] & mask;
            if ((q - home & mask) >= (q - gap & mask)) {
                index[gap] = index[q];
                gap = q;
            }
        }
        index[gap] = 0;
        valueMod++;
    }

    /** Moves a linked value from ordinal {@code from} to {@code to} without changing the value set. */
    private void moveValue(int from, int to) {
        index[positionOf(from)] = to + 1;
        valueHashes[to] = valueHashes[from];
        vals[to] = vals[from];
        vals[from] = null;
        present[from >>> 6] &= ~(1L << from);
        present[to >>> 6] |= 1L << to;
    }

    private void setForward(int o, Object value) {
        vals[o] = value;
        if (!has(o)) {
            present[o >>> 6] |= 1L << o;
            size++;
        }
    }

    private void clearForward(int o) {
        vals[o] = null;
        present[o >>> 6] &= ~(1L << o);
        size--;
    }

    private ClassCastException wrongKey(Object key) {
        return new ClassCastException(key.getClass() + " != " + keyType);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // ------------------------------------------------------------------ forward Map/BiMap API

    @Override public int size() {
        return size;
    }

    @Override public boolean isEmpty() {
        return size == 0;
    }

    @Override public boolean containsKey(Object key) {
        int o = ord(key);
        return o >= 0 && has(o);
    }

    @Override public V get(Object key) {
        int o = ord(key);
        return o >= 0 && has(o) ? cast(vals[o]) : null;
    }

    @Override public boolean containsValue(Object value) {
        return findValue(value) >= 0;
    }

    @Override public V put(K key, V value) {
        return putForward(key, value, false);
    }

    @Override public V forcePut(K key, V value) {
        return putForward(key, value, true);
    }

    private V putForward(K key, V value, boolean force) {
        Objects.requireNonNull(key);
        int o = ord(key);
        boolean contained = o >= 0 && has(o);
        if (contained && Objects.equals(value, vals[o])) { return value; }
        if (force) {
            removeValue(value);
        } else if (findValue(value) >= 0) {
            throw new IllegalArgumentException("value already present: " + value);
        }
        if (o < 0) { throw wrongKey(key); }
        V old = null;
        if (contained) {
            old = cast(vals[o]);
            unlinkValue(o);
        }
        setForward(o, value);
        linkValue(o);
        return old;
    }

    /** Removes the mapping whose value is {@code value}; returns its key or null. */
    private K removeValue(Object value) {
        int o = findValue(value);
        if (o < 0) { return null; }
        unlinkValue(o);
        clearForward(o);
        return universe[o];
    }

    @Override public V remove(Object key) {
        int o = ord(key);
        if (o < 0 || !has(o)) { return null; }
        V old = cast(vals[o]);
        unlinkValue(o);
        clearForward(o);
        return old;
    }

    @Override public void clear() {
        Arrays.fill(vals, null);
        Arrays.fill(present, 0L);
        Arrays.fill(index, 0);
        size = 0;
        valueMod++;
    }

    /**
     * Guava's sequence: replace every value in key order, then rebuild the value index in key order and
     * drop each later key whose new value is already taken; a drop fails as Guava's removed EnumMap entry does.
     */
    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        Object[] replaced = vals.clone();
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) { replaced[o] = function.apply(universe[o], cast(vals[o])); }
        vals = replaced;
        Arrays.fill(index, 0);
        valueMod++;
        boolean broken = false;
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) {
            if (findValue(vals[o]) >= 0) {
                clearForward(o);
                broken = true;
            } else {
                linkValue(o);
            }
        }
        if (broken) { throw new IllegalStateException("Entry was removed"); }
    }

    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) { action.accept(universe[o], cast(vals[o])); }
    }

    @Override public BiMap<V, K> inverse() {
        Inverse v = inverseView;
        return v != null ? v : (inverseView = new Inverse());
    }

    @Override public Set<K> keySet() {
        Set<K> v = keySetView;
        return v != null ? v : (keySetView = new ForwardKeys());
    }

    @Override public Set<V> values() {
        Set<V> v = valuesView;
        return v != null ? v : (valuesView = new ForwardValues());
    }

    @Override public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> v = entrySetView;
        return v != null ? v : (entrySetView = new ForwardEntries());
    }

    /** EnumMap's equality: a value-for-value comparison with no exception guard. */
    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Map<?, ?> m) || m.size() != size) { return false; }
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) {
            Object value = vals[o];
            K key = universe[o];
            if (value == null ? !(m.get(key) == null && m.containsKey(key)) : !value.equals(m.get(key))) { return false; }
        }
        return true;
    }

    @Override public int hashCode() {
        int h = 0;
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) { h += universe[o].hashCode() ^ Objects.hashCode(vals[o]); }
        return h;
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) {
            if (b.length() > 1) { b.append(", "); }
            b.append(universe[o]).append('=').append(vals[o]);
        }
        return b.append('}').toString();
    }

    /** Checks presence bits, size and that the value index holds exactly the present ordinals. */
    void verifyInvariants() {
        int count = 0;
        for (int o = 0; o < universe.length; o++) {
            if (has(o)) {
                count++;
                if (findValue(vals[o]) != o) { throw new AssertionError("value index"); }
            } else if (vals[o] != null) {
                throw new AssertionError("absent slot");
            }
        }
        int linked = 0;
        for (int p = 0; p < index.length; p++) {
            if (index[p] != 0) {
                linked++;
                if (!has(index[p] - 1)) { throw new AssertionError("dangling index"); }
            }
        }
        if (count != size || linked != size) { throw new AssertionError("counts"); }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(keyType);
        out.writeInt(size);
        for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) {
            out.writeObject(universe[o]);
            out.writeObject(vals[o]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Class<?> type = (Class<?>) in.readObject();
        if (type == null || !type.isEnum()) { throw new InvalidObjectException("Invalid key type " + type); }
        init(cast(type));
        int n = in.readInt();
        if (n < 0 || n > universe.length) { throw new InvalidObjectException("Invalid size " + n); }
        for (int i = 0; i < n; i++) {
            Object key = in.readObject();
            if (ord(key) < 0) { throw new InvalidObjectException("Invalid key " + key); }
            put(cast(key), cast(in.readObject()));
        }
    }

    // ------------------------------------------------------------------ forward views (EnumMap semantics)

    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    /** Weakly consistent ordinal cursor, as EnumMap's; removal also drops the value from the inverse. */
    private final class ForwardCursor<T> implements Iterator<T> {
        private final int kind;
        private int next;
        private int last = -1;
        private ForwardEntry lastEntry;

        ForwardCursor(int kind) {
            this.kind = kind;
        }

        @Override public boolean hasNext() {
            while (next < universe.length && !has(next)) { next++; }
            return next != universe.length;
        }

        @Override public T next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            last = next++;
            if (kind == KEYS) { return cast(universe[last]); }
            if (kind == VALUES) { return cast(vals[last]); }
            lastEntry = new ForwardEntry(last);
            return cast(lastEntry);
        }

        @Override public void remove() {
            if (last < 0) { throw new IllegalStateException("no calls to next() since the last call to remove()"); }
            if (has(last)) {
                unlinkValue(last);
                clearForward(last);
            }
            if (lastEntry != null) { lastEntry.removed = true; }
            last = -1;
            lastEntry = null;
        }
    }

    /** Boundary flyweight for one forward entry: live, ordinal-bound, unusable after iterator removal. */
    private final class ForwardEntry implements Map.Entry<K, V> {
        private final int o;
        private boolean removed;

        ForwardEntry(int o) {
            this.o = o;
        }

        private void check() {
            if (removed) { throw new IllegalStateException("Entry was removed"); }
        }

        @Override public K getKey() {
            check();
            return universe[o];
        }

        @Override public V getValue() {
            check();
            return cast(vals[o]);
        }

        @Override public V setValue(V value) {
            check();
            if (!has(o)) { throw new IllegalStateException("entry no longer in map"); }
            if (Objects.equals(value, vals[o])) { return value; }
            if (findValue(value) >= 0) { throw new IllegalArgumentException("value already present: " + value); }
            V old = cast(vals[o]);
            unlinkValue(o);
            vals[o] = value;
            linkValue(o);
            return old;
        }

        @Override public boolean equals(Object other) {
            if (removed) { return other == this; }
            return other instanceof Map.Entry<?, ?> e && e.getKey() == universe[o] && Objects.equals(vals[o], e.getValue());
        }

        @Override public int hashCode() {
            return removed ? System.identityHashCode(this) : universe[o].hashCode() ^ Objects.hashCode(vals[o]);
        }

        @Override public String toString() {
            return removed ? "Entry was removed" : universe[o] + "=" + vals[o];
        }
    }

    private final class ForwardKeys extends AbstractSet<K> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override public boolean remove(Object o) {
            if (!containsKey(o)) { return false; }
            M3EnumHashBiMap.this.remove(o);
            return true;
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        @Override public Iterator<K> iterator() {
            return new ForwardCursor<>(KEYS);
        }
    }

    private final class ForwardValues extends AbstractSet<V> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override public boolean remove(Object o) {
            return removeValue(o) != null;
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        @Override public Iterator<V> iterator() {
            return new ForwardCursor<>(VALUES);
        }
    }

    private final class ForwardEntries extends AbstractSet<Map.Entry<K, V>> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) { return false; }
            int k = ord(e.getKey());
            return k >= 0 && has(k) && Objects.equals(vals[k], e.getValue());
        }

        @Override public boolean remove(Object o) {
            if (!contains(o)) { return false; }
            M3EnumHashBiMap.this.remove(((Map.Entry<?, ?>) o).getKey());
            return true;
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        @Override public Iterator<Map.Entry<K, V>> iterator() {
            return new ForwardCursor<>(ENTRIES);
        }
    }

    // ------------------------------------------------------------------ inverse (Guava's HashMap side)

    /** Fail-fast cursor over the value side: the successor is precomputed, as a hash map iterator does. */
    private final class InverseCursor<T> implements Iterator<T> {
        private final int kind;
        private int nextOrd = nextPresent(0);
        private int last = -1;
        private int expected = valueMod;

        InverseCursor(int kind) {
            this.kind = kind;
        }

        @Override public boolean hasNext() {
            return nextOrd >= 0;
        }

        @Override public T next() {
            if (valueMod != expected) { throw new ConcurrentModificationException(); }
            if (nextOrd < 0) { throw new NoSuchElementException(); }
            last = nextOrd;
            nextOrd = nextPresent(last + 1);
            if (kind == KEYS) { return cast(vals[last]); }
            if (kind == VALUES) { return cast(universe[last]); }
            return cast(new InverseEntry(vals[last], universe[last]));
        }

        @Override public void remove() {
            if (last < 0) { throw new IllegalStateException("no calls to next() since the last call to remove()"); }
            if (valueMod != expected) { throw new ConcurrentModificationException(); }
            unlinkValue(last);
            clearForward(last);
            expected = valueMod;
            last = -1;
        }
    }

    /** Boundary flyweight for one inverse entry; like a hash map node it keeps its last key after removal. */
    private final class InverseEntry implements Map.Entry<V, K> {
        private final Object value;
        private K key;

        InverseEntry(Object value, K key) {
            this.value = value;
            this.key = key;
        }

        @Override public V getKey() {
            return cast(value);
        }

        @Override public K getValue() {
            int o = findValue(value);
            if (o >= 0) { key = universe[o]; }
            return key;
        }

        @Override public K setValue(K newKey) {
            Objects.requireNonNull(newKey);
            int o = findValue(value);
            if (o < 0) { throw new IllegalStateException("entry no longer in map"); }
            if (newKey == universe[o]) { return newKey; }
            if (containsKey(newKey)) { throw new IllegalArgumentException("value already present: " + newKey); }
            int target = ord(newKey);
            if (target < 0) { throw wrongKey(newKey); }
            K old = universe[o];
            moveValue(o, target);
            key = newKey;
            return old;
        }

        @Override public boolean equals(Object other) {
            return other instanceof Map.Entry<?, ?> e && Objects.equals(value, e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        @Override public int hashCode() {
            return Objects.hashCode(value) ^ Objects.hashCode(getValue());
        }

        @Override public String toString() {
            return value + "=" + getValue();
        }
    }

    /** The inverse view {@code V -> K}; a flyweight over the forward lanes. */
    private final class Inverse extends AbstractMap<V, K> implements BiMap<V, K>, Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private transient Set<V> keys;
        private transient Set<K> values;
        private transient Set<Map.Entry<V, K>> entries;

        @Override public int size() {
            return size;
        }

        @Override public boolean isEmpty() {
            return size == 0;
        }

        @Override public boolean containsKey(Object value) {
            return findValue(value) >= 0;
        }

        @Override public K get(Object value) {
            int o = findValue(value);
            return o < 0 ? null : universe[o];
        }

        @Override public boolean containsValue(Object key) {
            return M3EnumHashBiMap.this.containsKey(key);
        }

        @Override public K put(V value, K key) {
            return putInverse(value, key, false);
        }

        @Override public K forcePut(V value, K key) {
            return putInverse(value, key, true);
        }

        private K putInverse(V value, K key, boolean force) {
            Objects.requireNonNull(key);
            int existing = findValue(value);
            if (existing >= 0 && universe[existing] == key) { return key; }
            if (force) {
                M3EnumHashBiMap.this.remove(key);
            } else if (M3EnumHashBiMap.this.containsKey(key)) {
                throw new IllegalArgumentException("value already present: " + key);
            }
            int target = ord(key);
            if (target < 0) { throw wrongKey(key); }
            if (existing >= 0) {
                K old = universe[existing];
                moveValue(existing, target);
                return old;
            }
            setForward(target, value);
            linkValue(target);
            return null;
        }

        @Override public K remove(Object value) {
            return removeValue(value);
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        /** Applies the function per value, then rebinds in key order; later conflicting values are dropped. */
        @Override public void replaceAll(BiFunction<? super V, ? super K, ? extends K> function) {
            Objects.requireNonNull(function);
            Object[] newKeys = new Object[universe.length];
            for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) {
                K k = function.apply(cast(vals[o]), universe[o]);
                if (ord(Objects.requireNonNull(k)) < 0) { throw wrongKey(k); }
                newKeys[o] = k;
            }
            Object[] oldVals = vals.clone();
            long[] oldPresent = present.clone();
            Arrays.fill(vals, null);
            Arrays.fill(present, 0L);
            Arrays.fill(index, 0);
            size = 0;
            Object broken = null;
            for (int o = 0; o < universe.length; o++) {
                if ((oldPresent[o >>> 6] & 1L << o) == 0) { continue; }
                int target = ord(newKeys[o]);
                if (has(target)) {
                    broken = newKeys[o];
                    valueMod++;
                } else {
                    setForward(target, oldVals[o]);
                    linkValue(target);
                    valueMod--;
                }
            }
            if (broken != null) { throw new IllegalArgumentException("value already present: " + broken); }
        }

        @Override public void forEach(BiConsumer<? super V, ? super K> action) {
            Objects.requireNonNull(action);
            int expected = valueMod;
            for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) {
                if (valueMod != expected) { throw new ConcurrentModificationException(); }
                action.accept(cast(vals[o]), universe[o]);
            }
            if (valueMod != expected) { throw new ConcurrentModificationException(); }
        }

        @Override public BiMap<K, V> inverse() {
            return M3EnumHashBiMap.this;
        }

        @Override public Set<V> keySet() {
            Set<V> v = keys;
            return v != null ? v : (keys = new InverseKeys());
        }

        @Override public Set<K> values() {
            Set<K> v = values;
            return v != null ? v : (values = new InverseValues());
        }

        @Override public Set<Map.Entry<V, K>> entrySet() {
            Set<Map.Entry<V, K>> v = entries;
            return v != null ? v : (entries = new InverseEntries());
        }

        @Override public int hashCode() {
            int h = 0;
            for (int o = nextPresent(0); o >= 0; o = nextPresent(o + 1)) { h += Objects.hashCode(vals[o]) ^ universe[o].hashCode(); }
            return h;
        }

        @Serial
        private Object writeReplace() throws ObjectStreamException {
            return new InverseForm(M3EnumHashBiMap.this);
        }
    }

    /** Serialized form of an inverse view: the forward bimap, resolved back to its inverse. */
    private static final class InverseForm implements Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private final M3EnumHashBiMap<?, ?> forward;

        InverseForm(M3EnumHashBiMap<?, ?> forward) {
            this.forward = forward;
        }

        @Serial
        private Object readResolve() throws ObjectStreamException {
            return forward.inverse();
        }
    }

    private final class InverseKeys extends AbstractSet<V> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return findValue(o) >= 0;
        }

        @Override public boolean remove(Object o) {
            return removeValue(o) != null;
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        @Override public Iterator<V> iterator() {
            return new InverseCursor<>(KEYS);
        }
    }

    private final class InverseValues extends AbstractSet<K> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return M3EnumHashBiMap.this.containsKey(o);
        }

        @Override public boolean remove(Object o) {
            if (!M3EnumHashBiMap.this.containsKey(o)) { return false; }
            M3EnumHashBiMap.this.remove(o);
            return true;
        }

        @Override public boolean removeAll(Collection<?> c) {
            return M3EnumHashBiMap.this.keySet().removeAll(c);
        }

        @Override public boolean retainAll(Collection<?> c) {
            return M3EnumHashBiMap.this.keySet().retainAll(c);
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        @Override public Iterator<K> iterator() {
            return new InverseCursor<>(VALUES);
        }
    }

    private final class InverseEntries extends AbstractSet<Map.Entry<V, K>> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) { return false; }
            int k = findValue(e.getKey());
            return k >= 0 && Objects.equals(universe[k], e.getValue());
        }

        @Override public boolean remove(Object o) {
            if (!contains(o)) { return false; }
            removeValue(((Map.Entry<?, ?>) o).getKey());
            return true;
        }

        @Override public void clear() {
            M3EnumHashBiMap.this.clear();
        }

        @Override public Iterator<Map.Entry<V, K>> iterator() {
            return new InverseCursor<>(ENTRIES);
        }
    }
}
