// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome.aix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable, explicit one-file source authority. Never discovers siblings or follows links. */
public final class AiXSourceSnapshot {
    public static final int DEFAULT_MAX_BYTES = 64 * 1024 * 1024;

    private final Path sourcePath;
    private final String name;
    private final byte[] bytes;
    private final String text;
    private final String sha256;
    private final BasicFileAttributes identity;

    private AiXSourceSnapshot(Path sourcePath, byte[] bytes, String text, BasicFileAttributes identity) {
        this.sourcePath = sourcePath;
        this.name = sourcePath.getFileName().toString();
        this.bytes = bytes.clone();
        this.text = text;
        this.sha256 = sha(bytes);
        this.identity = identity;
    }

    public static AiXSourceSnapshot read(Path source) throws IOException {
        return read(source, DEFAULT_MAX_BYTES);
    }

    public static AiXSourceSnapshot read(Path source, int maxBytes) throws IOException {
        Objects.requireNonNull(source, "source");
        if (maxBytes < 1 || maxBytes > DEFAULT_MAX_BYTES) {
            throw new IllegalArgumentException("maxBytes");
        }
        Path path = source.toAbsolutePath().normalize();
        rejectLinkedAncestry(path);
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() > maxBytes) {
            throw new IOException("SOURCE_NOT_BOUNDED_REGULAR_FILE");
        }
        if (before.size() > Integer.MAX_VALUE) {
            throw new IOException("SOURCE_TOO_LARGE_FOR_MEMORY_SNAPSHOT");
        }
        byte[] data;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(before.size()));
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer);
                if (read < 0) {
                    throw new IOException("SOURCE_SHORT_READ");
                }
            }
            data = buffer.array();
        }
        BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!sameIdentity(before, after)) {
            throw new IOException("SOURCE_CHANGED_DURING_READ");
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("SOURCE_MUST_BE_STRICT_UTF8", exception);
        }
        if (!java.util.Arrays.equals(data, text.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("UTF8_ROUND_TRIP_MISMATCH");
        }
        return new AiXSourceSnapshot(path, data, text, after);
    }

    public String name() { return name; }
    public int size() { return bytes.length; }
    public String sha256() { return sha256; }
    public String text() { return text; }
    public byte[] bytes() { return bytes.clone(); }
    public Path sourcePath() { return sourcePath; }

    public void verifyUnchanged() throws IOException {
        rejectLinkedAncestry(sourcePath);
        BasicFileAttributes now = Files.readAttributes(sourcePath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!sameIdentity(identity, now)) {
            throw new IOException("SOURCE_CHANGED_AFTER_SNAPSHOT");
        }
        AiXSourceSnapshot current = read(sourcePath, Math.max(1, bytes.length));
        if (current.size() != bytes.length || !current.sha256().equals(sha256)) {
            throw new IOException("SOURCE_HASH_CHANGED_AFTER_SNAPSHOT");
        }
    }

    static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    static void rejectLinkedAncestry(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path cursor = absolute.getRoot();
        if (cursor == null) {
            throw new IOException("SOURCE_ROOT_MISSING");
        }
        for (Path part : absolute) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) {
                throw new IOException("SOURCE_SYMLINK_FORBIDDEN");
            }
        }
    }

    private static boolean sameIdentity(BasicFileAttributes left, BasicFileAttributes right) {
        return left.isRegularFile()
                && right.isRegularFile()
                && left.size() == right.size()
                && left.lastModifiedTime().equals(right.lastModifiedTime())
                && Objects.equals(left.fileKey(), right.fileKey());
    }
}
