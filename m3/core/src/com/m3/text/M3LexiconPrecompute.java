/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable target-side owners for Synexia lexicon precompute.
 *
 * <p>This is a source-bound M3JDK candidate, not a {@code java.lang.String}
 * field and not a second canonical text interner. Every owner keeps the
 * identity/scope that makes its derived values safe to reuse. Array and map
 * boundaries defensively copy, so a JNI or exporter buffer cannot mutate a
 * published projection.</p>
 */
public final class M3LexiconPrecompute {
    private M3LexiconPrecompute() { }

    /** Immutable SI-unit metadata decoded from the canonical Synexia payload. */
    public record SiUnitPrecompute(int decimalExponent, long dimensionPacked,
                                   double offset, boolean prefixable) {
        public SiUnitPrecompute {
            if (decimalExponent < -100 || decimalExponent > 100)
                throw new IllegalArgumentException("SI decimal exponent outside -100..100");
            if (!Double.isFinite(offset))
                throw new IllegalArgumentException("SI offset is not finite");
        }
    }

    public static final class TranslationProjection {
        private static final String UNPINNED = "UNPINNED";
        private final String lexiconFingerprint;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final String sourceFingerprint;
        private final int[] translatedTokenIds;
        private final int mappedTokenCount;
        private final String sourceRevision;
        private final String sourceBlobSha;

        public TranslationProjection(String lexiconFingerprint, String sourceLanguage,
                                     String targetLanguage, String sourceFingerprint,
                                     int[] translatedTokenIds) {
            this(lexiconFingerprint, sourceLanguage, targetLanguage, sourceFingerprint,
                    translatedTokenIds, translatedTokenIds == null ? 0 : translatedTokenIds.length,
                    UNPINNED, UNPINNED);
        }

        public TranslationProjection(String lexiconFingerprint, String sourceLanguage,
                                     String targetLanguage, String sourceFingerprint,
                                     int[] translatedTokenIds, int mappedTokenCount) {
            this(lexiconFingerprint, sourceLanguage, targetLanguage, sourceFingerprint,
                    translatedTokenIds, mappedTokenCount, UNPINNED, UNPINNED);
        }

        public TranslationProjection(String lexiconFingerprint, String sourceLanguage,
                                     String targetLanguage, String sourceFingerprint,
                                     int[] translatedTokenIds, int mappedTokenCount,
                                     String sourceRevision, String sourceBlobSha) {
            this.lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            this.sourceLanguage = text(sourceLanguage, "sourceLanguage");
            this.targetLanguage = text(targetLanguage, "targetLanguage");
            this.sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
            this.sourceRevision = provenance(sourceRevision, "sourceRevision");
            this.sourceBlobSha = provenance(sourceBlobSha, "sourceBlobSha");
            if (UNPINNED.equals(this.sourceRevision) != UNPINNED.equals(this.sourceBlobSha))
                throw new IllegalArgumentException("source provenance must be both pinned or both UNPINNED");
            Objects.requireNonNull(translatedTokenIds, "translatedTokenIds");
            this.translatedTokenIds = translatedTokenIds.clone();
            for (int tokenId : this.translatedTokenIds) nonNegative(tokenId, "translated token id");
            if (mappedTokenCount < 0 || mappedTokenCount > this.translatedTokenIds.length)
                throw new IllegalArgumentException("mappedTokenCount outside projection");
            this.mappedTokenCount = mappedTokenCount;
        }

        public String lexiconFingerprint() { return lexiconFingerprint; }
        public String sourceLanguage() { return sourceLanguage; }
        public String targetLanguage() { return targetLanguage; }
        public String sourceFingerprint() { return sourceFingerprint; }
        public String sourceRevision() { return sourceRevision; }
        public String sourceBlobSha() { return sourceBlobSha; }
        public boolean sourceBound() {
            return !UNPINNED.equals(sourceRevision) && !UNPINNED.equals(sourceBlobSha);
        }
        public int mappedTokenCount() { return mappedTokenCount; }
        public int translatedTokenIdAt(int index) {
            return translatedTokenIds[Objects.checkIndex(index, translatedTokenIds.length)];
        }
        public int[] translatedTokenIds() { return translatedTokenIds.clone(); }

