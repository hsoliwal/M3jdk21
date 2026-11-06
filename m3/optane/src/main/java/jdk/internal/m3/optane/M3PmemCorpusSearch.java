// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import jdk.m3.optane.M3PmemSearch;
import jdk.m3.optane.M3PmemSourceImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * A bounded read-only first-hit signal over the original PMEMSDK source image.
 * Uses the incumbent file-local native/POSIX or FILE Java KMP search; neither
 * source bytes nor one object per file are retained. Hits are not proof that
 * a problem category or donor implementation is semantically applicable.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSearch.java blob 31f6ab6e4190; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
public final class M3PmemCorpusSearch {
    public static final int MAX_HITS = 20_000;
    private M3PmemCorpusSearch() {}

    /** Primitive, ordered result lanes; all offsets are relative to their file. */
    public static final class Result {
        private final String sourceRoot;
        private final String patternSha256;
        private final int scannedFiles;
        private final boolean complete;
        private final int[] fileIndices;
        private final long[] fileOffsets;

        private Result(String sourceRoot, String patternSha256, int scannedFiles,
                       boolean complete, int[] fileIndices, long[] fileOffsets) {
            this.sourceRoot = sourceRoot;
            this.patternSha256 = patternSha256;
            this.scannedFiles = scannedFiles;
            this.complete = complete;
            this.fileIndices = fileIndices;
            this.fileOffsets = fileOffsets;
        }
        public String sourceRoot() { return sourceRoot; }
        public String patternSha256() { return patternSha256; }
        public int scannedFiles() { return scannedFiles; }
        /** False means the result budget stopped the scan; never infer absence. */
        public boolean complete() { return complete; }
        public int hitCount() { return fileIndices.length; }
        public int fileIndexAt(int hit) { return fileIndices[hit]; }
        public long fileOffsetAt(int hit) { return fileOffsets[hit]; }
    }

    /**
     * Returns at most one first occurrence for each source file, in canonical
     * sorted image order. The copied pattern is stable even if a caller mutates
     * its array during a native call. Positive hit budgets are bounded; short
     * files are skipped without a JNI call. Cancellation is fail-closed: no
     * incomplete response escapes as a successful source search.
     *
     * The source snapshot must remain immutable, open and externally protected
     * against truncation throughout this scan; M3PmemSourceImage checks its own
     * region leases but cannot prevent unrelated writers from mutating files.
     */
    public static Result firstHits(M3PmemSourceImage image, byte[] pattern,
                                   int maxHits, BooleanSupplier cancelled) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(cancelled, "cancelled");
        if (pattern.length > M3PmemSearch.MAX_PATTERN_BYTES)
            throw new IllegalArgumentException("pattern exceeds incumbent PMEM search bound");
        if (maxHits < 1 || maxHits > MAX_HITS)
            throw new IllegalArgumentException("result hit budget must be 1..20000");
        byte[] needle = pattern.clone();
        String sourceRoot = image.sourceRoot();
        String patternRoot = sha256(needle);
        int files = image.fileCount();
        int[] fileIndices = new int[Math.min(files, maxHits)];
        long[] offsets = new long[fileIndices.length];
        int hits = 0;
        int scanned = 0;
        for (int i = 0; i < files; i++) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
                throw new CancellationException("PMEM source search cancelled before file " + i);
            int length = image.lengthAt(i);
            // First-occurrence search cannot match a longer pattern. Empty
            // pattern matches at position zero, including an empty file.
            long found = needle.length == 0 ? 0L
                    : needle.length > length ? -1L : image.indexOf(i, needle);
            if (found < -1L || found >= 0L && (found > (long) length - needle.length))
                throw new IllegalStateException("invalid native/FILE first-hit offset at file " + i);
            scanned++;
            if (found >= 0L) {
                fileIndices[hits] = i;
                offsets[hits] = found;
                hits++;
                if (hits == maxHits) break;
            }
        }
        // Cancellation after the last search must not be silently accepted.
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new CancellationException("PMEM source search cancelled after file " + scanned);
        if (!image.sourceRoot().equals(sourceRoot))
            throw new IllegalStateException("source image generation changed during search");
        return new Result(sourceRoot, patternRoot, scanned, scanned == files,
                Arrays.copyOf(fileIndices, hits), Arrays.copyOf(offsets, hits));
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
