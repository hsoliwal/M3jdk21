/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.indexdb;

import java.util.Objects;

/**
 * Immutable, composable 64-bit SimHash vote summary for prehashed chunk features.
 * <p>Similarity is a discovery signal, never content identity or semantic equivalence.
 * The exact SHA-256 / structural / logic roots remain with M3IndexDbSemanticFingerprint.
 * Children may only compose with the same explicit feature-extraction domain, including
 * tokenization, weighting and hash algorithm/version. A final 64-bit SimHash alone is
 * insufficient to reconstruct the votes needed for composition.
 * <p>One input feature is one previously prepared 64-bit shingle/chunk feature hash.
 * Hashing whole, large chunks is normally unsuitable for near-duplicate detection:
 * small edits change the entire feature. Callers choose stable shingles/atoms.
 * No text, entry objects, source arrays or JNI pointers are retained.
 */
public final class M3IndexDbChunkSimHash {
    private static final int BITS = Long.SIZE;
    private final String featureDomain;
    private final long[] votes;
    private final long totalWeight;
    private final long featureCount;

    private M3IndexDbChunkSimHash(String featureDomain, long[] votes,
            long totalWeight, long featureCount) {
        this.featureDomain = featureDomain;
        this.votes = votes;
        this.totalWeight = totalWeight;
        this.featureCount = featureCount;
    }

    /** Empty identity for one specifically declared feature/weighting protocol. */
    public static M3IndexDbChunkSimHash empty(String featureDomain) {
        return new M3IndexDbChunkSimHash(checkDomain(featureDomain), new long[BITS], 0L, 0L);
    }

    /**
     * Summarize a window of prehashed features with an independently offset weight lane.
     * Values in weights must be positive. All geometry is checked before accumulation;
     * exact integer arithmetic refuses overflow instead of producing a false similarity.
     */
    public static M3IndexDbChunkSimHash fromFeatures(String featureDomain,
            long[] featureHashes, int hashOffset, int[] weights, int weightOffset, int length) {
        String domain = checkDomain(featureDomain);
        Objects.requireNonNull(featureHashes, "featureHashes");
        Objects.requireNonNull(weights, "weights");
        Objects.checkFromIndexSize(hashOffset, length, featureHashes.length);
        Objects.checkFromIndexSize(weightOffset, length, weights.length);
        long[] accumulator = new long[BITS];
        long total = 0;
        for (int index = 0; index < length; index++) {
            int weight = weights[weightOffset + index];
            if (weight <= 0) throw new IllegalArgumentException("positive feature weights required");
            long feature = featureHashes[hashOffset + index];
            total = Math.addExact(total, weight);
            for (int bit = 0; bit < BITS; bit++) {
                long delta = ((feature >>> bit) & 1L) != 0 ? weight : -(long) weight;
                accumulator[bit] = Math.addExact(accumulator[bit], delta);
            }
        }
        return new M3IndexDbChunkSimHash(domain, accumulator, total, length);
    }

    /** Exact algebra of adjacent or disjoint feature summaries; not an ordered text hash. */
    public M3IndexDbChunkSimHash merge(M3IndexDbChunkSimHash other) {
        Objects.requireNonNull(other, "other");
        if (!featureDomain.equals(other.featureDomain)) {
            throw new IllegalArgumentException("feature extraction domains differ");
        }
        long[] combined = new long[BITS];
        for (int bit = 0; bit < BITS; bit++) {
            combined[bit] = Math.addExact(votes[bit], other.votes[bit]);
        }
        return new M3IndexDbChunkSimHash(featureDomain, combined,
                Math.addExact(totalWeight, other.totalWeight),
                Math.addExact(featureCount, other.featureCount));
    }

    /** Logical repetition of exactly the same feature multiset without rescanning it. */
    public M3IndexDbChunkSimHash repeat(long times) {
        if (times < 0) throw new IllegalArgumentException("negative repetitions");
        long[] scaled = new long[BITS];
        for (int bit = 0; bit < BITS; bit++) scaled[bit] = Math.multiplyExact(votes[bit], times);
        return new M3IndexDbChunkSimHash(featureDomain, scaled,
                Math.multiplyExact(totalWeight, times), Math.multiplyExact(featureCount, times));
    }

    /** Ties vote one, matching the existing M3IndexDB SimHash tie convention. */
    public long simHash64() {
        if (featureCount == 0) return 0L;
        long signature = 0;
        for (int bit = 0; bit < BITS; bit++) {
            if (votes[bit] >= 0) signature |= 1L << bit;
        }
        return signature;
    }

    public int hammingDistance(M3IndexDbChunkSimHash other) {
        Objects.requireNonNull(other, "other");
        if (!featureDomain.equals(other.featureDomain)) {
            throw new IllegalArgumentException("different similarity domains");
        }
        return Long.bitCount(simHash64() ^ other.simHash64());
    }

    /** Defensive copy for indexing, persistence or an independent implementation oracle. */
    public long[] voteCopy() { return votes.clone(); }
    public String featureDomain() { return featureDomain; }
    public long totalWeight() { return totalWeight; }
    public long featureCount() { return featureCount; }

    private static String checkDomain(String domain) {
        String value = Objects.requireNonNull(domain, "featureDomain");
        if (value.isBlank() || value.length() > 256 || !value.equals(value.strip())
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("explicit bounded feature domain required");
        }
        return value;
    }
}
