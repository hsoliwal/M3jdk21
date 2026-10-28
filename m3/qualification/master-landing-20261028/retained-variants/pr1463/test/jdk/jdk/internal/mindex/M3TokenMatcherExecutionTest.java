/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Tagged token VM, capture priority, exhaustive JDK differential cases and cold predicate masks
 * @modules java.base/jdk.internal.mindex
 * @run main M3TokenMatcherExecutionTest
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

/** Independent JDK regex oracle uses single-character tokens so both coordinates coincide. */
public final class M3TokenMatcherExecutionTest {
    private static long checks;
    private static final M3TokenPredicateCache.Vocabulary CHARACTERS = spelling ->
            spelling.length() == 1 ? spelling.charAt(0) + 1 : 0;

    private M3TokenMatcherExecutionTest() { }

    public static void main(String[] args) {
        differential();
        spanPriority();
        nullableProgress();
        projectedCoordinates();
        predicateMasks();
        refusalAndScale();
        System.out.println("M3TokenMatcherExecutionTest PASS checks=" + checks);
    }

    private static M3TokenIndex characters(String text) {
        int size = text.length();
        int[] kinds = new int[size], lexemes = new int[size];
        long[] starts = new long[size], ends = new long[size], flags = new long[size];
        for (int i = 0; i < size; i++) {
            kinds[i] = 1;
            lexemes[i] = text.charAt(i) + 1;
            starts[i] = i;
            ends[i] = i + 1;
        }
        return M3TokenIndex.of(19, kinds, lexemes, starts, ends, flags);
    }

    private static M3TokenMatcher matcher(M3TokenPattern pattern, String text, int mode) {
        return M3TokenMatcher.of(M3TokenPredicateCache.bind(pattern, characters(text), text, CHARACTERS),
                mode, false);
    }

    private static M3TokenMatcher matcher(String source, String text, int mode) {
        return matcher(M3TokenPattern.compile(source), text, mode);
    }

    private static void differential() {
        List<String> patterns = new ArrayList<>(List.of(
                "a", "[]", "^ a $", "^ $", "a?", "a??", "a*", "a*?", "a+", "a+?",
                "a{0}", "a{1,3}", "a{1,3}?", "a{2,}", "a{2,}?", "a | b", "a b | a",
                "(a | a a) b", "(a a | a) b", "(a | a a)* b", "(a a | a)* b",
                "(a | b)+", "(a | b)+?", "(a b)*", "(a b)*?", "(a? b)*", "(a?? b)*?",
                "(a*) (a*)", "(a*?) (a*)", "(a*) (a*?)", "(a?) (a?)", "(a??) (a?)",
                "(?<first>a+) (?<last>b?)", "((a) | (b))*", "((a) b | a (b))*",
                "(a | b){1,3} a?", "(a | b){1,3}? a??", "[]* a", "[]*? a", "a $", "^ b",
                "(a{1} | a{2}) (a{2} | a{1})", "((a | b) a){0,2}", "(a? b){1,2}"));
        for (String left : List.of("a", "a?", "a*", "a*?", "(a b | b)", "(a | b)+")) {
            for (String right : List.of("a", "b?", "[]*?")) patterns.add(left + " " + right);
        }
        for (String source : patterns) {
            M3TokenPattern token = M3TokenPattern.compile(source);
            Pattern reference = Pattern.compile(source.replace(" ", "").replace("[]", "."));
            for (int length = 0; length <= 6; length++) {
                for (int bits = 0; bits < (1 << length); bits++) {
                    StringBuilder text = new StringBuilder(length);
                    for (int i = 0; i < length; i++) text.append((bits & (1 << i)) == 0 ? 'a' : 'b');
                    compare(token, reference, text.toString());
                }
            }
        }
    }

    private static void compare(M3TokenPattern pattern, Pattern reference, String input) {
        String label = pattern.source() + " on '" + input + "'";
        M3TokenMatcher actual = matcher(pattern, input, M3TokenMatcher.FIND_NEXT);
        Matcher expected = reference.matcher(input);
        boolean whole = expected.matches();
        check(actual.matches() == whole, "matches " + label);
        if (whole) compareGroups(actual, expected, label);
        actual.reset();
        expected.reset();
        int finds = 0;
        while (expected.find()) {
            check(actual.find(), "missing find " + label);
            compareGroups(actual, expected, label);
            check(++finds <= input.length() + 1, "zero-width progress " + label);
        }
        check(!actual.find(), "extra find " + label);
        check(!actual.find(), "stable exhaustion " + label);
        throwsType(IllegalStateException.class, actual::start);
    }

    private static void compareGroups(M3TokenMatcher actual, Matcher expected, String label) {
        check(actual.groupCount() == expected.groupCount(), "group count " + label);
        for (int group = 0; group <= actual.groupCount(); group++) {
            check(actual.start(group) == expected.start(group) && actual.end(group) == expected.end(group),
                    "group " + group + " " + label + " expected=" + expected.start(group) + ","
                    + expected.end(group) + " actual=" + actual.start(group) + "," + actual.end(group));
            check(actual.sourceStart(group) == expected.start(group)
                    && actual.sourceEnd(group) == expected.end(group), "source group " + label);
        }
    }

