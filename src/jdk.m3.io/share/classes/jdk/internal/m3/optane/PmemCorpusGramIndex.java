// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

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
 * Original-source two-byte-gram filter, with sparse sorted words or dense bits
 * chosen independently for each file. The filter has no false negatives while
 * the PMEMSDK/M3File source-generation lease remains immutable. All remaining
 * first hits are still resolved by the original Java/FILE or native JNI KMP.
 * A gram hit never constitutes semantic, donor-copy or source-edit authority.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/main/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndex.java blob c3a33cabb2de; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
final class PmemCorpusGramIndex {
    private static final int GRAM_BITS = 1 << 16;
    private static final int DENSE_BYTES = GRAM_BITS / Byte.SIZE; // 8192 per dense file
    private static final byte DENSE = 1;
    private static final int MAX_RETAINED_BYTES = 512 * 1024 * 1024;
    private static final byte[] INDEX_DOMAIN =
            "M3_PMEM_ADAPTIVE_2GRAM_INDEX_V1\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] SELECTION_DOMAIN =
            "M3_PMEM_2GRAM_SELECTION_V1\0".getBytes(StandardCharsets.US_ASCII);

    private final String sourceRoot;
    private final String indexRoot;
    private final int[] offsets;
    private final byte[] formats;
    private final byte[] grams;
    private final int[] fileFrequency;
    private final long retainedPrimitiveBytes;
    private final int denseFiles;

    private PmemCorpusGramIndex(String root, int[] offsets, byte[] formats, byte[] grams,
                                int[] frequencies, long retainedBytes, int denseCount) {
        sourceRoot = root;
        this.offsets = offsets;
        this.formats = formats;
        this.grams = grams;
        fileFrequency = frequencies;
        retainedPrimitiveBytes = retainedBytes;
        denseFiles = denseCount;
        indexRoot = sealIndex(root, offsets, formats, grams);
    }

