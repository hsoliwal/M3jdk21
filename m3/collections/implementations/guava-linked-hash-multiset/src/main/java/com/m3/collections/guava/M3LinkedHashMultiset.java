/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Guava 33.4.8-jre com.google.common.collect.LinkedHashMultiset.
 * Guava supplies only the Multiset API interface (Apache-2.0); no Guava implementation class, helper,
 * interface default or body is called or copied. Dense lanes with backward-shift deletion follow the owned
 * M3HashMultiset (PR #1418); the insertion-order links follow the first-party M3LinkedHashMap pattern.
 */
package com.m3.collections.guava;

import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

/**
 * Owned insertion-ordered multiset over dense parallel lanes; no per-element or per-count objects.
 *
 * <p>Lanes: {@code Object[] elements}, {@code int[] counts}, {@code int[] hashes}, {@code int[] gens},
 * {@code int[] before} and {@code int[] after} are dense (offsets {@code 0..distinct-1}); a power-of-two
 * {@code int[] index} maps a probe position to dense offset+1 (0 empty), probed with the {@code length - 1}
 * mask and cleared by backward shift, so no tombstones accumulate. Removing an element moves the last dense
 * element into its offset and re-points that element's index entry and order links. {@code before}/{@code after}
 * chain the offsets in insertion order from {@code first} to {@code last}; an element removed and re-added
 * joins the end, as in Guava's LinkedHashMap. The total size is a {@code long}, saturated to {@code int} by
 * {@link #size()}. {@code modCount} changes only when an element appears or disappears, matching Guava's
 * backing-map fail-fast rules; {@code gens} (one clock) lets an occurrence iterator recognise a removed and
 * re-added element.
 *
 * <p>{@code elementSet()} and {@code entrySet()} are flyweight views; {@link Multiset.Entry} objects are
 * created only when {@code entrySet()} iteration returns them, and their counts are read live.
 *
 * @param <E> element type
 */
public final class M3LinkedHashMultiset<E> extends AbstractCollection<E> implements Multiset<E>, Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;
    private static final Object[] NO_ELEMENTS = {};
    private static final int[] NO_INTS = {};
    private static final String NO_NEXT = "no calls to next() since the last call to remove()";

    private transient Object[] elements;
    private transient int[] counts;
    private transient int[] hashes;
    private transient int[] gens;
    private transient int[] before;
    private transient int[] after;
    private transient int[] index;
    private transient int first = -1;
    private transient int last = -1;
    private transient int distinct;
    private transient long total;
    private transient int modCount;
    private transient int clock;
    private transient Set<E> elementSetView;
    private transient Set<Multiset.Entry<E>> entrySetView;

    private M3LinkedHashMultiset(int distinctElements) {
        if (distinctElements < 0) {
            throw new IllegalArgumentException("expectedSize cannot be negative but was: " + distinctElements);
        }
        initEmpty();
        if (distinctElements > 0) { reserve(distinctElements); }
    }

    /** Creates an empty multiset. */
    public static <E> M3LinkedHashMultiset<E> create() {
        return new M3LinkedHashMultiset<>(0);
    }

    /** Creates an empty multiset sized for {@code distinctElements} distinct elements. */
    public static <E> M3LinkedHashMultiset<E> create(int distinctElements) {
        return new M3LinkedHashMultiset<>(distinctElements);
    }

    /** Creates a multiset containing every element of {@code elements}, in their order. */
    public static <E> M3LinkedHashMultiset<E> create(Iterable<? extends E> elements) {
        M3LinkedHashMultiset<E> created = create(elements instanceof Multiset<?> m ? m.elementSet().size() : 11);
        if (elements instanceof Collection<? extends E> c) {
            created.addAll(c);
        } else {
            for (E e : elements) { created.add(e); }
        }
        return created;
    }

    private void initEmpty() {
        elements = NO_ELEMENTS;
        counts = NO_INTS;
        hashes = NO_INTS;
        gens = NO_INTS;
        before = NO_INTS;
        after = NO_INTS;
        index = NO_INTS;
        first = -1;
        last = -1;
    }

    // ------------------------------------------------------------------ lanes

    private static int spread(Object element) {
        int h = element == null ? 0 : element.hashCode();
        return h ^ h >>> 16;
    }

    static int tableFor(int distinctElements) {
        long needed = Math.max(4L, distinctElements * 3L / 2 + 1);
        if (needed > 1 << 30) { throw new OutOfMemoryError("multiset index capacity exhausted"); }
        return Integer.highestOneBit((int) needed - 1) << 1;
    }

    static int grownLanes(int old, int needed) {
        if (needed > MAX_ARRAY) { throw new OutOfMemoryError("multiset capacity exhausted"); }
        return (int) Math.min(MAX_ARRAY, Math.max(needed, (long) old + (old >> 1) + 1L));
    }

    private void reserve(int needed) {
        if (needed > elements.length) {
            int n = grownLanes(elements.length, needed);
            elements = Arrays.copyOf(elements, n);
            counts = Arrays.copyOf(counts, n);
            hashes = Arrays.copyOf(hashes, n);
            gens = Arrays.copyOf(gens, n);
            before = Arrays.copyOf(before, n);
            after = Arrays.copyOf(after, n);
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
        int r = locate(element);
        return r >= 0 ? r : -1;
    }

    /**
     * The dense offset of {@code element}, or {@code -(p + 2)} for the empty index position {@code p} that ends its
     * probe, so an insert can reuse it (-1 when there is no index yet).
     */
    private int locate(Object element) {
        int[] table = index;
        if (table.length == 0) { return -1; }
        int h = spread(element);
        int mask = table.length - 1;
        int p = h & mask;
        for (; table[p] != 0; p = p + 1 & mask) {
            int d = table[p] - 1;
            if (hashes[d] == h && Objects.equals(element, elements[d])) { return d; }
        }
        return -(p + 2);
    }

    private int positionOf(int d) {
        int mask = index.length - 1;
        int p = hashes[d] & mask;
        while (index[p] != d + 1) { p = p + 1 & mask; }
        return p;
    }

    /** Inserts a new element; {@code miss} is locate()'s result, whose empty position is reused unless reserve() rehashed. */
    private int insert(Object element, int count, int miss) {
        int[] probed = index;
        reserve(distinct + 1);
        int d = distinct++;
        int h = spread(element);
        elements[d] = element;
        counts[d] = count;
        hashes[d] = h;
        int t = ++clock;
        gens[d] = t != 0 ? t : ++clock;
        before[d] = last;
        after[d] = -1;
        if (last >= 0) { after[last] = d; } else { first = d; }
        last = d;
        int p;
        if (index == probed && miss < -1) {
            p = -(miss + 2);
        } else {
            int mask = index.length - 1;
            p = h & mask;
            while (index[p] != 0) { p = p + 1 & mask; }
        }
        index[p] = d + 1;
        total += count;
        modCount++;
        return d;
    }

    /** Unlinks offset {@code d} from the order chain. */
    private void unlink(int d) {
        int b = before[d];
        int a = after[d];
        if (b >= 0) { after[b] = a; } else { first = a; }
        if (a >= 0) { before[a] = b; } else { last = b; }
    }

    /** Removes dense offset {@code d}: unlink it, backward-shift its index entry, then move the last offset into it. */
    private int removeAt(int d) {
        int removed = counts[d];
        unlink(d);
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
        int moved = --distinct;
        if (d != moved) {
            index[positionOf(moved)] = d + 1;
            elements[d] = elements[moved];
            counts[d] = counts[moved];
            hashes[d] = hashes[moved];
            gens[d] = gens[moved];
            int b = before[moved];
            int a = after[moved];
            before[d] = b;
            after[d] = a;
            if (b >= 0) { after[b] = d; } else { first = d; }
            if (a >= 0) { before[a] = d; } else { last = d; }
        }
        elements[moved] = null;
        counts[moved] = 0;
        total -= removed;
        modCount++;
        return removed;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    // ------------------------------------------------------------------ Multiset API

    @Override public int size() {
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    @Override public boolean isEmpty() {
        return distinct == 0;
    }

    @Override public boolean contains(Object element) {
        return find(element) >= 0;
    }

    @Override public int count(Object element) {
        int d = find(element);
        return d < 0 ? 0 : counts[d];
    }

    @Override public boolean add(E element) {
        add(element, 1);
        return true;
    }

    @Override public int add(E element, int occurrences) {
        if (occurrences == 0) { return count(element); }
        if (occurrences < 0) { throw new IllegalArgumentException("occurrences cannot be negative: " + occurrences); }
        int d = locate(element);
        if (d < 0) {
            insert(element, occurrences, d);
            return 0;
        }
        int old = counts[d];
        long updated = (long) old + occurrences;
        if (updated > Integer.MAX_VALUE) { throw new IllegalArgumentException("too many occurrences: " + updated); }
        counts[d] = (int) updated;
        total += occurrences;
        return old;
    }

    @Override public boolean remove(Object element) {
        return remove(element, 1) > 0;
    }

    @Override public int remove(Object element, int occurrences) {
        if (occurrences == 0) { return count(element); }
        if (occurrences < 0) { throw new IllegalArgumentException("occurrences cannot be negative: " + occurrences); }
        int d = find(element);
        if (d < 0) { return 0; }
        int old = counts[d];
        if (old > occurrences) {
            counts[d] = old - occurrences;
            total -= occurrences;
        } else {
            removeAt(d);
        }
        return old;
    }

    @Override public int setCount(E element, int count) {
        if (count < 0) { throw new IllegalArgumentException("count cannot be negative but was: " + count); }
        int d = locate(element);
        if (d < 0) {
            if (count > 0) { insert(element, count, d); }
            return 0;
        }
        int old = counts[d];
        if (count == 0) {
            removeAt(d);
        } else {
            counts[d] = count;
            total += count - old;
        }
        return old;
    }

    @Override public boolean setCount(E element, int oldCount, int newCount) {
        if (oldCount < 0) { throw new IllegalArgumentException("oldCount cannot be negative but was: " + oldCount); }
        if (newCount < 0) { throw new IllegalArgumentException("newCount cannot be negative but was: " + newCount); }
        if (count(element) != oldCount) { return false; }
        setCount(element, newCount);
        return true;
    }

    @Override public void clear() {
        Arrays.fill(elements, 0, distinct, null);
        Arrays.fill(counts, 0, distinct, 0);
        Arrays.fill(index, 0);
        distinct = 0;
        total = 0;
        first = -1;
        last = -1;
        modCount++;
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        Objects.requireNonNull(c);
        if (c instanceof M3LinkedHashMultiset<? extends E> other) {
            if (other.distinct == 0) { return false; }
            for (int d = other.first; d >= 0; d = other.after[d]) { add(cast(other.elements[d]), other.counts[d]); }
            return true;
        }
        if (c instanceof Multiset<? extends E> other) {
            if (other.isEmpty()) { return false; }
            other.forEachEntry(this::add);
            return true;
        }
        if (c.isEmpty()) { return false; }
        boolean changed = false;
        for (E e : c) { changed |= add(e); }
        return changed;
    }

    @Override public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return elementSet().removeAll(c instanceof Multiset<?> m ? m.elementSet() : c);
    }

    @Override public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return elementSet().retainAll(c instanceof Multiset<?> m ? m.elementSet() : c);
    }

    @Override public Set<E> elementSet() {
        Set<E> v = elementSetView;
        return v != null ? v : (elementSetView = new ElementSetView());
    }

    @Override public Set<Multiset.Entry<E>> entrySet() {
        Set<Multiset.Entry<E>> v = entrySetView;
        return v != null ? v : (entrySetView = new EntrySetView());
    }

    @Override public Iterator<E> iterator() {
        return new Occurrences();
    }

    /** Visits each distinct element in insertion order with its count; fail-fast when an element appears or disappears. */
    @Override public void forEachEntry(ObjIntConsumer<? super E> action) {
        Objects.requireNonNull(action);
        int expected = modCount;
        for (int d = first; d >= 0; d = after[d]) {
            action.accept(cast(elements[d]), counts[d]);
            if (modCount != expected) { throw new ConcurrentModificationException(); }
        }
    }

    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        int expected = modCount;
        for (int d = first; d >= 0; d = after[d]) {
            if (modCount != expected) { throw new ConcurrentModificationException(); }
            E e = cast(elements[d]);
            for (int i = counts[d]; i > 0; i--) { action.accept(e); }
        }
        if (modCount != expected) { throw new ConcurrentModificationException(); }
    }

    @Override public Spliterator<E> spliterator() {
        return new OccurrenceSpliterator();
    }

    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof Multiset<?> that) || that.size() != size() || that.entrySet().size() != distinct) { return false; }
        for (Multiset.Entry<?> e : that.entrySet()) {
            if (count(e.getElement()) != e.getCount()) { return false; }
        }
        return true;
    }

    @Override public int hashCode() {
        int h = 0;
        for (int d = 0; d < distinct; d++) { h += Objects.hashCode(elements[d]) ^ counts[d]; }
        return h;
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder("[");
        for (int d = first; d >= 0; d = after[d]) {
            if (d != first) { b.append(", "); }
            b.append(elements[d]);
            if (counts[d] != 1) { b.append(" x ").append(counts[d]); }
        }
        return b.append(']').toString();
    }

    /** Checks dense lanes against the index and the order chain, totals, and that probing finds every element. */
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
        if (sum != total || occupied != distinct) { throw new AssertionError("totals"); }
        for (int d = distinct; d < elements.length; d++) {
            if (elements[d] != null || counts[d] != 0) { throw new AssertionError("tail"); }
        }
        int walked = 0;
        int previous = -1;
        for (int d = first; d >= 0; d = after[d]) {
            if (d >= distinct || before[d] != previous) { throw new AssertionError("links"); }
            walked++;
            previous = d;
        }
        if (walked != distinct || previous != last) { throw new AssertionError("links"); }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(distinct);
        for (int d = first; d >= 0; d = after[d]) {
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
    }

    // ------------------------------------------------------------------ cursors and views

    /** Order-chain cursor; a removal moves the last dense offset into the removed one, so the chain pointer follows it. */
    private abstract class LinkCursor<T> implements Iterator<T> {
        int next = first;
        int lastRet = -1;
        int expected = modCount;

        abstract T at(int d);

        @Override public boolean hasNext() {
            return next >= 0;
        }

        @Override public T next() {
            if (modCount != expected) { throw new ConcurrentModificationException(); }
            if (next < 0) { throw new NoSuchElementException(); }
            lastRet = next;
            next = after[lastRet];
            return at(lastRet);
        }

        @Override public void remove() {
            if (lastRet < 0) { throw new IllegalStateException(NO_NEXT); }
            if (modCount != expected) { throw new ConcurrentModificationException(); }
            if (next == distinct - 1) { next = lastRet; }
            removeAt(lastRet);
            lastRet = -1;
            expected = modCount;
        }
    }

    /** Occurrence iterator: each element repeated count times; removal decrements, the last one removes. */
    private final class Occurrences implements Iterator<E> {
        private final LinkCursor<Object> entries = new LinkCursor<>() {
            @Override Object at(int d) {
                return elements[d];
            }
        };
        private Object current;
        private int currentGen;
        private int occurrencesLeft;
        private boolean canRemove;

        @Override public boolean hasNext() {
            return occurrencesLeft > 0 || entries.hasNext();
        }

        @Override public E next() {
            if (occurrencesLeft == 0) {
                current = entries.next();
                currentGen = gens[entries.lastRet];
                occurrencesLeft = counts[entries.lastRet];
            }
            occurrencesLeft--;
            canRemove = true;
            return cast(current);
        }

        @Override public void remove() {
            if (!canRemove) { throw new IllegalStateException(NO_NEXT); }
            int d = find(current);
            if (d < 0 || gens[d] != currentGen) { throw new ConcurrentModificationException(); }
            if (counts[d] == 1) {
                entries.lastRet = d;
                entries.remove();
            } else {
                counts[d]--;
                total--;
            }
            canRemove = false;
        }
    }

    /** Sized occurrence spliterator (Guava reports SIZED only); it does not split. */
    private final class OccurrenceSpliterator implements Spliterator<E> {
        private Iterator<E> source;
        private long remaining = size();

        @Override public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            if (source == null) { source = iterator(); }
            if (!source.hasNext()) { return false; }
            action.accept(source.next());
            remaining--;
            return true;
        }

        @Override public Spliterator<E> trySplit() {
            return null;
        }

        @Override public long estimateSize() {
            return Math.max(remaining, 0);
        }

        @Override public int characteristics() {
            return Spliterator.SIZED;
        }
    }

    private final class ElementSetView extends AbstractSet<E> {
        @Override public int size() {
            return distinct;
        }

        @Override public boolean isEmpty() {
            return distinct == 0;
        }

        @Override public boolean contains(Object o) {
            return find(o) >= 0;
        }

        @Override public boolean remove(Object o) {
            return M3LinkedHashMultiset.this.remove(o, Integer.MAX_VALUE) > 0;
        }

        @Override public void clear() {
            M3LinkedHashMultiset.this.clear();
        }

        @Override public Iterator<E> iterator() {
            return new LinkCursor<>() {
                @Override E at(int d) {
                    return cast(elements[d]);
                }
            };
        }

        /** Guava's removeAll order: iterate this set when the argument is a larger set, else the argument. */
        @Override public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            Collection<?> other = c instanceof Multiset<?> m ? m.elementSet() : c;
            boolean changed = false;
            if (other instanceof Set<?> && other.size() > size()) {
                for (Iterator<E> it = iterator(); it.hasNext();) {
                    if (other.contains(it.next())) {
                        it.remove();
                        changed = true;
                    }
                }
                return changed;
            }
            for (Object o : other) { changed |= remove(o); }
            return changed;
        }

        @Override public Spliterator<E> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT);
        }
    }

    private final class EntrySetView extends AbstractSet<Multiset.Entry<E>> {
        @Override public int size() {
            return distinct;
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof Multiset.Entry<?> e) || e.getCount() <= 0) { return false; }
            return count(e.getElement()) == e.getCount();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof Multiset.Entry<?> e) || e.getCount() == 0) { return false; }
            return setCount(cast(e.getElement()), e.getCount(), 0);
        }

        @Override public void clear() {
            M3LinkedHashMultiset.this.clear();
        }

        @Override public Iterator<Multiset.Entry<E>> iterator() {
            return new LinkCursor<>() {
                @Override Multiset.Entry<E> at(int d) {
                    return new CountView(cast(elements[d]));
                }
            };
        }

        @Override public Spliterator<Multiset.Entry<E>> spliterator() {
            return Spliterators.spliterator(this, Spliterator.DISTINCT);
        }
    }

    /** Boundary flyweight for one entry; its count is read live from the lanes. */
    private final class CountView implements Multiset.Entry<E> {
        private final E element;

        CountView(E element) {
            this.element = element;
        }

        @Override public E getElement() {
            return element;
        }

        @Override public int getCount() {
            return count(element);
        }

        @Override public boolean equals(Object other) {
            return other instanceof Multiset.Entry<?> e && getCount() == e.getCount() && Objects.equals(element, e.getElement());
        }

        @Override public int hashCode() {
            return Objects.hashCode(element) ^ getCount();
        }

        @Override public String toString() {
            int n = getCount();
            return n == 1 ? String.valueOf(element) : element + " x " + n;
        }
    }
}
