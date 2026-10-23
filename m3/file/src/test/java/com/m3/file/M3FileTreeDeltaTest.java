// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S6 differential contract: {@link M3FileTreeDelta} against a brute-force map diff of two snapshots. */
class M3FileTreeDeltaTest {
    private static final int COMPARED = M3FileChange.ADDED | M3FileChange.REMOVED | M3FileChange.CONTENT_CHANGED
            | M3FileChange.SIZE | M3FileChange.MTIME | M3FileChange.KIND;

    @TempDir
    Path root;

    @BeforeEach
    void fixture() throws IOException {
        for (String file : List.of("keep.txt", "edit.txt", "touch.txt", "gone.txt", "olddir/x.txt", "olddir/sub/y.txt",
                "src/main/A.java", "src/main/B.java", "flip", "space.txt")) {
            write(root.resolve(file), file);
        }
    }

    @Test
    void deltaEqualsBruteForceDiff() throws Exception {
        M3FileTree before = M3FileTree.open(root);
        before.hash(before.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
        Map<String, Snapshot> old = snapshot(root);
        rewrite(root.resolve("edit.txt"), "edited content");
        Path touch = root.resolve("touch.txt");
        Files.setLastModifiedTime(touch, FileTime.fromMillis(Files.getLastModifiedTime(touch).toMillis() + 9_000));
        Files.delete(root.resolve("gone.txt"));
        try (Stream<Path> doomed = Files.walk(root.resolve("olddir"))) {
            for (Path p : doomed.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
        write(root.resolve("newdir/n1.txt"), "n1");
        write(root.resolve("newdir/deep/n2.txt"), "n2");
        write(root.resolve("src/main/C.java"), "class C {}");
        Files.delete(root.resolve("flip"));
        write(root.resolve("flip/inside.txt"), "now a directory");
        rewrite(root.resolve("space.txt"), "space . txt");
        M3FileTree after = M3FileTree.open(root);
        Map<String, Snapshot> now = snapshot(root);

        M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 1 << 20, M3FileProgress.NONE,
                M3FileChangeProvider.ATTRIBUTES);
        Map<String, Integer> actual = new TreeMap<>();
        delta.stream().forEach(change -> actual.put(relative(change.path()), change.bits() & COMPARED));
        actual.values().removeIf(bits -> bits == 0);
        assertEquals(expected(old, now), actual);
        List<String> order = delta.stream().map(change -> relative(change.path())).toList();
        assertEquals(order.stream().sorted(M3FileTreeDeltaTest::preOrder).toList(), order, "name-sorted pre-order");
        assertEquals(new TreeSet<>(List.of("newdir", "newdir/deep", "newdir/deep/n2.txt", "newdir/n1.txt",
                "src/main/C.java", "flip/inside.txt")), relatives(delta.paths(M3FileChange.ADDED)));
        assertEquals(new TreeSet<>(List.of("gone.txt", "olddir", "olddir/sub", "olddir/sub/y.txt", "olddir/x.txt")),
                relatives(delta.paths(M3FileChange.REMOVED)));
        M3FileChange flip = delta.stream().filter(c -> relative(c.path()).equals("flip")).findFirst().orElseThrow();
        assertTrue(flip.has(M3FileChange.KIND));
        assertEquals(M3FileKind.FILE, flip.before().orElseThrow().kind());
        assertEquals(M3FileKind.DIRECTORY, flip.after().orElseThrow().kind());
        M3FileChange spaced = delta.stream().filter(c -> relative(c.path()).equals("space.txt")).findFirst().orElseThrow();
        assertTrue(spaced.has(M3FileChange.CONTENT_CHANGED) && !spaced.has(M3FileChange.TOKENS), "whitespace-only edit");
        assertTrue(delta.rows(M3FileChange.ADDED).allMatch(row -> !after.flag(M3FileFlag.REGULAR, delta.afterId(row))
                || after.flag(M3FileFlag.HASHED, delta.afterId(row))), "added files are observed once");
    }

    @Test
    void secondDeltaWithoutEditsIsEmptyAndReadsNothing() throws Exception {
        M3FileTree first = M3FileTree.open(root);
        first.hash(first.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
        M3FileTree second = M3FileTree.open(root);
        AtomicLong read = new AtomicLong();
        M3FileTreeDelta none = M3FileTreeDelta.between(first, second, 1 << 20,
                M3FileProgress.of(read::addAndGet, () -> false), M3FileChangeProvider.ATTRIBUTES);
        assertTrue(none.isEmpty(), () -> none.stream().map(M3FileChange::toString).toList().toString());
        assertEquals(0, read.get());
        assertEquals(first.walk().kinds(M3FileKind.FILE).count(),
                second.walk().filter(f -> f.flag(M3FileFlag.HASHED)).count(), "observations carried forward");
        rewrite(root.resolve("src/main/A.java"), "class A { /* edited */ }");
        M3FileTree third = M3FileTree.open(root);
        M3FileTreeDelta one = M3FileTreeDelta.between(second, third, 1 << 20,
                M3FileProgress.of(read::addAndGet, () -> false), M3FileChangeProvider.ATTRIBUTES);
        List<String> changedFiles = new ArrayList<>();
        one.stream().filter(c -> c.entry().isRegularFile()).forEach(c -> changedFiles.add(relative(c.path())));
        assertEquals(List.of("src/main/A.java"), changedFiles);
        assertEquals(Files.size(root.resolve("src/main/A.java")), read.get(), "only the tripped gate is read");
    }

    private static Map<String, Integer> expected(Map<String, Snapshot> old, Map<String, Snapshot> now) {
        Map<String, Integer> out = new TreeMap<>();
        for (String path : old.keySet()) if (!now.containsKey(path)) out.put(path, M3FileChange.REMOVED);
        for (Map.Entry<String, Snapshot> entry : now.entrySet()) {
            Snapshot was = old.get(entry.getKey());
            Snapshot is = entry.getValue();
            if (was == null) {
                out.put(entry.getKey(), M3FileChange.ADDED);
                continue;
            }
            int bits = 0;
            if (was.kind != is.kind) bits |= M3FileChange.KIND;
            if (was.size != is.size) bits |= M3FileChange.SIZE;
            if (was.mtime != is.mtime) bits |= M3FileChange.MTIME;
            boolean bothFiles = was.kind == M3FileKind.FILE && is.kind == M3FileKind.FILE;
            if (bothFiles && bits != 0 && !was.sha.equals(is.sha)) bits |= M3FileChange.CONTENT_CHANGED;
            if (bits != 0) out.put(entry.getKey(), bits);
        }
        return out;
    }

    private record Snapshot(M3FileKind kind, long size, long mtime, String sha) {
    }

    private Map<String, Snapshot> snapshot(Path start) throws Exception {
        Map<String, Snapshot> out = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(start)) {
            for (Path p : paths.toList()) {
                BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                String sha = a.isRegularFile()
                        ? HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))) : "";
                out.put(relative(p), new Snapshot(M3FileKind.of(a), a.size(), a.lastModifiedTime().toMillis(), sha));
            }
        }
        return out;
    }

    private TreeSet<String> relatives(Stream<Path> paths) {
        TreeSet<String> out = new TreeSet<>();
        paths.forEach(p -> out.add(relative(p)));
        return out;
    }

    private String relative(Path p) {
        return root.relativize(p).toString().replace('\\', '/');
    }

    private static int preOrder(String a, String b) {
        String[] left = a.isEmpty() ? new String[0] : a.split("/");
        String[] right = b.isEmpty() ? new String[0] : b.split("/");
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            int order = left[i].compareTo(right[i]);
            if (order != 0) return order;
        }
        return left.length - right.length;
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static void rewrite(Path file, String text) throws IOException {
        FileTime previous = Files.getLastModifiedTime(file);
        Files.writeString(file, text, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(file, FileTime.fromMillis(previous.toMillis() + 7_000));
    }
}
