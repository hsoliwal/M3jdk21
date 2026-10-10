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

    public record ValueScope(Coordinate coordinate, String valueFingerprint) {
        public ValueScope {
            coordinate = Objects.requireNonNull(coordinate, "coordinate");
            valueFingerprint = text(valueFingerprint, "valueFingerprint");
        }
    }

    private final Map<TranslationIdentity, M3LexiconPrecompute.TranslationProjection> translations;
    private final Map<SpellScope, M3LexiconPrecompute.SpellIndex> spellIndexes;
    private final Map<TokenRange, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes;
    private final Map<ValueToken, M3LexiconPrecompute.PrefixCounts> prefixCounts;
    /** Exact source identity for the canonical Synexia acronym family. */
    public record AcronymIdentity(String sourceId, String recordId,
                                  String sourceRevision, String sourceBlobSha,
                                  String snapshotSha256) {
        private static final String UNPINNED = "UNPINNED";

        public AcronymIdentity(String sourceId, String recordId) {
            this(sourceId, recordId, UNPINNED, UNPINNED, UNPINNED);
        }

        public AcronymIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            sourceRevision = provenance(sourceRevision, "sourceRevision", 40);
            sourceBlobSha = provenance(sourceBlobSha, "sourceBlobSha", 40);
            snapshotSha256 = provenance(snapshotSha256, "snapshotSha256", 64);
            if (!"dictlang.acronyms".equals(sourceId))
                throw new IllegalArgumentException("acronym identity has the wrong source family");
        }

        private static String provenance(String value, String name, int hexLength) {
            text(value, name);
            if (UNPINNED.equals(value)) return value;
            if (value.length() != hexLength
                    || !value.matches("[0-9a-f]{" + hexLength + "}")) {
                throw new IllegalArgumentException(name + " must be lowercase " + hexLength
                        + "-hex provenance");
            }
            return value;
        }
    }

    private final Map<ValueScope, M3LexiconPrecompute.TokenFrequency> tokenFrequencies;
    private final Map<AcronymIdentity, M3LexiconPrecompute.AcronymPrecompute> acronyms;

    private SharedLexiconPrecomputeCatalog(Builder builder) {
        translations = Map.copyOf(builder.translations);
        spellIndexes = Map.copyOf(builder.spellIndexes);
        tokenHashes = Map.copyOf(builder.tokenHashes);
        prefixCounts = Map.copyOf(builder.prefixCounts);
        tokenFrequencies = Map.copyOf(builder.tokenFrequencies);
        acronyms = Map.copyOf(builder.acronyms);
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

    public Optional<M3LexiconPrecompute.AcronymPrecompute> acronymAt(AcronymIdentity identity) {
        return Optional.ofNullable(acronyms.get(Objects.requireNonNull(identity, "identity")));
    }

    public static final class Builder {
        private final Map<TranslationIdentity, M3LexiconPrecompute.TranslationProjection> translations = new HashMap<>();
        private final Map<SpellScope, M3LexiconPrecompute.SpellIndex> spellIndexes = new HashMap<>();
        private final Map<TokenRange, M3LexiconPrecompute.TokenHashPrecompute> tokenHashes = new HashMap<>();
        private final Map<ValueToken, M3LexiconPrecompute.PrefixCounts> prefixCounts = new HashMap<>();
        private final Map<ValueScope, M3LexiconPrecompute.TokenFrequency> tokenFrequencies = new HashMap<>();
        private final Map<AcronymIdentity, M3LexiconPrecompute.AcronymPrecompute> acronyms = new HashMap<>();

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

        public Builder acronym(AcronymIdentity identity, M3LexiconPrecompute.AcronymPrecompute value) {
            Objects.requireNonNull(identity, "acronym key");
            Objects.requireNonNull(value, "acronym value");
            if (!identity.recordId().equals(value.acronym())
                    || "UNPINNED".equals(identity.sourceRevision())
                    || "UNPINNED".equals(identity.sourceBlobSha())
                    || "UNPINNED".equals(identity.snapshotSha256())
                    || !identity.sourceRevision().equals(value.sourceRevision())
                    || !identity.sourceBlobSha().equals(value.sourceBlobSha())
                    || !identity.snapshotSha256().equals(value.snapshotSha256())) {
                throw new IllegalArgumentException("acronym identity does not match source-bound precompute");
            }
            put(acronyms, identity, value, "acronym"); return this;
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
