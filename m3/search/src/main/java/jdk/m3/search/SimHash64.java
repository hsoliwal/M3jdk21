/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/reduce/SimHash64.java (blob 0b4ae294a0ad) by M3_SEARCH_RELOCATE v4.
package jdk.m3.search;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small allocation-bounded 64-bit token SimHash for near-duplicate candidate filtering. */
public final class SimHash64 {
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}_']+");

    private SimHash64() {
    }

    public static long of(String text) {
        int[] weights = new int[64];
        String source = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN.matcher(source);
        while (matcher.find()) {
            long hash = Utf8Fnv1a64.hash(source, matcher.start(), matcher.end());
            for (int bit = 0; bit < 64; bit++) {
                weights[bit] += ((hash >>> bit) & 1L) == 1L ? 1 : -1;
            }
        }
        long result = 0L;
        for (int bit = 0; bit < 64; bit++) {
            if (weights[bit] >= 0) {
                result |= 1L << bit;
            }
        }
        return result;
    }

    public static int hamming(long left, long right) {
        return Long.bitCount(left ^ right);
    }
}
