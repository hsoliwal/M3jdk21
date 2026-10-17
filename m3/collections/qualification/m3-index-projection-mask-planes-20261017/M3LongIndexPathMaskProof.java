// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Standalone Java21/real-JNI differential proof over the EXACT product classes. */
public final class M3LongIndexPathMaskProof {
    private static long checks;
    private M3LongIndexPathMaskProof() { }

    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why + " check=" + checks);
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable work) {
        try {
            work.run();
        } catch (Throwable caught) {
            check(type.isInstance(caught), "wrong exception " + caught);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static boolean matches(int[][] planes, int[] required, int[] forbidden, int physical) {
        for (int stage = 0; stage < planes.length; stage++) {
            int state = planes[stage][physical];
            if ((state & required[stage]) != required[stage]
                    || (state & forbidden[stage]) != 0) return false;
        }
        return true;
    }

    private static void verify(M3LongIndexProjection view,
            int[][] planes, int[] required, int[] forbidden, boolean nativeRequired) {
        int[] expected = new int[view.size()];
        int count = 0;
        int first = -1;
        for (int ordinal = 0; ordinal < view.size(); ordinal++) {
            int physical = view.physicalIndex(ordinal);
            if (!matches(planes, required, forbidden, physical)) continue;
            if (first == -1) first = ordinal;
            expected[count++] = physical;
        }
        expected = Arrays.copyOf(expected, count);
        int observedFirst = view.firstMatchingOrdinalByStatePlanes(planes, required, forbidden);
        check(observedFirst == first, "first matching ordinal is relative to slice");
        M3LongIndexProjection chosen = view.filteredByStatePlanes(planes, required, forbidden,
                M3IndexPathMasks.Backend.JAVA);
        check(chosen.order() == view.order(), "order metadata unchanged");
        check(Arrays.equals(expected, chosen.physicalIndices()), "stable physical permutation");
        for (int ordinal = 0; ordinal < chosen.size(); ordinal++) {
            int physical = expected[ordinal];
            check(chosen.physicalIndex(ordinal) == physical, "physical slot");
            check(chosen.get(ordinal) == viewAtPhysical(view, physical), "value owner");
        }
        if (nativeRequired) {
            check(M3IndexPathMasks.nativeAvailable(), "expected checked JNI provider");
            M3LongIndexProjection jni = view.filteredByStatePlanes(planes, required, forbidden,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            check(Arrays.equals(chosen.physicalIndices(), jni.physicalIndices()), "real JNI parity");
            check(Arrays.equals(chosen.toArray(), jni.toArray()), "real JNI values");
        } else if (!M3IndexPathMasks.nativeAvailable()) {
            throwsType(UnsatisfiedLinkError.class, () -> view.filteredByStatePlanes(
                    planes, required, forbidden, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }

    private static long viewAtPhysical(M3LongIndexProjection view, int slot) {
        for (int i = 0; i < view.size(); i++) {
            if (view.physicalIndex(i) == slot) return view.get(i);
        }
        throw new AssertionError("physical index missing in source view");
    }

    public static void main(String[] arguments) {
        boolean realJni = arguments.length != 0 && arguments[0].equals("jni");
        Random random = new Random(0x4d33494e4445585aL);
        for (int trial = 0; trial < 3_000; trial++) {
            int n = random.nextInt(65);
            long[] data = new long[n];
            for (int i = 0; i < n; i++) {
                data[i] = switch (random.nextInt(7)) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> random.nextInt(17) - 8L;
                };
            }
            M3LongIndexProjection root = M3LongIndexProjection.snapshot(data);
            M3LongIndexProjection sorted = switch (trial % 3) {
                case 0 -> root;
                case 1 -> root.sortedAscending();
                default -> root.sortedDescending();
            };
            int from = random.nextInt(n + 1);
            int length = random.nextInt(n - from + 1);
            M3LongIndexProjection view = sorted.slice(from, length);
            int stages = 1 + random.nextInt(4);
            int[][] planes = new int[stages][n];
            int[] needs = new int[stages], denies = new int[stages];
            for (int stage = 0; stage < stages; stage++) {
                for (int i = 0; i < n; i++) planes[stage][i] = random.nextInt();
                needs[stage] = 1 << random.nextInt(32);
                denies[stage] = 1 << random.nextInt(32);
            }
            verify(view, planes, needs, denies, realJni && trial < 300);
            if (n != 0) {
                int physical = root.physicalIndex(0);
                data[0] ^= -1L;
                check(root.get(0) != data[physical], "immutable canonical payload");
            }
        }

        M3LongIndexProjection root = M3LongIndexProjection.snapshot(
                new long[] {10, 20, 20, 5, 40, 20});
        int[][] plane = {{0, 1, 1, 0, 1, 1}};
        int[] keep = {1}, reject = {0};
        verify(root.sortedAscending().slice(1, 4), plane, keep, reject, realJni);
        verify(root.sortedDescending(), plane, keep, reject, realJni);
        verify(root.slice(6, 0), plane, keep, reject, realJni);
        throwsType(IndexOutOfBoundsException.class, () -> root.filteredByStatePlanes(
                new int[][] {new int[5]}, keep, reject, M3IndexPathMasks.Backend.JAVA));
        throwsType(NullPointerException.class, () -> root.filteredByStatePlanes(
                null, keep, reject, M3IndexPathMasks.Backend.JAVA));
        throwsType(IllegalArgumentException.class, () -> root.filteredByStatePlanes(
                plane, new int[0], reject, M3IndexPathMasks.Backend.JAVA));
        int[][] every = {{-1, -1, -1, -1, -1, -1}};
        M3LongIndexProjection unchanged = root.filteredByStatePlanes(
                every, keep, reject, M3IndexPathMasks.Backend.JAVA);
        check(unchanged == root, "full-selection identity and immutable backing reuse");
        System.out.println("M3_LONG_PROJECTION_PATH_MASK_PASS checks=" + checks
                + " real_jni=" + realJni);
    }
}
