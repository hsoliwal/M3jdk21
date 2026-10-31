// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    /**
     * Opens one monitored stream whose shadows advance only when the primary stream is read.
     *
     * <p>Observers are invoked in declaration order and receive borrowed buffer ranges only.
     * No observer causes a second file read.</p>
     */
    public InputStream openInputStream(
            M3ProgressMonitor progress, M3StreamObserver... observers) throws IOException {
        M3ProgressMonitor monitor = Objects.requireNonNull(progress, "progress");
        return new M3ShadowInputStream(openInputStream(), monitor, compose(observers));
    }

    /**
     * Bounded complete read; a size precheck is only an optimization. The actual
     * stream is checked again to handle concurrent file growth or replacement.
     */
    public byte[] readAllBytes(int maxBytes) throws IOException {
        return readAllBytes(maxBytes, M3ProgressMonitor.none());
    }

    public byte[] readAllBytes(
            int maxBytes, M3ProgressMonitor progress, M3StreamObserver... observers)
            throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        Metadata before = metadata();
        if (before.size() > maxBytes) {
            throw new IOException("resource exceeds configured read limit");
        }
        try (InputStream input = openInputStream(progress, observers)) {
            byte[] data = input.readNBytes(maxBytes);
            if (input.read() != -1) {
                throw new IOException("resource exceeds configured read limit");
            }
            return data;
        }
    }

    /** Cheap metadata-only observation for change detection. */
    public M3FileSnapshot snapshot() throws IOException {
        return new M3FileSnapshot(metadata(), null);
    }

    /**
     * Exact-content observation. SHA-256 is computed as a shadow of one sequential read and no file
     * payload is retained. Metadata is checked before and after hashing; a visible metadata change
     * rejects the observation rather than publishing a mixed-generation snapshot.
     */
    public M3FileSnapshot snapshotWithSha256(M3ProgressMonitor progress) throws IOException {
        return indexedSnapshot(progress).source();
    }

    /**
     * Drives source hashing and all supplied parser/index fact observers from one physical read.
     *
     * <p>Observers may maintain arbitrary incremental parser/index state but publish only a
     * fixed-width fact receipt back to this module. M3File therefore never owns M3AST, regex,
     * JSON, binary-format or index-specific runtime objects.</p>
     */
    public M3FileIndexedSnapshot indexedSnapshot(
            M3ProgressMonitor progress, M3FileFactObserver... observers) throws IOException {
        Metadata before = metadata();
        if (!before.regularFile()) throw new IOException("resource is not a regular file");

        M3FileFactObserver[] checked =
                Objects.requireNonNull(observers, "observers").clone();
        MessageDigest sourceDigest = sha256();
        M3StreamObserver[] shadows = new M3StreamObserver[checked.length + 1];
        shadows[0] = sourceDigest::update;
        for (int index = 0; index < checked.length; index++) {
            shadows[index + 1] = Objects.requireNonNull(checked[index], "fact observer");
        }

        try (InputStream input = openInputStream(progress, shadows)) {
            byte[] buffer = new byte[64 * 1024];
            while (input.read(buffer) >= 0) {
                // The shadows perform all layered work; this loop only drives the primary stream.
            }
        }

        Metadata after = metadata();
        if (!before.equals(after)) {
            throw new IOException("resource metadata changed while indexing");
        }

        M3FileFact[] facts = new M3FileFact[checked.length];
        for (int index = 0; index < checked.length; index++) {
            M3FileFact fact = Objects.requireNonNull(checked[index].fact(), "fact");
            if (!checked[index].factName().equals(fact.name())) {
                throw new IllegalStateException("fact observer name changed after publication");
            }
            facts[index] = fact;
        }
        return new M3FileIndexedSnapshot(
                new M3FileSnapshot(after, sourceDigest.digest()),
                M3FileFacts.of(facts));
    }

    public M3FileDelta deltaSince(
            M3FileSnapshot before, boolean exactContent, M3ProgressMonitor progress)
            throws IOException {
        M3FileSnapshot after =
                exactContent ? snapshotWithSha256(progress) : snapshot();
        return Objects.requireNonNull(before, "before").deltaTo(after);
    }

    private static M3StreamObserver compose(M3StreamObserver[] observers) {
        M3StreamObserver[] checked = Objects.requireNonNull(observers, "observers").clone();
        if (checked.length == 0) return (bytes, offset, length) -> { };
        M3StreamObserver result = Objects.requireNonNull(checked[0], "observer");
        for (int index = 1; index < checked.length; index++) {
            result = result.andThen(Objects.requireNonNull(checked[index], "observer"));
        }
        return result;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
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
