/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/**
 * Source-owned, dependency-free Java 21 differential for paged, masked M3 viewports.
 * Its oracle is the existing full-window selector; it never assumes sorted rows
 * imply unique entries. It optionally compares the existing JNI full-window export.
 */
public final class M3MaskViewportPageContract {
    private static long checks;

    private M3MaskViewportPageContract() { }

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why + " at check " + checks);
    }

    private static void refuses(Class<? extends Throwable> expected, Runnable attempt) {
        checks++;
        try {
            attempt.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError("unexpected error instead of " + expected.getName(), t);
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private static void boundaries(boolean nativeLoaded) {
        int[] offsets = {12, 2, 2, Integer.MAX_VALUE, 0};
        int[] states = {0, Integer.MIN_VALUE, -1, 1, 0};
        int[] out = {77, 77, 77, 77, 77};
        check(M3MaskViewportKernel.projectPage(offsets, states, 0, 5,
                Integer.MIN_VALUE, 0, 1, 1, out, 2) == 1, "one visible row");
        check(Arrays.equals(out, new int[]{77, 77, 2, 77, 77}), "exact output fence");
        int[] untouched = out.clone();
        check(M3MaskViewportKernel.projectPage(offsets, states, 0, 5,
                0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, out, 0) == 0,
                "overflow-safe exhausted rank");
        check(Arrays.equals(untouched, out), "exhausted no write");
        check(M3MaskViewportKernel.projectPage(offsets, states, 0, 5,
                1, 1, 0, 4, out, 0) == 0, "contradictory mask");
        check(M3MaskViewportKernel.projectPage(offsets, states, 0, 5,
                0, 0, 0, 0, out, 0) == 0, "zero demand");
        refuses(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.projectPage(
                offsets, states, 0, 5, 0, 0, 0, 5, out, 1));
        check(Arrays.equals(untouched, out), "capacity refusal preserved all cells");
        refuses(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.projectPage(
                offsets, states, 0, 5, 0, 0, -1, 1, out, 0));
        refuses(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.projectPage(
                offsets, states, 0, 5, 0, 0, 1, -1, out, 0));
        refuses(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.projectPage(
                offsets, states, Integer.MAX_VALUE, 1, 0, 0, 0, 0, out, 0));
        refuses(IllegalArgumentException.class, () -> M3MaskViewportKernel.projectPage(
                new int[]{3, -1}, new int[]{1, 0}, 0, 2, 1, 0, 0, 0, out, 0));
        refuses(IllegalArgumentException.class, () -> M3MaskViewportKernel.projectPage(
                new int[]{3, 4}, new int[]{1}, 0, 1, 1, 0, 0, 0, out, 0));
        refuses(NullPointerException.class, () -> M3MaskViewportKernel.projectPage(
                null, states, 0, 0, 0, 0, 0, 0, out, 0));
        refuses(NullPointerException.class, () -> M3MaskViewportKernel.projectPage(
                offsets, null, 0, 0, 0, 0, 0, 0, out, 0));
        refuses(NullPointerException.class, () -> M3MaskViewportKernel.projectPage(
                offsets, states, 0, 0, 0, 0, 0, 0, null, 0));
        int[] aliasOffsets = {5, 3, 5, 9, 11, 2};
        int[] stateMask = {1, 0, 1, 1, 0, 1};
        check(M3MaskViewportKernel.projectPage(aliasOffsets, stateMask, 0, 6,
                1, 0, 1, 2, aliasOffsets, 3) == 2, "offset alias count");
        check(Arrays.equals(aliasOffsets, new int[]{5, 3, 5, 5, 9, 2}),
                "offset alias overlapping slice preserved");
        int[] aliasState = {1, 0, 1, 1};
        check(M3MaskViewportKernel.projectPage(new int[]{20, 30, 40, 50}, aliasState,
                0, 4, 1, 0, 0, 2, aliasState, 1) == 2, "states alias count");
        check(Arrays.equals(aliasState, new int[]{1, 20, 40, 1}), "states alias captured");
        int[] sameArray = {1, 1, 1, 1};
        check(M3MaskViewportKernel.projectPage(sameArray, sameArray, 0, 4,
                1, 0, 1, 2, sameArray, 2) == 2, "all-three alias count");
        check(Arrays.equals(sameArray, new int[]{1, 1, 1, 1}), "all-three alias contents");
        if (nativeLoaded) {
            int[] nativePage = {77, 77, 77, 77, 77};
            check(M3MaskViewportNative.projectPage(offsets, states, 0, 5,
                    Integer.MIN_VALUE, 0, 1, 1, nativePage, 2) == 1,
                    "native high-bit page count");
            check(Arrays.equals(nativePage, new int[]{77, 77, 2, 77, 77}),
                    "native high-bit result");
            int[] beforePage = nativePage.clone();
            refuses(IllegalArgumentException.class,
                    () -> M3MaskViewportNative.projectPage(new int[]{3, -1},
                            new int[]{1, 0}, 0, 2, 1, 0,
                            0, 0, nativePage, 0));
            check(Arrays.equals(nativePage, beforePage),
                    "native empty page validates and preserves target");
            refuses(IndexOutOfBoundsException.class,
                    () -> M3MaskViewportNative.projectPage(offsets, states,
                            0, 5, 0, 0, 0, 5, nativePage, 1));
            check(Arrays.equals(nativePage, beforePage),
                    "native capacity refusal preserves target");
            int[] nativeAlias = {5, 3, 5, 9, 11, 2};
            check(M3MaskViewportNative.projectPage(nativeAlias, stateMask,
                    0, 6, 1, 0, 1, 2, nativeAlias, 3) == 2,
                    "native aliased page count");
            check(Arrays.equals(nativeAlias, aliasOffsets),
                    "native alias parity");
            int[] stateAlias = {1, 0, 1, 1};
            check(M3MaskViewportNative.projectPage(new int[]{20, 30, 40, 50},
                    stateAlias, 0, 4, 1, 0, 0, 2, stateAlias, 1) == 2,
                    "native state alias count");
            check(Arrays.equals(stateAlias, aliasState), "native state alias parity");
        }
    }

    private static void randomized(boolean nativeLoaded) {
        Random r = new Random(0x6d33566965775061L);
        for (int trial = 0; trial < 40_000; trial++) {
            int size = r.nextInt(240);
            int[] ids = new int[size];
            int[] masks = new int[size];
            for (int i = 0; i < size; i++) {
                ids[i] = r.nextInt(600); // duplicates and pre-permuted sorted/filtered slots
                masks[i] = r.nextInt();
            }
            int first = r.nextInt(size + 1);
            int length = r.nextInt(size - first + 1);
            int required = trial % 17 == 0 ? Integer.MIN_VALUE :
                    trial % 7 == 0 ? 0 : r.nextInt();
            int forbidden = trial % 13 == 0 ? required : r.nextInt();
            int[] all = new int[length];
            int total = M3MaskViewportKernel.project(ids, masks, first, length,
                    required, forbidden, all, 0);
            int skip = trial % 19 == 0 ? Integer.MAX_VALUE : r.nextInt(total + 4);
            int budget = trial % 23 == 0 ? Integer.MAX_VALUE : r.nextInt(20);
            int expected = skip >= total ? 0 : Math.min(total - skip, budget);
            int[] target = new int[expected + 5];
            Arrays.fill(target, -55);
            int[] originalIds = ids.clone(), originalMasks = masks.clone();
            int actual = M3MaskViewportKernel.projectPage(ids, masks, first, length,
                    required, forbidden, skip, budget, target, 2);
            check(actual == expected, "size round " + trial);
            check(target[0] == -55 && target[1] == -55
                    && target[expected + 2] == -55
                    && target[expected + 4] == -55, "output guards round " + trial);
            for (int i = 0; i < expected; i++) {
                check(target[i + 2] == all[skip + i], "order round " + trial + " row " + i);
            }
            check(Arrays.equals(ids, originalIds) && Arrays.equals(masks, originalMasks),
                    "source immutable round " + trial);
            if (nativeLoaded) {
                int[] nativeFull = new int[length];
                int count = M3MaskViewportNative.project(ids, masks, first, length,
                        required, forbidden, nativeFull, 0);
                check(count == total && Arrays.equals(Arrays.copyOf(nativeFull, count),
                        Arrays.copyOf(all, total)), "existing JNI full-window parity " + trial);
                for (int i = 0; i < expected; i++) {
                    check(target[i + 2] == nativeFull[skip + i],
                            "JNI-selected page parity " + trial);
                }
                int[] nativePage = new int[expected + 5];
                Arrays.fill(nativePage, -55);
                check(M3MaskViewportNative.projectPage(ids, masks, first,
                        length, required, forbidden, skip, budget, nativePage, 2)
                        == expected, "native page count " + trial);
                check(Arrays.equals(nativePage, target),
                        "native/Java visible page parity " + trial);
            }
        }
    }

    public static void main(String[] args) {
        boolean nativeLoaded = args.length > 0 && "--native".equals(args[0]);
        if (nativeLoaded) {
            String path = System.getProperty("m3.native.path");
            if (path == null) throw new IllegalArgumentException("m3.native.path required");
            System.load(path);
        }
        boundaries(nativeLoaded);
        randomized(nativeLoaded);
        System.out.println("M3_MASK_VIEWPORT_PAGE_PASS checks=" + checks
                + " existingJni=" + nativeLoaded);
    }
}
