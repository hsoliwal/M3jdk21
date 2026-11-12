// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

/** Read-only search using native or bounded Java streaming kernels. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSearch.java blob a8e2b66123f7; recipe m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class PmemSearch {
    /** Bounds peak search scratch space to a small fraction of a large model. */
    public static final int MAX_PATTERN_BYTES = 1 << 20;

    private PmemSearch() {}

    /**
     * Finds the first exact byte sequence within {@code [start, start+count)}.
     * Returns an offset relative to {@code region}, or -1 if absent.
     * An empty pattern matches at {@code start}, including the end of a region.
     * The entire scan borrows one region read lease, blocking close.
     * This does not protect against external processes modifying/truncating the
     * backing file; the file must remain immutable during execution.
     * Complexity: O(count + pattern.length) time, O(pattern.length) prefix scratch plus a 64 KiB FILE scan window.
     */
    public static long indexOf(PmemRegion region, long start, long count, byte[] pattern) {
        return PmemMapping.indexOf(region, start, count, pattern);
    }
    /**
     * Returns the first 0-based element index with value >= {@code key} in a
     * sorted, signed, little-endian 64-bit integer run. A miss returns
     * {@code elementCount}; duplicates return their first position.
     * {@code byteOffset} is relative to the mapped region (not the file),
     * and need not be 8-byte aligned. Caller guarantees ascending order and
     * that other processes do not mutate/truncate the file during execution.
     * O(log elementCount) comparisons, O(1) additional memory.
     */
    public static long lowerBoundLongLE(PmemRegion region, long byteOffset,
                                        long elementCount, long key) {
        return PmemMapping.boundLongLE(region, byteOffset, elementCount, key, false);
    }

    /** First 0-based element index with value > {@code key}; see lowerBoundLongLE. */
    public static long upperBoundLongLE(PmemRegion region, long byteOffset,
                                        long elementCount, long key) {
        return PmemMapping.boundLongLE(region, byteOffset, elementCount, key, true);
    }

    /** Maximum query dimension; bounds copied query scratch to 4 MiB. */
    public static final int MAX_VECTOR_DIMENSION = 1 << 20;
    /** Maximum retained results; bounds primitive result arrays to 16 MiB. */
    public static final int MAX_VECTOR_RESULTS = 1 << 20;

    /** Immutable best-first row ordinals and cosine similarities. */
    public static final class VectorMatches {
        private final long[] rows;
        private final double[] scores;

        private VectorMatches(long[] rows, double[] scores) {
            this.rows = rows;
            this.scores = scores;
        }

        public int size() { return rows.length; }
        public long row(int index) { return rows[index]; }
        public double score(int index) { return scores[index]; }
    }

    /** See {@link #topKCosineFloatLE(PmemRegion, long, long, float[], int, java.util.function.LongConsumer)}. */
    public static VectorMatches topKCosineFloatLE(PmemRegion region, long byteOffset,
                                                 long rowCount, float[] query, int k) {
        return topKCosineFloatLE(region, byteOffset, rowCount, query, k, ignored -> { });
    }

    /**
     * Exact cosine retrieval over tightly packed, row-major little-endian FP32 vectors.
     * Each row has {@code query.length} components. Returns at most {@code k} rows,
     * ordered by descending similarity, then ascending zero-based row ordinal.
     * Row ordinals are relative to this run, not byte offsets or document IDs.
     * Finite, nonzero query and stored vectors are required; a malformed scanned row
     * refuses the whole result. Empty runs are allowed. No embeddings are generated.
     *
     * <p>Validates range arithmetic before allocating, copies the query, and borrows
     * one read lease for the entire scan, blocking this mapping's writes and close.
     * The backing file must remain immutable against other mappings and processes.
     * Scores accumulate in double precision with explicit column order; no native
     * hardware or DAX capability is inferred from successful execution.
     *
     * <p>Scratch is O(query.length + min(k,rowCount)) plus a 64 KiB scan window;
     * time is O(rowCount * query.length + rowCount * log(k)). No corpus-sized heap
     * copy, row object, vector index format, or approximate search is introduced.
     * Cooperative interruption checks throw {@link java.util.concurrent.CancellationException}
     * without clearing the interrupt flag. Lease acquisition follows the region lock contract;
     * an interrupted FILE read may also fail through the underlying channel I/O contract. Progress receives completed rows at zero,
     * every 256 rows and completion. It runs under the read lease and must not write
     * persist or close this region. Callback failure aborts the scan without a partial result.
     */
    public static VectorMatches topKCosineFloatLE(PmemRegion region, long byteOffset,
                                                 long rowCount, float[] query, int k,
                                                 java.util.function.LongConsumer progress) {
        java.util.Objects.requireNonNull(region, "region");
        java.util.Objects.requireNonNull(query, "query");
        java.util.Objects.requireNonNull(progress, "progress");
        if (query.length == 0 || query.length > MAX_VECTOR_DIMENSION)
            throw new IllegalArgumentException("vector dimension outside supported bounds");
        if (k < 1 || k > MAX_VECTOR_RESULTS)
            throw new IllegalArgumentException("result count outside supported bounds");
        long rowBytes = (long) query.length * Float.BYTES;
        if (rowCount < 0 || rowCount > Long.MAX_VALUE / rowBytes)
            throw new IllegalArgumentException("vector byte count overflow");
        long byteCount = rowCount * rowBytes;
        java.util.Objects.checkFromIndexSize(byteOffset, byteCount, region.length());
        float[] ownedQuery = query.clone();
        double queryNormSquared = 0;
        for (float component : ownedQuery) {
            if (!Float.isFinite(component)) throw new IllegalArgumentException("nonfinite query component");
            queryNormSquared += (double) component * component;
        }
        if (queryNormSquared == 0) throw new IllegalArgumentException("zero query norm");
        double queryNorm = Math.sqrt(queryNormSquared);
        return PmemMapping.withReadLease(region, () -> scanVectors(region, byteOffset,
                rowCount, ownedQuery, queryNorm, k, progress));
    }

    private static VectorMatches scanVectors(PmemRegion region, long byteOffset, long rowCount,
                                             float[] query, double queryNorm, int k,
                                             java.util.function.LongConsumer progress) {
        checkVectorInterrupted();
        progress.accept(0);
        checkVectorInterrupted();
        int limit = (int) Math.min((long) k, rowCount);
        long[] rows = new long[limit];
        double[] scores = new double[limit];
        if (limit == 0) return new VectorMatches(rows, scores);
        byte[] window = new byte[64 * 1024];
        long remaining = rowCount * (long) query.length * Float.BYTES;
        long nextOffset = byteOffset;
        int cursor = 0, available = 0, size = 0;
        for (long row = 0; row < rowCount; row++) {
            checkVectorInterrupted();
            double dot = 0, normSquared = 0;
            for (int column = 0; column < query.length; column++) {
                if (cursor == available) {
                    checkVectorInterrupted();
                    available = (int) Math.min((long) window.length, remaining);
                    region.read(nextOffset, window, 0, available);
                    nextOffset += available;
                    remaining -= available;
                    cursor = 0;
                }
                int bits = (window[cursor] & 255) | ((window[cursor + 1] & 255) << 8)
                        | ((window[cursor + 2] & 255) << 16) | (window[cursor + 3] << 24);
                cursor += Float.BYTES;
                float component = Float.intBitsToFloat(bits);
                if (!Float.isFinite(component))
                    throw new IllegalArgumentException("nonfinite vector component at row " + row);
                dot += (double) query[column] * component;
                normSquared += (double) component * component;
            }
            if (normSquared == 0) throw new IllegalArgumentException("zero vector norm at row " + row);
            double score = Math.max(-1d, Math.min(1d, dot / queryNorm / Math.sqrt(normSquared)));
            if (score == 0d) score = 0d; // Canonicalize signed zero before deterministic ties.
            if (size < limit) {
                int index = size++;
                rows[index] = row;
                scores[index] = score;
                while (index > 0) {
                    int parent = (index - 1) >>> 1;
                    if (!vectorWorse(scores[index], rows[index], scores[parent], rows[parent])) break;
                    swapVector(rows, scores, index, parent);
                    index = parent;
                }
            } else if (vectorWorse(scores[0], rows[0], score, row)) {
                rows[0] = row;
                scores[0] = score;
                siftVector(rows, scores, size);
            }
            if (((row + 1) & 255) == 0 || row + 1 == rowCount) {
                progress.accept(row + 1);
                checkVectorInterrupted();
            }
        }
        // Worst-at-root heap extraction fills the arrays from worst to best.
        for (int end = size - 1; end > 0; end--) {
            if ((end & 1023) == 0) checkVectorInterrupted();
            swapVector(rows, scores, 0, end);
            siftVector(rows, scores, end);
        }
        checkVectorInterrupted();
        return new VectorMatches(rows, scores);
    }

    private static void checkVectorInterrupted() {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("vector scan interrupted");
    }

    private static boolean vectorWorse(double score, long row, double otherScore, long otherRow) {
        return score < otherScore || (score == otherScore && row > otherRow);
    }

    private static void swapVector(long[] rows, double[] scores, int a, int b) {
        long row = rows[a]; rows[a] = rows[b]; rows[b] = row;
        double score = scores[a]; scores[a] = scores[b]; scores[b] = score;
    }

    private static void siftVector(long[] rows, double[] scores, int size) {
        int parent = 0;
        while (parent < size / 2) {
            int child = parent * 2 + 1;
            if (child + 1 < size && vectorWorse(scores[child + 1], rows[child + 1], scores[child], rows[child]))
                child++;
            if (!vectorWorse(scores[child], rows[child], scores[parent], rows[parent])) break;
            swapVector(rows, scores, parent, child);
            parent = child;
        }
    }

}
