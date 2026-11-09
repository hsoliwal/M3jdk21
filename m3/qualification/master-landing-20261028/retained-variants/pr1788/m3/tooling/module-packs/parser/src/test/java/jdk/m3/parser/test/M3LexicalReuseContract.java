/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3ParseResult;
import jdk.m3.parser.M3Parser;
import jdk.m3.parser.grammar.M3Grammar;
import jdk.m3.parser.grammar.M3GrammarBuilder;
import jdk.m3.parser.grammar.M3GrammarFamily;
import jdk.m3.parser.grammar.M3GrammarLexer;
import jdk.m3.parser.grammar.M3GrammarLimits;
import jdk.m3.parser.grammar.M3GrammarParseResult;
import jdk.m3.parser.grammar.M3GrammarParseTree;
import jdk.m3.parser.grammar.M3GrammarParser;
import jdk.m3.parser.grammar.M3GrammarRecognizer;
import jdk.m3.parser.grammar.M3GrammarRef;
import jdk.m3.parser.grammar.M3GrammarStrictness;
import jdk.m3.parser.grammar.M3GrammarValidation;
import jdk.m3.parser.grammar.M3LexerProfile;
import jdk.m3.parser.grammar.M3TokenClass;
import jdk.m3.parser.grammar.M3TokenStream;

/** Original-owner differential observations, then optional private reuse/lifetime evidence. */
public final class M3LexicalReuseContract {
    private static final MessageDigest OBSERVATIONS = digest();
    private static int checks;

    private M3LexicalReuseContract() { }

    public static void main(String[] arguments) throws Exception {
        List<String> modes = Arrays.asList(arguments);
        for (String mode : modes) {
            if (!mode.equals("--expect-reuse") && !mode.equals("--retention")) {
                throw new IllegalArgumentException("unknown mode: " + mode);
            }
        }
        lexicalCorpus();
        facadeAndTrees();
        mutableInputs();
        workBoundaries();
        independentLimits();
        concurrentSessions();
        System.out.println("M3_LEXICAL_REUSE_PUBLIC_PASS checks=" + checks
                + " digest=" + HexFormat.of().formatHex(OBSERVATIONS.digest()));
        if (modes.contains("--expect-reuse")) {
            int before = checks;
            privateReuse();
            System.out.println("M3_LEXICAL_REUSE_PRIVATE_PASS checks=" + (checks - before));
        }
        if (modes.contains("--retention")) {
            retainedProfileDoesNotOwnSource();
            System.out.println("M3_LEXICAL_REUSE_RETENTION_PASS");
        }
    }

    private static M3LexerProfile profile(int flavor) {
        M3LexerProfile.Builder builder = M3LexerProfile.builder("lexical-reuse-fixture-v1")
                .operators("===", "==", "!=", "=>", "+", "-", "*", "(", ")");
        return switch (flavor) {
            case 0 -> builder.build();
            case 1 -> builder.singleQuoteString(true).doubleQuoteString(true)
                    .lineCommentSlashSlash(true).lineCommentDashDash(true).blockComments(true).build();
            case 2 -> builder.doubleQuoteIdentifier(true).backtickIdentifier(true)
                    .bracketQuotedIdentifier(true).questionParameter(true).colonParameter(true)
                    .atParameter(true).dollarParameter(true).hexNumbers(true).build();
            case 3 -> builder.singleQuoteString(true).backtickJsonLiteral(true)
                    .hashNamePlaceholder(true).colonValuePlaceholder(true)
                    .specialNumberLiterals(true).dollarInIdentifier(true).build();
            default -> throw new AssertionError("unknown fixture policy");
        };
    }

    private static M3Grammar grammar(M3LexerProfile profile) {
        return new M3GrammarBuilder("lexical-reuse-sum-v1", M3GrammarFamily.CUSTOM,
                M3GrammarStrictness.STRICT, profile).start("Sum")
                .production("Sum", M3GrammarRef.nonTerminal("Sum"), M3GrammarRef.literal("+"),
                        M3GrammarRef.nonTerminal("Atom"))
                .production("Sum", M3GrammarRef.nonTerminal("Atom"))
                .production("Atom", M3GrammarRef.token(M3TokenClass.NUMBER))
                .production("Atom", M3GrammarRef.literal("("), M3GrammarRef.nonTerminal("Sum"),
                        M3GrammarRef.literal(")"))
                .build();
    }

