// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Slice-1 contract of {@link M3FileTree} and the {@link M3File} entry role (DESIGN.md section 7). */
class M3FileTreeTest {
    private static final List<String> FILES = List.of(
            "a/a1.txt", "a/a2.txt", "a/deep/d1.txt", "a/deep/d2.txt", "a/deep/d3.txt",
            "b/b1.txt", "b/b2.txt", ".hidden", "z.txt", "m.txt", "k.txt", "q.txt");
    private static final int DIRECTORIES = 5; // root, a, a/deep, b, empty
    private static final int ROWS = FILES.size() + DIRECTORIES;

    @TempDir
    Path root;

    @BeforeEach
    void fixture() throws IOException {
        Files.createDirectories(root.resolve("empty"));
        for (int i = 0; i < FILES.size(); i++) {
            Path file = root.resolve(FILES.get(i));
            Files.createDirectories(file.getParent());
            Files.writeString(file, "x".repeat(i + 1), StandardCharsets.US_ASCII);
        }
    }

    @Test
    void scansEveryEntryInPreOrder() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        assertEquals(ROWS, tree.rows());
        assertEquals(ROWS, tree.liveCount());
        assertEquals(0, tree.depth(0));
        assertEquals(-1, tree.parent(0));
        assertEquals(root.getFileName().toString(), tree.name(0));
        for (int id = 1; id < tree.rows(); id++) {
            assertTrue(tree.parent(id) < id, "parent before child at " + id);
            assertEquals(tree.depth(tree.parent(id)) + 1, tree.depth(id));
        }
    }

    @Test
    void childSlicesSortedAndComplete() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        int[] seen = tree.ids().flatMap(tree::childIds).sorted().toArray();
        assertArrayEquals(IntStream.range(1, ROWS).toArray(), seen);
        for (int id = 0; id < tree.rows(); id++) {
            if (!tree.flag(M3FileFlag.DIR, id)) assertEquals(0, tree.childCount(id));
            else assertTrue(tree.flag(M3FileFlag.SCANNED, id));
            for (int i = 1; i < tree.childCount(id); i++) {
                assertTrue(tree.compareNames(tree.childId(id, i - 1), tree.childId(id, i)) < 0);
            }
        }
        List<String> rootNames = tree.rootFile().children().map(M3File::name).toList();
        assertEquals(List.of(".hidden", "a", "b", "empty", "k.txt", "m.txt", "q.txt", "z.txt"), rootNames);
    }

    @Test
    void lanesMatchFileSystem() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        for (int id = 0; id < tree.rows(); id++) {
            Path path = tree.path(id);
            boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
            assertEquals(directory, tree.flag(M3FileFlag.DIR, id), path.toString());
            assertFalse(tree.flag(M3FileFlag.LINK, id));
            if (directory) continue; // directory times may move after the scan; lanes are a point-in-time observation
            assertEquals(Files.size(path), tree.size(id));
            assertEquals(Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis(), tree.mtime(id));
        }
        assertEquals(root, tree.path(0));
        assertEquals(root.resolve("a").resolve("deep").resolve("d2.txt"), tree.path(idOf(tree, "a/deep/d2.txt")));
        assertTrue(tree.flag(M3FileFlag.HIDDEN, idOf(tree, ".hidden")));
        assertFalse(tree.flag(M3FileFlag.HIDDEN, idOf(tree, "a/a1.txt")));
        assertEquals(0, tree.childCount(idOf(tree, "empty")));
    }

    @Test
    void entryFacadeReadsLanesAndBytes() throws IOException {
        M3FileTree tree = M3FileTree.open(root);
        M3File d1 = tree.file(idOf(tree, "a/deep/d1.txt"));
        assertTrue(d1.isTreeEntry());
        assertEquals("d1.txt", d1.name());
        assertEquals(3, d1.depth());
        assertEquals("deep", d1.parent().orElseThrow().name());
        assertEquals("a", d1.parent().orElseThrow().parent().orElseThrow().name());
        assertTrue(tree.rootFile().parent().isEmpty());
        assertFalse(d1.isDirectory());
        assertEquals(3, d1.size());
        assertEquals("xxx", new String(d1.readAllBytes(16), StandardCharsets.US_ASCII));
        assertEquals(3, d1.metadata().size());
        M3File a = tree.file(idOf(tree, "a"));
        assertTrue(a.isDirectory());
        assertEquals(3, a.childCount());
        assertEquals(List.of("a1.txt", "a2.txt", "deep"), a.children().map(M3File::name).toList());
    }

    @Test
    void residencyIsBoundedAndIdentityIsTheRow() throws IOException {
        M3FileTree tree = M3FileTree.open(root, 2);
        M3File first = tree.file(1);
        assertSame(first, tree.file(1));
        tree.file(0);
        tree.file(3); // slot 1: evicts row 1
        assertEquals(2, tree.residentCount());
        assertFalse(tree.flag(M3FileFlag.RESIDENT, 1));
        assertTrue(tree.flag(M3FileFlag.RESIDENT, 3));
        assertEquals(2, tree.ids().filter(id -> tree.flag(M3FileFlag.RESIDENT, id)).count());
        M3File again = tree.file(1);
        assertNotSame(first, again);
        assertEquals(first, again);
        assertEquals(first.hashCode(), again.hashCode());
        assertFalse(tree.flag(M3FileFlag.RESIDENT, 3));
    }

    @Test
    void detachedEdgeIsUnchanged() throws IOException {
        Path z = root.resolve("z.txt");
        M3File detached = M3File.of(z);
        assertFalse(detached.isTreeEntry());
        assertEquals(-1, detached.id());
        assertSame(z, detached.path());
        assertEquals("x".repeat(9), new String(detached.readAllBytes(64), StandardCharsets.US_ASCII));
        assertThrows(IOException.class, () -> detached.readAllBytes(4));
        assertThrows(IllegalStateException.class, detached::name);
        assertNotEquals(M3File.of(z), detached, "the original detached edge uses Object identity");
        assertEquals(0, M3FileTree.detached().rows());
    }

    private int idOf(M3FileTree tree, String relative) {
        Path target = root.resolve(relative);
        return tree.ids().filter(id -> tree.path(id).equals(target)).findFirst().orElseThrow();
    }
}
