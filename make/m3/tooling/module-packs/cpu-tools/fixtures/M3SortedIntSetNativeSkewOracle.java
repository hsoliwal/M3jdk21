// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3SortedIntSet;
import jdk.m3.tools.cpu.M3SortedIntSetNative;
import java.util.Arrays;
import java.util.SplittableRandom;

/** Executable Java/JNI signed-order, skew, symmetry and ownership parity fixture. */
public final class M3SortedIntSetNativeSkewOracle {
    private static long checks;
    private static void check(boolean valid, String context) {
        checks++;
        if (!valid) throw new AssertionError(context + " at assertion " + checks);
    }
    private static int[] lane(int start, int step, int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = start + i * step;
        return result;
    }
    private static void compare(int[] a, int[] b) {
        M3SortedIntSet left = M3SortedIntSet.copyOfSorted(a);
        M3SortedIntSet right = M3SortedIntSet.copyOfSorted(b);
        int[] javaOutput = left.intersection(right);
        int[] nativeOutput = M3SortedIntSetNative.intersection(left, right);
        check(Arrays.equals(javaOutput, nativeOutput), "jni equals java");
        check(Arrays.equals(nativeOutput, M3SortedIntSetNative.intersection(right, left)),
                "symmetry");
        if (nativeOutput.length != 0) nativeOutput[0] ^= 0x7621;
        check(Arrays.equals(javaOutput, left.intersection(right)), "detached native output");
        check(Arrays.equals(a, left.toArray()) && Arrays.equals(b, right.toArray()),
                "input owner unchanged");
    }
    public static void main(String[] args) {
        compare(new int[0], new int[0]);
        compare(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE});
        compare(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, new int[0]);
        compare(new int[]{7}, lane(-1000000, 1, 100000));
        compare(new int[]{-999999, 0, 999999}, lane(-1000000, 1, 100000));
        SplittableRandom random = new SplittableRandom(0x66ea3c190911L);
        for (int k = 0; k < 7000; k++) {
            int small = random.nextInt(0, 20);
            int large = random.nextInt(18, 9000);
            int start = random.nextInt(-1000000, 1000000);
            int step = random.nextInt(1, 5);
            int[] a = lane(start, step, small);
            int[] b = lane(start, step, large);
            if (random.nextBoolean()) compare(a, b);
            else compare(b, a);
        }
        for (int k = 0; k < 90; k++) {
            int small = random.nextInt(1, 30);
            int[] a = lane(-200000 + k, 700, small);
            int[] b = lane(-200000, 1, 200000);
            compare(a, b);
            compare(b, a);
        }
        System.out.println("M3_SORTED_JNI_SKEW_PASS checks=" + checks);
    }
}