    private static void lexicalCorpus() {
        String[] sources = {"", " \t\r\n", "1 + 2 + 3", "1e+ .5 0xFF 1_000 2.3e-7",
                "alpha _x $name true FALSE null Infinity NaN", "a===b==c!=d=>e",
                "// comment\r\n1 -- tail\n+ /* block */ 2", "'a''b' \"x\\\"y\"",
                "`a``b` [a]]b] ?12 :name @name $name #name", "/* unfinished", "'unfinished",
                "\"unfinished", "[unfinished", "`unfinished", "a\uD800b\uDC00c\uD83D\uDE00d\u0000"};
        for (int flavor = 0; flavor < 4; flavor++) {
            M3GrammarLexer lexer = new M3GrammarLexer(profile(flavor));
            for (String source : sources) {
                M3TokenStream[] retained = new M3TokenStream[2];
                String first = observe(() -> tokens(retained[0] = lexer.lex(source)));
                String second = observe(() -> tokens(retained[1] = lexer.lex(source)));
                same(first, second, "cold/warm lex flavor=" + flavor);
                if (retained[0] != null) {
                    check(retained[0] != retained[1], "fresh lexical envelope");
                    check(retained[0].source() == source && retained[1].source() == source,
                            "exact String source owner");
                    for (int token = 0; token < retained[0].size(); token++) {
                        M3TokenStream stream = retained[0];
                        String text = stream.textAt(token);
                        check(stream.hashCodeAt(token) == text.hashCode(), "String-compatible token hash");
                        check(stream.matchesLiteral(token, text, false), "exact token literal");
                        check(stream.matchesLiteral(token, text, true), "folded token literal");
                        for (int unit = 0; unit < text.length(); unit++) {
                            check(stream.charAt(token, unit) == text.charAt(unit), "token UTF-16 unit");
                        }
                    }
                }
                record("lex:" + flavor, first);
                Reference.reachabilityFence(retained);
            }
        }
        M3GrammarLexer lexer = new M3GrammarLexer(profile(1));
        String failure = observe(() -> tokens(lexer.lex((CharSequence) null)));
        check(failure.equals("ERROR:java.lang.NullPointerException:source"), "null source contract");
        record("null-source", failure);
    }

    private static void facadeAndTrees() {
        M3Parser parser = new M3Parser(grammar(profile(1)));
        String text = new String("(1 + 2) + 3 /* retained */");
        M3File source = M3File.of(URI.create("memory:/first.expr"), text);
        M3TokenStream lexed = parser.lex(source);
        M3GrammarValidation validation = parser.validate(source);
        M3ParseResult first = parser.parse(source);
        M3ParseResult second = parser.parse(source);
        check(validation.conforms() && first.conforms() && second.conforms(), "facade acceptance");
        check(first != second && first.requireTree() != second.requireTree(), "fresh result and tree");
        check(first.requireTree().tokens() != second.requireTree().tokens(), "fresh tree token envelope");
        check(first.source() == source && second.source() == source, "facade source owner");
        check(first.requireTree().tokens().source() == text, "tree source owner");
        same(tokens(lexed), tokens(first.requireTree().tokens()), "facade lexical projection");
        same(parsed(first.syntax()), parsed(second.syntax()), "facade full tree");
        same(validated(validation), validated(first.syntax().validation()), "recognition/tree validation");
        record("facade", parsed(first.syntax()));
        M3File alternate = M3File.of(URI.create("memory:/alternate.expr"), text);
        M3ParseResult other = parser.parse(alternate);
        check(other.source() == alternate && !other.source().uri().equals(source.uri()),
                "URI remains per-result even for shared String");
        same(parsed(first.syntax()), parsed(other.syntax()), "same text different URI");
        for (String invalid : new String[] {"1 +", "1 2", "", "(1 + 2", "1 + true"}) {
            M3File file = M3File.of(URI.create("memory:/invalid.expr"), invalid);
            String once = parsed(parser.parse(file).syntax());
            String twice = parsed(parser.parse(file).syntax());
            same(once, twice, "repeat grammar rejection");
            check(!parser.parse(file).conforms(), "invalid fixture rejected");
            record("invalid", once);
        }
        Reference.reachabilityFence(lexed);
        Reference.reachabilityFence(first);
        Reference.reachabilityFence(second);
    }

