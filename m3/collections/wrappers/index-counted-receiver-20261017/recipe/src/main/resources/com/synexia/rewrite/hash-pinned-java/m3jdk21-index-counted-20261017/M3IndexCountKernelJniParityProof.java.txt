// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Deterministic Java21 oracle for Java and opt-in C17/JNI numeric count atoms. */
public final class M3IndexCountKernelJniParityProof {
    private static long checks;
    private M3IndexCountKernelJniParityProof() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " at check=" + checks);
    }

    private static long locateReference(int[] counts, int from, int to,
            long ordinal, boolean descending) {
        long remaining = ordinal;
        for (int i = descending ? to - 1 : from;
                descending ? i >= from : i < to; i += descending ? -1 : 1) {
            if (remaining < counts[i]) return ((long) i << 32) | remaining;
            remaining -= counts[i];
        }
        throw new IndexOutOfBoundsException("occurrence outside count span");
    }

    private static void rejects(Class<? extends Throwable> type, Runnable action, String description) {
        try {
            action.run();
            throw new AssertionError("missing exception: " + description);
        } catch (AssertionError assertion) {
            throw assertion;
        } catch (Throwable error) {
            check(type.isInstance(error), description + " threw " + error);
        }
    }

    private static int[] selectedReference(long[] words, int logicalRows) {
        int[] scratch = new int[logicalRows];
        int count = 0;
        for (int row = 0; row < logicalRows; row++) {
            if ((words[row >>> 6] & (1L << (row & 63))) != 0) scratch[count++] = row;
        }
        return Arrays.copyOf(scratch, count);
    }

    public static void main(String[] ignored) {
        Random random = new Random(20261017L);
        long checksum = 0xcbf29ce484222325L;
        for (int round = 0; round < 768; round++) {
            int n = round % 129;
            int[] counts = new int[n];
            for (int i = 0; i < n; i++) {
                counts[i] = switch (random.nextInt(8)) {
                    case 0, 1, 2 -> 0;
                    case 3 -> Integer.MAX_VALUE;
                    default -> random.nextInt(1, 1024);
                };
            }
            int from = random.nextInt(n + 1);
            int to = from + random.nextInt(n - from + 1);
            long total = 0L;
            for (int i = from; i < to; i++) total += counts[i];
            long actual = M3IndexCountKernel.sum(counts, from, to);
            check(actual == total, "sum round=" + round);
            checksum = (checksum ^ actual) * 0x100000001b3L;
            for (boolean descending : new boolean[]{false, true}) {
                if (total == 0L) {
                    rejects(IndexOutOfBoundsException.class,
                            () -> M3IndexCountKernel.locate(counts, from, to, 0L, descending),
                            "zero count span");
                } else {
                    long ordinal = Math.floorMod(random.nextLong(), total);
                    long expected = locateReference(counts, from, to, ordinal, descending);
                    long found = M3IndexCountKernel.locate(counts, from, to, ordinal, descending);
                    check(expected == found, "ordinal lookup round=" + round);
                    checksum = (checksum ^ found) * 0x100000001b3L;
                }
            }
            int logicalRows = random.nextInt(0, 8193);
            long[] words = new long[(logicalRows >>> 6) + ((logicalRows & 63) == 0 ? 0 : 1)];
            for (int i = 0; i < words.length; i++) words[i] = random.nextLong();
            int[] selected = M3IndexCountKernel.selectedRows(words, logicalRows);
            check(Arrays.equals(selected, selectedReference(words, logicalRows)),
                    "selected row order round=" + round);
            checksum = (checksum ^ selected.length) * 0x100000001b3L;
            if (selected.length != 0) checksum = (checksum ^ selected[selected.length - 1]) * 0x100000001b3L;
        }
        rejects(NullPointerException.class, () -> M3IndexCountKernel.sum(null, 0, 0), "null counts");
        rejects(IndexOutOfBoundsException.class,
                () -> M3IndexCountKernel.sum(new int[]{1}, 0, 2), "invalid span");
        rejects(IllegalArgumentException.class,
                () -> M3IndexCountKernel.sum(new int[]{1, -1, 2}, 0, 3), "negative count");
        rejects(IllegalArgumentException.class,
                () -> M3IndexCountKernel.locate(new int[]{1, -1}, 0, 2, 0, false),
                "negative count after successful located offset");
        rejects(NullPointerException.class,
                () -> M3IndexCountKernel.selectedRows(null, 0), "null mask");
        rejects(IllegalArgumentException.class,
                () -> M3IndexCountKernel.selectedRows(new long[0], 65), "mask extent");
        rejects(IllegalArgumentException.class,
                () -> M3IndexCountKernel.selectedRows(new long[0], -1), "negative rows");
        check(M3IndexCountKernel.nativeEnabled() ==
                Boolean.getBoolean("m3.collections.counts.native"), "native dispatch must be explicit");
        System.out.println("M3_COUNTED_JNI_PARITY_PASS checks=" + checks
                + " checksum=" + Long.toUnsignedString(checksum));
    }
}
