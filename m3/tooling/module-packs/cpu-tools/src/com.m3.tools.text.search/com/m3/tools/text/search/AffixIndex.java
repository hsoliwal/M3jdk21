// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.text.search;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable UTF-16 lexicon with exact prefix and suffix range queries.
 * Canonical word IDs index one sorted String[]; reverse-lexical queries use
 * only a second primitive int[] of IDs, never a retained reversed-string pool.
 * No normalization, Unicode folding or code-point reinterpretation occurs.
 *
 * <p>The input is a set of exact words: duplicate inputs collapse to one ID.
 * Construction is bounded and queries return detached primitive IDs. This is
 * an indexed dictionary, not a mutable LeetCode-style trie or a regex engine.
 */
public final class AffixIndex {
    public static final int MAX_TERMS = 100_000;
    public static final int MAX_TERM_UTF16 = 8_192;
    public static final int MAX_TOTAL_UTF16 = 1_048_576;
    private static final Runnable NOOP = () -> { };

    private final String[] words;
    private final int[] suffixOrdinals;
    private final int inputCount;
    private final String sha256;

    private AffixIndex(String[] words, int[] suffixOrdinals, int inputCount,
                       String sha256) {
        this.words = words;
        this.suffixOrdinals = suffixOrdinals;
        this.inputCount = inputCount;
        this.sha256 = sha256;
    }

    public static AffixIndex prepare(List<String> terms) {
        return prepare(terms, NOOP);
    }

    /**
     * Checkpoints cover admission, reverse-index sort and canonical hashing.
     * The caller retains ownership of its input collection; this index holds
     * only its immutable String values, never the input collection itself.
     */
    public static AffixIndex prepare(List<String> terms, Runnable checkpoint) {
        Objects.requireNonNull(terms, "terms");
        Objects.requireNonNull(checkpoint, "checkpoint");
        checkpoint.run();
        if (terms.size() > MAX_TERMS) throw new IllegalArgumentException("lexicon term budget");
        String[] sorted = new String[terms.size()];
        int units = 0, cursor = 0;
        for (String term : terms) {
            if ((cursor & 4095) == 0) checkpoint.run();
            if (cursor == sorted.length) throw new IllegalArgumentException("concurrent lexicon mutation");
            Objects.requireNonNull(term, "term");
            if (term.length() > MAX_TERM_UTF16) {
                throw new IllegalArgumentException("lexicon term UTF-16 budget");
            }
            units = Math.addExact(units, term.length());
            if (units > MAX_TOTAL_UTF16) {
                throw new IllegalArgumentException("lexicon total UTF-16 budget");
            }
            sorted[cursor++] = term;
        }
        if (cursor != sorted.length) throw new IllegalArgumentException("concurrent lexicon mutation");
        Arrays.sort(sorted);
        int distinct = 0;
        for (int i = 0; i < sorted.length; i++) {
            if ((i & 4095) == 0) checkpoint.run();
            String word = sorted[i];
            if (distinct == 0 || !word.equals(sorted[distinct - 1])) sorted[distinct++] = word;
        }
        String[] dictionary = Arrays.copyOf(sorted, distinct);
        int[] suffixIds = reverseOrder(dictionary, checkpoint);
        String fingerprint = fingerprint(dictionary, checkpoint);
        checkpoint.run();
        return new AffixIndex(dictionary, suffixIds, terms.size(), fingerprint);
    }

    public int inputCount() { return inputCount; }
    public int distinctCount() { return words.length; }
    public String wordAt(int ordinal) { return words[Objects.checkIndex(ordinal, words.length)]; }
    public String sha256() { return sha256; }

    public boolean contains(String word) {
        return Arrays.binarySearch(words, Objects.requireNonNull(word, "word")) >= 0;
    }

    public int countPrefix(String prefix) {
        int[] bounds = range(Objects.requireNonNull(prefix, "prefix"), false, NOOP);
        return bounds[1] - bounds[0];
    }

    public int countSuffix(String suffix) {
        int[] bounds = range(Objects.requireNonNull(suffix, "suffix"), true, NOOP);
        return bounds[1] - bounds[0];
    }

    public Result prefix(String prefix, int maxHits) {
        return prefix(prefix, maxHits, NOOP);
    }

    public Result suffix(String suffix, int maxHits) {
        return suffix(suffix, maxHits, NOOP);
    }

    /** Canonical UTF-16 lexical word-ID order for exact prefix matches. */
    public Result prefix(String prefix, int maxHits, Runnable checkpoint) {
        return query(prefix, maxHits, false, checkpoint);
    }

    /** Reverse UTF-16 lexical order for suffix matches; IDs remain canonical. */
    public Result suffix(String suffix, int maxHits, Runnable checkpoint) {
        return query(suffix, maxHits, true, checkpoint);
    }

