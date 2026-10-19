/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.arrays;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Contract oracle for immutable joins, duplicate segment boundaries and explicit JNI projection. */
public final class M3ArrayEmptySegmentsSelfTest {
    private static int checks;
    private static int cases;

    private M3ArrayEmptySegmentsSelfTest() { }

    public static void main(String[] args) {
        if (args.length > 1) throw new IllegalArgumentException("optional native library path");
        boolean nativeRequired = args.length == 1;
        if (nativeRequired) M3ArrayNative.load(Path.of(args[0]));
        if (M3ArrayNative.isLoaded() != nativeRequired) {
            throw new AssertionError("run Java fallback and JNI in separate fresh JVMs");
        }
        runContract();
        System.out.println("M3_ARRAY_EMPTY_SEGMENTS_PASS checks=" + checks
                + " cases=" + cases + " native=" + nativeRequired);
    }

    static void runContract() {
        checks = 0;
        cases = 0;
        Random random = new Random(0x3a22_0ff5_2026L);
        // Every placement of empty components within eight descriptors.
        for (int mask = 0; mask < 256; mask++) {
            int[] lengths = new int[8];
            for (int i = 0; i < lengths.length; i++) lengths[i] = (mask >>> i & 1) == 0 ? 0 : i % 3 + 1;
            exercise(lengths, random, false);
            exercise(lengths, random, true);
        }
        // Ragged multi-element components exercise boundaries away from byte/word alignment.
        for (int round = 0; round < 400; round++) {
            int[] lengths = new int[random.nextInt(25)];
            for (int i = 0; i < lengths.length; i++) lengths[i] = random.nextBoolean() ? 0 : random.nextInt(9);
            exercise(lengths, random, (round & 1) != 0);
        }
        maximumLogicalOffsets();
        failureContracts();
        nativeChunkBoundaries(random);
    }

