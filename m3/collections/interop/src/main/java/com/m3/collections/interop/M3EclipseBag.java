/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
// SPDX-License-Identifier: EPL-1.0 OR BSD-3-Clause
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project:
// adapt Eclipse Collections 11.1.0 bag hooks to shared primitive M3 count slots.
// Donor commit fb6abf7fe655ff04acf616158bce49725f2df5ad;
// AbstractHashBag blob 6a8a81b5cfd12ef193b344d84842aaaf51a32894;
// AbstractMutableBag blob 381dbbff5b090da9b2533168272bebe6563f5b65;
// AbstractMutableBagIterable blob 6d26bdff9030aa4f2bc76205ee253817c23903c3.
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3ObjectCounts;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.impl.bag.mutable.AbstractMutableBag;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap;
import org.eclipse.collections.impl.set.mutable.SetAdapter;

/**
 * Eclipse {@link MutableBag} contracts over an explicitly shareable M3 count owner.
 *
 * <p>Only the owner is retained: keys occupy existing M3 stable slots, with primitive
 * count and flag columns. There is no backing Eclipse hash bag, boxed counter, or
 * retained entry object. Null elements are supported. Encounter order follows first
 * insertion and equal elements occur consecutively during iteration.
 *
 * <p>Counts are nonnegative integers and an overflowing per-element addition fails
 * before that element changes. The exact total remains in the owner's {@code long};
 * {@link #size()} saturates at {@link Integer#MAX_VALUE}. Eclipse operations returning
 * an {@code int} index or materializing every occurrence have their usual integer
 * representability and memory limits. Eclipse 11.1.0's wrapped negative counts and its
 * last-occurrence iterator removal corruption are deliberately not reproduced.
 *
 * <p>The inherited public Eclipse hierarchy supplies rich traversal algorithms.
 * Select, reject, collect, flatCollect, countBy, toBag, and item/count map exports
 * use M3 owners, including their occurrence-aware and parameterized variants.
 * Immutable, sorted, primitive, partition, and multimap result factories retain the
 * pinned provider's result implementations; these are detached results, not this
 * bag's storage. Synchronized and unmodifiable wrappers are live provider views.
 * This mutable owner and independently obtained facades are not thread-safe.
 */
public final class M3EclipseBag<E> extends AbstractMutableBag<E> {
    private final M3ObjectCounts<E> counts;

    /** Creates an empty M3-owned Eclipse bag. */
    public M3EclipseBag() { this(new M3ObjectCounts<>()); }

    /** Shares the supplied owner, including subsequent count and application-flag changes. */
    public M3EclipseBag(M3ObjectCounts<E> counts) {
        this.counts = Objects.requireNonNull(counts, "counts");
    }

    @Override public int size() { return (int) Math.min(Integer.MAX_VALUE, counts.totalCount()); }
    @Override public int sizeDistinct() { return counts.distinctSize(); }
    @Override public int occurrencesOf(Object item) { return counts.count(item); }
    @Override public boolean isEmpty() { return counts.distinctSize() == 0; }
    @Override public boolean contains(Object item) { return counts.count(item) != 0; }
    @Override public void clear() { counts.clear(); }

    @Override public boolean add(E item) { counts.add(item, 1); return true; }

    /** Eclipse returns the new count, unlike Guava's previous-count convention. */
    @Override public int addOccurrences(E item, int occurrences) {
        return counts.add(item, occurrences) + occurrences;
    }

    @Override public boolean remove(Object item) { return counts.remove(item, 1) != 0; }

    @Override public boolean removeOccurrences(Object item, int occurrences) {
        int old = counts.remove(item, occurrences);
        return occurrences != 0 && old != 0;
    }

    @Override public boolean setOccurrences(E item, int occurrences) {
        return counts.setCount(item, occurrences) != occurrences;
    }

    @Override public Iterator<E> iterator() {
        return new M3ApacheCountIterator<>(counts, () -> 0L);
    }

    @Override protected RichIterable<E> getKeysView() {
        return SetAdapter.adapt(counts.elementSet());
    }

    /** A live, unmodifiable distinct view, matching Eclipse's bag view contract. */
    @Override public RichIterable<E> distinctView() {
        return SetAdapter.adapt(counts.elementSet()).asUnmodifiable();
    }

    @Override public void forEachWithOccurrences(ObjectIntProcedure<? super E> procedure) {
        counts.forEachCount((item, occurrences) -> procedure.value(item, occurrences));
    }

    @Override public void each(Procedure<? super E> procedure) {
        counts.forEachCount((item, occurrences) -> {
            for (int i = 0; i < occurrences; i++) procedure.value(item);
        });
    }