    private Result query(String fragment, int maxHits, boolean reverse, Runnable checkpoint) {
        Objects.requireNonNull(fragment, "fragment");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (fragment.length() > MAX_TERM_UTF16) throw new IllegalArgumentException("affix UTF-16 budget");
        if (maxHits < 0 || maxHits > MAX_TERMS) throw new IllegalArgumentException("affix result budget");
        checkpoint.run();
        int[] bounds = range(fragment, reverse, checkpoint);
        int total = bounds[1] - bounds[0];
        int count = Math.min(maxHits, total);
        int[] matches = new int[count];
        for (int i = 0; i < count; i++) {
            if ((i & 4095) == 0) checkpoint.run();
            matches[i] = reverse ? suffixOrdinals[bounds[0] + i] : bounds[0] + i;
        }
        checkpoint.run();
        return new Result(matches, total);
    }

    private int[] range(String fragment, boolean reverse, Runnable checkpoint) {
        int[] result = new int[2];
        for (int edge = 0; edge < 2; edge++) {
            int lo = 0, hi = words.length;
            while (lo < hi) {
                checkpoint.run();
                int mid = (lo + hi) >>> 1;
                String word = words[reverse ? suffixOrdinals[mid] : mid];
                int compared = compareAffix(word, fragment, reverse);
                if (compared < 0 || (edge == 1 && compared == 0)) lo = mid + 1;
                else hi = mid;
            }
            result[edge] = lo;
        }
        return result;
    }

    private static int compareAffix(String word, String fragment, boolean reverse) {
        int n = Math.min(word.length(), fragment.length());
        for (int i = 0; i < n; i++) {
            char a = word.charAt(reverse ? word.length() - i - 1 : i);
            char b = fragment.charAt(reverse ? fragment.length() - i - 1 : i);
            if (a != b) return Character.compare(a, b);
        }
        return word.length() < fragment.length() ? -1 : 0;
    }

    /** Bottom-up primitive merge sort: no boxed IDs or retained per-trie-node objects. */
    private static int[] reverseOrder(String[] words, Runnable checkpoint) {
        int n = words.length;
        int[] ids = new int[n];
        int[] spare = new int[n];
        for (int i = 0; i < n; i++) ids[i] = i;
        for (int width = 1; width < n; width <<= 1) {
            checkpoint.run();
            for (int base = 0; base < n; base += 2 * width) {
                int mid = Math.min(base + width, n);
                int end = Math.min(base + 2 * width, n);
                int left = base, right = mid;
                for (int out = base; out < end; out++) {
                    if ((out & 4095) == 0) checkpoint.run();
                    if (left == mid) spare[out] = ids[right++];
                    else if (right == end) spare[out] = ids[left++];
                    else if (compareReverse(words[ids[left]], words[ids[right]]) <= 0) {
                        spare[out] = ids[left++];
                    } else spare[out] = ids[right++];
                }
            }
            int[] old = ids;
            ids = spare;
            spare = old;
        }
        return ids;
    }

    private static int compareReverse(String left, String right) {
        int size = Math.min(left.length(), right.length());
        for (int i = 0; i < size; i++) {
            char a = left.charAt(left.length() - 1 - i);
            char b = right.charAt(right.length() - 1 - i);
            if (a != b) return Character.compare(a, b);
        }
        return Integer.compare(left.length(), right.length());
    }

    private static String fingerprint(String[] words, Runnable checkpoint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("m3-affix-lexicon-utf16-v1".getBytes(StandardCharsets.US_ASCII));
            number(digest, words.length);
            for (int i = 0; i < words.length; i++) {
                if ((i & 4095) == 0) checkpoint.run();
                String word = words[i];
                number(digest, word.length());
                for (int at = 0; at < word.length(); at++) {
                    if ((at & 4095) == 0) checkpoint.run();
                    char unit = word.charAt(at);
                    digest.update((byte) (unit >>> 8));
                    digest.update((byte) unit);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void number(MessageDigest digest, int number) {
        digest.update((byte) (number >>> 24));
        digest.update((byte) (number >>> 16));
        digest.update((byte) (number >>> 8));
        digest.update((byte) number);
    }

    public static final class Result {
        private final int[] ordinals;
        private final int total;

        private Result(int[] ordinals, int total) {
            this.ordinals = ordinals;
            this.total = total;
        }

        public int size() { return ordinals.length; }
        public int totalMatches() { return total; }
        public boolean complete() { return ordinals.length == total; }
        public int ordinalAt(int index) { return ordinals[Objects.checkIndex(index, ordinals.length)]; }
        public int[] ordinals() { return ordinals.clone(); }
    }
}
