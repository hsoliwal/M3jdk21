/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Source-preserving successor: compose Eclipse bag hooks over existing M3CountedBag.
 */
package com.m3.collections.interop;

import com.m3.collections.M3CountedBag;
import com.m3.collections.M3LinkedHashMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.bag.mutable.AbstractMutableBag;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;

/**
 * Eclipse Collections mutable bag API over the existing M3 packed count owner.
 *
 * <p>The same owner can back Guava and Apache facades without a second count map.
 * Values and counts occupy M3's existing dynamic indexed arrays, not per-occurrence
 * entry objects. The upstream AbstractMutableBag remains responsible for Eclipse
 * collection-specific algorithms and factory return contracts. This is a
 * single-threaded mutable owner, not the vendor's concrete HashBag/serialized type.
 * The per-occurrence iterator is the incumbent fail-fast M3CountedBag cursor.
 */
public final class M3EclipseBag<E> extends AbstractMutableBag<E> {
    private final M3CountedBag<E> counts;

    public M3EclipseBag() { this(new M3CountedBag<>()); }

    /** New provider façade, same canonical M3 indexed count/value owner. */
    public M3EclipseBag(M3CountedBag<E> counts) {
        this.counts = Objects.requireNonNull(counts, "counts");
    }

    public M3CountedBag<E> m3Backend() { return counts; }

    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Bag<?> bag) || sizeDistinct() != bag.sizeDistinct()) return false;
        for (E element : counts.distinctElements()) {
            if (counts.count(element) != bag.occurrencesOf(element)) return false;
        }
        return true;
    }

    @Override public int hashCode() {
        final int[] result = {0};
        counts.forEachCount((element, quantity) ->
                result[0] += Objects.hashCode(element) ^ quantity);
        return result[0];
    }

    @Override protected RichIterable<E> getKeysView() {
        return SetAdapter.adapt(counts.distinctElements());
    }

    @Override public int size() { return counts.size(); }
    @Override public int sizeDistinct() { return counts.distinctSize(); }
    @Override public boolean isEmpty() { return counts.isEmpty(); }
    @Override public boolean contains(Object element) { return counts.contains(element); }
    @Override public int occurrencesOf(Object element) { return counts.count(element); }

    /** Eclipse returns the NEW count, unlike Guava/Apache previous-count methods. */
    @Override public int addOccurrences(E element, int quantity) {
        int previous = counts.addOccurrences(element, quantity);
        return previous + quantity;
    }

    @Override public boolean removeOccurrences(Object element, int quantity) {
        int previous = counts.removeOccurrences(element, quantity);
        return previous > 0 && quantity > 0;
    }

    @Override public boolean setOccurrences(E element, int quantity) {
        return counts.setOccurrences(element, quantity) != quantity;
    }

    @Override public boolean add(E element) { counts.add(element); return true; }
    @Override public boolean remove(Object element) { return counts.remove(element); }
    @Override public void clear() { counts.clear(); }

    @Override public void forEachWithOccurrences(ObjectIntProcedure<? super E> action) {
        Objects.requireNonNull(action, "action");
        counts.forEachCount((key, quantity) -> action.value(key, quantity));
    }

    @Override public E detectWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long expected = counts.revision();
        for (E element : counts.distinctElements()) {
            boolean matches = predicate.accept(element, counts.count(element));
            if (expected != counts.revision()) throw new ConcurrentModificationException();
            if (matches) return element;
        }
        return null;
    }

    @Override public boolean anySatisfyWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        long expected = counts.revision();
        for (E element : counts.distinctElements()) {
            boolean matches = predicate.accept(element, counts.count(element));
            if (expected != counts.revision()) throw new ConcurrentModificationException();
            if (matches) return true;
        }
        return false;
    }

    @Override public boolean allSatisfyWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return !anySatisfyWithOccurrences((item, quantity) -> !predicate.accept(item, quantity));
    }

    @Override public boolean noneSatisfyWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        return !anySatisfyWithOccurrences(predicate);
    }

    /** Remove full key multiplicities, not one matching occurrence at a time. */
    @Override public boolean removeAllIterable(Iterable<?> input) {
        Objects.requireNonNull(input, "input");
        if (input == this) {
            boolean changed = !isEmpty();
            clear();
            return changed;
        }
        ArrayList<Object> requested = new ArrayList<>();
        for (Object item : input) requested.add(item);
        boolean changed = false;
        for (Object item : requested) changed |= counts.removeOccurrences(item, Integer.MAX_VALUE) > 0;
        return changed;
    }

    @Override public boolean removeIf(org.eclipse.collections.api.block.predicate.Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return removeIfWith((key, test) -> test.accept(key), predicate);
    }

    @Override public <P> boolean removeIfWith(
            org.eclipse.collections.api.block.predicate.Predicate2<? super E, ? super P> predicate, P parameter) {
        Objects.requireNonNull(predicate, "predicate");
        long revision = counts.revision();
        ArrayList<E> doomed = new ArrayList<>();
        for (E item : counts.distinctElements()) {
            boolean remove = predicate.accept(item, parameter);
            if (counts.revision() != revision) throw new ConcurrentModificationException();
            if (remove) doomed.add(item);
        }
        boolean changed = false;
        for (E item : doomed) changed |= counts.removeOccurrences(item, Integer.MAX_VALUE) > 0;
        return changed;
    }

    @Override public <V> org.eclipse.collections.api.multimap.bag.MutableBagMultimap<V,E> groupBy(
            org.eclipse.collections.api.block.function.Function<? super E,? extends V> mapping) {
        var result = org.eclipse.collections.impl.multimap.bag.HashBagMultimap.<V,E>newMultimap();
        for (E item : this) result.put(mapping.valueOf(item), item);
        return result;
    }

    @Override public <V> org.eclipse.collections.api.multimap.bag.MutableBagMultimap<V,E> groupByEach(
            org.eclipse.collections.api.block.function.Function<? super E,? extends Iterable<V>> mapping) {
        var result = org.eclipse.collections.impl.multimap.bag.HashBagMultimap.<V,E>newMultimap();
        for (E item : this) for (V key : mapping.valueOf(item)) result.put(key, item);
        return result;
    }

    @Override public MutableBag<E> selectByOccurrences(
            org.eclipse.collections.api.block.predicate.primitive.IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        M3EclipseBag<E> result = new M3EclipseBag<>();
        counts.forEachCount((item, count) -> {
            if (predicate.accept(count)) result.addOccurrences(item, count);
        });
        return result;
    }

    @Override public org.eclipse.collections.api.map.MutableMap<E,Integer> toMapOfItemToCount() {
        M3LinkedHashMap<E,Integer> result = new M3LinkedHashMap<>();
        counts.forEachCount(result::put);
        return MapAdapter.adapt(result);
    }

    @Override public void each(Procedure<? super E> proc) {
        Objects.requireNonNull(proc, "proc");
        for (E item : this) proc.value(item);
    }

    @Override public Iterator<E> iterator() { return counts.iterator(); }
    @Override public MutableBag<E> newEmpty() { return new M3EclipseBag<>(); }
}
