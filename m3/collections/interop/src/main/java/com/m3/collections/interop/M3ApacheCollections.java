/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import com.m3.collections.M3LinkedList;
import com.m3.collections.M3TreeMap;
import com.m3.collections.M3TreeSet;
import java.util.Comparator;
import java.util.Objects;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.list.PredicatedList;
import org.apache.commons.collections4.map.PredicatedMap;
import org.apache.commons.collections4.map.PredicatedSortedMap;
import org.apache.commons.collections4.set.PredicatedSet;
import org.apache.commons.collections4.set.PredicatedSortedSet;

/**
 * Apache Commons Collections 4 predicated API over existing M3 storage owners.
 *
 * <p>These are <em>Apache predicated</em> (write-validating) decorators, not
 * live filtering views. Existing elements remain visible, rejected insertions
 * raise Apache-defined exceptions, and accepted mutations are forwarded to
 * the M3 storage owner. Do not substitute these for Guava filtered collections
 * or for {@link com.m3.collections.M3FilteredViews}, which hides elements.
 *
 * <p>The public Commons API names, predicate type and sorted range contracts
 * remain Apache-owned. The M3 owners retain their array/slot/index layouts;
 * each factory creates fresh state, with no extra per-element collection node.
 * Decorator objects themselves are allocated, and Commons may allocate results
 * for operations whose contract calls for them. No JDK implementation or JNI
 * symbol is replaced; Java callbacks never cross the existing mask JNI bridge.
 * The separately built interop artifact (not java.base) owns this dependency.
 * Instances are not thread-safe and do not promise donor concrete-class
 * identity or serialized-form interoperability.
 */
public final class M3ApacheCollections {
    private M3ApacheCollections() { }

    /** Apache list admission semantics with M3 linked-index storage. */
    @SuppressWarnings("deprecation")
    public static <E> PredicatedList<E> predicatedList(Predicate<? super E> predicate) {
        return PredicatedList.predicatedList(new M3LinkedList<>(),
                Objects.requireNonNull(predicate, "predicate"));
    }

    /** Apache set admission semantics with M3 array/slot-backed membership. */
    @SuppressWarnings("deprecation")
    public static <E> PredicatedSet<E> predicatedSet(Predicate<? super E> predicate) {
        return PredicatedSet.predicatedSet(new M3LinkedHashSet<>(),
                Objects.requireNonNull(predicate, "predicate"));
    }

    /** Apache sorted-set admission semantics; range views preserve M3 comparator ownership. */
    @SuppressWarnings("deprecation")
    public static <E> PredicatedSortedSet<E> predicatedSortedSet(
            Comparator<? super E> order, Predicate<? super E> predicate) {
        return PredicatedSortedSet.predicatedSortedSet(
                new M3TreeSet<>(Objects.requireNonNull(order, "order")),
                Objects.requireNonNull(predicate, "predicate"));
    }

    /** Apache map key/value admission over M3 reference key/value index lanes. */
    @SuppressWarnings("deprecation")
    public static <K, V> PredicatedMap<K, V> predicatedMap(
            Predicate<? super K> keys, Predicate<? super V> values) {
        return PredicatedMap.predicatedMap(new M3LinkedHashMap<>(), keys, values);
    }

    /** Apache sorted-map key/value admission over the existing M3 tree index. */
    @SuppressWarnings("deprecation")
    public static <K, V> PredicatedSortedMap<K, V> predicatedSortedMap(
            Comparator<? super K> order, Predicate<? super K> keys,
            Predicate<? super V> values) {
        return PredicatedSortedMap.predicatedSortedMap(
                new M3TreeMap<>(Objects.requireNonNull(order, "order")), keys, values);
    }
}
