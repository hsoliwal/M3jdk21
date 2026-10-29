<<<<<<< HEAD
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
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Differential geometry, signed-mask and JNI oracle for composable physical paths. */
final class M3LongIndexProjectionPathMasksTest {
    private static final long SEED = 0x4d33504154484d41L;

    @Test
    void stagedMasksKeepLogicalOffsetsSortedDirectionAndPhysicalIdentity() {
        long[] source = {5, -7, 5, Long.MIN_VALUE, 9, Long.MAX_VALUE, -7};
        long[] original = source.clone();
        M3LongIndexProjection root = M3LongIndexProjection.snapshot(source);
        Arrays.fill(source, 99L);
        M3LongIndexProjection view = root.sortedAscending().slice(1, 5);
        int[][] planes = {
            {Integer.MIN_VALUE, -1, 0, Integer.MIN_VALUE, -1, -1, 0},
            {-1, 0, 0, 0x55, 0x22, -1, 0}
        };
        int[] required = {Integer.MIN_VALUE, 0};
        int[] forbidden = {0, 0x01};

        compare(view, original, planes, required, forbidden, 4, 5, -1, true);
        compare(view, original, planes, required, forbidden, 0, 5, 1, true);
        compare(root, original, planes, new int[] {0, 0},
                new int[] {0, 0}, 6, 0, -1, true);

        M3LongIndexProjection all =
                view.filteredPathMasks(planes, new int[] {0, 0},
                        new int[] {0, 0}, 4, 5, -1, M3IndexPathMasks.Backend.JAVA);
        check(all.order() == M3LongIndexProjection.Order.DESCENDING, "reversed ascending");
        M3LongIndexProjection ascending =
                view.sortedDescending().filteredPathMasks(
                        planes, new int[] {0, 0}, new int[] {0, 0},
                        4, 5, -1, M3IndexPathMasks.Backend.JAVA);
        check(ascending.order() == M3LongIndexProjection.Order.ASCENDING, "reversed descending");
        check(Arrays.equals(original, root.toArray()), "source isolation");

        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        view.size(), 1, 1, M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        0, 2, -1, M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        0, 1, 0, M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        0, -1, 1, M3IndexPathMasks.Backend.JAVA));
        reject(NullPointerException.class, () ->
                view.filteredPathMasks(planes, required, forbidden, 0, 0, 1, null));
        reject(IllegalArgumentException.class, () ->
                view.filteredPathMasks(planes, new int[0], forbidden, 0, 0, 1,
                        M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(new int[][] {{0}, {0}}, required, forbidden, 0, 1, 1,
                        M3IndexPathMasks.Backend.JAVA));
    }

    @Test
    void deterministicMetamorphicSortSliceFilterAndStrideCompositions() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 512; trial++) {
            long[] values = new long[random.nextInt(90)];
            for (int i = 0; i < values.length; i++) {
                values[i] = switch (i % 13) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(13) - 6L;
                };
            }
            long[] original = values.clone();
            M3LongIndexProjection view = M3LongIndexProjection.snapshot(values);
            view = switch (trial % 3) {
                case 0 -> view.sortedAscending();
                case 1 -> view.sortedDescending();
                default -> view;
            };
            int from = random.nextInt(view.size() + 1);
            view = view.slice(from, random.nextInt(view.size() - from + 1));
            if (trial % 4 == 0) {
                byte[] aligned = new byte[view.size()];
                random.nextBytes(aligned);
                view = view.filtered(aligned, 0x80, M3IndexMaskCompactor.Backend.JAVA);
            }
            int[][] planes = {new int[values.length], new int[values.length]};
            for (int i = 0; i < values.length; i++) {
                planes[0][i] = random.nextInt();
                planes[1][i] = random.nextInt();
            }
            int[] required = {
                trial % 5 == 0 ? Integer.MIN_VALUE : random.nextInt(),
                trial % 7 == 0 ? 0 : random.nextInt()
            };
            int[] forbidden = {
                trial % 9 == 0 ? 0 : random.nextInt(),
                trial % 11 == 0 ? -1 : random.nextInt()
            };
            int step = 1 + random.nextInt(5);
            if ((trial & 1) == 0) step = -step;
            int first = view.size() == 0 ? 0 : random.nextInt(view.size());
            int max = view.size() == 0 ? 0 :
                    (step > 0 ? (view.size() - 1 - first) / step + 1
                              : first / -step + 1);
            int count = random.nextInt(max + 1);
            compare(view, original, planes, required, forbidden, first, count, step,
                    trial < 12);
        }
    }

    private static void compare(M3LongIndexProjection view, long[] physicalValues,
            int[][] stages, int[] required, int[] forbidden,
            int first, int count, int stride, boolean nativeCheck) {
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int physical = view.physicalIndex(first + i * stride);
            boolean keep = true;
            for (int stage = 0; stage < stages.length; stage++) {
                int word = stages[stage][physical];
                if ((word & required[stage]) != required[stage]
                        || (word & forbidden[stage]) != 0) {
                    keep = false;
                    break;
                }
            }
            if (keep) expected.add(physical);
        }
        int[] slots = expected.stream().mapToInt(Integer::intValue).toArray();
        M3LongIndexProjection selected = view.filteredPathMasks(
                stages, required, forbidden, first, count, stride,
                M3IndexPathMasks.Backend.JAVA);
        check(Arrays.equals(slots, selected.physicalIndices()), "physical slot selection");
        check(selected.size() == slots.length, "logical size");
        for (int i = 0; i < slots.length; i++) {
            check(selected.get(i) == physicalValues[slots[i]], "value by original slot");
        }
        M3LongIndexProjection.Order expectedOrder =
                stride >= 0 ? view.order() : switch (view.order()) {
                    case ASCENDING -> M3LongIndexProjection.Order.DESCENDING;
                    case DESCENDING -> M3LongIndexProjection.Order.ASCENDING;
                    case ENCOUNTER -> M3LongIndexProjection.Order.ENCOUNTER;
                };
        check(selected.order() == expectedOrder, "sorted-direction metadata");
        if (selected.size() != 0) {
            int[] escaped = selected.physicalIndices();
            escaped[0] = -1;
            check(selected.physicalIndex(0) == slots[0], "detached index directory");
        }
        if (nativeCheck) {
            if (M3IndexPathMasks.nativeAvailable()) {
                M3LongIndexProjection nativeSelected = view.filteredPathMasks(
                        stages, required, forbidden, first, count, stride,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(slots, nativeSelected.physicalIndices()),
                        "actual native path parity");
            } else {
                reject(UnsatisfiedLinkError.class, () -> view.filteredPathMasks(
                        stages, required, forbidden, first, count, stride,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            }
        }
    }

    private static void reject(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong failure: " + expected.getName(), failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
||||||| merged common ancestors
=======
/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Differential geometry, signed-mask and JNI oracle for composable physical paths. */
final class M3LongIndexProjectionPathMasksTest {
    private static final long SEED = 0x4d33504154484d41L;

    @Test
    void stagedMasksKeepLogicalOffsetsSortedDirectionAndPhysicalIdentity() {
        long[] source = {5, -7, 5, Long.MIN_VALUE, 9, Long.MAX_VALUE, -7};
        long[] original = source.clone();
        M3LongIndexProjection root = M3LongIndexProjection.snapshot(source);
        Arrays.fill(source, 99L);
        M3LongIndexProjection view = root.sortedAscending().slice(1, 5);
        int[][] planes = {
            {Integer.MIN_VALUE, -1, 0, Integer.MIN_VALUE, -1, -1, 0},
            {-1, 0, 0, 0x55, 0x22, -1, 0}
        };
        int[] required = {Integer.MIN_VALUE, 0};
        int[] forbidden = {0, 0x01};

        compare(view, original, planes, required, forbidden, 4, 5, -1, true);
        compare(view, original, planes, required, forbidden, 0, 5, 1, true);
        compare(root, original, planes, new int[] {0, 0},
                new int[] {0, 0}, 6, 0, -1, true);

        M3LongIndexProjection all =
                view.filteredPathMasks(planes, new int[] {0, 0},
                        new int[] {0, 0}, 4, 5, -1, M3IndexPathMasks.Backend.JAVA);
        check(all.order() == M3LongIndexProjection.Order.DESCENDING, "reversed ascending");
        M3LongIndexProjection ascending =
                view.sortedDescending().filteredPathMasks(
                        planes, new int[] {0, 0}, new int[] {0, 0},
                        4, 5, -1, M3IndexPathMasks.Backend.JAVA);
        check(ascending.order() == M3LongIndexProjection.Order.ASCENDING, "reversed descending");
        check(Arrays.equals(original, root.toArray()), "source isolation");

        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        view.size(), 1, 1, M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        0, 2, -1, M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        0, 1, 0, M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(planes, required, forbidden,
                        0, -1, 1, M3IndexPathMasks.Backend.JAVA));
        reject(NullPointerException.class, () ->
                view.filteredPathMasks(planes, required, forbidden, 0, 0, 1, null));
        reject(IllegalArgumentException.class, () ->
                view.filteredPathMasks(planes, new int[0], forbidden, 0, 0, 1,
                        M3IndexPathMasks.Backend.JAVA));
        reject(IndexOutOfBoundsException.class, () ->
                view.filteredPathMasks(new int[][] {{0}, {0}}, required, forbidden, 0, 1, 1,
                        M3IndexPathMasks.Backend.JAVA));
    }

    @Test
    void deterministicMetamorphicSortSliceFilterAndStrideCompositions() {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 512; trial++) {
            long[] values = new long[random.nextInt(90)];
            for (int i = 0; i < values.length; i++) {
                values[i] = switch (i % 13) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(13) - 6L;
                };
            }
            long[] original = values.clone();
            M3LongIndexProjection view = M3LongIndexProjection.snapshot(values);
            view = switch (trial % 3) {
                case 0 -> view.sortedAscending();
                case 1 -> view.sortedDescending();
                default -> view;
            };
            int from = random.nextInt(view.size() + 1);
            view = view.slice(from, random.nextInt(view.size() - from + 1));
            if (trial % 4 == 0) {
                byte[] aligned = new byte[view.size()];
                random.nextBytes(aligned);
                view = view.filtered(aligned, 0x80, M3IndexMaskCompactor.Backend.JAVA);
            }
            int[][] planes = {new int[values.length], new int[values.length]};
            for (int i = 0; i < values.length; i++) {
                planes[0][i] = random.nextInt();
                planes[1][i] = random.nextInt();
            }
            int[] required = {
                trial % 5 == 0 ? Integer.MIN_VALUE : random.nextInt(),
                trial % 7 == 0 ? 0 : random.nextInt()
            };
            int[] forbidden = {
                trial % 9 == 0 ? 0 : random.nextInt(),
                trial % 11 == 0 ? -1 : random.nextInt()
            };
            int step = 1 + random.nextInt(5);
            if ((trial & 1) == 0) step = -step;
            int first = view.size() == 0 ? 0 : random.nextInt(view.size());
            int max = view.size() == 0 ? 0 :
                    (step > 0 ? (view.size() - 1 - first) / step + 1
                              : first / -step + 1);
            int count = random.nextInt(max + 1);
            compare(view, original, planes, required, forbidden, first, count, step,
                    trial < 12);
        }
    }

    private static void compare(M3LongIndexProjection view, long[] physicalValues,
            int[][] stages, int[] required, int[] forbidden,
            int first, int count, int stride, boolean nativeCheck) {
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int physical = view.physicalIndex(first + i * stride);
            boolean keep = true;
            for (int stage = 0; stage < stages.length; stage++) {
                int word = stages[stage][physical];
                if ((word & required[stage]) != required[stage]
                        || (word & forbidden[stage]) != 0) {
                    keep = false;
                    break;
                }
            }
            if (keep) expected.add(physical);
        }
        int[] slots = expected.stream().mapToInt(Integer::intValue).toArray();
        M3LongIndexProjection selected = view.filteredPathMasks(
                stages, required, forbidden, first, count, stride,
                M3IndexPathMasks.Backend.JAVA);
        check(Arrays.equals(slots, selected.physicalIndices()), "physical slot selection");
        check(selected.size() == slots.length, "logical size");
        for (int i = 0; i < slots.length; i++) {
            check(selected.get(i) == physicalValues[slots[i]], "value by original slot");
        }
        M3LongIndexProjection.Order expectedOrder =
                stride >= 0 ? view.order() : switch (view.order()) {
                    case ASCENDING -> M3LongIndexProjection.Order.DESCENDING;
                    case DESCENDING -> M3LongIndexProjection.Order.ASCENDING;
                    case ENCOUNTER -> M3LongIndexProjection.Order.ENCOUNTER;
                };
        check(selected.order() == expectedOrder, "sorted-direction metadata");
        if (selected.size() != 0) {
            int[] escaped = selected.physicalIndices();
            escaped[0] = -1;
            check(selected.physicalIndex(0) == slots[0], "detached index directory");
        }
        if (nativeCheck) {
            if (M3IndexPathMasks.nativeAvailable()) {
                M3LongIndexProjection nativeSelected = view.filteredPathMasks(
                        stages, required, forbidden, first, count, stride,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                check(Arrays.equals(slots, nativeSelected.physicalIndices()),
                        "actual native path parity");
            } else {
                reject(UnsatisfiedLinkError.class, () -> view.filteredPathMasks(
                        stages, required, forbidden, first, count, stride,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            }
        }
    }

    private static void reject(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("wrong failure: " + expected.getName(), failure);
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
>>>>>>> d46d46ab7f20a4cf782e72d353a105f634e10692