    private static void exercise(int[] lengths, Random random, boolean nested) {
        M3ByteArrayView[] bytes = new M3ByteArrayView[lengths.length];
        M3Utf16ArrayView[] chars = new M3Utf16ArrayView[lengths.length];
        M3IntArrayView[] ints = new M3IntArrayView[lengths.length];
        M3LongArrayView[] longs = new M3LongArrayView[lengths.length];
        char[][] originalChars = new char[lengths.length][];
        int total = Arrays.stream(lengths).sum();
        byte[] expectedBytes = new byte[total];
        char[] expectedChars = new char[total];
        int[] expectedInts = new int[total];
        long[] expectedLongs = new long[total];
        int offset = 0;
        int segments = 0;
        for (int s = 0; s < lengths.length; s++) {
            int length = lengths[s];
            byte[] b = new byte[length];
            char[] c = new char[length];
            int[] n = new int[length];
            long[] l = new long[length];
            for (int i = 0; i < length; i++) {
                b[i] = (byte) random.nextInt();
                c[i] = (char) random.nextInt(0x10000);
                n[i] = random.nextInt();
                l[i] = random.nextLong();
            }
            // Paired or unpaired surrogates can meet across any number of empty components.
            if (length > 0) c[length - 1] = (s & 1) == 0 ? '\ud83d' : '\ude00';
            bytes[s] = M3Arrays.bytes(b);
            chars[s] = M3Arrays.chars(c);
            ints[s] = M3Arrays.ints(n);
            longs[s] = M3Arrays.longs(l);
            originalChars[s] = c;
            System.arraycopy(b, 0, expectedBytes, offset, length);
            System.arraycopy(c, 0, expectedChars, offset, length);
            System.arraycopy(n, 0, expectedInts, offset, length);
            System.arraycopy(l, 0, expectedLongs, offset, length);
            offset += length;
            if (length > 0) segments++;
        }

        M3ByteArrayView b = M3Arrays.join(bytes);
        M3Utf16ArrayView c = M3Arrays.join(chars);
        M3IntArrayView n = M3Arrays.join(ints);
        M3LongArrayView l = M3Arrays.join(longs);
        if (nested) {
            int middle = lengths.length / 2;
            b = M3Arrays.join(M3Arrays.bytes(new byte[0]),
                    M3Arrays.join(Arrays.copyOfRange(bytes, 0, middle)),
                    M3Arrays.bytes(new byte[0]), M3Arrays.bytes(new byte[0]),
                    M3Arrays.join(Arrays.copyOfRange(bytes, middle, bytes.length)),
                    M3Arrays.bytes(new byte[0]));
            c = M3Arrays.join(M3Arrays.chars(new char[0]),
                    M3Arrays.join(Arrays.copyOfRange(chars, 0, middle)),
                    M3Arrays.chars(new char[0]), M3Arrays.chars(new char[0]),
                    M3Arrays.join(Arrays.copyOfRange(chars, middle, chars.length)),
                    M3Arrays.chars(new char[0]));
            n = M3Arrays.join(M3Arrays.ints(new int[0]),
                    M3Arrays.join(Arrays.copyOfRange(ints, 0, middle)),
                    M3Arrays.ints(new int[0]), M3Arrays.ints(new int[0]),
                    M3Arrays.join(Arrays.copyOfRange(ints, middle, ints.length)),
                    M3Arrays.ints(new int[0]));
            l = M3Arrays.join(M3Arrays.longs(new long[0]),
                    M3Arrays.join(Arrays.copyOfRange(longs, 0, middle)),
                    M3Arrays.longs(new long[0]), M3Arrays.longs(new long[0]),
                    M3Arrays.join(Arrays.copyOfRange(longs, middle, longs.length)),
                    M3Arrays.longs(new long[0]));
        }
        checkViews(b, c, n, l, expectedBytes, expectedChars, expectedInts, expectedLongs);
        equal(segments, b.segmentCount());
        equal(segments, c.segmentCount());
        check(M3Arrays.join(b) == b && M3Arrays.join(c) == c
                && M3Arrays.join(n) == n && M3Arrays.join(l) == l, "single-owner identity");
        check(Arrays.equals(expectedChars, M3ArrayNative.concatenateUtf16(originalChars)), "JNI/fallback empty segments");
        equal(0, M3ArrayNative.compareUtf16(c.copy(), 0, expectedChars, 0, total));

        for (int trial = 0; trial < 5; trial++) {
            int from = random.nextInt(total + 1);
            int to = from + random.nextInt(total - from + 1);
            checkViews(b.slice(from, to), c.subSequence(from, to), n.slice(from, to), l.slice(from, to),
                    Arrays.copyOfRange(expectedBytes, from, to), Arrays.copyOfRange(expectedChars, from, to),
                    Arrays.copyOfRange(expectedInts, from, to), Arrays.copyOfRange(expectedLongs, from, to));
            int again = random.nextInt(to - from + 1);
            check(Arrays.equals(Arrays.copyOfRange(expectedInts, from + again, to),
                    n.slice(from, to).slice(again, to - from).copy()), "nested int slice");
            check(Arrays.equals(Arrays.copyOfRange(expectedLongs, from + again, to),
                    l.slice(from, to).slice(again, to - from).copy()), "nested long slice");
        }
        cases++;
    }

