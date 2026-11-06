// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

/** Independent multiplicity-frequency reference corpus, runnable without JUnit. */
public final class M3SortedIntMultisetsSelfTest {
    private M3SortedIntMultisetsSelfTest() { }
    private static long checks;

    private static void check(boolean ok, String explanation) {
        checks++;
        if (!ok) throw new AssertionError(explanation + " check=" + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            check(type.isInstance(error), "wrong exception: " + error);
            return;
        }
        throw new AssertionError("missing exception: " + type);
    }

    private static int[] oracle(int[] a, int[] b, M3SortedIntMultisets.Operation operation) {
        TreeMap<Integer, int[]> counts = new TreeMap<>();
        for (int x : a) counts.computeIfAbsent(x, ignored -> new int[2])[0]++;
        for (int x : b) counts.computeIfAbsent(x, ignored -> new int[2])[1]++;
        int size = 0;
        for (int x : counts.keySet()) {
            int[] count = counts.get(x);
            size += multiplicity(count[0], count[1], operation);
        }
        int[] result = new int[size];
        int offset = 0;
        for (int x : counts.keySet()) {
            int[] count = counts.get(x);
            int take = multiplicity(count[0], count[1], operation);
            Arrays.fill(result, offset, offset + take, x);
            offset += take;
        }
        return result;
    }

    private static int multiplicity(int a, int b, M3SortedIntMultisets.Operation operation) {
        return switch (operation) {
            case UNION_MAX -> Math.max(a, b);
            case INTERSECTION_MIN -> Math.min(a, b);
            case SUBTRACT -> Math.max(0, a - b);
            case SYMMETRIC_DIFFERENCE -> Math.abs(a - b);
        };
    }

    private static void exercise(int[] a, int[] b, boolean nativeEnabled) {
        int[] oldA = a.clone();
        int[] oldB = b.clone();
        for (var op : M3SortedIntMultisets.Operation.values()) {
            int[] expected = oracle(a, b, op);
            int[] java = M3SortedIntMultisets.apply(a, b, op);
            check(Arrays.equals(expected, java), "Java " + op + " with A=" + a.length + " B=" + b.length);
            if (nativeEnabled) {
                int[] actual = M3SortedIntMultisets.applyRequiredNative(a, b, op);
                check(Arrays.equals(expected, actual), "JNI " + op + " with A=" + a.length + " B=" + b.length);
                if (actual.length > 0) {
                    int before = actual[0];
                    actual[0]++;
                    check(a.length == 0 || Arrays.equals(oldA, a), "JNI output aliases input");
                    actual[0] = before;
                }
            }
        }
        check(Arrays.equals(oldA, a), "left source mutated");
        check(Arrays.equals(oldB, b), "right source mutated");
    }

    public static void run() {
        checks = 0;
        String configured = System.getProperty("m3.arrays.native.path");
        boolean nativeEnabled = M3ArrayNative.isLoaded() || (configured != null && !configured.isBlank());
        if (nativeEnabled && !M3ArrayNative.isLoaded()) M3ArrayNative.load(Path.of(configured));
        exercise(new int[0], new int[0], nativeEnabled);
        exercise(new int[] {1, 1, 1}, new int[] {1, 1}, nativeEnabled);
        exercise(new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE},
                new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE}, nativeEnabled);
        exercise(new int[] {0, 0, 0, 0}, new int[] {1, 1, 1}, nativeEnabled);
        exercise(new int[] {9, 9, 9}, new int[0], nativeEnabled);
        exercise(new int[0], new int[] {7, 7, 7}, nativeEnabled);
        Random random = new Random(0x4d33534f52544d55L);
        for (int iteration = 0; iteration < (nativeEnabled ? 2_400 : 20_000); iteration++) {
            int an = random.nextInt(74), bn = random.nextInt(74);
            int[] a = new int[an], b = new int[bn];
            for (int i = 0; i < an; i++) a[i] = switch (i % 13) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2, 3, 4 -> 0;
                default -> random.nextInt(51) - 25;
            };
            for (int i = 0; i < bn; i++) b[i] = switch (i % 11) {
                case 0 -> Integer.MAX_VALUE;
                case 1 -> Integer.MIN_VALUE;
                case 2, 3 -> 0;
                default -> random.nextInt(51) - 25;
            };
            Arrays.sort(a);
            Arrays.sort(b);
            exercise(a, b, nativeEnabled);
        }
        int[] largeA = new int[4097], largeB = new int[8193];
        for (int i = 0; i < largeA.length; i++) largeA[i] = i / 2 - 2100;
        for (int i = 0; i < largeB.length; i++) largeB[i] = i / 3 - 2100;
        exercise(largeA, largeB, nativeEnabled);
        expect(IllegalArgumentException.class,
                () -> M3SortedIntMultisets.union(new int[] {2, 1}, new int[] {3}));
        expect(IllegalArgumentException.class,
                () -> M3SortedIntMultisets.union(new int[] {1}, new int[] {2, 1}));
        expect(NullPointerException.class,
                () -> M3SortedIntMultisets.apply(null, new int[0], M3SortedIntMultisets.Operation.UNION_MAX));
        expect(NullPointerException.class,
                () -> M3SortedIntMultisets.apply(new int[0], null, M3SortedIntMultisets.Operation.UNION_MAX));
        expect(NullPointerException.class,
                () -> M3SortedIntMultisets.apply(new int[0], new int[0], null));
        if (!nativeEnabled) {
            expect(UnsatisfiedLinkError.class, () -> M3SortedIntMultisets.applyRequiredNative(
                    new int[0], new int[0], M3SortedIntMultisets.Operation.INTERSECTION_MIN));
        }
        System.out.println("M3_MULTISET_PASS checks=" + checks + " native=" + nativeEnabled);
    }

    public static void main(String[] args) { run(); }
}
