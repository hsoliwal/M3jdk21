// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.List;

/** Dependency-free Java 21 qualification entrypoint for the shared atom/AST/DAG mechanics. */
public final class M3UniversalAstDagSelfTest {
    private static long checks;

    private M3UniversalAstDagSelfTest() { }

    private record Node(int type, long payload, long state, List<Node> children) { }

    private static final M3Ast.NodeAdapter<Node> ADAPTER = new M3Ast.NodeAdapter<>() {
        @Override public int type(Node node) { return node.type(); }
        @Override public long payloadId(Node node) { return node.payload(); }
        @Override public long metadataBits(Node node) { return node.state(); }
        @Override public List<Node> children(Node node) { return node.children(); }
    };

    public static void main(String[] args) {
        canonicalSubtrees();
        deterministicDag();
        interactionMatrix();
        randomizedTrees();
        System.out.println("M3_UNIVERSAL_AST_DAG_PASS checks=" + checks);
    }

    private static void canonicalSubtrees() {
        Node leafA = new Node(1, 101L, 0L, List.of());
        Node leafB = new Node(1, 101L, 0L, List.of());
        Node branchA = new Node(2, 202L, 3L, List.of(leafA));
        Node branchB = new Node(2, 202L, 3L, List.of(leafB));
        M3Ast ast = M3Ast.compile(
                new Node(3, 303L, 7L, List.of(branchA, branchB)), ADAPTER);
        int root = ast.root(0);
        equal(ast.childAt(root, 0), ast.childAt(root, 1), "shared branch");
        equal(3, ast.atoms().atomCount(), "canonical rows");
        equal(5, ast.expandedSize(root), "expanded occurrences");
        equal(3, ast.depth(root), "depth");
        M3Dag dag = ast.asDag(99L);
        equal(3, dag.edgeCount(), "AST edges");
        int branch = ast.childAt(root, 0);
        equal(1, dag.outgoingCount(branch), "shared branch outgoing count");
        equal(2, dag.outgoingCount(root), "root ordered outgoing count");
        M3Dag.EdgeView branchEdge = dag.outgoingEdge(branch, 0);
        equal(branch, branchEdge.fromAtomId(), "branch edge source");
        equal(ast.childAt(branch, 0), branchEdge.toAtomId(), "branch edge target");
        equal(0, branchEdge.ordinal(), "branch child ordinal");
        equal(99L, branchEdge.relationWordId(), "branch child relation");
        for (int ordinal = 0; ordinal < 2; ordinal++) {
            M3Dag.EdgeView edge = dag.outgoingEdge(root, ordinal);
            equal(root, edge.fromAtomId(), "root edge source");
            equal(branch, edge.toAtomId(), "shared root edge target");
            equal(ordinal, edge.ordinal(), "distinct root child ordinal");
            equal(99L, edge.relationWordId(), "root child relation");
        }
        check(dag.outgoingEdge(root, 0).edgeId() != dag.outgoingEdge(root, 1).edgeId(),
                "shared target retains two ordered edges");
        equal(M3InteractionKind.STRUCTURAL, dag.edge(0).interactionKind(), "AST edge kind");
    }

    private static void deterministicDag() {
        M3AtomStore.Builder builder = M3AtomStore.builder();
        int a = builder.intern(M3AtomDomain.DAG, 1, 1L);
        int b = builder.intern(M3AtomDomain.DAG, 1, 2L);
        int c = builder.intern(M3AtomDomain.DAG, 1, 3L);
        M3AtomStore atoms = builder.freeze();
        M3Dag first = new M3Dag.Builder(atoms)
                .edge(a, 10L, b)
                .edge(b, 11L, c)
                .freeze();
        M3Dag second = new M3Dag.Builder(atoms)
                .edge(b, 11L, c)
                .edge(a, 10L, b)
                .freeze();
        array(first.topologicalOrder(), second.topologicalOrder(), "topology");
        equal(first.contentHash(), second.contentHash(), "DAG hash");
        rejects(() -> new M3Dag.Builder(atoms)
                .edge(a, 10L, b)
                .edge(b, 10L, a)
                .freeze(), "cycle");
    }

    private static void interactionMatrix() {
        equal(M3InteractionKind.EVIDENCE,
                M3Interaction.classify(M3AtomDomain.STRING, M3AtomDomain.FACT,
                        M3InteractionMode.CONNECT), "evidence");
        equal(M3InteractionKind.LOGIC,
                M3Interaction.classify(M3AtomDomain.STRING, M3AtomDomain.LOGIC,
                        M3InteractionMode.DERIVE), "logic");
        equal(M3InteractionKind.REASON,
                M3Interaction.classify(M3AtomDomain.STRING, M3AtomDomain.REASON,
                        M3InteractionMode.CONNECT), "reason");
        equal(M3InteractionKind.ARGUMENT,
                M3Interaction.classify(M3AtomDomain.STRING, M3AtomDomain.STRING,
                        M3InteractionMode.OPPOSE), "argument");
        equal(M3InteractionKind.BRAINSTORM,
                M3Interaction.classify(M3AtomDomain.STRING, M3AtomDomain.STRING,
                        M3InteractionMode.EXPLORE), "brainstorm");
        equal(M3InteractionKind.CONTEXT_ACTION,
                M3Interaction.classify(M3AtomDomain.STRING, M3AtomDomain.CONTEXT,
                        M3InteractionMode.APPLY), "context action");
    }

    private static void randomizedTrees() {
        long state = 0x6a09e667f3bcc909L;
        for (int round = 0; round < 512; round++) {
            ArrayList<Node> leaves = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                state = mix(state);
                leaves.add(new Node(1, 1L + (state & 7L), state & 3L, List.of()));
            }
            Node root = new Node(7, round + 1000L, 0L, List.copyOf(leaves));
            M3Ast ast = M3Ast.compile(root, ADAPTER);
            equal(9, ast.expandedSize(ast.root(0)), "random expanded");
            check(ast.atoms().atomCount() <= 9, "random canonical bound");
            check(ast.structureHash(ast.root(0)) != 0L, "random structure hash");
        }
    }

    private static long mix(long x) {
        x ^= x >>> 12;
        x ^= x << 25;
        x ^= x >>> 27;
        return x * 0x2545f4914f6cdd1dL;
    }

    private static void array(int[] left, int[] right, String label) {
        checks++;
        if (!java.util.Arrays.equals(left, right)) throw new AssertionError(label);
    }

    private static void equal(long expected, long actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label + ": " + expected + " != " + actual);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": " + expected + " != " + actual);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void rejects(Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(label + " did not reject");
    }
}
