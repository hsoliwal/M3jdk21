// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Detached, cheap attribute observation with independent delta bits.
 * A zero attribute delta is not proof of unchanged content. Use complete byte
 * snapshots or an applicable provider generation contract for that decision.
 * Access times are deliberately excluded: reading a file may update them.
 */
public final class M3FileObservation {
    public static final int CREATED = 1;
    public static final int DELETED = 1 << 1;
    public static final int TYPE = 1 << 2;
    public static final int SIZE = 1 << 3;
    public static final int MODIFIED_TIME = 1 << 4;
    public static final int CREATION_TIME = 1 << 5;
    public static final int FILE_KEY = 1 << 6;
    public static final int PROVIDER_VERSION = 1 << 7;
    public static final int CHANGE_TIME = 1 << 8;

    private final Path path;
    private final M3File.Metadata metadata;
    private final FileTime creationTime;
    private final FileTime changeTime;
    private final Object fileKey;
    private final M3FileVersionView.Version version;
    private final boolean other;

    M3FileObservation(Path path, BasicFileAttributes attributes, FileTime changeTime,
            M3FileVersionView.Version version) {
        this.path = Objects.requireNonNull(path, "path");
        metadata = attributes == null ? null : new M3File.Metadata(attributes.isRegularFile(),
                attributes.isDirectory(), attributes.isSymbolicLink(), attributes.size(),
                attributes.lastModifiedTime());
        creationTime = attributes == null ? null : attributes.creationTime();
        this.changeTime = changeTime;
        fileKey = attributes == null ? null : attributes.fileKey();
        other = attributes != null && attributes.isOther();
        this.version = version;
    }

    public Path path() { return path; }
    public boolean exists() { return metadata != null; }
    public Optional<M3File.Metadata> metadata() { return Optional.ofNullable(metadata); }
    public Optional<FileTime> creationTime() { return Optional.ofNullable(creationTime); }
    public Optional<FileTime> changeTime() { return Optional.ofNullable(changeTime); }
    public Optional<M3FileVersionView.Version> version() { return Optional.ofNullable(version); }

    /** Combined changed-attribute bits, or only CREATED/DELETED on an existence transition. */
    public int changesFrom(M3FileObservation previous) {
        requireSamePath(previous);
        if (!previous.exists()) return exists() ? CREATED : 0;
        if (!exists()) return DELETED;
        int changes = 0;
        if (metadata.regularFile() != previous.metadata.regularFile()
                || metadata.directory() != previous.metadata.directory()
                || metadata.symbolicLink() != previous.metadata.symbolicLink()
                || other != previous.other) changes |= TYPE;
        if (metadata.size() != previous.metadata.size()) changes |= SIZE;
        if (!metadata.lastModified().equals(previous.metadata.lastModified())) changes |= MODIFIED_TIME;
        if (!Objects.equals(creationTime, previous.creationTime)) changes |= CREATION_TIME;
        if (!Objects.equals(changeTime, previous.changeTime)) changes |= CHANGE_TIME;
        if (!Objects.equals(fileKey, previous.fileKey)) changes |= FILE_KEY;
        if (!Objects.equals(version, previous.version)) changes |= PROVIDER_VERSION;
        return changes;
    }

    /**
     * Whether both present observations carry the same provider version token.
     * This consumes the provider's explicit generation guarantee, not metadata
     * equality. It is false when either token is unavailable or the file is absent.
     */
    public boolean sameProviderVersion(M3FileObservation previous) {
        requireSamePath(previous);
        return exists() && previous.exists() && version != null && version.equals(previous.version);
    }

    private void requireSamePath(M3FileObservation previous) {
        Objects.requireNonNull(previous, "previous");
        if (path.getFileSystem() != previous.path.getFileSystem() || !path.equals(previous.path)) {
            throw new IllegalArgumentException("observations refer to different provider paths");
        }
    }
}
