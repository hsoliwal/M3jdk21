// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * CPU-authoritative LF byte scan with opt-in, ABI-checked JNI over direct buffers.
 * All JNI calls are synchronous, input-only and retain no Java address or pointer.
 * No implicit native installation or GPU dependency is permitted.
 */
public final class LfCount {
    private static String loadedPath;
    private static String loadedSha;
    private static volatile boolean available;

    private LfCount() { }

    /** Absolute indexes [start,start+length) within the current buffer limit. */
    public static long javaCount(ByteBuffer buffer, int start, int length) {
        check(buffer, start, length);
        long sum = 0;
        for (int i = 0; i < length; i++) {
            if ((i & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("LF scan interrupted");
            }
            if (buffer.get(start + i) == (byte) '\n') sum++;
        }
        return sum;
    }

    /** Native direct-buffer leaf; Java remains the reference and library install is explicit. */
    public static long nativeCount(ByteBuffer buffer, int start, int length) {
        check(buffer, start, length);
        if (!buffer.isDirect()) throw new IllegalArgumentException("direct buffer required");
        if (!available) throw new IllegalStateException("native LF kernel not installed");
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("LF scan interrupted");
        }
        return countLf0(buffer, start, length);
    }

    public static synchronized void install(Path library, String expectedSha) throws IOException {
        Objects.requireNonNull(library, "library");
        if (!library.isAbsolute() || expectedSha == null || !expectedSha.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("absolute library and lower-case SHA-256 required");
        }
        Path normalized = library.normalize();
        for (Path p = normalized; p != null; p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw new IOException("symbolic native library path");
        }
        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)
                || Files.size(normalized) > 16L * 1024 * 1024) {
            throw new IOException("missing/oversized native library");
        }
        if (!shaFile(normalized).equals(expectedSha)) throw new IOException("native library SHA drift");
        if (loadedPath != null) {
            if (!loadedPath.equals(normalized.toString()) || !loadedSha.equals(expectedSha)) {
                throw new IOException("different JNI library already selected");
            }
            if (!available) throw new IOException("previous JNI admission failed");
            return;
        }
        // Latch identity before load; failures never authorize switching the in-process image.
        loadedPath = normalized.toString();
        loadedSha = expectedSha;
        System.load(normalized.toString());
        if (abi0() != 1 || !shaFile(normalized).equals(expectedSha)) {
            throw new IOException("JNI ABI or post-load file identity differs");
        }
        available = true;
    }

    private static void check(ByteBuffer b, int start, int length) {
        Objects.requireNonNull(b, "buffer");
        Objects.checkFromIndexSize(start, length, b.limit());
    }

    private static String shaFile(Path path) throws IOException {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(path)) {
                byte[] block = new byte[65536];
                int read;
                long total = 0;
                while ((read = stream.read(block)) >= 0) {
                    total += read;
                    if (total > 16L * 1024 * 1024) throw new IOException("native image grew");
                    md.update(block, 0, read);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    private static native int abi0();
    private static native long countLf0(ByteBuffer buffer, int start, int length);
}
