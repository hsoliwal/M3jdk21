// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Reject incomplete native outcomes before admitting text to the candidate tree.
package com.m3.llm.chrome.aix;

import com.m3.llm.chrome.ChromeMlRunner;
import com.m3.llm.chrome.ChromeMlRunnerRequest;
import com.m3.llm.chrome.ChromeMlRunnerResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Sequential candidate-only leaf summaries and deterministic summary-of-summaries topology. */
public final class AiXChromeMlMultipass {
    public record Config(int fanout, int maxOutputTokens, int maxCandidateChars) {
        public Config {
            if (fanout < 2 || fanout > 8 || maxOutputTokens < 16 || maxOutputTokens > 2048
                    || maxCandidateChars < 128 || maxCandidateChars > 2048) {
                throw new IllegalArgumentException("multipass config out of range");
            }
        }
        public static Config defaults() { return new Config(6, 256, 2048); }
    }

    public record Candidate(
            String id,
            String kind,
            int level,
            int ordinal,
            int byteStart,
            int byteEnd,
            String inputRoot,
            String outputSha256,
            String text,
            List<String> childIds,
            boolean candidateOnly) {
        public Candidate { childIds = List.copyOf(childIds); }
    }

    public record Result(
            String schema,
            String sourceSha256,
            List<Candidate> candidates,
            String rootCandidateId,
            int modelCalls,
            String candidateRoot,
            boolean promotionAuthorized) {
        public Result { candidates = List.copyOf(candidates); }
    }

    private AiXChromeMlMultipass() {}

    public static Result run(AiXMechanicalPlan.Plan plan, ChromeMlRunner runner) throws Exception {
        return run(plan, runner, Config.defaults());
    }

    public static Result run(AiXMechanicalPlan.Plan plan, ChromeMlRunner runner, Config config) throws Exception {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(runner, "runner");
        Objects.requireNonNull(config, "config");
        List<Candidate> all = new ArrayList<>();
        List<Candidate> level = new ArrayList<>();
        int calls = 0;
        for (AiXMechanicalPlan.Chunk chunk : plan.chunks()) {
            String prompt = leafPrompt(plan, chunk);
            Candidate candidate = invoke(runner, config, "LEAF", 0, chunk.ordinal(), chunk.byteStart(), chunk.byteEnd(),
                    chunk.sha256(), List.of(), prompt);
            all.add(candidate);
            level.add(candidate);
            calls++;
        }
        int depth = 1;
        while (level.size() > 1) {
            List<Candidate> next = new ArrayList<>();
            for (int offset = 0; offset < level.size(); offset += config.fanout()) {
                List<Candidate> children = List.copyOf(level.subList(offset, Math.min(level.size(), offset + config.fanout())));
                int start = children.getFirst().byteStart();
                int end = children.getLast().byteEnd();
                String inputRoot = AiXMechanicalPlan.hashCanonical(children.stream().map(c ->
                        List.of(c.id(), c.outputSha256(), c.byteStart(), c.byteEnd())).toList());
                String prompt = reductionPrompt(plan, children, depth, inputRoot);
                Candidate parent = invoke(runner, config, "REDUCTION", depth, next.size(), start, end, inputRoot,
                        children.stream().map(Candidate::id).toList(), prompt);
                all.add(parent);
                next.add(parent);
                calls++;
            }
            level = next;
            depth++;
            if (depth > 64) { throw new IllegalStateException("SUMMARY_TREE_DEPTH_LIMIT"); }
        }
        Candidate root = level.getFirst();
        String candidateRoot = AiXMechanicalPlan.hashCanonical(all.stream().map(c -> List.of(
                c.id(), c.kind(), c.level(), c.ordinal(), c.byteStart(), c.byteEnd(), c.inputRoot(),
                c.outputSha256(), c.childIds())).toList());
        return new Result("aix.java.chromeml-multipass/1", plan.sourceSha256(), all, root.id(), calls,
                candidateRoot, false);
    }

    private static Candidate invoke(
            ChromeMlRunner runner,
            Config config,
            String kind,
            int level,
            int ordinal,
            int start,
            int end,
            String inputRoot,
            List<String> childIds,
            String prompt) throws Exception {
        if (prompt.length() > 16_384) { throw new IllegalArgumentException("SUMMARY_PROMPT_LIMIT"); }
        ChromeMlRunnerRequest request = new ChromeMlRunnerRequest(
                prompt, List.of(), null, 16_000, config.maxOutputTokens(), null, null, null,
                false, false, false);
        ChromeMlRunnerResult result = runner.generate(request);
        // Same completion contract as ChromeMlContextAdvisor: only kComplete=1.
        // Do not turn error/in-progress prose into a successful reduction input.
        if (result == null || !Integer.valueOf(1).equals(result.completeStatus())) {
            throw new IllegalStateException("MODEL_CANDIDATE_NATIVE_INCOMPLETE");
        }
        String output = Objects.requireNonNull(result.text(), "model text").strip();
        if (output.isEmpty()) { throw new IllegalStateException("EMPTY_MODEL_CANDIDATE"); }
        if (output.length() > config.maxCandidateChars()) {
            throw new IllegalStateException("MODEL_CANDIDATE_CHAR_LIMIT");
        }
        String outputSha = AiXSourceSnapshot.sha(output.getBytes(StandardCharsets.UTF_8));
        String id = AiXMechanicalPlan.hashCanonical(List.of(
                "aix.java.summary-candidate/1", kind, level, ordinal, start, end, inputRoot, outputSha, childIds));
        return new Candidate(id, kind, level, ordinal, start, end, inputRoot, outputSha, output, childIds, true);
    }

    private static String leafPrompt(AiXMechanicalPlan.Plan plan, AiXMechanicalPlan.Chunk chunk) {
        return "AIX_LEAF_SUMMARY_V1\n"
                + "Source SHA-256: " + plan.sourceSha256() + "\n"
                + "Exact byte range: [" + chunk.byteStart() + "," + chunk.byteEnd() + ")\n"
                + "Chunk SHA-256: " + chunk.sha256() + "\n"
                + "Mechanical entropy root: " + chunk.entropy().root() + "\n"
                + "Task: compress only facts present in this bounded source range. Preserve names, invariants, conflicts and TODOs. Do not infer missing facts. Output concise plain text.\n"
                + "---SOURCE---\n" + chunk.text();
    }

    private static String reductionPrompt(
            AiXMechanicalPlan.Plan plan, List<Candidate> children, int level, String inputRoot) {
        StringBuilder prompt = new StringBuilder("AIX_SUMMARY_REDUCTION_V1\n")
                .append("Source SHA-256: ").append(plan.sourceSha256()).append('\n')
                .append("Tree level: ").append(level).append('\n')
                .append("Child-set root: ").append(inputRoot).append('\n')
                .append("Task: consolidate the supplied candidate summaries without inventing facts or deleting conflicts. Output concise plain text.\n");
        for (Candidate child : children) {
            prompt.append("---CHILD ").append(child.id()).append(" RANGE[")
                    .append(child.byteStart()).append(',').append(child.byteEnd()).append(")---\n")
                    .append(child.text()).append('\n');
        }
        return prompt.toString();
    }
}
