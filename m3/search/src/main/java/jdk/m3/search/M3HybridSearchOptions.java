/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia synexia-fastsearch/com/synexia/fastsearch/knowledge/index/HybridSearchOptions.java (blob 02d974bd9d0e) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

/**
 * Query-local retrieval budget and non-negative finite RRF weights.
 *
 * <p>Weights do not rebuild the index. A zero weight disables that retrieval lane.
 * Blank queries have only the lexical browse lane; a vector-only blank query
 * therefore returns no hits. At least one lane must have positive weight.
 */
public record M3HybridSearchOptions(
        int limit, int candidateMultiplier, float lexicalWeight, float vectorWeight) {
    public M3HybridSearchOptions {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        if (candidateMultiplier < 1) {
            throw new IllegalArgumentException("candidateMultiplier must be >= 1");
        }
        validateWeight(lexicalWeight, "lexicalWeight");
        validateWeight(vectorWeight, "vectorWeight");
        if (lexicalWeight == 0.0f && vectorWeight == 0.0f) {
            throw new IllegalArgumentException("at least one weight must be positive");
        }
    }

    /** Preserves the existing four-times retrieval budget and equal RRF weights. */
    public static M3HybridSearchOptions defaults(int limit) {
        return new M3HybridSearchOptions(limit, 4, 1.0f, 1.0f);
    }

    int candidateCount(int numDocs) {
        if (numDocs < 0) {
            throw new IllegalArgumentException("numDocs must be >= 0");
        }
        // Retain the existing saturation cap, but multiply before narrowing.
        long expanded = (long) limit * candidateMultiplier;
        long budget = Math.max(limit, Math.min(Integer.MAX_VALUE / 4L, expanded));
        return (int) Math.min(numDocs, budget);
    }

    private static void validateWeight(float weight, String name) {
        if (!Float.isFinite(weight) || weight < 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
