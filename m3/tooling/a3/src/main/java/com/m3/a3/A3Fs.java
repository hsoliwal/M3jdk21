// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

final class A3Fs {

    private A3Fs() {
    }

    static Path root(Path value) {
        return Objects.requireNonNull(value, "root")
                .toAbsolutePath()
                .normalize();
    }

    static Path out(Path root, Path value) {
        Path checkedRoot = root(root);
        Path requested = Objects.requireNonNull(value, "out");
        Path resolved =
                (requested.isAbsolute() ? requested : checkedRoot.resolve(requested))
                        .toAbsolutePath()
                        .normalize();
        Path buildRoot = checkedRoot.resolve("m3/build").normalize();
        if (!resolved.startsWith(buildRoot)) {
            throw new IllegalArgumentException(
                    "A3 output must remain under m3/build: " + resolved);
        }
        return resolved;
    }

    static Path source(Path root, Path value) {
        Path checkedRoot = root(root);
        Path requested = Objects.requireNonNull(value, "source");
        Path resolved =
                (requested.isAbsolute() ? requested : checkedRoot.resolve(requested))
                        .toAbsolutePath()
                        .normalize();
        if (!resolved.startsWith(checkedRoot)) {
            throw new IllegalArgumentException(
                    "A3 source escapes repository root: " + resolved);
        }
        return resolved;
    }

    static String rel(Path root, Path value) {
        return root(root)
                .relativize(source(root, value))
                .toString()
                .replace('\\', '/');
    }

    static String sha(byte[] bytes) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    static String sha(String value) {
        return sha(
                Objects.requireNonNull(value, "value")
                        .getBytes(StandardCharsets.UTF_8));
    }

    static void write(Path root, Path out, String content) throws IOException {
        Path target = out(root, out);
        Files.createDirectories(target.getParent());
        Files.writeString(
                target,
                Objects.requireNonNull(content, "content"),
                StandardCharsets.UTF_8);
    }

    static String cell(String value) {
        String checked = Objects.toString(value, "");
        if (checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("A3 TSV cell contains NUL");
        }
        return checked
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }
}
