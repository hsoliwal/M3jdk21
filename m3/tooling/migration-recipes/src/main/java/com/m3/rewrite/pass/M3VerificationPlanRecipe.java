// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import java.util.List;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

/**
 * Non-mutating materialization of the M3 proof order.
 *
 * <p>This recipe does not claim that any proof was executed. It emits the verification contract
 * that an external runner must satisfy before a candidate can be promoted.</p>
 */
public final class M3VerificationPlanRecipe extends Recipe {
    private static final List<Step> STEPS = List.of(
            new Step(0, "DIFF", true, true, "candidate diff is bounded to the declared scope"),
            new Step(1, "LINT", true, true, "configured static checks pass"),
            new Step(2, "COMPILE", true, true, "Java/compiler contract passes"),
            new Step(3, "TESTS", true, true, "configured JUnit/contract tests pass"),
            new Step(4, "RUNTIME", false, true, "requested runtime/benchmark evidence passes"));

    private transient VerificationTable verification = new VerificationTable(this);

    @Override
    public String getDisplayName() {
        return "Emit M3 verification contract";
    }

    @Override
    public String getDescription() {
        return "Emits the mandatory M3 proof order without executing or claiming proof.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "verification",
                "proof",
                "diff",
                "lint",
                "compile",
                "junit",
                "runtime",
                "non-mutating");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<org.openrewrite.Tree, ExecutionContext>() {
            private boolean emitted;

            @Override
            public org.openrewrite.Tree preVisit(
                    org.openrewrite.Tree tree,
                    ExecutionContext context) {
                if (!emitted) {
                    emitted = true;
                    for (Step step : STEPS) {
                        verification.insertRow(
                                context,
                                new Row(
                                        step.ordinal(),
                                        step.phase(),
                                        step.required(),
                                        step.stopOnFailure(),
                                        step.successCondition()));
                    }
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    public static List<Step> steps() {
        return STEPS;
    }

    public record Step(
            int ordinal,
            String phase,
            boolean required,
            boolean stopOnFailure,
            String successCondition) {
        public Step {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            if (phase == null || phase.isBlank()) throw new IllegalArgumentException("phase");
            if (successCondition == null || successCondition.isBlank()) {
                throw new IllegalArgumentException("successCondition");
            }
        }
    }

    public static final class VerificationTable extends DataTable<Row> {
        VerificationTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 verification contract",
                    "Ordered proof gates. Rows are requirements, not execution claims.");
        }
    }

    public static final class Row {
        @Column(displayName = "Ordinal", description = "Mandatory proof order.")
        private final int ordinal;
        @Column(displayName = "Phase", description = "DIFF, LINT, COMPILE, TESTS or RUNTIME.")
        private final String phase;
        @Column(displayName = "Required", description = "Whether the phase is mandatory by default.")
        private final boolean required;
        @Column(displayName = "Stop on failure", description = "Whether failure blocks promotion.")
        private final boolean stopOnFailure;
        @Column(displayName = "Success condition", description = "Evidence required to pass this phase.")
        private final String successCondition;

        Row(
                int ordinal,
                String phase,
                boolean required,
                boolean stopOnFailure,
                String successCondition) {
            this.ordinal = ordinal;
            this.phase = phase;
            this.required = required;
            this.stopOnFailure = stopOnFailure;
            this.successCondition = successCondition;
        }

        public int ordinal() { return ordinal; }
        public String phase() { return phase; }
        public boolean required() { return required; }
        public boolean stopOnFailure() { return stopOnFailure; }
        public String successCondition() { return successCondition; }
    }
}
