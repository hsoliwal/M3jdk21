// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Random;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/** Real M3ASTIndex.Builder regression for ordinal identity and bounded names. */
public final class M3ASTBuilderOrdinalContract {
    private static long checks;
    private M3ASTBuilderOrdinalContract() { }
    private static void check(boolean yes) {
        if (!yes) throw new AssertionError("M3_AST_BUILDER_ORDINAL check " + checks);
        checks++;
    }
    private static void refuse(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable thrown) {
            if (expected.isInstance(thrown)) { checks++; return; }
            throw new AssertionError("wrong rejection type", thrown);
        }
        throw new AssertionError("expected source-owner refusal " + checks);
    }
    private static M3File text(String source) {
        return M3File.of(URI.create("memory:/dictionary-source"), source);
    }
    private static void legacyV1() {
        var builder = M3ASTIndex.externalBuilder(text("a+b"), "tree-sitter:fixture", 3);
        int root = builder.add(-1, "binary", 0, 3, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        builder.portable(root, null, M3ASTIndex.NAMED);
        int first = builder.add(root, "Aa", 0, 1, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        builder.portable(first, "left", M3ASTIndex.TOKEN | M3ASTIndex.NAMED);
        int second = builder.add(root, "BB", 2, 3, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        builder.portable(second, "right", M3ASTIndex.TOKEN | M3ASTIndex.NAMED);
        M3ASTIndex result = builder.seal();
        // Independently computed using the original M3_AST_STRUCTURE_SHA256_V1 frame.
        check("a1999ae3fd97d7109fa86c6100fc9a44731213a43cbdf97073f5d885d4c83a2e"
                .equals(result.structuralHashes(3, 1000).sha256(0)));
        check(result.kind(1).equals("Aa") && result.kind(2).equals("BB"));
        check(result.childRole(1).orElseThrow().equals("left"));
        check(result.childRole(2).orElseThrow().equals("right"));
        check(result.parent(2) == 0 && result.childCount(root) == 2);
    }
    private static void boundedKindsAndRoles() {
        var builder = M3ASTIndex.externalBuilder(text("x"), "external:dictionary-budget", 4100);
        int root = builder.add(-1, "root", 0, 1, M3ASTIndex.EXACT_RANGE);
        builder.portable(root, null, M3ASTIndex.NAMED);
        for (int i = 0; i < 4095; i++) {
            int child = builder.add(root, "kind_" + i, 0, 0, M3ASTIndex.EXACT_RANGE);
            builder.portable(child, "field_" + i, M3ASTIndex.NAMED);
            check(child == i + 1);
        }
        // Existing root + 4095 distinct children exhaust 4096 kind IDs.
        refuse(IllegalArgumentException.class,
                () -> builder.add(root, "new_kind_outside_budget", 0, 0, M3ASTIndex.EXACT_RANGE));
        int last = builder.add(root, "kind_0", 0, 0, M3ASTIndex.EXACT_RANGE);
        builder.portable(last, "field_last", M3ASTIndex.NAMED);
        int unclassified = builder.add(root, "kind_0", 0, 0, M3ASTIndex.EXACT_RANGE);
        refuse(IllegalArgumentException.class,
                () -> builder.portable(unclassified, "field_outside_budget", M3ASTIndex.NAMED));
        refuse(IllegalStateException.class, builder::seal);
    }
    private static void repeatedAndColliding() {
        Random r = new Random(20261026L);
        var builder = M3ASTIndex.externalBuilder(text("a"), "external:repeated", 100001);
        int root = builder.add(-1, "root", 0, 1, M3ASTIndex.EXACT_RANGE);
        builder.portable(root, null, 0);
        String[] names = {"Aa", "BB", "AaAa", "BBBB", "lambda", "module", "class",
                          "expression", "trivia", "token"};
        for (int i = 0; i < 100000; i++) {
            String name = names[r.nextInt(names.length)];
            int child = builder.add(root, new String(name), 0, 0, M3ASTIndex.EXACT_RANGE);
            builder.portable(child, i % 2 == 0 ? "name" : "body", M3ASTIndex.NAMED);
        }
        M3ASTIndex result = builder.seal();
        check("Aa".hashCode() == "BB".hashCode());
        check(result.nodeCount() == 100001 && result.childCount(0) == 100000);
        check(result.nextSibling(1) == 2 && result.parent(100000) == 0);
        check(result.childRole(100000).orElseThrow().equals("body"));
    }
    public static void main(String[] args) {
        legacyV1();
        boundedKindsAndRoles();
        repeatedAndColliding();
        System.out.println("M3_AST_BUILDER_ORDINAL_PASS checks=" + checks);
    }
}
