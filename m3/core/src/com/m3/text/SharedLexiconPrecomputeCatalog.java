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
    public record TranslationIdentity(String sourceId, String recordId,
                                      String sourceLanguage, String targetLanguage,
                                      String lexiconFingerprint, String sourceFingerprint) {
        public TranslationIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            sourceLanguage = text(sourceLanguage, "sourceLanguage");
            targetLanguage = text(targetLanguage, "targetLanguage");
            lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
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

    public record NumberIdentity(String sourceId, String recordId, int value, String languageTag) {
        public NumberIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            languageTag = text(languageTag, "languageTag");
            if (value < M3LexiconPrecompute.NumberPrecompute.MIN_VALUE
                    || value > M3LexiconPrecompute.NumberPrecompute.MAX_VALUE)
                throw new IllegalArgumentException("number value outside 0..10000");
        }
    }

    public record ValueScope(Coordinate coordinate, String valueFingerprint) {
        public ValueScope {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
        }
    }

    /** Exact source identity for the canonical Synexia SI-unit family. */
    public record SiUnitIdentity(String sourceId, String recordId) {
        public SiUnitIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            if (!"dictlang.si-units".equals(sourceId))
                throw new IllegalArgumentException("SI-unit identity has the wrong source family");
        }
    }

    private final Map<TranslationIdentity, M3LexiconPrecompute.TranslationProjection> translations;
    private final Map<SpellScope, M3LexiconPrecompute.SpellIndex> spellIndexes;
    private final Map<TokenRange, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes;
    private final Map<ValueToken, M3LexiconPrecompute.PrefixCounts> prefixCounts;
    private final Map<ValueScope, M3LexiconPrecompute.TokenFrequency> tokenFrequencies;
    private final Map<NumberIdentity, M3LexiconPrecompute.NumberPrecompute> numbers;
    private final Map<SiUnitIdentity, M3LexiconPrecompute.SiUnitPrecompute> siUnits;

    private SharedLexiconPrecomputeCatalog(Builder builder) {
        translations = Map.copyOf(builder.translations);
        spellIndexes = Map.copyOf(builder.spellIndexes);
        tokenHashes = Map.copyOf(builder.tokenHashes);
        prefixCounts = Map.copyOf(builder.prefixCounts);
        tokenFrequencies = Map.copyOf(builder.tokenFrequencies);
        numbers = Map.copyOf(builder.numbers);
        siUnits = Map.copyOf(builder.siUnits);
    }

    public static Builder builder() { return new Builder(); }

    public Optional<M3LexiconPrecompute.NumberPrecompute> numberAt(NumberIdentity identity) {
        return Optional.ofNullable(numbers.get(Objects.requireNonNull(identity, "identity")));
    }

    public Optional<M3LexiconPrecompute.SiUnitPrecompute> siUnitAt(SiUnitIdentity identity) {
        return Optional.ofNullable(siUnits.get(Objects.requireNonNull(identity, "identity")));
    }

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

    public static final class Builder {
        private final Map<TranslationIdentity, M3LexiconPrecompute.TranslationProjection> translations = new HashMap<>();
        private final Map<SpellScope, M3LexiconPrecompute.SpellIndex> spellIndexes = new HashMap<>();
        private final Map<TokenRange, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes = new HashMap<>();
        private final Map<ValueToken, M3LexiconPrecompute.PrefixCounts> prefixCounts = new HashMap<>();
        private final Map<ValueScope, M3LexiconPrecompute.TokenFrequency> tokenFrequencies = new HashMap<>();
        private final Map<NumberIdentity, M3LexiconPrecompute.NumberPrecompute> numbers = new HashMap<>();
        private final Map<SiUnitIdentity, M3LexiconPrecompute.SiUnitPrecompute> siUnits = new HashMap<>();

        public Builder number(NumberIdentity identity, M3LexiconPrecompute.NumberPrecompute value) {
            Objects.requireNonNull(identity, "number key");
            Objects.requireNonNull(value, "number value");
            if (identity.value() != value.value()
                    || !identity.languageTag().equals(value.languageTag()))
                throw new IllegalArgumentException("number identity does not match precompute");
            put(numbers, identity, value, "number");
            return this;
        }

        public Builder siUnit(SiUnitIdentity identity, M3LexiconPrecompute.SiUnitPrecompute value) {
            put(siUnits, identity, value, "SI unit");
            return this;
        }

        public Builder translation(TranslationIdentity identity, M3LexiconPrecompute.TranslationProjection value) {
            Objects.requireNonNull(identity, "translation key");
            Objects.requireNonNull(value, "translation value");
            if (!identity.sourceLanguage().equals(value.sourceLanguage())
                    || !identity.targetLanguage().equals(value.targetLanguage())
                    || !identity.lexiconFingerprint().equals(value.lexiconFingerprint())
                    || !identity.sourceFingerprint().equals(value.sourceFingerprint())) {
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
