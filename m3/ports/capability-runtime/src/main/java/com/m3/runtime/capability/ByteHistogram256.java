// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Bounded unsigned-byte frequency and run algorithms over existing source ownership.
 * No separate text interner, per-element object graph or source-byte payload is retained.
 * Input bytes remain bytes until the existing M3String decoding boundary.
 */
public final class ByteHistogram256 {
    public static final int BUCKETS = 256;
    public static final int MAX_RUNS = 1_000_000;

    private ByteHistogram256() { }

    /** Callback receives an unsigned byte (0..255) and source-relative [from,to). */
    @FunctionalInterface
    public interface RunSink {
        void accept(int unsignedByte, int fromInclusive, int toExclusive);
    }

    /** One linear pass over the immutable borrowed source range and 2 KiB of counters. */
    public static long[] javaCount(SourceArtifact.Span span) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "span");
        long[] counts = new long[BUCKETS];
        for (int i = 0; i < source.length(); i++) {
            checkpoint(i);
            counts[Byte.toUnsignedInt(source.byteAt(i))]++;
        }
        return counts;
    }

    /** Absolute ByteBuffer positions, bounded by limit, never mutating position or limit. */
    public static long[] javaCount(ByteBuffer buffer, int start, int length) {
        checkRange(buffer, start, length);
        long[] counts = new long[BUCKETS];
        for (int i = 0; i < length; i++) {
            checkpoint(i);
            counts[Byte.toUnsignedInt(buffer.get(start + i))]++;
        }
        return counts;
    }

    /**
     * Explicit optional JNI leaf. The caller must first install the SHA-256/ABI-checked
     * existing LfCount native library. A legacy image without this new symbol fails closed.
     */
    public static long[] nativeCount(ByteBuffer buffer, int start, int length) {
        checkRange(buffer, start, length);
        if (!buffer.isDirect()) throw new IllegalArgumentException("direct ByteBuffer required");
        checkpoint(0);
        final long[] counts;
        try {
            counts = histogram0(buffer, start, length);
        } catch (UnsatisfiedLinkError unavailable) {
            throw new IllegalStateException("SHA-pinned JNI histogram not installed", unavailable);
        }
        if (counts == null || counts.length != BUCKETS) {
            throw new IllegalStateException("native histogram result contract");
        }
        long sum = 0;
        for (long count : counts) {
            if (count < 0) throw new IllegalStateException("negative native count");
            sum = Math.addExact(sum, count);
        }
        if (sum != length) throw new IllegalStateException("native histogram count mismatch");
        return counts;
    }

    /** Native boundary explicitly materializes one temporary direct byte shadow. */
    public static long[] nativeCount(SourceArtifact.Span span) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "span");
        byte[] bytes = source.copy();
        ByteBuffer shadow = ByteBuffer.allocateDirect(bytes.length);
        shadow.put(bytes).flip();
        return nativeCount(shadow, 0, source.length());
    }

    /**
     * Exact Shannon entropy in bits per byte. Zero-length input has entropy zero.
     * The frequency histogram is borrowed, never retained or mutated.
     */
    public static double entropy(long[] frequencies) {
        Objects.requireNonNull(frequencies, "frequencies");
        if (frequencies.length != BUCKETS) throw new IllegalArgumentException("256 bins required");
        long total = 0;
        for (long count : frequencies) {
            if (count < 0) throw new IllegalArgumentException("negative histogram count");
            total = Math.addExact(total, count);
        }
        if (total == 0) return 0.0;
        double inverse = 1.0 / total;
        double entropy = 0.0;
        for (long count : frequencies) {
            if (count == 0) continue;
            double probability = count * inverse;
            entropy -= probability * (Math.log(probability) / Math.log(2.0));
        }
        return entropy;
    }

    /**
     * Two-pass run scan: rejects an insufficient maximum before ANY callback.
     * Output uses primitive source-relative coordinates and has O(1) retained space.
     * These are byte runs, not Unicode code point or grapheme runs.
     */
    public static int scanRuns(SourceArtifact.Span span, int maxRuns, RunSink sink) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "span");
        Objects.requireNonNull(sink, "sink");
        if (maxRuns < 0 || maxRuns > MAX_RUNS) {
            throw new IllegalArgumentException("invalid run output budget");
        }
        int length = source.length();
        int total = 0;
        for (int i = 0; i < length; i++) {
            checkpoint(i);
            if (i == 0 || source.byteAt(i) != source.byteAt(i - 1)) {
                if (total == maxRuns) throw new IllegalArgumentException("run budget exceeded");
                total++;
            }
        }
        int from = 0;
        while (from < length) {
            checkpoint(from);
            int unsignedByte = Byte.toUnsignedInt(source.byteAt(from));
            int to = from + 1;
            while (to < length && Byte.toUnsignedInt(source.byteAt(to)) == unsignedByte) {
                checkpoint(to);
                to++;
            }
            sink.accept(unsignedByte, from, to);
            from = to;
        }
        return total;
    }

    private static void checkRange(ByteBuffer buffer, int start, int length) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.checkFromIndexSize(start, length, buffer.limit());
        if (length > SourceArtifact.MAX_BYTES) throw new IllegalArgumentException("source byte budget");
    }

    private static void checkpoint(int position) {
        if ((position & 4095) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("histogram/run scan interrupted");
        }
    }

    private static native long[] histogram0(ByteBuffer buffer, int start, int length);
}
