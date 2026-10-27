// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import jdk.m3.parser.M3ASTExternalIngress;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3ASTSyntaxCoverage;
import jdk.m3.parser.M3CodePointSourceMap;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Utf8SourceMap;

/** Code-point offsets from real source Unicode are projected into the one AST. */
public final class M3CodePointSourceContract {
    private static int checks;
    private M3CodePointSourceContract() { }
    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why);
    }
    private static void refuse(Class<? extends Throwable> type, Runnable call) {
        checks++;
        try { call.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("wrong Unicode coordinate refusal", problem);
        }
        throw new AssertionError("invalid code-point coordinates admitted");
    }

    private record Node(String kind, int begin, int end, int tags, Node[] children) { }
    private static final class Adapter implements M3ASTExternalIngress.Nodes<Node> {
        private final M3CodePointSourceMap map;
        Adapter(M3CodePointSourceMap map) { this.map = map; }
        @Override public String parserId() { return "antlr4:codepoint-positions-fixture"; }
        @Override public String kind(Node node) { return node.kind(); }
        @Override public int childCount(Node node) { return node.children().length; }
        @Override public Node child(Node node, int at) { return node.children()[at]; }
        @Override public String childRole(Node node, int at) { return at == 0 ? "lhs" : null; }
        @Override public int startUtf16(Node node) { return map.utf16Offset(node.begin()); }
        @Override public int endUtf16(Node node) { return map.utf16Offset(node.end()); }
        @Override public int portableFlags(Node node) { return node.tags(); }
    }

    private static void fixture() throws Exception {
        String text = "a😀é₹\r\n";
        byte[] raw = text.getBytes(StandardCharsets.UTF_8);
        M3File source = M3File.fromCapturedBytes(URI.create("memory:/codepoint.txt"), raw,
                StandardCharsets.UTF_8, raw.length);
        M3CodePointSourceMap positions = M3CodePointSourceMap.of(source, text.length(), 6);
        M3Utf8SourceMap bytes = M3Utf8SourceMap.verified(source, raw, raw.length);
        check(positions.source() == source && positions.codePointCount() == 6,
                "one captured M3File and six original Unicode code points");
        int[] unitOffsets = {0, 1, 3, 4, 5, 6, 7};
        int[] byteOffsets = {0, 1, 5, 7, 10, 11, 12};
        for (int point = 0; point < unitOffsets.length; point++) {
            int codepoint = point;
            check(positions.utf16Offset(codepoint) == unitOffsets[point]
                    && positions.codePointOffset(unitOffsets[point]) == codepoint,
                    "exact Unicode code point to UTF-16 unit mapping");
            check(bytes.byteOffset(positions.utf16Offset(codepoint)) == byteOffsets[point]
                    && bytes.utf16Offset(byteOffsets[point]) == unitOffsets[point],
                    "code point, UTF16 and UTF8 coordinate triangle");
        }
        refuse(IllegalArgumentException.class, () -> positions.codePointOffset(2));
        var root = new Node("source", 0, 6, M3ASTIndex.NAMED, new Node[]{
                new Node("ascii", 0, 1, M3ASTIndex.TOKEN, new Node[0]),
                new Node("emoji", 1, 2, M3ASTIndex.TOKEN, new Node[0]),
                new Node("accent", 2, 3, M3ASTIndex.TOKEN, new Node[0]),
                new Node("currency", 3, 4, M3ASTIndex.TOKEN, new Node[0]),
                new Node("linebreak", 4, 6, M3ASTIndex.TRIVIA, new Node[0])
        });
        var index = M3ASTExternalIngress.project(source, root, new Adapter(positions),
                new M3ASTIngress.Limits(100, 10, 10));
        check(index.nodeCount() == 6 && index.source() == source
                && index.domain() == M3ASTIndex.Domain.EXTERNAL,
                "no second syntax tree; original M3ASTIndex occurrences retained");
        check(index.start(2) == 1 && index.end(2) == 3
                && index.sourceText(2).equals("😀"),
                "supplementary scalar is two UTF16 units in AST source");
        check(index.childRole(1).orElseThrow().equals("lhs"),
                "parser native child roles stay attached to original edges");
        check((index.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                "external parser source binding remains unverified");
        var coverage = M3ASTSyntaxCoverage.verify(index, 10, 100, false);
        check(coverage.tokens() == 4 && coverage.trivia() == 1
                && coverage.coveredUtf16() == text.length()
                && coverage.utf16Sha256().equals(source.utf16Sha256()),
                "real exact codepoint-ingress token/trivia source fidelity");
        refuse(IndexOutOfBoundsException.class, () ->
                M3ASTExternalIngress.project(source,
                        new Node("bad", 0, 7, 0, new Node[0]),
                        new Adapter(positions), new M3ASTIngress.Limits(100, 10, 10)));
        check(positions.checkpointCount() >= 2 && positions.retainedPrimitiveBytes() > 0,
                "bounded primitive checkpoint memory");
    }

    private static void random() {
        Random rng = new Random(0x4d33434f4445504fL);
        String[] atoms = {"a", "é", "₹", "𝄞", "😀", "💬", "\r\n", "\n", "α", "界"};
        for (int trial = 0; trial < 240; trial++) {
            StringBuilder b = new StringBuilder();
            for (int i = rng.nextInt(800); i > 0; i--) b.append(atoms[rng.nextInt(atoms.length)]);
            String text = b.toString();
            M3File source = M3File.of(URI.create("memory:/fuzz.txt"), text);
            int count = text.codePointCount(0, text.length());
            var positions = M3CodePointSourceMap.of(source, text.length(), count);
            check(positions.source() == source && positions.codePointCount() == count,
                    "original M3File and exact codepoint count");
            int point = 0;
            for (int at = 0; at < source.length();) {
                check(positions.utf16Offset(point) == at
                        && positions.codePointOffset(at) == point,
                        "independent source codepoint offset roundtrip");
                if (Character.isHighSurrogate(source.charAt(at))) {
                    int interior = at + 1;
                    refuse(IllegalArgumentException.class, () -> positions.codePointOffset(interior));
                }
                at += Character.charCount(text.codePointAt(at));
                point++;
            }
            check(positions.utf16Offset(point) == source.length()
                    && positions.codePointOffset(source.length()) == point,
                    "source EOF is a representable scalar boundary");
            check(positions.retainedPrimitiveBytes() == 8L * positions.checkpointCount(),
                    "two compact primitive checkpoint lanes");
        }
        for (String malformed : new String[]{"\ud800a", "a\udc00", "\udc00\ud800", "\ud800"}) {
            M3File source = M3File.of(URI.create("memory:/malformed.txt"), malformed);
            refuse(IllegalArgumentException.class, () ->
                    M3CodePointSourceMap.of(source, malformed.length(), malformed.length()));
        }
        var empty = M3CodePointSourceMap.of(M3File.of(URI.create("memory:/empty"), ""), 0, 0);
        check(empty.utf16Offset(0) == 0 && empty.codePointOffset(0) == 0,
                "empty UTF16 source has one original zero-width boundary");
        refuse(IndexOutOfBoundsException.class, () -> empty.utf16Offset(1));
        refuse(IllegalArgumentException.class, () ->
                M3CodePointSourceMap.of(M3File.of(URI.create("memory:/one"), "a"), 1, 0));
    }

    public static void main(String[] args) throws Exception {
        fixture();
        random();
        System.out.println("M3_CODEPOINT_SOURCE_PASS checks=" + checks);
    }
}
