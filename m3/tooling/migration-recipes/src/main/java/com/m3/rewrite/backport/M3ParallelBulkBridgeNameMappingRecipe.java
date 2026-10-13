// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Records the optional M3 parallel bulk provider without changing the existing JDK scheduler. */
public final class M3ParallelBulkBridgeNameMappingRecipe extends Recipe {
    static final String PATH = "m3/docs/name-mapping.json";
    static final String MARKER =
            "\"source_family\": \"Synexia/M3JDK reviewed bulk executor plus M3 parallel fabric\"";
    static final String ANCHOR = "\n  ],\n  \"recipe_authority\": {";
    static final String ENTRY =
            """
                {
                  "source_family": "Synexia/M3JDK reviewed bulk executor plus M3 parallel fabric",
                  "target_family": "optional M3 parallel bulk provider",
                  "public_api": "none; optional independent m3/ports module",
                  "target_owners": [
                    "com.m3.parallel.bulk.M3ParallelIntAddProvider",
                    "jdk.internal.vm.parallel.BulkScheduler",
                    "com.m3.parallel.Plan",
                    "com.m3.parallel.Verified"
                  ],
                  "rule": "One reviewed bounded integer lane; no arbitrary Runnable; snapshotted inputs; required accelerators fail closed; full independent Java parity; no java.base dependency on the M3 parallel provider."
                }
            """;

    @Override
    public String getDisplayName() {
        return "Map optional M3 parallel bulk provider";
    }

    @Override
    public String getDescription() {
        return "Adds one internal provider-owner mapping without modifying the existing "
                + "jdk.internal.vm.parallel family or source semantics.";
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
                int existing = source.indexOf(MARKER);
                if (existing >= 0) {
                    if (existing != source.lastIndexOf(MARKER)) {
                        throw new IllegalStateException("duplicate M3 parallel bulk mapping");
                    }
                    return text;
                }
                int offset = source.indexOf(ANCHOR);
                if (offset < 4 || offset != source.lastIndexOf(ANCHOR)
                        || !source.substring(0, offset).endsWith("    }")) {
                    throw new IllegalStateException("M3 parallel bulk map anchor drift");
                }
                return text.withText(source.substring(0, offset) + ",\n"
                        + ENTRY + source.substring(offset));
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
