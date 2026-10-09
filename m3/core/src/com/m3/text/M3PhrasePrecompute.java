/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable token-ID phrase precompute derived from Synexia's IndexPhraseTable.
 *
 * <p>The receiver preserves vocabulary scope, source-token order, replacement
 * token order, and left-to-right longest-prefix semantics. It never stores
 * ordinary text and never assumes that two vocabulary spaces share token IDs.</p>
 */
public final class M3PhrasePrecompute {
    public static final String SCHEMA_VERSION = "m3phrase-v1";

    private M3PhrasePrecompute() {
    }

    /** Scope that makes a phrase table safe to reuse. */
    public record Scope(String sourceId, String recordId, String sourceRevision,
                        String vocabularyFingerprint) {
        public Scope {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            sourceRevision = text(sourceRevision, "sourceRevision");
            vocabularyFingerprint = text(vocabularyFingerprint, "vocabularyFingerprint");
        }
    }

    /** One immutable source-token sequence and its replacement sequence. */
    public static final class Phrase {
        private final int[] sourceTokenIds;
        private final int[] replacementTokenIds;
        private final Scope scope;

        /** Construct an unbound phrase; admission binds it to the builder's vocabulary. */
        public Phrase(int[] sourceTokenIds, int[] replacementTokenIds) {
            this(null, sourceTokenIds, replacementTokenIds, false);
        }

        /** Construct a phrase explicitly bound to a vocabulary scope. */
        public Phrase(Scope scope, int[] sourceTokenIds, int[] replacementTokenIds) {
            this(Objects.requireNonNull(scope, "scope"),
                    sourceTokenIds, replacementTokenIds, true);
        }

        private Phrase(Scope scope, int[] sourceTokenIds,
                       int[] replacementTokenIds, boolean ignored) {
            Objects.requireNonNull(sourceTokenIds, "sourceTokenIds");
            Objects.requireNonNull(replacementTokenIds, "replacementTokenIds");
            if (sourceTokenIds.length == 0)
                throw new IllegalArgumentException("source phrase must not be empty");
            this.sourceTokenIds = sourceTokenIds.clone();
            this.replacementTokenIds = replacementTokenIds.clone();
            this.scope = scope;
        }

        public int[] sourceTokenIds() {
            return sourceTokenIds.clone();
        }

        public int[] replacementTokenIds() {
            return replacementTokenIds.clone();
        }

        /** Synexia's target-token name; replacementTokenIds remains a compatibility alias. */
        public int[] targetTokenIds() {
            return replacementTokenIds();
        }

        private Phrase bindTo(Scope scope) {
            if (this.scope == null) {
                return new Phrase(scope, sourceTokenIds, replacementTokenIds, true);
            }
            if (!this.scope.equals(scope)) {
                throw new IllegalArgumentException(
                        "phrase belongs to a different vocabulary scope");
            }
            return this;
        }

        public int sourceLength() {
            return sourceTokenIds.length;
        }

        public int replacementLength() {
            return replacementTokenIds.length;
        }

        private int sourceTokenAt(int index) {
            return sourceTokenIds[index];
        }

        private void copyReplacementTo(int[] output, int offset) {
            System.arraycopy(replacementTokenIds, 0, output, offset, replacementTokenIds.length);
        }
    }

    /** A longest-prefix result with a defensive replacement view. */
    public static final class Match {
        private final int consumedLength;
        private final int[] replacementTokenIds;

        private Match(int consumedLength, int[] replacementTokenIds) {
            this.consumedLength = consumedLength;
            this.replacementTokenIds = replacementTokenIds.clone();
        }

        public int consumedLength() {
            return consumedLength;
        }

        public int[] replacementTokenIds() {
            return replacementTokenIds.clone();
        }

        /** Synexia's target-token name; replacementTokenIds remains a compatibility alias. */
        public int[] targetTokenIds() {
            return replacementTokenIds();
        }
    }

    /** Immutable phrase trie and allocation-free rewrite metadata. */
    public static final class Catalog {
        private final Scope scope;
        private final Node root;
        private final int phraseCount;
        private final int maxSourceLength;

        private Catalog(Scope scope, Node root, int phraseCount, int maxSourceLength) {
            this.scope = scope;
            this.root = root;
            this.phraseCount = phraseCount;
            this.maxSourceLength = maxSourceLength;
        }

        public Scope scope() {
            return scope;
        }

