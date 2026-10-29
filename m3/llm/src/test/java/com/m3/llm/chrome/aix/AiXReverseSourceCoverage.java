// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome.aix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/** Whole-source byte coverage in reverse chunk traversal. Not semantic reading or model proof. */
public final class AiXReverseSourceCoverage {
    private AiXReverseSourceCoverage() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Explicit source paths required");
        long bytes = 0;
        int chunks = 0;
        for (String name : args) {
            AiXSourceSnapshot snapshot = AiXSourceSnapshot.read(Path.of(name));
            AiXMechanicalPlan.Plan plan = AiXMechanicalPlan.build(snapshot);
            byte[] restored = new byte[snapshot.size()];
            int expectedEnd = snapshot.size();
            for (AiXMechanicalPlan.Chunk chunk : plan.chunks().reversed()) {
                if (chunk.byteEnd() != expectedEnd) throw new AssertionError("reverse gap");
                byte[] exact = chunk.text().getBytes(StandardCharsets.UTF_8);
                if (exact.length != chunk.byteLength()) throw new AssertionError("range length");
                System.arraycopy(exact, 0, restored, chunk.byteStart(), exact.length);
                expectedEnd = chunk.byteStart();
            }
            if (expectedEnd != 0 || !Arrays.equals(restored, snapshot.bytes())) throw new AssertionError("source loss");
            snapshot.verifyUnchanged();
            bytes += snapshot.size(); chunks += plan.chunks().size();
            System.out.println("COVERED\t" + snapshot.name() + "\t" + snapshot.size() + "\t"
                    + plan.chunks().size() + "\t" + snapshot.sha256() + "\t" + plan.planRoot());
        }
        System.out.println("REVERSE_BYTE_COVERAGE files=" + args.length + " chunks=" + chunks + " bytes=" + bytes
                + " modelCalls=0; coverage is not semantic verification");
    }
}
