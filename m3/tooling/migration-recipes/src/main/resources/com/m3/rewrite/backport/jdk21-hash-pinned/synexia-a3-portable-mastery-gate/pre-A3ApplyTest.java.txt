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
                        List.of(relative));

        assertEquals(1, receipts.size());
        A3Apply.Receipt receipt = receipts.getFirst();
        assertEquals(relative, receipt.path());
        assertEquals("FILE", receipt.scope());
        assertEquals(A3RecipeHome.RECIPE_CLASS, receipt.recipe());
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
                                        "src/java.base/share/classes/a.txt")));
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
                                List.of(relative))
                        .getFirst();

        assertFalse(receipt.changed());
        assertTrue(receipt.fixedPoint());
        assertEquals(
                before,
                Files.readString(
                        root.resolve("m3/build/a3/no-change/candidate")
                                .resolve(relative)));
    }
}
