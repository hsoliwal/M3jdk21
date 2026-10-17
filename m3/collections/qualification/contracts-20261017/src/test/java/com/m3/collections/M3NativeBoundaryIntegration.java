/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** Exercises the existing nine JNI owners in eight native libraries; no new runtime protocol. */
public final class M3NativeBoundaryIntegration {
    private static int checks;
    private static final M3IndexMaskCompactor.Backend JAVA = M3IndexMaskCompactor.Backend.JAVA;
    private static final M3IndexMaskCompactor.Backend JNI = M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
    private M3NativeBoundaryIntegration() { }

    public static void main(String[] args) {
        boolean required = args.length == 1 && args[0].equals("native");
        check(M3IndexMaskCompactor.nativeAvailable() == required, "mask library selection");
        check(M3LongAddressKernel.nativeAvailable() == required, "address library selection");
        check(M3IndexPathMasks.nativeAvailable() == required, "path-mask library selection");
        check(M3ResidencyStateMaskIndex.nativeAvailable() == required, "parent-state library selection");
        check(M3LongRankBatchNative.nativeAvailable() == required, "rank-batch opt-in selection");
        check(M3SignalOrderNative.available() == required, "signal-order opt-in selection");
        check(M3SpanIndex.nativeAvailable() == required, "span library selection");
        if (required) System.loadLibrary("segmented_bits");
        else expect(UnsatisfiedLinkError.class, () -> System.loadLibrary("segmented_bits"));
        if (required) {
            byteMasks();
            stridedPathMasks();
            gatherBoundaries();
            parentWords();
            sortedQueries();
            sparseBitLanes();
        } else absentBackends();
        System.out.println("M3_NATIVE_BOUNDARY_INTEGRATION_PASS checks=" + checks
                + " libraries=8 owners=9 required=" + required);
    }

