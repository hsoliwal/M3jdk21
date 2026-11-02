// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3IntIntervalAlgebra;
import com.m3.tools.cpu.M3IntIntervalAlgebraNative;
import com.m3.tools.cpu.M3IntIntervalSet;
import java.util.Arrays;
import java.util.Random;

/** Differential JNI/Java interval intersection with detached outputs and signed bounds. */
public final class M3IntIntervalAlgebraJniOracle {
    private static long checks;
    private static void check(boolean p, String label) {
        checks++; if (!p) throw new AssertionError(label + " assertion " + checks);
    }
    private static void compare(int[] left, int[] right) {
        var a = M3IntIntervalSet.copyOf(left);
        var b = M3IntIntervalSet.copyOf(right);
        int[] aBefore = a.toPairs(), bBefore = b.toPairs();
        int[] expected = M3IntIntervalAlgebra.intersection(a, b).toPairs();
        var nativeResult = M3IntIntervalAlgebraNative.intersection(a, b);
        check(Arrays.equals(expected, nativeResult.toPairs()), "JNI parity");
        check(Arrays.equals(expected, M3IntIntervalAlgebraNative.intersection(b, a).toPairs()), "symmetric");
        check(Arrays.equals(aBefore, a.toPairs()) && Arrays.equals(bBefore, b.toPairs()), "owners unchanged");
        int[] exported = nativeResult.toPairs();
        if (exported.length != 0) exported[0] ^= 0x4567;
        check(Arrays.equals(expected, nativeResult.toPairs()), "result export detached");
    }
    public static void main(String[] args) {
        compare(new int[0], new int[0]);
        compare(new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}, new int[]{-1, 1});
        compare(new int[]{0, 1}, new int[]{1, 2}); // half-open endpoint equality is empty
        compare(new int[]{0, 10, 20, 30}, new int[]{9, 21});
        Random r = new Random(0x4d334a4e495aL);
        for (int trial = 0; trial < 8500; trial++) {
            int[] a = new int[2 * r.nextInt(12)];
            int[] b = new int[2 * r.nextInt(12)];
            for (int[] arr : new int[][]{a,b}) {
                for (int k = 0; k < arr.length; k += 2) {
                    int v = r.nextInt(400)-200, w = r.nextInt(400)-200;
                    arr[k] = Math.min(v,w); arr[k+1] = Math.max(v,w);
                }
            }
            compare(a, b);
        }
        System.out.println("M3_INTERVAL_ALGEBRA_JNI_PASS checks=" + checks);
    }
}
