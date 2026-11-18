/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3 sparse bit-lane word-range writes agree with BitSet and preserve rank/select
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED M3BitLane28RangeTest
 */

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.TreeSet;
import jdk.internal.mindex.M3Address28;
import jdk.internal.mindex.M3BitLane28;

public class M3BitLane28RangeTest {
    private static final int END = M3Address28.MAX_SLOTS;
    private static final int PAGE = M3Address28.VALUES_PER_BLOCK;
    private static final int REGION = 1 << 20;

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    @FunctionalInterface
    interface Action { void run(); }

    private static void invalid(Action action) {
        try { action.run(); }
        catch (IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("Expected IndexOutOfBoundsException");
    }

    /** Independent bitmap and sorted-coordinate models avoid huge sparse BitSet scans. */
    private static final class Model {
        private final BitSet bitmap = new BitSet();
        private final TreeSet<Integer> ordered = new TreeSet<>();

        void set(int from, int to) {
            bitmap.set(from, to);
            for (int p = from; p < to; p++) ordered.add(p);
        }

        void clear(int from, int to) {
            bitmap.clear(from, to);
            for (int p = from; p < to; p++) ordered.remove(p);
        }

        void flip(int from, int to) {
            bitmap.flip(from, to);
            for (int p = from; p < to; p++) {
                if (!ordered.add(p)) ordered.remove(p);
            }
        }

        boolean get(int position) { return bitmap.get(position); }
        int cardinality() { return bitmap.cardinality(); }

        int nextSetBit(int position) {
            Integer next = ordered.ceiling(position);
            return next == null ? -1 : next;
        }

        int previousSetBit(int position) {
            Integer previous = ordered.floor(position);
            return previous == null ? -1 : previous;
        }
    }

    private static int[] referencePositions(Model expected) {
        int[] positions = new int[expected.ordered.size()];
        int index = 0;
        for (int bit : expected.ordered) {
            check(expected.get(bit), "BitSet/TreeSet models agree at " + bit);
            positions[index++] = bit;
        }
        check(index == expected.cardinality(), "BitSet/TreeSet cardinality inventory");
        return positions;
    }

    private static void verify(M3BitLane28 actual, Model expected) {
        check(actual.cardinality() == expected.cardinality(), "exact total cardinality");
        int[] positions = referencePositions(expected);
        check(actual.payloadBytes() == (long) actual.allocatedBlockCount()
                        * M3Address28.OCCUPANCY_WORDS_PER_BLOCK * Long.BYTES,
                "word-plane accounting");
        int[] probes = {0, 1, 62, 63, 64, 65, 127, 128, PAGE - 2,
                PAGE - 1, PAGE, PAGE + 1, REGION - 1, REGION, REGION + 1,
                REGION + PAGE, END - PAGE, END - 64, END - 1, END};
        for (int p : probes) {
            int index = Arrays.binarySearch(positions, p);
            int rank = index >= 0 ? index : -(index + 1);
            check(actual.rank(p) == rank, "rank at " + p);
            check(actual.nextSetBit(p) == expected.nextSetBit(p), "next at " + p);
            if (p < END) {
                check(actual.get(p) == expected.get(p), "value at " + p);
                check(actual.previousSetBit(p) == expected.previousSetBit(p),
                        "previous at " + p);
            }
        }
        int cardinality = expected.cardinality();
        for (int ordinal : new int[] {-1, 0, 1, cardinality >>> 1,
                cardinality - 1, cardinality, cardinality + 1}) {
            int match = ordinal < 0 || ordinal >= positions.length
                    ? -1 : positions[ordinal];
            check(actual.select(ordinal) == match,
                    "select ordinal " + ordinal);
        }
        check(actual.previousSetBit(-1) == -1, "empty prefix");
        check(actual.cardinality(0, END) == cardinality, "full range count");
    }

    private static void fixedBoundaries() {
        M3BitLane28 actual = new M3BitLane28();
        Model expected = new Model();
        actual.clear(0, END);
        actual.set(END, END);
        actual.flip(END, END);
        actual.clear(END, END);
        check(actual.allocatedBlockCount() == 0, "zero and empty ranges must not allocate");
        actual.set(0, 0, true);
        actual.set(0, 0, false);
        check(actual.allocatedRegionCount() == 0, "empty conditional range");

        int[][] intervals = {{0, 1}, {63, 65}, {PAGE - 2, PAGE + 3},
                {REGION - 2, REGION + 2}, {END - 2, END}};
        for (int[] interval : intervals) {
            actual.set(interval[0], interval[1]);
            expected.set(interval[0], interval[1]);
            verify(actual, expected);
        }
        // Prepare rank/select first; all following writes must maintain the fact rows.
        check(actual.select(0) == expected.nextSetBit(0), "prepared count baseline");
        actual.flip(63, 65);
        expected.flip(63, 65);
        actual.clear(PAGE - 2, PAGE + 3);
        expected.clear(PAGE - 2, PAGE + 3);
        actual.set(REGION - 2, REGION + 2, false);
        expected.clear(REGION - 2, REGION + 2);
        actual.set(62, 128, true);
        expected.set(62, 128);
        actual.flip(62, 128);
        expected.flip(62, 128);
        verify(actual, expected);

        // A large fill is word-oriented, not 65,536 individual set() calls.
        actual.set(PAGE * 4, PAGE * 20);
        expected.set(PAGE * 4, PAGE * 20);
        verify(actual, expected);
        actual.clear(PAGE * 4, PAGE * 20);
        expected.clear(PAGE * 4, PAGE * 20);
        verify(actual, expected);

        invalid(() -> actual.set(-1, 1));
        invalid(() -> actual.set(2, 1));
        invalid(() -> actual.set(0, END + 1));
        invalid(() -> actual.set(-1, 1, true));
        invalid(() -> actual.set(-1, 1, false));
        invalid(() -> actual.clear(END + 1, END + 1));
        invalid(() -> actual.flip(END - 1, END + 1));
    }

    private static void randomizedAgainstBitSet() {
        M3BitLane28 actual = new M3BitLane28();
        Model expected = new Model();
        int[] anchors = {0, 32, 63, 64, 95, PAGE - 64, PAGE - 1,
                PAGE, PAGE + 1, REGION - PAGE, REGION - 64, REGION - 1,
                REGION, REGION + 1, REGION + PAGE, END - PAGE,
                END - 192, END - 64, END - 1};
        Random random = new Random(0x6d333131L);
        for (int step = 0; step < 4000; step++) {
            long shifted = (long) anchors[random.nextInt(anchors.length)]
                    + random.nextInt(129) - 64;
            int from = (int) Math.max(0L, Math.min((long) END, shifted));
            int to = (int) Math.min((long) END, from + (long) random.nextInt(145));
            switch (random.nextInt(5)) {
                case 0 -> { actual.set(from, to); expected.set(from, to); }
                case 1 -> { actual.clear(from, to); expected.clear(from, to); }
                case 2 -> { actual.flip(from, to); expected.flip(from, to); }
                case 3 -> { actual.set(from, to, false); expected.clear(from, to); }
                default -> { actual.set(from, to, true); expected.set(from, to); }
            }
            if (step % 83 == 0) verify(actual, expected);
        }
        verify(actual, expected);
    }

    public static void main(String[] args) {
        fixedBoundaries();
        randomizedAgainstBitSet();
        System.out.println("M3 sparse word-range bit-lane PASS");
    }
}
