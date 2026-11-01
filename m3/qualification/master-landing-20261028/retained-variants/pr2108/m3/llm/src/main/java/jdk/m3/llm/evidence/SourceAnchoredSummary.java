// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.evidence;

import com.m3.llm.internal.PlanHash;
import com.m3.llm.internal.LuhnSummarizer;
import com.m3.llm.internal.M3ContractException;
import com.m3.llm.internal.Sha256;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Exact-source extractive summary with character-span ancestry. */
public record SourceAnchoredSummary(String sourceRoot, List<Span> spans, String root) {
    public record Span(int startInclusive, int endExclusive, String text, String contentHash) {
        public Span {
            if (startInclusive < 0 || endExclusive < startInclusive || text == null || contentHash == null) {
                throw new M3ContractException("invalid extractive summary span");
            }
        }
    }

    public SourceAnchoredSummary {
        spans = List.copyOf(spans);
        String expected = calculateRoot(sourceRoot, spans);
        if (root == null || root.isBlank()) root = expected;
        else if (!root.equals(expected)) throw new M3ContractException("summary root mismatch");
    }

    public static SourceAnchoredSummary summarize(String source, int maxSentences) {
        if (source == null || maxSentences < 1) throw new M3ContractException("source and positive sentence bound are required");
        String sourceRoot = Sha256.digest(source.getBytes(StandardCharsets.UTF_8));
        List<String> selected = LuhnSummarizer.summarize(source, maxSentences);
        ArrayList<Span> spans = new ArrayList<>(selected.size());
        int cursor = 0;
        for (String sentence : selected) {
            int start = source.indexOf(sentence, cursor);
            if (start < 0) throw new M3ContractException("extractive sentence is not present in source");
            int end = start + sentence.length();
            spans.add(new Span(start, end, sentence, Sha256.digest(sentence.getBytes(StandardCharsets.UTF_8))));
            cursor = end;
        }
        return new SourceAnchoredSummary(sourceRoot, spans, "");
    }

    public String render(String source) {
        if (!Sha256.digest(source.getBytes(StandardCharsets.UTF_8)).equals(sourceRoot)) {
            throw new M3ContractException("summary source root mismatch");
        }
        return spans.stream().map(span -> {
            String exact = source.substring(span.startInclusive(), span.endExclusive());
            if (!exact.equals(span.text())) throw new M3ContractException("summary source span mismatch");
            return exact;
        }).reduce("", (left, right) -> left.isEmpty() ? right : left + System.lineSeparator() + right);
    }

    private static String calculateRoot(String sourceRoot, List<Span> spans) {
        StringBuilder canonical = new StringBuilder();
        PlanHash.append(canonical, sourceRoot);
        for (Span span : spans) {
            PlanHash.append(canonical, Integer.toString(span.startInclusive()));
            PlanHash.append(canonical, Integer.toString(span.endExclusive()));
            PlanHash.append(canonical, span.contentHash());
        }
        return PlanHash.sha256(canonical.toString());
    }
}
