// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.regex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
/** Uses the canonical JDK Pattern/Matcher contract, including zero-length matches. */
public final class RegexMatcher {
    private RegexMatcher() { }
    public record Hit(int start, int end, String group) { }
    public static List<Hit> find(String regex, CharSequence input, int maxMatches) {
        Objects.requireNonNull(regex); Objects.requireNonNull(input);
        if (maxMatches < 0) throw new IllegalArgumentException("negative match budget");
        return prepare(regex).find(input, maxMatches);
    }

    /** Compiles once through the authoritative JDK/M3Regex Pattern implementation. */
    public static Prepared prepare(String regex) { return new Prepared(regex); }

    /** The match's original file ordinal and half-open, file-local UTF-16 coordinates. */
    public record CorpusHit(int documentId, int start, int end) { }

    /** Primitive result lanes: no retained per-hit objects or copied regex group text. */
    public static final class CorpusResult {
        private final int[] documents;
        private final int[] starts;
        private final int[] ends;
        private final int scannedDocuments;
        private final boolean complete;

        private CorpusResult(int[] documents, int[] starts, int[] ends, int size,
                             int scannedDocuments, boolean complete) {
            this.documents = Arrays.copyOf(documents, size);
            this.starts = Arrays.copyOf(starts, size);
            this.ends = Arrays.copyOf(ends, size);
            this.scannedDocuments = scannedDocuments;
            this.complete = complete;
        }
        public int size() { return documents.length; }
        public boolean complete() { return complete; }
        public int scannedDocuments() { return scannedDocuments; }
        public CorpusHit hit(int index) { return new CorpusHit(documents[index], starts[index], ends[index]); }
        public List<CorpusHit> hits() {
            List<CorpusHit> result = new ArrayList<>(size());
            for (int i = 0; i < size(); i++) result.add(hit(i));
            return List.copyOf(result);
        }
    }

    /** Immutable regex plan; each file gets a fresh Matcher to preserve anchor semantics. */
    public static final class Prepared {
        public static final int MAX_REGEX_UTF16 = 4096;
        public static final int MAX_DOCUMENTS = 4096;
        public static final int MAX_DOCUMENT_UTF16 = 16 * 1024 * 1024;
        public static final long MAX_CORPUS_UTF16 = 64L * 1024 * 1024;
        public static final int MAX_MATCHES = 1_000_000;

        private final String expression;
        private final Pattern pattern;

        private Prepared(String regex) {
            this.expression = Objects.requireNonNull(regex, "regex");
            this.pattern = Pattern.compile(regex);
        }

        public String expression() { return expression; }

        /** Legacy one-input behavior, including captured full-match group strings. */
        public List<Hit> find(CharSequence input, int maxMatches) {
            Objects.requireNonNull(input, "input");
            if (maxMatches < 0) throw new IllegalArgumentException("negative match budget");
            var matcher = pattern.matcher(input);
            List<Hit> hits = new ArrayList<>();
            while (hits.size() < maxMatches && matcher.find())
                hits.add(new Hit(matcher.start(), matcher.end(), matcher.group()));
            return List.copyOf(hits);
        }

        public CorpusResult findCorpus(List<String> corpus, int maxMatches) {
            return findCorpus(corpus, maxMatches, () -> { });
        }

        /**
         * Search ordered, immutable String owners without joining files or compiling
         * the regex again. One extra match proves whether a capped prefix is complete.
         * The match cap does NOT bound worst-case Pattern/Matcher execution time;
         * cancellation is only cooperative outside each Matcher.find() call.
         */
        public CorpusResult findCorpus(List<String> corpus, int maxMatches, Runnable checkpoint) {
            Objects.requireNonNull(corpus, "corpus");
            Objects.requireNonNull(checkpoint, "checkpoint");
            if (maxMatches < 0 || maxMatches > MAX_MATCHES) throw new IllegalArgumentException("match budget");
            if (expression.length() > MAX_REGEX_UTF16) throw new IllegalArgumentException("regex UTF-16 budget");
            if (corpus.size() > MAX_DOCUMENTS) throw new IllegalArgumentException("corpus document count budget");
            List<String> sources = List.copyOf(corpus);
            long units = 0;
            for (String source : sources) {
                checkpoint.run();
                if (source.length() > MAX_DOCUMENT_UTF16) throw new IllegalArgumentException("source UTF-16 budget");
                units += source.length();
                if (units > MAX_CORPUS_UTF16) throw new IllegalArgumentException("corpus UTF-16 budget");
            }
            int[] documents = new int[Math.min(maxMatches, 16)];
            int[] starts = new int[documents.length];
            int[] ends = new int[documents.length];
            int count = 0;
            for (int document = 0; document < sources.size(); document++) {
                checkpoint.run();
                var matcher = pattern.matcher(sources.get(document));
                while (matcher.find()) {
                    if ((count & 4095) == 0) checkpoint.run();
                    if (count == maxMatches) {
                        return new CorpusResult(documents, starts, ends, count, document + 1, false);
                    }
                    if (count == documents.length) {
                        int increased = Math.min(maxMatches, Math.max(16, count * 2));
                        documents = Arrays.copyOf(documents, increased);
                        starts = Arrays.copyOf(starts, increased);
                        ends = Arrays.copyOf(ends, increased);
                    }
                    documents[count] = document;
                    starts[count] = matcher.start();
                    ends[count] = matcher.end();
                    count++;
                }
            }
            checkpoint.run();
            return new CorpusResult(documents, starts, ends, count, sources.size(), true);
        }
    }
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--self-test")) {
            if (find("a+", "baaa ac", 3).size() != 2) throw new AssertionError("regex count");
            if (find("", "ab", 9).size() != 3) throw new AssertionError("empty pattern");
            if (!find("a", "aba", 0).isEmpty()) throw new AssertionError("bounded");
            System.out.println("M3_JMOD_TEST_PASS text.regex");
        } else if (args.length >= 2) {
            find(args[0], args[1], 1000).forEach(h -> System.out.println(h.start()+"\t"+h.end()+"\t"+h.group()));
        } else { throw new IllegalArgumentException("regex and text arguments required"); }
    }
}
