/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.ListValuedMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests real public API names for all FIVE distinct providers over existing M3 owners.
 * Distinct size semantics are explicitly tested, not normalized into an invented ABI.
 * Requires the actual Guava/Eclipse/Spring/Apache dependency jars and M3 core.
 */
final class M3FiveLibraryInteropContractTest {
    @Test void allFiveFamiliesKeepTheirOwnNamesAndMultiplicityContracts() {
        // JDK java.util Map<K,List<V>> counts keys, never flattened occurrences.
        Map<String,List<Integer>> jdk = new M3LinkedHashMap<>();
        jdk.put("k", new M3LinkedList<>());
        jdk.get("k").add(7);
        jdk.get("k").add(7);

        // Guava and Apache have pair-counting multimap contracts.
        ListMultimap<String,Integer> guava = M3LibraryCollections.guavaListMultimap();
        ListValuedMap<String,Integer> apache = M3LibraryCollections.apacheListValuedMap();
        MutableListMultimap<String,Integer> eclipse = new M3EclipseListMultimap<>();
        MultiValueMap<String,Integer> spring = M3LibraryCollections.springMultiValueMap();
        for (int i=0;i<2;i++) {
            assertTrue(guava.put("k",7));
            assertTrue(apache.put("k",7));
            assertTrue(eclipse.put("k",7));
            spring.add("k",7);
        }
        assertEquals(1,jdk.size());
        assertEquals(2,guava.size());
        assertEquals(2,apache.size());
        assertEquals(2,eclipse.size());
        assertEquals(1,spring.size());
        assertEquals(List.of(7,7), jdk.get("k"));
        assertEquals(List.of(7,7), guava.get("k"));
        assertEquals(List.of(7,7), apache.get("k"));
        assertEquals(List.of(7,7), eclipse.get("k").toList());
        assertEquals(List.of(7,7), spring.get("k"));

        // Guava live absent-key view may insert; Eclipse absent-key list is unmodifiable.
        guava.get("missing").add(1);
        assertEquals(1,guava.get("missing").size());
        assertThrows(UnsupportedOperationException.class,
                () -> eclipse.get("missing").add(1));
        assertNull(spring.get("missing"));

        assertNotSame(guava, M3LibraryCollections.<String,Integer>guavaListMultimap());
        assertNotSame(eclipse, new M3EclipseListMultimap<String,Integer>());
        assertNotSame(spring, M3LibraryCollections.<String,Integer>springMultiValueMap());
        assertNotSame(apache, M3LibraryCollections.<String,Integer>apacheListValuedMap());
    }

    /** All five actual upstream APIs compose established M3 payload/index stores. */
    @Test void additionalTypedFactoriesUseRealM3BackingsWithoutContractConflation() {
        List<String> jdk = M3LibraryCollections.jdkArrayList();
        assertInstanceOf(com.m3.collections.M3ArrayList.class, jdk);
        jdk.add("x");
        jdk.add("x");
        assertEquals(List.of("x", "x"), jdk);

        var guava = M3LibraryCollections.<String>guavaMultiset();
        assertInstanceOf(M3GuavaMultiset.class, guava);
        assertEquals(0, guava.add("x", 2));
        assertEquals(2, guava.count("x"));
        assertEquals(2, guava.size());

        var eclipseList = M3LibraryCollections.<String, Integer>eclipseListMultimap();
        assertInstanceOf(M3EclipseListMultimap.class, eclipseList);
        assertTrue(eclipseList.put("k", 7));
        assertTrue(eclipseList.put("k", 7));
        assertEquals(List.of(7, 7), eclipseList.get("k").toList());
        var eclipseSet = M3LibraryCollections.<String, Integer>eclipseSetMultimap();
        assertInstanceOf(M3EclipseSetMultimap.class, eclipseSet);
        assertTrue(eclipseSet.put("k", 7));
        assertFalse(eclipseSet.put("k", 7));
        assertEquals(1, eclipseSet.size());

        var spring = M3LibraryCollections.<String, Integer>springMultiValueMap();
        assertInstanceOf(M3SpringArrayMultiValueMap.class, spring);
        spring.add("k", 7);
        spring.add("k", 7);
        assertEquals(1, spring.size());
        assertEquals(List.of(7, 7), spring.get("k"));

        var apache = M3LibraryCollections.<String>apacheMultiSet();
        assertInstanceOf(M3ApacheMultiSet.class, apache);
        apache.add("x", 2);
        assertEquals(2, apache.getCount("x"));

        assertNotSame(guava, M3LibraryCollections.<String>guavaMultiset());
        assertNotSame(eclipseList, M3LibraryCollections.<String, Integer>eclipseListMultimap());
        assertNotSame(apache, M3LibraryCollections.<String>apacheMultiSet());
    }
}
