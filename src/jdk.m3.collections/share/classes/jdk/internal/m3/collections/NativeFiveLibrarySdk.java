/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import java.util.Comparator;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.Objects;
import jdk.m3.collections.M3ArrayList;
import jdk.m3.collections.CollectionViews;
import jdk.m3.collections.CountedBag;
import jdk.m3.collections.FilteredDeques;
import jdk.m3.collections.FilteredViews;
import jdk.m3.collections.IndexedListSnapshot;
import jdk.m3.collections.IndexedTable;
import jdk.m3.collections.M3LinkedHashMap;
import jdk.m3.collections.M3LinkedHashSet;
import jdk.m3.collections.M3LinkedList;
import jdk.m3.collections.LongIndexProjection;
import jdk.m3.collections.MutableFilteredViews;
import jdk.m3.collections.M3TreeMap;
import jdk.m3.collections.M3TreeSet;

/**
 * Five-library capability substrate backed entirely by canonical M3 owners.
 * These are M3-native contracts, not Guava/Eclipse/Apache/Spring aliases.
 * Corresponding vendor interfaces are separate optional compatibility layers.
 */
public final class NativeFiveLibrarySdk {
    private NativeFiveLibrarySdk() { }
    public static <E> M3ArrayList<E> list() { return new M3ArrayList<>(); }
    public static <E> M3LinkedHashSet<E> set() { return new M3LinkedHashSet<>(); }
    public static <E> M3LinkedList<E> deque() { return new M3LinkedList<>(); }
    public static <K,V> M3LinkedHashMap<K,V> map() { return new M3LinkedHashMap<>(); }
    public static <K,V> M3TreeMap<K,V> sortedMap(Comparator<? super K> order) {
        return new M3TreeMap<>(Objects.requireNonNull(order, "order"));
    }
    public static <E> M3TreeSet<E> sortedSet(Comparator<? super E> order) {
        return new M3TreeSet<>(Objects.requireNonNull(order, "order"));
    }
    public static <E> CountedBag<E> bag() { return new CountedBag<>(); }
    public static <K,V> IndexedBiMap<K,V> biMap() { return new IndexedBiMap<>(); }
    public static <K,V> IndexedMultiValueMap<K,V> listMultimap() {
        return new IndexedMultiValueMap<>();
    }
    public static <K,V> IndexedSetMultiMap<K,V> setMultimap() {
        return new IndexedSetMultiMap<>();
    }
    public static <K,V> ListMultiValueMap<K,V> mapOfLists() {
        return new ListMultiValueMap<>();
    }
    public static <R,C,V> IndexedTable<R,C,V> table() { return new IndexedTable<>(); }
    public static <E> IndexedListSnapshot<E> snapshot(java.util.Collection<? extends E> values) {
        return IndexedListSnapshot.copyOf(values);
    }
    public static LongIndexProjection longProjection(long[] values) {
        return LongIndexProjection.snapshot(values);
    }

    /**
     * Live collection filter from the existing M3 backed-view owner.
     * Does not invoke Guava Collections2 or retain a second payload.
     * Rejected writes fail before the underlying owner is modified.
     */
    public static <E> Collection<E> filteredCollection(
            Collection<E> owner, Predicate<? super E> accepted) {
        return CollectionViews.filtered(Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(accepted, "accepted"));
    }

    /** Writable indexed-list filter; hidden elements remain in the sole backing owner. */
    public static <E> List<E> filteredList(
            List<E> owner, Predicate<? super E> accepted) {
        return FilteredLists.filtered(Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(accepted, "accepted"));
    }

    /** Live deque selection, retaining the M3/JDK queue order and mutation contract. */
    public static <E> Deque<E> filteredDeque(
            Deque<E> owner, Predicate<? super E> accepted) {
        return FilteredDeques.filtered(Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(accepted, "accepted"));
    }

    /** Writable sorted filter using the existing M3 comparator and range-view owner. */
    public static <E> NavigableSet<E> filteredNavigableSet(
            NavigableSet<E> owner, Predicate<? super E> accepted) {
        return MutableFilteredViews.filter(Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(accepted, "accepted"));
    }

    /** Writable map-entry filter; key comparators and stored representatives stay authoritative. */
    public static <K, V> NavigableMap<K, V> filteredNavigableMapEntries(
            NavigableMap<K, V> owner, BiPredicate<? super K, ? super V> accepted) {
        return MutableFilteredViews.filterEntries(Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(accepted, "accepted"));
    }

    /** Explicit immutable facade over a live filter, separate from writable filters. */
    public static <E> Collection<E> readOnlyFilteredCollection(
            Collection<E> owner, Predicate<? super E> accepted) {
        return FilteredViews.filter(Objects.requireNonNull(owner, "owner"),
                Objects.requireNonNull(accepted, "accepted"));
    }
}
