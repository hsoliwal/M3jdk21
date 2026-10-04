// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3Jdk21SourceConvergenceMainTest {
    @Test
    void admittedFileRunsAtomPatternDocumentationAndFixedPoint() {
        String before =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        var receipt =
                M3Jdk21SourceConvergenceMain.convergeSource(
                        "src/java.base/share/classes/example/Sample.java",
                        before);

        assertTrue(receipt.changed());
        assertTrue(receipt.atomizationChanged());
        assertTrue(receipt.patternizationChanged());
        assertTrue(receipt.documentationChanged());
        assertTrue(receipt.fixedPoint());
        assertTrue(receipt.candidateSource().contains("int m3$pureIntAtom ="));
        assertTrue(receipt.candidateSource().contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(receipt.candidateSource().contains("M3-ATOM: m3$pureIntAtom"));
        assertFalse(receipt.preSha256().equals(receipt.postSha256()));
    }

    @Test
    void unsupportedFileIsStableAndReceiptedWithoutCandidate() {
        String source =
                """
                package example;
                public final class Stable {
                    public String value() {
                        return "stable";
                    }
                }
                """;

        var receipt =
                M3Jdk21SourceConvergenceMain.convergeSource(
                        "src/java.base/share/classes/example/Stable.java",
                        source);

        assertFalse(receipt.changed());
        assertFalse(receipt.hold());
        assertTrue(receipt.fixedPoint());
        assertEquals(receipt.preSha256(), receipt.postSha256());
        assertTrue(receipt.candidateSource().isEmpty());
    }

    @Test
    void malformedJavaIsHoldRatherThanFalseConvergence() {
        var receipt =
                M3Jdk21SourceConvergenceMain.convergeSource(
                        "src/java.base/share/classes/example/Broken.java",
                        "package example; class Broken {");

        assertTrue(receipt.hold());
        assertFalse(receipt.fixedPoint());
        assertTrue(receipt.message().contains("parse"));
    }

    @Test
    void treeRunWritesOnlyTargetCandidatesAndDeterministicReceipts() throws Exception {
        Path root = Files.createTempDirectory("m3-jdk-root-");
        Path source =
                root.resolve(
                        "src/java.base/share/classes/example/Sample.java");
        Path stable =
                root.resolve(
                        "src/java.base/share/classes/example/Stable.java");
        Files.createDirectories(source.getParent());
        Files.writeString(
                source,
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return a + b;
                    }
                }
                """);
        Files.writeString(
                stable,
                """
                package example;
                final class Stable {
                    String text() { return "x"; }
                }
                """);
        String original = Files.readString(source);
        Path output = root.resolve("m3/target/source-convergence");

        var first = M3Jdk21SourceConvergenceMain.convergeTree(root, output, 2);
        assertEquals(2, first.files());
        assertEquals(1, first.changed());
        assertEquals(0, first.holds());
        assertEquals(original, Files.readString(source));
        assertTrue(
                Files.exists(
                        output.resolve(
                                "candidates/src/java.base/share/classes/example/Sample.java")));
        assertTrue(Files.exists(output.resolve(M3Jdk21SourceConvergenceMain.MANIFEST)));
        assertTrue(Files.exists(output.resolve(M3Jdk21SourceConvergenceMain.SUMMARY)));

        Path replay = root.resolve("m3/target/source-convergence-replay");
        var second = M3Jdk21SourceConvergenceMain.convergeTree(root, replay, 1);
        assertEquals(first.root(), second.root());
        assertEquals(
                Files.readString(output.resolve(M3Jdk21SourceConvergenceMain.MANIFEST)),
                Files.readString(replay.resolve(M3Jdk21SourceConvergenceMain.MANIFEST)));
    }

    @Test
    void testTreeIsConvergedByTheSameFileLocalDag() throws Exception {
        Path root = Files.createTempDirectory("m3-jdk-root-test-");
        Files.createDirectories(root.resolve("src"));
        Path testSource =
                root.resolve("test/jdk/example/TestSample.java");
        Files.createDirectories(testSource.getParent());
        Files.writeString(
                testSource,
                """
                package example;
                final class TestSample {
                    private static int compute(int a, int b) {
                        return a + b;
                    }
                }
                """);
        Path output = root.resolve("m3/target/test-source-convergence");

        var summary = M3Jdk21SourceConvergenceMain.convergeTree(root, output, 1);

        assertEquals(1, summary.files());
        assertEquals(1, summary.changed());
        assertTrue(
                Files.exists(
                        output.resolve("candidates/test/jdk/example/TestSample.java")));
    }

    @Test
    void outputFenceRejectsSourceTreeAndInvalidThreadCount() throws Exception {
        Path root = Files.createTempDirectory("m3-jdk-root-");
        Files.createDirectories(root.resolve("src"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3Jdk21SourceConvergenceMain.convergeTree(
                                root, root.resolve("src/generated"), 1));
        Files.createDirectories(root.resolve("test"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3Jdk21SourceConvergenceMain.convergeTree(
                                root, root.resolve("test/generated"), 1));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3Jdk21SourceConvergenceMain.convergeTree(
                                root, root.resolve("m3/target/out"), 0));
    }
}
