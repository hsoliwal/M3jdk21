// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

/** Fail-closed filesystem admission shared by runtime assets and production local media. */
final class ChromeMlPathPolicy {
    record FileState(
            Path realPath,
            long size,
            FileTime lastModified,
            Object fileKey) {
        FileState {
            realPath = Objects.requireNonNull(realPath, "realPath");
            lastModified = Objects.requireNonNull(lastModified, "lastModified");
        }
    }

    private ChromeMlPathPolicy() {}

    static Path requireDirectoryNoLinks(Path path) throws IOException {
        Path absolute = normalized(path, "directory");
        rejectSymlinkAncestry(absolute);
        if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(absolute)) {
            throw new IOException("DIRECTORY_REQUIRED:" + absolute);
        }
        return absolute.toRealPath(LinkOption.NOFOLLOW_LINKS);
    }

    static FileState inspectRegular(Path path, long maxBytes) throws IOException {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes");
        }
        Path absolute = normalized(path, "file");
        rejectSymlinkAncestry(absolute);
        BasicFileAttributes attributes =
                Files.readAttributes(
                        absolute,
                        BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || Files.isSymbolicLink(absolute)) {
            throw new IOException("REGULAR_FILE_REQUIRED:" + absolute);
        }
        if (attributes.size() < 0 || attributes.size() > maxBytes) {
            throw new IOException("FILE_BYTE_LIMIT:" + absolute);
        }
        return new FileState(
                absolute.toRealPath(LinkOption.NOFOLLOW_LINKS),
                attributes.size(),
                attributes.lastModifiedTime(),
                attributes.fileKey());
    }

    static Path requireRegularInside(Path root, Path candidate, long maxBytes)
            throws IOException {
        Path rootReal = requireDirectoryNoLinks(root);
        Path requested = Objects.requireNonNull(candidate, "candidate");
        if (!requested.isAbsolute()) {
            requested = rootReal.resolve(requested);
        }
        requested = requested.toAbsolutePath().normalize();
        if (!requested.startsWith(rootReal)) {
            throw new IOException("PATH_OUTSIDE_MEDIA_ROOT:" + requested);
        }
        FileState state = inspectRegular(requested, maxBytes);
        if (!state.realPath().startsWith(rootReal)) {
            throw new IOException("PATH_ESCAPED_MEDIA_ROOT:" + state.realPath());
        }
        return state.realPath();
    }

    static void requireUnchanged(FileState before, FileState after) throws IOException {
        if (!before.realPath().equals(after.realPath())
                || before.size() != after.size()
                || !before.lastModified().equals(after.lastModified())
                || !Objects.equals(before.fileKey(), after.fileKey())) {
            throw new IOException("FILE_CHANGED_DURING_READ:" + before.realPath());
        }
    }

    static void rejectSymlinkAncestry(Path path) throws IOException {
        Path absolute = normalized(path, "path");
        Path current = absolute.getRoot();
        if (current == null) {
            throw new IOException("ABSOLUTE_PATH_REQUIRED:" + absolute);
        }
        for (Path component : absolute) {
            current = current.resolve(component);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther()) {
                throw new IOException("SPECIAL_PATH_REJECTED:" + current);
            }
            if (Files.isSymbolicLink(current)) {
                throw new IOException("SYMLINK_PATH_REJECTED:" + current);
            }
        }
    }

    private static Path normalized(Path value, String name) {
        return Objects.requireNonNull(value, name).toAbsolutePath().normalize();
    }
}
