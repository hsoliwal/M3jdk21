// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/com/synexia/m3/search/M3BatchReverseSearch.java (blob c1498ece536a) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Batched last exact-byte occurrence, preserving caller query order and immutable inputs.
 *
 * <p>For each bound the result is the greatest match start at or before that bound,
 * or -1. ByteBuffer offsets are relative to the original position and never change
 * its position, limit, or mark. A single reverse KMP scan answers all query bounds.
 * Costs: O(n + m + q log q) time and O(m + q) scratch.</p>
 */
public final class M3BatchReverseSearch {
    private M3BatchReverseSearch() { }

    /** Java-only default, with no native load. */
    public static int[] lastIndexOfMany(byte[] haystack, byte[] needle, int[] fromIndices) {
        return lastIndexOfMany(haystack, needle, fromIndices, M3BatchOffsetSearch.Backend.JAVA);
    }

    /** Explicit backend policy matching the existing batch forward-search owner. */
    public static int[] lastIndexOfMany(byte[] haystack, byte[] needle, int[] fromIndices,
            M3BatchOffsetSearch.Backend backend) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(fromIndices, "fromIndices");
        Objects.requireNonNull(backend, "backend");
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        if (backend != M3BatchOffsetSearch.Backend.JAVA) {
            if (new M3JniSearchEngine().available()) {
                try {
                    return lastIndexOfManyNative(haystack, needle, fromIndices);
                } catch (UnsatisfiedLinkError missingOptionalSymbol) {
                    if (backend == M3BatchOffsetSearch.Backend.NATIVE) throw missingOptionalSymbol;
                }
            } else if (backend == M3BatchOffsetSearch.Backend.NATIVE) {
                throw new IllegalStateException("JNI search provider is unavailable");
            }
        }
        return lastArrayJava(haystack, needle, fromIndices);
    }

    /** Remaining-range, window-relative ByteBuffer search without a payload copy. */
    public static int[] lastIndexOfMany(ByteBuffer haystack, byte[] needle, int[] fromIndices) {
        return lastIndexOfMany(haystack, needle, fromIndices, M3BatchOffsetSearch.Backend.JAVA);
    }

    /** Direct JNI is optional; AUTO falls back to Java without mutating the buffer cursor. */
    public static int[] lastIndexOfMany(ByteBuffer haystack, byte[] needle, int[] fromIndices,
            M3BatchOffsetSearch.Backend backend) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(fromIndices, "fromIndices");
        Objects.requireNonNull(backend, "backend");
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        if (backend != M3BatchOffsetSearch.Backend.JAVA && haystack.isDirect()) {
            if (new M3JniSearchEngine().available()) {
                try {
                    int[] answer = lastIndexOfManyDirectNative(haystack, haystack.position(),
                            haystack.limit(), needle, fromIndices);
                    if (answer != null) return answer;
                    if (backend == M3BatchOffsetSearch.Backend.NATIVE) {
                        throw new IllegalStateException("JNI direct buffer unavailable");
                    }
                } catch (UnsatisfiedLinkError missingOptionalSymbol) {
                    if (backend == M3BatchOffsetSearch.Backend.NATIVE) throw missingOptionalSymbol;
                }
            } else if (backend == M3BatchOffsetSearch.Backend.NATIVE) {
                throw new IllegalStateException("JNI search provider is unavailable");
            }
        } else if (backend == M3BatchOffsetSearch.Backend.NATIVE) {
            throw new IllegalArgumentException("NATIVE requires a direct ByteBuffer");
        }
        return lastBufferJava(haystack, needle, fromIndices);
    }

    private static int[] lastArrayJava(byte[] haystack, byte[] needle, int[] fromIndices) {
        int[] answers = new int[fromIndices.length];
        Arrays.fill(answers, -1);
        if (answers.length == 0 || needle.length > haystack.length) return answers;
        long[] queries = sortedBounds(fromIndices, haystack.length - needle.length);
        int next = queries.length - 1;
        if ((int) (queries[next] >> 32) < 0) return answers;
        int[] prefix = reversePrefix(needle);
        int matched = 0;
        for (int index = haystack.length - 1; index >= 0 && next >= 0
                && (int) (queries[next] >> 32) >= 0; index--) {
            byte value = haystack[index];
            while (matched > 0 && value != needle[needle.length - 1 - matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[needle.length - 1 - matched]) matched++;
            if (matched == needle.length) {
                while (next >= 0 && (int) (queries[next] >> 32) >= index) {
                    answers[(int) queries[next]] = index;
                    next--;
                }
                matched = prefix[matched - 1];
            }
        }
        return answers;
    }

    private static int[] lastBufferJava(ByteBuffer haystack, byte[] needle, int[] fromIndices) {
        int[] answers = new int[fromIndices.length];
        Arrays.fill(answers, -1);
        int base = haystack.position();
        int length = haystack.remaining();
        if (answers.length == 0 || needle.length > length) return answers;
        long[] queries = sortedBounds(fromIndices, length - needle.length);
        int next = queries.length - 1;
        if ((int) (queries[next] >> 32) < 0) return answers;
        int[] prefix = reversePrefix(needle);
        int matched = 0;
        for (int index = length - 1; index >= 0 && next >= 0
                && (int) (queries[next] >> 32) >= 0; index--) {
            byte value = haystack.get(base + index);
            while (matched > 0 && value != needle[needle.length - 1 - matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[needle.length - 1 - matched]) matched++;
            if (matched == needle.length) {
                while (next >= 0 && (int) (queries[next] >> 32) >= index) {
                    answers[(int) queries[next]] = index;
                    next--;
                }
                matched = prefix[matched - 1];
            }
        }
        return answers;
    }

    private static long[] sortedBounds(int[] fromIndices, int highestValidStart) {
        long[] requests = new long[fromIndices.length];
        boolean alreadyOrdered = true;
        long previous = Long.MIN_VALUE;
        for (int ordinal = 0; ordinal < requests.length; ordinal++) {
            int bound = Math.min(fromIndices[ordinal], highestValidStart);
            long request = ((long) bound << 32) | (ordinal & 0xffff_ffffL);
            requests[ordinal] = request;
            if (request < previous) alreadyOrdered = false;
            previous = request;
        }
        // Reuse a preordered primitive lane; equal bounds retain original ordinal order.
        if (!alreadyOrdered) Arrays.sort(requests);
        return requests;
    }

    /** A reversed needle is read via indices without allocating a second payload array. */
    private static int[] reversePrefix(byte[] needle) {
        int[] table = new int[needle.length];
        for (int index = 1, matched = 0; index < needle.length; index++) {
            byte value = needle[needle.length - 1 - index];
            while (matched > 0 && value != needle[needle.length - 1 - matched]) {
                matched = table[matched - 1];
            }
            if (value == needle[needle.length - 1 - matched]) matched++;
            table[index] = matched;
        }
        return table;
    }

    /** Opt-in JNI ABI extension; the preceding native exports are unchanged. */
    private static native int[] lastIndexOfManyNative(byte[] haystack, byte[] needle,
            int[] fromIndices);

    /** Direct-buffer JNI ABI extension; null with no exception means unsupported storage. */
    private static native int[] lastIndexOfManyDirectNative(ByteBuffer haystack, int position,
            int limit, byte[] needle, int[] fromIndices);
}
