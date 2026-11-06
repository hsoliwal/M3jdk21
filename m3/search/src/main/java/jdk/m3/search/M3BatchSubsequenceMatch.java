// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/com/synexia/m3/search/BatchSubsequenceMatch.java (blob d49ea390ba99) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;
import java.util.Objects;

/** Private batch-subsequence search proof with primitive UTF-16 waiting buckets. */
final class M3BatchSubsequenceMatch {
    static final int MAX_SOURCE = 1_000_000;
    static final int MAX_PATTERNS = 65_536;
    static final int MAX_TOTAL_PATTERN_UNITS = 1_000_000;
    private static final int CODE_UNIT_COUNT = 65_536;

    private M3BatchSubsequenceMatch() { }

    /** Immutable admission. No public product API or global registry mutation. */
    static void admit(String source, String[] patterns) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(patterns, "patterns");
        if (source.length() > MAX_SOURCE || patterns.length > MAX_PATTERNS) {
            throw new IllegalArgumentException("batch subsequence budget");
        }
        int units = 0;
        for (String pattern : patterns) {
            Objects.requireNonNull(pattern, "pattern");
            if (pattern.length() > MAX_TOTAL_PATTERN_UNITS - units) {
                throw new IllegalArgumentException("batch subsequence pattern units");
            }
            units += pattern.length();
        }
    }

    /** Count matching input entries, including duplicates and empty patterns. */
    static long count(String source, String[] patterns) {
        admit(source, patterns);
        int[] head = new int[CODE_UNIT_COUNT];
        Arrays.fill(head, -1);
        int[] next = new int[patterns.length];
        int[] cursor = new int[patterns.length];
        long matched = 0;
        for (int id = 0; id < patterns.length; id++) {
            String pattern = patterns[id];
            if (pattern.isEmpty()) {
                matched++;
            } else {
                int c = pattern.charAt(0);
                next[id] = head[c];
                head[c] = id;
            }
        }
        for (int i = 0; i < source.length(); i++) {
            int c = source.charAt(i);
            int pending = head[c];
            head[c] = -1; // detach: a pattern cannot consume the same source unit twice
            while (pending >= 0) {
                int id = pending;
                pending = next[id];
                int position = ++cursor[id];
                String pattern = patterns[id];
                if (position == pattern.length()) {
                    matched++;
                } else {
                    int target = pattern.charAt(position);
                    next[id] = head[target];
                    head[target] = id;
                }
            }
        }
        return matched;
    }

    /** Explicitly opt-in JNI entry; no automatic library load or existing export change. */
    static native long countNative(char[] source, char[] packedPatterns, int[] offsets);
}