        public boolean appliesTo(String lexicon, String source, String target, String fingerprint) {
            return lexiconFingerprint.equals(lexicon) && sourceLanguage.equals(source)
                    && targetLanguage.equals(target) && sourceFingerprint.equals(fingerprint);
        }

        public boolean appliesTo(String lexicon, String source, String target, String fingerprint,
                                 String revision, String blobSha) {
            return appliesTo(lexicon, source, target, fingerprint)
                    && sourceRevision.equals(revision) && sourceBlobSha.equals(blobSha);
        }

        private static String provenance(String value, String name) {
            text(value, name);
            if (UNPINNED.equals(value)) return value;
            if (!value.matches("[0-9a-f]{40}"))
                throw new IllegalArgumentException(name + " must be lowercase 40-hex provenance");
            return value;
        }
    }

    public static final class SpellIndex {
        private final String lexiconFingerprint;
        private final String language;
        private final int maxEditDistance;
        private final int prefixLength;
        private final String sourceFingerprint;
        private final NavigableMap<String, int[]> deleteToTokenIds;
        private final NavigableMap<Integer, Long> frequencies;

        public SpellIndex(String lexiconFingerprint, String language, int maxEditDistance,
                          int prefixLength, String sourceFingerprint,
                          Map<String, int[]> deleteToTokenIds,
                          Map<Integer, Long> frequencies) {
            this.lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            this.language = text(language, "language");
            this.maxEditDistance = nonNegative(maxEditDistance, "maxEditDistance");
            this.prefixLength = nonNegative(prefixLength, "prefixLength");
            this.sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
            Objects.requireNonNull(deleteToTokenIds, "deleteToTokenIds");
            Objects.requireNonNull(frequencies, "frequencies");
            TreeMap<String, int[]> deletes = new TreeMap<>();
            deleteToTokenIds.forEach((key, ids) -> {
                String canonical = Objects.requireNonNull(key, "delete key");
                Objects.requireNonNull(ids, "delete token ids");
                int[] copy = ids.clone();
                int previous = -1;
                for (int id : copy) {
                    nonNegative(id, "delete token id");
                    if (id <= previous) throw new IllegalArgumentException("delete token ids not strictly sorted");
                    previous = id;
                }
                if (deletes.put(canonical, copy) != null)
                    throw new IllegalArgumentException("duplicate delete key");
            });
            TreeMap<Integer, Long> counts = new TreeMap<>();
            frequencies.forEach((id, count) -> {
                Objects.requireNonNull(id, "frequency token id");
                Objects.requireNonNull(count, "frequency value");
                nonNegative(id, "frequency token id");
                if (count < 0) throw new IllegalArgumentException("negative frequency");
                if (counts.put(id, count) != null) throw new IllegalArgumentException("duplicate frequency key");
            });
            this.deleteToTokenIds = immutableArrays(deletes);
            this.frequencies = Collections.unmodifiableNavigableMap(counts);
        }

        public String lexiconFingerprint() { return lexiconFingerprint; }
        public String language() { return language; }
        public int maxEditDistance() { return maxEditDistance; }
        public int prefixLength() { return prefixLength; }
        public String sourceFingerprint() { return sourceFingerprint; }
        public int[] tokenIdsForDelete(String deleteKey) {
            int[] ids = deleteToTokenIds.get(Objects.requireNonNull(deleteKey, "deleteKey"));
            return ids == null ? new int[0] : ids.clone();
        }
        public NavigableMap<String, int[]> deleteToTokenIds() { return immutableArraysCopy(deleteToTokenIds); }
        public long frequencyAt(int tokenId) {
            nonNegative(tokenId, "tokenId");
            return frequencies.getOrDefault(tokenId, 0L);
        }
        public NavigableMap<Integer, Long> frequencies() { return frequencies; }

