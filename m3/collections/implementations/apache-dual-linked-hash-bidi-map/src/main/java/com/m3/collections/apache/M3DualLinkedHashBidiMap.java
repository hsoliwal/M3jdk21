/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Apache Commons Collections 4.5.0
 * org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap. Commons supplies only the BidiMap,
 * MapIterator and ResettableIterator API interfaces (Apache-2.0); no Commons implementation class, helper or
 * body is called or copied. The ordered slot table is adapted from the first-party M3LinkedHashMap.Table.
 */
package com.m3.collections.apache;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

/**
 * Owned insertion-ordered bidi map over two slot tables of arrays, offsets and masks; no per-entry objects.
 *
 * <p>Each direction is one ordered slot table: {@code Object[] keys/values}, {@code int[] hashes}, a
 * power-of-two {@code int[] index} (slot+1, 0 empty, -1 tombstone; probe masked by {@code length - 1}) with
 * {@code int[] positions}, {@code int[] before/after} encounter links, a free-slot chain and an
 * {@code int[] births} generation stamp per slot. Table 0 maps keys to values and table 1 values to keys;
 * their orders evolve independently, exactly as the two LinkedHashMaps behind Commons' class do
 * (re-putting a mapping moves its value to the end of the inverse order, for example). Every operation is
 * side-symmetric: side 0 is this map and side 1 is {@link #inverseBidiMap()}, a flyweight over the same
 * tables. Iterators are fail-fast on their table's {@code modCount}, as LinkedHashMap iterators are.
 *
 * <p>Entry objects exist only where {@code entrySet()} iteration returns them (as Commons' decorators do);
 * {@code mapIterator()}, {@code forEach}, {@code equals}, {@code hashCode} and {@code toString} create none.
 * A returned entry stays bound to its slot while the slot's birth stamp is unchanged, so it reads live
 * values like a LinkedHashMap node and keeps its last observed value once its mapping is gone.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class M3DualLinkedHashBidiMap<K, V> implements BidiMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    private transient Table[] tables;
    private transient Side<V, K> inverseView;
    private transient Set<K> keySetView;
    private transient Set<V> valuesView;
    private transient Set<Map.Entry<K, V>> entrySetView;

    /** Creates an empty bidi map. */
    public M3DualLinkedHashBidiMap() {
        tables = new Table[] {new Table(), new Table()};
    }

    /**
     * Creates a bidi map holding the mappings of {@code map}, put in its iteration order.
     *
     * @param map the mappings to copy
     */
    public M3DualLinkedHashBidiMap(Map<? extends K, ? extends V> map) {
        this();
        putAll(map);
    }

    // ------------------------------------------------------------------ ordered slot table

    /** One LinkedHashMap-shaped direction: slot lanes, a hash index, encounter links and birth stamps. */
    static final class Table {
        private static final Object[] NO_OBJECTS = {};
        private static final int[] NO_INTS = {};
        Object[] keys = NO_OBJECTS;
        Object[] values = NO_OBJECTS;
        int[] hashes = NO_INTS;
        int[] before = NO_INTS;
        int[] after = NO_INTS;
        int[] positions = NO_INTS;
        int[] births = NO_INTS;
        int[] index = NO_INTS;
        int first = -1;
        int last = -1;
        int free = -1;
        int used;
        int occupied;
        int size;
        int modCount;
        int born;

        static int spread(Object key) {
            int h = key == null ? 0 : key.hashCode();
            return h ^ h >>> 16;
        }

        int find(Object key) {
            if (index.length == 0) { return -1; }
            int hash = spread(key);
            int mask = index.length - 1;
            for (int p = hash & mask; index[p] != 0; p = p + 1 & mask) {
                int s = index[p] - 1;
                if (s >= 0 && hashes[s] == hash && Objects.equals(key, keys[s])) { return s; }
            }
            return -1;
        }

        boolean live(int slot, int birth) {
            return positions[slot] >= 0 && births[slot] == birth;
        }

        static int grown(int old) {
            if (old >= Integer.MAX_VALUE - 8) { throw new OutOfMemoryError("bidi map capacity exhausted"); }
            return (int) Math.min(Integer.MAX_VALUE - 8L, Math.max(8L, (long) old + (old >> 1) + 1L));
        }

        static int doubled(int length) {
            if (length >= 1 << 30) { throw new OutOfMemoryError("bidi map index capacity exhausted"); }
            return length << 1;
        }

        private void rehash(int capacity) {
            int[] table = new int[capacity];
            int mask = capacity - 1;
            for (int s = first; s >= 0; s = after[s]) {
                int p = hashes[s] & mask;
                while (table[p] != 0) { p = p + 1 & mask; }
                table[p] = s + 1;
                positions[s] = p;
            }
            index = table;
            occupied = size;
        }

        private void ensureSlot() {
            if (free >= 0 || used < keys.length) { return; }
            int n = grown(keys.length);
            keys = Arrays.copyOf(keys, n);
            values = Arrays.copyOf(values, n);
            hashes = Arrays.copyOf(hashes, n);
            before = Arrays.copyOf(before, n);
            after = Arrays.copyOf(after, n);
            positions = Arrays.copyOf(positions, n);
            births = Arrays.copyOf(births, n);
        }

        private void ensureIndex() {
            if (index.length == 0) {
                rehash(16);
            } else if (occupied + 1 >= index.length - index.length / 3) {
                rehash(size + 1 >= index.length - index.length / 3 ? doubled(index.length) : index.length);
            }
        }

        /**
         * Puts like LinkedHashMap: an existing key keeps its slot, position and key object; a new key goes last.
         *
         * @return the previous value, or null when the key was new
         */
        Object put(Object key, Object value) {
            int s = find(key);
            if (s >= 0) {
                Object old = values[s];
                values[s] = value;
                return old;
            }
            ensureSlot();
            ensureIndex();
            int hash = spread(key);
            int mask = index.length - 1;
            int p = hash & mask;
            while (index[p] > 0) { p = p + 1 & mask; }
            if (index[p] == 0) { occupied++; }
            if (free < 0) { s = used++; } else { s = free; free = after[s]; }
            keys[s] = key;
            values[s] = value;
            hashes[s] = hash;
            births[s] = ++born;
            index[p] = s + 1;
            positions[s] = p;
            before[s] = last;
            after[s] = -1;
            if (last < 0) { first = s; } else { after[last] = s; }
            last = s;
            size++;
            modCount++;
            return null;
        }

        void erase(int s) {
            int b = before[s];
            int a = after[s];
            if (b < 0) { first = a; } else { after[b] = a; }
            if (a < 0) { last = b; } else { before[a] = b; }
            index[positions[s]] = -1;
            keys[s] = null;
            values[s] = null;
            before[s] = -1;
            positions[s] = -1;
            after[s] = free;
            free = s;
            size--;
            modCount++;
        }

        /** Removes {@code key} when present. */
        void remove(Object key) {
            int s = find(key);
            if (s >= 0) { erase(s); }
        }

        void clear() {
            Arrays.fill(keys, 0, used, null);
            Arrays.fill(values, 0, used, null);
            Arrays.fill(positions, 0, used, -1);
            Arrays.fill(index, 0);
            first = -1;
            last = -1;
            free = -1;
            used = 0;
            occupied = 0;
            size = 0;
            modCount++;
        }
    }

    // ------------------------------------------------------------------ side-symmetric operations

    private boolean containsKey(int s, Object key) {
        return tables[s].find(key) >= 0;
    }

    private Object get(int s, Object key) {
        int slot = tables[s].find(key);
        return slot < 0 ? null : tables[s].values[slot];
    }

    /** Commons' put: drop the key's old value and the value's old key, then put both ways. */
    private Object put(int s, Object key, Object value) {
        Table normal = tables[s];
        Table reverse = tables[1 - s];
        int k = normal.find(key);
        if (k >= 0) { reverse.remove(normal.values[k]); }
        int v = reverse.find(value);
        if (v >= 0) { normal.remove(reverse.values[v]); }
        Object old = normal.put(key, value);
        reverse.put(value, key);
        return old;
    }

    private Object remove(int s, Object key) {
        int slot = tables[s].find(key);
        if (slot < 0) { return null; }
        Object value = tables[s].values[slot];
        tables[s].erase(slot);
        tables[1 - s].remove(value);
        return value;
    }

    private void clearAll() {
        tables[0].clear();
        tables[1].clear();
    }

    /** Throws when a value is already owned by a key other than {@code key} (identity, as Commons compares). */
    private void checkOwner(int s, Object key, Object value) {
        int owner = tables[1 - s].find(value);
        if (owner >= 0 && tables[1 - s].values[owner] != key) {
            throw new IllegalArgumentException("Cannot use setValue() when the object being set is already in the map");
        }
    }

    /** LinkedHashMap's equality (AbstractMap rules, exception guard included). */
    private boolean equalsMap(int s, Object self, Object other) {
        if (other == self) { return true; }
        if (!(other instanceof Map<?, ?> m) || m.size() != tables[s].size) { return false; }
        Table t = tables[s];
        try {
            for (int slot = t.first; slot >= 0; slot = t.after[slot]) {
                Object key = t.keys[slot];
                Object value = t.values[slot];
                if (value == null ? !(m.get(key) == null && m.containsKey(key)) : !value.equals(m.get(key))) { return false; }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
        return true;
    }

    private int hash(int s) {
        Table t = tables[s];
        int h = 0;
        for (int slot = t.first; slot >= 0; slot = t.after[slot]) { h += Objects.hashCode(t.keys[slot]) ^ Objects.hashCode(t.values[slot]); }
        return h;
    }

    /** LinkedHashMap's text form of table {@code s} (Commons prints its delegate, so no self-reference guard). */
    private String text(int s) {
        Table t = tables[s];
        StringBuilder b = new StringBuilder("{");
        for (int slot = t.first; slot >= 0; slot = t.after[slot]) {
            if (slot != t.first) { b.append(", "); }
            b.append(t.keys[slot]).append('=').append(t.values[slot]);
        }
        return b.append('}').toString();
    }

    /** Map.forEach over the entry iterator: a concurrent change surfaces when the walk advances. */
    private void forEach(int s, BiConsumer<Object, Object> action) {
        Objects.requireNonNull(action);
        Table t = tables[s];
        int expected = t.modCount;
        for (int slot = t.first; slot >= 0;) {
            int next = t.after[slot];
            action.accept(t.keys[slot], t.values[slot]);
            if (next >= 0 && t.modCount != expected) { throw new ConcurrentModificationException(); }
            slot = next;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // ------------------------------------------------------------------ BidiMap API (side 0)

    @Override public int size() {
        return tables[0].size;
    }

    @Override public boolean isEmpty() {
        return tables[0].size == 0;
    }

    @Override public boolean containsKey(Object key) {
        return containsKey(0, key);
    }

    @Override public boolean containsValue(Object value) {
        return containsKey(1, value);
    }

    @Override public V get(Object key) {
        return cast(get(0, key));
    }

    @Override public K getKey(Object value) {
        return cast(get(1, value));
    }

    @Override public V put(K key, V value) {
        return cast(put(0, key, value));
    }

    @Override public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) { put(0, e.getKey(), e.getValue()); }
    }

    @Override public V remove(Object key) {
        return cast(remove(0, key));
    }

    @Override public K removeValue(Object value) {
        return cast(remove(1, value));
    }

    @Override public void clear() {
        clearAll();
    }

    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        forEach(0, cast(action));
    }

    @Override public BidiMap<V, K> inverseBidiMap() {
        Side<V, K> v = inverseView;
        return v != null ? v : (inverseView = new Side<>(1));
    }

    @Override public MapIterator<K, V> mapIterator() {
        return new Cursor<>(0);
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
        return equalsMap(0, this, other);
    }

    @Override public int hashCode() {
        return hash(0);
    }

    @Override public String toString() {
        return text(0);
    }

    /** Checks both tables (links, index, free chain) and that each mapping is mirrored by the other side. */
    void verifyInvariants() {
        verifyStructure();
        for (int s = 0; s < 2; s++) {
            Table t = tables[s];
            Table o = tables[1 - s];
            for (int slot = t.first; slot >= 0; slot = t.after[slot]) {
                int mirror = o.find(t.values[slot]);
                if (mirror < 0 || !Objects.equals(o.values[mirror], t.keys[slot])) { throw new AssertionError("mirror"); }
            }
        }
    }

    /**
     * Checks each table on its own (links, index, free chain, size). Commons' raw entry-set arrays and its
     * values-iterator removal can leave the two directions out of step; this check still holds then.
     */
    void verifyStructure() {
        for (int s = 0; s < 2; s++) {
            Table t = tables[s];
            boolean[] seen = new boolean[t.used];
            int count = 0;
            int previous = -1;
            for (int slot = t.first; slot >= 0; slot = t.after[slot]) {
                if (seen[slot] || t.before[slot] != previous || t.index[t.positions[slot]] != slot + 1 || t.find(t.keys[slot]) != slot) {
                    throw new AssertionError("links");
                }
                seen[slot] = true;
                previous = slot;
                count++;
            }
            if (count != t.size || previous != t.last) { throw new AssertionError("size"); }
            for (int slot = t.free; slot >= 0; slot = t.after[slot]) {
                if (seen[slot] || t.keys[slot] != null || t.positions[slot] >= 0) { throw new AssertionError("free"); }
                seen[slot] = true;
                count++;
            }
            if (count != t.used) { throw new AssertionError("ownership"); }
        }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        Table t = tables[0];
        out.writeInt(t.size);
        for (int slot = t.first; slot >= 0; slot = t.after[slot]) {
            out.writeObject(t.keys[slot]);
            out.writeObject(t.values[slot]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        tables = new Table[] {new Table(), new Table()};
        int n = in.readInt();
        if (n < 0) { throw new InvalidObjectException("Invalid size " + n); }
        for (int i = 0; i < n; i++) { put(0, in.readObject(), in.readObject()); }
    }

    // ------------------------------------------------------------------ cursors and entries

    /** LinkedHashMap-style fail-fast walk over table {@code s} with a precomputed successor. */
    private class Walk {
        final int s;
        int next;
        int current = -1;
        int expected;

        Walk(int s) {
            this.s = s;
            restart();
        }

        final void restart() {
            next = tables[s].first;
            current = -1;
            expected = tables[s].modCount;
        }

        final int advance() {
            Table t = tables[s];
            if (t.modCount != expected) { throw new ConcurrentModificationException(); }
            if (next < 0) { throw new NoSuchElementException(); }
            current = next;
            next = t.after[current];
            return current;
        }

        /** Removes the current mapping from this table, then its live value from the other table, as Commons does. */
        final void drop() {
            drop(tables[s].values[current]);
        }

        /**
         * Removes the current mapping from this table, then {@code reverseKey} from the other table. Commons'
         * values iterator passes the value it delivered, which may be stale after a concurrent replacement.
         */
        final void drop(Object reverseKey) {
            Table t = tables[s];
            if (t.modCount != expected) { throw new ConcurrentModificationException(); }
            t.erase(current);
            current = -1;
            expected = t.modCount;
            tables[1 - s].remove(reverseKey);
        }
    }

    /** The map iterator: Commons' messages, remove-once, setValue through put, and reset. */
    private final class Cursor<A, B> extends Walk implements MapIterator<A, B>, ResettableIterator<A> {
        private Object lastKey;
        private Object lastValue;
        private int lastSlot = -1;
        private int lastBirth;
        private boolean canRemove;

        Cursor(int s) {
            super(s);
        }

        @Override public boolean hasNext() {
            return next >= 0;
        }

        @Override public A next() {
            int slot = advance();
            Table t = tables[s];
            lastSlot = slot;
            lastBirth = t.births[slot];
            lastKey = t.keys[slot];
            lastValue = t.values[slot];
            canRemove = true;
            return cast(lastKey);
        }

        private void checkLast(String method) {
            if (lastSlot < 0) { throw new IllegalStateException("Iterator " + method + "() can only be called after next() and before remove()"); }
        }

        @Override public A getKey() {
            checkLast("getKey");
            return cast(lastKey);
        }

        /** Reads the slot while it keeps its birth stamp; a removed mapping keeps its last observed value. */
        private Object observe() {
            Table t = tables[s];
            if (t.live(lastSlot, lastBirth)) { lastValue = t.values[lastSlot]; }
            return lastValue;
        }

        @Override public B getValue() {
            checkLast("getValue");
            return cast(observe());
        }

        /** Puts through the map, as Commons does; a removed mapping is re-added and the cursor keeps its old value. */
        @Override public B setValue(B value) {
            checkLast("setValue");
            checkOwner(s, lastKey, value);
            observe();
            Object old = put(s, lastKey, value);
            observe();
            return cast(old);
        }

        @Override public void remove() {
            if (!canRemove) { throw new IllegalStateException("Iterator remove() can only be called once after next()"); }
            drop();
            lastSlot = -1;
            canRemove = false;
        }

        @Override public void reset() {
            restart();
            lastSlot = -1;
            canRemove = false;
        }

        @Override public String toString() {
            return lastSlot >= 0 ? "MapIterator[" + getKey() + "=" + getValue() + "]" : "MapIterator[]";
        }
    }

    /** Key, value or entry iterator over table {@code s}; removal drops the mapping from both tables. */
    private final class ViewCursor<T> extends Walk implements Iterator<T> {
        private final int kind;
        private boolean canRemove;
        private Object delivered;

        ViewCursor(int s, int kind) {
            super(s);
            this.kind = kind;
        }

        @Override public boolean hasNext() {
            return next >= 0;
        }

        @Override public T next() {
            int slot = advance();
            Table t = tables[s];
            canRemove = true;
            if (kind == KEYS) { return cast(t.keys[slot]); }
            if (kind == VALUES) { return cast(delivered = t.values[slot]); }
            return cast(new Entry<>(s, slot, t.births[slot], t.keys[slot], t.values[slot], false));
        }

        /** The values iterator drops the value it delivered (Commons' ValuesIterator); the others drop the live value. */
        @Override public void remove() {
            if (!canRemove) { throw new IllegalStateException("Iterator remove() can only be called once after next()"); }
            if (kind == VALUES) {
                drop(delivered);
                delivered = null;
            } else {
                drop();
            }
            canRemove = false;
        }
    }

    /**
     * Boundary flyweight for one entry, node-like: live while its slot keeps its birth stamp, then frozen. A
     * guarded entry (iterator, stream, forEach) sets through {@code put}; a raw entry ({@code toArray}) writes
     * only this direction's value, as the LinkedHashMap nodes Commons' entry-set arrays expose do.
     */
    private final class Entry<A, B> implements Map.Entry<A, B> {
        private final int s;
        private final int slot;
        private final int birth;
        private final Object key;
        private final boolean raw;
        private Object value;

        Entry(int s, int slot, int birth, Object key, Object value, boolean raw) {
            this.s = s;
            this.slot = slot;
            this.birth = birth;
            this.key = key;
            this.value = value;
            this.raw = raw;
        }

        @Override public A getKey() {
            return cast(key);
        }

        @Override public B getValue() {
            Table t = tables[s];
            if (t.live(slot, birth)) { value = t.values[slot]; }
            return cast(value);
        }

        /** Commons' entry decorator: refuse a value owned by another key, put, then report the node's value. */
        @Override public B setValue(B newValue) {
            if (raw) { return cast(setRaw(newValue)); }
            checkOwner(s, key, newValue);
            boolean wasLive = tables[s].live(slot, birth);
            Object old = getValue();
            put(s, key, newValue);
            value = newValue;
            return cast(wasLive ? newValue : old);
        }

        /** Node setValue: no ownership check, no inverse maintenance, no modCount change. */
        private Object setRaw(Object newValue) {
            Table t = tables[s];
            Object old = getValue();
            if (t.live(slot, birth)) { t.values[slot] = newValue; }
            value = newValue;
            return old;
        }

        @Override public boolean equals(Object other) {
            return other == this || other instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        @Override public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }

        @Override public String toString() {
            return key + "=" + getValue();
        }
    }

    // ------------------------------------------------------------------ views

    /** Commons' view rules: bulk removals short-circuit on an empty map, retain-nothing clears it. */
    private abstract class View<E> extends AbstractCollection<E> implements Set<E> {
        final int s;

        View(int s) {
            this.s = s;
        }

        @Override public final int size() {
            return tables[s].size;
        }

        @Override public final boolean isEmpty() {
            return tables[s].size == 0;
        }

        @Override public final void clear() {
            clearAll();
        }

        @Override public final boolean removeAll(Collection<?> coll) {
            if (tables[s].size == 0 || coll.isEmpty()) { return false; }
            boolean modified = false;
            for (Object o : coll) { modified |= remove(o); }
            return modified;
        }

        @Override public final boolean removeIf(Predicate<? super E> filter) {
            if (tables[s].size == 0 || filter == null) { return false; }
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (filter.test(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override public final boolean retainAll(Collection<?> coll) {
            if (tables[s].size == 0) { return false; }
            if (coll.isEmpty()) {
                clearAll();
                return true;
            }
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (!coll.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        /** AbstractSet equality, as LinkedHashMap's key and entry views compare. */
        final boolean setEquals(Object other) {
            if (other == this) { return true; }
            if (!(other instanceof Set<?> o) || o.size() != size()) { return false; }
            try {
                return containsAll(o);
            } catch (ClassCastException | NullPointerException unused) {
                return false;
            }
        }
    }

    private final class Keys<A> extends View<A> {
        Keys(int s) {
            super(s);
        }

        @Override public boolean contains(Object o) {
            return containsKey(s, o);
        }

        @Override public boolean remove(Object o) {
            if (!containsKey(s, o)) { return false; }
            M3DualLinkedHashBidiMap.this.remove(s, o);
            return true;
        }

        @Override public Iterator<A> iterator() {
            return new ViewCursor<>(s, KEYS);
        }

        @Override public boolean equals(Object other) {
            return setEquals(other);
        }

        @Override public int hashCode() {
            Table t = tables[s];
            int h = 0;
            for (int slot = t.first; slot >= 0; slot = t.after[slot]) { h += Objects.hashCode(t.keys[slot]); }
            return h;
        }
    }

    /** Values view: membership and removal go through the inverse table; equality is identity, as Commons' is. */
    private final class Values<B> extends View<B> {
        Values(int s) {
            super(s);
        }

        @Override public boolean contains(Object o) {
            return containsKey(1 - s, o);
        }

        @Override public boolean remove(Object o) {
            if (!containsKey(1 - s, o)) { return false; }
            M3DualLinkedHashBidiMap.this.remove(1 - s, o);
            return true;
        }

        @Override public Iterator<B> iterator() {
            return new ViewCursor<>(s, VALUES);
        }

        /** Commons delegates containsAll to the forward values collection: a scan, not the inverse index. */
        @Override public boolean containsAll(Collection<?> coll) {
            Table t = tables[s];
            for (Object o : coll) {
                boolean found = false;
                for (int slot = t.first; slot >= 0 && !found; slot = t.after[slot]) { found = Objects.equals(o, t.values[slot]); }
                if (!found) { return false; }
            }
            return true;
        }

        @Override public boolean equals(Object other) {
            return other == this;
        }

        @Override public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    private final class Entries<A, B> extends View<Map.Entry<A, B>> {
        Entries(int s) {
            super(s);
        }

        private int slotOf(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) { return -1; }
            int slot = tables[s].find(e.getKey());
            return slot >= 0 && Objects.equals(tables[s].values[slot], e.getValue()) ? slot : -1;
        }

        @Override public boolean contains(Object o) {
            return slotOf(o) >= 0;
        }

        @Override public boolean remove(Object o) {
            int slot = slotOf(o);
            if (slot < 0) { return false; }
            Object value = tables[s].values[slot];
            tables[s].erase(slot);
            tables[1 - s].remove(value);
            return true;
        }

        @Override public Iterator<Map.Entry<A, B>> iterator() {
            return new ViewCursor<>(s, ENTRIES);
        }

        /** Raw entries, as Commons' decorated LinkedHashMap entry-set arrays hold its nodes. */
        @Override public Object[] toArray() {
            Table t = tables[s];
            Object[] out = new Object[t.size];
            int i = 0;
            for (int slot = t.first; slot >= 0; slot = t.after[slot]) { out[i++] = new Entry<>(s, slot, t.births[slot], t.keys[slot], t.values[slot], true); }
            return out;
        }

        @Override public <T> T[] toArray(T[] a) {
            Object[] raw = toArray();
            if (a.length < raw.length) {
                @SuppressWarnings("unchecked") T[] grown = (T[]) Arrays.copyOf(raw, raw.length, a.getClass());
                return grown;
            }
            System.arraycopy(raw, 0, a, 0, raw.length);
            if (a.length > raw.length) { a[raw.length] = null; }
            return a;
        }

        @Override public boolean equals(Object other) {
            return setEquals(other);
        }

        @Override public int hashCode() {
            return hash(s);
        }
    }

    /** The inverse bidi map: side 1 of the same tables. */
    private final class Side<A, B> implements BidiMap<A, B>, Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private final int s;
        private transient Set<A> keys;
        private transient Set<B> values;
        private transient Set<Map.Entry<A, B>> entries;

        Side(int s) {
            this.s = s;
        }

        @Override public int size() {
            return tables[s].size;
        }

        @Override public boolean isEmpty() {
            return tables[s].size == 0;
        }

        @Override public boolean containsKey(Object key) {
            return M3DualLinkedHashBidiMap.this.containsKey(s, key);
        }

        @Override public boolean containsValue(Object value) {
            return M3DualLinkedHashBidiMap.this.containsKey(1 - s, value);
        }

        @Override public B get(Object key) {
            return cast(M3DualLinkedHashBidiMap.this.get(s, key));
        }

        @Override public A getKey(Object value) {
            return cast(M3DualLinkedHashBidiMap.this.get(1 - s, value));
        }

        @Override public B put(A key, B value) {
            return cast(M3DualLinkedHashBidiMap.this.put(s, key, value));
        }

        @Override public void putAll(Map<? extends A, ? extends B> map) {
            for (Map.Entry<? extends A, ? extends B> e : map.entrySet()) { M3DualLinkedHashBidiMap.this.put(s, e.getKey(), e.getValue()); }
        }

        @Override public B remove(Object key) {
            return cast(M3DualLinkedHashBidiMap.this.remove(s, key));
        }

        @Override public A removeValue(Object value) {
            return cast(M3DualLinkedHashBidiMap.this.remove(1 - s, value));
        }

        @Override public void clear() {
            clearAll();
        }

        @Override public void forEach(BiConsumer<? super A, ? super B> action) {
            M3DualLinkedHashBidiMap.this.forEach(s, cast(action));
        }

        @Override public BidiMap<B, A> inverseBidiMap() {
            return cast(M3DualLinkedHashBidiMap.this);
        }

        @Override public MapIterator<A, B> mapIterator() {
            return new Cursor<>(s);
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
            return equalsMap(s, this, other);
        }

        @Override public int hashCode() {
            return hash(s);
        }

        @Override public String toString() {
            return text(s);
        }

        @Serial
        private Object writeReplace() throws ObjectStreamException {
            return new InverseForm(M3DualLinkedHashBidiMap.this);
        }
    }

    /** Serialized form of an inverse view: the forward map, resolved back to its inverse. */
    private static final class InverseForm implements Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private final M3DualLinkedHashBidiMap<?, ?> forward;

        InverseForm(M3DualLinkedHashBidiMap<?, ?> forward) {
            this.forward = forward;
        }

        @Serial
        private Object readResolve() throws ObjectStreamException {
            return forward.inverseBidiMap();
        }
    }
}
