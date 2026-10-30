// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.evidence;

import com.m3.llm.internal.CanonicalJson;
import com.m3.llm.internal.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Receipt adapter from the existing browser Chrome Summarizer summary-tree candidate into the
 * provider-neutral CPULLM mixed-summary contract.
 *
 * <p>This class does not execute a model. It verifies the content-addressed browser receipt,
 * including every node result, source binding, semantic root and authority fence. Invalid receipts
 * fail closed as explicit ABSTAIN results.</p>
 */
public final class BrowserSummaryTreeCandidateProvider
        implements MixedModeSummaryConsolidator.CandidateProvider {
    private static final String SCHEMA = "cpullm.chrome-summary-tree-candidate.v1";
    private static final String SEMANTIC_SCHEMA = "cpullm.chrome-summary-tree-semantic.v1";
    private static final String PROVIDER = "CHROME_SUMMARIZER_TREE";
    private static final Set<String> REQUIRED_CANDIDATE_FIELDS =
            Set.of(
                    "schema",
                    "revision",
                    "source_ref",
                    "source_sha256",
                    "mechanical_root",
                    "root_node_id",
                    "root_result_id",
                    "root_summary",
                    "nodes",
                    "model_calls_observed",
                    "resumed_nodes",
                    "exact_duplicate_leaf_reuses",
                    "node_count_completed",
                    "session_strategy",
                    "provider_fallback",
                    "candidate_only",
                    "admitted",
                    "execution_authorized",
                    "merge_authorized",
                    "deletion_authorized",
                    "locality_widening_authorized",
                    "structured_entropy",
                    "semantic_candidate_root",
                    "note",
                    "candidate_root");
    private static final Set<String> OPTIONAL_RUNTIME_FIELDS =
            Set.of("session_creations", "availability", "download_progress_million");
    private static final Set<String> REQUIRED_NODE_FIELDS =
            Set.of(
                    "node_id",
                    "kind",
                    "level",
                    "ordinal",
                    "children",
                    "coverage_leaf_ordinals",
                    "input_sha256",
                    "output_sha256",
                    "child_result_ids",
                    "result_id",
                    "summary",
                    "provider",
                    "candidate_only",
                    "admitted",
                    "execution_authorized",
                    "merge_authorized");

    @FunctionalInterface
    public interface ReceiptSource {
        String receiptJson(MixedModeSummaryConsolidator.CandidateRequest request);
    }

    private final ReceiptSource source;

    public BrowserSummaryTreeCandidateProvider(String receiptJson) {
        this(request -> receiptJson);
    }

    public BrowserSummaryTreeCandidateProvider(ReceiptSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public MixedModeSummaryConsolidator.CandidateAttempt propose(
            MixedModeSummaryConsolidator.CandidateRequest request) {
        Objects.requireNonNull(request, "request");
        String json;
        try {
            json = Objects.requireNonNull(source.receiptJson(request), "receiptJson");
            VerifiedReceipt verified = verify(request, json);
            if (verified.summary().getBytes(StandardCharsets.UTF_8).length
                    > request.maxOutputBytes()) {
                return MixedModeSummaryConsolidator.CandidateAttempt.abstain(
                        PROVIDER, "BROWSER_SUMMARY_OUTPUT_BUDGET", verified.candidateRoot());
            }
            return MixedModeSummaryConsolidator.CandidateAttempt.candidate(
                    PROVIDER, verified.summary(), verified.candidateRoot());
        } catch (RuntimeException invalid) {
            return MixedModeSummaryConsolidator.CandidateAttempt.abstain(
                    PROVIDER, "INVALID_BROWSER_SUMMARY_RECEIPT", "");
        }
    }

    public static String sourceRef(MixedModeSummaryConsolidator.CandidateRequest request) {
        return "cpullm-mixed-summary:" + Objects.requireNonNull(request, "request").root();
    }

    static VerifiedReceipt verify(
            MixedModeSummaryConsolidator.CandidateRequest request, String json) {
        Map<String, Object> body = map(CanonicalJson.parse(json), "candidate");
        requireFields(body, REQUIRED_CANDIDATE_FIELDS, OPTIONAL_RUNTIME_FIELDS, "candidate");
        if (!SCHEMA.equals(string(body, "schema"))
                || number(body, "revision") != 1L
                || !Boolean.TRUE.equals(body.get("candidate_only"))
                || !Boolean.FALSE.equals(body.get("admitted"))
                || !Boolean.FALSE.equals(body.get("execution_authorized"))
                || !Boolean.FALSE.equals(body.get("merge_authorized"))
                || !Boolean.FALSE.equals(body.get("deletion_authorized"))
                || !Boolean.FALSE.equals(body.get("locality_widening_authorized"))
                || !Boolean.FALSE.equals(body.get("provider_fallback"))) {
            throw new IllegalArgumentException("candidate authority/schema");
        }

        string(body, "session_strategy");
        string(body, "note");
        map(body.get("structured_entropy"), "structured_entropy");
        if (number(body, "model_calls_observed") < 0
                || number(body, "resumed_nodes") < 0
                || number(body, "exact_duplicate_leaf_reuses") < 0) {
            throw new IllegalArgumentException("candidate counters");
        }

        String expectedSourceRef = sourceRef(request);
        if (!expectedSourceRef.equals(string(body, "source_ref"))) {
            throw new IllegalArgumentException("source ref");
        }
        String sourceSha = sha(string(body, "source_sha256"), "source_sha256");
        String expectedSourceSha =
                Sha256.digest(request.deterministicSummary().getBytes(StandardCharsets.UTF_8));
        if (!sourceSha.equals(expectedSourceSha)) {
            throw new IllegalArgumentException("source sha");
        }

        String mechanicalRoot = sha(string(body, "mechanical_root"), "mechanical_root");
        String rootNodeId = sha(string(body, "root_node_id"), "root_node_id");
        String rootResultId = sha(string(body, "root_result_id"), "root_result_id");
        String rootSummary = string(body, "root_summary");
        if (rootSummary.isBlank()) throw new IllegalArgumentException("root summary");

        List<Object> rawNodes = list(body.get("nodes"), "nodes");
        if (rawNodes.isEmpty() || rawNodes.size() > 100_000) {
            throw new IllegalArgumentException("node count");
        }
        if (number(body, "node_count_completed") != rawNodes.size()) {
            throw new IllegalArgumentException("completed node count");
        }

        LinkedHashMap<String, NodeReceipt> byId = new LinkedHashMap<>();
        ArrayList<String> resultIds = new ArrayList<>(rawNodes.size());
        for (Object raw : rawNodes) {
            NodeReceipt node = verifyNode(map(raw, "node"), byId);
            if (byId.putIfAbsent(node.nodeId(), node) != null) {
                throw new IllegalArgumentException("duplicate node");
            }
            resultIds.add(node.resultId());
        }

        NodeReceipt root = byId.get(rootNodeId);
        if (root == null
                || !root.resultId().equals(rootResultId)
                || !root.summary().equals(rootSummary)) {
            throw new IllegalArgumentException("root node binding");
        }

        TreeMap<String, Object> semantic = new TreeMap<>();
        semantic.put("schema", SEMANTIC_SCHEMA);
        semantic.put("source_sha256", sourceSha);
        semantic.put("mechanical_root", mechanicalRoot);
        semantic.put("root_node_id", rootNodeId);
        semantic.put("root_result_id", rootResultId);
        semantic.put("node_result_ids", List.copyOf(resultIds));
        String semanticRoot = digestCanonical(semantic);
        if (!semanticRoot.equals(sha(
                string(body, "semantic_candidate_root"), "semantic_candidate_root"))) {
            throw new IllegalArgumentException("semantic root");
        }

        String candidateRoot = sha(string(body, "candidate_root"), "candidate_root");
        TreeMap<String, Object> unsigned = new TreeMap<>(body);
        unsigned.remove("candidate_root");
        // Browser runtime attaches these after candidate_root is sealed.
        unsigned.remove("session_creations");
        unsigned.remove("availability");
        unsigned.remove("download_progress_million");
        if (!candidateRoot.equals(digestCanonical(unsigned))) {
            throw new IllegalArgumentException("candidate root");
        }

        return new VerifiedReceipt(candidateRoot, semanticRoot, rootSummary, rootResultId);
    }

    private static NodeReceipt verifyNode(
            Map<String, Object> body, Map<String, NodeReceipt> previous) {
        requireFields(body, REQUIRED_NODE_FIELDS, Set.of("reused_from_node_id"), "node");
        String nodeId = sha(string(body, "node_id"), "node_id");
        String kind = string(body, "kind");
        if (!kind.equals("LEAF") && !kind.equals("REDUCE")) {
            throw new IllegalArgumentException("node kind");
        }
        long level = number(body, "level");
        long ordinal = number(body, "ordinal");
        if (level < 0 || ordinal < 0) {
            throw new IllegalArgumentException("node position");
        }
        List<String> children = shaList(body.get("children"), "children");
        List<Long> coverage = numberList(body.get("coverage_leaf_ordinals"), "coverage");
        for (int i = 1; i < coverage.size(); i++) {
            if (coverage.get(i - 1) >= coverage.get(i)) {
                throw new IllegalArgumentException("coverage order");
            }
        }
        String inputSha = sha(string(body, "input_sha256"), "input_sha256");
        String outputSha = sha(string(body, "output_sha256"), "output_sha256");
        List<String> childResultIds = shaList(body.get("child_result_ids"), "child_result_ids");
        if ((kind.equals("LEAF") && (!children.isEmpty() || !childResultIds.isEmpty()))
                || (kind.equals("REDUCE") && children.isEmpty())) {
            throw new IllegalArgumentException("node shape");
        }
        String resultId = sha(string(body, "result_id"), "result_id");
        String summary = string(body, "summary");
        String provider = string(body, "provider");
        if (provider.isBlank()) throw new IllegalArgumentException("node provider");
        if (body.containsKey("reused_from_node_id")) {
            sha(string(body, "reused_from_node_id"), "reused_from_node_id");
        }
        if (summary.isBlank()
                || !Boolean.TRUE.equals(body.get("candidate_only"))
                || !Boolean.FALSE.equals(body.get("admitted"))
                || !Boolean.FALSE.equals(body.get("execution_authorized"))
                || !Boolean.FALSE.equals(body.get("merge_authorized"))) {
            throw new IllegalArgumentException("node authority");
        }
        if (!Sha256.digest(summary.getBytes(StandardCharsets.UTF_8)).equals(outputSha)) {
            throw new IllegalArgumentException("node output sha");
        }
        if (children.size() != childResultIds.size()) {
            throw new IllegalArgumentException("child result count");
        }
        for (int i = 0; i < children.size(); i++) {
            NodeReceipt child = previous.get(children.get(i));
            if (child == null || !child.resultId().equals(childResultIds.get(i))) {
                throw new IllegalArgumentException("child result binding");
            }
        }

        TreeMap<String, Object> identity = new TreeMap<>();
        identity.put("node_id", nodeId);
        identity.put("kind", kind);
        identity.put("level", level);
        identity.put("ordinal", ordinal);
        identity.put("children", children);
        identity.put("coverage_leaf_ordinals", coverage);
        identity.put("input_sha256", inputSha);
        identity.put("output_sha256", outputSha);
        identity.put("child_result_ids", childResultIds);
        if (!resultId.equals(digestCanonical(identity))) {
            throw new IllegalArgumentException("node result root");
        }
        return new NodeReceipt(nodeId, resultId, summary);
    }

    private static String digestCanonical(Object value) {
        return Sha256.digest(CanonicalJson.string(value).getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String field) {
        if (!(value instanceof Map<?, ?> input)) throw new IllegalArgumentException(field);
        TreeMap<String, Object> result = new TreeMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException(field);
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static List<Object> list(Object value, String field) {
        if (!(value instanceof List<?> input)) throw new IllegalArgumentException(field);
        return List.copyOf(input);
    }

    private static String string(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof String text) || text.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(key);
        }
        return text;
    }

    private static long number(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof Float || value instanceof Double) {
            double number = ((Number) value).doubleValue();
            if (Double.isFinite(number)
                    && number == StrictMath.rint(number)
                    && number >= Long.MIN_VALUE
                    && number <= Long.MAX_VALUE) {
                return (long) number;
            }
        }
        throw new IllegalArgumentException(key);
    }

    private static void requireFields(
            Map<String, Object> body,
            Set<String> required,
            Set<String> optional,
            String field) {
        if (!body.keySet().containsAll(required)) {
            throw new IllegalArgumentException(field + " missing field");
        }
        for (String key : body.keySet()) {
            if (!required.contains(key) && !optional.contains(key)) {
                throw new IllegalArgumentException(field + " unknown field");
            }
        }
    }

    private static List<String> shaList(Object value, String field) {
        ArrayList<String> result = new ArrayList<>();
        for (Object item : list(value, field)) {
            if (!(item instanceof String text)) throw new IllegalArgumentException(field);
            result.add(sha(text, field));
        }
        return List.copyOf(result);
    }

    private static List<Long> numberList(Object value, String field) {
        ArrayList<Long> result = new ArrayList<>();
        for (Object item : list(value, field)) {
            TreeMap<String, Object> wrapper = new TreeMap<>();
            wrapper.put(field, item);
            long number = number(wrapper, field);
            if (number < 0) throw new IllegalArgumentException(field);
            result.add(number);
        }
        return List.copyOf(result);
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    record VerifiedReceipt(
            String candidateRoot, String semanticRoot, String summary, String rootResultId) {}

    private record NodeReceipt(String nodeId, String resultId, String summary) {}
}
