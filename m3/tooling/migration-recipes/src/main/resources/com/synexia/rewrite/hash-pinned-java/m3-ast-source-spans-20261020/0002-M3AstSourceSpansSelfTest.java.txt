// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.m3.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Standalone contract proof: execute with javac/java 21, without JUnit or JNI dependencies. */
public final class M3AstSourceSpansSelfTest {
    private record Node(int kind, long payload, List<Node> children) { }
    private static final M3Ast.NodeAdapter<Node> ADAPTER = new M3Ast.NodeAdapter<>() {
        @Override public int type(Node node) { return node.kind(); }
        @Override public long payloadId(Node node) { return node.payload(); }
        @Override public List<Node> children(Node node) { return node.children(); }
    };
    private static int checks;

    private M3AstSourceSpansSelfTest() { }

    public static void main(String[] args) {
        occurrenceNotAtomGeometry();
        sourceIdentityAndDefensiveOwnership();
        unknownAndRecoveredSpans();
        malformedInputsRefuse();
        preorderAndForestDifferential();
        System.out.println("M3_AST_STRING_SPANS_PASS checks=" + checks);
    }

    private static void occurrenceNotAtomGeometry() {
        Node left = new Node(1, 101L, List.of());
        Node right = new Node(1, 101L, List.of());
        M3Ast ast = M3Ast.compile(new Node(2, 201L, List.of(left, right)), ADAPTER);
        int[] starts = {0, 0, 4};
        int[] ends = {8, 2, 8};
        int[] masks = {1, 2, 4};
        M3AstSourceSpans spans = M3AstSourceSpans.index(ast, 17L, 42L, 3L, 8,
                starts, ends, masks);
        yes(ast.atoms().atomCount() == 2, "same text/payload/shape shares canonical atom");
        yes(spans.occurrenceCount() == 3, "expanded occurrence count");
        yes(spans.atomId(1) == spans.atomId(2), "identical atoms reused across offsets");
        yes(spans.startUtf16(1) != spans.startUtf16(2), "distinct locations retained");
        yes(spans.rootOccurrence(0) == 0 && spans.childCount(0) == 2, "root geometry");
        yes(spans.childOccurrence(0, 0) == 1 && spans.childOccurrence(0, 1) == 2,
                "root children retained in ordinal order");
        yes(spans.parentOccurrence(1) == 0 && spans.parentOccurrence(2) == 0,
                "parent linkage");
        yes(spans.stateMask(1) == 2 && spans.stateMask(2) == 4, "per-occurrence masks");
        yes(spans.hasSpan(0) && spans.hasSpan(1) && spans.hasSpan(2), "known bitmap");
        yes(spans.sourceOwnerId() == 17L && spans.sourceCoordinate() == 42L
                && spans.sourceGeneration() == 3L, "source id fields");
        yes(spans.primitiveBytes() > 0L, "primitive byte accounting");
        starts[1] = 99;
        ends[2] = 99;
        masks[2] = -1;
        yes(spans.startUtf16(1) == 0 && spans.endUtf16(2) == 8
                && spans.stateMask(2) == 4, "caller array mutation cannot change retained facts");
    }

    private static void sourceIdentityAndDefensiveOwnership() {
        Node leaf = new Node(1, 7L, List.of());
        M3Ast ast = M3Ast.compile(leaf, ADAPTER);
        M3AstSourceSpans spans = M3AstSourceSpans.index(ast, 9, 10, 11, 0,
                new int[] {0}, new int[] {0});
        spans.requireSource(9, 10, 11);
        throwsType(IllegalStateException.class, () -> spans.requireSource(8, 10, 11));
        throwsType(IllegalStateException.class, () -> spans.requireSource(9, 8, 11));
        throwsType(IllegalStateException.class, () -> spans.requireSource(9, 10, 12));
        yes(spans.startUtf16(0) == 0 && spans.endUtf16(0) == 0,
                "empty source valid without materialization");
    }