    private static void spanPriority() {
        spans("(a{1} | a{2}) (a{2} | a{1})", "aaaaa",
                "0:3 0:2 0:4 1:4 1:3 1:5 2:5 2:4 3:5");
        spans("a*", "aaa", "0:3 0:2 0:1 0:0 1:3 1:2 1:1 2:3 2:2 3:3");
        spans("a*?", "aaa", "0:0 0:1 0:2 0:3 1:1 1:2 1:3 2:2 2:3 3:3");
        spans("(a | a a) (a a | a)", "aaaa", "0:3 0:2 0:4 1:4 1:3 2:4");
        spans("(a | a)", "aa", "0:1 1:2");
        M3TokenMatcher captures = matcher("((?<first>a) | (?<second>a))", "a", 0);
        check(captures.find() && captures.start("first") == 0 && captures.start("second") == -1,
                "same-endpoint written branch capture priority");
        check(!captures.find(), "deduplicate same endpoint");
    }

    private static void spans(String pattern, String input, String expected) {
        M3TokenMatcher matcher = matcher(pattern, input, M3TokenMatcher.FIND_ALL_SPANS);
        List<String> actual = new ArrayList<>();
        while (matcher.find()) {
            actual.add(matcher.start() + ":" + matcher.end());
            check(actual.size() <= (input.length() + 1) * (input.length() + 1), "span termination");
        }
        check(String.join(" ", actual).equals(expected), "all-span priority " + pattern + " " + actual);
    }

    private static void nullableProgress() {
        spans("(a?)*", "aa", "0:2 0:1 0:0 1:2 1:1 2:2");
        spans("(a?)*?", "aa", "0:0 0:1 0:2 1:1 1:2 2:2");
        spans("((a?)*)*", "a", "0:1 0:0 1:1");
        spans("((a?)*)*?", "a", "0:0 0:1 1:1");
        // Existing token contract: a non-consuming iteration ends its repetition. It cannot
        // be replayed merely to satisfy more mandatory iterations, unlike Java text regex.
        check(!matcher("(a?){2}", "", 1).matches(), "zero iteration cannot satisfy minimum twice");
        check(matcher("(a?){2}", "a", 1).matches(), "one consuming then one empty required iteration");
        M3TokenMatcher last = matcher("(?<last>a?)*", "a", 1);
        check(last.matches() && last.start("last") == 1 && last.end("last") == 1,
                "last participating empty capture");
        M3TokenMatcher absent = matcher("((a) | b)*", "ab", 1);
        check(absent.matches() && absent.start(2) == 0 && absent.end(2) == 1,
                "last participating capture retained across absent alternative");
    }

    private static void projectedCoordinates() {
        String source = " a  b ";
        M3TokenIndex index = M3TokenIndex.of(1, new int[] {0, 1, 0, 1, 0},
                new int[] {0, 98, 0, 99, 0}, new long[] {0, 1, 2, 4, 5},
                new long[] {1, 2, 4, 5, 6}, new long[] {1, 0, 1, 0, 1});
        M3TokenPattern pattern = M3TokenPattern.compile("(?<a>a) (?<b>b) (?<empty>a?)");
        M3TokenMatcher m = M3TokenMatcher.of(M3TokenPredicateCache.bind(pattern, index, source, CHARACTERS), 1, true);
        check(m.matches(), "projected match");
        check(m.start() == 1 && m.end() == 4 && m.sourceStart(0) == 1 && m.sourceEnd(0) == 5,
                "whole lossless token/source coordinates");
        check(m.start("a") == 1 && m.end("a") == 2 && m.start("b") == 3 && m.end("b") == 4,
                "projected capture coordinates");
        check(m.start("empty") == 5 && m.end("empty") == 5, "empty projected capture at lossless tape end");
        check(!m.find(), "projected matches updates next cursor");
        M3TokenMatcher empty = M3TokenMatcher.of(M3TokenPredicateCache.bind(M3TokenPattern.compile("a*?"),
                index, source, CHARACTERS), 1, true);
        int[] boundaries = {1, 3, 5};
        for (int b : boundaries) check(empty.find() && empty.start() == b && empty.end() == b, "empty progress projection");
        check(!empty.find() && empty.searchFrom() == 6, "projected exhaustion");
    }

