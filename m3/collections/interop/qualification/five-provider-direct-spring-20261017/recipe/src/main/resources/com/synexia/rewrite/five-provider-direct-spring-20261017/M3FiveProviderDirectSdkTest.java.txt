/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.Multiset;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3ObjectCounts;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MultiSet;
import org.eclipse.collections.api.bag.MutableBag;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

/** Real five-provider contracts against their corresponding M3 owner classes. */
final class M3FiveProviderDirectSdkTest {

    @Test void jdk21SequencedMapOverCanonicalM3Index() {
        SequencedMap<String, Integer> map = M3FiveProviderDirectSdk.jdkSequencedMap();
        assertInstanceOf(M3LinkedHashMap.class, map);
        map.put("a", 1); map.put("b", 2); map.putFirst("b", 3);
        assertEquals("b", map.firstEntry().getKey());
        assertEquals(List.of("b", "a"), List.copyOf(map.sequencedKeySet()));
        map.reversed().putFirst("c", 4);
        assertEquals("c", map.lastEntry().getKey());
    }

    @Test void guavaBiMapMaintainsLiveInverse() {
        BiMap<String, Integer> map = M3FiveProviderDirectSdk.guavaBiMap();
        map.put("a", 1); map.put("b", 2);
        assertEquals("a", map.inverse().get(1));
        assertSame(map, map.inverse().inverse());
        assertThrows(IllegalArgumentException.class, () -> map.put("c", 2));
        map.forcePut("c", 2);
        assertFalse(map.containsKey("b"));
        assertEquals("c", map.inverse().get(2));
    }

    @Test void guavaMultisetIsOccurrenceCounted() {
        Multiset<String> bag = M3FiveProviderDirectSdk.guavaMultiset();
        assertEquals(0, bag.add("x", 2));
        assertEquals(2, bag.count("x"));
        assertEquals(2, bag.size());
        assertEquals(2, bag.remove("x", 1));
        assertEquals(1, bag.count("x"));
    }

    @Test void eclipseMutableBagUsesM3Counts() {
        MutableBag<String> bag = M3FiveProviderDirectSdk.eclipseBag();
        bag.addOccurrences("x", 3);
        assertEquals(3, bag.occurrencesOf("x"));
        assertEquals(3, bag.size());
        bag.removeOccurrences("x", 2);
        assertEquals(1, bag.occurrencesOf("x"));
    }

    @Test void apacheMultiSetHasIndependentContract() {
        MultiSet<String> bag = M3FiveProviderDirectSdk.apacheMultiSet();
        assertEquals(0, bag.add("x", 3));
        assertEquals(3, bag.getCount("x"));
        assertEquals(3, bag.size());
        bag.remove("x", 1);
        assertEquals(2, bag.getCount("x"));
    }

    @Test void apacheBidiMapHasNativeApacheValueDisplacement() {
        BidiMap<String, Integer> map = M3FiveProviderDirectSdk.apacheBidiMap();
        map.put("a", 1); map.put("b", 2); map.put("c", 2);
        assertFalse(map.containsKey("b"));
        assertEquals("c", map.getKey(2));
        assertEquals("a", map.inverseBidiMap().get(1));
    }

    @Test void springDirectMapDoesNotInheritSpringAdapter() {
        MultiValueMap<String, Integer> map = M3FiveProviderDirectSdk.springMultiValueMap();
        assertInstanceOf(M3SpringArrayMultiValueMap.class, map);
        map.add("k", 1); map.add("k", 2);
        assertEquals(1, map.size());
        assertEquals(List.of(1, 2), map.get("k"));
        Map<String, Integer> snapshot = map.toSingleValueMap();
        Map<String, Integer> live = map.asSingleValueMap();
        map.set("k", 3);
        assertEquals(Integer.valueOf(1), snapshot.get("k"));
        assertEquals(Integer.valueOf(3), live.get("k"));
    }

    @Test void threeCountProvidersMayShareOneCanonicalOwner() {
        M3ObjectCounts<String> source = new M3ObjectCounts<>();
        Multiset<String> guava = new M3GuavaMultiset<>(source);
        MutableBag<String> eclipse = new M3EclipseBag<>(source);
        MultiSet<String> apache = new M3ApacheMultiSet<>(source);
        guava.add("same", 2);
        assertEquals(2, eclipse.occurrencesOf("same"));
        assertEquals(2, apache.getCount("same"));
        eclipse.addOccurrences("same", 1);
        assertEquals(3, guava.count("same"));
        assertEquals(3, apache.getCount("same"));
        assertEquals(Set.of("same"), guava.elementSet());
    }
}
