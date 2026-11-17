// SPDX-License-Identifier: Apache-2.0
package jdk.m3.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** ContractProbe for semantic identity, role composition and candidate-only query boundaries. */
final class SemanticBoundaryTest {
    private static final String ZERO = "0".repeat(64);
    private static final String ONE = "1".repeat(64);

    @Test
    void emptyCompositionsAreDeterministicAndQueriesAreTotalForAbsentValues() {
        var fingerprint = SemanticFingerprint.leaf(" EMPTY ", List.of(), List.of(), "");
        var composed = SemanticFingerprint.compose("EMPTY", List.of());
        assertEquals(composed, SemanticFingerprint.compose("EMPTY", List.of()));
        assertEquals("EMPTY|", fingerprint.normalizedComposition());
        var empty = SemanticIndex.of(List.of(), List.of());
        assertTrue(empty.nodes().isEmpty());
        assertTrue(empty.edges().isEmpty());
        assertTrue(empty.exactSha256(ZERO).isEmpty());
        assertTrue(empty.structuralSha256(ZERO).isEmpty());
        assertTrue(empty.logicSha256(ZERO).isEmpty());
        assertTrue(empty.structuralHash64(0).isEmpty());
        assertTrue(empty.logicHash64(0).isEmpty());
        assertTrue(empty.patternRole(" unknown ").isEmpty());
        assertTrue(empty.nearSimHash(0, 64).isEmpty());
        assertArrayEquals(empty.encode(), SemanticIndex.decode(empty.encode()).encode());
    }

    @Test
    void identityConstructorsRejectEveryInvalidTokenPosition() {
        var fingerprint = SemanticFingerprint.leaf("ATOM", List.of(), List.of(), "x");
        for (String invalid : List.of("", " ", "a\0b")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SemanticFingerprint.Component(invalid, fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> SemanticFingerprint.leaf(invalid, List.of(), List.of(), ""));
            assertThrows(IllegalArgumentException.class,
                    () -> new SemanticNode(ZERO, SemanticKind.ATOM,
                            invalid, "", "symbol", "role", fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> new SemanticNode(ZERO, SemanticKind.ATOM,
                            "key", "", invalid, "role", fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> new SemanticNode(ZERO, SemanticKind.ATOM,
                            "key", "", "symbol", invalid, fingerprint));
            assertThrows(IllegalArgumentException.class,
                    () -> new SemanticEdge(ZERO, ONE, invalid, 0));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new SemanticNode("bad", SemanticKind.ATOM,
                        "key", "", "s", "r", fingerprint));
        assertThrows(IllegalArgumentException.class,
                () -> new SemanticEdge("bad", ONE, "ROLE", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SemanticEdge(ZERO, "bad", "ROLE", 0));
        assertThrows(NullPointerException.class,
                () -> new SemanticFingerprint.Component("ROLE", null));
        assertThrows(NullPointerException.class,
                () -> SemanticFingerprint.utf16Sha256(null));
    }

    @Test
    void absentAndMalformedQueryInputsAreNotInventedMatches() {
        var node = node("key", SemanticKind.ATOM);
        var index = SemanticIndex.of(List.of(node, node), List.of());
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
        assertThrows(NullPointerException.class, () -> SemanticIndex.load(null, "name"));
        assertThrows(IllegalArgumentException.class, () -> new SemanticIndex.Similarity(node, -1));
        assertThrows(IllegalArgumentException.class, () -> new SemanticIndex.Similarity(node, 65));
        assertThrows(NullPointerException.class, () -> new SemanticIndex.Similarity(null, 0));
        assertEquals(64, new SemanticIndex.Similarity(node, 64).hammingDistance());
    }

    @Test
    void explicitOrderedRolesAndSharedParentsRoundTrip() {
        var a = node("a", SemanticKind.ATOM);
        var b = node("b", SemanticKind.ATOM);
        var p = node("p", SemanticKind.METHOD);
        var q = node("q", SemanticKind.METHOD);
        var edge = new SemanticEdge(p.nodeId(), a.nodeId(), "ORDERED:BODY", 1);
        var index = SemanticIndex.of(List.of(q, a, p, b), List.of(edge, edge,
                new SemanticEdge(p.nodeId(), b.nodeId(), "ORDERED:BODY", 2),
                new SemanticEdge(q.nodeId(), a.nodeId(), "MEMBER", 0)));
        assertEquals(3, index.edges().size());
        assertEquals(List.of(a, b), index.children(p.nodeId()));
        assertEquals(List.of(index.require(p.nodeId()), index.require(q.nodeId())), index.parents(a.nodeId()));
        assertArrayEquals(index.encode(), SemanticIndex.decode(index.encode()).encode());
        assertThrows(IllegalArgumentException.class, () -> SemanticIndex.of(List.of(a),
                List.of(new SemanticEdge(ZERO, a.nodeId(), "MEMBER", 0))));
    }

    private static SemanticNode node(String key, SemanticKind kind) {
        return new SemanticNode(SemanticIndex.nodeId(kind, key), kind,
                key, "src/Probe.java", key, "ContractProbe",
                SemanticFingerprint.leaf(kind.name(), List.of("node"), List.of(key), key));
    }
}
