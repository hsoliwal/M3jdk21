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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    private static final int FINGERPRINT_BUFFER_BYTES = 8192;
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
     * Stream a bounded raw-byte SHA-256 observation without retaining file content.
     *
     * <p>Every call reads the content through this owner's regular-file, nofollow
     * stream. Neither modification time nor file size authorizes reuse. The
     * accepted length is inclusive; at most one additional byte is read to
     * detect a limit violation. The stream closes on success and failure.</p>
     *
     * <p>The result describes bytes observed during this read, not an atomic
     * filesystem version. Equal digests are not proof of class ABI or semantic
     * equivalence. Binary class files, archives and resources are never decoded
     * as text.</p>
     *
     * @param maxBytes maximum accepted byte count, from zero to Long.MAX_VALUE
     * @return immutable length and digest metadata, with no retained payload
     * @throws IllegalArgumentException if maxBytes is negative
     * @throws IOException if the file cannot be read, is not a regular file,
     *         exceeds the limit, or a read makes no progress
     */
    public Fingerprint fingerprint(long maxBytes) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        MessageDigest digest = sha256Digest();
        byte[] block = new byte[FINGERPRINT_BUFFER_BYTES];
        long size = 0;
        try (InputStream input = openInputStream()) {
            while (true) {
                long remaining = maxBytes - size;
                int limit = remaining >= block.length ? block.length : (int) remaining + 1;
                int count = input.read(block, 0, limit);
                if (count < 0) break;
                if (count == 0) throw new IOException("resource read made no progress");
                if (count > remaining) {
                    throw new IOException("resource exceeds configured fingerprint limit");
                }
                digest.update(block, 0, count);
                size += count;
            }
        }
        return new Fingerprint(size, HexFormat.of().formatHex(digest.digest()));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("required SHA-256 provider unavailable", impossible);
        }
    }

    private void requireRegular() throws IOException {
        if (!metadata().regularFile()) {
            throw new IOException("resource is not a regular file");
        }
    }

    /** Metadata for observed bytes; digest equality does not certify class ABI equality. */
    public record Fingerprint(long size, String sha256) {
        public Fingerprint {
            if (size < 0) throw new IllegalArgumentException("negative file size");
            Objects.requireNonNull(sha256, "sha256");
            if (sha256.length() != 64) throw new IllegalArgumentException("invalid SHA-256");
            for (int index = 0; index < sha256.length(); index++) {
                char digit = sha256.charAt(index);
                if (!(digit >= '0' && digit <= '9' || digit >= 'a' && digit <= 'f')) {
                    throw new IllegalArgumentException("invalid lowercase SHA-256");
                }
            }
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