    private static void predicateMasks() {
        CountingSource source = new CountingSource("abba");
        M3TokenPattern pattern = M3TokenPattern.compile("[({text:/a/} | {binding==9}) & !{flags:0x1}]");
        M3TokenIndex index = M3TokenIndex.of(7, new int[] {1, 1, 1, 1}, new int[] {98, 99, 99, 98},
                new long[] {0, 1, 2, 3}, new long[] {1, 2, 3, 4}, new long[] {0, 0, 0, 1},
                new int[] {0, 9, 0, 0}, new String[0], new int[0][]);
        M3TokenPredicateCache cache = M3TokenPredicateCache.bind(pattern, index, source, CHARACTERS);
        int reads = source.reads;
        source.sealed = true;
        int expression = pattern.nodeX(pattern.rootNode());
        for (int repeat = 0; repeat < 1000; repeat++) {
            for (int t = 0; t < 4; t++) check(cache.test(expression, t) == (t < 2), "occurrence masks");
        }
        check(source.reads == reads && reads == 2, "regex once per known lexeme, zero warm spelling reads");
        M3TokenMatcher m = M3TokenMatcher.of(cache, 1, false);
        check(m.find() && m.start() == 0 && m.find() && m.start() == 1 && !m.find(), "matcher consumes only masks");
        throwsType(IndexOutOfBoundsException.class, () -> cache.row(0, -1));
        throwsType(IndexOutOfBoundsException.class, () -> cache.test(expression, 4));

        String[] spellings = {"12", "-3", "+4", "9223372036854775807", "-9223372036854775808",
                "9223372036854775808", "-9223372036854775809", "x", "+"};
        for (String spelling : spellings) {
            boolean numeric;
            try { numeric = Long.parseLong(spelling) > 2; } catch (NumberFormatException bad) { numeric = false; }
            check(oneToken("[{text>2}]", spelling, null) == numeric, "numeric text " + spelling);
        }
        for (String regex : List.of("a.*", "(?i)a", "(?<=x)a", "a(?=b)", ".", "\\uD800", "(a)\\1")) {
            for (String text : List.of("a", "aa", "b", "😀", "\uD800", "\uDC00")) {
                check(oneToken("/" + regex + "/", text, null) == Pattern.matches(regex, text),
                        "JDK spelling regex oracle " + regex);
            }
        }
        M3TokenPredicateCache.Vocabulary symbolic = new M3TokenPredicateCache.SymbolVocabulary() {
            @Override public int lexemeOf(CharSequence spelling) { return 0; }
            @Override public CharSequence symbolSpelling(String key, long value) {
                return key.equals("kind") && value == 1 ? "IDENT" : null;
            }
        };
        check(oneToken("[{kind:/ID.*/}]", "x", symbolic), "canonical reverse symbolic spelling");
        throwsType(IllegalArgumentException.class, () -> oneToken("[{kind:/ID.*/}]", "x", null));
        throwsType(PatternSyntaxException.class, () -> oneToken("/[a/", "x", null));
        throwsType(IllegalArgumentException.class, () -> M3TokenPredicateCache.bind(M3TokenPattern.compile("a"),
                characters("aa"), "a", CHARACTERS));
    }

    private static boolean oneToken(String pattern, String text, M3TokenPredicateCache.Vocabulary vocabulary) {
        M3TokenIndex index = M3TokenIndex.of(1, new int[] {1}, new int[1], new long[1],
                new long[] {text.length()}, new long[1]);
        M3TokenPredicateCache cache = M3TokenPredicateCache.bind(M3TokenPattern.compile(pattern), index,
                text, vocabulary == null ? spelling -> 0 : vocabulary);
        return M3TokenMatcher.of(cache, 1, false).matches();
    }

    private static void refusalAndScale() {
        throwsType(PatternSyntaxException.class, () -> M3TokenPattern.compile("(a{4096}){4096}"));
        String input = "a".repeat(12000);
        check(!matcher("(a | a a)* b", input, 1).find(), "ambiguous failing input completes without backtracking explosion");
        M3TokenMatcher greedy = matcher("a*", input, 1);
        check(greedy.find() && greedy.start() == 0 && greedy.end() == input.length(), "greedy retained-leaf growth");
        M3TokenMatcher bounded = matcher("(".repeat(24) + "a*" + ")".repeat(24), "a".repeat(300000), 1);
        check(bounded.find() && bounded.end() == 300000 && bounded.end(24) == 300000,
                "FIND_NEXT capture storage bounded by program, not every accepted endpoint");
        M3TokenMatcher interrupted = matcher("a+", input, 1);
        Thread.currentThread().interrupt();
        try {
            throwsType(IllegalStateException.class, interrupted::find);
            check(Thread.currentThread().isInterrupted(), "interruption remains observable");
        } finally {
            Thread.interrupted();
        }
        M3TokenPattern large = M3TokenPattern.compile("a ".repeat(2000));
        String largeInput = "a".repeat(300000);
        M3TokenIndex index = characters(largeInput);
        throwsType(IllegalArgumentException.class, () -> M3TokenPredicateCache.bind(large, index, largeInput, CHARACTERS));
    }

    private static final class CountingSource implements CharSequence {
        private final String value;
        private int reads;
        private boolean sealed;
        CountingSource(String value) { this.value = value; }
        @Override public int length() { return value.length(); }
        @Override public char charAt(int index) {
            if (sealed) throw new AssertionError("warm path touched spelling");
            reads++;
            return value.charAt(index);
        }
        @Override public CharSequence subSequence(int from, int to) { throw new AssertionError("copied spelling"); }
        @Override public String toString() { throw new AssertionError("materialized spelling"); }
    }

    private static void throwsType(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) { checks++; return; }
            throw new AssertionError("expected " + type + " got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }
}
