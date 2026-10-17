/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.collections.interop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Real Eclipse Collections 11.1.0 API oracle, never a mocked provider JAR. */
final class M3EclipseMultimapsTest {
    private static <K,V> void same(Multimap<K,V> actual, Multimap<K,V> donor) {
        assertEquals(donor.size(), actual.size(), "pair count");
        assertEquals(donor.sizeDistinct(), actual.sizeDistinct(), "distinct keys");
        assertEquals(donor.isEmpty(), actual.isEmpty(), "empty");
        assertEquals(donor, actual, "Eclipse multimap equality");
        assertEquals(donor.hashCode(), actual.hashCode(), "Eclipse multimap hash");
        assertEquals(donor.toMap(), actual.toMap(), "value collection export");
        assertEquals(donor.keySet(), actual.keySet(), "distinct key domain");
    }

    private static void traceList(long seed) {
        MutableListMultimap<Integer, Integer> actual = M3EclipseMultimaps.list();
        MutableListMultimap<Integer, Integer> donor = new FastListMultimap<>();
        Random r = new Random(seed);
        for (int i = 0; i < 1700; i++) {
            Integer key = r.nextInt(11) == 0 ? null : r.nextInt(22);
            Integer value = r.nextInt(9) == 0 ? null : r.nextInt(7);
            switch (r.nextInt(6)) {
                case 0 -> assertEquals(donor.put(key, value), actual.put(key, value));
                case 1 -> assertEquals(donor.remove(key, value), actual.remove(key, value));
                case 2 -> { donor.putAll(key, List.of(value == null ? -1 : value, 3));
                            actual.putAll(key, List.of(value == null ? -1 : value, 3)); }
                case 3 -> assertEquals(donor.removeAll(key), actual.removeAll(key));
                case 4 -> assertEquals(donor.replaceValues(key, List.of(1, 1, 2)),
                        actual.replaceValues(key, List.of(1, 1, 2)));
                default -> assertEquals(donor.get(key), actual.get(key));
            }
            same(actual, donor);
        }
        same(actual.flip(), donor.flip());
        var copy = actual.toMutable();
        same(copy, actual);
        copy.put(987, 654);
        assertFalse(actual.containsKey(987), "toMutable returns detached M3-backed copy");
        assertTrue(copy instanceof M3EclipseListMultimap<?, ?>);
        assertTrue(actual.newEmpty() instanceof M3EclipseListMultimap<?, ?>);
    }

    private static void traceSet(long seed) {
        MutableSetMultimap<Integer, Integer> actual = M3EclipseMultimaps.set();
        MutableSetMultimap<Integer, Integer> donor = new UnifiedSetMultimap<>();
        Random r = new Random(seed);
        for (int i = 0; i < 1700; i++) {
            Integer key = r.nextInt(11) == 0 ? null : r.nextInt(22);
            Integer value = r.nextInt(9) == 0 ? null : r.nextInt(7);
            switch (r.nextInt(6)) {
                case 0 -> assertEquals(donor.put(key, value), actual.put(key, value));
                case 1 -> assertEquals(donor.remove(key, value), actual.remove(key, value));
                case 2 -> { donor.putAll(key, List.of(1, 1, 2));
                            actual.putAll(key, List.of(1, 1, 2)); }
                case 3 -> assertEquals(donor.removeAll(key), actual.removeAll(key));
                case 4 -> assertEquals(donor.replaceValues(key, List.of(1, 1, 2)),
                        actual.replaceValues(key, List.of(1, 1, 2)));
                default -> assertEquals(donor.get(key), actual.get(key));
            }
            same(actual, donor);
        }
        same(actual.flip(), donor.flip());
        var copy = actual.toMutable();
        same(copy, actual);
        copy.put(987, 654);
        assertFalse(actual.containsKey(987));
        assertTrue(copy instanceof M3EclipseSetMultimap<?, ?>);
        assertTrue(actual.newEmpty() instanceof M3EclipseSetMultimap<?, ?>);
    }

    private static void traceBag(long seed) {
        MutableBagMultimap<Integer, Integer> actual = M3EclipseMultimaps.bag();
        MutableBagMultimap<Integer, Integer> donor = new HashBagMultimap<>();
        Random r = new Random(seed);
        for (int i = 0; i < 1700; i++) {
            Integer key = r.nextInt(11) == 0 ? null : r.nextInt(22);
            Integer value = r.nextInt(9) == 0 ? null : r.nextInt(7);
            switch (r.nextInt(6)) {
                case 0 -> assertEquals(donor.put(key, value), actual.put(key, value));
                case 1 -> assertEquals(donor.remove(key, value), actual.remove(key, value));
                case 2 -> { donor.putAll(key, List.of(1, 1, 2));
                            actual.putAll(key, List.of(1, 1, 2)); }
                case 3 -> assertEquals(donor.removeAll(key), actual.removeAll(key));
                case 4 -> assertEquals(donor.replaceValues(key, List.of(1, 1, 2)),
                        actual.replaceValues(key, List.of(1, 1, 2)));
                default -> assertEquals(donor.get(key), actual.get(key));
            }
            same(actual, donor);
        }
        same(actual.flip(), donor.flip());
        var copy = actual.toMutable();
        same(copy, actual);
        copy.put(987, 654);
        assertFalse(actual.containsKey(987));
        assertTrue(copy instanceof M3EclipseBagMultimap<?, ?>);
        assertTrue(actual.newEmpty() instanceof M3EclipseBagMultimap<?, ?>);
    }

