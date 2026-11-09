// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3SortedIntMultiset;
import jdk.m3.tools.cpu.M3SortedIntMultisetNative;
import java.util.Arrays;
import java.util.SplittableRandom;

/** Differential JVM/JNI oracle; separate process tests disabled-native loading. */
public final class M3SortedIntMultisetJniOracle {
    private static long checks;

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " at check " + checks);
    }

    private static int[] sorted(SplittableRandom r, int n) {
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            int kind = r.nextInt(14);
            out[i] = kind == 0 ? Integer.MIN_VALUE
                    : kind == 1 ? Integer.MAX_VALUE
                    : kind == 2 ? r.nextInt()
                    : r.nextInt(-12, 13);
        }
        Arrays.sort(out);
        return out;
    }

    private static void compare(int[] a, int[] b) {
        M3SortedIntMultiset left = M3SortedIntMultiset.copyOfSorted(a);
        M3SortedIntMultiset right = M3SortedIntMultiset.copyOfSorted(b);
        int[] expected = left.intersection(right);
        int[] actual = M3SortedIntMultisetNative.intersection(left, right);
        check(Arrays.equals(expected, actual), "JNI/Java exact duplicate intersection");
        if (actual.length > 0) actual[0] ^= 0x4bc;
        check(Arrays.equals(expected, left.intersection(right)), "native output detached");
        check(Arrays.equals(a, left.toArray()), "native input a unchanged");
        check(Arrays.equals(b, right.toArray()), "native input b unchanged");
    }

    public static void main(String[] args) {
        if (System.getProperty("m3.cpu.multiset.native.path") == null) {
            boolean refused = false;
            try {
                M3SortedIntMultisetNative.intersection(
                        M3SortedIntMultiset.copyOfSorted(new int[]{2, 2}),
                        M3SortedIntMultiset.copyOfSorted(new int[]{2}));
            } catch (IllegalStateException expected) { refused = true; }
            check(refused, "absent native property must refuse");
            System.out.println("M3_MULTISET_JNI_DISABLED_PASS checks=" + checks);
            return;
        }
        compare(new int[0], new int[0]);
        compare(new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, 0, 0, 0, Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE});
        compare(new int[]{7, 7, 7, 7}, new int[]{7, 7});
        SplittableRandom random = new SplittableRandom(0xc4cd3116L);
        for (int k = 0; k < 24_000; k++) {
            compare(sorted(random, random.nextInt(130)), sorted(random, random.nextInt(130)));
        }
        for (int k = 0; k < 250; k++) {
            compare(sorted(random, random.nextInt(1, 8)),
                    sorted(random, random.nextInt(2200, 3200)));
        }
        System.out.println("M3_MULTISET_JNI_PARITY_PASS checks=" + checks);
    }
}
