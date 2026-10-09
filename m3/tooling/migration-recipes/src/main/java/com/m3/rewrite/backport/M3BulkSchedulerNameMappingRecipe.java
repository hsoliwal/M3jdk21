// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Upgrades the existing bulk-scheduler ownership map with provider/scheduler target owners. */
public final class M3BulkSchedulerNameMappingRecipe extends Recipe {
    static final String PATH = "m3/docs/name-mapping.json";
    static final String OLD =
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
    static final String NEW =
            """
                {
                  "source_family": "Synexia bulk executor/provider/scheduler + TornadoVM verified RV32IM provider",
                  "target_family": "M3 internal bulk scheduler",
                  "public_api": "none; internal jdk.internal.vm.parallel candidate",
                  "target_owners": [
                    "jdk.internal.vm.parallel.BulkTask",
                    "jdk.internal.vm.parallel.BulkExecutor",
                    "jdk.internal.vm.parallel.BulkExecution",
                    "jdk.internal.vm.parallel.BulkExecutorProvider",
                    "jdk.internal.vm.parallel.BulkScheduler"
                  ],
                  "rule": "One reviewed operation over bounded independent work items; deterministic provider order is priority descending then provider ID; arbitrary Runnable is not accelerator admission; providers remain outside java.base and accelerated success requires provider-specific proof."
                }
            """;

    @Override
    public String getDisplayName() {
        return "Upgrade M3 bulk scheduler ownership map";
    }

    @Override
    public String getDescription() {
        return "Adds the target-owned provider and deterministic scheduler owners to the existing "
                + "internal bulk-scheduler mapping.";
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
                if (source.contains(NEW)) return text;
                int first = source.indexOf(OLD);
                if (first < 0 || first != source.lastIndexOf(OLD)) {
                    throw new IllegalStateException("M3JDK bulk scheduler mapping drift");
                }
                return text.withText(
                        source.substring(0, first)
                                + NEW
                                + source.substring(first + OLD.length()));
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
