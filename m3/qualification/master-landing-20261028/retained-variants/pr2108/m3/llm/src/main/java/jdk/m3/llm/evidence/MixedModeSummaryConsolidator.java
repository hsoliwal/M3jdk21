// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.evidence;

import com.m3.llm.internal.Sha256;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Mixed deterministic/model summary contract.
 *
 * <p>The source-anchored CPU summary is deterministic evidence. An optional model provider may
 * return only a candidate or abstain. Model text never becomes evidence, never mutates canon and
 * never changes the source root. Promotion remains serial and external to this class.</p>
 */
public final class MixedModeSummaryConsolidator {
    public enum CandidateStatus {
        CANDIDATE_ONLY,
        ABSTAIN
    }

    public record Evidence(String sourceId, String text, String contentSha256) {
        public Evidence {
            sourceId = token(sourceId, "sourceId");
            text = Objects.requireNonNull(text, "text");
            if (text.indexOf('\0') >= 0) throw new IllegalArgumentException("text");
            String actual = Sha256.digest(text.getBytes(StandardCharsets.UTF_8));
            if (contentSha256 == null || contentSha256.isBlank()) contentSha256 = actual;
            if (!actual.equals(contentSha256)) {
                throw new IllegalArgumentException("evidence content hash mismatch");
            }
        }

        public Evidence(String sourceId, String text) {
            this(sourceId, text, "");
        }
    }

    public record CandidateRequest(
            String sourceRoot,
            String deterministicSummary,
            List<String> sourceIds,
            List<String> omittedSourceIds,
            List<String> evidenceRoots,
            List<String> omittedEvidenceRoots,
            int maxOutputBytes,
            String root) {
        public CandidateRequest {
            sourceRoot = sha(sourceRoot, "sourceRoot");
            deterministicSummary = Objects.requireNonNull(deterministicSummary, "deterministicSummary");
            if (deterministicSummary.isBlank() || deterministicSummary.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("deterministicSummary");
            }
            sourceIds = stableTokens(sourceIds, "sourceIds");
            omittedSourceIds = stableTokens(omittedSourceIds, "omittedSourceIds");
            evidenceRoots = stableHashes(evidenceRoots, "evidenceRoots");
            omittedEvidenceRoots = stableHashes(omittedEvidenceRoots, "omittedEvidenceRoots");
            if ((sourceIds.isEmpty() && omittedSourceIds.isEmpty())
                    || (evidenceRoots.isEmpty() && omittedEvidenceRoots.isEmpty())) {
                throw new IllegalArgumentException("candidate request requires source lineage");
            }

            Set<String> sourceOverlap = new java.util.HashSet<>(sourceIds);
            sourceOverlap.retainAll(omittedSourceIds);
            if (!sourceOverlap.isEmpty()) {
                throw new IllegalArgumentException("selected and omitted source IDs overlap");
            }
            Set<String> rootOverlap = new java.util.HashSet<>(evidenceRoots);
            rootOverlap.retainAll(omittedEvidenceRoots);
            if (!rootOverlap.isEmpty()) {
                throw new IllegalArgumentException("selected and omitted evidence roots overlap");
            }
            if (maxOutputBytes < 1 || maxOutputBytes > 262_144) {
                throw new IllegalArgumentException("maxOutputBytes");
            }
            String expected =
                    digest(
                            "CPULLM_MIXED_SUMMARY_CANDIDATE_REQUEST_V3",
                            sourceRoot,
                            deterministicSummary,
                            String.join("\u001f", sourceIds),
                            String.join("\u001f", omittedSourceIds),
                            String.join("\u001f", evidenceRoots),
                            String.join("\u001f", omittedEvidenceRoots),
                            Integer.toString(maxOutputBytes));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("candidate request root mismatch");
        }

        /** Source-compatible constructor retaining evidence-level omission metadata. */
        public CandidateRequest(
                String sourceRoot,
                String deterministicSummary,
                List<String> sourceIds,
                List<String> evidenceRoots,
                List<String> omittedEvidenceRoots,
                int maxOutputBytes,
                String root) {
            this(
                    sourceRoot,
                    deterministicSummary,
                    sourceIds,
                    List.of(),
                    evidenceRoots,
                    omittedEvidenceRoots,
                    maxOutputBytes,
                    root);
        }

        /** Backward-compatible source constructor for providers without omission metadata. */
        public CandidateRequest(
                String sourceRoot,
                String deterministicSummary,
                List<String> sourceIds,
                List<String> evidenceRoots,
                int maxOutputBytes,
                String root) {
            this(
                    sourceRoot,
                    deterministicSummary,
                    sourceIds,
                    List.of(),
                    evidenceRoots,
                    List.of(),
                    maxOutputBytes,
                    root);
        }
    }

