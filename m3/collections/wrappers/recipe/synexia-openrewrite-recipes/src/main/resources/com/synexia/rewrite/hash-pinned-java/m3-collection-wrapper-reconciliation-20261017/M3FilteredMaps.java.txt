/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026: target namespace; reused typed wrappers and raw queries; saturated size.
 * Design references: Synexia M3CollectionViews; Guava Maps filtered-entry views
 * (Google LLC, Apache-2.0). Exact donor/category decisions accompany the recipe.
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Live filtered map wrappers over an authoritative caller-owned map.
 *
 * <p>Predicates see the stored key representative and value, including nulls
 * admitted by the owner. Existing equal/comparator-equivalent keys retain their
 * stored representative on put. Rejected writes throw IllegalArgumentException;
 * hidden entries survive clear, removal and default-method updates. A successful
 * put may make a hidden mapping visible, but returns null for its previously
 * invisible value. Bulk writes have the Map prefix-update, not transaction,
 * contract. Predicates must be pure and stable during an operation.
 *
 * <p>Entry traversal exposes guarded write-through entries. Navigation and poll
 * methods return immutable snapshots. Iterator removal is unsupported; explicit
 * and bulk view removal is supported when the owner supports it. Entry adapters
 * exist only at the public Entry boundary; no copied index or retained per-entry
 * wrapper store is created. Exact size scans; streams do not count first.
 *
 * <p>The admitted owner profile is ordinary equals-key maps or comparator-key
 * SortedMaps whose put retains an existing stored key. Identity/weak-key maps,
 * concurrent atomicity, serialization and synchronization are not admitted by
 * this wrapper contract. Wrap shared owners with an external common lock.
 */
public final class M3FilteredMaps {
    private M3FilteredMaps() { }

    /** Retains sorted/navigable capabilities even when the source is typed Map. */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> filtered(
            Map<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        if (source instanceof NavigableMap<?, ?>) {
            return filtered((NavigableMap<K, V>) source, predicate);
        }
        if (source instanceof SortedMap<?, ?>) {
            return filtered((SortedMap<K, V>) source, predicate);
        }
        return new FilteredMap<>(source, predicate);
    }

    /** Preserves comparator and live range bounds. */
    @SuppressWarnings("unchecked")
    public static <K, V> SortedMap<K, V> filtered(
            SortedMap<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        if (source instanceof NavigableMap<?, ?>) {
            return filtered((NavigableMap<K, V>) source, predicate);
        }
        return new FilteredSortedMap<>(source, predicate);
    }

    /** Preserves bounded navigation, descending maps and navigable key views. */
    public static <K, V> NavigableMap<K, V> filtered(
            NavigableMap<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        return new FilteredNavigableMap<>(source, predicate);
    }

    /**
     * Returns a live read-only view, retaining a runtime sorted/navigable owner even
     * through an erased Map reference. This is not a snapshot and does not freeze
     * mutable values. JDK Collections owns entry guarding and range propagation.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> unmodifiableMap(Map<K, V> source) {
        if (source instanceof NavigableMap<?, ?>) return unmodifiableMap((NavigableMap<K, V>) source);
        if (source instanceof SortedMap<?, ?>) return unmodifiableMap((SortedMap<K, V>) source);
        return Collections.unmodifiableMap(source);
    }

    /** Retains the comparator and live bounds in a JDK-owned read-only wrapper. */
    @SuppressWarnings("unchecked")
    public static <K, V> SortedMap<K, V> unmodifiableMap(SortedMap<K, V> source) {
        if (source instanceof NavigableMap<?, ?>) return unmodifiableMap((NavigableMap<K, V>) source);
        return Collections.unmodifiableSortedMap(source);
    }

    /** Retains navigable/descending views without copying mappings. */
    public static <K, V> NavigableMap<K, V> unmodifiableMap(NavigableMap<K, V> source) {
        return Collections.unmodifiableNavigableMap(source);
    }

    /**
     * Adds JDK dynamic key/value type checks while retaining sorted capability.
     * Existing contents are not scanned; other aliases can still bypass checks.
     * Place this outside a filter to check raw calls before its typed predicate.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> checkedMap(Map<K, V> source, Class<K> keys, Class<V> values) {
        if (source instanceof NavigableMap<?, ?>) return checkedMap((NavigableMap<K, V>) source, keys, values);
        if (source instanceof SortedMap<?, ?>) return checkedMap((SortedMap<K, V>) source, keys, values);
        return Collections.checkedMap(source, keys, values);
    }

    /** Retains sorted bounds while delegating dynamic type safety to the JDK. */
    @SuppressWarnings("unchecked")
    public static <K, V> SortedMap<K, V> checkedMap(SortedMap<K, V> source, Class<K> keys, Class<V> values) {
        if (source instanceof NavigableMap<?, ?>) return checkedMap((NavigableMap<K, V>) source, keys, values);
        return Collections.checkedSortedMap(source, keys, values);
    }