    private static void unknownAndRecoveredSpans() {
        Node leaf = new Node(1, 7L, List.of());
        M3Ast ast = M3Ast.compile(new Node(2, 8L, List.of(leaf)), ADAPTER);
        // Offsets are Java UTF-16, so a range may cross a surrogate pair or tuple seam.
        M3AstSourceSpans spans = M3AstSourceSpans.index(ast, 1, 2, 0, 6,
                new int[] {0, -1}, new int[] {6, -1});
        yes(spans.hasSpan(0) && !spans.hasSpan(1), "unknown span bitmap");
        yes(spans.startUtf16(1) == -1 && spans.endUtf16(1) == -1,
                "unresolved spans round trip");
        yes(spans.stateMask(0) == 0 && spans.stateMask(1) == 0,
                "null state lane defaults to zeros");
        M3AstSourceSpans seam = M3AstSourceSpans.index(ast, 1, 2, 0, 6,
                new int[] {0, 1}, new int[] {6, 5});
        yes(seam.startUtf16(1) == 1 && seam.endUtf16(1) == 5,
                "no scalar normalization of composite UTF-16 offsets");
    }

    private static void malformedInputsRefuse() {
        M3Ast ast = M3Ast.compile(new Node(1, 1L, List.of()), ADAPTER);
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 0, 1, 0, 2, new int[] {0}, new int[] {2}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 0, 0, 2, new int[] {0}, new int[] {2}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, -1, 2, new int[] {0}, new int[] {2}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, -1, new int[] {0}, new int[] {2}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2, new int[] {}, new int[] {}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2,
                        new int[] {0}, new int[] {2}, new int[] {1, 2}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2, new int[] {2}, new int[] {1}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2, new int[] {-1}, new int[] {2}));
        throwsType(IllegalArgumentException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2, new int[] {0}, new int[] {3}));
        throwsType(IndexOutOfBoundsException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2,
                        new int[] {0}, new int[] {2}).childOccurrence(0, 0));
        throwsType(IndexOutOfBoundsException.class, () ->
                M3AstSourceSpans.index(ast, 1, 1, 0, 2,
                        new int[] {0}, new int[] {2}).atomId(1));
        throwsType(NullPointerException.class, () ->
                M3AstSourceSpans.index(null, 1, 1, 0, 2, new int[] {}, new int[] {}));
    }

    private static void preorderAndForestDifferential() {
        Random random = new Random(0x3a5710L);
        for (int trial = 0; trial < 150; trial++) {
            int rootCount = 1 + random.nextInt(4);
            List<Node> roots = new ArrayList<>();
            for (int i = 0; i < rootCount; i++) roots.add(tree(random, 4));
            M3Ast ast = M3Ast.compileForest(roots, ADAPTER);
            int size = 0;
            for (int i = 0; i < rootCount; i++) size += ast.expandedSize(ast.root(i));
            int[] starts = new int[size];
            int[] ends = new int[size];
            int[] states = new int[size];
            Arrays.fill(ends, 200);
            for (int i = 0; i < size; i++) {
                starts[i] = i % 201;
                states[i] = i ^ trial;
            }
            M3AstSourceSpans index = M3AstSourceSpans.index(ast, 1, 2, trial, 200,
                    starts, ends, states);
            yes(index.occurrenceCount() == size, "total expanded occurrences");
            int offset = 0;
            for (int root = 0; root < rootCount; root++) {
                int[] preorder = ast.preOrder(root);
                yes(index.rootOccurrence(root) == offset, "forest root starts in root order");
                for (int atom : preorder) {
                    yes(index.atomId(offset) == atom, "preorder agrees with original AST");
                    yes(index.startUtf16(offset) == starts[offset]
                            && index.endUtf16(offset) == 200, "source range preserved");
                    yes(index.stateMask(offset) == states[offset], "state mask preserved");
                    for (int child = 0; child < index.childCount(offset); child++) {
                        int at = index.childOccurrence(offset, child);
                        yes(index.parentOccurrence(at) == offset, "reversed parent/child relationship");
                        yes(index.atomId(at) == ast.childAt(atom, child), "child atom order");
                    }
                    offset++;
                }
            }
            yes(offset == size, "all occurrences visited once");
        }
    }

    private static Node tree(Random random, int depth) {
        int width = depth == 0 ? 0 : random.nextInt(4);
        List<Node> children = new ArrayList<>(width);
        for (int i = 0; i < width; i++) children.add(tree(random, depth - 1));
        return new Node(random.nextInt(7), 1L + random.nextInt(8), children);
    }

    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("Expected " + type + ", got " + actual, actual);
        }
        throw new AssertionError("Expected " + type);
    }
}
