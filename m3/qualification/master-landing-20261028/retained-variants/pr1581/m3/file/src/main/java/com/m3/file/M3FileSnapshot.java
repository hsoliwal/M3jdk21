// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable existence, metadata and raw-byte SHA-256 observation from {@link M3File}.
 *
 * <p>The digest occupies four primitive longs. No source/class bytes, stream,
 * compiler objects or file tree are retained. Comparison uses complete SHA-256
 * and byte length, not timestamps alone. Hash equality is a content identity
 * check, not proof of semantic or class ABI equivalence or an atomic disk version.</p>
 */
public final class M3FileSnapshot {
    private static final int SHA256_BYTES = 32;

    private final Path path;
    private final M3File.Metadata metadata;
    private final long hash0;
    private final long hash1;
    private final long hash2;
    private final long hash3;

    private M3FileSnapshot(Path path, M3File.Metadata metadata, byte[] digest) {
        this.path = Objects.requireNonNull(path, "path");
        this.metadata = metadata;
        if (metadata == null) {
            hash0 = hash1 = hash2 = hash3 = 0;
        } else {
            if (Objects.requireNonNull(digest, "digest").length != SHA256_BYTES) {
                throw new IllegalArgumentException("SHA-256 must contain exactly 32 bytes");
            }
            ByteBuffer bytes = ByteBuffer.wrap(digest);
            hash0 = bytes.getLong();
            hash1 = bytes.getLong();
            hash2 = bytes.getLong();
            hash3 = bytes.getLong();
        }
    }

    static M3FileSnapshot absent(Path path) {
        return new M3FileSnapshot(path, null, null);
    }

    static M3FileSnapshot present(Path path, M3File.Metadata metadata, byte[] digest) {
        return new M3FileSnapshot(path, Objects.requireNonNull(metadata, "metadata"), digest);
    }

    /** The original immutable Path object supplied to M3File, with no normalization. */
    public Path path() { return path; }

    /** Whether a complete regular-file observation was admitted. */
    public boolean exists() { return metadata != null; }

    /** Existing M3File metadata, absent when the initial observation found no file. */
    public Optional<M3File.Metadata> metadata() { return Optional.ofNullable(metadata); }

    /** Lowercase complete raw-byte SHA-256, absent for a missing file. */
    public Optional<String> sha256() {
        return sha256Bytes().map(bytes -> HexFormat.of().formatHex(bytes));
    }

    /** Fresh caller-owned digest bytes on every invocation; no internal array is exposed. */
    public Optional<byte[]> sha256Bytes() {
        if (!exists()) return Optional.empty();
        return Optional.of(ByteBuffer.allocate(SHA256_BYTES)
                .putLong(hash0).putLong(hash1).putLong(hash2).putLong(hash3).array());
    }

    /**
     * Classify this observation relative to an earlier observation of the same path.
     *
     * <p>Both paths must belong to the same FileSystem instance and compare equal
     * under their provider's Path contract. Aliases, normalized spellings and hard
     * links are not coalesced. Content changes take priority over metadata changes.
     * METADATA_CHANGED compares the existing M3File.Metadata fields only.</p>
     *
     * @param previous the earlier observation
     * @return the direction of change from previous to this snapshot
     * @throws IllegalArgumentException if the observations refer to different provider paths
     */
    public Change changeFrom(M3FileSnapshot previous) {
        Objects.requireNonNull(previous, "previous");
        if (path.getFileSystem() != previous.path.getFileSystem()
                || !path.equals(previous.path)) {
            throw new IllegalArgumentException("snapshots refer to different provider paths");
        }
        if (!previous.exists()) return exists() ? Change.CREATED : Change.UNCHANGED;
        if (!exists()) return Change.DELETED;
        if (!sameContent(previous)) return Change.CONTENT_CHANGED;
        return metadata.equals(previous.metadata) ? Change.UNCHANGED : Change.METADATA_CHANGED;
    }

    private boolean sameContent(M3FileSnapshot previous) {
        return metadata.size() == previous.metadata.size()
                && hash0 == previous.hash0 && hash1 == previous.hash1
                && hash2 == previous.hash2 && hash3 == previous.hash3;
    }

    /** File observation deltas; none authorizes compiler or build-task reuse on its own. */
    public enum Change {
        UNCHANGED,
        METADATA_CHANGED,
        CONTENT_CHANGED,
        CREATED,
        DELETED
    }
}