        public int phraseCount() {
            return phraseCount;
        }

        public int maxSourceLength() {
            return maxSourceLength;
        }

        public Optional<Match> longestMatchAt(int[] input, int start) {
            return longestMatchAt(scope, input, start);
        }

        /** Match input only when it belongs to the catalog's vocabulary space. */
        public Optional<Match> longestMatchAt(Scope inputScope, int[] input, int start) {
            requireVocabulary(inputScope);
            Objects.requireNonNull(input, "input");
            if (start < 0 || start > input.length)
                throw new IndexOutOfBoundsException("start: " + start);
            Candidate candidate = candidateAt(input, start);
            return candidate == null
                    ? Optional.empty()
                    : Optional.of(new Match(candidate.consumedLength,
                            candidate.phrase.replacementTokenIds));
        }

        /** Apply the source IndexPhraseTable rule in the catalog's vocabulary space. */
        public int[] rewrite(int[] input) {
            return rewrite(scope, input);
        }

        /** Apply the rewrite only when the input scope has the same vocabulary space. */
        public int[] rewrite(Scope inputScope, int[] input) {
            requireVocabulary(inputScope);
            Objects.requireNonNull(input, "input");
            int[] output = new int[Math.max(4, input.length)];
            int outputLength = 0;
            int start = 0;
            while (start < input.length) {
                Candidate candidate = candidateAt(input, start);
                if (candidate == null) {
                    if (outputLength == output.length)
                        output = Arrays.copyOf(output, output.length * 2);
                    output[outputLength++] = input[start++];
                    continue;
                }
                int required = outputLength + candidate.phrase.replacementLength();
                if (required > output.length) {
                    int next = output.length;
                    while (next < required)
                        next = Math.max(next * 2, 1);
                    output = Arrays.copyOf(output, next);
                }
                candidate.phrase.copyReplacementTo(output, outputLength);
                outputLength = required;
                start += candidate.consumedLength;
            }
            return Arrays.copyOf(output, outputLength);
        }

        private void requireVocabulary(Scope inputScope) {
            Objects.requireNonNull(inputScope, "inputScope");
            if (!scope.equals(inputScope)) {
                throw new IllegalArgumentException(
                        "input belongs to a different vocabulary scope");
            }
        }

        private Candidate candidateAt(int[] input, int start) {
            Node node = root;
            Phrase last = null;
            int lastLength = 0;
            for (int cursor = start; cursor < input.length; cursor++) {
                node = node.children.get(input[cursor]);
                if (node == null)
                    break;
                if (node.phrase != null) {
                    last = node.phrase;
                    lastLength = cursor - start + 1;
                }
            }
            return last == null ? null : new Candidate(lastLength, last);
        }
    }

    public static Builder builder(Scope scope) {
        return new Builder(scope);
    }

    public static final class Builder {
        private final Scope scope;
        private final Node root = new Node();
        private int phraseCount;
        private int maxSourceLength;

        private Builder(Scope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
        }

        public Builder put(Phrase phrase) {
            Phrase bound = Objects.requireNonNull(phrase, "phrase").bindTo(scope);
            Node node = root;
            for (int index = 0; index < bound.sourceLength(); index++)
                node = node.children.computeIfAbsent(bound.sourceTokenAt(index),
                        ignored -> new Node());
            // Synexia IndexPhraseTable is last-write-wins for an existing source phrase.
            if (node.phrase == null) {
                phraseCount++;
                maxSourceLength = Math.max(maxSourceLength, bound.sourceLength());
            }
            node.phrase = bound;
            return this;
        }

        public Catalog build() {
            // An empty Synexia IndexPhraseTable is a valid identity rewrite.
            return new Catalog(scope, Node.freeze(root), phraseCount, maxSourceLength);
        }
    }

    private static final class Node {
        private final Map<Integer, Node> children;
        private Phrase phrase;

        private Node() {
            children = new HashMap<>();
        }

        private Node(Map<Integer, Node> children, Phrase phrase) {
            this.children = children;
            this.phrase = phrase;
        }

        private static Node freeze(Node source) {
            Map<Integer, Node> children = new HashMap<>();
            source.children.forEach((token, child) ->
                    children.put(token, freeze(child)));
            return new Node(Map.copyOf(children), source.phrase);
        }
    }

    private record Candidate(int consumedLength, Phrase phrase) {
    }

    private static String text(String value, String name) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException(name + " is empty");
        return value;
    }
}