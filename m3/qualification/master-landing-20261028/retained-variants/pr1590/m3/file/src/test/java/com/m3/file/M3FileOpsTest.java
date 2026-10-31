// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S9a contract: du, wc, grep, render and zip writing over walks, differential against plain Java oracles. */
class M3FileOpsTest {
    @TempDir
    Path root;

    @BeforeEach
    void fixture() throws IOException {
        write("src/A.java", "class A {\r\n  int x = 1; // TODO fix\n}\n");
        write("src/B.java", "class B { }\tTODO\n\nété TODO\r");
        write("docs/readme.md", "# Title\nsome  words\there\nTODO: more");
        write("docs/empty.txt", "");
        Files.write(root.resolve("docs/blob.bin"), new byte[] {'T', 'O', 'D', 'O', 0, 1, 2, '\n'});
        Files.createDirectories(root.resolve("empty"));
    }

    @Test
    void duAndWcEqualByteAndPosixCounts() throws Exception {
        M3FileTree tree = M3FileTree.open(root);
        List<Path> files = regularFiles();
        long bytes = 0;
        long lines = 0;
        long words = 0;
        for (Path file : files) {
            byte[] data = Files.readAllBytes(file);
            String latin = new String(data, StandardCharsets.ISO_8859_1);
            bytes += data.length;
            lines += latin.chars().filter(c -> c == '\n').count();
            words += Arrays.stream(latin.split("\\s+")).filter(w -> !w.isEmpty()).count();
        }
        assertEquals(bytes, tree.walk().bytes());
        AtomicLong worked = new AtomicLong();
        M3FileWordCount wc = tree.walk().wc(1 << 20, M3FileProgress.of(worked::addAndGet, () -> false));
        assertEquals(new M3FileWordCount(files.size(), lines, words, bytes, 0), wc);
        assertEquals(bytes, worked.get());
        assertEquals(Files.size(root.resolve("src/A.java")) + Files.size(root.resolve("src/B.java")),
                tree.walk().types("java").bytes());
    }

    @Test
    void grepEqualsLineOracleAndSkipsBinaries() throws Exception {
        M3FileTree tree = M3FileTree.open(root);
        Pattern todo = Pattern.compile("TODO");
        List<String> expected = new ArrayList<>();
        for (Path file : regularFiles()) {
            byte[] data = Files.readAllBytes(file);
            if (new String(data, StandardCharsets.ISO_8859_1).indexOf('\0') >= 0) continue;
            List<String> lines = new BufferedReader(new StringReader(new String(data, StandardCharsets.UTF_8))).lines().toList();
            for (int i = 0; i < lines.size(); i++) {
                if (todo.matcher(lines.get(i)).find()) expected.add(file + ":" + (i + 1) + ":" + lines.get(i));
            }
        }
        List<String> actual = tree.walk().grep(todo, 1 << 20, M3FileProgress.NONE).stream()
                .map(m -> m.file().toPath() + ":" + m.line() + ":" + m.text()).toList();
        assertEquals(expected, actual);
        assertTrue(actual.stream().anyMatch(s -> s.contains("été TODO")));
    }

    @Test
    void renderFollowsNameOrderAndWalkFilters() throws Exception {
        M3FileTree tree = M3FileTree.open(root);
        String name = root.getFileName().toString();
        assertEquals(String.join("\n", name,
                "├── docs",
                "│   ├── blob.bin",
                "│   ├── empty.txt",
                "│   └── readme.md",
                "├── empty",
                "└── src",
                "    ├── A.java",
                "    └── B.java", ""), tree.walk().render());
        assertEquals(String.join("\n", name,
                "└── src",
                "    ├── A.java",
                "    └── B.java", ""), tree.walk().types("java").render());
        assertEquals(String.join("\n", name, "├── docs", "├── empty",
                "└── src", ""), tree.walk().maxDepth(1).render());
    }

    @Test
    void zipWritingRoundTripsAndRepacks() throws Exception {
        M3FileTree tree = M3FileTree.open(root);
        Path zip = root.getParent().resolve(root.getFileName() + "-out.zip");
        AtomicLong worked = new AtomicLong();
        long entries = tree.walk().zipTo(zip, 1 << 20, M3FileProgress.of(worked::addAndGet, () -> false));
        assertEquals(tree.rows() - 1, entries);
        assertEquals(tree.walk().bytes(), worked.get());
        Map<String, byte[]> expected = new TreeMap<>();
        for (Path file : regularFiles()) expected.put(root.relativize(file).toString().replace('\\', '/'), Files.readAllBytes(file));
        try (ZipFile written = new ZipFile(zip.toFile())) {
            Map<String, byte[]> actual = new TreeMap<>();
            for (ZipEntry entry : Collections.list(written.entries())) {
                if (entry.isDirectory()) continue;
                actual.put(entry.getName(), written.getInputStream(entry).readAllBytes());
                long mtime = Files.getLastModifiedTime(root.resolve(entry.getName())).toMillis();
                assertTrue(Math.abs(entry.getLastModifiedTime().toMillis() - mtime) < 2000, entry.getName());
            }
            assertEquals(expected.keySet(), actual.keySet());
            for (String key : expected.keySet()) assertArrayEquals(expected.get(key), actual.get(key), key);
            assertTrue(written.getEntry("empty/") != null && written.getEntry("empty/").isDirectory());
        }
        try (M3FileTree archive = M3FileTree.open(zip)) {
            List<String> original = new ArrayList<>();
            tree.walk().minDepth(1).forEach(f -> original.add(M3FileZipWriter.name(tree, 0, f.id())));
            List<String> reopened = new ArrayList<>();
            archive.walk().minDepth(1).forEach(f -> reopened.add(M3FileZipWriter.name(archive, 0, f.id())));
            assertEquals(original.stream().sorted().toList(), reopened.stream().sorted().toList());
            ByteArrayOutputStream repacked = new ByteArrayOutputStream();
            assertEquals(2, archive.walk().types("java").zipTo(repacked, 1 << 20, M3FileProgress.NONE));
            Path repack = Files.write(root.getParent().resolve(root.getFileName() + "-java.zip"), repacked.toByteArray());
            try (ZipFile java = new ZipFile(repack.toFile())) {
                assertEquals(List.of("src/A.java", "src/B.java"),
                        Collections.list(java.entries()).stream().map(ZipEntry::getName).sorted().toList());
                assertArrayEquals(expected.get("src/B.java"), java.getInputStream(java.getEntry("src/B.java")).readAllBytes());
            }
        }
    }

    private List<Path> regularFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList();
        }
    }

    private void write(String relative, String text) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
