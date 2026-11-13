// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3SortedIntSet;
import jdk.m3.tools.cpu.M3SortedIntSetNative;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.TreeSet;

/** Executable Java/JNI parity proof; tests explicit absent library and actual native loading. */
public final class M3SortedIntSetJniOracle {
    private static long checks;

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message + " at check " + checks);
    }

    private static int[] sorted(SplittableRandom random, int size) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int i = 0; i < size; i++) {
            int value = random.nextInt(8) == 0 ? random.nextInt() : random.nextInt(-200, 201);
            set.add(value);
        }
        int[] lane = new int[set.size()];
        int at = 0;
        for (int v : set) lane[at++] = v;
        return lane;
    }

    private static void compare(int[] a, int[] b) {
        M3SortedIntSet left = M3SortedIntSet.copyOfSorted(a);
        M3SortedIntSet right = M3SortedIntSet.copyOfSorted(b);
        int[] original = left.intersection(right);
        int[] nativeCopy = M3SortedIntSetNative.intersection(left, right);
        check(Arrays.equals(original, nativeCopy), "native/java exact signed intersection");
        if (nativeCopy.length > 0) nativeCopy[0] ^= 0x7ab5;
        check(Arrays.equals(original, left.intersection(right)), "native output detached");
        check(Arrays.equals(a, left.toArray()), "native left unchanged");
        check(Arrays.equals(b, right.toArray()), "native right unchanged");
    }

    public static void main(String[] arguments) {
        if (System.getProperty("m3.cpu.sorted.native.path") == null) {
            boolean refused = false;
            try {
                M3SortedIntSetNative.intersection(M3SortedIntSet.copyOfSorted(new int[]{1}),
                        M3SortedIntSet.copyOfSorted(new int[]{1}));
            } catch (IllegalStateException expected) { refused = true; }
            check(refused, "native must refuse without opt-in path");
            System.out.println("M3_SORTED_JNI_DISABLED_PASS checks=" + checks);
            return;
        }
        compare(new int[0], new int[0]);
        compare(new int[0], new int[]{1});
        compare(new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        SplittableRandom random = new SplittableRandom(0x23df76402abL);
        for (int k = 0; k < 22000; k++) compare(sorted(random, random.nextInt(128)),
                sorted(random, random.nextInt(128)));
        for (int k = 0; k < 200; k++) compare(sorted(random, random.nextInt(1, 9)),
                sorted(random, random.nextInt(2500, 3100)));
        System.out.println("M3_SORTED_JNI_PARITY_PASS checks=" + checks);
    }
}
