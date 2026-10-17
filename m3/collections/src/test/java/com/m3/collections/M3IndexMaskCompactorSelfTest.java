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
