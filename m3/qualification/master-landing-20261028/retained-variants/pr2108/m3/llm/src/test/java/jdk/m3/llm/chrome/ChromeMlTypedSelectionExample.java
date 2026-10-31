// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compiled, typed caller example. The supplied runner is a fixture, not a model. */
public final class ChromeMlTypedSelectionExample {
    private ChromeMlTypedSelectionExample() { }
    public static void main(String[] args) throws Exception {
        var original = new ChromeMlContextPacket.Evidence(new ChromeMlContextPacket.EvidenceKey(
                ChromeMlContextPacket.Source.HOST, "fixture.java#L1"), "return a + b;  \n");
        var packet = ChromeMlContextPacket.capture("task-context", 1, Map.of(), Set.of(), List.of(original));
        ChromeMlRunner fixture = (request, sink) -> new ChromeMlRunnerResult("[0]", 1, "", "");
        try (var advisor = new ChromeMlContextAdvisor(fixture, ChromeMlContextAdvisor.Limits.standard())) {
            ChromeMlContextAdvisor.EvidenceSelection selection = advisor.selectEvidence(packet,
                    "Select source evidence", source -> {
                        if (!source.root().equals(packet.root())) throw new SecurityException("stale");
                    }, 65536);
            if (!selection.evidenceAt(0).equals(original) || selection.executionAuthority()) throw new AssertionError();
            System.out.println("TYPED_JAVA_SELECTION_OK");
        }
    }
}
