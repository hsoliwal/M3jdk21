/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Owned M3 body for the public contract of Guava 33.4.8-jre com.google.common.collect.EnumMultiset.
 * Guava supplies only the Multiset API interface (Apache-2.0); no Guava implementation class, helper,
 * interface default or body is called or copied. Ordinal lanes with a presence mask follow the
 * first-party M3EnumLongMap pattern (m3/collections core, Apache-2.0).
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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

/**
 * Owned enum multiset over ordinal lanes: an {@code int[] counts} lane indexed by ordinal and a
 * {@code long[] present} mask with one bit per constant whose count is positive. The enum universe is
 * shared per enum class (never cloned per instance), so an instance retains only its two lanes.
 *
 * <p>Iteration is ordinal order and, as in Guava, never fail-fast: cursors read the lanes live.
 * {@code elementSet()} and {@code entrySet()} are flyweight views; a {@link Multiset.Entry} is created only
 * when entry iteration returns one, and its count is read live by ordinal. The total size is a {@code long},
 * saturated to {@code int} by {@link #size()}.
 *
 * @param <E> enum element type
 */
public final class M3EnumMultiset<E extends Enum<E>> extends M3EnumMultisetBase<E> implements Serializable {
    @Serial private static final long serialVersionUID = 0L;
    private static final String NO_NEXT = "no calls to next() since the last call to remove()";
    /** One shared universe per enum class; getEnumConstants() clones on every call. */
    private static final ClassValue<Object[]> UNIVERSES = new ClassValue<>() {
        @Override protected Object[] computeValue(Class<?> type) {
            return type.getEnumConstants();
        }
    };

    private transient Class<E> type;
    private transient Object[] universe;
    private transient int[] counts;
    private transient long[] present;
    private transient int distinct;
    private transient long total;
    private transient Set<E> elementSetView;
    private transient Set<Multiset.Entry<E>> entrySetView;

    private M3EnumMultiset(Class<E> type) {
        this.type = type;
        if (!type.isEnum()) { throw new IllegalArgumentException(); }
        initLanes();
    }

    /** Creates an empty multiset for the constants of {@code type}. */
    public static <E extends Enum<E>> M3EnumMultiset<E> create(Class<E> type) {
        return new M3EnumMultiset<>(type);
    }

    /** Creates a multiset of {@code elements}; the type is the first element's declaring class. */
    public static <E extends Enum<E>> M3EnumMultiset<E> create(Iterable<E> elements) {
        Iterator<E> iterator = elements.iterator();
        if (!iterator.hasNext()) { throw new IllegalArgumentException("EnumMultiset constructor passed empty Iterable"); }
        M3EnumMultiset<E> multiset = new M3EnumMultiset<>(iterator.next().getDeclaringClass());
        multiset.addAllFrom(elements);
        return multiset;
    }

    /** Creates a multiset for {@code type} containing {@code elements}. */
    public static <E extends Enum<E>> M3EnumMultiset<E> create(Iterable<E> elements, Class<E> type) {
        M3EnumMultiset<E> result = create(type);
        result.addAllFrom(elements);
        return result;
    }

    private void addAllFrom(Iterable<E> elements) {
        if (elements instanceof Collection<E> c) {
            addAll(c);
        } else {
            for (Iterator<E> it = Objects.requireNonNull(elements).iterator(); it.hasNext();) { add(it.next()); }
        }
    }

    private void initLanes() {
        universe = UNIVERSES.get(type);
        counts = new int[universe.length];
        present = new long[(universe.length + 63) >>> 6];
    }

    // ------------------------------------------------------------------ lanes

    private boolean isActuallyE(Object o) {
        if (o instanceof Enum<?> e) {
            int ordinal = e.ordinal();
            return ordinal < universe.length && universe[ordinal] == e;
        }
        return false;
    }

    private void checkIsE(Object element) {
        Objects.requireNonNull(element);
        if (!isActuallyE(element)) { throw new ClassCastException("Expected an " + type + " but got " + element); }
    }

    static void checkNonnegative(int value, String name) {
        if (value < 0) { throw new IllegalArgumentException(name + " cannot be negative but was: " + value); }
    }

    /** First ordinal at or after {@code from} whose count is positive, or {@code universe.length}. */
    int nextPresent(int from) {
        int word = from >>> 6;
        if (word >= present.length) { return universe.length; }
        long bits = present[word] & -1L << from;
        while (bits == 0) {
            if (++word == present.length) { return universe.length; }
            bits = present[word];
        }
        return (word << 6) + Long.numberOfTrailingZeros(bits);
    }

    /** Writes a count; keeps the presence bit, distinct count and total in step. */
    private void put(int ordinal, int count) {
        int old = counts[ordinal];
        counts[ordinal] = count;
        total += (long) count - old;
        if (old == 0 && count > 0) {
            present[ordinal >>> 6] |= 1L << ordinal;
            distinct++;
        } else if (old > 0 && count == 0) {
            present[ordinal >>> 6] &= ~(1L << ordinal);
            distinct--;
        }
    }

