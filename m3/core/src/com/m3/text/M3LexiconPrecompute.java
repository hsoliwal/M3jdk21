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

    /** Immutable source-owned acronym metadata; the generic numeric payload remains separate. */
    public record AcronymPrecompute(String acronym, String expansion, String domain) {
        private static final String ACRONYM_PATTERN = "[A-Za-z][A-Za-z0-9+.-]{0,31}";
        private static final String DOMAIN_PATTERN = "[a-z][a-z0-9._-]*";

        public AcronymPrecompute {
            acronym = text(acronym, "acronym");
            expansion = text(expansion, "expansion");
            domain = text(domain, "domain");
            if (!acronym.matches(ACRONYM_PATTERN))
                throw new IllegalArgumentException("invalid acronym");
            if (expansion.isBlank())
                throw new IllegalArgumentException("blank acronym expansion");
            if (!domain.matches(DOMAIN_PATTERN))
                throw new IllegalArgumentException("invalid acronym domain");
        }
    }

    public static final class TranslationProjection {
        private final String lexiconFingerprint;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final String sourceFingerprint;
        private final int[] translatedTokenIds;
        private final int mappedTokenCount;

        public TranslationProjection(String lexiconFingerprint, String sourceLanguage,
                                     String targetLanguage, String sourceFingerprint,
                                     int[] translatedTokenIds) {
            this(lexiconFingerprint, sourceLanguage, targetLanguage, sourceFingerprint,
                    translatedTokenIds, translatedTokenIds == null ? 0 : translatedTokenIds.length);
        }

        public TranslationProjection(String lexiconFingerprint, String sourceLanguage,
                                     String targetLanguage, String sourceFingerprint,
                                     int[] translatedTokenIds, int mappedTokenCount) {
            this.lexiconFingerprint = text(lexiconFingerprint, "lexiconFingerprint");
            this.sourceLanguage = text(sourceLanguage, "sourceLanguage");
            this.targetLanguage = text(targetLanguage, "targetLanguage");
            this.sourceFingerprint = text(sourceFingerprint, "sourceFingerprint");
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
        public int mappedTokenCount() { return mappedTokenCount; }
        public int translatedTokenIdAt(int index) {
            return translatedTokenIds[Objects.checkIndex(index, translatedTokenIds.length)];
        }
        public int[] translatedTokenIds() { return translatedTokenIds.clone(); }

        public boolean appliesTo(String lexicon, String source, String target, String fingerprint) {
            return lexiconFingerprint.equals(lexicon) && sourceLanguage.equals(source)
                    && targetLanguage.equals(target) && sourceFingerprint.equals(fingerprint);
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
