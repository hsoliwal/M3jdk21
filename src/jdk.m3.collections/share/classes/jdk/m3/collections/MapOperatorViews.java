// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and Synexia contributors
package jdk.m3.collections;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Opt-in map composition with an explicit equals-key collision policy.
 *
 * <p>No {@link Map} signature changes. The logical result owns only references
 * to the two backing maps and a collision policy; every read projects a fresh
 * temporary columnar key/value snapshot with packed source-membership bits.
 * Keys are compared by the normal Java equals/hashCode contract, not the
 * identity or comparator policy of a source Map. Unlike sets, duplicate keys
 * under this equality policy <em>within either source</em> are rejected instead
 * of being silently collapsed. On a cross-source match the left key object
 * remains authoritative, while the selected policy decides the value.
 *
 * <p>A result is unmodifiable and live across reads. An entry iterator visits
 * the immutable projection captured on iterator creation. A spliterator binds
 * its projection on first traversal. No cross-source atomic snapshot or
 * stronger concurrent-modification guarantee is promised.
 */
public final class MapOperatorViews {
    private MapOperatorViews() { }

    /** The mandatory, explicit resolution of an equal key in both maps. */
    public enum CollisionPolicy { REJECT, KEEP_LEFT, KEEP_RIGHT }

    /** Java 21 named default-method owner; no policy-free plus exists. */
    @FunctionalInterface
    public interface MapOperator<K, V> {
        Map<K, V> source();

        default Map<K, V> plus(Map<? extends K, ? extends V> right,
                               CollisionPolicy collisionPolicy) {
            return MapOperatorViews.overlayByEquals(source(), right, collisionPolicy);
        }
    }

    /** Explicit equality adapter; normal Map implementations gain no methods. */
    public static <K, V> MapOperator<K, V> asEqualsOperator(Map<K, V> source) {
        Objects.requireNonNull(source, "source");
        return () -> source;
    }

    /**
     * New live read-only logical map. Collision rejection is checked on read,
     * after current source contents have been projected; not at construction.
     */
    public static <K, V> Map<K, V> overlayByEquals(Map<? extends K, ? extends V> left,
                                                   Map<? extends K, ? extends V> right,
                                                   CollisionPolicy collisionPolicy) {
        return new Overlay<>(left, right, collisionPolicy);
    }

    private static final class Overlay<K, V> extends AbstractMap<K, V> {
        private final Map<? extends K, ? extends V> left;
        private final Map<? extends K, ? extends V> right;
        private final CollisionPolicy policy;
        private final Set<Entry<K, V>> entries = new EntryView();

        Overlay(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right,
                CollisionPolicy policy) {
            this.left = Objects.requireNonNull(left, "left");
            this.right = Objects.requireNonNull(right, "right");
            this.policy = Objects.requireNonNull(policy, "collisionPolicy");
        }

        private Snapshot<K, V> project() {
            SlotTable<K, V> table = new SlotTable<>();
            table.add(left, true, policy);
            table.add(right, false, policy);
            return table.snapshot();
        }

        @Override public int size() { return project().size; }
        @Override public boolean isEmpty() { return project().size == 0; }
        @Override public boolean containsKey(Object key) { return project().containsKey(key); }
        @Override public boolean containsValue(Object value) { return project().containsValue(value); }
        @Override public V get(Object key) { return project().get(key); }
        @Override public V getOrDefault(Object key, V fallback) {
            Snapshot<K, V> snapshot = project();
            return snapshot.containsKey(key) ? snapshot.get(key) : fallback;
        }
        @Override public Set<Entry<K, V>> entrySet() { return entries; }

