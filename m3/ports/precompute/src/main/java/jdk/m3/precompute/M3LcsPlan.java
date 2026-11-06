// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** First-party candidate-only UTF-16 code-unit LCS proof; no product mutation authority. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-lcs-bitvector-20261105/src/main/java/com/synexia/m3/proof/Utf16BitVectorLcsOracle.java blob 079e66607954; recipe m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py v1; split members MAX_TEXT,MAX_SHORT,ALPHABET,<init>,validate,bitVectorLength
public final class M3LcsPlan {
    private static final int MAX_TEXT = 4_096;
    private static final int MAX_SHORT = 63;
    private static final int ALPHABET = 1 << 16;

    private M3LcsPlan() { }

    private static void validate(char[] first, char[] second) {
        if (first == null || second == null) {
            throw new NullPointerException("first/second");
        }
        if (first.length > MAX_TEXT || second.length > MAX_TEXT
                || Math.min(first.length, second.length) > MAX_SHORT) {
            throw new IllegalArgumentException("bounded LCS requires <=4096 UTF-16 units each and min<=63");
        }
    }

    /** O(max(lengths) + min(lengths) + 65,536), O(65,536) primitive mask space. */
    public static int bitVectorLength(char[] first, char[] second) {
        validate(first, second);
        char[] shorter = first.length <= second.length ? first : second;
        char[] longer = first.length <= second.length ? second : first;
        if (shorter.length == 0) return 0;
        long[] masks = new long[ALPHABET];
        for (int i = 0; i < shorter.length; i++) masks[shorter[i]] |= 1L << i;
        long state = 0L;
        for (char unit : longer) {
            long x = masks[unit] | state;
            long shifted = (state << 1) | 1L;
            state = x & ~(x - shifted);
        }
        return Long.bitCount(state);
    }
}
