// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Random;

/** Java 21 standalone differential oracle, reusable by the Maven/JUnit wrapper. */
//FLLDCIM: RELOCATE - donor blob eefb67e38f65 at ac5feec00a6d; recipe make/m3/collections/recipe/entryless-lane-kernel-20261106/relocate.py v3
public final class IndexMaskLaneKernelProof {
    private IndexMaskLaneKernelProof() { }
    private static int checked;

    private static void same(long[] k, long[] v, byte[] m, int start, int count,
            long lowK, long highK, long lowV, long highV) {
        int[] expected = new int[count];
        int size = 0;
        for (int i = start; i < start + count; i++) {
            if ((m[i] & 1) != 0 && k[i] >= lowK && k[i] <= highK
                    && v[i] >= lowV && v[i] <= highV) expected[size++] = i;
        }
        expected = Arrays.copyOf(expected, size);
        int[] java = IndexMaskCompactor.selectJava(k, v, m, start, count,
                lowK, highK, lowV, highV);
        int[] selected = IndexMaskCompactor.select(k, v, m, start, count,
                lowK, highK, lowV, highV);
        if (!Arrays.equals(expected, java) || !Arrays.equals(expected, selected)) {
            throw new AssertionError("slot mismatch at case " + checked);
        }
        // Reuse a caller-owned result lane. Guard words must remain unchanged.
        int[] intoJava = new int[size + 4];
        int[] intoSelected = new int[size + 4];
        Arrays.fill(intoJava, -77);
        Arrays.fill(intoSelected, -77);
        int writtenJava = IndexMaskCompactor.selectIntoJava(k, v, m, start, count,
                lowK, highK, lowV, highV, intoJava, 2);
        int writtenSelected = IndexMaskCompactor.selectInto(k, v, m, start, count,
                lowK, highK, lowV, highV, intoSelected, 2);
        if (writtenJava != size || writtenSelected != size
                || !Arrays.equals(expected, Arrays.copyOfRange(intoJava, 2, 2 + size))
                || !Arrays.equals(expected, Arrays.copyOfRange(intoSelected, 2, 2 + size))
                || intoJava[0] != -77 || intoJava[1] != -77 || intoJava[size + 2] != -77
                || intoSelected[0] != -77 || intoSelected[1] != -77
                || intoSelected[size + 2] != -77) {
            throw new AssertionError("reusable output lane mismatch at case " + checked);
        }
        if (size > 0) {
            int[] tooSmall = new int[size];
            Arrays.fill(tooSmall, -91);
            expectsNoCount(IndexOutOfBoundsException.class,
                    () -> IndexMaskCompactor.selectInto(k, v, m, start, count,
                            lowK, highK, lowV, highV, tooSmall, 1));
            for (int slot : tooSmall) {
                if (slot != -91) throw new AssertionError("failed capacity check mutated result");
            }
        }
        int expectedFirst = size == 0 ? -1 : expected[0];
        int firstJava = IndexMaskCompactor.firstMatchingJava(k, v, m, start, count,
                lowK, highK, lowV, highV);
        int first = IndexMaskCompactor.firstMatching(k, v, m, start, count,
                lowK, highK, lowV, highV);
        int countJava = IndexMaskCompactor.countMatchingJava(k, v, m, start, count,
                lowK, highK, lowV, highV);
        int matching = IndexMaskCompactor.countMatching(k, v, m, start, count,
                lowK, highK, lowV, highV);
        if (firstJava != expectedFirst || first != expectedFirst
                || countJava != size || matching != size) {
            throw new AssertionError("first/count mismatch at case " + checked);
        }
        if (IndexMaskCompactor.laneKernelNativeAvailable()) {
            int[] nativeResult = IndexMaskCompactor.selectNativeRequired(k, v, m,
                    start, count, lowK, highK, lowV, highV);
            if (!Arrays.equals(expected, nativeResult)) {
                throw new AssertionError("native mismatch at case " + checked);
            }
            int nativeFirst = IndexMaskCompactor.firstMatchingNativeRequired(k, v, m,
                    start, count, lowK, highK, lowV, highV);
            int nativeCount = IndexMaskCompactor.countMatchingNativeRequired(k, v, m,
                    start, count, lowK, highK, lowV, highV);
            int[] nativeInto = new int[size + 3];
            Arrays.fill(nativeInto, -71);
            int nativeWritten = IndexMaskCompactor.selectIntoNativeRequired(k, v, m,
                    start, count, lowK, highK, lowV, highV, nativeInto, 1);
            if (nativeWritten != size
                    || !Arrays.equals(expected, Arrays.copyOfRange(nativeInto, 1, size + 1))
                    || nativeInto[0] != -71 || nativeInto[size + 1] != -71) {
                throw new AssertionError("native reusable output mismatch at case " + checked);
            }
            if (nativeFirst != expectedFirst || nativeCount != size) {
                throw new AssertionError("native first/count mismatch at case " + checked);
            }
        }
        checked++;
    }

