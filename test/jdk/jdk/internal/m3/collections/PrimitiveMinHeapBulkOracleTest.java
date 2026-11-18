/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Primitive bulk heapification preserves ordering, extrema and detached inputs.
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm PrimitiveMinHeapBulkOracleTest
 */

import jdk.internal.m3.collections.IntMinHeap;
import jdk.internal.m3.collections.LongMinHeap;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

/** Standalone Java 21 oracle for additive primitive bulk heapification. */
public final class PrimitiveMinHeapBulkOracleTest {
    private PrimitiveMinHeapBulkOracleTest() { }
    private static long assertions;

    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }

    private static int nextInt(SplittableRandom random) {
        return switch (random.nextInt(12)) {
            case 0 -> Integer.MIN_VALUE;
            case 1 -> Integer.MAX_VALUE;
            case 2, 3 -> -1;
            case 4, 5 -> 0;
            default -> random.nextInt(-1_000_000, 1_000_001);
        };
    }

    private static long nextLong(SplittableRandom random) {
        return switch (random.nextInt(12)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2, 3 -> -1L;
            case 4, 5 -> 0L;
            default -> random.nextLong();
        };
    }

    private static void runInt(long seed, int operations) {
        var rng = new SplittableRandom(seed);
        var heap = new IntMinHeap(rng.nextInt(40));
        var oracle = new PriorityQueue<Integer>();
        for (int step = 0; step < operations; step++) {
            int mode = rng.nextInt(100);
            if (mode < 24 && oracle.size() < 50_000) {
                int n = rng.nextInt(45);
                int[] batch = new int[n];
                for (int j = 0; j < n; j++) batch[j] = nextInt(rng);
                heap.addAll(batch);
                for (int v : batch) oracle.add(v);
                if (n > 0) {
                    int expected = batch[0];
                    Arrays.fill(batch, ~expected); // caller mutations cannot change the heap
                }
            } else if (mode < 55 && oracle.size() < 50_000) {
                int v = nextInt(rng);
                heap.add(v);
                oracle.add(v);
            } else if (mode < 88 && !oracle.isEmpty()) {
                check(Objects.equals(oracle.poll(), heap.poll()), "int poll seed=" + seed + " step=" + step);
            } else if (mode == 99) {
                oracle.clear();
                heap.clear();
            } else if (!oracle.isEmpty()) {
                check(Objects.equals(oracle.peek(), heap.peek()), "int peek seed=" + seed + " step=" + step);
            }
            check(heap.size() == oracle.size(), "int size seed=" + seed + " step=" + step);
            check(heap.isEmpty() == oracle.isEmpty(), "int empty seed=" + seed + " step=" + step);
            check(heap.payloadBytes() >= (long) heap.size() * Integer.BYTES, "int payload capacity");
            if (!oracle.isEmpty()) check(Objects.equals(oracle.peek(), heap.peek()), "int root invariant");
        }
        while (!oracle.isEmpty()) check(Objects.equals(oracle.poll(), heap.poll()), "int full drain");
        check(heap.isEmpty(), "int drained");
        heap.addAll(new int[0]);
        check(heap.size() == 0, "int empty batch");
        try { heap.addAll(null); throw new AssertionError("int null accepted"); }
        catch (NullPointerException expected) { assertions++; }
        try { heap.peek(); throw new AssertionError("int empty peek accepted"); }
        catch (NoSuchElementException expected) { assertions++; }
        try { heap.poll(); throw new AssertionError("int empty poll accepted"); }
        catch (NoSuchElementException expected) { assertions++; }
    }

    private static void runLong(long seed, int operations) {
        var rng = new SplittableRandom(seed);
        var heap = new LongMinHeap(rng.nextInt(40));
        var oracle = new PriorityQueue<Long>();
        for (int step = 0; step < operations; step++) {
            int mode = rng.nextInt(100);
            if (mode < 24 && oracle.size() < 50_000) {
                int n = rng.nextInt(45);
                long[] batch = new long[n];
                for (int j = 0; j < n; j++) batch[j] = nextLong(rng);
                heap.addAll(batch);
                for (long v : batch) oracle.add(v);
                if (n > 0) {
                    long expected = batch[0];
                    Arrays.fill(batch, ~expected);
                }
            } else if (mode < 55 && oracle.size() < 50_000) {
                long v = nextLong(rng);
                heap.add(v);
                oracle.add(v);
            } else if (mode < 88 && !oracle.isEmpty()) {
                check(Objects.equals(oracle.poll(), heap.poll()), "long poll seed=" + seed + " step=" + step);
            } else if (mode == 99) {
                oracle.clear();
                heap.clear();
            } else if (!oracle.isEmpty()) {
                check(Objects.equals(oracle.peek(), heap.peek()), "long peek seed=" + seed + " step=" + step);
            }
            check(heap.size() == oracle.size(), "long size seed=" + seed + " step=" + step);
            check(heap.isEmpty() == oracle.isEmpty(), "long empty seed=" + seed + " step=" + step);
            check(heap.payloadBytes() >= (long) heap.size() * Long.BYTES, "long payload capacity");
            if (!oracle.isEmpty()) check(Objects.equals(oracle.peek(), heap.peek()), "long root invariant");
        }
        while (!oracle.isEmpty()) check(Objects.equals(oracle.poll(), heap.poll()), "long full drain");
        check(heap.isEmpty(), "long drained");
        heap.addAll(new long[0]);
        check(heap.size() == 0, "long empty batch");
        try { heap.addAll(null); throw new AssertionError("long null accepted"); }
        catch (NullPointerException expected) { assertions++; }
        try { heap.peek(); throw new AssertionError("long empty peek accepted"); }
        catch (NoSuchElementException expected) { assertions++; }
        try { heap.poll(); throw new AssertionError("long empty poll accepted"); }
        catch (NoSuchElementException expected) { assertions++; }
    }

    private static void bulkThenDrain(int batchSize) {
        int[] ints = new int[batchSize];
        long[] longs = new long[batchSize];
        for (int i = 0; i < batchSize; i++) { ints[i] = batchSize - i; longs[i] = batchSize - i; }
        var iHeap = new IntMinHeap(0);
        var lHeap = new LongMinHeap(0);
        iHeap.addAll(ints);
        lHeap.addAll(longs);
        for (int i = 1; i <= batchSize; i++) {
            check(iHeap.poll() == i, "int Floyd sorted drain " + i);
            check(lHeap.poll() == i, "long Floyd sorted drain " + i);
        }
        check(iHeap.isEmpty() && lHeap.isEmpty(), "bulk drained");
    }

    public static void main(String[] args) {
        for (int seed = 0; seed < 6; seed++) {
            runInt(0x6a09e667f3bcc909L + seed, 75_000);
            runLong(0xbb67ae8584caa73bL + seed, 75_000);
        }
        for (int count : new int[] {0, 1, 2, 3, 7, 31, 255, 8192, 65_537}) bulkThenDrain(count);
        System.out.println("PASS primitive bulk heap oracle assertions=" + assertions);
    }
}
