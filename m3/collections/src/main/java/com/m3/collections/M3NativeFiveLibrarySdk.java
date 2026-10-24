/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Comparator;
import java.util.Objects;

/**
 * Five-library capability substrate backed entirely by canonical M3 owners.
 * These are M3-native contracts, not Guava/Eclipse/Apache/Spring aliases.
 * Corresponding vendor interfaces are separate optional compatibility layers.
 */
public final class M3NativeFiveLibrarySdk {
    private M3NativeFiveLibrarySdk() { }
    public static <E> M3ArrayList<E> list() { return new M3ArrayList<>(); }
    public static <E> M3LinkedHashSet<E> set() { return new M3LinkedHashSet<>(); }
    public static <K,V> M3LinkedHashMap<K,V> map() { return new M3LinkedHashMap<>(); }
    public static <K,V> M3TreeMap<K,V> sortedMap(Comparator<? super K> order) {
        return new M3TreeMap<>(Objects.requireNonNull(order, "order"));
    }
    public static <E> M3CountedBag<E> bag() { return new M3CountedBag<>(); }
    public static <K,V> M3IndexedBiMap<K,V> biMap() { return new M3IndexedBiMap<>(); }
    public static <K,V> M3IndexedMultiValueMap<K,V> listMultimap() {
        return new M3IndexedMultiValueMap<>();
    }
    public static <K,V> M3IndexedSetMultiMap<K,V> setMultimap() {
        return new M3IndexedSetMultiMap<>();
    }
    public static <K,V> M3ListMultiValueMap<K,V> mapOfLists() {
        return new M3ListMultiValueMap<>();
    }
    public static <R,C,V> M3IndexedTable<R,C,V> table() { return new M3IndexedTable<>(); }
    public static <E> M3IndexedListSnapshot<E> snapshot(java.util.Collection<? extends E> values) {
        return M3IndexedListSnapshot.copyOf(values);
    }
    public static M3LongIndexProjection longProjection(long[] values) {
        return M3LongIndexProjection.snapshot(values);
    }
}
