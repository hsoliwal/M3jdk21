// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Differential and allocation-shape oracle over immutable index/offset/mask projections. */
public final class M3LongIndexProjectionMaskOffsetProof {
    private static long checks;
    private static final long SEED = 0x4d33494e44455831L;
    private M3LongIndexProjectionMaskOffsetProof() { }

    private static void ok(boolean result, String reason) {
        checks++;
        if (!result) throw new AssertionError(reason);
    }
    private static void eq(int[] expected, int[] actual, String label) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(label + ": " + Arrays.toString(expected)
                    + " != " + Arrays.toString(actual));
        }
    }
    private static void refuse(Class<? extends Throwable> expected, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError("unexpected exception", failure);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }
    private static Object indexDirectory(M3LongIndexProjection view) {
        try {
            Field field = M3LongIndexProjection.class.getDeclaredField("indices");
            field.setAccessible(true);
            return field.get(view);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static void compare(M3LongIndexProjection view, byte[] states,
            int mask, String scope) {
        int[] physical = view.physicalIndices();
        byte[] originalStates = states.clone();
        int count = 0;
        for (byte bits : states) if (((bits & 255) & mask) == mask) count++;
        int[] expected = new int[count];
        int written = 0;
        for (int i = 0; i < physical.length; i++) {
            if (((states[i] & 255) & mask) == mask) expected[written++] = physical[i];
        }
        M3LongIndexProjection actual = view.filtered(states, mask,
                M3IndexMaskCompactor.Backend.JAVA);
        eq(expected, actual.physicalIndices(), scope + " exact physical slot order");
        ok(actual.order() == view.order(), scope + " ordering metadata");
        ok(actual.size() == expected.length, scope + " selected count");
        ok(Arrays.equals(originalStates, states), scope + " caller states unchanged");
        ok((indexDirectory(actual) == indexDirectory(view)) == (count == physical.length),
                scope + " private index directory reused iff all selected");
        for (int i = 0; i < expected.length; i++) {
            int ordinal = -1;
            for (int j = 0; j < physical.length; j++) {
                if (physical[j] == expected[i]) { ordinal = j; break; }
            }
            ok(ordinal >= 0 && actual.get(i) == view.get(ordinal),
                    scope + " value matches original physical slot");
        }
        int[] escaped = actual.physicalIndices();
        if (escaped.length > 0) {
            int first = actual.physicalIndex(0);
            escaped[0] = -1;
            ok(actual.physicalIndex(0) == first,
                    scope + " returned index vector never aliases internal directory");
        }
        // An explicit native request may never fall back to the Java path.
        if (M3IndexMaskCompactor.nativeAvailable()) {
            M3LongIndexProjection nativeView = view.filtered(states, mask,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            eq(actual.physicalIndices(), nativeView.physicalIndices(), scope + " actual JNI");
        } else {
            refuse(UnsatisfiedLinkError.class, () -> view.filtered(states, mask,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
    }
    private static void fixedCases() {
        long[] values = {Long.MIN_VALUE, 5, 5, 0, Long.MAX_VALUE, 5, -7};
        M3LongIndexProjection root = M3LongIndexProjection.snapshot(values);
        Arrays.fill(values, 11L);
        M3LongIndexProjection slice = root.sortedDescending().slice(1, 5);
        compare(slice, new byte[] {0, -1, -1, 0, 0}, 0x80, "high-bit sliced");
        compare(slice, new byte[] {-1, -1, -1, -1, -1}, 0xff, "all selected sliced");
        compare(slice, new byte[5], 0, "mask zero always selects all");
        compare(slice, new byte[5], 0xff, "no selected rows");
        compare(root.slice(6, 0), new byte[0], 0xff, "empty tail window");
        refuse(IllegalArgumentException.class, () -> slice.filtered(new byte[4], 0,
                M3IndexMaskCompactor.Backend.JAVA));
        refuse(IllegalArgumentException.class, () -> slice.filtered(new byte[5], -1,
                M3IndexMaskCompactor.Backend.JAVA));
        refuse(IllegalArgumentException.class, () -> slice.filtered(new byte[5], 256,
                M3IndexMaskCompactor.Backend.JAVA));
        refuse(NullPointerException.class, () -> slice.filtered(null, 0,
                M3IndexMaskCompactor.Backend.JAVA));
        refuse(NullPointerException.class, () -> slice.filtered(new byte[5], 0, null));
    }
    private static void comparatorAndStableTieOracles() {
        long[] values = {3, -4, 3, 3, Long.MIN_VALUE, Long.MAX_VALUE, -4};
        M3LongIndexProjection source = M3LongIndexProjection.snapshot(values);
        for (boolean desc : new boolean[] {false, true}) {
            M3LongIndexProjection sorted = desc ? source.sortedDescending()
                    : source.sortedAscending();
            List<Integer> expected = new ArrayList<>();
            for (int i = 0; i < values.length; i++) expected.add(i);
            Comparator<Integer> cmp = Comparator.comparingLong(i -> values[i]);
            if (desc) cmp = cmp.reversed();
            expected.sort(cmp);
            for (int i = 0; i < values.length; i++) {
                ok(sorted.physicalIndex(i) == expected.get(i), "signed sort and stable ties");
            }
        }
    }
    private static void differentialSeededWindows() {
        Random rng = new Random(SEED);
        for (int trial = 0; trial < 2048; trial++) {
            long[] values = new long[rng.nextInt(170)];
            for (int i = 0; i < values.length; i++) {
                values[i] = switch (i % 17) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    default -> rng.nextInt(31) - 15L;
                };
            }
            M3LongIndexProjection view = M3LongIndexProjection.snapshot(values);
            if (trial % 3 == 0) view = view.sortedAscending();
            if (trial % 3 == 1) view = view.sortedDescending();
            int from = rng.nextInt(view.size() + 1);
            view = view.slice(from, rng.nextInt(view.size() - from + 1));
            byte[] states = new byte[view.size()];
            rng.nextBytes(states);
            int required = switch (trial % 7) {
                case 0 -> 0;
                case 1 -> 255;
                case 2 -> 128;
                case 3 -> 0x55;
                default -> rng.nextInt(256);
            };
            if (trial % 9 == 0) Arrays.fill(states, (byte) 0xff);
            compare(view, states, required, "random trial " + trial);
        }
    }
    public static void main(String[] args) {
        fixedCases();
        comparatorAndStableTieOracles();
        differentialSeededWindows();
        System.out.println("M3_MASK_OFFSET_PROJECTION_PASS checks=" + checks + " seed=" + SEED);
    }
}
