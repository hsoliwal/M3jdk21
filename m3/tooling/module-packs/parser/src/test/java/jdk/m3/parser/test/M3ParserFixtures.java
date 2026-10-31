/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import jdk.m3.parser.M3JavacTree;
import jdk.m3.parser.grammar.M3Grammar;
import jdk.m3.parser.grammar.M3GrammarBuilder;
import jdk.m3.parser.grammar.M3GrammarFamily;
import jdk.m3.parser.grammar.M3GrammarParseTree;
import jdk.m3.parser.grammar.M3GrammarRef;
import jdk.m3.parser.grammar.M3GrammarStrictness;
import jdk.m3.parser.grammar.M3LexerProfile;
import jdk.m3.parser.grammar.M3TokenClass;

/** Small deterministic fixture language and portable receipts for its observations. */
final class M3ParserFixtures {
    private M3ParserFixtures() { }

    static M3LexerProfile profile() {
        return M3LexerProfile.builder("integer-expression-fixture-v1")
                .doubleQuoteString(true).lineCommentSlashSlash(true).blockComments(true)
                .operators("+", "*", "(", ")").build();
    }

    static M3Grammar expression() {
        return new M3GrammarBuilder("integer-expression-fixture-v1", M3GrammarFamily.CUSTOM,
                M3GrammarStrictness.STRICT, profile()).start("Expr")
                .production("Expr", nt("Expr"), literal("+"), nt("Term"))
                .production("Expr", nt("Term"))
                .production("Term", nt("Term"), literal("*"), nt("Atom"))
                .production("Term", nt("Atom"))
                .production("Atom", M3GrammarRef.token(M3TokenClass.NUMBER))
                .production("Atom", literal("("), nt("Expr"), literal(")"))
                .build();
    }

    static M3GrammarRef nt(String name) { return M3GrammarRef.nonTerminal(name); }
    static M3GrammarRef literal(String text) { return M3GrammarRef.literal(text); }

    /** Interpret this fixture's chosen tree; expected arithmetic values are independent constants. */
    static long evaluate(M3GrammarParseTree tree, int node) {
        if (tree.terminal(node)) return Long.parseLong(tree.tokenText(node));
        int count = tree.childCount(node);
        if (count == 1) return evaluate(tree, tree.child(node, 0));
        if (count != 3) throw new AssertionError("unexpected expression production");
        return switch (tree.symbolName(node)) {
            case "Expr" -> evaluate(tree, tree.child(node, 0)) + evaluate(tree, tree.child(node, 2));
            case "Term" -> evaluate(tree, tree.child(node, 0)) * evaluate(tree, tree.child(node, 2));
            case "Atom" -> evaluate(tree, tree.child(node, 1));
            default -> throw new AssertionError("unexpected fixture symbol " + tree.symbolName(node));
        };
    }

    static String grammarDigest(M3GrammarParseTree tree) {
        MessageDigest digest = digest();
        put(digest, "M3-PARSER-FIXTURE-TREE-1");
        put(digest, tree.root());
        put(digest, tree.nodeCount());
        for (int node = 0; node < tree.nodeCount(); node++) {
            put(digest, tree.symbol(node)); put(digest, tree.production(node));
            put(digest, tree.tokenIndex(node)); put(digest, tree.fromToken(node));
            put(digest, tree.toToken(node)); put(digest, tree.childCount(node));
            for (int child : tree.children(node)) put(digest, child);
        }
        for (int token = 0; token < tree.tokens().size(); token++) {
            put(digest, tree.tokens().start(token)); put(digest, tree.tokens().end(token));
            put(digest, tree.tokens().tokenClass(token).name());
            put(digest, tree.tokens().textAt(token));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static String javacDigest(M3JavacTree tree) {
        MessageDigest digest = digest();
        put(digest, "M3-PARSER-FIXTURE-JAVAC-1");
        put(digest, tree.source().uri().toASCIIString());
        put(digest, tree.source().utf16Sha256());
        put(digest, tree.nodeCount());
        for (int node = 0; node < tree.nodeCount(); node++) {
            put(digest, tree.kind(node).name()); put(digest, tree.parent(node));
            put(digest, tree.start(node)); put(digest, tree.end(node));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void put(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    private static void put(MessageDigest digest, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        put(digest, bytes.length);
        digest.update(bytes);
    }
}
