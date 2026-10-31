// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MixedModeSummaryConsolidatorTest {

    @Test
    void cpuSummaryRemainsCanonicalWhenNoModelProviderExists() {
        var result =
                MixedModeSummaryConsolidator.summarize(
                        List.of(
                                new MixedModeSummaryConsolidator.Evidence(
                                        "dawn-readme",
                                        "Dawn implements WebGPU. Tint compiles WGSL shaders."),
                                new MixedModeSummaryConsolidator.Evidence(
                                        "dawn-support",
                                        "Dawn has native backends for multiple GPU APIs.")),
                        3,
                        4096,
                        null);

        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN,
                result.candidate().status());
        assertEquals("NO_CANDIDATE_PROVIDER", result.candidate().reason());
        assertTrue(result.serialPromotionRequired());
        assertFalse(result.modelAuthority());
        assertFalse(result.promotionAuthority());
        assertFalse(result.candidate().canonWritable());
        assertTrue(result.rendered().contains("Deterministic source-anchored summary"));
        assertTrue(result.rendered().contains("Model candidate - non-authoritative"));
        assertTrue(result.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void extractiveSelectionPreservesWholeSourceOmissions() {
        var high =
                new MixedModeSummaryConsolidator.Evidence(
                        "high-salience",
                        "alpha alpha alpha alpha. tiny.");
        var low =
                new MixedModeSummaryConsolidator.Evidence(
                        "low-salience",
                        "omega.");
        AtomicReference<MixedModeSummaryConsolidator.CandidateRequest> observed =
                new AtomicReference<>();

        var result =
                MixedModeSummaryConsolidator.summarize(
                        List.of(high, low),
                        1,
                        1024,
                        request -> {
                            observed.set(request);
                            return MixedModeSummaryConsolidator.CandidateAttempt.abstain(
                                    "fixture-provider",
                                    "NO_MODEL",
                                    "");
                        });

        var request = observed.get();
        assertEquals(List.of("high-salience"), request.sourceIds());
        assertEquals(List.of("low-salience"), request.omittedSourceIds());
        assertEquals(List.of(high.contentSha256()), request.evidenceRoots());
        assertEquals(List.of(low.contentSha256()), request.omittedEvidenceRoots());
        assertTrue(result.rendered().contains(high.contentSha256()));
        assertTrue(result.rendered().contains(low.contentSha256()));
        assertTrue(result.rendered().contains("omitted-evidence-roots"));
    }

    @Test
    void modelCandidateIsBoundToExactCpuRequestButNeverAdmitted() {
        var first =
                MixedModeSummaryConsolidator.summarize(
                        List.of(
                                new MixedModeSummaryConsolidator.Evidence(
                                        "source-a",
                                        "WebGPU exposes a portable GPU API. WGSL is its shader language.")),
                        2,
                        2048,
                        request ->
                                MixedModeSummaryConsolidator.CandidateAttempt.candidate(
                                        "fixture-provider",
                                        "Candidate synthesis.",
                                        "a".repeat(64)));
        var second =
                MixedModeSummaryConsolidator.summarize(
                        List.of(
                                new MixedModeSummaryConsolidator.Evidence(
                                        "source-a",
                                        "WebGPU exposes a portable GPU API. WGSL is its shader language.")),
                        2,
                        2048,
                        request ->
                                MixedModeSummaryConsolidator.CandidateAttempt.candidate(
                                        "fixture-provider",
                                        "Candidate synthesis.",
                                        "a".repeat(64)));

        assertEquals(first.sourceRoot(), second.sourceRoot());
        assertEquals(first.request().root(), second.request().root());
        assertEquals(first.root(), second.root());
        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.CANDIDATE_ONLY,
                first.candidate().status());
        assertTrue(first.candidate().candidateOnly());
        assertFalse(first.candidate().admitted());
        assertFalse(first.candidate().canonWritable());
    }
}
