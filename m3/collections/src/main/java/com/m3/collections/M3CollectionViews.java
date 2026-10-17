/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Collection;
import java.util.CollectionViews;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;

/**
 * Source/binary compatibility entry points for the migrated java.util owner.
 * All storage and behavior belong to {@link CollectionViews}; this class retains no
 * predicate, element or secondary implementation. Requires the matched M3JDK image.
 * Existing clients keep their JVM method descriptors. New clients use CollectionViews.
 * Not marked @Deprecated yet: warnings-as-errors users retain source compatibility.
 */
public final class M3CollectionViews {
    private M3CollectionViews() { }
    public static <E> Collection<E> filtered(Collection<E> source, Predicate<? super E> predicate) {
        return CollectionViews.filtered(source, predicate);
    }
    public static <E> Set<E> filtered(Set<E> source, Predicate<? super E> predicate) {
        return CollectionViews.filtered(source, predicate);
    }
    public static <E> SortedSet<E> filtered(SortedSet<E> source, Predicate<? super E> predicate) {
        return CollectionViews.filtered(source, predicate);
    }
    public static <E> NavigableSet<E> filtered(NavigableSet<E> source, Predicate<? super E> predicate) {
        return CollectionViews.filtered(source, predicate);
    }
    public static <E> List<E> sortedSnapshot(Collection<? extends E> source, Comparator<? super E> order) {
        return CollectionViews.sortedSnapshot(source, order);
    }
    public static <E> List<E> filteredSnapshot(Collection<? extends E> source, Predicate<? super E> predicate) {
        return CollectionViews.filteredSnapshot(source, predicate);
    }
    public static <E> List<E> filteredSortedSnapshot(Collection<? extends E> source,
            Predicate<? super E> predicate, Comparator<? super E> order) {
        return CollectionViews.filteredSortedSnapshot(source, predicate, order);
    }
}
