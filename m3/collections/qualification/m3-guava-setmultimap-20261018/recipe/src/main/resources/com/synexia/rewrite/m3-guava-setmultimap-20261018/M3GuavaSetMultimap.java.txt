/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Recipe-generated M3 five-library SDK receiver. No vendor implementation code copied.
 */
package com.m3.collections.interop;

import com.google.common.collect.ForwardingSetMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Objects;

/**
 * Actual Guava {@link SetMultimap} API over M3 reference/index storage.
 *
 * <p>The single authoritative directory is {@link M3LinkedHashMap}; each key's
 * values belong to an {@link M3LinkedHashSet}. Guava's existing, qualified
 * public multimap view machinery supplies contract-specific live {@code get},
 * {@code asMap}, {@code entries}, {@code keys}, and removal behavior. No Guava
 * hashmap or independent multimap payload store is allocated. The forwarding
 * object and public views are API objects, not a stored Entry/Node per mapping.
 *
 * <p>Null keys and values are admitted; duplicate key/value pairs are not.
 * Equality, iteration and mutation contracts remain Guava's, rather than
 * being confused with JDK Map, Apache MultiValuedMap, Eclipse Multimap or
 * Spring's key-count MultiValueMap. This class is single-threaded and has no
 * concrete-class serialization promise.
 *
 * <p>An opt-in M3 extension can snapshot a full-width {@code int[]} state lane
 * aligned to the current <em>entry encounter order</em> and select a detached
 * M3-backed result by the existing index-path Java/C17 JNI selector. Java
 * objects, predicates and comparators never cross JNI; native calls retain
 * no heap pointer. This snapshot does not turn a live filter into a cache.
 */
public final class M3GuavaSetMultimap<K, V> extends ForwardingSetMultimap<K, V> {
    private final M3LinkedHashMap<K, Collection<V>> directory = new M3LinkedHashMap<>();
    private final SetMultimap<K, V> view =
            Multimaps.newSetMultimap(directory, M3LinkedHashSet::new);

    /** Creates an independently owned M3-backed set multimap. */
    public M3GuavaSetMultimap() { }

    /** Strongly typed M3 entry point, distinct from the existing Guava factory. */
    public static <K, V> M3GuavaSetMultimap<K, V> create() {
        return new M3GuavaSetMultimap<>();
    }

    @Override protected SetMultimap<K, V> delegate() { return view; }

    /** Returns the number of distinct keys backed by M3's canonical indexed map. */
    public int distinctKeys() { return directory.size(); }

    /**
     * Selects occurrence rows matching every required and no forbidden bit.
     * The state array must be aligned to {@link #entries()} encounter order at
     * invocation. Duplicate entries are forbidden by the SetMultimap contract.
     *
     * <p>All 32 bits are significant; bits may be signed. A result owns new
     * M3 reference/offset collections and is unaffected by subsequent edits.
     * The required native mode is explicit and never silently falls back.
     */
    public M3GuavaSetMultimap<K, V> selectedByStates(
            int[] states, int requiredMask, int forbiddenMask,
            M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        int count = size();
        if (states.length != count) {
            throw new IllegalArgumentException("aligned state length must equal entry count");
        }
        Object[] keys = new Object[count];
        Object[] values = new Object[count];
        int[] positions = new int[count];
        int ordinal = 0;
        for (Map.Entry<K, V> entry : entries()) {
            if (ordinal >= count) throw new ConcurrentModificationException("set multimap resized during capture");
            keys[ordinal] = entry.getKey();
            values[ordinal] = entry.getValue();
            positions[ordinal] = ordinal;
            ordinal++;
        }
        if (ordinal != count || size() != count) {
            throw new ConcurrentModificationException("set multimap resized during capture");
        }
        int[] selected = M3IndexPathMasks.select(
                positions, positions, new int[][] {states},
                new int[] {requiredMask}, new int[] {forbiddenMask},
                0, count, 1, backend);
        if (size() != count) {
            throw new ConcurrentModificationException("set multimap resized during selection");
        }
        M3GuavaSetMultimap<K, V> copy = new M3GuavaSetMultimap<>();
        for (int index : selected) {
            @SuppressWarnings("unchecked") K key = (K) keys[index];
            @SuppressWarnings("unchecked") V value = (V) values[index];
            copy.put(key, value);
        }
        return copy;
    }
}
