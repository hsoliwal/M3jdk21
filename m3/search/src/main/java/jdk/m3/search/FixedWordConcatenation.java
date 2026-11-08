// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/FixedWordConcatenation.java (blob c353734ef7c0) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;
import java.util.Objects;

/**
 * Private bounded fixed-width word-multiset concatenation search.
 * UTF-16 code units are literal; exact comparison is mandatory after hashing.
 */
final class FixedWordConcatenation {
    static final int MAX_SOURCE = 8192;
    static final int MAX_WORDS = 32;
    static final int MAX_WIDTH = 16;
    private static final int TABLE_SIZE = 128;
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private FixedWordConcatenation() { }

    static int[] find(String source, String[] words) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(words, "words");
        if (source.length() > MAX_SOURCE || words.length == 0 || words.length > MAX_WORDS) {
            throw new IllegalArgumentException("fixed-word search budget");
        }
        String first = Objects.requireNonNull(words[0], "word");
        int width = first.length();
        if (width == 0 || width > MAX_WIDTH) {
            throw new IllegalArgumentException("invalid fixed word width");
        }
        // Entire input is validated before any result or workspace is produced.
        for (String word : words) {
            Objects.requireNonNull(word, "word");
            if (word.length() != width) {
                throw new IllegalArgumentException("inconsistent fixed word width");
            }
        }
        if ((long) words.length * width > source.length()) return new int[0];

        Dictionary dictionary = new Dictionary(words);
        int[] seen = new int[dictionary.distinct];
        boolean[] found = new boolean[source.length()];
        for (int offset = 0; offset < width; offset++) {
            Arrays.fill(seen, 0);
            int left = offset;
            int count = 0;
            for (int right = offset; right + width <= source.length(); right += width) {
                int id = dictionary.lookup(source, right, width);
                if (id < 0) {
                    Arrays.fill(seen, 0);
                    count = 0;
                    left = right + width;
                    continue;
                }
                seen[id]++;
                count++;
                while (seen[id] > dictionary.required[id]) {
                    int removed = dictionary.lookup(source, left, width);
                    if (removed < 0) throw new IllegalStateException("invalid active window");
                    seen[removed]--;
                    count--;
                    left += width;
                }
                if (count == words.length) {
                    found[left] = true;
                    int removed = dictionary.lookup(source, left, width);
                    if (removed < 0) throw new IllegalStateException("invalid matching window");
                    seen[removed]--;
                    count--;
                    left += width;
                }
            }
        }
        int matches = 0;
        for (boolean hit : found) if (hit) matches++;
        int[] result = new int[matches];
        int cursor = 0;
        for (int i = 0; i < found.length; i++) {
            if (found[i]) result[cursor++] = i;
        }
        return result;
    }

    private static long hash(String sequence, int at, int width) {
        long value = FNV_OFFSET;
        for (int i = at; i < at + width; i++) {
            value = (value ^ sequence.charAt(i)) * FNV_PRIME;
        }
        return value;
    }

    private static final class Dictionary {
        final String[] spellings;
        final long[] hashes;
        final int[] required;
        final int[] slots = new int[TABLE_SIZE];
        int distinct;

        Dictionary(String[] words) {
            spellings = new String[words.length];
            hashes = new long[words.length];
            required = new int[words.length];
            for (String word : words) {
                long key = hash(word, 0, word.length());
                int slot = ((int) key) & (TABLE_SIZE - 1);
                for (;;) {
                    int resident = slots[slot] - 1;
                    if (resident < 0) {
                        spellings[distinct] = word;
                        hashes[distinct] = key;
                        required[distinct] = 1;
                        slots[slot] = ++distinct;
                        break;
                    }
                    if (hashes[resident] == key && spellings[resident].equals(word)) {
                        required[resident]++;
                        break;
                    }
                    slot = (slot + 1) & (TABLE_SIZE - 1);
                }
            }
        }

        int lookup(String source, int at, int width) {
            long key = hash(source, at, width);
            int slot = ((int) key) & (TABLE_SIZE - 1);
            for (;;) {
                int resident = slots[slot] - 1;
                if (resident < 0) return -1;
                if (hashes[resident] == key
                        && source.regionMatches(at, spellings[resident], 0, width)) {
                    return resident;
                }
                slot = (slot + 1) & (TABLE_SIZE - 1);
            }
        }
    }

    /** Test-owned JNI boundary. No native library is loaded implicitly. */
    static native int[] findNative(char[] source, char[][] words);
}
