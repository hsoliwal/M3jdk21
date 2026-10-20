/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Guava 33.4.8-jre com.google.common.collect.ArrayListMultimap.
 * Guava supplies only the ListMultimap/Multimap/Multiset API interfaces (Apache-2.0); no Guava
 * implementation class, helper, interface default or body is called or copied. The key directory
 * mechanics are adapted from the first-party M3LinkedHashMap.Table (m3/collections core, Apache-2.0).
 */
package com.m3.collections.guava;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Owned list multimap over dynamic arrays, offsets and bit masks; no per-entry or per-value objects.
 *
 * <p>Key directory: {@code Object[] keys}, {@code int[] hashes} and a power-of-two {@code int[] index}
 * (slot+1, 0 empty, -1 tombstone; linear probe masked by {@code length - 1}) with {@code int[] positions};
 * {@code int[] before/after} keep key encounter order and {@code long[] live} holds one bit per key slot.
 * Values: each key slot owns one dynamic {@code Object[]} segment with its length in {@code int[] counts},
 * so {@code get(key).get(i)} is one offset read. {@code int[] stamps} (drawn from one clock) and
 * {@code int[] births} make per-key views fail-fast; {@code keyModCount} does the same for key-directory
 * views. A key whose last value is removed leaves the directory, as in Guava.
 *
 * <p>Views ({@code get}, {@code keySet}, {@code values}, {@code entries}, {@code keys}, {@code asMap}) are
 * flyweights over these lanes. Entry-shaped results ({@code entries()} and {@code asMap().entrySet()}
 * elements, {@code keys().entrySet()} elements) are created only when that API returns them; internal
 * traversal ({@code forEach}, {@code equals}, {@code hashCode}, {@code containsEntry}) creates none.
 * Key iteration follows insertion order; Guava leaves key order unspecified.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class M3ArrayListMultimap<K, V> implements ListMultimap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int DEFAULT_VALUES_PER_KEY = 3;
    private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;
    private static final Object[] NO_VALUES = {};
    private static final Object[] NO_KEYS = {};
    private static final Object[][] NO_SEGMENTS = {};
    private static final int[] NO_INTS = {};
    private static final long[] NO_WORDS = {};

    private transient int expectedValuesPerKey;
    private transient Object[] keys;
    private transient Object[][] segments;
    private transient int[] hashes;
    private transient int[] positions;
    private transient int[] before;
    private transient int[] after;
    private transient int[] counts;
    private transient int[] stamps;
    private transient int[] births;
    private transient long[] live;
    private transient int[] index;
    private transient int first;
    private transient int last;
    private transient int free;
    private transient int used;
    private transient int occupied;
    private transient int keyCount;
    private transient int size;
    private transient int keyModCount;
    private transient int clock;
    private transient Set<K> keySetView;
    private transient Collection<V> valuesView;
    private transient Collection<Map.Entry<K, V>> entriesView;
    private transient Multiset<K> keysView;
    private transient Map<K, Collection<V>> asMapView;

    private M3ArrayListMultimap(int expectedKeys, int expectedValuesPerKey) {
        nonNegative(expectedKeys, "expectedSize");
        nonNegative(expectedValuesPerKey, "expectedValuesPerKey");
        this.expectedValuesPerKey = expectedValuesPerKey;
        initEmpty();
    }

    /** Creates an empty multimap with the default capacities. */
    public static <K, V> M3ArrayListMultimap<K, V> create() {
        return new M3ArrayListMultimap<>(0, DEFAULT_VALUES_PER_KEY);
    }

    /** Creates an empty multimap sized for the given key and per-key value counts. */
    public static <K, V> M3ArrayListMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
        return new M3ArrayListMultimap<>(expectedKeys, expectedValuesPerKey);
    }

    /** Creates a multimap holding the same mappings as {@code multimap}. */
    public static <K, V> M3ArrayListMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
        M3ArrayListMultimap<K, V> created = new M3ArrayListMultimap<>(multimap.keySet().size(),
                multimap instanceof M3ArrayListMultimap<?, ?> m3 ? m3.expectedValuesPerKey : DEFAULT_VALUES_PER_KEY);
        created.putAll(multimap);
        return created;
    }

    private static void nonNegative(int value, String name) {
        if (value < 0) { throw new IllegalArgumentException(name + " cannot be negative but was: " + value); }
    }

    private void initEmpty() {
        keys = NO_KEYS;
        segments = NO_SEGMENTS;
        hashes = NO_INTS;
        positions = NO_INTS;
        before = NO_INTS;
        after = NO_INTS;
        counts = NO_INTS;
        stamps = NO_INTS;
        births = NO_INTS;
        live = NO_WORDS;
        index = NO_INTS;
        first = -1;
        last = -1;
        free = -1;
    }

    // ------------------------------------------------------------------ key directory lanes

    private static int spread(Object key) {
        int h = key == null ? 0 : key.hashCode();
        return h ^ h >>> 16;
    }

    private int tick() {
        int t = ++clock;
        return t != 0 ? t : ++clock;
    }

    int find(Object key) {
        return find(key, spread(key));
    }

    private int find(Object key, int hash) {
        int[] table = index;
        if (table.length == 0) { return -1; }
        int mask = table.length - 1;
        for (int p = hash & mask; table[p] != 0; p = p + 1 & mask) {
            int s = table[p] - 1;
            if (s >= 0 && hashes[s] == hash && Objects.equals(key, keys[s])) { return s; }
        }
        return -1;
    }

    private int stampOf(Object key) {
        int s = find(key);
        return s < 0 ? 0 : stamps[s];
    }

    private int birthOf(Object key) {
        int s = find(key);
        return s < 0 ? 0 : births[s];
    }

    private int slotFor(Object key) {
        int h = spread(key);
        int s = find(key, h);
        return s >= 0 ? s : insertKey(key, h);
    }

    private int insertKey(Object key, int hash) {
        if (free < 0 && used == keys.length) { growSlots(); }
        if (index.length == 0) {
            rehash(16);
        } else if (occupied + 1 >= index.length - index.length / 3) {
            rehash(keyCount + 1 >= index.length - index.length / 3 ? doubled(index.length) : index.length);
        }
        int mask = index.length - 1;
        int p = hash & mask;
        while (index[p] > 0) { p = p + 1 & mask; }
        int s;
        if (free < 0) { s = used++; } else { s = free; free = after[s]; }
        if (index[p] == 0) { occupied++; }
        keys[s] = key;
        hashes[s] = hash;
        index[p] = s + 1;
        positions[s] = p;
        segments[s] = NO_VALUES;
        counts[s] = 0;
        before[s] = last;
        after[s] = -1;
        if (last < 0) { first = s; } else { after[last] = s; }
        last = s;
        live[s >>> 6] |= 1L << s;
        keyCount++;
        keyModCount++;
        stamps[s] = tick();
        births[s] = stamps[s];
        return s;
    }

    static int doubled(int length) {
        if (length >= 1 << 30) { throw new OutOfMemoryError("multimap key index capacity exhausted"); }
        return length << 1;
    }

    static int grownSlots(int old) {
        if (old >= MAX_ARRAY) { throw new OutOfMemoryError("multimap key capacity exhausted"); }
        return (int) Math.min(MAX_ARRAY, Math.max(8L, (long) old + (old >> 1) + 1L));
    }

    static void checkValueCapacity(long needed) {
        if (needed > MAX_ARRAY) { throw new OutOfMemoryError("multimap value capacity exhausted"); }
    }

    private void growSlots() {
        int n = grownSlots(keys.length);
        keys = Arrays.copyOf(keys, n);
        segments = Arrays.copyOf(segments, n);
        hashes = Arrays.copyOf(hashes, n);
        positions = Arrays.copyOf(positions, n);
        before = Arrays.copyOf(before, n);
        after = Arrays.copyOf(after, n);
        counts = Arrays.copyOf(counts, n);
        stamps = Arrays.copyOf(stamps, n);
        births = Arrays.copyOf(births, n);
        live = Arrays.copyOf(live, n + 63 >>> 6);
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
        occupied = keyCount;
    }

    private void eraseKey(int s) {
        int b = before[s];
        int a = after[s];
        if (b < 0) { first = a; } else { after[b] = a; }
        if (a < 0) { last = b; } else { before[a] = b; }
        index[positions[s]] = -1;
        size -= counts[s];
        keys[s] = null;
        segments[s] = NO_VALUES;
        counts[s] = 0;
        live[s >>> 6] &= ~(1L << s);
        before[s] = -1;
        positions[s] = -1;
        after[s] = free;
        free = s;
        keyCount--;
        keyModCount++;
        stamps[s] = tick();
    }

    // ------------------------------------------------------------------ value segment lanes

    private int capacityAfter(int length, int needed) {
        long grown = length == 0 ? Math.max(1, expectedValuesPerKey) : (long) length + (length >> 1) + 1L;
        return (int) Math.min(MAX_ARRAY, Math.max(grown, needed));
    }

    private void insertValues(int s, int at, Object[] values) {
        Object[] seg = segments[s];
        int n = counts[s];
        int length = values.length;
        checkValueCapacity(n + (long) length);
        if (n + length > seg.length) {
            seg = Arrays.copyOf(seg, capacityAfter(seg.length, n + length));
            segments[s] = seg;
        }
        System.arraycopy(seg, at, seg, at + length, n - at);
        System.arraycopy(values, 0, seg, at, length);
        counts[s] = n + length;
        size += length;
        stamps[s] = tick();
    }

    private void insertValue(int s, int at, Object value) {
        Object[] seg = segments[s];
        int n = counts[s];
        checkValueCapacity(n + 1L);
        if (n == seg.length) {
            seg = Arrays.copyOf(seg, capacityAfter(n, n + 1));
            segments[s] = seg;
        }
        System.arraycopy(seg, at, seg, at + 1, n - at);
        seg[at] = value;
        counts[s] = n + 1;
        size++;
        stamps[s] = tick();
    }

    private Object removeValueAt(int s, int at) {
        Object[] seg = segments[s];
        Object old = seg[at];
        int n = counts[s] - 1;
        System.arraycopy(seg, at + 1, seg, at, n - at);
        seg[n] = null;
        counts[s] = n;
        size--;
        stamps[s] = tick();
        if (n == 0) { eraseKey(s); }
        return old;
    }

    private void removeValueRange(int s, int from, int to) {
        Object[] seg = segments[s];
        int n = counts[s];
        System.arraycopy(seg, to, seg, from, n - to);
        Arrays.fill(seg, n - (to - from), n, null);
        counts[s] = n - (to - from);
        size -= to - from;
        stamps[s] = tick();
        if (counts[s] == 0) { eraseKey(s); }
    }

    /** Selects removals into a long[] mask first (callbacks may throw), then compacts once with two pointers. */
    private boolean removeMatching(int s, int from, int to, Predicate<Object> remove) {
        Object[] seg = segments[s];
        int width = to - from;
        long[] mask = new long[width + 63 >>> 6];
        int hits = 0;
        int stamp = stamps[s];
        for (int i = 0; i < width && stamps[s] == stamp; i++) {
            if (remove.test(seg[from + i])) {
                mask[i >>> 6] |= 1L << i;
                hits++;
            }
        }
        if (stamps[s] != stamp) { throw new ConcurrentModificationException(); }
        if (hits == 0) { return false; }
        int w = from;
        for (int i = 0; i < width; i++) {
            if ((mask[i >>> 6] & 1L << i) == 0) { seg[w++] = seg[from + i]; }
        }
        int n = counts[s];
        System.arraycopy(seg, to, seg, w, n - to);
        Arrays.fill(seg, n - hits, n, null);
        counts[s] = n - hits;
        size -= hits;
        stamps[s] = tick();
        if (counts[s] == 0) { eraseKey(s); }
        return true;
    }

    private int indexIn(int s, Object value, int from, int to) {
        Object[] seg = segments[s];
        for (int i = from; i < to; i++) {
            if (Objects.equals(value, seg[i])) { return i; }
        }
        return -1;
    }

    private int lastIndexIn(int s, Object value, int from, int to) {
        Object[] seg = segments[s];
        for (int i = to - 1; i >= from; i--) {
            if (Objects.equals(value, seg[i])) { return i; }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> detached(Object[] values) {
        return (List<T>) Arrays.asList(values);
    }

    private int segmentHash(int s, int from, int to) {
        Object[] seg = segments[s];
        int h = 1;
        for (int i = from; i < to; i++) { h = 31 * h + Objects.hashCode(seg[i]); }
        return h;
    }

    private boolean segmentEquals(int s, int from, int to, Object other) {
        if (!(other instanceof List<?> list) || list.size() != to - from) { return false; }
        Object[] seg = segments[s];
        int i = from;
        for (Object o : list) {
            if (i >= to || !Objects.equals(seg[i++], o)) { return false; }
        }
        return i == to;
    }

    // ------------------------------------------------------------------ Multimap API

    @Override public int size() {
        return size;
    }

    @Override public boolean isEmpty() {
        return size == 0;
    }

    @Override public boolean containsKey(Object key) {
        return find(key) >= 0;
    }

    @Override public boolean containsValue(Object value) {
        for (int s = first; s >= 0; s = after[s]) {
            if (indexIn(s, value, 0, counts[s]) >= 0) { return true; }
        }
        return false;
    }

    @Override public boolean containsEntry(Object key, Object value) {
        int s = find(key);
        return s >= 0 && indexIn(s, value, 0, counts[s]) >= 0;
    }

    @Override public boolean put(K key, V value) {
        int s = slotFor(key);
        insertValue(s, counts[s], value);
        return true;
    }

    @Override public boolean remove(Object key, Object value) {
        int s = find(key);
        if (s < 0) { return false; }
        int i = indexIn(s, value, 0, counts[s]);
        if (i < 0) { return false; }
        removeValueAt(s, i);
        return true;
    }

    @Override public boolean putAll(K key, Iterable<? extends V> values) {
        Objects.requireNonNull(values);
        if (values instanceof Collection<?> c) {
            if (c.isEmpty()) { return false; }
            Object[] copy = c.toArray();
            if (copy.length == 0) { return false; }
            int s = slotFor(key);
            insertValues(s, counts[s], copy);
            return true;
        }
        Iterator<? extends V> it = values.iterator();
        if (!it.hasNext()) { return false; }
        List<V> view = get(key);
        while (it.hasNext()) { view.add(it.next()); }
        return true;
    }

    @Override public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        Objects.requireNonNull(multimap);
        if (multimap == this) {
            boolean changed = false;
            for (Map.Entry<? extends K, ? extends V> e : multimap.entries()) { changed |= put(e.getKey(), e.getValue()); }
            return changed;
        }
        int sizeBefore = size;
        multimap.forEach(this::put);
        return size != sizeBefore;
    }

    @Override public List<V> replaceValues(K key, Iterable<? extends V> values) {
        Iterator<? extends V> it = values.iterator();
        if (!it.hasNext()) { return removeAll(key); }
        int s = slotFor(key);
        Object[] old = Arrays.copyOf(segments[s], counts[s]);
        Arrays.fill(segments[s], 0, counts[s], null);
        size -= counts[s];
        counts[s] = 0;
        stamps[s] = tick();
        while (it.hasNext()) { insertValue(s, counts[s], it.next()); }
        return Collections.unmodifiableList(detached(old));
    }

    @Override public List<V> removeAll(Object key) {
        int s = find(key);
        if (s < 0) { return Collections.emptyList(); }
        Object[] old = Arrays.copyOf(segments[s], counts[s]);
        eraseKey(s);
        return Collections.unmodifiableList(detached(old));
    }

    @Override public void clear() {
        for (int s = first; s >= 0; s = after[s]) { stamps[s] = tick(); }
        Arrays.fill(keys, 0, used, null);
        Arrays.fill(segments, 0, used, NO_VALUES);
        Arrays.fill(counts, 0, used, 0);
        Arrays.fill(live, 0L);
        Arrays.fill(index, 0);
        first = -1;
        last = -1;
        free = -1;
        used = 0;
        occupied = 0;
        keyCount = 0;
        size = 0;
        keyModCount++;
    }

    @Override public List<V> get(K key) {
        return new ValueList(key);
    }

    @Override public Set<K> keySet() {
        Set<K> v = keySetView;
        return v != null ? v : (keySetView = new KeySetView());
    }

    @Override public Multiset<K> keys() {
        Multiset<K> v = keysView;
        return v != null ? v : (keysView = new KeysView());
    }

    @Override public Collection<V> values() {
        Collection<V> v = valuesView;
        return v != null ? v : (valuesView = new ValuesView());
    }

    @Override public Collection<Map.Entry<K, V>> entries() {
        Collection<Map.Entry<K, V>> v = entriesView;
        return v != null ? v : (entriesView = new EntriesView());
    }

    @Override public Map<K, Collection<V>> asMap() {
        Map<K, Collection<V>> v = asMapView;
        return v != null ? v : (asMapView = new AsMapView());
    }

    /** Visits every mapping without creating entries; fail-fast like Guava's backing map and lists. */
    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedKeys = keyModCount;
        for (int s = first; s >= 0; s = after[s]) {
            K key = cast(keys[s]);
            int stamp = stamps[s];
            for (int i = 0; i < counts[s] && stamps[s] == stamp; i++) { action.accept(key, cast(segments[s][i])); }
            if (stamps[s] != stamp || keyModCount != expectedKeys) { throw new ConcurrentModificationException(); }
        }
    }

    /** Trims every value segment to its size. */
    @Deprecated
    public void trimToSize() {
        for (int s = first; s >= 0; s = after[s]) {
            if (segments[s].length > counts[s]) { segments[s] = Arrays.copyOf(segments[s], counts[s]); }
        }
    }

    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Multimap<?, ?> that)) { return false; }
        Map<?, ? extends Collection<?>> map = that.asMap();
        if (map.size() != keyCount) { return false; }
        try {
            for (int s = first; s >= 0; s = after[s]) {
                Collection<?> values = map.get(keys[s]);
                if (values == null || !segmentEquals(s, 0, counts[s], values)) { return false; }
            }
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }
        return true;
    }

    @Override public int hashCode() {
        int h = 0;
        for (int s = first; s >= 0; s = after[s]) { h += Objects.hashCode(keys[s]) ^ segmentHash(s, 0, counts[s]); }
        return h;
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (int s = first; s >= 0; s = after[s]) {
            if (s != first) { b.append(", "); }
            b.append(keys[s]).append("=[");
            for (int i = 0; i < counts[s]; i++) {
                if (i > 0) { b.append(", "); }
                b.append(segments[s][i]);
            }
            b.append(']');
        }
        return b.append('}').toString();
    }

    /** Checks index membership, encounter links, live bits, free-slot ownership and value counts. */
    void verifyInvariants() {
        boolean[] seen = new boolean[used];
        int slots = 0;
        long values = 0;
        int previous = -1;
        for (int s = first; s >= 0; s = after[s]) {
            if (s >= used || seen[s] || before[s] != previous || index[positions[s]] != s + 1
                    || find(keys[s]) != s || (live[s >>> 6] & 1L << s) == 0 || counts[s] <= 0) {
                throw new AssertionError("key lanes");
            }
            seen[s] = true;
            previous = s;
            slots++;
            values += counts[s];
        }
        if (slots != keyCount || previous != last || values != size) { throw new AssertionError("counts"); }
        int buckets = 0;
        for (int p = 0; p < index.length; p++) {
            if (index[p] > 0) {
                buckets++;
                if (positions[index[p] - 1] != p) { throw new AssertionError("bucket"); }
            }
        }
        if (buckets != keyCount) { throw new AssertionError("buckets"); }
        for (int s = free; s >= 0; s = after[s]) {
            if (s >= used || seen[s] || keys[s] != null || counts[s] != 0 || (live[s >>> 6] & 1L << s) != 0) {
                throw new AssertionError("free lanes");
            }
            seen[s] = true;
            slots++;
        }
        if (slots != used) { throw new AssertionError("slot ownership"); }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(keyCount);
        for (int s = first; s >= 0; s = after[s]) {
            out.writeObject(keys[s]);
            out.writeInt(counts[s]);
            for (int i = 0; i < counts[s]; i++) { out.writeObject(segments[s][i]); }
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        expectedValuesPerKey = DEFAULT_VALUES_PER_KEY;
        initEmpty();
        int distinct = in.readInt();
        if (distinct < 0) { throw new InvalidObjectException("Invalid key count " + distinct); }
        for (int k = 0; k < distinct; k++) {
            Object key = in.readObject();
            int n = in.readInt();
            if (n <= 0) { throw new InvalidObjectException("Invalid value count " + n); }
            int s = slotFor(key);
            for (int i = 0; i < n; i++) { insertValue(s, counts[s], in.readObject()); }
        }
    }

    // ------------------------------------------------------------------ per-key list view

    /** Live list of one key's values (or a sub-list of it); holds no values, only the key and offsets. */
    private final class ValueList extends AbstractList<V> implements RandomAccess {
        private final Object key;
        private final ValueList parent;
        private final int offset;
        private int length;
        private int expected;

        ValueList(Object key) {
            this(key, null, 0, 0);
        }

        private ValueList(Object key, ValueList parent, int offset, int length) {
            this.key = key;
            this.parent = parent;
            this.offset = offset;
            this.length = length;
            this.expected = stampOf(key);
        }

        private void check() {
            if (parent != null && stampOf(key) != expected) { throw new ConcurrentModificationException(); }
        }

        int rawSize() {
            if (parent != null) { return length; }
            int s = find(key);
            return s < 0 ? 0 : counts[s];
        }

        @Override public int size() {
            check();
            return rawSize();
        }

        private void changed(int delta) {
            modCount++;
            int stamp = stampOf(key);
            for (ValueList l = this; l != null; l = l.parent) {
                if (l.parent != null) { l.length += delta; }
                l.expected = stamp;
            }
        }

        @Override public V get(int i) {
            Objects.checkIndex(i, size());
            return cast(segments[find(key)][offset + i]);
        }

        @Override public V set(int i, V value) {
            Objects.checkIndex(i, size());
            Object[] seg = segments[find(key)];
            V old = cast(seg[offset + i]);
            seg[offset + i] = value;
            return old;
        }

        @Override public void add(int i, V value) {
            int n = size();
            if (i < 0 || i > n) { throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + n); }
            insertValue(slotFor(key), offset + i, value);
            changed(1);
        }

        @Override public boolean add(V value) {
            add(size(), value);
            return true;
        }

        @Override public boolean addAll(Collection<? extends V> values) {
            return addAll(size(), values);
        }

        @Override public boolean addAll(int i, Collection<? extends V> values) {
            if (values.isEmpty()) { return false; }
            int n = size();
            if (i < 0 || i > n) { throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + n); }
            Object[] copy = values.toArray();
            if (copy.length == 0) { return false; }
            insertValues(slotFor(key), offset + i, copy);
            changed(copy.length);
            return true;
        }

        @Override public V remove(int i) {
            Objects.checkIndex(i, size());
            V old = cast(removeValueAt(find(key), offset + i));
            changed(-1);
            return old;
        }

        @Override public boolean remove(Object value) {
            int n = size();
            if (n == 0) { return false; }
            int s = find(key);
            int at = indexIn(s, value, offset, offset + n);
            if (at < 0) { return false; }
            removeValueAt(s, at);
            changed(-1);
            return true;
        }

        @Override public void clear() {
            int n = size();
            if (n == 0) { return; }
            removeValueRange(find(key), offset, offset + n);
            changed(-n);
        }

        @Override public boolean removeIf(Predicate<? super V> filter) {
            Objects.requireNonNull(filter);
            return bulkRemove(v -> filter.test(cast(v)));
        }

        @Override public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return bulkRemove(c::contains);
        }

        @Override public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return bulkRemove(v -> !c.contains(v));
        }

        private boolean bulkRemove(Predicate<Object> remove) {
            int n = size();
            if (n == 0) { return false; }
            int s = find(key);
            int countBefore = counts[s];
            if (!removeMatching(s, offset, offset + n, remove)) { return false; }
            changed(counts[s] - countBefore);
            return true;
        }

        @Override public void replaceAll(UnaryOperator<V> operator) {
            Objects.requireNonNull(operator);
            int n = size();
            if (n == 0) { return; }
            int s = find(key);
            int stamp = stamps[s];
            Object[] seg = segments[s];
            for (int i = 0; i < n && stamps[s] == stamp; i++) { seg[offset + i] = operator.apply(cast(seg[offset + i])); }
            if (stamps[s] != stamp) { throw new ConcurrentModificationException(); }
        }

        /** Sorts a detached copy, then writes it back through a cursor, as the List.sort contract specifies. */
        @Override public void sort(Comparator<? super V> comparator) {
            int n = size();
            if (n == 0) { return; }
            Object[] sorted = Arrays.copyOfRange(segments[find(key)], offset, offset + n);
            Comparator<Object> order = cast(comparator);
            Arrays.sort(sorted, order);
            ListIterator<V> it = listIterator();
            for (Object value : sorted) {
                it.next();
                it.set(cast(value));
            }
        }

        @Override public boolean contains(Object value) {
            return indexOf(value) >= 0;
        }

        @Override public int indexOf(Object value) {
            int n = size();
            if (n == 0) { return -1; }
            int at = indexIn(find(key), value, offset, offset + n);
            return at < 0 ? -1 : at - offset;
        }

        @Override public int lastIndexOf(Object value) {
            int n = size();
            if (n == 0) { return -1; }
            int at = lastIndexIn(find(key), value, offset, offset + n);
            return at < 0 ? -1 : at - offset;
        }

        @Override public Iterator<V> iterator() {
            return new ValueCursor(this, 0);
        }

        @Override public ListIterator<V> listIterator() {
            return new ValueCursor(this, 0);
        }

        @Override public ListIterator<V> listIterator(int i) {
            int n = size();
            if (i < 0 || i > n) { throw new IndexOutOfBoundsException("Index: " + i); }
            return new ValueCursor(this, i);
        }

        @Override public List<V> subList(int from, int to) {
            int n = size();
            if (from < 0) { throw new IndexOutOfBoundsException("fromIndex = " + from); }
            if (to > n) { throw new IndexOutOfBoundsException("toIndex = " + to); }
            if (from > to) { throw new IllegalArgumentException("fromIndex(" + from + ") > toIndex(" + to + ")"); }
            return new ValueList(key, this, offset + from, to - from);
        }

        @Override public Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, Spliterator.ORDERED);
        }

        @Override public int hashCode() {
            int n = size();
            return n == 0 ? 1 : segmentHash(find(key), offset, offset + n);
        }

        @Override public boolean equals(Object other) {
            if (other == this) { return true; }
            int n = size();
            if (n == 0) { return other instanceof List<?> l && l.isEmpty(); }
            return segmentEquals(find(key), offset, offset + n, other);
        }
    }

    /** List iterator over one key's values; fail-fast on that key's stamp and on key re-creation. */
    private final class ValueCursor implements ListIterator<V> {
        private final ValueList list;
        private final int birth;
        private int cursor;
        private int lastRet = -1;
        private int expected;

        ValueCursor(ValueList list, int at) {
            this.list = list;
            this.cursor = at;
            this.birth = birthOf(list.key);
            this.expected = stampOf(list.key);
        }

        private void validate() {
            int b = birthOf(list.key);
            if (b != 0 && b != birth) { throw new ConcurrentModificationException(); }
        }

        private void check() {
            validate();
            if (stampOf(list.key) != expected) { throw new ConcurrentModificationException(); }
        }

        @Override public boolean hasNext() {
            validate();
            return cursor != list.rawSize();
        }

        @Override public boolean hasPrevious() {
            validate();
            return cursor != 0;
        }

        @Override public int nextIndex() {
            validate();
            return cursor;
        }

        @Override public int previousIndex() {
            validate();
            return cursor - 1;
        }

        @Override public V next() {
            check();
            int i = cursor;
            if (i >= list.rawSize()) { throw new NoSuchElementException(); }
            cursor = i + 1;
            lastRet = i;
            return cast(segments[find(list.key)][list.offset + i]);
        }

        @Override public V previous() {
            check();
            int i = cursor - 1;
            if (i < 0) { throw new NoSuchElementException(); }
            cursor = i;
            lastRet = i;
            return cast(segments[find(list.key)][list.offset + i]);
        }

        @Override public void remove() {
            if (lastRet < 0) { throw new IllegalStateException(); }
            check();
            list.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
            expected = stampOf(list.key);
        }

        @Override public void set(V value) {
            if (lastRet < 0) { throw new IllegalStateException(); }
            check();
            list.set(lastRet, value);
        }

        @Override public void add(V value) {
            check();
            list.add(cursor++, value);
            lastRet = -1;
            expected = stampOf(list.key);
        }
    }

    // ------------------------------------------------------------------ whole-multimap cursors

    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    /** Walks key slots (precomputed successor, like a hash-map iterator) and each key's segment offsets. */
    private final class MappingCursor<T> implements Iterator<T> {
        private final int kind;
        private int slot = -1;
        private int nextSlot = first;
        private int at;
        private int lastAt = -1;
        private int expectedKeys = keyModCount;
        private int expectedStamp;

        MappingCursor(int kind) {
            this.kind = kind;
        }

        @Override public boolean hasNext() {
            return slot >= 0 && at != counts[slot] || nextSlot >= 0;
        }

        @Override public T next() {
            if (slot < 0 || at == counts[slot]) {
                if (keyModCount != expectedKeys) { throw new ConcurrentModificationException(); }
                if (nextSlot < 0) { throw new NoSuchElementException(); }
                slot = nextSlot;
                nextSlot = after[slot];
                at = 0;
                expectedStamp = stamps[slot];
            }
            if (stamps[slot] != expectedStamp) { throw new ConcurrentModificationException(); }
            lastAt = at++;
            Object key = keys[slot];
            Object value = segments[slot][lastAt];
            return cast(kind == KEYS ? key : kind == VALUES ? value : new AbstractMap.SimpleImmutableEntry<>(key, value));
        }

        @Override public void remove() {
            if (lastAt < 0) { throw new IllegalStateException("no calls to next() since the last call to remove()"); }
            if (stamps[slot] != expectedStamp) { throw new ConcurrentModificationException(); }
            if (counts[slot] == 1 && keyModCount != expectedKeys) { throw new ConcurrentModificationException(); }
            removeValueAt(slot, lastAt);
            at = lastAt;
            lastAt = -1;
            expectedStamp = stamps[slot];
            expectedKeys = keyModCount;
        }
    }

    /** Iterates key slots only; {@code entries} selects asMap entries instead of keys. */
    private final class KeyCursor<T> implements Iterator<T> {
        private final boolean entries;
        private int nextSlot = first;
        private int lastSlot = -1;
        private int expectedKeys = keyModCount;

        KeyCursor(boolean entries) {
            this.entries = entries;
        }

        @Override public boolean hasNext() {
            return nextSlot >= 0;
        }

        @Override public T next() {
            if (keyModCount != expectedKeys) { throw new ConcurrentModificationException(); }
            int s = nextSlot;
            if (s < 0) { throw new NoSuchElementException(); }
            nextSlot = after[s];
            lastSlot = s;
            Object key = keys[s];
            return cast(entries ? new AbstractMap.SimpleImmutableEntry<>(key, new ValueList(key)) : key);
        }

        @Override public void remove() {
            if (lastSlot < 0) { throw new IllegalStateException("no calls to next() since the last call to remove()"); }
            if (keyModCount != expectedKeys) { throw new ConcurrentModificationException(); }
            eraseKey(lastSlot);
            lastSlot = -1;
            expectedKeys = keyModCount;
        }
    }

    // ------------------------------------------------------------------ whole-multimap views

    private final class KeySetView extends AbstractSet<K> {
        @Override public int size() {
            return keyCount;
        }

        @Override public boolean contains(Object key) {
            return find(key) >= 0;
        }

        @Override public boolean remove(Object key) {
            int s = find(key);
            if (s < 0) { return false; }
            eraseKey(s);
            return true;
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public Iterator<K> iterator() {
            return new KeyCursor<>(false);
        }

        @Override public Spliterator<K> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT);
        }
    }

    private final class ValuesView extends AbstractCollection<V> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object value) {
            return containsValue(value);
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public Iterator<V> iterator() {
            return new MappingCursor<>(VALUES);
        }

        @Override public void forEach(Consumer<? super V> action) {
            Objects.requireNonNull(action);
            M3ArrayListMultimap.this.forEach((k, v) -> action.accept(v));
        }

        @Override public Spliterator<V> spliterator() {
            return Spliterators.spliterator(this, 0);
        }
    }

    private final class EntriesView extends AbstractCollection<Map.Entry<K, V>> {
        @Override public int size() {
            return size;
        }

        @Override public boolean contains(Object o) {
            return o instanceof Map.Entry<?, ?> e && containsEntry(e.getKey(), e.getValue());
        }

        @Override public boolean remove(Object o) {
            return o instanceof Map.Entry<?, ?> e && M3ArrayListMultimap.this.remove(e.getKey(), e.getValue());
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public Iterator<Map.Entry<K, V>> iterator() {
            return new MappingCursor<>(ENTRIES);
        }

        @Override public Spliterator<Map.Entry<K, V>> spliterator() {
            return Spliterators.spliterator(this, 0);
        }
    }

    private final class AsMapView extends AbstractMap<K, Collection<V>> {
        private Set<Map.Entry<K, Collection<V>>> entrySetView;

        @Override public int size() {
            return keyCount;
        }

        @Override public boolean containsKey(Object key) {
            return find(key) >= 0;
        }

        @Override public Collection<V> get(Object key) {
            return find(key) < 0 ? null : new ValueList(key);
        }

        @Override public Collection<V> remove(Object key) {
            int s = find(key);
            if (s < 0) { return null; }
            List<V> out = new ArrayList<>(counts[s]);
            for (int i = 0; i < counts[s]; i++) { out.add(cast(segments[s][i])); }
            eraseKey(s);
            return out;
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public Set<K> keySet() {
            return M3ArrayListMultimap.this.keySet();
        }

        @Override public Set<Map.Entry<K, Collection<V>>> entrySet() {
            Set<Map.Entry<K, Collection<V>>> v = entrySetView;
            return v != null ? v : (entrySetView = new AsMapEntries());
        }

        @Override public int hashCode() {
            return M3ArrayListMultimap.this.hashCode();
        }

        @Override public String toString() {
            return M3ArrayListMultimap.this.toString();
        }
    }

    private final class AsMapEntries extends AbstractSet<Map.Entry<K, Collection<V>>> {
        @Override public int size() {
            return keyCount;
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) { return false; }
            int s = find(e.getKey());
            return s >= 0 && segmentEquals(s, 0, counts[s], e.getValue());
        }

        @Override public boolean remove(Object o) {
            if (!contains(o)) { return false; }
            eraseKey(find(((Map.Entry<?, ?>) o).getKey()));
            return true;
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public Iterator<Map.Entry<K, Collection<V>>> iterator() {
            return new KeyCursor<>(true);
        }

        @Override public Spliterator<Map.Entry<K, Collection<V>>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT);
        }
    }

    /** The keys() multiset: counts are the key segments' lengths; additions are unsupported, as in Guava. */
    private final class KeysView extends AbstractCollection<K> implements Multiset<K> {
        private Set<Multiset.Entry<K>> entrySetView;

        @Override public int size() {
            return size;
        }

        @Override public boolean isEmpty() {
            return size == 0;
        }

        @Override public boolean contains(Object element) {
            return find(element) >= 0;
        }

        @Override public int count(Object element) {
            int s = find(element);
            return s < 0 ? 0 : counts[s];
        }

        @Override public int add(K element, int occurrences) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean add(K element) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean addAll(Collection<? extends K> elements) {
            Objects.requireNonNull(elements);
            if (elements.isEmpty()) { return false; }
            throw new UnsupportedOperationException();
        }

        @Override public int remove(Object element, int occurrences) {
            nonNegative(occurrences, "occurrences");
            int s = find(element);
            if (s < 0) { return 0; }
            int old = counts[s];
            if (occurrences == 0) { return old; }
            if (occurrences >= old) { eraseKey(s); } else { removeValueRange(s, 0, occurrences); }
            return old;
        }

        @Override public boolean remove(Object element) {
            return remove(element, 1) > 0;
        }

        @Override public int setCount(K element, int count) {
            nonNegative(count, "count");
            int old = count(element);
            if (count > old) { throw new UnsupportedOperationException(); }
            if (count < old) { remove(element, old - count); }
            return old;
        }

        @Override public boolean setCount(K element, int oldCount, int newCount) {
            nonNegative(oldCount, "oldCount");
            nonNegative(newCount, "newCount");
            if (count(element) != oldCount) { return false; }
            setCount(element, newCount);
            return true;
        }

        @Override public Set<K> elementSet() {
            return keySet();
        }

        @Override public Set<Multiset.Entry<K>> entrySet() {
            Set<Multiset.Entry<K>> v = entrySetView;
            return v != null ? v : (entrySetView = new CountEntries());
        }

        @Override public Iterator<K> iterator() {
            return new MappingCursor<>(KEYS);
        }

        @Override public void forEach(Consumer<? super K> action) {
            Objects.requireNonNull(action);
            M3ArrayListMultimap.this.forEach((k, v) -> action.accept(k));
        }

        @Override public void forEachEntry(ObjIntConsumer<? super K> action) {
            Objects.requireNonNull(action);
            int expectedKeys = keyModCount;
            for (int s = first; s >= 0; s = after[s]) {
                action.accept(cast(keys[s]), counts[s]);
                if (keyModCount != expectedKeys) { throw new ConcurrentModificationException(); }
            }
        }

        @Override public Spliterator<K> spliterator() {
            return Spliterators.spliterator(this, 0);
        }

        @Override public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return keySet().removeAll(c instanceof Multiset<?> m ? m.elementSet() : c);
        }

        @Override public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return keySet().retainAll(c instanceof Multiset<?> m ? m.elementSet() : c);
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public boolean equals(Object other) {
            if (other == this) { return true; }
            if (!(other instanceof Multiset<?> that) || that.size() != size || that.entrySet().size() != keyCount) {
                return false;
            }
            for (Multiset.Entry<?> e : that.entrySet()) {
                if (count(e.getElement()) != e.getCount()) { return false; }
            }
            return true;
        }

        @Override public int hashCode() {
            int h = 0;
            for (int s = first; s >= 0; s = after[s]) { h += Objects.hashCode(keys[s]) ^ counts[s]; }
            return h;
        }

        @Override public String toString() {
            StringBuilder b = new StringBuilder("[");
            for (int s = first; s >= 0; s = after[s]) {
                if (s != first) { b.append(", "); }
                b.append(keys[s]);
                if (counts[s] != 1) { b.append(" x ").append(counts[s]); }
            }
            return b.append(']').toString();
        }
    }

    private final class CountEntries extends AbstractSet<Multiset.Entry<K>> {
        @Override public int size() {
            return keyCount;
        }

        @Override public boolean isEmpty() {
            return keyCount == 0;
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof Multiset.Entry<?> e)) { return false; }
            int s = find(e.getElement());
            return s >= 0 && counts[s] == e.getCount();
        }

        @Override public boolean remove(Object o) {
            if (!contains(o)) { return false; }
            eraseKey(find(((Multiset.Entry<?>) o).getElement()));
            return true;
        }

        @Override public void clear() {
            M3ArrayListMultimap.this.clear();
        }

        @Override public Iterator<Multiset.Entry<K>> iterator() {
            Iterator<K> keyIterator = new KeyCursor<>(false);
            return new Iterator<>() {
                @Override public boolean hasNext() {
                    return keyIterator.hasNext();
                }

                @Override public Multiset.Entry<K> next() {
                    return new CountView(keyIterator.next());
                }

                @Override public void remove() {
                    keyIterator.remove();
                }
            };
        }
    }

    /** Boundary flyweight for one keys() entry; its count reads the live key segment length. */
    private final class CountView implements Multiset.Entry<K> {
        private final K element;

        CountView(K element) {
            this.element = element;
        }

        @Override public K getElement() {
            return element;
        }

        @Override public int getCount() {
            int s = find(element);
            return s < 0 ? 0 : counts[s];
        }

        @Override public boolean equals(Object other) {
            return other instanceof Multiset.Entry<?> e && getCount() == e.getCount() && Objects.equals(element, e.getElement());
        }

        @Override public int hashCode() {
            return Objects.hashCode(element) ^ getCount();
        }

        @Override public String toString() {
            int n = getCount();
            return n == 1 ? String.valueOf(element) : element + " x " + n;
        }
    }
}
