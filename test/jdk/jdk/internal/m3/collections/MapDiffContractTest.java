/* Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others. SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Differential proofs for the difference of two maps. */
final class MapDiffContractTest {
    /** Independent oracle: the four groups as insertion-ordered maps, built by the plain definition. */
    private record Model(Map<Object, Object> onlyLeft, Map<Object, Object> onlyRight, Map<Object, Object> common,
            Map<Object, Object> differingLeft, Map<Object, Object> differingRight) {
        static Model of(Map<Object, Object> left, Map<Object, Object> right) {
            Model model = new Model(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
            left.forEach((key, value) -> {
                if (!right.containsKey(key)) {
                    model.onlyLeft.put(key, value);
                } else if (Objects.equals(value, right.get(key))) {
                    model.common.put(key, value);
                } else {
                    model.differingLeft.put(key, value);
                    model.differingRight.put(key, right.get(key));
                }
            });
            right.forEach((key, value) -> {
                if (!left.containsKey(key)) {
                    model.onlyRight.put(key, value);
                }
            });
            return model;
        }
    }

    private static Map<Object, Object> map(SplittableRandom random, int size, int universe) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            int key = random.nextInt(universe + 1);
            map.put(key == universe ? null : key, random.nextInt(4) == 0 ? null : random.nextInt(3));
        }
        return map;
    }

    /** Same entries in the same order. */
    private static void sameOrdered(Map<Object, Object> expected, Map<Object, Object> actual) {
        assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
    }

    private static void group(Map<Object, Object> lefts, Map<Object, Object> rights, MapDiff<Object, Object> diff, int kind) {
        List<Object> keys = new ArrayList<>(lefts == null ? rights.keySet() : lefts.keySet());
        assertEquals(keys.size(), diff.count(kind));
        for (int i = 0; i < keys.size(); i++) {
            assertEquals(keys.get(i), diff.key(kind, i));
            assertEquals(lefts == null ? null : lefts.get(keys.get(i)), diff.left(kind, i));
            assertEquals(rights == null ? null : rights.get(keys.get(i)), diff.right(kind, i));
        }
    }

    private static void sameAsModel(Model model, MapDiff<Object, Object> diff) {
        sameOrdered(model.onlyLeft, diff.onlyLeft());
        sameOrdered(model.onlyRight, diff.onlyRight());
        sameOrdered(model.common, diff.common());
        sameOrdered(model.differingLeft, diff.differingLeft());
        sameOrdered(model.differingRight, diff.differingRight());
        group(model.onlyLeft, null, diff, MapDiff.ONLY_LEFT);
        group(null, model.onlyRight, diff, MapDiff.ONLY_RIGHT);
        group(model.common, model.common, diff, MapDiff.SAME);
        group(model.differingLeft, model.differingRight, diff, MapDiff.DIFFERENT);
        assertEquals(model.onlyLeft.isEmpty() && model.onlyRight.isEmpty() && model.differingLeft.isEmpty(), diff.areEqual());
    }

    @Test void randomMapsMatchTheDefinition() {
        SplittableRandom random = new SplittableRandom(20261116);
        for (int round = 0; round < 3000; round++) {
            Map<Object, Object> left = map(random, random.nextInt(30), 1 + random.nextInt(40));
            Map<Object, Object> right = map(random, random.nextInt(30), 1 + random.nextInt(40));
            sameAsModel(Model.of(left, right), MapDiff.between(left, right));
        }
    }

    @Test void equalMapsAndEmptyMapsAreEqual() {
        Map<String, Integer> left = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(MapDiff.between(left, new TreeMap<>(left)).areEqual());
        assertTrue(MapDiff.between(Map.of(), Map.of()).areEqual());
        MapDiff<String, Integer> diff = MapDiff.between(left, Map.of("a", 1));
        assertFalse(diff.areEqual());
        assertEquals(Map.of("b", 2), diff.onlyLeft());
        assertEquals(Map.of("a", 1), diff.common());
        assertThrows(UnsupportedOperationException.class, () -> diff.onlyLeft().put("x", 0));
    }

    @Test void theEquivalenceDecidesAndIsCalledOncePerSharedKey() {
        Map<String, String> left = new LinkedHashMap<>();
        left.put("a", "X");
        left.put("b", "y");
        left.put("c", "z");
        Map<String, String> right = Map.of("a", "x", "b", "Q", "d", "w");
        int[] calls = {0};
        MapDiff<String, String> diff = MapDiff.between(left, right, (l, r) -> {
            calls[0]++;
            return l.equalsIgnoreCase(r);
        });
        assertEquals(2, calls[0]);
        assertEquals(Map.of("a", "X"), diff.common());
        assertEquals(Map.of("b", "y"), diff.differingLeft());
        assertEquals(Map.of("b", "Q"), diff.differingRight());
        assertEquals(Map.of("c", "z"), diff.onlyLeft());
        assertEquals(Map.of("d", "w"), diff.onlyRight());
    }

    @Test void theDifferenceIsASnapshotAndRefusesBadArguments() {
        Map<String, Integer> left = new HashMap<>(Map.of("a", 1));
        MapDiff<String, Integer> diff = MapDiff.between(left, Map.of());
        left.put("b", 2);
        left.put("a", 9);
        assertEquals(1, diff.count(MapDiff.ONLY_LEFT));
        assertEquals(1, diff.left(MapDiff.ONLY_LEFT, 0));
        assertNull(diff.right(MapDiff.ONLY_LEFT, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> diff.key(MapDiff.ONLY_LEFT, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> diff.count(4));
        assertThrows(NullPointerException.class, () -> MapDiff.between(null, Map.of()));
        assertThrows(NullPointerException.class, () -> MapDiff.between(Map.of(), null));
        assertThrows(NullPointerException.class, () -> MapDiff.between(Map.of(), Map.of(), null));
    }
}
