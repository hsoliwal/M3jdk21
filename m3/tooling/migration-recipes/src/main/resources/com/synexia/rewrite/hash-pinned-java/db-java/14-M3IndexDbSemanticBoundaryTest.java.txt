// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** ContractProbe for semantic identity, role composition and candidate-only query boundaries. */
final class M3IndexDbSemanticBoundaryTest {
    private static final String ZERO = "0".repeat(64);
    private static final String ONE = "1".repeat(64);

    @Test
    void emptyCompositionsAreDeterministicAndQueriesAreTotalForAbsentValues() {
        var fingerprint = M3IndexDbSemanticFingerprint.leaf(" EMPTY ", List.of(), List.of(), "");
        var composed = M3IndexDbSemanticFingerprint.compose("EMPTY", List.of());
        assertEquals(composed, M3IndexDbSemanticFingerprint.compose("EMPTY", List.of()));
        assertEquals("EMPTY|", fingerprint.normalizedComposition());
        var empty = M3IndexDbSemanticIndex.of(List.of(), List.of());
        assertTrue(empty.nodes().isEmpty());
        assertTrue(empty.edges().isEmpty());
        assertTrue(empty.exactSha256(ZERO).isEmpty());
        assertTrue(empty.structuralSha256(ZERO).isEmpty());
        assertTrue(empty.logicSha256(ZERO).isEmpty());
        assertTrue(empty.structuralHash64(0).isEmpty());
        assertTrue(empty.logicHash64(0).isEmpty());
        assertTrue(empty.patternRole(" unknown ").isEmpty());
        assertTrue(empty.nearSimHash(0, 64).isEmpty());
        assertArrayEquals(empty.encode(), M3IndexDbSemanticIndex.decode(empty.encode()).encode());
    }

    @Test
    void identityConstructorsRejectEveryInvalidTokenPosition() {
        var fingerprint = M3IndexDbSemanticFingerprint.leaf("ATOM", List.of(), List.of(), "x");
        for (String invalid : List.of("", " ", "a\0b")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new M3IndexDbSemanticFingerprint.Component(invalid, fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> M3IndexDbSemanticFingerprint.leaf(invalid, List.of(), List.of(), ""));
            assertThrows(IllegalArgumentException.class,
                    () -> new M3IndexDbSemanticNode(ZERO, M3IndexDbSemanticKind.ATOM,
                            invalid, "", "symbol", "role", fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> new M3IndexDbSemanticNode(ZERO, M3IndexDbSemanticKind.ATOM,
                            "key", "", invalid, "role", fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> new M3IndexDbSemanticNode(ZERO, M3IndexDbSemanticKind.ATOM,
                            "key", "", "symbol", invalid, fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> new M3IndexDbSemanticEdge(ZERO, ONE, invalid, 0));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new M3IndexDbSemanticNode("bad", M3IndexDbSemanticKind.ATOM,
                        "key", "", "s", "r", fingerprint));
        assertThrows(IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge("bad", ONE, "ROLE", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new M3IndexDbSemanticEdge(ZERO, "bad", "ROLE", 0));
        assertThrows(NullPointerException.class,
                () -> new M3IndexDbSemanticFingerprint.Component("ROLE", null));
        assertThrows(NullPointerException.class,
                () -> M3IndexDbSemanticFingerprint.utf16Sha256(null));
    }

    @Test
    void absentAndMalformedQueryInputsAreNotInventedMatches() {
        var node = node("key", M3IndexDbSemanticKind.ATOM);
        var index = M3IndexDbSemanticIndex.of(List.of(node, node), List.of());
        assertEquals(1, index.nodes().size());
        assertEquals(List.of(node), index.patternRole(" ContractProbe "));
        assertThrows(IllegalArgumentException.class, () -> index.patternRole(" "));
        assertThrows(IllegalArgumentException.class, () -> index.structuralSha256("BAD"));
        assertThrows(IllegalArgumentException.class, () -> index.logicSha256("BAD"));
        assertThrows(NullPointerException.class, () -> index.find(null));
        assertThrows(NullPointerException.class, () -> index.nodesOfKind(null));
        assertThrows(NullPointerException.class, () -> index.children(null));
        assertThrows(NullPointerException.class, () -> index.parents(null));
        assertThrows(NullPointerException.class, () -> index.store(null, "name"));
        assertThrows(NullPointerException.class, () -> M3IndexDbSemanticIndex.load(null, "name"));
        assertThrows(IllegalArgumentException.class, () -> new M3IndexDbSemanticIndex.Similarity(node, -1));
        assertThrows(IllegalArgumentException.class, () -> new M3IndexDbSemanticIndex.Similarity(node, 65));
        assertThrows(NullPointerException.class, () -> new M3IndexDbSemanticIndex.Similarity(null, 0));
        assertEquals(64, new M3IndexDbSemanticIndex.Similarity(node, 64).hammingDistance());
    }

    @Test
    void explicitOrderedRolesAndSharedParentsRoundTrip() {
        var a = node("a", M3IndexDbSemanticKind.ATOM);
        var b = node("b", M3IndexDbSemanticKind.ATOM);
        var p = node("p", M3IndexDbSemanticKind.METHOD);
        var q = node("q", M3IndexDbSemanticKind.METHOD);
        var edge = new M3IndexDbSemanticEdge(p.nodeId(), a.nodeId(), "ORDERED:BODY", 1);
        var index = M3IndexDbSemanticIndex.of(List.of(q, a, p, b), List.of(edge, edge,
                new M3IndexDbSemanticEdge(p.nodeId(), b.nodeId(), "ORDERED:BODY", 2),
                new M3IndexDbSemanticEdge(q.nodeId(), a.nodeId(), "MEMBER", 0)));
        assertEquals(3, index.edges().size());
        assertEquals(List.of(a, b), index.children(p.nodeId()));
        assertEquals(List.of(index.require(p.nodeId()), index.require(q.nodeId())), index.parents(a.nodeId()));
        assertArrayEquals(index.encode(), M3IndexDbSemanticIndex.decode(index.encode()).encode());
        assertThrows(IllegalArgumentException.class, () -> M3IndexDbSemanticIndex.of(List.of(a),
                List.of(new M3IndexDbSemanticEdge(ZERO, a.nodeId(), "MEMBER", 0))));
    }

    private static M3IndexDbSemanticNode node(String key, M3IndexDbSemanticKind kind) {
        return new M3IndexDbSemanticNode(M3IndexDbSemanticIndex.nodeId(kind, key), kind,
                key, "src/Probe.java", key, "ContractProbe",
                M3IndexDbSemanticFingerprint.leaf(kind.name(), List.of("node"), List.of(key), key));
    }
}
