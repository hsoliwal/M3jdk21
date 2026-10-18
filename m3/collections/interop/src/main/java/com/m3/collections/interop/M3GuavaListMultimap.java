/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Source-first M3/OpenRewrite candidate. Guava API references are Apache-2.0;
 * this implementation is original code and does not copy a donor method body.
 */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Direct Guava {@link ListMultimap} backed by M3's indexed array/offset collection owners.
 *
 * <p>One M3LinkedHashMap owns a key directory, each key owns one M3LinkedList of values.
 * No retained mapping-level Entry or Node class and no duplicate Guava multimap engine.
 * Keys are kept in insertion order, and all values (including null and duplicates) are
 * retained in encounter order. Live list, asMap, entries, values, keySet and counted-key
 * views mutate the same array/index backing; removing a last value releases its key.
 * This is not concurrent and does not claim Guava concrete-class or wire identity.</p>
 *
 * <p>Index and state-mask projection uses the existing M3IndexPathMasks Java/C17 JNI
 * selector with full 32-bit state words. JNI receives primitive indexes and masks only:
 * Java references, user predicates and comparators remain in Java. The selection result
 * is an independent M3 owner; it is not a live filtered predicate view.</p>
 */
public final class M3GuavaListMultimap<K, V> implements ListMultimap<K, V> {
    private final Map<K, List<V>> directory = new M3LinkedHashMap<>();
    private int pairs;
    private long revision;
    private transient Map<K, Collection<V>> asMap;
    private transient Set<K> keys;
    private transient Multiset<K> countedKeys;
    private transient Collection<Map.Entry<K, V>> entries;
    private transient Collection<V> values;

    public M3GuavaListMultimap() { }

    @Override public int size() { return pairs; }
    @Override public boolean isEmpty() { return pairs == 0; }
    @Override public boolean containsKey(Object key) { return directory.containsKey(key); }
    @Override public boolean containsValue(Object value) {
        for (List<V> list : directory.values()) if (list.contains(value)) return true;
        return false;
    }
    @Override public boolean containsEntry(Object key, Object value) {
        List<V> list = directory.get(key);
        return list != null && list.contains(value);
    }

    @Override public boolean put(K key, V value) { get(key).add(value); return true; }
    @Override public boolean remove(Object key, Object value) {
        List<V> list = directory.get(key);
        if (list == null) return false;
        int index = list.indexOf(value);
        if (index < 0) return false;
        get(castKey(key)).remove(index);
        return true;
    }
    @SuppressWarnings("unchecked") private K castKey(Object key) { return (K) key; }