        @Override public V put(K key, V value) { throw readOnly(); }
        @Override public V remove(Object key) { throw readOnly(); }
        @Override public void putAll(Map<? extends K, ? extends V> values) { throw readOnly(); }
        @Override public void clear() { throw readOnly(); }
        @Override public V putIfAbsent(K key, V value) { throw readOnly(); }
        @Override public boolean remove(Object key, Object value) { throw readOnly(); }
        @Override public boolean replace(K key, V oldValue, V newValue) { throw readOnly(); }
        @Override public V replace(K key, V value) { throw readOnly(); }
        @Override public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw readOnly();
        }
        @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> function) {
            throw readOnly();
        }
        @Override public V computeIfPresent(K key,
                                             BiFunction<? super K, ? super V, ? extends V> function) {
            throw readOnly();
        }
        @Override public V compute(K key,
                                   BiFunction<? super K, ? super V, ? extends V> function) {
            throw readOnly();
        }
        @Override public V merge(K key, V value,
                                 BiFunction<? super V, ? super V, ? extends V> function) {
            throw readOnly();
        }

        private UnsupportedOperationException readOnly() {
            return new UnsupportedOperationException("read-only equals-map overlay");
        }

        private final class EntryView extends AbstractSet<Entry<K, V>> {
            @Override public int size() { return Overlay.this.size(); }
            @Override public boolean isEmpty() { return Overlay.this.isEmpty(); }
            @Override public Iterator<Entry<K, V>> iterator() { return project().entries(); }

            @Override public boolean contains(Object value) {
                if (!(value instanceof Entry<?, ?> entry)) return false;
                Snapshot<K, V> snapshot = project();
                return snapshot.containsKey(entry.getKey())
                        && Objects.equals(snapshot.get(entry.getKey()), entry.getValue());
            }

            /** No SIZED marker, since the source maps are live until first touch. */
            @Override public Spliterator<Entry<K, V>> spliterator() {
                return new Spliterator<>() {
                    private Iterator<Entry<K, V>> iterator;

                    @Override public boolean tryAdvance(Consumer<? super Entry<K, V>> action) {
                        Objects.requireNonNull(action, "action");
                        if (iterator == null) iterator = project().entries();
                        if (!iterator.hasNext()) return false;
                        action.accept(iterator.next());
                        return true;
                    }

                    @Override public Spliterator<Entry<K, V>> trySplit() { return null; }
                    @Override public long estimateSize() { return Long.MAX_VALUE; }
                    @Override public int characteristics() { return DISTINCT | ORDERED | NONNULL; }
                };
            }

            @Override public boolean add(Entry<K, V> entry) { throw readOnly(); }
            @Override public boolean addAll(Collection<? extends Entry<K, V>> entries) {
                throw readOnly();
            }
            @Override public boolean remove(Object entry) { throw readOnly(); }
            @Override public void clear() { throw readOnly(); }
            @Override public boolean removeAll(Collection<?> entries) { throw readOnly(); }
            @Override public boolean retainAll(Collection<?> entries) { throw readOnly(); }
            @Override public boolean removeIf(Predicate<? super Entry<K, V>> predicate) {
                throw readOnly();
            }
        }
    }

    /** Ephemeral offset-to-slot lookup and source-origin bit masks. */
    private static final class SlotTable<K, V> {
        private final HashMap<Object, Integer> offsets = new HashMap<>();
        private Object[] keys = new Object[8];
        private Object[] values = new Object[8];
        private long[] seenLeft = new long[1];
        private long[] seenRight = new long[1];
        private int size;

        void add(Map<? extends K, ? extends V> source, boolean fromLeft,
                 CollisionPolicy policy) {
            for (Map.Entry<? extends K, ? extends V> entry : source.entrySet()) {
                Object key = entry.getKey();
                Integer old = offsets.get(key);
                int slot;
                if (old == null) {
                    slot = size;
                    grow();
                    keys[size] = key;
                    values[size] = entry.getValue();
                    offsets.put(key, size++);
                } else {
                    slot = old;
                    long bit = 1L << (slot & 63);
                    long seen = fromLeft ? seenLeft[slot >>> 6] : seenRight[slot >>> 6];
                    if ((seen & bit) != 0) {
                        throw new IllegalArgumentException(
                                "source has duplicate keys under explicit Java equality");
                    }
                    if (!fromLeft) {
                        switch (policy) {
                            case REJECT -> throw new IllegalArgumentException(
                                    "equal key collision between source maps");
                            case KEEP_LEFT -> { /* retain existing key and value */ }
                            case KEEP_RIGHT -> values[slot] = entry.getValue();
                        }
                    }
                }
                long[] mask = fromLeft ? seenLeft : seenRight;
                mask[slot >>> 6] |= 1L << (slot & 63);
            }
        }

        private void grow() {
            if (size < keys.length) return;
            int capacity = Math.multiplyExact(keys.length, 2);
            keys = Arrays.copyOf(keys, capacity);
            values = Arrays.copyOf(values, capacity);
            int words = (capacity >>> 6) + ((capacity & 63) == 0 ? 0 : 1);
            seenLeft = Arrays.copyOf(seenLeft, words);
            seenRight = Arrays.copyOf(seenRight, words);
        }

        Snapshot<K, V> snapshot() {
            return new Snapshot<>(Arrays.copyOf(keys, size), Arrays.copyOf(values, size),
                    offsets, size);
        }
    }

    /** Stable per-iterator columns and slot index, never stored in Overlay. */
    private static final class Snapshot<K, V> {
        private final Object[] keys;
        private final Object[] values;
        private final HashMap<Object, Integer> offsets;
        final int size;

        Snapshot(Object[] keys, Object[] values, HashMap<Object, Integer> offsets, int size) {
            this.keys = keys;
            this.values = values;
            this.offsets = offsets;
            this.size = size;
        }

        boolean containsKey(Object key) { return offsets.containsKey(key); }

        boolean containsValue(Object value) {
            for (Object candidate : values) {
                if (Objects.equals(candidate, value)) return true;
            }
            return false;
        }

        V get(Object key) {
            Integer slot = offsets.get(key);
            if (slot == null) return null;
            @SuppressWarnings("unchecked") V value = (V) values[slot];
            return value;
        }

        Iterator<Map.Entry<K, V>> entries() {
            return new Iterator<>() {
                private int offset;

                @Override public boolean hasNext() { return offset < size; }

                @Override public Map.Entry<K, V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    @SuppressWarnings("unchecked") K key = (K) keys[offset];
                    @SuppressWarnings("unchecked") V value = (V) values[offset++];
                    return new AbstractMap.SimpleImmutableEntry<>(key, value);
                }
            };
        }
    }
}
