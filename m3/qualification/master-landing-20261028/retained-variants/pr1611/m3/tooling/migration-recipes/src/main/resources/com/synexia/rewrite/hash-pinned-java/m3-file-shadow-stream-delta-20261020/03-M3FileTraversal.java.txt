// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazy, provider-preserving filesystem traversal that materializes no retained directory tree.
 *
 * <p>Roots (drives/mount roots) are independent streams and can be traversed in parallel.
 * Downstream file work may also use {@link Stream#parallel()}; provider-specific directory walking
 * remains owned by {@link Files#walk(Path, int, java.nio.file.FileVisitOption...)}.</p>
 */
public final class M3FileTraversal {
    private M3FileTraversal() { }

    public record Options(int maxDepth, boolean parallelRoots, Predicate<Path> include) {
        public Options {
            if (maxDepth < 0) throw new IllegalArgumentException("negative maxDepth");
            include = Objects.requireNonNull(include, "include");
        }

        public static Options all() {
            return new Options(Integer.MAX_VALUE, false, path -> true);
        }

        public static Options suffixes(String... suffixes) {
            String[] copy = Objects.requireNonNull(suffixes, "suffixes").clone();
            for (int i = 0; i < copy.length; i++) {
                String suffix = Objects.requireNonNull(copy[i], "suffix").strip();
                if (suffix.isEmpty() || suffix.indexOf('/') >= 0 || suffix.indexOf('\0') >= 0) {
                    throw new IllegalArgumentException("invalid suffix");
                }
                copy[i] = suffix;
            }
            return new Options(
                    Integer.MAX_VALUE,
                    false,
                    path -> {
                        if (!Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                            return false;
                        }
                        String name = path.getFileName() == null ? "" : path.getFileName().toString();
                        for (String suffix : copy) if (name.endsWith(suffix)) return true;
                        return false;
                    });
        }

        public Options withParallelRoots(boolean parallel) {
            return new Options(maxDepth, parallel, include);
        }

        public Options withMaxDepth(int depth) {
            return new Options(depth, parallelRoots, include);
        }
    }

    public static Stream<M3File> system(Options options, M3ProgressMonitor progress) {
        return roots(FileSystems.getDefault(), options, progress);
    }

    public static Stream<M3File> roots(
            FileSystem fileSystem, Options options, M3ProgressMonitor progress) {
        Objects.requireNonNull(fileSystem, "fileSystem");
        return roots(
                StreamSupport.stream(fileSystem.getRootDirectories().spliterator(), false),
                options,
                progress);
    }

    public static Stream<M3File> roots(
            Stream<Path> roots, Options options, M3ProgressMonitor progress) {
        Stream<Path> rootStream = Objects.requireNonNull(roots, "roots");
        Options checked = Objects.requireNonNull(options, "options");
        M3ProgressMonitor monitor = Objects.requireNonNull(progress, "progress");
        if (checked.parallelRoots()) rootStream = rootStream.parallel();

        return rootStream.flatMap(root -> walk(root, checked, monitor));
    }

    /**
     * Immediate children only. This is the directory-level partition boundary for schedulers that
     * want to process independent directories/files concurrently without retaining a tree.
     */
    public static Stream<M3File> children(
            Path directory, Options options, M3ProgressMonitor progress) {
        Path parent = Objects.requireNonNull(directory, "directory");
        Options checked = Objects.requireNonNull(options, "options");
        M3ProgressMonitor monitor = Objects.requireNonNull(progress, "progress");
        monitor.checkCanceled();
        try {
            return Files.list(parent)
                    .peek(ignored -> {
                        monitor.worked(1L);
                        monitor.checkCanceled();
                    })
                    .filter(checked.include())
                    .map(M3File::of);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static Stream<M3File> walk(
            Path root, Options options, M3ProgressMonitor progress) {
        Path start = Objects.requireNonNull(root, "root");
        Options checked = Objects.requireNonNull(options, "options");
        M3ProgressMonitor monitor = Objects.requireNonNull(progress, "progress");
        monitor.checkCanceled();
        try {
            return Files.walk(start, checked.maxDepth())
                    .peek(ignored -> {
                        monitor.worked(1L);
                        monitor.checkCanceled();
                    })
                    .filter(checked.include())
                    .map(M3File::of);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /** Convenience for several known roots without retaining or sorting their contents. */
    public static Stream<M3File> roots(
            List<Path> roots, Options options, M3ProgressMonitor progress) {
        return roots(Objects.requireNonNull(roots, "roots").stream(), options, progress);
    }
}
