/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections.text;

import java.util.Arrays;
import java.util.Random;

/**
 * Independent suffix-order, Kasai LCP and prefix-range oracle for bounded
 * dense and sparse signed integer domains. Test-only boxed sorting is allowed.
 * Production implementation stores no per-suffix objects or boxed tokens.
 */
public final class IntSuffixArrayDenseRankSelfTest {
    private static long checks;
    private IntSuffixArrayDenseRankSelfTest() { }

    public static void main(String[] args) {
        edgeCases();
        randomized();
        largeDenseAndUtf16();
        System.out.println("PASS IntSuffixArrayDenseRankSelfTest checks=" + checks);
    }

    private static void edgeCases() {
        for (int[] values : new int[][] {
                {}, {0}, {-1}, {1, 1}, {2, 1}, {0, 1, 0, 1},
                {Integer.MIN_VALUE, Integer.MAX_VALUE, 0},
                {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2},
                {Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE},
                {-65535, 0, 0, -65534}, {0, 65535, 65534, 0},
                {0xd800, 0xdfff, 0xffff, 0, 0xdfff, 0}
        }) verify(values);
        var empty = new IntSuffixArray(new int[0]);
        same(0, empty.length(), "empty size");
        same(new IntSuffixArray.Range(0, 0), empty.prefixRange(new int[0]),
                "empty prefix in empty source");
        var one = new IntSuffixArray(new int[] {7});
        same(new IntSuffixArray.Range(0, 1), one.prefixRange(new int[0]),
                "empty prefix in nonempty source");
        same(new IntSuffixArray.Range(0, 1), one.prefixRange(new int[] {7}),
                "single-token lookup");
    }

    private static void randomized() {
        Random rng = new Random(0x4d3344454e534552L);
        for (int trial = 0; trial < 3_000; trial++) {
            int n = trial % 11 == 0 ? rng.nextInt(230) : rng.nextInt(115);
            int[] source = new int[n];
            for (int i = 0; i < n; i++) {
                source[i] = switch (trial % 12) {
                    case 0 -> rng.nextInt(2);
                    case 1 -> rng.nextInt(256);
                    case 2 -> rng.nextInt(65536);
                    case 3 -> Integer.MIN_VALUE + rng.nextInt(30);
                    case 4 -> Integer.MAX_VALUE - rng.nextInt(30);
                    case 5 -> rng.nextInt();
                    case 6 -> rng.nextInt(1 << 20);
                    case 7 -> trial % 3 == 0 ? 0 : 0xd800;
                    case 8 -> i % 7;
                    case 9 -> -i;
                    case 10 -> i;
                    default -> rng.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                };
            }
            verify(source);
        }
    }

    private static void verify(int[] input) {
        int[] source = input.clone();
        IntSuffixArray index = new IntSuffixArray(input);
        Arrays.fill(input, 1010101); // input ownership must be detached
        same(source.length, index.length(), "index length");
        for (int i = 0; i < source.length; i++) {
            same(source[i], index.tokenAt(i), "owned token snapshot");
        }
        Integer[] sorted = new Integer[source.length];
        for (int i = 0; i < sorted.length; i++) sorted[i] = i;
        Arrays.sort(sorted, (a, b) -> compareSuffix(source, a, b));
        for (int rank = 0; rank < sorted.length; rank++) {
            int suffix = sorted[rank];
            same(suffix, index.suffixAtRank(rank), "signed suffix position");
            same(rank, index.rankOfSuffix(suffix), "inverse suffix rank");
            int lcp = rank == 0 ? 0 : lcp(source, sorted[rank - 1], suffix);
            same(lcp, index.lcpAtRank(rank), "Kasai LCP");
        }
        same(4L * 6L * source.length, index.payloadBytes(),
                "six owned primitive lanes");
        Random queries = new Random(0x413d32L + source.length * 19L);
        for (int q = 0; q < 14; q++) {
            int length = queries.nextInt(9);
            int[] pattern = new int[length];
            for (int k = 0; k < length; k++) {
                pattern[k] = source.length > 0 && q % 4 == 0
                        ? source[queries.nextInt(source.length)]
                        : switch (q % 5) {
                            case 0 -> 0;
                            case 1 -> 0xffff;
                            case 2 -> Integer.MIN_VALUE;
                            case 3 -> Integer.MAX_VALUE;
                            default -> queries.nextInt(8) - 4;
                        };
            }
            int lo = 0, hi = 0;
            for (int rank = 0; rank < sorted.length; rank++) {
                int comparison = comparePrefix(source, sorted[rank], pattern);
                if (comparison < 0) lo++;
                if (comparison <= 0) hi++;
            }
            IntSuffixArray.Range want = new IntSuffixArray.Range(lo, hi);
            same(want, index.prefixRange(pattern), "prefix rank range");
            int[] framed = new int[pattern.length + 2];
            framed[0] = 1003;
            framed[framed.length - 1] = -1004;
            System.arraycopy(pattern, 0, framed, 1, pattern.length);
            same(want, index.prefixRange(framed, 1, pattern.length),
                    "offset prefix range");
        }
    }