        private static NavigableMap<String, int[]> immutableArrays(Map<String, int[]> values) {
            TreeMap<String, int[]> copy = new TreeMap<>();
            values.forEach((key, value) -> copy.put(key, value.clone()));
            return Collections.unmodifiableNavigableMap(copy);
        }

        private static NavigableMap<String, int[]> immutableArraysCopy(Map<String, int[]> values) {
            return immutableArrays(values);
        }
    }


    /** Immutable dictionary-level facts keyed by one source record and lexeme. */
    public static final class IndexWordFacts {
        private final String sourceRevision;
        private final int languageId;
        private final int wordCount;
        private final long lexiconFingerprint;
        private final long totalCorpusTokens;
        private final long totalDocuments;
        private final int flags;
        private final long corpusCount;
        private final int documentFrequency;
        private final int[] memberships;
        private final long[] conceptIds;
        private final long[] subjects;
        private final long[] expansionWordIds;

        public IndexWordFacts(String sourceRevision, int languageId, int wordCount,
                              long lexiconFingerprint, long totalCorpusTokens,
                              long totalDocuments, int flags,
                              long corpusCount, int documentFrequency, int[] memberships,
                              long[] conceptIds, long[] subjects, long[] expansionWordIds) {
            this.sourceRevision = text(sourceRevision, "sourceRevision");
            if (languageId < 0 || wordCount < 0 || totalCorpusTokens < 0
                    || totalDocuments < 0 || corpusCount < 0 || documentFrequency < 0)
                throw new IllegalArgumentException("negative dictionary fact");
            this.languageId = languageId;
            this.wordCount = wordCount;
            this.lexiconFingerprint = lexiconFingerprint;
            this.totalCorpusTokens = totalCorpusTokens;
            this.totalDocuments = totalDocuments;
            this.flags = flags;
            this.corpusCount = corpusCount;
            this.documentFrequency = documentFrequency;
            this.memberships = copy(memberships, "memberships");
            this.conceptIds = copy(conceptIds, "conceptIds");
            this.subjects = copy(subjects, "subjects");
            this.expansionWordIds = copy(expansionWordIds, "expansionWordIds");
        }

        public String sourceRevision() { return sourceRevision; }
        public int languageId() { return languageId; }
        public int wordCount() { return wordCount; }
        public long lexiconFingerprint() { return lexiconFingerprint; }
        public long totalCorpusTokens() { return totalCorpusTokens; }
        public long totalDocuments() { return totalDocuments; }
        public int flags() { return flags; }
        public long corpusCount() { return corpusCount; }
        public int documentFrequency() { return documentFrequency; }

        public int[] memberships() { return memberships.clone(); }
        public int membershipCount() { return memberships.length; }
        public int membershipAt(int index) {
            return memberships[Objects.checkIndex(index, memberships.length)];
        }

        public long[] conceptIds() { return conceptIds.clone(); }
        public int conceptCount() { return conceptIds.length; }
        public long conceptAt(int index) {
            return conceptIds[Objects.checkIndex(index, conceptIds.length)];
        }

        public long[] subjects() { return subjects.clone(); }
        public int subjectCount() { return subjects.length; }
        public long subjectAt(int index) {
            return subjects[Objects.checkIndex(index, subjects.length)];
        }

        public long[] expansionWordIds() { return expansionWordIds.clone(); }
        public int expansionSize() { return expansionWordIds.length; }
        public long expansionWordIdAt(int index) {
            return expansionWordIds[Objects.checkIndex(index, expansionWordIds.length)];
        }

        public boolean appliesTo(int language, long fingerprint) {
            return languageId == language && lexiconFingerprint == fingerprint;
        }

        private static int[] copy(int[] value, String name) {
            return Objects.requireNonNull(value, name).clone();
        }

        private static long[] copy(long[] value, String name) {
            return Objects.requireNonNull(value, name).clone();
        }
    }

