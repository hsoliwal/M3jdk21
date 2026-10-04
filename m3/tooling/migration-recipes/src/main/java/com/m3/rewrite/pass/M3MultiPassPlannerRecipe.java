// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * Non-mutating OpenRewrite view of the canonical M3 multi-pass convergence plan.
 *
 * <p>This recipe emits planning evidence only. The recipes listed in a pass retain their own
 * independently tested scope authority; this planner cannot promote or mutate source.
 */
public final class M3MultiPassPlannerRecipe extends Recipe {
    private transient PassTable passes = new PassTable(this);

    @Override
    public String getDisplayName() {
        return "Plan canonical M3 multi-pass convergence";
    }

    @Override
    public String getDescription() {
        return "Emits the bounded inventory-to-proof M3 pass sequence with scope ceilings, "
                + "recipe chains, mutation authority and explicit stop conditions.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "multi-pass",
                "planner",
                "fixed-point",
                "scope-aware",
                "non-mutating",
                "recipe-first");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        M3MultiPassPlan plan = M3MultiPassPlan.canonical();
        return new TreeVisitor<org.openrewrite.Tree, ExecutionContext>() {
            private boolean emitted;

            @Override
            public org.openrewrite.Tree preVisit(
                    org.openrewrite.Tree tree,
                    ExecutionContext context) {
                if (!emitted) {
                    emitted = true;
                    for (M3MultiPassPlan.Pass pass : plan.passes()) {
                        passes.insertRow(
                                context,
                                new PassRow(
                                        pass.ordinal(),
                                        pass.passId(),
                                        pass.mode().name(),
                                        pass.maximumScope().name(),
                                        pass.fileParallel(),
                                        pass.mutationAuthority(),
                                        String.join(">", pass.recipeClasses()),
                                        pass.stopCondition()));
                    }
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    public static final class PassTable extends DataTable<PassRow> {
        PassTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 canonical multi-pass plan",
                    "Bounded pass sequence. Planning rows grant no mutation or promotion authority.");
        }
    }

    public static final class PassRow {
        @Column(displayName = "Ordinal", description = "Canonical pass order.")
        private final int ordinal;
        @Column(displayName = "Pass ID", description = "Stable pass identity.")
        private final String passId;
        @Column(displayName = "Mode", description = "Inventory/transform/relation/fan-in/admission/verify.")
        private final String mode;
        @Column(displayName = "Maximum scope", description = "Broadest authority this pass may require.")
        private final String maximumScope;
        @Column(displayName = "FILE parallel", description = "Whether work may fan out independently by file.")
        private final boolean fileParallel;
        @Column(displayName = "Mutation authority", description = "Whether this pass may apply tested source mutations.")
        private final boolean mutationAuthority;
        @Column(displayName = "Recipe chain", description = "Admitted recipes executed within this pass.")
        private final String recipeChain;
        @Column(displayName = "Stop condition", description = "Explicit convergence condition for the pass.")
        private final String stopCondition;

        PassRow(
                int ordinal,
                String passId,
                String mode,
                String maximumScope,
                boolean fileParallel,
                boolean mutationAuthority,
                String recipeChain,
                String stopCondition) {
            this.ordinal = ordinal;
            this.passId = passId;
            this.mode = mode;
            this.maximumScope = maximumScope;
            this.fileParallel = fileParallel;
            this.mutationAuthority = mutationAuthority;
            this.recipeChain = recipeChain;
            this.stopCondition = stopCondition;
        }

        public int ordinal() { return ordinal; }
        public String passId() { return passId; }
        public String mode() { return mode; }
        public String maximumScope() { return maximumScope; }
        public boolean fileParallel() { return fileParallel; }
        public boolean mutationAuthority() { return mutationAuthority; }
        public String recipeChain() { return recipeChain; }
        public String stopCondition() { return stopCondition; }
    }
}
