// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays.acceptance;

import com.m3.arrays.M3ArrayNative;
import com.m3.arrays.M3Arrays;
import com.m3.arrays.M3ByteArrayView;
import com.m3.arrays.M3IntArrayView;
import com.m3.arrays.M3LongArrayView;
import com.m3.arrays.M3Utf16ArrayView;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Compile against the pinned baseline, then run unchanged bytecode on the installed candidate. */
public final class LegacyArraysClient {
    private static int checks;

    private LegacyArraysClient() { }

    public static void main(String[] arguments) throws Exception {
        require(arguments.length == 1, "one explicit installed native-library path is required");
        Module module = M3Arrays.class.getModule();
        require(module.isNamed() && "com.m3.arrays".equals(module.getName()), "named arrays owner");
        require(module == M3ArrayNative.class.getModule(), "one existing arrays module");
        require(module.getLayer() == ModuleLayer.boot(), "arrays owner in boot layer");
        require("jrt".equals(ModuleLayer.boot().configuration().findModule("com.m3.arrays")
                .orElseThrow().reference().location().orElseThrow().getScheme()), "image module location");
        Path installed = Path.of(System.getProperty("java.home"), "lib", System.mapLibraryName("m3_arrays"));
        require(Files.isRegularFile(installed), "installed native library exists");
        Path supplied = Path.of(arguments[0]).toRealPath();
        require(supplied.equals(installed.toRealPath()), "explicit library belongs to running image");
        require(!M3ArrayNative.isLoaded(), "fresh process exercises the old fallback first");

        checkUtf16Boundary("fallback");
        checkNegativeAllocations();
        checkImmutableViews();
        require(!M3ArrayNative.isLoaded(), "portable checks do not load JNI");

        M3ArrayNative.load(supplied);
        require(M3ArrayNative.isLoaded(), "explicit installed library loaded");
        checkUtf16Boundary("native");
        checkNegativeAllocations();
        checkRequiredAllocations();
        System.out.println("M3_LEGACY_ARRAYS_CLIENT_PASS checks=" + checks
                + " module=com.m3.arrays origin=jrt native_loaded=true");
    }

    private static void checkUtf16Boundary(String mode) {
        char[] left = {'x', 'a', 0, '\uffff', '\ud83d', '\ude00', 'z'};
        char[] right = {'y', 'a', 0, '\uffff', '\ud83d', '\ude00', 'w'};
        char[] originalLeft = left.clone();
        char[] originalRight = right.clone();
        require(M3ArrayNative.compareUtf16(left, 1, right, 1, 5) == 0, mode + " offset equality");
        require(M3ArrayNative.compareUtf16(left, 0, right, 0, left.length) == -1,
                mode + " first mismatch");
        require(M3ArrayNative.compareUtf16(left, 6, right, 6, 1) == 3, mode + " exact difference");
        require(M3ArrayNative.compareUtf16(new char[] {'\uffff'}, 0, new char[] {0}, 0, 1) == 65535,
                mode + " unsigned UTF-16 difference is not normalized");
        require(M3ArrayNative.compareUtf16(left, left.length, right, right.length, 0) == 0,
                mode + " empty terminal ranges");
        require(Arrays.equals(left, originalLeft) && Arrays.equals(right, originalRight),
                mode + " compare inputs unchanged");

        char[] first = {'A', '\ud83d'};
        char[] last = {'\ude00', 'B', '\ud800'};
        char[] concatenated = M3ArrayNative.concatenateUtf16(new char[0], first, new char[0], last);
        require(Arrays.equals(new char[] {'A', '\ud83d', '\ude00', 'B', '\ud800'}, concatenated),
                mode + " concatenation preserves paired and unpaired surrogates");
        concatenated[0] = '?';
        require(first[0] == 'A', mode + " concatenate output is detached");
        char[] empty = M3ArrayNative.concatenateUtf16();
        require(empty.length == 0 && empty != M3ArrayNative.concatenateUtf16(),
                mode + " empty concatenate returns a fresh array");
        expect(NullPointerException.class, () -> M3ArrayNative.compareUtf16(null, 0, right, 0, 0),
                mode + " null compare input");
        expect(IndexOutOfBoundsException.class, () -> M3ArrayNative.compareUtf16(left, -1, right, 0, 0),
                mode + " invalid compare range");
        expect(NullPointerException.class, () -> M3ArrayNative.concatenateUtf16((char[][]) null),
                mode + " null segments array");
        expect(NullPointerException.class, () -> M3ArrayNative.concatenateUtf16(first, null),
                mode + " null segment");
    }

