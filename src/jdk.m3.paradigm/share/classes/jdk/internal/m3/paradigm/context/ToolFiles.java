/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Bounded local files in a host-owned checkout. Not a concurrent filesystem security sandbox.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/tooling/ToolFiles.java}
 * blob {@code cb708164babae1d1da51f8a0fa37a3695498f3da} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ToolFiles {
    private ToolFiles() { }

    public static Path relative(String name) {
        if (name == null || name.isBlank() || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0
                || name.chars().anyMatch(c -> c < 32)) {
            throw new IllegalArgumentException("Expected a portable relative path");
        }
        Path path = Path.of(name);
        if (path.isAbsolute() || !path.equals(path.normalize())
                || path.startsWith("..") || name.startsWith("/")) {
            throw new IllegalArgumentException("Path traversal rejected");
        }
        return path;
    }

    public static Path resolve(Path root, String name) throws IOException {
        Path base = root.toRealPath();
        Path path = base;
        for (Path component : relative(name)) {
            path = path.resolve(component);
            if (Files.isSymbolicLink(path)) throw new IOException("Symbolic link rejected");
        }
        if (!path.startsWith(base)) throw new IOException("Path outside checkout");
        return path;
    }

    public static byte[] read(Path path, int maximumBytes) throws IOException {
        if (maximumBytes < 1 || maximumBytes > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("Invalid file byte bound");
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Expected a regular non-link file");
        }
        try (SeekableByteChannel channel = Files.newByteChannel(path,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            if (channel.size() > maximumBytes) throw new IOException("File exceeds byte bound");
            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(channel.size() + 1, maximumBytes + 1L));
            while (buffer.hasRemaining() && channel.read(buffer) != -1) { /* bounded channel read */ }
            if (buffer.position() > maximumBytes) throw new IOException("File exceeds byte bound");
            if (!buffer.hasRemaining() && channel.read(ByteBuffer.allocate(1)) != -1) {
                throw new IOException("File changed beyond initial read bound");
            }
            return java.util.Arrays.copyOf(buffer.array(), buffer.position());
        }
    }

    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("SHA-256 unavailable", failure); }
    }
}
