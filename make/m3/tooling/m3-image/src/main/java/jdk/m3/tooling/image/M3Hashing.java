/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** SHA-256 and content-tree identities. */
public final class M3Hashing {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private M3Hashing() {
    }

    /** Hashes raw bytes. */
    public static String sha256(byte[] bytes) {
        MessageDigest digest = newDigest();
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Hashes UTF-8 text exactly. */
    public static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Hashes a regular file. */
    public static String sha256(Path file) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Hashes a file or a deterministic directory tree. */
    public static String hashPath(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isRegularFile(normalized)) {
            return sha256(normalized);
        }
        if (!Files.isDirectory(normalized)) {
            throw new IOException("Input is neither a regular file nor directory: " + normalized);
        }
        List<String> rows = new ArrayList<>();
        try (var stream = Files.walk(normalized)) {
            for (Path file : stream.sorted().toList()) {
                if (Files.isSymbolicLink(file)) {
                    throw new IOException("Symbolic path is not admitted in content identity: " + file);
                }
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String relative = normalized.relativize(file).toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                rows.add(sha256(file) + "\t" + relative + "\n");
            }
        }
        MessageDigest digest = newDigest();
        for (String row : rows) {
            digest.update(row.getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Validates and returns a lowercase SHA-256 value. */
    public static String requireSha256(String value, String name) {
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " is not a lowercase SHA-256 value: " + value);
        }
        return value;
    }

    /** Hashes ordered manifest rows exactly. */
    public static String hashRows(List<String> rows) {
        MessageDigest digest = newDigest();
        for (String row : rows) {
            digest.update(row.getBytes(StandardCharsets.UTF_8));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by Java", exception);
        }
    }

}
