package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        List<String> owner = new ArrayList<>(Arrays.asList("a", "b"));
        var snapshot = M3ObjectStream.snapshot(owner);
        var live = M3ObjectStream.live(owner);

        owner.add("c");

        assertEquals(List.of("a", "b"), snapshot.toBoundedList(2));
        assertEquals(List.of("a", "b", "c"), live.toBoundedList(3));
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
}
