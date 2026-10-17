/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Bounded Java/JNI differential and input-boundary proof for detached ordered row indexes. */
public final class M3IndexMaskCompactorSelfTest {
    private static long checks;

    private M3IndexMaskCompactorSelfTest() {
    }

    public static void main(String[] args) {
        boolean requireNative = args.length == 1 && "--native".equals(args[0]);
        if (requireNative && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("actual JNI library required by qualification test");
        }
        boundaryContract(requireNative);
        compositionPreservesOrder(requireNative);
        allMasksAndLengths(requireNative);
        windowContracts(requireNative);
        randomizedDifferential(requireNative);
        primitiveOwnersAndLongIds();
        System.out.println("PASS M3IndexMaskCompactorSelfTest checks=" + checks
                + " JNI=" + requireNative);
    }

    private static void boundaryContract(boolean nativeRun) {
        M3IndexMaskCompactor.Backend java = M3IndexMaskCompactor.Backend.JAVA;
        int[] source = {10, -17, 10, 44};
        byte[] states = {1, (byte) 0xff, 2, 0};
        int[] all = M3IndexMaskCompactor.select(source, states, 0, java);
        eq(source, all);
        ok(all != source);
        eq(new int[] {-17}, M3IndexMaskCompactor.select(source, states, 255, java));
        eq(new int[] {10, -17}, M3IndexMaskCompactor.select(source, states, 1, java));
        eq(new int[] {}, M3IndexMaskCompactor.select(new int[0], new byte[0], 128, java));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.select(null, states, 0, java));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.select(source, null, 0, java));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.select(source, states, 0, null));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.select(source, new byte[3], 0, java));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.select(source, states, -1, java));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.select(source, states, 256, java));
        if (nativeRun) {
            M3IndexMaskCompactor.Backend nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
            eq(new int[] {-17}, M3IndexMaskCompactor.select(source, states, 255, nativeBackend));
            eq(source, M3IndexMaskCompactor.select(source, states, 0, nativeBackend));
        }
    }

    private static void compositionPreservesOrder(boolean nativeRun) {
        int[] order = {9, 3, 7, 3, 0}; // e.g. a sorted/reversed/indexed upstream view
        byte[] states = {2, 3, 0, 2, 3};
        int[] expected = {9, 3, 3, 0};
        eq(expected, M3IndexMaskCompactor.select(order, states, 2, M3IndexMaskCompactor.Backend.JAVA));
        if (nativeRun) {
            eq(expected, M3IndexMaskCompactor.select(order, states, 2,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        eq(new int[] {9, 3, 7, 3, 0}, order);
    }

    private static void allMasksAndLengths(boolean nativeRun) {
        for (int n : new int[] {0, 1, 2, 3, 7, 31, 255, 256, 257, 511, 512, 513, 1025}) {
            int[] rows = new int[n];
            byte[] states = new byte[n];
            for (int i = 0; i < n; i++) {
                rows[i] = i % 9 - 4;
                states[i] = (byte) ((i * 101 + (i >>> 3)) & 255);
            }
            for (int mask = 0; mask < 256; mask++) {
                int[] expected = oracle(rows, states, mask);
                eq(expected, M3IndexMaskCompactor.select(rows, states, mask,
                        M3IndexMaskCompactor.Backend.JAVA));
                if (nativeRun) {
                    eq(expected, M3IndexMaskCompactor.select(rows, states, mask,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                }
            }
        }
    }

    private static void windowContracts(boolean nativeRun) {
        M3IndexMaskCompactor.Backend[] backends = nativeRun
                ? new M3IndexMaskCompactor.Backend[] {M3IndexMaskCompactor.Backend.JAVA,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED}
                : new M3IndexMaskCompactor.Backend[] {M3IndexMaskCompactor.Backend.JAVA};
        for (M3IndexMaskCompactor.Backend backend : backends) {
            for (int length : new int[] {0, 1, 255, 256, 257}) {
                zeroMaskWindow(length, 0, 0, backend);
                zeroMaskWindow(length, 3, 5, backend);
                zeroMaskWindow(length, 5, 3, backend);
            }
            unsignedWindowMasks(backend);
            windowValidation(backend);
        }
        if (!M3IndexMaskCompactor.nativeAvailable()) {
            fails(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectWindow(
                    new int[0], 0, new byte[0], 0, 0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            fails(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.selectLongWindow(
                    new long[0], 0, new byte[0], 0, 0, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            windowValidation(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
        }
    }

    /** All-zero states discriminate zero-mask identity from an accidental nonzero mask. */
    private static void zeroMaskWindow(int length, int rowOffset, int stateOffset,
            M3IndexMaskCompactor.Backend backend) {
        int[] rows = new int[rowOffset + length + 4];
        long[] longRows = new long[rowOffset + length + 4];
        byte[] states = new byte[stateOffset + length + 4];
        Arrays.fill(rows, 0x6b6b6b6b);
        Arrays.fill(longRows, 0x6b6b6b6b6b6b6b6bL);
        Arrays.fill(states, (byte) 0xff);
        Arrays.fill(states, stateOffset, stateOffset + length, (byte) 0);
        for (int i = 0; i < length; i++) {
            rows[rowOffset + i] = switch (i % 5) {
                case 0, 3 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> 0;
                default -> -7;
            };
            longRows[rowOffset + i] = switch (i % 5) {
                case 0, 3 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> 0L;
                default -> -7L;
            };
        }
        int[] originalRows = rows.clone();
        long[] originalLongRows = longRows.clone();
        byte[] originalStates = states.clone();
        int[] expected = Arrays.copyOfRange(rows, rowOffset, rowOffset + length);
        long[] expectedLong = Arrays.copyOfRange(longRows, rowOffset, rowOffset + length);
        int[] selected = M3IndexMaskCompactor.selectWindow(
                rows, rowOffset, states, stateOffset, length, 0, backend);
        long[] selectedLong = M3IndexMaskCompactor.selectLongWindow(
                longRows, rowOffset, states, stateOffset, length, 0, backend);
        eq(expected, selected);
        eq(expectedLong, selectedLong);
        ok(selected != rows);
        ok(selectedLong != longRows);
        eq(originalRows, rows); // Prefix/suffix canaries and selected slots are unchanged.
        eq(originalLongRows, longRows);
        ok(Arrays.equals(originalStates, states));
        eq(new int[0], M3IndexMaskCompactor.selectWindow(
                rows, rowOffset, states, stateOffset, length, 255, backend));
        eq(new long[0], M3IndexMaskCompactor.selectLongWindow(
                longRows, rowOffset, states, stateOffset, length, 255, backend));
        if (length != 0) {
            selected[0] ^= 1;
            selectedLong[0] ^= 1L;
            eq(originalRows, rows);
            eq(originalLongRows, longRows);
            selected[0] = expected[0];
            selectedLong[0] = expectedLong[0];
            rows[rowOffset] ^= 1;
            longRows[rowOffset] ^= 1L;
            eq(expected, selected);
            eq(expectedLong, selectedLong);
        }
        eq(new int[0], M3IndexMaskCompactor.selectWindow(
                rows, rows.length, states, states.length, 0, 0, backend));
        eq(new long[0], M3IndexMaskCompactor.selectLongWindow(
                longRows, longRows.length, states, states.length, 0, 0, backend));
    }

    private static void unsignedWindowMasks(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {91, Integer.MIN_VALUE, 7, Integer.MIN_VALUE, Integer.MAX_VALUE, 92};
        long[] longRows = {91, Long.MIN_VALUE, 7, Long.MIN_VALUE, Long.MAX_VALUE, 92};
        byte[] states = {0, 0, (byte) 0x80, (byte) 0xff, 0x7f, (byte) 0x80, 0};
        eq(new int[] {Integer.MIN_VALUE, 7, Integer.MAX_VALUE},
                M3IndexMaskCompactor.selectWindow(rows, 1, states, 2, 4, 128, backend));
        eq(new long[] {Long.MIN_VALUE, 7, Long.MAX_VALUE},
                M3IndexMaskCompactor.selectLongWindow(longRows, 1, states, 2, 4, 128, backend));
        eq(new int[] {7}, M3IndexMaskCompactor.selectWindow(rows, 1, states, 2, 4, 255, backend));
        eq(new long[] {7}, M3IndexMaskCompactor.selectLongWindow(longRows, 1, states, 2, 4, 255, backend));
        eq(new int[] {7, Integer.MIN_VALUE},
                M3IndexMaskCompactor.selectWindow(rows, 1, states, 2, 4, 127, backend));
        eq(new long[] {7, Long.MIN_VALUE},
                M3IndexMaskCompactor.selectLongWindow(longRows, 1, states, 2, 4, 127, backend));
    }

    private static void windowValidation(M3IndexMaskCompactor.Backend backend) {
        int[] rows = {11, 22};
        long[] longRows = {11, 22};
        byte[] states = {0, 0};
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectWindow(null, -1, states, -1, -1, -1, backend));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(null, -1, states, -1, -1, -1, backend));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, null, -1, -1, -1, backend));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longRows, -1, null, -1, -1, -1, backend));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, states, -1, -1, -1, null));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longRows, -1, states, -1, -1, -1, null));
        for (int[] window : new int[][] {{-1, 0, 0}, {0, -1, 0}, {0, 0, -1},
                {3, 0, 0}, {0, 3, 0}, {1, 0, 2}, {0, 1, 2},
                {Integer.MAX_VALUE, 0, 1}, {0, Integer.MAX_VALUE, 1},
                {1, 1, Integer.MAX_VALUE}}) {
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                    rows, window[0], states, window[1], window[2], 0, backend));
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                    longRows, window[0], states, window[1], window[2], 0, backend));
        }
        // Bounds remain prior to mask validation, even for explicit native requests.
        fails(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectWindow(rows, -1, states, 0, 0, 256, backend));
        fails(IndexOutOfBoundsException.class,
                () -> M3IndexMaskCompactor.selectLongWindow(longRows, -1, states, 0, 0, 256, backend));
        for (int mask : new int[] {-1, 256}) {
            fails(IllegalArgumentException.class,
                    () -> M3IndexMaskCompactor.selectWindow(rows, 0, states, 0, 2, mask, backend));
            fails(IllegalArgumentException.class,
                    () -> M3IndexMaskCompactor.selectLongWindow(longRows, 0, states, 0, 2, mask, backend));
        }
        eq(new int[] {11, 22}, rows);
        eq(new long[] {11, 22}, longRows);
        ok(Arrays.equals(new byte[] {0, 0}, states));
    }

    private static void randomizedDifferential(boolean nativeRun) {
        Random random = new Random(0x4d335f4d41534b31L);
        for (int trial = 0; trial < 1_024; trial++) {
            int size = random.nextInt(300);
            int[] rows = new int[size];
            byte[] states = new byte[size];
            for (int i = 0; i < size; i++) {
                rows[i] = random.nextInt();
                states[i] = (byte) random.nextInt(256);
            }
            int mask = random.nextInt(256);
            int[] expected = oracle(rows, states, mask);
            eq(expected, M3IndexMaskCompactor.select(rows, states, mask,
                    M3IndexMaskCompactor.Backend.JAVA));
            if (nativeRun) {
                eq(expected, M3IndexMaskCompactor.select(rows, states, mask,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
        }
    }

    /** Differential test over actual primitive List and circular Deque owners. */
    private static void primitiveOwnersAndLongIds() {
        Random random = new Random(0x4d334d41534b5632L);
        for (int trial = 0; trial < 1_024; trial++) {
            int length = random.nextInt(288);
            long[] inputs = new long[length];
            byte[] states = new byte[length];
            int required = trial & 255;
            java.util.ArrayList<Long> expectedValues = new java.util.ArrayList<>();
            for (int i = 0; i < length; i++) {
                long v = switch (i % 17) {
                    case 0 -> Long.MIN_VALUE;
                    case 1 -> Long.MAX_VALUE;
                    case 2 -> 0L;
                    default -> random.nextInt(29) - 14L;
                };
                inputs[i] = v;
                states[i] = (byte) random.nextInt(256);
                if (((states[i] & 255) & required) == required) expectedValues.add(v);
            }
            long[] expected = new long[expectedValues.size()];
            for (int i = 0; i < expected.length; i++) expected[i] = expectedValues.get(i);
            eq(expected, M3IndexMaskCompactor.selectLong(inputs, states, required));
            ok(M3IndexMaskCompactor.selectedCount(states, required) == expected.length);

            M3LongArrayList list = M3LongArrayList.copyOf(inputs);
            M3LongArrayDeque deque = new M3LongArrayDeque(0);
            // Wrap the ring before selection, then exercise wrapped and grown states.
            for (int i = 0; i < 13; i++) {
                deque.addLast(i);
                deque.removeFirstLong();
            }
            for (long v : inputs) deque.addLast(v);
            M3LongIterator listCursor = list.iterator();
            M3LongIterator dequeCursor = deque.iterator();
            int removed = length - expected.length;
            ok(list.retainByStateMask(states, required) == removed);
            ok(deque.retainByStateMask(states, required) == removed);
            eq(expected, list.toArray());
            eq(expected, deque.toArray());
            if (removed > 0) {
                fails(java.util.ConcurrentModificationException.class, listCursor::hasNext);
                fails(java.util.ConcurrentModificationException.class, dequeCursor::hasNext);
            } else {
                ok(listCursor.hasNext() == (length != 0));
                ok(dequeCursor.hasNext() == (length != 0));
            }
        }
        M3LongArrayList list = M3LongArrayList.copyOf(new long[] {1, 2, 3});
        M3LongArrayDeque deque = new M3LongArrayDeque();
        deque.addLast(1); deque.addLast(2); deque.addLast(3);
        for (int invalid : new int[] {-1, 256}) {
            fails(IllegalArgumentException.class,
                    () -> list.retainByStateMask(new byte[] {1, 2, 3}, invalid));
            fails(IllegalArgumentException.class,
                    () -> deque.retainByStateMask(new byte[] {1, 2, 3}, invalid));
        }
        fails(IllegalArgumentException.class, () -> list.retainByStateMask(new byte[2], 0));
        fails(IllegalArgumentException.class, () -> deque.retainByStateMask(new byte[4], 0));
        fails(NullPointerException.class, () -> list.retainByStateMask(null, 1));
        fails(NullPointerException.class, () -> deque.retainByStateMask(null, 1));
        eq(new long[] {1, 2, 3}, list.toArray());
        eq(new long[] {1, 2, 3}, deque.toArray());
        for (Class<?> owner : new Class<?>[] {M3LongArrayList.class, M3LongArrayDeque.class}) {
            for (java.lang.reflect.Field field : owner.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (fieldType.isArray()) ok(fieldType.getComponentType().isPrimitive());
            }
        }
    }

    private static void eq(long[] expected, long[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("long selection mismatch: "
                    + Arrays.toString(expected) + " vs " + Arrays.toString(actual));
        }
    }

    private static int[] oracle(int[] order, byte[] states, int mask) {
        int[] candidates = new int[order.length];
        int count = 0;
        for (int position = 0; position < order.length; position++) {
            if (((states[position] & 0xff) & mask) == mask) {
                candidates[count++] = order[position];
            }
        }
        return Arrays.copyOf(candidates, count);
    }

    private static void eq(int[] expected, int[] actual) {
        checks++;
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " but was " + Arrays.toString(actual));
        }
    }

    private static void fails(Class<? extends Throwable> expected, Runnable task) {
        checks++;
        try {
            task.run();
        } catch (Throwable thrown) {
            if (expected.isInstance(thrown)) {
                return;
            }
            throw new AssertionError("unexpected failure", thrown);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    private static void ok(boolean predicate) {
        checks++;
        if (!predicate) {
            throw new AssertionError("condition was false");
        }
    }
}
