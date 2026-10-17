/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.ObjIntConsumer;
import org.apache.commons.collections4.MultiSet;

/**
 * Apache MultiSet contract over the incumbent M3 primitive-count owner.
 * This is not the legacy Apache Bag contract, a concurrent collection, or a
 * serialized-form clone of a provider concrete class. Null elements are supported.
 * Counts are checked ints; size saturates while longSize retains the exact total.
 * Distinct elements traverse in insertion order. All count changes invalidate
 * existing iterators except their own remove. Keys must be stable and non-reentrant.
 * No retained Entry nodes or boxed counts are added; the existing directory retains
 * a boxed row coordinate per distinct key. No VM allocation-failure transaction is
 * promised. Entry values are immutable boundary snapshots; their set remains live.
 */
public final class M3ApacheMultiSet<E> extends AbstractCollection<E> implements MultiSet<E> {
    private final M3Multiset<E> counts = new M3Multiset<>();
    public M3ApacheMultiSet() { }
    @Override public int getCount(Object element) { return counts.count(element); }
    @Override public int setCount(E element, int count) { return counts.setCount(element, count); }
    @Override public int add(E element, int occurrences) { return counts.add(element, occurrences); }
    @Override public int remove(Object element, int occurrences) { return counts.remove(element, occurrences); }
    @Override public boolean add(E element) { counts.add(element, 1); return true; }
    @Override public boolean remove(Object element) { return counts.remove(element, 1) != 0; }
    @Override public boolean contains(Object element) { return counts.contains(element); }
    @Override public int size() { return counts.size(); }
    public long longSize() { return counts.longSize(); }
    @Override public boolean isEmpty() { return counts.isEmpty(); }
    @Override public void clear() { counts.clear(); }
    @Override public Iterator<E> iterator() { return counts.iterator(); }
    @Override public Spliterator<E> spliterator() {
        return Spliterators.spliterator(iterator(), longSize(), Spliterator.ORDERED);
    }
    @Override public Set<E> uniqueSet() { return counts.elementSet(); }
    /** Visits distinct keys without occurrence expansion or Apache Entry projections. */
    public void forEachEntry(ObjIntConsumer<? super E> action) { counts.forEachEntry(action); }
    @Override public boolean addAll(Collection<? extends E> source) {
        Objects.requireNonNull(source, "source");
        if (source == this) { return counts.addAll(counts); }
        if (source instanceof MultiSet<? extends E> multiset) {
            boolean changed = false;
            for (MultiSet.Entry<? extends E> entry : multiset.entrySet()) {
                int n = entry.getCount(); counts.add(entry.getElement(), n); changed |= n != 0;
            }
            return changed;
        }
        return counts.addAll(source);
    }
    private Collection<?> distinct(Collection<?> source) {
        Objects.requireNonNull(source, "source");
        return source == this ? counts : source instanceof MultiSet<?> multiset ? multiset.uniqueSet() : source;
    }
    @Override public boolean containsAll(Collection<?> source) { return counts.containsAll(distinct(source)); }
    @Override public boolean removeAll(Collection<?> source) { return counts.removeAll(distinct(source)); }
    @Override public boolean retainAll(Collection<?> source) { return counts.retainAll(distinct(source)); }
    @Override public Set<MultiSet.Entry<E>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return counts.elementSet().size(); }
            @Override public void clear() { counts.clear(); }
            @Override public Iterator<MultiSet.Entry<E>> iterator() {
                var entries = counts.entrySet().iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { return entries.hasNext(); }
                    @Override public MultiSet.Entry<E> next() {
                        var entry = entries.next(); return new CountEntry<>(entry.getElement(), entry.getCount());
                    }
                    @Override public void remove() { entries.remove(); }
                };
            }
            @Override public boolean contains(Object value) {
                return value instanceof MultiSet.Entry<?> entry && entry.getCount() > 0
                        && counts.count(entry.getElement()) == entry.getCount();
            }
            @Override public boolean remove(Object value) {
                if (!contains(value)) { return false; }
                MultiSet.Entry<?> entry = (MultiSet.Entry<?>) value;
                counts.remove(entry.getElement(), entry.getCount()); return true;
            }
        };
    }
    @Override public boolean equals(Object other) {
        if (other == this) { return true; }
        if (!(other instanceof MultiSet<?> multiset) || size() != multiset.size()
                || uniqueSet().size() != multiset.uniqueSet().size()) { return false; }
        for (E element : uniqueSet()) {
            if (counts.count(element) != multiset.getCount(element)) { return false; }
        }
        return true;
    }
    @Override public int hashCode() { return counts.hashCode(); }
    private record CountEntry<E>(E element, int count) implements MultiSet.Entry<E> {
        @Override public E getElement() { return element; }
        @Override public int getCount() { return count; }
        @Override public boolean equals(Object other) {
            return other instanceof MultiSet.Entry<?> entry && count == entry.getCount()
                    && Objects.equals(element, entry.getElement());
        }
        @Override public int hashCode() { return Objects.hashCode(element) ^ count; }
    }
}
