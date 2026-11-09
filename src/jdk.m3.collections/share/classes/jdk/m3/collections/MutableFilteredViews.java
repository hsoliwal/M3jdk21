/* *****************************************************************************
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
// Modified 2026 by Hitesh Soliwal and contributors: preserve comparator metadata across filtered splits.
// Modified 2026 by Hitesh Soliwal and contributors: propagate mutable navigation comparator failures.
// Modified 2026 by Hitesh Soliwal and contributors: fail-fast physical-slot cursors and a once-per-entry values view over M3 owners (census2 P2-098).
package jdk.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Mutable, live filtering over JDK navigable owners without owning per-element nodes.
 *
 * <p>Unlike {@code FilteredViews} (read-only) and Commons predicated admission,
 * this wrapper hides mismatching elements already present in the owner. Mutations via
 * the wrapper can add/replace only matching values and remove only matching values;
 * underlying owner changes remain visible. Range and descending views retain the
 * underlying JDK comparator, range checks and reference identities.</p>
 *
 * <p>Predicates must be stable, deterministic and not mutate either owner. The
 * underlying owner must not be concurrently mutated without external synchronization;
 * iterators are cursor-based and do not promise the JDK's fail-fast detection.
 * Over an M3TreeMap or M3TreeSet owner the cursors walk its physical slots and are
 * fail-fast: the owner revision is read before the first slot and checked at every call and
 * after every predicate call and slot step; the predicate runs once per slot,
 * values() reads the slot's current value without a second predicate call, and an own
 * remove keeps the prepared successor.
 * Iterator/entry wrappers are created on demand, never stored per element. This is
 * not a snapshot, not a concurrent collection and not a transparent JNI facade.</p>
 */
public final class MutableFilteredViews {
    private MutableFilteredViews() { }

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

    //FLLDCIM: MRL - census2 P2-098, recipe FILTERED_VIEWS_OWNER v1 (crate m3jdk21-20261019, donor blob 883195b9b194 = retained variant pr1463): fail-fast physical-slot cursor
    /**
     * Physical-slot cursor shared by the set and map views over an M3 owner (an M3TreeMap or
     * the owner of an M3TreeSet; bounds and direction included). The owner revision is read
     * before the first slot and checked at every call and after every predicate call and slot
     * step: an owner change, a mutating predicate or comparator, and a prepared or exhausted
     * cursor fail fast, and remove checks it before its next-required state. The predicate runs
     * once per slot. remove erases the returned slot itself (no comparator lookup, so no
     * re-entrant window) and keeps the prepared successor slot.
     */
    private abstract static class SlotCursor<T> implements Iterator<T> {
        private final M3TreeMap<?, ?> slots;
        private int expected;
        private int pending;
        private int last = -1;
        private boolean prepared;

        SlotCursor(M3TreeMap<?, ?> slots, boolean fromLast) {
            this.slots = slots;
            expected = slots.revision();
            pending = slots.cursorStart(fromLast); // TREEMAP_ABA_GUARD: creation never fails
        }

        /** Predicate visibility of a live slot. */
        abstract boolean visible(int slot);
        /** The following slot in this cursor's direction, or -1. */
        abstract int step(int slot);
        /** The caller-visible element of a live slot. */
        abstract T take(int slot);

        final void check() { slots.check(expected); }

        @Override public final boolean hasNext() {
            check();
            if (prepared) return true;
            while (pending >= 0) {
                boolean visible = visible(pending);
                check();
                if (visible) {
                    prepared = true;
                    return true;
                }
                pending = step(pending);
                check();
            }
            return false;
        }

        @Override public final T next() {
            if (!hasNext()) throw new NoSuchElementException();
            int slot = pending;
            pending = step(slot);
            check();
            prepared = false;
            T result = take(slot);
            last = slot;
            return result;
        }

