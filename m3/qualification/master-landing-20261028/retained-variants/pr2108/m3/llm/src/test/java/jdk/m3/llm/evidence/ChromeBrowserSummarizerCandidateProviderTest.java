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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChromeBrowserSummarizerCandidateProviderTest {

    @Test
    void resultIdentityMatchesBrowserCrossLanguageVector() {
        TreeMap<String, Object> identity =
                identity(
                        "CANDIDATE_ONLY",
                        "a".repeat(64),
                        List.of("b".repeat(64)),
                        "Browser candidate synthesis.",
                        "available",
                        "",
                        1);

        assertEquals(
                "6f024cc964d3ba31b6b0a339dc21770c672c8c9e8b57896d858e610a106485ef",
                ChromeBrowserSummarizerCandidateProvider.resultId(identity));
    }

    @Test
    void browserEnvelopeMapsToTheSameCandidateContract() {
        AtomicReference<Map<String, Object>> observed = new AtomicReference<>();
        ChromeBrowserSummarizerCandidateProvider.BrowserTransport transport =
                new ChromeBrowserSummarizerCandidateProvider.BrowserTransport() {
                    @Override
                    public String identity() {
                        return hash("browser-fixture");
                    }

                    @Override
                    public byte[] exchange(byte[] canonicalRequest) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> request =
                                (Map<String, Object>)
                                        CanonicalJson.parse(
                                                new String(
                                                        canonicalRequest,
                                                        StandardCharsets.UTF_8));
                        observed.set(request);
                        return candidateResult(
                                String.valueOf(request.get("candidate_id")),
                                castStrings(request.get("source_refs")));
                    }
                };

        var provider = new ChromeBrowserSummarizerCandidateProvider(transport);
        var request = request();

        var attempt = provider.propose(request);

        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.CANDIDATE_ONLY,
                attempt.status());
        assertEquals("CHROME_BROWSER_SUMMARIZER:available", attempt.provider());
        assertEquals("Browser candidate synthesis.", attempt.text());
        assertTrue(attempt.providerReceiptRoot().matches("[0-9a-f]{64}"));
        assertFalse(attempt.admitted());
        assertFalse(attempt.canonWritable());

        Map<String, Object> packet = observed.get();
        assertEquals(
                ChromeBrowserSummarizerCandidateProvider.REQUEST_SCHEMA,
                packet.get("schema"));
        assertEquals("Summarizer", packet.get("api"));
        assertEquals(request.root(), packet.get("candidate_id"));
        assertEquals(request.sourceIds(), packet.get("source_refs"));
        assertEquals(request.omittedSourceIds(), packet.get("omitted_refs"));
        assertFalse(packet.get("source_refs").equals(request.evidenceRoots()));
        assertEquals("CANDIDATE_ONLY", packet.get("authority"));
        assertEquals(0L, packet.get("model_calls"));
        assertEquals(0L, packet.get("network_calls"));
    }

    @Test
    void requestPacketCarriesSelectedAndOmittedEvidenceSeparately() {
        var provider =
                new ChromeBrowserSummarizerCandidateProvider(
                        new ChromeBrowserSummarizerCandidateProvider.BrowserTransport() {
                            @Override
                            public String identity() {
                                return hash("packet-fixture");
                            }

                            @Override
                            public byte[] exchange(byte[] canonicalRequest) {
                                throw new AssertionError("exchange should not run");
                            }
                        });
        var request =
                new MixedModeSummaryConsolidator.CandidateRequest(
                        "a".repeat(64),
                        "Selected deterministic summary.",
                        List.of("source-a"),
                        List.of("source-b"),
                        List.of("b".repeat(64)),
                        List.of("c".repeat(64)),
                        2048,
                        "");

        Map<String, Object> packet = provider.requestPacket(request);

        assertEquals(List.of("source-a"), packet.get("source_refs"));
        assertEquals(List.of("source-b"), packet.get("omitted_refs"));
        assertEquals("CANDIDATE_ONLY", packet.get("status"));
    }

    @Test
    void requestWithNoSelectedEvidenceAbstainsBeforeBrowserModelUse() {
        var provider =
                new ChromeBrowserSummarizerCandidateProvider(
                        new ChromeBrowserSummarizerCandidateProvider.BrowserTransport() {
                            @Override
                            public String identity() {
                                return hash("cpu-abstain-fixture");
                            }

                            @Override
                            public byte[] exchange(byte[] canonicalRequest) {
                                throw new AssertionError("exchange should not run");
                            }
                        });
        var request =
                new MixedModeSummaryConsolidator.CandidateRequest(
                        "a".repeat(64),
                        "Marker-only deterministic projection.",
                        List.of(),
                        List.of("source-a"),
                        List.of(),
                        List.of("c".repeat(64)),
                        2048,
                        "");

        Map<String, Object> packet = provider.requestPacket(request);

        assertEquals("ABSTAIN", packet.get("status"));
        assertEquals(List.of(), packet.get("source_refs"));
        assertEquals(List.of("source-a"), packet.get("omitted_refs"));
    }

    @Test
    void widenedSourceReferencesFailClosedToAbstain() {
        ChromeBrowserSummarizerCandidateProvider.BrowserTransport transport =
                new ChromeBrowserSummarizerCandidateProvider.BrowserTransport() {
                    @Override
                    public String identity() {
                        return hash("widening-fixture");
                    }

                    @Override
                    public byte[] exchange(byte[] canonicalRequest) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> request =
                                (Map<String, Object>)
                                        CanonicalJson.parse(
                                                new String(
                                                        canonicalRequest,
                                                        StandardCharsets.UTF_8));
                        List<String> widened =
                                List.of(
                                        castStrings(request.get("source_refs")).getFirst(),
                                        "f".repeat(64));
                        return candidateResult(
                                String.valueOf(request.get("candidate_id")),
                                widened);
                    }
                };

        var attempt =
                new ChromeBrowserSummarizerCandidateProvider(transport)
                        .propose(request());

        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN,
                attempt.status());
        assertEquals("BROWSER_RESULT_INVALID", attempt.reason());
        assertEquals(hash("widening-fixture"), attempt.providerReceiptRoot());
        assertFalse(attempt.admitted());
    }

    @Test
    void browserAbstainReceiptRemainsContentAddressedAndNonAuthoritative() {
        ChromeBrowserSummarizerCandidateProvider.BrowserTransport transport =
                new ChromeBrowserSummarizerCandidateProvider.BrowserTransport() {
                    @Override
                    public String identity() {
                        return hash("abstain-fixture");
                    }

                    @Override
                    public byte[] exchange(byte[] canonicalRequest) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> request =
                                (Map<String, Object>)
                                        CanonicalJson.parse(
                                                new String(
                                                        canonicalRequest,
                                                        StandardCharsets.UTF_8));
                        return abstainResult(
                                String.valueOf(request.get("candidate_id")),
                                castStrings(request.get("source_refs")),
                                "MODEL_UNAVAILABLE");
                    }
                };

        var attempt =
                new ChromeBrowserSummarizerCandidateProvider(transport)
                        .propose(request());

        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN,
                attempt.status());
        assertEquals("MODEL_UNAVAILABLE", attempt.reason());
        assertEquals("CHROME_BROWSER_SUMMARIZER:unavailable", attempt.provider());
        assertTrue(attempt.providerReceiptRoot().matches("[0-9a-f]{64}"));
        assertFalse(attempt.admitted());
    }

    @Test
    void transportFailureNeverEscalatesIntoModelAuthority() {
        ChromeBrowserSummarizerCandidateProvider.BrowserTransport transport =
                new ChromeBrowserSummarizerCandidateProvider.BrowserTransport() {
                    @Override
                    public String identity() {
                        return hash("transport-fixture");
                    }

                    @Override
                    public byte[] exchange(byte[] canonicalRequest) {
                        throw new IllegalStateException("fixture");
                    }
                };

        var attempt =
                new ChromeBrowserSummarizerCandidateProvider(transport)
                        .propose(request());

        assertEquals(
                MixedModeSummaryConsolidator.CandidateStatus.ABSTAIN,
                attempt.status());
        assertEquals("BROWSER_TRANSPORT_FAILED", attempt.reason());
        assertFalse(attempt.admitted());
        assertFalse(attempt.canonWritable());
    }

    private static MixedModeSummaryConsolidator.CandidateRequest request() {
        return new MixedModeSummaryConsolidator.CandidateRequest(
                "a".repeat(64),
                "Dawn implements WebGPU and Tint compiles WGSL.",
                List.of("dawn-source"),
                List.of("b".repeat(64)),
                4096,
                "");
    }

    private static byte[] candidateResult(String candidateId, List<String> sourceRefs) {
        TreeMap<String, Object> identity = identity(
                "CANDIDATE_ONLY",
                candidateId,
                sourceRefs,
                "Browser candidate synthesis.",
                "available",
                "",
                1);
        return result(identity, List.of(250_000, 1_000_000));
    }

    private static byte[] abstainResult(
            String candidateId, List<String> sourceRefs, String reason) {
        TreeMap<String, Object> identity = identity(
                "ABSTAIN",
                candidateId,
                sourceRefs,
                "",
                "unavailable",
                reason,
                0);
        return result(identity, List.of());
    }

    private static TreeMap<String, Object> identity(
            String status,
            String candidateId,
            List<String> sourceRefs,
            String text,
            String availability,
            String reason,
            int modelCalls) {
        TreeMap<String, Object> identity = new TreeMap<>();
        identity.put("schema", ChromeBrowserSummarizerCandidateProvider.RESULT_SCHEMA);
        identity.put("status", status);
        identity.put("source_candidate_id", candidateId);
        identity.put("source_refs", sourceRefs);
        identity.put("text", text);
        identity.put("availability", availability);
        identity.put("reason", reason);
        identity.put("authority", "MODEL_CANDIDATE");
        identity.put("candidate_only", true);
        identity.put("admitted", false);
        identity.put("model_calls", modelCalls);
        identity.put("direct_network_calls", 0);
        return identity;
    }

    private static byte[] result(
            TreeMap<String, Object> identity, List<Integer> progress) {
        TreeMap<String, Object> result = new TreeMap<>(identity);
        result.put(
                "result_id",
                ChromeBrowserSummarizerCandidateProvider.resultId(identity));
        result.put("browser_managed_download_possible", true);
        result.put("download_progress_million", progress);
        return CanonicalJson.bytes(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> castStrings(Object value) {
        return (List<String>) value;
    }

    private static String hash(String value) {
        return Sha256.digest(value.getBytes(StandardCharsets.UTF_8));
    }
}
