// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Recovers provider/scheduler owners while preserving the already-admitted Tornado adapter owner. */
public final class M3BulkSchedulerRecoveryNameMappingRecipe extends Recipe {
    static final String PATH = "m3/docs/name-mapping.json";
    static final String OLD = "    {\n      \"source_family\": \"Synexia BulkTask/BulkExecutor/M3BulkExecutor + TornadoVM verified RV32IM bulk provider\",\n      \"target_family\": \"M3 internal bulk scheduler\",\n      \"public_api\": \"none; internal jdk.internal.vm.parallel candidate\",\n      \"target_owners\": [\n        \"jdk.internal.vm.parallel.BulkTask\",\n        \"jdk.internal.vm.parallel.BulkExecutor\",\n        \"jdk.internal.vm.parallel.BulkExecution\",\n        \"com.m3.tornado.Rv32iBulkExecutorAdapter\"\n      ],\n      \"rule\": \"Reviewed operation identity plus bounded work geometry; arbitrary Runnable is not accelerator admission; providers stay outside java.base and accelerated admission requires provider-specific proof including CPU parity where applicable.\"\n    }";
    static final String NEW = "    {\n      \"source_family\": \"Synexia bulk executor/provider/scheduler + TornadoVM verified RV32IM provider\",\n      \"target_family\": \"M3 internal bulk scheduler\",\n      \"public_api\": \"none; internal jdk.internal.vm.parallel candidate\",\n      \"target_owners\": [\n        \"jdk.internal.vm.parallel.BulkTask\",\n        \"jdk.internal.vm.parallel.BulkExecutor\",\n        \"jdk.internal.vm.parallel.BulkExecution\",\n        \"jdk.internal.vm.parallel.BulkExecutorProvider\",\n        \"jdk.internal.vm.parallel.BulkScheduler\",\n        \"com.m3.tornado.Rv32iBulkExecutorAdapter\"\n      ],\n      \"rule\": \"One reviewed operation over bounded independent work items; deterministic provider order is priority descending then provider ID; arbitrary Runnable is not accelerator admission; providers remain outside java.base and accelerated success requires provider-specific proof.\"\n    }";

    @Override
    public String getDisplayName() {
        return "Recover M3 bulk scheduler provider ownership";
    }

    @Override
    public String getDescription() {
        return "Adds the lost provider/scheduler target owners to the current bulk mapping without "
                + "removing the already-admitted Tornado adapter owner.";
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
                    throw new IllegalStateException("M3JDK bulk scheduler recovery mapping drift");
                }
                return text.withText(source.substring(0, first) + NEW
                        + source.substring(first + OLD.length()));
            }
        };
    }

    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
}
