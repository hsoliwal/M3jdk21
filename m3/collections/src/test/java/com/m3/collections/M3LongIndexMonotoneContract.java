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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Independent stable-order oracle for existing immutable physical-index windows. */
public final class M3LongIndexMonotoneContract {
    private static long checks;
    private static long scenarios;
    private static long digest;

    private M3LongIndexMonotoneContract() { }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static M3LongIndexProjection verify(long[] values, M3LongIndexProjection view,
            boolean descending) {
        scenarios++;
        int[] before = view.physicalIndices();
        M3LongIndexProjection.Order beforeOrder = view.order();
        List<Integer> expected = new ArrayList<>();
        for (int physical : before) expected.add(physical);
        Comparator<Integer> order = Comparator.comparingLong(physical -> values[physical]);
        expected.sort(descending ? order.reversed() : order);
        M3LongIndexProjection sorted = descending ? view.sortedDescending() : view.sortedAscending();
        check(sorted.size() == expected.size(), "sorted cardinality");
        check(sorted.order() == (descending ? M3LongIndexProjection.Order.DESCENDING
                : M3LongIndexProjection.Order.ASCENDING), "reported order");
        long[] materialized = sorted.toArray();
        int[] physical = sorted.physicalIndices();
        for (int i = 0; i < expected.size(); i++) {
            int expectedPhysical = expected.get(i);
            check(physical[i] == expectedPhysical, "stable physical occurrence");
            check(sorted.physicalIndex(i) == expectedPhysical, "physical index lookup");
            check(sorted.get(i) == values[expectedPhysical], "signed value lookup");
            check(materialized[i] == values[expectedPhysical], "materialized value");
            digest = digest * 31 + expectedPhysical;
            digest = digest * 31 + materialized[i];
        }
        check(Arrays.equals(before, view.physicalIndices()), "source window remains unchanged");
        check(view.order() == beforeOrder, "source metadata remains unchanged");
        M3LongIndexProjection again = descending ? sorted.sortedDescending() : sorted.sortedAscending();
        check(Arrays.equals(sorted.physicalIndices(), again.physicalIndices()), "same-order repeat");
        if (physical.length > 0) {
            physical[0] = -1;
            materialized[0] ^= Long.MIN_VALUE;
            check(sorted.physicalIndex(0) == expected.getFirst(), "exported index isolation");
            check(sorted.get(0) == values[expected.getFirst()], "exported value isolation");
        }
        return sorted;
    }

    private static void explicitCases() {
        long[] values = {Long.MIN_VALUE, 0, 0, Long.MAX_VALUE, Long.MIN_VALUE, 0};
        M3LongIndexProjection view = M3LongIndexProjection.snapshot(values);
        check(Arrays.equals(view.sortedAscending().physicalIndices(), new int[] {0, 4, 1, 2, 5, 3}),
                "explicit ascending ties and extrema");
        check(Arrays.equals(view.sortedDescending().physicalIndices(), new int[] {3, 1, 2, 5, 0, 4}),
                "explicit descending ties and extrema");
        verify(values, view, false);
        verify(values, view, true);
        long[] original = values.clone();
        Arrays.fill(values, 41);
        check(Arrays.equals(original, view.toArray()), "snapshot owns immutable payload");
        verify(original, view.slice(1, 3), false);
        verify(original, view.slice(1, 3), true);
        M3LongIndexProjection sorted = view.sortedAscending();
        verify(original, sorted.slice(1, 4), true);
        verify(original, sorted.slice(1, 4).sortedDescending(), false);
        check(view.slice(view.size(), 0).sortedAscending().isEmpty(), "empty tail window");
    }

    private static void exhaustiveWindows() {
        for (int length = 0, combinations = 1; length <= 6; length++, combinations *= 3) {
            for (int code = 0; code < combinations; code++) {
                long[] values = new long[length];
                int remaining = code;
                for (int i = 0; i < length; i++) {
                    values[i] = remaining % 3 - 1;
                    remaining /= 3;
                }
                M3LongIndexProjection root = M3LongIndexProjection.snapshot(values);
                for (int first = 0; first <= length; first++) {
                    for (int count = 0; count <= length - first; count++) {
                        M3LongIndexProjection window = root.slice(first, count);
                        M3LongIndexProjection ascending = verify(values, window, false);
                        M3LongIndexProjection descending = verify(values, window, true);
                        verify(values, ascending, true);
                        verify(values, descending, false);
                    }
                }
            }
        }
    }