    private static void checkViews(M3ByteArrayView bytes, M3Utf16ArrayView chars,
            M3IntArrayView ints, M3LongArrayView longs,
            byte[] b, char[] c, int[] n, long[] l) {
        equal(b.length, bytes.length());
        equal(c.length, chars.length());
        equal(n.length, ints.length());
        equal(l.length, longs.length());
        for (int i = 0; i < b.length; i++) {
            equal(b[i], bytes.byteAt(i));
            equal(c[i], chars.charAt(i));
            equal(n[i], ints.intAt(i));
            equal(l[i], longs.longAt(i));
        }
        check(Arrays.equals(b, bytes.copy()), "byte materialization");
        check(Arrays.equals(c, chars.copy()), "char materialization");
        check(Arrays.equals(n, ints.copy()), "int materialization");
        check(Arrays.equals(l, longs.copy()), "long materialization");
        equal(bytes.asReadOnlyBuffers().length, bytes.segmentCount());
        equal(chars.asReadOnlyCharBuffers().length, chars.segmentCount());
        String expected = new String(c);
        equal(expected.hashCode(), chars.javaHashCode());
        equal(expected.codePointCount(0, c.length), chars.codePointCount());
        check(Arrays.equals(expected.codePoints().toArray(), chars.codePoints().toArray()), "UTF-16 seam code points");
        check(expected.equals(chars.toString()), "UTF-16 string projection");
        check(chars.contentEquals(M3Arrays.chars(c)), "UTF-16 exact equality");
        equal(0, chars.compareTo(M3Arrays.chars(c)));
        if (c.length != 0) {
            equal(expected.codePointAt(0), chars.codePointAt(0));
            equal(expected.codePointBefore(c.length), chars.codePointBefore(c.length));
            String needle = expected.substring(c.length / 2);
            equal(expected.indexOf(needle), chars.indexOf(needle, 0));
            equal(expected.lastIndexOf(needle), chars.lastIndexOf(needle, c.length));
        }
        // Explicit projections preserve guard cells and produce caller-owned arrays.
        byte[] targetB = new byte[b.length + 4]; Arrays.fill(targetB, (byte) 93);
        char[] targetC = new char[c.length + 4]; Arrays.fill(targetC, '\ufefd');
        int[] targetN = new int[n.length + 4]; Arrays.fill(targetN, Integer.MIN_VALUE);
        long[] targetL = new long[l.length + 4]; Arrays.fill(targetL, Long.MIN_VALUE);
        bytes.copyTo(0, targetB, 2, b.length);
        chars.copyTo(0, targetC, 2, c.length);
        ints.copyTo(0, targetN, 2, n.length);
        longs.copyTo(0, targetL, 2, l.length);
        check(Arrays.equals(b, Arrays.copyOfRange(targetB, 2, targetB.length - 2)), "byte copy window");
        check(Arrays.equals(c, Arrays.copyOfRange(targetC, 2, targetC.length - 2)), "char copy window");
        check(Arrays.equals(n, Arrays.copyOfRange(targetN, 2, targetN.length - 2)), "int copy window");
        check(Arrays.equals(l, Arrays.copyOfRange(targetL, 2, targetL.length - 2)), "long copy window");
        for (int i : new int[] {0, 1, b.length + 2, b.length + 3}) {
            equal(93, targetB[i]); equal('\ufefd', targetC[i]);
            equal(Integer.MIN_VALUE, targetN[i]); equal(Long.MIN_VALUE, targetL[i]);
        }
        expect(IndexOutOfBoundsException.class, () -> bytes.byteAt(-1));
        expect(IndexOutOfBoundsException.class, () -> chars.charAt(c.length));
        expect(IndexOutOfBoundsException.class, () -> ints.intAt(n.length));
        expect(IndexOutOfBoundsException.class, () -> longs.longAt(-1));
        for (ByteBuffer buffer : bytes.asReadOnlyBuffers()) {
            check(buffer.isReadOnly() && buffer.hasRemaining(), "byte descriptor shape");
            expect(ReadOnlyBufferException.class, () -> buffer.put(buffer.position(), (byte) 0));
        }
        for (CharBuffer buffer : chars.asReadOnlyCharBuffers()) {
            check(buffer.isReadOnly() && buffer.hasRemaining(), "char descriptor shape");
            expect(ReadOnlyBufferException.class, () -> buffer.put(buffer.position(), 'x'));
        }
    }

    private static void maximumLogicalOffsets() {
        // Virtual immutable value functions need no near-2 GiB allocation to test legal int offsets.
        M3IntArrayView largeInts = new IntRange(0, Integer.MAX_VALUE - 1);
        M3LongArrayView largeLongs = new LongRange(0, Integer.MAX_VALUE - 1);
        M3IntArrayView ints = M3Arrays.join(largeInts, M3Arrays.ints(new int[0]),
                M3Arrays.ints(new int[0]), M3Arrays.ints(new int[] {-7}));
        M3LongArrayView longs = M3Arrays.join(largeLongs, M3Arrays.longs(new long[0]),
                M3Arrays.longs(new long[0]), M3Arrays.longs(new long[] {Long.MIN_VALUE}));
        equal(Integer.MAX_VALUE, ints.length()); equal(Integer.MAX_VALUE, longs.length());
        equal(Integer.MAX_VALUE - 2, ints.intAt(Integer.MAX_VALUE - 2));
        equal(Integer.MAX_VALUE - 2L, longs.longAt(Integer.MAX_VALUE - 2));
        equal(-7, ints.intAt(Integer.MAX_VALUE - 1));
        equal(Long.MIN_VALUE, longs.longAt(Integer.MAX_VALUE - 1));
        check(Arrays.equals(new int[] {Integer.MAX_VALUE - 2, -7},
                ints.slice(Integer.MAX_VALUE - 2, Integer.MAX_VALUE).copy()), "maximum int slice");
        check(Arrays.equals(new long[] {Integer.MAX_VALUE - 2L, Long.MIN_VALUE},
                longs.slice(Integer.MAX_VALUE - 2, Integer.MAX_VALUE).copy()), "maximum long slice");
        expect(ArithmeticException.class, () -> M3Arrays.join(ints, M3Arrays.ints(new int[] {1})));
        expect(ArithmeticException.class, () -> M3Arrays.join(longs, M3Arrays.longs(new long[] {1})));
        expect(IndexOutOfBoundsException.class, () -> ints.intAt(Integer.MAX_VALUE));
    }

