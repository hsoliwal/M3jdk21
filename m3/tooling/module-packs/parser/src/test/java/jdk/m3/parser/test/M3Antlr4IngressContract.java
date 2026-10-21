// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Objects;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3Antlr4Ingress;
import jdk.m3.parser.M3File;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

/** ANTLR4 public-API-shape contract; no ANTLR parser, compiler or source reread. */
public final class M3Antlr4IngressContract {
    private static int checks;
    private M3Antlr4IngressContract() { }
    private static void yes(boolean success, String label) {
        checks++; if (!success) throw new AssertionError(label);
    }
    private static void equals(Object wanted, Object actual, String label) {
        yes(Objects.equals(wanted, actual), label + " expected=" + wanted + " got=" + actual);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void refuses(Class<? extends Throwable> type, Checked action, String why) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError(why + ": unexpected " + problem, problem);
        }
        throw new AssertionError("missing refusal: " + why);
    }
    private static M3File source(String text) { return M3File.of(URI.create("memory:/upper.dsl"), text); }
    private static M3ASTIngress.Limits limits() { return new M3ASTIngress.Limits(1000, 100, 20); }
    private static Token token(CharStream input, int type, int first, int last) {
        return new CommonToken(type, first, last, input);
    }
    private record Input(ParserRuleContext root, CharStream chars, Parser parser, M3File source) {}
    private static Input build(String text, boolean codePoints) {
        M3File m3 = source(text);
        CharStream chars = codePoints ? new CodePointCharStream(text) : new ANTLRInputStream(text);
        int end = chars.size() - 1;
        ParserRuleContext root = new ParserRuleContext(0, token(chars, 1, 0, end), token(chars, 1, 0, end));
        return new Input(root, chars, new Parser("document", "assignment"), m3);
    }
    private static int indexAt(String text, int utf16Offset, boolean codePoints) {
        return codePoints ? text.codePointCount(0, utf16Offset) : utf16Offset;
    }
    private static void project(boolean codePoints) {
        String text = "α😀\r\nname=42\n";
        Input input = build(text, codePoints);
        int nameAt = text.indexOf("name");
        int numberAt = text.indexOf("42");
        int nameFirst = indexAt(text, nameAt, codePoints);
        int numberFirst = indexAt(text, numberAt, codePoints);
        ParserRuleContext child = new ParserRuleContext(1,
                token(input.chars(), 8, nameFirst, nameFirst + 3),
                token(input.chars(), 9, numberFirst, numberFirst + 1));
        child.add(new TerminalNodeImpl(token(input.chars(), 8, nameFirst, nameFirst + 3)));
        child.add(new TerminalNodeImpl(token(input.chars(), 9, numberFirst, numberFirst + 1)));
        input.root().add(new TerminalNodeImpl(token(input.chars(), 11, indexAt(text, 1, codePoints),
                indexAt(text, 3, codePoints) - 1))); // supplementary character
        input.root().add(child);
        input.root().add(new ErrorNodeImpl(token(null, 13, -1, -1)));
        M3ASTIndex indexed = M3Antlr4Ingress.index(input.source(), input.root(), input.parser(),
                input.chars(), limits());
        M3ASTIndex viaOwner = M3ASTIngress.antlr4(input.source(), input.root(), input.parser(), input.chars(), limits());
        equals(indexed.nodeCount(), viaOwner.nodeCount(), "original M3AST ingress facade reuses same donor");
        equals(M3ASTIndex.Domain.ANTLR4, indexed.domain(), "ANTLR domain");
        equals(6, indexed.nodeCount(), "preorder occurrences");
        equals("ANTLR_RULE:document", indexed.kind(0), "original grammar root");
        equals("ANTLR_TOKEN:11", indexed.kind(1), "symbolic token kind");
        equals("😀", indexed.sourceText(1), "Unicode geometry from donor CharStream");
        equals(1, indexed.start(1), "Unicode UTF-16 first offset");
        equals(3, indexed.end(1), "Unicode UTF-16 exclusive end");
        equals("ANTLR_RULE:assignment", indexed.kind(2), "nested grammar rule name");
        equals(0, indexed.parent(2), "parent row identity");
        equals("name=42", indexed.sourceText(2), "grammar span mapped to M3 source");
        equals(2, indexed.startLine(2), "CRLF line index");
        equals(2, indexed.endLine(2), "CRLF final line");
        equals("name", indexed.sourceText(3), "lexical name projection");
        equals("42", indexed.sourceText(4), "literal source without a second lexer");
        yes((indexed.flags(5) & M3ASTIndex.RECOVERED) != 0, "original ANTLR error node preserved");
        yes(!indexed.hasExactRange(5), "synthetic token cannot claim exact source range");
        refuses(IllegalStateException.class, () -> indexed.sourceText(5), "synthetic range unavailable");
        input.root().add(new TerminalNodeImpl(token(input.chars(), 5, nameFirst, nameFirst)));
        equals(6, indexed.nodeCount(), "detached indexed facts survive original tree mutation");
        child.exception = new IllegalStateException("recovered");
        M3ASTIndex recovered = M3Antlr4Ingress.index(input.source(), input.root(),
                input.parser(), input.chars(), limits());
        yes((recovered.flags(2) & M3ASTIndex.RECOVERED) != 0, "rule exception field survives upper-layer recovery");
    }
    private static void provenanceAndBudgets() {
        Input input = build("a😀z", true);
        input.root().add(new TerminalNodeImpl(token(input.chars(), 1, 0, 0)));
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(source("changed"), input.root(), input.parser(), input.chars(), limits()),
                "wrong retained source");
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), input.parser(),
                        new CodePointCharStream("changed"), limits()), "wrong CharStream generation");
        input.root().add(new TerminalNodeImpl(token(new CodePointCharStream("a😀z"), 2, 1, 1)));
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), input.parser(), input.chars(), limits()),
                "foreign token stream with identical bytes is a different parser generation");
        input.root().add(new TerminalNodeImpl(token(input.chars(), 3, 999, 999)));
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), input.parser(), input.chars(), limits()),
                "out of bounds token");
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), input.parser(), input.chars(),
                        new M3ASTIngress.Limits(2, 100, 20)), "source length budget");
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), input.parser(), input.chars(),
                        new M3ASTIngress.Limits(1000, 1, 20)), "node budget");
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), input.parser(), input.chars(),
                        new M3ASTIngress.Limits(1000, 100, 1)), "tree depth budget");
        refuses(IllegalArgumentException.class,
                () -> M3Antlr4Ingress.index(input.source(), input.root(), new Object(), input.chars(), limits()),
                "foreign parser instance");
    }
    public static void main(String[] args) {
        project(true);
        project(false);
        provenanceAndBudgets();
        System.out.println("M3_ANTLR4_INGRESS_PASS checks=" + checks);
    }
}
