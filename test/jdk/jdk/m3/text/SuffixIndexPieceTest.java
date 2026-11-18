// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

/*
 * @test
 * @summary Build an indexed UTF-16 suffix search directly from joined M3StringPiece views
 * @modules java.base/jdk.m3.text
 *          jdk.m3.collections/jdk.m3.algorithm
 *          jdk.m3.collections/jdk.m3.collections.text
 * @run main/othervm SuffixIndexPieceTest
 */

import jdk.m3.algorithm.StringAlgorithms;
import jdk.m3.collections.text.IntSuffixArray;
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.M3StringPiece;

public class SuffixIndexPieceTest {
    private static int checks;

    public static void main(String[] args) {
        LocalM3Arena arena = new LocalM3Arena();
        M3StringPiece source = M3StringPiece.join(
                arena.copyUtf16(new char[] {'b', 0, 'a', (char) 0xD800}),
                arena.copyUtf16(new char[] {'b', (char) 0xDFFF, 0, 'a'}));
        IntSuffixArray index = StringAlgorithms.indexUtf16(source);
        check(index.length() == source.length(), "source length");
        for (int i = 0; i < source.length(); i++) {
            check(index.tokenAt(i) == source.charAt(i), "source unit " + i);
        }
        for (int i = 0; i < index.length(); i++) {
            int start = index.suffixAtRank(i);
            check(index.rankOfSuffix(start) == i, "inverse rank " + i);
        }
        for (int from = 0; from <= source.length(); from++) {
            for (int to = from; to <= source.length(); to++) {
                M3StringPiece pattern = source.subSequence(from, to);
                IntSuffixArray.Range range = index.utf16PrefixRange(pattern);
                int smaller = 0;
                int nonGreater = 0;
                for (int rank = 0; rank < index.length(); rank++) {
                    int compared = comparison(index, index.suffixAtRank(rank), pattern);
                    if (compared < 0) smaller++;
                    if (compared <= 0) nonGreater++;
                }
                check(range.fromRankInclusive() == smaller && range.toRankExclusive() == nonGreater,
                        "rank interval for " + from + ".." + to);
            }
        }
        // An unrelated string checks negative matches without converting source to String.
        check(!index.utf16Contains("missing"), "missing");
        check(index.utf16Contains("\0a"), "embedded NUL");
        check(index.utf16Contains(""), "empty pattern on nonempty source");
        System.out.println("PASS checks=" + checks);
    }

    private static int comparison(IntSuffixArray index, int suffix, CharSequence pattern) {
        int remaining = index.length() - suffix;
        int compared = Math.min(remaining, pattern.length());
        for (int i = 0; i < compared; i++) {
            int ordering = Integer.compare(index.tokenAt(suffix + i), pattern.charAt(i));
            if (ordering != 0) return ordering;
        }
        return pattern.length() <= remaining ? 0 : -1;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
