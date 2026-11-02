// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Dependency-free deterministic differential that JUnit and javac can both execute. */
public final class M3SparseLongIntArraySelfTest {
    private M3SparseLongIntArraySelfTest() {}

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        final long huge = (1L << 49) + 25_001;
        M3SparseLongIntArray actual = new M3SparseLongIntArray(huge);
        Map<Long, Integer> oracle = new HashMap<>();
        Random random = new Random(0x4d33535041525345L);
        long checks = 0;
        final int blockSize = M3SparseLongIntArray.BLOCK_SIZE;
        require(actual.length() == huge && actual.nonZeroCount() == 0, "initial long domain");
        require(actual.allocatedBlockCount() == 0, "initial sparse storage");
        checks += 2;
        long[] edges = {0, 1, blockSize - 1L, blockSize, blockSize + 1L,
                (long) Integer.MAX_VALUE - 1, (long) Integer.MAX_VALUE,
                (long) Integer.MAX_VALUE + 1, (1L << 32) - 1,
                (1L << 32), (1L << 40), huge - 2, huge - 1};
        for (int iteration = 0; iteration < 54_000; iteration++) {
            // Exercise many colliding/sparse blocks without making a 128 MiB
            // test heap materialize 54,000 distinct 16 KiB blocks.
            long blockBase = switch (random.nextInt(3)) {
                case 0 -> 0L;
                case 1 -> (1L << 32) >>> M3SparseLongIntArray.BLOCK_SHIFT;
                default -> (huge >>> M3SparseLongIntArray.BLOCK_SHIFT) - 1024;
            };
            long randomized = ((blockBase + random.nextInt(512))
                    << M3SparseLongIntArray.BLOCK_SHIFT) + random.nextInt(blockSize);
            long index = iteration % 3 == 0
                    ? edges[random.nextInt(edges.length)] : randomized;
            int newValue = switch (random.nextInt(8)) {
                case 0, 1, 2 -> 0;
                case 3 -> Integer.MIN_VALUE;
                case 4 -> Integer.MAX_VALUE;
                default -> random.nextInt(1001) - 500;
            };
            int before = oracle.getOrDefault(index, 0);
            int replaced = actual.set(index, newValue);
            if (newValue == 0) oracle.remove(index);
            else oracle.put(index, newValue);
            require(replaced == before, "old value parity");
            require(actual.get(index) == newValue, "current value parity");
            require(actual.nonZeroCount() == oracle.size(), "nonzero cardinality parity");
            checks += 3;
            if (iteration % 997 == 0) {
                Set<Long> physical = new HashSet<>();
                for (long key : oracle.keySet()) physical.add(key >>> M3SparseLongIntArray.BLOCK_SHIFT);
                require(actual.allocatedBlockCount() == physical.size(), "block ownership parity");
                // A read of an absent block may not allocate one.
                int blockCount = actual.allocatedBlockCount();
                require(actual.get(2L << 32) == oracle.getOrDefault(2L << 32, 0), "missing block zero");
                require(actual.allocatedBlockCount() == blockCount, "read allocates no block");
                checks += 3;
            }
            if (iteration % 3000 == 0) {
                M3SparseLongIntArray copy = actual.snapshot();
                require(copy.length() == actual.length() && copy.nonZeroCount() == actual.nonZeroCount(),
                        "snapshot metadata");
                int original = actual.get(index);
                actual.set(index, original == 0 ? 1 : 0);
                require(copy.get(index) == original, "snapshot detached storage");
                actual.set(index, original);
                checks += 2;
            }
        }

        // The required JNI backend is an independent zero-copy-ownership audit.
        // It deliberately fails rather than silently using Java when absent.
        if (System.getProperty("m3.arrays.native.path") != null) {
            require(actual.verifyNonZeroCountRequiredNative() == oracle.size(),
                    "native occupancy disagreement");
            checks++;
        }
        Map<Long, Integer> visited = new HashMap<>();
        actual.forEachNonZero((index, value) -> {
            Integer previous = visited.put(index, value);
            if (previous != null) throw new AssertionError("duplicate traversal position");
        });
        require(visited.equals(oracle), "nonzero traversal parity");
        checks++;

        final long boundary = (1L << 42) - 4;
        int[] supplied = {0, 3, 0, -17, 28, 0, Integer.MIN_VALUE,
                Integer.MAX_VALUE, 9, 0, -11, 8};
        actual.copyFrom(boundary, supplied, 0, supplied.length);
        int[] result = new int[supplied.length + 8];
        Arrays.fill(result, 999);
        actual.copyTo(boundary - 3, result, 4, supplied.length + 3);
        require(result[0] == 999 && result[result.length - 1] == 999, "destination guards");
        for (int i = 0; i < supplied.length; i++) {
            require(result[i + 7] == supplied[i], "range copy parity");
            checks++;
        }
        // Dense zero source must not allocate additional blocks.
        int blocksBefore = actual.allocatedBlockCount();
        long zeroStart = (1L << 47);
        actual.copyFrom(zeroStart, new int[16], 0, 16);
        require(actual.allocatedBlockCount() == blocksBefore, "zero bulk created a block");
        checks++;
        int[] preserved = result.clone();
        expect(IndexOutOfBoundsException.class,
                () -> actual.copyTo(huge - 5, result, 0, 6));
        require(Arrays.equals(result, preserved), "copyTo validation must precede writes");
        checks++;
        expect(IndexOutOfBoundsException.class, () -> actual.copyFrom(huge, supplied, 0, 1));
        expect(IndexOutOfBoundsException.class, () -> actual.set(huge, 1));
        expect(IndexOutOfBoundsException.class, () -> actual.get(-1));
        expect(IllegalArgumentException.class, () -> actual.growTo(-1));
        checks += 4;

        long visitedIndex = boundary + 1;
        expect(ConcurrentModificationException.class, () -> actual.forEachNonZero((index, value) -> {
            actual.set(visitedIndex, actual.get(visitedIndex) + 1);
        }));
        checks++;

        M3SparseLongIntArray emptied = new M3SparseLongIntArray(Long.MAX_VALUE);
        long last = Long.MAX_VALUE - 1;
        emptied.set(last, 37);
        require(emptied.get(last) == 37, "last signed-long position");
        require(emptied.allocatedBlockCount() == 1, "one physical block at end");
        emptied.set(last, 0);
        require(emptied.allocatedBlockCount() == 0 && emptied.nonZeroCount() == 0,
                "last zero must reclaim its block");
        emptied.growTo(Long.MAX_VALUE);
        require(emptied.length() == Long.MAX_VALUE, "growth upper bound");
        checks += 4;

        actual.clear();
        require(actual.length() == huge, "clear changed logical length");
        require(actual.nonZeroCount() == 0 && actual.allocatedBlockCount() == 0,
                "clear did not reclaim all blocks");
        for (long index : edges) require(actual.get(index) == 0, "cleared edge not zero");
        checks += 2 + edges.length;
        M3SparseLongIntArray grow = new M3SparseLongIntArray(0);
        expect(IndexOutOfBoundsException.class, () -> grow.get(0));
        grow.growTo((1L << 41) + 1);
        require(grow.length() == (1L << 41) + 1, "logical growth");
        require(grow.get(1L << 41) == 0, "new logical space is zero");
        require(grow.allocatedBlockCount() == 0, "grow is allocation-free");
        checks += 3;
        System.out.println("M3_SPARSE_LONG_INT_PASS checks=" + checks
                + " logicalLength=" + huge + " seededOperations=54000");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong refusal: " + failure, failure);
        }
        throw new AssertionError("missing expected refusal: " + type.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
