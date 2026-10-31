// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Thin, stateless read boundary over the caller's original NIO Path/provider.
 *
 * <p>Adapted from Synexia MIndexResourceAccess file-edge semantics. No new
 * filesystem, directory tree, interned path table or retained file descriptor is
 * created. Metadata is a point-in-time observation and is not an immutable view
 * of an externally changing filesystem. File-open operations refuse a symbolic
 * link at the final component; ancestor resolution remains provider-owned.</p>
 */
public final class M3File {
    private static final int HASH_BUFFER_BYTES = 8192;
    private final Path path;

    private M3File(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public static M3File of(Path path) { return new M3File(path); }

    /** Original provider-owned Path, not an invented global file identifier. */
    public Path path() { return path; }

    /** Every invocation refreshes metadata with NOFOLLOW_LINKS. */
    public Metadata metadata() throws IOException {
        return metadata(attributes());
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

    /**
     * Observe existence, metadata and a complete raw-byte SHA-256 using bounded memory.
     *
     * <p>Only an initial {@link NoSuchFileException} produces an absent observation.
     * Present files must be regular files and satisfy both the observed-size and
     * streamed-length budget. The final path component is opened with NOFOLLOW_LINKS,
     * as in {@link #openReadChannel()}; ancestor resolution remains provider-owned.
     * Before/after type, size, modification time, creation time and file key must
     * match, and both sizes must match the bytes read. Other failures propagate.</p>
     *
     * <p>This identifies bytes observed through one closed stream, not an atomic
     * filesystem revision. Undetectable concurrent writes or ancestor replacement
     * can still occur. A caller needing a stable build input must control its input
     * generation. No decoded source, AST, class ABI or payload copy is retained.</p>
     *
     * @param maxBytes maximum complete file length, including zero for an empty file
     * @return a detached immutable observation of this original provider path
     * @throws IllegalArgumentException if maxBytes is negative
     * @throws IOException if admission, reading, the budget or observed stability fails
     */
    public M3FileSnapshot snapshot(long maxBytes) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        BasicFileAttributes before;
        try {
            before = attributes();
        } catch (NoSuchFileException absent) {
            return M3FileSnapshot.absent(path);
        }
        if (!before.isRegularFile()) throw new IOException("resource is not a regular file");
        if (before.size() > maxBytes) throw new IOException("resource exceeds configured read limit");

        MessageDigest digest = sha256();
        long count;
        try (InputStream input = Channels.newInputStream(Files.newByteChannel(
                path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            count = hash(input, digest, maxBytes);
        }
        BasicFileAttributes after = attributes();
        requireStable(before, after, count);
        return M3FileSnapshot.present(path, metadata(after), digest.digest());
    }

    private BasicFileAttributes attributes() throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }

    private static Metadata metadata(BasicFileAttributes attributes) {
        return new Metadata(attributes.isRegularFile(), attributes.isDirectory(),
                attributes.isSymbolicLink(), attributes.size(), attributes.lastModifiedTime());
    }

    private static long hash(InputStream input, MessageDigest digest, long maxBytes)
            throws IOException {
        byte[] buffer = new byte[HASH_BUFFER_BYTES];
        long count = 0;
        while (count < maxBytes) {
            int allowed = (int) Math.min(buffer.length, maxBytes - count);
            int read = input.read(buffer, 0, allowed);
            if (read < 0) return count;
            if (read == 0) {
                int value = input.read();
                if (value < 0) return count;
                digest.update((byte) value);
                count++;
            } else {
                digest.update(buffer, 0, read);
                count += read;
            }
        }
        if (input.read() != -1) throw new IOException("resource exceeds configured read limit");
        return count;
    }

    private static void requireStable(BasicFileAttributes before, BasicFileAttributes after,
            long count) throws IOException {
        if (!after.isRegularFile() || before.size() != count || after.size() != count
                || before.isDirectory() != after.isDirectory()
                || before.isSymbolicLink() != after.isSymbolicLink()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !before.creationTime().equals(after.creationTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("resource changed during snapshot observation");
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("required SHA-256 provider unavailable", impossible);
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