    private static void allMasks(M3IndexMaskCompactor.Backend backend) {
        long[][] cases = {
            {Long.MIN_VALUE, -9, -9, 0, 0, 8, Long.MAX_VALUE},
            {Long.MAX_VALUE, 8, 8, 0, 0, -9, Long.MIN_VALUE},
            {9, -4, 2, -4, 8, 2, Long.MIN_VALUE},
            {6, 6, 6, 6, 6, 6, 6}
        };
        for (long[] values : cases) {
            M3LongIndexProjection root = M3LongIndexProjection.snapshot(values);
            for (int mask = 0; mask <= 255; mask++) {
                for (int first = 0; first <= values.length; first++) {
                    int count = values.length - first;
                    byte[] states = new byte[count];
                    for (int i = 0; i < count; i++) states[i] = (byte) (i * 73 + mask * 17);
                    M3LongIndexProjection selected = root.slice(first, count).filtered(states, mask, backend);
                    List<Integer> expected = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        if ((Byte.toUnsignedInt(states[i]) & mask) == mask) expected.add(first + i);
                    }
                    check(selected.size() == expected.size(), "mask count");
                    for (int i = 0; i < expected.size(); i++)
                        check(selected.physicalIndex(i) == expected.get(i), "mask occurrence");
                    int[] selectedBefore = selected.physicalIndices();
                    Arrays.fill(states, (byte) ~mask);
                    check(Arrays.equals(selectedBefore, selected.physicalIndices()), "mask input not retained");
                    verify(values, selected, false);
                    verify(values, selected, true);
                    if (!selected.isEmpty()) {
                        verify(values, selected.slice(1, selected.size() - 1), false);
                        verify(values, selected.slice(1, selected.size() - 1), true);
                    }
                }
            }
        }
    }

    private static void seededWindows(M3IndexMaskCompactor.Backend backend) {
        Random random = new Random(0x4d334d4f4e4fL);
        for (int trial = 0; trial < 384; trial++) {
            int length = 2 + random.nextInt(255);
            long[] values = new long[length];
            for (int i = 0; i < length; i++) {
                values[i] = switch (trial % 6) {
                    case 0 -> i / 3;
                    case 1 -> -(i / 3);
                    case 2 -> random.nextInt(11) - 5L;
                    case 3 -> random.nextLong();
                    case 4 -> i == length - 1 ? -1 : i;
                    default -> i == 0 ? Long.MAX_VALUE : i;
                };
            }
            int first = random.nextInt(length + 1);
            int count = random.nextInt(length - first + 1);
            M3LongIndexProjection window = M3LongIndexProjection.snapshot(values).slice(first, count);
            verify(values, window, false);
            verify(values, window, true);
            byte[] states = new byte[count];
            random.nextBytes(states);
            M3LongIndexProjection filtered = window.filtered(states, random.nextInt(256), backend);
            verify(values, filtered, false);
            verify(values, filtered, true);
        }
    }

    public static void main(String[] args) {
        if (args.length > 1 || args.length == 1 && !args[0].equals("--native"))
            throw new IllegalArgumentException("optional --native requires the actual JNI selector");
        checks = 0;
        scenarios = 0;
        digest = 0;
        M3IndexMaskCompactor.Backend backend = args.length == 0
                ? M3IndexMaskCompactor.Backend.JAVA : M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        if (backend == M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                && !M3IndexMaskCompactor.nativeAvailable())
            throw new IllegalStateException("native mask library required");
        explicitCases();
        exhaustiveWindows();
        allMasks(backend);
        seededWindows(backend);
        System.out.println("PASS M3LongIndexMonotoneContract backend=" + backend + " checks=" + checks
                + " scenarios=" + scenarios + " digest=" + Long.toUnsignedString(digest, 16));
    }
}
