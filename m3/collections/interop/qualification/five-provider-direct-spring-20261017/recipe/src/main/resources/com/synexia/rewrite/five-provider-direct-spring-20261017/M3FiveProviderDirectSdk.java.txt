/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multiset;
import com.m3.collections.M3LinkedHashMap;
import java.util.SequencedMap;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiSet;
import org.eclipse.collections.api.bag.MutableBag;
import org.springframework.util.MultiValueMap;

/**
 * Opt-in SDK entrances with correct provider interfaces and M3 physical storage.
 *
 * <p>Every method creates its own direct M3 object/index/count owner, never
 * a renamed upstream class. Five public provider namespaces remain separate:
 * JDK java.util, Guava, Eclipse Collections, Apache Commons, Spring.
 *
 * <p>The existing M3LibraryCollections APIs remain unchanged; in particular
 * its historical Spring adapter is preserved for current clients. This new
 * Spring factory selects the directly implemented M3SpringArrayMultiValueMap,
 * not Spring's MultiValueMapAdapter. These are explicit provider-specific
 * implementations, not an assertion that all upstream APIs are implemented.
 */
public final class M3FiveProviderDirectSdk {
    private M3FiveProviderDirectSdk() { }

    /** JDK21 encounter-ordered Map contract with M3 parallel key/value lanes. */
    public static <K, V> SequencedMap<K, V> jdkSequencedMap() {
        return new M3LinkedHashMap<>();
    }

    /** Guava one-to-one BiMap with two existing M3 indexed map owners. */
    public static <K, V> BiMap<K, V> guavaBiMap() {
        return new M3GuavaBiMap<>();
    }

    /** Guava mutable Multiset sharing an M3 counted-slot substrate. */
    public static <E> Multiset<E> guavaMultiset() {
        return new M3GuavaMultiset<>();
    }

    /** Eclipse Collections MutableBag with parallel primitive count/state lanes. */
    public static <E> MutableBag<E> eclipseBag() {
        return new M3EclipseBag<>();
    }

    /** Apache Commons MultiSet, retaining Apache count/entry/view contracts. */
    public static <E> MultiSet<E> apacheMultiSet() {
        return new M3ApacheMultiSet<>();
    }

    /** Apache Commons BidiMap: Apache's displacement, not Guava forcePut semantics. */
    public static <K, V> BidiMap<K, V> apacheBidiMap() {
        return new M3ApacheBidiMap<>();
    }

    /** Spring MultiValueMap implemented directly on M3 map/list references. */
    public static <K, V> MultiValueMap<K, V> springMultiValueMap() {
        return new M3SpringArrayMultiValueMap<>();
    }
}
