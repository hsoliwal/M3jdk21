/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import org.pcollections.AbstractUnmodifiableCollection;
import org.pcollections.AbstractUnmodifiableMap;
import org.pcollections.AbstractUnmodifiableSet;
import org.pcollections.PMap;

/**
 * Persistent insertion-ordered map with alternating key/value vector slots,
 * bitmap liveness and a shared hash-trie key-to-ordinal index. Replacements keep
 * the original key representative and position. Removal followed by insertion
 * appends. Null keys and values are supported. Entries are immutable projections,
 * never retained storage. Old versions keep their complete contents.
 *
 * <p>Deletion clears payload slots immediately in the new version. When physical
 * entries exceed max(64, twice the live size), deterministic compaction rebuilds
 * encounter order. The index stores boxed integer ordinals as map values; masks
 * are shared per 32 entries, not per entry. Compaction costs O(n log n), whereas
 * ordinary updates copy only changed map/vector paths.
 */
public final class M3POrderedMap<K, V> extends AbstractUnmodifiableMap<K, V>
        implements PMap<K, V>, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3PMap<K, Integer> positions;
    private final M3PVector<Object> slots;
    private final M3PVector<Integer> masks;

    private M3POrderedMap(M3PMap<K, Integer> positions, M3PVector<Object> slots,
                         M3PVector<Integer> masks) {
        this.positions = positions;
        this.slots = slots;
        this.masks = masks;
    }

    public static <K, V> M3POrderedMap<K, V> empty() {
        return new M3POrderedMap<>(M3PMap.empty(), M3PVector.empty(), M3PVector.empty());
    }

    public static <K, V> M3POrderedMap<K, V> singleton(K key, V value) {
        return M3POrderedMap.<K, V>empty().plus(key, value);
    }

    /** Uses the source map's entry encounter order. */
    public static <K, V> M3POrderedMap<K, V> from(Map<? extends K, ? extends V> source) {
        return M3POrderedMap.<K, V>empty().plusAll(source);
    }

    @Override public int size() { return positions.size(); }
    @Override public boolean containsKey(Object key) { return positions.containsKey(key); }

    @SuppressWarnings("unchecked")
    private K keyAt(int ordinal) { return (K) slots.get(ordinal * 2); }

    @SuppressWarnings("unchecked")
    private V valueAt(int ordinal) { return (V) slots.get(ordinal * 2 + 1); }

    @Override public V get(Object key) {
        Integer ordinal = positions.get(key);
        return ordinal == null ? null : valueAt(ordinal);
    }

    @Override public M3POrderedMap<K, V> plus(K key, V value) {
        Integer ordinal = positions.get(key);
        if (ordinal != null) {
            M3PVector<Object> next = slots.with(ordinal * 2 + 1, value);
            return next == slots ? this : new M3POrderedMap<>(positions, next, masks);
        }
        if (slots.size() > Integer.MAX_VALUE - 2) {
            if (size() < slots.size() / 2) return compact().plus(key, value);
            throw new OutOfMemoryError("M3 ordered-map slot capacity exceeded");
        }
        int index = slots.size() / 2;
        int word = index >>> 5;
        int bit = 1 << (index & 31);
        M3PVector<Integer> nextMasks = word == masks.size()
                ? masks.plus(bit) : masks.with(word, masks.get(word) | bit);
        return new M3POrderedMap<>(positions.plus(key, index),
                slots.plus(key).plus(value), nextMasks);
    }

    @Override public M3POrderedMap<K, V> plusAll(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        M3POrderedMap<K, V> result = this;
        for (Entry<? extends K, ? extends V> entry : source.entrySet()) {
            result = result.plus(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override public M3POrderedMap<K, V> minus(Object key) {
        Integer ordinal = positions.get(key);
        if (ordinal == null) return this;
        if (size() == 1) return empty();
        int word = ordinal >>> 5;
        M3POrderedMap<K, V> next = new M3POrderedMap<>(positions.minus(key),
                slots.with(ordinal * 2, null).with(ordinal * 2 + 1, null),
                masks.with(word, masks.get(word) & ~(1 << (ordinal & 31))));
        return next.slots.size() / 2L > Math.max(64L, 2L * next.size())
                ? next.compact() : next;
    }

    @Override public M3POrderedMap<K, V> minusAll(Collection<?> keys) {
        Objects.requireNonNull(keys, "keys");
        M3POrderedMap<K, V> result = this;
        for (Object key : keys) result = result.minus(key);
        return result;
    }

    private M3POrderedMap<K, V> compact() {
        M3POrderedMap<K, V> result = empty();
        SlotCursor cursor = new SlotCursor();
        while (cursor.hasNext()) {
            int ordinal = cursor.nextOrdinal();
            result = result.plus(keyAt(ordinal), valueAt(ordinal));
        }
        return result;
    }

    /** Primitive cursor skips dead slots one mask word at a time. */
    private final class SlotCursor {
        private int word;
        private int remaining = masks.isEmpty() ? 0 : masks.get(0);

        boolean hasNext() {
            while (remaining == 0 && word + 1 < masks.size()) remaining = masks.get(++word);
            return remaining != 0;
        }

        int nextOrdinal() {
            if (!hasNext()) throw new NoSuchElementException();
            int bit = Integer.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1;
            return (word << 5) + bit;
        }
    }

    private Entry<K, V> entryAt(int ordinal) {
        return new SimpleImmutableEntry<>(keyAt(ordinal), valueAt(ordinal));
    }

    @Override public Set<Entry<K, V>> entrySet() {
        return new AbstractUnmodifiableSet<>() {
            @Override public int size() { return M3POrderedMap.this.size(); }
            @Override public Spliterator<Entry<K, V>> spliterator() {
                return Spliterators.spliterator(this,
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE);
            }
            @Override public boolean contains(Object candidate) {
                if (!(candidate instanceof Entry<?, ?> entry)) return false;
                Integer ordinal = positions.get(entry.getKey());
                return ordinal != null && Objects.equals(valueAt(ordinal), entry.getValue());
            }
            @Override public Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {
                    private final SlotCursor cursor = new SlotCursor();
                    @Override public boolean hasNext() { return cursor.hasNext(); }
                    @Override public Entry<K, V> next() { return entryAt(cursor.nextOrdinal()); }
                };
            }
        };
    }

    @Override public Set<K> keySet() {
        return new AbstractUnmodifiableSet<>() {
            @Override public int size() { return M3POrderedMap.this.size(); }
            @Override public Spliterator<K> spliterator() {
                return Spliterators.spliterator(this,
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE);
            }
            @Override public boolean contains(Object key) { return positions.containsKey(key); }
            @Override public Iterator<K> iterator() {
                return new Iterator<>() {
                    private final SlotCursor cursor = new SlotCursor();
                    @Override public boolean hasNext() { return cursor.hasNext(); }
                    @Override public K next() { return keyAt(cursor.nextOrdinal()); }
                };
            }
        };
    }

    @Override public Collection<V> values() {
        return new AbstractUnmodifiableCollection<>() {
            @Override public int size() { return M3POrderedMap.this.size(); }
            @Override public Spliterator<V> spliterator() {
                return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.IMMUTABLE);
            }
            @Override public Iterator<V> iterator() {
                return new Iterator<>() {
                    private final SlotCursor cursor = new SlotCursor();
                    @Override public boolean hasNext() { return cursor.hasNext(); }
                    @Override public V next() { return valueAt(cursor.nextOrdinal()); }
                };
            }
        };
    }

    /** @deprecated Unsupported destructive operation, including no-op inputs. */
    @Deprecated @Override public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Unsupported destructive operation, including no-op inputs. */
    @Deprecated @Override public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    @Serial private Object writeReplace() {
        Object[] visible = new Object[Math.multiplyExact(size(), 2)];
        SlotCursor cursor = new SlotCursor();
        for (int index = 0; cursor.hasNext();) {
            int ordinal = cursor.nextOrdinal();
            visible[index++] = keyAt(ordinal);
            visible[index++] = valueAt(ordinal);
        }
        return new SerializationProxy(visible);
    }

    @Serial private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("Serialization proxy required");
    }

    private static final class SerializationProxy implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        // ObjectOutputStream checks serializability of each visible key/value.
        @SuppressWarnings("serial") private final Object[] entries;
        SerializationProxy(Object[] entries) { this.entries = entries; }
        @Serial private Object readResolve() throws InvalidObjectException {
            if (entries == null || (entries.length & 1) != 0) {
                throw new InvalidObjectException("Invalid ordered-map entry lanes");
            }
            M3POrderedMap<Object, Object> result = empty();
            for (int index = 0; index < entries.length; index += 2) {
                if (result.containsKey(entries[index])) {
                    throw new InvalidObjectException("Duplicate ordered-map key");
                }
                result = result.plus(entries[index], entries[index + 1]);
            }
            return result;
        }
    }
}
