/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Locale;
import java.util.Objects;

/**
 * Typed, immutable M3JDK receiver for the Synexia LangDex projection.
 *
 * <p>This class owns contracts and precomputed fields only. It does not
 * download, embed, or silently replace a LangDex/Hugging Face dataset. The
 * caller supplies an explicitly identified, licensed snapshot at the catalog
 * boundary.</p>
 */
public final class M3LangDexPrecompute {
    private M3LangDexPrecompute() { }

    /** Stable source record identity; source revision is part of every key. */
    public record Identity(String sourceId, String recordId, String glottocode,
                           String surface, String sourceRevision) {
        public Identity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            glottocode = glottocode(glottocode);
            surface = text(surface, "surface");
            sourceRevision = text(sourceRevision, "sourceRevision");
        }
    }

    /** Immutable LangDex word/sense projection entry. */
    public record Entry(String glottocode, String surface, long conceptId,
                        long frequency, int flags, String lemma) {
        public Entry {
            glottocode = glottocode(glottocode);
            surface = text(surface, "surface");
            lemma = text(lemma, "lemma");
            if (conceptId <= 0) throw new IllegalArgumentException("conceptId must be positive");
            if (frequency < 0) throw new IllegalArgumentException("negative frequency");
            if (flags < 0 || flags > 0xffff)
                throw new IllegalArgumentException("flags outside unsigned 16-bit range");
        }
    }

    /** Immutable six-field Synexia word profile with its donor bounds intact. */
    public record WordProfile(long lexicalClassMask, long semanticClassMask,
                              int subjectId, int featureBits, int evidenceMask,
                              int confidencePermille) {
        public WordProfile {
            if (subjectId < 0) throw new IllegalArgumentException("negative subjectId");
            if (confidencePermille < 0 || confidencePermille > 1000)
                throw new IllegalArgumentException("confidence outside 0..1000");
        }

        public static WordProfile unclassified() {
            return new WordProfile(0L, 0L, 0, 0, 0, 0);
        }
    }

    /** Profile-level lexical signals shared by compatible LangDex records. */
    public record LexicalProfile(long lexicalClassMask, int featureBits,
                                 int evidenceMask, int confidencePermille) {
        public LexicalProfile {
            if (confidencePermille < 0 || confidencePermille > 1000)
                throw new IllegalArgumentException("confidence outside 0..1000");
        }
    }

    /** Stable identity for a source-to-target LangDex translation row. */
    public record TranslationIdentity(String sourceId, String recordId,
                                      String sourceGlottocode, String sourceSurface,
                                      String targetGlottocode, String targetSurface,
                                      String sourceRevision) {
        public TranslationIdentity {
            sourceId = text(sourceId, "sourceId");
            recordId = text(recordId, "recordId");
            sourceGlottocode = glottocode(sourceGlottocode);
            sourceSurface = text(sourceSurface, "sourceSurface");
            targetGlottocode = glottocode(targetGlottocode);
            targetSurface = text(targetSurface, "targetSurface");
            sourceRevision = text(sourceRevision, "sourceRevision");
        }
    }

    /** Immutable exact translation coordinate; target ID is never renumbered. */
    public record Translation(long conceptId, String sourceGlottocode,
                              String sourceSurface, String targetGlottocode,
                              String targetSurface, long targetLexemeId) {
        public Translation {
            if (conceptId <= 0) throw new IllegalArgumentException("conceptId must be positive");
            sourceGlottocode = glottocode(sourceGlottocode);
            sourceSurface = text(sourceSurface, "sourceSurface");
            targetGlottocode = glottocode(targetGlottocode);
            targetSurface = text(targetSurface, "targetSurface");
            if (targetLexemeId < 0)
                throw new IllegalArgumentException("negative targetLexemeId");
        }
    }

    static boolean acceptedSource(String sourceId) {
        return "unicodex.langdex.lexemes".equals(sourceId)
                || "dictlang.huggingface".equals(sourceId);
    }

    static String text(String value, String name) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException(name + " is empty");
        return value;
    }

    static String glottocode(String value) {
        String normalized = text(value, "glottocode").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("glottocode is empty");
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isWhitespace(normalized.charAt(i)))
                throw new IllegalArgumentException("glottocode contains whitespace");
        }
        return normalized;
    }

    static void requireSame(String expected, String actual, String field) {
        if (!expected.equals(actual))
            throw new IllegalArgumentException("LangDex " + field + " identity mismatch");
    }

    static void requireSameGlottocode(String expected, String actual, String field) {
        requireSame(glottocode(expected), glottocode(actual), field);
    }

    static void requireSameText(String expected, String actual, String field) {
        requireSame(text(expected, field), text(actual, field), field);
    }

    static void requireSameRecord(Identity key, Entry value) {
        requireSameGlottocode(key.glottocode(), value.glottocode(), "glottocode");
        requireSameText(key.surface(), value.surface(), "surface");
    }

    static void requireSameTranslation(TranslationIdentity key, Translation value) {
        requireSameGlottocode(key.sourceGlottocode(), value.sourceGlottocode(), "sourceGlottocode");
        requireSameText(key.sourceSurface(), value.sourceSurface(), "sourceSurface");
        requireSameGlottocode(key.targetGlottocode(), value.targetGlottocode(), "targetGlottocode");
        requireSameText(key.targetSurface(), value.targetSurface(), "targetSurface");
    }

    static <T> T required(T value, String name) {
        return Objects.requireNonNull(value, name);
    }
}
