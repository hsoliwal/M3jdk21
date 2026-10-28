/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Executable T-Rex subset parser, token index, precomputed predicates and tagged matcher
 * @modules java.base/jdk.internal.mindex
 * @run main M3TokenRegexTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jdk.internal.mindex.M3TokenIndex;
import jdk.internal.mindex.M3TokenMatcher;
import jdk.internal.mindex.M3TokenPattern;
import jdk.internal.mindex.M3TokenPredicateCache;

/**
 * Reference cases transpiled from Square T-Rex (c84ddf5a, Apache-2.0: MatcherTest, PatternTest,
 * README grammar examples) plus the Java-source example of REGEX_PRECOMPUTE_DESIGN 2f. T-Rex
 * inputs are re-tokenized here by single spaces (no CoreNLP); expected spans are kept verbatim.
 */
public final class M3TokenRegexTest {
    private static final String[] KINDS = {"NONE", "KEYWORD", "IDENT", "OPERATOR", "NUMBER", "PUNCT",
            "STRING", "COMMENT", "WS", "NEWLINE", "WORD"};
    private static final String JAVA = "int count = 0; String s = \"count\"; // count\ncount++;";
    private static int checks;

    private M3TokenRegexTest() {
    }

    public static void main(String[] args) {
        parserCases();
        refusalCases();
        indexCases();
        predicateCases();
        matcherCases();
        System.out.println("M3TokenRegexTest PASS checks=" + checks);
    }

    private static void parserCases() {
        parses("a b c", "seq([{text=\"a\"}] [{text=\"b\"}] [{text=\"c\"}])");
        parses("(?<a> a) b c d e",
                "seq(g1<a>([{text=\"a\"}]) [{text=\"b\"}] [{text=\"c\"}] [{text=\"d\"}] [{text=\"e\"}])");
        parses("((?<a> a) b) c d e",
                "seq(g1(seq(g2<a>([{text=\"a\"}]) [{text=\"b\"}])) [{text=\"c\"}] [{text=\"d\"}] [{text=\"e\"}])");
        parses("((?<a> a) | b) b c d e", "seq(g1(alt(g2<a>([{text=\"a\"}]), [{text=\"b\"}]))"
                + " [{text=\"b\"}] [{text=\"c\"}] [{text=\"d\"}] [{text=\"e\"}])");
        parses("((?<a> a) | (?<b> b)) b (?<c> c)",
                "seq(g1(alt(g2<a>([{text=\"a\"}]), g3<b>([{text=\"b\"}]))) [{text=\"b\"}] g4<c>([{text=\"c\"}]))");
        parses("(a{1} | a{2}) (a{2} | a{1})", "seq(g1(alt(rep{1,1}([{text=\"a\"}]), rep{2,2}([{text=\"a\"}])))"
                + " g2(alt(rep{2,2}([{text=\"a\"}]), rep{1,1}([{text=\"a\"}]))))");
        parses("/[abc]/", "[{text=/[abc]/}]");
        parses("hello (world | [{movie: \"Star Wars\"}]+)",
                "seq([{text=\"hello\"}] g1(alt([{text=\"world\"}], rep{1,-1}([{movie=\"Star Wars\"}]))))");
        parses("[{ner:PERSON}]+ [{lemma:be}] born (on [!{pos:/VB.?/}])? in [{ner:LOCATION}]+",
                "seq(rep{1,-1}([{ner=\"PERSON\"}]) [{lemma=\"be\"}] [{text=\"born\"}]"
                + " rep{0,1}(g1(seq([{text=\"on\"}] [!{pos=/VB.?/}]))) [{text=\"in\"}]"
                + " rep{1,-1}([{ner=\"LOCATION\"}]))");
        parses("[({word:play} | {word:run}) & !{pos:NN}]",
                "[(({text=\"play\"}|{text=\"run\"})&!{pos=\"NN\"})]");
        parses("[{pos:/NN.?.?/} | {pos:JJ}]+", "rep{1,-1}([({pos=/NN.?.?/}|{pos=\"JJ\"})])");
        parses("[{key>10}]", "[{key>10}]");
        parses("[{key<=-3}]", "[{key<=-3}]");
        parses("[{key!=4}]", "[{key!=4}]");
        parses("[]", "[]");
        parses("cat??", "rep{0,1}?([{text=\"cat\"}])");
        parses("cat*?", "rep{0,-1}?([{text=\"cat\"}])");
        parses("cat+?", "rep{1,-1}?([{text=\"cat\"}])");
        parses("cat{3,5}?", "rep{3,5}?([{text=\"cat\"}])");
        parses("cat{3,}", "rep{3,-1}([{text=\"cat\"}])");
        parses("cat{3}", "rep{3,3}([{text=\"cat\"}])");
        parses("[{!lemma}]", "[{!lemma}]");
        parses("[!lemma]", "[{!lemma}]");
        parses("[key:value]", "[{key=\"value\"}]");
        parses("^ a $", "seq(^ [{text=\"a\"}] $)");
        parses("a /* inner */ b // tail", "seq([{text=\"a\"}] [{text=\"b\"}])");
        parses("\"hello world\"", "[{text=\"hello world\"}]");
        parses("[{a:b} | {c:d} & {e:f}]", "[(({a=\"b\"}|{c=\"d\"})&{e=\"f\"})]");
        parses("[{kind:IDENT} & {text:count}]", "[({kind=\"IDENT\"}&{text=\"count\"})]");
        parses("[{flags:0x4} & {binding>0} & {lexeme==7}]",
                "[(({flags=\"0x4\"}&{binding>0})&{lexeme==7})]");
        M3TokenPattern named = M3TokenPattern.compile("((?<a> a) | (?<b> b)) b (?<c> c)");
        check(named.groupCount() == 4, "group count");
        check(named.groupIndex("a") == 2 && named.groupIndex("c") == 4 && named.groupIndex("z") == -1, "group index");
        check(named.groupName(1) == null && "b".equals(named.groupName(3)), "group names");
        check(named.patternRoot().length() == 64, "pattern root");
        check(!named.patternRoot().equals(M3TokenPattern.compile(named.source(),
                M3TokenPattern.CASE_INSENSITIVE).patternRoot()), "root covers flags");
    }

