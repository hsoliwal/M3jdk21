// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.verbatim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3HashPinnedCrateGeneratorTest {
    @TempDir
    Path temp;

    @Test
    void generatesDeterministicShardedCratesAndProposedApprovals() throws Exception {
        Path baseline = temp.resolve("baseline");
        Path reviewed = temp.resolve("reviewed");

        write(
                baseline,
                "src/java.base/share/classes/p/A.java",
                "package p; final class A { int value() { return 1; } }\n");
        write(
                baseline,
                "src/java.base/share/classes/p/Stable.java",
                "package p; final class Stable {}\n");
        write(
                reviewed,
                "src/java.base/share/classes/p/A.java",
                "package p; final class A { int value() { return 2; } }\n");
        write(
                reviewed,
                "src/java.base/share/classes/p/Stable.java",
                "package p; final class Stable {}\n");
        write(
                reviewed,
                "test/jdk/p/NewTest.java",
                "package p; final class NewTest {}\n");

        Path resources = temp.resolve("resources");
        Path approvals = temp.resolve("proposed.tsv");
        var result = M3HashPinnedCrateGenerator.generate(
                baseline,
                reviewed,
                resources,
                approvals,
                "jdk21-pass",
                1);

        assertEquals(2, result.deltas().size());
        assertEquals(2, result.crates().size());
        assertEquals("jdk21-pass-0000", result.crates().get(0).crateName());
        assertEquals(0, result.crates().get(0).offset());
        assertEquals(1, result.crates().get(0).size());
        assertEquals("jdk21-pass-0001", result.crates().get(1).crateName());
        assertEquals(resources.toAbsolutePath().normalize(), result.outputResourceRoot());
        assertEquals(approvals.toAbsolutePath().normalize(), result.proposedApprovalTsv());

        Path firstManifest = resources.resolve("jdk21-pass-0000/manifest.tsv");
        Path secondManifest = resources.resolve("jdk21-pass-0001/manifest.tsv");
        assertTrue(Files.isRegularFile(firstManifest));
        assertTrue(Files.isRegularFile(secondManifest));

        String first = Files.readString(firstManifest, StandardCharsets.UTF_8);
        String second = Files.readString(secondManifest, StandardCharsets.UTF_8);
        assertTrue(first.startsWith("src/java.base/share/classes/p/A.java\t"));
        assertTrue(second.startsWith("test/jdk/p/NewTest.java\tABSENT\t"));
        assertTrue(first.endsWith("\t0000.java.txt\n"));
        assertTrue(second.endsWith("\t0000.java.txt\n"));

        String firstTemplate =
                Files.readString(resources.resolve("jdk21-pass-0000/0000.java.txt"));
        String secondTemplate =
                Files.readString(resources.resolve("jdk21-pass-0001/0000.java.txt"));
        assertTrue(firstTemplate.contains("return 2"));
        assertTrue(secondTemplate.contains("class NewTest"));

        List<String> approvalRows = Files.readAllLines(approvals, StandardCharsets.UTF_8);
        assertEquals(3, approvalRows.size());
        assertEquals(
                "path\tbefore_sha256\tafter_sha256\trecipe_id",
                approvalRows.getFirst());
        assertTrue(approvalRows.get(1).endsWith("\thash-pinned:jdk21-pass-0000"));
        assertTrue(approvalRows.get(2).contains("\tABSENT\t"));
        assertTrue(approvalRows.get(2).endsWith("\thash-pinned:jdk21-pass-0001"));

        assertTrue(result.deltas().getFirst().beforeExists());
        assertFalse(result.deltas().getLast().beforeExists());
        assertEquals(64, result.deltas().getFirst().beforeSha256().length());
        assertEquals(64, result.deltas().getFirst().afterSha256().length());
    }

    @Test
    void noDiffProducesNoCratesAndOnlyApprovalHeader() throws Exception {
        Path baseline = temp.resolve("baseline-same");
        Path reviewed = temp.resolve("reviewed-same");
        String text = "package p; final class A {}\n";
        write(baseline, "src/java.base/share/classes/p/A.java", text);
        write(reviewed, "src/java.base/share/classes/p/A.java", text);

        var result = M3HashPinnedCrateGenerator.generate(
                baseline,
                reviewed,
                temp.resolve("same-resources"),
                temp.resolve("same-approvals.tsv"),
                "jdk21-same",
                32);

        assertTrue(result.deltas().isEmpty());
        assertTrue(result.crates().isEmpty());
        assertEquals(
                List.of("path\tbefore_sha256\tafter_sha256\trecipe_id"),
                Files.readAllLines(result.proposedApprovalTsv()));
    }

    @Test
    void deletionIsRejectedBecauseSnapshotRecipeCannotHideFileRemoval() throws Exception {
        Path baseline = temp.resolve("baseline-delete");
        Path reviewed = temp.resolve("reviewed-delete");
        write(
                baseline,
                "src/java.base/share/classes/p/A.java",
                "package p; final class A {}\n");
        Files.createDirectories(reviewed);

        assertThrows(
                IllegalArgumentException.class,
                () -> M3HashPinnedCrateGenerator.generate(
                        baseline,
                        reviewed,
                        temp.resolve("delete-resources"),
                        temp.resolve("delete.tsv"),
                        "jdk21-delete",
                        32));
    }

    @Test
    void invalidUtf8ReviewedJavaFailsBeforeRecipeIsGenerated() throws Exception {
        Path baseline = temp.resolve("baseline-utf8");
        Path reviewed = temp.resolve("reviewed-utf8");
        Files.createDirectories(baseline);
        Path file = reviewed.resolve("src/java.base/share/classes/p/A.java");
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {(byte) 0xc3, (byte) 0x28});

        assertThrows(
                java.io.IOException.class,
                () -> M3HashPinnedCrateGenerator.generate(
                        baseline,
                        reviewed,
                        temp.resolve("utf8-resources"),
                        temp.resolve("utf8.tsv"),
                        "jdk21-utf8",
                        32));
    }

    @Test
    void nonJavaProductFilesDoNotBecomeSnapshotRecipeTargets() throws Exception {
        Path baseline = temp.resolve("baseline-other");
        Path reviewed = temp.resolve("reviewed-other");
        write(baseline, "make/data/info.txt", "before\n");
        write(reviewed, "make/data/info.txt", "after\n");
        write(baseline, "doc/readme.md", "before\n");
        write(reviewed, "doc/readme.md", "after\n");

        var result = M3HashPinnedCrateGenerator.generate(
                baseline,
                reviewed,
                temp.resolve("other-resources"),
                temp.resolve("other.tsv"),
                "jdk21-java-only",
                32);
        assertTrue(result.deltas().isEmpty());
    }

    @Test
    void generatorMetadataGuardsFailClosed() throws Exception {
        Path baseline = temp.resolve("baseline-invalid");
        Path reviewed = temp.resolve("reviewed-invalid");
        Files.createDirectories(baseline);
        Files.createDirectories(reviewed);

        assertThrows(
                IllegalArgumentException.class,
                () -> M3HashPinnedCrateGenerator.generate(
                        temp.resolve("missing"),
                        reviewed,
                        temp.resolve("out"),
                        temp.resolve("approval.tsv"),
                        "jdk21",
                        1));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3HashPinnedCrateGenerator.generate(
                        baseline,
                        reviewed,
                        temp.resolve("out"),
                        temp.resolve("approval.tsv"),
                        "Bad_Crate",
                        1));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3HashPinnedCrateGenerator.generate(
                        baseline,
                        reviewed,
                        temp.resolve("out"),
                        temp.resolve("approval.tsv"),
                        "jdk21",
                        0));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3HashPinnedCrateGenerator.generate(
                        baseline,
                        reviewed,
                        temp.resolve("out"),
                        temp.resolve("approval.tsv"),
                        "jdk21",
                        257));

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedCrateGenerator.Delta(
                        "src/A.java",
                        "bad",
                        "0".repeat(64),
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedCrateGenerator.Crate("bad_name", 0, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3HashPinnedCrateGenerator.Crate("good", -1, 1));
    }

    private static void write(Path root, String path, String text) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
