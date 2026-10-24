/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Independent occurrence oracles for the two retained donor projection atoms. */
class M3LongIndexProjectionUnionTest {
    @Test
    void trustedOrdinalsRetainSelectedPhysicalOccurrencesAndDetachBits() {
        Random random = new Random(0x4d33554e494f4eL);
        for (int trial = 0; trial < 256; trial++) {
            long[] values = new long[random.nextInt(129)];
            List<Integer> reference = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                values[i] = switch (i % 17) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(13) - 6L;
                };
                reference.add(i);
            }
            M3LongIndexProjection source = M3LongIndexProjection.snapshot(values);
            Comparator<Integer> comparator = Comparator.comparingLong(i -> values[i]);
            if (trial % 3 == 1) {
                reference.sort(comparator);
                source = source.sortedAscending();
            } else if (trial % 3 == 2) {
                reference.sort(comparator.reversed());
                source = source.sortedDescending();
            }
            int first = random.nextInt(values.length + 1);
            int count = random.nextInt(values.length - first + 1);
            source = source.slice(first, count);
            reference = new ArrayList<>(reference.subList(first, first + count));
            int[] sourceIds = source.physicalIndices();
            BitSet admitted = new BitSet();
            List<Integer> kept = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (random.nextBoolean()) {
                    admitted.set(i);
                    kept.add(reference.get(i));
                }
            }
            // This package-private donor atom requires a private plain BitSet whose
            // bounded extent and count were admitted together. Opaque origins use
            // the existing bitmap-plan byte-mask route, covered separately below.
            M3LongIndexProjection selected = source.filteredOrdinals(admitted, kept.size());
            int[] expected = kept.stream().mapToInt(Integer::intValue).toArray();
            assertArrayEquals(expected, selected.physicalIndices());
            assertEquals(source.order(), selected.order());
            for (int i = 0; i < expected.length; i++) assertEquals(values[expected[i]], selected.get(i));
            admitted.flip(0, count);
            assertArrayEquals(expected, selected.physicalIndices(), "selection does not retain bits");
            assertArrayEquals(sourceIds, source.physicalIndices(), "source remains immutable");
        }
    }

    @Test
    void allSelectedReusesTheAdmittedImmutableWindow() throws ReflectiveOperationException {
        M3LongIndexProjection source = M3LongIndexProjection.snapshot(new long[] {9, 1, 3, 3, 7})
                .sortedAscending().slice(1, 3);
        BitSet all = new BitSet();
        all.set(0, source.size());
        M3LongIndexProjection selected = source.filteredOrdinals(all, source.size());
        assertNotSame(source, selected);
        assertArrayEquals(source.physicalIndices(), selected.physicalIndices());
        for (String name : List.of("values", "indices")) {
            Field field = M3LongIndexProjection.class.getDeclaredField(name);
            assertTrue(field.trySetAccessible());
            assertSame(field.get(source), field.get(selected));
        }
        assertEquals(source.order(), source.filteredOrdinals(new BitSet(), 0).order());
        assertTrue(source.filteredOrdinals(new BitSet(), 0).isEmpty());
    }

    @Test
    void reversedPathMasksPreserveNewEncounterTieOrderWhenResorted() {
        long[] values = {3, 1, 3, 1, 5, Long.MIN_VALUE, Long.MAX_VALUE};
        M3LongIndexProjection source = M3LongIndexProjection.snapshot(values).sortedAscending();
        int[][] planes = {new int[values.length]};
        M3LongIndexProjection selected = source.filteredPathMasks(planes, new int[] {0},
                new int[] {0}, source.size() - 1, source.size(), -1, M3IndexPathMasks.Backend.JAVA);
        List<Integer> expected = Arrays.stream(selected.physicalIndices()).boxed().toList();
        for (boolean descending : new boolean[] {false, true}) {
            List<Integer> ordered = new ArrayList<>(expected);
            Comparator<Integer> comparator = Comparator.comparingLong(i -> values[i]);
            ordered.sort(descending ? comparator.reversed() : comparator);
            M3LongIndexProjection actual = descending ? selected.sortedDescending() : selected.sortedAscending();
            assertArrayEquals(ordered.stream().mapToInt(Integer::intValue).toArray(), actual.physicalIndices());
        }
        Arrays.fill(planes[0], -1);
        assertArrayEquals(expected.stream().mapToInt(Integer::intValue).toArray(), selected.physicalIndices());
    }

    @Test
    void originalOpaqueBitmapOriginContractRemainsExecutable() throws Exception {
        M3BitmapOrdinalSelectionSelfTest.main(new String[0]);
    }

    @Test
    void anExplicitNativeQualificationCannotPassThroughAnAbsentLibrary() {
        if (Boolean.getBoolean("m3.projection.nativeRequired")) {
            assertTrue(M3IndexPathMasks.nativeAvailable(), "actual path-mask JNI is required");
            assertTrue(M3IndexMaskCompactor.nativeAvailable(), "actual byte-mask JNI is required");
        }
    }
}
