// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Content-addressed Java-grade qualification over the canonical Nebula M3 proof and backport stack.
 *
 * <p>This class runs no mutation. It binds independently produced proof evidence to the exact
 * recipe DAG, scheduler projections, scope ladder, transfer contract and backport plan. A fully
 * qualified receipt still requires manual transfer review and never grants mutation/promotion.</p>
 */
public final class NebulaM3JavaGradeQualification {
    public static final String SCHEMA = "NEBULA_M3_JAVA_GRADE_V1";
    public static final double MINIMUM_SEMANTIC_LINE_COVERAGE = 0.99d;

    public record Receipt(
            NebulaM3ProofGate.Receipt proof,
            String transferRoot,
            String backportPlanRoot,
            String backportBundleRoot,
            String orchestratorPlansRoot,
            List<NebulaM3RefactorScope> scopeOrder,
            Set<NebulaM3RecipeDagManifest.Orchestrator> orchestrators) {
        public Receipt {
            proof = Objects.requireNonNull(proof, "proof");
            transferRoot = sha(transferRoot, "transferRoot");
            backportPlanRoot = sha(backportPlanRoot, "backportPlanRoot");
            backportBundleRoot = sha(backportBundleRoot, "backportBundleRoot");
            orchestratorPlansRoot = sha(orchestratorPlansRoot, "orchestratorPlansRoot");
            scopeOrder = List.copyOf(Objects.requireNonNull(scopeOrder, "scopeOrder"));
            orchestrators = Set.copyOf(Objects.requireNonNull(orchestrators, "orchestrators"));

            if (!NebulaM3RecipeDagManifest.root().equals(proof.dagRoot())) {
                throw new IllegalArgumentException("proof DAG root drift");
            }
            if (!NebulaM3TransferContract.root().equals(transferRoot)) {
                throw new IllegalArgumentException("transfer root drift");
            }
            if (!NebulaM3BackportPlan.root().equals(backportPlanRoot)) {
                throw new IllegalArgumentException("backport plan root drift");
            }
            if (!NebulaM3BackportBundle.root().equals(backportBundleRoot)) {
                throw new IllegalArgumentException("backport bundle root drift");
            }
            if (!NebulaM3OrchestratorPlans.root().equals(orchestratorPlansRoot)) {
                throw new IllegalArgumentException("orchestrator plans root drift");
            }
            if (!scopeOrder.equals(NebulaM3RefactorScope.canonicalOrder())) {
                throw new IllegalArgumentException("refactor scope order drift");
            }
            if (!orchestrators.equals(NebulaM3RecipeDagManifest.orchestrationTargets())) {
                throw new IllegalArgumentException("orchestrator target set drift");
            }
            if (NebulaM3TransferContract.JAVA_RELEASE != 21
                    || !"8.90.4".equals(NebulaM3TransferContract.OPENREWRITE_VERSION)
                    || NebulaM3RecipeDagManifest.orchestratorMutationAuthority()
                    || NebulaM3RecipeDagManifest.orchestratorPromotionAuthority()
                    || NebulaM3TransferContract.directSourceMutationAuthority()
                    || NebulaM3TransferContract.promotionAuthority()
                    || NebulaM3BackportPlan.directSourceMutationAuthority()
                    || NebulaM3BackportPlan.promotionAuthority()) {
                throw new IllegalArgumentException("Java-grade baseline/authority drift");
            }
        }

        public boolean qualified() {
            return proof.complete();
        }

        public String disposition() {
            return qualified() ? "MANUAL_TRANSFER_REVIEW_REQUIRED" : "HOLD";
        }

        public boolean mutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }

        public String root() {
            StringBuilder framed = new StringBuilder();
            append(framed, SCHEMA);
            append(framed, Integer.toString(NebulaM3TransferContract.JAVA_RELEASE));
            append(framed, NebulaM3TransferContract.OPENREWRITE_VERSION);
            append(framed, Double.toString(MINIMUM_SEMANTIC_LINE_COVERAGE));
            append(framed, proof.root());
            append(framed, proof.dagRoot());
            append(framed, transferRoot);
            append(framed, backportPlanRoot);
            append(framed, backportBundleRoot);
            append(framed, orchestratorPlansRoot);
            append(framed, NebulaM3TransferContract.refactorScopeOrder());
            orchestrators.stream().sorted(java.util.Comparator.comparing(Enum::name))
                    .forEach(value -> append(framed, value.name()));
            NebulaM3BackportPlan.steps().forEach(step -> {
                append(framed, step.id());
                append(framed, step.repository());
                append(framed, step.maximumScope().name());
            });
            append(framed, disposition());
            append(framed, "mutation=false;promotion=false");
            return sha256(framed.toString());
        }

        public String toTsv() {
            return """
                    key	value
                    schema	%s
                    javaRelease	%d
                    openRewriteVersion	%s
                    minimumSemanticLineCoverage	%.2f
                    proofRoot	%s
                    dagRoot	%s
                    transferRoot	%s
                    backportPlanRoot	%s
                    backportBundleRoot	%s
                    orchestratorPlansRoot	%s
                    refactorScopeOrder	%s
                    orchestrators	%s
                    transferTargets	%s
                    disposition	%s
                    mutationAuthority	false
                    promotionAuthority	false
                    qualificationRoot	%s
                    """
                    .formatted(
                            SCHEMA,
                            NebulaM3TransferContract.JAVA_RELEASE,
                            NebulaM3TransferContract.OPENREWRITE_VERSION,
                            MINIMUM_SEMANTIC_LINE_COVERAGE,
                            proof.root(),
                            proof.dagRoot(),
                            transferRoot,
                            backportPlanRoot,
                            backportBundleRoot,
                            orchestratorPlansRoot,
                            NebulaM3TransferContract.refactorScopeOrder(),
                            orchestrators.stream().sorted(java.util.Comparator.comparing(Enum::name))
                                    .map(Enum::name).collect(java.util.stream.Collectors.joining(",")),
                            NebulaM3TransferContract.targets().stream()
                                    .map(NebulaM3TransferContract.Target::repository)
                                    .collect(java.util.stream.Collectors.joining(",")),
                            disposition(),
                            root());
        }
    }

    private NebulaM3JavaGradeQualification() {}

    public static Receipt fromProof(NebulaM3ProofGate.Receipt proof) {
        return new Receipt(
                proof,
                NebulaM3TransferContract.root(),
                NebulaM3BackportPlan.root(),
                NebulaM3BackportBundle.root(),
                NebulaM3OrchestratorPlans.root(),
                NebulaM3RefactorScope.canonicalOrder(),
                NebulaM3RecipeDagManifest.orchestrationTargets());
    }

    public static Receipt pending() {
        return fromProof(NebulaM3ProofGate.pending(NebulaM3RecipeDagManifest.root()));
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return value;
    }

    private static void append(StringBuilder target, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(value);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
