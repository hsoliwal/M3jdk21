// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Executable seeded reference: brute-force row scan vs M3 set-bit index cursor. */
public final class M3MaskIndexCursorSelfTest {
    private static int checks;
    private M3MaskIndexCursorSelfTest() { }

    private static void same(Object actual, Object expected) {
        checks++;
        if (!actual.equals(expected)) throw new AssertionError("assertion " + checks + ": " + actual + " != " + expected);
    }

    private static void refused(Class<? extends Throwable> kind, Runnable task) {
        checks++;
        try { task.run(); }
        catch (Throwable observed) {
            if (kind.isInstance(observed)) return;
            throw new AssertionError("Wrong refusal", observed);
        }
        throw new AssertionError("Missing " + kind.getSimpleName() + " refusal");
    }

    private static void check(long[] bits, int n) {
        List<Integer> reference = new ArrayList<>();
        for (int i = 0; i < n; i++) if ((bits[i >>> 6] & (1L << (i & 63))) != 0) reference.add(i);
        List<Integer> actual = new ArrayList<>();
        M3MaskIndexCursor.forEach(bits, n, actual::add);
        same(actual, reference);
        int[] count = {0};
        M3MaskIndexCursor.forEach(bits, n, index -> {
            if (index < 0 || index >= n) throw new AssertionError("out-of-range bit");
            if (++count[0] > n) throw new AssertionError("duplicate bit");
        });
        same(count[0], reference.size());
        M3MaskIndexCursor.requireSubset(bits, bits, n);
        checks++;
    }

    private static long[] mask(int size, int style, Random random) {
        long[] bits = new long[(size + 63) >>> 6];
        for (int i = 0; i < size; i++) {
            boolean keep = switch (style) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> i % 64 == 63;
                case 3 -> i % 64 == 0;
                case 4 -> i == size - 1;
                case 5 -> random.nextInt(1024) == 0;
                case 6 -> random.nextInt(2) == 0;
                default -> random.nextInt(7) != 0;
            };
            if (keep) bits[i >>> 6] |= 1L << (i & 63);
        }
        return bits;
    }

    public static void main(String[] args) {
        Random random = new Random(0x4d334249544d4153L);
        int[] boundaries = {0, 1, 2, 7, 8, 63, 64, 65, 66, 127, 128, 129,
                            255, 256, 257, 1023, 1024, 1025, 4095, 4096, 4097};
        for (int size : boundaries)
            for (int style = 0; style < 8; style++) check(mask(size, style, random), size);
        for (int iteration = 0; iteration < 8_000; iteration++) {
            int n = random.nextInt(4097);
            long[] bits = mask(n, random.nextInt(8), random);
            check(bits, n);
            if ((iteration & 7) == 0) {
                long[] subset = bits.clone();
                for (int j = 0; j < subset.length; j++) subset[j] &= random.nextLong();
                M3MaskIndexCursor.requireSubset(subset, bits, n);
                checks++;
                check(subset, n);
            }
        }
        for (int n : boundaries) {
            long[] first = mask(n, 1, random);
            if (n > 0) {
                long[] invalid = first.clone();
                invalid[0] = 0;
                long[] out = first.clone();
                if (n > 1) {
                    out[0] ^= 1;
                    // Only a mask with a zero bit in prior can be detected as unexpected.
                    M3MaskIndexCursor.requireSubset(out, first, n);
                    checks++;
                }
                long[] unselected = new long[(n + 63) >>> 6];
                refused(IllegalStateException.class, () -> M3MaskIndexCursor.requireSubset(first, unselected, n));
            }
            long[] wrong = Arrays.copyOf(first, first.length + 1);
            refused(IllegalArgumentException.class, () -> M3MaskIndexCursor.forEach(wrong, n, ignore -> { }));
            if ((n & 63) != 0) {
                long[] padded = first.clone();
                padded[padded.length - 1] |= 1L << (n & 63);
                refused(IllegalStateException.class, () -> M3MaskIndexCursor.forEach(padded, n, ignore -> { }));
            }
        }
        refused(NullPointerException.class, () -> M3MaskIndexCursor.forEach(null, 1, ignore -> { }));
        refused(NullPointerException.class, () -> M3MaskIndexCursor.forEach(new long[0], 0, null));
        refused(IllegalArgumentException.class, () -> M3MaskIndexCursor.forEach(new long[0], -1, ignore -> { }));
        long[] one = {1L};
        refused(IllegalStateException.class, () -> M3MaskIndexCursor.requireSubset(one, new long[]{0}, 1));
        int[] visits = {0};
        refused(IllegalStateException.class, () -> M3MaskIndexCursor.forEach(new long[]{3L}, 1, ignored -> visits[0]++));
        same(visits[0], 0);
        List<Integer> partial = new ArrayList<>();
        refused(IllegalStateException.class, () -> M3MaskIndexCursor.forEach(new long[]{7L}, 3, i -> {
            partial.add(i);
            if (i == 1) throw new IllegalStateException("consumer failure");
        }));
        same(partial, List.of(0, 1));
        System.out.println("M3_UNIX4J_MASK_CURSOR_PASS checks=" + checks);
    }
}
