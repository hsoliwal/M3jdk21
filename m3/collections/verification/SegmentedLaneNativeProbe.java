/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3Address28;
import com.m3.collections.M3BitLane28;
import java.util.Random;

/** Explicit native runtime gate; run with -Xcheck:jni and an absolute library argument. */
public final class SegmentedLaneNativeProbe {
    private SegmentedLaneNativeProbe() { }
    public static void main(String[] args) {
        System.load(args[0]);
        M3BitLane28 lane = new M3BitLane28();
        Random random = new Random(3729);
        int limit = (1 << 20) + 5000;
        for (int i = 0; i < 18000; i++) lane.set(random.nextInt(limit));
        int[] boundaries = {0, 1, 63, 64, 65, 4095, 4096, (1 << 20) - 1, 1 << 20,
                M3Address28.MAX_SLOT, M3Address28.MAX_SLOTS};
        lane.set(M3Address28.MAX_SLOT);
        for (int from : boundaries) for (int to : boundaries) if (from <= to) check(lane, from, to);
        for (int i = 0; i < 1200; i++) {
            int from = random.nextInt(limit + 1);
            int to = from + random.nextInt(limit - from + 1);
            check(lane, from, to);
            int bit = random.nextInt(limit); lane.flip(bit);
        }
        try { lane.cardinalityNative(-1, 0); throw new AssertionError("negative range accepted"); }
        catch (IndexOutOfBoundsException expected) { /* Java boundary rejected before JNI. */ }
        System.out.println("JNI parity PASS: 66 boundary ranges, 1200 changing random ranges, checked JNI");
    }
    private static void check(M3BitLane28 lane, int from, int to) {
        int expected = lane.cardinality(from, to);
        int actual = lane.cardinalityNative(from, to);
        if (expected != actual) throw new AssertionError(from + ".." + to + ": " + expected + " != " + actual);
    }
}
