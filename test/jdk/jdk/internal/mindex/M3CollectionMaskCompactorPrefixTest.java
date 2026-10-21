/*
 * @test
 * @summary M3JDK21 array-offset deletion bitmask prefix-safe compaction
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3CollectionMaskCompactorPrefixTest
 */
import java.util.Arrays;
import java.util.Random;
import jdk.internal.mindex.M3CollectionMaskCompactor;

public final class M3CollectionMaskCompactorPrefixTest {
    private static long assertions;

    public static void main(String[] args) {
        Random random = new Random(0x21_2026_1021L);
        int[] sizes = {0, 1, 2, 3, 31, 63, 64, 65, 66, 127, 128,
                255, 256, 510, 511, 512, 513, 1023, 1024, 1025, 4095};
        for (int length : sizes) {
            for (int offset : new int[]{0, 1, 13}) {
                for (int shape = 0; shape < 9; shape++) {
                    verify(length, offset, shape, random);
                }
            }
        }
        for (int trial = 0; trial < 2000; trial++) {
            verify(random.nextInt(2048), random.nextInt(31), 8, random);
        }
        verifyErrors();
        System.out.println("PASS M3JDK21 direct java.base mask-prefix checks=" + assertions);
    }

    private static void verify(int length, int offset, int shape, Random random) {
        int tail = 5;
        Object[] original = new Object[offset + length + tail];
        for (int i = 0; i < original.length; i++) {
            original[i] = i % 7 == 0 ? null : new Object();
        }
        Object[] expected = original.clone();
        Object[] actual = original.clone();
        int words = Math.max(1, (length + 63) / 64);
        long[] bits = new long[words + 2];
        for (int i = 0; i < length; i++) {
            boolean delete = switch (shape) {
                case 0 -> false;
                case 1 -> true;
                case 2 -> i == 0;
                case 3 -> i == length - 1;
                case 4 -> i == length / 2;
                case 5 -> i >= length / 2;
                case 6 -> i % 65 == 0;
                case 7 -> i % 3 == 0;
                default -> random.nextBoolean();
            };
            if (delete) bits[i >>> 6] |= 1L << (i & 63);
        }
        // Deliberately poison unused words/bits beyond the selected physical range.
        if (length > 0 && (length & 63) != 0) {
            bits[(length - 1) >>> 6] |= -1L << (length & 63);
        }
        bits[words] = -1L;
        bits[words + 1] = -1L;
        int target = offset;
        for (int i = 0; i < length; i++) {
            if ((bits[i >>> 6] & (1L << (i & 63))) == 0) {
                expected[target++] = original[offset + i];
            }
        }
        int actualEnd = M3CollectionMaskCompactor.compact(actual, offset, offset + length, bits);
        yes(actualEnd == target, "survivor end");
        for (int i = 0; i < actual.length; i++) {
            if (i < offset || i >= actualEnd) {
                // The compactor owns only the selected output prefix, never cleared tail.
                if (i >= actualEnd && i < offset + length) {
                    // Overlapping compaction may have written into former removed slots
                    // within output prefix only. Everything at/after end is untouched.
                }
                yes(actual[i] == original[i], "outside output changed at " + i);
            } else {
                yes(actual[i] == expected[i], "physical survivor reference changed " + i);
            }
        }
        // Arrays of references must preserve nulls and duplicate object identity.
        yes(Arrays.equals(Arrays.copyOf(actual, actualEnd), Arrays.copyOf(expected, target)), "prefix");
    }

    private static void verifyErrors() {
        error(NullPointerException.class, () -> M3CollectionMaskCompactor.compact(null, 0, 0, new long[0]));
        error(NullPointerException.class, () -> M3CollectionMaskCompactor.compact(new Object[1], 0, 1, null));
        error(IndexOutOfBoundsException.class, () -> M3CollectionMaskCompactor.compact(new Object[4], -1, 2, new long[1]));
        error(IndexOutOfBoundsException.class, () -> M3CollectionMaskCompactor.compact(new Object[4], 0, 5, new long[1]));
        error(IllegalArgumentException.class, () -> M3CollectionMaskCompactor.compact(new Object[65], 0, 65, new long[1]));
        yes(M3CollectionMaskCompactor.compact(new Object[0], 0, 0, new long[0]) == 0, "empty");
    }

    private interface Action { void call(); }
    private static void error(Class<? extends Throwable> expected, Action task) {
        try { task.call(); } catch (Throwable thrown) {
            yes(expected.isInstance(thrown), "expected=" + expected.getName() + " got=" + thrown);
            return;
        }
        throw new AssertionError("missing expected " + expected.getName());
    }
    private static void yes(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message + " @" + assertions);
    }
}
