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
