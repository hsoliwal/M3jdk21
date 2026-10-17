/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Named M3 SDK entry points, not copies of source-provider framework classes.
 * JDK/Guava/Eclipse/Spring/Commons FQNs retain their original meaning.
 * Semantic analogues and pending interfaces are listed in SDK_FAMILY_MAP.tsv.
 *
 * <p>This class delegates to the existing M3 collection owners. Arbitrary Java
 * Predicate/Comparator callbacks and object payloads never enter JNI. Primitive
 * coordinate/mask selection may opt into the existing C17 JNI kernel explicitly.
 */
public final class M3CollectionSdk {
    private M3CollectionSdk() { }

    /** Exactly a JDK ArrayList fallback, not a disguised M3 implementation. */
    public static <E> ArrayList<E> jdkArrayList() { return new ArrayList<>(); }

    public static M3LongArrayList longArrayList() { return M3Collections.longArrayList(); }
    public static M3LongHashSet longHashSet() { return M3Collections.longHashSet(); }
    public static M3LongLongHashMap longLongHashMap() { return M3Collections.longLongHashMap(); }
    public static M3LongBag longBag() { return new M3LongBag(); }
    public static <K, V> M3TreeMap<K, V> treeMap() { return M3Collections.treeMap(); }
    public static <E> M3TreeSet<E> treeSet() { return M3Collections.treeSet(); }

    /** Unmodifiable lazy outer partition index, mutable JDK-backed inner ranges. */
    public static <E> List<List<E>> partition(List<E> source, int width) {
        return M3IndexPartitions.view(source, width);
    }

    public static <E> List<E> readOnlyFilteredList(
            List<E> source, Predicate<? super E> predicate) {
        return M3FilteredLists.readOnlyFiltered(source, predicate);
    }

    public static <E> List<E> mutableFilteredList(
            List<E> source, Predicate<? super E> predicate) {
        return M3FilteredLists.filtered(source, predicate);
    }

    public static <E> NavigableSet<E> readOnlyFilteredNavigableSet(
            NavigableSet<E> source, Predicate<? super E> predicate) {
        return M3FilteredViews.filter(source, predicate);
    }

    public static <E> NavigableSet<E> mutableFilteredNavigableSet(
            NavigableSet<E> source, Predicate<? super E> predicate) {
        return M3MutableFilteredViews.filter(source, predicate);
    }

    public static <K, V> NavigableMap<K, V> readOnlyFilteredNavigableMap(
            NavigableMap<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        return M3FilteredViews.filterEntries(source, predicate);
    }

    public static <K, V> NavigableMap<K, V> mutableFilteredNavigableMap(
            NavigableMap<K, V> source, BiPredicate<? super K, ? super V> predicate) {
        return M3MutableFilteredViews.filterEntries(source, predicate);
    }

    public static M3LongIndexProjection indexedLongSnapshot(long[] input) {
        return M3LongIndexProjection.snapshot(input);
    }

    /** Pure precomputed 32-bit state-mask selection over signed index paths. */
    public static int[] selectMaskedPath(int[] rows, int[] physicalOffsets,
            int[][] statePlanes, int[] required, int[] forbidden,
            int first, int count, int stride, M3IndexPathMasks.Backend backend) {
        return M3IndexPathMasks.select(rows, physicalOffsets, statePlanes,
                required, forbidden, first, count, stride, backend);
    }
}
