/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0. */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Standalone source-backed Java21 + optional C17/JNI oracle for bitmap index jumps. */
public final class M3BitLaneJumpProof {
    private static final int MAX = M3Address28.MAX_SLOTS;
    private static long assertions;

    private M3BitLaneJumpProof() { }

    public static void main(String[] args) {
        boolean withNative = args.length == 1;
        if (args.length > 1) throw new IllegalArgumentException("optional JNI library path");
        if (withNative) System.load(java.nio.file.Path.of(args[0]).toAbsolutePath().toString());
        M3BitLane28 lane = new M3BitLane28();
        TreeSet<Integer> oracle = new TreeSet<>();
        emptyAndBounds(lane);
        int[] boundary = {0, 1, 31, 63, 64, 65, 4095, 4096, 4097,
                (1 << 20) - 1, 1 << 20, (1 << 20) + 63,
                MAX - 4097, MAX - 4096, MAX - 64, MAX - 1};
        for (int point : boundary) {
            lane.set(point);
            oracle.add(point);
        }
        compareAll(lane, oracle, queries(boundary), withNative);
        Random random = new Random(0x5A17F3L);
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < 1000; i++) candidates.add(i);
        for (int i = 0; i < 500; i++) candidates.add((1 << 20) + i);
        for (int i = 0; i < 500; i++) candidates.add(MAX - 1 - i);
        for (int i = 0; i < 5000; i++) {
            int slot = candidates.get(random.nextInt(candidates.size()));
            switch (random.nextInt(3)) {
                case 0 -> { lane.set(slot); oracle.add(slot); }
                case 1 -> { lane.clear(slot); oracle.remove(slot); }
                default -> {
                    lane.flip(slot);
                    if (!oracle.add(slot)) oracle.remove(slot);
                }
            }
            if (i % 250 == 0) compareAll(lane, oracle, queries(boundary), withNative);
        }
        compareAll(lane, oracle, queries(boundary), withNative);
        boundedBitSetDifferential();
        System.out.println("M3_BITLANE_JUMP_PASS assertions=" + assertions + " native=" + withNative);
    }

    private static void emptyAndBounds(M3BitLane28 lane) {
        check(lane.nextSetBit(0) == -1, "empty next");
        check(lane.nextSetBit(MAX) == -1, "exclusive upper endpoint");
        check(lane.previousSetBit(-1) == -1, "negative predecessor sentinel");
        check(lane.previousSetBit(MAX - 1) == -1, "empty previous");
        check(lane.indexPayloadBytes() == 0, "empty index does not allocate");
        expectIndexError(() -> lane.nextSetBit(-1));
        expectIndexError(() -> lane.nextSetBit(MAX + 1));
        expectIndexError(() -> lane.previousSetBit(-2));
        expectIndexError(() -> lane.previousSetBit(MAX));
    }

    private static int[] queries(int[] boundary) {
        TreeSet<Integer> result = new TreeSet<>();
        result.add(0);
        result.add(MAX - 1);
        for (int value : boundary) {
            for (int d = -2; d <= 2; d++) {
                long v = (long) value + d;
                if (v >= 0 && v < MAX) result.add((int) v);
            }
        }
        for (int v = 0; v < 10000; v += 103) result.add(v);
        result.add(MAX - 4096);
        result.add(MAX - 4095);
        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void compareAll(M3BitLane28 lane, TreeSet<Integer> oracle,
            int[] probes, boolean withNative) {
        check(lane.cardinality() == oracle.size(), "cardinality");
        int[] expectedNext = new int[probes.length];
        int[] expectedPrev = new int[probes.length];
        for (int p = 0; p < probes.length; p++) {
            int q = probes[p];
            Integer next = oracle.ceiling(q);
            Integer prev = oracle.floor(q);
            expectedNext[p] = next == null ? -1 : next;
            expectedPrev[p] = prev == null ? -1 : prev;
            check(lane.nextSetBit(q) == expectedNext[p], "next " + q);
            check(lane.previousSetBit(q) == expectedPrev[p], "previous " + q);
        }
        int ordinal = 0;
        for (int value : oracle) {
            check(lane.select(ordinal) == value, "select " + ordinal);
            check(lane.rank(value) == ordinal, "rank " + value);
            ordinal++;
        }
        check(lane.select(-1) == -1, "negative select");
        check(lane.select(oracle.size()) == -1, "terminal select");
        check(lane.rank(MAX) == oracle.size(), "rank exclusive end");
        check(lane.nextSetBit(MAX) == -1, "next MAX");
        check(lane.previousSetBit(-1) == -1, "previous -1");
        if (withNative) {
            int observed = verifyNative(lane, probes, expectedNext, expectedPrev);
            check(observed == probes.length * 2, "checked JNI results");
        }
    }

    private static void boundedBitSetDifferential() {
        BitSet bits = new BitSet();
        M3BitLane28 lane = new M3BitLane28();
        Random r = new Random(0xC0FFEE);
        for (int i = 0; i < 3000; i++) {
            int p = r.nextInt(16000);
            lane.flip(p);
            bits.flip(p);
        }
        for (int i = 0; i <= 16000; i++) {
            check(lane.nextSetBit(i) == bits.nextSetBit(i), "BitSet next " + i);
            check(lane.previousSetBit(i) == bits.previousSetBit(i), "BitSet previous " + i);
        }
        check(Arrays.equals(collect(lane), bits.stream().toArray()), "BitSet full set traversal");
    }

    private static int[] collect(M3BitLane28 lane) {
        List<Integer> values = new ArrayList<>();
        for (int next = lane.nextSetBit(0); next >= 0; next = lane.nextSetBit(next + 1)) {
            values.add(next);
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void check(boolean success, String label) {
        assertions++;
        if (!success) throw new AssertionError(label);
    }

    private static void expectIndexError(Runnable operation) {
        try {
            operation.run();
        } catch (IndexOutOfBoundsException expected) {
            assertions++;
            return;
        }
        throw new AssertionError("expected IndexOutOfBoundsException");
    }

    private static native int verifyNative(M3BitLane28 lane,
            int[] probes, int[] expectedNext, int[] expectedPrevious);
}
