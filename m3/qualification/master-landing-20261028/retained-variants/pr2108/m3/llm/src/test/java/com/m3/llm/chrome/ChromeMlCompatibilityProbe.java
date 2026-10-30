// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
/** Baseline/candidate executable observation, including legacy prompt and hash identity. */
public final class ChromeMlCompatibilityProbe {
    private ChromeMlCompatibilityProbe() { }
    public static void main(String[] args) throws Exception {
        for (int n = 0; n < 256; n++) {
            var rows = new ArrayList<ChromeMlContextPacket.Evidence>();
            for (int i = 0; i < n % 17; i++) rows.add(new ChromeMlContextPacket.Evidence(
                new ChromeMlContextPacket.EvidenceKey(ChromeMlContextPacket.Source.values()[i % 7], "id:" + i),
                "[" + i + "] MUST NOT " + "😀界\r\n0:;".repeat(n % 8)));
            var packet = ChromeMlContextPacket.capture("id:" + n, n,
                Map.of("present", "é" + n, "unused", new Object()), Set.of("present", "absent"), rows);
            System.out.println("PACKET " + n + " " + packet.root() + " " + ChromeMlContextPacket.hash(packet.encoded()));
            var advisor = new ChromeMlContextAdvisor((r,s) -> {
                System.out.println("PROMPT " + ChromeMlContextPacket.hash(r.prompt()));
                return new ChromeMlRunnerResult("reply", 1, "", "");
            }, ChromeMlContextAdvisor.Limits.standard());
            try {
                var output = advisor.advise(packet, "review " + n, p -> { });
                System.out.println("ADVICE " + output.requestRoot() + " " + output.responseRoot());
            } catch (IllegalArgumentException expectedBound) {
                System.out.println("BOUND " + expectedBound.getMessage());
            }
        }
    }
}
