/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Product-owner Java 21 contract for orientation-only indexed reversal. */
final class M3IndexedReversedCoordinates20261019Test {
    private record Item(int score, String id) {}

    private static final Item[] INPUT = {
        new Item(3, "a"), new Item(1, "b"), new Item(2, "c"),
        new Item(1, "d"), new Item(2, "e"), new Item(1, "f")
    };

    private static M3IndexedListSnapshot<Item> sample() {
        return M3IndexedListSnapshot.copyOf(INPUT);
    }

    @Test void reverseRetainsTheCanonicalOwnerAndCoordinatePlane() throws Exception {
        M3IndexedListSnapshot<Item> base = sample();
        M3IndexedListSnapshot<Item> reverse = base.reversed();
        assertSame(field(base, "values"), field(reverse, "values"));
        assertSame(field(base, "positions"), field(reverse, "positions"));
        assertArrayEquals(new int[] {5, 4, 3, 2, 1, 0}, reverse.physicalIndices());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5},
                reverse.reversed().physicalIndices());
        assertArrayEquals(base.toArray(), reverse.reversed().toArray());
        assertNotSame(base.toArray(), reverse.toArray());
        assertEquals(INPUT[5], reverse.getFirst());
        assertEquals(INPUT[0], reverse.getLast());
        assertSame(INPUT[5], reverse.get(0));
        assertTrue(reverse.includesPhysicalIndex(0));
        assertTrue(reverse.includesPhysicalIndex(5));
        assertFalse(reverse.includesPhysicalIndex(6));
        assertEquals(6, reverse.physicalMaskCopy().cardinality());
    }

    @Test void nestedSlicesUseOppositeHalfOpenOffsetsWithoutCopyingIndexes()
            throws Exception {
        M3IndexedListSnapshot<Item> base = sample();
        M3IndexedListSnapshot<Item> view = base.reversed().subList(1, 5);
        assertSame(field(base, "positions"), field(view, "positions"));
        assertArrayEquals(new int[] {4, 3, 2, 1}, view.physicalIndices());
        assertArrayEquals(new int[] {2, 3}, view.slice(1, 2).physicalIndices());
        assertArrayEquals(new int[] {1, 2, 3, 4},
                view.reversed().physicalIndices());
        assertEquals(List.of(INPUT[4], INPUT[3], INPUT[2], INPUT[1]), view);
        assertFalse(view.includesPhysicalIndex(5));
        assertTrue(view.includesPhysicalIndex(3));
        assertEquals(4, view.physicalMaskCopy().cardinality());
        assertArrayEquals(new int[0], view.subList(0, 0).reversed().physicalIndices());
    }

    @Test void predicateRunsOncePerLogicalOccurrenceInReverseOrder() {
        M3IndexedListSnapshot<Item> view = sample().reversed();
        List<String> visited = new ArrayList<>();
        M3IndexedListSnapshot<Item> filtered = view.filtered(item -> {
            visited.add(item.id());
            return (item.score() & 1) != 0;
        });
        assertEquals(List.of("f", "e", "d", "c", "b", "a"), visited);
        assertArrayEquals(new int[] {5, 3, 1, 0}, filtered.physicalIndices());
        assertArrayEquals(new int[] {5, 3, 1, 4, 2, 0},
                view.sorted(Comparator.comparingInt(Item::score)).physicalIndices());
        assertArrayEquals(new int[] {5, 3, 1, 0},
                view.filteredSorted(item -> (item.score() & 1) != 0,
                        Comparator.comparingInt(Item::score)).physicalIndices());
        assertArrayEquals(new int[] {5, 3, 1, 0},
                filtered.physicalIndices());
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5}, sample().physicalIndices());
    }

    @Test void fullWidthMultiPlaneMasksKeepReversedPhysicalOccurrenceOrder() {
        M3IndexedListSnapshot<Item> reversed = sample().reversed();
        int[] highBit = {
            Integer.MIN_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, 0, Integer.MIN_VALUE
        };
        int[] second = {1, 1, 0, 1, 1, 1};
        int[][] states = {highBit, second};
        int[] required = {Integer.MIN_VALUE, 1};
        int[] forbidden = {0, 0};
        M3IndexedListSnapshot<Item> selected = reversed.maskedByPhysicalStates(
                states, required, forbidden, M3IndexPathMasks.Backend.JAVA);
        assertArrayEquals(new int[] {5, 3, 0}, selected.physicalIndices());
        assertArrayEquals(new int[] {3},
                reversed.subList(1, 5).maskedByPhysicalStates(
                        states, required, forbidden,
                        M3IndexPathMasks.Backend.JAVA).physicalIndices());
        assertArrayEquals(new int[0],
                reversed.subList(0, 0).maskedByPhysicalStates(
                        states, required, forbidden,
                        M3IndexPathMasks.Backend.JAVA).physicalIndices());
        assertArrayEquals(new int[] {5, 3, 0},
                reversed.sortedMaskedByPhysicalStates(states, required, forbidden,
                        Comparator.comparingInt(Item::score),
                        M3IndexPathMasks.Backend.JAVA).physicalIndices());
        Arrays.fill(highBit, 0);
        assertArrayEquals(new int[] {5, 3, 0}, selected.physicalIndices(),
                "returned index directory must not retain caller mask arrays");
    }

    @Test void nullAndEqualButDistinctOccurrencesKeepTheirOwnPhysicalSlots() {
        String first = new String("same");
        String second = new String("same");
        assertEquals(first, second);
        assertNotSame(first, second);
        M3IndexedListSnapshot<String> reverse =
                M3IndexedListSnapshot.copyOf(new String[] {
                    first, null, second, null
                }).reversed();
        assertSame(second, reverse.get(1));
        assertSame(first, reverse.get(3));
        assertArrayEquals(new int[] {3, 2, 1, 0}, reverse.physicalIndices());
        assertArrayEquals(new int[] {3, 1}, reverse.filtered(
                java.util.Objects::isNull).physicalIndices());
        assertArrayEquals(new int[] {3, 1, 2, 0},
                reverse.sorted(Comparator.nullsFirst(
                        Comparator.naturalOrder())).physicalIndices());
        assertArrayEquals(new int[] {3, 1},
                reverse.maskedByPhysicalState(new int[] {0, 1, 0, 1},
                        1, 0, M3IndexPathMasks.Backend.JAVA).physicalIndices());
    }

    @Test void noOpMutationsAlwaysRefuseWithoutCallback() {
        M3IndexedListSnapshot<Item> empty = sample().subList(0, 0).reversed();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(UnsupportedOperationException.class, empty::removeFirst);
        assertThrows(UnsupportedOperationException.class, empty::removeLast);
        assertThrows(UnsupportedOperationException.class, empty::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> empty.addAll(Collections.emptyList()));
        assertThrows(UnsupportedOperationException.class,
                () -> empty.removeIf(value -> { calls.incrementAndGet(); return true; }));
        assertThrows(UnsupportedOperationException.class,
                () -> empty.replaceAll(value -> { calls.incrementAndGet(); return value; }));
        assertThrows(UnsupportedOperationException.class,
                () -> empty.sort(Comparator.comparing(Item::id)));
        assertEquals(0, calls.get());
    }

    @Test void signedStridedSelectionMatchesScalarOracleForRandomSlices() {
        Random random = new Random(0x5eed21);
        for (int trial = 0; trial < 240; trial++) {
            int n = random.nextInt(130);
            Integer[] data = new Integer[n];
            int[] state = new int[n];
            for (int i = 0; i < n; i++) {
                data[i] = random.nextInt(13);
                state[i] = random.nextInt();
            }
            M3IndexedListSnapshot<Integer> base = M3IndexedListSnapshot.copyOf(data);
            M3IndexedListSnapshot<Integer> reverse = base.reversed();
            int start = random.nextInt(n + 1);
            int end = start + random.nextInt(n - start + 1);
            M3IndexedListSnapshot<Integer> slice = reverse.subList(start, end);
            int required = (trial & 1) == 0 ? Integer.MIN_VALUE : 0x40;
            int forbidden = (trial & 2) == 0 ? 0x10 : 0x20;
            List<Integer> oracle = new ArrayList<>();
            for (int ordinal = 0; ordinal < slice.size(); ordinal++) {
                int physical = slice.physicalIndex(ordinal);
                int word = state[physical];
                if ((word & required) == required && (word & forbidden) == 0) {
                    oracle.add(physical);
                }
            }
            int[] actual = slice.maskedByPhysicalState(
                    state, required, forbidden, M3IndexPathMasks.Backend.JAVA)
                    .physicalIndices();
            assertArrayEquals(oracle.stream().mapToInt(Integer::intValue).toArray(),
                    actual, "trial " + trial);
            if (M3IndexPathMasks.nativeAvailable()) {
                assertArrayEquals(actual,
                        slice.maskedByPhysicalState(state, required, forbidden,
                                M3IndexPathMasks.Backend.NATIVE_REQUIRED)
                                .physicalIndices(),
                        "JNI trial " + trial);
            }
        }
    }

    @Test void nativeBackendIsRequiredOrRefusesExplicitly() {
        M3IndexedListSnapshot<Item> reversed = sample().reversed();
        int[] states = {
            Integer.MIN_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, 0, Integer.MIN_VALUE
        };
        int[] expected = reversed.maskedByPhysicalState(
                states, Integer.MIN_VALUE, 0, M3IndexPathMasks.Backend.JAVA)
                .physicalIndices();
        if (Boolean.getBoolean("m3.indexedReversedNativeRequired")) {
            assertTrue(M3IndexPathMasks.nativeAvailable(),
                    "real native library required for this JNI parity qualification");
        }
        if (M3IndexPathMasks.nativeAvailable()) {
            int[] nativeResult = reversed.maskedByPhysicalState(
                    states, Integer.MIN_VALUE, 0,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED).physicalIndices();
            assertArrayEquals(expected, nativeResult);
        } else {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> reversed.maskedByPhysicalState(
                            states, Integer.MIN_VALUE, 0,
                            M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }
}