    /** Immutable dictionary signal profile, including frequency and lexical ranks. */
    public static final class IndexWordSignal {
        private final String sourceRevision;
        private final int utf16Length;
        private final int codePointLength;
        private final int firstCodePoint;
        private final int lastCodePoint;
        private final int flags;
        private final int frequencyRank;
        private final long lemmaId;
        private final int lexicalRank;
        private final int morphologyMask;
        private final long phoneticId;
        private final int posMask;
        private final long presence64;
        private final int scriptOrdinal;
        private final long simHash64;
        private final long stemId;

        public IndexWordSignal(String sourceRevision, int utf16Length, int codePointLength,
                               int firstCodePoint, int lastCodePoint, int flags,
                               int frequencyRank, long lemmaId,
                               int lexicalRank, int morphologyMask, long phoneticId,
                               int posMask, long presence64, int scriptOrdinal,
                               long simHash64, long stemId) {
            this.sourceRevision = text(sourceRevision, "sourceRevision");
            if (utf16Length < 0 || codePointLength < 0 || codePointLength > utf16Length
                    || frequencyRank < 0 || lexicalRank < 0 || scriptOrdinal < 0)
                throw new IllegalArgumentException("invalid dictionary signal length or rank");
            if (!validBoundary(firstCodePoint) || !validBoundary(lastCodePoint)
                    || (codePointLength == 0
                        ? firstCodePoint != -1 || lastCodePoint != -1
                        : firstCodePoint < 0 || lastCodePoint < 0))
                throw new IllegalArgumentException("invalid dictionary signal boundary");
            if (lemmaId < -1 || phoneticId < -1 || stemId < -1)
                throw new IllegalArgumentException("invalid optional dictionary signal id");
            this.utf16Length = utf16Length;
            this.codePointLength = codePointLength;
            this.firstCodePoint = firstCodePoint;
            this.lastCodePoint = lastCodePoint;
            this.flags = flags;
            this.frequencyRank = frequencyRank;
            this.lemmaId = lemmaId;
            this.lexicalRank = lexicalRank;
            this.morphologyMask = morphologyMask;
            this.phoneticId = phoneticId;
            this.posMask = posMask;
            this.presence64 = presence64;
            this.scriptOrdinal = scriptOrdinal;
            this.simHash64 = simHash64;
            this.stemId = stemId;
        }

        public String sourceRevision() { return sourceRevision; }
        public int utf16Length() { return utf16Length; }
        public int codePointLength() { return codePointLength; }
        public int firstCodePoint() { return firstCodePoint; }
        public int lastCodePoint() { return lastCodePoint; }
        public int flags() { return flags; }
        public int frequencyRank() { return frequencyRank; }
        public long lemmaId() { return lemmaId; }
        public int lexicalRank() { return lexicalRank; }
        public int morphologyMask() { return morphologyMask; }
        public long phoneticId() { return phoneticId; }
        public int posMask() { return posMask; }
        public long presence64() { return presence64; }
        public int scriptOrdinal() { return scriptOrdinal; }
        public long simHash64() { return simHash64; }
        public long stemId() { return stemId; }

        private static boolean validBoundary(int codePoint) {
            return codePoint == -1 || Character.isValidCodePoint(codePoint);
        }
    }

    public record RangeFingerprint(long first, long second, int length) {
        public RangeFingerprint {
            if (length < 0) throw new IllegalArgumentException("negative range length");
        }
    }

    public static final class TokenHashPrecompute {
        private static final int DIGEST_BYTES = 32;
        private final String valueFingerprint;
        private final int rangeStart;
        private final int rangeEnd;
        private final byte[][] tokenSha256;
        private final RangeFingerprint rangeFingerprint;
        private final byte[] rangeSha256;

        public TokenHashPrecompute(String valueFingerprint, int rangeStart, int rangeEnd,
                                   byte[][] tokenSha256, RangeFingerprint rangeFingerprint,
                                   byte[] rangeSha256) {
            this.valueFingerprint = text(valueFingerprint, "valueFingerprint");
            if (rangeStart < 0 || rangeEnd < rangeStart)
                throw new IllegalArgumentException("invalid token range");
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            Objects.requireNonNull(tokenSha256, "tokenSha256");
            if (tokenSha256.length != rangeEnd - rangeStart)
                throw new IllegalArgumentException("token digest count does not match range");
            this.tokenSha256 = digestMatrix(tokenSha256);
            this.rangeFingerprint = Objects.requireNonNull(rangeFingerprint, "rangeFingerprint");
            if (rangeFingerprint.length() != rangeEnd - rangeStart)
                throw new IllegalArgumentException("range fingerprint length mismatch");
            this.rangeSha256 = digest(rangeSha256, "rangeSha256");
        }