    public record CandidateAttempt(
            CandidateStatus status,
            String provider,
            String text,
            String providerReceiptRoot,
            String reason,
            boolean admitted,
            String root) {
        public CandidateAttempt {
            status = Objects.requireNonNull(status, "status");
            provider = token(provider, "provider");
            text = Objects.toString(text, "");
            reason = Objects.toString(reason, "");
            if (text.indexOf('\0') >= 0 || reason.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("candidate text/reason");
            }
            if (admitted) {
                throw new IllegalArgumentException("model candidate cannot be admitted");
            }
            if (status == CandidateStatus.CANDIDATE_ONLY) {
                if (text.isBlank()) throw new IllegalArgumentException("candidate text required");
                providerReceiptRoot = sha(providerReceiptRoot, "providerReceiptRoot");
            } else {
                if (reason.isBlank()) throw new IllegalArgumentException("abstain reason required");
                providerReceiptRoot =
                        providerReceiptRoot == null || providerReceiptRoot.isBlank()
                                ? ""
                                : sha(providerReceiptRoot, "providerReceiptRoot");
            }
            String expected =
                    digest(
                            "CPULLM_MIXED_SUMMARY_CANDIDATE_ATTEMPT_V1",
                            status.name(),
                            provider,
                            text,
                            providerReceiptRoot,
                            reason,
                            Boolean.toString(admitted));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("candidate attempt root mismatch");
        }

        public static CandidateAttempt candidate(
                String provider, String text, String providerReceiptRoot) {
            return new CandidateAttempt(
                    CandidateStatus.CANDIDATE_ONLY,
                    provider,
                    text,
                    providerReceiptRoot,
                    "",
                    false,
                    "");
        }

        public static CandidateAttempt abstain(
                String provider, String reason, String providerReceiptRoot) {
            return new CandidateAttempt(
                    CandidateStatus.ABSTAIN,
                    provider,
                    "",
                    providerReceiptRoot,
                    reason,
                    false,
                    "");
        }

        public boolean candidateOnly() {
            return status == CandidateStatus.CANDIDATE_ONLY;
        }

        public boolean canonWritable() {
            return false;
        }
    }

    @FunctionalInterface
    public interface CandidateProvider {
        CandidateAttempt propose(CandidateRequest request);
    }