    /** Retains navigable bounds and checked Entry.setValue through JDK adapters. */
    public static <K, V> NavigableMap<K, V> checkedMap(NavigableMap<K, V> source, Class<K> keys, Class<V> values) {
        return Collections.checkedNavigableMap(source, keys, values);
    }

    /**
     * Adds the JDK synchronized wrapper outside an existing view. All access must
     * use that same wrapper/monitor; direct backing aliases bypass it. Hold the
     * returned root wrapper's monitor during iteration, stream use, compound
     * actions, and traversal of range/descending views. This is not lock-free.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> source) {
        if (source instanceof NavigableMap<?, ?>) return synchronizedMap((NavigableMap<K, V>) source);
        if (source instanceof SortedMap<?, ?>) return synchronizedMap((SortedMap<K, V>) source);
        return Collections.synchronizedMap(source);
    }

    /** Range views share the returned root wrapper's monitor, per JDK contract. */
    @SuppressWarnings("unchecked")
    public static <K, V> SortedMap<K, V> synchronizedMap(SortedMap<K, V> source) {
        if (source instanceof NavigableMap<?, ?>) return synchronizedMap((NavigableMap<K, V>) source);
        return Collections.synchronizedSortedMap(source);
    }

    /** Navigable/descending/range views use the same root monitor. */
    public static <K, V> NavigableMap<K, V> synchronizedMap(NavigableMap<K, V> source) {
        return Collections.synchronizedNavigableMap(source);
    }

    private static class FilteredMap<K, V> extends AbstractMap<K, V> {
        protected final Map<K, V> backing;
        protected final BiPredicate<? super K, ? super V> predicate;

        @SuppressWarnings("unchecked")
        private FilteredMap(Map<K, V> source, BiPredicate<? super K, ? super V> filter) {
            Objects.requireNonNull(source, "backing");
            Objects.requireNonNull(filter, "predicate");
            if (source instanceof FilteredMap<?, ?> old) {
                FilteredMap<K, V> prior = (FilteredMap<K, V>) old;
                backing = prior.backing;
                predicate = (key, value) -> prior.predicate.test(key, value) && filter.test(key, value);
            } else {
                backing = source;
                predicate = filter;
            }
        }

        protected final boolean visible(Entry<K, V> entry) {
            return entry != null && predicate.test(entry.getKey(), entry.getValue());
        }

        /** The ordinary-map profile uses equals, never probe-side predicate evaluation. */
        protected Entry<K, V> stored(Object key) {
            if (!backing.containsKey(key)) return null;
            for (Entry<K, V> entry : backing.entrySet()) {
                if (Objects.equals(key, entry.getKey())) return entry;
            }
            throw new IllegalStateException("owner violates the admitted key-equivalence profile");
        }

        protected final Set<Entry<K, V>> selectedEntries() {
            return M3CollectionViews.filtered(backing.entrySet(), this::visible);
        }

        // Reuse the existing raw-entry scan atom. No public Entry escapes here;
        // callers of entrySet still receive the unchanged GuardedEntry adapter.
        @Override public int size() {
            int count = 0;
            for (Entry<K, V> entry : backing.entrySet()) if (visible(entry) && count != Integer.MAX_VALUE) count++;
            return count;
        }
        @Override public boolean isEmpty() {
            for (Entry<K, V> entry : backing.entrySet()) if (visible(entry)) return false;
            return true;
        }
        @Override public boolean containsValue(Object value) {
            for (Entry<K, V> entry : backing.entrySet()) {
                if (visible(entry) && (value == null ? entry.getValue() == null : value.equals(entry.getValue()))) return true;
            }
            return false;
        }
        @Override public int hashCode() {
            int hash = 0;
            for (Entry<K, V> entry : backing.entrySet()) {
                if (visible(entry)) hash += Objects.hashCode(entry.getKey()) ^ Objects.hashCode(entry.getValue());
            }
            return hash;
        }
        @Override public boolean containsKey(Object key) { return visible(stored(key)); }
        @Override public V get(Object key) {
            Entry<K, V> entry = stored(key);
            return visible(entry) ? entry.getValue() : null;
        }
        @Override public V put(K key, V value) {
            Entry<K, V> entry = stored(key);
            K representative = entry == null ? key : entry.getKey();
            if (!predicate.test(representative, value)) {
                throw new IllegalArgumentException("mapping rejected by view predicate");
            }
            V old = visible(entry) ? entry.getValue() : null;
            backing.put(representative, value);
            return old;
        }
        @Override public V remove(Object key) {
            Entry<K, V> entry = stored(key);
            return visible(entry) ? backing.remove(entry.getKey()) : null;
        }
        @Override public void clear() { removeMatching(entry -> true, false); }
        @Override public void forEach(BiConsumer<? super K, ? super V> action) {
            Objects.requireNonNull(action, "action");
            for (Entry<K, V> entry : backing.entrySet()) {
                if (visible(entry)) action.accept(entry.getKey(), entry.getValue());
            }
        }

