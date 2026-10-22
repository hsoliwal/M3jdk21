// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NebulaM3A3ApplyTest {
    @TempDir
    Path root;

    @Test
    void materializesCandidateWithoutWritingOriginalAndReachesFixedPoint()
            throws Exception {
        String relative = "widgets/demo/src/example/Sample.java";
        String before =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;
        write(relative, before);

        List<NebulaM3A3Apply.Receipt> receipts =
                NebulaM3A3Apply.run(
                        root,
                        Path.of("m3/recipe-first/target/a3-proof"),
                        List.of(relative));

        assertEquals(1, receipts.size());
        NebulaM3A3Apply.Receipt receipt = receipts.getFirst();
        assertEquals(relative, receipt.path());
        assertTrue(receipt.changed());
        assertTrue(receipt.fixedPoint());
        assertFalse(receipt.beforeSha256().equals(receipt.afterSha256()));

        assertEquals(before, Files.readString(root.resolve(relative)));

        Path candidate =
                root.resolve(
                        "m3/recipe-first/target/a3-proof/candidate/")
                        .resolve(relative);
        String after = Files.readString(candidate);
        assertTrue(after.contains("int m3$pureIntAtom ="));
        assertTrue(after.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(after.contains("M3-ATOM: m3$pureIntAtom"));

        String receiptText =
                Files.readString(
                        root.resolve(
                                "m3/recipe-first/target/a3-proof/receipt.tsv"));
        assertTrue(receiptText.contains(relative));
        assertTrue(receiptText.endsWith("\ttrue\ttrue\n"));
    }

    @Test
    void noChangeCandidateIsReportedHonestly() throws Exception {
        String relative = "org.eclipse.nebula.nebface/src/example/NoChange.java";
        String before =
                """
                package example;
                final class NoChange {
                    static int divide(int a, int b) {
                        return a / b;
                    }
                }
                """;
        write(relative, before);

        NebulaM3A3Apply.Receipt receipt =
                NebulaM3A3Apply.run(
                                root,
                                Path.of("m3/recipe-first/target/no-change"),
                                List.of(relative))
                        .getFirst();

        assertFalse(receipt.changed());
        assertTrue(receipt.fixedPoint());
        assertEquals(receipt.beforeSha256(), receipt.afterSha256());
        assertEquals(
                before,
                Files.readString(
                        root.resolve(
                                        "m3/recipe-first/target/no-change/candidate")
                                .resolve(relative)));
    }

    @Test
    void sortsDeduplicatesAndKeepsIndependentFilesInOneReceipt()
            throws Exception {
        String first = "widgets/a/src/p/A.java";
        String second = "widgets/b/src/p/B.java";
        write(first, source("A", "a + b"));
        write(second, source("B", "a ^ b"));

        List<NebulaM3A3Apply.Receipt> receipts =
                NebulaM3A3Apply.run(
                        root,
                        Path.of("m3/recipe-first/target/batch"),
                        List.of(second, first, second));

        assertEquals(List.of(first, second), receipts.stream()
                .map(NebulaM3A3Apply.Receipt::path)
                .toList());
        assertTrue(receipts.stream().allMatch(NebulaM3A3Apply.Receipt::fixedPoint));
        assertEquals(source("A", "a + b"), Files.readString(root.resolve(first)));
        assertEquals(source("B", "a ^ b"), Files.readString(root.resolve(second)));
    }

    @Test
    void refusesSourceAndOutputEscapes() throws Exception {
        write("widgets/a/src/p/A.java", source("A", "a + b"));
        write("m3/internal/Tool.java", "final class Tool {}\n");
        write("target/Generated.java", "final class Generated {}\n");
        write("widgets/a/src/p/data.txt", "x\n");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3A3Apply.run(
                                root,
                                Path.of("widgets/a"),
                                List.of("widgets/a/src/p/A.java")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3A3Apply.run(
                                root,
                                Path.of("m3/recipe-first/target/reject-m3"),
                                List.of("m3/internal/Tool.java")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3A3Apply.run(
                                root,
                                Path.of("m3/recipe-first/target/reject-target"),
                                List.of("target/Generated.java")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3A3Apply.run(
                                root,
                                Path.of("m3/recipe-first/target/reject-text"),
                                List.of("widgets/a/src/p/data.txt")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3A3Apply.run(
                                root,
                                Path.of("m3/recipe-first/target/reject-parent"),
                                List.of("../outside.java")));
    }

    private void write(String relative, String content) throws Exception {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static String source(String type, String expression) {
        return """
                package p;
                final class %s {
                    private static int compute(int a, int b) {
                        return %s;
                    }
                }
                """.formatted(type, expression);
    }
}
