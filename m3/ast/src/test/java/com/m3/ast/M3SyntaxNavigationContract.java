// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/** Differential positional navigation against the existing source occurrence owner. */
public final class M3SyntaxNavigationContract {
    private M3SyntaxNavigationContract() { }
    private static int checks;
    private static void check(boolean holds, String reason) {
        if (!holds) throw new AssertionError(reason);
        checks++;
    }
    private static void refuses(Class<? extends Throwable> wanted, Runnable operation) {
        try { operation.run(); }
        catch (Throwable actual) {
            if (!wanted.isInstance(actual)) throw new AssertionError("wrong refusal type", actual);
            checks++;
            return;
        }
        throw new AssertionError("invalid input was accepted");
    }
    private static M3SyntaxTree sample() {
        byte[] source = " α + 🧪;\n".getBytes(StandardCharsets.UTF_8);
        var b = M3SyntaxTree.builder("tree-sitter/test", source);
        b.open("source_file", "", 0, M3SyntaxTree.NAMED);
        b.leaf("MISSING", "recovery", 1, 1, M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN);
        b.leaf("name", "name", 1, 3, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        b.leaf("punctuation", "op", 4, 5, M3SyntaxTree.TOKEN);
        b.leaf("MISSING", "recovery", 6, 6, M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN);
        b.open("expression", "expression", 6, M3SyntaxTree.NAMED);
        b.leaf("name", "item", 6, 10, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        b.close(10);
        b.leaf("punctuation", "delimiter", 10, 11, M3SyntaxTree.TOKEN);
        b.close(source.length);
        return b.build();
    }
    public static void main(String[] args) {
        var tree = sample();
        var index = new M3SyntaxNavigation(tree);
        check(index.nodes() == tree.size(), "all original occurrences retained");
        check(index.sourceSha256().equals(tree.sourceSha256())
                && index.projectionSha256().equals(tree.projectionSha256()), "source and parser seals");
        check(index.directChildren(0) == 6 && index.directChildren(5) == 1,
                "flat child arena preserves exact owner topology");
        check(index.directChild(0, 0) == 1 && index.directChild(0, 4) == 5
                && index.directChild(5, 0) == 6, "direct child ordinals");
        check(index.subtreeExit(0) == tree.size() && index.subtreeExit(5) == 7
                && index.containsNode(5, 6) && !index.containsNode(5, 7), "preorder descendant interval");
        check(index.firstChildWithField(0, "expression") == 5
                && index.firstChildWithField(0, "absent") == -1, "provider field identity");
        check(Arrays.equals(index.occurrencesOfKind("name"), new int[] {2, 6})
                && index.occurrencesOfKind("none").length == 0, "grammar kind query");
        for (int byteOffset = 0; byteOffset <= tree.sourceLength(); byteOffset++) {
            check(index.deepestAtByte(byteOffset) == tree.deepestAtByte(byteOffset),
                    "indexed lookup matches original half-open byte range");
            int[] path = index.pathAtByte(byteOffset);
            if (byteOffset == tree.sourceLength()) {
                check(path.length == 0, "EOF has no containing syntax");
            } else {
                check(path.length >= 1 && path[0] == 0 && path[path.length - 1] == index.deepestAtByte(byteOffset),
                        "indexed query yields root-to-source path");
                for (int p = 1; p < path.length; p++) {
                    check(tree.parent(path[p]) == path[p - 1], "path reflects original parent structure");
                }
            }
        }
        check(index.deepestAtByte(6) == 6 && index.deepestAtByte(1) == 2,
                "zero-width missing tokens do not conceal positive-width syntax");
        check(index.deepestAtByte(3) == 0 && index.pathAtByte(3).length == 1,
                "unparsed gaps resolve to source root");
        index.requireTree(tree);
        byte[] changed = tree.sourceBytes(); changed[0]++;
        var changedTree = M3SyntaxTree.builder(tree.grammar(), changed);
        changedTree.open("file", "", 0, M3SyntaxTree.NAMED);
        changedTree.close(changed.length);
        refuses(IllegalStateException.class, () -> index.requireTree(changedTree.build()));
        refuses(IndexOutOfBoundsException.class, () -> index.directChild(0, 100));
        refuses(IndexOutOfBoundsException.class, () -> index.directChild(-1, 0));
        refuses(IndexOutOfBoundsException.class, () -> index.subtreeExit(-1));
        refuses(IndexOutOfBoundsException.class, () -> index.deepestAtByte(-1));
        refuses(IndexOutOfBoundsException.class, () -> index.pathAtByte(tree.sourceLength() + 1));
        refuses(NullPointerException.class, () -> index.firstChildWithField(0, null));

        // Deep parsers must never overflow the Java stack during index build
        // or subsequent navigation. This also exercises preorder subtree spans.
        var deep = M3SyntaxTree.builder("deep/test", new byte[] {42});
        for (int i = 0; i < 5_000; i++) deep.open("inner", "", 0, M3SyntaxTree.NAMED);
        deep.leaf("leaf", "item", 0, 1, M3SyntaxTree.TOKEN);
        for (int i = 0; i < 5_000; i++) deep.close(1);
        var d = new M3SyntaxNavigation(deep.build());
        check(d.nodes() == 5001 && d.deepestAtByte(0) == 5000
                && d.pathAtByte(0).length == 5001, "5001 levels, iterative directory and query");
        check(d.containsNode(0, 5000) && !d.containsNode(5000, 4999), "deep descendant contract");

        var empty = M3SyntaxTree.builder("empty", new byte[0]);
        empty.leaf("file", "", 0, 0, M3SyntaxTree.NAMED);
        var e = new M3SyntaxNavigation(empty.build());
        check(e.deepestAtByte(0) == -1 && e.pathAtByte(0).length == 0
                && e.directChildren(0) == 0, "zero-byte source and empty children");

        // Fixed-seed randomized wide sibling trees, duplicate zero-width
        // recovery nodes, and source gaps. The legacy O(siblings) query is the
        // differential oracle for the new binary-search directory.
        Random random = new Random(0x4d33504cL);
        for (int iteration = 0; iteration < 240; iteration++) {
            int length = 24 + random.nextInt(384);
            byte[] source = new byte[length];
            random.nextBytes(source);
            var builder = M3SyntaxTree.builder("random/1", source);
            builder.open("file", "", 0, M3SyntaxTree.NAMED);
            int p = 0;
            while (p <= length) {
                int gaps = random.nextInt(3);
                for (int k = 0; k < gaps; k++)
                    builder.leaf("missing", "expected", p, p, M3SyntaxTree.MISSING | M3SyntaxTree.TOKEN);
                if (p == length) break;
                if (random.nextBoolean()) {
                    int gap = Math.min(length - p, random.nextInt(4));
                    p += gap;
                    if (p == length) break;
                }
                int to = Math.min(length, p + 1 + random.nextInt(5));
                if (random.nextBoolean()) {
                    builder.open("expression", "body", p, M3SyntaxTree.NAMED);
                    builder.leaf("token", "item", p, to, M3SyntaxTree.TOKEN);
                    builder.close(to);
                } else builder.leaf("token", "item", p, to, M3SyntaxTree.TOKEN);
                p = to;
            }
            builder.close(length);
            var original = builder.build();
            var fast = new M3SyntaxNavigation(original);
            for (int offset = 0; offset <= length; offset++) {
                check(fast.deepestAtByte(offset) == original.deepestAtByte(offset),
                        "random wide-tree byte/zero-width lookup differential");
            }
            // All direct children and preorder exits are independently checked.
            for (int node = 0; node < original.size(); node++) {
                int actualCount = 0;
                for (int c = original.firstChild(node); c >= 0; c = original.nextSibling(c)) {
                    check(fast.directChild(node, actualCount) == c, "random child ordinal");
                    actualCount++;
                }
                check(fast.directChildren(node) == actualCount, "random child count");
                int exit = node + 1;
                while (exit < original.size()) {
                    int walk = original.parent(exit);
                    boolean descends = false;
                    while (walk >= 0) {
                        if (walk == node) { descends = true; break; }
                        walk = original.parent(walk);
                    }
                    if (!descends) break;
                    exit++;
                }
                check(fast.subtreeExit(node) == exit, "random preorder subtree exit");
            }
        }
        System.out.println("M3_SYNTAX_NAVIGATION_PASS checks=" + checks);
    }
}
