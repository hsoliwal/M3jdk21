/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Strict, process-lifetime, read-only JNI mapping at one designated virtual address.
 *
 * <p>Admission either maps the requested backing image at precisely {@code base}, or fails.
 * No relocation, anonymous copy, heap fallback, or {@code MAP_FIXED} overwrite is permitted.
 * Identical virtual addresses require all participating processes to choose the same base
 * and image. A trusted publisher must keep image bytes and length immutable throughout their
 * lifetime: neither memory mapping nor SHA-256 admission prevents later external file writes.
 * This is a separately built proving owner, not a replacement for {@code java.lang.String}.
 */
public final class M3FixedAddressMapping {
    public static final long DEFAULT_BASE = 0x6000_0000_0000L;
    private static final Map<Long, Image> IMAGES = new HashMap<>();
    private static Path loadedLibrary;

    private M3FixedAddressMapping() { }

    /** A published mapping deliberately has no close/unmap method: views are process-lifetime. */
    public static final class Image {
        private final Path file;
        private final long address;
        private final String sha256;
        private final ByteBuffer owner;

        private Image(Path file, long address, String sha256, ByteBuffer owner) {
            this.file = file;
            this.address = address;
            this.sha256 = sha256;
            this.owner = owner;
        }

        public Path file() { return file; }
        public long address() { return address; }
        public long byteLength() { return owner.capacity(); }
        public String sha256() { return sha256; }
        public long addressOfOffset(int offset) {
            Objects.checkIndex(offset, owner.capacity());
            return Math.addExact(address, offset);
        }
        public ByteBuffer readOnlyBuffer() {
            return owner.asReadOnlyBuffer().clear();
        }
    }

    /**
     * Maps only the exact address. Every process must use the same file, digest and address.
     * If another mapping occupies the range, no existing mapping is overwritten.
     */
    public static synchronized Image open(Path path, long address, String requiredSha256,
            Path nativeLibrary) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(requiredSha256, "requiredSha256");
        Objects.requireNonNull(nativeLibrary, "nativeLibrary");
        if (address <= 0 || (address & 0xffffL) != 0) {
            throw new IllegalArgumentException("fixed base must be positive and 64-KiB aligned");
        }
        if (!requiredSha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("SHA-256 must contain exactly 64 hexadecimal digits");
        }
        String digest = requiredSha256.toLowerCase(Locale.ROOT);
        Path file = path.toRealPath();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("M3 shared image must be a regular file");
        }
        long length = Files.size(file);
        if (length == 0 || length > Integer.MAX_VALUE) {
            throw new IOException("M3 fixed mapping requires 1..Integer.MAX_VALUE bytes");
        }
        if (address > Long.MAX_VALUE - length) {
            throw new IllegalArgumentException("fixed region overflows signed address range");
        }
        Image previous = IMAGES.get(address);
        if (previous != null) {
            if (!previous.file.equals(file) || !previous.sha256.equals(digest)
                    || previous.byteLength() != length) {
                throw new IllegalStateException("different canonical image already owns fixed base");
            }
            return previous;
        }
        Path library = nativeLibrary.toRealPath();
        if (loadedLibrary == null) {
            System.load(library.toString());
            loadedLibrary = library;
        } else if (!loadedLibrary.equals(library)) {
            throw new IllegalStateException("M3 fixed mapper JNI library already bound");
        }
        ByteBuffer raw = mapExact0(file.toString(), address, length);
        if (raw == null || !raw.isDirect() || raw.capacity() != length
                || directAddress0(raw) != address) {
            throw new IllegalStateException("JNI mapping did not return exact requested address");
        }
        String actual = digest(raw.asReadOnlyBuffer());
        if (!actual.equals(digest)) {
            unmapRejected0(address, length);
            throw new IOException("M3 canonical image SHA-256 mismatch");
        }
        Image admitted = new Image(file, address, actual, raw);
        IMAGES.put(address, admitted);
        return admitted;
    }

    private static String digest(ByteBuffer buffer) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(buffer.clear());
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /* Only the native proof in this package may deliberately preoccupy the target range. */
    static synchronized void reserveCollisionForProof(long address, long length, Path nativeLibrary)
            throws IOException {
        Path library = nativeLibrary.toRealPath();
        if (loadedLibrary == null) {
            System.load(library.toString());
            loadedLibrary = library;
        } else if (!loadedLibrary.equals(library)) {
            throw new IllegalStateException("M3 fixed mapper JNI library already bound");
        }
        reserveCollision0(address, length);
    }

    private static native ByteBuffer mapExact0(String path, long address, long length)
            throws IOException;
    private static native long directAddress0(ByteBuffer mapped);
    private static native void unmapRejected0(long address, long length) throws IOException;
    private static native void reserveCollision0(long address, long length) throws IOException;
}
