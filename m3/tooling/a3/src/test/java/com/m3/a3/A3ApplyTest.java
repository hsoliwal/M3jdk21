// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3ApplyTest {

    @TempDir
    Path root;

    @Test
    void appliesRetainedConvergenceRecipeWithoutWritingJdkSource()
            throws Exception {
        String relative =
                "src/java.base/share/classes/example/Sample.java";
        String before =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;
        Path source = root.resolve(relative);
        Files.createDirectories(source.getParent());
        Files.writeString(source, before);

        List<A3Apply.Receipt> receipts =
                A3Apply.run(
                        root,
                        Path.of("m3/build/a3/apply"),
                        List.of(relative),
                        A3MasteryTest.receipt());

        assertEquals(1, receipts.size());
        A3Apply.Receipt receipt = receipts.getFirst();
        assertEquals(relative, receipt.path());
        assertEquals("FILE", receipt.scope());
        assertTrue(receipt.changed());
        assertTrue(receipt.fixedPoint());
        assertFalse(receipt.beforeSha().equals(receipt.afterSha()));

        assertEquals(before, Files.readString(source));

        Path candidate =
                root.resolve("m3/build/a3/apply/candidate")
                        .resolve(relative);
        String after = Files.readString(candidate);
        assertTrue(after.contains("int m3$pureIntAtom ="));
        assertTrue(after.contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(after.contains("M3-ATOM: m3$pureIntAtom"));

        String receiptTsv =
                Files.readString(
                        root.resolve("m3/build/a3/apply/receipt.tsv"));
        assertTrue(receiptTsv.contains(relative));
        assertTrue(receiptTsv.contains("\tFILE\t"));
        assertTrue(receiptTsv.endsWith("\ttrue\ttrue\n"));
        String masteryTsv =
                Files.readString(
                        root.resolve("m3/build/a3/apply/mastery.tsv"));
        assertTrue(masteryTsv.contains(A3Mastery.SCHEMA));
        assertTrue(masteryTsv.contains(A3MasteryTest.receipt().root()));
    }

    @Test
    void refusesRootWritesAndNonJavaTargets() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Fs.out(
                                root,
                                Path.of("src")));

        Path text = root.resolve("src/java.base/share/classes/a.txt");
        Files.createDirectories(text.getParent());
        Files.writeString(text, "x");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Apply.run(
                                root,
                                Path.of("m3/build/a3/apply"),
                                List.of(
                                        "src/java.base/share/classes/a.txt"),
                                A3MasteryTest.receipt()));
    }

    @Test
    void legacyOverloadFailsClosedWithoutPinnedMasteryProperties() {
        String beforeReceipt = System.getProperty("m3.a3.mastery.receipt");
        String beforeRoot = System.getProperty("m3.a3.mastery.root");
        try {
            System.clearProperty("m3.a3.mastery.receipt");
            System.clearProperty("m3.a3.mastery.root");
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            A3Apply.run(
                                    root,
                                    Path.of("m3/build/a3/legacy"),
                                    List.of("src/java.base/share/classes/example/Missing.java")));
        } finally {
            restore("m3.a3.mastery.receipt", beforeReceipt);
            restore("m3.a3.mastery.root", beforeRoot);
        }
    }

    @Test
    void unmatchedJavaStillProducesFixedPointCandidateReceipt()
            throws Exception {
        String relative =
                "test/jdk/example/NoChange.java";
        String before =
                """
                final class NoChange {
                    static int divide(int a, int b) {
                        return a / b;
                    }
                }
                """;
        Path source = root.resolve(relative);
        Files.createDirectories(source.getParent());
        Files.writeString(source, before);

        A3Apply.Receipt receipt =
                A3Apply.run(
                                root,
                                Path.of("m3/build/a3/no-change"),
                                List.of(relative),
                                A3MasteryTest.receipt())
                        .getFirst();

        assertFalse(receipt.changed());
        assertTrue(receipt.fixedPoint());
        assertEquals(
                before,
                Files.readString(
                        root.resolve("m3/build/a3/no-change/candidate")
                                .resolve(relative)));
    }
    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

}
