/* Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others. SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the bag difference of two collections. */
final class CollectionDiffContractTest {
    /** An element whose hash keeps only a few bits, so probe runs collide and wrap. */
    private record Crowded(int id, int bits) {
        @Override public int hashCode() { return id & bits; }
    }

    /** Independent oracle: element to {left count, right count}, in first-seen order. */
    private static Map<Object, int[]> model(List<Object> left, List<Object> right) {
        Map<Object, int[]> counts = new LinkedHashMap<>();
        for (Object element : left) {
            counts.computeIfAbsent(element, unused -> new int[2])[0]++;
        }
        for (Object element : right) {
            counts.computeIfAbsent(element, unused -> new int[2])[1]++;
        }
        return counts;
    }

    private static List<Object> bag(SplittableRandom random, int size, int universe, int bits) {
        List<Object> bag = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int id = random.nextInt(universe + 1);
            bag.add(id == universe ? null : bits < 0 ? (Object) id : new Crowded(id, bits));
        }
        return bag;
    }

    /** Each element {@code times.applyAsInt(left, right)} times, in the model's order. */
    private static List<Object> expanded(Map<Object, int[]> model, java.util.function.IntBinaryOperator times) {
        List<Object> list = new ArrayList<>();
        model.forEach((element, count) -> {
            for (int i = times.applyAsInt(count[0], count[1]); i > 0; i--) {
                list.add(element);
            }
        });
        return list;
    }

    private static void sameAsModel(Map<Object, int[]> model, CollectionDiff<Object> diff) {
        assertEquals(model.size(), diff.distinct());
        int slot = 0;
        for (Map.Entry<Object, int[]> entry : model.entrySet()) {
            assertEquals(entry.getKey(), diff.element(slot), "first-seen order at " + slot);
            assertEquals(entry.getValue()[0], diff.leftCount(slot));
            assertEquals(entry.getValue()[1], diff.rightCount(slot));
            assertEquals(entry.getValue()[1] - entry.getValue()[0], diff.delta(slot++));
        }
        assertEquals(expanded(model, (l, r) -> l - r), diff.onlyLeft());
        assertEquals(expanded(model, (l, r) -> r - l), diff.onlyRight());
        assertEquals(expanded(model, Math::min), diff.common());
        assertEquals(diff.onlyLeft().size(), diff.removed());
        assertEquals(diff.onlyRight().size(), diff.added());
        assertEquals(diff.removed() == 0 && diff.added() == 0, diff.isEmpty());
    }

    private static void lanesAgree(CollectionDiff<?> diff) throws Exception {
        Field lane = CollectionDiff.class.getDeclaredField("indexLane");
        lane.setAccessible(true);
        int[] index = (int[]) lane.get(diff);
        assertEquals(diff.distinct(), Arrays.stream(index).filter(entry -> entry != 0).count(), "one index entry per element");
        assertTrue(2 * diff.distinct() <= index.length, "index lane at most half full");
    }

    private static void rounds(SplittableRandom random, int bits) throws Exception {
        for (int round = 0; round < 600; round++) {
            List<Object> left = bag(random, random.nextInt(40), 12, bits);
            List<Object> right = bag(random, random.nextInt(300), 12 + random.nextInt(200), bits);
            CollectionDiff<Object> diff = CollectionDiff.between(left, right);
            sameAsModel(model(left, right), diff);
            lanesAgree(diff);
        }
    }

    @Test void randomBagsMatchTheCountModel() throws Exception {
        SplittableRandom random = new SplittableRandom(20261116);
        for (int bits : new int[] {-1, 0, 3, 31}) {
            rounds(random, bits);
        }
    }

    @Test void equalBagsInAnyOrderAreAnEmptyDifference() {
        CollectionDiff<String> diff = CollectionDiff.between(List.of("a", "b", "a", "c"), List.of("c", "a", "b", "a"));
        assertTrue(diff.isEmpty());
        assertEquals(0, diff.added());
        assertEquals(0, diff.removed());
        assertEquals(List.of("a", "a", "b", "c"), diff.common());
        assertTrue(CollectionDiff.between(List.of(), List.of()).isEmpty());
    }

    @Test void addedAndRemovedOccurrencesAreCounted() {
        CollectionDiff<String> diff = CollectionDiff.between(Arrays.asList("a", "a", "b", null), Arrays.asList("a", "c", "c", null, null));
        assertEquals(List.of("a", "b"), diff.onlyLeft());
        assertEquals(Arrays.asList(null, "c", "c"), diff.onlyRight());
        assertEquals(Arrays.asList("a", null), diff.common());
        assertEquals(2, diff.removed());
        assertEquals(3, diff.added());
        assertEquals(-1, diff.delta(0));
        assertEquals(2, diff.delta(3));
        assertFalse(diff.isEmpty());
    }

    @Test void theDifferenceIsASnapshotAndRefusesBadArguments() {
        List<String> left = new ArrayList<>(List.of("a"));
        CollectionDiff<String> diff = CollectionDiff.between(left, List.of("b"));
        left.add("z");
        assertEquals(2, diff.distinct());
        assertThrows(IndexOutOfBoundsException.class, () -> diff.element(2));
        assertThrows(IndexOutOfBoundsException.class, () -> diff.leftCount(-1));
        assertThrows(NullPointerException.class, () -> CollectionDiff.between(null, List.of()));
        assertThrows(NullPointerException.class, () -> CollectionDiff.between(List.of(), null));
    }
}