        /** Raw entries are private to this atom; caller callbacks receive guarded entries/values. */
        protected final boolean removeMatching(Predicate<? super Entry<K, V>> condition, boolean first) {
            boolean changed = false;
            Iterator<Entry<K, V>> iterator = backing.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<K, V> entry = iterator.next();
                if (visible(entry) && condition.test(entry)) {
                    iterator.remove();
                    changed = true;
                    if (first) break;
                }
            }
            return changed;
        }

        @Override public Set<K> keySet() { return new Keys(); }
        @Override public Collection<V> values() { return new Values(); }
        @Override public Set<Entry<K, V>> entrySet() { return new Entries(); }

        private final class GuardedEntry implements Entry<K, V> {
            private final Entry<K, V> entry;
            private GuardedEntry(Entry<K, V> entry) { this.entry = entry; }
            @Override public K getKey() { return entry.getKey(); }
            @Override public V getValue() { return entry.getValue(); }
            @Override public V setValue(V value) {
                if (!predicate.test(getKey(), value)) {
                    throw new IllegalArgumentException("mapping rejected by view predicate");
                }
                return entry.setValue(value);
            }
            @Override public boolean equals(Object other) {
                return other instanceof Entry<?, ?> e
                        && Objects.equals(getKey(), e.getKey())
                        && Objects.equals(getValue(), e.getValue());
            }
            @Override public int hashCode() {
                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
            }
            @Override public String toString() { return getKey() + "=" + getValue(); }
        }

        private final class Entries extends AbstractSet<Entry<K, V>> {
            @Override public int size() { return FilteredMap.this.size(); }
            @Override public boolean isEmpty() { return FilteredMap.this.isEmpty(); }
            @Override public Iterator<Entry<K, V>> iterator() {
                return mapIterator(selectedEntries().iterator(), GuardedEntry::new);
            }
            @Override public Spliterator<Entry<K, V>> spliterator() {
                return mapSpliterator(selectedEntries().spliterator(), GuardedEntry::new);
            }
            @Override public boolean contains(Object value) {
                if (!(value instanceof Entry<?, ?> probe)) return false;
                Entry<K, V> entry = stored(probe.getKey());
                return visible(entry) && Objects.equals(entry.getValue(), probe.getValue());
            }
            @Override public boolean remove(Object value) {
                if (!(value instanceof Entry<?, ?> probe)) return false;
                Entry<K, V> entry = stored(probe.getKey());
                if (!visible(entry) || !Objects.equals(entry.getValue(), probe.getValue())) return false;
                backing.remove(entry.getKey());
                return true;
            }
            @Override public void clear() { FilteredMap.this.clear(); }
            @Override public boolean removeIf(Predicate<? super Entry<K, V>> filter) {
                Objects.requireNonNull(filter, "filter");
                return removeMatching(entry -> filter.test(new GuardedEntry(entry)), false);
            }
            @Override public boolean removeAll(Collection<?> values) {
                Objects.requireNonNull(values, "values");
                return removeIf(values::contains);
            }
            @Override public boolean retainAll(Collection<?> values) {
                Objects.requireNonNull(values, "values");
                return removeIf(entry -> !values.contains(entry));
            }
        }

        private final class Keys extends AbstractSet<K> {
            @Override public int size() { return FilteredMap.this.size(); }
            @Override public boolean isEmpty() { return FilteredMap.this.isEmpty(); }
            @Override public boolean contains(Object key) { return containsKey(key); }
            @Override public Iterator<K> iterator() {
                return mapIterator(selectedEntries().iterator(), Entry::getKey);
            }
            @Override public Spliterator<K> spliterator() {
                return mapSpliterator(selectedEntries().spliterator(), Entry::getKey);
            }
            @Override public void clear() { FilteredMap.this.clear(); }
            @Override public boolean remove(Object key) {
                Entry<K, V> entry = stored(key);
                if (!visible(entry)) return false;
                backing.remove(entry.getKey());
                return true;
            }
            @Override public boolean removeIf(Predicate<? super K> filter) {
                Objects.requireNonNull(filter, "filter");
                return removeMatching(entry -> filter.test(entry.getKey()), false);
            }
            @Override public boolean removeAll(Collection<?> keys) {
                Objects.requireNonNull(keys, "keys");
                return removeIf(keys::contains);
            }
            @Override public boolean retainAll(Collection<?> keys) {
                Objects.requireNonNull(keys, "keys");
                return removeIf(key -> !keys.contains(key));
            }
        }

