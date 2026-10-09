/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, source-scoped catalog for LangDex precomputed projections.
 *
 * <p>The catalog stores typed rows supplied by an operator or exporter. It
 * deliberately does not perform network ingestion or dataset substitution.
 * Duplicate logical keys and mismatched coordinates fail closed.</p>
 */
public final class SharedLangDexPrecomputeCatalog {
    private final Map<M3LangDexPrecompute.Identity, M3LangDexPrecompute.Entry> entries;
    private final Map<M3LangDexPrecompute.Identity, M3LangDexPrecompute.WordProfile> wordProfiles;
    private final Map<M3LangDexPrecompute.Identity, M3LangDexPrecompute.LexicalProfile> lexicalProfiles;
    private final Map<M3LangDexPrecompute.TranslationIdentity,
            M3LangDexPrecompute.Translation> translations;

    private SharedLangDexPrecomputeCatalog(Builder builder) {
        entries = Map.copyOf(builder.entries);
        wordProfiles = Map.copyOf(builder.wordProfiles);
        lexicalProfiles = Map.copyOf(builder.lexicalProfiles);
        translations = Map.copyOf(builder.translations);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<M3LangDexPrecompute.Entry> entryAt(M3LangDexPrecompute.Identity identity) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(identity, "entry identity")));
    }

    public Optional<M3LangDexPrecompute.WordProfile> wordProfileAt(
            M3LangDexPrecompute.Identity identity) {
        return Optional.ofNullable(wordProfiles.get(
                Objects.requireNonNull(identity, "word profile identity")));
    }

    public Optional<M3LangDexPrecompute.LexicalProfile> lexicalProfileAt(
            M3LangDexPrecompute.Identity identity) {
        return Optional.ofNullable(lexicalProfiles.get(
                Objects.requireNonNull(identity, "lexical profile identity")));
    }

    public Optional<M3LangDexPrecompute.Translation> translationAt(
            M3LangDexPrecompute.TranslationIdentity identity) {
        return Optional.ofNullable(translations.get(
                Objects.requireNonNull(identity, "translation identity")));
    }

    public int entryCount() {
        return entries.size();
    }

    public int wordProfileCount() {
        return wordProfiles.size();
    }

    public int lexicalProfileCount() {
        return lexicalProfiles.size();
    }

    public int translationCount() {
        return translations.size();
    }

    public static final class Builder {
        private final Map<M3LangDexPrecompute.Identity, M3LangDexPrecompute.Entry> entries =
                new HashMap<>();
        private final Map<M3LangDexPrecompute.Identity, M3LangDexPrecompute.WordProfile> wordProfiles =
                new HashMap<>();
        private final Map<M3LangDexPrecompute.Identity, M3LangDexPrecompute.LexicalProfile>
                lexicalProfiles = new HashMap<>();
        private final Map<M3LangDexPrecompute.TranslationIdentity,
                M3LangDexPrecompute.Translation> translations = new HashMap<>();

        public Builder entry(M3LangDexPrecompute.Identity identity,
                             M3LangDexPrecompute.Entry value) {
            checkSource(identity);
            M3LangDexPrecompute.requireSameRecord(identity,
                    M3LangDexPrecompute.required(value, "entry"));
            put(entries, identity, value, "entry");
            return this;
        }

        public Builder wordProfile(M3LangDexPrecompute.Identity identity,
                                   M3LangDexPrecompute.WordProfile value) {
            checkSource(identity);
            put(wordProfiles, identity,
                    M3LangDexPrecompute.required(value, "word profile"),
                    "word profile");
            return this;
        }

        public Builder lexicalProfile(M3LangDexPrecompute.Identity identity,
                                      M3LangDexPrecompute.LexicalProfile value) {
            checkSource(identity);
            put(lexicalProfiles, identity,
                    M3LangDexPrecompute.required(value, "lexical profile"),
                    "lexical profile");
            return this;
        }

        public Builder translation(M3LangDexPrecompute.TranslationIdentity identity,
                                   M3LangDexPrecompute.Translation value) {
            checkSource(identity);
            M3LangDexPrecompute.requireSameTranslation(identity,
                    M3LangDexPrecompute.required(value, "translation"));
            put(translations, identity, value, "translation");
            return this;
        }

        public SharedLangDexPrecomputeCatalog build() {
            return new SharedLangDexPrecomputeCatalog(this);
        }

        private static void checkSource(M3LangDexPrecompute.Identity identity) {
            Objects.requireNonNull(identity, "LangDex identity");
            if (!M3LangDexPrecompute.acceptedSource(identity.sourceId())) {
                throw new IllegalArgumentException("unsupported LangDex source family");
            }
        }

        private static void checkSource(M3LangDexPrecompute.TranslationIdentity identity) {
            Objects.requireNonNull(identity, "LangDex translation identity");
            if (!M3LangDexPrecompute.acceptedSource(identity.sourceId())) {
                throw new IllegalArgumentException("unsupported LangDex source family");
            }
        }

        private static <K, V> void put(Map<K, V> values, K key, V value, String family) {
            if (values.putIfAbsent(Objects.requireNonNull(key, family + " key"),
                    Objects.requireNonNull(value, family + " value")) != null) {
                throw new IllegalArgumentException("duplicate " + family + " identity");
            }
        }
    }
}
