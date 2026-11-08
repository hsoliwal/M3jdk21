/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/** Executable Java 21 contract oracle; deliberately independent of JUnit runtime. */
public final class M3LazyTopKWindowContract {
    private M3LazyTopKWindowContract() {}

    public static void main(String[] args) {
        exactPrefix();
        secondarySignalAndTies();
        descendingStableTies();
        allPrimitiveKinds();
        doubleOrdering();
        sourceRevision();
        memoryPressure();
        zeroAndInvalidInputs();
        cancellationAtomicity();
        randomizedLargeCollection();
        System.out.println("M3LazyTopKWindow: 10/10 contract groups passed; seed=3a7f1623");
    }

    public static void exactPrefix() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.LONG);
        for (long x : new long[]{3, 1, 2, 2, -4, 1, 12}) source.append1(x);
        for (boolean up : new boolean[]{true, false}) {
            var window = new M3LazyTopKWindow(source, 0, up);
            Integer[] oracle = oracle(source, 0, -1, up);
            for (int k = 1; k <= source.size(); k++) {
                int[] out = new int[k];
                check(window.fill(k, 0, out, 0) == k, "size");
                for (int i = 0; i < k; i++) check(out[i] == oracle[i], "prefix order");
            }
        }
        check(source.getBits(0, 0) == 3L, "no source permutation");
    }

    public static void secondarySignalAndTies() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.INT, PrimitiveKind.INT);
        source.append2(5, 9); source.append2(2, 8);
        source.append2(5, 3); source.append2(2, 8);
        for (boolean up : new boolean[]{true, false}) {
            var window = new M3LazyTopKWindow(source, 0, 1, up);
            int[] out = new int[4];
            window.fill(4, 0, out, 0);
            Integer[] expected = oracle(source, 0, 1, up);
            for (int i = 0; i < 4; i++) check(out[i] == expected[i], "second lane");
        }
    }

    public static void descendingStableTies() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.LONG);
        for (int i = 0; i < 5000; i++) source.append1(7);
        var window = new M3LazyTopKWindow(source, 0, false);
        for (int k : new int[]{2, 17, M3LazyTopKWindow.MAX_WINDOW, 3}) {
            int[] out = new int[k + 4];
            Arrays.fill(out, -1);
            check(window.fill(k, 0, out, 2) == k, "descending tie size");
            for (int i = 0; i < k; i++)
                check(out[i + 2] == i, "descending final ties retain source order");
            check(out[0] == -1 && out[1] == -1 && out[k + 2] == -1
                    && out[k + 3] == -1, "offset padding intact");
        }
        check(window.cacheHits() > 0, "stable cached descending prefix");
    }

    public static void allPrimitiveKinds() {
        Random random = new Random(0x3a7f1623L);
        for (PrimitiveKind kind : PrimitiveKind.values()) {
            var source = new M3PrimitiveSoaStore(kind, kind);
            for (int i = 0; i < 127; i++) {
                long a = i % 3 == 0 ? 7 : random.nextLong();
                long b = i % 3 == 0 ? 11 : random.nextLong();
                source.append2(a, b);
            }
            for (boolean up : new boolean[]{true, false}) {
                var window = new M3LazyTopKWindow(source, 0, 1, up);
                int[] out = new int[63];
                check(window.fill(out.length, 0, out, 0) == out.length, "all kinds size");
                Integer[] expected = oracle(source, 0, 1, up);
                for (int i = 0; i < out.length; i++)
                    check(out[i] == expected[i], "independent key order " + kind);
            }
        }
    }

    public static void doubleOrdering() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.DOUBLE);
        for (double x : new double[]{Double.NaN, -0.0, 0.0,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN})
            source.append1(Double.doubleToRawLongBits(x));
        for (boolean up : new boolean[]{true, false}) {
            var window = new M3LazyTopKWindow(source, 0, up);
            int[] out = new int[6];
            window.fill(6, 0, out, 0);
            int[] expected = up ? new int[]{3,1,2,4,0,5} : new int[]{0,5,4,2,1,3};
            check(Arrays.equals(out, expected), "IEEE compare and stable NaN ties");
        }
    }

    public static void sourceRevision() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.LONG);
        source.append1(30); source.append1(10); source.append1(20);
        var window = new M3LazyTopKWindow(source, 0, true);
        int[] out = new int[3];
        window.fill(3, 0, out, 0);
        long builds = window.buildCount();
        window.fill(2, 0, out, 0);
        check(window.cacheHits() > 0 && window.buildCount() == builds, "cache hit");
        source.setBits(0, 0, -99);
        window.fill(2, 0, out, 0);
        check(out[0] == 0 && window.buildCount() > builds, "revision invalidation");
        window.invalidate();
        check(window.cachedRows() == 0 && source.getBits(0, 0) == -99, "safe cache eviction");
    }

    public static void memoryPressure() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.LONG);
        for (int i = 0; i < 30; i++) source.append1(30 - i);
        var window = new M3LazyTopKWindow(source, 0, true);
        int[] out = new int[12];
        var normal = new M3RuntimeHeap.Snapshot(128L << 20, 80L << 20, 20L << 20);
        var warm = new M3RuntimeHeap.Snapshot(128L << 20, 112L << 20, 94L << 20);
        var high = new M3RuntimeHeap.Snapshot(128L << 20, 124L << 20, 118L << 20);
        check(window.fill(3, 6, out, 0, normal, () -> false) == 9, "normal overscan");
        check(window.fill(3, 6, out, 0, warm, () -> false) == 6, "warm overscan");
        check(window.fill(3, 6, out, 0, high, () -> false) == 3, "high no overscan");
        check(window.cachedPrimitiveBytes() == 0, "high evicts only derived prefix");
        check(window.sourcePrimitiveBytes() > 0 && source.size() == 30, "source retained");
    }

    public static void zeroAndInvalidInputs() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.INT);
        var window = new M3LazyTopKWindow(source, 0, true);
        int[] untouched = {7, 8, 9};
        check(window.fill(0, 100, untouched, 0) == 0, "zero visible");
        check(window.fill(1, 10, untouched, 0) == 0, "empty source");
        expect(IllegalArgumentException.class, () -> window.fill(4096, 1, untouched, 0));
        source.append1(12);
        expect(IndexOutOfBoundsException.class, () -> window.fill(1, 0, untouched, 3));
        check(Arrays.equals(untouched, new int[]{7,8,9}), "refusal has no output effects");
    }

    public static void cancellationAtomicity() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.LONG);
        for (int i = 0; i < 8200; i++) source.append1(i % 37);
        var window = new M3LazyTopKWindow(source, 0, true);
        int[] out = new int[20];
        Arrays.fill(out, 777);
        AtomicInteger calls = new AtomicInteger();
        expect(CancellationException.class, () ->
            window.fill(20, 0, out, 0, null, () -> calls.incrementAndGet() >= 3));
        for (int x : out) check(x == 777, "cancel leaves output intact");
    }

    public static void randomizedLargeCollection() {
        var source = new M3PrimitiveSoaStore(PrimitiveKind.LONG);
        Random random = new Random(0x3a7f1623L);
        for (int i = 0; i < 25_000; i++) source.append1(random.nextInt(10_000) - 5000);
        for (boolean up : new boolean[]{true, false}) {
            var window = new M3LazyTopKWindow(source, 0, up);
            Integer[] expected = oracle(source, 0, -1, up);
            int retainedRows = 0;
            for (int k : new int[]{1,17,64,127,M3LazyTopKWindow.MAX_WINDOW,64}) {
                int[] top = new int[k];
                window.fill(k, 0, top, 0);
                for (int i = 0; i < top.length; i++)
                    check(top[i] == expected[i], "random Top-K in both directions");
                retainedRows = Math.max(retainedRows, k);
                check(window.cachedPrimitiveBytes() == retainedRows * (long) Integer.BYTES,
                        "O(k) memory including cached prefixes");
            }
        }
    }

    private static Integer[] oracle(M3PrimitiveSoaStore source, int first, int second, boolean up) {
        Integer[] ids = new Integer[source.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = i;
        PrimitiveKind kind = source.laneKind(first);
        PrimitiveKind secondKind = second == -1 ? null : source.laneKind(second);
        Arrays.sort(ids, (a,b) -> {
            int c = compareCarrier(kind, source.getBits(first, a), source.getBits(first, b));
            if (c == 0 && secondKind != null)
                c = compareCarrier(secondKind, source.getBits(second, a), source.getBits(second, b));
            // Object-array sort is stable; equal keys retain the initial source order.
            return up ? c : -c;
        });
        return ids;
    }

    private static int compareCarrier(PrimitiveKind kind, long a, long b) {
        return switch (kind) {
            case BOOLEAN -> Boolean.compare(a != 0, b != 0);
            case BYTE -> Byte.compare((byte) a, (byte) b);
            case SHORT -> Short.compare((short) a, (short) b);
            case CHAR -> Character.compare((char) a, (char) b);
            case INT -> Integer.compare((int) a, (int) b);
            case LONG -> Long.compare(a, b);
            case FLOAT -> Float.compare(Float.intBitsToFloat((int) a), Float.intBitsToFloat((int) b));
            case DOUBLE -> Double.compare(Double.longBitsToDouble(a), Double.longBitsToDouble(b));
        };
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("unexpected exception", error);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
