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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S7 differential contract: archive trees against {@code ZipFile.entries()} and {@code ZipInputStream}. */
class M3FileZipTest {
    private static final long TIME = 1_700_000_000_000L;

    @TempDir
    Path root;

    @Test
    void rowsEqualCentralDirectoryAndNestedListings() throws Exception {
        Path jar = fixture("app.jar", outerEntries(bytes("class A v1"), true));
        try (M3FileTree tree = M3FileTree.open(jar); ZipFile zip = new ZipFile(jar.toFile())) {
            assertTrue(tree.flag(M3FileFlag.ARCHIVE, 0) && tree.flag(M3FileFlag.REGULAR, 0));
            assertEquals("app.jar", tree.name(0));
            Map<String, Expected> expected = oracle(zip);
            Map<String, Expected> actual = new TreeMap<>();
            for (int id = 1; id < tree.rows(); id++) {
                actual.put(relative(tree, id), new Expected(tree.flag(M3FileFlag.DIR, id),
                        tree.flag(M3FileFlag.DIR, id) ? 0 : tree.size(id), null));
            }
            Map<String, Expected> shapes = new TreeMap<>();
            expected.forEach((k, v) -> shapes.put(k, new Expected(v.directory, v.size, null)));
            assertEquals(shapes, actual);
            Set<String> archives = new TreeSet<>();
            tree.ids().filter(id -> tree.flag(M3FileFlag.ARCHIVE, id)).forEach(id -> archives.add(relative(tree, id)));
            assertEquals(Set.of("", "lib/inner.jar", "lib/inner.jar/deep.zip"), archives);
            for (ZipEntry entry : Collections.list(zip.entries())) {
                if (entry.isDirectory()) continue;
                int id = idOf(tree, M3FileZipScanner.normalize(entry.getName()));
                assertEquals(entry.getCrc(), tree.entryCrc(id), entry.getName());
                assertEquals(entry.getLastModifiedTime().toMillis(), tree.mtime(id));
            }
            assertEquals(-1, tree.entryCrc(idOf(tree, "com/x")), "implied directory");
            assertFalse(tree.flag(M3FileFlag.ARCHIVE, idOf(tree, "notzip.jar")));
            assertTrue(tree.flag(M3FileFlag.HIDDEN, idOf(tree, ".dotfile")));
        }
    }

