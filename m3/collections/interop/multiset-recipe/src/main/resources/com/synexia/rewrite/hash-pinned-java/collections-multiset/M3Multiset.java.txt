/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.ObjIntConsumer;

/**
 * Mutable, null-tolerant Guava multiset over the existing M3 stable-slot map.
 * A distinct element maps to a row ordinal; positive counts occupy an int lane.
 * Deleted count slots encode the free list and are reused. No count wrapper,
 * occurrence object, retained Entry, or linked node is allocated per occurrence.
 * The existing generic map does retain a boxed Integer row ID per distinct key;
 * this facade does not claim a fully unboxed object-to-int directory.
 *
 * <p>Distinct traversal preserves first-insertion order; reinsertion appends.
 * The exact total is a long; Collection.size saturates at Integer.MAX_VALUE.
 * Entry objects are immutable snapshots created only at the entrySet boundary.
 * The entry set itself and element set are live, remove-through views.
 *
 * <p>Not thread-safe or Serializable. Keys must have stable, non-reentrant
 * hash/equality methods. Iterators fail fast on observed count changes, including
 * count-only changes, except their own remove. This is best-effort detection,
 * not a concurrency guarantee. Inherited bulk operations may partially complete
 * if a callback fails. No all-VM-failure rollback or universal speedup is claimed.
 *
 * @param <E> element type
 */
public final class M3Multiset<E> extends AbstractCollection<E> implements Multiset<E> {
    private final M3LinkedHashMap<E, Integer> rows = new M3LinkedHashMap<>();
    private int[] counts = new int[0];
    private int highWater;
    private int freeHead = -1;
    private long total;
    private int version;

    public M3Multiset() { }

    public static <E> M3Multiset<E> create() { return new M3Multiset<>(); }

    public static <E> M3Multiset<E> create(Iterable<? extends E> source) {
        Objects.requireNonNull(source, "source");
        M3Multiset<E> result = new M3Multiset<>();
        if (source instanceof Multiset<? extends E> bag) {
            bag.forEachEntry(result::add);
        } else {
            source.forEach(result::add);
        }
        return result;
    }

    /** Exact occurrence total, without expanding duplicate elements. */
    public long longSize() { return total; }
    @Override public int size() { return (int) Math.min(total, Integer.MAX_VALUE); }
    @Override public boolean isEmpty() { return rows.isEmpty(); }
    @Override public boolean contains(Object element) { return rows.containsKey(element); }
    @Override public int count(Object element) {
        Integer row = rows.get(element);
        return row == null ? 0 : counts[row];
    }

    private static void nonnegative(int value) {
        if (value < 0) throw new IllegalArgumentException("negative occurrences: " + value);
    }

    private void reserveCountSlot() {
        if (freeHead >= 0 || highWater < counts.length) return;
        int capacity = counts.length;
        long grown = Math.max(8L, (long) capacity + (capacity >>> 1) + 1L);
        int next = (int) Math.min(grown, Integer.MAX_VALUE - 8L);
        if (next <= capacity) throw new OutOfMemoryError("M3 count lane capacity exhausted");
        // Publish only after the array allocation/copy succeeds, before key admission.
        counts = Arrays.copyOf(counts, next);
    }

    private void free(int row) {
        counts[row] = -freeHead - 1;
        freeHead = row;
    }

    @Override public int add(E element, int occurrences) {
        nonnegative(occurrences);
        Integer row = rows.get(element);
        int previous = row == null ? 0 : counts[row];
        if (occurrences == 0) return previous;
        if (occurrences > Integer.MAX_VALUE - previous) {
            throw new IllegalArgumentException("element count exceeds Integer.MAX_VALUE");
        }
        if (row == null) {
            reserveCountSlot();
            int chosen = freeHead >= 0 ? freeHead : highWater;
            int nextFree = freeHead >= 0 ? -counts[chosen] - 1 : -1;
            // Existing M3 map owns hashing and reference lifetime. Do not publish
            // occurrence metadata until it has admitted the element/row mapping.
            rows.put(element, chosen);
            if (freeHead >= 0) freeHead = nextFree;
            else highWater++;
            counts[chosen] = occurrences;
        } else {
            counts[row] = previous + occurrences;
        }
        total += occurrences;
        version++;
        return previous;
    }

    @Override public boolean add(E element) { add(element, 1); return true; }

    @Override public int remove(Object element, int occurrences) {
        nonnegative(occurrences);
        Integer row = rows.get(element);
        int previous = row == null ? 0 : counts[row];
        if (previous == 0 || occurrences == 0) return previous;
        int removed = Math.min(previous, occurrences);
        if (removed == previous) {
            rows.remove(element);
            free(row);
        } else {
            counts[row] = previous - removed;
        }
        total -= removed;
        version++;
        return previous;
    }

    @Override public boolean remove(Object element) { return remove(element, 1) > 0; }

    @Override public int setCount(E element, int desired) {
        nonnegative(desired);
        int previous = count(element);
        if (desired > previous) add(element, desired - previous);
        else if (desired < previous) remove(element, previous - desired);
        return previous;
    }

    /** Expected-count update, not a concurrent compare-and-set. */
    @Override public boolean setCount(E element, int expected, int desired) {
        nonnegative(expected);
        nonnegative(desired);
        if (count(element) != expected) return false;
        setCount(element, desired);
        return true;
    }