        private final class Values extends AbstractCollection<V> {
            @Override public int size() { return FilteredMap.this.size(); }
            @Override public boolean isEmpty() { return FilteredMap.this.isEmpty(); }
            @Override public Iterator<V> iterator() {
                return mapIterator(selectedEntries().iterator(), Entry::getValue);
            }
            @Override public Spliterator<V> spliterator() {
                return mapSpliterator(selectedEntries().spliterator(), Entry::getValue);
            }
            @Override public void clear() { FilteredMap.this.clear(); }
            @Override public boolean remove(Object value) {
                return removeMatching(entry -> Objects.equals(value, entry.getValue()), true);
            }
            @Override public boolean removeIf(Predicate<? super V> filter) {
                Objects.requireNonNull(filter, "filter");
                return removeMatching(entry -> filter.test(entry.getValue()), false);
            }
            @Override public boolean removeAll(Collection<?> values) {
                Objects.requireNonNull(values, "values");
                return removeIf(values::contains);
            }
            @Override public boolean retainAll(Collection<?> values) {
                Objects.requireNonNull(values, "values");
                return removeIf(value -> !values.contains(value));
            }
        }
    }

    private static class FilteredSortedMap<K, V> extends FilteredMap<K, V>
            implements SortedMap<K, V> {
        protected final SortedMap<K, V> sorted;
        private FilteredSortedMap(SortedMap<K, V> source, BiPredicate<? super K, ? super V> filter) {
            super(source, filter);
            sorted = (SortedMap<K, V>) backing;
        }
        @Override @SuppressWarnings("unchecked")
        protected Entry<K, V> stored(Object key) {
            if (!sorted.containsKey(key)) return null;
            return sorted.tailMap((K) key).entrySet().iterator().next();
        }
        @Override public Comparator<? super K> comparator() { return sorted.comparator(); }
        @Override public K firstKey() { return selectedEntries().iterator().next().getKey(); }
        @Override public K lastKey() {
            Iterator<Entry<K, V>> iterator = selectedEntries().iterator();
            Entry<K, V> entry = iterator.next();
            while (iterator.hasNext()) entry = iterator.next();
            return entry.getKey();
        }
        @Override public SortedMap<K, V> subMap(K from, K to) {
            return filtered(sorted.subMap(from, to), predicate);
        }
        @Override public SortedMap<K, V> headMap(K to) { return filtered(sorted.headMap(to), predicate); }
        @Override public SortedMap<K, V> tailMap(K from) { return filtered(sorted.tailMap(from), predicate); }
    }

    private static final class FilteredNavigableMap<K, V> extends FilteredSortedMap<K, V>
            implements NavigableMap<K, V> {
        private final NavigableMap<K, V> navigable;
        private FilteredNavigableMap(NavigableMap<K, V> source, BiPredicate<? super K, ? super V> filter) {
            super(source, filter);
            navigable = (NavigableMap<K, V>) backing;
        }
        /** Anchor on a stored neighbor: a probe itself may be outside the backing range. */
        private Entry<K, V> seek(Entry<K, V> anchor, boolean descending) {
            if (anchor == null) return null;
            NavigableMap<K, V> range = descending
                    ? navigable.headMap(anchor.getKey(), true).descendingMap()
                    : navigable.tailMap(anchor.getKey(), true);
            return firstVisible(range);
        }
        private Entry<K, V> firstVisible(Map<K, V> source) {
            for (Entry<K, V> entry : source.entrySet()) {
                if (visible(entry)) return new SimpleImmutableEntry<>(entry);
            }
            return null;
        }
        private Entry<K, V> poll(Map<K, V> source) {
            Iterator<Entry<K, V>> iterator = source.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<K, V> entry = iterator.next();
                if (visible(entry)) {
                    Entry<K, V> result = new SimpleImmutableEntry<>(entry);
                    iterator.remove();
                    return result;
                }
            }
            return null;
        }
        @Override public Entry<K, V> lowerEntry(K key) { return seek(navigable.lowerEntry(key), true); }
        @Override public Entry<K, V> floorEntry(K key) { return seek(navigable.floorEntry(key), true); }
        @Override public Entry<K, V> ceilingEntry(K key) { return seek(navigable.ceilingEntry(key), false); }
        @Override public Entry<K, V> higherEntry(K key) { return seek(navigable.higherEntry(key), false); }
        @Override public K lowerKey(K key) { return keyOrNull(lowerEntry(key)); }
        @Override public K floorKey(K key) { return keyOrNull(floorEntry(key)); }
        @Override public K ceilingKey(K key) { return keyOrNull(ceilingEntry(key)); }
        @Override public K higherKey(K key) { return keyOrNull(higherEntry(key)); }
        @Override public Entry<K, V> firstEntry() { return firstVisible(navigable); }
        @Override public Entry<K, V> lastEntry() { return firstVisible(navigable.descendingMap()); }
        @Override public K firstKey() { return requireEntry(firstEntry()).getKey(); }
        @Override public K lastKey() { return requireEntry(lastEntry()).getKey(); }
        @Override public Entry<K, V> pollFirstEntry() { return poll(navigable); }
        @Override public Entry<K, V> pollLastEntry() { return poll(navigable.descendingMap()); }
        @Override public NavigableMap<K, V> descendingMap() {
            return filtered(navigable.descendingMap(), predicate);
        }
        @Override public NavigableSet<K> navigableKeySet() {
            return M3CollectionViews.filtered(navigable.navigableKeySet(),
                    key -> predicate.test(key, navigable.get(key)));
        }
        @Override public NavigableSet<K> keySet() { return navigableKeySet(); }
        @Override public NavigableSet<K> descendingKeySet() { return descendingMap().navigableKeySet(); }
        @Override public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
            return filtered(navigable.subMap(from, fromInclusive, to, toInclusive), predicate);
        }
        @Override public NavigableMap<K, V> headMap(K to, boolean inclusive) {
            return filtered(navigable.headMap(to, inclusive), predicate);
        }
        @Override public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
            return filtered(navigable.tailMap(from, inclusive), predicate);
        }
        @Override public NavigableMap<K, V> subMap(K from, K to) { return subMap(from, true, to, false); }
        @Override public NavigableMap<K, V> headMap(K to) { return headMap(to, false); }
        @Override public NavigableMap<K, V> tailMap(K from) { return tailMap(from, true); }
    }

    private static <K, V> K keyOrNull(Map.Entry<K, V> entry) {
        return entry == null ? null : entry.getKey();
    }
    private static <K, V> Map.Entry<K, V> requireEntry(Map.Entry<K, V> entry) {
        if (entry == null) throw new NoSuchElementException();
        return entry;
    }
    private static <F, T> Iterator<T> mapIterator(Iterator<F> source, Function<? super F, ? extends T> map) {
        return new Iterator<>() {
            @Override public boolean hasNext() { return source.hasNext(); }
            @Override public T next() { return map.apply(source.next()); }
        };
    }
    /**
     * One projection descriptor per cursor (and per explicit split), never a
     * Stream pipeline or retained per-entry array. JDK Stream.map clears the
     * source's DISTINCT, SORTED, NONNULL, CONCURRENT and IMMUTABLE flags: retain
     * only ORDERED/SIZED/SUBSIZED, matching the original map() contract.
     */
    private static <F, T> Spliterator<T> mapSpliterator(
            Spliterator<F> source, Function<? super F, ? extends T> map) {
        return new ProjectingSpliterator<>(
                Objects.requireNonNull(source, "source"),
                Objects.requireNonNull(map, "map"));
    }

    private static final class ProjectingSpliterator<F, T> implements Spliterator<T> {
        private final Spliterator<F> source;
        private final Function<? super F, ? extends T> mapper;

        private ProjectingSpliterator(
                Spliterator<F> source, Function<? super F, ? extends T> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override public boolean tryAdvance(java.util.function.Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            return source.tryAdvance(value -> action.accept(mapper.apply(value)));
        }

        @Override public void forEachRemaining(java.util.function.Consumer<? super T> action) {
            Objects.requireNonNull(action, "action");
            source.forEachRemaining(value -> action.accept(mapper.apply(value)));
        }

        @Override public Spliterator<T> trySplit() {
            Spliterator<F> prefix = source.trySplit();
            return prefix == null ? null : new ProjectingSpliterator<>(prefix, mapper);
        }

        @Override public long estimateSize() {
            return source.estimateSize();
        }

        @Override public int characteristics() {
            return source.characteristics() & (ORDERED | SIZED | SUBSIZED);
        }

        @Override public Comparator<? super T> getComparator() {
            throw new IllegalStateException();
        }
    }
}