    private static int compareSuffix(int[] source, int first, int second) {
        if (first == second) return 0;
        while (first < source.length && second < source.length) {
            int compared = Integer.compare(source[first], source[second]);
            if (compared != 0) return compared;
            first++;
            second++;
        }
        return Integer.compare(source.length - first, source.length - second);
    }

    private static int lcp(int[] source, int a, int b) {
        int count = 0;
        while (a + count < source.length && b + count < source.length
                && source[a + count] == source[b + count]) count++;
        return count;
    }

    private static int comparePrefix(int[] source, int at, int[] pattern) {
        int available = source.length - at;
        for (int i = 0; i < Math.min(pattern.length, available); i++) {
            int compared = Integer.compare(source[at + i], pattern[i]);
            if (compared != 0) return compared;
        }
        return pattern.length <= available ? 0 : -1;
    }

    private static void largeDenseAndUtf16() {
        int[] codeUnits = new int[65536];
        for (int i = 0; i < codeUnits.length; i++) codeUnits[i] = i;
        IntSuffixArray chars = new IntSuffixArray(codeUnits);
        for (int i = 0; i < codeUnits.length; i++) {
            same(i, chars.suffixAtRank(i), "full UTF-16 domain suffix");
            same(i, chars.rankOfSuffix(i), "full UTF-16 inverse rank");
            same(0, chars.lcpAtRank(i), "unique code-unit LCP");
        }
        same(new IntSuffixArray.Range(0xD800, 0xD801),
                chars.prefixRange(new int[] {0xd800}), "surrogate code-unit lookup");
        int[] periodic = new int[131072];
        for (int i = 0; i < periodic.length; i++) periodic[i] = 0xd800 + i % 3;
        IntSuffixArray large = new IntSuffixArray(periodic);
        boolean[] seen = new boolean[periodic.length];
        for (int rank = 0; rank < periodic.length; rank++) {
            int offset = large.suffixAtRank(rank);
            check(offset >= 0 && offset < periodic.length && !seen[offset],
                    "periodic permutation");
            seen[offset] = true;
            same(rank, large.rankOfSuffix(offset), "periodic inverse rank");
        }
        char[] utf16 = new char[65536];
        for (int i = 0; i < utf16.length; i++) utf16[i] = (char) i;
        IntSuffixArray text = IntSuffixArray.utf16(new String(utf16));
        same(chars.length(), text.length(), "UTF-16 source cardinality");
        for (int rank = 0; rank < text.length(); rank++) {
            same(chars.suffixAtRank(rank), text.suffixAtRank(rank),
                    "UTF-16 and signed-token parity");
        }
    }

    private static void check(boolean value, String explanation) {
        checks++;
        if (!value) throw new AssertionError(explanation + " at check " + checks);
    }
    private static void same(long expected, long actual, String explanation) {
        checks++;
        if (expected != actual) throw new AssertionError(
                explanation + " expected=" + expected + " actual=" + actual);
    }
    private static void same(Object expected, Object actual, String explanation) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(
                explanation + " expected=" + expected + " actual=" + actual);
    }
}