    @Test
    void contentThroughTheSourceWithLimitsAndShadows() throws Exception {
        Path jar = fixture("app.jar", outerEntries(bytes("class A v1"), true));
        try (M3FileTree tree = M3FileTree.open(jar); ZipFile zip = new ZipFile(jar.toFile())) {
            Map<String, Expected> expected = oracle(zip);
            for (Map.Entry<String, Expected> entry : expected.entrySet()) {
                if (entry.getValue().directory) continue;
                M3File file = tree.file(idOf(tree, entry.getKey()));
                assertArrayEquals(entry.getValue().content, file.readAllBytes(1 << 20), entry.getKey());
            }
            M3File nested = tree.file(idOf(tree, "lib/inner.jar/deep.zip/z.txt"));
            M3FileDigestShadow sha = M3FileShadow.sha256();
            AtomicLong worked = new AtomicLong();
            assertEquals(nested.size(), nested.observe(1 << 20, M3FileProgress.of(worked::addAndGet, () -> false), sha));
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(bytes("deepest")), sha.bytes());
            assertEquals(nested.size(), worked.get());
            M3File a = tree.file(idOf(tree, "com/x/A.class"));
            assertThrows(IOException.class, () -> a.readAllBytes(2));
            assertThrows(IOException.class, () -> a.openOutputStream(M3FileProgress.NONE));
            assertThrows(IOException.class, a::openReadChannel);
            assertThrows(IOException.class, () -> tree.file(idOf(tree, "com")).readAllBytes(10));
            assertArrayEquals(Files.readAllBytes(jar), tree.rootFile().readAllBytes(1 << 20));
            assertEquals(a.size(), a.metadata().size(), "central-directory metadata");
        }
    }

    @Test
    void walksAndGlobsSeeArchiveEntries() throws Exception {
        Path jar = fixture("app.jar", outerEntries(bytes("class A v1"), true));
        try (M3FileTree tree = M3FileTree.open(jar); ZipFile zip = new ZipFile(jar.toFile())) {
            Map<String, Expected> expected = oracle(zip);
            PathMatcher classes = root.getFileSystem().getPathMatcher("glob:**/*.class");
            List<String> globbed = new ArrayList<>();
            tree.walk().glob("**/*.class").forEach(f -> globbed.add(relative(tree, f.id())));
            assertEquals(expected.keySet().stream().filter(k -> classes.matches(Path.of(k))).sorted().toList(),
                    globbed.stream().sorted().toList());
            assertEquals(expected.values().stream().filter(e -> e.directory).count(),
                    tree.walk().kinds(M3FileKind.DIRECTORY).count());
            assertEquals(globbed.size(), tree.walk().types("class").count());
            assertEquals(List.of("lib/inner.jar/deep.zip/z.txt"), tree.walk().regex(".*/deep\\.zip/.*")
                    .map(f -> relative(tree, f.id())).toList());
        }
    }

    @Test
    void deltaBetweenArchiveVersionsGatesOnEntryCrc() throws Exception {
        Path v1 = fixture("v1.jar", outerEntries(bytes("class A v1"), true));
        Map<String, byte[]> next = outerEntries(bytes("class A v2"), false);
        next.put("com/x/C.class", bytes("class C"));
        Path v2 = fixture("v2.jar", next);
        Files.setLastModifiedTime(v2, FileTime.fromMillis(Files.getLastModifiedTime(v1).toMillis() + 5_000));
        try (M3FileTree before = M3FileTree.open(v1); M3FileTree after = M3FileTree.open(v2)) {
            AtomicLong read = new AtomicLong();
            M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 1 << 20,
                    M3FileProgress.of(read::addAndGet, () -> false), M3FileChangeProvider.ATTRIBUTES);
            Map<String, String> changes = new TreeMap<>();
            delta.stream().forEach(c -> changes.put(relative(c.entry().tree(), c.entry().id()),
                    M3FileChange.describe(c.bits() & (M3FileChange.ADDED | M3FileChange.REMOVED
                            | M3FileChange.CONTENT_CHANGED))));
            assertEquals("CONTENT_CHANGED", changes.get("com/x/A.class"), "same size and time, CRC differs");
            assertEquals("REMOVED", changes.get("com/x/B.class"));
            assertEquals("ADDED", changes.get("com/x/C.class"));
            assertFalse(changes.containsKey("res/data.json"));
            assertFalse(changes.containsKey("lib/inner.jar/p/Q.class"));
            long expectedRead = Files.size(v2) + after.size(idOf(after, "com/x/A.class")) + after.size(idOf(after, "com/x/C.class"));
            assertEquals(expectedRead, read.get(), "unchanged entries are never read");
        }
    }

    @Test
    void plainFilesDirectoriesAndClose() throws Exception {
        Path text = Files.writeString(root.resolve("notes.txt"), "PK is not enough");
        try (M3FileTree single = M3FileTree.open(text)) {
            assertEquals(1, single.rows());
            assertFalse(single.flag(M3FileFlag.ARCHIVE, 0));
        }
        Path folder = Files.createDirectories(root.resolve("folder"));
        Path jar = fixture("folder/app.jar", outerEntries(bytes("class A v1"), true));
        try (M3FileTree tree = M3FileTree.open(folder)) {
            int id = idOf(tree, "app.jar");
            assertEquals(0, tree.childCount(id), "archives below a directory root stay files");
            assertFalse(tree.flag(M3FileFlag.ARCHIVE, id));
        }
        M3FileTree archive = M3FileTree.open(jar);
        M3File a = archive.file(idOf(archive, "com/x/A.class"));
        archive.close();
        assertThrows(IllegalStateException.class, () -> a.readAllBytes(1 << 20));
        Files.delete(jar);
    }

    private record Expected(boolean directory, long size, byte[] content) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Expected that && that.directory == directory && that.size == size;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(directory) * 31 + Long.hashCode(size);
        }
    }

    /** Expected rows: central directory, implied parents, nested listings below archive-typed entries. */
    private static Map<String, Expected> oracle(ZipFile zip) throws IOException {
        Map<String, Expected> out = new TreeMap<>();
        for (ZipEntry entry : Collections.list(zip.entries())) {
            String name = M3FileZipScanner.normalize(entry.getName());
            byte[] content = entry.isDirectory() ? null : zip.getInputStream(entry).readAllBytes();
            put(out, "", name, entry.isDirectory(), content);
        }
        return out;
    }

    private static void put(Map<String, Expected> out, String prefix, String name, boolean directory, byte[] content)
            throws IOException {
        String full = prefix + name;
        for (int slash = full.indexOf('/', prefix.length()); slash > 0; slash = full.indexOf('/', slash + 1)) {
            out.putIfAbsent(full.substring(0, slash), new Expected(true, 0, null));
        }
        out.putIfAbsent(full, new Expected(directory, directory ? 0 : content.length, content));
        if (directory || !M3FileZipScanner.nestable(name)) return;
        try (ZipInputStream nested = new ZipInputStream(new ByteArrayInputStream(content))) {
            for (ZipEntry entry = nested.getNextEntry(); entry != null; entry = nested.getNextEntry()) {
                byte[] inner = entry.isDirectory() ? null : nested.readAllBytes();
                put(out, full + "/", M3FileZipScanner.normalize(entry.getName()), entry.isDirectory(), inner);
            }
        }
    }

    private static Map<String, byte[]> outerEntries(byte[] aClass, boolean withB) throws IOException {
        Map<String, byte[]> deep = new LinkedHashMap<>();
        deep.put("z.txt", bytes("deepest"));
        Map<String, byte[]> inner = new LinkedHashMap<>();
        inner.put("META-INF/", null);
        inner.put("p/Q.class", bytes("class Q"));
        inner.put("deep.zip", zip(deep));
        Map<String, byte[]> outer = new LinkedHashMap<>();
        outer.put("META-INF/MANIFEST.MF", bytes("Manifest-Version: 1.0\n"));
        outer.put("com/", null);
        outer.put("com/x/A.class", aClass);
        if (withB) outer.put("com/x/B.class", bytes("class B"));
        outer.put("res/data.json", bytes("{\"a\": 1}"));
        outer.put("empty.txt", new byte[0]);
        outer.put("stored.bin", new byte[] {0, 1, 2, 3, 4});
        outer.put("lib/inner.jar", zip(inner));
        outer.put("notzip.jar", bytes("not a zip at all"));
        outer.put(".dotfile", bytes("hidden"));
        return outer;
    }

    private Path fixture(String name, Map<String, byte[]> entries) throws IOException {
        return Files.write(root.resolve(name), zip(entries));
    }

    private static byte[] zip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setTime(TIME);
                if (entry.getKey().equals("stored.bin")) {
                    CRC32 crc = new CRC32();
                    crc.update(entry.getValue());
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setSize(entry.getValue().length);
                    zipEntry.setCrc(crc.getValue());
                }
                out.putNextEntry(zipEntry);
                if (entry.getValue() != null) out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String relative(M3FileTree tree, int id) {
        StringBuilder out = new StringBuilder();
        tree.appendRelative(0, id, out);
        return out.toString();
    }

    private static int idOf(M3FileTree tree, String relative) {
        return tree.ids().filter(id -> relative(tree, id).replace('\\', '/').equals(relative)).findFirst().orElseThrow();
    }
}
