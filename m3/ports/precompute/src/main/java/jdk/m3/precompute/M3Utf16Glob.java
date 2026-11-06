// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

/** Candidate-only UTF-16 whole-input glob matching; no regex or product-ABI authority. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/recipe-crates/m3-utf16-wildcard-glob-20261105/src/main/java/com/synexia/m3/proof/Utf16WildcardOracle.java blob 3727bd2168e5; recipe m3/ports/precompute/recipe/utf16-atoms-20261106/relocate.py v1; split members MAX_TEXT,MAX_PATTERN,<init>,requireValid,match
public final class M3Utf16Glob {
    public static final int MAX_TEXT = 1_048_576;
    public static final int MAX_PATTERN = 65_536;

    private M3Utf16Glob() { }

    private static void requireValid(char[] text, char[] pattern) {
        if (text == null || pattern == null) throw new NullPointerException("text/pattern");
        if (text.length > MAX_TEXT || pattern.length > MAX_PATTERN) {
            throw new IllegalArgumentException("UTF-16 glob input exceeds bound");
        }
    }

    /** '?' matches one UTF-16 unit; '*' matches zero or more; full-string match only.
     * Worst case O(n*m), O(1) auxiliary storage; literals are compared by code unit.
     */
    public static boolean match(char[] text, char[] pattern) {
        requireValid(text, pattern);
        int t = 0, p = 0, lastStar = -1, starText = 0;
        while (t < text.length) {
            if (p < pattern.length && pattern[p] == '*') {
                lastStar = p++;
                starText = t;
            } else if (p < pattern.length && (pattern[p] == '?' || pattern[p] == text[t])) {
                p++;
                t++;
            } else if (lastStar >= 0 && starText < text.length) {
                p = lastStar + 1;
                t = ++starText;
            } else {
                return false;
            }
        }
        while (p < pattern.length && pattern[p] == '*') p++;
        return p == pattern.length;
    }
}
