/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Separate immutable catalog for the five index-shaped family sidecars.
 *
 * <p>The ordinary {@code SharedLexiconCatalog} remains the owner of mapped
 * text, source mappings, and fixed text facts. This catalog owns only typed
 * derived families and keys each one by its real logical scope. It is
 * intentionally constructed from already verified sidecar rows; file parsing,
 * schema hashes, and exporter/verifier admission remain a separate boundary.
 * In particular, it never treats an image row as a source identity and never
 * decodes the ordinary opaque per-record JSON payload.</p>
 */
public final class SharedLexiconPrecomputeCatalog {
    private static final String DICTIONARY_WORD_FACTS_SOURCE = "dictlang.dictionary";
    private static final String FREQUENCY_WORD_SIGNAL_SOURCE = "dictlang.frequency";
    public record TranslationIdentity(String sourceId, String recordId,
                                      String sourceLanguage, String targetLanguage,
                                      String lexiconFingerprint, String sourceFingerprint,
                                      String sourceRevision, String sourceBlobSha) {
        private static final String UNPINNED = "UNPINNED";

        public TranslationIdentity(String sourceId, String recordId,
                                   String sourceLanguage, String targetLanguage,
                                   String lexiconFingerprint, String sourceFingerprint) {
            this(sourceId, recordId, sourceLanguage, targetLanguage, lexiconFingerprint,
                    sourceFingerprint, UNPINNED, UNPINNED);
        }

        public TranslationIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            sourceLanguage = text(sourceLanguage, "sourceLanguage");
            targetLanguage = text(targetLanguage, "targetLanguage");
            lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
            sourceRevision = provenance(sourceRevision, "sourceRevision");
            sourceBlobSha = provenance(sourceBlobSha, "sourceBlobSha");
            if (UNPINNED.equals(sourceRevision) != UNPINNED.equals(sourceBlobSha))
                throw new IllegalArgumentException("source provenance must be both pinned or both UNPINNED");
        }

