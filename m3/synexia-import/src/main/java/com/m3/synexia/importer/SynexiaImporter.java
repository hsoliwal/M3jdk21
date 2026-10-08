// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Verifies and optionally materializes one sealed Synexia Apache delivery into M3JDK21. */
public final class SynexiaImporter {
    @FunctionalInterface
    interface MaterializeGate {
        void beforeMove(int writeIndex, SynexiaImportManifest.Entry entry) throws IOException;
    }

    private record Created(Path target, String expectedSha256) {}

    private static final MaterializeGate NOOP_GATE = (index, entry) -> {};

    private SynexiaImporter() {}

    public static void verify(
            Path synexiaRoot,
            Path m3jdkRoot,
            SynexiaImportManifest manifest) throws IOException {
        verifySources(synexiaRoot, manifest);
        Path targetRoot = root(m3jdkRoot, "m3jdkRoot");
        Objects.requireNonNull(manifest, "manifest");

        for (SynexiaImportManifest.Entry entry : manifest.entries()) {
            Path target = resolve(targetRoot, entry.targetPath(), "target");
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || Files.isSymbolicLink(target)) {
                    throw new IllegalStateException("M3JDK21 target is not a regular file: " + entry.targetPath());
                }
                String current = sha256(Files.readAllBytes(target));
                if (!current.equals(entry.sha256())) {
                    throw new IllegalStateException(
                            "M3JDK21 target drift: "
                                    + entry.targetPath()
                                    + " expected="
                                    + entry.sha256()
                                    + " actual="
                                    + current);
                }
            }
        }
    }

    /** Verifies only the sealed Synexia source side of a delivery manifest. */
    public static void verifySources(
            Path synexiaRoot,
            SynexiaImportManifest manifest) throws IOException {
        Path sourceRoot = root(synexiaRoot, "synexiaRoot");
        Objects.requireNonNull(manifest, "manifest");
        for (SynexiaImportManifest.Entry entry : manifest.entries()) {
            Path source = resolve(sourceRoot, entry.sourcePath(), "source");
            if (!Files.isRegularFile(source) || Files.isSymbolicLink(source)) {
                throw new IllegalStateException(
                        "Synexia source missing or not regular: " + entry.sourcePath());
            }
            String actual = sha256(Files.readAllBytes(source));
            if (!actual.equals(entry.sha256())) {
                throw new IllegalStateException(
                        "Synexia source hash drift: "
                                + entry.sourcePath()
                                + " expected="
                                + entry.sha256()
                                + " actual="
                                + actual);
            }
        }
    }

    public static void verifyTargetSnapshot(
            Path m3jdkRoot,
            SynexiaImportManifest manifest) throws IOException {
        Path targetRoot = root(m3jdkRoot, "m3jdkRoot");
        Objects.requireNonNull(manifest, "manifest");
        for (SynexiaImportManifest.Entry entry : manifest.entries()) {
            Path target = resolve(targetRoot, entry.targetPath(), "target");
            if (!Files.isRegularFile(target) || Files.isSymbolicLink(target)) {
                throw new IllegalStateException(
                        "M3JDK21 imported target missing or not regular: " + entry.targetPath());
            }
            String current = sha256(Files.readAllBytes(target));
            if (!current.equals(entry.sha256())) {
                throw new IllegalStateException(
                        "M3JDK21 imported target hash drift: "
                                + entry.targetPath()
                                + " expected="
                                + entry.sha256()
                                + " actual="
                                + current);
            }
        }
    }

    public static void materialize(
            Path synexiaRoot,
            Path m3jdkRoot,
            SynexiaImportManifest manifest) throws IOException {
        materialize(synexiaRoot, m3jdkRoot, manifest, NOOP_GATE);
    }

    static void materialize(
            Path synexiaRoot,
            Path m3jdkRoot,
            SynexiaImportManifest manifest,
            MaterializeGate gate) throws IOException {
        verify(synexiaRoot, m3jdkRoot, manifest);
        Path sourceRoot = root(synexiaRoot, "synexiaRoot");
        Path targetRoot = root(m3jdkRoot, "m3jdkRoot");
        Objects.requireNonNull(gate, "gate");

        List<Created> created = new ArrayList<>();
        List<Path> createdDirectories = new ArrayList<>();
        int writeIndex = 0;
        try {
            for (SynexiaImportManifest.Entry entry : manifest.entries()) {
                Path source = resolve(sourceRoot, entry.sourcePath(), "source");
                Path target = resolve(targetRoot, entry.targetPath(), "target");
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) continue;
                Path parent = target.getParent();
                if (parent != null) {
                    createDirectoriesTracked(targetRoot, parent, createdDirectories);
                }
                Path temporary = Files.createTempFile(
                        parent == null ? targetRoot : parent,
                        target.getFileName().toString(),
                        ".tmp");
                boolean moved = false;
                try {
                    Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                    String copied = sha256(Files.readAllBytes(temporary));
                    if (!copied.equals(entry.sha256())) {
                        throw new IllegalStateException(
                                "copied Synexia file hash mismatch: " + entry.targetPath());
                    }
                    gate.beforeMove(writeIndex++, entry);
                    try {
                        Files.move(
                                temporary,
                                target,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                        Files.move(temporary, target);
                    }
                    moved = true;
                    created.add(new Created(target, entry.sha256()));
                } finally {
                    if (!moved) Files.deleteIfExists(temporary);
                }
            }
            verify(synexiaRoot, m3jdkRoot, manifest);
        } catch (IOException | RuntimeException failure) {
            rollback(created, createdDirectories, failure);
            throw failure;
        }
    }

    private static Path root(Path value, String field) {
        Path root = Objects.requireNonNull(value, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException(field + " must be an existing real directory");
        }
        return root;
    }

    private static Path resolve(Path root, String relative, String field) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(field + " path escapes root");
        }
        rejectSymlinkAncestors(root, resolved.getParent(), field);
        return resolved;
    }

    private static void rejectSymlinkAncestors(Path root, Path parent, String field) {
        if (parent == null || !parent.startsWith(root)) {
            throw new IllegalArgumentException(field + " parent escapes root");
        }
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(current)
                            || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS))) {
                throw new IllegalStateException(
                        field + " path contains unsafe ancestor: " + current);
            }
        }
    }

    private static void createDirectoriesTracked(
            Path root, Path parent, List<Path> createdDirectories) throws IOException {
        Path current = root;
        for (Path part : root.relativize(parent)) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                requireRealDirectory(current);
                continue;
            }
            try {
                Files.createDirectory(current);
                createdDirectories.add(current);
            } catch (FileAlreadyExistsException raced) {
                requireRealDirectory(current);
            }
        }
    }

    private static void requireRealDirectory(Path path) {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IllegalStateException(
                    "import path component is not a real directory: " + path);
        }
    }

    private static void rollback(
            List<Created> created, List<Path> createdDirectories, Throwable primary) {
        for (int index = created.size() - 1; index >= 0; index--) {
            Created item = created.get(index);
            try {
                if (!Files.exists(item.target(), LinkOption.NOFOLLOW_LINKS)) continue;
                if (!Files.isRegularFile(item.target(), LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(item.target())) {
                    throw new IllegalStateException(
                            "rollback target is no longer a regular file: " + item.target());
                }
                String current = sha256(Files.readAllBytes(item.target()));
                if (!current.equals(item.expectedSha256())) {
                    throw new IllegalStateException(
                            "rollback refuses changed target: " + item.target());
                }
                Files.delete(item.target());
            } catch (IOException | RuntimeException rollbackFailure) {
                primary.addSuppressed(rollbackFailure);
            }
        }
        for (int index = createdDirectories.size() - 1; index >= 0; index--) {
            Path directory = createdDirectories.get(index);
            try {
                if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) continue;
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                        || Files.isSymbolicLink(directory)) {
                    throw new IllegalStateException(
                            "rollback directory is no longer a real directory: " + directory);
                }
                Files.delete(directory);
            } catch (IOException | RuntimeException rollbackFailure) {
                primary.addSuppressed(rollbackFailure);
            }
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
