/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.pcollections;

import com.m3.collections.M3CollectionViews;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;

/**
 * Entry points for the additive M3 persistent collection family. The unchanged
 * org.pcollections module remains available for donor concrete factories and
 * their exact historical contracts. Methods named liveFiltered delegate to the
 * existing M3 backing-collection wrappers; view/copyOf create immutable snapshots.
 */
public final class M3PCollections {
    private M3PCollections() { }

    public static <E> M3PVector<E> vector() { return M3PVector.empty(); }
    public static <E> M3PVector<E> vector(Collection<? extends E> values) { return M3PVector.from(values); }
    public static <E> M3PStack<E> stack() { return M3PStack.empty(); }
    public static <E> M3PStack<E> stack(Collection<? extends E> values) { return M3PStack.from(values); }
    public static <E> M3PQueue<E> queue() { return M3PQueue.empty(); }
    public static <E> M3PQueue<E> queue(Collection<? extends E> values) { return M3PQueue.from(values); }
    public static <K, V> M3PMap<K, V> map() { return M3PMap.empty(); }
    public static <K, V> M3PMap<K, V> map(Map<? extends K, ? extends V> values) { return M3PMap.from(values); }
    public static <E> M3PSet<E> set() { return M3PSet.empty(); }
    public static <E> M3PSet<E> set(Collection<? extends E> values) { return M3PSet.from(values); }
    public static <E> M3PBag<E> bag() { return M3PBag.empty(); }
    public static <E> M3PBag<E> bag(Collection<? extends E> values) { return M3PBag.from(values); }
    public static <K, V> M3POrderedMap<K, V> orderedMap() { return M3POrderedMap.empty(); }
    public static <K, V> M3POrderedMap<K, V> orderedMap(Map<? extends K, ? extends V> values) {
        return M3POrderedMap.from(values);
    }
    public static <E> M3POrderedSet<E> orderedSet() { return M3POrderedSet.empty(); }
    public static <E> M3POrderedSet<E> orderedSet(Collection<? extends E> values) {
        return M3POrderedSet.from(values);
    }
    public static <E extends Comparable<? super E>> M3PSortedSet<E> sortedSet() {
        return M3PSortedSet.empty();
    }
    public static <E> M3PSortedSet<E> sortedSet(Comparator<? super E> comparator) {
        return M3PSortedSet.empty(comparator);
    }
    public static <E> M3PSortedSet<E> sortedSet(Collection<? extends E> values,
                                               Comparator<? super E> comparator) {
        return M3PSortedSet.from(comparator, values);
    }
    public static <K extends Comparable<? super K>, V> M3PSortedMap<K, V> sortedMap() {
        return M3PSortedMap.empty();
    }
    public static <K, V> M3PSortedMap<K, V> sortedMap(Comparator<? super K> comparator) {
        return M3PSortedMap.empty(comparator);
    }
    public static <K, V> M3PSortedMap<K, V> sortedMap(Map<? extends K, ? extends V> values,
                                                  Comparator<? super K> comparator) {
        return M3PSortedMap.from(comparator, values);
    }
    public static <E> M3PListView<E> view(M3PVector<E> source) { return M3PListView.of(source); }
    public static <E> M3PListView<E> copyOf(Collection<? extends E> source) { return M3PListView.copyOf(source); }

    /** Live filtered collection, with the existing owner's complete mutation contract. */
    public static <E> Collection<E> liveFiltered(Collection<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }
    public static <E> Set<E> liveFiltered(Set<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }
    public static <E> SortedSet<E> liveFiltered(SortedSet<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }
    public static <E> NavigableSet<E> liveFiltered(NavigableSet<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }
}
