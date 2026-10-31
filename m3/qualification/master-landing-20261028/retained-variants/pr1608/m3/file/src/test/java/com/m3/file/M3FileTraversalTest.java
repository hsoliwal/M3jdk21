// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Executable provider-preserving, lazy traversal contract (no test stubs). */
public final class M3FileTraversalTest {
    private M3FileTraversalTest() { }
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    private static void exercise(Path root) throws Exception {
        Files.createDirectories(root.resolve("nested/deeper"));
        Files.writeString(root.resolve("first.txt"), "text");
        Files.writeString(root.resolve("nested/data.json"), "{}");
        Files.write(root.resolve("nested/deeper/raw.bin"), new byte[]{1});
        Files.writeString(root.resolve("ignored.log"), "log");
        AtomicInteger examined = new AtomicInteger();
        M3File owner = M3File.of(root);
        try (var stream = owner.walk(8, p -> { examined.incrementAndGet(); return true; })) {
            check(examined.get() == 0);
            check(stream.findFirst().orElseThrow().path().equals(root));
            check(examined.get() == 1);
        }
        Predicate<Path> extensions = p -> p.toString().endsWith(".txt")
                || p.toString().endsWith(".json") || p.toString().endsWith(".bin");
        try (var stream = owner.walk(8, extensions)) {
            var selected = stream.filter(f -> !f.path().toString().endsWith(".txt")).toList();
            check(selected.size() == 2);
            for (M3File file : selected) check(file.path().getFileSystem() == root.getFileSystem());
        }
        try (var stream = owner.walk(1, extensions)) {
            check(stream.count() == 1);
        }
        try (var stream = owner.walk(0, p -> true)) { check(stream.count() == 1); }
        List<Path> expected;
        try (var stream = Files.walk(root, 8)) { expected = stream.toList(); }
        try (var stream = owner.walk(8, p -> true)) {
            check(stream.map(M3File::path).toList().equals(expected));
        }
        try { owner.walk(-1, p -> true); throw new AssertionError(); }
        catch (IllegalArgumentException expectedFailure) { checks++; }
        try { owner.walk(1, null); throw new AssertionError(); }
        catch (NullPointerException expectedFailure) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3-lazy-traversal");
        Path archive = dir.resolve("provider.zip");
        try {
            Path nativeRoot = Files.createDirectory(dir.resolve("native"));
            exercise(nativeRoot);
            Path outside = Files.createDirectories(dir.resolve("outside"));
            Files.writeString(outside.resolve("hidden.json"), "{}");
            Files.createSymbolicLink(nativeRoot.resolve("link"), outside);
            try (var stream = M3File.of(nativeRoot).walk(8, p -> p.toString().endsWith("hidden.json"))) {
                check(stream.count() == 0);
            }
            try (FileSystem zip = FileSystems.newFileSystem(archive, Map.of("create", "true"))) {
                exercise(zip.getPath("/"));
            }
            System.out.println("M3FileTraversalTest checks=" + checks);
        } finally {
            try (var stream = Files.walk(dir)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
