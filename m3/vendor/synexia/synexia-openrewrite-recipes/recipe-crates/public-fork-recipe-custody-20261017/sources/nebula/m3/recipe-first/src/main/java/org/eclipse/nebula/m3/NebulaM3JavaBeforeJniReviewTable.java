// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/** Read-only Java/JNI admission evidence. */
public final class NebulaM3JavaBeforeJniReviewTable
        extends DataTable<NebulaM3JavaBeforeJniReviewTable.Row> {

    public NebulaM3JavaBeforeJniReviewTable(Recipe recipe) {
        super(
                recipe,
                "Nebula M3 Java-before-JNI review",
                "Fail-closed native review rows; Java remains the semantic oracle.");
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative Java source path.")
        private final String sourcePath;

        @Column(displayName = "Native methods", description = "Native declaration count in the file.")
        private final int nativeMethods;

        @Column(displayName = "Fast-search signal", description = "Observed file-local search signals.")
        private final String fastSearchSignal;

        @Column(displayName = "Decision", description = "Deterministic Java/JNI review decision.")
        private final String decision;

        @Column(displayName = "Reason", description = "Reason for the review decision.")
        private final String reason;

        @Column(displayName = "Java oracle required", description = "Always true.")
        private final boolean javaOracleRequired;

        @Column(displayName = "Differential corpus required", description = "Required before native candidate.")
        private final boolean differentialCorpusRequired;

        @Column(displayName = "Lifecycle/fallback required", description = "Required before native candidate.")
        private final boolean lifecycleFallbackRequired;

        @Column(displayName = "Setup benchmark required", description = "Required before performance claim.")
        private final boolean setupIncludedBenchmarkRequired;

        @Column(displayName = "Native execution authority", description = "Always false in review.")
        private final boolean nativeExecutionAuthority;

        @Column(displayName = "Promotion authority", description = "Always false in review.")
        private final boolean promotionAuthority;

        Row(
                NebulaM3InventoryRecipe.SourceFacts facts,
                NebulaM3JavaBeforeJniPolicy.Review review) {
            this.sourcePath = facts.sourcePath();
            this.nativeMethods = facts.nativeMethodCount();
            this.fastSearchSignal = facts.fastSearchSignal();
            this.decision = review.decision().name();
            this.reason = review.reason();
            this.javaOracleRequired = review.javaOracleRequired();
            this.differentialCorpusRequired = review.differentialCorpusRequired();
            this.lifecycleFallbackRequired = review.lifecycleFallbackRequired();
            this.setupIncludedBenchmarkRequired = review.setupIncludedBenchmarkRequired();
            this.nativeExecutionAuthority = review.nativeExecutionAuthority();
            this.promotionAuthority = review.promotionAuthority();
        }

        public String getSourcePath() {
            return sourcePath;
        }

        public int getNativeMethods() {
            return nativeMethods;
        }

        public String getFastSearchSignal() {
            return fastSearchSignal;
        }

        public String getDecision() {
            return decision;
        }

        public String getReason() {
            return reason;
        }

        public boolean isJavaOracleRequired() {
            return javaOracleRequired;
        }

        public boolean isDifferentialCorpusRequired() {
            return differentialCorpusRequired;
        }

        public boolean isLifecycleFallbackRequired() {
            return lifecycleFallbackRequired;
        }

        public boolean isSetupIncludedBenchmarkRequired() {
            return setupIncludedBenchmarkRequired;
        }

        public boolean isNativeExecutionAuthority() {
            return nativeExecutionAuthority;
        }

        public boolean isPromotionAuthority() {
            return promotionAuthority;
        }
    }
}