    @SuppressWarnings("unchecked")
    private E constant(int ordinal) {
        return (E) universe[ordinal];
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
        return count(element) > 0;
    }

    @Override public int count(Object element) {
        return element == null || !isActuallyE(element) ? 0 : counts[((Enum<?>) element).ordinal()];
    }

    @Override public boolean add(E element) {
        add(element, 1);
        return true;
    }

    @Override public int add(E element, int occurrences) {
        checkIsE(element);
        checkNonnegative(occurrences, "occurrences");
        if (occurrences == 0) { return count(element); }
        int ordinal = element.ordinal();
        int old = counts[ordinal];
        long updated = (long) old + occurrences;
        if (updated > Integer.MAX_VALUE) { throw new IllegalArgumentException("too many occurrences: " + updated); }
        put(ordinal, (int) updated);
        return old;
    }

    @Override public boolean remove(Object element) {
        return remove(element, 1) > 0;
    }

    @Override public int remove(Object element, int occurrences) {
        if (element == null || !isActuallyE(element)) { return 0; }
        checkNonnegative(occurrences, "occurrences");
        if (occurrences == 0) { return count(element); }
        int ordinal = ((Enum<?>) element).ordinal();
        int old = counts[ordinal];
        if (old != 0) { put(ordinal, old <= occurrences ? 0 : old - occurrences); }
        return old;
    }

    @Override public int setCount(E element, int count) {
        checkIsE(element);
        checkNonnegative(count, "count");
        int ordinal = element.ordinal();
        int old = counts[ordinal];
        put(ordinal, count);
        return old;
    }

    @Override public void clear() {
        Arrays.fill(counts, 0);
        Arrays.fill(present, 0L);
        total = 0;
        distinct = 0;
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        Objects.requireNonNull(c);
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

    /** Visits each present constant in ordinal order with its live count. */
    @Override public void forEachEntry(ObjIntConsumer<? super E> action) {
        Objects.requireNonNull(action);
        for (int i = nextPresent(0); i < universe.length; i = nextPresent(i + 1)) { action.accept(constant(i), counts[i]); }
    }

    /** Each present constant repeated by the count read when it is reached. */
    @Override public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (int i = nextPresent(0); i < universe.length; i = nextPresent(i + 1)) {
            E e = constant(i);
            for (int n = counts[i]; n > 0; n--) { action.accept(e); }
        }
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
        for (int i = nextPresent(0); i < universe.length; i = nextPresent(i + 1)) { h += universe[i].hashCode() ^ counts[i]; }
        return h;
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder("[");
        String separator = "";
        for (int i = nextPresent(0); i < universe.length; i = nextPresent(i + 1)) {
            b.append(separator).append(universe[i]);
            if (counts[i] != 1) { b.append(" x ").append(counts[i]); }
            separator = ", ";
        }
        return b.append(']').toString();
    }

    /** Checks the shared universe, the presence mask, the distinct count and the total against the count lane. */
    void verifyInvariants() {
        if (universe != UNIVERSES.get(type) || counts.length != universe.length || present.length != (universe.length + 63) >>> 6) {
            throw new AssertionError("lanes");
        }
        long sum = 0;
        int seen = 0;
        for (int i = 0; i < counts.length; i++) {
            boolean bit = (present[i >>> 6] & 1L << i) != 0;
            if (counts[i] < 0 || bit != counts[i] > 0) { throw new AssertionError("mask"); }
            sum += counts[i];
            seen += bit ? 1 : 0;
        }
        if ((universe.length & 63) != 0 && (present[present.length - 1] & -1L << universe.length) != 0) {
            throw new AssertionError("mask tail");
        }
        if (sum != total || seen != distinct) { throw new AssertionError("totals"); }
    }

    // ------------------------------------------------------------------ serialization (owned format)

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(type);
        out.writeInt(distinct);
        for (int i = nextPresent(0); i < universe.length; i = nextPresent(i + 1)) {
            out.writeObject(universe[i]);
            out.writeInt(counts[i]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Object read = in.readObject();
        if (!(read instanceof Class<?> c) || !c.isEnum()) { throw new InvalidObjectException("Invalid enum type " + read); }
        type = cast(c);
        initLanes();
        int n = in.readInt();
        if (n < 0 || n > universe.length) { throw new InvalidObjectException("Invalid element count " + n); }
        for (int i = 0; i < n; i++) {
            Object element = in.readObject();
            int count = in.readInt();
            if (!isActuallyE(element) || count <= 0 || counts[((Enum<?>) element).ordinal()] != 0) {
                throw new InvalidObjectException("Invalid entry " + element + " x " + count);
            }
            put(((Enum<?>) element).ordinal(), count);
        }
    }

    // ------------------------------------------------------------------ cursors and views

    /** Guava's ordinal cursor: hasNext advances to the next live count; removal zeroes the last ordinal. */
    private abstract class Cursor<T> implements Iterator<T> {
        int index;
        int toRemove = -1;

        abstract T output(int ordinal);

        @Override public boolean hasNext() {
            index = nextPresent(index);
            return index < universe.length;
        }

        @Override public T next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            T result = output(index);
            toRemove = index++;
            return result;
        }

        @Override public void remove() {
            if (toRemove < 0) { throw new IllegalStateException(NO_NEXT); }
            put(toRemove, 0);
            toRemove = -1;
        }
    }

