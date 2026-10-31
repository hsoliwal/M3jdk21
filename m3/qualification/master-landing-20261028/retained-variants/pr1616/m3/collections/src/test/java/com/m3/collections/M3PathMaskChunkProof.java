/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Independent scalar oracle for the existing ordered physical-slot mask owner. */
public final class M3PathMaskChunkProof {
    private static int checks;
    private static long digest = 1;
    private M3PathMaskChunkProof() { }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
    private static void reject(Class<? extends Throwable> type, Checked body) throws Exception {
        try { body.run(); } catch (Throwable failure) {
            Throwable cause = failure instanceof InvocationTargetException ? failure.getCause() : failure;
            check(type.isInstance(cause), "expected " + type + ", got " + cause);
            return;
        }
        throw new AssertionError("missing refusal: " + type.getName());
    }
    private static int[] oracle(int[] rows, int[] offsets, int[][] planes,
            int[] required, int[] denied, int first, int count, int stride) {
        int[] result = new int[count];
        int size = 0;
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int index = (int) (first + (long) ordinal * stride);
            boolean accepted = true;
            for (int stage = 0; stage < planes.length; stage++) {
                int state = planes[stage][offsets[index]];
                if ((state & required[stage]) != required[stage] || (state & denied[stage]) != 0) {
                    accepted = false;
                    break;
                }
            }
            if (accepted) result[size++] = rows[index];
        }
        return Arrays.copyOf(result, size);
    }
    private static void fixture(Random random, int length, int layout,
            M3IndexPathMasks.Backend backend) {
        int[] rows = new int[length], offsets = new int[length];
        int[][] planes = new int[1 + layout % 3][Math.max(1, length + 3)];
        int[] required = new int[planes.length], denied = new int[planes.length];
        for (int i = 0; i < length; i++) {
            rows[i] = i % 5 == 0 ? Integer.MIN_VALUE : random.nextInt(19) - 9;
            offsets[i] = switch (layout % 4) {
                case 0 -> i;
                case 1 -> length - 1 - i;
                case 2 -> i % 7;
                default -> random.nextInt(planes[0].length);
            };
            if (offsets[i] >= planes[0].length) offsets[i] %= planes[0].length;
        }
        for (int stage = 0; stage < planes.length; stage++) {
            for (int i = 0; i < planes[stage].length; i++) planes[stage][i] = random.nextInt();
            required[stage] = switch (layout % 5) {
                case 0 -> 0;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> 1 << stage;
                case 3 -> -1;
                default -> 0;
            };
            denied[stage] = layout % 5 == 4 ? Integer.MIN_VALUE : (layout % 5 == 3 ? -1 : 0);
        }
        int stride = switch (layout % 6) { case 0 -> 1; case 1 -> -1; case 2 -> 2;
            case 3 -> -3; case 4 -> Integer.MAX_VALUE; default -> Integer.MIN_VALUE; };
        int first = length == 0 ? 0 : stride > 0 ? 0 : length - 1;
        int count = length == 0 ? 0 : (int) ((length - 1L) / Math.abs((long) stride) + 1);
        int[] savedRows = rows.clone(), savedOffsets = offsets.clone();
        int[][] savedPlanes = Arrays.stream(planes).map(int[]::clone).toArray(int[][]::new);
        int[] savedRequired = required.clone(), savedDenied = denied.clone();
        int[] expected = oracle(rows, offsets, planes, required, denied, first, count, stride);
        int[] actual = M3IndexPathMasks.select(rows, offsets, planes, required, denied,
                first, count, stride, backend);
        check(Arrays.equals(expected, actual), "oracle mismatch length/layout " + length + "/" + layout);
        for (int value : actual) digest = 31 * digest + value;
        int firstIndex = M3IndexPathMasks.firstMatchingIndex(rows, offsets, planes,
                required, denied, first, count, stride);
        int expectedFirst = -1;
        for (int i = 0; i < count; i++) {
            int at = (int) (first + (long) i * stride);
            if (oracle(rows, offsets, planes, required, denied, at, 1, 1).length != 0) {
                expectedFirst = at; break;
            }
        }
        check(firstIndex == expectedFirst, "first matching logical index");
        int[] gathered = new int[expected.length];
        int written = 0, next = 0;
        while (next < count) {
            int[] window = new int[37];
            Arrays.fill(window, 0x12345678);
            int limit = 1 + layout % 31;
            long receipt = M3IndexPathMasks.scanWindow(rows, offsets, planes, required, denied,
                    first, count, stride, next, window, 2, limit, backend);
            int stop = M3IndexPathMasks.nextOrdinal(receipt), n = M3IndexPathMasks.written(receipt);
            check(stop > next && stop <= count && n >= 0 && n <= limit, "window progress/bounds");
            check(window[0] == 0x12345678 && window[1] == 0x12345678, "prefix untouched");
            for (int i = 2 + n; i < window.length; i++) check(window[i] == 0x12345678, "tail untouched");
            System.arraycopy(window, 2, gathered, written, n);
            written += n; next = stop;
        }
        check(Arrays.equals(gathered, expected), "window composition");
        check(Arrays.equals(rows, savedRows) && Arrays.equals(offsets, savedOffsets)
                && Arrays.deepEquals(planes, savedPlanes) && Arrays.equals(required, savedRequired)
                && Arrays.equals(denied, savedDenied), "caller input mutated");
        if (actual.length != 0) actual[0] ^= 123;
        check(Arrays.equals(rows, savedRows), "detached output");
    }
    private static void publicRefusals(M3IndexPathMasks.Backend backend) throws Exception {
        int[] rows = {9, 8}, offsets = {0, 1};
        int[][] planes = {{1, 2}};
        int[] need = {0}, deny = {0}, out = {77, 77, 77, 77};
        reject(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows, offsets,
                planes, need, deny, 0, 2, Integer.MAX_VALUE, backend));
        reject(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows, offsets,
                planes, need, deny, 0, 1, 0, backend));
        reject(IllegalArgumentException.class, () -> M3IndexPathMasks.select(rows, new int[0],
                planes, need, deny, 0, 0, 1, backend));
        reject(NullPointerException.class, () -> M3IndexPathMasks.select(rows, offsets,
                new int[][]{null}, need, deny, 2, 0, 1, backend));
        reject(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.select(rows, new int[]{0, 2},
                planes, new int[]{4}, deny, 0, 2, 1, backend));
        for (int[] alias : new int[][]{rows, offsets, planes[0], need, deny}) {
            reject(IllegalArgumentException.class, () -> M3IndexPathMasks.scanWindow(rows, offsets,
                    planes, need, deny, 0, 2, 1, 0, alias, 0, 0, backend));
        }
        long zero = M3IndexPathMasks.scanWindow(rows, new int[]{0, 999}, planes, need, deny,
                0, 2, 1, 0, out, 1, 0, backend);
        check(zero == 0 && Arrays.equals(out, new int[]{77,77,77,77}), "zero window no scan/write");
        long first = M3IndexPathMasks.scanWindow(rows, new int[]{0, 999}, planes, need, deny,
                0, 2, 1, 0, out, 1, 1, backend);
        check(M3IndexPathMasks.nextOrdinal(first) == 1 && out[1] == 9, "later offset deferred");
        int[] saved = out.clone();
        reject(IndexOutOfBoundsException.class, () -> M3IndexPathMasks.scanWindow(rows, new int[]{0,999},
                planes, need, deny, 0, 2, 1, 1, out, 0, 1, backend));
        check(Arrays.equals(saved, out), "bad window does not write");
        check(M3IndexPathMasks.select(rows, offsets, planes, need, deny, 2, 0, -1, backend).length == 0,
                "empty path at end");
    }
    private static void defensive(String which) throws Exception {
        check(M3IndexPathMasks.nativeAvailable(), "native library must exist");
        Method method = M3IndexPathMasks.class.getDeclaredMethod("selectNative", int[].class,
                int[].class, int[][].class, int[].class, int[].class, int.class, int.class, int.class);
        method.setAccessible(true);
        if (which.equals("null")) {
            reject(NullPointerException.class, () -> method.invoke(null, new int[0], new int[0],
                    new int[][]{new int[0], null}, new int[2], new int[2], 0, 0, 1));
        } else {
            reject(IndexOutOfBoundsException.class, () -> method.invoke(null, new int[]{4}, new int[]{1},
                    new int[][]{new int[]{0,0}, new int[0]}, new int[]{1,0}, new int[2], 0, 1, 1));
        }
        System.out.println("PASS defensive=" + which + " checks=" + checks);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].startsWith("defensive-")) {
            defensive(args[0].substring("defensive-".length())); return;
        }
        if (args.length == 1 && args[0].equals("missing")) {
            check(!M3IndexPathMasks.nativeAvailable(), "missing library fixture");
            reject(UnsatisfiedLinkError.class, () -> M3IndexPathMasks.select(new int[0], new int[0],
                    new int[][]{new int[0]}, new int[1], new int[1], 0, 0, 1,
                    M3IndexPathMasks.Backend.NATIVE_REQUIRED));
            System.out.println("PASS missing checks=" + checks); return;
        }
        M3IndexPathMasks.Backend backend = args.length == 1 && args[0].equals("native")
                ? M3IndexPathMasks.Backend.NATIVE_REQUIRED : M3IndexPathMasks.Backend.JAVA;
        Random random = new Random(0x4d332026L);
        for (int length : new int[]{0,1,2,7,31,255,256,257,511,512,513,1025,4097,8193}) {
            for (int layout = 0; layout < 30; layout++) fixture(random, length, layout, backend);
        }
        for (int i = 0; i < 1200; i++) fixture(random, random.nextInt(1100), i % 30, backend);
        publicRefusals(backend);
        System.out.println("PASS backend=" + backend + " cases=1620 checks=" + checks
                + " digest=" + Long.toUnsignedString(digest, 16));
    }
}
