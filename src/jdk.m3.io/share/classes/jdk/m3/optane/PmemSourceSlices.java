// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Read-only primitive offset/length/hash view over an existing PMEMSDK mapping.
 * The canonical M3File owner builds and seals the packed source image and its
 * file identities; this adapter neither discovers files nor owns model calls.
 * M3File must retain its immutable source snapshot lease until dispatch ends.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSourceSlices.java blob 234f261627d3; recipe make/m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
public final class PmemSourceSlices {
    private static final int SHA256_BYTES = 32;
    private static final int MAX_FILES = 1_000_000;
    public static final int DEFAULT_MAX_SOURCE_BYTES = 1 << 20;
    private final PmemRegion source;
    private final long[] offsets;
    private final int[] lengths;
    private final byte[] digests;

    /**
     * All index arrays are copied. Each 32-byte digest matches one file's exact
     * byte contents; offsets are relative to the mapped region, not the file.
     */
    public PmemSourceSlices(PmemRegion source, long[] offsets, int[] lengths, byte[] sha256) {
        this(source, offsets, lengths, sha256, DEFAULT_MAX_SOURCE_BYTES);
    }

    public PmemSourceSlices(PmemRegion source, long[] offsets, int[] lengths,
                            byte[] sha256, int maxSourceBytes) {
        this.source = Objects.requireNonNull(source, "source");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        Objects.requireNonNull(sha256, "sha256");
        if (source.access() != PmemAccess.READ_ONLY || !source.isOpen())
            throw new IllegalArgumentException("source image must be an open read-only mapping");
        if (maxSourceBytes <= 0 || maxSourceBytes > 16 * 1024 * 1024)
            throw new IllegalArgumentException("source read budget must be within (0,16MiB]");
        if (offsets.length != lengths.length || offsets.length > MAX_FILES
                || sha256.length != ((long) offsets.length) * SHA256_BYTES)
            throw new IllegalArgumentException("source image index geometry mismatch");
        // Copy first: validation must apply to the exact arrays this view owns.
        this.offsets = offsets.clone();
        this.lengths = lengths.clone();
        this.digests = sha256.clone();
        long end = 0;
        long mappedLength = source.length();
        if (mappedLength < 0) throw new IllegalArgumentException("negative mapping size");
        for (int i = 0; i < this.offsets.length; i++) {
            long start = this.offsets[i];
            int count = this.lengths[i];
            // Subtraction avoids overflow even when the mapping uses 64-bit spans.
            if (start < end || count < 0 || count > maxSourceBytes || start > mappedLength
                    || (long) count > mappedLength - start)
                throw new IllegalArgumentException("invalid or overlapping source extent at " + i);
            end = start + count;
        }
    }

    /** Number of entries in the external M3File-owned image, no entry objects. */
    public int fileCount() { return offsets.length; }

    public int lengthAt(int fileIndex) { return lengths[fileIndex]; }

    public String sha256At(int fileIndex) {
        checkIndex(fileIndex);
        int start = fileIndex * SHA256_BYTES;
        return HexFormat.of().formatHex(digests, start, start + SHA256_BYTES);
    }

    /**
     * Materialize only the requested file for the existing one-call LLD workflow.
     * Byte identity is checked on the actual returned bytes after the JNI read.
     * Mismatched sources fail closed before they reach any AI model.
     */
    public byte[] readSealed(int fileIndex) {
        checkIndex(fileIndex);
        byte[] bytes = new byte[lengths[fileIndex]];
        // Even an empty source must acquire the existing region read lease.
        source.read(offsets[fileIndex], bytes, 0, bytes.length);
        byte[] actual;
        try {
            actual = MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JDK SHA-256 unavailable", impossible);
        }
        int base = fileIndex * SHA256_BYTES;
        int differences = 0;
        for (int i = 0; i < SHA256_BYTES; i++) differences |= actual[i] ^ digests[base + i];
        if (differences != 0) throw new SecurityException("M3File source image SHA-256 drift at " + fileIndex);
        return bytes;
    }

    /**
     * Reuse caller-owned storage for an exact SHA-verified source extent.
     * Only [arrayOffset, arrayOffset + fileLength) is overwritten and hashed.
     * The caller must not modify that extent during the read. Any failure
     * leaves the destination untrusted. Even an empty file acquires the
     * original PMEMSDK read/close lease through source.read.
     *
     * Donor: existing Synexia PMEMSDK reusable-buffer source-slices PR #15204.
     */
    public int readSealed(int fileIndex, byte[] target, int arrayOffset) {
        checkIndex(fileIndex);
        Objects.requireNonNull(target, "target");
        int count = lengths[fileIndex];
        Objects.checkFromIndexSize(arrayOffset, count, target.length);
        source.read(offsets[fileIndex], target, arrayOffset, count);
        byte[] actual;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(target, arrayOffset, count);
            actual = digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JDK SHA-256 unavailable", impossible);
        }
        int base = fileIndex * SHA256_BYTES;
        int differences = 0;
        for (int i = 0; i < SHA256_BYTES; i++) differences |= actual[i] ^ digests[base + i];
        if (differences != 0) throw new SecurityException("M3File source image SHA-256 drift at " + fileIndex);
        return count;
    }

    private void checkIndex(int fileIndex) {
        if (fileIndex < 0 || fileIndex >= lengths.length)
            throw new IndexOutOfBoundsException("source file index: " + fileIndex);
    }
}