    private static void checkNegativeAllocations() {
        expect(NegativeArraySizeException.class, () -> M3ArrayNative.allocateObjectsRequired(-1),
                "negative Object allocation precedes loading");
        expect(NegativeArraySizeException.class, () -> M3ArrayNative.allocateBytesRequired(-1),
                "negative byte allocation precedes loading");
        expect(NegativeArraySizeException.class, () -> M3ArrayNative.allocateIntsRequired(-1),
                "negative int allocation precedes loading");
        expect(NegativeArraySizeException.class, () -> M3ArrayNative.allocateLongsRequired(-1),
                "negative long allocation precedes loading");
    }

    private static void checkRequiredAllocations() {
        for (int length : new int[] {0, 1, 257}) {
            Object[] objects = M3ArrayNative.allocateObjectsRequired(length);
            byte[] bytes = M3ArrayNative.allocateBytesRequired(length);
            int[] ints = M3ArrayNative.allocateIntsRequired(length);
            long[] longs = M3ArrayNative.allocateLongsRequired(length);
            Object[] otherObjects = M3ArrayNative.allocateObjectsRequired(length);
            byte[] otherBytes = M3ArrayNative.allocateBytesRequired(length);
            int[] otherInts = M3ArrayNative.allocateIntsRequired(length);
            long[] otherLongs = M3ArrayNative.allocateLongsRequired(length);
            require(objects.getClass() == Object[].class && Arrays.equals(objects, new Object[length]),
                    "Object storage has exact component type, length and null initialization");
            require(Arrays.equals(bytes, new byte[length]), "byte storage is zero initialized");
            require(Arrays.equals(ints, new int[length]), "int storage is zero initialized");
            require(Arrays.equals(longs, new long[length]), "long storage is zero initialized");
            require(objects != otherObjects && bytes != otherBytes && ints != otherInts && longs != otherLongs,
                    "every required allocation is detached, including empty arrays");
            if (length != 0) {
                objects[0] = new Object();
                bytes[0] = -1;
                ints[0] = Integer.MIN_VALUE;
                longs[0] = Long.MAX_VALUE;
                require(otherObjects[0] == null && otherBytes[0] == 0
                        && otherInts[0] == 0 && otherLongs[0] == 0, "independent mutable allocations");
            }
        }
    }

