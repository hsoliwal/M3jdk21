// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Compiled exact byte-pattern search over immutable source coordinates.
 *
 * <p>The byte-oriented plan is not a text decoder or a second String owner:
 * it snapshots one nonempty needle and retains only its KMP prefix table.
 * All hits are offsets into the supplied source span, including overlapping
 * hits and matches crossing LF/CRLF or UTF-8 boundaries. Limits are explicit.
 * Java is the reference; optional JNI is an independently checked execution
 * leaf of the existing SHA-256-pinned LF native library.</p>
 */
public final class BytePatternSearch {
    public static final int MAX_PATTERN_BYTES = 4_096;
    public static final int MAX_MATCHES = 1_000_000;

    private final byte[] needle;
    private final int[] failure;

    private BytePatternSearch(byte[] owned) {
        needle = owned;
        failure = new int[needle.length];
        for (int i = 1, matched = 0; i < needle.length; i++) {
            checkpoint(i);
            while (matched > 0 && needle[i] != needle[matched]) {
                matched = failure[matched - 1];
            }
            if (needle[i] == needle[matched]) matched++;
            failure[i] = matched;
        }
    }

    /** Snapshot a bounded nonempty needle. Caller mutation cannot change a compiled plan. */
    public static BytePatternSearch compile(byte[] needle) {
        byte[] input = Objects.requireNonNull(needle, "needle");
        if (input.length == 0 || input.length > MAX_PATTERN_BYTES) {
            throw new IllegalArgumentException("byte-pattern length budget 1..4096");
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-pattern preparation interrupted");
        }
        return new BytePatternSearch(input.clone());
    }

    public int patternLength() { return needle.length; }

    /** Exact retained array bytes; excludes Java array/object headers. */
    public long retainedPrimitiveBytes() {
        return (long) needle.length * (Byte.BYTES + Integer.BYTES);
    }

    /**
     * Deterministic overlapping hits, relative to the selected immutable span.
     * Two O(n+m) scans allocate only an exact-sized result int[]; a result
     * beyond maxMatches fails, rather than silently truncating evidence.
     */
    public int[] findAll(SourceArtifact.Span span, int maxMatches) {
        SourceArtifact.Span input = Objects.requireNonNull(span, "span");
        requireLimit(maxMatches);
        if (input.length() < needle.length) return new int[0];
        int count = scan(input, maxMatches, null);
        int[] hits = new int[count];
        if (scan(input, maxMatches, hits) != count) {
            throw new IllegalStateException("immutable source scan changed");
        }
        return hits;
    }

    /**
     * Explicit JNI candidate using the existing pinned LfCount native library.
     * The source range is copied into one temporary direct ByteBuffer; no native
     * pointer survives the call. It is not an automatic acceleration decision.
     * A legacy JNI image without this optional symbol refuses, never falls back
     * silently while claiming native parity.
     */
    public int[] findAllNative(SourceArtifact.Span span, int maxMatches) {
        SourceArtifact.Span input = Objects.requireNonNull(span, "span");
        requireLimit(maxMatches);
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-pattern scan interrupted");
        }
        byte[] source = input.copy();
        ByteBuffer direct = ByteBuffer.allocateDirect(source.length);
        direct.put(source).flip();
        // The KMP symbol alone is not native admission: another library in the
        // class loader could export it without the source-pinned LF owner.
        // Reuse the installed and independently hashed v1 ABI before dispatch.
        if (LfCount.nativeCount(direct, 0, 0) != 0L) {
            throw new IllegalStateException("pinned LF JNI preflight returned nonzero");
        }
        try {
            int[] result = findKmp0(direct, 0, source.length, needle, maxMatches);
            if (result == null || result.length > maxMatches) {
                throw new IllegalStateException("invalid native byte-search result");
            }
            int last = -1;
            for (int hit : result) {
                if (hit <= last || hit < 0 || hit > source.length - needle.length) {
                    throw new IllegalStateException("invalid native byte-search offsets");
                }
                last = hit;
            }
            return result;
        } catch (UnsatisfiedLinkError absent) {
            throw new IllegalStateException("pinned native library lacks byte-search extension", absent);
        }
    }

    private int scan(SourceArtifact.Span input, int maxMatches, int[] hits) {
        int matched = 0;
        int found = 0;
        for (int i = 0; i < input.length(); i++) {
            checkpoint(i);
            byte current = input.byteAt(i);
            while (matched > 0 && current != needle[matched]) {
                matched = failure[matched - 1];
            }
            if (current == needle[matched]) matched++;
            if (matched == needle.length) {
                if (found == maxMatches) {
                    throw new IllegalArgumentException("byte-search result budget exceeded");
                }
                if (hits != null) hits[found] = i + 1 - needle.length;
                found++;
                matched = failure[matched - 1];
            }
        }
        checkpoint(input.length());
        return found;
    }

    private static void requireLimit(int count) {
        if (count < 0 || count > MAX_MATCHES) {
            throw new IllegalArgumentException("byte-search match budget 0..1000000");
        }
    }

    private static void checkpoint(int position) {
        if ((position & 4095) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-pattern scan interrupted");
        }
    }

    private static native int[] findKmp0(
            ByteBuffer buffer, int start, int length, byte[] needle, int maxMatches);
}
