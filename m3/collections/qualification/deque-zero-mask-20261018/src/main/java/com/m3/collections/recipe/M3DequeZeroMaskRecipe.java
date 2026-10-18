/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

/** Source-sealed validate-then-identity fast path for the M3 circular deque. */
public final class M3DequeZeroMaskRecipe extends Recipe {
    private static final String PATH = "M3LongArrayDeque.java";
    private static final String BEFORE =
            "        M3IndexMaskCompactor.validate(alignedStates, size, requiredMask);\n"
          + "        int previous = size;";
    private static final String AFTER =
            "        M3IndexMaskCompactor.validate(alignedStates, size, requiredMask);\n"
          + "        // A zero required mask admits every logical element. Validate first, then preserve\n"
          + "        // the circular storage, logical size, and iterator epoch without scanning the ring.\n"
          + "        if (requiredMask == 0) return 0;\n"
          + "        int previous = size;";

    @Override
    public String getDisplayName() {
        return "Add the M3 deque zero-mask fast path";
    }

    @Override
    public String getDescription() {
        return "Reuses the existing list identity-selection contract without changing the deque API or JNI ABI.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText source, ExecutionContext ctx) {
                PlainText text = super.visitText(source, ctx);
                if (!source.getSourcePath().toString().endsWith(PATH)) {
                    return text;
                }
                String original = text.getText();
                int first = original.indexOf(BEFORE);
                if (first < 0 || original.indexOf(BEFORE, first + BEFORE.length()) >= 0) {
                    throw new IllegalStateException("M3 deque source drift: expected exactly one sealed atom");
                }
                return text.withText(original.substring(0, first) + AFTER
                        + original.substring(first + BEFORE.length()));
            }
        };
    }
}