    private static void checkImmutableViews() {
        byte[] sourceBytes = {1, -2, 3, 4};
        M3ByteArrayView bytes = M3Arrays.bytes(sourceBytes);
        sourceBytes[1] = 99;
        require(bytes.length() == 4 && bytes.byteAt(1) == -2, "byte snapshot owns its payload");
        byte[] detachedBytes = bytes.copy();
        detachedBytes[0] = 99;
        require(bytes.byteAt(0) == 1, "byte copy is detached");
        M3ByteArrayView byteJoin = M3Arrays.join(M3Arrays.bytes(new byte[0]), bytes.slice(0, 2),
                M3Arrays.bytes(new byte[0]), bytes.slice(2, 4), M3Arrays.bytes(new byte[0]));
        require(Arrays.equals(new byte[] {1, -2, 3, 4}, byteJoin.copy()), "byte join skips empty seams");
        require(Arrays.equals(new byte[] {-2, 3}, byteJoin.slice(1, 3).copy()), "byte slice crosses seam");
        require(byteJoin.segmentCount() == 2, "byte physical nonempty segment count");
        ByteBuffer[] byteBuffers = byteJoin.asReadOnlyBuffers();
        require(byteBuffers.length == 2 && byteBuffers[0].isReadOnly() && byteBuffers[1].isReadOnly(),
                "read-only byte descriptors");
        expect(ReadOnlyBufferException.class, () -> byteBuffers[0].put(0, (byte) 9), "byte write refused");
        byteBuffers[0].position(byteBuffers[0].limit());
        require(byteJoin.asReadOnlyBuffers()[0].remaining() == 2, "byte descriptor positions are independent");
        byte[] byteTarget = {9, 9, 9, 9};
        byteJoin.copyTo(1, byteTarget, 1, 2);
        require(Arrays.equals(new byte[] {9, -2, 3, 9}, byteTarget), "byte range copy boundaries");
        expect(IndexOutOfBoundsException.class, () -> byteJoin.copyTo(0, byteTarget, 3, 2),
                "byte range rejected before write");
        require(Arrays.equals(new byte[] {9, -2, 3, 9}, byteTarget), "invalid byte copy leaves target unchanged");

        int[] sourceInts = {Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE};
        M3IntArrayView ints = M3Arrays.ints(sourceInts);
        sourceInts[0] = 99;
        require(ints.length() == 4 && ints.intAt(0) == Integer.MIN_VALUE, "int snapshot owns its payload");
        int[] detachedInts = ints.copy();
        detachedInts[1] = 99;
        require(ints.intAt(1) == -1, "int copy is detached");
        M3IntArrayView intJoin = M3Arrays.join(M3Arrays.ints(new int[0]), ints.slice(0, 2),
                M3Arrays.ints(new int[0]), ints.slice(2, 4), M3Arrays.ints(new int[0]));
        require(Arrays.equals(new int[] {Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}, intJoin.copy()),
                "int join preserves full-width values");
        require(Arrays.equals(new int[] {-1, 0}, intJoin.slice(1, 3).copy()), "int slice crosses seam");
        int[] intTarget = {9, 9, 9, 9};
        intJoin.copyTo(1, intTarget, 1, 2);
        require(Arrays.equals(new int[] {9, -1, 0, 9}, intTarget), "int range copy boundaries");
        expect(IndexOutOfBoundsException.class, () -> intJoin.copyTo(3, intTarget, 0, 2),
                "int range rejected before write");
        require(Arrays.equals(new int[] {9, -1, 0, 9}, intTarget), "invalid int copy leaves target unchanged");

        long[] sourceLongs = {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE};
        M3LongArrayView longs = M3Arrays.longs(sourceLongs);
        sourceLongs[0] = 99;
        require(longs.length() == 4 && longs.longAt(0) == Long.MIN_VALUE, "long snapshot owns its payload");
        long[] detachedLongs = longs.copy();
        detachedLongs[1] = 99;
        require(longs.longAt(1) == -1, "long copy is detached");
        M3LongArrayView longJoin = M3Arrays.join(M3Arrays.longs(new long[0]), longs.slice(0, 2),
                M3Arrays.longs(new long[0]), longs.slice(2, 4), M3Arrays.longs(new long[0]));
        require(Arrays.equals(new long[] {Long.MIN_VALUE, -1, 0, Long.MAX_VALUE}, longJoin.copy()),
                "long join preserves full-width values");
        require(Arrays.equals(new long[] {-1, 0}, longJoin.slice(1, 3).copy()), "long slice crosses seam");
        long[] longTarget = {9, 9, 9, 9};
        longJoin.copyTo(1, longTarget, 1, 2);
        require(Arrays.equals(new long[] {9, -1, 0, 9}, longTarget), "long range copy boundaries");
        expect(IndexOutOfBoundsException.class, () -> longJoin.copyTo(0, longTarget, -1, 2),
                "long range rejected before write");
        require(Arrays.equals(new long[] {9, -1, 0, 9}, longTarget), "invalid long copy leaves target unchanged");

        char[] sourceChars = {'A', '\ud83d', '\ude00', 'B', '\ud800'};
        char[] expectedChars = sourceChars.clone();
        M3Utf16ArrayView chars = M3Arrays.chars(sourceChars);
        sourceChars[0] = '?';
        require(chars.length() == 5 && chars.charAt(0) == 'A', "UTF-16 snapshot owns its payload");
        char[] detachedChars = chars.copy();
        detachedChars[0] = '?';
        require(chars.charAt(0) == 'A', "UTF-16 copy is detached");
        StringBuilder mutable = new StringBuilder("owned");
        M3Utf16ArrayView characterSequenceSnapshot = M3Arrays.chars(mutable);
        mutable.setCharAt(0, '?');
        require("owned".contentEquals(characterSequenceSnapshot), "CharSequence factory snapshots mutable input");
        M3Utf16ArrayView charJoin = M3Arrays.join(M3Arrays.chars(""), chars.subSequence(0, 2),
                M3Arrays.chars(""), chars.subSequence(2, 5), M3Arrays.chars(""));
        String expectedString = new String(expectedChars);
        require(Arrays.equals(expectedChars, charJoin.copy()), "UTF-16 join preserves surrogate seam");
        require(charJoin.contentEquals(chars) && charJoin.compareTo(chars) == 0, "UTF-16 content and order");
        require(charJoin.javaHashCode() == expectedString.hashCode(), "UTF-16 String hash compatibility");
        require(charJoin.codePointCount() == 4 && Arrays.equals(new int[] {'A', 0x1f600, 'B', 0xd800},
                charJoin.codePoints().toArray()), "paired and unpaired code points across seams");
        require(charJoin.codePointAt(1) == 0x1f600 && charJoin.codePointBefore(3) == 0x1f600,
                "surrogate-aware code point access");
        require(charJoin.offsetByCodePoints(0, 2) == 3, "UTF-16 coordinates remain code-unit offsets");
        require(charJoin.indexOf("\ud83d\ude00", 0) == 1 && charJoin.lastIndexOf("B", 5) == 3,
                "UTF-16 search crosses seam");
        require(Arrays.equals(new char[] {'\ud83d', '\ude00', 'B'}, charJoin.subSequence(1, 4).copy()),
                "UTF-16 subsequence crosses seam");
        require(charJoin.segmentCount() == 2, "UTF-16 physical nonempty segment count");
        CharBuffer[] charBuffers = charJoin.asReadOnlyCharBuffers();
        require(charBuffers.length == 2 && charBuffers[0].isReadOnly() && charBuffers[1].isReadOnly(),
                "read-only UTF-16 descriptors");
        expect(ReadOnlyBufferException.class, () -> charBuffers[0].put(0, '?'), "UTF-16 write refused");
        charBuffers[0].position(charBuffers[0].limit());
        require(charJoin.asReadOnlyCharBuffers()[0].remaining() == 2, "UTF-16 descriptor positions are independent");
        char[] charTarget = {'?', '?', '?', '?'};
        charJoin.copyTo(1, charTarget, 1, 2);
        require(Arrays.equals(new char[] {'?', '\ud83d', '\ude00', '?'}, charTarget), "UTF-16 range copy");
        expect(IndexOutOfBoundsException.class, () -> charJoin.copyTo(4, charTarget, 0, 2),
                "UTF-16 range rejected before write");
        require(Arrays.equals(new char[] {'?', '\ud83d', '\ude00', '?'}, charTarget),
                "invalid UTF-16 copy leaves target unchanged");
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation, String message) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(message + ": wrong exception " + failure.getClass().getName(), failure);
        }
        throw new AssertionError(message + ": expected " + type.getName());
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
