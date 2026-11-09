// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import jdk.m3.optane.PmemSearch;
import jdk.m3.optane.PmemSourceImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Package-private sparse file selection over the incumbent PMEMSDK search.
 * Not another JNI kernel, full-corpus index, source reader or public API.
 * Original PmemCorpusSearch remains byte-identical.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedSearch.java blob 5b4d1a8b935c; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
final class PmemCorpusSelectedSearch {
    static final int MAX_SELECTED_FILES = 20_000;
    private static final byte[] DOMAIN =
            "M3_PMEM_SELECTED_FILES_V1\0".getBytes(StandardCharsets.US_ASCII);

    private PmemCorpusSelectedSearch() {}

    /**
     * Completeness is relative to the exact selected set, not the whole source
     * corpus; cache keys must include selectionSha256. No mutable array escapes.
     */
    static final class Result {
        private final String sourceRoot;
        private final String patternSha256;
        private final String selectionSha256;
        private final int selectedFiles;
        private final int scannedFiles;
        private final int[] fileIndices;
        private final long[] fileOffsets;

        Result(String sourceRoot, String patternSha256, String selectionSha256,
               int selectedFiles, int scannedFiles, int[] fileIndices, long[] fileOffsets) {
            this.sourceRoot = sourceRoot;
            this.patternSha256 = patternSha256;
            this.selectionSha256 = selectionSha256;
            this.selectedFiles = selectedFiles;
            this.scannedFiles = scannedFiles;
            this.fileIndices = fileIndices;
            this.fileOffsets = fileOffsets;
        }
        String sourceRoot() { return sourceRoot; }
        String patternSha256() { return patternSha256; }
        String selectionSha256() { return selectionSha256; }
        int selectedFiles() { return selectedFiles; }
        int scannedFiles() { return scannedFiles; }
        boolean complete() { return scannedFiles == selectedFiles; }
        int hitCount() { return fileIndices.length; }
        int fileIndexAt(int hit) { return fileIndices[hit]; }
        long fileOffsetAt(int hit) { return fileOffsets[hit]; }
    }

    /**
     * Search an explicitly catalogued subset of stable canonical file indices.
     * Strictly ascending, unique indices are fully checked before a callback
     * or any JNI operation. The cloned input lanes cannot be changed by the
     * caller once review begins. A returned incomplete list cannot prove absence.
     *
     * The M3File/PMEMSDK owner must prevent external backing mutation and
     * truncation while this read-only operation is in progress. An unchanged
     * manifest root alone does not prove external source immutability.
     */
    static Result firstHits(PmemSourceImage image, int[] selectedFileIndices,
                            byte[] pattern, int maxHits, BooleanSupplier cancelled) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(selectedFileIndices, "selectedFileIndices");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(cancelled, "cancelled");
        if (pattern.length > PmemSearch.MAX_PATTERN_BYTES)
            throw new IllegalArgumentException("pattern exceeds incumbent PMEM search bound");
        if (maxHits < 1 || maxHits > PmemCorpusSearch.MAX_HITS)
            throw new IllegalArgumentException("result budget must be 1..20000");
        if (selectedFileIndices.length > MAX_SELECTED_FILES)
            throw new IllegalArgumentException("selected file count exceeds 20000");

        byte[] needle = pattern.clone();
        int[] selected = selectedFileIndices.clone();
        String sourceRoot = image.sourceRoot();
        int corpusFiles = image.fileCount();
        int previous = -1;
        for (int index : selected) {
            if (index <= previous || index >= corpusFiles)
                throw new IllegalArgumentException("selection must be sorted, unique and in range");
            previous = index;
        }
        String selectionRoot = selectionSha256(sourceRoot, selected);
        String patternRoot = sha256(needle);
        int[] hitFiles = new int[Math.min(selected.length, maxHits)];
        long[] hitOffsets = new long[hitFiles.length];
        int hits = 0;
        int scanned = 0;
        for (int index : selected) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
                throw new CancellationException("selected search cancelled before file " + index);
            int length = image.lengthAt(index);
            // Empty pattern matches offset 0 even in an empty file; searches
            // cannot span neighboring packed source file boundaries.
            long found = needle.length == 0 ? 0L
                    : needle.length > length ? -1L : image.indexOf(index, needle);
            if (found < -1L || found >= 0L && found > (long) length - needle.length)
                throw new IllegalStateException("invalid native/FILE first-hit offset at file " + index);
            scanned++;
            if (found >= 0L) {
                hitFiles[hits] = index;
                hitOffsets[hits] = found;
                if (++hits == maxHits) break;
            }
        }
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new CancellationException("selected search cancelled after file " + scanned);
        if (!image.sourceRoot().equals(sourceRoot))
            throw new IllegalStateException("source image generation changed during selected search");
        return new Result(sourceRoot, patternRoot, selectionRoot, selected.length, scanned,
                Arrays.copyOf(hitFiles, hits), Arrays.copyOf(hitOffsets, hits));
    }

    /** Domain-separated, big-endian index framing; one reused four-byte lane. */
    private static String selectionSha256(String sourceRoot, int[] sortedIndices) {
        MessageDigest digest = digest();
        digest.update(DOMAIN);
        digest.update(sourceRoot.getBytes(StandardCharsets.US_ASCII));
        byte[] word = new byte[Integer.BYTES];
        digestInt(digest, word, sortedIndices.length);
        for (int index : sortedIndices) digestInt(digest, word, index);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void digestInt(MessageDigest digest, byte[] word, int value) {
        word[0] = (byte) (value >>> 24);
        word[1] = (byte) (value >>> 16);
        word[2] = (byte) (value >>> 8);
        word[3] = (byte) value;
        digest.update(word);
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(digest().digest(bytes));
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
