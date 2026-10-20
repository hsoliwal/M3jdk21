// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/**
 * Thin, stateless read boundary over the caller's original NIO Path/provider.
 *
 * <p>Adapted from Synexia MIndexResourceAccess file-edge semantics. No new
 * filesystem, directory tree, interned path table or retained file descriptor is
 * created. Metadata is a point-in-time observation and is not an immutable view
 * of an externally changing filesystem. Symbolic links are never followed by
 * the file-open operations.</p>
 */
public final class M3File {
    private final Path path;

    private M3File(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public static M3File of(Path path) { return new M3File(path); }

    /** Original provider-owned Path, not an invented global file identifier. */
    public Path path() { return path; }

    /** Every invocation refreshes metadata with NOFOLLOW_LINKS. */
    public Metadata metadata() throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return new Metadata(attributes.isRegularFile(), attributes.isDirectory(),
                attributes.isSymbolicLink(), attributes.size(),
                attributes.lastModifiedTime());
    }

    /** Opens a caller-owned channel; the caller MUST close it. */
    public SeekableByteChannel openReadChannel() throws IOException {
        requireRegular();
        return Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }

    /** Caller owns the returned stream and closes the underlying channel. */
    public InputStream openInputStream() throws IOException {
        return Channels.newInputStream(openReadChannel());
    }

    /** Monitor read/skip byte work without buffering or duplicating source data. */
    public InputStream openInputStream(M3StreamProgress progress) throws IOException {
        M3StreamProgress.require(progress);
        return M3ShadowStreams.monitoredInput(openInputStream(), progress);
    }

    /**
     * Open one original provider channel with a bounded, shared shadow-page arena.
     * All layered consumers obtain cursors using {@code shared.shadow(child)};
     * the owning caller closes this stream. No filesystem scan or new source owner.
     */
    public M3ShadowStreams.SharedInput openShadowInputStream(
            int maxRetainedBytes, M3StreamProgress progress) throws IOException {
        if (maxRetainedBytes < 0) throw new IllegalArgumentException("negative shadow capacity");
        M3StreamProgress.require(progress);
        return M3ShadowStreams.input(openInputStream(), maxRetainedBytes, progress);
    }

    /**
     * Bounded complete read; a size precheck is only an optimization. The actual
     * stream is checked again to handle concurrent file growth or replacement.
     */
    public byte[] readAllBytes(int maxBytes) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        if (metadata().size() > maxBytes) {
            throw new IOException("resource exceeds configured read limit");
        }
        try (InputStream input = openInputStream()) {
            byte[] data = input.readNBytes(maxBytes);
            if (input.read() != -1) {
                throw new IOException("resource exceeds configured read limit");
            }
            return data;
        }
    }

    private void requireRegular() throws IOException {
        if (!metadata().regularFile()) {
            throw new IOException("resource is not a regular file");
        }
    }

    public record Metadata(boolean regularFile, boolean directory,
            boolean symbolicLink, long size, FileTime lastModified) {
        public Metadata {
            if (size < 0) throw new IllegalArgumentException("negative file size");
            Objects.requireNonNull(lastModified, "lastModified");
        }
    }
}
