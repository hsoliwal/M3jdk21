// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/M3BatchOffsetSearch.java (blob 4badcc3a401d) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Exact multiple-start byte search, reusing one KMP prefix preparation and one haystack scan.
 *
 * <p>Each output is the first occurrence of {@code needle} at or after the corresponding
 * {@code fromIndices[i]}; negative starts are clamped to zero, missing matches return -1,
 * duplicates and input order are preserved. Inputs must not be concurrently mutated during
 * execution. No array or native pointer is retained after the method returns.</p>
 *
 * <p>The Java implementation costs O(n + m + q log q) time and O(m + q) scratch space
 * for n haystack bytes, m needle bytes and q offsets. JNI is an optional accelerator;
 * ordinary Java behavior and all existing {@code M3CompiledSearch} contracts are unchanged.</p>
 */
public final class M3BatchOffsetSearch {
    /** Explicit backend policy. NATIVE refuses a missing provider; AUTO may fall back. */
    public enum Backend { JAVA, NATIVE, AUTO }

    private M3BatchOffsetSearch() { }

    /** Java default; no native load or JNI crossing. */
    public static int[] indexOfMany(byte[] haystack, byte[] needle, int[] fromIndices) {
        return indexOfMany(haystack, needle, fromIndices, Backend.JAVA);
    }

    /**
     * Batch first-occurrence search with immutable source semantics and caller-order results.
     *
     * @throws NullPointerException for any null input or backend
     * @throws IllegalArgumentException for an empty needle
     * @throws IllegalStateException when NATIVE is explicitly selected but unavailable
     */
    public static int[] indexOfMany(byte[] haystack, byte[] needle, int[] fromIndices,
            Backend backend) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(fromIndices, "fromIndices");
        Objects.requireNonNull(backend, "backend");
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        if (backend != Backend.JAVA) {
            if (new M3JniSearchEngine().available()) {
                try {
                    return indexOfManyNative(haystack, needle, fromIndices);
                } catch (UnsatisfiedLinkError oldNativeLibrary) {
                    if (backend == Backend.NATIVE) throw oldNativeLibrary;
                    // AUTO only: an older ABI-1 library might lack this additive symbol.
                }
            } else if (backend == Backend.NATIVE) {
                throw new IllegalStateException("JNI search provider is unavailable");
            }
        }
        return indexOfManyJava(haystack, needle, fromIndices);
    }

    /** ByteBuffer overload scans the remaining window without copying or changing cursor/mark. */
    public static int[] indexOfMany(ByteBuffer haystack, byte[] needle, int[] fromIndices) {
        return indexOfMany(haystack, needle, fromIndices, Backend.JAVA);
    }

    /**
     * Offsets are relative to {@code haystack.position()}, never its absolute array index.
     * NATIVE requires a direct buffer; AUTO uses Java for heap buffers and older JNI builds.
     * The caller's position, limit and mark are never modified.
     */
    public static int[] indexOfMany(ByteBuffer haystack, byte[] needle, int[] fromIndices,
            Backend backend) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        Objects.requireNonNull(fromIndices, "fromIndices");
        Objects.requireNonNull(backend, "backend");
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        if (backend != Backend.JAVA && haystack.isDirect()) {
            if (new M3JniSearchEngine().available()) {
                try {
                    int[] result = indexOfManyDirectNative(haystack,
                            haystack.position(), haystack.limit(), needle, fromIndices);
                    if (result != null) return result;
                    if (backend == Backend.NATIVE) {
                        throw new IllegalStateException("JNI direct buffer address unavailable");
                    }
                } catch (UnsatisfiedLinkError oldNativeLibrary) {
                    if (backend == Backend.NATIVE) throw oldNativeLibrary;
                    // AUTO is compatible with native libraries predating this symbol.
                }
            } else if (backend == Backend.NATIVE) {
                throw new IllegalStateException("JNI search provider is unavailable");
            }
        } else if (backend == Backend.NATIVE) {
            throw new IllegalArgumentException("NATIVE requires a direct ByteBuffer");
        }
        return indexOfManyBufferJava(haystack, needle, fromIndices);
    }

    private static int[] indexOfManyJava(byte[] haystack, byte[] needle, int[] fromIndices) {
        int count = fromIndices.length;
        int[] result = new int[count];
        Arrays.fill(result, -1);
        if (count == 0 || needle.length > haystack.length) return result;

        // Sort primitive (normalized start, caller ordinal) pairs. No per-query wrappers.
        long[] requests = new long[count];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int start = Math.max(0, fromIndices[ordinal]);
            requests[ordinal] = ((long) start << 32) | (ordinal & 0xffff_ffffL);
        }
        Arrays.sort(requests);
        int[] prefix = prefix(needle);
        int matched = 0;
        int nextRequest = 0;
        for (int index = 0; index < haystack.length && nextRequest < count; index++) {
            byte value = haystack[index];
            while (matched > 0 && value != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) {
                int matchStart = index + 1 - needle.length;
                while (nextRequest < count
                        && (int) (requests[nextRequest] >>> 32) <= matchStart) {
                    result[(int) requests[nextRequest]] = matchStart;
                    nextRequest++;
                }
                matched = prefix[matched - 1];
            }
        }
        return result;
    }

    private static int[] indexOfManyBufferJava(ByteBuffer haystack, byte[] needle,
            int[] fromIndices) {
        int count = fromIndices.length;
        int[] result = new int[count];
        Arrays.fill(result, -1);
        int base = haystack.position();
        int length = haystack.remaining();
        if (count == 0 || needle.length > length) return result;

        long[] requests = new long[count];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int start = Math.max(0, fromIndices[ordinal]);
            requests[ordinal] = ((long) start << 32) | (ordinal & 0xffff_ffffL);
        }
        Arrays.sort(requests);
        int[] prefix = prefix(needle);
        int matched = 0;
        int nextRequest = 0;
        for (int index = 0; index < length && nextRequest < count; index++) {
            byte value = haystack.get(base + index);
            while (matched > 0 && value != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) {
                int matchStart = index + 1 - needle.length;
                while (nextRequest < count
                        && (int) (requests[nextRequest] >>> 32) <= matchStart) {
                    result[(int) requests[nextRequest]] = matchStart;
                    nextRequest++;
                }
                matched = prefix[matched - 1];
            }
        }
        return result;
    }

    private static int[] prefix(byte[] pattern) {
        int[] table = new int[pattern.length];
        for (int index = 1, matched = 0; index < pattern.length; index++) {
            while (matched > 0 && pattern[index] != pattern[matched]) {
                matched = table[matched - 1];
            }
            if (pattern[index] == pattern[matched]) matched++;
            table[index] = matched;
        }
        return table;
    }

    /** Optional ABI-1 addition to the existing m3search JNI library. */
    private static native int[] indexOfManyNative(byte[] haystack, byte[] needle,
            int[] fromIndices);

    /** Optional zero-copy direct-buffer ABI-1 extension; null signals unsupported storage. */
    private static native int[] indexOfManyDirectNative(ByteBuffer haystack, int position,
            int limit, byte[] needle, int[] fromIndices);
}
