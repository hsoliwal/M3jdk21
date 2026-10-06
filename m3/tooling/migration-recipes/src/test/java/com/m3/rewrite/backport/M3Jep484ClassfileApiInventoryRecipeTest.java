// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3Jep484ClassfileApiInventoryRecipeTest implements RewriteTest {
    @Test
    void emitsCumulativeLineageAndOptInOnlyJava21PolicyWithoutChangingSource() {
        var recipe = new M3Jep484ClassfileApiInventoryRecipe();
        rewriteRun(
                spec -> spec.recipe(recipe)
                        .dataTable(M3Jep484ClassfileApiInventoryRecipe.StageRow.class, rows -> {
                            assertEquals(4, rows.size());
                            assertEquals("JDK21-INTERNAL-CLASSFILE", rows.get(0).identity());
                            assertEquals("JEP-457", rows.get(1).identity());
                            assertEquals("JEP-466", rows.get(2).identity());
                            assertEquals("JEP-484", rows.get(3).identity());
                            assertEquals(
                                    "2b00ac0d02a110326846c75ea7ea535dccbb1924",
                                    rows.get(1).commit());
                            assertEquals(
                                    "19a99d023e32fa9f4d26b76bd36993719e1dfe21",
                                    rows.get(2).commit());
                            assertEquals(
                                    "84ffb64cd73f8af11cf3670c6f19d282c2ac6961",
                                    rows.get(3).commit());
                            assertEquals("LIBRARY_API", rows.get(3).scope());
                            assertEquals("NO", rows.get(3).defaultJava21());
                            assertEquals("JEP-466", rows.get(3).dependsOn());
                            assertEquals(165, rows.get(3).headlineTouchedPaths());
                            assertTrue(rows.stream().allMatch(row -> row.root().matches("[0-9a-f]{64}")));
                        })
                        .dataTable(M3Jep484ClassfileApiInventoryRecipe.SummaryRow.class, rows -> {
                            assertEquals(1, rows.size());
                            var row = rows.getFirst();
                            assertEquals(241, row.jdk21InternalFiles());
                            assertEquals(0, row.jdk21PublicFiles());
                            assertEquals(161, row.jdk24PublicFiles());
                            assertEquals(84, row.jdk24InternalFiles());
                            assertEquals(245, row.jdk24CombinedFiles());
                            assertEquals(238, row.normalizedDirectDescendants());
                            assertEquals(3, row.normalizedRemoved());
                            assertEquals(7, row.normalizedAdded());
                            assertEquals(59, row.historyCommits());
                            assertEquals("LIBRARY_API", row.scope());
                            assertEquals("NO", row.defaultJava21());
                            assertEquals("YES", row.optInExtension());
                            assertEquals("EXPLICIT_CONTRACT_CHANGE", row.contract());
                            assertEquals(
                                    "JAVA21_MAJOR_65_DEFAULT_REQUIRED",
                                    row.classfileDefault());
                            assertTrue(row.planRoot().matches("[0-9a-f]{64}"));
                            assertFalse(row.mutationAuthority());
                            assertFalse(row.promotionAuthority());
                        }),
                java(
                        """
                        package example;
                        final class Unchanged {
                            int value() { return 21; }
                        }
                        """));
    }

    @Test
    void metadataAndPlanRootAreDeterministic() {
        var recipe = new M3Jep484ClassfileApiInventoryRecipe();
        assertTrue(recipe.getDisplayName().contains("JEP 484"));
        assertTrue(recipe.getDescription().contains("JDK21-internal"));
        assertTrue(recipe.getTags().contains("jep-457"));
        assertTrue(recipe.getTags().contains("jep-466"));
        assertTrue(recipe.getTags().contains("jep-484"));
        assertTrue(recipe.getTags().contains("non-mutating"));
        assertEquals(1, recipe.maxCycles());
        assertFalse(recipe.causesAnotherCycle());
        assertEquals(4, M3Jep484ClassfileApiInventoryRecipe.stages().size());
        assertEquals(
                M3Jep484ClassfileApiInventoryRecipe.planRoot(),
                M3Jep484ClassfileApiInventoryRecipe.planRoot());
        assertTrue(M3Jep484ClassfileApiInventoryRecipe.planRoot().matches("[0-9a-f]{64}"));
    }
}