    /**
     * Two byte-sealed serial passes, with one fixed 65,536-element stamp and
     * scratch lane; selected dense files use an exact 8 KiB bit map rather than
     * allocating up to 128 KiB of sorted byte-pair postings per file. In a
     * 20,000-file source image, even maximally diverse binary files retain at
     * most 20,000*8192 bytes plus bounded offset/mode/frequency lanes.
     * Temporary source reads, KMP buffers and OS cache are NOT included in
     * maxRetainedBytes: the backing source image owns their budgets and leases.
     */
    static PmemCorpusGramIndex build(PmemSourceImage image, long maxRetainedBytes,
                                     BooleanSupplier cancelled) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(cancelled, "cancelled");
        int files = image.fileCount();
        if (files < 0 || files > 1_000_000 || maxRetainedBytes < 0
                || maxRetainedBytes > MAX_RETAINED_BYTES)
            throw new IllegalArgumentException("invalid file count or index capacity");
        long baseBytes = (files + 1L) * Integer.BYTES
                + files + (long) GRAM_BITS * Integer.BYTES;
        if (maxRetainedBytes < baseBytes)
            throw new IllegalArgumentException("budget too small for immutable index metadata");
        String root = image.sourceRoot();
        // Read original source bytes twice using one caller-owned buffer; the
        // snapshot owner validates the SHA-256 of each selected extent.
        int maxFileLength = 0;
        for (int file = 0; file < files; file++)
            maxFileLength = Math.max(maxFileLength, image.lengthAt(file));
        byte[] sourceBytes = new byte[maxFileLength];
        int[] offsets = new int[files + 1];
        byte[] formats = new byte[files];
        int[] frequency = new int[GRAM_BITS];
        int[] seen = new int[GRAM_BITS];
        int[] scratch = new int[GRAM_BITS];
        long total = 0;
        int denseCount = 0;
        for (int file = 0; file < files; file++) {
            checkCancelled(cancelled, file);
            int length = image.readSealed(file, sourceBytes, 0);
            int distinct = gather(sourceBytes, length, seen, file + 1, scratch, frequency);
            boolean dense = ((long) distinct * Character.BYTES) > DENSE_BYTES;
            int retained = dense ? DENSE_BYTES : distinct * Character.BYTES;
            if (retained > maxRetainedBytes - baseBytes - total)
                throw new IllegalArgumentException("2-gram index retained capacity exceeded");
            if (dense) { formats[file] = DENSE; denseCount++; }
            total += retained;
            offsets[file + 1] = (int) total;
        }
        byte[] payload = new byte[(int) total];
        for (int file = 0; file < files; file++) {
            checkCancelled(cancelled, file);
            int length = image.readSealed(file, sourceBytes, 0);
            int count = gather(sourceBytes, length, seen, files + file + 1, scratch, null);
            int base = offsets[file];
            if (formats[file] == DENSE) {
                if ((long) count * Character.BYTES <= DENSE_BYTES)
                    throw new IllegalStateException("source gram representation changed across sealed passes");
                for (int i = 0; i < count; i++) {
                    int gram = scratch[i];
                    payload[base + (gram >>> 3)] |= (byte) (1 << (gram & 7));
                }
            } else {
                if (count * Character.BYTES != offsets[file + 1] - base)
                    throw new IllegalStateException("source gram count changed across sealed passes");
                Arrays.sort(scratch, 0, count);
                for (int i = 0; i < count; i++) {
                    int gram = scratch[i];
                    payload[base + 2 * i] = (byte) (gram >>> 8);
                    payload[base + 2 * i + 1] = (byte) gram;
                }
            }
        }
        checkCancelled(cancelled, files);
        if (!root.equals(image.sourceRoot()))
            throw new IllegalStateException("source generation changed during index build");
        return new PmemCorpusGramIndex(root, offsets, formats, payload, frequency,
                baseBytes + payload.length, denseCount);
    }

    /** Operate only on the verified file extent, not stale buffer suffix bytes. */
    private static int gather(byte[] bytes, int byteLength, int[] seen, int stamp,
                              int[] scratch, int[] frequency) {
        int count = 0;
        for (int i = 1; i < byteLength; i++) {
            int gram = ((bytes[i - 1] & 255) << 8) | (bytes[i] & 255);
            if (seen[gram] == stamp) continue;
            seen[gram] = stamp;
            scratch[count++] = gram;
            if (frequency != null) frequency[gram]++;
        }
        return count;
    }

    static final class Result {
        private final String originalSelectionRoot;
        private final String indexRoot;
        private final int selectedFiles;
        private final int candidates;
        private final PmemCorpusSelectedSearch.Result matching;
        private Result(String selection, String index, int files, int count,
                       PmemCorpusSelectedSearch.Result matching) {
            originalSelectionRoot = selection;
            indexRoot = index;
            selectedFiles = files;
            candidates = count;
            this.matching = matching;
        }
        String sourceRoot() { return matching.sourceRoot(); }
        String patternSha256() { return matching.patternSha256(); }
        String originalSelectionSha256() { return originalSelectionRoot; }
        String indexSha256() { return indexRoot; }
        int selectedFiles() { return selectedFiles; }
        int prunedFiles() { return selectedFiles - candidates; }
        int candidateFiles() { return candidates; }
        int searchedCandidates() { return matching.scannedFiles(); }
        /** Full proof of absence needs every possible candidate, not every pruned file. */
        boolean complete() { return matching.complete(); }
        int hitCount() { return matching.hitCount(); }
        int fileIndexAt(int hit) { return matching.fileIndexAt(hit); }
        long fileOffsetAt(int hit) { return matching.fileOffsetAt(hit); }
    }

    /**
     * Rarest pair is a necessary condition, never sufficient; recheck every
     * candidate through the original exact file-local KMP. Selection is
     * verified in canonical strictly ascending order before any model/native
     * callback. All arrays consumed from callers are copied, including needle.
     */
    Result firstHits(PmemSourceImage image, int[] selectedFileIndices, byte[] pattern,
                     int maxHits, BooleanSupplier cancelled) {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(selectedFileIndices, "selectedFileIndices");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(cancelled, "cancelled");
        if (selectedFileIndices.length > PmemCorpusSelectedSearch.MAX_SELECTED_FILES)
            throw new IllegalArgumentException("selection exceeds 20000 file indices");
        if (maxHits < 1 || maxHits > PmemCorpusSearch.MAX_HITS)
            throw new IllegalArgumentException("result budget outside 1..20000");
        if (pattern.length > jdk.m3.optane.PmemSearch.MAX_PATTERN_BYTES)
            throw new IllegalArgumentException("PMEM pattern bytes exceed incumbent bound");
        int[] selected = selectedFileIndices.clone();
        byte[] needle = pattern.clone();
        int previous = -1;
        for (int file : selected) {
            if (file <= previous || file >= offsets.length - 1)
                throw new IllegalArgumentException("source selection must be sorted, unique and in range");
            previous = file;
        }
        if (image.fileCount() != offsets.length - 1 || !sourceRoot.equals(image.sourceRoot()))
            throw new IllegalArgumentException("index is bound to a different source generation");
        String originalSelection = sealSelection(sourceRoot, selected);
        int[] candidates;
        if (needle.length < 2) {
            candidates = selected;
        } else {
            // Up to three distinct adjacent byte-pairs are necessary for a
            // complete match. The third check is conditional on the first two
            // succeeding and never replaces exact Java/FILE or native JNI KMP.
            // Ties retain original pattern order; repeated pairs are ignored.
            int rarest = -1, second = -1, third = -1;
            int lowestFrequency = Integer.MAX_VALUE;
            int secondFrequency = Integer.MAX_VALUE, thirdFrequency = Integer.MAX_VALUE;
            for (int i = 1; i < needle.length; i++) {
                int gram = ((needle[i - 1] & 255) << 8) | (needle[i] & 255);
                if (gram == rarest || gram == second || gram == third) continue;
                int frequency = fileFrequency[gram];
                if (frequency < lowestFrequency) {
                    third = second;
                    thirdFrequency = secondFrequency;
                    second = rarest;
                    secondFrequency = lowestFrequency;
                    rarest = gram;
                    lowestFrequency = frequency;
                } else if (frequency < secondFrequency) {
                    third = second;
                    thirdFrequency = secondFrequency;
                    second = gram;
                    secondFrequency = frequency;
                } else if (frequency < thirdFrequency) {
                    third = gram;
                    thirdFrequency = frequency;
                }
            }
            int[] scratch = new int[selected.length];
            int retained = 0;
            for (int file : selected) {
                checkCancelled(cancelled, file);
                if (lowestFrequency != 0 && image.lengthAt(file) >= needle.length
                        && containsGram(file, rarest)
                        && (second < 0 || containsGram(file, second))
                        && (third < 0 || containsGram(file, third)))
                    scratch[retained++] = file;
            }
            candidates = Arrays.copyOf(scratch, retained);
        }
        PmemCorpusSelectedSearch.Result exact =
                PmemCorpusSelectedSearch.firstHits(image, candidates, needle, maxHits, cancelled);
        if (!sourceRoot.equals(image.sourceRoot()))
            throw new IllegalStateException("source generation changed after indexed search");
        return new Result(originalSelection, indexRoot, selected.length, candidates.length, exact);
    }

    private boolean containsGram(int file, int gram) {
        int from = offsets[file];
        if (formats[file] == DENSE)
            return (grams[from + (gram >>> 3)] & (1 << (gram & 7))) != 0;
        int low = 0, high = (offsets[file + 1] - from) / Character.BYTES;
        while (low < high) {
            int middle = low + ((high - low) >>> 1);
            int at = from + 2 * middle;
            int value = ((grams[at] & 255) << 8) | (grams[at + 1] & 255);
            if (value < gram) low = middle + 1;
            else high = middle;
        }
        if (low == (offsets[file + 1] - from) / Character.BYTES) return false;
        int at = from + 2 * low;
        return ((grams[at] & 255) << 8 | (grams[at + 1] & 255)) == gram;
    }

    String sourceRoot() { return sourceRoot; }
    String indexSha256() { return indexRoot; }
    int fileCount() { return offsets.length - 1; }
    int denseFileCount() { return denseFiles; }
    long retainedPrimitiveBytes() { return retainedPrimitiveBytes; }
    int documentFrequency(int unsignedGram) {
        if (unsignedGram < 0 || unsignedGram >= GRAM_BITS)
            throw new IndexOutOfBoundsException("invalid unsigned two-byte gram");
        return fileFrequency[unsignedGram];
    }

    private static String sealIndex(String root, int[] offsets, byte[] formats, byte[] grams) {
        MessageDigest digest = digest();
        digest.update(INDEX_DOMAIN);
        digest.update(root.getBytes(StandardCharsets.US_ASCII));
        byte[] word = new byte[Integer.BYTES];
        writeInt(digest, word, formats.length);
        for (int value : offsets) writeInt(digest, word, value);
        digest.update(formats);
        digest.update(grams);
        return HexFormat.of().formatHex(digest.digest());
    }
    private static String sealSelection(String root, int[] indices) {
        MessageDigest digest = digest();
        digest.update(SELECTION_DOMAIN);
        digest.update(root.getBytes(StandardCharsets.US_ASCII));
        byte[] word = new byte[Integer.BYTES];
        writeInt(digest, word, indices.length);
        for (int index : indices) writeInt(digest, word, index);
        return HexFormat.of().formatHex(digest.digest());
    }
    private static void writeInt(MessageDigest digest, byte[] bytes, int value) {
        bytes[0] = (byte) (value >>> 24);
        bytes[1] = (byte) (value >>> 16);
        bytes[2] = (byte) (value >>> 8);
        bytes[3] = (byte) value;
        digest.update(bytes);
    }
    private static void checkCancelled(BooleanSupplier cancelled, int file) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
            throw new CancellationException("PMEM index cancelled at source file " + file);
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
