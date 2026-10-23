/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.SortedSet;
 import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Mutable, live filtering over JDK navigable owners without owning per-element nodes.
 *
 * <p>Unlike {@code M3FilteredViews} (read-only) and Commons predicated admission,
 * this wrapper hides mismatching elements already present in the owner. Mutations via
 * the wrapper can add/replace only matching values and remove only matching values;
 * underlying owner changes remain visible. Range and descending views retain the
 * underlying JDK comparator, range checks and reference identities.</p>
 *
 * <p>Predicates must be stable, deterministic and not mutate either owner. The
 * underlying owner must not be concurrently mutated without external synchronization;
 * iterators are cursor-based and do not promise the JDK's fail-fast detection.
 * Iterator/entry wrappers are created on demand, never stored per element. This is
 * not a snapshot, not a concurrent collection and not a transparent JNI facade.</p>
 */
public final class M3MutableFilteredViews {
    private M3MutableFilteredViews() { }

    public static <E> NavigableSet<E> filter(
            NavigableSet<E> source, Predicate<? super E> predicate) {
        return new SetView<>(source, predicate);
    }

    public static <K, V> NavigableMap<K, V> filterEntries(
            NavigableMap<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        return new MapView<>(source, predicate);
    }

    /** Shared comparator ordering atom for bounded mutable-set recovery. */
    @SuppressWarnings("unchecked")
    private static <T> int compareStored(T stored, T query, Comparator<? super T> comparator) {
        return comparator != null ? comparator.compare(stored, query)
                : ((Comparable<? super T>) stored).compareTo(query);
    }

    private static <E> E firstMatch(Iterator<E> cursor, Predicate<? super E> visible) {
        while (cursor.hasNext()) {
            E candidate = cursor.next();
            if (visible.test(candidate)) return candidate;
        }
        return null;
    }

    private static final class SetView<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableSet<E> source;
        private final Predicate<? super E> predicate;
        /** Map key-set views must not admit addition, even for a predicate-accepted key. */
        private final boolean allowAdd;

        private SetView(NavigableSet<E> source, Predicate<? super E> predicate) {
            this(source, predicate, true);
        }

        private SetView(NavigableSet<E> source, Predicate<? super E> predicate,
                boolean allowAdd) {
            this.source = Objects.requireNonNull(source, "source");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
            this.allowAdd = allowAdd;
        }

        @Override public Comparator<? super E> comparator() { return source.comparator(); }

        @Override public int size() {
            int count = 0;
            for (E value : source) if (predicate.test(value)) count++;
            return count;
        }

        @Override public boolean isEmpty() {
            for (E value : source) if (predicate.test(value)) return false;
            return true;
        }

        @Override public boolean contains(Object value) {
            if (!source.contains(value)) return false;
            @SuppressWarnings("unchecked") E alias = (E) value;
            // The comparator may equate a query with a different stored
            // representative; visibility belongs to the latter.
            return predicate.test(source.ceiling(alias));
        }

        @Override public boolean add(E value) {
            if (!allowAdd) throw new UnsupportedOperationException("map key-set addition");
            if (!predicate.test(value)) throw new IllegalArgumentException("element rejected by filtered view");
            return source.add(value);
        }

        /** Reject every invalid input before making a predicate-admitted batch mutation. */
        @Override public boolean addAll(Collection<? extends E> values) {
            if (!allowAdd) throw new UnsupportedOperationException("map key-set addition");
            Objects.requireNonNull(values, "values");
            // Capture exactly the references admitted in this traversal. Reopening a lazy
            // input could otherwise write different, never-validated occurrences.
            ArrayList<E> admitted = new ArrayList<>();
            for (E value : values) {
                if (!predicate.test(value)) {
                    throw new IllegalArgumentException("element rejected by filtered view");
                }
                admitted.add(value);
            }
            return source.addAll(admitted);
        }

        @Override public boolean remove(Object value) {
            return contains(value) && source.remove(value);
        }

        @Override public void clear() { removeIf(value -> true); }