        @Override public final void remove() {
            check();
            if (last < 0) throw new IllegalStateException("next required");
            slots.eraseAt(last);
            expected = slots.revision();
            last = -1;
        }
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
        /** Keep the live iterator/size policy and retain ordering on split children. */
        @Override public Spliterator<E> spliterator() {
            return SlotSpliterators.sorted(this);
        }

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
            M3TreeMap<E, ?> owner = source instanceof M3TreeSet<E> m3 ? m3.indexedOwner() : null;
            if (owner != null) return new SlotCursor<E>(owner, descending) {
                @Override boolean visible(int slot) { return predicate.test(owner.keyAt(slot)); }
                @Override int step(int slot) {
                    return descending ? owner.previousIndexedSlot(slot) : owner.nextSlot(slot);
                }
                @Override E take(int slot) { return owner.keyAt(slot); }
            };
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

        /** One-seek owned path; external navigation never catches user comparator failures. */
        private E bounded(E query, boolean forward, boolean inclusive) {
            NavigableSet<E> range;
            if (source instanceof M3TreeSet<E> owner) {
                range = owner.rangeOrNull(query, forward, inclusive);
                if (range == null) return boundedFallback(query, forward, inclusive);
            } else {
                // Near-element queries accept keys outside bounded JDK views. Their
                // comparator failures are real failures, not range-rejection signals.
                E nearest = forward
                        ? (inclusive ? source.ceiling(query) : source.higher(query))
                        : (inclusive ? source.floor(query) : source.lower(query));
                if (nearest == null) return nullCandidate(query, forward, inclusive);
                if (predicate.test(nearest)) return nearest;
                // A returned representative lies inside the source range, so this
                // strict suffix/prefix is valid without catching arbitrary exceptions.
                range = forward ? source.tailSet(nearest, false) : source.headSet(nearest, false);
            }
            Iterator<E> cursor = forward ? range.iterator() : range.descendingIterator();
            return seek(cursor);
        }

        /** Distinguish an absent result from an actual null member without probing contains(null). */
        private E nullCandidate(E query, boolean forward, boolean inclusive) {
            // Reuse the original read-only view's null-disambiguation atom. Natural
            // order, including its reverse, cannot contain a null representative.
            if (source.comparator() == null) return null;
            NavigableSet<E> reverse = source.descendingSet();
            if (reverse.comparator() == null) return null;
            NavigableSet<E> ordered = forward ? source : reverse;
            Iterator<E> cursor = ordered.iterator();
            if (!cursor.hasNext()) return null;
            Comparator<? super E> order = ordered.comparator();
            int fromFirst = compareStored(query, cursor.next(), order);
            if (fromFirst < 0 || (fromFirst == 0 && inclusive)) {
                return seek(ordered.iterator());
            }
            int fromLast = compareStored(query, ordered.last(), order);
            if (fromLast > 0 || (fromLast == 0 && !inclusive)) return null;
            return seek(ordered.tailSet(query, inclusive).iterator());
        }

        private E boundedFallback(E query, boolean forward, boolean inclusive) {
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
            int count = 0;
            for (Entry<K, V> entry : source.entrySet()) if (visible(entry)) count++;
            return count;
        }

        @Override public boolean isEmpty() {
            for (Entry<K, V> entry : source.entrySet()) if (visible(entry)) return false;
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
            return visible(storedEntry(key));
        }

        @Override public boolean containsKey(Object key) { return hasVisibleKey(key); }
        @Override public V get(Object key) {
            Entry<K, V> retained = storedEntry(key);
            return visible(retained) ? retained.getValue() : null;
        }

