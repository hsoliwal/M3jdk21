/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/com/synexia/cpullm/chrome/MechanicalModel.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable value model for the Java mechanical Chrome-AI pipeline. */
public final class MechanicalModel {
    private MechanicalModel() {}

    public enum Authority {
        MODEL_CANDIDATE(10), DESCRIPTION_CANDIDATE(20), OBSERVED(30), USER_DECLARED(40), VERIFIED(50);
        private final int rank;
        Authority(int rank) { this.rank = rank; }
        public int rank() { return rank; }
        public static Authority max(Authority a, Authority b) { return a.rank >= b.rank ? a : b; }
    }

    public enum PatternKind {
        HEADING, DECLARATION, IMPORT, CONSTRAINT, FAILURE, DECISION, CAUSAL,
        LIST_ITEM, BRANCH, CODE, CODE_BLOCK, CONTRACT, TOOL, TEXT
    }

    public enum TrajectoryClass {
        NO_CHANGE,
        SURFACE_CHURN_STRUCTURAL_STALL,
        STRUCTURE_CHANGED_WITH_STABLE_INPUT,
        STRUCTURAL_DELTA,
        STRUCTURAL_CYCLE_RETURN
    }

    public enum PipelineStage {
        INVENTORIED, EXTRACTED, CONSOLIDATED, TREE_PLANNED, MODEL_CANDIDATE, REPAIRED, ASSEMBLED
    }

    public record SourceRecord(
            String sourceRef, String scope, String kind, String boundary, String text, Authority authority) {
        public SourceRecord {
            Objects.requireNonNull(sourceRef);
            Objects.requireNonNull(scope);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(boundary);
            Objects.requireNonNull(text);
            Objects.requireNonNull(authority);
            if (sourceRef.isBlank() || boundary.isBlank()) throw new IllegalArgumentException("sourceRef/boundary required");
        }
    }

    public record EvidenceNode(
            String evidenceId, String sourceRef, int ordinal, PatternKind kind, String text,
            String structuralKey, String scope, String boundary, Authority authority) {
        public EvidenceNode {
            Objects.requireNonNull(evidenceId); Objects.requireNonNull(sourceRef); Objects.requireNonNull(kind);
            Objects.requireNonNull(text); Objects.requireNonNull(structuralKey); Objects.requireNonNull(scope);
            Objects.requireNonNull(boundary); Objects.requireNonNull(authority);
        }
    }

    public record MechanicalClaim(
            String claimId, String structuralKey, PatternKind kind, String text, String boundary,
            Authority authority, List<String> evidenceIds, List<String> sourceRefs) {
        public MechanicalClaim {
            evidenceIds = List.copyOf(evidenceIds);
            sourceRefs = List.copyOf(sourceRefs);
        }
    }

    public record ConflictSet(String conflictId, String structuralKey, List<String> alternatives) {
        public ConflictSet { alternatives = List.copyOf(alternatives); }
    }

    public record StructuredCodeEntropy(
            int sourceCount, long sourceBytes, int evidenceOccurrences, int uniqueClaims,
            int uniqueStructures, int duplicateOccurrences, int duplicatePressurePpm,
            int compressionRatioPpm, int structuralDensityPpm, int conflictCount,
            int byteEntropyMillibits, int tokenEntropyMillibits, String semanticRoot) {}

    public record SupersetBundle(
            List<EvidenceNode> evidence, List<MechanicalClaim> claims, List<ConflictSet> conflicts,
            StructuredCodeEntropy structuredEntropy, String mechanicalRoot,
            boolean candidateOnly, boolean admissionAuthorized, boolean executionAuthorized, boolean mergeAuthorized) {
        public SupersetBundle {
            evidence = List.copyOf(evidence);
            claims = List.copyOf(claims);
            conflicts = List.copyOf(conflicts);
        }
    }

    public record EntropyPoint(String inputRoot, String semanticRoot) {
        public EntropyPoint {
            Objects.requireNonNull(inputRoot);
            Objects.requireNonNull(semanticRoot);
        }
    }

    public record RepairedSelection(
            Set<String> requestedRetainedClaimIds, Set<String> restoredClaimIds, Set<String> finalClaimIds) {
        public RepairedSelection {
            requestedRetainedClaimIds = Set.copyOf(requestedRetainedClaimIds);
            restoredClaimIds = Set.copyOf(restoredClaimIds);
            finalClaimIds = Set.copyOf(finalClaimIds);
        }
    }

    public record SummaryNode(
            String nodeId, String kind, int level, int ordinal, int startByte, int endByte,
            String inputSha256, List<String> children, List<Integer> coverageLeafOrdinals) {
        public SummaryNode {
            children = List.copyOf(children);
            coverageLeafOrdinals = List.copyOf(coverageLeafOrdinals);
        }
    }

    public record SummaryTreePlan(
            String sourceRef, String sourceSha256, int sourceBytes, SummaryTreePlanner.TreeConfig config,
            int leafCount, int levelCount, List<SummaryNode> nodes, String rootNodeId, String mechanicalRoot) {
        public SummaryTreePlan { nodes = List.copyOf(nodes); }
        public boolean verify(String sourceText) { return SummaryTreePlanner.verify(this, sourceText); }
        public SummaryNode root() {
            return nodes.stream().filter(n -> n.nodeId().equals(rootNodeId)).findFirst().orElseThrow();
        }
    }

    public record ChromeRequest(
            String requestId, String nodeId, String input, String inputSha256, List<String> sourceRefs,
            String authority, boolean candidateOnly, int modelCalls, int directNetworkCalls) {
        public ChromeRequest { sourceRefs = List.copyOf(sourceRefs); }
    }

    public record ChromeResult(
            String status, String resultId, String sourceRequestId, List<String> sourceRefs, String text,
            String authority, boolean candidateOnly, boolean admitted, int modelCalls, int directNetworkCalls) {
        public ChromeResult { sourceRefs = List.copyOf(sourceRefs); }
    }

    public record SummaryNodeResult(
            String nodeId, String resultId, String summarySha256, String summary, List<String> sourceRefs) {
        public SummaryNodeResult { sourceRefs = List.copyOf(sourceRefs); }
    }

    public record SummaryTreeCandidate(
            String status, String sourceSha256, String mechanicalRoot, List<SummaryNodeResult> nodeResults,
            String rootResultId, String rootSummary, String candidateRoot,
            boolean candidateOnly, boolean admitted, boolean mergeAuthorized) {
        public SummaryTreeCandidate { nodeResults = List.copyOf(nodeResults); }
    }

    public record AssemblyReceipt(
            String mechanicalRoot, String candidateRoot, String sourceMechanicalRoot,
            String treeMechanicalRoot, String treeCandidateRoot, Set<String> finalClaimIds,
            Set<String> restoredClaimIds, boolean candidateOnly, boolean mergeAuthorized) {
        public AssemblyReceipt {
            finalClaimIds = Set.copyOf(finalClaimIds);
            restoredClaimIds = Set.copyOf(restoredClaimIds);
        }
    }

    public record Checkpoint(String runId, PipelineStage stage, String mechanicalRoot, String candidateRoot) {}

    public record MechanicalRun(
            SupersetBundle superset, SummaryTreePlan plan, SummaryTreeCandidate candidate,
            RepairedSelection repaired, AssemblyReceipt assembly) {}

    public static <K, V> Map<K, V> immutable(Map<K, V> input) { return Map.copyOf(input); }
    public static <T> Optional<T> optional(T value) { return Optional.ofNullable(value); }
}
