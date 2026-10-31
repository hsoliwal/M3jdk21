// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Executable JDK21 differential oracle for prevalidated 32-bit zero-state path masks. */
public final class M3IndexPathZeroStagesSelfTest {
    private static long checks;
    private static final long SEED = 0x4d335a45524f5041L;
    private M3IndexPathZeroStagesSelfTest() { }

    private static void ok(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
    private static void equal(int expected, int actual, String reason) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(reason + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void equal(int[] expected, int[] actual, String reason) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(reason + ": expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(actual));
        }
    }
    private static void refusal(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("unexpected failure: " + error, error);
        }
        throw new AssertionError("did not refuse " + type.getSimpleName());
    }
    private static boolean admits(int[][] planes, int[] need, int[] deny, int slot) {
        for (int j = 0; j < planes.length; j++) {
            int state = planes[j][slot];
            if ((state & need[j]) != need[j] || (state & deny[j]) != 0) return false;
        }
        return true;
    }
    private static void compare(int[] rows, int[] offsets, int[][] planes,
            int[] need, int[] deny, int first, int count, int stride,
            boolean nativeMode, String scope) {
        var reference = new ArrayList<Integer>();
        int firstAccepted = -1;
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int position = (int) ((long) first + (long) ordinal * stride);
            if (admits(planes, need, deny, offsets[position])) {
                if (firstAccepted < 0) firstAccepted = position;
                reference.add(rows[position]);
            }
        }
        int[] expected = new int[reference.size()];
        for (int i = 0; i < expected.length; i++) expected[i] = reference.get(i);
        int[] oldRows = rows.clone();
        int[] oldOffsets = offsets.clone();
        int[][] oldPlanes = Arrays.stream(planes).map(int[]::clone).toArray(int[][]::new);
        int[] actual = M3IndexPathMasks.select(rows, offsets, planes,
                need, deny, first, count, stride, M3IndexPathMasks.Backend.JAVA);
        equal(expected, actual, scope + " stable path rows");
        equal(firstAccepted, M3IndexPathMasks.firstMatchingIndex(
                rows, offsets, planes, need, deny, first, count, stride),
                scope + " first path ordinal");
        equal(oldRows, rows, scope + " source row IDs untouched");
        equal(oldOffsets, offsets, scope + " aligned physical offsets untouched");
        for (int i = 0; i < planes.length; i++) {
            equal(oldPlanes[i], planes[i], scope + " state word plane " + i);
        }
        if (actual.length > 0) {
            actual[0] = Integer.MIN_VALUE;
            equal(oldRows, rows, scope + " output is always detached");
        }
        if (nativeMode) {
            int[] nativeSelected = M3IndexPathMasks.select(rows, offsets, planes,
                    need, deny, first, count, stride,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED);
            equal(expected, nativeSelected, scope + " JNI original kernel parity");
        }
    }
    private static void boundaryCases(boolean nativeMode) {
        int[] rows = {Integer.MIN_VALUE, 9, 9, 0, Integer.MAX_VALUE, -7};
        int[] positions = {2, 0, 2, 1, 0, 1};
        int[][] planes = {{0, Integer.MIN_VALUE, -1}, {Integer.MAX_VALUE, 0, 0x80000001}};
        compare(rows, positions, planes, new int[] {0, 0}, new int[] {0, 0},
                4, 5, -1, nativeMode, "reverse all-zero");
        compare(rows, positions, planes, new int[] {0, 0}, new int[] {0, 0},
                0, 3, 2, nativeMode, "stride positive all-zero");
        compare(rows, positions, planes, new int[] {0, 0}, new int[] {0, 0},
                rows.length, 0, -1, nativeMode, "empty terminal");
        compare(rows, positions, planes, new int[] {0, 0}, new int[] {0, 0},
                0, 1, Integer.MIN_VALUE, nativeMode, "singleton extreme stride");
        compare(rows, positions, planes, new int[] {0, Integer.MIN_VALUE},
                new int[] {0, 0}, 0, 6, 1, nativeMode, "required high bit");
        compare(rows, positions, planes, new int[] {0, 0},
                new int[] {Integer.MIN_VALUE, 0}, 0, 6, 1,
                nativeMode, "forbidden high bit not all pass");
        compare(rows, positions, planes, new int[] {1, 0},
                new int[] {1, 0}, 5, 6, -1, nativeMode, "overlapping masks reject");
        refusal(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.select(rows, new int[] {2, 0, 2, 1, 0, 3},
                        planes, new int[] {0, 0}, new int[] {0, 0},
                        0, 6, 1, M3IndexPathMasks.Backend.JAVA));
        refusal(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.firstMatchingIndex(rows,
                        new int[] {2, 0, 2, 1, 0, 3}, planes,
                        new int[] {0, 0}, new int[] {0, 0}, 0, 6, 1));
        refusal(IllegalArgumentException.class,
                () -> M3IndexPathMasks.select(rows, positions, new int[0][],
                        new int[0], new int[0], 0, 6, 1, M3IndexPathMasks.Backend.JAVA));
        refusal(IllegalArgumentException.class,
                () -> M3IndexPathMasks.select(rows, positions, planes,
                        new int[] {0}, new int[] {0}, 0, 6, 1,
                        M3IndexPathMasks.Backend.JAVA));
        refusal(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.select(rows, positions, planes,
                        new int[] {0, 0}, new int[] {0, 0},
                        0, 6, 0, M3IndexPathMasks.Backend.JAVA));
        refusal(NullPointerException.class,
                () -> M3IndexPathMasks.select(rows, positions, planes,
                        new int[] {0, 0}, new int[] {0, 0},
                        0, 6, 1, null));
        if (!nativeMode && !M3IndexPathMasks.nativeAvailable()) {
            refusal(UnsatisfiedLinkError.class,
                    () -> M3IndexPathMasks.select(rows, positions, planes,
                            new int[] {0, 0}, new int[] {0, 0}, 0, 6, 1,
                            M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        }
    }
    private static void randomized(boolean nativeMode) {
        Random random = new Random(SEED);
        for (int trial = 0; trial < 1536; trial++) {
            int length = random.nextInt(130);
            int[] rows = new int[length];
            int[] offsets = new int[length];
            int nplanes = 1 + random.nextInt(5);
            int physicalSlots = 1 + random.nextInt(75);
            int[][] planes = new int[nplanes][physicalSlots];
            int[] required = new int[nplanes], forbidden = new int[nplanes];
            for (int i = 0; i < length; i++) {
                rows[i] = switch (i % 17) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    default -> random.nextInt(41) - 20;
                };
                offsets[i] = random.nextInt(physicalSlots);
            }
            for (int i = 0; i < nplanes; i++) {
                for (int j = 0; j < physicalSlots; j++) planes[i][j] = random.nextInt();
                if ((trial & 3) != 0) {
                    required[i] = switch (trial % 13) {
                        case 0 -> 0;
                        case 1 -> Integer.MIN_VALUE;
                        default -> random.nextInt();
                    };
                    forbidden[i] = random.nextInt();
                }
            }
            // Include zero-length paths and negative strides, avoiding invalid geometry.
            int count = length == 0 ? 0 : random.nextInt(length + 1);
            int stride = (trial & 1) == 0 ? 1 : -1;
            int first = count == 0 ? length
                    : stride == 1 ? random.nextInt(length - count + 1)
                            : count - 1 + random.nextInt(length - count + 1);
            compare(rows, offsets, planes, required, forbidden,
                    first, count, stride, nativeMode, "seeded " + trial);
        }
    }
    public static void main(String[] args) {
        boolean nativeMode = args.length == 1 && "--native".equals(args[0]);
        if (args.length > 1 || (args.length == 1 && !nativeMode)) {
            throw new IllegalArgumentException("use no args or --native");
        }
        if (nativeMode) ok(M3IndexPathMasks.nativeAvailable(), "real JNI library required");
        boundaryCases(nativeMode);
        randomized(nativeMode);
        System.out.println("M3_ZERO_STAGES_PATH_MASK_PASS checks=" + checks
                + " JNI=" + nativeMode);
    }
}
