// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Immutable, source-backed CPU statistics over exact bytes. This is not a Unicode
 * tokenizer, a new string interner or a native-text owner: decoded text belongs
 * to the separately qualified M3String bridge. A source range is read directly
 * and only fixed-size primitive counters and bit masks are retained.
 */
public final class ByteStatistics {
    public static final int ALPHABET = 256;
    public static final int MAX_RUNS = 1_000_000;
    private static final double LOG_TWO = StrictMath.log(2.0);

    /** Zero-allocation callback over an unsigned byte and source-relative run range. */
    @FunctionalInterface
    public interface RunSink {
        void accept(int unsignedByte, int startInclusive, int endExclusive);
    }

    private final SourceArtifact.Span source;
    private final long[] histogram;
    private final long[] presence;
    private final int distinct;
    private final int mode;
    private final double entropy;

    private ByteStatistics(SourceArtifact.Span source, long[] histogram, long[] presence,
                           int distinct, int mode, double entropy) {
        this.source = source;
        this.histogram = histogram;
        this.presence = presence;
        this.distinct = distinct;
        this.mode = mode;
        this.entropy = entropy;
    }

    /**
     * One bounded source pass; the only retained arrays are 256 long counters
     * and four 64-bit presence-mask words. Ties for the mode choose the smaller
     * unsigned byte. No input payload or source-to-String copy is created.
     */
    public static ByteStatistics scan(SourceArtifact.Span span) {
        SourceArtifact.Span checked = Objects.requireNonNull(span, "source span");
        long[] counts = new long[ALPHABET];
        long[] masks = new long[4];
        for (int offset = 0; offset < checked.length(); offset++) {
            checkpoint(offset);
            int value = Byte.toUnsignedInt(checked.byteAt(offset));
            counts[value]++;
            masks[value >>> 6] |= 1L << (value & 63);
        }
        int distinct = 0;
        int mode = -1;
        long largest = 0;
        double information = 0.0;
        for (int value = 0; value < ALPHABET; value++) {
            long count = counts[value];
            if (count == 0) continue;
            distinct++;
            if (count > largest) {
                largest = count;
                mode = value;
            }
            double probability = (double) count / checked.length();
            information -= probability * (StrictMath.log(probability) / LOG_TWO);
        }
        checkpoint(checked.length());
        return new ByteStatistics(checked, counts, masks, distinct, mode, information);
    }

    public String sourceRoot() { return source.source().root(); }
    public String rangeRoot() { return source.sha256(); }
    public int byteLength() { return source.length(); }
    public int distinctByteCount() { return distinct; }
    public int modalUnsignedByte() { return mode; }
    public double shannonEntropyBitsPerByte() { return entropy; }
    public long retainedPrimitiveBytes() {
        return ((long) ALPHABET + presence.length) * Long.BYTES;
    }

    public long count(int unsignedByte) {
        return histogram[Objects.checkIndex(unsignedByte, ALPHABET)];
    }

    /** Four indexed 64-bit occupancy words, without allocating a bitmap object. */
    public long presenceMaskWord(int word) {
        return presence[Objects.checkIndex(word, presence.length)];
    }

    /** Explicit compatibility materialization: caller mutations never affect this owner. */
    public long[] histogramCopy() { return histogram.clone(); }

    /** Non-ASCII bytes are not themselves evidence of valid UTF-8. */
    public long nonAsciiBytes() {
        long ascii = 0;
        for (int i = 0; i < 128; i++) ascii += histogram[i];
        return byteLength() - ascii;
    }

    /**
     * Exact symmetric Jensen-Shannon divergence between two *nonempty* byte
     * frequency distributions, expressed in bits (0 to 1). This is a histogram
     * comparison, not lexical/semantic similarity. Empty distributions refuse.
     */
    public double jensenShannonBits(ByteStatistics other) {
        ByteStatistics checked = Objects.requireNonNull(other, "other");
        if (byteLength() == 0 || checked.byteLength() == 0) {
            throw new IllegalArgumentException("Jensen-Shannon requires nonempty sources");
        }
        double value = 0;
        for (int symbol = 0; symbol < ALPHABET; symbol++) {
            double p = (double) histogram[symbol] / byteLength();
            double q = (double) checked.histogram[symbol] / checked.byteLength();
            double mid = (p + q) * 0.5;
            if (p > 0) value += 0.5 * p * StrictMath.log(p / mid) / LOG_TWO;
            if (q > 0) value += 0.5 * q * StrictMath.log(q / mid) / LOG_TWO;
        }
        // Floating-point rounding at the endpoints is not information.
        return Math.min(1.0, Math.max(0.0, value));
    }

    /**
     * Bounded run-length traversal of exact source bytes. A negative or
     * excessive output budget refuses before callbacks. Reaching maxRuns
     * returns the first maxRuns runs; it never claims the source was exhausted.
     * Coordinates are relative to the input span, not decoded UTF-16 positions.
     */
    public static int scanRuns(SourceArtifact.Span span, int maxRuns, RunSink sink) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "source span");
        Objects.requireNonNull(sink, "sink");
        if (maxRuns < 0 || maxRuns > MAX_RUNS) {
            throw new IllegalArgumentException("byte-run result budget");
        }
        checkpoint(0);
        int emitted = 0;
        for (int from = 0; from < source.length() && emitted < maxRuns;) {
            int value = Byte.toUnsignedInt(source.byteAt(from));
            int end = from + 1;
            while (end < source.length()) {
                checkpoint(end);
                if (Byte.toUnsignedInt(source.byteAt(end)) != value) break;
                end++;
            }
            sink.accept(value, from, end);
            emitted++;
            from = end;
        }
        return emitted;
    }

    private static void checkpoint(int offset) {
        if ((offset & 4095) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("byte-statistics scan interrupted");
        }
    }
}
