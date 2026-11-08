/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Exact scalar-array oracles; no packed decoder or block-bound logic is reused. */
public final class M3BlockPackedLongArrayContract {
    private static final long SEED = 0x6b10c5ca20261108L;
    private static final long PAD = 0x51ce51ce51ce51ceL;

    private M3BlockPackedLongArrayContract() {}

    public static void main(String[] args) {
        representationAndWidths();
        copyRangesAndAtomicity();
        exactCountsAndBounds();
        randomizedOracle();
        System.out.println("M3BlockPackedLongArray: 4/4 contract groups passed; seed="
                + Long.toHexString(SEED));
    }

    public static void representationAndWidths() {
        var empty = M3BlockPackedLongArray.of(new long[0]);
        check(empty.size() == 0 && empty.blocks() == 0 && empty.payloadBytes() == 0, "empty");
        check(empty.toArray().length == 0, "empty materialization");
        empty.copyTo(0, new long[0], 0, 0);
        check(empty.countInRange(Long.MIN_VALUE, Long.MAX_VALUE) == 0, "empty count");
        expect(NullPointerException.class, () -> M3BlockPackedLongArray.of(null));
        expect(IllegalArgumentException.class, () -> M3BlockPackedLongArray.of(new long[0], 0));
        expect(IndexOutOfBoundsException.class, () -> empty.get(0));

        Random random = new Random(SEED);
        for (int width = 0; width < Long.SIZE; width++) {
            long[] source = new long[129];
            long base = width == 63 ? Long.MIN_VALUE : -901;
            long mask = width == 0 ? 0 : -1L >>> (Long.SIZE - width);
            for (int i = 0; i < source.length; i++) source[i] = base + (random.nextLong() & mask);
            source[0] = base;
            source[source.length - 1] = width == 0 ? base : base + (1L << (width - 1));
            var packed = M3BlockPackedLongArray.of(source, source.length);
            check(packed.blockBitWidth(0) == width, "width " + width);
            verifySnapshot(source, packed);
            long expectedBytes = 13L + (width == 0 ? 0 : ((129L * width + 63) / 64) * Long.BYTES);
            check(packed.payloadBytes() == expectedBytes, "unchanged block payload " + width);
            verifyCopy(source, packed, 1, 127);
            verifyCount(source, packed, 1, 128, base, source[source.length - 1]);
            verifyCount(source, packed, 0, 129, Long.MIN_VALUE, Long.MAX_VALUE);
        }

        long[] original = {Long.MIN_VALUE, Long.MAX_VALUE, 0, -1, 1};
        long[] saved = original.clone();
        var plain = M3BlockPackedLongArray.of(original, original.length);
        check(plain.blockBitWidth(0) == 64, "full signed domain plain block");
        Arrays.fill(original, 42);
        verifySnapshot(saved, plain);
        check(plain.payloadBytes() == 13L + 5L * Long.BYTES, "plain payload");
        var singletons = M3BlockPackedLongArray.of(saved, 1);
        for (int block = 0; block < singletons.blocks(); block++)
            check(singletons.blockBitWidth(block) == 0, "singleton constant");
        verifySnapshot(saved, singletons);
        verifySnapshot(saved, M3BlockPackedLongArray.of(saved, Integer.MAX_VALUE));
    }

    public static void copyRangesAndAtomicity() {
        long[] source = {17,17,17,17,17, -2,1,4,0,-1, Long.MIN_VALUE,Long.MAX_VALUE,9,-7,1, 29,29,29};
        var packed = M3BlockPackedLongArray.of(source, 5);
        check(packed.blockBitWidth(0) == 0 && packed.blockBitWidth(1) > 0
                && packed.blockBitWidth(1) < 64 && packed.blockBitWidth(2) == 64, "mixed modes");
        for (int from = 0; from <= source.length; from++)
            for (int length = 0; length <= source.length - from; length++)
                verifyCopy(source, packed, from, length);
        long[] destination = new long[8];
        Arrays.fill(destination, PAD);
        int[][] bad = {
            {-1,0,1}, {source.length,0,1}, {0,-1,1}, {0,8,1}, {0,0,-1},
            {Integer.MAX_VALUE,0,2}, {0,Integer.MAX_VALUE,2}, {0,0,Integer.MAX_VALUE}
        };
        for (int[] slice : bad) {
            expect(IndexOutOfBoundsException.class,
                    () -> packed.copyTo(slice[0], destination, slice[1], slice[2]));
            for (long value : destination) check(value == PAD, "refusal leaves destination intact");
        }
        expect(NullPointerException.class, () -> packed.copyTo(0, null, 0, 0));
        packed.copyTo(source.length, destination, destination.length, 0);
        for (long value : destination) check(value == PAD, "zero copy leaves destination intact");
        expect(IndexOutOfBoundsException.class, () -> packed.get(-1));
        expect(IndexOutOfBoundsException.class, () -> packed.get(source.length));
        expect(IndexOutOfBoundsException.class, () -> packed.blockBitWidth(-1));
        expect(IndexOutOfBoundsException.class, () -> packed.blockBitWidth(packed.blocks()));
    }

