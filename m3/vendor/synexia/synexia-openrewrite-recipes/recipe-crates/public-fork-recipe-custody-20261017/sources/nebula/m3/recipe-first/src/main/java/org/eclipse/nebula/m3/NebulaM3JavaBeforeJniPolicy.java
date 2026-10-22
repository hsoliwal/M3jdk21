// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Objects;

/** Fail-closed Java-before-JNI review policy; it grants no native execution authority. */
public final class NebulaM3JavaBeforeJniPolicy {
    public enum Decision {
        NO_NATIVE_ACTION,
        PROFILE_JAVA_HOT_PATH_BEFORE_JNI,
        REVIEW_EXISTING_NATIVE_DECLARATION
    }

    public record Review(
            Decision decision,
            String reason,
            boolean javaOracleRequired,
            boolean differentialCorpusRequired,
            boolean lifecycleFallbackRequired,
            boolean setupIncludedBenchmarkRequired,
            boolean nativeExecutionAuthority,
            boolean promotionAuthority) {
        public Review {
            decision = Objects.requireNonNull(decision, "decision");
            reason = Objects.requireNonNull(reason, "reason").strip();
            if (reason.isEmpty()) {
                throw new IllegalArgumentException("reason");
            }
            if (!javaOracleRequired || nativeExecutionAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "Nebula native review cannot bypass the Java oracle or gain authority");
            }
        }
    }

    public record NativeEvidence(
            String javaOracleSha256,
            String differentialCorpusSha256,
            String lifecycleFallbackSha256,
            String setupIncludedBenchmarkSha256) {
        public NativeEvidence {
            javaOracleSha256 = digest(javaOracleSha256, "javaOracleSha256");
            differentialCorpusSha256 =
                    digest(differentialCorpusSha256, "differentialCorpusSha256");
            lifecycleFallbackSha256 =
                    digest(lifecycleFallbackSha256, "lifecycleFallbackSha256");
            setupIncludedBenchmarkSha256 =
                    digest(setupIncludedBenchmarkSha256, "setupIncludedBenchmarkSha256");
        }
    }

    public record NativeAdmission(
            Decision decision,
            NativeEvidence evidence,
            boolean candidateOnly,
            boolean nativeExecutionAuthority,
            boolean promotionAuthority) {
        public NativeAdmission {
            decision = Objects.requireNonNull(decision, "decision");
            evidence = Objects.requireNonNull(evidence, "evidence");
            if (decision == Decision.NO_NATIVE_ACTION) {
                throw new IllegalArgumentException("NO_NATIVE_ACTION cannot create a native candidate");
            }
            if (!candidateOnly || nativeExecutionAuthority || promotionAuthority) {
                throw new IllegalArgumentException(
                        "native admission is candidate-only and grants no authority");
            }
        }
    }

    private NebulaM3JavaBeforeJniPolicy() {
        throw new AssertionError("No instances");
    }

    public static Review review(NebulaM3InventoryRecipe.SourceFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return review(
                facts.nativeMethodCount() > 0,
                facts.loopCount(),
                facts.fastSearchSignal());
    }

    public static Review review(NebulaM3InventoryRecipe.MethodFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return review(
                facts.nativeMethod(),
                facts.loopCount(),
                facts.fastSearchSignal());
    }

    private static Review review(
            boolean nativeDeclaration,
            int loopCount,
            String fastSearchSignal) {
        if (nativeDeclaration) {
            return candidate(
                    Decision.REVIEW_EXISTING_NATIVE_DECLARATION,
                    "EXISTING_NATIVE_DECLARATION_REQUIRES_PARITY_LIFECYCLE_FALLBACK_AND_BENCHMARK");
        }
        if (loopCount > 0 && !"NONE".equals(fastSearchSignal)) {
            return candidate(
                    Decision.PROFILE_JAVA_HOT_PATH_BEFORE_JNI,
                    "SEARCH_LOOP_IS_ONLY_A_PROFILE_CANDIDATE_NOT_NATIVE_AUTHORITY");
        }
        return new Review(
                Decision.NO_NATIVE_ACTION,
                "NO_PROVEN_HOT_PRIMITIVE",
                true,
                false,
                false,
                false,
                false,
                false);
    }

    public static NativeAdmission admitNativeCandidate(
            Review review, NativeEvidence evidence) {
        Review checked = Objects.requireNonNull(review, "review");
        NativeEvidence checkedEvidence = Objects.requireNonNull(evidence, "evidence");
        if (checked.decision() == Decision.NO_NATIVE_ACTION) {
            throw new IllegalStateException("no proven native candidate");
        }
        if (!checked.javaOracleRequired()
                || !checked.differentialCorpusRequired()
                || !checked.lifecycleFallbackRequired()
                || !checked.setupIncludedBenchmarkRequired()) {
            throw new IllegalStateException("native candidate evidence requirements incomplete");
        }
        return new NativeAdmission(
                checked.decision(),
                checkedEvidence,
                true,
                false,
                false);
    }

    private static Review candidate(Decision decision, String reason) {
        return new Review(
                decision,
                reason,
                true,
                true,
                true,
                true,
                false,
                false);
    }
    private static String digest(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }

}
