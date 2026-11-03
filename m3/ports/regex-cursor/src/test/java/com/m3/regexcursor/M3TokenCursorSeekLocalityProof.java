// SPDX-License-Identifier: Apache-2.0
// Independent contract oracle for current/adjacent-lane seek hint; test-only.
package com.m3.regexcursor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class M3TokenCursorSeekLocalityProof {
    private M3TokenCursorSeekLocalityProof() { }
    private static long tests;
    private static long digest = 0x5ee45ee45ee45ee4L;

    private static void equal(long expected, long actual, String label) {
        tests++;
        if (expected != actual) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
        digest = Long.rotateLeft(digest ^ expected ^ (tests << 1), 11) * 0x9e3779b97f4a7c15L;
    }

    private static int[] positions(int[][] input) {
        List<int[]> good = new ArrayList<>();
        for (int[] lane : input) if (lane.length != 0) good.add(lane);
        int[] starts = new int[good.size() + 1];
        for (int i = 0; i < good.size(); i++) starts[i + 1] = starts[i] + good.get(i).length;
        return starts;
    }

    private static int oracleLane(int[] start, int position) {
        if (start.length == 1) return 0;
        int index = Arrays.binarySearch(start, 0, start.length - 1, position);
        return index >= 0 ? index : -index - 2;
    }

    private static int expectedUnit(int[][] input, int[] start, int position) {
        int lane = oracleLane(start, position);
        int count = 0;
        for (int[] run : input) {
            if (run.length == 0) continue;
            if (count++ == lane) return run[position - start[lane]];
        }
        throw new AssertionError("reference out of bounds");
    }

    private static void test(int[][] runs, Random rnd, int trials, String scenario) {
        int[] boundaries = positions(runs);
        int total = boundaries[boundaries.length - 1];
        M3TokenCursor cursor = M3TokenCursor.lanes(runs);
        equal(total, cursor.totalLength(), scenario + " total");
        long presence = 0;
        for (int[] part : runs) for (int id : part) presence |= M3CursorFacts.bit(id);
        equal(presence, cursor.unitPresenceMask(), scenario + " presence");
        for (int test = 0; test < trials; test++) {
            int position;
            switch (test % 9) {
                case 0 -> position = 0;
                case 1 -> position = total;
                case 2 -> position = rnd.nextInt(total + 1);
                case 3 -> position = boundaries[rnd.nextInt(boundaries.length)];
                case 4 -> position = boundaries[Math.min(boundaries.length - 1, 1)];
                case 5 -> position = Math.max(0, total - 1);
                case 6 -> position = boundaries[Math.max(0, boundaries.length - 2)];
                case 7 -> position = rnd.nextBoolean() ? 0 : total;
                default -> position = rnd.nextInt(total + 1);
            }
            cursor.seek(position);
            int target = oracleLane(boundaries, position);
            equal(boundaries[target], cursor.offset(), scenario + " seek offset");
            int expectedLength = boundaries.length == 1 ? 0 : boundaries[target + 1] - boundaries[target];
            equal(expectedLength, cursor.chunkLength(), scenario + " length");
            if (position < total) {
                int expected = expectedUnit(runs, boundaries, position);
                equal(expected, cursor.unit(position - boundaries[target]), scenario + " unit after seek");
                equal(expected, cursor.unitAt(position), scenario + " unitAt");
            }
        }
        for (int invalid : new int[] {-1, total + 1}) {
            try {
                cursor.seek(invalid);
                throw new AssertionError(scenario + " invalid range accepted: " + invalid);
            } catch (IndexOutOfBoundsException expected) {
                equal(1, 1, scenario + " invalid rejection");
            }
        }
    }

    public static void main(String[] args) {
        Random rnd = new Random(0x7ee4deedL);
        test(new int[][] {}, rnd, 20, "empty");
        test(new int[][] {new int[0], new int[0]}, rnd, 20, "all-empty");
        test(new int[][] {new int[] {0}}, rnd, 100, "one");
        test(new int[][] {{1,2,3}, {}, {4}, {}, {5,6,7,8}}, rnd, 300, "heterogeneous");
        for (int sample = 0; sample < 45_000; sample++) {
            int count = rnd.nextInt(70);
            int[][] arrays = new int[count][];
            for (int lane = 0; lane < count; lane++) {
                int size = rnd.nextInt(9);
                arrays[lane] = new int[size];
                for (int i = 0; i < size; i++) arrays[lane][i] = rnd.nextInt(4096);
            }
            test(arrays, rnd, 25, "random" + sample);
        }
        System.out.printf("M3_SEEK_LOCALITY_ORACLE_PASS cases=%d digest=%016x%n", tests, digest);
    }
}
