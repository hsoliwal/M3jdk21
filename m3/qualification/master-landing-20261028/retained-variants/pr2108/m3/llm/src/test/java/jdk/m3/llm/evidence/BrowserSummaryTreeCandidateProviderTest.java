// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.llm.internal.CanonicalJson;
import com.m3.llm.internal.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class BrowserSummaryTreeCandidateProviderTest {
    @Test
    void verifiedBrowserTreeBecomesSameCandidateOnlyProviderContract() {
        var request = request();
        String receipt = receipt(request, false, false);

        var attempt = new BrowserSummaryTreeCandidateProvider(receipt).propose(request);

        assertEquals(MixedModeSummaryConsolidator.CandidateStatus.CANDIDATE_ONLY, attempt.status());
        assertEquals("CHROME_SUMMARIZER_TREE", attempt.provider());
        assertEquals("Browser tree candidate.", attempt.text());
        assertTrue(attempt.providerReceiptRoot().matches("[0-9a-f]{64}"));
        assertFalse(attempt.admitted());
        assertFalse(attempt.canonWritable());
    }

    @Test
    void verifiedBrowserReceiptRunsThroughCanonicalMixedConsolidator() {
        var provider =
                new BrowserSummaryTreeCandidateProvider(
                        request -> receipt(request, false, false));

        var result =
                MixedModeSummaryConsolidator.summarize(
                        List.of(
                                new MixedModeSummaryConsolidator.Evidence(
                                        "dawn-browser-fixture",
                                        "Dawn exposes WebGPU. Tint compiles WGSL.")),
                        2,
                        4096,
                        provider);

        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.CANDIDATE_ONLY,
                result.candidate().status());
        assertEquals("CHROME_SUMMARIZER_TREE", result.candidate().provider());
        assertEquals("Browser tree candidate.", result.candidate().text());
        assertTrue(result.serialPromotionRequired());
        assertFalse(result.modelAuthority());
        assertFalse(result.promotionAuthority());
        assertFalse(result.candidate().canonWritable());
    }

    @Test
    void sourceDriftOrAuthorityWideningFailsClosedAsAbstain() {
        var request = request();

        var drift =
                new BrowserSummaryTreeCandidateProvider(receipt(request, true, false))
                        .propose(request);
        var widened =
                new BrowserSummaryTreeCandidateProvider(receipt(request, false, true))
                        .propose(request);

        assertEquals(MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN, drift.status());
        assertEquals(MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN, widened.status());
        assertEquals("INVALID_BROWSER_SUMMARY_RECEIPT", drift.reason());
        assertEquals("INVALID_BROWSER_SUMMARY_RECEIPT", widened.reason());
    }

    @Test
    void unknownFieldsAndFractionalIntegersFailClosed() {
        var request = request();
        @SuppressWarnings("unchecked")
        Map<String, Object> unknown =
                new TreeMap<>((Map<String, Object>) CanonicalJson.parse(receipt(request, false, false)));
        unknown.put("execution_override", true);

        @SuppressWarnings("unchecked")
        Map<String, Object> fractional =
                new TreeMap<>((Map<String, Object>) CanonicalJson.parse(receipt(request, false, false)));
        fractional.put("revision", 1.5);

        var unknownAttempt =
                new BrowserSummaryTreeCandidateProvider(CanonicalJson.string(unknown))
                        .propose(request);
        var fractionalAttempt =
                new BrowserSummaryTreeCandidateProvider(CanonicalJson.string(fractional))
                        .propose(request);

        assertEquals(MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN, unknownAttempt.status());
        assertEquals(MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN, fractionalAttempt.status());
        assertEquals("INVALID_BROWSER_SUMMARY_RECEIPT", unknownAttempt.reason());
        assertEquals("INVALID_BROWSER_SUMMARY_RECEIPT", fractionalAttempt.reason());
    }

    @Test
    void runtimeFieldsAddedAfterSealingDoNotChangeCandidateRoot() {
        var request = request();
        String receipt = receipt(request, false, false);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed =
                new TreeMap<>((Map<String, Object>) CanonicalJson.parse(receipt));
        parsed.put("session_creations", 1L);
        parsed.put("availability", "available");
        parsed.put("download_progress_million", List.of(1000000L));

        var attempt =
                new BrowserSummaryTreeCandidateProvider(CanonicalJson.string(parsed))
                        .propose(request);

        assertEquals(MixedModeSummaryConsolidator.CandidateStatus.CANDIDATE_ONLY, attempt.status());
    }

    private static MixedModeSummaryConsolidator.CandidateRequest request() {
        return new MixedModeSummaryConsolidator.CandidateRequest(
                "a".repeat(64),
                "Deterministic source-anchored projection.",
                List.of("source-a"),
                List.of("b".repeat(64)),
                4096,
                "");
    }

    private static String receipt(
            MixedModeSummaryConsolidator.CandidateRequest request,
            boolean sourceDrift,
            boolean authorityWidening) {
        String sourceSha =
                sourceDrift
                        ? "c".repeat(64)
                        : sha(request.deterministicSummary());
        String rootNodeId = "d".repeat(64);
        String summary = "Browser tree candidate.";
        String outputSha = sha(summary);

        TreeMap<String, Object> identity = new TreeMap<>();
        identity.put("node_id", rootNodeId);
        identity.put("kind", "LEAF");
        identity.put("level", 0L);
        identity.put("ordinal", 0L);
        identity.put("children", List.of());
        identity.put("coverage_leaf_ordinals", List.of(0L));
        identity.put("input_sha256", sourceSha);
        identity.put("output_sha256", outputSha);
        identity.put("child_result_ids", List.of());
        String resultId = digest(identity);

        TreeMap<String, Object> node = new TreeMap<>(identity);
        node.put("result_id", resultId);
        node.put("summary", summary);
        node.put("provider", "Summarizer");
        node.put("candidate_only", true);
        node.put("admitted", false);
        node.put("execution_authorized", false);
        node.put("merge_authorized", false);

        String mechanicalRoot = "e".repeat(64);
        TreeMap<String, Object> semantic = new TreeMap<>();
        semantic.put("schema", "cpullm.chrome-summary-tree-semantic.v1");
        semantic.put("source_sha256", sourceSha);
        semantic.put("mechanical_root", mechanicalRoot);
        semantic.put("root_node_id", rootNodeId);
        semantic.put("root_result_id", resultId);
        semantic.put("node_result_ids", List.of(resultId));

        TreeMap<String, Object> body = new TreeMap<>();
        body.put("schema", "cpullm.chrome-summary-tree-candidate.v1");
        body.put("revision", 1L);
        body.put("source_ref", BrowserSummaryTreeCandidateProvider.sourceRef(request));
        body.put("source_sha256", sourceSha);
        body.put("mechanical_root", mechanicalRoot);
        body.put("root_node_id", rootNodeId);
        body.put("root_result_id", resultId);
        body.put("root_summary", summary);
        body.put("nodes", List.of(node));
        body.put("model_calls_observed", 1L);
        body.put("resumed_nodes", 0L);
        body.put("exact_duplicate_leaf_reuses", 0L);
        body.put("node_count_completed", 1L);
        body.put("session_strategy", "ONE_SESSION_SERIAL_TREE");
        body.put("provider_fallback", false);
        body.put("candidate_only", true);
        body.put("admitted", false);
        body.put("execution_authorized", authorityWidening);
        body.put("merge_authorized", false);
        body.put("deletion_authorized", false);
        body.put("locality_widening_authorized", false);
        body.put(
                "structured_entropy",
                Map.ofEntries(
                        Map.entry(
                                "source_utf8_bytes",
                                (long)
                                        request.deterministicSummary()
                                                .getBytes(StandardCharsets.UTF_8)
                                                .length),
                        Map.entry("leaf_nodes", 1L),
                        Map.entry("exact_duplicate_leaf_occurrences", 0L),
                        Map.entry("tree_nodes", 1L),
                        Map.entry("tree_depth", 0L),
                        Map.entry("provider_calls", 1L),
                        Map.entry("resumed_nodes", 0L),
                        Map.entry("exact_duplicate_leaf_reuses", 0L),
                        Map.entry("coverage_ppm", 1000000L),
                        Map.entry("root_compression_ppm", 1000L),
                        Map.entry("repeated_output_ppm", 0L)));
        body.put("semantic_candidate_root", digest(semantic));
        body.put(
                "note",
                "Hierarchical compression candidate only; exact source/tree and mechanical multipass remain authoritative.");
        body.put("candidate_root", digest(body));
        return CanonicalJson.string(body);
    }

    private static String digest(Object value) {
        return Sha256.digest(CanonicalJson.string(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha(String value) {
        return Sha256.digest(value.getBytes(StandardCharsets.UTF_8));
    }
}