    private static void mutableInputs() {
        M3GrammarLexer lexer = new M3GrammarLexer(profile(1));
        Tape input = new Tape("12 + 3");
        M3TokenStream first = lexer.lex(input);
        same(input.events.toString(), "L;C0;C1;C2;C3;C4;C5;", "first snapshot callbacks");
        input.events.setLength(0);
        M3TokenStream repeated = lexer.lex(input);
        same(tokens(first), tokens(repeated), "mutable owner repeated text");
        check(first.source() != repeated.source(), "non-String snapshot remains fresh");
        same(input.events.toString(), "L;C0;C1;C2;C3;C4;C5;", "repeat snapshot callbacks");
        input.reset("4 + 56");
        M3TokenStream changed = lexer.lex(input);
        check(changed.textAt(0).equals("4") && first.textAt(0).equals("12"), "mutable input edit");
        record("mutable-first", tokens(first));
        record("mutable-changed", tokens(changed));
        input.reset("12 + 3");
        input.failAt = 2;
        try {
            lexer.lex(input);
            throw new AssertionError("snapshot callback exception swallowed");
        } catch (IllegalStateException failure) {
            check(failure == input.failure, "callback exception identity");
        }
        same(input.events.toString(), "L;C0;C1;C2;", "callback failure order");
        record("mutable-callback", input.events.toString());
        input.reset("12");
        M3GrammarLimits sourceLimit = new M3GrammarLimits(1, 8, 8, 8, 8, 100);
        String oversized = observe(() -> tokens(new M3GrammarLexer(profile(1), sourceLimit).lex(input)));
        check(oversized.endsWith("maxSourceChars"), "source admission before callbacks");
        same(input.events.toString(), "L;", "no charAt after source refusal");
        record("source-refusal", oversized + input.events);
        input.reset("12");
        M3GrammarLimits workLimit = new M3GrammarLimits(8, 8, 8, 8, 8, 1);
        String exhausted = observe(() -> tokens(new M3GrammarLexer(profile(1), workLimit).lex(input)));
        check(exhausted.endsWith("maxWork"), "snapshot work admission before callbacks");
        same(input.events.toString(), "L;", "no charAt after snapshot work refusal");
        record("snapshot-work-refusal", exhausted + input.events);
        Reference.reachabilityFence(first);
        Reference.reachabilityFence(repeated);
    }

    private static void workBoundaries() {
        String source = new String("1 + 2");
        int combinedRefusals = 0;
        int successes = 0;
        for (long work = 1; work <= 800; work++) {
            M3GrammarLimits limits = new M3GrammarLimits(128, 64, 1024, 256, 64, work);
            M3Grammar grammar = grammar(profile(1));
            M3GrammarLexer lexer = new M3GrammarLexer(grammar.lexerProfile(), limits);
            M3GrammarParser parser = new M3GrammarParser(grammar, limits);
            M3GrammarRecognizer recognizer = new M3GrammarRecognizer(grammar, limits);
            M3TokenStream[] held = new M3TokenStream[1];
            String lexical = observe(() -> tokens(held[0] = lexer.lex(source)));
            String warmParse = observe(() -> parsed(parser.parse(source)));
            String coldParse = observe(() -> parsed(parser.parse(new String(source))));
            same(coldParse, warmParse, "aggregate parse work=" + work);
            if (lexical.startsWith("OK:") && warmParse.endsWith("maxWork")) combinedRefusals++;
            if (warmParse.startsWith("OK:")) successes++;
            observe(() -> tokens(held[0] = lexer.lex(source)));
            String warmValidate = observe(() -> validated(recognizer.validate(source)));
            String coldValidate = observe(() -> validated(recognizer.validate(new String(source))));
            same(coldValidate, warmValidate, "aggregate validation work=" + work);
            record("work:" + work, lexical + '\n' + warmParse + '\n' + warmValidate);
            Reference.reachabilityFence(held);
        }
        check(combinedRefusals > 0, "sweep observes successful lex plus refused aggregate parse");
        check(successes > 0, "sweep crosses successful aggregate parse boundary");
        record("work-boundary-coverage", combinedRefusals + ":" + successes);
    }

