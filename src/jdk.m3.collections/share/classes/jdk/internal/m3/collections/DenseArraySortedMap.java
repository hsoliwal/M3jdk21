/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Mutable, flat-array navigable map. Lookup is O(log n); insertion/removal O(n).
 *
 * <p>All views share the same table. No entry object is retained per mapping.
 * Keys are non-null; values may be null. Ordering must be stable and consistent
 * with equals when ordinary Map equality is required. This class is not thread-safe.
 * Navigational entries are snapshots; iterator entries write through while present.
 * Callbacks must not mutate the map or ordering-relevant key state.
 * Key/value/entry spliterators bind on first traversal, split or size query; each
 * split is an index range over this table, not a buffered element snapshot.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseArraySortedMap.java}
 * blob {@code 6af3c3745225107deb85587cbbb1410d9d1f19b4} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseArraySortedMap<K, V> extends AbstractMap<K, V>
        implements NavigableMap<K, V> {
    private static final Object PRESENT = new Object();

    /** One table-level owner, not one record per mapping. */
    private static final class Table<K> {
        private Object[] keys = new Object[0];
        private Object[] values;
        private final Comparator<? super K> comparator;
        private final boolean keyOnly;
        private int size;
        private int modifications;
        private long version;

        private Table(Comparator<? super K> comparator, boolean keyOnly) {
            this.comparator = comparator;
            this.keyOnly = keyOnly;
            this.values = keyOnly ? null : new Object[0];
        }
    }

    private final Table<K> table;
    private final K low;
    private final K high;
    private final boolean hasLow;
    private final boolean lowInclusive;
    private final boolean hasHigh;
    private final boolean highInclusive;
    private final boolean descending;

    public DenseArraySortedMap() {
        this(null);
    }

    public DenseArraySortedMap(Comparator<? super K> comparator) {
        this(new Table<>(comparator, false), null, false, false, null, false, false, false);
    }

    private DenseArraySortedMap(Table<K> table, K low, boolean hasLow, boolean lowInclusive,
            K high, boolean hasHigh, boolean highInclusive, boolean descending) {
        this.table = table;
        this.low = low;
        this.hasLow = hasLow;
        this.lowInclusive = lowInclusive;
        this.high = high;
        this.hasHigh = hasHigh;
        this.highInclusive = highInclusive;
        this.descending = descending;
    }

    static <E> DenseArraySortedMap<E, Object> keyOnly(Comparator<? super E> comparator) {
        return new DenseArraySortedMap<>(new Table<>(comparator, true),
                null, false, false, null, false, false, false);
    }

    boolean addKey(K key) {
        if (!table.keyOnly) {
            throw new UnsupportedOperationException("map key-set insertion");
        }
        boolean absent = !containsKey(key);
        @SuppressWarnings("unchecked") V marker = (V) PRESENT;
        put(key, marker);
        return absent;
    }

    @SuppressWarnings("unchecked")
    private K keyAt(int index) {
        return (K) table.keys[index];
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int index) {
        return (V) (table.keyOnly ? PRESENT : table.values[index]);
    }

    @SuppressWarnings("unchecked")
    private int compare(K left, K right) {
        Objects.requireNonNull(left, "key");
        Objects.requireNonNull(right, "key");
        return table.comparator == null
                ? ((Comparable<? super K>) left).compareTo(right)
                : table.comparator.compare(left, right);
    }

    @SuppressWarnings("unchecked")
    private int compareObjects(Object left, Object right) {
        return compare((K) left, (K) right);
    }

    @SuppressWarnings("unchecked")
    private K checkedKey(Object key) {
        K result = (K) Objects.requireNonNull(key, "key");
        compare(result, result);
        return result;
    }

    private int lowerBound(K key) {
        int left = 0;
        int right = table.size;
        while (left < right) {
            int middle = (left + right) >>> 1;
            if (compare(keyAt(middle), key) < 0) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }
        return left;
    }

    private int upperBound(K key) {
        int left = 0;
        int right = table.size;
        while (left < right) {
            int middle = (left + right) >>> 1;
            if (compare(keyAt(middle), key) <= 0) {
                left = middle + 1;
            } else {
                right = middle;
            }
        }
        return left;
    }

    private int start() {
        return !hasLow ? 0 : lowInclusive ? lowerBound(low) : upperBound(low);
    }

    private int end() {
        return !hasHigh ? table.size : highInclusive ? upperBound(high) : lowerBound(high);
    }

    private boolean inRange(K key) {
        if (hasLow) {
            int comparison = compare(key, low);
            if (comparison < 0 || comparison == 0 && !lowInclusive) {
                return false;
            }
        }
        if (hasHigh) {
            int comparison = compare(key, high);
            if (comparison > 0 || comparison == 0 && !highInclusive) {
                return false;
            }
        }
        return true;
    }

    private void checkEndpoint(K key, boolean inclusive) {
        checkedKey(key);
        if (hasLow) {
            int comparison = compare(key, low);
            if (comparison < 0 || comparison == 0 && inclusive && !lowInclusive) {
                throw new IllegalArgumentException("endpoint below view");
            }
        }
        if (hasHigh) {
            int comparison = compare(key, high);
            if (comparison > 0 || comparison == 0 && inclusive && !highInclusive) {
                throw new IllegalArgumentException("endpoint above view");
            }
        }
    }

    private int indexOf(K key) {
        if (!inRange(key)) {
            return -1;
        }
        int index = lowerBound(key);
        return index < table.size && compare(keyAt(index), key) == 0 ? index : -1;
    }

    @Override public Comparator<? super K> comparator() {
        return descending ? Collections.reverseOrder(table.comparator) : table.comparator;
    }

    @Override public int size() {
        return Math.max(0, end() - start());
    }

    @Override public boolean containsKey(Object key) {
        return indexOf(checkedKey(key)) >= 0;
    }

    @Override public V get(Object key) {
        int index = indexOf(checkedKey(key));
        return index < 0 ? null : valueAt(index);
    }

    @Override public V getOrDefault(Object key, V defaultValue) {
        int index = indexOf(checkedKey(key));
        return index < 0 ? defaultValue : valueAt(index);
    }

    @Override public boolean containsValue(Object value) {
        for (int i = start(), limit = end(); i < limit; i++) {
            if (Objects.equals(valueAt(i), value)) {
                return true;
            }
        }
        return false;
    }

    @Override public V put(K key, V value) {
        checkedKey(key);
        if (!inRange(key)) {
            throw new IllegalArgumentException("key outside view");
        }
        int index = lowerBound(key);
        if (index < table.size && compare(keyAt(index), key) == 0) {
            V previous = valueAt(index);
            if (!table.keyOnly) {
                table.values[index] = value;
            }
            table.version++;
            return previous;
        }
        int required = DenseArrayOrder.sum(table.size, 1);
        if (required > table.keys.length) {
            int capacity = DenseArrayOrder.capacity(table.keys.length, required);
            Object[] keys = Arrays.copyOf(table.keys, capacity);
            Object[] values = table.keyOnly ? null : Arrays.copyOf(table.values, capacity);
            table.keys = keys;
            table.values = values;
        }
        System.arraycopy(table.keys, index, table.keys, index + 1, table.size - index);
        table.keys[index] = key;
        if (!table.keyOnly) {
            System.arraycopy(table.values, index, table.values, index + 1, table.size - index);
            table.values[index] = value;
        }
        table.size++;
        table.modifications++;
        table.version++;
        return null;
    }

    private V removeAt(int index) {
        V previous = valueAt(index);
        int moved = table.size - index - 1;
        System.arraycopy(table.keys, index + 1, table.keys, index, moved);
        table.keys[table.size - 1] = null;
        if (!table.keyOnly) {
            System.arraycopy(table.values, index + 1, table.values, index, moved);
            table.values[table.size - 1] = null;
        }
        table.size--;
        table.modifications++;
        table.version++;
        return previous;
    }

    @Override public V remove(Object key) {
        int index = indexOf(checkedKey(key));
        return index < 0 ? null : removeAt(index);
    }

    boolean removeKey(Object key) {
        int index = indexOf(checkedKey(key));
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    @Override public void clear() {
        int first = start();
        int limit = end();
        if (first >= limit) {
            return;
        }
        int removed = limit - first;
        System.arraycopy(table.keys, limit, table.keys, first, table.size - limit);
        Arrays.fill(table.keys, table.size - removed, table.size, null);
        if (!table.keyOnly) {
            System.arraycopy(table.values, limit, table.values, first, table.size - limit);
            Arrays.fill(table.values, table.size - removed, table.size, null);
        }
        table.size -= removed;
        table.modifications++;
        table.version++;
    }

    /** Sequential standard Map semantics; use putAllAtomic for a staged batch. */
    @Override public void putAll(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        source.forEach(this::put);
    }

    /**
     * Stage once, stable-sort once, merge once; failed validation/comparison does
     * not publish this batch. Does not undo mutations performed by user callbacks.
     */
    public void putAllAtomic(Map<? extends K, ? extends V> source) {
        putAllFilteredAtomic(source, (key, value) -> true);
    }

    /** Returns accepted source mappings, not the number of newly distinct keys. */
    public int putAllFilteredAtomic(Map<? extends K, ? extends V> source,
            BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        long version = table.version;
        // One staging owner per batch, never one entry/tuple per mapping.
        class Batch implements BiConsumer<K, V> {
            private Object[] keys = new Object[Math.max(0, source.size())];
            private Object[] values = table.keyOnly ? null : new Object[keys.length];
            private int count;

            @Override public void accept(K incomingKey, V value) {
                K key = checkedKey(incomingKey);
                if (!predicate.test(key, value)) {
                    return;
                }
                if (!inRange(key)) {
                    throw new IllegalArgumentException("key outside view");
                }
                if (count == keys.length) {
                    int capacity = DenseArrayOrder.capacity(keys.length, DenseArrayOrder.sum(count, 1));
                    keys = Arrays.copyOf(keys, capacity);
                    if (values != null) {
                        values = Arrays.copyOf(values, capacity);
                    }
                }
                keys[count] = key;
                if (values != null) {
                    values[count] = value;
                }
                count++;
            }
        }
        Batch batch = new Batch();
        source.forEach(batch);
        mergeBatch(batch.keys, batch.values, batch.count, version);
        return batch.count;
    }

    boolean addKeysAtomic(Collection<? extends K> source) {
        if (!table.keyOnly) {
            throw new UnsupportedOperationException("map key-set insertion");
        }
        long version = table.version;
        Object[] keys = Objects.requireNonNull(source, "source").toArray();
        for (Object key : keys) {
            if (!inRange(checkedKey(key))) {
                throw new IllegalArgumentException("key outside view");
            }
        }
        int previous = table.size;
        mergeBatch(keys, null, keys.length, version);
        return previous != table.size;
    }

    private void mergeBatch(Object[] keys, Object[] values, int count, long version) {
        // No accepted proposals: preserve callback interference detection without
        // sorting, allocating replacement lanes, or copying the existing table.
        if (count == 0) {
            if (table.version != version) {
                throw new ConcurrentModificationException("mutation during batch callback");
            }
            return;
        }
        int[] order = DenseArrayOrder.order(keys, count, this::compareObjects);
        Object[] uniqueKeys = new Object[count];
        Object[] uniqueValues = table.keyOnly ? null : new Object[count];
        int unique = 0;
        for (int index : order) {
            if (unique == 0 || compareObjects(uniqueKeys[unique - 1], keys[index]) != 0) {
                uniqueKeys[unique++] = keys[index];
            }
            if (uniqueValues != null) {
                uniqueValues[unique - 1] = values[index];
            }
        }
        int capacity = DenseArrayOrder.sum(table.size, unique);
        Object[] mergedKeys = new Object[capacity];
        Object[] mergedValues = table.keyOnly ? null : new Object[capacity];
        int old = 0;
        int incoming = 0;
        int out = 0;
        while (old < table.size || incoming < unique) {
            int comparison = old == table.size ? 1 : incoming == unique ? -1
                    : compareObjects(table.keys[old], uniqueKeys[incoming]);
            if (comparison < 0) {
                mergedKeys[out] = table.keys[old];
                if (mergedValues != null) {
                    mergedValues[out] = table.values[old];
                }
                old++;
            } else {
                mergedKeys[out] = comparison == 0 ? table.keys[old++] : uniqueKeys[incoming];
                if (mergedValues != null) {
                    mergedValues[out] = uniqueValues[incoming];
                }
                incoming++;
            }
            out++;
        }
        if (table.version != version) {
            throw new ConcurrentModificationException("mutation during batch callback");
        }
        if (count == 0) {
            return;
        }
        if (table.size != out) {
            table.modifications++;
        }
        table.keys = mergedKeys;
        table.values = mergedValues;
        table.size = out;
        table.version++;
    }

    private Entry<K, V> snapshot(int index) {
        return index < start() || index >= end() ? null
                : new SimpleImmutableEntry<>(keyAt(index), valueAt(index));
    }

    private int seekIndex(K key, int relation) {
        checkedKey(key);
        int direction = descending ? -relation : relation;
        int index;
        if (direction < 0) {
            index = (direction == -2 ? lowerBound(key) : upperBound(key)) - 1;
            index = Math.min(index, end() - 1);
        } else {
            index = direction == 1 ? lowerBound(key) : upperBound(key);
            index = Math.max(index, start());
        }
        return index < start() || index >= end() ? -1 : index;
    }

    private K keyOrNull(int index) {
        return index < 0 ? null : keyAt(index);
    }

    @Override public Entry<K, V> lowerEntry(K key) { return snapshot(seekIndex(key, -2)); }
    @Override public Entry<K, V> floorEntry(K key) { return snapshot(seekIndex(key, -1)); }
    @Override public Entry<K, V> ceilingEntry(K key) { return snapshot(seekIndex(key, 1)); }
    @Override public Entry<K, V> higherEntry(K key) { return snapshot(seekIndex(key, 2)); }
    @Override public K lowerKey(K key) { return keyOrNull(seekIndex(key, -2)); }
    @Override public K floorKey(K key) { return keyOrNull(seekIndex(key, -1)); }
    @Override public K ceilingKey(K key) { return keyOrNull(seekIndex(key, 1)); }
    @Override public K higherKey(K key) { return keyOrNull(seekIndex(key, 2)); }
    @Override public Entry<K, V> firstEntry() { return snapshot(descending ? end() - 1 : start()); }
    @Override public Entry<K, V> lastEntry() { return snapshot(descending ? start() : end() - 1); }

    @Override public K firstKey() {
        if (isEmpty()) { throw new NoSuchElementException(); }
        return keyAt(descending ? end() - 1 : start());
    }

    @Override public K lastKey() {
        if (isEmpty()) { throw new NoSuchElementException(); }
        return keyAt(descending ? start() : end() - 1);
    }

    K pollKey(boolean first) {
        if (isEmpty()) { return null; }
        int index = first != descending ? start() : end() - 1;
        K key = keyAt(index);
        removeAt(index);
        return key;
    }

    @Override public Entry<K, V> pollFirstEntry() {
        Entry<K, V> entry = firstEntry();
        if (entry != null) { removeAt(descending ? end() - 1 : start()); }
        return entry;
    }

    @Override public Entry<K, V> pollLastEntry() {
        Entry<K, V> entry = lastEntry();
        if (entry != null) { removeAt(descending ? start() : end() - 1); }
        return entry;
    }

    @Override public DenseArraySortedMap<K, V> descendingMap() {
        return new DenseArraySortedMap<>(table, low, hasLow, lowInclusive,
                high, hasHigh, highInclusive, !descending);
    }

    @Override public DenseArraySortedMap<K, V> subMap(K from, boolean fromInclusive,
            K to, boolean toInclusive) {
        checkEndpoint(from, fromInclusive);
        checkEndpoint(to, toInclusive);
        int comparison = compare(from, to);
        if (descending ? comparison < 0 : comparison > 0) {
            throw new IllegalArgumentException("reversed endpoints");
        }
        return descending
                ? new DenseArraySortedMap<>(table, to, true, toInclusive,
                        from, true, fromInclusive, true)
                : new DenseArraySortedMap<>(table, from, true, fromInclusive,
                        to, true, toInclusive, false);
    }

    @Override public DenseArraySortedMap<K, V> headMap(K to, boolean inclusive) {
        checkEndpoint(to, inclusive);
        return descending
                ? new DenseArraySortedMap<>(table, to, true, inclusive,
                        high, hasHigh, highInclusive, true)
                : new DenseArraySortedMap<>(table, low, hasLow, lowInclusive,
                        to, true, inclusive, false);
    }

    @Override public DenseArraySortedMap<K, V> tailMap(K from, boolean inclusive) {
        checkEndpoint(from, inclusive);
        return descending
                ? new DenseArraySortedMap<>(table, low, hasLow, lowInclusive,
                        from, true, inclusive, true)
                : new DenseArraySortedMap<>(table, from, true, inclusive,
                        high, hasHigh, highInclusive, false);
    }

    @Override public SortedMap<K, V> subMap(K from, K to) { return subMap(from, true, to, false); }
    @Override public SortedMap<K, V> headMap(K to) { return headMap(to, false); }
    @Override public SortedMap<K, V> tailMap(K from) { return tailMap(from, true); }
    @Override public NavigableSet<K> navigableKeySet() { return new DenseArraySortedSet<>(this, false); }
    @Override public NavigableSet<K> keySet() { return navigableKeySet(); }
    @Override public NavigableSet<K> descendingKeySet() { return descendingMap().navigableKeySet(); }

    private abstract class ArrayIterator<T> implements Iterator<T> {
        private int expected = table.modifications;
        private int next = descending ? end() - 1 : start();
        // Boundaries are nonnegative table indexes. Complement the boundary when
        // remove is not permitted; this reuses the former last-index field, not
        // another field or per-entry state. The last index follows from next.
        private int fence = ~(descending ? start() : end());

        private void check() {
            if (expected != table.modifications) { throw new ConcurrentModificationException(); }
        }

        @Override public boolean hasNext() {
            check();
            int bound = fence < 0 ? ~fence : fence;
            return descending ? next >= bound : next < bound;
        }

        @Override public T next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            int index = next;
            next += descending ? -1 : 1;
            if (fence < 0) { fence = ~fence; }
            return project(index);
        }

        abstract T project(int index);

        @Override public void remove() {
            check();
            if (fence < 0) { throw new IllegalStateException(); }
            removeAt(next + (descending ? 1 : -1));
            if (!descending) {
                next--;
                fence--;
            }
            fence = ~fence;
            expected = table.modifications;
        }
    }

    Iterator<K> keyIterator() {
        return new ArrayIterator<>() {
            @Override K project(int index) { return keyAt(index); }
        };
    }

    /** Late-bound direct lane traversal; splitting allocates no element buffer. */
    private final class ArraySpliterator<T> implements Spliterator<T> {
        private final int lane;
        private int first;
        private int limit = -1;
        private int expected;

        private ArraySpliterator(int lane) { this.lane = lane; }

        private ArraySpliterator(int lane, int first, int limit, int expected) {
            this.lane = lane;
            this.first = first;
            this.limit = limit;
            this.expected = expected;
        }

        private void bind() {
            if (limit < 0) {
                first = start();
                limit = Math.max(first, end());
                expected = table.modifications;
            }
        }

        private void check() {
            if (expected != table.modifications) { throw new ConcurrentModificationException(); }
        }

        @SuppressWarnings("unchecked")
        private T project(int index) {
            return (T) (lane == 0 ? keyAt(index) : lane == 1 ? valueAt(index)
                    : new LiveEntry(keyAt(index)));
        }

        @Override public Spliterator<T> trySplit() {
            bind();
            check();
            int middle = first + ((limit - first) >>> 1);
            if (middle == first) { return null; }
            ArraySpliterator<T> prefix = descending
                    ? new ArraySpliterator<>(lane, middle, limit, expected)
                    : new ArraySpliterator<>(lane, first, middle, expected);
            if (descending) { limit = middle; } else { first = middle; }
            return prefix;
        }

        @Override public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            bind();
            check();
            if (first == limit) { return false; }
            T value = project(descending ? --limit : first++);
            action.accept(value);
            check();
            return true;
        }

        @Override public long estimateSize() {
            bind();
            return limit - first;
        }

        @Override public int characteristics() {
            return ORDERED | SIZED | SUBSIZED
                    | (lane == 1 ? 0 : DISTINCT | SORTED | NONNULL);
        }

        @Override @SuppressWarnings("unchecked")
        public Comparator<? super T> getComparator() {
            if (lane == 1) { throw new IllegalStateException(); }
            if (lane == 0) { return (Comparator<? super T>) comparator(); }
            return (left, right) -> {
                K a = ((Entry<K, V>) left).getKey();
                K b = ((Entry<K, V>) right).getKey();
                return descending ? compare(b, a) : compare(a, b);
            };
        }
    }

    Spliterator<K> keySpliterator() { return new ArraySpliterator<>(0); }

    @Override public Collection<V> values() {
        return new AbstractCollection<>() {
            @Override public int size() { return DenseArraySortedMap.this.size(); }
            @Override public void clear() { DenseArraySortedMap.this.clear(); }
            @Override public Spliterator<V> spliterator() { return new ArraySpliterator<>(1); }
            @Override public Iterator<V> iterator() {
                return new ArrayIterator<>() {
                    @Override V project(int index) { return valueAt(index); }
                };
            }
        };
    }

    private final class LiveEntry implements Entry<K, V> {
        private final K key;
        private LiveEntry(K key) { this.key = key; }
        @Override public K getKey() { return key; }
        @Override public V getValue() { return DenseArraySortedMap.this.get(key); }
        @Override public V setValue(V value) {
            if (!containsKey(key)) { throw new IllegalStateException("entry removed"); }
            return put(key, value);
        }
        @Override public int hashCode() { return Objects.hashCode(key) ^ Objects.hashCode(getValue()); }
        @Override public boolean equals(Object other) {
            return other instanceof Entry<?, ?> entry && Objects.equals(key, entry.getKey())
                    && Objects.equals(getValue(), entry.getValue());
        }
        @Override public String toString() { return key + "=" + getValue(); }
    }

    @Override public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return DenseArraySortedMap.this.size(); }
            @Override public void clear() { DenseArraySortedMap.this.clear(); }
            @Override public Spliterator<Entry<K, V>> spliterator() { return new ArraySpliterator<>(2); }
            @Override public Iterator<Entry<K, V>> iterator() {
                return new ArrayIterator<>() {
                    @Override Entry<K, V> project(int index) { return new LiveEntry(keyAt(index)); }
                };
            }
            @Override public boolean contains(Object other) {
                if (!(other instanceof Entry<?, ?> entry)) { return false; }
                try {
                    int index = indexOf(checkedKey(entry.getKey()));
                    return index >= 0 && Objects.equals(valueAt(index), entry.getValue());
                } catch (ClassCastException | NullPointerException incompatible) {
                    return false;
                }
            }
            @Override public boolean remove(Object other) {
                if (!contains(other)) { return false; }
                Entry<?, ?> entry = (Entry<?, ?>) other;
                return removeKey(entry.getKey());
            }
        };
    }

    /** Direct lane update in this view's encounter order; no temporary entries. */
    @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function, "function");
        int expected = table.modifications;
        int first = start();
        int limit = end();
        for (int i = descending ? limit - 1 : first;
                descending ? i >= first : i < limit; i += descending ? -1 : 1) {
            V value = function.apply(keyAt(i), valueAt(i));
            if (expected != table.modifications) { throw new ConcurrentModificationException(); }
            if (!table.keyOnly) { table.values[i] = value; }
            table.version++;
        }
    }

    /** Map equality over the lanes; entry views remain explicit compatibility APIs. */
    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Map<?, ?> map) || map.size() != size()) { return false; }
        int expected = table.modifications;
        int first = start();
        int limit = end();
        try {
            for (int i = descending ? limit - 1 : first;
                    descending ? i >= first : i < limit; i += descending ? -1 : 1) {
                K key = keyAt(i);
                V value = valueAt(i);
                if (!Objects.equals(value, map.get(key)) || value == null && !map.containsKey(key)) {
                    return false;
                }
                if (expected != table.modifications) { throw new ConcurrentModificationException(); }
            }
        } catch (ClassCastException | NullPointerException incompatible) {
            return false;
        }
        return true;
    }

    /** Standard Map hash without materializing entries. */
    @Override public int hashCode() {
        int hash = 0;
        int expected = table.modifications;
        int first = start();
        int limit = end();
        for (int i = descending ? limit - 1 : first;
                descending ? i >= first : i < limit; i += descending ? -1 : 1) {
            hash += Objects.hashCode(keyAt(i)) ^ Objects.hashCode(valueAt(i));
            if (expected != table.modifications) { throw new ConcurrentModificationException(); }
        }
        return hash;
    }

    /** Standard Map text in view order, including the self-reference marker. */
    @Override public String toString() {
        StringBuilder text = new StringBuilder("{");
        int expected = table.modifications;
        int first = start();
        int limit = end();
        for (int i = descending ? limit - 1 : first;
                descending ? i >= first : i < limit; i += descending ? -1 : 1) {
            if (text.length() > 1) { text.append(", "); }
            K key = keyAt(i);
            V value = valueAt(i);
            text.append(key == this ? "(this Map)" : key).append('=')
                    .append(value == this ? "(this Map)" : value);
            if (expected != table.modifications) { throw new ConcurrentModificationException(); }
        }
        return text.append('}').toString();
    }

    /** Direct key/value traversal; no compatibility Entry allocation per mapping. */
    @Override public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action, "action");
        int expected = table.modifications;
        int first = start();
        int limit = end();
        for (int i = descending ? limit - 1 : first;
                descending ? i >= first : i < limit; i += descending ? -1 : 1) {
            action.accept(keyAt(i), valueAt(i));
            if (expected != table.modifications) { throw new ConcurrentModificationException(); }
        }
    }
}