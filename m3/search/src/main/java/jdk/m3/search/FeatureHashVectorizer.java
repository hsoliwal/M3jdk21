/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/FeatureHashVectorizer.java (blob f3fe34600425) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model-free lexical feature hashing for the vector retrieval lane.
 *
 * <p>Unigrams and adjacent token bigrams are projected into a fixed power-of-two vector using
 * stable FNV-1a hashes and signed hashing, then L2-normalized. Tokens are streamed directly from
 * the matcher: no token list and no concatenated bigram String are created.
 */
public final class FeatureHashVectorizer {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_']+");

    private final int dimensions;

    public FeatureHashVectorizer(int dimensions) {
        if (dimensions < 16 || dimensions > 1024
                || Integer.bitCount(dimensions) != 1) {
            throw new IllegalArgumentException(
                    "dimensions must be a power of two in [16,1024]");
        }
        this.dimensions = dimensions;
    }

    public int dimensions() {
        return dimensions;
    }

    public float[] vector(String text) {
        float[] vector = new float[dimensions];
        String source = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN.matcher(source);
        long previousTokenHash = 0L;
        boolean previous = false;
        while (matcher.find()) {
            // Hash matcher ranges directly: no group String or UTF-8 byte[] per token.
            int start = matcher.start();
            int end = matcher.end();
            long currentHash = Utf8Fnv1a64.hash(source, start, end);
            addHash(vector, currentHash, 1.0f);
            if (previous) {
                long pairHash = Utf8Fnv1a64.updateByte(previousTokenHash, 0);
                pairHash = Utf8Fnv1a64.update(pairHash, source, start, end);
                addHash(vector, pairHash, 0.5f);
            }
            previousTokenHash = currentHash;
            previous = true;
        }

        double sumSquares = 0.0;
        for (float value : vector) {
            sumSquares += (double) value * value;
        }
        if (sumSquares == 0.0) {
            vector[0] = 1.0f;
            return vector;
        }

        float inverseNorm = (float) (1.0 / Math.sqrt(sumSquares));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= inverseNorm;
        }
        return vector;
    }

    private void addHash(float[] vector, long hash, float weight) {
        int index = (int) hash & (dimensions - 1);
        float sign = (hash & (1L << 63)) == 0L ? 1.0f : -1.0f;
        vector[index] += sign * weight;
    }

}
