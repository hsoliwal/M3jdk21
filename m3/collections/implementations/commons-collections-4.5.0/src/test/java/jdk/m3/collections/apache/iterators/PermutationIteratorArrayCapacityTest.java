/*
 * Copyright 2026 Hitesh Soliwal and contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.apache.iterators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Source-identity and exact enumeration contract for preallocated permutation results. */
class PermutationIteratorArrayCapacityTest {

    @Test
    void allSmallFactorialsPreserveOriginalOccurrences() {
        for (int count = 0; count <= 6; count++) {
            List<String> original = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                original.add(i % 3 == 0 ? null : new String(i % 2 == 0 ? "same" : "other"));
            }
            IdentityHashMap<String, Integer> expectedCounts = occurrences(original);
            PermutationIterator<String> actual = new PermutationIterator<>(original);
            int produced = 0;
            while (actual.hasNext()) {
                List<String> permutation = actual.next();
                assertEquals(count, permutation.size());
                assertEquals(expectedCounts, occurrences(permutation));
                produced++;
            }
            assertEquals(factorial(count), produced);
            assertFalse(actual.hasNext());
            assertThrows(NoSuchElementException.class, actual::next);
            assertThrows(UnsupportedOperationException.class, actual::remove);
        }
    }

    @Test
    void distinctOutputListsDoNotAliasSubsequentPermutationContent() {
        List<Object> values = Arrays.asList(new Object(), new Object(), new Object());
        PermutationIterator<Object> actual = new PermutationIterator<>(values);
        List<Object> first = actual.next();
        List<Object> retained = new ArrayList<>(first);
        assertTrue(actual.hasNext());
        List<Object> second = actual.next();
        assertEquals(retained, first);
        assertEquals(values.size(), second.size());
        for (Object element : second) {
            assertTrue(values.contains(element));
        }
    }

    private static int factorial(int n) {
        int value = 1;
        for (int i = 2; i <= n; i++) {
            value *= i;
        }
        return value;
    }

    private static <E> IdentityHashMap<E, Integer> occurrences(List<E> input) {
        IdentityHashMap<E, Integer> counts = new IdentityHashMap<>();
        for (E object : input) {
            Integer count = counts.get(object);
            counts.put(object, count == null ? 1 : count + 1);
        }
        return counts;
    }
}
