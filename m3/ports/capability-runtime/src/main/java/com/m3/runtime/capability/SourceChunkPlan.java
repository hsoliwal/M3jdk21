// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Immutable byte-range chunk geometry over one canonical source span.
 *
 * <p>Prefer the last existing LF boundary within the caller's [min,max]
 * window. When none exists, cut at max; a final tail may be shorter than min.
 * The plan stores only monotonically increasing relative end offsets, never
 * source text. No UTF-8 decoding, CR removal, or native-address retention.
 * Java and optional JNI share this exact deterministic boundary policy; JNI
 * supplies only the already-proved {@link ByteLineIndex} LF coordinates.</p>
 */
public final class SourceChunkPlan {
    public static final int MAX_CHUNKS = 1_000_000;

    private final SourceArtifact.Span source;
    private final int minimumBytes;
    private final int maximumBytes;
    private final int[] ends;

    private SourceChunkPlan(SourceArtifact.Span source, int minimumBytes,
                            int maximumBytes, int[] ends) {
        this.source = source;
        this.minimumBytes = minimumBytes;
        this.maximumBytes = maximumBytes;
        this.ends = ends;
    }

    /** CPU-authoritative plan; source and all resulting spans share the same owner. */
    public static SourceChunkPlan javaPlan(SourceArtifact.Span source, int minimum, int maximum) {
        SourceArtifact.Span checked = require(source, minimum, maximum);
        return compile(checked, ByteLineIndex.javaIndex(checked), minimum, maximum);
    }

    /**
     * Explicit JNI LF precompute, not an implicit accelerator. The existing
     * SHA-256-pinned native library must have been installed by {@link LfCount}.
     * No new native method or ABI version is introduced.
     */
    public static SourceChunkPlan nativePlan(SourceArtifact.Span source, int minimum, int maximum) {
        SourceArtifact.Span checked = require(source, minimum, maximum);
        return compile(checked, ByteLineIndex.nativeIndex(checked), minimum, maximum);
    }

    private static SourceArtifact.Span require(SourceArtifact.Span source,
                                               int minimum, int maximum) {
        SourceArtifact.Span checked = Objects.requireNonNull(source, "source");
        if (minimum < 1 || maximum < minimum || maximum > SourceArtifact.MAX_BYTES) {
            throw new IllegalArgumentException("chunk range must be 1 <= min <= max <= 16 MiB");
        }
        checkpoint();
        return checked;
    }

    private static SourceChunkPlan compile(SourceArtifact.Span source,
                                           ByteLineIndex lines, int minimum, int maximum) {
        // Two geometric passes allocate exactly one retained int[] rather than
        // growing and copying an intermediate per-chunk buffer.
        int count = 0;
        for (int at = 0; at < source.length();) {
            if ((count & 4095) == 0) checkpoint();
            if (count == MAX_CHUNKS) throw new IllegalArgumentException("chunk-count budget");
            at = nextCut(lines, at, source.length(), minimum, maximum);
            count++;
        }
        int[] ends = new int[count];
        int at = 0;
        for (int i = 0; i < count; i++) {
            if ((i & 4095) == 0) checkpoint();
            at = nextCut(lines, at, source.length(), minimum, maximum);
            ends[i] = at;
        }
        if (at != source.length()) throw new IllegalStateException("source chunk coverage changed");
        checkpoint();
        return new SourceChunkPlan(source, minimum, maximum, ends);
    }

    private static int nextCut(ByteLineIndex lines, int from, int length,
                               int minimum, int maximum) {
        int ceiling = (int) Math.min((long) from + maximum, length);
        if (ceiling == length) return ceiling;
        // Existing LF coordinates are absolute within this SourceArtifact.Span.
        // lineAtOffset is a logarithmic predecessor query, even for the EOF cursor.
        int line = lines.lineAtOffset(ceiling);
        int aligned = lines.startOffset(line);
        return (long) aligned - from >= minimum ? aligned : ceiling;
    }

    public String sourceRoot() { return source.source().root(); }
    public String rangeRoot() { return source.sha256(); }
    public int sourceLength() { return source.length(); }
    public int minimumBytes() { return minimumBytes; }
    public int maximumBytes() { return maximumBytes; }
    public int chunkCount() { return ends.length; }
    public long retainedPrimitiveBytes() { return (long) ends.length * Integer.BYTES; }

    public int startOffset(int chunk) {
        Objects.checkIndex(chunk, ends.length);
        return chunk == 0 ? 0 : ends[chunk - 1];
    }

    public int endOffset(int chunk) { return ends[Objects.checkIndex(chunk, ends.length)]; }

    /** An allocated logical coordinate; materialize bytes only by explicitly calling Span.copy(). */
    public SourceArtifact.Span chunk(int ordinal) {
        int start = startOffset(ordinal);
        return source.slice(start, endOffset(ordinal) - start);
    }

    /** The source-relative EOF cursor maps to chunkCount(), not the last payload chunk. */
    public int chunkAtOffset(int offset) {
        Objects.checkFromIndexSize(offset, 0, source.length());
        int found = Arrays.binarySearch(ends, offset);
        return found >= 0 ? found + 1 : -found - 1;
    }

    private static void checkpoint() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("source-chunk planning interrupted");
        }
    }
}