    /** Ordinal-only cursor for the occurrence iterator; it returns no object. */
    private final class Ordinals extends Cursor<Object> {
        @Override Object output(int ordinal) {
            return null;
        }
    }

    /** Occurrence iterator over the ordinal cursor; the last occurrence removes through the cursor. */
    private final class Occurrences implements Iterator<E> {
        private final Ordinals ordinals = new Ordinals();
        private int current;
        private int laterCount;
        private int totalCount;
        private boolean canRemove;

        @Override public boolean hasNext() {
            return laterCount > 0 || ordinals.hasNext();
        }

        @Override public E next() {
            if (!hasNext()) { throw new NoSuchElementException(); }
            if (laterCount == 0) {
                ordinals.next();
                current = ordinals.toRemove;
                totalCount = laterCount = counts[current];
            }
            laterCount--;
            canRemove = true;
            return constant(current);
        }

        @Override public void remove() {
            if (!canRemove) { throw new IllegalStateException(NO_NEXT); }
            if (totalCount == 1) {
                ordinals.remove();
            } else {
                M3EnumMultiset.this.remove(constant(current), 1);
            }
            totalCount--;
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

    /** Guava's ImprovedAbstractSet bulk rules shared by both views. */
    private abstract class View<T> extends AbstractSet<T> {
        @Override public final int size() {
            return distinct;
        }

        @Override public final void clear() {
            M3EnumMultiset.this.clear();
        }

        @Override public final boolean removeAll(Collection<?> c) {
            Collection<?> other = Objects.requireNonNull(c) instanceof Multiset<?> m ? m.elementSet() : c;
            boolean changed = false;
            if (other instanceof Set<?> && other.size() > size()) {
                for (Iterator<T> it = iterator(); it.hasNext();) {
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

        @Override public final boolean retainAll(Collection<?> c) {
            return super.retainAll(Objects.requireNonNull(c));
        }
    }

    private final class ElementSetView extends View<E> {
        @Override public boolean isEmpty() {
            return M3EnumMultiset.this.isEmpty();
        }

        @Override public boolean contains(Object o) {
            return M3EnumMultiset.this.contains(o);
        }

        @Override public boolean containsAll(Collection<?> c) {
            return M3EnumMultiset.this.containsAll(c);
        }

        @Override public boolean remove(Object o) {
            return M3EnumMultiset.this.remove(o, Integer.MAX_VALUE) > 0;
        }

        @Override public Iterator<E> iterator() {
            return new Cursor<>() {
                @Override E output(int ordinal) {
                    return constant(ordinal);
                }
            };
        }
    }

    private final class EntrySetView extends View<Multiset.Entry<E>> {
        @Override public boolean contains(Object o) {
            if (!(o instanceof Multiset.Entry<?> e) || e.getCount() <= 0) { return false; }
            return count(e.getElement()) == e.getCount();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof Multiset.Entry<?> e)) { return false; }
            int n = e.getCount();
            return n != 0 && casCount(e.getElement(), n, 0);
        }

        @Override public Iterator<Multiset.Entry<E>> iterator() {
            return new Cursor<>() {
                @Override Multiset.Entry<E> output(int ordinal) {
                    return new CountView(ordinal);
                }
            };
        }
    }

    /** Boundary flyweight for one ordinal; its count is read live. */
    private final class CountView implements Multiset.Entry<E> {
        private final int ordinal;

        CountView(int ordinal) {
            this.ordinal = ordinal;
        }

        @Override public E getElement() {
            return constant(ordinal);
        }

        @Override public int getCount() {
            return counts[ordinal];
        }

        @Override public boolean equals(Object other) {
            return other instanceof Multiset.Entry<?> e && getCount() == e.getCount() && Objects.equals(getElement(), e.getElement());
        }

        @Override public int hashCode() {
            return getElement().hashCode() ^ getCount();
        }

        @Override public String toString() {
            int n = getCount();
            return n == 1 ? String.valueOf(getElement()) : getElement() + " x " + n;
        }
    }
}

/**
 * Holds the compare-and-set count with an unbounded element type, as Guava's AbstractMultiset does: its erasure
 * takes {@code Object}, so a raw call with a foreign element compares counts (and returns false) instead of
 * failing the {@code Enum} cast of a bridge method.
 */
abstract class M3EnumMultisetBase<E> extends AbstractCollection<E> implements Multiset<E> {
    @Override public final boolean setCount(E element, int oldCount, int newCount) {
        return casCount(element, oldCount, newCount);
    }

    /** Guava's setCountImpl: argument checks, then the transition only when the current count matches. */
    @SuppressWarnings("unchecked")
    final boolean casCount(Object element, int oldCount, int newCount) {
        M3EnumMultiset.checkNonnegative(oldCount, "oldCount");
        M3EnumMultiset.checkNonnegative(newCount, "newCount");
        if (count(element) != oldCount) { return false; }
        setCount((E) element, newCount);
        return true;
    }
}
