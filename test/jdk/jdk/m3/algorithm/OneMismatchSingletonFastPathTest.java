// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

/** Fixed Hamming-1 single UTF-16-unit geometry; no native or interface change. */
//FLLDCIM: RELOCATE - donor blob 5bdd8b499629 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class OneMismatchSingletonFastPathTest {
    @Test
    void nonemptyTextAndOneUnitNeedleAdmitEveryPosition() {
        assertArrayEquals(new int[0], StringAlgorithms.oneMismatchAllIndices("", "x"));
        assertEquals(-1, StringAlgorithms.oneMismatchFirstIndex("", "x"));
        assertArrayEquals(new int[] {0}, StringAlgorithms.oneMismatchAllIndices("a", "b"));
        assertEquals(0, StringAlgorithms.oneMismatchFirstIndex("a", "b"));
        int[] expected = new int[65_536];
        for (int i = 0; i < expected.length; i++) expected[i] = i;
        assertArrayEquals(expected,
                StringAlgorithms.oneMismatchAllIndices("x".repeat(expected.length), "z"));
    }

    @Test
    void isolatedSurrogatesAndNulAreRawUtf16Units() {
        String text = "\u0000\ud800\udfff\uffff$z";
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5},
                StringAlgorithms.oneMismatchAllIndices(text, "\ud800"));
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5},
                StringAlgorithms.oneMismatchAllIndices(text, "\u0000"));
        assertEquals(0, StringAlgorithms.oneMismatchFirstIndex(text, "\udfff"));
    }

    @Test
    void randomizedSingletonResultsAgreeWithDirectHammingOracle() {
        Random random = new Random(0x53494e474c45544fL);
        for (int trial = 0; trial < 4_000; trial++) {
            char[] units = new char[random.nextInt(128)];
            for (int i = 0; i < units.length; i++) {
                units[i] = switch (random.nextInt(6)) {
                    case 0 -> '\u0000';
                    case 1 -> '\ud800';
                    case 2 -> '\udfff';
                    case 3 -> '\uffff';
                    default -> (char) random.nextInt(Character.MAX_VALUE + 1);
                };
            }
            String text = new String(units);
            String needle = String.valueOf((char) random.nextInt(Character.MAX_VALUE + 1));
            int[] expected = new int[text.length()];
            for (int i = 0; i < expected.length; i++) expected[i] = i;
            assertArrayEquals(expected, StringAlgorithms.oneMismatchAllIndices(text, needle),
                    "trial=" + trial);
            assertEquals(text.isEmpty() ? -1 : 0,
                    StringAlgorithms.oneMismatchFirstIndex(text, needle), "trial=" + trial);
        }
    }

    @Test
    void explicitMonitorRetainsOriginalSearchPathAndResults() {
        ProgressMonitor monitor = ProgressMonitor.none();
        String text = "\u0000\ud800$\udfffzy";
        int[] expected = {0, 1, 2, 3, 4, 5};
        assertArrayEquals(expected,
                StringAlgorithms.oneMismatchAllIndices(text, "x", monitor));
        assertEquals(0, StringAlgorithms.oneMismatchFirstIndex(text, "x", monitor));
    }

    @Test
    void nullAndEmptyContractsAreUnchanged() {
        assertThrows(NullPointerException.class,
                () -> StringAlgorithms.oneMismatchAllIndices(null, "a"));
        assertThrows(NullPointerException.class,
                () -> StringAlgorithms.oneMismatchFirstIndex("a", null));
        assertThrows(IllegalArgumentException.class,
                () -> StringAlgorithms.oneMismatchAllIndices("a", ""));
        assertArrayEquals(new int[0],
                StringAlgorithms.oneMismatchAllIndices("x", "much-longer"));
    }

    @Test
    void interruptionRemainsObservableAndReturnedArraysAreIndependent() {
        boolean interrupted = false;
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class,
                    () -> StringAlgorithms.oneMismatchAllIndices("abc", "a"));
            interrupted = true;
        } finally {
            Thread.interrupted();
        }
        assertEquals(1, interrupted ? 1 : 0);
        int[] first = StringAlgorithms.oneMismatchAllIndices("abc", "a");
        first[0] = 999;
        assertArrayEquals(new int[] {0, 1, 2},
                StringAlgorithms.oneMismatchAllIndices("abc", "a"));
    }
}