    @Test void listMultimapDifferential() { traceList(0xA617301L); }
    @Test void setMultimapDifferential() { traceSet(0xA617302L); }
    @Test void bagMultimapDifferential() { traceBag(0xA617303L); }

    @Test void factoriesAreRealEclipseAndIndependentM3Owners() {
        var a = M3EclipseMultimaps.<String, Integer>list();
        var b = M3EclipseMultimaps.<String, Integer>list();
        a.put("a", 1);
        a.put("a", 1);
        assertEquals(2, a.size());
        assertEquals(0, b.size());
        assertEquals(2, a.get("a").size());
        assertEquals(1, a.sizeDistinct());
        var distinct = M3EclipseMultimaps.<String, Integer>set();
        assertTrue(distinct.put("a", 1));
        assertFalse(distinct.put("a", 1));
        assertEquals(1, distinct.size());
        var bag = M3EclipseMultimaps.<String, Integer>bag();
        bag.put("a", 1);
        bag.put("a", 1);
        assertEquals(2, bag.size());
        assertEquals(2, bag.get("a").occurrencesOf(1));
    }

    @Test void composableEclipseInversesUseM3Owners() {
        var list = M3EclipseMultimaps.<String, Integer>list();
        list.put("a", 7);
        list.put("a", 7);
        list.put("b", 7);
        var invertedList = list.flip();
        assertInstanceOf(M3EclipseBagMultimap.class, invertedList);
        assertEquals(3, invertedList.size());
        assertEquals(2, invertedList.get(7).occurrencesOf("a"));

        var set = M3EclipseMultimaps.<String, Integer>set();
        set.put("a", 7);
        set.put("b", 7);
        var invertedSet = set.flip();
        assertInstanceOf(M3EclipseSetMultimap.class, invertedSet);
        assertEquals(2, invertedSet.size());

        var bag = M3EclipseMultimaps.<String, Integer>bag();
        bag.put("a", 7);
        bag.put("a", 7);
        var invertedBag = bag.flip();
        assertInstanceOf(M3EclipseBagMultimap.class, invertedBag);
        assertEquals(2, invertedBag.get(7).occurrencesOf("a"));
    }

    @Test void noEclipseConcreteClassNameSpoofing() {
        assertEquals("com.m3.collections.interop.M3EclipseListMultimap",
                M3EclipseMultimaps.list().getClass().getName());
        assertEquals("com.m3.collections.interop.M3EclipseSetMultimap",
                M3EclipseMultimaps.set().getClass().getName());
        assertEquals("com.m3.collections.interop.M3EclipseBagMultimap",
                M3EclipseMultimaps.bag().getClass().getName());
    }

    @Test void fiveProviderMultivalueContractsShareM3OwnersButNotSemantics() {
        java.util.Map<String, java.util.List<Integer>> jdk = new com.m3.collections.M3LinkedHashMap<>();
        jdk.computeIfAbsent("k", k -> new com.m3.collections.M3LinkedList<>()).add(1);
        jdk.get("k").add(1);
        var eclipse = M3EclipseMultimaps.<String, Integer>list();
        var guava = M3LibraryCollections.<String, Integer>guavaListMultimap();
        var apache = M3LibraryCollections.<String, Integer>apacheListValuedMap();
        var spring = M3LibraryCollections.<String, Integer>springMultiValueMap();
        for (int i = 0; i < 2; i++) {
            eclipse.put("k", 1);
            guava.put("k", 1);
            apache.put("k", 1);
            spring.add("k", 1);
        }
        assertEquals(java.util.List.of(1, 1), jdk.get("k"));
        assertEquals(java.util.List.of(1, 1), eclipse.get("k"));
        assertEquals(java.util.List.of(1, 1), guava.get("k"));
        assertEquals(java.util.List.of(1, 1), new java.util.ArrayList<>(apache.get("k")));
        assertEquals(java.util.List.of(1, 1), spring.get("k"));
        assertEquals(1, jdk.size());
        assertEquals(2, eclipse.size());
        assertEquals(2, guava.size());
        assertEquals(2, apache.size());
        assertEquals(1, spring.size());
        assertThrows(UnsupportedOperationException.class, () -> eclipse.get("k").add(4));
        guava.get("k").add(4);
        apache.get("k").add(4);
        spring.get("k").add(4);
        assertEquals(3, guava.size());
        assertEquals(3, apache.size());
        assertEquals(1, spring.size());
        assertEquals(3, spring.get("k").size());
    }

}
