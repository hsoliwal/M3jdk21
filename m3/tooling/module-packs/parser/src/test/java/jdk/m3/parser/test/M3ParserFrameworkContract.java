/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3ParseResult;
import jdk.m3.parser.M3Parser;
import jdk.m3.parser.grammar.M3Grammar;
import jdk.m3.parser.grammar.M3GrammarBuilder;
import jdk.m3.parser.grammar.M3GrammarException;
import jdk.m3.parser.grammar.M3GrammarFamily;
import jdk.m3.parser.grammar.M3GrammarLimits;
import jdk.m3.parser.grammar.M3GrammarParseTree;
import jdk.m3.parser.grammar.M3GrammarRef;
import jdk.m3.parser.grammar.M3GrammarStrictness;
import jdk.m3.parser.grammar.M3LexerProfile;
import jdk.m3.parser.grammar.M3TokenClass;
import jdk.m3.parser.grammar.M3TokenStream;

/** Executable source, grammar, collision, failure-budget and ownership contracts. */
public final class M3ParserFrameworkContract {
    private static int checks;

    private M3ParserFrameworkContract() { }

    public static void main(String[] arguments) throws Exception {
        sourceIdentity();
        sourceSealBoundaries();
        fileAdmission();
        expressions();
        lexerSpansAndErrors();
        nullableAndLeftRecursive();
        collisionIsolation();
        acceptancePolicies();
        resourceRefusals();
        parallelSessions();
        System.out.println("M3_PARSER_FRAMEWORK_PASS checks=" + checks);
    }

    private static M3File source(String text) {
        return M3File.of(URI.create("memory:/fixture.expr"), text);
    }

    private static void sourceIdentity() throws Exception {
        String text = new String(new char[] {'x', '\r', '\n', '\ud83d', '\ude00', '\ud800', 'z', '\udc00'});
        M3File file = source(text);
        check(file.text() == text && file.toString() == text, "one immutable String owner");
        check(file.length() == text.length() && file.charAt(5) == '\ud800', "exact UTF-16 coordinates");
        check(file.subSequence(3, 5).equals("\ud83d\ude00"), "supplementary source span");
        check(file.subSequence(5, 6).charAt(0) == '\ud800', "unpaired source span retained");
        check(file.byteSha256().isEmpty(), "character-only source has no invented byte seal");
        expect(IndexOutOfBoundsException.class, () -> file.subSequence(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> file.subSequence(2, 1));
        expect(IndexOutOfBoundsException.class, () -> file.charAt(file.length()));
        expect(NullPointerException.class, () -> M3File.of(null, text));

        char[] everyUnit = new char[Character.MAX_VALUE + 1];
        for (int value = 0; value < everyUnit.length; value++) everyUnit[value] = (char) value;
        ByteBuffer bytes = ByteBuffer.allocate(everyUnit.length * Character.BYTES).order(ByteOrder.BIG_ENDIAN);
        bytes.asCharBuffer().put(everyUnit);
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.array()));
        check(source(new String(everyUnit)).utf16Sha256().equals(expected), "all 65536 code units use raw UTF-16BE seal");

