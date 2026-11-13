// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.util.Arrays;
import java.util.Objects;

/** Immutable, indexed UTF-16 suffix search over a private char[] and primitive rank lanes. */
public final class M3SuffixArray {
    private static final int MAX_CODE_UNITS = 250_000;
    private final char[] units;
    private final int[] order;
    private final int[] lcp;

    private M3SuffixArray(char[] units, int[] order, int[] lcp) {
        this.units = units;
        this.order = order;
        this.lcp = lcp;
    }

    /** Copies code units through charAt; supports non-flattenable M3StringPiece views. */
    public static M3SuffixArray index(CharSequence source) {
        Objects.requireNonNull(source, "source");
        int n = source.length();
        if (n > MAX_CODE_UNITS) throw new IllegalArgumentException("text index budget exceeded");
        char[] units = new char[n];
        for (int i = 0; i < n; i++) units[i] = source.charAt(i);
        int[] order = new int[n], rank = new int[n], nextRank = new int[n];
        int[] scratch = new int[n];
        // Compact the actual UTF-16 alphabet first: tiny texts do not allocate a
        // 65,536-entry counter merely because char has a 16-bit value domain.
        char[] alphabet = units.clone();
        Arrays.sort(alphabet);
        int unique = 0;
        for (char ch : alphabet) {
            if (unique == 0 || alphabet[unique - 1] != ch) alphabet[unique++] = ch;
        }
        for (int i = 0; i < n; i++) {
            order[i] = i;
            rank[i] = Arrays.binarySearch(alphabet, 0, unique, units[i]) + 1;
        }
        int maxKey = unique;
        int[] counts = new int[Math.max(maxKey + 2, n + 2)];
        for (int width = 1; width < n;) {
            radix(order, scratch, rank, width, maxKey, counts);
            int classes = 1;
            nextRank[order[0]] = classes;
            for (int i = 1; i < n; i++) {
                int a = order[i - 1], b = order[i];
                if (rank[a] != rank[b]
                        || second(rank, a, width) != second(rank, b, width)) classes++;
                nextRank[b] = classes;
            }
            int[] previous = rank;
            rank = nextRank;
            nextRank = previous;
            maxKey = classes;
            if (classes == n) break;
            width <<= 1;
        }
        int[] lcp = new int[n];
        int[] inverse = new int[n];
        for (int i = 0; i < n; i++) inverse[order[i]] = i;
        for (int i = 0, h = 0; i < n; i++) {
            int at = inverse[i];
            if (at == 0) { h = 0; continue; }
            int before = order[at - 1];
            while (i + h < n && before + h < n && units[i + h] == units[before + h]) h++;
            lcp[at] = h;
            if (h > 0) h--;
        }
        return new M3SuffixArray(units, order, lcp);
    }

    private static int second(int[] rank, int at, int width) {
        return at + width < rank.length ? rank[at + width] : 0;
    }

    private static void radix(int[] order, int[] scratch, int[] rank, int width,
                              int maxKey, int[] counts) {
        Arrays.fill(counts, 0, maxKey + 2, 0);
        for (int at : order) counts[second(rank, at, width) + 1]++;
        for (int key = 1; key <= maxKey + 1; key++) counts[key] += counts[key - 1];
        for (int at : order) scratch[counts[second(rank, at, width)]++] = at;
        Arrays.fill(counts, 0, maxKey + 2, 0);
        for (int at : scratch) counts[rank[at] + 1]++;
        for (int key = 1; key <= maxKey + 1; key++) counts[key] += counts[key - 1];
        for (int at : scratch) order[counts[rank[at]]++] = at;
    }

    public int length() { return units.length; }
    /** Original offsets ordered by lexicographic UTF-16 suffix, shortest first. */
    public int[] suffixOrder() { return order.clone(); }
    /** LCP of each suffix with its previous sorted neighbor; first entry is zero. */
    public int[] longestCommonPrefixes() { return lcp.clone(); }

    /** Number of exact UTF-16 substring occurrences, including overlapping ones. */
    public int count(CharSequence pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length() == 0) return units.length + 1;
        return bound(pattern, true) - bound(pattern, false);
    }

    /** Positions are ascending original text offsets. Refuses rather than truncates above maxResults. */
    public int[] positions(CharSequence pattern, int maxResults) {
        Objects.requireNonNull(pattern, "pattern");
        if (maxResults < 0) throw new IllegalArgumentException("negative result budget");
        if (pattern.length() == 0) {
            if (units.length + 1 > maxResults) throw new IllegalArgumentException("result budget exceeded");
            int[] all = new int[units.length + 1];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }
        int left = bound(pattern, false), right = bound(pattern, true);
        if (right - left > maxResults) throw new IllegalArgumentException("result budget exceeded");
        int[] hits = Arrays.copyOfRange(order, left, right);
        Arrays.sort(hits);
        return hits;
    }

    private int bound(CharSequence pattern, boolean upper) {
        int lo = 0, hi = order.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            int relation = compare(order[mid], pattern);
            if (relation < 0 || upper && relation == 0) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }

    /** Suffix-versus-pattern comparison, returning zero when pattern is a prefix. */
    private int compare(int suffix, CharSequence pattern) {
        for (int j = 0, n = pattern.length(); j < n; j++) {
            if (suffix + j >= units.length) return -1;
            char a = units[suffix + j], b = pattern.charAt(j);
            if (a != b) return Character.compare(a, b);
        }
        return 0;
    }
}
