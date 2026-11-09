// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3SortedIntSet;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.TreeSet;

/** Independent behavioral oracle; no production search/set implementation reused. */
public final class M3SortedIntSetOracle {
    private static long checks;

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason + " at check " + checks);
    }

    private static int[] set(SplittableRandom random, int count) {
        TreeSet<Integer> values = new TreeSet<>();
        for (int i = 0; i < count; i++) {
            int value = random.nextInt(9) == 0 ? random.nextInt() : random.nextInt(-96, 97);
            values.add(value);
        }
        int[] result = new int[values.size()];
        int at = 0;
        for (int value : values) result[at++] = value;
        return result;
    }

    private static int[] expected(int[] left, int[] right, String operation) {
        TreeSet<Integer> result = new TreeSet<>();
        TreeSet<Integer> rhs = new TreeSet<>();
        for (int value : right) rhs.add(value);
        if (operation.equals("intersection")) {
            for (int value : left) if (rhs.contains(value)) result.add(value);
        } else if (operation.equals("union")) {
            for (int value : left) result.add(value);
            result.addAll(rhs);
        } else {
            for (int value : left) if (!rhs.contains(value)) result.add(value);
        }
        int[] out = new int[result.size()];
        int at = 0;
        for (int value : result) out[at++] = value;
        return out;
    }

    private static void compare(int[] left, int[] right) {
        int[] a = left.clone();
        int[] b = right.clone();
        M3SortedIntSet lhs = M3SortedIntSet.copyOfSorted(left);
        M3SortedIntSet rhs = M3SortedIntSet.copyOfSorted(right);
        check(Arrays.equals(a, lhs.toArray()), "copy owner left");
        check(Arrays.equals(b, rhs.toArray()), "copy owner right");
        check(Arrays.equals(expected(a, b, "intersection"), lhs.intersection(rhs)), "intersection");
        check(Arrays.equals(expected(a, b, "union"), lhs.union(rhs)), "union");
        check(Arrays.equals(expected(a, b, "difference"), lhs.difference(rhs)), "difference");
        check(Arrays.equals(expected(b, a, "difference"), rhs.difference(lhs)), "reverse difference");
        check(lhs.size() == a.length, "size");
        int[] exported = lhs.toArray();
        if (exported.length != 0) exported[0] ^= 0x12345678;
        check(Arrays.equals(a, lhs.toArray()), "detached output");
        if (left.length > 0) left[0] ^= 0x321654;
        if (right.length > 0) right[0] ^= 0x451234;
        check(Arrays.equals(a, lhs.toArray()), "input left copy");
        check(Arrays.equals(b, rhs.toArray()), "input right copy");
        for (int value : new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE + 1,
                -123, -1, 0, 1, 123, Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) {
            int expectedPosition = 0;
            while (expectedPosition < a.length && a[expectedPosition] < value) expectedPosition++;
            check(lhs.lowerBound(value) == expectedPosition, "signed lowerBound");
            check(lhs.contains(value) == (expectedPosition < a.length && a[expectedPosition] == value),
                    "signed contains");
        }
    }

    private static void refuse(Runnable runnable) {
        boolean refused = false;
        try { runnable.run(); } catch (IllegalArgumentException expected) { refused = true; }
        check(refused, "expected bounded/ordering refusal");
    }

    public static void main(String[] arguments) {
        SplittableRandom random = new SplittableRandom(0x421679a72ef8L);
        compare(new int[0], new int[0]);
        compare(new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
                new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        compare(new int[0], new int[]{1, 2});
        compare(new int[]{1, 2}, new int[0]);
        for (int k = 0; k < 25000; k++) compare(set(random, random.nextInt(65)),
                set(random, random.nextInt(65)));
        for (int k = 0; k < 300; k++) compare(set(random, random.nextInt(1, 7)),
                set(random, random.nextInt(1400, 1900)));
        refuse(() -> M3SortedIntSet.copyOfSorted(new int[]{1, 1}));
        refuse(() -> M3SortedIntSet.copyOfSorted(new int[]{1, -1}));
        refuse(() -> M3SortedIntSet.copyOfSorted(new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}));
        // Fail closed before retaining or sorting input beyond the documented budget.
        refuse(() -> M3SortedIntSet.copyOfSorted(new int[M3SortedIntSet.MAX_ITEMS + 1]));
        boolean nullRefused = false;
        try { M3SortedIntSet.copyOfSorted(null); } catch (NullPointerException expected) { nullRefused = true; }
        check(nullRefused, "null source");
        // Resource boundary: exact output > cap is a refusal, not partial success.
        int[] evens = new int[M3SortedIntSet.MAX_ITEMS / 2 + 1];
        int[] odds = new int[M3SortedIntSet.MAX_ITEMS / 2 + 1];
        for (int i = 0; i < evens.length; i++) { evens[i] = i * 2; odds[i] = i * 2 + 1; }
        M3SortedIntSet a = M3SortedIntSet.copyOfSorted(evens);
        M3SortedIntSet b = M3SortedIntSet.copyOfSorted(odds);
        refuse(() -> a.union(b));
        check(a.intersection(b).length == 0, "large empty intersection");
        System.out.println("M3_SORTED_SET_ORACLE_PASS checks=" + checks);
    }
}
