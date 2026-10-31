/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Differential copy contract, using the unchanged interface defaults as the oracle. */
public final class M3ArrayNumericCopySelfTest {
    private static int checks;

    private M3ArrayNumericCopySelfTest() { }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        runContract();
        System.out.println("M3_ARRAY_NUMERIC_COPY_PASS checks=" + checks);
    }

    static void runContract() throws InterruptedException {
        checks = 0;
        allWindows();
        allFailureWindows();
        externalAccessorEffects();
        maximumLogicalOffsets();
        concurrentImmutableCopies();
    }

    private static void allWindows() {
        Random random = new Random(0x4d33_0ff5_2026L);
        for (int length = 0; length <= 18; length++) {
            int[] ints = new int[length];
            long[] longs = new long[length];
            for (int i = 0; i < length; i++) {
                ints[i] = random.nextInt();
                longs[i] = random.nextLong();
            }
            List<M3IntArrayView> intViews = intShapes(ints);
            List<M3LongArrayView> longViews = longShapes(longs);
            for (int shape = 0; shape < intViews.size(); shape++) {
                M3IntArrayView iv = intViews.get(shape);
                M3LongArrayView lv = longViews.get(shape);
                for (int from = 0; from <= length; from++) {
                    for (int count = 0; count <= length - from; count++) {
                        for (int destination = 0; destination <= 3; destination++) {
                            int[] actualI = intTarget(length + 5);
                            int[] expectedI = actualI.clone();
                            long[] actualL = longTarget(length + 5);
                            long[] expectedL = actualL.clone();
                            System.arraycopy(ints, from, expectedI, destination, count);
                            System.arraycopy(longs, from, expectedL, destination, count);
                            iv.copyTo(from, actualI, destination, count);
                            lv.copyTo(from, actualL, destination, count);
                            check(Arrays.equals(expectedI, actualI), "int copied range and guards");
                            check(Arrays.equals(expectedL, actualL), "long copied range and guards");
                            int half = count / 2;
                            Arrays.fill(actualI, 0x43218765);
                            Arrays.fill(actualL, 0x43218765fedcba09L);
                            iv.copyTo(from, actualI, destination, half);
                            iv.copyTo(from + half, actualI, destination + half, count - half);
                            lv.copyTo(from, actualL, destination, half);
                            lv.copyTo(from + half, actualL, destination + half, count - half);
                            check(Arrays.equals(expectedI, actualI), "int adjacent range composition");
                            check(Arrays.equals(expectedL, actualL), "long adjacent range composition");
                        }
                    }
                }
                check(Arrays.equals(ints, iv.copy()), "int copy materialization");
                check(Arrays.equals(longs, lv.copy()), "long copy materialization");
                int[] detachedI = iv.copy();
                long[] detachedL = lv.copy();
                Arrays.fill(detachedI, 1);
                Arrays.fill(detachedL, 1);
                check(Arrays.equals(ints, iv.copy()), "int returned array is detached");
                check(Arrays.equals(longs, lv.copy()), "long returned array is detached");
            }
            // Caller inputs remain mutable, independently of all admitted views.
            Arrays.fill(ints, 2);
            Arrays.fill(longs, 2);
            check(Arrays.equals(intViews.get(0).copy(), intViews.get(1).copy()), "int snapshot admission");
            check(Arrays.equals(longViews.get(0).copy(), longViews.get(1).copy()), "long snapshot admission");
        }
    }

    private static List<M3IntArrayView> intShapes(int[] values) {
        int[] padded = new int[values.length + 4];
        System.arraycopy(values, 0, padded, 2, values.length);
        M3IntArrayView snapshot = M3Arrays.ints(values);
        M3IntArrayView slice = M3Arrays.ints(padded).slice(1, values.length + 3).slice(1, values.length + 1);
        int middle = values.length / 2;
        M3IntArrayView join = M3Arrays.join(M3Arrays.ints(new int[0]),
                snapshot.slice(0, middle), M3Arrays.ints(new int[0]), M3Arrays.ints(new int[0]),
                slice.slice(middle, values.length), M3Arrays.ints(new int[0]));
        M3IntArrayView nested = M3Arrays.join(M3Arrays.ints(new int[0]), join,
                M3Arrays.ints(new int[0]), M3Arrays.ints(new int[0]));
        M3IntArrayView joinedSlice = M3Arrays.join(M3Arrays.ints(new int[] {42}), join,
                M3Arrays.ints(new int[] {43})).slice(1, values.length + 1);
        return List.of(snapshot, slice, join, nested, joinedSlice);
    }

    private static List<M3LongArrayView> longShapes(long[] values) {
        long[] padded = new long[values.length + 4];
        System.arraycopy(values, 0, padded, 2, values.length);
        M3LongArrayView snapshot = M3Arrays.longs(values);
        M3LongArrayView slice = M3Arrays.longs(padded).slice(1, values.length + 3).slice(1, values.length + 1);
        int middle = values.length / 2;
        M3LongArrayView join = M3Arrays.join(M3Arrays.longs(new long[0]),
                snapshot.slice(0, middle), M3Arrays.longs(new long[0]), M3Arrays.longs(new long[0]),
                slice.slice(middle, values.length), M3Arrays.longs(new long[0]));
        M3LongArrayView nested = M3Arrays.join(M3Arrays.longs(new long[0]), join,
                M3Arrays.longs(new long[0]), M3Arrays.longs(new long[0]));
        M3LongArrayView joinedSlice = M3Arrays.join(M3Arrays.longs(new long[] {42}), join,
                M3Arrays.longs(new long[] {43})).slice(1, values.length + 1);
        return List.of(snapshot, slice, join, nested, joinedSlice);
    }

    private static void allFailureWindows() {
        int[] arguments = {-1, 0, 1, 2, 3, 4, Integer.MAX_VALUE};
        for (int length : new int[] {0, 1, 3}) {
            List<M3IntArrayView> ints = intShapes(new int[length]);
            List<M3LongArrayView> longs = longShapes(new long[length]);
            for (int shape = 0; shape < ints.size(); shape++) {
                M3IntArrayView iv = ints.get(shape);
                M3LongArrayView lv = longs.get(shape);
                for (int from : arguments) for (int to : arguments) for (int count : arguments) {
                    for (int targetLength : new int[] {-1, 0, 2, 4}) {
                        int[] actualI = targetLength < 0 ? null : intTarget(targetLength);
                        int[] expectedI = actualI == null ? null : actualI.clone();
                        long[] actualL = targetLength < 0 ? null : longTarget(targetLength);
                        long[] expectedL = actualL == null ? null : actualL.clone();
                        Outcome expectedInt = outcome(() -> intOracle(iv).copyTo(from, expectedI, to, count));
                        Outcome actualInt = outcome(() -> iv.copyTo(from, actualI, to, count));
                        Outcome expectedLong = outcome(() -> longOracle(lv).copyTo(from, expectedL, to, count));
                        Outcome actualLong = outcome(() -> lv.copyTo(from, actualL, to, count));
                        check(expectedInt.equals(actualInt), "int null/bounds order and exact failure");
                        check(expectedLong.equals(actualLong), "long null/bounds order and exact failure");
                        check(Arrays.equals(expectedI, actualI), "int failure preserves mutation boundary");
                        check(Arrays.equals(expectedL, actualL), "long failure preserves mutation boundary");
                    }
                }
            }
        }
    }

    private static void externalAccessorEffects() {
        RuntimeException failure = new IllegalStateException("external accessor identity");
        for (int shape = 0; shape < 3; shape++) {
            for (int throwAt : new int[] {-1, 0, 2}) {
                Probe expected = new Probe(throwAt, failure);
                Probe actual = new Probe(throwAt, failure);
                M3IntArrayView expectedI = probeInts(expected, shape);
                M3IntArrayView actualI = probeInts(actual, shape);
                expected.armed = true;
                actual.armed = true;
                int count = Math.min(6, expectedI.length() - 1);
                Probe expectedIntProbe = expected;
                Probe actualIntProbe = actual;
                Throwable before = thrown(() -> intOracle(expectedI).copyTo(1, expectedIntProbe.ints, 2, count));
                Throwable after = thrown(() -> actualI.copyTo(1, actualIntProbe.ints, 2, count));
                check(before == after, "int exact external throwable identity");
                check(Arrays.equals(expected.ints, actual.ints), "int exact partial target");
                check(expected.events.equals(actual.events), "int callback order observes exact target prefix");
                check(actual.copyCalls == 0, "int external copyTo must not be dispatched");
                actual.events.clear();
                actualI.copyTo(actualI.length(), actual.ints, actual.ints.length, 0);
                check(actual.events.isEmpty(), "int zero copy has no accessor/length callback");

                expected = new Probe(throwAt, failure);
                actual = new Probe(throwAt, failure);
                M3LongArrayView expectedL = probeLongs(expected, shape);
                M3LongArrayView actualL = probeLongs(actual, shape);
                expected.armed = true;
                actual.armed = true;
                Probe expectedProbe = expected;
                Probe actualProbe = actual;
                int longCount = Math.min(6, expectedL.length() - 1);
                before = thrown(() -> longOracle(expectedL).copyTo(1, expectedProbe.longs, 2, longCount));
                after = thrown(() -> actualL.copyTo(1, actualProbe.longs, 2, longCount));
                check(before == after, "long exact external throwable identity");
                check(Arrays.equals(expected.longs, actual.longs), "long exact partial target");
                check(expected.events.equals(actual.events), "long callback order observes exact target prefix");
                check(actual.copyCalls == 0, "long external copyTo must not be dispatched");
                actual.events.clear();
                actualL.copyTo(actualL.length(), actual.longs, actual.longs.length, 0);
                check(actual.events.isEmpty(), "long zero copy has no accessor/length callback");
            }
        }
    }

    private static final class Probe {
        final int[] ints = intTarget(12);
        final long[] longs = longTarget(12);
        final List<String> events = new ArrayList<>();
        final int throwAt;
        final RuntimeException failure;
        boolean armed;
        int copyCalls;

        Probe(int throwAt, RuntimeException failure) {
            this.throwAt = throwAt;
            this.failure = failure;
        }

        int length() {
            if (armed) throw new AssertionError("external length re-read during copy");
            return 4;
        }

        int integer(int index) {
            Objects.checkIndex(index, 4);
            events.add("i" + index + ":" + Arrays.toString(ints));
            if (index == throwAt) throw failure;
            return 100 + index;
        }

        long wide(int index) {
            Objects.checkIndex(index, 4);
            events.add("l" + index + ":" + Arrays.toString(longs));
            if (index == throwAt) throw failure;
            return 0xfeedbeef00000000L + index;
        }
    }

    private static M3IntArrayView probeInts(Probe probe, int shape) {
        M3IntArrayView external = new M3IntArrayView() {
            public int length() { return probe.length(); }
            public int intAt(int index) { return probe.integer(index); }
            public M3IntArrayView slice(int start, int end) { throw new AssertionError("external slice"); }
            public void copyTo(int start, int[] target, int offset, int count) {
                probe.copyCalls++;
                throw new AssertionError("external copyTo");
            }
        };
        M3IntArrayView joined = M3Arrays.join(M3Arrays.ints(new int[] {17, 18}),
                M3Arrays.ints(new int[0]), external, M3Arrays.ints(new int[0]),
                M3Arrays.ints(new int[] {27, 28}));
        if (shape == 0) return joined;
        if (shape == 1) return M3Arrays.join(M3Arrays.ints(new int[0]), joined, M3Arrays.ints(new int[0]));
        return M3Arrays.join(M3Arrays.ints(new int[] {3}), joined).slice(1, joined.length() + 1);
    }

    private static M3LongArrayView probeLongs(Probe probe, int shape) {
        M3LongArrayView external = new M3LongArrayView() {
            public int length() { return probe.length(); }
            public long longAt(int index) { return probe.wide(index); }
            public M3LongArrayView slice(int start, int end) { throw new AssertionError("external slice"); }
            public void copyTo(int start, long[] target, int offset, int count) {
                probe.copyCalls++;
                throw new AssertionError("external copyTo");
            }
        };
        M3LongArrayView joined = M3Arrays.join(M3Arrays.longs(new long[] {17, 18}),
                M3Arrays.longs(new long[0]), external, M3Arrays.longs(new long[0]),
                M3Arrays.longs(new long[] {27, 28}));
        if (shape == 0) return joined;
        if (shape == 1) return M3Arrays.join(M3Arrays.longs(new long[0]), joined, M3Arrays.longs(new long[0]));
        return M3Arrays.join(M3Arrays.longs(new long[] {3}), joined).slice(1, joined.length() + 1);
    }

    private static void maximumLogicalOffsets() {
        M3IntArrayView virtualI = new M3IntArrayView() {
            public int length() { return Integer.MAX_VALUE - 2; }
            public int intAt(int index) { return Objects.checkIndex(index, length()) ^ 0x34567890; }
            public M3IntArrayView slice(int start, int end) { throw new AssertionError("virtual slice"); }
        };
        M3LongArrayView virtualL = new M3LongArrayView() {
            public int length() { return Integer.MAX_VALUE - 2; }
            public long longAt(int index) { return (long) Objects.checkIndex(index, length()) << 32; }
            public M3LongArrayView slice(int start, int end) { throw new AssertionError("virtual slice"); }
        };
        M3IntArrayView ints = M3Arrays.join(M3Arrays.ints(new int[0]), virtualI,
                M3Arrays.ints(new int[0]), M3Arrays.ints(new int[0]), M3Arrays.ints(new int[] {6, 7}));
        M3LongArrayView longs = M3Arrays.join(M3Arrays.longs(new long[0]), virtualL,
                M3Arrays.longs(new long[0]), M3Arrays.longs(new long[0]), M3Arrays.longs(new long[] {6, 7}));
        int from = Integer.MAX_VALUE - 4;
        int[] expectedI = intTarget(8);
        int[] actualI = expectedI.clone();
        long[] expectedL = longTarget(8);
        long[] actualL = expectedL.clone();
        intOracle(ints).copyTo(from, expectedI, 2, 4);
        longOracle(longs).copyTo(from, expectedL, 2, 4);
        ints.copyTo(from, actualI, 2, 4);
        longs.copyTo(from, actualL, 2, 4);
        check(Arrays.equals(expectedI, actualI), "maximum int offsets");
        check(Arrays.equals(expectedL, actualL), "maximum long offsets");
        check(Arrays.equals(ints.slice(from, Integer.MAX_VALUE).copy(), Arrays.copyOfRange(actualI, 2, 6)), "maximum int slice");
        check(Arrays.equals(longs.slice(from, Integer.MAX_VALUE).copy(), Arrays.copyOfRange(actualL, 2, 6)), "maximum long slice");
    }

    private static void concurrentImmutableCopies() throws InterruptedException {
        int[] valuesI = new int[129];
        long[] valuesL = new long[129];
        for (int i = 0; i < valuesI.length; i++) { valuesI[i] = i; valuesL[i] = (long) i << 33; }
        List<M3IntArrayView> ints = intShapes(valuesI);
        List<M3LongArrayView> longs = longShapes(valuesL);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[4];
        for (int worker = 0; worker < threads.length; worker++) {
            threads[worker] = new Thread(() -> {
                try {
                    start.await();
                    int[] targetI = new int[valuesI.length];
                    long[] targetL = new long[valuesL.length];
                    for (int round = 0; round < 400; round++) {
                        int shape = round % ints.size();
                        ints.get(shape).copyTo(0, targetI, 0, targetI.length);
                        longs.get(shape).copyTo(0, targetL, 0, targetL.length);
                        if (!Arrays.equals(valuesI, targetI) || !Arrays.equals(valuesL, targetL)) {
                            throw new AssertionError("immutable source concurrent copy");
                        }
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            });
            threads[worker].start();
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        check(failure.get() == null, "concurrent immutable sources and separate targets");
    }

    private static M3IntArrayView intOracle(M3IntArrayView view) {
        return new M3IntArrayView() {
            public int length() { return view.length(); }
            public int intAt(int index) { return view.intAt(index); }
            public M3IntArrayView slice(int start, int end) { return intOracle(view.slice(start, end)); }
        };
    }

    private static M3LongArrayView longOracle(M3LongArrayView view) {
        return new M3LongArrayView() {
            public int length() { return view.length(); }
            public long longAt(int index) { return view.longAt(index); }
            public M3LongArrayView slice(int start, int end) { return longOracle(view.slice(start, end)); }
        };
    }

    private static int[] intTarget(int length) {
        int[] target = new int[length];
        Arrays.fill(target, 0x43218765);
        return target;
    }

    private static long[] longTarget(int length) {
        long[] target = new long[length];
        Arrays.fill(target, 0x43218765fedcba09L);
        return target;
    }

    private record Outcome(Class<?> type, String message) { }

    private static Outcome outcome(Runnable operation) {
        Throwable failure = thrown(operation);
        return failure == null ? new Outcome(null, null) : new Outcome(failure.getClass(), failure.getMessage());
    }

    private static Throwable thrown(Runnable operation) {
        try { operation.run(); return null; } catch (Throwable failure) { return failure; }
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message + " at check " + checks);
    }
}
