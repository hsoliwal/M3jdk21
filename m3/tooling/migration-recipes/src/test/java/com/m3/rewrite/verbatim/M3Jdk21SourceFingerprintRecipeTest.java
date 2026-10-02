// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.verbatim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3Jdk21SourceFingerprintRecipeTest implements RewriteTest {
    @Test
    void emitsExactRenderedOpenJdkSourceFingerprintWithoutMutation() {
        var recipe = new M3Jdk21SourceFingerprintRecipe();
        rewriteRun(
                spec -> spec.recipe(recipe)
                        .dataTable(M3Jdk21SourceFingerprintRecipe.Row.class, rows -> {
                            assertEquals(1, rows.size());
                            var row = rows.getFirst();
                            assertEquals(
                                    "src/java.base/share/classes/p/A.java",
                                    row.sourcePath());
                            assertEquals(
                                    "9ee9181e34c29dfe1067865618b65e5fbb1e4db116d502f95bbab20f220a9f0e",
                                    row.sha256());
                            assertEquals(72, row.utf8Bytes());
                            assertEquals(8, row.lineCount());
                        }),
                java(
                                """
                                package p;

                                final class A {
                                    int value() {
                                        return 1;
                                    }
                                }
                                """)
                        .path("src/java.base/share/classes/p/A.java"));
    }

    @Test
    void emptyCompilationUnitStillHasAStableFingerprintRow() {
        rewriteRun(
                spec -> spec.recipe(new M3Jdk21SourceFingerprintRecipe())
                        .dataTable(M3Jdk21SourceFingerprintRecipe.Row.class, rows -> {
                            assertEquals(1, rows.size());
                            assertEquals(64, rows.getFirst().sha256().length());
                            assertTrue(rows.getFirst().utf8Bytes() >= 0);
                            assertTrue(rows.getFirst().lineCount() >= 0);
                        }),
                java(""));
    }

    @Test
    void metadataDeclaresFileLocalNonMutatingPreimagePurpose() {
        var recipe = new M3Jdk21SourceFingerprintRecipe();
        assertTrue(recipe.getDisplayName().contains("preimages"));
        assertTrue(recipe.getDescription().contains("without modifying source"));
        assertTrue(recipe.getTags().contains("jdk21"));
        assertTrue(recipe.getTags().contains("verbatim"));
        assertTrue(recipe.getTags().contains("sha256"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("non-mutating"));
    }
}