    /** Validate bounded one-pass pages and continuation against the full selection oracle. */
    private static void pageSame(long[] k, long[] v, byte[] m, int start, int count,
            long lowK, long highK, long lowV, long highV) {
        int[] all = IndexMaskCompactor.selectJava(k, v, m, start, count,
                lowK, highK, lowV, highV);
        int pageSize = count > 50000 ? 257 : 1 + (count % 19);
        int[] javaBuffer = new int[pageSize + 1];
        int[] resultBuffer = new int[pageSize + 1];
        int[] nativeBuffer = new int[pageSize + 1];
        int next = start;
        int consumed = 0;
        while (next < start + count) {
            Arrays.fill(javaBuffer, -31);
            Arrays.fill(resultBuffer, -31);
            Arrays.fill(nativeBuffer, -31);
            int remaining = start + count - next;
            long j = IndexMaskCompactor.scanPageIntoJava(k, v, m, next, remaining,
                    lowK, highK, lowV, highV, javaBuffer, 1);
            long a = IndexMaskCompactor.scanPageInto(k, v, m, next, remaining,
                    lowK, highK, lowV, highV, resultBuffer, 1);
            if (j != a) throw new AssertionError("scan page cursor/count mismatch");
            if (IndexMaskCompactor.laneKernelNativeAvailable()) {
                long n = IndexMaskCompactor.scanPageIntoNativeRequired(k, v, m, next, remaining,
                        lowK, highK, lowV, highV, nativeBuffer, 1);
                if (j != n || !Arrays.equals(javaBuffer, nativeBuffer)) {
                    throw new AssertionError("JNI page mismatch");
                }
            }
            int written = IndexMaskCompactor.pageWritten(j);
            int following = IndexMaskCompactor.pageNext(j);
            if (written < 0 || written > pageSize || following <= next
                    || following > start + count || consumed + written > all.length
                    || javaBuffer[0] != -31 || resultBuffer[0] != -31
                    || !Arrays.equals(javaBuffer, resultBuffer)) {
                throw new AssertionError("page progression/output bounds mismatch");
            }
            for (int i = 0; i < written; i++) {
                if (javaBuffer[1 + i] != all[consumed + i]) {
                    throw new AssertionError("page lost/duplicated matching slot");
                }
            }
            consumed += written;
            next = following;
        }
        if (consumed != all.length) throw new AssertionError("page lost matching slots");
        // An empty input window admits an empty destination and returns its original cursor.
        long empty = IndexMaskCompactor.scanPageInto(k, v, m, start + count, 0,
                lowK, highK, lowV, highV, new int[0], 0);
        if (IndexMaskCompactor.pageNext(empty) != start + count
                || IndexMaskCompactor.pageWritten(empty) != 0) {
            throw new AssertionError("empty page result mismatch");
        }
        checked++;
    }

    private static void expects(Class<? extends Throwable> type, Runnable operation) {
        try { operation.run(); } catch (Throwable ex) {
            if (type.isInstance(ex)) { checked++; return; }
            throw new AssertionError("expected " + type + "; got " + ex, ex);
        }
        throw new AssertionError("expected " + type.getName());
    }

    /** Rejection check that does not affect the number of test vectors reported. */
    private static void expectsNoCount(Class<? extends Throwable> type, Runnable operation) {
        try { operation.run(); } catch (Throwable ex) {
            if (type.isInstance(ex)) return;
            throw new AssertionError("expected " + type + "; got " + ex, ex);
        }
        throw new AssertionError("expected " + type.getName());
    }

