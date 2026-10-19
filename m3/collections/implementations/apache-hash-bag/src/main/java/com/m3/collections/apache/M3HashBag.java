/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Apache Commons Collections 4.5.0
 * org.apache.commons.collections4.bag.HashBag. Commons supplies only the Bag API interface (Apache-2.0);
 * no Commons implementation class, helper or body is called or copied. The dense lanes and
 * backward-shift index are adapted from the first-party M3HashMultiset (#1418).
 */
package com.m3.collections.apache;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.collections4.Bag;

/**
 * Owned hash bag over dense parallel lanes; no per-element or per-count objects.
 *
 * <p>Lanes: {@code Object[] elements}, {@code int[] counts} and {@code int[] hashes} are dense (offsets
 * {@code 0..distinct-1}); a power-of-two {@code int[] index} maps a probe position to dense offset+1
 * (0 empty), probed with the {@code length - 1} mask and cleared by backward shift. Removing an element
 * moves the last dense element into its offset. Two counters mirror the provider's fail-fast rules:
 * {@code modCount} changes on every bag mutation except iterator removal (as Commons does), and
 * {@code structMod} changes when an element appears or disappears (as Commons' backing HashMap does).
 *
 * <p>{@code uniqueSet()} is an unmodifiable flyweight view. Iteration order is dense order; Commons'
 * HashMap order is unspecified. Commons semantics are kept where they differ from {@link Collection}:
 * {@code add} returns true only for a new element, {@code removeAll} removes one copy per argument
 * occurrence, {@code containsAll}/{@code retainAll} compare counts, and {@code toArray(T[])} null-fills
 * the whole tail.
 *
 * @param <E> element type
 */
public final class M3HashBag<E> implements Bag<E>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;
    private static final Object[] NO_ELEMENTS = {};
    private static final int[] NO_INTS = {};

    private transient Object[] elements;
    private transient int[] counts;
    private transient int[] hashes;
    private transient int[] index;
    private transient int distinct;
    private transient int size;
    private transient int modCount;
    private transient int structMod;
    private transient Set<E> uniqueSetView;

    /** Creates an empty bag. */
    public M3HashBag() {
        initEmpty();
    }

    /** Creates a bag containing the elements of {@code iterable}. */
    public M3HashBag(Iterable<? extends E> iterable) {
        this();
        iterable.forEach(this::add);
    }

    /** Creates a bag containing the elements of {@code collection}. */
    public M3HashBag(Collection<? extends E> collection) {
        this();
        addAll(collection);
    }

    private void initEmpty() {
        elements = NO_ELEMENTS;
        counts = NO_INTS;
        hashes = NO_INTS;
        index = NO_INTS;
    }

    // ------------------------------------------------------------------ lanes

    private static int spread(Object element) {
        int h = element == null ? 0 : element.hashCode();
        return h ^ h >>> 16;
    }

    static int tableFor(int distinctElements) {
        long needed = Math.max(4L, distinctElements * 3L / 2 + 1);
        if (needed > 1 << 30) { throw new OutOfMemoryError("bag index capacity exhausted"); }
        return Integer.highestOneBit((int) needed - 1) << 1;
    }

    static int grownLanes(int old, int needed) {
        if (needed > MAX_ARRAY) { throw new OutOfMemoryError("bag capacity exhausted"); }
        return (int) Math.min(MAX_ARRAY, Math.max(needed, (long) old + (old >> 1) + 1L));
    }

    private void reserve(int needed) {
        if (needed > elements.length) {
            int n = grownLanes(elements.length, needed);
            elements = Arrays.copyOf(elements, n);
            counts = Arrays.copyOf(counts, n);
            hashes = Arrays.copyOf(hashes, n);
        }
        if ((long) needed * 3 > (long) index.length * 2) { rehash(tableFor(needed)); }
    }

    private void rehash(int capacity) {
        int[] table = new int[capacity];
        int mask = capacity - 1;
        for (int d = 0; d < distinct; d++) {
            int p = hashes[d] & mask;
            while (table[p] != 0) { p = p + 1 & mask; }
            table[p] = d + 1;
        }
        index = table;
    }

    int find(Object element) {
        int[] table = index;
        if (table.length == 0) { return -1; }
        int h = spread(element);
        int mask = table.length - 1;
        for (int p = h & mask; table[p] != 0; p = p + 1 & mask) {
            int d = table[p] - 1;
            if (hashes[d] == h && Objects.equals(element, elements[d])) { return d; }
        }
        return -1;
    }

    private int positionOf(int d) {
        int mask = index.length - 1;
        int p = hashes[d] & mask;
        while (index[p] != d + 1) { p = p + 1 & mask; }
        return p;
    }

    private void insert(Object element, int count) {
        reserve(distinct + 1);
        int d = distinct++;
        int h = spread(element);
        elements[d] = element;
        counts[d] = count;
        hashes[d] = h;
        int mask = index.length - 1;
        int p = h & mask;
        while (index[p] != 0) { p = p + 1 & mask; }
        index[p] = d + 1;
        size += count;
        structMod++;
    }

    /** Removes dense offset {@code d}: backward-shift its index entry, then move the last offset into it. */
    private void removeAt(int d) {
        int mask = index.length - 1;
        int gap = positionOf(d);
        for (int q = gap + 1 & mask; index[q] != 0; q = q + 1 & mask) {
            int home = hashes[index[q] - 1] & mask;
            if ((q - home & mask) >= (q - gap & mask)) {
                index[gap] = index[q];
                gap = q;
            }
        }
        index[gap] = 0;
        size -= counts[d];
        int last = --distinct;
        if (d != last) {
            index[positionOf(last)] = d + 1;
            elements[d] = elements[last];
            counts[d] = counts[last];
            hashes[d] = hashes[last];
        }
        elements[last] = null;
        counts[last] = 0;
        structMod++;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // ------------------------------------------------------------------ Bag API

    @Override public int size() {
        return size;
    }

    @Override public boolean isEmpty() {
        return distinct == 0;
    }

    @Override public boolean contains(Object object) {
        return find(object) >= 0;
    }

    @Override public int getCount(Object object) {
        int d = find(object);
        return d < 0 ? 0 : counts[d];
    }

    @Override public boolean add(E object) {
        return add(object, 1);
    }

    /** Adds copies; true only when the element was absent, as in Commons. Every call counts as a modification. */
    @Override public boolean add(E object, int nCopies) {
        modCount++;
        if (nCopies <= 0) { return false; }
        int d = find(object);
        if (d < 0) {
            insert(object, nCopies);
            return true;
        }
        counts[d] += nCopies;
        size += nCopies;
        return false;
    }

    @Override public boolean addAll(Collection<? extends E> coll) {
        boolean changed = false;
        for (E e : coll) { changed |= add(e); }
        return changed;
    }

    @Override public void clear() {
        modCount++;
        Arrays.fill(elements, 0, distinct, null);
        Arrays.fill(counts, 0, distinct, 0);
        Arrays.fill(index, 0);
        distinct = 0;
        size = 0;
        structMod++;
    }

    /** Removes every copy. */
    @Override public boolean remove(Object object) {
        int d = find(object);
        if (d < 0) { return false; }
        modCount++;
        removeAt(d);
        return true;
    }

    @Override public boolean remove(Object object, int nCopies) {
        int d = find(object);
        if (d < 0 || nCopies <= 0) { return false; }
        modCount++;
        if (nCopies < counts[d]) {
            counts[d] -= nCopies;
            size -= nCopies;
        } else {
            removeAt(d);
        }
        return true;
    }

    /** Removes one copy for each occurrence in {@code coll}; a null argument changes nothing. */
    @Override public boolean removeAll(Collection<?> coll) {
        boolean result = false;
        if (coll != null) {
            for (Object o : coll) { result |= remove(o, 1); }
        }
        return result;
    }

    /** True when this bag has at least as many copies of every element of {@code coll} (counted as a bag). */
    @Override public boolean containsAll(Collection<?> coll) {
        if (coll instanceof Bag<?> other) { return containsAllCounts(other); }
        return containsAllCounts(new M3HashBag<Object>(coll));
    }

    private boolean containsAllCounts(Bag<?> other) {
        for (Object o : other.uniqueSet()) {
            if (getCount(o) < other.getCount(o)) { return false; }
        }
        return true;
    }

    /**
     * Commons' retain rule: an element keeps {@code otherCount} copies when {@code 1 <= otherCount <= count};
     * otherwise every copy is removed. The excess is removed one copy at a time, as Commons does.
     */
    @Override public boolean retainAll(Collection<?> coll) {
        Bag<?> other = coll instanceof Bag<?> b ? b : new M3HashBag<Object>(coll);
        M3HashBag<Object> excess = new M3HashBag<>();
        for (int d = 0; d < distinct; d++) {
            int mine = counts[d];
            int theirs = other.getCount(elements[d]);
            excess.add(elements[d], 1 <= theirs && theirs <= mine ? mine - theirs : mine);
        }
        if (excess.isEmpty()) { return false; }
        removeAll(excess);
        return true;
    }

    @Override public Iterator<E> iterator() {
        return new Copies();
    }

    @Override public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (int d = 0; d < distinct; d++) {
            for (int c = counts[d]; c > 0; c--) { result[i++] = elements[d]; }
        }
        return result;
    }

    /** Commons' variant: the whole tail after the copied elements is set to null. */
    @Override public <T> T[] toArray(T[] array) {
        T[] out = array;
        if (out.length < size) { out = cast(Array.newInstance(array.getClass().getComponentType(), size)); }
        int i = 0;
        for (int d = 0; d < distinct; d++) {
            for (int c = counts[d]; c > 0; c--) { out[i++] = cast(elements[d]); }
        }
        Arrays.fill(out, i, out.length, null);
        return out;
    }

    @Override public Set<E> uniqueSet() {
        Set<E> v = uniqueSetView;
        return v != null ? v : (uniqueSetView = new UniqueSet());
    }

    @Override public boolean equals(Object object) {
        if (object == this) { return true; }
        if (!(object instanceof Bag<?> other) || other.size() != size) { return false; }
        for (int d = 0; d < distinct; d++) {
            if (other.getCount(elements[d]) != counts[d]) { return false; }
        }
        return true;
    }

    @Override public int hashCode() {
        int h = 0;
        for (int d = 0; d < distinct; d++) { h += Objects.hashCode(elements[d]) ^ counts[d]; }
        return h;
    }

    /** Commons' format: {@code [2:a,1:b]}. */
    @Override public String toString() {
        if (distinct == 0) { return "[]"; }
        StringBuilder b = new StringBuilder("[");
        for (int d = 0; d < distinct; d++) {
            if (d > 0) { b.append(','); }
            b.append(counts[d]).append(':').append(elements[d]);
        }
        return b.append(']').toString();
    }

    /** Checks dense lanes against the index and the size. */
    void verifyInvariants() {
        long sum = 0;
        int occupied = 0;
        for (int d = 0; d < distinct; d++) {
            if (counts[d] <= 0 || hashes[d] != spread(elements[d]) || find(elements[d]) != d) { throw new AssertionError("dense lanes"); }
            sum += counts[d];
        }
        for (int p = 0; p < index.length; p++) {
            if (index[p] != 0) {
                occupied++;
                if (index[p] > distinct) { throw new AssertionError("index range"); }
            }
        }
        if (sum != size || occupied != distinct) { throw new AssertionError("totals"); }
        for (int d = distinct; d < elements.length; d++) {
            if (elements[d] != null || counts[d] != 0) { throw new AssertionError("tail"); }
        }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(distinct);
        for (int d = 0; d < distinct; d++) {
            out.writeObject(elements[d]);
            out.writeInt(counts[d]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initEmpty();
        int n = in.readInt();
        if (n < 0) { throw new InvalidObjectException("Invalid element count " + n); }
        for (int i = 0; i < n; i++) {
            Object element = in.readObject();
            int count = in.readInt();
            if (count <= 0) { throw new InvalidObjectException("Invalid count " + count); }
            add(cast(element), count);
        }
        modCount = 0;
    }

    // ------------------------------------------------------------------ cursors and views

    /** Copy iterator: Commons' modCount rule on every call, its backing map's rule when advancing elements. */
    private final class Copies implements Iterator<E> {
        private final int mods = modCount;
        private int expectedStruct = structMod;
        private int cursor;
        private int limit = distinct;
        private int currentOffset = -1;
        private Object current;
        private int itemCount;
        private boolean canRemove;

        @Override public boolean hasNext() {
            return itemCount > 0 || cursor < limit;
        }

        @Override public E next() {
            if (modCount != mods) { throw new ConcurrentModificationException(); }
            if (itemCount == 0) {
                if (structMod != expectedStruct) { throw new ConcurrentModificationException(); }
                if (cursor >= distinct) { throw new NoSuchElementException(); }
                currentOffset = cursor++;
                limit = distinct;
                current = elements[currentOffset];
                itemCount = counts[currentOffset];
            }
            canRemove = true;
            itemCount--;
            return cast(current);
        }

        @Override public void remove() {
            if (modCount != mods) { throw new ConcurrentModificationException(); }
            if (!canRemove) { throw new IllegalStateException(); }
            if (structMod != expectedStruct) {
                currentOffset = find(current);
                if (currentOffset < 0) { throw new ConcurrentModificationException(); }
            }
            if (counts[currentOffset] > 1) {
                counts[currentOffset]--;
                size--;
            } else {
                if (structMod != expectedStruct) { throw new ConcurrentModificationException(); }
                removeAt(currentOffset);
                cursor = currentOffset;
                limit = distinct;
                expectedStruct = structMod;
            }
            canRemove = false;
        }
    }

    /** Unmodifiable distinct-element view; every mutator throws, as Commons' UnmodifiableSet does. */
    private final class UniqueSet extends AbstractSet<E> {
        @Override public int size() {
            return distinct;
        }

        @Override public boolean contains(Object o) {
            return find(o) >= 0;
        }

        @Override public Iterator<E> iterator() {
            return new Iterator<>() {
                private int cursor;
                private final int limit = distinct;
                private final int expected = structMod;

                @Override public boolean hasNext() {
                    return cursor < limit;
                }

                @Override public E next() {
                    if (structMod != expected) { throw new ConcurrentModificationException(); }
                    if (cursor >= distinct) { throw new NoSuchElementException(); }
                    return cast(elements[cursor++]);
                }

                @Override public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        @Override public void clear() {
            throw new UnsupportedOperationException();
        }
    }
}