    private static void independentLimits() {
        String source = new String("1 + 2");
        M3GrammarLimits[] bounds = {
                new M3GrammarLimits(4, 64, 1024, 256, 64, 10000),
                new M3GrammarLimits(128, 2, 1024, 256, 64, 10000),
                new M3GrammarLimits(128, 64, 1, 256, 64, 10000),
                new M3GrammarLimits(128, 64, 1024, 1, 64, 10000),
                new M3GrammarLimits(128, 64, 1024, 256, 1, 10000)};
        String[] names = {"maxSourceChars", "maxTokens", "maxStates", "maxTreeNodes", "maxTreeDepth"};
        for (int index = 0; index < bounds.length; index++) {
            M3Grammar grammar = grammar(profile(1));
            M3TokenStream admitted = new M3GrammarLexer(grammar.lexerProfile()).lex(source);
            M3GrammarParser limited = new M3GrammarParser(grammar, bounds[index]);
            String first = observe(() -> parsed(limited.parse(source)));
            String second = observe(() -> parsed(limited.parse(source)));
            same(first, second, "repeat independent limit");
            check(first.endsWith(names[index]), "independent limit " + names[index]);
            record("limit:" + names[index], first);
            Reference.reachabilityFence(admitted);
        }
        M3GrammarLimits zero = new M3GrammarLimits(0, 0, 8, 8, 8, 1);
        M3GrammarLexer empty = new M3GrammarLexer(profile(0), zero);
        M3TokenStream first = empty.lex("");
        M3TokenStream second = empty.lex("");
        check(first.size() == 0 && second.size() == 0 && first != second, "zero-token empty source");
        record("zero", tokens(second));
        Reference.reachabilityFence(first);
    }

