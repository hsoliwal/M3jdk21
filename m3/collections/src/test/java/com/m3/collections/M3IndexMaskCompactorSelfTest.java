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
        randomizedDifferential(requireNative);
        windowSelectionContracts(requireNative);
        stateWordMaskContracts(requireNative);
        primitiveOwnersAndLongIds(requireNative);
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
        long[] ids = {Long.MIN_VALUE, 0L, Long.MAX_VALUE, Long.MIN_VALUE};
        eq(ids, M3IndexMaskCompactor.selectLong(ids, states, 0, java));
        eq(new long[] {0L}, M3IndexMaskCompactor.selectLong(ids, states, 255, java));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLong(ids, states, 0, null));
        fails(NullPointerException.class,
                () -> M3IndexMaskCompactor.selectLong(null, states, 0, java));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectLong(ids, new byte[3], 0, java));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectLong(ids, states, -1, java));
        fails(IllegalArgumentException.class,
                () -> M3IndexMaskCompactor.selectLong(ids, states, 256, java));
        if (nativeRun) {
            eq(ids, M3IndexMaskCompactor.selectLong(ids, states, 0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            eq(new long[] {0L}, M3IndexMaskCompactor.selectLong(ids, states, 255,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
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
            long[] longRows = new long[n];
            for (int i = 0; i < n; i++) {
                longRows[i] = i % 3 == 0 ? Long.MIN_VALUE : (i % 3 == 1 ? Long.MAX_VALUE : i - 700L);
            }
            for (int mask = 0; mask < 256; mask++) {
                int[] expected = oracle(rows, states, mask);
                eq(expected, M3IndexMaskCompactor.select(rows, states, mask,
                        M3IndexMaskCompactor.Backend.JAVA));
                if (nativeRun) {
                    eq(expected, M3IndexMaskCompactor.select(rows, states, mask,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                }
                long[] longExpected = M3IndexMaskCompactor.selectLong(longRows, states, mask);
                eq(longExpected, M3IndexMaskCompactor.selectLong(longRows, states, mask,
                        M3IndexMaskCompactor.Backend.JAVA));
                if (nativeRun) {
                    eq(longExpected, M3IndexMaskCompactor.selectLong(longRows, states, mask,
                            M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
                }
            }
        }
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
    private static void primitiveOwnersAndLongIds(boolean nativeRun) {
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
            eq(expected, M3IndexMaskCompactor.selectLong(inputs, states, required,
                    M3IndexMaskCompactor.Backend.JAVA));
            if (nativeRun) {
                eq(expected, M3IndexMaskCompactor.selectLong(inputs, states, required,
                        M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
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


    /**
     * Checked primitive offset views: independent logical row/mask offsets must
     * be equivalent to detached JDK copyOfRange inputs, without copying them
     * in the production code. Exhaustively test all 256 mask values over the
     * JNI chunk/array-boundary lengths and asymmetric offsets.
     */
    private static void windowSelectionContracts(boolean nativeRun) {
        int[] rows = new int[1044];
        long[] longRows = new long[1044];
        byte[] states = new byte[1044];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i % 7 == 0 ? Integer.MIN_VALUE : (i % 7 == 1
                    ? Integer.MAX_VALUE : i % 13 - 6);
            longRows[i] = i % 7 == 0 ? Long.MIN_VALUE : (i % 7 == 1
                    ? Long.MAX_VALUE : i % 13 - 6L);
            states[i] = (byte) ((i * 71 + (i >>> 2)) & 255);
        }
        int[] rowBefore = rows.clone();
        long[] longBefore = longRows.clone();
        byte[] masksBefore = states.clone();
        var javaBackend = M3IndexMaskCompactor.Backend.JAVA;
        var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        for (int size : new int[] {0, 1, 2, 255, 256, 257, 511, 512, 513, 1025}) {
            for (int rowOffset : new int[] {0, 1, 7}) {
                for (int maskOffset : new int[] {0, 2, 9}) {
                    int[] expectedRows = Arrays.copyOfRange(rows, rowOffset, rowOffset + size);
                    long[] expectedLongs = Arrays.copyOfRange(
                            longRows, rowOffset, rowOffset + size);
                    byte[] expectedStates = Arrays.copyOfRange(
                            states, maskOffset, maskOffset + size);
                    for (int mask = 0; mask < 256; mask++) {
                        int[] expected = M3IndexMaskCompactor.select(
                                expectedRows, expectedStates, mask, javaBackend);
                        long[] expectedLong = M3IndexMaskCompactor.selectLong(
                                expectedLongs, expectedStates, mask);
                        eq(expected, M3IndexMaskCompactor.selectWindow(
                                rows, rowOffset, states, maskOffset, size, mask, javaBackend));
                        eq(expectedLong, M3IndexMaskCompactor.selectLongWindow(
                                longRows, rowOffset, states, maskOffset, size, mask, javaBackend));
                        if (nativeRun) {
                            eq(expected, M3IndexMaskCompactor.selectWindow(
                                    rows, rowOffset, states, maskOffset, size,
                                    mask, nativeBackend));
                            eq(expectedLong, M3IndexMaskCompactor.selectLongWindow(
                                    longRows, rowOffset, states, maskOffset, size,
                                    mask, nativeBackend));
                        }
                    }
                }
            }
        }

        Random random = new Random(0x4d33574f46465345L);
        for (int trial = 0; trial < 512; trial++) {
            int size = random.nextInt(1026);
            int rowOffset = random.nextInt(rows.length - size + 1);
            int maskOffset = random.nextInt(states.length - size + 1);
            int mask = random.nextInt(256);
            int[] candidateRows = Arrays.copyOfRange(rows, rowOffset, rowOffset + size);
            long[] candidateLongs = Arrays.copyOfRange(
                    longRows, rowOffset, rowOffset + size);
            byte[] candidateMarks = Arrays.copyOfRange(
                    states, maskOffset, maskOffset + size);
            int[] expected = M3IndexMaskCompactor.select(
                    candidateRows, candidateMarks, mask, javaBackend);
            long[] expectedLong = M3IndexMaskCompactor.selectLong(
                    candidateLongs, candidateMarks, mask);
            eq(expected, M3IndexMaskCompactor.selectWindow(
                    rows, rowOffset, states, maskOffset, size, mask, javaBackend));
            eq(expectedLong, M3IndexMaskCompactor.selectLongWindow(
                    longRows, rowOffset, states, maskOffset, size, mask, javaBackend));
            if (nativeRun) {
                eq(expected, M3IndexMaskCompactor.selectWindow(
                        rows, rowOffset, states, maskOffset, size, mask, nativeBackend));
                eq(expectedLong, M3IndexMaskCompactor.selectLongWindow(
                        longRows, rowOffset, states, maskOffset, size, mask, nativeBackend));
            }
        }
        eq(rowBefore, rows);
        eq(longBefore, longRows);
        ok(Arrays.equals(masksBefore, states));

        eq(new int[0], M3IndexMaskCompactor.selectWindow(
                rows, rows.length, states, states.length, 0, 255, javaBackend));
        eq(new long[0], M3IndexMaskCompactor.selectLongWindow(
                longRows, longRows.length, states, states.length, 0, 255, javaBackend));
        for (var backend : M3IndexMaskCompactor.Backend.values()) {
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                    rows, -1, states, 0, 1, 1, backend));
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                    rows, 0, states, -1, 1, 1, backend));
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                    rows, 0, states, 0, -1, 1, backend));
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(
                    rows, Integer.MAX_VALUE, states, 0, 1, 1, backend));
            fails(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                    longRows, 0, states, Integer.MAX_VALUE, 1, 1, backend));
            fails(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectWindow(
                    rows, 0, states, 0, 1, -1, backend));
            fails(IllegalArgumentException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                    longRows, 0, states, 0, 1, 256, backend));
            fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(
                    null, 0, states, 0, 1, 0, backend));
            fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                    longRows, 0, null, 0, 1, 0, backend));
        }
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectWindow(
                rows, 0, states, 0, 1, 0, null));
        fails(NullPointerException.class, () -> M3IndexMaskCompactor.selectLongWindow(
                longRows, 0, states, 0, 1, 0, null));
    }


    /** Oracle: bit 63 and all lower parent-state flags, independent offsets. */
    private static void stateWordMaskContracts(boolean nativeRun) {
        int n = 1044;
        int[] rows = new int[n];
        long[] longRows = new long[n];
        long[] states = new long[n];
        Random rng = new Random(0x534c4f544d41534bL);
        for (int i = 0; i < n; i++) {
            rows[i] = i % 5 == 0 ? Integer.MIN_VALUE : (i % 5 == 1
                    ? Integer.MAX_VALUE : (i % 17 - 7));
            longRows[i] = i % 5 == 0 ? Long.MIN_VALUE : (i % 5 == 1
                    ? Long.MAX_VALUE : (i % 17 - 7L));
            states[i] = i % 8 == 0 ? -1L : (i % 8 == 1
                    ? Long.MIN_VALUE : rng.nextLong());
        }
        int[] unchangedRows = rows.clone();
        long[] unchangedLongRows = longRows.clone();
        long[] unchangedStates = states.clone();

        M3ObjectResidency<Object> residency =
                new M3ObjectResidency<>(n, 0, 0L, 0L, null, null, null);
        for (int i = 0; i < n; i++) residency.writeParentBits(i, states[i]);
        long[] snapshot = new long[n + 5];
        residency.copyParentBits(0, snapshot, 3, n);
        eq(unchangedStates, Arrays.copyOfRange(snapshot, 3, n + 3));
        long before = snapshot[3];
        residency.writeParentBits(0, 0L);
        ok(snapshot[3] == before); // detached source state is not a live object view
        fails(IndexOutOfBoundsException.class, () ->
                residency.copyParentBits(-1, snapshot, 0, 1));
        fails(IndexOutOfBoundsException.class, () ->
                residency.copyParentBits(0, snapshot, snapshot.length, 1));
        fails(IndexOutOfBoundsException.class, () ->
                residency.copyParentBits(0, snapshot, 0, Integer.MAX_VALUE));
        fails(NullPointerException.class, () ->
                residency.copyParentBits(0, null, 0, 1));
        long[] unchangedCopy = snapshot.clone();
        residency.copyParentBits(n, snapshot, snapshot.length, 0);
        eq(unchangedCopy, snapshot);

        long[] requiredMasks = new long[74];
        requiredMasks[0] = 0L;
        requiredMasks[1] = -1L;
        requiredMasks[2] = Long.MIN_VALUE;
        requiredMasks[3] = Long.MAX_VALUE;
        requiredMasks[4] = 0xffL;
        requiredMasks[5] = 1L << 32;
        for (int bit = 0; bit < 64; bit++) {
            requiredMasks[6 + bit] = 1L << bit;
        }
        requiredMasks[70] = Long.MIN_VALUE | 1L;
        requiredMasks[71] = (1L << 31) | (1L << 63);
        requiredMasks[72] = 0xf0f0f0f0f0f0f0f0L;
        requiredMasks[73] = 0x00ff00ff00ff00ffL;

        var javaBackend = M3IndexMaskCompactor.Backend.JAVA;
        var nativeBackend = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        for (int count : new int[] {0, 1, 2, 63, 64, 65, 255, 256, 257, 1025}) {
            for (int rowStart : new int[] {0, 1, 7}) {
                for (int stateStart : new int[] {0, 2, 9}) {
                    for (long mask : requiredMasks) {
                        int[] oracle = stateWordIntOracle(
                                rows, rowStart, states, stateStart, count, mask);
                        long[] longOracle = stateWordLongOracle(
                                longRows, rowStart, states, stateStart, count, mask);
                        eq(oracle, M3IndexMaskCompactor.selectStateWordWindow(
                                rows, rowStart, states, stateStart, count, mask, javaBackend));
                        eq(longOracle, M3IndexMaskCompactor.selectLongStateWordWindow(
                                longRows, rowStart, states, stateStart, count, mask, javaBackend));
                        if (nativeRun) {
                            eq(oracle, M3IndexMaskCompactor.selectStateWordWindow(
                                    rows, rowStart, states, stateStart, count, mask, nativeBackend));
                            eq(longOracle, M3IndexMaskCompactor.selectLongStateWordWindow(
                                    longRows, rowStart, states, stateStart, count, mask, nativeBackend));
                        }
                    }
                }
            }
        }
        Random random = new Random(0x4d334f424a535441L);
        for (int i = 0; i < 512; i++) {
            int count = random.nextInt(n + 1);
            int rowStart = random.nextInt(n - count + 1);
            int stateStart = random.nextInt(n - count + 1);
            long mask = random.nextLong();
            int[] expected = stateWordIntOracle(rows, rowStart,
                    states, stateStart, count, mask);
            long[] expectedLong = stateWordLongOracle(
                    longRows, rowStart, states, stateStart, count, mask);
            eq(expected, M3IndexMaskCompactor.selectStateWordWindow(
                    rows, rowStart, states, stateStart, count, mask, javaBackend));
            eq(expectedLong, M3IndexMaskCompactor.selectLongStateWordWindow(
                    longRows, rowStart, states, stateStart, count, mask, javaBackend));
            if (nativeRun) {
                eq(expected, M3IndexMaskCompactor.selectStateWordWindow(
                        rows, rowStart, states, stateStart, count, mask, nativeBackend));
                eq(expectedLong, M3IndexMaskCompactor.selectLongStateWordWindow(
                        longRows, rowStart, states, stateStart, count, mask, nativeBackend));
            }
        }
        eq(rows, unchangedRows);
        eq(longRows, unchangedLongRows);
        eq(states, unchangedStates);
        for (var backend : M3IndexMaskCompactor.Backend.values()) {
            fails(IndexOutOfBoundsException.class, () ->
                    M3IndexMaskCompactor.selectStateWordWindow(
                            rows, -1, states, 0, 1, -1L, backend));
            fails(IndexOutOfBoundsException.class, () ->
                    M3IndexMaskCompactor.selectStateWordWindow(
                            rows, 0, states, n, 1, 0L, backend));
            fails(IndexOutOfBoundsException.class, () ->
                    M3IndexMaskCompactor.selectLongStateWordWindow(
                            longRows, 0, states, 0, Integer.MAX_VALUE, 1L, backend));
            fails(NullPointerException.class, () ->
                    M3IndexMaskCompactor.selectStateWordWindow(
                            null, 0, states, 0, 0, 0L, backend));
            fails(NullPointerException.class, () ->
                    M3IndexMaskCompactor.selectLongStateWordWindow(
                            longRows, 0, null, 0, 0, 0L, backend));
        }
        fails(NullPointerException.class, () ->
                M3IndexMaskCompactor.selectStateWordWindow(
                        rows, 0, states, 0, 0, 0L, null));
        fails(NullPointerException.class, () ->
                M3IndexMaskCompactor.selectLongStateWordWindow(
                        longRows, 0, states, 0, 0, 0L, null));
    }

    private static int[] stateWordIntOracle(int[] rows, int rowFrom,
            long[] words, int wordFrom, int count, long required) {
        int[] values = new int[count];
        int used = 0;
        for (int i = 0; i < count; i++) {
            if ((words[wordFrom + i] & required) == required) {
                values[used++] = rows[rowFrom + i];
            }
        }
        return Arrays.copyOf(values, used);
    }

    private static long[] stateWordLongOracle(long[] rows, int rowFrom,
            long[] words, int wordFrom, int count, long required) {
        long[] values = new long[count];
        int used = 0;
        for (int i = 0; i < count; i++) {
            if ((words[wordFrom + i] & required) == required) {
                values[used++] = rows[rowFrom + i];
            }
        }
        return Arrays.copyOf(values, used);
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
