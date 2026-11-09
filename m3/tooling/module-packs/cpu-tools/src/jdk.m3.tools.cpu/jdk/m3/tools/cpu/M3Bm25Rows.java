// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.tools.cpu;

import java.util.Objects;

/** Exact bounded BM25 over document CSR token lanes, returning detached scores and rank IDs.
 * Term IDs are 0-based integers; this does not tokenize, stem or provide Lucene syntax.
 */
public final class M3Bm25Rows {
    private M3Bm25Rows() { }
    public record Ranked(double[] score, int[] documents) {
        public Ranked { score = score.clone(); documents = documents.clone(); }
        @Override public double[] score() { return score.clone(); }
        @Override public int[] documents() { return documents.clone(); }
    }

    public static Ranked rank(int[] offsets, int[] terms, int vocabulary,
                              int[] query, double k1, double b) {
        Objects.requireNonNull(offsets); Objects.requireNonNull(terms); Objects.requireNonNull(query);
        int docs = offsets.length - 1;
        if (docs < 0 || docs > 10_000 || vocabulary < 1 || vocabulary > 65_536
                || offsets[0] != 0 || offsets[docs] != terms.length
                || query.length > vocabulary || !Double.isFinite(k1)
                || k1 <= 0 || k1 > 1_000 || !Double.isFinite(b) || b < 0 || b > 1)
            throw new IllegalArgumentException("ranking geometry");
        for (int at = 1; at < offsets.length; at++) if (offsets[at] < offsets[at - 1])
            throw new IllegalArgumentException("nonmonotonic document offsets");
        for (int term : terms) if (term < 0 || term >= vocabulary)
            throw new IllegalArgumentException("invalid term ID");
        long[] requested = new long[(vocabulary + 63) >>> 6];
        for (int term : query) {
            if (term < 0 || term >= vocabulary || (requested[term >>> 6] & (1L << (term & 63))) != 0)
                throw new IllegalArgumentException("duplicate or invalid query term");
            requested[term >>> 6] |= 1L << (term & 63);
        }
        int[] frequency = new int[vocabulary];
        int[] documentStamp = new int[vocabulary];
        int[] scoreStamp = new int[vocabulary];
        int[] postings = new int[vocabulary];
        for (int doc = 0; doc < docs; doc++) {
            int stamp = doc + 1;
            for (int pos = offsets[doc]; pos < offsets[doc + 1]; pos++) {
                int term = terms[pos];
                if (documentStamp[term] != stamp) {
                    documentStamp[term] = stamp;
                    postings[term]++;
                }
            }
        }
        double[] scores = new double[docs];
        double averageLength = docs == 0 ? 0 : (double) terms.length / docs;
        for (int doc = 0; doc < docs; doc++) {
            int stamp = doc + 1;
            for (int pos = offsets[doc]; pos < offsets[doc + 1]; pos++) {
                int term = terms[pos];
                if ((requested[term >>> 6] & (1L << (term & 63))) == 0) continue;
                if (scoreStamp[term] != stamp) { scoreStamp[term] = stamp; frequency[term] = 0; }
                frequency[term]++;
            }
            double length = offsets[doc + 1] - offsets[doc];
            double norm = 1.0 - b + b * (averageLength == 0 ? 0 : length / averageLength);
            double score = 0;
            for (int term : query) {
                int tf = frequency[term];
                if (tf == 0) continue;
                double idf = Math.log1p((docs - postings[term] + 0.5) / (postings[term] + 0.5));
                score += idf * (tf * (k1 + 1.0)) / (tf + k1 * norm);
                frequency[term] = 0;
            }
            scores[doc] = score;
        }
        int[] rank = new int[docs], scratch = new int[docs];
        for (int i = 0; i < docs; i++) rank[i] = i;
        for (int width = 1; width < docs; width = width > docs / 2 ? docs : width * 2) {
            for (int start = 0; start < docs;) {
                int middle = Math.min(docs, start + width);
                int end = Math.min(docs, middle + width);
                int left = start, right = middle, next = start;
                while (left < middle && right < end) {
                    int a = rank[left], c = rank[right];
                    boolean before = scores[a] > scores[c] || (scores[a] == scores[c] && a < c);
                    scratch[next++] = before ? rank[left++] : rank[right++];
                }
                while (left < middle) scratch[next++] = rank[left++];
                while (right < end) scratch[next++] = rank[right++];
                start = end;
            }
            int[] temp = rank; rank = scratch; scratch = temp;
            if (width == docs) break;
        }
        return new Ranked(scores, rank);
    }
}
