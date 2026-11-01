/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.google.common.collect.Multiset;
import com.m3.collections.M3IndexPathMasks;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import com.m3.collections.M3ObjectCounts;
import com.m3.collections.M3TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import org.apache.commons.collections4.MultiSet;
import org.eclipse.collections.api.bag.MutableBag;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

/** Five actual SDK contracts on primitive M3 index/flag snapshots. */
final class M3FiveProviderFlagSelectionTest {
    private static final M3IndexPathMasks.Backend JAVA = M3IndexPathMasks.Backend.JAVA;

    @Test void selectedCountsAreM3OwnedForAllFiveRealProviders() {
        M3ObjectCounts<String> source = new M3ObjectCounts<>();
        source.add("alpha", 3);
        source.setFlags("alpha", Integer.MIN_VALUE | 1);
        source.add(null, 2);
        source.setFlags(null, Integer.MIN_VALUE | 1);
        source.add("beta", 4);
        source.setFlags("beta", 4);
        source.add("ALPHA", 1);
        source.setFlags("ALPHA", Integer.MIN_VALUE | 1);

        M3FiveProviderFlagSelection<String> selected =
                M3FiveProviderFlagSelection.capture(source, Integer.MIN_VALUE, 4, JAVA);
        assertEquals(3, selected.distinctSize());
        assertEquals(6L, selected.totalCount());
        assertEquals(Integer.MIN_VALUE | 1, selected.flags(null));
        Map<String, Integer> jdk = selected.jdkCounts();
        assertInstanceOf(M3LinkedHashMap.class, jdk);
        assertEquals(Arrays.asList("alpha", null, "ALPHA"), new ArrayList<>(jdk.keySet()));
        assertEquals(Map.of("alpha", 3, "ALPHA", 1).size() + 1, jdk.size());
        assertEquals(2, jdk.get(null));

        NavigableMap<String, Integer> sorted = selected.jdkSortedCounts(
                Comparator.nullsFirst(Comparator.naturalOrder()));
        assertInstanceOf(M3TreeMap.class, sorted);
        assertEquals(Arrays.asList(null, "ALPHA", "alpha"), new ArrayList<>(sorted.keySet()));
        assertThrows(IllegalArgumentException.class,
                () -> selected.jdkSortedCounts(
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        Multiset<String> guava = selected.guavaMultiset();
        MutableBag<String> eclipse = selected.eclipseBag();
        MultiSet<String> apache = selected.apacheMultiSet();
        assertInstanceOf(M3GuavaMultiset.class, guava);
        assertInstanceOf(M3EclipseBag.class, eclipse);
        assertInstanceOf(M3ApacheMultiSet.class, apache);
        assertEquals(3, guava.count("alpha"));
        assertEquals(3, eclipse.occurrencesOf("alpha"));
        assertEquals(3, apache.getCount("alpha"));
        assertEquals(2, guava.count(null));

        guava.add("alpha", 2);
        assertEquals(5, eclipse.occurrencesOf("alpha"));
        assertEquals(5, apache.getCount("alpha"));
        assertEquals(6L + 2L, selected.totalCount());
        assertEquals(3, source.count("alpha"));
        assertEquals(3, jdk.get("alpha"));
        assertEquals(3, sorted.get("alpha"));

        MultiValueMap<String, Integer> spring = selected.springCountLists();
        assertInstanceOf(M3SpringArrayMultiValueMap.class, spring);
        assertEquals(List.of(5), spring.get("alpha"));
        assertEquals(List.of(2), spring.get(null));
        assertInstanceOf(M3LinkedList.class, spring.get("alpha"));
        eclipse.addOccurrences("alpha", 1);
        assertEquals(6, apache.getCount("alpha"));
        assertEquals(List.of(5), spring.get("alpha"));
        assertEquals(3, source.count("alpha"));

        // Distinct output classes and their equality/size contracts stay separate.
        assertEquals(9, guava.size());
        assertEquals(9, eclipse.size());
        assertEquals(9, apache.size());
        assertEquals(3, spring.size());
        assertEquals(3, jdk.size());
        source.verifyInvariants();
    }

    @Test void actualDirectSpringSdkFactoryKeepsLiveMapAndListAliasing() {
        MultiValueMap<String, Integer> spring = M3FiveProviderDirectSdk.springMultiValueMap();
        assertInstanceOf(M3SpringArrayMultiValueMap.class, spring);
        spring.add("k", 1);
        spring.add("k", 2);
        assertEquals(List.of(1, 2), spring.get("k"));
        assertInstanceOf(M3LinkedList.class, spring.get("k"));
        List<Integer> supplied = new ArrayList<>(Arrays.asList(5, null));
        spring.put("borrowed", supplied);
        assertSame(supplied, spring.get("borrowed"));
        supplied.add(7);
        assertEquals(Arrays.asList(5, null, 7), spring.get("borrowed"));
        assertInstanceOf(M3LinkedHashMap.class, spring.toSingleValueMap());
    }

    @Test void seededFull32BitFlagAndDuplicateCountOracle() {
        if (Boolean.getBoolean("m3.interop.requireNative")) {
            assertTrue(M3IndexPathMasks.nativeAvailable(), "required C17 JNI backend must load");
        }
        Random random = new Random(0x4d3305F17L);
        M3ObjectCounts<Integer> source = new M3ObjectCounts<>();
        LinkedHashMap<Integer, Integer> flags = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < 2400; i++) {
            Integer value = i % 113 == 0 ? null : random.nextInt(50);
            if (!counts.containsKey(value)) {
                source.add(value, 1);
                counts.put(value, 1);
                flags.put(value, 0);
            } else if (counts.get(value) < 100) {
                source.add(value, 1);
                counts.put(value, counts.get(value) + 1);
            }
            if (i % 3 == 0) {
                int flag = random.nextInt();
                source.setFlags(value, flag);
                flags.put(value, flag);
            }
            if (i % 31 != 0) continue;
            int required = random.nextInt() & 0x80004001;
            int excluded = random.nextInt() & 0x20000004;
            M3FiveProviderFlagSelection<Integer> selection =
                    M3FiveProviderFlagSelection.capture(source, required, excluded, JAVA);
            Map<Integer, Integer> reference = new LinkedHashMap<>();
            flags.forEach((key, state) -> {
                if ((state & required) == required && (state & excluded) == 0) {
                    reference.put(key, counts.get(key));
                }
            });
            assertEquals(reference, selection.jdkCounts());
            assertEquals(new ArrayList<>(reference.keySet()),
                    new ArrayList<>(selection.jdkCounts().keySet()));
            assertEquals(reference.size(), selection.distinctSize());
            Multiset<Integer> guava = selection.guavaMultiset();
            MultiSet<Integer> apache = selection.apacheMultiSet();
            MutableBag<Integer> eclipse = selection.eclipseBag();
            MultiValueMap<Integer, Integer> spring = selection.springCountLists();
            reference.forEach((key, count) -> {
                assertEquals(count.intValue(), guava.count(key));
                assertEquals(count.intValue(), apache.getCount(key));
                assertEquals(count.intValue(), eclipse.occurrencesOf(key));
                assertEquals(List.of(count), spring.get(key));
            });
            if (M3IndexPathMasks.nativeAvailable()) {
                var nativeSnapshot = M3FiveProviderFlagSelection.capture(source, required,
                        excluded, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                assertEquals(reference, nativeSnapshot.jdkCounts());
            }
        }
    }

    @Test void emptyOverlapAndCallerMutationStayIndependent() {
        M3ObjectCounts<String> source = new M3ObjectCounts<>();
        source.add("x", 2);
        source.setFlags("x", Integer.MIN_VALUE);
        var none = M3FiveProviderFlagSelection.capture(source, 1, 1, JAVA);
        assertTrue(none.jdkCounts().isEmpty());
        assertTrue(none.guavaMultiset().isEmpty());
        assertTrue(none.eclipseBag().isEmpty());
        assertTrue(none.apacheMultiSet().isEmpty());
        assertTrue(none.springCountLists().isEmpty());
        var positive = M3FiveProviderFlagSelection.capture(source, Integer.MIN_VALUE, 0, JAVA);
        source.remove("x", 2);
        assertEquals(2, positive.guavaMultiset().count("x"));
        assertEquals(2, positive.springCountLists().getFirst("x"));
        assertThrows(NullPointerException.class,
                () -> M3FiveProviderFlagSelection.capture(null, 0, 0, JAVA));
        assertThrows(NullPointerException.class,
                () -> M3FiveProviderFlagSelection.capture(source, 0, 0, null));
    }
}