    @Override public void clear() {
        if (rows.isEmpty()) return;
        rows.clear();
        Arrays.fill(counts, 0, highWater, 0);
        highWater = 0;
        freeHead = -1;
        total = 0;
        version++;
    }

    @Override public boolean addAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) return false;
        if (source == this) {
            for (int i = 0; i < highWater; i++) {
                if (counts[i] > Integer.MAX_VALUE / 2) {
                    throw new IllegalArgumentException("element count exceeds Integer.MAX_VALUE");
                }
            }
            for (int i = 0; i < highWater; i++) if (counts[i] > 0) counts[i] *= 2;
            total *= 2;
            version++;
        } else if (source instanceof Multiset<? extends E> bag) {
            bag.forEachEntry(this::add);
        } else {
            for (E element : source) add(element);
        }
        return true;
    }

    @Override public boolean containsAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        Iterable<?> distinct = source instanceof Multiset<?> bag ? bag.elementSet() : source;
        for (Object value : distinct) if (!contains(value)) return false;
        return true;
    }

    @Override public boolean removeAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        if (source == this) { boolean changed = !isEmpty(); clear(); return changed; }
        return removeDistinct(source, false);
    }

    @Override public boolean retainAll(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        if (source == this) return false;
        return removeDistinct(source, true);
    }

    private boolean removeDistinct(Collection<?> source, boolean retain) {
        boolean changed = false;
        Iterator<E> iterator = elementSet().iterator();
        while (iterator.hasNext()) {
            if (source.contains(iterator.next()) != retain) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override public void forEachEntry(ObjIntConsumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        int expected = version;
        rows.forEach((element, row) -> {
            checkVersion(expected);
            action.accept(element, counts[row]);
            checkVersion(expected);
        });
    }

    private void checkVersion(int expected) {
        if (expected != version) throw new ConcurrentModificationException();
    }

    @Override public Iterator<E> iterator() {
        return new Iterator<>() {
            private final Iterator<E> keys = rows.keySet().iterator();
            private int expected = version;
            private int row;
            private E current;
            private int remaining;
            private boolean removable;
            @Override public boolean hasNext() {
                checkVersion(expected);
                return remaining > 0 || keys.hasNext();
            }
            @Override public E next() {
                checkVersion(expected);
                if (remaining == 0) {
                    if (!keys.hasNext()) throw new NoSuchElementException();
                    current = keys.next();
                    row = rows.get(current);
                    remaining = counts[row];
                }
                remaining--;
                removable = true;
                return current;
            }
            @Override public void remove() {
                checkVersion(expected);
                if (!removable) throw new IllegalStateException();
                if (counts[row] == 1) {
                    keys.remove();
                    free(row);
                } else counts[row]--;
                total--;
                version++;
                expected = version;
                removable = false;
            }
        };
    }

    @Override public Set<E> elementSet() {
        return new AbstractSet<>() {
            @Override public int size() { return rows.size(); }
            @Override public boolean contains(Object key) { return M3Multiset.this.contains(key); }
            @Override public void clear() { M3Multiset.this.clear(); }
            @Override public boolean remove(Object key) {
                return M3Multiset.this.remove(key, Integer.MAX_VALUE) > 0;
            }
            @Override public Iterator<E> iterator() {
                return new Iterator<>() {
                    private final Iterator<E> keys = rows.keySet().iterator();
                    private int expected = version;
                    private int row;
                    private boolean removable;
                    @Override public boolean hasNext() { checkVersion(expected); return keys.hasNext(); }
                    @Override public E next() {
                        checkVersion(expected);
                        E key = keys.next();
                        row = rows.get(key);
                        removable = true;
                        return key;
                    }
                    @Override public void remove() {
                        checkVersion(expected);
                        if (!removable) throw new IllegalStateException();
                        keys.remove();
                        total -= counts[row];
                        free(row);
                        version++;
                        expected = version;
                        removable = false;
                    }
                };
            }
        };
    }

    @Override public Set<Multiset.Entry<E>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return rows.size(); }
            @Override public void clear() { M3Multiset.this.clear(); }
            @Override public boolean contains(Object candidate) {
                if (!(candidate instanceof Multiset.Entry<?> entry) || entry.getCount() <= 0) return false;
                return count(entry.getElement()) == entry.getCount();
            }
            @Override public boolean remove(Object candidate) {
                if (!contains(candidate)) return false;
                Multiset.Entry<?> entry = (Multiset.Entry<?>) candidate;
                M3Multiset.this.remove(entry.getElement(), Integer.MAX_VALUE);
                return true;
            }
            @Override public Iterator<Multiset.Entry<E>> iterator() {
                Iterator<E> keys = elementSet().iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { return keys.hasNext(); }
                    @Override public Multiset.Entry<E> next() {
                        E key = keys.next();
                        return Multisets.immutableEntry(key, count(key));
                    }
                    @Override public void remove() { keys.remove(); }
                };
            }
        };
    }

    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Multiset<?> bag) || size() != bag.size()
                || rows.size() != bag.elementSet().size()) return false;
        for (E key : rows.keySet()) if (count(key) != bag.count(key)) return false;
        return true;
    }

    @Override public int hashCode() {
        int hash = 0;
        for (E key : rows.keySet()) hash += Objects.hashCode(key) ^ count(key);
        return hash;
    }

    @Override public String toString() { return entrySet().toString(); }
}
