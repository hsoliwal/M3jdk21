// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3InventoryPureIntAtomCandidatesTest implements RewriteTest {
    @Test
    void metadataDeclaresNonMutatingFileLocalAtomPatternInventory() {
        var recipe = new M3InventoryPureIntAtomCandidates();
        assertTrue(recipe.getDisplayName().contains("FILE"));
        assertTrue(recipe.getDescription().contains("source is not modified"));
        assertTrue(recipe.getTags().contains("inventory"));
        assertTrue(recipe.getTags().contains("atomization"));
        assertTrue(recipe.getTags().contains("patternization"));
        assertTrue(recipe.getTags().contains("iop"));
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("non-mutating"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    @Test
    void emitsOnlyCandidatesAcceptedByTheMutationRecipeAndDoesNotChangeSource() {
        var recipe = new M3InventoryPureIntAtomCandidates();
        rewriteRun(
                spec -> spec.recipe(recipe)
                        .dataTable(M3FileAtomCandidateTable.Row.class, rows -> {
                            assertEquals(1, rows.size());
                            var row = rows.getFirst();
                            assertEquals("src/main/java/example/Sample.java", row.sourcePath());
                            assertEquals("compute", row.methodName());
                            assertEquals("FILE", row.scope());
                            assertEquals(
                                    "BEHAVIOR_AND_CONTRACT_PRESERVING",
                                    row.contractMode());
                            assertEquals("PURE_INT_EXPRESSION", row.patternRole());
                            assertEquals(
                                    M3AtomizePureIntReturnRecipe.class.getName(),
                                    row.recipeClass());
                        }),
                java(
                                """
                                package example;

                                final class Sample {
                                    private static int compute(int a, int b) {
                                        return (a + b) * 31;
                                    }

                                    public static int notFileLocal(int a, int b) {
                                        return a + b;
                                    }

                                    private static int mayThrow(int a, int b) {
                                        return a / b;
                                    }
                                }
                                """)
                        .path("src/main/java/example/Sample.java"));
    }

    @Test
    void candidateTableCarriesAllMechanicalFields() {
        var table = new M3FileAtomCandidateTable(new M3InventoryPureIntAtomCandidates());
        var row = new M3FileAtomCandidateTable.Row(
                "src/main/java/a/A.java",
                "compute",
                "FILE",
                "BEHAVIOR_AND_CONTRACT_PRESERVING",
                "PURE_INT_EXPRESSION",
                M3AtomizePureIntReturnRecipe.class.getName());

        assertTrue(table.getDisplayName().contains("FILE"));
        assertEquals("src/main/java/a/A.java", row.sourcePath());
        assertEquals("compute", row.methodName());
        assertEquals("FILE", row.scope());
        assertEquals("BEHAVIOR_AND_CONTRACT_PRESERVING", row.contractMode());
        assertEquals("PURE_INT_EXPRESSION", row.patternRole());
        assertEquals(M3AtomizePureIntReturnRecipe.class.getName(), row.recipeClass());
    }
}
