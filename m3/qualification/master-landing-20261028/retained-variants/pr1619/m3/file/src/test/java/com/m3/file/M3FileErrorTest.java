// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S9b contract: error lane, limit exceptions, archives expanded below directory roots. */
class M3FileErrorTest {
    @TempDir
    Path root;

    @Test
    void errorLaneRecordsAndClearsFailures() throws Exception {
        write("keep.txt", "keep");
        write("gone.txt", "gone");
        write("big.txt", "x".repeat(100));
        M3FileTree tree = M3FileTree.open(root);
        Files.delete(root.resolve("gone.txt"));
        assertEquals(1, tree.hash(tree.walk(), 50, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES), "keep only");
        M3File gone = file(tree, "gone.txt");
        assertTrue(gone.flag(M3FileFlag.FAILED));
        assertEquals(M3FileError.NOT_FOUND, gone.error());
        assertEquals(M3FileError.LIMIT, file(tree, "big.txt").error());
        assertEquals(M3FileError.NONE, file(tree, "keep.txt").error());
        write("gone.txt", "back");
        assertEquals(3, tree.hash(tree.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES));
        assertEquals(M3FileError.NONE, gone.error());
        assertFalse(gone.flag(M3FileFlag.FAILED));
        Files.delete(root.resolve("keep.txt"));
        M3FileWordCount wc = tree.walk().wc(1 << 20, M3FileProgress.NONE);
        assertEquals(1, wc.failed());
        assertEquals(M3FileError.NOT_FOUND, file(tree, "keep.txt").error());
    }

    @Test
    void limitsThrowTheLimitExceptionAndFailuresClassify() throws Exception {
        Path big = write("big.txt", "x".repeat(100));
        assertThrows(M3FileLimitException.class, () -> M3File.of(big).readAllBytes(10));
        assertThrows(M3FileLimitException.class, () -> M3File.of(big).openInputStream(10, M3FileProgress.NONE));
        try (InputStream in = new M3FileShadowInputStream(new ByteArrayInputStream(new byte[20]), 5, M3FileProgress.NONE)) {
            assertThrows(M3FileLimitException.class, in::readAllBytes);
        }
        assertEquals(M3FileError.NOT_FOUND, M3FileError.of(new NoSuchFileException("x")));
        assertEquals(M3FileError.ACCESS_DENIED, M3FileError.of(new AccessDeniedException("x")));
        assertEquals(M3FileError.LIMIT, M3FileError.of(new M3FileLimitException("x")));
        assertEquals(M3FileError.CORRUPT, M3FileError.of(new ZipException("x")));
        assertEquals(M3FileError.LOOP, M3FileError.of(new FileSystemLoopException("x")));
        assertEquals(M3FileError.IO, M3FileError.of(new IOException("x")));
        Files.write(root.resolve("bad.zip"), new byte[] {'P', 'K', 3, 4, 9, 9, 9});
        assertThrows(IOException.class, () -> M3FileTree.open(root.resolve("bad.zip")));
    }

    @Test
    void archivesBelowADirectoryRootBecomeSubTrees() throws Exception {
        Map<String, byte[]> inner = new LinkedHashMap<>();
        inner.put("p/Q.class", bytes("class Q"));
        Map<String, byte[]> outer = new LinkedHashMap<>();
        outer.put("com/A.class", bytes("class A"));
        outer.put("lib/inner.jar", zip(inner));
        outer.put("README", bytes("readme"));
        Path jar = Files.write(Files.createDirectories(root.resolve("lib")).resolve("app.jar"), zip(outer));
        write("lib/fake.jar", "not a zip");
        write("src/Main.java", "class Main {}");
        M3FileScanSpec archives = M3FileScanSpec.EAGER.withArchives();
        try (M3FileTree tree = M3FileTree.open(root, archives); M3FileTree direct = M3FileTree.open(jar)) {
            M3File app = file(tree, "lib/app.jar");
            assertTrue(app.flag(M3FileFlag.ARCHIVE) && app.isRegularFile());
            assertFalse(file(tree, "lib/fake.jar").flag(M3FileFlag.ARCHIVE));
            assertEquals(names(direct, 0), names(tree, app.id()));
            for (String entry : names(direct, 0)) {
                int expected = idByName(direct, 0, entry);
                if (!direct.flag(M3FileFlag.REGULAR, expected)) continue;
                assertArrayEquals(direct.file(expected).readAllBytes(1 << 20),
                        tree.file(idByName(tree, app.id(), entry)).readAllBytes(1 << 20), entry);
            }
            M3File q = tree.file(idByName(tree, app.id(), "lib/inner.jar/p/Q.class"));
            assertEquals(direct.size(idByName(direct, 0, "lib/inner.jar/p/Q.class")), q.metadata().size());
            assertThrows(IOException.class, q::openReadChannel);
            assertThrows(IOException.class, () -> q.openOutputStream(M3FileProgress.NONE));
            assertArrayEquals(Files.readAllBytes(jar), app.readAllBytes(1 << 20), "the archive row itself is a file");
            assertEquals(2, tree.walk().glob("**/*.class").count(), "A.class and the nested Q.class");
        }
        try (M3FileTree lazy = M3FileTree.open(root, new M3FileScanSpec(1, 4096, true))) {
            int lib = idByName(lazy, 0, "lib");
            lazy.scanChildren(lib);
            lazy.scanChildren(idByName(lazy, 0, "src"));
            M3FileTree eager = M3FileTree.open(root, archives);
            assertEquals(names(eager, 0), names(lazy, 0));
            eager.close();
        }
        try (M3FileTree before = M3FileTree.open(root, archives)) {
            before.hash(before.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
            outer.put("com/A.class", bytes("class A changed"));
            FileTime time = Files.getLastModifiedTime(jar);
            Files.write(jar, zip(outer));
            Files.setLastModifiedTime(jar, FileTime.fromMillis(time.toMillis() + 5_000));
            try (M3FileTree after = M3FileTree.open(root, archives)) {
                M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 1 << 20, M3FileProgress.NONE,
                        M3FileChangeProvider.ATTRIBUTES);
                List<String> changed = new ArrayList<>();
                delta.stream().filter(c -> c.has(M3FileChange.CONTENT_CHANGED))
                        .forEach(c -> changed.add(M3FileZipWriter.name(after, 0, c.entry().id())));
                assertEquals(List.of("lib/app.jar", "lib/app.jar/com/A.class"), changed);
            }
        }
    }

    private static List<String> names(M3FileTree tree, int from) {
        List<String> out = new ArrayList<>();
        tree.walk(from).minDepth(1).forEach(f -> out.add(M3FileZipWriter.name(tree, from, f.id())));
        return out.stream().sorted().toList();
    }

    private static int idByName(M3FileTree tree, int from, String name) {
        return tree.walk(from).ids().filter(id -> id != from && M3FileZipWriter.name(tree, from, id).equals(name))
                .findFirst().orElseThrow();
    }

    private M3File file(M3FileTree tree, String relative) {
        return tree.file(idByName(tree, 0, relative));
    }

    private Path write(String relative, String text) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(1_700_000_000_000L);
                out.putNextEntry(zipEntry);
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