    private static void parses(String source, String expected) {
        String actual = M3TokenPattern.compile(source).describe();
        check(expected.equals(actual), "parse " + source + " -> " + actual + " expected " + expected);
    }

    private static void refusalCases() {
        String deep = "(".repeat(M3TokenPattern.MAX_DEPTH + 1) + "a" + ")".repeat(M3TokenPattern.MAX_DEPTH + 1);
        String groups = "(a) ".repeat(M3TokenPattern.MAX_GROUPS + 1);
        for (String bad : List.of("", "   ", "play & [{pos:NN}]+", "$FOO", "(?$name a)", "[{a:b}", "a{5,3}",
                "[{key>x}]", "(a", "a |", "| a", "[hello]", "// only a comment", "\"\"", "a{99999}", "//",
                "a b)", "[{a:}]", "/unterminated", "[{:b}]", deep, groups, "x".repeat(4097))) {
            try {
                M3TokenPattern.compile(bad);
                check(false, "refusal expected for " + bad);
            } catch (PatternSyntaxException expected) {
                check(true, "refused");
            }
        }
        try {
            M3TokenPattern.compile("a", 2);
            check(false, "unknown flag accepted");
        } catch (IllegalArgumentException expected) {
            check(true, "flags refused");
        }
    }

    private static void indexCases() {
        M3TokenIndex java = javaIndex();
        check(java.count() == 23, "java token count");
        check(java.significantCount() == 13, "significant count " + java.significantCount());
        check(java.significantAt(0) == 0 && java.significantAt(1) == 2 && java.significantAt(12) == 22, "significant view");
        check(java.spanStart(2, 3) == 4 && java.spanEnd(2, 3) == 9, "token span");
        check(java.spanStart(15, 17) == 26 && java.spanEnd(15, 17) == 34, "range span");
        check(java.spanStart(23, 23) == 52 && java.spanEnd(23, 23) == 52, "empty span at tape end");
        check(java.spanStart(5, 5) == 11 && java.spanEnd(5, 5) == 11, "empty span inside");
        check(java.retainedPrimitiveBytes() == 23L * 36, "retained bytes");
        refusesIndex(new int[2], new int[2], new long[] {0, 2}, new long[] {3, 4}, new long[2]);
        refusesIndex(new int[1], new int[1], new long[] {5}, new long[] {4}, new long[1]);
        refusesIndex(new int[1], new int[1], new long[] {-1}, new long[] {1}, new long[1]);
        refusesIndex(new int[2], new int[1], new long[] {0, 1}, new long[] {1, 2}, new long[2]);
        M3TokenIndex attrs = attributeIndex();
        check(attrs.attributeColumn("pos") == 0 && attrs.attributeColumn("ner") == -1, "attribute column");
        check(attrs.attributeAt(0, 2) == 5 && attrs.bindingAt(1) == 0, "attribute lane");
    }

