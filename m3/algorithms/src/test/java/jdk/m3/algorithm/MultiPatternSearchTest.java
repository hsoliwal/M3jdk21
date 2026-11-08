// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob 04d6812ddbf6 at commit a7b7bb639802 (repository, path, URL and digests: m3/algorithms/recipe/corpus-search-20261108/PROVENANCE.tsv); recipe m3/algorithms/recipe/corpus-search-20261108/relocate.py v1
class MultiPatternSearchTest {

    @Test
    void classicOverlappingPatternsHaveFirstOccurrenceSemantics() {
        var prepared =
                MultiPatternSearch.prepare(
                        List.of(
                                new byte[0],
                                bytes("he"),
                                bytes("she"),
                                bytes("hers"),
                                bytes("his"),
                                bytes("he")));
        assertArrayEquals(
                new int[] {0, 4, 3, 4, 1, 4},
                MultiPatternSearch.firstOffsetsJava(
                        prepared, bytes("ahishers")));
        assertEquals(6, prepared.patternCount());
        assertArrayEquals(bytes("she"), prepared.pattern(2));
    }

    @Test
    void preparedAhoTrieExposesExactAndPrefixMultiplicityWithoutAnotherTree() {
        var prepared =
                MultiPatternSearch.prepare(
                        List.of(
                                new byte[0],
                                bytes("he"),
                                bytes("she"),
                                bytes("hers"),
                                bytes("his"),
                                bytes("he")));

        assertEquals(6, prepared.prefixCount(new byte[0]));
        assertEquals(4, prepared.prefixCount(bytes("h")));
        assertEquals(3, prepared.prefixCount(bytes("he")));
        assertEquals(1, prepared.prefixCount(bytes("s")));
        assertEquals(0, prepared.prefixCount(bytes("x")));

        assertEquals(1, prepared.exactCount(new byte[0]));
        assertEquals(2, prepared.exactCount(bytes("he")));
        assertEquals(1, prepared.exactCount(bytes("hers")));
        assertEquals(0, prepared.exactCount(bytes("her")));

        assertTrue(prepared.startsWith(new byte[0]));
        assertTrue(prepared.startsWith(bytes("hi")));
        assertFalse(prepared.startsWith(bytes("hero")));
        assertTrue(prepared.containsExact(bytes("he")));
        assertFalse(prepared.containsExact(bytes("her")));

        assertArrayEquals(
                new int[] {0, 4, 3, 4, 1, 4},
                MultiPatternSearch.firstOffsetsJava(
                        prepared, bytes("ahishers")));
    }

    @Test
    void emptyPreparedTrieHasNoMembershipAndPrefixQueriesHonorCancellation() {
        var empty = MultiPatternSearch.prepare(List.of());
        assertEquals(0, empty.prefixCount(new byte[0]));
        assertFalse(empty.startsWith(new byte[0]));
        assertFalse(empty.containsExact(new byte[0]));

        var prepared = MultiPatternSearch.prepare(List.of(bytes("alpha")));
        var monitor = new CancelableProgressMonitor();
        monitor.setCanceled(true);
        assertThrows(
                java.util.concurrent.CancellationException.class,
                () ->
                        MultiPatternSearch.prefixCount(
                                prepared, bytes("a"), monitor));
    }

    @Test
    void preparationOwnsInputsAndSupportsBinaryBytes() {
        byte[] mutable = {(byte) 0xff, 0, 1};
        var prepared =
                MultiPatternSearch.prepare(
                        List.of(mutable, new byte[] {0, 1}, new byte[] {(byte) 0xff}));
        mutable[0] = 0;
        assertArrayEquals(
                new int[] {1, 2, 1},
                MultiPatternSearch.firstOffsetsJava(
                        prepared, new byte[] {4, (byte) 0xff, 0, 1, 8}));
        assertArrayEquals(new byte[] {(byte) 0xff, 0, 1}, prepared.pattern(0));
    }

    @Test
    void preparationAndScanEnforceBudgetsAndCancellation() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        MultiPatternSearch.prepare(
                                java.util.Collections.nCopies(
                                        MultiPatternSearch.MAX_PATTERNS + 1,
                                        new byte[] {1})));
        byte[] tooLarge = new byte[MultiPatternSearch.MAX_TOTAL_PATTERN_BYTES + 1];
        assertThrows(
                IllegalArgumentException.class,
                () -> MultiPatternSearch.prepare(List.of(tooLarge)));

        var monitor = new CancelableProgressMonitor();
        monitor.setCanceled(true);
        assertThrows(
                java.util.concurrent.CancellationException.class,
                () -> MultiPatternSearch.prepare(List.of(bytes("a")), monitor));

        var prepared = MultiPatternSearch.prepare(List.of(bytes("a")));
        assertThrows(
                java.util.concurrent.CancellationException.class,
                () -> MultiPatternSearch.firstOffsets(prepared, bytes("aaaa"), monitor));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
