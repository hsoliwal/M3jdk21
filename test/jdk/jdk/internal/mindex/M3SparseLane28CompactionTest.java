/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Sparse M3 primitive/bit lanes reclaim empty pages without altering logical slots
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm --add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED M3SparseLane28CompactionTest
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import jdk.internal.mindex.M3Address28;
import jdk.internal.mindex.M3BitLane28;
import jdk.internal.mindex.M3IntLane28;
import jdk.internal.mindex.M3LongLane28;

public class M3SparseLane28CompactionTest {
    private static final int END = M3Address28.MAX_SLOTS;
    private static final int LAST = M3Address28.MAX_SLOT;
    private static final int REGION = 1 << 20;
    private static final int PAGE = M3Address28.VALUES_PER_BLOCK;

    private static void check(boolean actual, String problem) {
        if (!actual) throw new AssertionError(problem);
    }

    @FunctionalInterface
    interface Action { void run(); }

    private static void invalid(Action action) {
        try { action.run(); }
        catch (IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("Expected IndexOutOfBoundsException");
    }

    private static void intLane() {
        M3IntLane28 lane = new M3IntLane28();
        check(lane.compact() == 0 && lane.allocatedBlockCount() == 0, "empty int compact");
        lane.set(0, 1);
        lane.set(PAGE - 1, 2); // same leaf
        lane.set(PAGE, 3);
        lane.set(REGION, 4);
        lane.set(LAST, 5);
        check(lane.allocatedBlockCount() == 4 && lane.allocatedRegionCount() == 3,
                "int initial geometry");
        lane.clear(PAGE);
        lane.clear(REGION);
        check(lane.allocatedBlockCount() == 4, "int clear must not scan hot pages");
        check(lane.compact() == 2, "int released leaf count");
        check(lane.allocatedBlockCount() == 2 && lane.allocatedRegionCount() == 2,
                "int released directories");
        check(lane.get(0) == 1 && lane.get(PAGE - 1) == 2 && lane.get(LAST) == 5,
                "int live payload preservation");
        check(lane.get(PAGE) == 0 && lane.get(REGION) == 0
                && !lane.isBlockAllocated(PAGE) && !lane.isBlockAllocated(REGION),
                "int zero pages no longer retained");
        check(lane.compact() == 0, "int fixed point");

        lane.fill(PAGE, PAGE + 4, 31);
        check(lane.isBlockAllocated(PAGE) && lane.get(PAGE + 3) == 31,
                "int reallocation after compact");
        lane.fill(PAGE, PAGE + 4, 0);
        check(lane.compact() == 1, "int zero fill reclaimed");

        int[] imported = {0, -7, 0, 127};
        lane.copyFrom(imported, 0, REGION, imported.length);
        int[] exported = new int[6];
        lane.copyTo(REGION, exported, 1, imported.length);
        for (int i = 0; i < imported.length; i++) {
            check(exported[i + 1] == imported[i], "int copy zero/nonzero " + i);
        }
        check(lane.compact() == 0, "int nonzero copy should remain");
        lane.copyFrom(new int[4], 0, REGION, 4);
        check(lane.compact() == 1, "int zero copy releases page");
        invalid(() -> lane.set(END, 1));
        invalid(() -> lane.fill(-1, 0, 0));
        invalid(() -> lane.copyFrom(new int[] {1}, 0, END, 1));

        lane.clear(0);
        lane.clear(PAGE - 1);
        lane.clear(LAST);
        check(lane.compact() == 2 && lane.allocatedRegionCount() == 0,
                "int complete release");
    }

    private static void longLane() {
        M3LongLane28 lane = new M3LongLane28();
        check(lane.compact() == 0, "empty long compact");
        lane.set(0, Long.MIN_VALUE);
        lane.set(PAGE - 1, Long.MAX_VALUE);
        lane.set(PAGE, -1);
        lane.set(REGION, 42);
        lane.set(LAST, 99);
        check(lane.allocatedBlockCount() == 4 && lane.allocatedRegionCount() == 3,
                "long initial geometry");
        lane.fill(PAGE, PAGE + 1, 0L);
        lane.fill(REGION, REGION + 1, 0L);
        check(lane.allocatedBlockCount() == 4, "long clear retains physical pages");
        check(lane.compact() == 2 && lane.allocatedRegionCount() == 2,
                "long released empty pages and regions");
        check(lane.get(0) == Long.MIN_VALUE && lane.get(PAGE - 1) == Long.MAX_VALUE
                && lane.get(LAST) == 99L, "long live payload preservation");
        check(!lane.isBlockAllocated(PAGE) && lane.get(REGION) == 0,
                "long missing pages return zero");

        long[] input = {3L, 0L, -9L};
        lane.copyFrom(input, 0, REGION, input.length);
        long[] output = new long[5];
        lane.copyTo(REGION, output, 1, input.length);
        for (int i = 0; i < input.length; i++) {
            check(input[i] == output[i + 1], "long copied span " + i);
        }
        lane.copyFrom(new long[3], 0, REGION, 3);
        check(lane.compact() == 1, "long zero copy reclaim");
        lane.clear(0);
        lane.clear(PAGE - 1);
        lane.clear(LAST);
        check(lane.compact() == 2 && lane.allocatedBlockCount() == 0,
                "long complete release");
        invalid(() -> lane.set(-1, 1L));
        invalid(() -> lane.copyTo(LAST, new long[2], 0, 2));
    }

    private static void randomizedPrimitiveOracles() {
        M3IntLane28 ints = new M3IntLane28();
        M3LongLane28 longs = new M3LongLane28();
        Map<Integer, Integer> intExpected = new HashMap<>();
        Map<Integer, Long> longExpected = new HashMap<>();
        int[] positions = {0, 1, 7, 4094, 4095, 4096, 4097,
                REGION - 1, REGION, REGION + 1, REGION + PAGE,
                LAST - 1, LAST};
        Random random = new Random(0x218cafeL);
        for (int step = 0; step < 3000; step++) {
            int slot = positions[random.nextInt(positions.length)];
            int ivalue = random.nextInt(33) - 16;
            long lvalue = (long) (random.nextInt(47) - 23) << 34;
            if (random.nextInt(5) == 0) {
                ivalue = 0;
                lvalue = 0L;
            }
            if (random.nextBoolean()) {
                check(ints.getAndSet(slot, ivalue)
                        == intExpected.getOrDefault(slot, 0), "random int old value");
                if (ivalue == 0) intExpected.remove(slot);
                else intExpected.put(slot, ivalue);
            } else {
                check(longs.getAndSet(slot, lvalue)
                        == longExpected.getOrDefault(slot, 0L), "random long old value");
                if (lvalue == 0L) longExpected.remove(slot);
                else longExpected.put(slot, lvalue);
            }
            if ((step % 37) == 0) {
                int beforeInts = ints.allocatedBlockCount();
                int beforeLongs = longs.allocatedBlockCount();
                int freedInts = ints.compact();
                int freedLongs = longs.compact();
                check(freedInts >= 0 && freedInts <= beforeInts
                        && freedLongs >= 0 && freedLongs <= beforeLongs,
                        "random reclaimed count");
                check(ints.compact() == 0 && longs.compact() == 0,
                        "random compaction fixed point");
                check(ints.payloadBytes() == (long) ints.allocatedBlockCount()
                                * PAGE * Integer.BYTES,
                        "random int payload accounting");
                check(longs.payloadBytes() == (long) longs.allocatedBlockCount()
                                * PAGE * Long.BYTES,
                        "random long payload accounting");
                for (int position : positions) {
                    check(ints.get(position) == intExpected.getOrDefault(position, 0),
                            "random int value " + position);
                    check(longs.get(position) == longExpected.getOrDefault(position, 0L),
                            "random long value " + position);
                }
            }
        }
    }

    private static void verifyBits(M3BitLane28 lane, TreeSet<Integer> model) {
        check(lane.cardinality() == model.size(), "bit cardinality");
        int[] coordinates = {0, 1, PAGE - 1, PAGE, PAGE + 1, REGION - 1, REGION,
                REGION + 1, LAST, END};
        for (int end : coordinates) {
            check(lane.rank(end) == model.headSet(end).size(), "bit rank " + end);
            check(lane.nextSetBit(end) == (model.ceiling(end) == null
                    ? -1 : model.ceiling(end)), "bit next " + end);
            if (end < END) {
                check(lane.previousSetBit(end) == (model.floor(end) == null
                        ? -1 : model.floor(end)), "bit previous " + end);
            }
        }
        check(lane.previousSetBit(-1) == -1, "bit empty prefix");
        check(lane.cardinality(0, END) == model.size(), "bit full range cardinality");
        ArrayList<Integer> values = new ArrayList<>(model);
        for (int i = 0; i < values.size(); i++) {
            check(lane.select(i) == values.get(i), "bit select " + i);
            check(lane.get(values.get(i)), "bit bit missing");
        }
        check(lane.select(values.size()) == -1 && lane.select(-1) == -1,
                "bit ordinal bounds");
    }

    private static void bitLane() {
        M3BitLane28 lane = new M3BitLane28();
        TreeSet<Integer> model = new TreeSet<>();
        check(lane.compact() == 0, "empty bit compact");
        for (int slot : new int[] {0, PAGE - 1, PAGE, REGION, LAST}) {
            lane.set(slot);
            model.add(slot);
        }
        verifyBits(lane, model); // prepares the exact derived rank/select arrays
        lane.clear(PAGE);
        lane.clear(REGION);
        model.remove(PAGE);
        model.remove(REGION);
        check(lane.allocatedBlockCount() == 4, "hot bit clear retains pages");
        check(lane.compact() == 2 && lane.allocatedRegionCount() == 2,
                "bit empty pages and regions released");
        verifyBits(lane, model);
        check(lane.compact() == 0, "bit fixed point");

        // The old cached region-count rows were released, and later writes must recreate them.
        lane.set(REGION);
        model.add(REGION);
        verifyBits(lane, model);
        lane.clear(REGION);
        model.remove(REGION);
        check(lane.compact() == 1, "bit reallocated region can be reclaimed");

        Random random = new Random(0x5e11afL);
        int[] candidates = {0, 1, 63, 64, PAGE - 1, PAGE, PAGE + 1,
                REGION - 1, REGION, REGION + 1, REGION + PAGE, LAST - 1, LAST};
        for (int step = 0; step < 1000; step++) {
            int slot = candidates[random.nextInt(candidates.length)];
            if (random.nextBoolean()) { lane.set(slot); model.add(slot); }
            else { lane.clear(slot); model.remove(slot); }
            if (step % 31 == 0) {
                lane.compact();
                verifyBits(lane, model);
            }
        }
        verifyBits(lane, model);
        for (int slot : model) lane.clear(slot);
        int before = lane.allocatedBlockCount();
        check(lane.compact() == before,
                "bit release count must match the former block count");
        check(lane.allocatedRegionCount() == 0 && lane.allocatedBlockCount() == 0,
                "bit complete release");
        invalid(() -> lane.set(-1));
        invalid(() -> lane.get(END));
        invalid(() -> lane.rank(END + 1));
    }

    public static void main(String[] args) {
        intLane();
        longLane();
        bitLane();
        randomizedPrimitiveOracles();
        System.out.println("M3 sparse int/long/bit compaction PASS");
    }
}