    private static void refusesIndex(int[] kinds, int[] lexemes, long[] starts, long[] ends, long[] flags) {
        try {
            M3TokenIndex.of(1, kinds, lexemes, starts, ends, flags);
            check(false, "index refusal expected");
        } catch (IllegalArgumentException expected) {
            check(true, "index refused");
        }
    }

    private static void predicateCases() {
        M3TokenIndex java = javaIndex();
        Vocab vocab = javaVocab();
        check(positions("[{kind:IDENT} & {text:count}]", 0, java, JAVA, vocab).equals(List.of(2, 20)), "2f identifiers");
        check(positions("[{text:count}]", 0, java, JAVA, vocab).equals(List.of(2, 20)), "2f spelling");
        check(positions("[{kind:STRING}]", 0, java, JAVA, vocab).equals(List.of(15)), "string literal kind");
        check(positions("[!{flags:0x1}]", 0, java, JAVA, vocab).size() == java.significantCount(), "trivia mask");
        check(positions("[{text:COUNT}]", M3TokenPattern.CASE_INSENSITIVE, java, JAVA, vocab)
                .equals(List.of(2, 20)), "case-insensitive span compare");
        check(positions("[{text:COUNT}]", 0, java, JAVA, vocab).isEmpty(), "case-sensitive");
        check(positions("[{text:\"\\\"count\\\"\"}]", 0, java, JAVA, vocab).equals(List.of(15)), "OOV literal span");
        check(positions("[{kind:9999}]", 0, java, JAVA, vocab).isEmpty(), "numeric kind");
        check(characterHits(JAVA, "count").equals(List.of(4, 27, 38, 44)), "char regex sees literal and comment");
        refusesBind("[{kind:BOGUS}]", java, vocab);
        refusesBind("[{missing:3}]", java, vocab);
        M3TokenPredicateCache regex = bind("[{text:/c.*t/}]", 0, java, JAVA, vocab);
        check(regex.test(regex.pattern().nodeX(regex.pattern().rootNode()), 2), "regex row precomputed");
        check(positions("[{text:/c.*t/}]", 0, java, JAVA, vocab).equals(List.of(2, 20)), "regex spelling rows");
        check(bind("[{text:count}]", 0, java, JAVA, vocab).cacheKey().endsWith("/0/2f"), "cache key");
        M3TokenIndex attrs = attributeIndex();
        String text = "aa bb cc";
        Vocab none = new Vocab(new String[0]);
        check(positions("[{pos>2}]", 0, attrs, text, none).equals(List.of(1, 2)), "attribute compare");
        check(positions("[{!pos}]", 0, attrs, text, none).isEmpty(), "attribute present");
        check(positions("[{!missing}]", 0, attrs, text, none).equals(List.of(0, 1, 2)), "attribute absent");
        check(positions("[{binding:9}]", 0, attrs, text, none).equals(List.of(0)), "binding");
    }

    private static void refusesBind(String source, M3TokenIndex index, Vocab vocab) {
        try {
            bind(source, 0, index, JAVA, vocab);
            check(false, "bind refusal expected for " + source);
        } catch (IllegalArgumentException expected) {
            check(true, "bind refused");
        }
    }

