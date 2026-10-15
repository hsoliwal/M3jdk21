// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * File-local A3 evidence writer that never truncates an existing output inode.
 *
 * <p>A3Fs owns the lexical m3/build boundary. This atom additionally checks every output
 * directory without following symlinks, refuses symlink/irregular destinations, and publishes
 * a fully written sibling temporary file by rename. It is not a security claim against an
 * actor concurrently replacing directories after validation.</p>
 */
final class A3OutputFence {
    private A3OutputFence() {}

    static void write(Path root, Path requested, String content) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path target = A3Fs.out(checkedRoot, requested);
        Objects.requireNonNull(content, "content");
        ensureParents(checkedRoot, target.getParent());
        requireOrdinaryTarget(target);

        Path temporary = Files.createTempFile(target.getParent(), ".m3-a3-", ".tmp");
        try {
            Files.writeString(
                    temporary,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            requireOrdinaryTarget(target);
            publish(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void ensureParents(Path root, Path parent) throws IOException {
        requireDirectory(root);
        Path current = root;
        for (Path component : root.relativize(parent)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("A3 output directory is a symlink: " + current);
            }
            if (Files.notExists(current, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectory(current);
                } catch (FileAlreadyExistsException concurrentCreation) {
                    if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                        throw concurrentCreation;
                    }
                }
            }
            requireDirectory(current);
        }
    }

    private static void requireDirectory(Path path) {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("A3 output parent is not a plain directory: " + path);
        }
    }

    private static void requireOrdinaryTarget(Path target) {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("A3 output target is not a plain file: " + target);
        }
    }

    private static void publish(Path temporary, Path target) throws IOException {
        try {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException unsupported) {
            requireOrdinaryTarget(target);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
