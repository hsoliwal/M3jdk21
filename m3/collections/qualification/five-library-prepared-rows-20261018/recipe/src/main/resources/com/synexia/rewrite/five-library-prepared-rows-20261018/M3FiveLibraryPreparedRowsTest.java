/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Real Java21 provider/JNI oracle for one-time predicate and five named families. */
final class M3FiveLibraryPreparedRowsTest {
    private record Pair(String key, Integer value) { }

    @Test void predicateEvaluatedOnceAndBitAlgebraPreservesLogicalOccurrences() {
        Random rnd = new Random(0x5f17ecaL);
        for (int repeat = 0; repeat < 250; repeat++) {
            int physical = 1 + rnd.nextInt(90);
            String[] keys = new String[physical];
            Integer[] values = new Integer[physical];
            byte[] states = new byte[physical];
            for (int i = 0; i < physical; i++) {
                keys[i] = "k" + (i % 13);
                values[i] = (i % 17) - 8;
                states[i] = (byte) rnd.nextInt(256);
            }
            int n = rnd.nextInt(2 * physical + 1);
            int[] rows = new int[n];
            for (int i = 0; i < n; i++) rows[i] = rnd.nextInt(physical);
            var source = M3FiveLibraryIndexViews.capture(keys, values, rows, states);
            List<Pair> original = new ArrayList<>();
            for (int row : rows) original.add(new Pair(keys[row], values[row]));
            Arrays.fill(values, Integer.MIN_VALUE);
            Arrays.fill(rows, -1);
            AtomicInteger firstCalls = new AtomicInteger();
            AtomicInteger secondCalls = new AtomicInteger();
            var first = M3FiveLibraryPreparedRows.where(source, (key, value) -> {
                firstCalls.incrementAndGet(); return value >= 0;
            });
            var second = M3FiveLibraryPreparedRows.where(source, (key, value) -> {
                secondCalls.incrementAndGet(); return (value & 1) == 0;
            });
            BitSet a = new BitSet(), b = new BitSet();
            for (int i = 0; i < original.size(); i++) {
                if (original.get(i).value() >= 0) a.set(i);
                if ((original.get(i).value() & 1) == 0) b.set(i);
            }
            var combined = first.and(second);
            BitSet ref = (BitSet) a.clone(); ref.and(b);
            assertEquals(ref, combined.positionsCopy());
            assertEquals(ref.cardinality(), combined.size());
            var disjunction = first.or(second);
            BitSet union = (BitSet) a.clone(); union.or(b);
            assertEquals(union, disjunction.positionsCopy());
            BitSet xor = (BitSet) a.clone(); xor.xor(b);
            assertEquals(xor, first.xor(second).positionsCopy());
            BitSet minus = (BitSet) a.clone(); minus.andNot(b);
            assertEquals(minus, first.minus(second).positionsCopy());
            BitSet complement = (BitSet) a.clone(); complement.flip(0, original.size());
            assertEquals(complement, first.not().positionsCopy());
            assertEquals(n, firstCalls.get());
            assertEquals(n, secondCalls.get());
            for (int i = 0; i < combined.size(); i++) {
                int sourceOrdinal = combined.sourceOrdinalAt(i);
                assertEquals(original.get(sourceOrdinal).key(), combined.keyAt(i));
                assertEquals(original.get(sourceOrdinal).value(), combined.valueAt(i));
                assertEquals(source.physicalIndexAt(sourceOrdinal), combined.physicalIndexAt(i));
            }
            var sorted = disjunction.sortedByValue(Comparator.naturalOrder());
            for (int i = 1; i < sorted.size(); i++) {
                assertTrue(sorted.valueAt(i - 1) <= sorted.valueAt(i));
                if (Objects.equals(sorted.valueAt(i - 1), sorted.valueAt(i))) {
                    assertTrue(sorted.sourceOrdinalAt(i - 1) < sorted.sourceOrdinalAt(i),
                            "stable tie must preserve prior encounter order");
                }
            }
            int from = rnd.nextInt(sorted.size() + 1);
            var suffix = sorted.slice(from, sorted.size());
            assertEquals(sorted.size() - from, suffix.size());
            for (int i = 0; i < suffix.size(); i++) {
                assertEquals(sorted.sourceOrdinalAt(from + i), suffix.sourceOrdinalAt(i));
            }
            AtomicInteger refinedCalls = new AtomicInteger();
            var refined = sorted.refine((key, value) -> {
                refinedCalls.incrementAndGet(); return value > 2;
            });
            assertEquals(sorted.size(), refinedCalls.get());
            for (int i = 0; i < refined.size(); i++) assertTrue(refined.valueAt(i) > 2);
            assertEquals(n, firstCalls.get());
            assertEquals(n, secondCalls.get());
            BitSet escaped = first.positionsCopy(); escaped.clear();
            assertEquals(a, first.positionsCopy());
            int[] escapedOrdinals = first.sourceOrdinalsCopy();
            Arrays.fill(escapedOrdinals, -1);
            assertEquals(a.cardinality(), first.size());
            var statesPlan = M3FiveLibraryPreparedRows.byState(source, 0x81, 0x04,
                    M3IndexMaskCompactor.Backend.JAVA);
            BitSet statesExpected = new BitSet();
            for (int i = 0; i < original.size(); i++) {
                int state = source.unsignedStateAt(i);
                if ((state & 0x81) == 0x81 && (state & 4) == 0) statesExpected.set(i);
            }
            assertEquals(statesExpected, statesPlan.positionsCopy());
            if (M3IndexMaskCompactor.nativeAvailable()) {
                var nativePlan = M3FiveLibraryPreparedRows.byState(source, 0x81, 0x04,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
                assertArrayEquals(statesPlan.sourceOrdinalsCopy(), nativePlan.sourceOrdinalsCopy());
            }
        }
    }

    @Test void correctVendorSemanticsWithDuplicateAndNullValues() {
        String[] keys = {"a", "a", "b", "b", "a"};
        Integer[] values = {1, 1, 2, 2, null};
        int[] rows = {0, 1, 2, 3, 4, 0};
        byte[] states = {1, 1, 1, 1, 1};
        var input = M3FiveLibraryIndexViews.capture(keys, values, rows, states);
        var image = M3FiveLibraryPreparedRows.where(input, (key, value) -> true);
        assertEquals(6, image.size());
        var jdk = image.asJdkList();
        assertEquals(Arrays.asList(1, 1, 2, 2, null, 1), jdk);
        var jdkMap = image.asJdkMapLastWins();
        assertEquals(2, jdkMap.size());
        assertEquals(Integer.valueOf(1), jdkMap.get("a"));
        assertEquals(Integer.valueOf(2), jdkMap.get("b"));
        var guavaList = image.asGuavaListMultimap();
        assertEquals(6, guavaList.size());
        assertEquals(Arrays.asList(1, 1, null, 1), guavaList.get("a"));
        var apacheList = image.asApacheListValuedMap();
        assertEquals(6, apacheList.size());
        assertEquals(4, apacheList.get("a").size());
        var spring = image.asSpringMultiValueMap();
        assertEquals(2, spring.size());
        assertEquals(4, spring.get("a").size());
        var eclipseList = image.asEclipseListMultimap();
        assertEquals(6, eclipseList.size());
        assertEquals(4, eclipseList.get("a").size());
        assertEquals(3, image.asGuavaSetMultimap().size());
        assertEquals(3, image.asApacheSetValuedMap().size());
        assertEquals(3, image.asEclipseSetMultimap().size());
        // Independent outputs: mutation of one result must not mutate other provider owners.
        guavaList.get("a").clear();
        assertEquals(4, apacheList.get("a").size());
        assertEquals(4, spring.get("a").size());
        assertEquals(6, image.size());
    }

    @Test void countedProvidersRemainIndependent() {
        String[] keys = {"a", "b", "c"};
        Integer[] values = {8, 8, 3};
        var source = M3FiveLibraryIndexViews.capture(keys, values, new int[] {0, 0, 1, 2},
                new byte[] {1, 1, 1});
        var selected = M3FiveLibraryPreparedRows.where(source, (k, v) -> true);
        assertEquals(3, selected.asGuavaMultiset().count(8));
        assertEquals(3, selected.asEclipseBag().occurrencesOf(8));
        assertEquals(3, selected.asApacheBag().getCount(8));
        assertEquals(4, selected.size());
    }

    @Test void failureIsolationAndNativeRequiredNoFallback() {
        var source = M3FiveLibraryIndexViews.capture(new String[] {"a"},
                new Integer[] {5}, new int[] {0, 0}, new byte[] {0});
        var left = M3FiveLibraryPreparedRows.where(source, (k, v) -> true);
        var another = M3FiveLibraryPreparedRows.where(
                M3FiveLibraryIndexViews.capture(new String[] {"a"},
                        new Integer[] {5}, new int[] {0, 0}, new byte[] {0}), (k, v) -> true);
        assertThrows(IllegalArgumentException.class, () -> left.and(another));
        BitSet outside = new BitSet(); outside.set(2);
        assertThrows(IndexOutOfBoundsException.class, () -> M3FiveLibraryPreparedRows.fromBits(
                source, outside, M3IndexMaskCompactor.Backend.JAVA));
        assertThrows(IndexOutOfBoundsException.class, () -> left.slice(0, 3));
        assertThrows(IllegalArgumentException.class, () -> M3FiveLibraryPreparedRows.byState(
                source, 0x100, 0, M3IndexMaskCompactor.Backend.JAVA));
        var empty = M3FiveLibraryPreparedRows.byState(source, 1, 1,
                M3IndexMaskCompactor.Backend.JAVA);
        assertTrue(empty.isEmpty());
        assertThrows(IllegalStateException.class, () -> left.refine((k, v) -> {
            throw new IllegalStateException("client predicate exception");
        }));
        assertEquals(2, left.size());
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            AtomicInteger calls = new AtomicInteger();
            assertThrows(UnsatisfiedLinkError.class, () -> M3FiveLibraryPreparedRows.where(
                    source, (k, v) -> { calls.incrementAndGet(); return true; },
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            assertEquals(0, calls.get());
        }
    }
}
