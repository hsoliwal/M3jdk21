// SPDX-License-Identifier: Apache-2.0
package com.m3.affix;

import java.util.Arrays;
import java.util.Random;

/** Diagnostic workload only; never turn this stock-Java21 fixture into a JVM speedup claim. */
public final class AffixRankBeforeAfter {
    private static volatile long observed;

    private static long run(M3StringRegexRowPrecompute.Prepared plan, int loops) {
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < loops; i++) {
            int[] rows = plan.candidateRows();
            sum += rows.length;
            if (rows.length > 0) sum += rows[0];
        }
        observed = sum;
        return System.nanoTime() - start;
    }

    public static void main(String[] args) {
        Random random = new Random(0x4d334146464958L);
        M3String[] data = new M3String[4_096];
        String chars = "abcXYZ0189_";
        for (int i = 0; i < data.length; i++) {
            StringBuilder text = new StringBuilder(12);
            if (i % 43 == 0) text.append("ab");
            for (int j = 0; j < 10; j++) text.append(chars.charAt(random.nextInt(chars.length())));
            data[i] = new M3String(text.toString());
        }
        M3StringRegexRowPrecompute before = M3StringRegexRowPrecompute.compile(data);
        M3StringRegexRowPrecompute after = M3StringRegexRowPrecompute.compile(data);
        M3StringRegexRowPrecompute.Prepared slow = before.prepare("ab.*", 0);
        for (int i = 0; i < 4; i++) after.prepare("ab.*", 0);
        M3StringRegexRowPrecompute.Prepared fast = after.prepare("ab.*", 0);
        if (before.affixesForProof() != null || after.affixesForProof() == null) {
            throw new AssertionError("cold/eligible ranked route not established");
        }
        int[] expected = slow.candidateRows(), actual = fast.candidateRows();
        if (!Arrays.equals(expected, actual) || !fast.indexedForProof()) {
            throw new AssertionError("ranked route disagrees with baseline candidate set");
        }
        long slowNs = Long.MAX_VALUE, fastNs = Long.MAX_VALUE;
        for (int trial = 0; trial < 5; trial++) {
            run(slow, 600);
            run(fast, 600);
            slowNs = Math.min(slowNs, run(slow, 2_000));
            fastNs = Math.min(fastNs, run(fast, 2_000));
        }
        System.out.printf("M3_AFFIX_AB_DIAGNOSTIC rows=%d candidates=%d "
                        + "baseBestNs=%d rankedBestNs=%d relative=%.2f "
                        + "fixture=stock_java21_not_m3jdk%n",
                data.length, expected.length, slowNs, fastNs,
                fastNs == 0 ? 0.0 : (double) slowNs / fastNs);
    }
}