    private static void matcherCases() {
        M3TokenMatcher fresh = matcher("a b c", "a b c d e", M3TokenMatcher.FIND_ALL_SPANS);
        expectIllegalState(fresh::start);
        expectIllegalState(fresh::end);
        expectIllegalState(() -> fresh.start(0));
        expectIllegalState(() -> fresh.start("name"));
        check(fresh.groupCount() == 0 && fresh.searchFrom() == 0, "fresh matcher");
        try {
            M3TokenMatcher.of(fresh.predicates(), 7, false);
            check(false, "mode refused");
        } catch (IllegalArgumentException expected) {
            check(true, "mode refused");
        }
        expectSpans("(a{1} | a{2}) (a{2} | a{1})", "a a a a a", M3TokenMatcher.FIND_ALL_SPANS,
                new int[][] {{0, 3}, {0, 2}, {0, 4}, {1, 4}});
        expectSpans("/[abc]/", "a b c d e", M3TokenMatcher.FIND_ALL_SPANS, new int[][] {{0, 1}, {1, 2}});
        expectSpans("a b c d e", "a b c d e", M3TokenMatcher.FIND_NEXT, new int[][] {{0, 5}});
        expectGroups("(?<a> a) b c d e", "a b c d e", new String[] {"a"}, new int[][] {{0, 1}});
        expectGroups("((?<a> a) | (?<b> b)) b (?<c> c)", "a b c d e",
                new String[] {"a", "b", "c"}, new int[][] {{0, 1}, {-1, -1}, {2, 3}});
        expectJavaFind();
    }

    private static void expectSpans(String pattern, String words, int mode, int[][] spans) {
        M3TokenMatcher m = matcher(pattern, words, mode);
        for (int[] span : spans) {
            check(m.find(), "find " + pattern);
            check(m.start() == span[0] && m.end() == span[1], "span " + pattern
                    + " actual=" + m.start() + "," + m.end());
        }
    }

    private static void expectGroups(String pattern, String words, String[] names, int[][] spans) {
        M3TokenMatcher m = matcher(pattern, words, M3TokenMatcher.FIND_ALL_SPANS);
        check(m.find(), "find " + pattern);
        for (int g = 0; g < names.length; g++) {
            check(m.start(names[g]) == spans[g][0] && m.end(names[g]) == spans[g][1], "group " + names[g]);
        }
    }

    private static void expectJavaFind() {
        M3TokenPredicateCache p = bind("[{kind:IDENT} & {text:count}]", 0, javaIndex(), JAVA, javaVocab());
        M3TokenMatcher m = M3TokenMatcher.of(p, M3TokenMatcher.FIND_NEXT, true);
        long[][] expected = {{2, 3, 4, 9}, {20, 21, 44, 49}};
        for (long[] e : expected) {
            check(m.find(), "2f find");
            check(m.start() == e[0] && m.end() == e[1], "2f tokens");
            check(m.sourceStart(0) == e[2] && m.sourceEnd(0) == e[3], "2f source span");
        }
        check(!m.find(), "2f exhausted");
        check(!m.matches(), "identifier cannot match complete projected Java tape");
    }

    private static void expectIllegalState(Runnable call) {
        try {
            call.run();
            check(false, "IllegalStateException expected");
        } catch (IllegalStateException expected) {
            check(true, "no match yet");
        }
    }

    private static M3TokenMatcher matcher(String pattern, String words, int mode) {
        String[] parts = words.split(" ");
        String[] sorted = Arrays.stream(parts).distinct().sorted().toArray(String[]::new);
        Vocab vocab = new Vocab(sorted);
        int n = parts.length;
        int[] kinds = new int[n];
        int[] lexemes = new int[n];
        long[] starts = new long[n];
        long[] ends = new long[n];
        long at = 0;
        for (int i = 0; i < n; i++) {
            kinds[i] = 10;
            lexemes[i] = vocab.lexemeOf(parts[i]);
            starts[i] = at;
            ends[i] = at + parts[i].length();
            at = ends[i] + 1;
        }
        M3TokenIndex index = M3TokenIndex.of(7, kinds, lexemes, starts, ends, new long[n]);
        return M3TokenMatcher.of(bind(pattern, 0, index, words, vocab), mode, false);
    }