        @Override public V put(K key, V value) {
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
         * The transient TreeMap holds only first-occurrence keys for new
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
            for (Entry<? extends K, ? extends V> entry : incoming.entrySet()) {
                K key = entry.getKey();
                Entry<K, V> existing = storedEntry(key);
                K effective;
                if (existing != null) {
                    effective = existing.getKey();
                } else {
                    if (newRepresentatives == null) {
                        newRepresentatives = new TreeMap<>(source.comparator());
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
            source.putAll(new CapturedEntries<>(admittedKeys, admittedValues));
        }

        @Override public V remove(Object key) {
            return hasVisibleKey(key) ? source.remove(key) : null;
        }

        @Override public void clear() {
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

        private Iterator<Entry<K, V>> entryCursor() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner != null) return new MapSlotCursor<Entry<K, V>>(owner) {
                @Override Entry<K, V> take(int slot) { return new LiveEntry(owner.keyAt(slot)); }
            };
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

        /** The array-backed AVL owner of an M3 map (bounds and direction included), else null. */
        private M3TreeMap<K, V> slotOwner() {
            return source instanceof M3TreeMap<K, V> m3 ? m3 : null;
        }

        /** Entries and values of an M3 owner share one slot walk; only the projection differs. */
        private abstract class MapSlotCursor<T> extends SlotCursor<T> {
            final M3TreeMap<K, V> owner;

            MapSlotCursor(M3TreeMap<K, V> owner) {
                super(owner, false);
                this.owner = owner;
            }

            @Override final boolean visible(int slot) {
                return predicate.test(owner.keyAt(slot), owner.valueAt(slot));
            }
            @Override final int step(int slot) { return owner.nextSlot(slot); }
        }

        //FLLDCIM: MRL - census2 P2-098, recipe FILTERED_VIEWS_OWNER v1 (crate m3jdk21-20261019, donor blob 883195b9b194 = retained variant pr1463): once-per-entry values view
        /**
         * Over an M3 owner values() walks the same slot cursor: the predicate runs once per
         * entry and each value is the slot's current payload. Other owners keep
         * AbstractMap.values() (an entry walk whose LiveEntry.getValue re-tests visibility).
         */
        @Override public Collection<V> values() {
            M3TreeMap<K, V> owner = slotOwner();
            if (owner == null) return super.values();
            return new AbstractCollection<>() {
                @Override public int size() { return MapView.this.size(); }
                @Override public boolean isEmpty() { return MapView.this.isEmpty(); }
                @Override public boolean contains(Object value) { return MapView.this.containsValue(value); }
                @Override public void clear() { MapView.this.clear(); }
                @Override public Iterator<V> iterator() {
                    return new MapSlotCursor<V>(owner) {
                        @Override V take(int slot) { return owner.valueAt(slot); }
                    };
                }
            };
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

        @Override public Entry<K, V> lowerEntry(K key) { return seek(source.lowerEntry(key), false); }
        @Override public K lowerKey(K key) { return keyOf(lowerEntry(key)); }
        @Override public Entry<K, V> floorEntry(K key) { return seek(source.floorEntry(key), false); }
        @Override public K floorKey(K key) { return keyOf(floorEntry(key)); }
        @Override public Entry<K, V> ceilingEntry(K key) { return seek(source.ceilingEntry(key), true); }
        @Override public K ceilingKey(K key) { return keyOf(ceilingEntry(key)); }
        @Override public Entry<K, V> higherEntry(K key) { return seek(source.higherEntry(key), true); }
        @Override public K higherKey(K key) { return keyOf(higherEntry(key)); }
        @Override public Entry<K, V> firstEntry() { return seek(source.firstEntry(), true); }
        @Override public Entry<K, V> lastEntry() { return seek(source.lastEntry(), false); }
        private K keyOf(Entry<K, V> entry) { return entry == null ? null : entry.getKey(); }
        @Override public K firstKey() {
            Entry<K, V> result = firstEntry();
            if (result == null) throw new NoSuchElementException();
            return result.getKey();
        }
        @Override public K lastKey() {
            Entry<K, V> result = lastEntry();
            if (result == null) throw new NoSuchElementException();
            return result.getKey();
        }
        @Override public Entry<K, V> pollFirstEntry() { return poll(entryCursor()); }
        @Override public Entry<K, V> pollLastEntry() { return poll(descendingMap().entrySet().iterator()); }
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
