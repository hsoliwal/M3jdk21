// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Standalone Java 21 contract; Maven/SDK and installed-image proof remain separate. */
public final class M3SyntaxTreeContract {
    private static int checks;
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
        checks++;
    }
    private static void rejects(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("expected rejection");
    }
    private record N(String kind, String field, int start, int end, int flags, List<N> children) { }
    private static final M3SyntaxTree.ForeignNode<N> ADAPTER = new M3SyntaxTree.ForeignNode<>() {
        public String kind(N n) { return n.kind; }
        public String field(N n) { return n.field; }
        public int startByte(N n) { return n.start; }
        public int endByte(N n) { return n.end; }
        public int flags(N n) { return n.flags; }
        public Iterable<N> children(N n) { return n.children; }
    };
    public static void main(String[] args) {
        byte[] source = " x\r\n😀;".getBytes(StandardCharsets.UTF_8);
        int n = source.length;
        N root = new N("source_file", null, 0, n, M3SyntaxTree.NAMED, List.of(
                new N("identifier", "name", 1, 2,
                        M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN, List.of()),
                new N("comment", "", 2, 4,
                        M3SyntaxTree.TRIVIA | M3SyntaxTree.TOKEN, List.of()),
                new N("ERROR", "", 4, 8, M3SyntaxTree.ERROR | M3SyntaxTree.TOKEN, List.of()),
                new N("missing", "expected", 8, 8,
                        M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN, List.of())));
        M3SyntaxTree tree = M3SyntaxTree.project("tree-sitter/java@fixture", source, root, ADAPTER);
        check(tree.size() == 5 && tree.parent(4) == 0, "dense preorder");
        check(tree.firstChild(0) == 1 && tree.nextSibling(1) == 2
                && tree.nextSibling(3) == 4 && tree.nextSibling(4) == -1, "sibling indexes");
        check(tree.kind(3).equals("ERROR") && tree.field(4).equals("expected"), "foreign names");
        check(tree.hasFlag(2, M3SyntaxTree.TRIVIA) && tree.hasFlag(4, M3SyntaxTree.MISSING), "flags");
        check(Arrays.equals(tree.sourceBytes(), source) && tree.deepestAtByte(0) == 0,
                "exact source and unparsed gaps");
        check(tree.deepestAtByte(4) == 3 && tree.deepestAtByte(n) == -1, "byte lookup");
        check(tree.sourceSha256().matches("[a-f0-9]{64}")
                && tree.projectionSha256().matches("[a-f0-9]{64}"), "hash");
        check(tree.projectionSha256().equals(
                M3SyntaxTree.project("tree-sitter/java@fixture", source, root, ADAPTER)
                        .projectionSha256()), "fixed shape identity");
        N fragment = new N("method", "body", 1, 2, M3SyntaxTree.NAMED, List.of());
        var projected = M3SyntaxTree.projectFragment("eclipse/jdt@fixture", source, fragment, ADAPTER);
        check(projected.size() == 2 && projected.kind(0).equals("m3.source_image")
                && projected.startByte(1) == 1 && projected.endByte(1) == 2,
                "partial AST maps through synthetic root");
        var invalid = M3SyntaxTree.builder("grammar", source);
        invalid.open("root", "", 0, 0);
        invalid.leaf("first", "", 1, 3, M3SyntaxTree.TOKEN);
        rejects(() -> invalid.leaf("overlap", "", 2, 4, M3SyntaxTree.TOKEN));
        rejects(() -> invalid.leaf("missing", "", 3, 4, M3SyntaxTree.MISSING));
        invalid.close(n);
        invalid.build();
        byte[] mutated = tree.sourceBytes(); mutated[0]++;
        check(Arrays.equals(tree.sourceBytes(), source), "defensive source ownership");
        // Iterative importer: a deep external tree must not recurse Java stack.
        N deep = new N("bottom", "", 0, n, 0, List.of());
        for (int i = 0; i < 5000; i++) deep = new N("nested", "", 0, n, 0, List.of(deep));
        check(M3SyntaxTree.project("deep/fixture", source, deep, ADAPTER).size() == 5001,
                "iterative deep projection");
        System.out.println("M3_SYNTAX_TREE_PASS checks=" + checks);
    }
}