    @Override public boolean anySatisfyWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        long expected = counts.countRevision();
        for (E item : counts.elementSet()) {
            boolean accepted = predicate.accept(item, counts.count(item));
            checkRevision(expected);
            if (accepted) return true;
        }
        return false;
    }

    @Override public boolean allSatisfyWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        return !anySatisfyWithOccurrences((item, occurrences) -> !predicate.accept(item, occurrences));
    }

    @Override public boolean noneSatisfyWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        return !anySatisfyWithOccurrences(predicate);
    }

    @Override public E detectWithOccurrences(ObjectIntPredicate<? super E> predicate) {
        long expected = counts.countRevision();
        for (E item : counts.elementSet()) {
            boolean accepted = predicate.accept(item, counts.count(item));
            checkRevision(expected);
            if (accepted) return item;
        }
        return null;
    }

    @Override public boolean removeIf(Predicate<? super E> predicate) {
        return removeIfWith((item, condition) -> condition.accept(item), predicate);
    }

    @Override public <P> boolean removeIfWith(Predicate2<? super E, ? super P> predicate, P parameter) {
        boolean changed = false;
        long expected = counts.countRevision();
        Iterator<E> iterator = counts.elementSet().iterator();
        while (iterator.hasNext()) {
            E item = iterator.next();
            boolean remove = predicate.accept(item, parameter);
            checkRevision(expected);
            if (remove) {
                iterator.remove();
                expected = counts.countRevision();
                changed = true;
            }
        }
        return changed;
    }

    @Override public boolean addAllIterable(Iterable<? extends E> source) {
        if (source == null) throw new IllegalArgumentException("Cannot perform a forEachWith on null");
        if (source instanceof Bag<? extends E> bag) return addAllBag(bag);
        boolean changed = false;
        for (E item : source) { add(item); changed = true; }
        return changed;
    }

    @Override protected boolean addAllBag(Bag<? extends E> source) {
        // Snapshot primitive counts so self-addition and aliased provider wrappers are safe.
        M3ObjectCounts<E> additions = new M3ObjectCounts<>();
        source.forEachWithOccurrences(additions::add);
        additions.forEachCount(counts::add);
        return additions.distinctSize() != 0;
    }

    @Override public boolean removeAllIterable(Iterable<?> source) {
        Set<Object> keys = distinctCopy(source);
        Set<E> elements = counts.elementSet();
        boolean changed = false;
        for (Object key : keys) changed |= elements.remove(key);
        return changed;
    }

    @Override public boolean retainAllIterable(Iterable<?> source) {
        Set<Object> keys = distinctCopy(source);
        return removeIfWith((item, retained) -> !retained.contains(item), keys);
    }

    private static Set<Object> distinctCopy(Iterable<?> source) {
        Objects.requireNonNull(source, "source");
        Set<Object> keys = new M3LinkedHashSet<>();
        if (source instanceof Bag<?> bag) bag.forEachWithOccurrences((item, occurrences) -> keys.add(item));
        else for (Object item : source) keys.add(item);
        return keys;
    }

    @Override public M3EclipseBag<E> newEmpty() { return new M3EclipseBag<>(); }

    @Override public M3EclipseBag<E> selectByOccurrences(IntPredicate predicate) {
        M3EclipseBag<E> result = newEmpty();
        forEachWithOccurrences((item, occurrences) -> {
            if (predicate.accept(occurrences)) result.addOccurrences(item, occurrences);
        });
        return result;
    }

    @Override public <S> M3EclipseBag<S> selectInstancesOf(Class<S> type) {
        M3EclipseBag<S> result = new M3EclipseBag<>();
        forEachWithOccurrences((item, occurrences) -> {
            if (type.isInstance(item)) result.addOccurrences(type.cast(item), occurrences);
        });
        return result;
    }

    @Override public <V> M3EclipseBag<V> collect(Function<? super E, ? extends V> function) {
        return collect(function, new M3EclipseBag<>());
    }

    @Override public <P, V> M3EclipseBag<V> collectWith(
            Function2<? super E, ? super P, ? extends V> function, P parameter) {
        return collectWith(function, parameter, new M3EclipseBag<>());
    }

    @Override public <V> M3EclipseBag<V> collectIf(
            Predicate<? super E> predicate, Function<? super E, ? extends V> function) {
        return collectIf(predicate, function, new M3EclipseBag<>());
    }

    @Override public <V> M3EclipseBag<V> collectWithOccurrences(
            ObjectIntToObjectFunction<? super E, ? extends V> function) {
        return collectWithOccurrences(function, new M3EclipseBag<>());
    }

    @Override public <V> M3EclipseBag<V> flatCollect(Function<? super E, ? extends Iterable<V>> function) {
        return flatCollect(function, new M3EclipseBag<>());
    }

    @Override public M3EclipseBag<E> toBag() {
        M3EclipseBag<E> result = newEmpty();
        forEachWithOccurrences(result::addOccurrences);
        return result;
    }

    @Override public MutableMap<E, Integer> toMapOfItemToCount() {
        // Integer values are mandated by the exported API; the source retains primitive counts.
        MutableMap<E, Integer> result = MapAdapter.adapt(new M3LinkedHashMap<>());
        forEachWithOccurrences(result::put);
        return result;
    }

    @Override public <V> MutableBagMultimap<V, E> groupBy(Function<? super E, ? extends V> function) {
        return groupBy(function, HashBagMultimap.newMultimap());
    }

    @Override public <V> MutableBagMultimap<V, E> groupByEach(
            Function<? super E, ? extends Iterable<V>> function) {
        return groupByEach(function, HashBagMultimap.newMultimap());
    }

    @Override public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Bag<?> bag) || sizeDistinct() != bag.sizeDistinct()) return false;
        return allSatisfyWithOccurrences((item, occurrences) -> bag.occurrencesOf(item) == occurrences);
    }

    @Override public int hashCode() {
        int hash = 0;
        for (E item : counts.elementSet()) hash += Objects.hashCode(item) ^ counts.count(item);
        return hash;
    }

    private void checkRevision(long expected) {
        if (counts.countRevision() != expected) throw new ConcurrentModificationException();
    }

    /** Checks primitive occupancy, slot ownership, and the exact total without expanding occurrences. */
    public void verifyInvariants() { counts.verifyInvariants(); }
}
