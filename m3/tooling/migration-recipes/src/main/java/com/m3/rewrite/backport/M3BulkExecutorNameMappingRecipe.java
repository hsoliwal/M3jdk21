// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Adds the target-owned bulk scheduler family to the canonical M3JDK name map. */
public final class M3BulkExecutorNameMappingRecipe extends Recipe {
    static final String PATH = "m3/docs/name-mapping.json";
    static final String MARKER =
            "\"source_family\": \"Synexia BulkTask/BulkExecutor/M3BulkExecutor"
                    + " + TornadoVM verified RV32IM bulk provider\"";
    static final String ANCHOR = "\n  ],\n  \"recipe_authority\": {";
    static final String ENTRY =
            """
                {
                  "source_family": "Synexia BulkTask/BulkExecutor/M3BulkExecutor + TornadoVM verified RV32IM bulk provider",
                  "target_family": "M3 internal bulk scheduler",
                  "public_api": "none; internal jdk.internal.vm.parallel candidate",
                  "target_owners": [
                    "jdk.internal.vm.parallel.BulkTask",
                    "jdk.internal.vm.parallel.BulkExecutor",
                    "jdk.internal.vm.parallel.BulkExecution"
                  ],
                  "rule": "Reviewed operation identity plus bounded work geometry; arbitrary Runnable is not accelerator admission; providers stay outside java.base and accelerated admission requires provider-specific proof including CPU parity where applicable."
                }
            """;

    @Override
    public String getDisplayName() {
        return "Map M3 bulk executor ownership";
    }

    @Override
    public String getDescription() {
        return "Adds the target-owned internal bulk scheduler family without changing public JDK APIs.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof PlainText text)
                        || !PATH.equals(text.getSourcePath().toString().replace('\\', '/'))) {
                    return tree;
                }
                stopAfterPreVisit();
                String source = text.getText();
                if (source.contains(MARKER)) {
                    return text;
                }
                int offset = source.indexOf(ANCHOR);
                if (offset < 4 || offset != source.lastIndexOf(ANCHOR)
                        || !source.substring(0, offset).endsWith("    }")) {
                    throw new IllegalStateException("M3JDK bulk mapping anchor drift");
                }
                return text.withText(
                        source.substring(0, offset)
                                + ",\n"
                                + ENTRY
                                + source.substring(offset));
            }
        };
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