    public static void main(String[] arguments) {
        long[] first = {-5, 0, 5, Long.MAX_VALUE, Long.MIN_VALUE, 17};
        long[] second = {7, 0, -1, Long.MAX_VALUE, Long.MIN_VALUE, 17};
        byte[] mask = {1, 0, 3, 1, 1, 0};
        same(first, second, mask, 0, first.length,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        same(first, second, mask, 1, 3, -10, 10, -2, 10);
        same(first, second, mask, first.length, 0, 0, 0, 0, 0);
        same(first, second, mask, 0, first.length, Long.MAX_VALUE, Long.MAX_VALUE,
                Long.MAX_VALUE, Long.MAX_VALUE);
        same(first, second, mask, 0, first.length, Long.MIN_VALUE, Long.MIN_VALUE,
                Long.MIN_VALUE, Long.MIN_VALUE);
        expects(NullPointerException.class, () -> IndexMaskCompactor.select(null, second, mask,
                0, 1, 0, 0, 0, 0));
        expects(NullPointerException.class, () -> IndexMaskCompactor.select(first, null, mask,
                0, 1, 0, 0, 0, 0));
        expects(NullPointerException.class, () -> IndexMaskCompactor.select(first, second, null,
                0, 1, 0, 0, 0, 0));
        expects(IllegalArgumentException.class, () -> IndexMaskCompactor.select(first, second,
                mask, 0, 1, 2, 1, 0, 0));
        expects(IllegalArgumentException.class, () -> IndexMaskCompactor.select(first, second,
                mask, 0, 1, 0, 0, 2, 1));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.select(first, second,
                mask, 0, Integer.MAX_VALUE, 0, 0, 0, 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.select(first, second,
                mask, -1, 1, 0, 0, 0, 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.select(first, second,
                mask, 2, -1, 0, 0, 0, 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.select(first,
                new long[2], mask, 1, 2, 0, 0, 0, 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.countMatching(
                first, second, mask, first.length, 1, 0, 0, 0, 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.firstMatching(
                first, second, mask, 0, Integer.MAX_VALUE, 0, 0, 0, 0));
        expects(NullPointerException.class, () -> IndexMaskCompactor.countMatching(
                first, null, mask, 0, 0, 0, 0, 0, 0));
        expects(IllegalArgumentException.class, () -> IndexMaskCompactor.firstMatching(
                first, second, mask, 0, 0, 1, 0, 0, 0));
        expects(NullPointerException.class, () -> IndexMaskCompactor.selectInto(
                first, second, mask, 0, 0, 0, 0, 0, 0, null, 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.selectInto(
                first, second, mask, 0, 0, 0, 0, 0, 0, new int[1], -1));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.selectInto(
                first, second, mask, 0, 0, 0, 0, 0, 0, new int[1], 2));
        expects(IllegalArgumentException.class, () -> IndexMaskCompactor.selectInto(
                first, second, mask, 0, 1, 2, 1, 0, 0, new int[2], 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.scanPageInto(
                first, second, mask, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE, new int[0], 0));
        expects(IndexOutOfBoundsException.class, () -> IndexMaskCompactor.scanPageInto(
                first, second, mask, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE, new int[2], -1));
        expects(NullPointerException.class, () -> IndexMaskCompactor.scanPageInto(
                first, second, mask, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE, null, 0));
        pageSame(first, second, mask, 0, first.length, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE);
        pageSame(first, second, mask, 3, 0, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE);
        if (!IndexMaskCompactor.laneKernelNativeAvailable()) {
            expects(UnsatisfiedLinkError.class, () -> IndexMaskCompactor.scanPageIntoNativeRequired(
                    first, second, mask, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE,
                    Long.MIN_VALUE, Long.MAX_VALUE, new int[1], 0));
            expects(UnsatisfiedLinkError.class, () -> IndexMaskCompactor.selectNativeRequired(
                    first, second, mask, 0, 1, 0, 0, 0, 0));
            expects(UnsatisfiedLinkError.class, () -> IndexMaskCompactor.countMatchingNativeRequired(
                    first, second, mask, 0, 1, 0, 0, 0, 0));
            expects(UnsatisfiedLinkError.class, () -> IndexMaskCompactor.selectIntoNativeRequired(
                    first, second, mask, 0, 0, 0, 0, 0, 0, new int[0], 0));
            expects(UnsatisfiedLinkError.class, () -> IndexMaskCompactor.firstMatchingNativeRequired(
                    first, second, mask, 0, 1, 0, 0, 0, 0));
        }
        Random random = new Random(0x5A17D00FL);
        for (int round = 0; round < 3200; round++) {
            int length = random.nextInt(1000);
            long[] keys = new long[length];
            long[] values = new long[length];
            byte[] flags = new byte[length];
            for (int i = 0; i < length; i++) {
                keys[i] = random.nextInt(801) - 400;
                values[i] = random.nextInt(801) - 400;
                flags[i] = (byte) random.nextInt(256);
            }
            int start = random.nextInt(length + 1);
            int count = random.nextInt(length - start + 1);
            long a = random.nextInt(1001) - 500;
            long b = random.nextInt(1001) - 500;
            long c = random.nextInt(1001) - 500;
            long d = random.nextInt(1001) - 500;
            same(keys, values, flags, start, count, Math.min(a, b), Math.max(a, b),
                    Math.min(c, d), Math.max(c, d));
            if (round % 4 == 0) pageSame(keys, values, flags, start, count,
                    Math.min(a, b), Math.max(a, b), Math.min(c, d), Math.max(c, d));
        }
        // Long batch (>256), all matching, large output and array-size growth.
        long[] bigKeys = new long[140000];
        long[] bigValues = new long[140000];
        byte[] bigState = new byte[140000];
        Arrays.fill(bigState, (byte) 1);
        same(bigKeys, bigValues, bigState, 0, bigKeys.length, 0, 0, 0, 0);
        pageSame(bigKeys, bigValues, bigState, 0, bigKeys.length, 0, 0, 0, 0);
        System.out.println("PASS cases=" + checked + " native=" + IndexMaskCompactor.laneKernelNativeAvailable());
    }
}
