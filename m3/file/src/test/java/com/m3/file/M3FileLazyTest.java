// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S8 contract: lazy scans splice expanded directories into pre-order with stable ids. */
class M3FileLazyTest {
    @TempDir
    Path root;

    @BeforeEach
    void fixture() throws IOException {
        for (String branch : List.of("a", "b", "c")) {
            for (int i = 0; i < 3; i++) {
                write(root.resolve(branch + "/f" + i + ".txt"));
                write(root.resolve(branch + "/d" + i + "/g.txt"));
                write(root.resolve(branch + "/d" + i + "/e/h" + i + ".bin"));
            }
        }
        write(root.resolve("top.txt"));
        Files.createDirectories(root.resolve("empty"));
    }

    @Test
    void expandingEveryDirectoryInAnyOrderEqualsTheEagerScan() throws IOException {
        for (long seed = 1; seed <= 5; seed++) {
            M3FileTree tree = M3FileTree.open(root, M3FileScanSpec.lazy(1));
            assertEquals(filesWalk(1).size(), tree.rows());
            Random random = new Random(seed);
            for (int[] open = unscanned(tree); open.length > 0; open = unscanned(tree)) {
                int id = open[random.nextInt(open.length)];
                int before = tree.rows();
                assertTrue(tree.scanChildren(id) >= 0);
                assertTrue(tree.flag(M3FileFlag.SCANNED, id));
                assertEquals(0, tree.scanChildren(id), "scanned directories are never rescanned");
                assertEquals(before + tree.childCount(id), tree.rows(), "one level per expansion");
                assertInvariants(tree);
            }
            assertEquals(filesWalk(Integer.MAX_VALUE), tree.walk().paths().toList(), "seed " + seed);
            M3FileTree eager = M3FileTree.open(root);
            assertEquals(eager.walk().prune(f -> f.name().equals("e")).paths().toList(),
                    tree.walk().prune(f -> f.name().equals("e")).paths().toList());
            assertEquals(tree.walk().paths().toList(), tree.walk().parallelStream().map(M3File::toPath).toList());
            assertEquals(eager.walk().glob("**/*.bin").paths().toList(), tree.walk().glob("**/*.bin").paths().toList());
        }
    }

    @Test
    void idsAndFacadesSurviveExpansion() throws IOException {
        M3FileTree tree = M3FileTree.open(root, M3FileScanSpec.lazy(1));
        int top = tree.ids().filter(id -> tree.name(id).equals("top.txt")).findFirst().orElseThrow();
        M3File file = tree.file(top);
        Path path = file.toPath();
        int[] orderBefore = tree.ids().toArray();
        int a = tree.ids().filter(id -> tree.name(id).equals("a")).findFirst().orElseThrow();
        int expandedPosition = tree.rank(a);
        int generation = tree.generation();
        int added = tree.scanChildren(a);
        assertTrue(added > 0);
        assertEquals(generation + 1, tree.generation());
        assertEquals(path, tree.file(top).toPath());
        assertEquals(file, tree.file(top));
        for (int position = 0; position < orderBefore.length; position++) {
            int expected = position + (position > expandedPosition ? added : 0);
            assertEquals(expected, tree.rank(orderBefore[position]), "only positions after the expansion shift");
        }
        assertEquals(0, tree.scanChildren(top), "files have no children");
    }

    @Test
    void depthPerScanAndRootOnly() throws IOException {
        M3FileTree two = M3FileTree.open(root, M3FileScanSpec.lazy(2));
        assertEquals(filesWalk(2), two.walk().paths().toList());
        int d0 = two.ids().filter(id -> two.path(id).equals(root.resolve("a/d0"))).findFirst().orElseThrow();
        assertFalse(two.flag(M3FileFlag.SCANNED, d0));
        assertEquals(3, two.scanChildren(d0), "g.txt and e, then e's child at the second level");
        M3FileTree rootOnly = M3FileTree.open(root, M3FileScanSpec.lazy(0));
        assertEquals(1, rootOnly.rows());
        assertFalse(rootOnly.flag(M3FileFlag.SCANNED, 0));
        assertEquals(filesWalk(1).size() - 1, rootOnly.scanChildren(0));
        assertEquals(filesWalk(1), rootOnly.walk().paths().toList());
        assertThrows(IllegalArgumentException.class, () -> new M3FileScanSpec(-1, 4));
        assertThrows(IllegalArgumentException.class, () -> new M3FileScanSpec(1, 3));
    }

    @Test
    void traversalsFailFastAcrossExpansion() throws IOException {
        M3FileTree tree = M3FileTree.open(root, M3FileScanSpec.lazy(1));
        Spliterator.OfInt ids = tree.walk().idSpliterator();
        M3FileCursor cursor = tree.walk().cursor();
        assertTrue(ids.tryAdvance((IntConsumer) id -> { }));
        assertTrue(cursor.nextId() >= 0);
        tree.scanChildren(tree.ids().filter(id -> tree.name(id).equals("b")).findFirst().orElseThrow());
        assertThrows(ConcurrentModificationException.class, () -> ids.tryAdvance((IntConsumer) id -> { }));
        assertThrows(ConcurrentModificationException.class, cursor::nextId);
        assertEquals(tree.rows(), tree.walk().count());
    }

    private static void assertInvariants(M3FileTree tree) {
        for (int position = 0; position < tree.rows(); position++) assertEquals(position, tree.rank(tree.idAt(position)));
        for (int id = 1; id < tree.rows(); id++) {
            assertTrue(tree.parent(id) < id);
            assertEquals(tree.depth(tree.parent(id)) + 1, tree.depth(id));
            assertTrue(tree.rank(tree.parent(id)) < tree.rank(id));
        }
        for (int id = 0; id < tree.rows(); id++) {
            Set<Integer> range = new HashSet<>();
            for (int p = tree.rank(id); p < tree.subtreeEnd(id); p++) range.add(tree.idAt(p));
            Set<Integer> subtree = new HashSet<>();
            for (int other = 0; other < tree.rows(); other++) if (descends(tree, other, id)) subtree.add(other);
            assertEquals(subtree, range, "subtree range of " + tree.name(id));
            for (int i = 1; i < tree.childCount(id); i++) {
                assertTrue(tree.compareNames(tree.childId(id, i - 1), tree.childId(id, i)) < 0);
            }
        }
    }

    private static boolean descends(M3FileTree tree, int id, int ancestor) {
        for (int at = id; at >= 0; at = tree.parent(at)) if (at == ancestor) return true;
        return false;
    }

    private static int[] unscanned(M3FileTree tree) {
        return tree.ids().filter(id -> tree.flag(M3FileFlag.DIR, id) && !tree.flag(M3FileFlag.SCANNED, id)).toArray();
    }

    private List<Path> filesWalk(int depth) throws IOException {
        try (Stream<Path> paths = Files.walk(root, depth)) {
            return paths.toList();
        }
    }

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, file.getFileName().toString(), StandardCharsets.UTF_8);
    }
}
