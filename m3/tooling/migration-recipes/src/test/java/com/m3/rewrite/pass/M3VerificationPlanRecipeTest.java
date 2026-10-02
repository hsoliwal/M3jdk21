// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

final class M3VerificationPlanRecipeTest implements RewriteTest {
    @Test
    void proofOrderIsStableAndRuntimeIsConditionalButBlockingWhenRequested() {
        List<M3VerificationPlanRecipe.Step> steps = M3VerificationPlanRecipe.steps();

        assertEquals(5, steps.size());
        assertEquals("DIFF", steps.get(0).phase());
        assertEquals("LINT", steps.get(1).phase());
        assertEquals("COMPILE", steps.get(2).phase());
        assertEquals("TESTS", steps.get(3).phase());
        assertEquals("RUNTIME", steps.get(4).phase());

        for (int index = 0; index < steps.size(); index++) {
            assertEquals(index, steps.get(index).ordinal());
            assertTrue(steps.get(index).stopOnFailure());
            assertFalse(steps.get(index).successCondition().isBlank());
        }

        assertTrue(steps.get(0).required());
        assertTrue(steps.get(1).required());
        assertTrue(steps.get(2).required());
        assertTrue(steps.get(3).required());
        assertFalse(steps.get(4).required());
    }

    @Test
    void recipeEmitsRequirementsWithoutMutatingSource() {
        rewriteRun(
                spec -> spec.recipe(new M3VerificationPlanRecipe())
                        .dataTable(M3VerificationPlanRecipe.Row.class, rows -> {
                            assertEquals(5, rows.size());
                            assertEquals("DIFF", rows.get(0).phase());
                            assertEquals("LINT", rows.get(1).phase());
                            assertEquals("COMPILE", rows.get(2).phase());
                            assertEquals("TESTS", rows.get(3).phase());
                            assertEquals("RUNTIME", rows.get(4).phase());
                            assertTrue(rows.get(0).required());
                            assertFalse(rows.get(4).required());
                            assertTrue(rows.stream().allMatch(M3VerificationPlanRecipe.Row::stopOnFailure));
                        }),
                java(
                        """
                        package p;
                        final class A {
                            int value() { return 1; }
                        }
                        """));
    }

    @Test
    void metadataSaysPlanNotProof() {
        var recipe = new M3VerificationPlanRecipe();
        assertTrue(recipe.getDisplayName().contains("verification"));
        assertTrue(recipe.getDescription().contains("without executing or claiming proof"));
        assertTrue(recipe.getTags().contains("proof"));
        assertTrue(recipe.getTags().contains("non-mutating"));
    }

    @Test
    void stepRecordRejectsInvalidMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3VerificationPlanRecipe.Step(-1, "DIFF", true, true, "ok"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3VerificationPlanRecipe.Step(0, "", true, true, "ok"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3VerificationPlanRecipe.Step(0, "DIFF", true, true, ""));
    }
}
