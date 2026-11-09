// SPDX-License-Identifier: Apache-2.0
package jdk.m3.lexicon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent test-only C17/JNI scalar and Liang-pattern differential proof. */
public final class M3CaseLiangJniParityProof {
    private M3CaseLiangJniParityProof() { }
    private static native boolean nativeBoundary(int kind, int before, int at, int after);
    private static native int[] nativeCuts(String word, String[] patterns, int leftMin, int rightMin);

    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("JNI absolute path required");
        System.load(args[0]);
        long checks = 0;
        for (M3SeparatorKind kind : List.of(M3SeparatorKind.CAMEL, M3SeparatorKind.PASCAL)) {
            int mode = kind == M3SeparatorKind.CAMEL ? 0 : 1;
            for (int before = -1; before <= 30; before++) {
                for (int at = -1; at <= 30; at++) {
                    for (int after = -1; after <= 30; after++) {
                        if (nativeBoundary(mode, before, at, after)
                                != M3WindowBoundarySummary.boundary(kind, before, at, after)) {
                            throw new AssertionError("C17/Java case transition mismatch");
                        }
                        checks++;
                    }
                }
            }
        }
        Random random = new Random(0x12712026L);
        for (int sample = 0; sample < 5_000; sample++) {
            int n = random.nextInt(26);
            StringBuilder word = new StringBuilder(n);
            for (int j = 0; j < n; j++) word.append((char) ('a' + random.nextInt(4)));
            List<String> patterns = new ArrayList<>();
            for (int i = 0, count = 1 + random.nextInt(10); i < count; i++) {
                StringBuilder pattern = new StringBuilder();
                if (random.nextInt(3) == 0) pattern.append('.');
                for (int j = 0, length = 1 + random.nextInt(5); j < length; j++) {
                    if (random.nextBoolean()) pattern.append((char) ('0' + random.nextInt(10)));
                    pattern.append((char) ('a' + random.nextInt(4)));
                }
                if (random.nextBoolean()) pattern.append((char) ('0' + random.nextInt(10)));
                if (random.nextInt(3) == 0) pattern.append('.');
                patterns.add(pattern.toString());
            }
            int left = 1 + random.nextInt(3), right = 1 + random.nextInt(3);
            long[] expected = M3LiangPatternPrecompute.compile("en", patterns, left, right)
                    .candidateBits(word);
            int[] actual = nativeCuts(word.toString(), patterns.toArray(String[]::new), left, right);
            if (actual == null || actual.length != n + 1) throw new AssertionError("JNI geometry mismatch");
            for (int i = 0; i <= n; i++) {
                if (M3BitLanes.get(expected, i) != (actual[i] != 0)) {
                    throw new AssertionError("C17/Java weighted-score mismatch");
                }
                checks++;
            }
        }
        System.out.println("M3_CASE_LIANG_JNI_PASS checks=" + checks);
    }
}
