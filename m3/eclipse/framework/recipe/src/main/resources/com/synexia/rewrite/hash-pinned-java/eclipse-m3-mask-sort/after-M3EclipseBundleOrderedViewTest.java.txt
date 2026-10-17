// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LongIndexProjection;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Differential checks for shared indexed-array filter/sort and Eclipse bundle references. */
class M3EclipseBundleOrderedViewTest {
    private static final M3IndexMaskCompactor.Backend JAVA = M3IndexMaskCompactor.Backend.JAVA;

    @Test
    void ascendingDescendingAndHalfOpenWindowsPreserveBundleIdentity() {
        Bundle ten = bundle(10L, Bundle.ACTIVE);
        Bundle two = bundle(2L, Bundle.RESOLVED);
        Bundle eight = bundle(8L, Bundle.ACTIVE);
        Bundle zero = bundle(0L, Bundle.STARTING);
        Bundle maximum = bundle(Long.MAX_VALUE, Bundle.ACTIVE);
        M3EclipseBundleSnapshot snapshot = M3EclipseBundleSnapshot.capture(
                context(ten, two, eight, zero, maximum));
        assertEquals(List.of(zero, two, eight, ten, maximum), snapshot.sortedById(false));
        assertEquals(List.of(maximum, ten, eight, two, zero), snapshot.sortedById(true));
        assertEquals(List.of(eight, ten, maximum), snapshot.sortedById(
                Bundle.ACTIVE, Bundle.STARTING, false, JAVA));
        assertEquals(List.of(maximum, ten, eight), snapshot.sortedById(
                Bundle.ACTIVE, Bundle.STARTING, true, JAVA));
        List<Bundle> window = snapshot.sortedWindowById(
                Bundle.ACTIVE, Bundle.STARTING, false, 1, 1, JAVA);
        assertEquals(1, window.size());
        assertSame(ten, window.get(0));
        assertThrows(UnsupportedOperationException.class, () -> window.clear());
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.sortedWindowById(
                0, 0, false, 5, 1, JAVA));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.sortedWindowById(
                0, 0, false, -1, 1, JAVA));
    }

    @Test
    void invalidOrContradictoryMaskRefusesBeforeIndexRead() {
        M3EclipseBundleSnapshot snapshot = M3EclipseBundleSnapshot.capture(context(
                bundle(1L, Bundle.ACTIVE)));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot.sortedById(0x100, 0, false, JAVA));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot.sortedById(Bundle.ACTIVE, Bundle.ACTIVE, false, JAVA));
        assertThrows(NullPointerException.class,
                () -> snapshot.sortedById(0, 0, false, null));
        assertEquals(List.of(), M3EclipseBundleSnapshot.capture(context()).sortedById(false));
    }

    @Test
    void composedMaskSortSlicePreservesPhysicalIndexAndStableTies() {
        long[] values = {9, 8, 0, 9, 9, Long.MIN_VALUE};
        byte[] flags = {1, 2, 1, 1, 3, 1};
        M3LongIndexProjection base = M3LongIndexProjection.snapshot(values);
        M3LongIndexProjection selected = base.filtered(flags, 1, 2, JAVA);
        assertArrayEquals(new long[]{9, 0, 9, Long.MIN_VALUE}, selected.toArray());
        M3LongIndexProjection ascending = selected.sortedAscending();
        assertArrayEquals(new long[]{Long.MIN_VALUE, 0, 9, 9}, ascending.toArray());
        assertArrayEquals(new int[]{5, 2, 0, 3}, ascending.physicalIndices());
        M3LongIndexProjection descending = selected.sortedDescending();
        assertArrayEquals(new int[]{0, 3, 2, 5}, descending.physicalIndices());
        assertArrayEquals(new long[]{9, 0}, descending.slice(1, 2).toArray());
        values[0] = -100;
        flags[0] = 0;
        assertEquals(9L, ascending.get(2));
    }

    @Test
    void randomizedOracleAgreesOnOccurrenceOrderAndSignedSort() {
        Random random = new Random(20261017L);
        for (int trial = 0; trial < 512; trial++) {
            int size = random.nextInt(129);
            long[] values = new long[size];
            byte[] states = new byte[size];
            for (int i = 0; i < size; i++) {
                values[i] = random.nextInt(17) - 8;
                states[i] = (byte) random.nextInt(256);
            }
            int required = random.nextInt(256);
            int excluded = random.nextInt(256) & ~required;
            M3LongIndexProjection base = M3LongIndexProjection.snapshot(values);
            M3LongIndexProjection filtered = base.filtered(states, required, excluded, JAVA);
            List<Integer> oracle = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int state = states[i] & 0xff;
                if ((state & required) == required && (state & excluded) == 0) oracle.add(i);
            }
            assertEquals(oracle.size(), filtered.size());
            for (int i = 0; i < oracle.size(); i++) {
                assertEquals(oracle.get(i).intValue(), filtered.physicalIndex(i));
            }
            oracle.sort(Comparator.comparingLong(i -> values[i]));
            assertEquals(oracle.size(), filtered.sortedAscending().size());
            for (int i = 0; i < oracle.size(); i++) {
                assertEquals(oracle.get(i).intValue(), filtered.sortedAscending().physicalIndex(i));
            }
            oracle.sort((a, b) -> Long.compare(values[b], values[a]));
            for (int i = 0; i < oracle.size(); i++) {
                assertEquals(oracle.get(i).intValue(), filtered.sortedDescending().physicalIndex(i));
            }
        }
    }

    @Test
    void oldRequiredOnlyMethodAndNewExcludedZeroAgree() {
        long[] values = {0L, Long.MIN_VALUE, Long.MAX_VALUE, 0L};
        byte[] masks = {(byte) 0xff, 0, 7, 3};
        M3LongIndexProjection source = M3LongIndexProjection.snapshot(values);
        assertArrayEquals(source.filtered(masks, 3, JAVA).toArray(),
                source.filtered(masks, 3, 0, JAVA).toArray());
        assertArrayEquals(new long[]{0L, Long.MAX_VALUE, 0L},
                source.filtered(masks, 3, 0, JAVA).toArray());
        assertThrows(IllegalArgumentException.class,
                () -> source.filtered(new byte[1], 0, 1, JAVA));
        assertThrows(IllegalArgumentException.class,
                () -> source.filtered(masks, -1, 0, JAVA));
        assertThrows(IllegalArgumentException.class,
                () -> source.filtered(masks, 1, 1, JAVA));
        assertThrows(NullPointerException.class,
                () -> source.filtered(masks, 0, 1, null));
    }

    @Test
    void optInJniSelectionHasParityOrRefusesMissingNativeLibrary() {
        M3LongIndexProjection source = M3LongIndexProjection.snapshot(
                new long[]{5L, 2L, 5L, Long.MIN_VALUE});
        byte[] masks = {1, 3, 1, 1};
        if (M3IndexMaskCompactor.nativeAvailable()) {
            assertArrayEquals(
                    source.filtered(masks, 1, 2, JAVA).toArray(),
                    source.filtered(masks, 1, 2,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).toArray());
        } else {
            assertThrows(UnsatisfiedLinkError.class,
                    () -> source.filtered(masks, 1, 2,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        assertFalse(source.sortedAscending().isEmpty());
    }

    private static Bundle bundle(long id, int state) {
        return (Bundle) Proxy.newProxyInstance(Bundle.class.getClassLoader(),
                new Class<?>[]{Bundle.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getBundleId" -> id;
                    case "getState" -> state;
                    case "equals" -> proxy == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "Bundle#" + id;
                    default -> throw new AssertionError("unexpected bundle method: " + method.getName());
                });
    }

    private static BundleContext context(Bundle... bundles) {
        return (BundleContext) Proxy.newProxyInstance(BundleContext.class.getClassLoader(),
                new Class<?>[]{BundleContext.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getBundles")) return bundles.clone();
                    throw new AssertionError("unexpected context method: " + method.getName());
                });
    }
}