    @Override public boolean putAll(K key, Iterable<? extends V> input) {
        Objects.requireNonNull(input, "values");
        ArrayList<V> staged = new ArrayList<>();
        for (V value : input) staged.add(value);
        return get(key).addAll(staged);
    }
    @Override public boolean putAll(Multimap<? extends K, ? extends V> input) {
        Objects.requireNonNull(input, "multimap");
        ArrayList<Map.Entry<K, V>> staged = new ArrayList<>();
        input.entries().forEach(entry -> staged.add(
                new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue())));
        for (Map.Entry<K, V> entry : staged) put(entry.getKey(), entry.getValue());
        return !staged.isEmpty();
    }
    @Override public List<V> get(K key) { return new ValueView(key); }
    @Override public List<V> removeAll(Object key) {
        List<V> removed = directory.remove(key);
        if (removed == null) return Collections.emptyList();
        List<V> copy = detached(removed);
        pairs -= removed.size();
        revision++;
        return copy;
    }
    @Override public List<V> replaceValues(K key, Iterable<? extends V> values) {
        Objects.requireNonNull(values, "values");
        ArrayList<V> staged = new ArrayList<>();
        for (V value : values) staged.add(value);
        List<V> existing = directory.get(key);
        List<V> old = existing == null ? Collections.emptyList() : detached(existing);
        if (existing == null) {
            if (!staged.isEmpty()) putAll(key, staged);
        } else {
            int before = existing.size();
            if (staged.isEmpty()) directory.remove(key);
            else { existing.clear(); existing.addAll(staged); }
            pairs += staged.size() - before;
            revision++;
        }
        return old;
    }
    @Override public void clear() {
        if (directory.isEmpty()) return;
        directory.clear();
        pairs = 0;
        revision++;
    }

    private static <V> List<V> detached(Collection<? extends V> source) {
        M3LinkedList<V> copy = new M3LinkedList<>();
        copy.addAll(source);
        return Collections.unmodifiableList(copy);
    }

    /** Live view: even an absent key becomes present on first valid addition. */
    private final class ValueView extends AbstractList<V> {
        private final K key;
        private ValueView(K key) { this.key = key; }
        private List<V> list() { return directory.get(key); }
        @Override public int size() { List<V> list = list(); return list == null ? 0 : list.size(); }
        @Override public V get(int index) {
            List<V> list = list();
            if (list == null) throw new IndexOutOfBoundsException(index);
            return list.get(index);
        }
        @Override public V set(int index, V element) {
            List<V> list = list();
            if (list == null) throw new IndexOutOfBoundsException(index);
            V previous = list.set(index, element);
            if (!Objects.equals(previous, element)) revision++;
            return previous;
        }
        @Override public void add(int index, V element) {
            List<V> list = list();
            if (list == null) {
                if (index != 0) throw new IndexOutOfBoundsException(index);
                M3LinkedList<V> created = new M3LinkedList<>();
                created.add(element);
                directory.put(key, created);
            } else list.add(index, element);
            pairs++;
            revision++;
            modCount++;
        }
        @Override public boolean addAll(int index, Collection<? extends V> elements) {
            Objects.requireNonNull(elements, "elements");
            ArrayList<V> staged = new ArrayList<>(elements);
            List<V> list = list();
            int size = list == null ? 0 : list.size();
            Objects.checkIndex(index, size + 1);
            if (staged.isEmpty()) return false;
            if (list == null) {
                M3LinkedList<V> created = new M3LinkedList<>();
                created.addAll(staged);
                directory.put(key, created);
            } else list.addAll(index, staged);
            pairs += staged.size();
            revision++;
            modCount++;
            return true;
        }
        @Override public V remove(int index) {
            List<V> list = list();
            if (list == null) throw new IndexOutOfBoundsException(index);
            V removed = list.remove(index);
            pairs--;
            if (list.isEmpty()) directory.remove(key);
            revision++;
            modCount++;
            return removed;
        }
        @Override public void clear() { if (list() != null) { M3GuavaListMultimap.this.removeAll(key); modCount++; } }
    }

    @Override public Set<K> keySet() {
        if (keys == null) keys = new AbstractSet<>() {
            @Override public int size() { return directory.size(); }
            @Override public boolean contains(Object key) { return directory.containsKey(key); }
            @Override public boolean remove(Object key) {
                if (!contains(key)) return false;
                M3GuavaListMultimap.this.removeAll(key);
                return true;
            }
            @Override public void clear() { M3GuavaListMultimap.this.clear(); }
            @Override public Iterator<K> iterator() {
                Iterator<Map.Entry<K, List<V>>> delegate = directory.entrySet().iterator();
                return new Iterator<>() {
                    private Map.Entry<K, List<V>> last;
                    @Override public boolean hasNext() { return delegate.hasNext(); }
                    @Override public K next() { last = delegate.next(); return last.getKey(); }
                    @Override public void remove() {
                        if (last == null) throw new IllegalStateException();
                        pairs -= last.getValue().size();
                        delegate.remove();
                        last = null;
                        revision++;
                    }
                };
            }
        };
        return keys;
    }

    @Override public Collection<Map.Entry<K, V>> entries() {
        if (entries == null) entries = new AbstractCollection<>() {
            @Override public int size() { return pairs; }
            @Override public void clear() { M3GuavaListMultimap.this.clear(); }
            @Override public Iterator<Map.Entry<K, V>> iterator() { return new PairCursor(); }
            @Override public boolean contains(Object o) {
                return o instanceof Map.Entry<?, ?> entry
                        && containsEntry(entry.getKey(), entry.getValue());
            }
            @Override public boolean remove(Object o) {
                return o instanceof Map.Entry<?, ?> entry
                        && M3GuavaListMultimap.this.remove(entry.getKey(), entry.getValue());
            }
        };
        return entries;
    }

    /** O(total occurrences) sequential iterator; no per-pair retained Entry/Node. */
    private final class PairCursor implements Iterator<Map.Entry<K, V>> {
        private final List<K> encounterKeys = new ArrayList<>(directory.keySet());
        private int group;
        private Iterator<V> cursor = Collections.emptyIterator();
        private Iterator<V> removableCursor;
        private K activeKey;
        private K removableKey;
        private long expected = revision;
        private boolean removable;
        private void check() { if (expected != revision) throw new ConcurrentModificationException(); }
        private boolean seek() {
            check();
            if (cursor.hasNext()) return true;
            while (group < encounterKeys.size()) {
                activeKey = encounterKeys.get(group++);
                List<V> list = directory.get(activeKey);
                cursor = list == null ? Collections.emptyIterator() : list.iterator();
                if (cursor.hasNext()) return true;
            }
            return false;
        }
        @Override public boolean hasNext() { return seek(); }
        @Override public Map.Entry<K, V> next() {
            if (!seek()) throw new NoSuchElementException();
            V value = cursor.next();
            removableKey = activeKey;
            removableCursor = cursor;
            removable = true;
            return new AbstractMap.SimpleImmutableEntry<>(activeKey, value);
        }
        @Override public void remove() {
            check();
            if (!removable) throw new IllegalStateException();
            removableCursor.remove();
            pairs--;
            if (directory.get(removableKey).isEmpty()) directory.remove(removableKey);
            revision++;
            expected = revision;
            removable = false;
        }
    }

    @Override public Collection<V> values() {
        if (values == null) values = new AbstractCollection<>() {
            @Override public int size() { return pairs; }
            @Override public void clear() { M3GuavaListMultimap.this.clear(); }
            @Override public Iterator<V> iterator() {
                Iterator<Map.Entry<K, V>> cursor = entries().iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { return cursor.hasNext(); }
                    @Override public V next() { return cursor.next().getValue(); }
                    @Override public void remove() { cursor.remove(); }
                };
            }
        };
        return values;
    }

    @Override public Map<K, Collection<V>> asMap() {
        if (asMap == null) asMap = new AbstractMap<>() {
            @Override public int size() { return directory.size(); }
            @Override public boolean containsKey(Object key) { return directory.containsKey(key); }
            @Override public Collection<V> get(Object key) {
                return containsKey(key) ? M3GuavaListMultimap.this.get(castKey(key)) : null;
            }
            @Override public Collection<V> remove(Object key) {
                return containsKey(key) ? M3GuavaListMultimap.this.removeAll(key) : null;
            }
            @Override public void clear() { M3GuavaListMultimap.this.clear(); }
            @Override public Set<Map.Entry<K, Collection<V>>> entrySet() {
                return new AbstractSet<>() {
                    @Override public int size() { return directory.size(); }
                    @Override public void clear() { M3GuavaListMultimap.this.clear(); }
                    @Override public Iterator<Map.Entry<K, Collection<V>>> iterator() {
                        Iterator<K> keys = M3GuavaListMultimap.this.keySet().iterator();
                        return new Iterator<>() {
                            @Override public boolean hasNext() { return keys.hasNext(); }
                            @Override public Map.Entry<K, Collection<V>> next() {
                                K key = keys.next();
                                return new SimpleImmutableEntry<>(key, M3GuavaListMultimap.this.get(key));
                            }
                            @Override public void remove() { keys.remove(); }
                        };
                    }
                };
            }
        };
        return asMap;
    }

    @Override public Multiset<K> keys() {
        if (countedKeys == null) countedKeys = new KeyCounts();
        return countedKeys;
    }

    /** Live read/write counted-key view; count insertion is intentionally unsupported. */
    private final class KeyCounts extends AbstractCollection<K> implements Multiset<K> {
        @Override public int size() { return pairs; }
        @Override public boolean isEmpty() { return pairs == 0; }
        @Override public int count(Object key) {
            List<V> list = directory.get(key);
            return list == null ? 0 : list.size();
        }
        @Override public boolean contains(Object key) { return directory.containsKey(key); }
        @Override public int add(K key, int occurrences) { throw new UnsupportedOperationException(); }
        @Override public boolean add(K key) { throw new UnsupportedOperationException(); }
        @Override public int remove(Object key, int occurrences) {
            if (occurrences < 0) throw new IllegalArgumentException("negative occurrences");
            int before = count(key);
            if (occurrences == 0 || before == 0) return before;
            List<V> list = directory.get(key);
            int actual = Math.min(occurrences, before);
            list.subList(0, actual).clear();
            pairs -= actual;
            if (list.isEmpty()) directory.remove(key);
            revision++;
            return before;
        }
        @Override public boolean remove(Object key) { return remove(key, 1) > 0; }
        @Override public int setCount(K key, int count) { throw new UnsupportedOperationException(); }
        @Override public boolean setCount(K key, int oldCount, int newCount) {
            throw new UnsupportedOperationException();
        }
        @Override public Set<K> elementSet() { return keySet(); }
        @Override public Set<Multiset.Entry<K>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return directory.size(); }
                @Override public Iterator<Multiset.Entry<K>> iterator() {
                    Iterator<K> iterator = keySet().iterator();
                    return new Iterator<>() {
                        @Override public boolean hasNext() { return iterator.hasNext(); }
                        @Override public Multiset.Entry<K> next() {
                            K key = iterator.next();
                            return new KeyEntry(key);
                        }
                        @Override public void remove() { iterator.remove(); }
                    };
                }
            };
        }
        @Override public Iterator<K> iterator() {
            Iterator<Map.Entry<K, V>> cursor = entries().iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return cursor.hasNext(); }
                @Override public K next() { return cursor.next().getKey(); }
                @Override public void remove() { cursor.remove(); }
            };
        }
        @Override public void clear() { M3GuavaListMultimap.this.clear(); }
        @Override public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Multiset<?> peer) || size() != peer.size()
                    || elementSet().size() != peer.elementSet().size()) return false;
            for (K key : elementSet()) if (count(key) != peer.count(key)) return false;
            return true;
        }
        @Override public int hashCode() {
            int hash = 0;
            for (K key : elementSet()) hash += Objects.hashCode(key) ^ count(key);
            return hash;
        }
        private final class KeyEntry implements Multiset.Entry<K> {
            private final K key;
            private KeyEntry(K key) { this.key = key; }
            @Override public K getElement() { return key; }
            @Override public int getCount() { return KeyCounts.this.count(key); }
            @Override public boolean equals(Object other) {
                return other instanceof Multiset.Entry<?> e
                        && Objects.equals(key, e.getElement()) && getCount() == e.getCount();
            }
            @Override public int hashCode() { return Objects.hashCode(key) ^ getCount(); }
            @Override public String toString() { return String.valueOf(key) + " x " + getCount(); }
        }
    }

    /** Exact 32-bit-mask indexed occurrence projection over detached selected rows. */
    public M3GuavaListMultimap<K, V> selectedByStates(int[] states, int required,
            int forbidden, M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        if (states.length != pairs) throw new IllegalArgumentException("unaligned state lane");
        long expected = revision;
        Object[] allKeys = new Object[pairs];
        Object[] allValues = new Object[pairs];
        int[] positions = new int[pairs];
        int ordinal = 0;
        for (Map.Entry<K, List<V>> group : directory.entrySet()) {
            K key = group.getKey();
            for (V value : group.getValue()) {
                allKeys[ordinal] = key;
                allValues[ordinal] = value;
                positions[ordinal] = ordinal++;
            }
        }
        if (ordinal != pairs || expected != revision) throw new ConcurrentModificationException();
        int[] kept = M3IndexPathMasks.select(positions, positions, new int[][]{states},
                new int[]{required}, new int[]{forbidden}, 0, pairs, 1, backend);
        if (expected != revision) throw new ConcurrentModificationException();
        M3GuavaListMultimap<K, V> result = new M3GuavaListMultimap<>();
        for (int index : kept) {
            @SuppressWarnings("unchecked") K key = (K) allKeys[index];
            @SuppressWarnings("unchecked") V value = (V) allValues[index];
            result.put(key, value);
        }
        return result;
    }

    @Override public boolean equals(Object object) {
        return object == this || object instanceof Multimap<?, ?> other
                && asMap().equals(other.asMap());
    }
    @Override public int hashCode() { return asMap().hashCode(); }
    @Override public String toString() { return asMap().toString(); }
}
