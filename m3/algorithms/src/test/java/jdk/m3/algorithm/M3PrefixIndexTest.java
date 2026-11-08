// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob c4147fbf5ed2 at commit a7b7bb639802 (repository, path, URL and digests: m3/algorithms/recipe/corpus-search-20261108/PROVENANCE.tsv); recipe m3/algorithms/recipe/corpus-search-20261108/relocate.py v1
final class M3PrefixIndexTest {
    @Test
    void facadeReusesTheSinglePreparedAhoOwnerAndRetainsMultiplicity() {
        M3PrefixIndex index =
                M3PrefixIndex.prepare(
                        List.of(
                                bytes("car"),
                                bytes("cat"),
                                bytes("car"),
                                bytes("cart"),
                                bytes("dog"),
                                new byte[0]));

        assertEquals(6, index.size());
        assertEquals(6, index.prefixCount(new byte[0]));
        assertEquals(4, index.prefixCount(bytes("ca")));
        assertEquals(3, index.prefixCount(bytes("car")));
        assertEquals(1, index.prefixCount(bytes("cart")));
        assertEquals(0, index.prefixCount(bytes("cow")));

        assertEquals(2, index.exactCount(bytes("car")));
        assertEquals(1, index.exactCount(bytes("cat")));
        assertEquals(1, index.exactCount(new byte[0]));
        assertEquals(0, index.exactCount(bytes("ca")));

        assertTrue(index.contains(bytes("car")));
        assertFalse(index.contains(bytes("ca")));
        assertTrue(index.startsWith(bytes("ca")));
        assertFalse(index.startsWith(bytes("cow")));

        assertSame(
                index.preparedAutomaton(),
                index.preparedAutomaton(),
                "the facade must not synthesize a second trie owner");
    }

    @Test
    void preparationOwnsInputBytesAndQueriesDoNotMutateThem() {
        byte[] mutable = bytes("alpha");
        M3PrefixIndex index =
                M3PrefixIndex.prepare(List.of(mutable, bytes("alpine")));
        mutable[0] = 'x';

        byte[] prefix = bytes("al");
        assertEquals(2, index.prefixCount(prefix));
        assertEquals('a', prefix[0]);

        byte[] exact = bytes("alpha");
        assertTrue(index.contains(exact));
        assertEquals(
                1,
                index.exactCount(exact, M3ProgressMonitor.none()));
        assertEquals('a', exact[0]);
    }

    @Test
    void cancellationAndNullContractsReuseAhoBoundary() {
        var monitor = new M3CancelableProgressMonitor();
        monitor.setCanceled(true);

        assertThrows(
                java.util.concurrent.CancellationException.class,
                () -> M3PrefixIndex.prepare(List.of(bytes("alpha")), monitor));

        M3PrefixIndex index =
                M3PrefixIndex.prepare(List.of(bytes("alpha")));
        assertThrows(NullPointerException.class, () -> index.prefixCount(null));
        assertThrows(
                java.util.concurrent.CancellationException.class,
                () -> index.prefixCount(bytes("a"), monitor));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
