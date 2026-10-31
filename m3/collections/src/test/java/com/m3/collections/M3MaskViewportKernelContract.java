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

/** Executable Java 21 contract oracle, with optional mandatory JNI comparison. */
public final class M3MaskViewportKernelContract {
    private static int checks;

    private M3MaskViewportKernelContract() {
    }

    public static void main(final String[] args) {
        final String lib = System.getProperty("m3.native.path");
        if (lib != null) {
            System.load(lib);
        }
        boundaries(lib != null);
        randomized(lib != null);
        System.out.println("PASS checks=" + checks + " jni=" + (lib != null));
    }

    private static void boundaries(final boolean nativeLoaded) {
        final int[] offsets = {12, 2, 2, Integer.MAX_VALUE, 0};
        final int[] masks = {0, Integer.MIN_VALUE, -1, 1, 0};
        final int[] out = {99, 99, 99, 99, 99, 99, 99};
        check(M3MaskViewportKernel.project(offsets, masks, 0, 5,
                Integer.MIN_VALUE, 0, out, 2) == 2, "full-width required");
        check(Arrays.equals(out, new int[]{99, 99, 2, 2, 99, 99, 99}),
                "stable duplicates and arbitrary destination offset");
        check(M3MaskViewportKernel.count(masks, 0, 5,
                Integer.MIN_VALUE, 1) == 1, "exclude full-width combination");
        check(M3MaskViewportKernel.atRank(offsets, masks, 0, 5,
                Integer.MIN_VALUE, 0, 1) == 2, "rank on duplicated offsets");
        check(M3MaskViewportKernel.count(masks, 0, 5, 1, 1) == 0,
                "contradictory required/forbidden bits");
        final int[] alias = {11, 22, 33, 44};
        final int[] aliasMasks = {1, 0, 1, 1};
        check(M3MaskViewportKernel.project(alias, aliasMasks, 0, 4,
                1, 0, alias, 1) == 3, "overlapping alias count");
        check(Arrays.equals(alias, new int[]{11, 11, 33, 44}),
                "alias snapshot before write");
        if (nativeLoaded) {
            final int[] nativeAlias = {11, 22, 33, 44};
            check(M3MaskViewportNative.project(nativeAlias, aliasMasks, 0, 4,
                    1, 0, nativeAlias, 1) == 3, "JNI alias count");
            check(Arrays.equals(alias, nativeAlias), "JNI alias parity");
        }
        final int[] unchanged = Arrays.copyOf(out, out.length);
        expect(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.project(
                offsets, masks, 0, 5, 0, 0, out, 3));
        check(Arrays.equals(out, unchanged), "capacity refusal did not mutate");
        expect(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.project(
                offsets, masks, Integer.MAX_VALUE, 2, 0, 0, out, 0));
        expect(IllegalArgumentException.class, () -> M3MaskViewportKernel.project(
                new int[]{1, -1}, new int[]{1, 1}, 0, 2, 1, 0, out, 0));
        expect(IllegalArgumentException.class, () -> M3MaskViewportKernel.project(
                new int[]{1, 2}, new int[]{1}, 0, 1, 1, 0, out, 0));
        expect(NullPointerException.class, () -> M3MaskViewportKernel.project(
                null, masks, 0, 0, 0, 0, out, 0));
        expect(IndexOutOfBoundsException.class, () -> M3MaskViewportKernel.atRank(
                offsets, masks, 0, 5, 0, 0, 5));
        if (nativeLoaded) {
            expect(IndexOutOfBoundsException.class, () -> M3MaskViewportNative.project(
                    offsets, masks, Integer.MAX_VALUE, 2, 0, 0, out, 0));
            expect(IllegalArgumentException.class, () -> M3MaskViewportNative.project(
                    new int[]{1, -1}, new int[]{1, 1}, 0, 2, 1, 0, out, 0));
            expect(IllegalArgumentException.class, () -> M3MaskViewportNative.project(
                    new int[]{1, 2}, new int[]{1}, 0, 1, 1, 0, out, 0));
            expect(NullPointerException.class, () -> M3MaskViewportNative.project(
                    null, masks, 0, 0, 0, 0, out, 0));
            check(Arrays.equals(out, unchanged), "JNI refusal did not mutate");
        }
    }

    private static void randomized(final boolean nativeLoaded) {
        final Random random = new Random(0x53ca1eL);
        for (int trial = 0; trial < 25000; trial++) {
            final int size = random.nextInt(200);
            final int[] offsets = new int[size];
            final int[] masks = new int[size];
            for (int i = 0; i < size; i++) {
                offsets[i] = random.nextInt(1000);
                masks[i] = random.nextInt();
            }
            final int first = random.nextInt(size + 1);
            final int length = random.nextInt(size - first + 1);
            final int required = trial % 7 == 0 ? Integer.MIN_VALUE :
                    trial % 5 == 0 ? 0 : random.nextInt();
            final int forbidden = trial % 11 == 0 ? required : random.nextInt();
            final List<Integer> reference = new ArrayList<>();
            for (int i = first; i < first + length; i++) {
                if ((masks[i] & required) == required
                        && (masks[i] & forbidden) == 0) {
                    reference.add(offsets[i]);
                }
            }
            final int[] javaOut = new int[reference.size() + 4];
            Arrays.fill(javaOut, -9);
            check(M3MaskViewportKernel.count(masks, first, length,
                    required, forbidden) == reference.size(), "count differential");
            check(M3MaskViewportKernel.project(offsets, masks, first, length,
                    required, forbidden, javaOut, 2) == reference.size(),
                    "project count differential");
            check(javaOut[0] == -9 && javaOut[1] == -9
                    && javaOut[javaOut.length - 1] == -9, "destination fences");
            for (int i = 0; i < reference.size(); i++) {
                check(javaOut[i + 2] == reference.get(i), "stable differential");
                check(M3MaskViewportKernel.atRank(offsets, masks, first,
                        length, required, forbidden, i) == reference.get(i),
                        "rank differential");
            }
            if (nativeLoaded) {
                final int[] nativeOut = new int[javaOut.length];
                Arrays.fill(nativeOut, -9);
                check(M3MaskViewportNative.project(offsets, masks, first,
                        length, required, forbidden, nativeOut, 2)
                        == reference.size(), "native count differential");
                check(Arrays.equals(nativeOut, javaOut), "native values differential");
            }
        }
    }

    private static void check(final boolean condition, final String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void expect(final Class<? extends Throwable> type,
            final Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (final Throwable throwable) {
            if (type.isInstance(throwable)) {
                return;
            }
            throw new AssertionError("expected " + type.getSimpleName(), throwable);
        }
        throw new AssertionError("missing " + type.getSimpleName());
    }
}