        StringBuilder mutable = new StringBuilder("12 + 3");
        M3File frozen = M3File.of(URI.create("memory:/mutable.expr"), mutable);
        mutable.replace(0, mutable.length(), "broken");
        M3ParseResult result = new M3Parser(M3ParserFixtures.expression()).parse(frozen);
        check(frozen.text().equals("12 + 3") && result.conforms(), "mutable ingress snapshotted once");
        check(result.source() == frozen && result.requireTree().tokens().source() == frozen.text(),
                "result and token ranges retain the admitted source");
    }

    private static void sourceSealBoundaries() throws Exception {
        for (int length : new int[] {0, 1, 2, 4095, 4096, 4097, 8191, 8192, 8193, 131073}) {
            char[] units = new char[length];
            for (int index = 0; index < length; index++) units[index] = (char) (index * 40503 + 0xd800);
            // Independent contiguous UTF-16BE oracle includes lone surrogates and block tails.
            ByteBuffer expected = ByteBuffer.allocate(length * Character.BYTES).order(ByteOrder.BIG_ENDIAN);
            expected.asCharBuffer().put(units);
            check(source(new String(units)).utf16Sha256().equals(hash(expected.array())),
                    "exact source seal at length " + length);
            if (length != 0) {
                String original = source(new String(units)).utf16Sha256();
                units[length - 1] ^= 1;
                check(!source(new String(units)).utf16Sha256().equals(original),
                        "last code unit remains part of seal at length " + length);
            }
        }
    }

    private static void fileAdmission() throws Exception {
        String configured = System.getProperty("m3.parser.test.work");
        if (configured == null) throw new IllegalStateException("explicit m3.parser.test.work is required");
        Path work = Path.of(configured);
        check(Files.isDirectory(work), "test files stay within supplied proof directory");
        Path file = work.resolve("source-utf8.txt");
        String text = "A\u00e9\ud83d\ude00\r\n";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW);
        M3File first = M3File.read(file, StandardCharsets.UTF_8, bytes.length);
        check(first.text().equals(text), "strict UTF-8 file admission");
        check(first.uri().equals(file.toAbsolutePath().normalize().toUri()), "normalized source URI");
        check(first.byteSha256().orElseThrow().equals(hash(bytes)), "raw bytes have their own seal");
        check(first.utf16Sha256().equals(source(text).utf16Sha256()), "decoded source seal matches character input");
        check(!first.utf16Sha256().equals(first.byteSha256().orElseThrow()), "chosen UTF-8 and UTF-16 fixture seals differ");
        expect(IOException.class, () -> M3File.read(file, StandardCharsets.UTF_8, bytes.length - 1));
        expect(IllegalArgumentException.class, () -> M3File.read(file, StandardCharsets.UTF_8, -1));
        expect(IllegalArgumentException.class, () -> M3File.read(file, StandardCharsets.UTF_8, Integer.MAX_VALUE));

        Path utf16 = work.resolve("source-utf16be.txt");
        byte[] utf16Bytes = text.getBytes(StandardCharsets.UTF_16BE);
        Files.write(utf16, utf16Bytes, StandardOpenOption.CREATE_NEW);
        M3File sameCharacters = M3File.read(utf16, StandardCharsets.UTF_16BE, utf16Bytes.length);
        check(sameCharacters.utf16Sha256().equals(first.utf16Sha256()), "same decoded generation across explicit encodings");
        check(!sameCharacters.uri().equals(first.uri()), "equal content does not erase source URI identity");
        check(sameCharacters.byteSha256().orElseThrow().equals(hash(utf16Bytes)), "UTF-16 file seal covers actual bytes");

        Files.writeString(file, "changed", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        M3File successor = M3File.read(file, StandardCharsets.UTF_8, 7);
        check(first.text().equals(text), "retained snapshot survives file edit");
        check(successor.text().equals("changed") && !successor.utf16Sha256().equals(first.utf16Sha256()),
                "file edit creates a distinct source generation");
        Path malformed = work.resolve("malformed-utf8.bin");
        Files.write(malformed, new byte[] {(byte) 0xc3, 0x28}, StandardOpenOption.CREATE_NEW);
        expect(CharacterCodingException.class, () -> M3File.read(malformed, StandardCharsets.UTF_8, 2));
        Path empty = work.resolve("empty.txt");
        Files.write(empty, new byte[0], StandardOpenOption.CREATE_NEW);
        check(M3File.read(empty, StandardCharsets.UTF_8, 0).length() == 0, "empty file with zero byte budget");
    }

    private static void expressions() {
        M3Grammar grammar = M3ParserFixtures.expression();
        M3Parser parser = new M3Parser(grammar);
        check(parser.grammar() == grammar, "configuration retains immutable grammar");
        check(grammar.leftRecursive(grammar.findNonTerminal("Expr")), "left-recursive fixture is actually left-recursive");
        String[] texts = {"0", "1", "2*(3+4)", "12 + 3 * (4 + 5)", "7+8*2+1", "((9))", "2 /* gap */ + 3"};
        long[] values = {0, 1, 14, 39, 24, 9, 5};
        for (int index = 0; index < texts.length; index++) {
            M3File input = source(texts[index]);
            M3ParseResult parsed = parser.parse(input);
            check(parsed.accepted() && parsed.conforms() && parser.validate(input).conforms(), "recognize/parse fixture " + index);
            M3GrammarParseTree tree = parsed.requireTree();
            check(M3ParserFixtures.evaluate(tree, tree.root()) == values[index], "precedence and derivation value " + index);
            check(tree.grammar() == grammar && tree.tokens().source() == input.text(), "tree owner identities " + index);
            check(tree.fromToken(tree.root()) == 0 && tree.toToken(tree.root()) == tree.tokens().size(), "complete input span " + index);
            check(M3ParserFixtures.grammarDigest(tree).equals(M3ParserFixtures.grammarDigest(parser.parse(input).requireTree())),
                    "deterministic derivation " + index);
            for (int node = 0; node < tree.nodeCount(); node++) {
                check(tree.fromToken(node) >= 0 && tree.toToken(node) >= tree.fromToken(node)
                        && tree.toToken(node) <= tree.tokens().size(), "bounded node token span");
                int[] children = tree.children(node);
                if (children.length != 0) {
                    int expectedChild = tree.child(node, 0);
                    children[0] = -1;
                    check(tree.child(node, 0) == expectedChild, "defensive child boundary");
                }
            }
        }
        for (String invalid : List.of("", "12 +", "1 )", "a", "2 ** 3")) {
            M3ParseResult failure = parser.parse(source(invalid));
            check(!failure.accepted() && !failure.conforms() && failure.syntax().tree().isEmpty(), "strict failure retains no tree");
            check(!parser.validate(source(invalid)).conforms(), "recognizer agrees with rejected syntax");
            expect(M3GrammarException.class, failure::requireTree);
        }
        var diagnostic = parser.validate(source("12 +"));
        check(diagnostic.furthestToken() == 2 && diagnostic.expectedSymbols().length != 0, "failure frontier includes expected terminals");
        int[] expected = diagnostic.expectedSymbols();
        int remembered = expected[0];
        expected[0] = -1;
        check(diagnostic.expectedSymbols()[0] == remembered, "defensive diagnostic symbols");
    }

    private static void lexerSpansAndErrors() {
        M3Parser parser = new M3Parser(M3ParserFixtures.expression());
        String text = " \u03b1 + \"\ud83d\ude00\"\r\n/* ignored */ 12";
        M3TokenStream tokens = parser.lex(source(text));
        check(tokens.source() == text && tokens.size() == 4, "zero-copy source and skipped whitespace/comments");
        check(tokens.start(0) == 1 && tokens.end(0) == 2 && tokens.textAt(0).equals("\u03b1"), "UTF-16 identifier span");
        check(tokens.tokenClass(2) == M3TokenClass.STRING && tokens.length(2) == 4, "quoted supplementary literal span");
        check(tokens.start(3) == text.lastIndexOf("12") && tokens.textAt(3).equals("12"), "CRLF/comment offsets retained");
        for (int token = 0; token < tokens.size(); token++) {
            check(tokens.textAt(token).equals(text.substring(tokens.start(token), tokens.end(token))), "token text comes from exact source range");
            check(tokens.hashCodeAt(token) == tokens.textAt(token).hashCode(), "allocation-free token hash agrees with String");
        }
        expect(IndexOutOfBoundsException.class, () -> tokens.start(-1));
        expect(IndexOutOfBoundsException.class, () -> tokens.charAt(0, tokens.length(0)));
        expect(M3GrammarException.class, () -> parser.lex(source("\"unterminated")));
        expect(M3GrammarException.class, () -> parser.lex(source("1 /* unterminated")));
        M3TokenStream permissive = parser.lex(source("1e+"));
        check(permissive.size() == 1 && permissive.tokenClass(0) == M3TokenClass.NUMBER,
                "generic numeric token policy is not a strict JSON number validator");
    }

    private static M3GrammarBuilder builder(String identity, M3GrammarStrictness strictness) {
        return new M3GrammarBuilder(identity, M3GrammarFamily.CUSTOM, strictness,
                M3LexerProfile.builder("literal-fixture-v1").build()).start("S");
    }

    private static void nullableAndLeftRecursive() {
        M3Grammar first = builder("nullable-late-waiter-a", M3GrammarStrictness.STRICT)
                .production("S", M3ParserFixtures.nt("A"), M3ParserFixtures.nt("B"))
                .production("A").production("B", M3ParserFixtures.nt("A")).build();
        M3Grammar second = builder("nullable-late-waiter-b", M3GrammarStrictness.STRICT)
                .production("S", M3ParserFixtures.nt("B"), M3ParserFixtures.nt("A"))
                .production("B", M3ParserFixtures.nt("A")).production("A").build();
        for (M3Grammar grammar : List.of(first, second)) {
            M3Parser parser = new M3Parser(grammar);
            check(parser.validate(source("")).conforms(), "completed nullable symbol reaches later waiter");
            M3GrammarParseTree tree = parser.parse(source("")).requireTree();
            check(tree.tokens().size() == 0 && tree.fromToken(tree.root()) == 0 && tree.toToken(tree.root()) == 0,
                    "nullable parse tree preserves empty source span");
            check(tree.nodeCount() >= 4 && tree.nodeCount() <= 8, "nullable tree has finite real derivation");
        }
        M3Grammar cyclic = builder("cyclic-nullable", M3GrammarStrictness.STRICT)
                .production("S", M3ParserFixtures.nt("A"))
                .production("A", M3ParserFixtures.nt("A")).production("A").build();
        M3Parser parser = new M3Parser(cyclic);
        check(parser.validate(source("")).conforms(), "cyclic-nullable recognition terminates");
        try {
            check(parser.parse(source("")).requireTree().nodeCount() < 16, "finite selected nullable derivation");
        } catch (M3GrammarException documentedRefusal) {
            check(documentedRefusal.getMessage().contains("cyclic nullable"), "only explicit cyclic-tree refusal is allowed");
        }
        expect(M3GrammarException.class, () -> builder("undefined", M3GrammarStrictness.STRICT)
                .production("S", M3ParserFixtures.nt("Missing")).build());
    }

    private static void collisionIsolation() {
        check("Aa".hashCode() == "BB".hashCode(), "literal hash collision fixture");
        M3Grammar aa = builder("same-identity", M3GrammarStrictness.STRICT)
                .production("S", M3GrammarRef.literalSensitive("Aa")).build();
        M3Grammar bb = builder("same-identity", M3GrammarStrictness.STRICT)
                .production("S", M3GrammarRef.literalSensitive("BB")).build();
        check(aa.fingerprint() == bb.fingerprint(), "exact grammar-fingerprint collision fixture");
        check(aa.decisionIndex() != bb.decisionIndex(), "decision index belongs to grammar identity");
        check(new M3Parser(aa).parse(source("Aa")).conforms(), "first colliding grammar recognizes its literal");
        check(!new M3Parser(aa).parse(source("BB")).conforms(), "candidate hash never proves literal equality");
        check(new M3Parser(bb).parse(source("BB")).conforms(), "second colliding grammar keeps its own dispatch");
        check(!new M3Parser(bb).parse(source("Aa")).conforms(), "second grammar cannot reuse first grammar semantics");
        check(aa.decisionIndex() == aa.decisionIndex(), "same immutable grammar reuses its decision index");

        check("an".hashCode() == "c0".hashCode(), "case-folded candidate collision fixture");
        M3Grammar insensitive = builder("insensitive-collision", M3GrammarStrictness.STRICT)
                .production("S", M3GrammarRef.literalInsensitive("an")).build();
        M3Parser parser = new M3Parser(insensitive);
        check(parser.parse(source("AN")).conforms() && !parser.parse(source("c0")).conforms(),
                "case-folded hashes still require exact case policy proof");
    }

    private static void acceptancePolicies() {
        M3Grammar tolerant = builder("tolerant", M3GrammarStrictness.TOLERANT)
                .production("S", M3GrammarRef.literal("yes")).build();
        M3ParseResult result = new M3Parser(tolerant).parse(source("no"));
        check(result.accepted() && !result.conforms() && result.syntax().tree().isEmpty(),
                "tolerant acceptance does not invent grammar conformance or tree");
    }

    private static void resourceRefusals() {
        M3Grammar grammar = M3ParserFixtures.expression();
        M3GrammarLimits[] limits = {
            new M3GrammarLimits(2, 128, 4096, 4096, 128, 100000),
            new M3GrammarLimits(1024, 2, 4096, 4096, 128, 100000),
            new M3GrammarLimits(1024, 128, 1, 4096, 128, 100000),
            new M3GrammarLimits(1024, 128, 4096, 1, 128, 100000),
            new M3GrammarLimits(1024, 128, 4096, 4096, 1, 100000),
            new M3GrammarLimits(1024, 128, 4096, 4096, 128, 1)
        };
        String[] inputs = {"123", "1+2", "1", "1", "1", "1"};
        for (int index = 0; index < limits.length; index++) {
            M3Parser bounded = new M3Parser(grammar, limits[index]);
            String input = inputs[index];
            check(bounded.limits() == limits[index], "explicit budget identity retained");
            expect(M3GrammarException.class, () -> bounded.parse(source(input)));
        }
        expect(M3GrammarException.class, () -> new M3Parser(grammar, limits[0]).validate(source("123")));
        expect(M3GrammarException.class, () -> new M3Parser(grammar, limits[1]).lex(source("1+2")));
        check(new M3Parser(grammar).parse(source("1+2")).conforms(), "budget failure does not poison grammar or successor sessions");
    }

    private static void parallelSessions() throws Exception {
        M3Parser parser = new M3Parser(M3ParserFixtures.expression());
        List<Future<Long>> results = new ArrayList<>();
        try (var workers = Executors.newFixedThreadPool(4)) {
            for (int ordinal = 0; ordinal < 16; ordinal++) {
                int value = ordinal;
                results.add(workers.submit(() -> {
                    var tree = parser.parse(source(value + " + 2 * 3")).requireTree();
                    return M3ParserFixtures.evaluate(tree, tree.root());
                }));
            }
            for (int ordinal = 0; ordinal < results.size(); ordinal++) {
                check(results.get(ordinal).get() == ordinal + 6L, "independent deterministic concurrent session " + ordinal);
            }
        }
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    @FunctionalInterface private interface Operation { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Operation action) {
        try { action.run(); }
        catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return type.cast(failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
