// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Verifies and optionally materializes one sealed Synexia Apache delivery into M3JDK21. */
public final class SynexiaImporter {
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
        verify(synexiaRoot, m3jdkRoot, manifest);
        Path sourceRoot = root(synexiaRoot, "synexiaRoot");
        Path targetRoot = root(m3jdkRoot, "m3jdkRoot");

        for (SynexiaImportManifest.Entry entry : manifest.entries()) {
            Path source = resolve(sourceRoot, entry.sourcePath(), "source");
            Path target = resolve(targetRoot, entry.targetPath(), "target");
            if (Files.exists(target)) continue;
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = Files.createTempFile(
                    parent == null ? targetRoot : parent,
                    target.getFileName().toString(),
                    ".tmp");
            boolean moved = false;
            try {
                Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
                String copied = sha256(Files.readAllBytes(temporary));
                if (!copied.equals(entry.sha256())) {
                    throw new IllegalStateException("copied Synexia file hash mismatch: " + entry.targetPath());
                }
                try {
                    Files.move(
                            temporary,
                            target,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, target);
                }
                moved = true;
            } finally {
                if (!moved) Files.deleteIfExists(temporary);
            }
        }
        verify(synexiaRoot, m3jdkRoot, manifest);
    }

    private static Path root(Path value, String field) {
        Path root = Objects.requireNonNull(value, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException(field + " must be an existing directory");
        }
        return root;
    }

    private static Path resolve(Path root, String relative, String field) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(field + " path escapes root");
        }
        return resolved;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
