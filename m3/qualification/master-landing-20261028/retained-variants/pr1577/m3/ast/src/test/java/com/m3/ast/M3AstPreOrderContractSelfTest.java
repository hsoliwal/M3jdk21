// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Provider-tree oracle and return-isolation checks for expanded AST preorder. */
public final class M3AstPreOrderContractSelfTest {
    private record Node(int type, long payload, long metadata, List<Node> children) { }
    private static final M3Ast.NodeAdapter<Node> ADAPTER = new M3Ast.NodeAdapter<>() {
        @Override public int type(Node node) { return node.type(); }
        @Override public long payloadId(Node node) { return node.payload(); }
        @Override public long metadataBits(Node node) { return node.metadata(); }
        @Override public List<Node> children(Node node) { return node.children(); }
    };
    private static int checks;
    private static int rootsChecked;
    private static long occurrences;
    private static long digest = 0x243f6a8885a308d3L;

    public static void main(String[] args) {
        leafAndRefusals();
        sharedForest();
        broadAndDeepTrees();
        seededProviderForests();
        System.out.println("M3_AST_PREORDER_CONTRACT_PASS checks=" + checks
                + " roots=" + rootsChecked + " occurrences=" + occurrences
                + " digest=" + Long.toUnsignedString(digest, 16));
    }

    private static void leafAndRefusals() {
        Node leaf = new Node(1, 701L, -1L, List.of());
        M3Ast ast = M3Ast.compile(leaf, ADAPTER);
        verify(ast, List.of(leaf));
        int[] expected = { ast.root(0) };
        check(Arrays.equals(expected, ast.preOrder(0)), "leaf preorder");
        for (int invalid : new int[] { -1, Integer.MIN_VALUE, 1, Integer.MAX_VALUE }) {
            String message;
            try {
                ast.root(invalid);
                throw new AssertionError("root unexpectedly accepted");
            } catch (IndexOutOfBoundsException expectedFailure) {
                message = expectedFailure.getMessage();
            }
            try {
                ast.preOrder(invalid);
                throw new AssertionError("preOrder unexpectedly accepted");
            } catch (IndexOutOfBoundsException expectedFailure) {
                check(java.util.Objects.equals(message, expectedFailure.getMessage()),
                        "invalid-root exception message");
            }
            check(Arrays.equals(expected, ast.preOrder(0)), "refusal leaves tree unchanged");
        }
    }

    private static void sharedForest() {
        Node leaf = new Node(1, 11L, 0L, List.of());
        Node equalLeaf = new Node(1, 11L, 0L, List.of());
        Node branch = new Node(2, 22L, 3L, List.of(leaf));
        Node equalBranch = new Node(2, 22L, 3L, List.of(equalLeaf));
        Node root = new Node(3, 33L, 7L, List.of(branch, equalBranch, leaf, branch));
        List<Node> forest = List.of(root, equalLeaf, equalBranch, root);
        M3Ast ast = M3Ast.compileForest(forest, ADAPTER);
        check(ast.atoms().atomCount() == 3, "equal provider subtrees canonicalize");
        int rootRow = ast.root(0);
        int branchRow = ast.childAt(rootRow, 0);
        int leafRow = ast.childAt(branchRow, 0);
        check(ast.childAt(rootRow, 1) == branchRow, "equal branch shares row");
        check(ast.root(3) == rootRow, "repeated forest root shares row");
        int[] expected = {
            rootRow, branchRow, leafRow, branchRow, leafRow, leafRow, branchRow, leafRow
        };
        check(Arrays.equals(expected, ast.preOrder(0)), "manual shared-occurrence order");
        check(Arrays.equals(expected, ast.preOrder(3)), "manual repeated-root order");
        int[] detachedRoots = ast.roots();
        Arrays.fill(detachedRoots, -1);
        check(ast.root(0) == rootRow, "root projection is detached");
        verify(ast, forest);
    }

    private static void broadAndDeepTrees() {
        Node shared = new Node(4, 44L, 0L, List.of());
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < 2048; i++) {
            children.add(i % 3 == 0 ? shared : new Node(5, i + 1000L, i, List.of()));
        }
        Node broad = new Node(6, 66L, 1L, List.copyOf(children));
        verify(M3Ast.compile(broad, ADAPTER), List.of(broad));
        Node deep = shared;
        for (int i = 0; i < 192; i++) deep = new Node(7, i + 5000L, i, List.of(deep));
        verify(M3Ast.compile(deep, ADAPTER), List.of(deep));
    }

    private static void seededProviderForests() {
        Random random = new Random(0x4d33415354504fL);
        for (int trial = 0; trial < 200; trial++) {
            Node leaf = new Node(8, trial + 9000L, trial, List.of());
            Node shared = new Node(9, trial + 12000L, ~trial, List.of(leaf, leaf));
            List<Node> roots = new ArrayList<>();
            int count = 1 + random.nextInt(5);
            for (int ordinal = 0; ordinal < count; ordinal++) {
                roots.add(random.nextInt(5) == 0 && !roots.isEmpty()
                        ? roots.get(0) : tree(random, 5, shared));
            }
            verify(M3Ast.compileForest(roots, ADAPTER), roots);
        }
    }

    private static Node tree(Random random, int depth, Node shared) {
        if (depth >= 2 && random.nextInt(5) == 0) return shared;
        int width = depth == 0 ? 0 : random.nextInt(5);
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < width; i++) children.add(tree(random, depth - 1, shared));
        return new Node(random.nextInt(8), random.nextInt(23),
                random.nextLong(), List.copyOf(children));
    }

    private static void verify(M3Ast ast, List<Node> roots) {
        check(ast.rootCount() == roots.size(), "forest root count");
        int[] originalRoots = ast.roots();
        for (int ordinal = 0; ordinal < roots.size(); ordinal++) {
            List<Node> expected = new ArrayList<>();
            flattenProvider(roots.get(ordinal), expected);
            int[] first = ast.preOrder(ordinal);
            int[] second = ast.preOrder(ordinal);
            check(first != second, "each call returns its own array");
            check(Arrays.equals(first, second), "repeated order");
            check(first.length == expected.size(), "provider-expanded exact length");
            compareProvider(ast, first, expected);
            Arrays.fill(first, Integer.MIN_VALUE);
            compareProvider(ast, second, expected);
            int[] third = ast.preOrder(ordinal);
            check(third != first && third != second, "fresh array after returned mutation");
            check(Arrays.equals(second, third), "mutation cannot alter subsequent traversal");
            compareProvider(ast, third, expected);
            Arrays.fill(second, Integer.MAX_VALUE);
            compareProvider(ast, third, expected);
            check(Arrays.equals(originalRoots, ast.roots()), "output cannot alias roots");
            occurrences += expected.size();
            rootsChecked++;
        }
    }

    private static void flattenProvider(Node node, List<Node> output) {
        output.add(node);
        for (Node child : node.children()) flattenProvider(child, output);
    }

    private static void compareProvider(M3Ast ast, int[] actual, List<Node> expected) {
        check(actual.length == expected.size(), "oracle occurrence count");
        for (int i = 0; i < actual.length; i++) {
            int atom = actual[i];
            Node node = expected.get(i);
            check(ast.type(atom) == node.type(), "oracle type");
            check(ast.payloadId(atom) == node.payload(), "oracle payload");
            check(ast.metadataBits(atom) == node.metadata(), "oracle metadata");
            check(ast.childCount(atom) == node.children().size(), "oracle child arity");
            digest = Long.rotateLeft(digest ^ Integer.toUnsignedLong(atom), 7)
                    + 0x9e3779b97f4a7c15L;
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