    private static void byteMasks() {
        Random random = new Random(0x821_0ff5_2026L);
        for (int length : new int[] {0, 1, 255, 256, 257, 1024, 4097}) {
            int[] rows = new int[length + 7];
            long[] longRows = new long[length + 7];
            byte[] states = new byte[length + 11];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = switch (i % 5) { case 0 -> Integer.MIN_VALUE; case 1 -> Integer.MAX_VALUE; default -> i % 7; };
                longRows[i] = switch (i % 5) { case 0 -> Long.MIN_VALUE; case 1 -> Long.MAX_VALUE; default -> i % 7; };
            }
            random.nextBytes(states);
            for (int mask : new int[] {0, 1, 128, 255}) {
                int[] expected = M3IndexMaskCompactor.selectWindow(rows, 3, states, 5, length, mask, JAVA);
                int[] actual = M3IndexMaskCompactor.selectWindow(rows, 3, states, 5, length, mask, JNI);
                equal(expected, actual); check(actual != rows, "detached native int window");
                check(Arrays.equals(M3IndexMaskCompactor.selectLongWindow(longRows, 3, states, 5, length, mask, JAVA),
                        M3IndexMaskCompactor.selectLongWindow(longRows, 3, states, 5, length, mask, JNI)), "signed 64-bit native row transfer");
            }
            int[] alignedRows = Arrays.copyOfRange(rows, 3, 3 + length);
            byte[] alignedStates = Arrays.copyOfRange(states, 5, 5 + length);
            for (int[] masks : new int[][] {{0, 0}, {128, 0}, {0, 128}, {128, 128}}) {
                equal(M3IndexMaskCompactor.select(alignedRows, alignedStates, masks[0], JAVA),
                        M3IndexMaskCompactor.select(alignedRows, alignedStates, masks[0], JNI));
                for (int limit : new int[] {0, 1, 17}) {
                    int[] left = new int[limit + 4], right = new int[limit + 4];
                    Arrays.fill(left, -991); Arrays.fill(right, -991);
                    long a = M3IndexMaskWindow.scan(alignedRows, alignedStates, 0, masks[0], masks[1], left, 2, limit, JAVA);
                    long b = M3IndexMaskWindow.scan(alignedRows, alignedStates, 0, masks[0], masks[1], right, 2, limit, JNI);
                    check(a == b, "native cursor/count"); equal(left, right);
                    check(right[0] == -991 && right[1] == -991 && right[right.length - 1] == -991, "native mask guards");
                }
            }
        }
        int[] rows = {1, 2}; byte[] states = {(byte) 255, 0}; int[] output = {7, 7};
        expect(IllegalArgumentException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, rows, 0, 1, JNI));
        expect(IndexOutOfBoundsException.class, () -> M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, Integer.MAX_VALUE, 1, JNI));
        expect(IllegalArgumentException.class, () -> M3IndexMaskCompactor.select(rows, states, 256, JNI));
        expect(IndexOutOfBoundsException.class, () -> M3IndexMaskCompactor.selectWindow(rows, Integer.MAX_VALUE, states, 0, 1, 0, JNI));
        expect(NullPointerException.class, () -> M3IndexMaskCompactor.select(null, states, 0, JNI));
        equal(new int[] {7, 7}, output);
    }

    private static void stridedPathMasks() {
        int[] rows = {Integer.MIN_VALUE, 9, 9, -1, Integer.MAX_VALUE};
        int[] offsets = {3, 1, 0, 2, 3};
        int[][] planes = {{Integer.MIN_VALUE | 1, Integer.MIN_VALUE | 3, 0, Integer.MIN_VALUE | 1}, {7, 7, 7, 5}};
        for (int[] required : new int[][] {{0, 0}, {Integer.MIN_VALUE | 1, 1}, {-1, 0}}) {
            for (int stride : new int[] {1, -1}) {
                int first = stride > 0 ? 0 : rows.length - 1;
                int[] forbidden = {2, 2};
                int[] expected = M3IndexPathMasks.select(rows, offsets, planes, required, forbidden, first, rows.length, stride, M3IndexPathMasks.Backend.JAVA);
                equal(expected, M3IndexPathMasks.select(rows, offsets, planes, required, forbidden, first, rows.length, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
                int[] left = {8, 8, 8, 8, 8}, right = left.clone();
                long a = M3IndexPathMasks.scanWindow(rows, offsets, planes, required, forbidden, first, rows.length, stride, 0, left, 1, 2, M3IndexPathMasks.Backend.JAVA);
                long b = M3IndexPathMasks.scanWindow(rows, offsets, planes, required, forbidden, first, rows.length, stride, 0, right, 1, 2, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
                check(a == b, "path ordinal cursor"); equal(left, right);
            }
        }
        int[] zero = {0, 0};
        expect(IllegalArgumentException.class, () -> M3IndexPathMasks.scanWindow(rows, offsets, planes, zero, zero, 0, rows.length, 1, 0, offsets, 0, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        expect(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows, offsets, planes, zero, zero, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        int[] bad = offsets.clone(); bad[2] = Integer.MAX_VALUE;
        int[] guarded = {81, 81, 81, 81, 81};
        expect(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.scanWindow(rows, bad, planes, zero, zero, 0, rows.length, 1, 0, guarded, 0, 4, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        equal(new int[] {81, 81, 81, 81, 81}, guarded);
    }

    private static void gatherBoundaries() {
        long[] payload = new long[600];
        for (int i = 0; i < payload.length; i++) payload[i] = (i & 1) == 0 ? Long.MIN_VALUE + i : Long.MAX_VALUE - i;
        int[] addresses = new int[520];
        for (int i = 0; i < addresses.length; i++) addresses[i] = i;
        for (int length : new int[] {0, 1, 255, 256, 257, 519}) {
            check(Arrays.equals(M3LongAddressKernel.gather(payload, addresses, 1, length, JAVA),
                    M3LongAddressKernel.gather(payload, addresses, 1, length, JNI)), "contiguous gather boundary");
        }
        for (int i = 0; i < addresses.length; i++) addresses[i] = (i * 37) % payload.length;
        check(Arrays.equals(M3LongAddressKernel.gather(payload, addresses, 0, addresses.length, JAVA),
                M3LongAddressKernel.gather(payload, addresses, 0, addresses.length, JNI)), "arbitrary gather critical region");
        for (int badAt : new int[] {0, 255, 256, 257, 519}) {
            int[] invalid = addresses.clone(); invalid[badAt] = Integer.MAX_VALUE;
            long[] left = new long[520], right = new long[520]; Arrays.fill(left, 17); Arrays.fill(right, 17);
            expect(IndexOutOfBoundsException.class, () -> M3LongAddressKernel.gatherInto(payload, invalid, 0, 520, left, 0, JAVA));
            expect(IndexOutOfBoundsException.class, () -> M3LongAddressKernel.gatherInto(payload, invalid, 0, 520, right, 0, JNI));
            check(Arrays.equals(left, right), "exact valid-prefix failure writes");
        }
        long[] saved = payload.clone();
        expect(IllegalArgumentException.class, () -> M3LongAddressKernel.gatherInto(payload, addresses, 0, 1, payload, 0, JNI));
        check(Arrays.equals(saved, payload), "gather alias refused without mutation");
    }

    private static void parentWords() {
        M3ObjectResidency<Object> arena = new M3ObjectResidency<>(4, M3ObjectResidency.LAZY_CREATE, 1000, 0,
                slot -> { throw new AssertionError("selection must not create a child"); }, null, null);
        long[] words = {Long.MIN_VALUE | 1, Long.MIN_VALUE | 3, 1, Long.MIN_VALUE | 1};
        for (int i = 0; i < words.length; i++) arena.writeParentBits(i, words[i]);
        int[] rows = {3, 1, 0, 3, 2};
        int allowed = M3ResidencyStateMaskIndex.states(M3ObjectResidency.State.ABSENT);
        for (int stride : new int[] {1, -1}) {
            int first = stride == 1 ? 0 : rows.length - 1;
            for (long required : new long[] {0, Long.MIN_VALUE, Long.MIN_VALUE | 1}) {
                equal(M3ResidencyStateMaskIndex.select(arena, rows, first, rows.length, stride, required, 2, allowed, M3ResidencyStateMaskIndex.Backend.JAVA),
                        M3ResidencyStateMaskIndex.select(arena, rows, first, rows.length, stride, required, 2, allowed, M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED));
            }
        }
        equal(new int[0], M3ResidencyStateMaskIndex.select(arena, rows, 0, rows.length, 1, 0, 0, 0, M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED));
        check(arena.residentBytes() == 0, "no child materialized or retained");
        for (int i = 0; i < words.length; i++) check(arena.readParentBits(i) == words[i], "parent word remains canonical");
        expect(IllegalArgumentException.class, () -> M3ResidencyStateMaskIndex.select(arena, rows, 0, rows.length, 1, Long.MIN_VALUE, Long.MIN_VALUE, allowed, M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED));
        expect(IndexOutOfBoundsException.class, () -> M3ResidencyStateMaskIndex.select(arena, rows, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, allowed, M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED));
    }

    private static void sortedQueries() {
        long[] input = {Long.MAX_VALUE, -1, 0, Long.MIN_VALUE, -1, Long.MAX_VALUE};
        M3LongRankIndex index = M3LongRankIndex.copyOf(input);
        long[] queries = new long[128];
        for (int i = 0; i < queries.length; i++) queries[i] = input[i % input.length];
        int[] result = index.lowerBounds(queries); // opt-in JNI route: nonempty owner and >=64 queries
        for (int i = 0; i < queries.length; i++) check(result[i] == index.lowerBound(queries[i]), "signed rank lower bound");
        double[] signals = {Double.NaN, -0.0, 0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                2, -2, Double.longBitsToDouble(0x7ff8000000000007L), -0.0};
        byte[] masks = new byte[signals.length]; Arrays.fill(masks, (byte) 128);
        for (boolean ascending : new boolean[] {true, false}) {
            Integer[] ordered = new Integer[signals.length];
            for (int i = 0; i < ordered.length; i++) ordered[i] = i;
            Comparator<Integer> comparator = Comparator.comparingDouble(i -> signals[i]);
            Arrays.sort(ordered, ascending ? comparator : comparator.reversed());
            int[] expected = Arrays.stream(ordered).mapToInt(Integer::intValue).toArray();
            equal(expected, M3SignalOrder.filtered(signals, masks, 128, 0, ascending, M3SignalOrder.Backend.NATIVE_REQUIRED).rows());
        }
        M3SpanIndex spans = new M3SpanIndex();
        spans.add(3, (long) Integer.MAX_VALUE + 17, Integer.MIN_VALUE);
        spans.add(0, 4, Integer.MIN_VALUE); spans.add(2, 2, Integer.MIN_VALUE);
        M3SpanIndex.Snapshot snapshot = spans.snapshot(Integer.MIN_VALUE, 0);
        int[] rows = {Integer.MIN_VALUE, -1, 0, 1, 3, 4, 5, Integer.MAX_VALUE};
        equal(snapshot.earliestCoveringBatch(rows, M3SpanIndex.Backend.JAVA),
                snapshot.earliestCoveringBatch(rows, M3SpanIndex.Backend.NATIVE_REQUIRED));
        equal(new int[0], snapshot.earliestCoveringBatch(new int[0], M3SpanIndex.Backend.NATIVE_REQUIRED));
        equal(new int[] {0, 0, 0, 1, 3, 4, 5, Integer.MAX_VALUE},
                new M3SpanIndex().snapshot(0, 0).earliestCoveringBatch(rows, M3SpanIndex.Backend.NATIVE_REQUIRED));
    }

    private static void sparseBitLanes() {
        M3BitLane28 bits = new M3BitLane28();
        int[] positions = {0, 63, 64, 4095, 4096, 1 << 20, M3Address28.MAX_SLOTS - 1};
        for (int position : positions) bits.set(position);
        for (int[] range : new int[][] {{0, 0}, {0, 65}, {63, 4096}, {4095, 4097},
                {1 << 20, (1 << 20) + 1}, {0, M3Address28.MAX_SLOTS}}) {
            check(bits.cardinality(range[0], range[1]) == bits.cardinalityNative(range[0], range[1]), "segmented bitmap JNI range");
        }
        expect(IndexOutOfBoundsException.class, () -> bits.cardinalityNative(-1, 0));
    }

    private static void absentBackends() {
        int[] empty = new int[0]; byte[] masks = new byte[0];
        expect(UnsatisfiedLinkError.class, () -> M3IndexMaskCompactor.select(empty, masks, 0, JNI));
        expect(UnsatisfiedLinkError.class, () -> M3IndexMaskWindow.scan(empty, masks, 0, 0, 0, new int[0], 0, 0, JNI));
        expect(UnsatisfiedLinkError.class, () -> M3LongAddressKernel.gather(new long[0], empty, 0, 0, JNI));
        expect(UnsatisfiedLinkError.class, () -> M3IndexPathMasks.select(empty, empty, new int[][] {empty}, new int[1], new int[1], 0, 0, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        M3ObjectResidency<Object> owner = new M3ObjectResidency<>(0, 0, 0, 0, null, null, null);
        expect(UnsatisfiedLinkError.class, () -> M3ResidencyStateMaskIndex.select(owner, empty, 0, 0, 1, 0, 0, 0, M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED));
        expect(UnsatisfiedLinkError.class, () -> M3SignalOrder.filtered(new double[0], masks, 0, 0, true, M3SignalOrder.Backend.NATIVE_REQUIRED));
        expect(UnsatisfiedLinkError.class, () -> new M3SpanIndex().snapshot(0, 0).earliestCoveringBatch(empty, M3SpanIndex.Backend.NATIVE_REQUIRED));
        expect(UnsatisfiedLinkError.class, () -> new M3BitLane28().cardinalityNative(0, 0));
        equal(new int[128], M3LongRankIndex.copyOf(new long[] {1}).lowerBounds(new long[128]));
    }

    private static void equal(int[] expected, int[] actual) { check(Arrays.equals(expected, actual), "primitive output parity"); }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
