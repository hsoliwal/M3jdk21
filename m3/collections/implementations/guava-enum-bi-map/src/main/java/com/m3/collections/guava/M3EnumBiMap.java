/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Guava 33.4.8-jre com.google.common.collect.EnumBiMap.
 * Guava supplies only the BiMap API interface (Apache-2.0); no Guava implementation class, helper,
 * interface default or body is called or copied. The side-symmetric ordinal lanes follow the owned
 * M3EnumHashBiMap (#1460).
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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Owned enum-to-enum bimap over two ordinal lanes; no per-entry objects and no hashing.
 *
 * <p>Two {@code int[]} lanes hold the whole state: {@code lanes[0][k]} is the value ordinal + 1 of key
 * ordinal {@code k} and {@code lanes[1][v]} is the key ordinal + 1 of value ordinal {@code v}; 0 marks
 * absence, so each lane is its own presence mask. Every operation is side-symmetric: side 0 is this
 * map and side 1 is {@link #inverse()}, a flyweight over the same lanes. Both directions iterate in
 * ordinal order and are weakly consistent, as Guava's two backing EnumMaps are. Entry objects exist only
 * where a view's iterator returns them; {@code forEach}, {@code equals}, {@code hashCode} and
 * {@code toString} create none.
 *
 * @param <K> enum key type
 * @param <V> enum value type
 */
public final class M3EnumBiMap<K extends Enum<K>, V extends Enum<V>> extends AbstractMap<K, V> implements BiMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;
    /** One shared, never-written universe per enum class; getEnumConstants() clones on every call. */
    private static final ClassValue<Enum<?>[]> UNIVERSES = new ClassValue<>() {
        @Override protected Enum<?>[] computeValue(Class<?> type) {
            return (Enum<?>[]) type.getEnumConstants();
        }
    };

    private transient Class<?>[] types;
    private transient Enum<?>[][] universes;
    private transient int[][] lanes;
    /** Hoisted aliases of {@code types[0]}, {@code lanes[0]} and {@code universes[1]} for the forward get(). */
    private transient Class<?> keyClass;
    private transient int[] keyLane;
    private transient Enum<?>[] valueConstants;
    private transient int size;
    private transient Side<V, K> inverseView;
    private transient Set<K> keySetView;
    private transient Set<V> valuesView;
    private transient Set<Map.Entry<K, V>> entrySetView;

    private M3EnumBiMap(Class<K> keyType, Class<V> valueType) {
        init(keyType, valueType);
    }

    /** Creates an empty bimap for the given key and value enum types. */
    public static <K extends Enum<K>, V extends Enum<V>> M3EnumBiMap<K, V> create(Class<K> keyType, Class<V> valueType) {
        return new M3EnumBiMap<>(keyType, valueType);
    }

    /** Creates a bimap with the mappings of {@code map}; both enum types are inferred as Guava does. */
    public static <K extends Enum<K>, V extends Enum<V>> M3EnumBiMap<K, V> create(Map<K, V> map) {
        M3EnumBiMap<K, V> created = create(inferKeyType(map), inferValueType(map));
        created.putAll(map);
        return created;
    }

    @SuppressWarnings("unchecked")
    static <K extends Enum<K>> Class<K> inferKeyType(Map<K, ?> map) {
        if (map instanceof M3EnumBiMap<?, ?> m3) { return (Class<K>) m3.types[0]; }
        if (map instanceof com.google.common.collect.EnumBiMap<K, ?> guava) { return guava.keyType(); }
        if (map instanceof com.google.common.collect.EnumHashBiMap<K, ?> guava) { return guava.keyType(); }
        if (map.isEmpty()) { throw new IllegalArgumentException(); }
        return map.keySet().iterator().next().getDeclaringClass();
    }

    @SuppressWarnings("unchecked")
    static <V extends Enum<V>> Class<V> inferValueType(Map<?, V> map) {
        if (map instanceof M3EnumBiMap<?, ?> m3) { return (Class<V>) m3.types[1]; }
        if (map instanceof com.google.common.collect.EnumBiMap<?, V> guava) { return guava.valueType(); }
        if (map.isEmpty()) { throw new IllegalArgumentException(); }
        return map.values().iterator().next().getDeclaringClass();
    }

    private void init(Class<?> keyType, Class<?> valueType) {
        types = new Class<?>[] {Objects.requireNonNull(keyType), Objects.requireNonNull(valueType)};
        universes = new Enum<?>[][] {UNIVERSES.get(keyType), UNIVERSES.get(valueType)};
        lanes = new int[][] {new int[universes[0].length], new int[universes[1].length]};
        keyClass = keyType;
        keyLane = lanes[0];
        valueConstants = universes[1];
    }

    /** Returns the key enum type. */
    @SuppressWarnings("unchecked")
    public Class<K> keyType() {
        return (Class<K>) types[0];
    }

    /** Returns the value enum type. */
    @SuppressWarnings("unchecked")
    public Class<V> valueType() {
        return (Class<V>) types[1];
    }

    // ------------------------------------------------------------------ side-symmetric lane operations

    private int ord(int s, Object key) {
        if (key == null) { return -1; }
        Class<?> c = key.getClass();
        return c == types[s] || c.getSuperclass() == types[s] ? ((Enum<?>) key).ordinal() : -1;
    }

    private int nextPresent(int s, int from) {
        int[] lane = lanes[s];
        for (int o = from; o < lane.length; o++) {
            if (lane[o] != 0) { return o; }
        }
        return -1;
    }

    private boolean contains(int s, Object key) {
        int o = ord(s, key);
        return o >= 0 && lanes[s][o] != 0;
    }

    /**
     * get() on one side over that side's hoisted arrays ({@code types[s]}, {@code lanes[s]},
     * {@code universes[1 - s]}): one class check, one lane read, one universe read.
     */
    private static Object lookup(Object key, Class<?> type, int[] lane, Enum<?>[] targets) {
        if (key == null) { return null; }
        Class<?> c = key.getClass();
        if (c != type && c.getSuperclass() != type) { return null; }
        int t = lane[((Enum<?>) key).ordinal()];
        return t == 0 ? null : targets[t - 1];
    }

    private ClassCastException wrong(int s, Object key) {
        return new ClassCastException(key.getClass() + " != " + types[s]);
    }

    private Object put(int s, Object key, Object value, boolean force) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        int o = ord(s, key);
        boolean contained = o >= 0 && lanes[s][o] != 0;
        if (contained && universes[1 - s][lanes[s][o] - 1] == value) { return value; }
        if (force) {
            remove(1 - s, value);
        } else if (contains(1 - s, value)) {
            throw new IllegalArgumentException("value already present: " + value);
        }
        if (o < 0) { throw wrong(s, key); }
        int v = ord(1 - s, value);
        if (v < 0) { throw wrong(1 - s, value); }
        Object old = null;
        if (contained) {
            int previous = lanes[s][o] - 1;
            old = universes[1 - s][previous];
            lanes[1 - s][previous] = 0;
        } else {
            size++;
        }
        lanes[s][o] = v + 1;
        lanes[1 - s][v] = o + 1;
        return old;
    }

    private Object remove(int s, Object key) {
        int o = ord(s, key);
        if (o < 0 || lanes[s][o] == 0) { return null; }
        int v = lanes[s][o] - 1;
        lanes[s][o] = 0;
        lanes[1 - s][v] = 0;
        size--;
        return universes[1 - s][v];
    }

    private void clearAll() {
        Arrays.fill(lanes[0], 0);
        Arrays.fill(lanes[1], 0);
        size = 0;
    }

    /**
     * Guava's sequence on either side: new values in this side's ordinal order (all validated before any
     * change), then the other lane is rebuilt in that order; a later key whose new value is taken is
     * dropped, and a drop fails as Guava's removed EnumMap entry does.
     */
    private void replaceAll(int s, BiFunction<Object, Object, Object> function) {
        Objects.requireNonNull(function);
        int[] lane = lanes[s];
        int[] next = new int[lane.length];
        for (int o = nextPresent(s, 0); o >= 0; o = nextPresent(s, o + 1)) {
            Object value = Objects.requireNonNull(function.apply(universes[s][o], universes[1 - s][lane[o] - 1]));
            int v = ord(1 - s, value);
            if (v < 0) { throw wrong(1 - s, value); }
            next[o] = v + 1;
        }
        Arrays.fill(lanes[1 - s], 0);
        boolean broken = false;
        for (int o = nextPresent(s, 0); o >= 0; o = nextPresent(s, o + 1)) {
            int v = next[o] - 1;
            if (lanes[1 - s][v] != 0) {
                lane[o] = 0;
                size--;
                broken = true;
            } else {
                lane[o] = v + 1;
                lanes[1 - s][v] = o + 1;
            }
        }
        if (broken) { throw new IllegalStateException("Entry was removed"); }
    }

    private void forEach(int s, BiConsumer<Object, Object> action) {
        Objects.requireNonNull(action);
        for (int o = nextPresent(s, 0); o >= 0; o = nextPresent(s, o + 1)) { action.accept(universes[s][o], universes[1 - s][lanes[s][o] - 1]); }
    }

    /** EnumMap's equality: value-for-value with no exception guard; values are never null here. */
    private boolean equalsMap(int s, Object other) {
        if (!(other instanceof Map<?, ?> m) || m.size() != size) { return false; }
        for (int o = nextPresent(s, 0); o >= 0; o = nextPresent(s, o + 1)) {
            if (!universes[1 - s][lanes[s][o] - 1].equals(m.get(universes[s][o]))) { return false; }
        }
        return true;
    }

    private int hash(int s) {
        int h = 0;
        for (int o = nextPresent(s, 0); o >= 0; o = nextPresent(s, o + 1)) { h += universes[s][o].hashCode() ^ universes[1 - s][lanes[s][o] - 1].hashCode(); }
        return h;
    }

    private String text(int s) {
        StringBuilder b = new StringBuilder("{");
        for (int o = nextPresent(s, 0); o >= 0; o = nextPresent(s, o + 1)) {
            if (b.length() > 1) { b.append(", "); }
            b.append(universes[s][o]).append('=').append(universes[1 - s][lanes[s][o] - 1]);
        }
        return b.append('}').toString();
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // ------------------------------------------------------------------ forward Map/BiMap API (side 0)

    @Override public int size() {
        return size;
    }

    @Override public boolean isEmpty() {
        return size == 0;
    }

    @Override public boolean containsKey(Object key) {
        return contains(0, key);
    }

    @Override public boolean containsValue(Object value) {
        return contains(1, value);
    }

    @Override public V get(Object key) {
        return cast(lookup(key, keyClass, keyLane, valueConstants));
    }

    @Override public V put(K key, V value) {
        return cast(put(0, key, value, false));
    }

    @Override public V forcePut(K key, V value) {
        return cast(put(0, key, value, true));
    }

    @Override public V remove(Object key) {
        return cast(remove(0, key));
    }

    @Override public void clear() {
        clearAll();
    }

    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        replaceAll(0, (k, v) -> function.apply(cast(k), cast(v)));
    }

    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        forEach(0, cast(action));
    }

    @Override public BiMap<V, K> inverse() {
        Side<V, K> v = inverseView;
        return v != null ? v : (inverseView = new Side<>(1));
    }

    @Override public Set<K> keySet() {
        Set<K> v = keySetView;
        return v != null ? v : (keySetView = new Keys<>(0));
    }

    @Override public Set<V> values() {
        Set<V> v = valuesView;
        return v != null ? v : (valuesView = new Values<>(0));
    }

    @Override public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> v = entrySetView;
        return v != null ? v : (entrySetView = new Entries<>(0));
    }

    @Override public boolean equals(Object other) {
        return other == this || equalsMap(0, other);
    }

    @Override public int hashCode() {
        return hash(0);
    }

    @Override public String toString() {
        return text(0);
    }

    /** Checks that both lanes mirror each other and agree with the size. */
    void verifyInvariants() {
        int count = 0;
        for (int s = 0; s < 2; s++) {
            int[] lane = lanes[s];
            int present = 0;
            for (int o = 0; o < lane.length; o++) {
                if (lane[o] != 0) {
                    present++;
                    if (lanes[1 - s][lane[o] - 1] != o + 1) { throw new AssertionError("mirror"); }
                }
            }
            if (s == 0) { count = present; }
        }
        if (count != size) { throw new AssertionError("size"); }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(types[0]);
        out.writeObject(types[1]);
        out.writeInt(size);
        for (int o = nextPresent(0, 0); o >= 0; o = nextPresent(0, o + 1)) {
            out.writeObject(universes[0][o]);
            out.writeObject(universes[1][lanes[0][o] - 1]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Class<?> keyType = (Class<?>) in.readObject();
        Class<?> valueType = (Class<?>) in.readObject();
        if (keyType == null || valueType == null || !keyType.isEnum() || !valueType.isEnum()) {
            throw new InvalidObjectException("Invalid enum types " + keyType + ", " + valueType);
        }
        init(keyType, valueType);
        int n = in.readInt();
        if (n < 0 || n > universes[0].length) { throw new InvalidObjectException("Invalid size " + n); }
        for (int i = 0; i < n; i++) {
            Object key = in.readObject();
            Object value = in.readObject();
            if (ord(0, key) < 0 || ord(1, value) < 0) { throw new InvalidObjectException("Invalid mapping " + key + "=" + value); }
            put(0, key, value, false);
        }
    }

    // ------------------------------------------------------------------ views, cursors and entries

    /** Weakly consistent ordinal cursor over side {@code s}; removal clears both lanes. */
    private final class Cursor<T> implements Iterator<T> {
        private final int s;
        private final int kind;
        private int next;
        private int last = -1;
        private Entry<?, ?> lastEntry;

        Cursor(int s, int kind) {
            this.s = s;
            this.kind = kind;
        }

        @Override public boolean hasNext() {
            int[] lane = lanes[s];
            while (next < lane.length && lane[next] == 0) { next++; }
            return next != lane.length;
        }

        @Override public T next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            last = next++;
            if (kind == KEYS) { return cast(universes[s][last]); }
            if (kind == VALUES) { return cast(universes[1 - s][lanes[s][last] - 1]); }
            lastEntry = new Entry<>(s, last);
            return cast(lastEntry);
        }

        @Override public void remove() {
            if (last < 0) { throw new IllegalStateException("no calls to next() since the last call to remove()"); }
            M3EnumBiMap.this.remove(s, universes[s][last]);
            if (lastEntry != null) { lastEntry.removed = true; }
            last = -1;
            lastEntry = null;
        }
    }

    /** Boundary flyweight for one entry of side {@code s}: ordinal-bound, live, unusable after iterator removal. */
    private final class Entry<A, B> implements Map.Entry<A, B> {
        private final int s;
        private final int o;
        private boolean removed;

        Entry(int s, int o) {
            this.s = s;
            this.o = o;
        }

        private void check() {
            if (removed) { throw new IllegalStateException("Entry was removed"); }
        }

        @Override public A getKey() {
            check();
            return cast(universes[s][o]);
        }

        @Override public B getValue() {
            check();
            return lanes[s][o] == 0 ? null : cast(universes[1 - s][lanes[s][o] - 1]);
        }

        @Override public B setValue(B value) {
            Objects.requireNonNull(value);
            check();
            if (lanes[s][o] == 0) { throw new IllegalStateException("entry no longer in map"); }
            return cast(put(s, universes[s][o], value, false));
        }

        @Override public boolean equals(Object other) {
            if (removed) { return other == this; }
            return other instanceof Map.Entry<?, ?> e && e.getKey() == universes[s][o] && Objects.equals(getValue(), e.getValue());
        }

        @Override public int hashCode() {
            return removed ? System.identityHashCode(this) : universes[s][o].hashCode() ^ Objects.hashCode(getValue());
        }

        @Override public String toString() {
            return removed ? "Entry was removed" : universes[s][o] + "=" + getValue();
        }
    }

    private final class Keys<T> extends AbstractSet<T> {
        private final int s;

        Keys(int s) {
            this.s = s;
        }

        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return M3EnumBiMap.this.contains(s, o);
        }

        @Override public boolean remove(Object o) {
            return M3EnumBiMap.this.remove(s, o) != null;
        }

        @Override public void clear() {
            clearAll();
        }

        @Override public Iterator<T> iterator() {
            return new Cursor<>(s, KEYS);
        }
    }

    private final class Values<T> extends AbstractSet<T> {
        private final int s;

        Values(int s) {
            this.s = s;
        }

        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return M3EnumBiMap.this.contains(1 - s, o);
        }

        @Override public boolean remove(Object o) {
            return M3EnumBiMap.this.remove(1 - s, o) != null;
        }

        @Override public void clear() {
            clearAll();
        }

        @Override public Iterator<T> iterator() {
            return new Cursor<>(s, VALUES);
        }
    }

    private final class Entries<A, B> extends AbstractSet<Map.Entry<A, B>> {
        private final int s;

        Entries(int s) {
            this.s = s;
        }

        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) { return false; }
            int k = ord(s, e.getKey());
            return k >= 0 && lanes[s][k] != 0 && universes[1 - s][lanes[s][k] - 1].equals(e.getValue());
        }

        @Override public boolean remove(Object o) {
            if (!contains(o)) { return false; }
            M3EnumBiMap.this.remove(s, ((Map.Entry<?, ?>) o).getKey());
            return true;
        }

        @Override public void clear() {
            clearAll();
        }

        @Override public Iterator<Map.Entry<A, B>> iterator() {
            return new Cursor<>(s, ENTRIES);
        }
    }

    /** The inverse view {@code V -> K}: side 1 of the same lanes. */
    private final class Side<A, B> extends AbstractMap<A, B> implements BiMap<A, B>, Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private final int s;
        /** Hoisted aliases of {@code types[s]}, {@code lanes[s]} and {@code universes[1 - s]} for get(). */
        private final transient Class<?> type;
        private final transient int[] lane;
        private final transient Enum<?>[] targets;
        private transient Set<A> keys;
        private transient Set<B> values;
        private transient Set<Map.Entry<A, B>> entries;

        Side(int s) {
            this.s = s;
            this.type = types[s];
            this.lane = lanes[s];
            this.targets = universes[1 - s];
        }

        @Override public int size() {
            return size;
        }

        @Override public boolean isEmpty() {
            return size == 0;
        }

        @Override public boolean containsKey(Object key) {
            return contains(s, key);
        }

        @Override public boolean containsValue(Object value) {
            return contains(1 - s, value);
        }

        @Override public B get(Object key) {
            return cast(lookup(key, type, lane, targets));
        }

        @Override public B put(A key, B value) {
            return cast(M3EnumBiMap.this.put(s, key, value, false));
        }

        @Override public B forcePut(A key, B value) {
            return cast(M3EnumBiMap.this.put(s, key, value, true));
        }

        @Override public B remove(Object key) {
            return cast(M3EnumBiMap.this.remove(s, key));
        }

        @Override public void clear() {
            clearAll();
        }

        @Override public void replaceAll(BiFunction<? super A, ? super B, ? extends B> function) {
            Objects.requireNonNull(function);
            M3EnumBiMap.this.replaceAll(s, (k, v) -> function.apply(cast(k), cast(v)));
        }

        @Override public void forEach(BiConsumer<? super A, ? super B> action) {
            Objects.requireNonNull(action);
            M3EnumBiMap.this.forEach(s, cast(action));
        }

        @Override public BiMap<B, A> inverse() {
            return cast(M3EnumBiMap.this);
        }

        @Override public Set<A> keySet() {
            Set<A> v = keys;
            return v != null ? v : (keys = new Keys<>(s));
        }

        @Override public Set<B> values() {
            Set<B> v = values;
            return v != null ? v : (values = new Values<>(s));
        }

        @Override public Set<Map.Entry<A, B>> entrySet() {
            Set<Map.Entry<A, B>> v = entries;
            return v != null ? v : (entries = new Entries<>(s));
        }

        @Override public boolean equals(Object other) {
            return other == this || equalsMap(s, other);
        }

        @Override public int hashCode() {
            return hash(s);
        }

        @Override public String toString() {
            return text(s);
        }

        @Serial
        private Object writeReplace() throws ObjectStreamException {
            return new InverseForm(M3EnumBiMap.this);
        }
    }

    /** Serialized form of an inverse view: the forward bimap, resolved back to its inverse. */
    private static final class InverseForm implements Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private final M3EnumBiMap<?, ?> forward;

        InverseForm(M3EnumBiMap<?, ?> forward) {
            this.forward = forward;
        }

        @Serial
        private Object readResolve() throws ObjectStreamException {
            return forward.inverse();
        }
    }
}
