// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
    /** A previously absent logical file now has an observed fingerprint. */
    public static final int ADDED = 1;
    /** A previously observed logical file is now known to be absent. */
    public static final int REMOVED = 2;
    /** Both observations exist and their byte count or raw SHA-256 differs. */
    public static final int CONTENT_CHANGED = 4;

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
     * Observe raw bytes once with bounded scratch space, for source or binary files.
     *
     * <p>The returned fingerprint retains only a byte count and four SHA-256 words.
     * Reuse it for an already admitted immutable generation instead of reading and
     * hashing again for each consumer. This method does not parse source or classes,
     * create a cache, or establish compilation/ABI equivalence.</p>
     *
     * <p>The byte limit is enforced during reading, including file growth. Detected
     * size, modification-time or file-key changes are refused. These checks do not
     * make an externally mutable file atomic: an undetected concurrent overwrite
     * can still produce a stream observation rather than one filesystem generation.
     * Build owners must establish stable input admission before reusing outputs.</p>
     *
     * @param maxBytes maximum accepted stream length, including zero
     * @throws IllegalArgumentException if maxBytes is negative
     * @throws IOException if reading fails, the file is not regular, the budget is
     *         exceeded, or a concurrent change is detected; errors never mean absence
     */
    public Fingerprint fingerprint(long maxBytes) throws IOException {
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        BasicFileAttributes before = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile()) throw new IOException("resource is not a regular file");
        if (before.size() > maxBytes) {
            throw new IOException("resource exceeds configured read limit");
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("required SHA-256 provider unavailable", impossible);
        }
        long count = 0;
        try (InputStream input = openInputStream()) {
            byte[] buffer = new byte[8192];
            while (count < maxBytes) {
                int read = input.read(buffer, 0, (int) Math.min(buffer.length, maxBytes - count));
                if (read < 0) break;
                if (read == 0) {
                    int single = input.read();
                    if (single < 0) break;
                    digest.update((byte) single);
                    count++;
                } else {
                    digest.update(buffer, 0, read);
                    count += read;
                }
            }
            if (count == maxBytes && input.read() != -1) {
                throw new IOException("resource exceeds configured read limit");
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || before.size() != count || after.size() != count
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("resource changed during fingerprint observation");
        }
        ByteBuffer hash = ByteBuffer.wrap(digest.digest());
        return new Fingerprint(count, hash.getLong(), hash.getLong(), hash.getLong(), hash.getLong());
    }

    /**
     * Compare two admitted content observations of the same logical file without I/O.
     *
     * <p>Null means caller-established absence, never unread/unknown/inaccessible.
     * Zero means equal fingerprints or two known absences; a nonzero result is one
     * of {@link #ADDED}, {@link #REMOVED}, or {@link #CONTENT_CHANGED}. Locations,
     * timestamps, ABI, compiler context and output validity are not compared.
     * SHA-256 equality is a content-addressing signal, not collision-free byte
     * equality proof or authorization to skip compiler, processor, test or plugin work.</p>
     */
    public static int contentDelta(Fingerprint previous, Fingerprint current) {
        if (previous == null) return current == null ? 0 : ADDED;
        if (current == null) return REMOVED;
        return previous.equals(current) ? 0 : CONTENT_CHANGED;
    }

    /**
     * Compact immutable raw-byte observation; digest words are in big-endian order.
     * This format is distinct from decoded UTF-16 and framed source-fact digests.
     * A caller importing existing digest words remains responsible for their provenance.
     */
    public record Fingerprint(long size, long digest0, long digest1, long digest2, long digest3) {
        public Fingerprint {
            if (size < 0) throw new IllegalArgumentException("negative fingerprint size");
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