    public static void exactCountsAndBounds() {
        long[] source = new long[257];
        for (int i = 0; i < 64; i++) source[i] = 17;
        for (int i = 64; i < 128; i++) source[i] = Long.MAX_VALUE - i % 5;
        for (int i = 128; i < 192; i++) source[i] = Long.MIN_VALUE + i % 19;
        for (int i = 192; i < source.length; i++)
            source[i] = switch (i % 5) {
                case 0 -> Long.MIN_VALUE;
                case 1 -> Long.MAX_VALUE;
                case 2 -> -1;
                case 3 -> 0;
                default -> 1;
            };
        var packed = M3BlockPackedLongArray.of(source, 64);
        long[] endpoints = {Long.MIN_VALUE,Long.MIN_VALUE + 18,-1,0,1,16,17,18,
                Long.MAX_VALUE - 5,Long.MAX_VALUE - 4,Long.MAX_VALUE - 1,Long.MAX_VALUE};
        int[][] slices = {{0,0},{0,257},{1,256},{63,65},{64,128},{65,127},{127,194},{256,257},{257,257}};
        for (int[] slice : slices) {
            for (long lower : endpoints) {
                verifyCount(source, packed, slice[0], slice[1], lower, lower);
                for (long upper : endpoints)
                    if (lower <= upper) verifyCount(source, packed, slice[0], slice[1], lower, upper);
            }
        }
        expect(IllegalArgumentException.class, () -> packed.countInRange(2,1));
        expect(IllegalArgumentException.class, () -> packed.countInRange(0,0,2,1));
        expect(IndexOutOfBoundsException.class, () -> packed.countInRange(-1,1,0,1));
        expect(IndexOutOfBoundsException.class, () -> packed.countInRange(2,1,0,1));
        expect(IndexOutOfBoundsException.class, () -> packed.countEqual(0,258,17));
        expect(IndexOutOfBoundsException.class,
                () -> packed.countInRange(0,Integer.MAX_VALUE,0,1));
    }

    public static void randomizedOracle() {
        Random random = new Random(SEED);
        int[] blocks = {1,2,3,5,7,31,63,64,65,129,256,511};
        for (int trial = 0; trial < 160; trial++) {
            long[] source = new long[random.nextInt(700)];
            for (int i = 0; i < source.length; i++) {
                source[i] = switch (trial % 6) {
                    case 0 -> random.nextLong();
                    case 1 -> random.nextInt(67) - 33;
                    case 2 -> Long.MAX_VALUE - random.nextInt(19);
                    case 3 -> Long.MIN_VALUE + random.nextInt(19);
                    case 4 -> trial;
                    default -> (i / 17 % 2 == 0) ? random.nextLong() : i / 17;
                };
            }
            var packed = M3BlockPackedLongArray.of(source, blocks[trial % blocks.length]);
            verifySnapshot(source, packed);
            long retained = packed.payloadBytes();
            for (int query = 0; query < 80; query++) {
                int from = random.nextInt(source.length + 1);
                int to = from + random.nextInt(source.length - from + 1);
                verifyCopy(source, packed, from, to - from);
                long a = sampleEndpoint(source, random);
                long b = sampleEndpoint(source, random);
                verifyCount(source, packed, from, to, Math.min(a,b), Math.max(a,b));
                verifyCount(source, packed, from, to, a, a);
            }
            check(packed.payloadBytes() == retained, "scans retain no expanded index");
        }
    }

    private static long sampleEndpoint(long[] source, Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> source.length == 0 ? 0 : source[random.nextInt(source.length)];
            case 3 -> random.nextInt(101) - 50;
            default -> random.nextLong();
        };
    }

    private static void verifySnapshot(long[] source, M3BlockPackedLongArray packed) {
        check(packed.size() == source.length, "snapshot size");
        check(Arrays.equals(source, packed.toArray()), "expanded snapshot");
        for (int i = 0; i < source.length; i++) check(source[i] == packed.get(i), "point access");
        long[] copy = packed.toArray();
        if (copy.length != 0) {
            copy[0] ^= -1L;
            check(packed.get(0) == source[0], "expanded array is detached");
        }
    }

    private static void verifyCopy(long[] source, M3BlockPackedLongArray packed, int from, int length) {
        long[] actual = new long[length + 6];
        Arrays.fill(actual, PAD);
        long[] expected = actual.clone();
        System.arraycopy(source, from, expected, 3, length);
        packed.copyTo(from, actual, 3, length);
        check(Arrays.equals(actual, expected), "copy slice " + from + "+" + length);
    }

    private static void verifyCount(long[] source, M3BlockPackedLongArray packed,
            int from, int to, long lower, long upper) {
        int expected = 0;
        for (int i = from; i < to; i++)
            if (source[i] >= lower && source[i] <= upper) expected++;
        check(packed.countInRange(from,to,lower,upper) == expected, "exact slice range count");
        if (lower == upper)
            check(packed.countEqual(from,to,lower) == expected, "exact slice equality count");
        if (from == 0 && to == source.length) {
            check(packed.countInRange(lower,upper) == expected, "exact whole range count");
            if (lower == upper) check(packed.countEqual(lower) == expected, "exact whole equality count");
        }
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
