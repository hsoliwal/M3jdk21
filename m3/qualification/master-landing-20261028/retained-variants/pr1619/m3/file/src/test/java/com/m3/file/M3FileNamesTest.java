// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S4 contract: interned name segments and type ids keep the tree small and unchanged in behaviour. */
class M3FileNamesTest {
    @TempDir
    Path root;

    @Test
    void repeatedSegmentsAreStoredOnce() throws IOException {
        for (int module = 0; module < 40; module++) {
            Path main = root.resolve("module" + module).resolve("src").resolve("main").resolve("java");
            Files.createDirectories(main);
            Files.writeString(main.getParent().getParent().getParent().resolve("pom.xml"), "<project/>");
            Files.writeString(main.resolve("App.java"), "class App {}");
        }
        M3FileTree tree = M3FileTree.open(root);
        Set<String> distinct = new HashSet<>();
        long chars = 0;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : paths.toList()) {
                if (distinct.add(p.getFileName().toString())) chars += p.getFileName().toString().length();
            }
        }
        assertEquals(40 * 6 + 1, tree.rows());
        assertEquals(distinct.size(), tree.names().count());
        assertEquals(chars, tree.names().poolChars());
        int src = tree.names().find("src");
        assertTrue(src >= 0);
        assertEquals(40, tree.ids().filter(id -> tree.nameId(id) == src).count());
        tree.ids().forEach(id -> assertEquals(tree.path(id).getFileName().toString(), tree.name(id)));
        assertEquals(-1, tree.names().find("missing"));
    }

    @Test
    void internerGrowsAndOrdersLikeString() {
        M3FileNames names = new M3FileNames(4);
        for (int i = 0; i < 10_000; i++) assertEquals(i, names.intern("n" + i));
        for (int i = 0; i < 10_000; i++) {
            assertEquals(i, names.intern("n" + i));
            assertEquals(i, names.find(new StringBuilder("n").append(i)));
        }
        assertEquals(10_000, names.count());
        List<String> words = List.of("", "a", "B", "ab", "é", "😀", "Aa", "BB");
        for (String w : words) names.intern(w);
        for (String a : words) {
            for (String b : words) {
                assertEquals(Integer.signum(a.compareTo(b)), Integer.signum(names.compare(names.find(a), names.find(b))), a + " vs " + b);
            }
        }
        assertEquals("Aa".hashCode(), "BB".hashCode());
        assertNotEquals(names.find("Aa"), names.find("BB"));
        assertEquals("BB", names.name(names.find("BB")));
    }

    @Test
    void typeIdsByExtension() throws IOException {
        for (String name : List.of("a.TXT", "b.txt", ".hidden", "archive.tar.gz", "noext", "x.Json", "y.bin")) {
            Files.writeString(root.resolve(name), name);
        }
        Files.createDirectories(root.resolve("dir.txt"));
        M3FileTree tree = M3FileTree.open(root);
        for (int id = 0; id < tree.rows(); id++) {
            String expected = tree.flag(M3FileFlag.DIR, id) ? "" : extension(tree.name(id));
            assertEquals(expected, tree.type(id), tree.name(id));
        }
        assertEquals(tree.typeId(idOf(tree, "a.TXT")), tree.typeId(idOf(tree, "b.txt")));
        assertEquals(0, tree.typeId(idOf(tree, ".hidden")));
        assertEquals(0, tree.typeId(idOf(tree, "dir.txt")));
        assertEquals("gz", tree.file(idOf(tree, "archive.tar.gz")).type());
        assertEquals(List.of("a.TXT", "b.txt"), names(tree.walk().types("txt")));
        assertEquals(List.of("a.TXT", "b.txt", "x.Json"), names(tree.walk().types(".TXT", "json")));
        assertEquals(List.of(), names(tree.walk().types("nope")));
        assertEquals(List.of(), names(tree.walk().types("txt").types("json")));
        assertEquals(List.of("x.Json"), names(tree.walk().types("txt", "json").types("json")));
        List<String> none = names(tree.walk().types("").kinds(M3FileKind.FILE));
        assertEquals(List.of(".hidden", "noext"), none);
        assertEquals("", M3FileTree.extension("trailing."));
        assertEquals("", M3FileTree.extension(".."));
        assertEquals("gz", M3FileTree.extension("a.tar.GZ"));
    }

    private static List<String> names(M3FileWalk walk) {
        return walk.map(M3File::name).sorted().toList();
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private int idOf(M3FileTree tree, String relative) {
        Path target = root.resolve(relative);
        return tree.ids().filter(id -> tree.path(id).equals(target)).findFirst().orElseThrow();
    }
}
