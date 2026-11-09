// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jdk.m3.parser.M3ASTExternalIngress;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3ASTSyntaxCoverage;
import jdk.m3.parser.M3CodePointSourceMap;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Utf8SourceMap;
import jdk.m3.parser.test.antlr.M3UnicodeTinyLexer;
import jdk.m3.parser.test.antlr.M3UnicodeTinyParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

/**
 * Real ANTLR4-generated parser/lexer input indexing into original M3File.
 * ANTLR lexer token stops are inclusive Unicode CODE POINT indices; M3AST
 * geometry always receives exact half-open UTF-16 offsets, never guessed.
 */
public final class M3ANTLR4RealParserContract {
    private static int checks;
    private M3ANTLR4RealParserContract() { }
    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why);
    }

    private record Occurrence(String kind, int startPoint, int endPoint,
                              int tags, List<Occurrence> children) { }

    private static final class IndexAdapter implements M3ASTExternalIngress.Nodes<Occurrence> {
        private final M3CodePointSourceMap coordinates;
        private IndexAdapter(M3CodePointSourceMap coordinates) {
            this.coordinates = coordinates;
        }
        @Override public String parserId() { return "antlr4-4.13.2:generated-test-grammar"; }
        @Override public String kind(Occurrence row) { return row.kind(); }
        @Override public int childCount(Occurrence row) { return row.children().size(); }
        @Override public Occurrence child(Occurrence row, int child) {
            return row.children().get(child);
        }
        @Override public String childRole(Occurrence row, int child) {
            return "lexeme";
        }
        @Override public int startUtf16(Occurrence row) {
            return coordinates.utf16Offset(row.startPoint());
        }
        @Override public int endUtf16(Occurrence row) {
            return coordinates.utf16Offset(row.endPoint());
        }
        @Override public int portableFlags(Occurrence row) { return row.tags(); }
    }


    /**
     * Project the ACTUAL ANTLR-owned ParserRuleContext/TerminalNode tree.
     * Native parser nodes are visited and then released by M3AST ingress.
     * Whitespace on ANTLR's hidden channel is deliberately absent here;
     * only the independent real token-stream CST is full-fidelity.
     */
    private static final class NativeParseTreeAdapter implements M3ASTExternalIngress.Nodes<ParseTree> {
        private final M3UnicodeTinyParser parser;
        private final M3CodePointSourceMap coordinates;

        NativeParseTreeAdapter(M3UnicodeTinyParser parser, M3CodePointSourceMap coordinates) {
            this.parser = parser;
            this.coordinates = coordinates;
        }

        @Override public String parserId() { return "antlr4-4.13.2:native-rule-context"; }

        @Override public String kind(ParseTree node) {
            if (node instanceof ParserRuleContext context) {
                return parser.getRuleNames()[context.getRuleIndex()];
            }
            if (node instanceof TerminalNode terminal) {
                Token token = terminal.getSymbol();
                if (token.getType() == Token.EOF) return "EOF";
                String named = parser.getVocabulary().getSymbolicName(token.getType());
                return named == null ? parser.getVocabulary().getDisplayName(token.getType()) : named;
            }
            throw new IllegalArgumentException("unsupported ANTLR parse tree node");
        }

        @Override public int childCount(ParseTree node) { return node.getChildCount(); }

        @Override public ParseTree child(ParseTree node, int index) { return node.getChild(index); }

        @Override public String childRole(ParseTree parent, int index) {
            ParseTree child = parent.getChild(index);
            if (child instanceof ParserRuleContext context)
                return parser.getRuleNames()[context.getRuleIndex()];
            return child instanceof ErrorNode ? "error" : "terminal";
        }

        private Token first(ParseTree node) {
            if (node instanceof ParserRuleContext context) return context.getStart();
            if (node instanceof TerminalNode terminal) return terminal.getSymbol();
            throw new IllegalArgumentException("unsupported ANTLR parse tree node");
        }

        private Token last(ParseTree node) {
            if (node instanceof ParserRuleContext context) return context.getStop();
            if (node instanceof TerminalNode terminal) return terminal.getSymbol();
            throw new IllegalArgumentException("unsupported ANTLR parse tree node");
        }

        @Override public int startUtf16(ParseTree node) {
            Token token = first(node);
            if (token == null) return -1;
            int point = token.getType() == Token.EOF
                    ? coordinates.codePointCount() : token.getStartIndex();
            return point < 0 ? -1 : coordinates.utf16Offset(point);
        }

        @Override public int endUtf16(ParseTree node) {
            Token token = last(node);
            if (token == null) return -1;
            int exclusive = token.getType() == Token.EOF
                    ? coordinates.codePointCount() : token.getStopIndex() + 1;
            return exclusive < 0 ? -1 : coordinates.utf16Offset(exclusive);
        }

        @Override public int portableFlags(ParseTree node) {
            if (node instanceof ParserRuleContext) return M3ASTIndex.NAMED;
            if (node instanceof TerminalNode terminal && terminal.getSymbol().getType() != Token.EOF)
                return M3ASTIndex.TOKEN;
            return 0; // EOF is zero-width, not a fake source-consuming token.
        }

        @Override public boolean recovered(ParseTree node) {
            if (node instanceof ParserRuleContext context) return context.exception != null;
            return node instanceof ErrorNode;
        }
    }

    private static void trial(String text, int sequence) throws Exception {
        byte[] raw = text.getBytes(StandardCharsets.UTF_8);
        M3File original = M3File.fromCapturedBytes(
                URI.create("memory:/antlr4/source-" + sequence + ".txt"),
                raw, StandardCharsets.UTF_8, raw.length);
        int scalars = text.codePointCount(0, text.length());
        M3CodePointSourceMap coordinates = M3CodePointSourceMap.of(
                original, original.length(), scalars);
        M3Utf8SourceMap bytes = M3Utf8SourceMap.verified(original, raw, raw.length);

        // These are REAL ANTLR4 Java 4.13.2 runtime and generated grammar APIs.
        CharStream stream = CharStreams.fromString(original.text());
        check(stream.size() == scalars, "ANTLR CharStreams uses Unicode code points");
        M3UnicodeTinyLexer lexer = new M3UnicodeTinyLexer(stream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        M3UnicodeTinyParser parser = new M3UnicodeTinyParser(tokenStream);
        M3UnicodeTinyParser.DocumentContext parsed = parser.document();
        check(parser.getNumberOfSyntaxErrors() == 0, "real generated ANTLR parser accepts fixture");
        check(parsed != null, "real parser returned a document context");

        List<Occurrence> leaves = new ArrayList<>();
        int previousPoint = 0, visible = 0, whitespace = 0;
        for (Token token : tokenStream.getTokens()) {
            if (token.getType() == Token.EOF) continue;
            int first = token.getStartIndex(), last = token.getStopIndex() + 1;
            check(first == previousPoint && last > first && last <= scalars,
                    "all real token/hidden-channel spans are exact and ordered");
            int startUnit = coordinates.utf16Offset(first);
            int endUnit = coordinates.utf16Offset(last);
            check(original.text().substring(startUnit, endUnit).equals(token.getText()),
                    "original M3File substring equals real ANTLR lexer text");
            int byteStart = bytes.byteOffset(startUnit), byteEnd = bytes.byteOffset(endUnit);
            check(byteStart <= byteEnd && byteEnd <= raw.length,
                    "ANTLR codepoint -> original UTF16 -> verified byte window");
            check(text.substring(0, startUnit).getBytes(StandardCharsets.UTF_8).length == byteStart,
                    "ANTLR lexeme byte coordinates agree with independent encoded prefix");
            boolean hidden = token.getChannel() == Token.HIDDEN_CHANNEL;
            if (hidden) whitespace++;
            else visible++;
            leaves.add(new Occurrence(
                    lexer.getVocabulary().getSymbolicName(token.getType()),
                    first, last, hidden ? M3ASTIndex.TRIVIA : M3ASTIndex.TOKEN,
                    List.of()));
            previousPoint = last;
        }
        check(previousPoint == scalars, "including all hidden whitespace in exact source coverage");
        check(parsed.item().size() == visible, "ANTLR parse context has real visible items");
        check(leaves.size() == visible + whitespace, "no hidden trivia discarded");

        var root = new Occurrence("antlr-document", 0, scalars,
                leaves.isEmpty() ? 0 : M3ASTIndex.NAMED, leaves);
        M3ASTIndex index = M3ASTExternalIngress.project(
                original, root, new IndexAdapter(coordinates),
                new M3ASTIngress.Limits(Math.max(1, original.length()), leaves.size() + 1, 8));
        check(index.nodeCount() == leaves.size() + 1 && index.source() == original,
                "only original M3ASTIndex retains detached occurrence rows");
        check(index.domain() == M3ASTIndex.Domain.EXTERNAL
                && index.parserId().orElseThrow().contains("antlr4"),
                "original parser ID and AST owner maintained");
        check((index.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                "parser input binding is not falsely certified by source resemblance");
        if (!leaves.isEmpty()) {
            check(index.childRole(1).orElseThrow().equals("lexeme"),
                    "ANTLR lexeme role retained without another syntax hierarchy");
        }
        M3ASTSyntaxCoverage.Proof proof = M3ASTSyntaxCoverage.verify(
                index, leaves.size() + 1, original.length(), false);
        check(proof.tokens() == visible && proof.trivia() == whitespace
                && proof.coveredUtf16() == original.length()
                && proof.utf16Sha256().equals(original.utf16Sha256()),
                "original M3File full-fidelity ANTLR token/trivia source seal");

        // Native ANTLR parse-tree RULES and TERMINALS are distinct from the
        // complete lexical token/trivia sequence verified above.
        M3ASTIndex parseTree = M3ASTExternalIngress.project(
                original, parsed, new NativeParseTreeAdapter(parser, coordinates),
                new M3ASTIngress.Limits(Math.max(1, original.length()),
                        visible * 2 + 2, 12));
        check(parseTree.nodeCount() == visible * 2 + 2,
                "original ANTLR parse tree has document, item/token pairs, and EOF");
        check(parseTree.kind(0).equals("document")
                && parseTree.domain() == M3ASTIndex.Domain.EXTERNAL
                && parseTree.parserId().orElseThrow().contains("native-rule-context"),
                "original rule and native-parser provider remain identifiable");
        check(parseTree.childCount(0) == visible + 1
                && parseTree.kind(parseTree.nodeCount() - 1).equals("EOF"),
                "native rule hierarchy includes every visible item and EOF");
        check((parseTree.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                "native ANTLR parse tree does not falsely certify source binding");
        int visibleIndex = 0;
        for (Token token : tokenStream.getTokens()) {
            if (token.getType() == Token.EOF || token.getChannel() == Token.HIDDEN_CHANNEL)
                continue;
            int rule = visibleIndex * 2 + 1;
            int terminal = rule + 1;
            check(parseTree.kind(rule).equals("item") && parseTree.parent(rule) == 0
                    && parseTree.childRole(rule).orElseThrow().equals("item"),
                    "actual ANTLR rule context remains a native named occurrence");
            check(parseTree.parent(terminal) == rule
                    && parseTree.childRole(terminal).orElseThrow().equals("terminal"),
                    "native TerminalNode is attached to original parse rule");
            check(parseTree.start(terminal) == coordinates.utf16Offset(token.getStartIndex())
                    && parseTree.end(terminal) == coordinates.utf16Offset(token.getStopIndex() + 1)
                    && parseTree.sourceText(terminal).equals(token.getText()),
                    "original M3AST terminal span equals real ANTLR Unicode token");
            check((parseTree.portableFlags(terminal) & M3ASTIndex.TOKEN) != 0,
                    "native ANTLR token flag retained independently of trivia CST");
            visibleIndex++;
        }
        check(visibleIndex == visible && parseTree.source() == original,
                "native parse tree and complete CST share the same original M3File");

    }

    public static void main(String[] args) throws Exception {
        String[] basics = {
                "", "a", "é", "₹", "😀", "x😀é₹", "😀 \t\r\n a",
                "𝄞", "\ufeffstart", "\r\n\r\n", "a😀é₹\r\n".repeat(160)
        };
        for (int i = 0; i < basics.length; i++) trial(basics[i], i);
        Random random = new Random(0x414e544c52344350L);
        String[] atoms = {"a","é","₹","😀","𝄞"," ","\t","\r\n","\n","xyz","\ufeff"};
        for (int iteration = 0; iteration < 120; iteration++) {
            StringBuilder builder = new StringBuilder();
            for (int n = random.nextInt(150); n > 0; n--)
                builder.append(atoms[random.nextInt(atoms.length)]);
            trial(builder.toString(), basics.length + iteration);
        }
        System.out.println("M3_ANTLR4_REAL_PARSER_PASS checks=" + checks);
    }
}
