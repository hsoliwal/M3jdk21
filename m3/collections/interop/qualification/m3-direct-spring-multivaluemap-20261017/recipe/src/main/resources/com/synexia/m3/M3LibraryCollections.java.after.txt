/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import com.m3.collections.M3TreeMap;
import com.m3.collections.M3TreeSet;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.map.sorted.mutable.SortedMapAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.set.sorted.mutable.SortedSetAdapter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.AutoPopulatingList;

/**
 * Composes existing M3 payload owners with the libraries' public contract adapters.
 * No alternate interner, collection hierarchy, or per-element storage node is introduced.
 *
 * <p>These factories create independent mutable collections, not replacements for concrete
 * JDK or donor classes. They are not thread-safe. Serialization, donor concrete-class
 * identity, installed-JDK replacement and universal performance improvements are not
 * promised. API entry/view objects may allocate. Operations producing new collections
 * (for example Eclipse collect/toImmutable) can retain donor-defined result storage.
 * Use M3LibraryCollectors for explicit M3-backed stream result composition. Spring
 * automatically created lists and single-value copies now reuse M3 owners; lists
 * supplied by callers to put/putAll deliberately retain their original identity.
 *
 * <p>The optional interop artifact carries third-party dependencies; the core M3 collection
 * module remains java.base-only. Reference payloads remain GC-traced Java references.
 * Each Guava key owns its own value collection; Spring deliberately retains its different
 * Map-of-lists semantics, including the distinction between empty and absent mappings.
 */
public final class M3LibraryCollections {
    private M3LibraryCollections() { }

    /** Insertion-ordered map with existing M3 stable-slot, parallel-array storage. */
    public static <K, V> Map<K, V> jdkMap() { return new M3LinkedHashMap<>(); }

    /** Insertion-ordered key-only M3 set. */
    public static <E> Set<E> jdkSet() { return new M3LinkedHashSet<>(); }

    /** M3 reference sequence with primitive slot links; positional access is not O(1). */
    public static <E> List<E> jdkList() { return new M3LinkedList<>(); }

    /** The same existing M3 indexed-link owner exposed as a deque. */
    public static <E> Deque<E> jdkDeque() { return new M3LinkedList<>(); }

    /** Comparator-ordered M3 map with backed navigable views. */
    public static <K, V> NavigableMap<K, V> jdkSortedMap(Comparator<? super K> order) {
        return new M3TreeMap<>(Objects.requireNonNull(order, "order"));
    }

    /** Comparator-ordered, key-only M3 set with backed navigable views. */
    public static <E> NavigableSet<E> jdkSortedSet(Comparator<? super E> order) {
        return new M3TreeSet<>(Objects.requireNonNull(order, "order"));
    }

    /** Guava list semantics: duplicates, ordered values, live get(), pair-count size. */
    public static <K, V> ListMultimap<K, V> guavaListMultimap() {
        return Multimaps.newListMultimap(new M3LinkedHashMap<>(), M3LinkedList::new);
    }

    /** Guava set semantics: duplicate suppression and automatic empty-key removal. */
    public static <K, V> SetMultimap<K, V> guavaSetMultimap() {
        return Multimaps.newSetMultimap(new M3LinkedHashMap<>(), M3LinkedHashSet::new);
    }

    /** Guava sorted-value semantics over separate M3 key and sorted-value owners. */
    public static <K, V> SortedSetMultimap<K, V> guavaSortedSetMultimap(Comparator<? super V> order) {
        Objects.requireNonNull(order, "order");
        return Multimaps.newSortedSetMultimap(new M3LinkedHashMap<>(), () -> new M3TreeSet<>(order));
    }

    /** Guava row/column/cell semantics; both the row directory and row maps use M3. */
    public static <R, C, V> Table<R, C, V> guavaTable() {
        return Tables.newCustomTable(new M3LinkedHashMap<>(), M3LinkedHashMap::new);
    }

    /** Eclipse MutableMap API over the existing M3 map owner, without copying. */
    public static <K, V> MutableMap<K, V> eclipseMap() {
        return MapAdapter.adapt(new M3LinkedHashMap<>());
    }

    /** Eclipse MutableList API over M3 indexed links, without copying. */
    public static <E> MutableList<E> eclipseList() {
        return ListAdapter.adapt(new M3LinkedList<>());
    }

    /** Eclipse MutableSet API over the existing key-only M3 owner. */
    public static <E> MutableSet<E> eclipseSet() {
        return SetAdapter.adapt(new M3LinkedHashSet<>());
    }

    /** Eclipse sorted-map operations and range views over M3's existing tree. */
    public static <K, V> MutableSortedMap<K, V> eclipseSortedMap(Comparator<? super K> order) {
        return SortedMapAdapter.adapt(new M3TreeMap<>(Objects.requireNonNull(order, "order")));
    }

    /** Eclipse sorted-set operations and range views over M3's existing key-only tree. */
    public static <E> MutableSortedSet<E> eclipseSortedSet(Comparator<? super E> order) {
        return SortedSetAdapter.adapt(new M3TreeSet<>(Objects.requireNonNull(order, "order")));
    }

    /**
     * Spring Map-of-lists semantics with M3-created key directories, value lists and
     * single-value copies. Unlike Guava, size counts distinct keys. Supplied lists stay aliased.
     */
    public static <K, V> MultiValueMap<K, V> springMultiValueMap() {
        return new M3SpringArrayMultiValueMap<>();
    }

    /** Spring's single-value-to-multi-value view; no multi-value emulation is promised. */
    public static <K, V> MultiValueMap<K, V> springSingleValueMap() {
        return MultiValueMap.fromSingleValue(new M3LinkedHashMap<K, V>());
    }

    /** Lazy Spring element creation over the existing M3 indexed-link list owner. */
    public static <E> List<E> springAutoPopulatingList(AutoPopulatingList.ElementFactory<E> factory) {
        return new AutoPopulatingList<>(new M3LinkedList<E>(), factory);
    }
    /** Apache ListValuedMap contract, not Guava ListMultimap or Spring MultiValueMap. */
    public static <K, V> org.apache.commons.collections4.ListValuedMap<K, V> apacheListValuedMap() {
        return new M3ApacheListValuedMap<>();
    }

    /** Apache SetValuedMap contract with M3-owned keys and value sets. */
    public static <K, V> org.apache.commons.collections4.SetValuedMap<K, V> apacheSetValuedMap() {
        return new M3ApacheSetValuedMap<>();
    }

    /**
     * Guava live filtered Collection over an existing M3 list owner; never a snapshot.
     * Guava owns predicate evaluation, insertion rejection and iterator-remove semantics.
     * Filtering does not acquire an independent value store or cache mutable predicates.
     */
    public static <E> java.util.Collection<E> guavaFilteredCollection(
            M3LinkedList<E> owner, com.google.common.base.Predicate<? super E> predicate) {
        return com.google.common.collect.Collections2.filter(
                Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(predicate, "predicate"));
    }

    /** Guava live sorted/range/descending view over the existing M3 navigable-set owner. */
    public static <E> NavigableSet<E> guavaFilteredNavigableSet(
            M3TreeSet<E> owner, com.google.common.base.Predicate<? super E> predicate) {
        return com.google.common.collect.Sets.filter(
                Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(predicate, "predicate"));
    }
}
