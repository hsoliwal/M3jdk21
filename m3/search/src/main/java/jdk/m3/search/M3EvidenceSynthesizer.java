/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/reduce/EvidenceSynthesizer.java (blob 051f7f0e7aab) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Query-aware extractive synthesis with explicit source evidence; no generative model required. */
public final class M3EvidenceSynthesizer {
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}_']+");
    private final M3LuhnSummarizer luhn;

    public M3EvidenceSynthesizer(M3LuhnSummarizer luhn) {
        this.luhn = luhn;
    }

    public Synthesis synthesize(String query, List<EvidenceDocument> documents, int sentenceLimit) {
        Set<String> queryTerms = terms(query);
        List<Candidate> candidates = new ArrayList<>();
        for (EvidenceDocument document : documents) {
            for (M3LuhnSummarizer.SummarySentence sentence : luhn.rank(document.text(), 6)) {
                int overlap = overlap(sentence.text(), queryTerms);
                double score = sentence.score() + document.hit().score() + overlap * 3.0;
                candidates.add(new Candidate(document.hit(), sentence.text(), score));
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparing(row -> row.hit().sourceUri().toASCIIString())
                .thenComparing(Candidate::sentence));

        Set<String> seen = new HashSet<>();
        List<Evidence> evidence = new ArrayList<>();
        StringBuilder summary = new StringBuilder();
        for (Candidate candidate : candidates) {
            if (evidence.size() >= sentenceLimit) {
                break;
            }
            String key = candidate.sentence().toLowerCase(Locale.ROOT)
                    .replaceAll("\\s+", " ").trim();
            if (!seen.add(key)) {
                continue;
            }
            int citation = evidence.size() + 1;
            evidence.add(new Evidence(
                    citation, candidate.hit().sourceUri().toASCIIString(), candidate.hit().title(),
                    candidate.sentence(), candidate.score()));
            if (summary.length() > 0) {
                summary.append(' ');
            }
            summary.append(candidate.sentence()).append(" [").append(citation).append(']');
        }
        return new Synthesis(summary.toString(), evidence);
    }

    private static Set<String> terms(String query) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = WORD.matcher(query.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String value = matcher.group();
            if (value.length() > 1) {
                result.add(value);
            }
        }
        return result;
    }

    private static int overlap(String sentence, Set<String> queryTerms) {
        int count = 0;
        Matcher matcher = WORD.matcher(sentence.toLowerCase(Locale.ROOT));
        Set<String> matched = new HashSet<>();
        while (matcher.find()) {
            String term = matcher.group();
            if (queryTerms.contains(term) && matched.add(term)) {
                count++;
            }
        }
        return count;
    }

    public record EvidenceDocument(M3IndexedHit hit, String text) {
    }

    public record Evidence(int citation, String sourceUri, String title, String sentence, double score) {
    }

    public record Synthesis(String summary, List<Evidence> evidence) {
        public Synthesis {
            evidence = List.copyOf(evidence);
        }
    }

    private record Candidate(M3IndexedHit hit, String sentence, double score) {
    }
}
