// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.verbatim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3Jdk21VerbatimCompareTest {
    @TempDir
    Path temp;

    @Test
    void cleanTreesProduceDeterministicAllFileInventory() throws Exception {
        Path baseline = temp.resolve("baseline");
        Path candidate = temp.resolve("candidate");
        write(baseline, "src/java.base/share/classes/p/A.java", "class A {}\n");
        write(baseline, "test/jdk/p/ATest.java", "class ATest {}\n");
        write(baseline, "make/data/info.txt", "make\n");
        write(baseline, "doc/testing.md", "doc\n");

        write(candidate, "src/java.base/share/classes/p/A.java", "class A {}\n");
        write(candidate, "test/jdk/p/ATest.java", "class ATest {}\n");
        write(candidate, "make/data/info.txt", "make\n");
        write(candidate, "doc/testing.md", "doc\n");

        Path approvals = approvals();
        Path output = temp.resolve("clean.tsv");

        var summary =
                M3Jdk21VerbatimCompare.compare(baseline, candidate, approvals, output);

        assertTrue(summary.clean());
        assertEquals(4, summary.total());
        assertEquals(4, summary.same());
        assertEquals(0, summary.modified());
        assertEquals(0, summary.added());
        assertEquals(0, summary.deleted());
        assertEquals(0, summary.approvedChanges());
        assertEquals(0, summary.unapproved());
        assertTrue(Files.isRegularFile(summary.output()));

        List<String> lines = Files.readAllLines(output, StandardCharsets.UTF_8);
        assertEquals(5, lines.size());
        assertEquals(
                "path\tstatus\tbefore_sha256\tafter_sha256\tbefore_bytes\tafter_bytes\trecipe_id\tapproved",
                lines.getFirst());
        assertTrue(lines.get(1).startsWith("doc/testing.md\tSAME\t"));
        assertTrue(lines.get(2).startsWith("make/data/info.txt\tSAME\t"));
        assertTrue(lines.get(3).startsWith("src/java.base/share/classes/p/A.java\tSAME\t"));
        assertTrue(lines.get(4).startsWith("test/jdk/p/ATest.java\tSAME\t"));
    }

    @Test
    void modifiedAddedDeletedFilesRequireExactRecipeApproval() throws Exception {
        Path baseline = temp.resolve("baseline-drift");
        Path candidate = temp.resolve("candidate-drift");
        write(baseline, "src/java.base/share/classes/p/A.java", "class A { int x = 1; }\n");
        write(baseline, "test/jdk/p/Old.java", "class Old {}\n");
        write(candidate, "src/java.base/share/classes/p/A.java", "class A { int x = 2; }\n");
        write(candidate, "test/jdk/p/New.java", "class New {}\n");

        Path noApprovals = approvals();
        Path output = temp.resolve("drift.tsv");
        var blocked =
                M3Jdk21VerbatimCompare.compare(
                        baseline, candidate, noApprovals, output);

        assertFalse(blocked.clean());
        assertEquals(3, blocked.total());
        assertEquals(1, blocked.modified());
        assertEquals(1, blocked.added());
        assertEquals(1, blocked.deleted());
        assertEquals(3, blocked.unapproved());

        List<String> rows = Files.readAllLines(output, StandardCharsets.UTF_8);
        String modified = rows.stream()
                .filter(line -> line.startsWith("src/java.base/share/classes/p/A.java\t"))
                .findFirst()
                .orElseThrow();
        String added = rows.stream()
                .filter(line -> line.startsWith("test/jdk/p/New.java\t"))
                .findFirst()
                .orElseThrow();
        String deleted = rows.stream()
                .filter(line -> line.startsWith("test/jdk/p/Old.java\t"))
                .findFirst()
                .orElseThrow();

        assertTrue(modified.contains("\tMODIFIED\t"));
        assertTrue(modified.endsWith("\t\tNO"));
        assertTrue(added.contains("\tADDED\tABSENT\t"));
        assertTrue(deleted.contains("\tDELETED\t"));
        assertTrue(deleted.contains("\tABSENT\t"));

        String[] modifiedCells = modified.split("\t", -1);
        String[] addedCells = added.split("\t", -1);
        String[] deletedCells = deleted.split("\t", -1);

        Path exactApprovals = temp.resolve("approved.tsv");
        Files.writeString(
                exactApprovals,
                "path\tbefore_sha256\tafter_sha256\trecipe_id\n"
                        + "src/java.base/share/classes/p/A.java\t"
                        + modifiedCells[2] + "\t" + modifiedCells[3] + "\tm3-A\n"
                        + "test/jdk/p/New.java\tABSENT\t"
                        + addedCells[3] + "\tm3-new\n"
                        + "test/jdk/p/Old.java\t"
                        + deletedCells[2] + "\tABSENT\tm3-old\n",
                StandardCharsets.UTF_8);

        var admitted = M3Jdk21VerbatimCompare.compare(
                baseline,
                candidate,
                exactApprovals,
                temp.resolve("admitted.tsv"));

        assertTrue(admitted.clean());
        assertEquals(3, admitted.approvedChanges());
        assertEquals(0, admitted.unapproved());
    }

    @Test
    void wrongApprovalHashDoesNotAdmitDrift() throws Exception {
        Path baseline = temp.resolve("baseline-wrong");
        Path candidate = temp.resolve("candidate-wrong");
        write(baseline, "src/java.base/share/classes/p/A.java", "class A {}\n");
        write(candidate, "src/java.base/share/classes/p/A.java", "class A { int x; }\n");

        Path approvals = temp.resolve("wrong.tsv");
        Files.writeString(
                approvals,
                "path\tbefore_sha256\tafter_sha256\trecipe_id\n"
                        + "src/java.base/share/classes/p/A.java\t"
                        + "0".repeat(64) + "\t" + "1".repeat(64) + "\tm3-wrong\n",
                StandardCharsets.UTF_8);

        var summary = M3Jdk21VerbatimCompare.compare(
                baseline,
                candidate,
                approvals,
                temp.resolve("wrong-output.tsv"));
        assertFalse(summary.clean());
        assertEquals(1, summary.unapproved());
        assertEquals(0, summary.approvedChanges());
    }

    @Test
    void runReturnsPortableExitCodesWithoutTerminatingJvm() throws Exception {
        Path baseline = temp.resolve("baseline-run");
        Path candidate = temp.resolve("candidate-run");
        write(baseline, "src/java.base/share/classes/p/A.java", "class A {}\n");
        write(candidate, "src/java.base/share/classes/p/A.java", "class A {}\n");
        Path approvals = approvals();
        Path output = temp.resolve("run.tsv");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int usage = M3Jdk21VerbatimCompare.run(
                new String[0],
                new PrintStream(stdout),
                new PrintStream(stderr));
        assertEquals(64, usage);
        assertTrue(stderr.toString(StandardCharsets.UTF_8).contains("usage:"));

        stdout.reset();
        stderr.reset();
        int ok = M3Jdk21VerbatimCompare.run(
                new String[] {
                    baseline.toString(),
                    candidate.toString(),
                    approvals.toString(),
                    output.toString()
                },
                new PrintStream(stdout),
                new PrintStream(stderr));
        assertEquals(0, ok);
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("unapproved=0"));

        write(candidate, "src/java.base/share/classes/p/A.java", "class A { int x; }\n");
        stdout.reset();
        int blocked = M3Jdk21VerbatimCompare.run(
                new String[] {
                    baseline.toString(),
                    candidate.toString(),
                    approvals.toString(),
                    temp.resolve("blocked.tsv").toString()
                },
                new PrintStream(stdout),
                new PrintStream(stderr));
        assertEquals(2, blocked);
    }

    @Test
    void invalidRootsApprovalMetadataAndSummaryFailClosed() throws Exception {
        Path baseline = temp.resolve("baseline-invalid");
        Path candidate = temp.resolve("candidate-invalid");
        Files.createDirectories(baseline);
        Files.createDirectories(candidate);

        assertThrows(
                IllegalArgumentException.class,
                () -> M3Jdk21VerbatimCompare.compare(
                        temp.resolve("missing"),
                        candidate,
                        approvals(),
                        temp.resolve("x.tsv")));

        Path badHeader = temp.resolve("bad-header.tsv");
        Files.writeString(badHeader, "wrong\n", StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3Jdk21VerbatimCompare.compare(
                        baseline, candidate, badHeader, temp.resolve("bad.tsv")));

        Path outside = temp.resolve("outside.tsv");
        Files.writeString(
                outside,
                "path\tbefore_sha256\tafter_sha256\trecipe_id\n"
                        + "m3/file.java\tABSENT\t"
                        + "0".repeat(64) + "\trecipe\n",
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3Jdk21VerbatimCompare.compare(
                        baseline, candidate, outside, temp.resolve("outside-output.tsv")));

        Path duplicate = temp.resolve("duplicate.tsv");
        String row = "src/p/A.java\tABSENT\t" + "0".repeat(64) + "\trecipe\n";
        Files.writeString(
                duplicate,
                "path\tbefore_sha256\tafter_sha256\trecipe_id\n" + row + row,
                StandardCharsets.UTF_8);
        assertThrows(
                IllegalArgumentException.class,
                () -> M3Jdk21VerbatimCompare.compare(
                        baseline, candidate, duplicate, temp.resolve("duplicate-output.tsv")));

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3Jdk21VerbatimCompare.Approval("bad", "0".repeat(64), "recipe"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3Jdk21VerbatimCompare.Summary(
                        1, 0, 0, 0, 0, 0, 0, temp.resolve("summary.tsv")));
    }

    private Path approvals() throws Exception {
        Path file = temp.resolve("approved-" + System.nanoTime() + ".tsv");
        Files.writeString(
                file,
                "path\tbefore_sha256\tafter_sha256\trecipe_id\n",
                StandardCharsets.UTF_8);
        return file;
    }

    private static void write(Path root, String path, String text) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
