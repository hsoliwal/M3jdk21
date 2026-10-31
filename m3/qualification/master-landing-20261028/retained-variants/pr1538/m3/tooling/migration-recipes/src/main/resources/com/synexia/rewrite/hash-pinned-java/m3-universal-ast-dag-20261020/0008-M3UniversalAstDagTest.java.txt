// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class M3UniversalAstDagTest {
    private record Node(int type, long payload, long state, List<Node> children) { }

    private static final M3Ast.NodeAdapter<Node> ADAPTER = new M3Ast.NodeAdapter<>() {
        @Override public int type(Node node) { return node.type(); }
        @Override public long payloadId(Node node) { return node.payload(); }
        @Override public long metadataBits(Node node) { return node.state(); }
        @Override public List<Node> children(Node node) { return node.children(); }
    };

    @Test
    void structurallyEqualImmutableSubtreesCanonicalize() {
        Node leftLeaf = new Node(1, 100L, 0L, List.of());
        Node rightLeaf = new Node(1, 100L, 0L, List.of());
        Node root = new Node(
                2, 200L, 7L,
                List.of(
                        new Node(3, 300L, 0L, List.of(leftLeaf)),
                        new Node(3, 300L, 0L, List.of(rightLeaf))));

        M3Ast ast = M3Ast.compile(root, ADAPTER);
        int rootId = ast.root(0);
        int firstBranch = ast.childAt(rootId, 0);
        int secondBranch = ast.childAt(rootId, 1);

        assertEquals(firstBranch, secondBranch, "equal immutable branch canonicalization");
        assertEquals(3, ast.atoms().atomCount(), "leaf + branch + root only");
        assertEquals(5, ast.expandedSize(rootId), "tree occurrence count remains expanded");
        assertEquals(3, ast.depth(rootId));
    }

    @Test
    void payloadChangesContentHashButNotShapeHash() {
        M3Ast first = M3Ast.compile(
                new Node(1, 10L, 0L, List.of()), ADAPTER);
        M3Ast second = M3Ast.compile(
                new Node(1, 11L, 0L, List.of()), ADAPTER);

        assertNotEquals(first.contentHash(first.root(0)), second.contentHash(second.root(0)));
        assertEquals(first.structureHash(first.root(0)), second.structureHash(second.root(0)));
    }

    @Test
    void donorCycleFailsClosed() {
        final class Mutable {
            final List<Mutable> children = new java.util.ArrayList<>();
        }
        Mutable a = new Mutable();
        Mutable b = new Mutable();
        a.children.add(b);
        b.children.add(a);

        assertThrows(
                IllegalArgumentException.class,
                () -> M3Ast.compile(
                        a,
                        new M3Ast.NodeAdapter<Mutable>() {
                            public int type(Mutable ignored) { return 1; }
                            public List<Mutable> children(Mutable node) { return node.children; }
                        }));
    }

    @Test
    void astProjectsToTypedPrimitiveDag() {
        Node root = new Node(
                2, 20L, 0L,
                List.of(
                        new Node(1, 10L, 0L, List.of()),
                        new Node(1, 11L, 0L, List.of())));
        M3Ast ast = M3Ast.compile(root, ADAPTER);
        M3Dag dag = ast.asDag(7001L);

        assertEquals(ast.atoms().atomCount(), dag.nodeCount());
        assertEquals(2, dag.edgeCount());
        assertEquals(1, dag.roots().length);
        assertEquals(2, dag.leaves().length);
        assertEquals(M3InteractionKind.STRUCTURAL, dag.edge(0).interactionKind());
        assertTrue(dag.primitiveBytes() > 0L);
    }

    @Test
    void dagOrderingIsDeterministicAndCyclesRefuse() {
        M3AtomStore.Builder atoms = M3AtomStore.builder();
        int a = atoms.intern(M3AtomDomain.DAG, 1, 1L);
        int b = atoms.intern(M3AtomDomain.DAG, 1, 2L);
        int c = atoms.intern(M3AtomDomain.DAG, 1, 3L);
        M3AtomStore store = atoms.freeze();

        M3Dag first = new M3Dag.Builder(store)
                .edge(a, 100L, b)
                .edge(b, 101L, c)
                .freeze();
        M3Dag second = new M3Dag.Builder(store)
                .edge(b, 101L, c)
                .edge(a, 100L, b)
                .freeze();

        assertArrayEquals(first.topologicalOrder(), second.topologicalOrder());
        assertEquals(first.contentHash(), second.contentHash());

        M3Dag.Builder cycle = new M3Dag.Builder(store)
                .edge(a, 100L, b)
                .edge(b, 100L, a);
        assertThrows(IllegalArgumentException.class, cycle::freeze);
    }

    @Test
    void semanticInteractionMeaningBelongsToTheEdge() {
        assertEquals(
                M3InteractionKind.EVIDENCE,
                M3Interaction.classify(
                        M3AtomDomain.STRING, M3AtomDomain.FACT, M3InteractionMode.CONNECT));
        assertEquals(
                M3InteractionKind.LOGIC,
                M3Interaction.classify(
                        M3AtomDomain.STRING, M3AtomDomain.LOGIC, M3InteractionMode.DERIVE));
        assertEquals(
                M3InteractionKind.REASON,
                M3Interaction.classify(
                        M3AtomDomain.STRING, M3AtomDomain.REASON, M3InteractionMode.CONNECT));
        assertEquals(
                M3InteractionKind.ARGUMENT,
                M3Interaction.classify(
                        M3AtomDomain.STRING, M3AtomDomain.STRING, M3InteractionMode.OPPOSE));
        assertEquals(
                M3InteractionKind.BRAINSTORM,
                M3Interaction.classify(
                        M3AtomDomain.STRING, M3AtomDomain.STRING, M3InteractionMode.EXPLORE));
        assertEquals(
                M3InteractionKind.CONTEXT_ACTION,
                M3Interaction.classify(
                        M3AtomDomain.STRING, M3AtomDomain.CONTEXT, M3InteractionMode.APPLY));
    }
}