        private static String provenance(String value, String name) {
            text(value, name);
            if (UNPINNED.equals(value)) return value;
            if (!value.matches("[0-9a-f]{40}"))
                throw new IllegalArgumentException(name + " must be lowercase 40-hex provenance");
            return value;
        }
    }

    public record Coordinate(int shardId, int imageRow) {
        public Coordinate {
            if (shardId < 0 || imageRow < 0) throw new IllegalArgumentException("negative coordinate");
        }
    }

    public record SpellScope(String lexiconFingerprint, String language, int maxEditDistance,
                             int prefixLength, String sourceFingerprint) {
        public SpellScope {
            lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            language = text(language, "language");
            sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
            if (maxEditDistance < 0 || prefixLength < 0) throw new IllegalArgumentException("negative spell scope");
        }
    }

    public record TokenRange(Coordinate coordinate, String valueFingerprint, String tokenizerVersion,
                             int startInclusive, int endExclusive) {
        public TokenRange {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
            tokenizerVersion = text(tokenizerVersion, "tokenizerVersion");
            if (startInclusive < 0 || endExclusive < startInclusive)
                throw new IllegalArgumentException("invalid token range");
        }
    }

    public record ValueToken(Coordinate coordinate, String valueFingerprint, int tokenId) {
        public ValueToken {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
            if (tokenId < 0) throw new IllegalArgumentException("negative tokenId");
        }
    }

    public record ValueScope(Coordinate coordinate, String valueFingerprint) {
        public ValueScope {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
        }
    }


    /** Exact source record and lexeme identity for dictionary-derived metadata. */
    public record WordIdentity(String sourceId, String recordId, String lexeme,
                               String sourceRevision) {
        public WordIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            lexeme = text(lexeme, "lexeme");
            sourceRevision = text(sourceRevision, "sourceRevision");
        }
    }

    private final Map<TranslationIdentity, M3LexiconPrecompute.TranslationProjection> translations;
    private final Map<SpellScope, M3LexiconPrecompute.SpellIndex> spellIndexes;
    private final Map<TokenRange, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes;
    private final Map<ValueToken, M3LexiconPrecompute.PrefixCounts> prefixCounts;
    private final Map<ValueScope, M3LexiconPrecompute.TokenFrequency> tokenFrequencies;
    private final Map<WordIdentity, M3LexiconPrecompute.IndexWordFacts> wordFacts;
    private final Map<WordIdentity, M3LexiconPrecompute.IndexWordSignal> wordSignals;

    private SharedLexiconPrecomputeCatalog(Builder builder) {
        translations = Map.copyOf(builder.translations);
        spellIndexes = Map.copyOf(builder.spellIndexes);
        tokenHashes = Map.copyOf(builder.tokenHashes);
        prefixCounts = Map.copyOf(builder.prefixCounts);
        tokenFrequencies = Map.copyOf(builder.tokenFrequencies);
        wordFacts = Map.copyOf(builder.wordFacts);
        wordSignals = Map.copyOf(builder.wordSignals);
    }

    public static Builder builder() { return new Builder(); }

    public Optional<M3LexiconPrecompute.TranslationProjection> translationAt(TranslationIdentity identity) {
        return Optional.ofNullable(translations.get(Objects.requireNonNull(identity, "identity")));
    }
    public Optional<M3LexiconPrecompute.SpellIndex> spellAt(SpellScope scope) {
        return Optional.ofNullable(spellIndexes.get(Objects.requireNonNull(scope, "scope")));
    }
    public Optional<M3LexiconPrecompute.TokenHashPrecompute> tokenHashesAt(TokenRange range) {
        return Optional.ofNullable(tokenHashes.get(Objects.requireNonNull(range, "range")));
    }
    public Optional<M3LexiconPrecompute.PrefixCounts> prefixCountsAt(ValueToken key) {
        return Optional.ofNullable(prefixCounts.get(Objects.requireNonNull(key, "key")));
    }
    public Optional<M3LexiconPrecompute.TokenFrequency> tokenFrequencyAt(ValueScope scope) {
        return Optional.ofNullable(tokenFrequencies.get(Objects.requireNonNull(scope, "scope")));
    }


    public Optional<M3LexiconPrecompute.IndexWordFacts> wordFactsAt(WordIdentity identity) {
        return Optional.ofNullable(wordFacts.get(Objects.requireNonNull(identity, "word facts key")));
    }
    public Optional<M3LexiconPrecompute.IndexWordSignal> wordSignalAt(WordIdentity identity) {
        return Optional.ofNullable(wordSignals.get(Objects.requireNonNull(identity, "word signal key")));
    }

    public static final class Builder {
        private final Map<TranslationIdentity, M3LexiconPrecompute.TranslationProjection> translations = new HashMap<>();
        private final Map<SpellScope, M3LexiconPrecompute.SpellIndex> spellIndexes = new HashMap<>();
        private final Map<TokenRange, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes = new HashMap<>();
        private final Map<ValueToken, M3LexiconPrecompute.PrefixCounts> prefixCounts = new HashMap<>();
        private final Map<ValueScope, M3LexiconPrecompute.TokenFrequency> tokenFrequencies = new HashMap<>();
        private final Map<WordIdentity, M3LexiconPrecompute.IndexWordFacts> wordFacts = new HashMap<>();
        private final Map<WordIdentity, M3LexiconPrecompute.IndexWordSignal> wordSignals = new HashMap<>();

        public Builder translation(TranslationIdentity identity, M3LexiconPrecompute.TranslationProjection value) {
            Objects.requireNonNull(identity, "translation key");
            Objects.requireNonNull(value, "translation value");
            if (!identity.sourceLanguage().equals(value.sourceLanguage())
                    || !identity.targetLanguage().equals(value.targetLanguage())
                    || !identity.lexiconFingerprint().equals(value.lexiconFingerprint())
                    || !identity.sourceFingerprint().equals(value.sourceFingerprint())
                    || !identity.sourceRevision().equals(value.sourceRevision())
                    || !identity.sourceBlobSha().equals(value.sourceBlobSha())) {
                throw new IllegalArgumentException("translation identity does not match projection");
            }
            put(translations, identity, value, "translation"); return this;
        }
        public Builder spell(SpellScope scope, M3LexiconPrecompute.SpellIndex value) {
            put(spellIndexes, scope, value, "spell"); return this;
        }
        public Builder tokenHashes(TokenRange range, M3LexiconPrecompute.TokenHashPrecompute value) {
            put(tokenHashes, range, value, "token hashes"); return this;
        }
        public Builder prefixCounts(ValueToken key, M3LexiconPrecompute.PrefixCounts value) {
            put(prefixCounts, key, value, "prefix counts"); return this;
        }
        public Builder tokenFrequency(ValueScope scope, M3LexiconPrecompute.TokenFrequency value) {
            put(tokenFrequencies, scope, value, "token frequency"); return this;
        }

        public Builder wordFacts(WordIdentity identity, M3LexiconPrecompute.IndexWordFacts value) {
            Objects.requireNonNull(identity, "word facts key");
            if (!DICTIONARY_WORD_FACTS_SOURCE.equals(identity.sourceId())) {
                throw new IllegalArgumentException("word facts source family");
            }
            Objects.requireNonNull(value, "word facts value");
            if (!identity.sourceRevision().equals(value.sourceRevision())) {
                throw new IllegalArgumentException("word facts source revision");
            }
            put(wordFacts, identity, value, "word facts"); return this;
        }
        public Builder wordSignal(WordIdentity identity, M3LexiconPrecompute.IndexWordSignal value) {
            Objects.requireNonNull(identity, "word signal key");
            if (!FREQUENCY_WORD_SIGNAL_SOURCE.equals(identity.sourceId())) {
                throw new IllegalArgumentException("word signal source family");
            }
            Objects.requireNonNull(value, "word signal value");
            if (!identity.sourceRevision().equals(value.sourceRevision())) {
                throw new IllegalArgumentException("word signal source revision");
            }
            put(wordSignals, identity, value, "word signal"); return this;
        }
        public SharedLexiconPrecomputeCatalog build() { return new SharedLexiconPrecomputeCatalog(this); }

        private static <K, V> void put(Map<K, V> map, K key, V value, String family) {
            Objects.requireNonNull(key, family + " key");
            Objects.requireNonNull(value, family + " value");
            if (map.putIfAbsent(key, value) != null)
                throw new IllegalArgumentException("duplicate " + family + " scope");
        }
    }

    private static String text(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }
}
