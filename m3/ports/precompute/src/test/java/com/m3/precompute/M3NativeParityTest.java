// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class M3NativeParityTest {
    @Test
    void nativeLexicalAndSimilarityBatchesMatchCanonicalJava() {
        String path = System.getProperty("m3.precompute.native.path");
        String pathFile = System.getProperty("m3.precompute.native.pathFile");
        Assumptions.assumeTrue(
                (path != null && !path.isBlank()) || (pathFile != null && !pathFile.isBlank()),
                "native M3 precompute path not configured");

        List<String> values =
                List.of(
                        "",
                        "plain text",
                        "while (x != 0) { x &= x - 1; }",
                        "^[a-z]+\\d{2}$",
                        "x -> x + 1",
                        "Type::method",
                        "unicode\u1680\u2003\u2028\u3000space",
                        "x".repeat(1023) + "->tail",
                        "x".repeat(1023) + "::tail");

        Packed packed = pack(values);
        var nativeLexical = M3CodeTextSignalBatchNative.loadRequired();
        long[] javaPacked =
                M3CodeTextSignalBatchJava.INSTANCE.analyze(
                        packed.units(),
                        packed.offsets(),
                        packed.lengths(),
                        M3CodeTextSignalBatch.Limits.DEFAULT,
                        null);
        long[] nativePacked =
                nativeLexical.analyze(
                        packed.units(),
                        packed.offsets(),
                        packed.lengths(),
                        M3CodeTextSignalBatch.Limits.DEFAULT,
                        null);
        assertArrayEquals(javaPacked, nativePacked);

        int[] lengths = new int[values.size()];
        long[] hashes = new long[values.size()];
        M3TextSignals query = M3TextSignals.compile(values.get(2));
        for (int index = 0; index < values.size(); index++) {
            M3TextSignals signals = M3TextSignals.compile(values.get(index));
            lengths[index] = signals.metrics().utf16Length();
            hashes[index] = signals.simHash64();
        }
        int[] javaScores =
                M3SimilarityBatchJava.INSTANCE.scores(
                        query.metrics().utf16Length(),
                        query.simHash64(),
                        lengths,
                        hashes,
                        M3SimilarityBatch.Limits.DEFAULT,
                        null);
        int[] nativeScores =
                M3SimilarityBatchNative.loadRequired().scores(
                        query.metrics().utf16Length(),
                        query.simHash64(),
                        lengths,
                        hashes,
                        M3SimilarityBatch.Limits.DEFAULT,
                        null);
        assertArrayEquals(javaScores, nativeScores);
    }

    private static Packed pack(List<String> values) {
        ArrayList<Character> all = new ArrayList<>();
        int[] offsets = new int[values.size()];
        int[] lengths = new int[values.size()];
        for (int row = 0; row < values.size(); row++) {
            String value = values.get(row);
            offsets[row] = all.size();
            lengths[row] = value.length();
            for (int index = 0; index < value.length(); index++) all.add(value.charAt(index));
        }
        char[] units = new char[all.size()];
        for (int index = 0; index < units.length; index++) units[index] = all.get(index);
        return new Packed(units, offsets, lengths);
    }

    private record Packed(char[] units, int[] offsets, int[] lengths) {}
}
