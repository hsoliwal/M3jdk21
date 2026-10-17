/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A contract-explicit entry point for purpose-specific Java collection wrappers.
 *
 * <p>Use JDK wrappers where they already implement the requested role. Custom
 * adapters are limited to distinct semantics (admission, mapping, fixed-size,
 * bounded, concatenation and pages). No wrapper silently changes a live view
 * into a snapshot or upgrades an unordered collection into a sorted set.
 *
 * <p>Like JDK collection decorators, these wrappers do not close the backdoor
 * of an external reference to the backing collection. No new synchronization,
 * thread-safety or serialization is implied unless the selected JDK wrapper
 * explicitly provides that property.
 */
public final class M3CollectionWrappers {
    private M3CollectionWrappers() {
    }

    /** Backed, read-only collection (not an immutable snapshot). */
    public static <E> Collection<E> readOnly(Collection<E> source) {
        return Collections.unmodifiableCollection(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only list retaining its list interface. */
    public static <E> List<E> readOnly(List<E> source) {
        return Collections.unmodifiableList(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only set retaining set equality and uniqueness. */
    public static <E> Set<E> readOnly(Set<E> source) {
        return Collections.unmodifiableSet(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only sorted set retaining its comparator and ranges. */
    public static <E> SortedSet<E> readOnly(SortedSet<E> source) {
        return Collections.unmodifiableSortedSet(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only navigable set, including descending/range views. */
    public static <E> NavigableSet<E> readOnly(NavigableSet<E> source) {
        return Collections.unmodifiableNavigableSet(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only map with unmodifiable entry views. */
    public static <K, V> Map<K, V> readOnly(Map<K, V> source) {
        return Collections.unmodifiableMap(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only sorted map. */
    public static <K, V> SortedMap<K, V> readOnly(SortedMap<K, V> source) {
        return Collections.unmodifiableSortedMap(Objects.requireNonNull(source, "source"));
    }

    /** Backed, read-only navigable map with guarded entry projections. */
    public static <K, V> NavigableMap<K, V> readOnly(NavigableMap<K, V> source) {
        return Collections.unmodifiableNavigableMap(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized collection; callers must synchronize on the result while iterating. */
    public static <E> Collection<E> synchronizedView(Collection<E> source) {
        return Collections.synchronizedCollection(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized list; external source aliases remain unsynchronized. */
    public static <E> List<E> synchronizedView(List<E> source) {
        return Collections.synchronizedList(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized set. */
    public static <E> Set<E> synchronizedView(Set<E> source) {
        return Collections.synchronizedSet(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized sorted set, retaining comparator semantics. */
    public static <E> SortedSet<E> synchronizedView(SortedSet<E> source) {
        return Collections.synchronizedSortedSet(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized navigable set, preserving ordered operations. */
    public static <E> NavigableSet<E> synchronizedView(NavigableSet<E> source) {
        return Collections.synchronizedNavigableSet(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized map. */
    public static <K, V> Map<K, V> synchronizedView(Map<K, V> source) {
        return Collections.synchronizedMap(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized sorted map, retaining comparator semantics. */
    public static <K, V> SortedMap<K, V> synchronizedView(SortedMap<K, V> source) {
        return Collections.synchronizedSortedMap(Objects.requireNonNull(source, "source"));
    }

    /** JDK synchronized navigable map. */
    public static <K, V> NavigableMap<K, V> synchronizedView(NavigableMap<K, V> source) {
        return Collections.synchronizedNavigableMap(Objects.requireNonNull(source, "source"));
    }

    /** Runtime element-type checks at the JDK wrapper mutation boundary. */
    public static <E> Collection<E> checked(Collection<E> source, Class<E> elementType) {
        return Collections.checkedCollection(source, elementType);
    }

    /** Runtime element-type checks, retaining the list interface. */
    public static <E> List<E> checked(List<E> source, Class<E> elementType) {
        return Collections.checkedList(source, elementType);
    }

    /** Runtime element-type checks, retaining set equality. */
    public static <E> Set<E> checked(Set<E> source, Class<E> elementType) {
        return Collections.checkedSet(source, elementType);
    }

    /** Runtime element-type checks for sorted sets. */
    public static <E> SortedSet<E> checked(SortedSet<E> source, Class<E> elementType) {
        return Collections.checkedSortedSet(source, elementType);
    }

    /** Runtime element-type checks for navigable sets. */
    public static <E> NavigableSet<E> checked(NavigableSet<E> source, Class<E> elementType) {
        return Collections.checkedNavigableSet(source, elementType);
    }

    /** Runtime key and value checks for maps. */
    public static <K, V> Map<K, V> checked(
            Map<K, V> source, Class<K> keyType, Class<V> valueType) {
        return Collections.checkedMap(source, keyType, valueType);
    }

    /** Runtime key and value checks for sorted maps. */
    public static <K, V> SortedMap<K, V> checked(
            SortedMap<K, V> source, Class<K> keyType, Class<V> valueType) {
        return Collections.checkedSortedMap(source, keyType, valueType);
    }

    /** Runtime key and value checks for navigable maps. */
    public static <K, V> NavigableMap<K, V> checked(
            NavigableMap<K, V> source, Class<K> keyType, Class<V> valueType) {
        return Collections.checkedNavigableMap(source, keyType, valueType);
    }

    /** Selects the live filtering owner introduced by {@link M3CollectionViews}. */
    public static <E> Collection<E> filtered(
            Collection<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }

    /** Selects a live filtered set without losing Set semantics. */
    public static <E> Set<E> filtered(Set<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }

    /** Selects a live filtered sorted set. */
    public static <E> SortedSet<E> filtered(
            SortedSet<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }

    /** Selects a live filtered navigable set. */
    public static <E> NavigableSet<E> filtered(
            NavigableSet<E> source, Predicate<? super E> predicate) {
        return M3CollectionViews.filtered(source, predicate);
    }

    /** Live rejection filter; complements filtered without creating a new storage role. */
    public static <E> Collection<E> rejected(
            Collection<E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return filtered(source, predicate.negate());
    }

    /** Live rejection filter retaining Set equality and uniqueness semantics. */
    public static <E> Set<E> rejected(Set<E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return filtered(source, predicate.negate());
    }

    /** Live rejection filter retaining SortedSet order. */
    public static <E> SortedSet<E> rejected(
            SortedSet<E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return filtered(source, predicate.negate());
    }

    /** Live rejection filter retaining NavigableSet neighbors and ranges. */
    public static <E> NavigableSet<E> rejected(
            NavigableSet<E> source, Predicate<? super E> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return filtered(source, predicate.negate());
    }

    /**
     * Admission guard: existing elements stay visible, but rejected additions
     * throw before writing. As with Apache PredicatedCollection, other aliases
     * can bypass this boundary. An addAll rejects all invalid candidates before
     * asking the delegate to mutate; delegate failures are not rolled back.
     */
    public static <E> Collection<E> admissionFiltered(
            Collection<E> source, Predicate<? super E> predicate) {
        return M3AdmissionWrappers.collection(source, predicate);
    }

    /** Admission guard retaining the List interface; replacement is validated too. */
    public static <E> List<E> admissionFiltered(
            List<E> source, Predicate<? super E> predicate) {
        return M3AdmissionWrappers.list(source, predicate);
    }

    /** Admission guard retaining set equality and uniqueness semantics. */
    public static <E> Set<E> admissionFiltered(
            Set<E> source, Predicate<? super E> predicate) {
        return M3AdmissionWrappers.set(source, predicate);
    }

    /** Allows replacing list elements but forbids structural modification. */
    public static <E> List<E> fixedSize(List<E> source) {
        return M3FixedSizeLists.of(source);
    }

    /**
     * Enforces a maximum size for writes through the returned List interface.
     * Externally retained aliases can bypass the bound; no atomic concurrency
     * guarantee is implied. Unlike an evicting queue, this never drops elements.
     */
    public static <E> List<E> bounded(List<E> source, int maximumSize) {
        return M3BoundedLists.of(source, maximumSize);
    }

    /** Lazy, live, read-only transformation; transformation need not be injective. */
    public static <F, T> Collection<T> mappedView(
            Collection<F> source, Function<? super F, ? extends T> mapper) {
        return M3MappedViews.collection(source, mapper);
    }

    /** Lazy, live, read-only mapped list; never turns a non-injective map into a Set. */
    public static <F, T> List<T> mappedView(
            List<F> source, Function<? super F, ? extends T> mapper) {
        return M3MappedViews.list(source, mapper);
    }

    /**
     * Lazily flattens mapped iterables without retaining a result per element.
     * A mapper that returns null fails at traversal, not at construction.
     */
    public static <F, T> Collection<T> flatMappedView(
            Collection<F> source,
            Function<? super F, ? extends Iterable<? extends T>> mapper) {
        return M3MappedViews.flatMapped(source, mapper);
    }

    /**
     * Read-only live Map value projection retaining original key equivalence.
     * Mapping keys is intentionally excluded: non-injective keys break Map.
     */
    public static <K, V, T> Map<K, T> mappedValuesView(
            Map<K, V> source, Function<? super V, ? extends T> mapper) {
        return M3MappedViews.values(source, mapper);
    }

    /** Read-only concatenation, preserving duplicates and both source orders. */
    public static <E> Collection<E> concatenatedView(
            Collection<? extends E> left, Collection<? extends E> right) {
        return M3ConcatenatedViews.of(left, right);
    }

    /** Backed range using the native JDK List.subList boundary. */
    public static <E> List<E> slice(List<E> source, int fromInclusive, int toExclusive) {
        return Objects.requireNonNull(source, "source").subList(fromInclusive, toExclusive);
    }

    /** Backed reversed List using Java 21 sequenced collection semantics. */
    public static <E> List<E> reversedView(List<E> source) {
        return Objects.requireNonNull(source, "source").reversed();
    }

    /** Backed reversed NavigableSet retaining navigation and comparator behavior. */
    public static <E> NavigableSet<E> reversedView(NavigableSet<E> source) {
        return Objects.requireNonNull(source, "source").descendingSet();
    }

    /**
     * Read-only outer list of read-only backed pages. Page boundaries are
     * recalculated on each access; pages obtained earlier use JDK subList rules.
     */
    public static <E> List<List<E>> partitionedView(List<E> source, int pageSize) {
        Objects.requireNonNull(source, "source");
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        return Collections.unmodifiableList(new AbstractList<>() {
            @Override
            public List<E> get(int index) {
                Objects.checkIndex(index, size());
                int start = Math.toIntExact((long) index * pageSize);
                int end = (int) Math.min((long) source.size(), (long) start + pageSize);
                return Collections.unmodifiableList(source.subList(start, end));
            }

            @Override
            public int size() {
                return (int) (((long) source.size() + pageSize - 1L) / pageSize);
            }
        });
    }

    /** Detached, read-only ordered snapshot; permits null elements. */
    public static <E> List<E> snapshot(Collection<? extends E> source) {
        return Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(source, "source")));
    }

    /** Detached, read-only insertion-ordered de-duplication by equals(). */
    public static <E> List<E> distinctSnapshot(Collection<? extends E> source) {
        return Collections.unmodifiableList(new ArrayList<>(
                new LinkedHashSet<>(Objects.requireNonNull(source, "source"))));
    }

    /** Detached stable sorted list retaining comparator-equal duplicates. */
    public static <E> List<E> sortedSnapshot(
            Collection<? extends E> source, Comparator<? super E> comparator) {
        return M3CollectionViews.sortedSnapshot(source, comparator);
    }

}
