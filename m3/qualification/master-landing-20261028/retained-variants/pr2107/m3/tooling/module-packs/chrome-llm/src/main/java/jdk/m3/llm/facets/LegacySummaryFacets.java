/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/com/synexia/cpullm/facets/LegacySummaryFacets.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Imports the existing Summary.hierarchical() sentence table, not its lossy selected-text frontier. */
public final class LegacySummaryFacets {
    private LegacySummaryFacets() { }
    public record Tags(long facets, int priority, boolean mandatory, String conflictGroup) { }

    /**
     * Validates exact sentence occurrences against original title/body records. This is not a claim
     * that the legacy sentence parser completely covers every source byte or detects contradictions.
     * All original title/body bytes remain accessible. Facets/obligations are explicit caller input.
     */
    public static FacetCorpus prepare(List<Map<String, Object>> records, Map<String, Object> legacySummary,
                                      List<String> vocabulary, Map<String, Tags> tagsByEvidenceId, int byteBudget) {
        Objects.requireNonNull(records, "records"); Objects.requireNonNull(legacySummary, "legacySummary");
        Objects.requireNonNull(tagsByEvidenceId, "tags");
        if (records.size() > FacetCorpus.MAX_ATOMS) throw new IllegalArgumentException("record limit");
        Object input = legacySummary.get("sentences");
        if (!(input instanceof List<?> sentences) || sentences.size() > FacetCorpus.MAX_ATOMS) throw new IllegalArgumentException("legacy sentence table missing/bounded");
        var sources = new ArrayList<FacetCorpus.Source>(); var textByRecord = new HashMap<String, String>();
        if (byteBudget < 0 || byteBudget > FacetCorpus.MAX_BYTES) throw new IllegalArgumentException("legacy byte budget");
        long bytes = 0;
        for (Map<String, Object> record : records) {
            String id = text(record.get("record_id")), title = text(record.get("title")), body = text(record.get("body"));
            if ((long) title.length() + body.length() + 2 > byteBudget - bytes) throw new IllegalArgumentException("legacy source character budget");
            String original = title + "\n\n" + body;
            byte[] encoded = FacetCorpus.utf8(original); bytes += encoded.length;
            if (bytes > byteBudget) throw new IllegalArgumentException("legacy source budget");
            if (textByRecord.putIfAbsent(id, original) != null) throw new IllegalArgumentException("duplicate record identity");
            sources.add(new FacetCorpus.Source(id, encoded));
        }
        Map<String, Integer> scores = leafScores(legacySummary);
        var cursor = new HashMap<String, Integer>(); var byteCursor = new HashMap<String, Integer>(); var seenIds = new HashSet<String>();
        var slices = new ArrayList<FacetCorpus.Slice>();
        for (Object value : sentences) {
            FacetCorpus.cancelled();
            if (!(value instanceof Map<?, ?> sentence)) throw new IllegalArgumentException("legacy sentence object required");
            String id = text(sentence.get("evidence_id")), record = text(sentence.get("record_id")), excerpt = text(sentence.get("text"));
            if (!seenIds.add(id) || excerpt.isEmpty()) throw new IllegalArgumentException("duplicate evidence ID or empty excerpt");
            String source = textByRecord.get(record);
            if (source == null) throw new IllegalArgumentException("unknown sentence source");
            int start = source.indexOf(excerpt, cursor.getOrDefault(record, 0));
            if (start < 0) throw new IllegalArgumentException("sentence not found in source order; fabricated/rewritten text rejected");
            int previous = cursor.getOrDefault(record, 0);
            int byteStart = byteCursor.getOrDefault(record, 0) + FacetCorpus.utf8(source.substring(previous, start)).length;
            cursor.put(record, start + excerpt.length());
            byteCursor.put(record, byteStart + FacetCorpus.utf8(excerpt).length);
            Tags tag = tagsByEvidenceId.getOrDefault(id, new Tags(0, scores.getOrDefault(id, 0), false, ""));
            slices.add(new FacetCorpus.Slice(record, byteStart, byteStart + FacetCorpus.utf8(excerpt).length,
                    tag.facets(), tag.priority(), tag.mandatory(), tag.conflictGroup()));
        }
        if (!seenIds.containsAll(scores.keySet())) throw new IllegalArgumentException("scores refer to missing evidence");
        if (!seenIds.containsAll(tagsByEvidenceId.keySet())) throw new IllegalArgumentException("tags refer to missing evidence");
        return new FacetCorpus(sources, vocabulary, slices, byteBudget);
    }
    private static Map<String, Integer> leafScores(Map<String, Object> summary) {
        var scores = new HashMap<String, Integer>();
        Object value = summary.get("nodes");
        if (value == null) return scores;
        if (!(value instanceof List<?> nodes) || nodes.size() > FacetCorpus.MAX_ATOMS * 2) throw new IllegalArgumentException("node list limit");
        int entries = 0;
        for (Object node : nodes) {
            if (!(node instanceof Map<?, ?> m)) throw new IllegalArgumentException("node object required");
            if (!"0".equals(String.valueOf(m.get("level")))) continue;
            if (!(m.get("method_scores") instanceof Map<?, ?> methods) || methods.size() > 16) throw new IllegalArgumentException("method score list");
            for (Object method : methods.values()) {
                if (!(method instanceof List<?> ranked) || ranked.size() > 4096) throw new IllegalArgumentException("ranked score budget");
                entries = Math.addExact(entries, ranked.size());
                if (entries > 16 * FacetCorpus.MAX_ATOMS) throw new IllegalArgumentException("score entry aggregate budget");
                var methodIds = new HashSet<String>();
                for (int i = 0; i < ranked.size(); i++) {
                    if (!(ranked.get(i) instanceof Map<?, ?> row)) throw new IllegalArgumentException("ranked score object");
                    String id = text(row.get("evidence_id"));
                    if (!methodIds.add(id)) throw new IllegalArgumentException("duplicate evidence in method ranking");
                    int score = scores.getOrDefault(id, 0) + 1_000_000 / (61 + i);
                    if (score > 1_000_000) throw new IllegalArgumentException("duplicate/inflated leaf scores");
                    scores.put(id, score);
                }
            }
        }
        return scores;
    }
    private static String text(Object value) {
        if (!(value instanceof String s)) throw new IllegalArgumentException("legacy string field required"); return s;
    }
}
