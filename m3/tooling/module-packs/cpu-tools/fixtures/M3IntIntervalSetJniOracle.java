// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.m3.tools.cpu.M3IntIntervalSet;
import com.m3.tools.cpu.M3IntIntervalSetNative;
import java.util.Arrays;
import java.util.Random;

/** JNI parity/ownership/threshold oracle. Disabled-mode test is a separate invocation. */
public final class M3IntIntervalSetJniOracle {
    private static long checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label + " @" + checks);
    }
    private static void compare(M3IntIntervalSet index, int[] queries) {
        int[] saved = queries.clone();
        byte[] flags = M3IntIntervalSetNative.contains(index, queries);
        check(flags.length == queries.length, "count");
        check(Arrays.equals(queries, saved), "queries not modified");
        for (int i = 0; i < queries.length; i++) {
            check(flags[i] == (index.contains(queries[i]) ? 1 : 0), "JNI parity " + i);
        }
        byte[] another = M3IntIntervalSetNative.contains(index, queries);
        check(flags != another && Arrays.equals(flags, another), "detached output");
        if (flags.length != 0) {
            flags[0] ^= 1;
            check(Arrays.equals(another, M3IntIntervalSetNative.contains(index,queries)),
                    "caller may not mutate backend");
        }
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--disabled")) {
            try {
                M3IntIntervalSetNative.contains(M3IntIntervalSet.copyOf(new int[]{0,3}),
                        new int[]{0});
                throw new AssertionError("missing library was admitted");
            } catch (IllegalStateException expected) {
                System.out.println("M3_INTERVAL_NATIVE_DISABLED_PASS checks=1");
                return;
            }
        }
        Random rng = new Random(0x45784bc7L);
        for (int trial = 0; trial < 500; trial++) {
            int[] pairs = new int[rng.nextInt(35) * 2];
            for (int j = 0; j < pairs.length; j += 2) {
                int a = rng.nextInt(400)-200;
                pairs[j] = a;
                pairs[j + 1] = a + rng.nextInt(15);
            }
            M3IntIntervalSet index = M3IntIntervalSet.copyOf(pairs);
            int length = trial % 4 == 0 ? 64 : trial % 4 == 1 ? 63 : trial % 4 == 2 ? 65 : 256;
            int[] queries = new int[length];
            for (int i = 0; i < length; i++) {
                queries[i] = rng.nextInt(500)-250;
            }
            if (trial % 2 == 0) Arrays.sort(queries);
            compare(index, queries);
            compare(index, new int[0]);
        }
        int[] pairs = new int[1000];
        for (int i = 0; i < pairs.length; i += 2) {
            pairs[i] = i * 2 - 1500;
            pairs[i + 1] = pairs[i] + 2;
        }
        M3IntIntervalSet index = M3IntIntervalSet.copyOf(pairs);
        int[] sorted = new int[10_000];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i/2-2000;
        compare(index, sorted);
        compare(index, new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE,0,-1,1});
        compare(M3IntIntervalSet.copyOf(new int[0]),sorted);
        System.out.println("M3_INTERVAL_JNI_PASS checks=" + checks);
    }
}
