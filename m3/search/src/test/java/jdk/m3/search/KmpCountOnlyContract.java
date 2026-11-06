// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/KmpCountOnlyContract.java (blob 0277a547382f) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent byte-by-byte oracle for one-pass, overlapping, count-only Java KMP. */
public final class KmpCountOnlyContract {
    private static long checks;
    private KmpCountOnlyContract() { }

    private static void check(boolean yes, String reason) {
        checks++;
        if (!yes) throw new AssertionError(reason);
    }

    private static List<Integer> oracle(byte[] input, byte[] pattern, int from) {
        List<Integer> result = new ArrayList<>();
        for (int start = Math.max(0, from); start <= input.length - pattern.length; start++) {
            int matched = 0;
            while (matched < pattern.length && input[start + matched] == pattern[matched]) matched++;
            if (matched == pattern.length) result.add(start);
        }
        return result;
    }

    public static long runOracle() {
        checks = 0;
        Random random = new Random(0x4d334b4d50434f55L);
        for (int trial = 0; trial < 25_000; trial++) {
            byte[] input = new byte[random.nextInt(160)];
            byte[] pattern = new byte[1 + random.nextInt(25)];
            for (int i = 0; i < input.length; i++) {
                input[i] = (byte) (random.nextInt(5) == 0 ? random.nextInt(256) : random.nextInt(5));
            }
            for (int i = 0; i < pattern.length; i++) {
                pattern[i] = (byte) (random.nextInt(5) == 0 ? random.nextInt(256) : random.nextInt(5));
            }
            int from = switch (trial % 7) {
                case 0 -> Integer.MAX_VALUE;
                case 1 -> Integer.MIN_VALUE;
                case 2 -> input.length;
                default -> random.nextInt(input.length + 20) - 10;
            };
            List<Integer> expected = oracle(input, pattern, from);
            M3KmpSearch kmp = new M3KmpSearch(pattern);
            int start = Math.max(0, from);
            check(kmp.countMatches(input, start, new M3DefaultProgressMonitor()) == expected.size(),
                    "direct count-only KMP vs oracle trial=" + trial);
            check(M3ByteOccurrences.count(kmp, input, from, null) == expected.size(),
                    "public count dispatch vs oracle trial=" + trial);
            List<Integer> visited = new ArrayList<>();
            check(M3ByteOccurrences.visit(kmp, input, from, Integer.MAX_VALUE,
                    visited::add, null) == expected.size(),
                    "existing visitor count vs oracle trial=" + trial);
            check(visited.equals(expected), "visit order/overlap trial=" + trial);
        }
        return checks;
    }

    public static void main(String[] args) {
        System.out.println("M3_KMP_COUNT_ONLY_ORACLE_PASS checks=" + runOracle());
    }
}
