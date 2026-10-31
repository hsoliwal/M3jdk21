// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
/** Snapshot size, not a caller implementation's size claim, owns the 16-record bound. */
public final class ChromeMlEvidenceCountProbe {
    private ChromeMlEvidenceCountProbe() { }
    public static void main(String[] args) {
        List<ChromeMlContextPacket.Evidence> rows = new ArrayList<>();
        for (int i = 0; i < 17; i++) rows.add(new ChromeMlContextPacket.Evidence(
            new ChromeMlContextPacket.EvidenceKey(ChromeMlContextPacket.Source.HOST, "id" + i), "text"));
        List<ChromeMlContextPacket.Evidence> misreported = new AbstractList<>() {
            @Override public int size() { return 1; }
            @Override public ChromeMlContextPacket.Evidence get(int i) { return rows.get(i); }
            @Override public Object[] toArray() { return rows.toArray(); }
        };
        try {
            ChromeMlContextPacket.capture("id", 0, Map.of(), Set.of(), misreported);
        } catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains("Projection count bound")) throw expected;
            System.out.println("CAPTURED_EVIDENCE_COUNT_BOUND_PASS"); return;
        }
        throw new AssertionError("captured evidence exceeds 16 despite misleading caller size");
    }
}
