/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

final class M3DequeZeroMaskRecipeTest implements RewriteTest {
    @Override
    public void defaults(org.openrewrite.test.RecipeSpec spec) {
        spec.recipe(new M3DequeZeroMaskRecipe());
    }

    @Test
    void insertsIdentityFastPathOnlyAfterValidation() {
        rewriteRun(
            text(
                "class M3LongArrayDeque {\n"
              + "    int retainByStateMask(byte[] alignedStates, int requiredMask) {\n"
              + "        M3IndexMaskCompactor.validate(alignedStates, size, requiredMask);\n"
              + "        int previous = size;\n"
              + "        return previous;\n"
              + "    }\n"
              + "}\n",
                "class M3LongArrayDeque {\n"
              + "    int retainByStateMask(byte[] alignedStates, int requiredMask) {\n"
              + "        M3IndexMaskCompactor.validate(alignedStates, size, requiredMask);\n"
              + "        // A zero required mask admits every logical element. Validate first, then preserve\n"
              + "        // the circular storage, logical size, and iterator epoch without scanning the ring.\n"
              + "        if (requiredMask == 0) return 0;\n"
              + "        int previous = size;\n"
              + "        return previous;\n"
              + "    }\n"
              + "}\n",
                spec -> spec.path("M3LongArrayDeque.java")
            )
        );
    }

    @Test
    void leavesUnrelatedSourceUnchanged() {
        rewriteRun(text("class Unrelated { int value; }\n", spec -> spec.path("Unrelated.java")));
    }

    @Test
    void refusesMissingAtom() {
        org.openrewrite.ExecutionContext ctx =
                new org.openrewrite.InMemoryExecutionContext(Throwable::printStackTrace);
        org.openrewrite.text.PlainText missing = org.openrewrite.text.PlainText.builder()
                .sourcePath(java.nio.file.Paths.get("M3LongArrayDeque.java"))
                .text("class M3LongArrayDeque {}\n").build();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new M3DequeZeroMaskRecipe().getVisitor().visit(missing, ctx));

        String duplicateAtom = "M3IndexMaskCompactor.validate(alignedStates, size, requiredMask);\\n"
                + "        int previous = size;\\n"
                + "        M3IndexMaskCompactor.validate(alignedStates, size, requiredMask);\\n"
                + "        int previous = size;\\n";
        org.openrewrite.text.PlainText duplicate = org.openrewrite.text.PlainText.builder()
                .sourcePath(java.nio.file.Paths.get("M3LongArrayDeque.java"))
                .text(duplicateAtom).build();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new M3DequeZeroMaskRecipe().getVisitor().visit(duplicate, ctx));
    }
}