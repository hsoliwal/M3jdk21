// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BitapExactNativeParityMain.java (blob 6bb647e70ff8) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/** Explicit real-JNI executable oracle: pass the absolute path to the compiled C17 .so. */
public final class BitapExactNativeParityMain {
    private BitapExactNativeParityMain() {}

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("one native library path required");
        System.load(args[0]);
        Random random = new Random(0xB17A2026L);
        long assertions = 0;
        for (int caseNo = 0; caseNo < 35_000; caseNo++) {
            byte[] haystack = new byte[random.nextInt(160)];
            byte[] needle = new byte[1 + random.nextInt(63)];
            random.nextBytes(haystack);
            random.nextBytes(needle);
            if ((caseNo & 3) == 0) {
                Arrays.fill(haystack, (byte) 0xFF);
                Arrays.fill(needle, (byte) 0xFF);
            }
            if (caseNo % 7 == 0 && haystack.length >= needle.length) {
                System.arraycopy(haystack,
                        random.nextInt(haystack.length - needle.length + 1),
                        needle, 0, needle.length);
            }
            int from = switch (caseNo % 7) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> -1;
                case 3 -> 0;
                default -> random.nextInt(250);
            };
            BitapExactSearch compiled = new BitapExactSearch(needle);
            int expectedFirst = oracle(haystack, needle, from, false);
            int expectedLast = oracle(haystack, needle, from, true);
            int expectedCount = oracleCount(haystack, needle, from);
            check(expectedFirst, compiled.indexOf(haystack, from), caseNo, "java-first");
            check(expectedFirst, BitapExactSearch.indexOfNative(needle, haystack, from),
                    caseNo, "jni-first");
            check(expectedLast, compiled.lastIndexOf(haystack, from), caseNo, "java-last");
            check(expectedLast, BitapExactSearch.lastIndexOfNative(needle, haystack, from),
                    caseNo, "jni-last");
            check(expectedCount, compiled.countOverlapping(haystack, from), caseNo, "java-count");
            check(expectedCount, BitapExactSearch.countOverlappingNative(needle, haystack, from),
                    caseNo, "jni-count");
            assertions += 6;
            byte[] padded = new byte[haystack.length + 6];
            System.arraycopy(haystack, 0, padded, 3, haystack.length);
            ByteBuffer heap = ByteBuffer.wrap(padded);
            ByteBuffer direct = ByteBuffer.allocateDirect(padded.length);
            direct.put(padded).flip();
            for (ByteBuffer buffer : new ByteBuffer[] {heap, direct, heap.asReadOnlyBuffer(),
                    direct.asReadOnlyBuffer()}) {
                buffer.position(3);
                buffer.limit(3 + haystack.length);
                check(expectedFirst, compiled.indexOf(buffer, from), caseNo, "buffer-first");
                check(expectedLast, compiled.lastIndexOf(buffer, from), caseNo, "buffer-last");
                check(expectedCount, compiled.countOverlapping(buffer, from), caseNo, "buffer-count");
                check(3, buffer.position(), caseNo, "position-preserved");
                check(3 + haystack.length, buffer.limit(), caseNo, "limit-preserved");
                assertions += 5;
            }
        }
        check(-1, BitapExactSearch.indexOfNative(new byte[] {2}, new byte[0], 0), -1, "empty");
        check(-1, BitapExactSearch.lastIndexOfNative(new byte[] {2}, new byte[] {2}, -1), -1, "negative");
        assertions += 2;
        requireThrows(NullPointerException.class,
                () -> BitapExactSearch.indexOfNative(null, new byte[2], 0));
        requireThrows(NullPointerException.class,
                () -> BitapExactSearch.indexOfNative(new byte[] {1}, null, 0));
        requireThrows(IllegalArgumentException.class,
                () -> BitapExactSearch.indexOfNative(new byte[64], new byte[100], 0));
        requireThrows(IllegalArgumentException.class,
                () -> BitapExactSearch.lastIndexOfNative(new byte[0], new byte[100], 0));
        requireThrows(NullPointerException.class,
                () -> BitapExactSearch.countOverlappingNative(null, new byte[2], 0));
        requireThrows(IllegalArgumentException.class,
                () -> BitapExactSearch.countOverlappingNative(new byte[0], new byte[2], 0));
        assertions += 6;
        System.out.println("BITAP_JAVA_JNI_PARITY PASS assertions=" + assertions + " seed=B17A2026");
    }

    private static void check(int expected, int actual, int caseNo, String operation) {
        if (expected != actual) {
            throw new AssertionError(operation + " case=" + caseNo
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void requireThrows(Class<? extends Throwable> expected, Runnable operation) {
        try { operation.run(); }
        catch (Throwable thrown) {
            if (expected.isInstance(thrown)) return;
            throw new AssertionError("unexpected exception " + thrown, thrown);
        }
        throw new AssertionError("missing " + expected.getSimpleName());
    }

    /** Independent count oracle. Counts overlaps without calling another search strategy. */
    private static int oracleCount(byte[] haystack, byte[] needle, int from) {
        int lastStart = haystack.length - needle.length;
        int count = 0;
        for (int start = Math.max(0, from); start <= lastStart; start++) {
            int matched = 0;
            while (matched < needle.length && haystack[start + matched] == needle[matched]) {
                matched++;
            }
            if (matched == needle.length) count++;
        }
        return count;
    }

    /** Deliberately quadratic and independent of the Shift-And state equations. */
    private static int oracle(byte[] haystack, byte[] needle, int from, boolean reverse) {
        int maximum = haystack.length - needle.length;
        if (maximum < 0 || (reverse && from < 0)) return -1;
        int start = reverse ? 0 : Math.max(0, from);
        int end = reverse ? Math.min(from, maximum) : maximum;
        int result = -1;
        for (int offset = start; offset <= end; offset++) {
            boolean equal = true;
            for (int i = 0; i < needle.length; i++) {
                if (haystack[offset + i] != needle[i]) { equal = false; break; }
            }
            if (equal) {
                result = offset;
                if (!reverse) break;
            }
        }
        return result;
    }
}
