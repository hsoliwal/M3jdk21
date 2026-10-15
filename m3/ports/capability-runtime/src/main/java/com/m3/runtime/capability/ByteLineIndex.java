// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Immutable source-byte line coordinates for evidence and retrieval. A line owns no
 * text: each view is a range over its original immutable {@link SourceArtifact}.
 * UTF-8, UTF-16 and CRLF are deliberately not guessed or normalized.
 *
 * <p>The final empty line exists after a terminating LF, and an empty source
 * has exactly one empty line. Newline characters belong to the preceding line;
 * {@link #content(int)} strips LF only (not CR). All offsets are byte offsets
 * relative to the exact selected source range.</p>
 */
public final class ByteLineIndex {
    /** Refuse unbounded index retention even when the original bytes fit. */
    public static final int MAX_LINE_BREAKS = 1_000_000;

    private final SourceArtifact.Span source;
    private final int[] starts;

    private ByteLineIndex(SourceArtifact.Span source, int[] starts) {
        this.source = Objects.requireNonNull(source, "source");
        this.starts = starts;
        if (starts.length < 1 || starts.length > MAX_LINE_BREAKS + 1
                || starts[0] != 0) {
            throw new IllegalArgumentException("invalid byte-line index geometry");
        }
        for (int i = 1; i < starts.length; i++) {
            int next = starts[i];
            if (next <= starts[i - 1] || next > source.length()
                    || source.byteAt(next - 1) != (byte) '\n') {
                throw new IllegalArgumentException("invalid LF line boundary");
            }
        }
    }

    /** Strict CPU reference: two bounded passes, only one final int[] allocation. */
    public static ByteLineIndex javaIndex(SourceArtifact.Span span) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "span");
        int breaks = 0;
        for (int i = 0; i < source.length(); i++) {
            checkpoint(i);
            if (source.byteAt(i) == (byte) '\n') {
                if (breaks == MAX_LINE_BREAKS) {
                    throw new IllegalArgumentException("byte-line index retained-memory budget");
                }
                breaks++;
            }
        }
        int[] starts = new int[breaks + 1];
        int cursor = 1;
        for (int i = 0; i < source.length(); i++) {
            checkpoint(i);
            if (source.byteAt(i) == (byte) '\n') starts[cursor++] = i + 1;
        }
        return new ByteLineIndex(source, starts);
    }

    /**
     * Explicit JNI candidate; copies a source range to a temporary direct buffer
     * at the native boundary. Never caches native pointers or owns source bytes.
     * Requires the SHA-256-pinned LfCount library already installed. Not an
     * automatic accelerator selection or a claim of measured performance gain.
     */
    public static ByteLineIndex nativeIndex(SourceArtifact.Span span) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "span");
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-line scan interrupted");
        }
        byte[] temporary = source.copy();
        ByteBuffer input = ByteBuffer.allocateDirect(temporary.length);
        input.put(temporary).flip();
        int[] ends = LfCount.nativeLineEnds(input, 0, source.length(), MAX_LINE_BREAKS);
        int[] starts = new int[Math.addExact(ends.length, 1)];
        System.arraycopy(ends, 0, starts, 1, ends.length);
        return new ByteLineIndex(source, starts);
    }

    public String sourceRoot() { return source.source().root(); }
    public String rangeRoot() { return source.sha256(); }
    public int sourceLength() { return source.length(); }
    public int lineCount() { return starts.length; }
    public long retainedPrimitiveBytes() { return (long) starts.length * Integer.BYTES; }

    public int startOffset(int line) { return starts[Objects.checkIndex(line, starts.length)]; }

    public int endOffset(int line) {
        Objects.checkIndex(line, starts.length);
        return line + 1 == starts.length ? source.length() : starts[line + 1];
    }

    /** Lossless line, including trailing LF when present. */
    public SourceArtifact.Span line(int ordinal) {
        int start = startOffset(ordinal);
        return source.slice(start, endOffset(ordinal) - start);
    }

    /** Exclude one LF terminator only; CR is source evidence, not decoration. */
    public SourceArtifact.Span content(int ordinal) {
        int start = startOffset(ordinal);
        int end = endOffset(ordinal);
        if (end > start && source.byteAt(end - 1) == (byte) '\n') end--;
        return source.slice(start, end - start);
    }

    /**
     * Locate a byte offset, including the EOF cursor (offset == sourceLength).
     * The right-hand empty line after a terminal LF is therefore addressable.
     */
    public int lineAtOffset(int offset) {
        Objects.checkFromIndexSize(offset, 0, source.length());
        int exact = Arrays.binarySearch(starts, offset);
        return exact >= 0 ? exact : -exact - 2;
    }

    private static void checkpoint(int index) {
        if ((index & 4095) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-line scan interrupted");
        }
    }
}