        @Override public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter, "filter");
            boolean modified = false;
            Iterator<E> cursor = source.iterator();
            while (cursor.hasNext()) {
                E value = cursor.next();
                if (predicate.test(value) && filter.test(value)) {
                    cursor.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override public Iterator<E> iterator() { return cursor(false); }
        @Override public Iterator<E> descendingIterator() { return cursor(true); }

        private Iterator<E> cursor(boolean descending) {
            return new Iterator<>() {
                private boolean started;
                private boolean prepared;
                private boolean exhausted;
                private boolean removable;
                private E previous;
                private E pending;

                private void prepare() {
                    if (prepared || exhausted) return;
                    Iterator<E> sourceCursor = !started
                            ? (descending ? source.descendingIterator() : source.iterator())
                            : (descending ? source.headSet(previous, false).descendingIterator()
                                    : source.tailSet(previous, false).iterator());
                    while (sourceCursor.hasNext()) {
                        E value = sourceCursor.next();
                        if (predicate.test(value)) {
                            pending = value;
                            prepared = true;
                            return;
                        }
                    }
                    exhausted = true;
                }

                @Override public boolean hasNext() {
                    prepare();
                    return prepared;
                }

                @Override public E next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    E value = pending;
                    pending = null;
                    prepared = false;
                    previous = value;
                    started = true;
                    removable = true;
                    return value;
                }

                @Override public void remove() {
                    if (!removable) throw new IllegalStateException("next required");
                    source.remove(previous);
                    removable = false;
                    prepared = false;
                    exhausted = false;
                }
            };
        }

        private E seek(Iterator<E> sourceCursor) {
            return firstMatch(sourceCursor, predicate);
        }

        /** One-seek normal path; recover only invalid bounded-range construction. */
        private E bounded(E query, boolean forward, boolean inclusive) {
            NavigableSet<E> range;
            try {
                range = forward ? source.tailSet(query, inclusive) : source.headSet(query, inclusive);
            } catch (IllegalArgumentException outOfRange) {
                Iterator<E> bounded = forward ? source.iterator() : source.descendingIterator();
                Comparator<? super E> comparator = source.comparator();
                while (bounded.hasNext()) {
                    E stored = bounded.next();
                    int relation = compareStored(stored, query, comparator);
                    if ((forward ? relation > 0 || (inclusive && relation == 0)
                            : relation < 0 || (inclusive && relation == 0))
                            && predicate.test(stored)) {
                        return stored;
                    }
                }
                return null;
            }
            Iterator<E> cursor = forward ? range.iterator() : range.descendingIterator();
            return seek(cursor);
        }

        @Override public E lower(E value) { return bounded(value, false, false); }
        @Override public E floor(E value) { return bounded(value, false, true); }
        @Override public E ceiling(E value) { return bounded(value, true, true); }
        @Override public E higher(E value) { return bounded(value, true, false); }
        @Override public E first() {
            Iterator<E> cursor = iterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            return cursor.next();
        }
        @Override public E last() {
            Iterator<E> cursor = descendingIterator();
            if (!cursor.hasNext()) throw new NoSuchElementException();
            return cursor.next();
        }
        @Override public E pollFirst() { return poll(iterator()); }
        @Override public E pollLast() { return poll(descendingIterator()); }
        private E poll(Iterator<E> cursor) {
            if (!cursor.hasNext()) return null;
            E selected = cursor.next();
            cursor.remove();
            return selected;
        }

        @Override public NavigableSet<E> descendingSet() {
            return new SetView<>(source.descendingSet(), predicate, allowAdd);
        }
        @Override public NavigableSet<E> subSet(E from, boolean fromInclusive,
                E to, boolean toInclusive) {
            return new SetView<>(source.subSet(from, fromInclusive, to, toInclusive), predicate, allowAdd);
        }
        @Override public NavigableSet<E> headSet(E to, boolean inclusive) {
            return new SetView<>(source.headSet(to, inclusive), predicate, allowAdd);
        }
        @Override public NavigableSet<E> tailSet(E from, boolean inclusive) {
            return new SetView<>(source.tailSet(from, inclusive), predicate, allowAdd);
        }
        @Override public SortedSet<E> subSet(E from, E to) {
            return subSet(from, true, to, false);
        }
        @Override public SortedSet<E> headSet(E to) { return headSet(to, false); }
        @Override public SortedSet<E> tailSet(E from) { return tailSet(from, true); }
    }

    /** Call-local parallel reference lanes; no eager per-mapping Entry object. */
    private static final class CapturedEntries<K, V> extends AbstractMap<K, V> {
        private final ArrayList<K> keys;
        private final ArrayList<V> values;

        CapturedEntries(ArrayList<K> keys, ArrayList<V> values) {
            this.keys = keys;
            this.values = values;
        }

        @Override public int size() { return keys.size(); }

        @Override public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return keys.size(); }
                @Override public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<>() {
                        private int index;
                        @Override public boolean hasNext() { return index < keys.size(); }
                        @Override public Entry<K, V> next() {
                            if (!hasNext()) throw new NoSuchElementException();
                            int slot = index++;
                            return new SimpleImmutableEntry<>(keys.get(slot), values.get(slot));
                        }
                    };
                }
            };
        }
    }

    private static final class MapView<K, V> extends AbstractMap<K, V>
            implements NavigableMap<K, V> {
        private final NavigableMap<K, V> source;
        private final BiPredicate<? super K, ? super V> predicate;

        private MapView(NavigableMap<K, V> source,
                BiPredicate<? super K, ? super V> predicate) {
            this.source = Objects.requireNonNull(source, "source");
            this.predicate = Objects.requireNonNull(predicate, "predicate");
        }

        private boolean visible(Entry<K, V> entry) {
            return entry != null && predicate.test(entry.getKey(), entry.getValue());
        }

        /** Native M3 backing: skip Map.Entry materialization for internal reads. */
        @SuppressWarnings("unchecked")
        private M3TreeMap<K, V> slotOwner() {
            return source instanceof M3TreeMap<?, ?> m3 ? (M3TreeMap<K, V>) m3 : null;
        }

        private boolean visibleSlot(M3TreeMap<K, V> owner, int slot) {
            return predicate.test(owner.keyAt(slot), owner.valueAt(slot));
        }

        private Entry<K, V> seek(Entry<K, V> candidate, boolean forward) {
            while (candidate != null && !visible(candidate)) {
                candidate = forward ? source.higherEntry(candidate.getKey())
                        : source.lowerEntry(candidate.getKey());
            }
            return snapshot(candidate);
        }

        private Entry<K, V> snapshot(Entry<K, V> entry) {
            return entry == null ? null : new SimpleImmutableEntry<>(entry);
        }

        @Override public Comparator<? super K> comparator() { return source.comparator(); }

        @Override public int size() {
            M3TreeMap<K, V> owner = slotOwner();
            int count = 0;
            if (owner != null) {
                for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                    if (visibleSlot(owner, slot)) count++;
                }
            } else {
                for (Entry<K, V> entry : source.entrySet()) if (visible(entry)) count++;
            }
            return count;
        }

        @Override public boolean isEmpty() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                    if (visibleSlot(owner, slot)) return false;
                }
            } else {
                for (Entry<K, V> entry : source.entrySet()) if (visible(entry)) return false;
            }
            return true;
        }

        /**
         * Resolve the actual retained key and value, not a comparator-equal
         * caller alias. This lookup is read-only and allocates no M3 entries.
         */
        private Entry<K, V> storedEntry(Object key) {
            if (!source.containsKey(key)) return null;
            @SuppressWarnings("unchecked") K alias = (K) key;
            return source.floorEntry(alias);
        }

        private boolean hasVisibleKey(Object key) {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                int slot = owner.findSlot(key);
                return slot >= 0 && visibleSlot(owner, slot);
            }
            return visible(storedEntry(key));
        }

        @Override public boolean containsKey(Object key) { return hasVisibleKey(key); }
        @Override public V get(Object key) {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                int slot = owner.findSlot(key);
                if (slot < 0) return null;
                V value = owner.valueAt(slot);
                return predicate.test(owner.keyAt(slot), value) ? value : null;
            }
            Entry<K, V> retained = storedEntry(key);
            return visible(retained) ? retained.getValue() : null;
        }

        @Override public V put(K key, V value) {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                int slot = owner.findSlot(key);
                K effective = slot < 0 ? key : owner.keyAt(slot);
                if (!predicate.test(effective, value)) {
                    throw new IllegalArgumentException("mapping rejected by filtered view");
                }
                V prior = null;
                if (slot >= 0) {
                    V old = owner.valueAt(slot);
                    if (predicate.test(effective, old)) prior = old;
                }
                source.put(key, value);
                return prior;
            }
            Entry<K, V> previous = storedEntry(key);
            K effective = previous == null ? key : previous.getKey();
            if (!predicate.test(effective, value)) {
                throw new IllegalArgumentException("mapping rejected by filtered view");
            }
            V prior = visible(previous) ? previous.getValue() : null;
            source.put(key, value);
            return prior;
        }

        /**
         * Validate the effective *stored* representatives before any writes.
         * The transient array-backed M3TreeMap holds first-occurrence keys for new
         * comparator classes; it is not a retained element/entry cache.
         * This also handles input maps with distinct comparator-equal keys. Each key
         * and value reference is captured once; the owner's bulk call reads that exact
         * encounter sequence rather than reopening the incoming view. This is not a
         * deep copy or a transaction: owner range/type/comparator failures retain the
         * owner's partial-write semantics. Predicate/input-read failures precede writes.
         */
        @Override public void putAll(Map<? extends K, ? extends V> incoming) {
            Objects.requireNonNull(incoming, "incoming");
            NavigableMap<K, K> newRepresentatives = null;
            ArrayList<K> admittedKeys = new ArrayList<>();
            ArrayList<V> admittedValues = new ArrayList<>();
            M3TreeMap<K, V> owner = slotOwner();
            for (Entry<? extends K, ? extends V> entry : incoming.entrySet()) {
                K key = entry.getKey();
                int existingSlot = owner == null ? -1 : owner.findSlot(key);
                Entry<K, V> existing = owner == null ? storedEntry(key) : null;
                K effective;
                if (existingSlot >= 0) {
                    effective = owner.keyAt(existingSlot);
                } else if (existing != null) {
                    effective = existing.getKey();
                } else {
                    if (newRepresentatives == null) {
                        // Call-local sorted index: the M3 AVL owner uses reference and
                        // primitive topology arrays, never JDK TreeMap.Node storage.
                        newRepresentatives = new M3TreeMap<>(source.comparator());
                    }
                    if (newRepresentatives.containsKey(key)) {
                        effective = newRepresentatives.get(key);
                    } else {
                        newRepresentatives.put(key, key);
                        effective = key;
                    }
                }
                V value = entry.getValue();
                if (!predicate.test(effective, value)) {
                    throw new IllegalArgumentException("mapping rejected by filtered view");
                }
                admittedKeys.add(key);
                admittedValues.add(value);
            }
            // Direct bulk write after complete predicate validation avoids API
            // entries for M3's own slot-backed owner; retain third-party putAll.
            if (owner != null) {
                for (int i = 0; i < admittedKeys.size(); i++) {
                    owner.put(admittedKeys.get(i), admittedValues.get(i));
                }
            } else {
                source.putAll(new CapturedEntries<>(admittedKeys, admittedValues));
            }
        }

        @Override public V remove(Object key) {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                int slot = owner.findSlot(key);
                return slot >= 0 && visibleSlot(owner, slot) ? owner.remove(key) : null;
            }
            return hasVisibleKey(key) ? source.remove(key) : null;
        }

        @Override public void clear() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                for (int slot = owner.firstSlot(); slot >= 0;) {
                    int next = owner.nextSlot(slot);
                    if (visibleSlot(owner, slot)) owner.remove(owner.keyAt(slot));
                    slot = next;
                }
                return;
            }
            Iterator<Entry<K, V>> cursor = source.entrySet().iterator();
            while (cursor.hasNext()) if (visible(cursor.next())) cursor.remove();
        }

        @Override public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override public int size() { return MapView.this.size(); }
                @Override public boolean isEmpty() { return MapView.this.isEmpty(); }
                @Override public void clear() { MapView.this.clear(); }
                @Override public boolean contains(Object value) {
                    if (!(value instanceof Entry<?, ?> entry)) return false;
                    return MapView.this.containsKey(entry.getKey())
                            && Objects.equals(MapView.this.get(entry.getKey()), entry.getValue());
                }
                @Override public boolean remove(Object value) {
                    if (!contains(value)) return false;
                    Entry<?, ?> entry = (Entry<?, ?>) value;
                    source.remove(entry.getKey());
                    return true;
                }
                @Override public Iterator<Entry<K, V>> iterator() {
                    return entryCursor();
                }
            };
        }

        /**
         * JDK values() API requests a collection view, not one Entry per value.
         * The M3 owner visits physical slots and returns value references directly.
         */
        @Override public Collection<V> values() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) return super.values();
            return new AbstractCollection<>() {
                @Override public int size() { return MapView.this.size(); }
                @Override public void clear() { MapView.this.clear(); }
                @Override public Iterator<V> iterator() {
                    return new Iterator<>() {
                        private int next = owner.firstSlot();
                        private int expected = owner.revision();
                        private K lastKey;
                        private boolean removable;

                        private void check() {
                            if (expected != owner.revision()) {
                                throw new ConcurrentModificationException();
                            }
                        }

                        @Override public boolean hasNext() {
                            check();
                            while (next >= 0 && !visibleSlot(owner, next)) {
                                next = owner.nextSlot(next);
                            }
                            return next >= 0;
                        }

                        @Override public V next() {
                            if (!hasNext()) throw new NoSuchElementException();
                            int slot = next;
                            next = owner.nextSlot(slot);
                            lastKey = owner.keyAt(slot);
                            removable = true;
                            return owner.valueAt(slot);
                        }

                        @Override public void remove() {
                            check();
                            if (!removable) throw new IllegalStateException("next required");
                            owner.remove(lastKey);
                            expected = owner.revision();
                            removable = false;
                            lastKey = null;
                        }
                    };
                }
            };
        }

        @Override public void forEach(BiConsumer<? super K, ? super V> action) {
            Objects.requireNonNull(action, "action");
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) {
                for (Entry<K, V> entry : entrySet()) {
                    action.accept(entry.getKey(), entry.getValue());
                }
                return;
            }
            int expected = owner.revision();
            for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                K key = owner.keyAt(slot);
                V value = owner.valueAt(slot);
                if (predicate.test(key, value)) action.accept(key, value);
                if (owner.revision() != expected) throw new ConcurrentModificationException();
            }
        }

        @Override public boolean containsValue(Object value) {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) return super.containsValue(value);
            for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                K key = owner.keyAt(slot);
                V stored = owner.valueAt(slot);
                if (predicate.test(key, stored) && Objects.equals(value, stored)) return true;
            }
            return false;
        }

        @Override public void replaceAll(
                BiFunction<? super K, ? super V, ? extends V> action) {
            Objects.requireNonNull(action, "action");
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) {
                for (Entry<K, V> entry : entrySet()) {
                    entry.setValue(action.apply(entry.getKey(), entry.getValue()));
                }
                return;
            }
            for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                K key = owner.keyAt(slot);
                V old = owner.valueAt(slot);
                if (!predicate.test(key, old)) continue;
                int expected = owner.revision();
                V value = action.apply(key, old);
                if (expected != owner.revision()) {
                    throw new ConcurrentModificationException();
                }
                put(key, value);
            }
        }

        @Override public int hashCode() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) return super.hashCode();
            int hash = 0;
            for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                K key = owner.keyAt(slot);
                V value = owner.valueAt(slot);
                if (predicate.test(key, value)) {
                    hash += Objects.hashCode(key) ^ Objects.hashCode(value);
                }
            }
            return hash;
        }

        @Override public boolean equals(Object other) {
            if (other == this) return true;
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) return super.equals(other);
            if (!(other instanceof Map<?, ?> map) || map.size() != size()) return false;
            try {
                for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                    K key = owner.keyAt(slot);
                    V value = owner.valueAt(slot);
                    if (!predicate.test(key, value)) continue;
                    if (!Objects.equals(value, map.get(key))
                            || value == null && !map.containsKey(key)) {
                        return false;
                    }
                }
            } catch (ClassCastException | NullPointerException ignored) {
                return false;
            }
            return true;
        }

        @Override public String toString() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) return super.toString();
            StringBuilder out = new StringBuilder("{");
            boolean comma = false;
            for (int slot = owner.firstSlot(); slot >= 0; slot = owner.nextSlot(slot)) {
                K key = owner.keyAt(slot);
                V value = owner.valueAt(slot);
                if (!predicate.test(key, value)) continue;
                if (comma) out.append(", ");
                comma = true;
                out.append(key == this ? "(this Map)" : key).append('=')
                        .append(value == this ? "(this Map)" : value);
            }
            return out.append('}').toString();
        }

        private Iterator<Entry<K, V>> entryCursor() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) return new IndexedEntryCursor(owner);
            return new Iterator<>() {
                private boolean started;
                private boolean prepared;
                private boolean exhausted;
                private boolean removable;
                private K previous;
                private Entry<K, V> pending;

                private void prepare() {
                    if (prepared || exhausted) return;
                    Entry<K, V> candidate = started
                            ? source.higherEntry(previous) : source.firstEntry();
                    while (candidate != null && !visible(candidate)) {
                        candidate = source.higherEntry(candidate.getKey());
                    }
                    if (candidate == null) exhausted = true;
                    else {
                        pending = candidate;
                        prepared = true;
                    }
                }

                @Override public boolean hasNext() { prepare(); return prepared; }
                @Override public Entry<K, V> next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    K key = pending.getKey();
                    pending = null;
                    prepared = false;
                    started = true;
                    previous = key;
                    removable = true;
                    return new LiveEntry(key);
                }
                @Override public void remove() {
                    if (!removable) throw new IllegalStateException("next required");
                    source.remove(previous);
                    removable = false;
                    prepared = false;
                    exhausted = false;
                }
            };
        }

        /** Only entrySet().iterator() API calls construct entries, never scans. */
        private final class IndexedEntryCursor implements Iterator<Entry<K, V>> {
            private final M3TreeMap<K, V> owner;
            private int next;
            private int expected;
            private K lastKey;
            private boolean removable;

            IndexedEntryCursor(M3TreeMap<K, V> owner) {
                this.owner = owner;
                next = owner.firstSlot();
                expected = owner.revision();
            }

            private void check() {
                if (expected != owner.revision()) throw new ConcurrentModificationException();
            }

            @Override public boolean hasNext() {
                check();
                while (next >= 0 && !visibleSlot(owner, next)) {
                    next = owner.nextSlot(next);
                }
                return next >= 0;
            }

            @Override public Entry<K, V> next() {
                if (!hasNext()) throw new NoSuchElementException();
                int slot = next;
                next = owner.nextSlot(slot);
                lastKey = owner.keyAt(slot);
                removable = true;
                return new LiveEntry(lastKey);
            }

            @Override public void remove() {
                check();
                if (!removable) throw new IllegalStateException("next required");
                owner.remove(lastKey);
                expected = owner.revision();
                removable = false;
                lastKey = null;
            }
        }

        private final class LiveEntry implements Entry<K, V> {
            private final K key;
            private LiveEntry(K key) { this.key = key; }
            @Override public K getKey() { return key; }
            @Override public V getValue() { return MapView.this.get(key); }
            @Override public V setValue(V value) {
                if (!hasVisibleKey(key)) throw new IllegalStateException("mapping no longer visible");
                return MapView.this.put(key, value);
            }
            @Override public boolean equals(Object other) {
                return other instanceof Entry<?, ?> entry
                        && Objects.equals(key, entry.getKey())
                        && Objects.equals(getValue(), entry.getValue());
            }
            @Override public int hashCode() {
                return Objects.hashCode(key) ^ Objects.hashCode(getValue());
            }
            @Override public String toString() { return key + "=" + getValue(); }
        }

        /** Search hidden candidates by AVL offsets rather than transient API entries. */
        private int visibleNear(M3TreeMap<K, V> owner, K key, int relation) {
            int slot = owner.nearIndexedSlot(key, relation);
            while (slot >= 0 && !visibleSlot(owner, slot)) {
                slot = relation < 0 ? owner.previousIndexedSlot(slot) : owner.nextSlot(slot);
            }
            return slot;
        }

        private int visibleEnd(M3TreeMap<K, V> owner, boolean first) {
            int slot = first ? owner.firstSlot() : owner.lastSlot();
            while (slot >= 0 && !visibleSlot(owner, slot)) {
                slot = first ? owner.nextSlot(slot) : owner.previousIndexedSlot(slot);
            }
            return slot;
        }

        private Entry<K, V> requestedEntry(M3TreeMap<K, V> owner, int slot) {
            // Only a public Entry-valued API reaches this allocation boundary.
            return slot < 0 ? null : new SimpleImmutableEntry<>(owner.keyAt(slot), owner.valueAt(slot));
        }

        private K requestedKey(M3TreeMap<K, V> owner, int slot) {
            return slot < 0 ? null : owner.keyAt(slot);
        }

        @Override public Entry<K, V> lowerEntry(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? seek(source.lowerEntry(key), false)
                    : requestedEntry(owner, visibleNear(owner, key, -2));
        }
        @Override public K lowerKey(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? keyOf(lowerEntry(key)) : requestedKey(owner, visibleNear(owner, key, -2));
        }
        @Override public Entry<K, V> floorEntry(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? seek(source.floorEntry(key), false)
                    : requestedEntry(owner, visibleNear(owner, key, -1));
        }
        @Override public K floorKey(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? keyOf(floorEntry(key)) : requestedKey(owner, visibleNear(owner, key, -1));
        }
        @Override public Entry<K, V> ceilingEntry(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? seek(source.ceilingEntry(key), true)
                    : requestedEntry(owner, visibleNear(owner, key, 1));
        }
        @Override public K ceilingKey(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? keyOf(ceilingEntry(key)) : requestedKey(owner, visibleNear(owner, key, 1));
        }
        @Override public Entry<K, V> higherEntry(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? seek(source.higherEntry(key), true)
                    : requestedEntry(owner, visibleNear(owner, key, 2));
        }
        @Override public K higherKey(K key) {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? keyOf(higherEntry(key)) : requestedKey(owner, visibleNear(owner, key, 2));
        }
        @Override public Entry<K, V> firstEntry() {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? seek(source.firstEntry(), true)
                    : requestedEntry(owner, visibleEnd(owner, true));
        }
        @Override public Entry<K, V> lastEntry() {
            M3TreeMap<K, V> owner = slotOwner();
            return owner == null ? seek(source.lastEntry(), false)
                    : requestedEntry(owner, visibleEnd(owner, false));
        }
        private K keyOf(Entry<K, V> entry) { return entry == null ? null : entry.getKey(); }
        @Override public K firstKey() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                int slot = visibleEnd(owner, true);
                if (slot < 0) throw new NoSuchElementException();
                return owner.keyAt(slot);
            }
            Entry<K, V> result = firstEntry();
            if (result == null) throw new NoSuchElementException();
            return result.getKey();
        }
        @Override public K lastKey() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) {
                int slot = visibleEnd(owner, false);
                if (slot < 0) throw new NoSuchElementException();
                return owner.keyAt(slot);
            }
            Entry<K, V> result = lastEntry();
            if (result == null) throw new NoSuchElementException();
            return result.getKey();
        }
        @Override public Entry<K, V> pollFirstEntry() {
            return slotOwner() == null ? poll(entryCursor()) : pollIndexed(true);
        }
        @Override public Entry<K, V> pollLastEntry() {
            return slotOwner() == null ? poll(descendingMap().entrySet().iterator()) : pollIndexed(false);
        }
        private Entry<K, V> pollIndexed(boolean first) {
            M3TreeMap<K, V> owner = slotOwner();
            int slot = visibleEnd(owner, first);
            if (slot < 0) return null;
            Entry<K, V> result = requestedEntry(owner, slot);
            owner.remove(owner.keyAt(slot));
            return result;
        }
        private Entry<K, V> poll(Iterator<Entry<K, V>> cursor) {
            if (!cursor.hasNext()) return null;
            Entry<K, V> entry = snapshot(cursor.next());
            cursor.remove();
            return entry;
        }
        @Override public NavigableMap<K, V> descendingMap() {
            return new MapView<>(source.descendingMap(), predicate);
        }
        @Override public NavigableSet<K> navigableKeySet() {
            return new SetView<>(source.navigableKeySet(), this::containsKey, false);
        }
        @Override public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }
        @Override public Set<K> keySet() { return navigableKeySet(); }
        @Override public NavigableMap<K, V> subMap(K from, boolean inclusiveFrom,
                K to, boolean inclusiveTo) {
            return new MapView<>(source.subMap(from, inclusiveFrom, to, inclusiveTo), predicate);
        }
        @Override public NavigableMap<K, V> headMap(K to, boolean inclusive) {
            return new MapView<>(source.headMap(to, inclusive), predicate);
        }
        @Override public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
            return new MapView<>(source.tailMap(from, inclusive), predicate);
        }
        @Override public SortedMap<K, V> subMap(K from, K to) {
            return subMap(from, true, to, false);
        }
        @Override public SortedMap<K, V> headMap(K to) { return headMap(to, false); }
        @Override public SortedMap<K, V> tailMap(K from) { return tailMap(from, true); }
    }
}