        public String valueFingerprint() { return valueFingerprint; }
        public int rangeStart() { return rangeStart; }
        public int rangeEnd() { return rangeEnd; }
        public byte[][] tokenSha256() { return digestMatrix(tokenSha256); }
        public RangeFingerprint rangeFingerprint() { return rangeFingerprint; }
        public byte[] rangeSha256() { return rangeSha256.clone(); }
        public boolean appliesTo(String fingerprint, int start, int end, RangeFingerprint range) {
            return valueFingerprint.equals(fingerprint) && rangeStart == start && rangeEnd == end
                    && rangeFingerprint.equals(range);
        }

        private static byte[][] digestMatrix(byte[][] values) {
            byte[][] copy = new byte[values.length][];
            for (int i = 0; i < values.length; i++) copy[i] = digest(values[i], "tokenSha256");
            return copy;
        }

        private static byte[] digest(byte[] value, String name) {
            Objects.requireNonNull(value, name);
            if (value.length != DIGEST_BYTES) throw new IllegalArgumentException(name + " must be SHA-256");
            return value.clone();
        }
    }

    public static final class PrefixCounts {
        private final String valueFingerprint;
        private final int tokenId;
        private final long[] prefixCounts;

        public PrefixCounts(String valueFingerprint, int tokenId, long[] prefixCounts) {
            this.valueFingerprint = text(valueFingerprint, "valueFingerprint");
            this.tokenId = nonNegative(tokenId, "tokenId");
            Objects.requireNonNull(prefixCounts, "prefixCounts");
            if (prefixCounts.length == 0 || prefixCounts[0] != 0)
                throw new IllegalArgumentException("prefix counts must start at zero");
            long previous = 0;
            for (long count : prefixCounts) {
                if (count < previous) throw new IllegalArgumentException("prefix counts must be monotonic");
                previous = count;
            }
            this.prefixCounts = prefixCounts.clone();
        }

        public String valueFingerprint() { return valueFingerprint; }
        public int tokenId() { return tokenId; }
        public long[] prefixCounts() { return prefixCounts.clone(); }
        public long rangeCount(int startInclusive, int endExclusive) {
            Objects.checkFromToIndex(startInclusive, endExclusive, prefixCounts.length);
            return prefixCounts[endExclusive] - prefixCounts[startInclusive];
        }
    }

    public static final class TokenFrequency {
        private final String valueFingerprint;
        private final NavigableMap<Integer, Integer> frequencies;

        public TokenFrequency(String valueFingerprint, Map<Integer, Integer> frequencies) {
            this.valueFingerprint = text(valueFingerprint, "valueFingerprint");
            Objects.requireNonNull(frequencies, "frequencies");
            TreeMap<Integer, Integer> copy = new TreeMap<>();
            frequencies.forEach((id, count) -> {
                Objects.requireNonNull(id, "frequency token id");
                Objects.requireNonNull(count, "frequency value");
                nonNegative(id, "frequency token id");
                if (count < 0) throw new IllegalArgumentException("negative frequency");
                if (copy.put(id, count) != null) throw new IllegalArgumentException("duplicate frequency key");
            });
            this.frequencies = Collections.unmodifiableNavigableMap(copy);
        }

        public String valueFingerprint() { return valueFingerprint; }
        public int frequencyAt(int tokenId) {
            nonNegative(tokenId, "tokenId");
            return frequencies.getOrDefault(tokenId, 0);
        }
        public NavigableMap<Integer, Integer> frequencies() { return frequencies; }
    }

    private static String text(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }

    private static int nonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " is negative");
        return value;
    }
}
