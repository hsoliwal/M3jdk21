// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.evidence;

import com.m3.llm.internal.CanonicalJson;
import com.m3.llm.internal.Sha256;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Candidate-only Java adapter for the browser CPULLM Chrome Summarizer contract.
 *
 * <p>The browser transport is host-owned (CDP/WebView/desktop bridge/etc). This class owns only
 * deterministic request construction and strict result validation. Browser model output never
 * becomes source evidence, never changes the CPU source root and never gains admission/promotion
 * authority.</p>
 */
public final class ChromeBrowserSummarizerCandidateProvider
        implements MixedModeSummaryConsolidator.CandidateProvider {
    public static final String REQUEST_SCHEMA = "cpullm.chrome-summarizer-request.v1";
    public static final String RESULT_SCHEMA = "cpullm.chrome-summarizer-result.v2";
    public static final String RESULT_NAMESPACE = "CPULLM_CHROME_SUMMARIZER_RESULT_V2";
    public static final String API = "Summarizer";

    private static final int MAX_INPUT_BYTES = 16 * 1024;
    private static final int MAX_REQUEST_BYTES = 64 * 1024;
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final Set<String> AVAILABILITY =
            Set.of("unavailable", "downloadable", "downloading", "available");
    private static final Set<String> RESULT_FIELDS =
            Set.of(
                    "schema",
                    "status",
                    "source_candidate_id",
                    "source_refs",
                    "text",
                    "availability",
                    "reason",
                    "authority",
                    "candidate_only",
                    "admitted",
                    "model_calls",
                    "direct_network_calls",
                    "result_id",
                    "browser_managed_download_possible",
                    "download_progress_million");

    /**
     * Host bridge to the browser workbench.
     *
     * <p>The identity is a SHA-256 receipt for the configured host/bridge implementation and is used
     * for explicit abstention provenance when the transport itself fails.</p>
     */
    public interface BrowserTransport {
        String identity();

        byte[] exchange(byte[] canonicalRequest) throws Exception;
    }

    private final BrowserTransport transport;

    public ChromeBrowserSummarizerCandidateProvider(BrowserTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
        requireSha(transport.identity(), "transport identity");
    }

    @Override
    public MixedModeSummaryConsolidator.CandidateAttempt propose(
            MixedModeSummaryConsolidator.CandidateRequest request) {
        Objects.requireNonNull(request, "request");

        byte[] input = request.deterministicSummary().getBytes(StandardCharsets.UTF_8);
        if (input.length > MAX_INPUT_BYTES) {
            return abstain("INPUT_BUDGET_EXCEEDED", transport.identity());
        }

        byte[] encoded = CanonicalJson.bytes(requestPacket(request));
        if (encoded.length > MAX_REQUEST_BYTES) {
            return abstain("REQUEST_BUDGET_EXCEEDED", transport.identity());
        }
        final byte[] response;
        try {
            response = transport.exchange(encoded.clone());
        } catch (Exception failure) {
            return abstain("BROWSER_TRANSPORT_FAILED", transport.identity());
        }
        if (response == null || response.length == 0 || response.length > MAX_RESPONSE_BYTES) {
            return abstain("BROWSER_RESULT_BOUND", transport.identity());
        }

        try {
            ResultEnvelope envelope = decode(response, request);
            String provider = "CHROME_BROWSER_SUMMARIZER:" + envelope.availability();
            if (envelope.status().equals("CANDIDATE_ONLY")) {
                byte[] output = envelope.text().getBytes(StandardCharsets.UTF_8);
                if (output.length > request.maxOutputBytes()) {
                    return MixedModeSummaryConsolidator.CandidateAttempt.abstain(
                            provider,
                            "MODEL_OUTPUT_BUDGET",
                            envelope.resultId());
                }
                return MixedModeSummaryConsolidator.CandidateAttempt.candidate(
                        provider,
                        envelope.text(),
                        envelope.resultId());
            }
            return MixedModeSummaryConsolidator.CandidateAttempt.abstain(
                    provider,
                    envelope.reason(),
                    envelope.resultId());
        } catch (RuntimeException invalid) {
            return abstain("BROWSER_RESULT_INVALID", transport.identity());
        }
    }

    /** Canonical request matching cpullm-opdag/web/chrome_ai_summarizer_tool.mjs. */
    public Map<String, Object> requestPacket(
            MixedModeSummaryConsolidator.CandidateRequest request) {
        Objects.requireNonNull(request, "request");
        TreeMap<String, Object> packet = new TreeMap<>();
        packet.put("schema", REQUEST_SCHEMA);
        packet.put("api", API);
        packet.put("candidate_id", request.root());
        packet.put("authority", "CANDIDATE_ONLY");
        packet.put(
                "status",
                request.sourceIds().isEmpty()
                        ? "ABSTAIN"
                        : "CANDIDATE_ONLY");
        packet.put("model_calls", 0);
        packet.put("network_calls", 0);
        packet.put("input", request.deterministicSummary());
        packet.put("source_refs", request.sourceIds());
        packet.put("omitted_refs", request.omittedSourceIds());
        return Collections.unmodifiableMap(packet);
    }

    private MixedModeSummaryConsolidator.CandidateAttempt abstain(
            String reason, String receiptRoot) {
        return MixedModeSummaryConsolidator.CandidateAttempt.abstain(
                "CHROME_BROWSER_SUMMARIZER",
                reason,
                receiptRoot);
    }

    private static ResultEnvelope decode(
            byte[] raw,
            MixedModeSummaryConsolidator.CandidateRequest request) {
        Object parsed = CanonicalJson.parse(strictUtf8(raw));
        Map<String, Object> map = object(parsed);
        if (!RESULT_FIELDS.equals(map.keySet())) {
            throw new IllegalArgumentException("browser result fields");
        }

        String schema = text(map.get("schema"), "schema");
        String status = text(map.get("status"), "status");
        String candidateId = text(map.get("source_candidate_id"), "source_candidate_id");
        List<String> sourceRefs = stringList(map.get("source_refs"), "source_refs");
        String text = textAllowEmpty(map.get("text"), "text");
        String availability = text(map.get("availability"), "availability");
        String reason = textAllowEmpty(map.get("reason"), "reason");
        String authority = text(map.get("authority"), "authority");
        boolean candidateOnly = bool(map.get("candidate_only"), "candidate_only");
        boolean admitted = bool(map.get("admitted"), "admitted");
        int modelCalls = integer(map.get("model_calls"), "model_calls");
        int directNetworkCalls =
                integer(map.get("direct_network_calls"), "direct_network_calls");
        String resultId = requireSha(text(map.get("result_id"), "result_id"), "result_id");
        boolean downloadPossible =
                bool(
                        map.get("browser_managed_download_possible"),
                        "browser_managed_download_possible");
        List<Integer> progress =
                integerList(map.get("download_progress_million"), "download_progress_million");

        if (!schema.equals(RESULT_SCHEMA)
                || (!status.equals("CANDIDATE_ONLY") && !status.equals("ABSTAIN"))
                || !candidateId.equals(request.root())
                || !sourceRefs.equals(request.sourceIds())
                || !availabilityAllowed(availability)
                || !authority.equals("MODEL_CANDIDATE")
                || !candidateOnly
                || admitted
                || directNetworkCalls != 0
                || !downloadPossible) {
            throw new IllegalArgumentException("browser result contract");
        }
        if (status.equals("CANDIDATE_ONLY")) {
            if (text.isBlank() || !reason.isEmpty() || modelCalls != 1) {
                throw new IllegalArgumentException("candidate result contract");
            }
        } else {
            if (!text.isEmpty() || reason.isBlank() || modelCalls < 0 || modelCalls > 1) {
                throw new IllegalArgumentException("abstain result contract");
            }
        }
        for (int value : progress) {
            if (value < 0 || value > 1_000_000) {
                throw new IllegalArgumentException("download progress");
            }
        }

        TreeMap<String, Object> identity = new TreeMap<>();
        identity.put("schema", schema);
        identity.put("status", status);
        identity.put("source_candidate_id", candidateId);
        identity.put("source_refs", sourceRefs);
        identity.put("text", text);
        identity.put("availability", availability);
        identity.put("reason", reason);
        identity.put("authority", authority);
        identity.put("candidate_only", candidateOnly);
        identity.put("admitted", admitted);
        identity.put("model_calls", modelCalls);
        identity.put("direct_network_calls", directNetworkCalls);
        String expected = resultId(identity);
        if (!expected.equals(resultId)) {
            throw new IllegalArgumentException("browser result identity mismatch");
        }

        return new ResultEnvelope(
                status,
                sourceRefs,
                text,
                availability,
                reason,
                modelCalls,
                resultId,
                progress);
    }

    /** Exact Java implementation of the browser result identity function. */
    public static String resultId(Map<String, Object> identity) {
        Objects.requireNonNull(identity, "identity");
        String canonical = CanonicalJson.string(identity);
        return Sha256.digest(
                (RESULT_NAMESPACE + "\0" + canonical).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean availabilityAllowed(String value) {
        return AVAILABILITY.contains(value);
    }

    private static String strictUtf8(byte[] raw) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw))
                    .toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("browser result UTF-8", invalid);
        }
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("browser result object");
        }
        TreeMap<String, Object> result = new TreeMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("browser result key");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static List<String> stringList(Object value, String field) {
        if (!(value instanceof List<?> raw)) throw new IllegalArgumentException(field);
        ArrayList<String> result = new ArrayList<>(raw.size());
        for (Object item : raw) result.add(text(item, field));
        List<String> sorted = result.stream().distinct().sorted().toList();
        if (!sorted.equals(result)) throw new IllegalArgumentException(field);
        return List.copyOf(result);
    }

    private static List<Integer> integerList(Object value, String field) {
        if (!(value instanceof List<?> raw)) throw new IllegalArgumentException(field);
        ArrayList<Integer> result = new ArrayList<>(raw.size());
        for (Object item : raw) result.add(integer(item, field));
        return List.copyOf(result);
    }

    private static int integer(Object value, String field) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException(field);
        long asLong = number.longValue();
        if (Double.compare(number.doubleValue(), (double) asLong) != 0
                || asLong < Integer.MIN_VALUE
                || asLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(field);
        }
        return (int) asLong;
    }

    private static boolean bool(Object value, String field) {
        if (!(value instanceof Boolean result)) throw new IllegalArgumentException(field);
        return result;
    }

    private static String text(Object value, String field) {
        String result = textAllowEmpty(value, field);
        if (result.isEmpty()) throw new IllegalArgumentException(field);
        return result;
    }

    private static String textAllowEmpty(Object value, String field) {
        if (!(value instanceof String result) || result.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return result;
    }

    private static String requireSha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    private record ResultEnvelope(
            String status,
            List<String> sourceRefs,
            String text,
            String availability,
            String reason,
            int modelCalls,
            String resultId,
            List<Integer> progress) {
        private ResultEnvelope {
            sourceRefs = List.copyOf(sourceRefs);
            progress = List.copyOf(progress);
        }
    }
}
