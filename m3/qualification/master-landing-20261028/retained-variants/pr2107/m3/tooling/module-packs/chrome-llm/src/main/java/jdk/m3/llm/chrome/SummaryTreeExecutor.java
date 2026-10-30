/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/com/synexia/cpullm/chrome/SummaryTreeExecutor.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ProgressMonitor;

/** One-session serial executor for leaf -> reduce-node Chrome summary candidates. */
public final class SummaryTreeExecutor {
    private final ChromeSummarizerPort port;

    public SummaryTreeExecutor(ChromeSummarizerPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public SummaryTreeCandidate execute(SummaryTreePlan plan, String sourceText) throws Exception {
        return execute(plan, sourceText, ProgressMonitor.NONE);
    }

    /** The supplied monitor is borrowed; only validated node results receive work credit. */
    @SuppressWarnings("try") // The inherited adapter close contract may propagate interruption.
    public SummaryTreeCandidate execute(SummaryTreePlan plan, String sourceText, ProgressMonitor monitor) throws Exception {
        Objects.requireNonNull(monitor, "monitor");
        monitor.checkCancelled();
        if (!plan.verify(sourceText)) throw new IllegalArgumentException("invalid summary tree plan/source binding");
        byte[] source = sourceText.getBytes(StandardCharsets.UTF_8);
        Map<String, SummaryNodeResult> results = new LinkedHashMap<>();

        monitor.checkCancelled();
        try (ChromeSummarizerPort.Session session = port.open()) {
            for (SummaryNode node : plan.nodes()) {
                monitor.checkCancelled();
                String input;
                List<String> sourceRefs;
                if (node.kind().equals("LEAF")) {
                    byte[] raw = Arrays.copyOfRange(source, node.startByte(), node.endByte());
                    input = new String(raw, StandardCharsets.UTF_8);
                    sourceRefs = List.of(plan.sourceRef() + "#leaf=" + node.ordinal());
                } else {
                    StringBuilder parent = new StringBuilder();
                    List<String> refs = new ArrayList<>();
                    for (String childId : node.children()) {
                        SummaryNodeResult child = results.get(childId);
                        if (child == null) throw new IllegalStateException("missing child result " + childId);
                        parent.append("child=").append(child.nodeId()).append("\n")
                                .append(child.summary()).append("\n");
                        refs.addAll(child.sourceRefs());
                    }
                    input = parent.toString();
                    sourceRefs = refs.stream().distinct().sorted().toList();
                    if (input.getBytes(StandardCharsets.UTF_8).length > plan.config().maxParentInputBytes()) {
                        throw new IllegalStateException("parent input budget exceeded");
                    }
                }

                ChromeRequest request = ChromeProtocol.request(node.nodeId(), input, sourceRefs);
                monitor.checkCancelled();
                ChromeResult result = session.summarize(request);
                monitor.checkCancelled();
                ChromeProtocol.verify(request, result);
                if (!result.status().equals("CANDIDATE_ONLY")) {
                    throw new IllegalStateException("model abstained at node " + node.nodeId());
                }
                if (result.text().getBytes(StandardCharsets.UTF_8).length > plan.config().maxNodeOutputBytes()) {
                    throw new IllegalStateException("node output budget exceeded");
                }
                SummaryNodeResult nodeResult = new SummaryNodeResult(
                        node.nodeId(), result.resultId(),
                        Hashing.sha256Hex(result.text().getBytes(StandardCharsets.UTF_8)),
                        result.text(), result.sourceRefs());
                results.put(node.nodeId(), nodeResult);
                monitor.worked(1);
            }
        }

        SummaryNodeResult root = results.get(plan.rootNodeId());
        List<SummaryNodeResult> ordered = plan.nodes().stream().map(n -> results.get(n.nodeId())).toList();
        String candidateRoot = Hashing.hash("CPULLM_JAVA_SUMMARY_TREE_CANDIDATE_V1",
                plan.sourceSha256(), plan.mechanicalRoot(), ordered.stream().map(SummaryNodeResult::resultId).toList());
        return new SummaryTreeCandidate(
                "CANDIDATE_ONLY", plan.sourceSha256(), plan.mechanicalRoot(), ordered,
                root.resultId(), root.summary(), candidateRoot, true, false, false);
    }

}
