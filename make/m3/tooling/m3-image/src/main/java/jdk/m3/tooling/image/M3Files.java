/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Deterministic and atomic file-system operations. */
public final class M3Files {
    private M3Files() {
    }

    /** Writes UTF-8 text atomically, creating parents. */
    public static void writeUtf8(Path target, String text) throws IOException {
        Path normalized = target.toAbsolutePath().normalize();
        Files.createDirectories(normalized.getParent());
        Path temporary = Files.createTempFile(normalized.getParent(), normalized.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, normalized, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, normalized, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    /** Copies a file or directory tree, refusing symlinks. */
    public static void copyTree(Path source, Path target) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(normalizedSource)) {
            throw new IOException("Symlink input is not admitted: " + normalizedSource);
        }
        if (Files.isRegularFile(normalizedSource)) {
            Files.createDirectories(normalizedTarget.getParent());
            Files.copy(normalizedSource, normalizedTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return;
        }
        if (!Files.isDirectory(normalizedSource)) {
            throw new IOException("Missing source tree: " + normalizedSource);
        }
        try (var stream = Files.walk(normalizedSource)) {
            for (Path path : stream.sorted().toList()) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Symlink input is not admitted: " + path);
                }
                Path relative = normalizedSource.relativize(path);
                Path destination = normalizedTarget.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else if (Files.isRegularFile(path)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    /** Deletes a path recursively without following symlinks. */
    public static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /** Returns deterministic manifest rows for all regular files under a root. */
    public static List<String> manifest(Path root, Set<String> excludedRelativePaths) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        List<String> rows = new ArrayList<>();
        try (var stream = Files.walk(normalized)) {
            for (Path file : stream.sorted().toList()) {
                if (Files.isSymbolicLink(file)) {
                    throw new IOException("Symbolic path is not admitted in manifest: " + file);
                }
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String relative = normalized.relativize(file).toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                if (!excludedRelativePaths.contains(relative)) {
                    rows.add(M3Hashing.sha256(file) + "\t" + relative + "\n");
                }
            }
        }
        return rows;
    }

    /** Attempts to make a script executable on POSIX file systems. */
    public static void makeExecutable(Path script) throws IOException {
        try {
            Set<PosixFilePermission> permissions = EnumSet.copyOf(Files.getPosixFilePermissions(script));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(script, permissions);
        } catch (UnsupportedOperationException ignored) {
            // Windows and non-POSIX file systems rely on the .cmd launcher.
        }
    }

    /** Ensures a new or empty output directory. */
    public static Path freshDirectory(Path output) throws IOException {
        Path normalized = output.toAbsolutePath().normalize();
        if (Files.exists(normalized)) {
            deleteTree(normalized);
        }
        Files.createDirectories(normalized);
        return normalized;
    }
}
