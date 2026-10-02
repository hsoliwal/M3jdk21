// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.dag;

import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/** Non-mutating fuzzy planner over the admitted M3 recipe-DAG catalogue. */
public final class M3RecipeDagPlannerRecipe extends Recipe {
    @Option(
            displayName = "Capability query",
            description = "Natural-language or tokenized capability description.",
            example = "inline MIndexDB donor and build semantic atom index")
    private final String capabilityQuery;

    @Option(
            displayName = "Limit",
            description = "Maximum candidate DAGs to emit.",
            example = "5",
            required = false)
    private final Integer limit;

    private transient PlanTable plans = new PlanTable(this);

    public M3RecipeDagPlannerRecipe(String capabilityQuery) {
        this(capabilityQuery, 5);
    }

    public M3RecipeDagPlannerRecipe(String capabilityQuery, Integer limit) {
        this.capabilityQuery = capabilityQuery;
        this.limit = limit == null ? 5 : limit;
        if (this.limit < 1 || this.limit > 100) throw new IllegalArgumentException("limit");
        // Eagerly validate query and catalogue.
        new M3RecipeDagCatalogue().fuzzy(capabilityQuery, 1);
    }

    @Override
    public String getDisplayName() {
        return "Plan M3 recipe DAG by fuzzy capability match";
    }

    @Override
    public String getDescription() {
        return "Ranks possible recipe DAGs without modifying source or granting promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "catalogue", "fuzzy", "dag", "planner", "non-mutating", "recipe-first");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        var matches = new M3RecipeDagCatalogue().fuzzy(capabilityQuery, limit);
        return new TreeVisitor<org.openrewrite.Tree, ExecutionContext>() {
            private boolean emitted;

            @Override
            public org.openrewrite.Tree preVisit(
                    org.openrewrite.Tree tree, ExecutionContext context) {
                if (!emitted) {
                    emitted = true;
                    for (int ordinal = 0; ordinal < matches.size(); ordinal++) {
                        var match = matches.get(ordinal);
                        plans.insertRow(context, new PlanRow(
                                ordinal,
                                match.entry().dagId(),
                                match.entry().maximumScope().name(),
                                String.join(">", match.entry().recipeClasses()),
                                match.score(),
                                match.exactTokenMatches(),
                                match.simHashDistance(),
                                false));
                    }
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    public static final class PlanTable extends DataTable<PlanRow> {
        PlanTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 fuzzy recipe DAG candidates",
                    "Ranked possible recipe DAGs. Rows are planning evidence and carry no mutation authority.");
        }
    }

    public static final class PlanRow {
        @Column(displayName = "Ordinal", description = "Rank after deterministic fuzzy matching.")
        private final int ordinal;
        @Column(displayName = "DAG ID", description = "Stable catalogue DAG identity.")
        private final String dagId;
        @Column(displayName = "Maximum scope", description = "Broadest scope required by the candidate DAG.")
        private final String maximumScope;
        @Column(displayName = "Recipe chain", description = "Ordered recipe class chain.")
        private final String recipeChain;
        @Column(displayName = "Score", description = "Combined exact-token/Jaccard and SimHash score.")
        private final double score;
        @Column(displayName = "Exact token matches", description = "Count of shared normalized capability tokens.")
        private final int exactTokenMatches;
        @Column(displayName = "SimHash distance", description = "Hamming distance between query and DAG capability SimHash.")
        private final int simHashDistance;
        @Column(displayName = "Mutation authority", description = "Always false for fuzzy catalogue planning.")
        private final boolean mutationAuthority;

        PlanRow(
                int ordinal,
                String dagId,
                String maximumScope,
                String recipeChain,
                double score,
                int exactTokenMatches,
                int simHashDistance,
                boolean mutationAuthority) {
            this.ordinal = ordinal;
            this.dagId = dagId;
            this.maximumScope = maximumScope;
            this.recipeChain = recipeChain;
            this.score = score;
            this.exactTokenMatches = exactTokenMatches;
            this.simHashDistance = simHashDistance;
            this.mutationAuthority = mutationAuthority;
        }

        public int ordinal() { return ordinal; }
        public String dagId() { return dagId; }
        public String maximumScope() { return maximumScope; }
        public String recipeChain() { return recipeChain; }
        public double score() { return score; }
        public int exactTokenMatches() { return exactTokenMatches; }
        public int simHashDistance() { return simHashDistance; }
        public boolean mutationAuthority() { return mutationAuthority; }
    }
}