    public record Result(
            String sourceRoot,
            SourceAnchoredSummary extractive,
            CandidateRequest request,
            CandidateAttempt candidate,
            String rendered,
            boolean serialPromotionRequired,
            String root) {
        public Result {
            sourceRoot = sha(sourceRoot, "sourceRoot");
            extractive = Objects.requireNonNull(extractive, "extractive");
            request = Objects.requireNonNull(request, "request");
            candidate = Objects.requireNonNull(candidate, "candidate");
            rendered = Objects.requireNonNull(rendered, "rendered");
            if (!extractive.sourceRoot().equals(sourceRoot)
                    || !request.sourceRoot().equals(sourceRoot)) {
                throw new IllegalArgumentException("mixed summary source-root mismatch");
            }
            if (!serialPromotionRequired) {
                throw new IllegalArgumentException("serial promotion is mandatory");
            }
            String expected =
                    digest(
                            "CPULLM_MIXED_SUMMARY_RESULT_V1",
                            sourceRoot,
                            extractive.root(),
                            request.root(),
                            candidate.root(),
                            Sha256.digest(rendered.getBytes(StandardCharsets.UTF_8)),
                            Boolean.toString(serialPromotionRequired));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("mixed summary root mismatch");
        }

        public boolean modelAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private MixedModeSummaryConsolidator() {}

    public static Result summarize(
            List<Evidence> evidence,
            int maxSentences,
            int maxOutputBytes,
            CandidateProvider provider) {
        if (maxSentences < 1 || maxSentences > 256) {
            throw new IllegalArgumentException("maxSentences");
        }
        List<Evidence> ordered =
                Objects.requireNonNull(evidence, "evidence").stream()
                        .map(Objects::requireNonNull)
                        .sorted(Comparator.comparing(Evidence::sourceId)
                                .thenComparing(Evidence::contentSha256))
                        .toList();
        if (ordered.isEmpty() || ordered.size() > 4096) {
            throw new IllegalArgumentException("evidence size");
        }
        TreeSet<String> identities = new TreeSet<>();
        for (Evidence item : ordered) {
            if (!identities.add(item.sourceId())) {
                throw new IllegalArgumentException("duplicate sourceId: " + item.sourceId());
            }
        }

        CanonicalSource source = canonicalSource(ordered);
        SourceAnchoredSummary extractive =
                SourceAnchoredSummary.summarize(source.text(), maxSentences);
        String deterministic = extractive.render(source.text());
        EvidenceSelection selection = selectEvidence(source.ranges(), extractive.spans());
        CandidateRequest request =
                new CandidateRequest(
                        extractive.sourceRoot(),
                        deterministic,
                        selection.selectedSourceIds(),
                        selection.omittedSourceIds(),
                        selection.selectedRoots(),
                        selection.omittedRoots(),
                        maxOutputBytes,
                        "");

        CandidateAttempt attempt =
                provider == null
                        ? CandidateAttempt.abstain("NONE", "NO_CANDIDATE_PROVIDER", "")
                        : request.evidenceRoots().isEmpty()
                                ? CandidateAttempt.abstain(
                                        "NONE",
                                        "CPU_CANDIDATE_ABSTAIN",
                                        "")
                                : Objects.requireNonNull(
                                        provider.propose(request),
                                        "candidate provider result");

        String rendered = render(deterministic, attempt, request);
        return new Result(
                extractive.sourceRoot(),
                extractive,
                request,
                attempt,
                rendered,
                true,
                "");
    }

    private static CanonicalSource canonicalSource(List<Evidence> evidence) {
        StringBuilder source = new StringBuilder();
        java.util.ArrayList<EvidenceRange> ranges = new java.util.ArrayList<>();
        for (Evidence item : evidence) {
            if (!source.isEmpty()) source.append("\n\n");
            source.append("[[source=")
                    .append(item.sourceId())
                    .append(" sha256=")
                    .append(item.contentSha256())
                    .append("]]\n");
            int contentStart = source.length();
            source.append(item.text());
            ranges.add(
                    new EvidenceRange(
                            item.sourceId(),
                            item.contentSha256(),
                            contentStart,
                            source.length()));
        }
        return new CanonicalSource(source.toString(), List.copyOf(ranges));
    }

    private static EvidenceSelection selectEvidence(
            List<EvidenceRange> ranges,
            List<SourceAnchoredSummary.Span> spans) {
        LinkedHashSet<String> selectedSourceIds = new LinkedHashSet<>();
        LinkedHashSet<String> selectedRoots = new LinkedHashSet<>();
        for (EvidenceRange range : ranges) {
            for (SourceAnchoredSummary.Span span : spans) {
                if (span.startInclusive() < range.endExclusive()
                        && span.endExclusive() > range.startInclusive()) {
                    selectedSourceIds.add(range.sourceId());
                    selectedRoots.add(range.contentSha256());
                    break;
                }
            }
        }

        TreeSet<String> selectedSourcesSorted = new TreeSet<>(selectedSourceIds);
        TreeSet<String> selectedRootsSorted = new TreeSet<>(selectedRoots);
        TreeSet<String> omittedSourceIds = new TreeSet<>();
        TreeSet<String> omittedRoots = new TreeSet<>();
        for (EvidenceRange range : ranges) {
            if (!selectedSourcesSorted.contains(range.sourceId())) {
                omittedSourceIds.add(range.sourceId());
                if (!selectedRootsSorted.contains(range.contentSha256())) {
                    omittedRoots.add(range.contentSha256());
                }
            }
        }
        return new EvidenceSelection(
                List.copyOf(selectedSourcesSorted),
                List.copyOf(omittedSourceIds),
                List.copyOf(selectedRootsSorted),
                List.copyOf(omittedRoots));
    }

    private record CanonicalSource(String text, List<EvidenceRange> ranges) {
        private CanonicalSource {
            text = Objects.requireNonNull(text, "text");
            ranges = List.copyOf(ranges);
        }
    }

    private record EvidenceRange(
            String sourceId,
            String contentSha256,
            int startInclusive,
            int endExclusive) {
        private EvidenceRange {
            sourceId = token(sourceId, "sourceId");
            contentSha256 = sha(contentSha256, "contentSha256");
            if (startInclusive < 0 || endExclusive < startInclusive) {
                throw new IllegalArgumentException("evidence range");
            }
        }
    }

    private record EvidenceSelection(
            List<String> selectedSourceIds,
            List<String> omittedSourceIds,
            List<String> selectedRoots,
            List<String> omittedRoots) {
        private EvidenceSelection {
            selectedSourceIds = List.copyOf(selectedSourceIds);
            omittedSourceIds = List.copyOf(omittedSourceIds);
            selectedRoots = List.copyOf(selectedRoots);
            omittedRoots = List.copyOf(omittedRoots);
        }
    }

    private static String render(
            String deterministic, CandidateAttempt attempt, CandidateRequest request) {
        StringBuilder out = new StringBuilder();
        out.append("# CPULLM Mixed Summary\n\n")
                .append("## Deterministic source-anchored summary\n\n")
                .append(deterministic)
                .append("\n\n## Model candidate - non-authoritative\n\n");
        if (attempt.status() == CandidateStatus.CANDIDATE_ONLY) {
            out.append(attempt.text());
        } else {
            out.append("_ABSTAIN: ").append(attempt.reason()).append('_');
        }
        out.append("\n\n## Receipt\n\n")
                .append("- source-root: ").append(request.sourceRoot()).append('\n')
                .append("- source-ids: ").append(String.join(",", request.sourceIds())).append('\n')
                .append("- omitted-source-ids: ")
                .append(String.join(",", request.omittedSourceIds()))
                .append('\n')
                .append("- evidence-roots: ").append(String.join(",", request.evidenceRoots())).append('\n')
                .append("- omitted-evidence-roots: ")
                .append(String.join(",", request.omittedEvidenceRoots()))
                .append('\n')
                .append("- candidate-request-root: ").append(request.root()).append('\n')
                .append("- candidate-attempt-root: ").append(attempt.root()).append('\n')
                .append("- candidate-only: true\n")
                .append("- serial-promotion-required: true\n")
                .append("- model-can-write-canon: false\n");
        return out.toString();
    }

    private static List<String> stableTokens(List<String> values, String field) {
        TreeSet<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, field)) {
            result.add(token(value, field));
        }
        return List.copyOf(result);
    }

    private static List<String> stableHashes(List<String> values, String field) {
        TreeSet<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, field)) {
            result.add(sha(value, field));
        }
        return List.copyOf(result);
    }

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = token(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String domain, String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, domain);
            for (String value : values) frame(digest, value);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
