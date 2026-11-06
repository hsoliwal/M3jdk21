// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Deterministic reference oracles that can run with only javac/JVM, or under JUnit. */
public final class M3IntLaneAlgorithmsSelfTest {
    private M3IntLaneAlgorithmsSelfTest() {}

    public static void run() {
        Random random = new Random(0x4d334c4545544cL);
        long operations = 0;
        String library = System.getProperty("m3.arrays.native.path");
        boolean nativeRequired = library != null && !library.isBlank();
        if (nativeRequired) M3ArrayNative.load(Path.of(library));
        for (int iteration = 0; iteration < 12_000; iteration++) {
            int length = random.nextInt(300);
            int[] original = new int[length];
            for (int i = 0; i < length; i++) {
                original[i] = switch (random.nextInt(13)) {
                    case 0 -> Integer.MIN_VALUE;
                    case 1 -> Integer.MAX_VALUE;
                    default -> random.nextInt(80) - 40;
                };
            }
            Arrays.sort(original);
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int key = switch (iteration % 11) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                default -> random.nextInt(100) - 50;
            };
            int lower = from;
            while (lower < to && original[lower] < key) lower++;
            int upper = from;
            while (upper < to && original[upper] <= key) upper++;
            require(lower == M3IntLaneAlgorithms.lowerBound(original, from, to, key), "lower bound");
            require(upper == M3IntLaneAlgorithms.upperBound(original, from, to, key), "upper bound");
            require(Arrays.equals(new int[] {lower, upper},
                    M3IntLaneAlgorithms.equalRange(original, from, to, key)), "equal range");
            operations += 3;
            if (nativeRequired) {
                require(lower == M3IntLaneAlgorithms.lowerBoundRequired(original, from, to, key), "JNI lower bound");
                require(upper == M3IntLaneAlgorithms.upperBoundRequired(original, from, to, key), "JNI upper bound");
                operations += 2;
            }
        }
        // Exercise both JNI's 1,024-element copy boundary and long-range bisection.
        int[] largeSizes = {0, 1, 1023, 1024, 1025, 2048, 65_536};
        for (int length : largeSizes) {
            int[] sorted = new int[length];
            for (int at = 0; at < length; at++) sorted[at] = at / 3 - 20_000;
            int[] keys = {Integer.MIN_VALUE, -20_000, -19_999, 0,
                    20_000, Integer.MAX_VALUE};
            for (int key : keys) {
                int first = M3IntLaneAlgorithms.lowerBound(sorted, 0, length, key);
                int last = M3IntLaneAlgorithms.upperBound(sorted, 0, length, key);
                if (nativeRequired) {
                    require(first == M3IntLaneAlgorithms.lowerBoundRequired(sorted, 0, length, key),
                            "JNI large lower");
                    require(last == M3IntLaneAlgorithms.upperBoundRequired(sorted, 0, length, key),
                            "JNI large upper");
                }
                operations += 2;
            }
        }
        for (int iteration = 0; iteration < 15_000; iteration++) {
            int length = random.nextInt(201);
            int[] initial = new int[length];
            for (int i = 0; i < length; i++) initial[i] = random.nextInt(17) - 8;
            int from = random.nextInt(length + 1);
            int to = from + random.nextInt(length - from + 1);
            int rejected = random.nextInt(17) - 8;
            int[] actual = initial.clone();
            int written = M3IntLaneAlgorithms.compactExcept(actual, from, to, rejected);
            int logical = from;
            for (int i = from; i < to; i++) {
                if (initial[i] != rejected) require(actual[logical++] == initial[i], "stable compaction");
            }
            require(logical == written, "compaction count");
            require(Arrays.equals(Arrays.copyOfRange(actual, 0, from),
                    Arrays.copyOfRange(initial, 0, from)), "compaction prefix changed");
            require(Arrays.equals(Arrays.copyOfRange(actual, to, length),
                    Arrays.copyOfRange(initial, to, length)), "compaction suffix changed");
            operations += 3;

            int rotation = switch (iteration % 5) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                default -> random.nextInt(800) - 400;
            };
            actual = initial.clone();
            M3IntLaneAlgorithms.rotateLeft(actual, from, to, rotation);
            int n = to - from;
            for (int at = 0; at < n; at++) {
                require(actual[from + at] == initial[from + ((at + Math.floorMod(rotation, n)) % n)],
                        "left rotation");
            }
            require(Arrays.equals(Arrays.copyOfRange(actual, 0, from),
                    Arrays.copyOfRange(initial, 0, from)), "rotation prefix changed");
            require(Arrays.equals(Arrays.copyOfRange(actual, to, length),
                    Arrays.copyOfRange(initial, to, length)), "rotation suffix changed");
            operations += 3;
            if (length != 0) {
                int window = 1 + random.nextInt(length);
                int[] windows = M3IntLaneAlgorithms.slidingWindowMaximum(initial, window);
                require(windows.length == length - window + 1, "window result length");
                for (int k = 0; k < windows.length; k++) {
                    int maximum = Integer.MIN_VALUE;
                    for (int m = k; m < k + window; m++) maximum = Math.max(maximum, initial[m]);
                    require(windows[k] == maximum, "monotonic deque");
                }
                operations++;
            }
        }
        for (int iteration = 0; iteration < 10_000; iteration++) {
            int length = random.nextInt(80);
            int queryCount = length == 0 ? 0 : random.nextInt(30);
            int[] starts = new int[queryCount];
            int[] ends = new int[queryCount];
            long[] deltas = new long[queryCount];
            long[] slow = new long[length];
            for (int i = 0; i < queryCount; i++) {
                int left = random.nextInt(length);
                int right = left + random.nextInt(length - left);
                long change = random.nextInt(201) - 100;
                starts[i] = left;
                ends[i] = right;
                deltas[i] = change;
                for (int at = left; at <= right; at++) slow[at] += change;
            }
            long expected = length == 0 ? 0 : Arrays.stream(slow).max().orElseThrow();
            require(expected == M3IntLaneAlgorithms.rangeAddMaximum(length, starts, ends, deltas),
                    "range-add maximum");
            operations++;
        }
        int[] extreme = {Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        require(M3IntLaneAlgorithms.lowerBound(extreme, 0, 3, Integer.MAX_VALUE) == 2, "signed lower extreme");
        require(M3IntLaneAlgorithms.upperBound(extreme, 0, 3, Integer.MAX_VALUE) == 3, "signed upper extreme");
        require(M3IntLaneAlgorithms.rangeAddMaximum(3, new int[] {0}, new int[] {2},
                new long[] {-5}) == -5, "full negative update");
        require(M3IntLaneAlgorithms.rangeAddMaximum(0, new int[0], new int[0], new long[0]) == 0,
                "empty differences");
        expect(IndexOutOfBoundsException.class,
                () -> M3IntLaneAlgorithms.lowerBound(extreme, 0, 4, 1));
        expect(IndexOutOfBoundsException.class,
                () -> M3IntLaneAlgorithms.rotateLeft(extreme, -1, 3, 1));
        expect(IllegalArgumentException.class,
                () -> M3IntLaneAlgorithms.slidingWindowMaximum(extreme, 0));
        expect(IllegalArgumentException.class,
                () -> M3IntLaneAlgorithms.rangeAddMaximum(3, new int[1], new int[0], new long[1]));
        expect(ArithmeticException.class,
                () -> M3IntLaneAlgorithms.rangeAddMaximum(2, new int[] {0}, new int[] {0},
                        new long[] {Long.MIN_VALUE}));
        System.out.println("M3_LEETCODIFY_ARRAY_KERNELS PASS checks=" + operations
                + " native=" + nativeRequired);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable found) {
            if (type.isInstance(found)) return;
            throw new AssertionError("wrong exception: " + found, found);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        run();
    }
}
