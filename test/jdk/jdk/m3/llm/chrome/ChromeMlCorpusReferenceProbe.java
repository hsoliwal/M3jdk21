// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import jdk.m3.llm.chrome.aix.AiXMechanicalPlan;
import jdk.m3.llm.chrome.aix.AiXSourceSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual Java snapshot/plan/packet/advisor path. Index replies are fixtures, never live inference. */
public final class ChromeMlCorpusReferenceProbe {
    private ChromeMlCorpusReferenceProbe() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Explicit source paths required");
        long totalBytes = 0;
        int totalChunks = 0, totalCalls = 0;
        for (String file : args) {
            var source = AiXSourceSnapshot.read(Path.of(file));
            var plan = AiXMechanicalPlan.build(source, new AiXMechanicalPlan.Limits(2048, 2048));
            byte[] restored = new byte[source.size()];
            int expectedEnd = source.size();
            AtomicInteger calls = new AtomicInteger();
            for (int end = plan.chunks().size(); end > 0;) {
                int begin = Math.max(0, end - 3);
                List<ChromeMlContextPacket.Evidence> rows = new ArrayList<>();
                Map<ChromeMlContextPacket.EvidenceKey, AiXMechanicalPlan.Chunk> spans = new HashMap<>();
                for (int i = begin; i < end; i++) {
                    var chunk = plan.chunks().get(i);
                    var key = new ChromeMlContextPacket.EvidenceKey(ChromeMlContextPacket.Source.HOST,
                            source.sha256() + ":" + String.format(Locale.ROOT, "%010d:%010d", chunk.byteStart(), chunk.byteEnd()));
                    rows.add(new ChromeMlContextPacket.Evidence(key, chunk.text())); spans.put(key, chunk);
                }
                var packet = ChromeMlContextPacket.capture(source.name(), begin,
                        Map.of("source", source.sha256()), Set.of("source"), rows);
                StringBuilder reply = new StringBuilder("[");
                for (int i = rows.size() - 1; i >= 0; i--) { if (i != rows.size() - 1) reply.append(','); reply.append(i); }
                reply.append(']');
                ChromeMlRunner fixture = (request, sink) -> {
                    calls.incrementAndGet();
                    if (request.prompt().getBytes(StandardCharsets.UTF_8).length > 8192) throw new AssertionError("prompt budget");
                    return new ChromeMlRunnerResult(reply.toString(), 1, "", "");
                };
                try (var advisor = new ChromeMlContextAdvisor(fixture, ChromeMlContextAdvisor.Limits.standard())) {
                    var selection = advisor.selectEvidence(packet, "Return all evidence indices in reverse order", approved -> {
                        if (!approved.root().equals(packet.root())) throw new SecurityException("stale context");
                    }, 65536);
                    if (selection.executionAuthority() || selection.size() != rows.size()) throw new AssertionError("selection boundary");
                    for (int i = 0; i < selection.size(); i++) {
                        var exact = selection.evidenceAt(i); var chunk = spans.get(exact.key());
                        if (chunk == null || !exact.excerpt().equals(chunk.text()) || chunk.byteEnd() != expectedEnd)
                            throw new AssertionError("source binding/reverse order");
                        byte[] bytes = exact.excerpt().getBytes(StandardCharsets.UTF_8);
                        if (bytes.length != chunk.byteLength()) throw new AssertionError("range length");
                        System.arraycopy(bytes, 0, restored, chunk.byteStart(), bytes.length);
                        expectedEnd = chunk.byteStart();
                    }
                }
                end = begin;
            }
            if (expectedEnd != 0 || !Arrays.equals(restored, source.bytes())) throw new AssertionError("source coverage");
            source.verifyUnchanged();
            totalBytes += source.size(); totalChunks += plan.chunks().size(); totalCalls += calls.get();
            System.out.println("JAVA_REFERENCE_CORPUS\t" + source.name() + "\t" + source.size() + "\t"
                    + plan.chunks().size() + "\t" + calls.get() + "\t" + source.sha256());
        }
        System.out.println("REFERENCE_CORPUS_PASS files=" + args.length + " sourceBytes=" + totalBytes
                + " exactSlices=" + totalChunks + " fixtureCalls=" + totalCalls + " liveModelCalls=0");
        System.out.println("Exact reverse source reconstruction, not proof of semantic relevance or complete historical-claim verification.");
    }
}
