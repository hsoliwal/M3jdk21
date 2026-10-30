/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/com/synexia/cpullm/chrome/JavaChromeAiMechanicalPipeline.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Facade + builder + state/observer orchestration for the complete Java mechanical lane.
 *
 * <p>Mechanical authority is always independent of model output. Structured Prompt-API
 * output may propose retained claim IDs; omission repair restores every mechanically
 * admitted claim before publication.
 */
public final class JavaChromeAiMechanicalPipeline {
    @FunctionalInterface
    public interface ProgressListener {
        void onStage(PipelineStage stage, String root);
        static ProgressListener noop() { return (stage, root) -> {}; }
    }

    public static final class Builder {
        /** Preserves the donor's implicitly public no-argument construction contract. */
        public Builder() {}
        private MechanicalPatternCatalog catalog = MechanicalPatternCatalog.defaultCatalog();
        private SummaryTreePlanner.TreeConfig treeConfig = SummaryTreePlanner.TreeConfig.defaults();
        private ChromeSummarizerPort summarizer;
        private StructuredPromptPort promptPort = StructuredPromptPort.mechanicalFloor();
        private CheckpointRepository checkpointRepository = CheckpointRepository.noop();
        private ProgressListener progressListener = ProgressListener.noop();

        public Builder catalog(MechanicalPatternCatalog value) { catalog = Objects.requireNonNull(value); return this; }
        public Builder treeConfig(SummaryTreePlanner.TreeConfig value) { treeConfig = Objects.requireNonNull(value); return this; }
        public Builder summarizer(ChromeSummarizerPort value) { summarizer = Objects.requireNonNull(value); return this; }
        public Builder promptPort(StructuredPromptPort value) { promptPort = Objects.requireNonNull(value); return this; }
        public Builder checkpointRepository(CheckpointRepository value) { checkpointRepository = Objects.requireNonNull(value); return this; }
        public Builder progressListener(ProgressListener value) { progressListener = Objects.requireNonNull(value); return this; }

        public JavaChromeAiMechanicalPipeline build() {
            if (summarizer == null) throw new IllegalStateException("summarizer port required");
            return new JavaChromeAiMechanicalPipeline(catalog, treeConfig, summarizer, promptPort,
                    checkpointRepository, progressListener);
        }
    }

    private final MechanicalPatternCatalog catalog;
    private final SummaryTreePlanner.TreeConfig treeConfig;
    private final ChromeSummarizerPort summarizer;
    private final StructuredPromptPort promptPort;
    private final CheckpointRepository checkpoints;
    private final ProgressListener progress;

    private JavaChromeAiMechanicalPipeline(
            MechanicalPatternCatalog catalog, SummaryTreePlanner.TreeConfig treeConfig,
            ChromeSummarizerPort summarizer, StructuredPromptPort promptPort,
            CheckpointRepository checkpoints, ProgressListener progress) {
        this.catalog = catalog;
        this.treeConfig = treeConfig;
        this.summarizer = summarizer;
        this.promptPort = promptPort;
        this.checkpoints = checkpoints;
        this.progress = progress;
    }

    public static Builder builder() { return new Builder(); }

    public MechanicalRun run(String runId, List<SourceRecord> sourceRecords) throws Exception {
        if (sourceRecords == null || sourceRecords.isEmpty()) throw new IllegalArgumentException("sourceRecords required");
        List<SourceRecord> records = sourceRecords.stream()
                .sorted(Comparator.comparing(SourceRecord::sourceRef))
                .toList();
        String sourceText = records.stream().map(SourceRecord::text).collect(Collectors.joining("\n"));

        progress.onStage(PipelineStage.INVENTORIED,
                Hashing.hash("CPULLM_JAVA_INVENTORY_V1", records.stream().map(SourceRecord::sourceRef).toList()));

        SupersetBundle bundle = new MechanicalSupersetConsolidator(catalog).consolidate(records);
        progress.onStage(PipelineStage.CONSOLIDATED, bundle.mechanicalRoot());

        SummaryTreePlan plan = SummaryTreePlanner.plan("urn:cpullm:java:" + runId, sourceText, treeConfig);
        progress.onStage(PipelineStage.TREE_PLANNED, plan.mechanicalRoot());

        SummaryTreeCandidate candidate = new SummaryTreeExecutor(summarizer).execute(plan, sourceText);
        progress.onStage(PipelineStage.MODEL_CANDIDATE, candidate.candidateRoot());

        Set<String> proposed = promptPort.retainedClaimIds(bundle);
        RepairedSelection repaired = OmissionRepair.repair(bundle.claims(), proposed);
        progress.onStage(PipelineStage.REPAIRED,
                Hashing.hash("CPULLM_JAVA_REPAIRED_SELECTION_V1", repaired.finalClaimIds(), repaired.restoredClaimIds()));

        AssemblyReceipt assembly = assemble(bundle, plan, candidate, repaired);
        progress.onStage(PipelineStage.ASSEMBLED, assembly.mechanicalRoot());

        saveCheckpoint(runId, assembly);
        return new MechanicalRun(bundle, plan, candidate, repaired, assembly);
    }

    public static AssemblyReceipt assemble(
            SupersetBundle bundle, SummaryTreePlan plan, SummaryTreeCandidate candidate, RepairedSelection repaired) {
        if (!bundle.candidateOnly() || bundle.mergeAuthorized()) throw new IllegalArgumentException("invalid superset authority");
        if (!candidate.candidateOnly() || candidate.admitted() || candidate.mergeAuthorized()) {
            throw new IllegalArgumentException("invalid model authority");
        }
        if (!candidate.mechanicalRoot().equals(plan.mechanicalRoot())) {
            throw new IllegalArgumentException("tree candidate not bound to plan");
        }
        String mechanicalRoot = Hashing.hash("CPULLM_JAVA_ASSEMBLY_MECHANICAL_V1",
                bundle.mechanicalRoot(), plan.mechanicalRoot(),
                repaired.finalClaimIds().stream().sorted().toList());
        String candidateRoot = Hashing.hash("CPULLM_JAVA_ASSEMBLY_CANDIDATE_V1",
                mechanicalRoot, candidate.candidateRoot(), repaired.restoredClaimIds().stream().sorted().toList());
        return new AssemblyReceipt(mechanicalRoot, candidateRoot, bundle.mechanicalRoot(),
                plan.mechanicalRoot(), candidate.candidateRoot(),
                repaired.finalClaimIds(), repaired.restoredClaimIds(), true, false);
    }

    private void saveCheckpoint(String runId, AssemblyReceipt assembly) throws IOException {
        checkpoints.save(new Checkpoint(runId, PipelineStage.ASSEMBLED,
                assembly.mechanicalRoot(), assembly.candidateRoot()));
    }
}
