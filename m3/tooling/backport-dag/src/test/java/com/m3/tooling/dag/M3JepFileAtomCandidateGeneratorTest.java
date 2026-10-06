// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3JepFileAtomCandidateGeneratorTest {
    @Test
    void generatesExactAuthorityFreeFileAtomsAndPostimages() throws Exception {
        Path current = Files.createTempDirectory("m3-current-");
        Path upstream = Files.createTempDirectory("m3-upstream-");
        Path output = Files.createTempDirectory("m3-output-");

        write(current, "a/A.java", "class A {}\n".getBytes(StandardCharsets.UTF_8));
        write(upstream, "a/A.java", "class A {}\n".getBytes(StandardCharsets.UTF_8));

        write(current, "b/config.txt", "old\n".getBytes(StandardCharsets.UTF_8));
        write(upstream, "b/config.txt", "new\n".getBytes(StandardCharsets.UTF_8));

        byte[] binary = {(byte) 0xff, 0, 1, 2};
        write(upstream, "c/blob.bin", binary);

        var report =
                M3JepFileAtomCandidateGenerator.generate(
                        current,
                        upstream,
                        List.of("a/A.java", "b/config.txt", "c/blob.bin"),
                        output);

        assertEquals(3, report.rows().size());
        assertEquals("JAVA", report.rows().get(0).kind());
        assertEquals("ALREADY_EQUIVALENT", report.rows().get(0).relation());
        assertEquals("TEXT", report.rows().get(1).kind());
        assertEquals("CHANGED", report.rows().get(1).relation());
        assertEquals("BINARY_HOLD", report.rows().get(2).kind());
        assertEquals("ADDITIVE", report.rows().get(2).relation());
        assertEquals("ABSENT", report.rows().get(2).currentState());
        assertEquals("", report.rows().get(2).currentSha256());

        assertTrue(report.root().matches("[0-9a-f]{64}"));
        assertEquals(4, report.tsv().lines().count());
        assertTrue(
                report.rows().stream()
                        .allMatch(
                                row ->
                                        !row.sourceMutationAuthority()
                                                && !row.promotionAuthority()
                                                && row.rowRoot().matches("[0-9a-f]{64}")));

        assertEquals(
                "class A {}\n",
                Files.readString(output.resolve("postimages/a/A.java"), StandardCharsets.UTF_8));
        assertEquals(
                "new\n",
                Files.readString(output.resolve("postimages/b/config.txt"), StandardCharsets.UTF_8));
        assertArrayEquals(binary, Files.readAllBytes(output.resolve("postimages/c/blob.bin")));
        assertEquals(
                report.root(),
                Files.readString(output.resolve("FILE_ATOMS.sha256"), StandardCharsets.UTF_8)
                        .strip());
        assertEquals(
                report.tsv(),
                Files.readString(output.resolve("FILE_ATOMS.tsv"), StandardCharsets.UTF_8));
    }

    @Test
    void parserAndGeneratorRequireCanonicalSortedUniquePaths() throws Exception {
        assertEquals(
                List.of("a/A.java", "b/B.java"),
                M3JepFileAtomCandidateGenerator.parsePaths(
                        "# comment\na/A.java\n\nb/B.java\n"));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.parsePaths("b/B.java\na/A.java\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.parsePaths("a/A.java\na/A.java\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.parsePaths("../escape.java\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.parsePaths("/absolute.java\n"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.parsePaths(""));
    }

    @Test
    void missingUpstreamOrNonFileCurrentPathFailsClosed() throws Exception {
        Path current = Files.createTempDirectory("m3-current-");
        Path upstream = Files.createTempDirectory("m3-upstream-");
        Path output = Files.createTempDirectory("m3-output-");

        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.generate(
                        current,
                        upstream,
                        List.of("a/Missing.java"),
                        output));

        write(upstream, "a/Dir.java", "class Dir {}\n".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(current.resolve("a/Dir.java"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JepFileAtomCandidateGenerator.generate(
                        current,
                        upstream,
                        List.of("a/Dir.java"),
                        output));
    }

    @Test
    void cliProducesSameEvidenceShape() throws Exception {
        Path root = Files.createTempDirectory("m3-file-atom-cli-");
        Path current = root.resolve("current");
        Path upstream = root.resolve("upstream");
        Path output = root.resolve("out");
        Files.createDirectories(current);
        Files.createDirectories(upstream);
        write(upstream, "src/A.java", "class A {}\n".getBytes(StandardCharsets.UTF_8));
        Path paths = root.resolve("PATHS.txt");
        Files.writeString(paths, "src/A.java\n", StandardCharsets.UTF_8);

        M3JepFileAtomCandidateMain.main(
                new String[] {
                    current.toString(),
                    upstream.toString(),
                    paths.toString(),
                    output.toString()
                });

        String tsv = Files.readString(output.resolve("FILE_ATOMS.tsv"), StandardCharsets.UTF_8);
        assertTrue(tsv.startsWith(M3JepFileAtomCandidateGenerator.HEADER + "\n"));
        assertTrue(tsv.contains("\tJAVA\tABSENT\tPRESENT\t"));
        assertTrue(tsv.contains("\tADDITIVE\tpostimages/src/A.java\tfalse\tfalse\t"));
    }

    @Test
    void reportRowsCannotBeForgedWithAuthority() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3JepFileAtomCandidateGenerator.Row(
                        0,
                        "a/A.java",
                        "JAVA",
                        "ABSENT",
                        "PRESENT",
                        "",
                        "0".repeat(64),
                        "ADDITIVE",
                        "postimages/a/A.java",
                        true,
                        false,
                        "1".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3JepFileAtomCandidateGenerator.Row(
                        0,
                        "a/A.java",
                        "JAVA",
                        "PRESENT",
                        "PRESENT",
                        "0".repeat(64),
                        "1".repeat(64),
                        "ADDITIVE",
                        "postimages/a/A.java",
                        false,
                        false,
                        "2".repeat(64)));
    }

    private static void write(Path root, String relative, byte[] bytes) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        assertFalse(Files.isDirectory(file));
    }
}
