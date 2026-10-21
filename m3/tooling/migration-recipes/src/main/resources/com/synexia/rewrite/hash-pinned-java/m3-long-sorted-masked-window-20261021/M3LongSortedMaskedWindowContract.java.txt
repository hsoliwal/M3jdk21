// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Independent sorted-window mask oracle over the original indexed long[] owner. */
public final class M3LongSortedMaskedWindowContract {
    private static long checks;
    private M3LongSortedMaskedWindowContract() { }

    private static void yes(boolean condition, String finding) {
        checks++;
        if (!condition) throw new AssertionError(finding + " check=" + checks);
    }

    private static void refused(Class<? extends Throwable> expected, Runnable call) {
        checks++;
        try {
            call.run();
        } catch (Throwable problem) {
            if (expected.isInstance(problem)) return;
            throw new AssertionError("incorrect exception", problem);
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    private static long[] oracle(long[] values, int offset, byte[] states,
            int stateOffset, int length, int required, int excluded) {
        long[] result = new long[length];
        int size = 0;
        for (int i = 0; i < length; i++) {
            int word = Byte.toUnsignedInt(states[stateOffset + i]);
            if ((word & required) == required && (word & excluded) == 0) {
                result[size++] = values[offset + i];
            }
        }
        return Arrays.copyOf(result, size);
    }

    private static void exactContracts(M3IndexMaskCompactor.Backend backend) {
        long[] parent = {Long.MAX_VALUE, Long.MIN_VALUE, -3, 1, 7, Long.MIN_VALUE};
        byte[] flags = {99, 1, 3, (byte) 0x81, 3, 0, 42};
        M3LongSortedSnapshot result = M3LongSortedSnapshot.fromAlignedMasksWindow(
                parent, 1, flags, 1, 4, 1, 0x80, backend);
        yes(Arrays.equals(new long[]{Long.MIN_VALUE, -3, 7}, result.toArray()),
                "signed ordered values, excluded states and independent offsets");
        yes(result.size() == 3, "detached sorted snapshot size");
        yes(result.first() == Long.MIN_VALUE && result.last() == 7, "sorted endpoints");
        yes(result.rank(0) == 2 && result.select(1) == -3
                && result.indexOf(7) == 2, "rank/select/index");
        yes(Arrays.equals(new long[]{-3}, result.subSet(-3, 7).toArray()),
                "bounded sorted range shares immutable snapshot");
        long[] output = result.toArray();
        output[0] = 12;
        parent[1] = Long.MAX_VALUE;
        flags[1] = 0;
        yes(result.first() == Long.MIN_VALUE, "detached parent row and flags");
        yes(result.select(0) == Long.MIN_VALUE, "output copy isolation");
        yes(M3LongSortedSnapshot.fromAlignedMasksWindow(parent, 1,
                flags, 1, 0, 1, 0x80, backend).size() == 0, "empty window");
        yes(M3LongSortedSnapshot.fromAlignedMasksWindow(new long[]{3, 1, 2}, 1,
                new byte[]{9, 1, 1}, 1, 2, 1, 0, backend).size() == 2,
                "rows outside bounded sorted window are irrelevant");
    }

    private static void invalidCases() {
        var backend = M3IndexMaskCompactor.Backend.JAVA;
        long[] row = {1, 2, 3};
        byte[] flags = {1, 2, 3};
        refused(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                new long[]{2, 1}, 0, flags, 0, 2, 1, 0, backend));
        refused(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                new long[]{1, 1}, 0, flags, 0, 2, 1, 0, backend));
        refused(IndexOutOfBoundsException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                row, -1, flags, 0, 1, 1, 0, backend));
        refused(IndexOutOfBoundsException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                row, 0, flags, 3, 1, 1, 0, backend));
        refused(NullPointerException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                null, 0, flags, 0, 1, 1, 0, backend));
        refused(NullPointerException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                row, 0, null, 0, 1, 1, 0, backend));
        refused(NullPointerException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                row, 0, flags, 0, 1, 1, 0, null));
        refused(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                row, 0, flags, 0, 1, 256, 0, backend));
        refused(IllegalArgumentException.class, () -> M3LongSortedSnapshot.fromAlignedMasksWindow(
                row, 0, flags, 0, 1, 1, -1, backend));
        yes(M3LongSortedSnapshot.fromAlignedMasksWindow(row, 0, flags, 0, 3, 1, 1,
                backend).isEmpty(), "contradictory exact masks admit nothing");
    }

    private static void randomized(M3IndexMaskCompactor.Backend backend) {
        Random random = new Random(0x6d33536f72746564L);
        for (int round = 0; round < 2048; round++) {
            int rowCount = random.nextInt(110);
            int stateCount = random.nextInt(110);
            long[] rows = new long[rowCount];
            byte[] flags = new byte[stateCount];
            random.nextBytes(flags);
            Arrays.fill(rows, Long.MIN_VALUE);
            int first = random.nextInt(rowCount + 1);
            int stateFirst = random.nextInt(stateCount + 1);
            int length = random.nextInt(Math.min(rowCount - first, stateCount - stateFirst) + 1);
            long current = -5000;
            for (int i = first; i < first + length; i++) {
                current += random.nextInt(10) + 1;
                rows[i] = current;
            }
            int required = random.nextInt(256), excluded = random.nextInt(256);
            if (round % 29 == 0) { required = 0; excluded = 0; }
            if (round % 31 == 0) { required = 0x80; excluded = 0x80; }
            long[] before = rows.clone();
            byte[] beforeFlags = flags.clone();
            long[] expected = oracle(rows, first, flags, stateFirst,
                    length, required, excluded);
            M3LongSortedSnapshot actual = M3LongSortedSnapshot.fromAlignedMasksWindow(
                    rows, first, flags, stateFirst, length, required, excluded, backend);
            yes(Arrays.equals(expected, actual.toArray()), "sorted mask oracle round " + round);
            yes(expected.length == actual.size(), "sorted size round " + round);
            for (int i = 0; i < expected.length; i++) {
                yes(expected[i] == actual.select(i) && actual.rank(expected[i]) == i,
                        "rank/select sorted parity round " + round + " ordinal " + i);
            }
            yes(Arrays.equals(before, rows) && Arrays.equals(beforeFlags, flags),
                    "no borrowed storage mutation round " + round);
        }
    }

    public static long run(M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(backend, "backend");
        long before = checks;
        exactContracts(backend);
        invalidCases();
        randomized(backend);
        return checks - before;
    }

    public static void main(String[] args) {
        long javaChecks = run(M3IndexMaskCompactor.Backend.JAVA);
        if (args.length > 0 && "--native".equals(args[0])) {
            yes(M3IndexMaskCompactor.nativeAvailable(), "required native library");
            long nativeChecks = run(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);
            System.out.println("M3_SORTED_WINDOW_MASK_PASS java=" + javaChecks
                    + " native=" + nativeChecks);
        } else {
            System.out.println("M3_SORTED_WINDOW_MASK_PASS java=" + javaChecks);
        }
    }
}
