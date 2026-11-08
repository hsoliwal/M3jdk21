// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.*;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** JDK 21 indexOf oracle for UTF-16 Z-search, including the separator in inputs. */
//FLLDCIM: RELOCATE - donor blob d4ea9270bc63 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class ZSearchFirstHitContractTest {
    private static final char[] ALPHABET = {'a', '$', 'b', '\0', '\ud800'};

    private static String word(int length, int digits) {
        char[] units = new char[length];
        for (int i = 0; i < length; i++) {
            units[i] = ALPHABET[digits % ALPHABET.length];
            digits /= ALPHABET.length;
        }
        return new String(units);
    }

    private static void match(String text, String pattern) {
        assertEquals(text.indexOf(pattern), StringAlgorithms.zSearch(text, pattern),
                "UTF-16 first occurrence in text=" + text.length() + " pattern=" + pattern.length());
    }

    @Test
    void separatorOverlapAndEmptyPatternCounterexamples() {
        match("ab$ab", "ab");
        match("ab$ab$ab", "ab");
        match("$", "");
        match("", "");
        match("", "a");
        match("ab$ab", "$");
        match("$aaa$", "aa");
        match("a\ud800b", "\ud800");
        match("\ud800\udc00$\ud800\udc00", "\ud800\udc00");
        assertThrows(NullPointerException.class, () -> StringAlgorithms.zSearch("abc", null));
    }

    @Test
    void exhaustiveFiveCharacterUnicodeUnitAlphabet() {
        for (int n = 0; n <= 5; n++) {
            int count = (int) Math.pow(ALPHABET.length, n);
            for (int t = 0; t < count; t++) {
                String text = word(n, t);
                for (int m = 0; m <= 4; m++) {
                    int patterns = (int) Math.pow(ALPHABET.length, m);
                    for (int p = 0; p < patterns; p++) match(text, word(m, p));
                }
            }
        }
    }

    @Test
    void seededHundredThousandCasesIncludingBorrowedSubstrings() {
        Random random = new Random(0x4d335a5345415243L);
        for (int trial = 0; trial < 100_000; trial++) {
            int n = random.nextInt(160);
            int m = random.nextInt(32);
            char[] text = new char[n];
            char[] pattern = new char[m];
            for (int i = 0; i < n; i++) text[i] = ALPHABET[random.nextInt(ALPHABET.length)];
            for (int i = 0; i < m; i++) pattern[i] = ALPHABET[random.nextInt(ALPHABET.length)];
            if (trial % 3 == 0 && n >= m && m > 0) {
                System.arraycopy(text, random.nextInt(n - m + 1), pattern, 0, m);
            }
            match(new String(text), new String(pattern));
        }
    }

    @Test
    void unchangedZArrayAndProgressCompatibilityBoundary() {
        assertArrayEquals(new int[] {0, 4, 3, 2, 1},
                StringAlgorithms.zFunction("aaaaa", ProgressMonitor.none()));
        assertArrayEquals(new int[0], StringAlgorithms.zFunction("", ProgressMonitor.none()));
        assertEquals(0, StringAlgorithms.zSearch("ab$ab", "ab", ProgressMonitor.none()));
        assertEquals(0, StringAlgorithms.zSearch("", "", ProgressMonitor.none()));
    }
}
