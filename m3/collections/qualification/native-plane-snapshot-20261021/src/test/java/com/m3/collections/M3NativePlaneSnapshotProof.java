/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Differential of the original Java owner and the native small-plane snapshot atom. */
public final class M3NativePlaneSnapshotProof {
    private static final Random RANDOM = new Random(0x4d334a444b32314cL);
    private static final MessageDigest DIGEST = digest();
    private static int cases;
    private static int eligible;
    private static int fallback;

    private M3NativePlaneSnapshotProof() { }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException missing) { throw new AssertionError(missing); }
    }

    private static void add(int[] values) {
        int length = values.length;
        addInt(length);
        for (int value : values) addInt(value);
    }

    private static void addInt(int value) {
        for (int shift = 0; shift <= 24; shift += 8) DIGEST.update((byte) (value >>> shift));
    }

    private static void equal(int[] rows, int[] offsets, int[][] planes,
            int[] required, int[] forbidden, int first, int count, int stride) {
        int[] expected = M3IndexPathMasks.select(rows, offsets, planes, required,
                forbidden, first, count, stride, M3IndexPathMasks.Backend.JAVA);
        int[] actual = M3IndexPathMasks.select(rows, offsets, planes, required,
                forbidden, first, count, stride, M3IndexPathMasks.Backend.NATIVE_REQUIRED);
        if (!Arrays.equals(expected, actual) || actual == rows) {
            throw new AssertionError("M3JDK21 Java/JNI mismatch: length=" + rows.length
                    + " count=" + count + " stride=" + stride + " planes=" + planes.length
                    + " output=" + Arrays.toString(actual));
        }
        if (rows.length > 0) {
            int original = rows[0];
            rows[0] ^= 0x5a5a5a5a;
            if (!Arrays.equals(expected, actual)) {
                throw new AssertionError("native detached row selection retained input alias");
            }
            rows[0] = original;
        }
        add(actual);
        cases++;
        boolean small = planes.length <= 8 && count >= 512;
        for (int[] plane : planes) small &= plane.length <= 256;
        if (small) eligible++; else fallback++;
    }

    private static void check(int count, int capacity, int stages, int pattern) {
        int[] rows = new int[count], offsets = new int[count];
        int[][] planes = new int[stages][];
        int[] required = new int[stages], forbidden = new int[stages];
        int smallest = capacity > 1 && pattern == 6 ? capacity - 1 : capacity;
        for (int row = 0; row < count; row++) {
            rows[row] = row % 11 == 0 ? Integer.MIN_VALUE
                    : row % 7 == 0 ? Integer.MAX_VALUE
                    : row % 3 == 0 ? -1 : RANDOM.nextInt(41) - 20;
            offsets[row] = RANDOM.nextInt(smallest);
        }
        for (int stage = 0; stage < stages; stage++) {
            int length = capacity > 1 && pattern == 6 && (stage & 1) == 1
                    ? capacity - 1 : capacity;
            planes[stage] = new int[length];
            for (int index = 0; index < length; index++) planes[stage][index] = RANDOM.nextInt();
            int bit = 1 << (stage % 32);
            switch (pattern) {
                case 0 -> { required[stage] = 0; forbidden[stage] = 0; }
                case 1 -> { required[stage] = bit; forbidden[stage] = bit; }
                case 2 -> { required[stage] = bit; forbidden[stage] = 0; }
                case 3 -> { required[stage] = 0; forbidden[stage] = bit; }
                case 4 -> { required[stage] = Integer.MIN_VALUE; forbidden[stage] = 0; }
                case 5 -> { required[stage] = 0; forbidden[stage] = Integer.MIN_VALUE; }
                case 6 -> { required[stage] = RANDOM.nextInt(); forbidden[stage] = RANDOM.nextInt(); }
                default -> throw new AssertionError(pattern);
            }
        }
        for (int stride : new int[]{1, -1, 3, -3}) {
            int first = stride < 0 ? Math.max(0, count - 1) : 0;
            int selected = (count + Math.abs(stride) - 1) / Math.abs(stride);
            equal(rows, offsets, planes, required, forbidden, first, selected, stride);
        }
        if (count > 2) {
            equal(rows, offsets, planes, required, forbidden, 1, count - 2, 1);
        }
    }

    private static void fails(Class<? extends Throwable> required, Runnable operation) {
        try { operation.run(); }
        catch (Throwable caught) {
            if (required.isInstance(caught)) return;
            throw new AssertionError("expected " + required.getName(), caught);
        }
        throw new AssertionError("missing exception " + required.getName());
    }

    private static void badShapes() {
        int[] rows = new int[512];
        int[] offsets = new int[512];
        int[][] planes = {{1}, {2}};
        int[] required = {0, 0}, forbidden = {0, 0};
        offsets[511] = 1;
        fails(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.select(rows, offsets, planes, required, forbidden,
                        0, 512, 1, M3IndexPathMasks.Backend.JAVA));
        fails(IndexOutOfBoundsException.class,
                () -> M3IndexPathMasks.select(rows, offsets, planes, required, forbidden,
                        0, 512, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        offsets[511] = 0;
        fails(IllegalArgumentException.class,
                () -> M3IndexPathMasks.select(rows, offsets, planes, new int[1], forbidden,
                        0, 512, 1, M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        fails(NullPointerException.class,
                () -> M3IndexPathMasks.select(rows, offsets, new int[][]{null},
                        new int[]{0}, new int[]{0}, 0, 512, 1,
                        M3IndexPathMasks.Backend.NATIVE_REQUIRED));
        equal(rows, offsets, new int[][]{new int[0]},
                new int[]{0}, new int[]{0}, 512, 0, 1);
    }

    public static void main(String[] arguments) {
        if (!M3IndexPathMasks.nativeAvailable()) {
            throw new AssertionError("native M3JDK21 backend is required");
        }
        badShapes();
        for (int size : new int[]{0, 1, 7, 8, 511, 512, 513, 1024, 2048}) {
            for (int capacity : new int[]{1, 7, 127, 255, 256, 257, 1024}) {
                for (int stages : new int[]{1, 4, 8, 9}) {
                    for (int pattern : new int[]{0, 1, 2, 4, 6}) {
                        check(size, capacity, stages, pattern);
                    }
                }
            }
        }
        for (int seed = 0; seed < 150; seed++) {
            int size = 512 + RANDOM.nextInt(3000);
            int capacity = seed % 3 == 0 ? 256 : seed % 3 == 1 ? 257 : 1 + RANDOM.nextInt(256);
            check(size, capacity, 1 + RANDOM.nextInt(10), RANDOM.nextInt(7));
        }
        if (eligible == 0 || fallback == 0) throw new AssertionError("missing native path evidence");
        System.out.println("M3JDK21 state-plane native comparisons=" + cases
                + " small-plane-eligible=" + eligible + " fallback=" + fallback
                + " sha256=" + HexFormat.of().formatHex(DIGEST.digest()));
    }
}
