/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Generated through the source-sealed Synexia M3 recipe; do not edit output directly.
 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ObjIntConsumer;

/**
 * Equality-keyed counted collection backed by the existing M3 parallel-array map.
 * No occurrence node or retained Entry object is required. One M3 map mapping
 * is retained per distinct key, with a strictly positive int occurrence count.
 * The long total is exact and Collection.size() saturates at Integer.MAX_VALUE.
 *
 * <p>This core class deliberately implements no Guava/Apache/Eclipse API:
 * those contract-bearing interfaces are adapted in the optional interop module.
 * Null is a valid key, just as it is in the existing M3LinkedHashMap. There is
 * no locking or concurrent modification support. Iterators fail fast on any
 * count change, including count changes that do not structurally change the
 * underlying map. Iteration over occurrences holds a snapshot of distinct key
 * references only; it never duplicates entries for repeated occurrences.
 */
public final class M3CountedBag<E> extends AbstractCollection<E> {
    private final M3LinkedHashMap<E, Integer> counts = new M3LinkedHashMap<>();
    private long total;
    private long revision;
    private transient Set<E> distinctView;

    /** Empty counted owner; no extra element storage allocated. */
    public M3CountedBag() { }

    /** Exact, potentially above-Integer.MAX_VALUE total occurrence count. */
    public long totalOccurrences() { return total; }
    public long revision() { return revision; }
    public int distinctSize() { return counts.size(); }
    public int count(Object element) {
        Integer current = counts.get(element);
        return current == null ? 0 : current;
    }

    /** Adds a bounded positive multiplicity; zero is a no-op. Returns old count. */
    public int addOccurrences(E element, int occurrences) {
        requireNonnegative(occurrences);
        int old = count(element);
        if (occurrences == 0) return old;
        if (old > Integer.MAX_VALUE - occurrences) {
            throw new IllegalArgumentException("occurrence count exceeds Integer.MAX_VALUE");
        }
        long nextTotal = Math.addExact(total, occurrences);
        counts.put(element, old + occurrences);
        total = nextTotal;
        revision++;
        return old;
    }

    /** Removes up to the requested multiplicity; zero is a no-op. Returns old count. */
    public int removeOccurrences(Object element, int occurrences) {
        requireNonnegative(occurrences);
        int old = count(element);
        if (occurrences == 0 || old == 0) return old;
        int removed = Math.min(old, occurrences);
        if (removed == old) counts.remove(element);
        else {
            @SuppressWarnings("unchecked") E key = (E) element;
            counts.put(key, old - removed);
        }
        total -= removed;
        revision++;
        return old;
    }

    /** Updates a key count (including removal at zero); returns old count. */
    public int setOccurrences(E element, int occurrences) {
        requireNonnegative(occurrences);
        int old = count(element);
        if (old == occurrences) return old;
        long nextTotal = Math.addExact(total, (long) occurrences - old);
        if (occurrences == 0) counts.remove(element);
        else counts.put(element, occurrences);
        total = nextTotal;
        revision++;
        return old;
    }

    /** Compare-and-set for single-threaded callers, not an atomic concurrent operation. */
    public boolean setOccurrences(E element, int expected, int replacement) {
        requireNonnegative(expected);
        requireNonnegative(replacement);
        if (count(element) != expected) return false;
        setOccurrences(element, replacement);
        return true;
    }

    private static void requireNonnegative(int count) {
        if (count < 0) throw new IllegalArgumentException("negative occurrences");
    }

