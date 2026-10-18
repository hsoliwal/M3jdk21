/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.google.common.collect.Multiset;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3ObjectCounts;
import com.m3.collections.M3TreeMap;
import java.util.ConcurrentModificationException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import org.apache.commons.collections4.MultiSet;
import org.eclipse.collections.api.bag.MutableBag;
import org.springframework.util.MultiValueMap;

/**
 * One selected, detached array/count/flag owner with five genuine provider projections.
 *
 * <p>A call to {@link #capture} freezes selected physical occurrences using the
 * incumbent M3IndexPathMasks Java/C17 JNI selector. Only primitive coordinates
 * and 32-bit state masks enter JNI: object references and provider callbacks
 * remain in Java. This is a bounded, explicit count projection, not an assertion
 * that a Spring multivalue map is a Guava multiset or an Apache bag.</p>
 *
 * <p>Guava, Eclipse, and Apache facades share the detached mutable M3ObjectCounts
 * owner, so mutations through one are visible to the other two. JDK and Spring
 * projections materialize independent M3-owned maps: their different Map and
 * map-of-lists semantics must not be conflated with occurrence updates.
 * Not concurrent, serializable, or a live filtered view of the original owner.</p>
 */
public final class M3FiveProviderFlagSelection<E> {
    private final M3ObjectCounts<E> selected;

    private M3FiveProviderFlagSelection(M3ObjectCounts<E> selected) {
        this.selected = selected;
    }

    /** Selection order follows the parent's distinct encounter coordinates. */
    public static <E> M3FiveProviderFlagSelection<E> capture(
            M3ObjectCounts<E> source, int requiredMask, int excludedMask,
            M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(backend, "backend");
        long revision = source.countRevision();
        List<E> keys = source.flaggedElements(requiredMask, excludedMask, backend);
        M3ObjectCounts<E> snapshot = new M3ObjectCounts<>();
        for (E key : keys) {
            int count = source.count(key);
            if (count <= 0) {
                throw new ConcurrentModificationException("selected owner changed during capture");
            }
            snapshot.add(key, count);
            snapshot.setFlags(key, source.flags(key));
        }
        if (source.countRevision() != revision) {
            throw new ConcurrentModificationException("selected owner changed during capture");
        }
        snapshot.verifyInvariants();
        return new M3FiveProviderFlagSelection<>(snapshot);
    }

    public int distinctSize() { return selected.distinctSize(); }

    public long totalCount() { return selected.totalCount(); }

    /** Actual 32-bit flag word at the selected M3 physical key slot. */
    public int flags(Object key) { return selected.flags(key); }

    /** Independent insertion-ordered java.util.Map, with M3 parallel slot storage. */
    public Map<E, Integer> jdkCounts() {
        Map<E, Integer> result = new M3LinkedHashMap<>();
        selected.forEachCount(result::put);
        return result;
    }

    /**
     * Independent JDK NavigableMap, with M3 tree/offset backing.
     * Comparator-equal but distinct source keys are rejected rather than
     * silently coalesced or replaced.
     */
    public NavigableMap<E, Integer> jdkSortedCounts(Comparator<? super E> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        NavigableMap<E, Integer> result = new M3TreeMap<>(comparator);
        selected.forEachCount((key, count) -> {
            if (result.containsKey(key)) {
                throw new IllegalArgumentException("comparator collapses distinct selected keys");
            }
            result.put(key, count);
        });
        return result;
    }

    /** Real Guava Multiset backed by selected M3 primitive counts. */
    public Multiset<E> guavaMultiset() { return new M3GuavaMultiset<>(selected); }

    /** Real Eclipse Collections MutableBag backed by the SAME selected counts. */
    public MutableBag<E> eclipseBag() { return new M3EclipseBag<>(selected); }

    /** Real Apache Commons MultiSet backed by the SAME selected counts. */
    public MultiSet<E> apacheMultiSet() { return new M3ApacheMultiSet<>(selected); }

    /**
     * Independent real Spring MultiValueMap: one singleton list holding a
     * count for each distinct key. This is an explicit count projection,
     * not a Spring implementation of Bag/Multiset.
     */
    public MultiValueMap<E, Integer> springCountLists() {
        MultiValueMap<E, Integer> result = new M3SpringArrayMultiValueMap<>();
        selected.forEachCount(result::add);
        return result;
    }
}