    private static void concurrentSessions() throws Exception {
        M3Grammar grammar = grammar(profile(1));
        M3Parser parser = new M3Parser(grammar);
        M3File[] files = new M3File[8];
        String[] expected = new String[files.length];
        for (int index = 0; index < files.length; index++) {
            String source = index + " + (2 + 3)";
            files[index] = M3File.of(URI.create("memory:/parallel-" + index + ".expr"), source);
            expected[index] = parsed(new M3GrammarParser(grammar(profile(1))).parse(source));
        }
        ExecutorService workers = Executors.newFixedThreadPool(4);
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int task = 0; task < 128; task++) {
                int slot = task % files.length;
                futures.add(workers.submit(() -> {
                    M3ParseResult retained = null;
                    String observed = "";
                    for (int repeat = 0; repeat < 5; repeat++) {
                        M3ParseResult result = parser.parse(files[slot]);
                        observed = parsed(result.syntax());
                        if (!observed.equals(expected[slot]) || result.source() != files[slot]
                                || result == retained) throw new AssertionError("concurrent result drift");
                        retained = result;
                    }
                    Reference.reachabilityFence(retained);
                    return observed;
                }));
            }
            for (int task = 0; task < futures.size(); task++) {
                String observed = futures.get(task).get();
                same(expected[task % files.length], observed, "concurrent invocation");
                record("parallel:" + task, observed);
            }
        } finally {
            workers.shutdownNow();
        }
    }

    private static void privateReuse() throws Exception {
        String source = new String("1 + 2");
        M3LexerProfile profile = profile(1);
        M3GrammarLexer lexer = new M3GrammarLexer(profile);
        M3TokenStream first = lexer.lex(source);
        M3TokenStream second = lexer.lex(source);
        shared(first, second, true, "same admitted generation");
        check(first != second, "reuse still returns a fresh envelope");
        M3TokenStream freshText = lexer.lex(new String(source));
        shared(second, freshText, false, "equal text has distinct source owner");
        M3TokenStream replaced = lexer.lex(source);
        shared(first, replaced, false, "one memo retains only the most recent generation");
        M3TokenStream equalLimits = new M3GrammarLexer(profile, M3GrammarLimits.defaults()).lex(source);
        shared(replaced, equalLimits, true, "equal limits value qualifies");
        M3GrammarLimits defaults = M3GrammarLimits.defaults();
        M3GrammarLimits different = new M3GrammarLimits(defaults.maxSourceChars(), defaults.maxTokens(),
                defaults.maxStates(), defaults.maxTreeNodes() - 1, defaults.maxTreeDepth(), defaults.maxWork());
        M3TokenStream differentLimits = new M3GrammarLexer(profile, different).lex(source);
        shared(equalLimits, differentLimits, false, "complete limits key includes tree nodes");
        M3TokenStream differentOwner = new M3GrammarLexer(profile(1), different).lex(source);
        shared(differentLimits, differentOwner, false, "equal policy fingerprints do not share owners");
        Object memo = field(M3LexerProfile.class, "lexicalReuse").get(profile);
        Field tokens = field(memo.getClass(), "tokens");
        check(tokens.getType() == WeakReference.class, "memo token retention is weak");
        WeakReference<?> weak = (WeakReference<?>) tokens.get(memo);
        check(weak.get() == differentLimits, "memo follows latest returned result");
        weak.clear();
        M3TokenStream afterClear = new M3GrammarLexer(profile, different).lex(source);
        shared(differentLimits, afterClear, false, "cleared weak memo causes a normal scan");
        same(tokens(differentLimits), tokens(afterClear), "clear fallback observations");
        String failure = observe(() -> tokens(new M3GrammarLexer(profile, different).lex("/* unfinished")));
        check(failure.contains("unterminated block comment"), "malformed scan refused");
        M3TokenStream afterFailure = new M3GrammarLexer(profile, different).lex(source);
        shared(afterClear, afterFailure, true, "failed scan does not publish partial tokens");
        Tape tape = new Tape("1 + 2");
        M3TokenStream oneSnapshot = lexer.lex(tape);
        M3TokenStream nextSnapshot = lexer.lex(tape);
        shared(oneSnapshot, nextSnapshot, false, "effectful input is snapshotted every time");
        M3Parser parser = new M3Parser(grammar(profile));
        M3File file = M3File.of(URI.create("memory:/retained.expr"), source);
        M3TokenStream initial = parser.lex(file);
        M3ParseResult parsed = parser.parse(file);
        shared(initial, parsed.requireTree().tokens(), true, "facade lex-to-parse integration");
        M3ParseResult again = parser.parse(file);
        shared(parsed.requireTree().tokens(), again.requireTree().tokens(), true, "retained tree reuse");
        check(parsed.requireTree() != again.requireTree(), "parse trees remain invocation-local");
        Reference.reachabilityFence(first);
        Reference.reachabilityFence(second);
        Reference.reachabilityFence(replaced);
        Reference.reachabilityFence(equalLimits);
        Reference.reachabilityFence(differentLimits);
        Reference.reachabilityFence(afterClear);
        Reference.reachabilityFence(afterFailure);
        Reference.reachabilityFence(initial);
        Reference.reachabilityFence(parsed);
    }

    private static void retainedProfileDoesNotOwnSource() {
        M3LexerProfile profile = profile(0);
        WeakReference<?>[] references = unretainedSource(profile);
        for (int attempt = 0; attempt < 64
                && (references[0].get() != null || references[1].get() != null); attempt++) {
            byte[][] pressure = new byte[8][];
            for (int index = 0; index < pressure.length; index++) pressure[index] = new byte[128 * 1024];
            System.gc();
            Reference.reachabilityFence(pressure);
        }
        check(references[0].get() == null && references[1].get() == null,
                "live profile must not retain source or returned token stream");
        Reference.reachabilityFence(profile);
    }

    private static WeakReference<?>[] unretainedSource(M3LexerProfile profile) {
        String source = new String("identifier ".repeat(2048));
        M3TokenStream stream = new M3GrammarLexer(profile).lex(source);
        return new WeakReference<?>[] {new WeakReference<>(source), new WeakReference<>(stream)};
    }

    private static void shared(M3TokenStream left, M3TokenStream right, boolean expected, String label)
            throws ReflectiveOperationException {
        for (String lane : new String[] {"starts", "ends", "classes"}) {
            Field field = field(M3TokenStream.class, lane);
            check((field.get(left) == field.get(right)) == expected, label + ':' + lane);
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static String tokens(M3TokenStream stream) {
        StringBuilder value = new StringBuilder("TOKENS:");
        value.append(stream.size()).append(':').append(stream.primitivePayloadBytes()).append(':');
        appendText(value, stream.source());
        for (int token = 0; token < stream.size(); token++) {
            value.append('|').append(stream.start(token)).append(',').append(stream.end(token)).append(',')
                    .append(stream.tokenClass(token)).append(',').append(stream.hashCodeAt(token)).append(',')
                    .append(stream.foldedHashCodeAt(token)).append(',');
            appendText(value, stream.textAt(token));
        }
        return value.toString();
    }

    private static String validated(M3GrammarValidation validation) {
        return "VALID:" + validation.accepted() + ':' + validation.conforms() + ':'
                + validation.tokenCount() + ':' + validation.furthestToken() + ':'
                + validation.expectedEndOfInput() + ':' + Arrays.toString(validation.expectedSymbols());
    }

    private static String parsed(M3GrammarParseResult result) {
        StringBuilder value = new StringBuilder(validated(result.validation()));
        if (result.tree().isEmpty()) return value.append(":NO_TREE").toString();
        M3GrammarParseTree tree = result.requireTree();
        value.append(":TREE:").append(tree.root()).append(':').append(tree.nodeCount()).append(':')
                .append(tree.primitivePayloadBytes()).append(':').append(tokens(tree.tokens()));
        for (int node = 0; node < tree.nodeCount(); node++) {
            value.append('|').append(tree.symbol(node)).append(',').append(tree.production(node)).append(',')
                    .append(tree.tokenIndex(node)).append(',').append(tree.fromToken(node)).append(',')
                    .append(tree.toToken(node)).append(',').append(Arrays.toString(tree.children(node)));
        }
        return value.toString();
    }

    private static void appendText(StringBuilder target, String text) {
        target.append(text.length()).append(':');
        for (int unit = 0; unit < text.length(); unit++) target.append((int) text.charAt(unit)).append(',');
    }

    private static String observe(Observation observation) {
        try {
            return "OK:" + observation.run();
        } catch (RuntimeException failure) {
            return "ERROR:" + failure.getClass().getName() + ':' + failure.getMessage();
        }
    }

    private static void record(String label, String value) {
        put(label);
        put(value);
    }

    private static void put(String value) {
        int length = value.length();
        for (int shift = 24; shift >= 0; shift -= 8) OBSERVATIONS.update((byte) (length >>> shift));
        for (int index = 0; index < length; index++) {
            char unit = value.charAt(index);
            OBSERVATIONS.update((byte) (unit >>> 8));
            OBSERVATIONS.update((byte) unit);
        }
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void same(String expected, String actual, String label) {
        check(expected.equals(actual), label + "\nexpected=" + expected + "\nactual=" + actual);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    @FunctionalInterface
    private interface Observation { String run(); }

    private static final class Tape implements CharSequence {
        private String value;
        private final StringBuilder events = new StringBuilder();
        private final IllegalStateException failure = new IllegalStateException("input callback failure");
        private int failAt = -1;

        private Tape(String value) { this.value = value; }
        private void reset(String replacement) { value = replacement; failAt = -1; events.setLength(0); }
        @Override public int length() { events.append("L;"); return value.length(); }
        @Override public char charAt(int index) {
            events.append('C').append(index).append(';');
            if (index == failAt) throw failure;
            return value.charAt(index);
        }
        @Override public CharSequence subSequence(int start, int end) {
            throw new AssertionError("lexer must not call source.subSequence");
        }
        @Override public String toString() { throw new AssertionError("lexer must not call source.toString"); }
    }
}
