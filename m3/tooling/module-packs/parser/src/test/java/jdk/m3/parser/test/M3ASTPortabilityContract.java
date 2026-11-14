// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import jdk.m3.parser.M3ASTExternalIngress;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;

public final class M3ASTPortabilityContract {
    private static int checks;
    private record Item(String kind, int start, int end, int flags,
                        boolean recovered, Item[] children, String[] roles) { }
    private static final class Adapter implements M3ASTExternalIngress.Nodes<Item> {
        private final String id;
        Adapter(String id) { this.id = id; }
        public String parserId() { return id; }
        public String kind(Item n) { return n.kind(); }
        public int childCount(Item n) { return n.children().length; }
        public Item child(Item n, int i) { return n.children()[i]; }
        public String childRole(Item n, int i) { return n.roles()[i]; }
        public int startUtf16(Item n) { return n.start(); }
        public int endUtf16(Item n) { return n.end(); }
        public int portableFlags(Item n) { return n.flags(); }
        public boolean recovered(Item n) { return n.recovered(); }
    }
    private static void check(boolean actual, String label) {
        checks++;
        if (!actual) throw new AssertionError(label);
    }
    private static void reject(Class<? extends Throwable> expected, Runnable code) {
        checks++;
        try { code.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("wrong refusal type", error);
        }
        throw new AssertionError("expected refusal: " + expected);
    }
    private static Item leaf(String name, int start, int end, int flags) {
        return new Item(name, start, end, flags, false, new Item[0], new String[0]);
    }
    private static Item expr(String firstRole) {
        Item[] children = {
            leaf("identifier", 0, 1, M3ASTIndex.TOKEN | M3ASTIndex.NAMED),
            leaf("+", 1, 2, M3ASTIndex.TOKEN),
            leaf("identifier", 2, 3, M3ASTIndex.TOKEN | M3ASTIndex.NAMED),
            leaf("missing_semicolon", 3, 3, M3ASTIndex.TOKEN | M3ASTIndex.MISSING)
        };
        return new Item("binary", 0, 3, M3ASTIndex.NAMED, true, children,
                new String[] {firstRole, "operator", "right", "terminator"});
    }
    private static M3ASTIndex parse(Item root, String grammar, int nodes, int depth) {
        return M3ASTExternalIngress.project(M3File.of(URI.create("memory:/expr"), "a+b"),
                root, new Adapter(grammar), new M3ASTIngress.Limits(128, nodes, depth));
    }
    public static void main(String[] args) {
        M3ASTIndex index = parse(expr("left"), "tree-sitter:fixture", 16, 8);
        check(index.domain() == M3ASTIndex.Domain.EXTERNAL, "one canonical index");
        check(index.parserId().orElseThrow().equals("tree-sitter:fixture"), "parser ID");
        check(index.nodeCount() == 5 && index.childCount(0) == 4, "preorder rows");
        check(index.parent(4) == 0 && index.nextSibling(3) == 4, "existing child geometry");
        check(index.childRole(1).orElseThrow().equals("left"), "native child field");
        check(index.childRole(0).isEmpty(), "no root field");
        check((index.portableFlags(2) & M3ASTIndex.TOKEN) != 0, "anonymous token");
        check((index.portableFlags(4) & M3ASTIndex.MISSING) != 0, "missing leaf");
        check(index.start(4) == 3 && index.end(4) == 3, "zero-width missing leaf");
        check(index.sourceText(1).equals("a"), "no duplicate source");
        check((index.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0, "external source unverified");
        check((index.flags(0) & M3ASTIndex.RECOVERED) != 0, "recovery marker");
        check(index.metadataBytes() >= 40, "optional primitive lanes");
        String hash = index.structuralHashes(5, 1000).sha256(0);
        check(!hash.equals(parse(expr("lhs"), "tree-sitter:fixture", 16, 8)
                .structuralHashes(5, 1000).sha256(0)), "edge roles part of hash");
        check(!hash.equals(parse(expr("left"), "antlr:fixture", 16, 8)
                .structuralHashes(5, 1000).sha256(0)), "grammar ID part of hash");
        reject(IllegalArgumentException.class, () -> parse(expr("left"), "", 16, 8));
        reject(IllegalArgumentException.class, () -> parse(expr("left"), "p", 4, 8));
        reject(IllegalArgumentException.class, () -> parse(expr("left"), "p", 16, 1));
        reject(IllegalArgumentException.class, () -> parse(expr(" "), "p", 16, 8));
        reject(IllegalArgumentException.class, () ->
                parse(expr("left"), "p", 16, 8).structuralHashes(4, 1000));
        reject(IllegalArgumentException.class, () ->
                parse(expr("left"), "p", 16, 8).structuralHashes(5, 1));
        reject(IllegalArgumentException.class, () -> parse(leaf("far", 0, 4, 0), "p", 16, 8));
        reject(IllegalArgumentException.class, () -> parse(leaf("bad", 0, 1, M3ASTIndex.MISSING), "p", 16, 8));
        reject(IllegalArgumentException.class, () -> parse(leaf("bad", 0, 0, 1024), "p", 16, 8));
        Item shared = leaf("identifier", 0, 1, M3ASTIndex.TOKEN);
        Item repeated = new Item("root", 0, 3, 0, false,
                new Item[] {shared, shared}, new String[] {"a", "b"});
        check(parse(repeated, "p", 8, 4).nodeCount() == 3, "shared nodes expand by occurrence");
        Item[] children = new Item[1];
        Item cycle = new Item("cycle", -1, -1, 0, false, children, new String[] {"next"});
        children[0] = cycle;
        reject(IllegalArgumentException.class, () -> parse(cycle, "p", 8, 3));
        System.out.println("M3_AST_PORTABILITY_PASS checks=" + checks);
    }
}
