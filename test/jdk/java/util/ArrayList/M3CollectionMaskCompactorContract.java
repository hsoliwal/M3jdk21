/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary M3JDK21 stable deletion-mask array compaction preserves references, offsets, and unchanged tail
 * @modules java.base/jdk.internal.mindex
 * @run main M3CollectionMaskCompactorContract
 */
import java.util.Random;
import jdk.internal.mindex.M3CollectionMaskCompactor;

/**
 * Independent stable-compaction oracle for both scalar and run-copy dispatch.
 * The caller, not the compactor, owns tail reference clearing.
 */
public final class M3CollectionMaskCompactorContract {
    private static long checks;
    private static final Random RANDOM = new Random(0x4D334A444B32314CL);

    public static void main(String[] args) {
        verifyNullsAndBounds();
        for (int length : new int[] {0, 1, 2, 63, 64, 65, 127, 128, 511, 512, 513,
                1023, 1024, 1025, 4096}) {
            for (int mode = 0; mode < 7; mode++) {
                check(length, mode, RANDOM.nextInt(7), 0);
            }
        }
        for (int i = 0; i < 1200; i++) {
            int length = RANDOM.nextInt(3200);
            check(length, RANDOM.nextInt(7), RANDOM.nextInt(9), i);
        }
        System.out.println("M3CollectionMaskCompactorContract checks=" + checks);
    }

    private static void verifyNullsAndBounds() {
        fail(NullPointerException.class, () -> M3CollectionMaskCompactor.compact(null, 0, 0, new long[0]));
        fail(NullPointerException.class, () -> M3CollectionMaskCompactor.compact(new Object[0], 0, 0, null));
        fail(IndexOutOfBoundsException.class, () -> M3CollectionMaskCompactor.compact(new Object[3], -1, 2, new long[1]));
        fail(IndexOutOfBoundsException.class, () -> M3CollectionMaskCompactor.compact(new Object[3], 2, 4, new long[1]));
        fail(IllegalArgumentException.class, () -> M3CollectionMaskCompactor.compact(new Object[66], 0, 66, new long[1]));
        Object[] input = new Object[] {new Object(), new Object(), new Object()};
        Object[] original = input.clone();
        equal(2, M3CollectionMaskCompactor.compact(input, 2, 2, new long[0]));
        identity(original, input, input.length);
    }

    private static void check(int length, int mode, int prefix, int seed) {
        Object[] input = new Object[prefix + length + 3];
        for (int i = 0; i < input.length; i++) input[i] = new Object();
        Object[] original = input.clone();
        long[] mask = new long[(length + 63) >>> 6];
        for (int i = 0; i < length; i++) {
            boolean remove = switch (mode) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> (i & 1) == 0;
                case 3 -> i == length - 1;
                case 4 -> i == 0;
                case 5 -> i >= (length / 3) && i < (length / 2);
                default -> RANDOM.nextInt(5) == 0;
            };
            if (remove) mask[i >>> 6] |= 1L << (i & 63);
        }
        // Poison bits outside the selected range; they must not affect it.
        if (length > 0 && (length & 63) != 0) mask[mask.length - 1] |= -1L << (length & 63);
        int actualEnd = M3CollectionMaskCompactor.compact(input, prefix, prefix + length, mask);
        int expectedEnd = prefix;
        for (int i = 0; i < length; i++) {
            if ((mask[i >>> 6] & (1L << (i & 63))) == 0) {
                if (input[expectedEnd] != original[prefix + i]) {
                    throw new AssertionError("survivor moved incorrectly mode=" + mode + " length=" + length
                            + " seed=" + seed + " relative=" + i);
                }
                expectedEnd++;
                checks++;
            }
        }
        equal(expectedEnd, actualEnd);
        // The discarded tail remains untouched; ArrayList/Vector own reference clearing.
        for (int i = actualEnd; i < input.length; i++) {
            if (input[i] != original[i]) {
                throw new AssertionError("discarded tail changed at " + i);
            }
            checks++;
        }
        for (int i = 0; i < prefix; i++) {
            if (input[i] != original[i]) throw new AssertionError("prefix changed");
        }
    }

    private static void identity(Object[] expected, Object[] actual, int count) {
        for (int i = 0; i < count; i++) {
            if (expected[i] != actual[i]) throw new AssertionError("identity changed " + i);
            checks++;
        }
    }

    private static void equal(int expected, int actual) {
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
        checks++;
    }

    private static void fail(Class<? extends Throwable> type, Runnable runnable) {
        try { runnable.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) { checks++; return; }
            throw new AssertionError("Unexpected failure type", failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