    private static List<Integer> positions(String source, int flags, M3TokenIndex index, String text, Vocab vocab) {
        M3TokenPredicateCache p = bind(source, flags, index, text, vocab);
        int expr = p.pattern().nodeX(p.pattern().rootNode());
        check(p.pattern().nodeOp(p.pattern().rootNode()) == M3TokenPattern.N_TOKEN, "single-token pattern");
        List<Integer> out = new ArrayList<>();
        for (int t = 0; t < index.count(); t++) {
            if (p.test(expr, t)) {
                out.add(t);
            }
        }
        return out;
    }

    private static M3TokenPredicateCache bind(String source, int flags, M3TokenIndex index, String text, Vocab vocab) {
        return M3TokenPredicateCache.bind(M3TokenPattern.compile(source, flags), index, text, vocab);
    }

    private static List<Integer> characterHits(String text, String regex) {
        List<Integer> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(text);
        while (m.find()) {
            out.add(m.start());
        }
        return out;
    }

    /** Hand-lexed tokens of JAVA: kind, start, end (whitespace, comment and newline are trivia). */
    private static final int[][] JAVA_TOKENS = {
        {1, 0, 3}, {8, 3, 4}, {2, 4, 9}, {8, 9, 10}, {3, 10, 11}, {8, 11, 12}, {4, 12, 13}, {5, 13, 14},
        {8, 14, 15}, {2, 15, 21}, {8, 21, 22}, {2, 22, 23}, {8, 23, 24}, {3, 24, 25}, {8, 25, 26},
        {6, 26, 33}, {5, 33, 34}, {8, 34, 35}, {7, 35, 43}, {9, 43, 44}, {2, 44, 49}, {3, 49, 51}, {5, 51, 52}};

    private static M3TokenIndex javaIndex() {
        Vocab vocab = javaVocab();
        int n = JAVA_TOKENS.length;
        int[] kinds = new int[n];
        int[] lexemes = new int[n];
        long[] starts = new long[n];
        long[] ends = new long[n];
        long[] flags = new long[n];
        for (int i = 0; i < n; i++) {
            int[] t = JAVA_TOKENS[i];
            kinds[i] = t[0];
            starts[i] = t[1];
            ends[i] = t[2];
            lexemes[i] = vocab.lexemeOf(JAVA.substring(t[1], t[2]));
            flags[i] = t[0] == 8 || t[0] == 7 ? M3TokenIndex.TRIVIA
                    : t[0] == 9 ? M3TokenIndex.TRIVIA | M3TokenIndex.NEWLINE : 0;
        }
        return M3TokenIndex.of(0x2f, kinds, lexemes, starts, ends, flags);
    }

    private static Vocab javaVocab() {
        String[] words = {"++", "0", ";", "=", "String", "count", "int", "s"};
        Arrays.sort(words);
        return new Vocab(words);
    }

    private static M3TokenIndex attributeIndex() {
        return M3TokenIndex.of(3, new int[] {10, 10, 10}, new int[3], new long[] {0, 3, 6}, new long[] {2, 5, 8},
                new long[3], new int[] {9, 0, 0}, new String[] {"pos"}, new int[][] {{1, 3, 5}});
    }

    /** Sorted-word lexicon: lexeme id = index + 1, kinds resolved by name. */
    private record Vocab(String[] words) implements M3TokenPredicateCache.Vocabulary {
        @Override
        public int lexemeOf(CharSequence spelling) {
            int at = Arrays.binarySearch(words, spelling.toString());
            return at < 0 ? 0 : at + 1;
        }

        @Override
        public int symbolOf(String key, String value) {
            return key.equals("kind") ? Arrays.asList(KINDS).indexOf(value) : -1;
        }
    }

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) {
            throw new AssertionError(what);
        }
    }
}