    private record IntRange(int base, int length) implements M3IntArrayView {
        @Override public int intAt(int index) { return base + Objects.checkIndex(index, length); }
        @Override public M3IntArrayView slice(int from, int to) {
            Objects.checkFromToIndex(from, to, length); return new IntRange(base + from, to - from);
        }
    }

    private record LongRange(int base, int length) implements M3LongArrayView {
        @Override public long longAt(int index) { return (long) base + Objects.checkIndex(index, length); }
        @Override public M3LongArrayView slice(int from, int to) {
            Objects.checkFromToIndex(from, to, length); return new LongRange(base + from, to - from);
        }
    }

    private static void failureContracts() {
        expect(NullPointerException.class, () -> M3Arrays.join((M3IntArrayView[]) null));
        expect(NullPointerException.class, () -> M3Arrays.join(M3Arrays.ints(new int[0]), (M3IntArrayView) null));
        expect(NullPointerException.class, () -> M3ArrayNative.concatenateUtf16(new char[0], null));
        char[] guarded = {'g', 'g', 'g'};
        M3Utf16ArrayView view = M3Arrays.join(M3Arrays.chars("a"), M3Arrays.chars(""));
        expect(IndexOutOfBoundsException.class, () -> view.copyTo(0, guarded, 3, 1));
        check(Arrays.equals(new char[] {'g', 'g', 'g'}, guarded), "failed copy leaves guards");
        expect(IndexOutOfBoundsException.class, () -> view.subSequence(1, 0));
        expect(IndexOutOfBoundsException.class, () -> M3ArrayNative.compareUtf16(guarded, Integer.MAX_VALUE, guarded, 0, 1));
        expect(IndexOutOfBoundsException.class, () -> M3ArrayNative.compareUtf16(guarded, 0, guarded, 0, -1));
    }

    private static void nativeChunkBoundaries(Random random) {
        for (int length : new int[] {0, 1, 1023, 1024, 1025, 2048, 4097}) {
            char[] left = new char[length + 7];
            for (int i = 0; i < left.length; i++) left[i] = (char) random.nextInt(0x10000);
            char[] right = left.clone();
            equal(0, M3ArrayNative.compareUtf16(left, 3, right, 3, length));
            for (int changed : new int[] {0, length / 2, length - 1}) {
                if (length == 0) continue;
                right[3 + changed] ^= '\u8000';
                equal((int) left[3 + changed] - right[3 + changed],
                        M3ArrayNative.compareUtf16(left, 3, right, 3, length));
                right[3 + changed] = left[3 + changed];
            }
            char[][] chunks = {new char[0], Arrays.copyOfRange(left, 3, 3 + length / 2),
                    new char[0], new char[0], Arrays.copyOfRange(left, 3 + length / 2, 3 + length), new char[0]};
            check(Arrays.equals(Arrays.copyOfRange(left, 3, 3 + length),
                    M3ArrayNative.concatenateUtf16(chunks)), "JNI region chunk and empty boundary");
        }
    }

    private static void equal(long expected, long actual) {
        check(expected == actual, "expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("expected " + kind.getName(), failure);
        }
        throw new AssertionError("expected " + kind.getName());
    }
}
