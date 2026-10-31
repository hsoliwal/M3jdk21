/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.mindex;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Frozen functional oracle for the existing java.base bitmap owner, not a replacement owner. */
public final class BitLaneNavigationProof {
    private static final int END = 1 << 28;
    private static long checks;
    private static long nativeChecks;
    private static long digest = 0x32424c616e65L;
    private static boolean nativeLoaded;

    private BitLaneNavigationProof() { }

    private static void equal(long expected, long actual) {
        if (expected != actual) throw new AssertionError("expected=" + expected + " actual=" + actual);
        checks++;
        digest = Long.rotateLeft(digest, 7) ^ (actual + 0x9e3779b97f4a7c15L);
    }

    private static Object field(M3BitLane28 lane, String name) throws ReflectiveOperationException {
        Field value = M3BitLane28.class.getDeclaredField(name);
        value.setAccessible(true);
        return value.get(lane);
    }

    private static void same(Object expected, Object actual) {
        if (expected != actual) throw new AssertionError("owner or lazy-metadata identity changed");
        checks++;
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError(type.getName(), failure);
            checks++;
            return;
        }
        throw new AssertionError("did not throw " + type.getName());
    }

    private static void query(M3BitLane28 lane, TreeSet<Integer> oracle, int at, boolean ranked) {
        Integer ceiling = oracle.ceiling(at);
        equal(ceiling == null ? -1 : ceiling, lane.nextSetBit(at));
        int previous = Math.min(at, END - 1);
        Integer floor = oracle.floor(previous);
        equal(floor == null ? -1 : floor, lane.previousSetBit(previous));
        equal(oracle.size(), lane.cardinality());
        if (ranked) {
            equal(oracle.headSet(at).size(), lane.rank(at));
            int stop = Math.min(END, at + 129);
            int expected = oracle.subSet(at, stop).size();
            equal(expected, lane.cardinality(at, stop));
            if (nativeLoaded) {
                equal(expected, lane.cardinalityNative(at, stop));
                nativeChecks++;
            }
        }
    }

    private static void walk(M3BitLane28 lane, TreeSet<Integer> oracle) {
        int forward = lane.nextSetBit(0);
        for (int value : oracle) {
            equal(value, forward);
            forward = lane.nextSetBit(value + 1);
        }
        equal(-1, forward);
        int backward = lane.previousSetBit(END - 1);
        for (int value : oracle.descendingSet()) {
            equal(value, backward);
            backward = lane.previousSetBit(value - 1);
        }
        equal(-1, backward);
        int ordinal = 0;
        for (int value : oracle) equal(value, lane.select(ordinal++));
        equal(-1, lane.select(-1));
        equal(-1, lane.select(oracle.size()));
    }

    private static void bounds(M3BitLane28 lane) {
        equal(-1, lane.nextSetBit(END));
        equal(-1, lane.previousSetBit(-1));
        for (int bad : new int[] {Integer.MIN_VALUE, -2, -1, END + 1, Integer.MAX_VALUE})
            expect(IndexOutOfBoundsException.class, () -> lane.nextSetBit(bad));
        for (int bad : new int[] {Integer.MIN_VALUE, -2, END, END + 1, Integer.MAX_VALUE})
            expect(IndexOutOfBoundsException.class, () -> lane.previousSetBit(bad));
    }

    private static void boundaries() throws ReflectiveOperationException {
        M3BitLane28 lane = new M3BitLane28();
        TreeSet<Integer> oracle = new TreeSet<>();
        bounds(lane);
        same(null, field(lane, "regionCounts"));
        same(null, field(lane, "pageCounts"));
        int[] values = {0, 1, 62, 63, 64, 65, 4094, 4095, 4096, 4097,
                (1 << 20) - 1, 1 << 20, (1 << 20) + 1, (128 << 20), END - 65, END - 64, END - 1};
        for (int value : values) { lane.set(value); oracle.add(value); }
        for (int value : values) {
            query(lane, oracle, value, false);
            query(lane, oracle, Math.max(0, value - 1), false);
            query(lane, oracle, value + 1, false);
        }
        same(null, field(lane, "regionCounts"));
        same(null, field(lane, "pageCounts"));
        long bytes = lane.payloadBytes();
        int blocks = lane.allocatedBlockCount();
        int regions = lane.allocatedRegionCount();
        lane.rank(END - 1);
        Object totals = field(lane, "regionCounts");
        Object pages = field(lane, "pageCounts");
        walk(lane, oracle);
        for (int value : values) query(lane, oracle, value, true);
        same(totals, field(lane, "regionCounts"));
        same(pages, field(lane, "pageCounts"));
        equal(bytes, lane.payloadBytes());
        equal(blocks, lane.allocatedBlockCount());
        equal(regions, lane.allocatedRegionCount());
        for (int value : values) { lane.clear(value); oracle.remove(value); }
        bounds(lane);
        walk(lane, oracle);
        // Counts warmed before any region below is allocated: test publication and maintenance.
        for (int r = 2; r < 128; r += 7) {
            int value = (r << 20) + 2047;
            lane.set(value); oracle.add(value);
            query(lane, oracle, value, true);
            lane.clear(value); oracle.remove(value);
            query(lane, oracle, value, true);
            lane.flip(value); oracle.add(value);
            query(lane, oracle, value, true);
        }
        walk(lane, oracle);
    }

    private static void sparseMutations() throws ReflectiveOperationException {
        for (int seed = 0; seed < 12; seed++) {
            Random random = new Random(0x5343414eL + seed);
            M3BitLane28 lane = new M3BitLane28();
            TreeSet<Integer> oracle = new TreeSet<>();
            for (int operation = 0; operation < 1536; operation++) {
                int value = random.nextInt(END);
                if ((operation & 7) == 0) value = (random.nextInt(256) << 20) + (operation & 63);
                switch (operation % 5) {
                    case 0, 1 -> { lane.set(value); oracle.add(value); }
                    case 2 -> { lane.clear(value); oracle.remove(value); }
                    case 3 -> { lane.flip(value); if (!oracle.remove(value)) oracle.add(value); }
                    default -> { lane.set(value, false); oracle.remove(value); }
                }
                query(lane, oracle, value, operation >= 32);
                query(lane, oracle, random.nextInt(END + 1), operation >= 32);
                if (operation == 31) {
                    same(null, field(lane, "regionCounts"));
                    same(null, field(lane, "pageCounts"));
                }
                if ((operation & 255) == 255) walk(lane, oracle);
            }
            for (int value : new ArrayList<>(oracle)) {
                lane.clear(value); oracle.remove(value);
                query(lane, oracle, value, false);
            }
            bounds(lane);
            walk(lane, oracle);
        }
    }

    private static void denseClearedSpans() throws ReflectiveOperationException {
        M3BitLane28 lane = new M3BitLane28();
        // Populate every page in four regions, then retain only the outer sentinels.
        for (int r = 100; r < 104; r++)
            for (int b = 0; b < 256; b++) lane.set((r << 20) | (b << 12) | 63);
        lane.rank(END - 1);
        for (int r = 100; r < 104; r++)
            for (int b = 0; b < 256; b++) lane.clear((r << 20) | (b << 12) | 63);
        int low = 100 << 20;
        int high = (104 << 20) - 1;
        lane.set(low); lane.set(high);
        long payload = lane.payloadBytes();
        Object totals = field(lane, "regionCounts");
        Object pages = field(lane, "pageCounts");
        for (int r = 100; r < 104; r++) {
            for (int b = 0; b < 256; b++) {
                int value = (r << 20) | (b << 12) | 2048;
                equal(high, lane.nextSetBit(value));
                equal(low, lane.previousSetBit(value));
                if (nativeLoaded) {
                    equal(lane.cardinality(value, high), lane.cardinalityNative(value, high));
                    nativeChecks++;
                }
            }
        }
        same(totals, field(lane, "regionCounts"));
        same(pages, field(lane, "pageCounts"));
        equal(payload, lane.payloadBytes());
    }

    private static void nativeRefusals() throws ReflectiveOperationException {
        Method method = M3BitLane28.class.getDeclaredMethod("cardinality0", long[][][].class, int.class, int.class);
        method.setAccessible(true);
        List<Object[]> bad = new ArrayList<>();
        bad.add(new Object[] {null, 0, 1});
        bad.add(new Object[] {new long[255][][], 0, 1});
        bad.add(new Object[] {new long[256][][], -1, 1});
        bad.add(new Object[] {new long[256][][], 2, 1});
        bad.add(new Object[] {new long[256][][], 0, END + 1});
        long[][][] wrongPages = new long[256][][];
        wrongPages[0] = new long[255][];
        bad.add(new Object[] {wrongPages, 0, 1});
        long[][][] wrongWords = new long[256][][];
        wrongWords[0] = new long[256][];
        wrongWords[0][0] = new long[63];
        bad.add(new Object[] {wrongWords, 0, 1});
        for (Object[] arguments : bad) {
            try { method.invoke(null, arguments); throw new AssertionError("native accepted malformed bitmap"); }
            catch (InvocationTargetException expected) {
                if (!(expected.getCause() instanceof IllegalArgumentException)) throw new AssertionError(expected);
                checks++; nativeChecks++;
            }
        }
    }

    public static void main(String[] arguments) throws ReflectiveOperationException {
        same(null, M3BitLane28.class.getClassLoader());
        if (!"java.base".equals(M3BitLane28.class.getModule().getName())) throw new AssertionError("not java.base");
        if (arguments.length > 0 && arguments[0].equals("--missing-native")) {
            expect(UnsatisfiedLinkError.class, () -> new M3BitLane28().cardinalityNative(0, 1));
            System.out.println("MISSING_NATIVE_REFUSAL_PASS checks=" + checks);
            return;
        }
        if (arguments.length > 0) { System.load(arguments[0]); nativeLoaded = true; }
        boundaries(); sparseMutations(); denseClearedSpans();
        if (nativeLoaded) nativeRefusals();
        System.out.println("M3_BITLANE_NAVIGATION_PASS checks=" + checks + " nativeChecks=" + nativeChecks
                + " digest=" + Long.toUnsignedString(digest, 16));
    }
}
