/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ArrayListMultimap;
import com.m3.collections.M3ObjectStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

class M3ObjectStreamProviderContractTest {
    @Test
    void providerCollectionViewsShareTheM3StreamContract() {
        ArrayListMultimap<String, String> guava = ArrayListMultimap.create();
        guava.put("k", "g1");
        guava.put("k", "g2");
        assertEquals(List.of("g1", "g2"),
                M3ObjectStream.snapshot(guava.get("k")).toBoundedList(2));

        MutableList<String> eclipse = Lists.mutable.of("e1", "e2", "e1");
        assertEquals(List.of("e1", "e2", "e1"),
                M3ObjectStream.snapshot(eclipse).toBoundedList(3));

        LinkedMultiValueMap<String, String> spring = new LinkedMultiValueMap<>();
        spring.add("k", "s1");
        spring.add("k", "s2");
        assertEquals(List.of("s1", "s2"),
                M3ObjectStream.snapshot(spring.get("k")).toBoundedList(2));

        List<String> commons = ListUtils.predicatedList(new ArrayList<>(), value -> value != null);
        commons.add("c1");
        commons.add("c2");
        assertEquals(List.of("c1", "c2"), M3ObjectStream.snapshot(commons).toBoundedList(2));
    }

    @Test
    void providerSnapshotsAreDetachedButLiveSourcesRemainLive() {
        ArrayListMultimap<String, String> guava = ArrayListMultimap.create();
        guava.put("k", "g1");
        List<String> guavaView = guava.get("k");
        var guavaSnapshot = M3ObjectStream.snapshot(guavaView);
        var guavaLive = M3ObjectStream.live(guavaView);
        guava.put("k", "g2");
        assertEquals(List.of("g1"), guavaSnapshot.toBoundedList(1));
        assertEquals(List.of("g1", "g2"), guavaLive.toBoundedList(2));

        MutableList<String> eclipse = Lists.mutable.of("e1");
        var eclipseSnapshot = M3ObjectStream.snapshot(eclipse);
        eclipse.add("e2");
        // RichIterable is intentionally adapted through Iterable snapshot mode. Do not
        // claim replayable live semantics for arbitrary Iterable implementations.
        assertEquals(List.of("e1"), eclipseSnapshot.toBoundedList(1));
        assertEquals(List.of("e1", "e2"), new ArrayList<>(eclipse));

        LinkedMultiValueMap<String, String> spring = new LinkedMultiValueMap<>();
        spring.add("k", "s1");
        List<String> springView = spring.get("k");
        var springSnapshot = M3ObjectStream.snapshot(springView);
        var springLive = M3ObjectStream.live(springView);
        spring.add("k", "s2");
        assertEquals(List.of("s1"), springSnapshot.toBoundedList(1));
        assertEquals(List.of("s1", "s2"), springLive.toBoundedList(2));

        List<String> backing = new ArrayList<>();
        List<String> commons = ListUtils.predicatedList(backing, value -> value != null);
        commons.add("c1");
        var commonsSnapshot = M3ObjectStream.snapshot(commons);
        var commonsLive = M3ObjectStream.live(commons);
        assertThrows(IllegalArgumentException.class, () -> commons.add(null));
        commons.add("c2");
        assertEquals(List.of("c1"), commonsSnapshot.toBoundedList(1));
        assertEquals(List.of("c1", "c2"), commonsLive.toBoundedList(2));
    }

    @Test
    void indexMaskSelectionPreservesProviderEncounterOrderAndDuplicates() {
        List<String> values = List.of("reject", "x", "x", "reject", "y");
        var selected = M3ObjectStream.snapshot(values)
                .indexedFiltered(value -> !value.equals("reject"), values.size());
        assertEquals(List.of("x", "x", "y"), new ArrayList<>(selected));
        assertThrows(IllegalStateException.class,
                () -> M3ObjectStream.snapshot(values).toBoundedList(values.size() - 1));
    }

    @Test
    void boundedOperatorsComposeOverProviderSnapshots() {
        List<String> values = List.of("z", "a", "z", "b", "c");
        assertEquals(List.of("A", "B", "C"),
                M3ObjectStream.snapshot(values)
                        .distinct(4)
                        .sorted(4)
                        .map(String::toUpperCase)
                        .limit(3)
                        .toBoundedList(3));

        assertEquals(List.of(List.of("a", "b"), List.of("c", "d")),
                M3ObjectStream.snapshot(List.of("a", "b", "c", "d", "e"))
                        .batch(2, M3ObjectStream.TailPolicy.DROP)
                        .toBoundedList(2));

        assertEquals(List.of(List.of("a", "b"), List.of("b", "c"), List.of("c", "d")),
                M3ObjectStream.snapshot(List.of("a", "b", "c", "d"))
                        .window(2)
                        .toBoundedList(3));

        assertEquals(List.of("a", "A", "bb", "BB"),
                M3ObjectStream.snapshot(List.of("a", "bb"))
                        .mapMulti((value, emit) -> {
                            emit.accept(value);
                            emit.accept(value.toUpperCase());
                        })
                        .toBoundedList(4));
    }

    @Test
    void replayAndTerminalOperatorsRemainExplicitAndBounded() {
        var once = M3ObjectStream.once(List.of(2, 3, 4).stream());
        assertEquals(9, once.reduce(0, Integer::sum));
        assertThrows(IllegalStateException.class, once::count);

        var mark = M3ObjectStream.once(List.of("m1", "m2", "m3").stream()).mark(3);
        assertTrue(mark.size() <= 3);
        assertEquals(List.of("m1", "m2", "m3"), mark.reprocess().toBoundedList(3));

        assertEquals(10, M3ObjectStream.snapshot(List.of(1, 2, 3, 4))
                .fold(() -> 0, Integer::sum));
        assertEquals(4, M3ObjectStream.snapshot(List.of(1, 2, 3, 4)).count());
    }
}
