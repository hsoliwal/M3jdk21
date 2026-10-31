/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.collections4.ListValuedMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.springframework.util.MultiValueMap;

/**
 * Exact, separately typed five-library list-valued collection entry points.
 *
 * <p>All factories create M3-backed payload/index owners. Each provider keeps
 * its original public API and distinct size, absent-key, iterator, mutation,
 * null and returned-view contracts. Java references never cross native JNI.
 * No global JDK or provider class replacement is performed.</p>
 */
public final class M3FiveListFamilies {
    private M3FiveListFamilies() { }

    /** One key to one caller-controlled list, with JDK distinct-key size. */
    public static <K,V> Map<K,List<V>> jdk() { return new M3LinkedHashMap<>(); }

    /** Owned direct Guava ListMultimap over M3 arrays and indexed slot links. */
    public static <K,V> ListMultimap<K,V> guava() { return new M3GuavaListMultimap<>(); }

    /** Owned Eclipse MutableListMultimap using M3-backed group/value collectors. */
    public static <K,V> MutableListMultimap<K,V> eclipse() { return new M3EclipseListMultimap<>(); }

    /** Direct Spring map-of-lists, preserving key-count and list-alias semantics. */
    public static <K,V> MultiValueMap<K,V> spring() { return new M3SpringArrayMultiValueMap<>(); }

    /** Apache ListValuedMap with the upstream API and existing M3 owners. */
    public static <K,V> ListValuedMap<K,V> apache() { return new M3ApacheListValuedMap<>(); }

    /**
     * Project arbitrary 32-bit required/excluded state words per Guava occurrence
     * into a detached M3 indexed multimap, preserving key/value encounter order.
     */
    public static <K,V> Snapshot<K,V> snapshot(M3GuavaListMultimap<K,V> source,
            int[] physicalStates, int required, int excluded, M3IndexPathMasks.Backend backend) {
        Objects.requireNonNull(source,"source");
        return new Snapshot<>(source.selectedByStates(physicalStates,required,excluded,backend));
    }

    /** M3-owned selected occurrences that can be materialized for each real provider. */
    public static final class Snapshot<K,V> {
        private final M3GuavaListMultimap<K,V> owner;
        private Snapshot(M3GuavaListMultimap<K,V> owner) { this.owner=owner; }
        public int occurrences() { return owner.size(); }
        public int distinctKeys() { return owner.keySet().size(); }

        /** Detached JDK Map-of-lists; modifications do not mutate selected owner. */
        public Map<K,List<V>> jdk() {
            Map<K,List<V>> map = new M3LinkedHashMap<>();
            for (K key : owner.keySet()) {
                M3LinkedList<V> values = new M3LinkedList<>();
                values.addAll(owner.get(key));
                map.put(key,values);
            }
            return map;
        }
        public ListMultimap<K,V> guava() {
            M3GuavaListMultimap<K,V> map = new M3GuavaListMultimap<>();
            map.putAll(owner);
            return map;
        }
        public MutableListMultimap<K,V> eclipse() {
            MutableListMultimap<K,V> map = M3FiveListFamilies.eclipse();
            owner.entries().forEach(e -> map.put(e.getKey(),e.getValue()));
            return map;
        }
        public MultiValueMap<K,V> spring() {
            MultiValueMap<K,V> map = M3FiveListFamilies.spring();
            owner.entries().forEach(e -> map.add(e.getKey(),e.getValue()));
            return map;
        }
        public ListValuedMap<K,V> apache() {
            ListValuedMap<K,V> map = M3FiveListFamilies.apache();
            owner.entries().forEach(e -> map.put(e.getKey(),e.getValue()));
            return map;
        }
    }
}