    @Override public boolean add(E element) { addOccurrences(element, 1); return true; }
    @Override public boolean remove(Object element) { return removeOccurrences(element, 1) != 0; }
    @Override public boolean contains(Object element) { return count(element) > 0; }
    @Override public boolean isEmpty() { return total == 0; }
    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, total); }
    @Override public void clear() {
        if (total == 0) return;
        counts.clear();
        total = 0;
        revision++;
    }

    /** Enumerates distinct elements in M3 insertion order; removal drops all copies. */
    public Set<E> distinctElements() {
        if (distinctView == null) distinctView = new DistinctElements();
        return distinctView;
    }

    /** Distinct key/count callback; rejects modification from the callback. */
    public void forEachCount(ObjIntConsumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        long expected = revision;
        for (E key : counts.keySet()) {
            verify(expected);
            action.accept(key, count(key));
            verify(expected);
        }
    }

    private void verify(long expected) {
        if (revision != expected) throw new ConcurrentModificationException("bag counts changed");
    }

    @Override public Iterator<E> iterator() {
        final Object[] keys = counts.keySet().toArray();
        return new Iterator<>() {
            private long expected = revision;
            private int keyIndex;
            private int remaining;
            private E current;
            private E delivered;
            private boolean canRemove;
            private void prepare() {
                verify(expected);
                while (remaining == 0 && keyIndex < keys.length) {
                    @SuppressWarnings("unchecked") E key = (E) keys[keyIndex++];
                    current = key;
                    remaining = count(key);
                }
            }
            @Override public boolean hasNext() { prepare(); return remaining != 0; }
            @Override public E next() {
                if (!hasNext()) throw new NoSuchElementException();
                remaining--;
                delivered = current;
                canRemove = true;
                return delivered;
            }
            @Override public void remove() {
                verify(expected);
                if (!canRemove) throw new IllegalStateException("no removable occurrence");
                removeOccurrences(delivered, 1);
                expected = revision;
                canRemove = false;
                delivered = null;
            }
        };
    }

    private final class DistinctElements extends AbstractSet<E> {
        @Override public int size() { return counts.size(); }
        @Override public boolean contains(Object o) { return count(o) != 0; }
        @Override public boolean remove(Object key) {
            int existing = count(key);
            return existing != 0 && removeOccurrences(key, existing) != 0;
        }
        @Override public void clear() { M3CountedBag.this.clear(); }
        @Override public Iterator<E> iterator() {
            final Object[] keys = counts.keySet().toArray();
            return new Iterator<>() {
                private int cursor;
                private long expected = revision;
                private E last;
                private boolean removable;
                @Override public boolean hasNext() { verify(expected); return cursor < keys.length; }
                @Override public E next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    @SuppressWarnings("unchecked") E key = (E) keys[cursor++];
                    last = key;
                    removable = true;
                    return key;
                }
                @Override public void remove() {
                    verify(expected);
                    if (!removable) throw new IllegalStateException();
                    int n = count(last);
                    if (n == 0) throw new ConcurrentModificationException();
                    removeOccurrences(last, n);
                    expected = revision;
                    removable = false;
                }
            };
        }
    }

    /**
     * Detached, stable sort of distinct count entries. Only the primitive index
     * vector changes order; source keys and counts stay in private snapshot lanes.
     * Comparator equivalence does not collapse distinct equality keys. This is
     * a read-only snapshot and not a Guava/Apache SortedMultiset implementation.
     */
    public CountSnapshot<E> sortedCounts(Comparator<? super E> comparator) {
        Object[] keys = counts.keySet().toArray();
        int[] occurrences = new int[keys.length];
        int[] indexes = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            occurrences[i] = count(keys[i]);
            indexes[i] = i;
        }
        int[] scratch = new int[indexes.length];
        for (long width = 1; width < indexes.length; width <<= 1) {
            for (long base = 0; base < indexes.length; base += width * 2) {
                int left = (int) base;
                int middle = (int) Math.min(indexes.length, base + width);
                int right = (int) Math.min(indexes.length, base + width * 2);
                int a = left, b = middle, target = left;
                while (a < middle && b < right) {
                    int cmp = compare(keys[indexes[a]], keys[indexes[b]], comparator);
                    if (cmp <= 0) scratch[target++] = indexes[a++];
                    else scratch[target++] = indexes[b++];
                }
                while (a < middle) scratch[target++] = indexes[a++];
                while (b < right) scratch[target++] = indexes[b++];
            }
            int[] swap = indexes; indexes = scratch; scratch = swap;
        }
        return new CountSnapshot<>(keys, occurrences, indexes, 0, indexes.length);
    }

    @SuppressWarnings("unchecked")
    private static <E> int compare(Object left, Object right, Comparator<? super E> comparator) {
        return comparator == null ? ((Comparable<? super E>) left).compareTo((E) right)
                : comparator.compare((E) left, (E) right);
    }

    /** Immutable counted projection over one key/count snapshot and an index slice. */
    public static final class CountSnapshot<E> {
        private final Object[] keys;
        private final int[] counts;
        private final int[] order;
        private final int from, to;
        private CountSnapshot(Object[] keys, int[] counts, int[] order, int from, int to) {
            this.keys = keys; this.counts = counts; this.order = order;
            this.from = from; this.to = to;
        }
        public int distinctSize() { return to - from; }
        @SuppressWarnings("unchecked")
        public E elementAt(int ordinal) {
            return (E) keys[order[from + Objects.checkIndex(ordinal, distinctSize())]];
        }
        public int countAt(int ordinal) {
            return counts[order[from + Objects.checkIndex(ordinal, distinctSize())]];
        }
        public CountSnapshot<E> slice(int start, int length) {
            Objects.checkFromIndexSize(start, length, distinctSize());
            return new CountSnapshot<>(keys, counts, order, from + start, from + start + length);
        }
        /** One evaluation per currently visible distinct key, then reusable index facts. */
        public CountSnapshot<E> filtered(Predicate<? super E> predicate) {
            Objects.requireNonNull(predicate, "predicate");
            int[] chosen = new int[distinctSize()];
            int kept = 0;
            for (int i = 0; i < distinctSize(); i++) {
                if (predicate.test(elementAt(i))) chosen[kept++] = order[from + i];
            }
            return new CountSnapshot<>(keys, counts, Arrays.copyOf(chosen, kept), 0, kept);
        }
        /** Original distinct positions, never exposes the owned primitive order array. */
        public int[] physicalIndexes() { return Arrays.copyOfRange(order, from, to); }
        public long totalOccurrences() {
            long sum = 0;
            for (int i = from; i < to; i++) sum += counts[order[i]];
            return sum;
        }
    }

    /** Distinct-key metadata filter, evaluated once and reused by Java/JNI mask selector.
     * The caller's state array is aligned with distinctElements iteration order.
     * Result is an independent count store; no predicates or objects cross JNI.
     */
    public M3CountedBag<E> selectedDistinct(byte[] states, int required,
                                            M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        Object[] keys = counts.keySet().toArray();
        if (states.length != keys.length) {
            throw new IllegalArgumentException("state length differs from distinct key count");
        }
        int[] rowIds = new int[keys.length];
        for (int i = 0; i < keys.length; i++) rowIds[i] = i;
        int[] chosen = M3IndexMaskCompactor.select(rowIds, states, required, backend);
        M3CountedBag<E> result = new M3CountedBag<>();
        for (int i : chosen) {
            @SuppressWarnings("unchecked") E key = (E) keys[i];
            result.addOccurrences(key, count(key));
        }
        return result;
    }
}
