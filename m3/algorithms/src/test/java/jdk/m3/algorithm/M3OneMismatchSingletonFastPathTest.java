// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

/** Fixed Hamming-1 single UTF-16-unit geometry; no native or interface change. */
//FLLDCIM: RELOCATE - donor blob 5bdd8b499629 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
final class M3OneMismatchSingletonFastPathTest {
    @Test
    void nonemptyTextAndOneUnitNeedleAdmitEveryPosition() {
        assertArrayEquals(new int[0], M3OneMismatchWindowSearch.allIndices("", "x"));
        assertEquals(-1, M3OneMismatchWindowSearch.firstIndex("", "x"));
        assertArrayEquals(new int[] {0}, M3OneMismatchWindowSearch.allIndices("a", "b"));
        assertEquals(0, M3OneMismatchWindowSearch.firstIndex("a", "b"));
        int[] expected = new int[65_536];
        for (int i = 0; i < expected.length; i++) expected[i] = i;
        assertArrayEquals(expected,
                M3OneMismatchWindowSearch.allIndices("x".repeat(expected.length), "z"));
    }

    @Test
    void isolatedSurrogatesAndNulAreRawUtf16Units() {
        String text = "\u0000\ud800\udfff\uffff$z";
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5},
                M3OneMismatchWindowSearch.allIndices(text, "\ud800"));
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5},
                M3OneMismatchWindowSearch.allIndices(text, "\u0000"));
        assertEquals(0, M3OneMismatchWindowSearch.firstIndex(text, "\udfff"));
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
            assertArrayEquals(expected, M3OneMismatchWindowSearch.allIndices(text, needle),
                    "trial=" + trial);
            assertEquals(text.isEmpty() ? -1 : 0,
                    M3OneMismatchWindowSearch.firstIndex(text, needle), "trial=" + trial);
        }
    }

    @Test
    void explicitMonitorRetainsOriginalSearchPathAndResults() {
        M3ProgressMonitor monitor = M3ProgressMonitor.none();
        String text = "\u0000\ud800$\udfffzy";
        int[] expected = {0, 1, 2, 3, 4, 5};
        assertArrayEquals(expected,
                M3OneMismatchWindowSearch.allIndices(text, "x", monitor));
        assertEquals(0, M3OneMismatchWindowSearch.firstIndex(text, "x", monitor));
    }

    @Test
    void nullAndEmptyContractsAreUnchanged() {
        assertThrows(NullPointerException.class,
                () -> M3OneMismatchWindowSearch.allIndices(null, "a"));
        assertThrows(NullPointerException.class,
                () -> M3OneMismatchWindowSearch.firstIndex("a", null));
        assertThrows(IllegalArgumentException.class,
                () -> M3OneMismatchWindowSearch.allIndices("a", ""));
        assertArrayEquals(new int[0],
                M3OneMismatchWindowSearch.allIndices("x", "much-longer"));
    }

    @Test
    void interruptionRemainsObservableAndReturnedArraysAreIndependent() {
        boolean interrupted = false;
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class,
                    () -> M3OneMismatchWindowSearch.allIndices("abc", "a"));
            interrupted = true;
        } finally {
            Thread.interrupted();
        }
        assertEquals(1, interrupted ? 1 : 0);
        int[] first = M3OneMismatchWindowSearch.allIndices("abc", "a");
        first[0] = 999;
        assertArrayEquals(new int[] {0, 1, 2},
                M3OneMismatchWindowSearch.allIndices("abc", "a"));
    }
}
