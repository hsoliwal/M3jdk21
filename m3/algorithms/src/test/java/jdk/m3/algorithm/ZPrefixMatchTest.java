// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.algorithm.ProgressMonitor;
import java.time.Instant;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob bac5312e1e67 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class ZPrefixMatchTest {

    @Test
    void prefixGeometryMatchesNaiveLcpAtEveryTextPosition() {
        Random random = new Random(0x5a5052454649584cL);
        for (int trial = 0; trial < 2_000; trial++) {
            String text = randomUtf16(random, random.nextInt(96));
            String pattern = randomUtf16(random, random.nextInt(24));
            int[] actual = ZPrefixMatch.prefixMatchLengths(text, pattern);
            assertEquals(text.length(), actual.length);
            for (int start = 0; start < text.length(); start++) {
                int expected = 0;
                while (expected < pattern.length()
                        && start + expected < text.length()
                        && pattern.charAt(expected) == text.charAt(start + expected)) {
                    expected++;
                }
                assertEquals(expected, actual[start], "trial=" + trial + ", start=" + start);
            }
        }
    }

    private static String randomUtf16(Random random, int length) {
        char[] value = new char[length];
        for (int index = 0; index < length; index++) {
            value[index] = switch (index & 15) {
                case 0 -> 0;
                case 1 -> '$';
                case 2 -> '\ud800';
                case 3 -> '\udfff';
                default -> (char) random.nextInt(Character.MAX_VALUE + 1);
            };
        }
        return new String(value);
    }

}
