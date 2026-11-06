/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Token regex builders, snapshots, regions, captures and owner-engine reuse
 * @run main/othervm TokenPatternTest
 * @run main/othervm -Xint TokenPatternTest
 */

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.TokenMatcher;
import java.util.regex.TokenPattern;

public class TokenPatternTest {
    private static long checks;
    private static long differentials;
    private static long languages;

    public static void main(String[] args) throws Exception {
        construction();
        capturesAndPolicies();
        regionsAndState();
        snapshotsAndExceptions();
        tokenBoundaries();
        generatedLanguages();
        sharedPatterns();
        unchangedCharacterPatterns();
        System.out.println("TokenPatternTest PASS checks=" + checks
                + " differentials=" + differentials + " independentLanguages=" + languages);
    }

    private static TokenPattern<Integer> literal(int value) {
        return TokenPattern.literal(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void construction() {
        TokenPattern<Integer> a = literal(0);
        TokenPattern<Integer> b = literal(1);
        check(a.matcher(List.of(0)).matches(), "literal");
        check(!a.matcher(List.of(1)).matches(), "literal rejects");
        check(a.then(b).matcher(List.of(0, 1)).matches(), "sequence");
        check(b.then(a).matcher(List.of(1, 0)).matches(), "reused graph independent");
        check(a.matcher(List.of(0)).matches(), "leaf graph remains independent");
        check(a.or(b).matcher(List.of(1)).matches(), "ordered alternative");
        check(TokenPattern.<Integer>sequence().matcher(List.of()).matches(), "empty sequence");
        check(TokenPattern.<Integer>empty().matcher(List.of()).matches(), "empty factory");
        check(TokenPattern.alternation(a).matcher(List.of(0)).matches(), "single alternative");
        check(TokenPattern.<String>token(value -> value.length() == 2).matcher(List.of("ab")).matches(),
                "typed predicate");
        TokenPattern<Object> broad = TokenPattern.token(Objects::nonNull);
        check(TokenPattern.<String>sequence(broad, TokenPattern.literal("x"))
                .matcher(List.of("s", "x")).matches(), "contravariant composition");
        TokenPattern<Integer>[] parts = new TokenPattern[] {a, b};
        TokenPattern<Integer> saved = TokenPattern.sequence(parts);
        parts[0] = b;
        check(saved.matcher(List.of(0, 1)).matches(), "operand array snapshot");
        TokenPattern<Integer> capture = a.capture();
        TokenPattern<Integer> twice = capture.then(capture);
        equal(twice.groupCount(), 2, "reused numbered capture gets fresh occurrence");
        TokenMatcher<Integer> twiceMatcher = twice.matcher(List.of(0, 0));
        check(twiceMatcher.matches(), "reused capture");
        equal(twiceMatcher.span(1), new TokenMatcher.Span(0, 1), "first occurrence");
        equal(twiceMatcher.span(2), new TokenMatcher.Span(1, 2), "second occurrence");
        TokenPattern<Integer> nested = a.capture("inner").capture("outer").then(b.capture("last"));
        equal(nested.namedGroups(), java.util.Map.of("outer", 1, "inner", 2, "last", 3),
                "capture preorder");
        expect(UnsupportedOperationException.class, () -> nested.namedGroups().put("new", 4));
        expect(NullPointerException.class, () -> TokenPattern.token(null));
        expect(NullPointerException.class, () -> TokenPattern.sequence((TokenPattern<Integer>[]) null));
        expect(NullPointerException.class, () -> TokenPattern.sequence(a, null));
        expect(NullPointerException.class, () -> a.then(null));
        expect(NullPointerException.class, () -> a.or(null));
        expect(IllegalArgumentException.class, () -> TokenPattern.alternation());
        expect(NullPointerException.class, () -> a.capture(null));
        for (String name : List.of("", "1x", "a_b", "a-b", "Ω", "a b")) {
            expect(IllegalArgumentException.class, () -> a.capture(name));
        }
        expect(IllegalArgumentException.class, () -> a.capture("x").capture("x"));
        expect(IllegalArgumentException.class, () -> a.capture("x").then(b.capture("x")));
        expect(IllegalArgumentException.class, () -> a.capture("x").or(b.capture("x")));
        expect(IllegalArgumentException.class, () -> a.repeat(-1, 1));
        expect(IllegalArgumentException.class, () -> a.repeat(2, 1));
        expect(NullPointerException.class, () -> a.repeat(0, 1, null));
        expect(NullPointerException.class, () -> a.optional(null));
        expect(NullPointerException.class, () -> a.zeroOrMore(null));
        expect(NullPointerException.class, () -> a.oneOrMore(null));
        expect(NullPointerException.class, () -> a.matcher(null));
        check(TokenPattern.<Object>literal(null).matcher(Arrays.asList((Object) null)).matches(),
                "null literal");
        check(TokenPattern.<Object>anyToken().matcher(Arrays.asList((Object) null)).matches(),
                "any accepts null");
        check(TokenPattern.<Object>token(Objects::isNull).matcher(Arrays.asList((Object) null)).matches(),
                "predicate receives null");
    }

    private static void capturesAndPolicies() {
        TokenPattern<Integer> a = literal(0);
        TokenPattern<Integer> b = literal(1);
        for (TokenPattern.Quantifier policy : TokenPattern.Quantifier.values()) {
            String suffix = suffix(policy);
            compare(a.capture("unit").oneOrMore(policy).then(a),
                    "(?:(?<unit>a))+" + suffix + "a", List.of(0, 0, 0));
            compare(a.or(a.then(b)).repeat(0, 4, policy).then(b),
                    "(?:a|ab){0,4}" + suffix + "b", List.of(0, 1, 1));
            compare(a.capture("x").optional(policy).then(a),
                    "(?:(?<x>a))?" + suffix + "a", List.of(0));
            compare(a.capture("x").repeat(0, 0, policy),
                    "(?:(?<x>a)){0}" + suffix, List.of());
            compare(TokenPattern.<Integer>empty().capture("e").repeat(2, 4, policy),
                    "(?:(?<e>)){2,4}" + suffix, List.of());
            compare(a.optional().capture("nullable").repeat(1, 3, policy).then(b),
                    "(?:(?<nullable>(?:a)?)){1,3}" + suffix + "b", List.of(0, 1));
            compare(a.or(b).capture("piece").zeroOrMore(policy).then(a),
                    "(?:(?<piece>a|b))*" + suffix + "a", List.of(0, 1, 0));
        }
        compare(a.then(b).capture("long").or(a.capture("short")).then(b),
                "(?:(?<long>ab)|(?<short>a))b", List.of(0, 1));
        compare(a.capture("lastA").then(b.capture("lastB").optional()).oneOrMore(),
                "(?:(?<lastA>a)(?:(?<lastB>b))?)+", List.of(0, 1, 0));
        compare(a.or(TokenPattern.empty()).zeroOrMore().then(b), "(?:a|)*b", List.of(0, 0, 1));
        compare(a.optional().zeroOrMore().optional().then(b), "(?:(?:(?:a)?)*)?b", List.of(0, 1));
        compare(TokenPattern.<Integer>token(value -> value < 2).or(a).oneOrMore().then(b),
                "(?:[ab]|a)+b", List.of(0, 0, 1));
        TokenMatcher<Integer> greedy = a.zeroOrMore().matcher(List.of(0, 0, 0));
        check(greedy.find(), "greedy");
        equal(greedy.span(), new TokenMatcher.Span(0, 3), "greedy endpoint");
        TokenMatcher<Integer> lazy = a.zeroOrMore(TokenPattern.Quantifier.RELUCTANT).matcher(List.of(0, 0, 0));
        check(lazy.find(), "reluctant");
        equal(lazy.span(), new TokenMatcher.Span(0, 0), "reluctant endpoint");
        check(!a.zeroOrMore(TokenPattern.Quantifier.POSSESSIVE).then(a)
                .matcher(List.of(0, 0, 0)).matches(), "possessive does not backtrack");
    }

    private static void regionsAndState() {
        List<Integer> input = List.of(0, 1, 0, 0, 1);
        TokenPattern<Integer> p = literal(0).oneOrMore().capture("a");
        for (int start = 0; start <= input.size(); start++) {
            for (int end = start; end <= input.size(); end++) {
                TokenMatcher<Integer> actual = p.matcher(input).region(start, end);
                Matcher expected = Pattern.compile("(?<a>a+)").matcher("abaab").region(start, end);
                equal(actual.regionStart(), start, "region start");
                equal(actual.regionEnd(), end, "region end");
                while (true) {
                    boolean left = actual.find();
                    boolean right = expected.find();
                    same(actual, expected, left, right, input);
                    if (!left) break;
                }
            }
        }
        TokenMatcher<Integer> matcher = p.matcher(input).region(2, 5);
        check(!matcher.hasMatch(), "region clears state");
        expect(IllegalStateException.class, matcher::start);
        check(matcher.find(3), "find(index)");
        equal(matcher.span(), new TokenMatcher.Span(3, 4), "find(index) token coordinates");
        equal(matcher.regionStart(), 0, "find(index) restores full region");
        equal(matcher.regionEnd(), 5, "find(index) full region end");
        for (Runnable invalid : List.<Runnable>of(
                () -> matcher.find(-1), () -> matcher.find(6),
                () -> matcher.region(-1, 3), () -> matcher.region(3, 2), () -> matcher.region(0, 6))) {
            expect(IndexOutOfBoundsException.class, invalid);
            check(matcher.hasMatch(), "invalid range preserves visible state");
            equal(matcher.span(), new TokenMatcher.Span(3, 4), "invalid range preserves capture");
        }
        expect(IndexOutOfBoundsException.class, () -> matcher.group(-1));
        expect(IndexOutOfBoundsException.class, () -> matcher.span(2));
        expect(IllegalArgumentException.class, () -> matcher.start("absent"));
        expect(IllegalArgumentException.class, () -> matcher.end("absent"));
        expect(IllegalArgumentException.class, () -> matcher.group("absent"));
        expect(IllegalArgumentException.class, () -> matcher.span("absent"));
        check(!matcher.find(), "exhausted search");
        unavailable(matcher);
        matcher.reset();
        check(matcher.find(), "reset restores search");
        TokenMatcher<Integer> empty = TokenPattern.<Integer>empty().capture("e")
                .matcher(input).region(2, 4);
        for (int i = 2; i <= 4; i++) {
            check(empty.find(), "empty boundary");
            equal(empty.span(), new TokenMatcher.Span(i, i), "empty advances one token");
        }
        check(!empty.find(), "empty end tried once");
        unavailable(empty);
        check(!empty.find(), "empty exhausted search remains exhausted");
        unavailable(empty);
        expect(NullPointerException.class, () -> empty.start((String) null));
        expect(NullPointerException.class, () -> empty.end((String) null));
        expect(NullPointerException.class, () -> empty.group((String) null));
        expect(NullPointerException.class, () -> empty.span((String) null));
        TokenMatcher<Integer> absent = literal(0).capture("a").optional().then(literal(1))
                .matcher(List.of(1));
        check(absent.matches(), "absent optional");
        equal(absent.start("a"), -1, "absent start");
        equal(absent.end("a"), -1, "absent end");
        equal(absent.group("a"), null, "absent group");
        equal(absent.span("a"), null, "absent span");
        expect(IllegalArgumentException.class, () -> new TokenMatcher.Span(-1, 0));
        expect(IllegalArgumentException.class, () -> new TokenMatcher.Span(2, 1));
    }

    private static void unavailable(TokenMatcher<?> matcher) {
        check(!matcher.hasMatch(), "no visible match");
        expect(IllegalStateException.class, matcher::start);
        expect(IllegalStateException.class, matcher::end);
        expect(IllegalStateException.class, matcher::group);
        expect(IllegalStateException.class, matcher::span);
        expect(IllegalStateException.class, () -> matcher.start(0));
        expect(IllegalStateException.class, () -> matcher.end(0));
        expect(IllegalStateException.class, () -> matcher.group(0));
        expect(IllegalStateException.class, () -> matcher.span(0));
        for (String name : matcher.namedGroups().keySet()) {
            expect(IllegalStateException.class, () -> matcher.start(name));
            expect(IllegalStateException.class, () -> matcher.end(name));
            expect(IllegalStateException.class, () -> matcher.group(name));
            expect(IllegalStateException.class, () -> matcher.span(name));
        }
    }

    private static void snapshotsAndExceptions() {
        ArrayList<String> original = new ArrayList<>(Arrays.asList("a", null, "b"));
        TokenPattern<String> pattern = TokenPattern.<String>anyToken().oneOrMore().capture("all");
        TokenMatcher<String> matcher = pattern.matcher(original);
        check(matcher.pattern() == pattern, "pattern identity");
        equal(matcher.groupCount(), 1, "matcher count");
        equal(matcher.namedGroups(), pattern.namedGroups(), "matcher names");
        original.clear();
        check(matcher.matches(), "input detached");
        equal(matcher.input(), Arrays.asList("a", null, "b"), "snapshot allows null");
        List<String> capture = matcher.group("all");
        expect(UnsupportedOperationException.class, () -> matcher.input().set(0, "x"));
        expect(UnsupportedOperationException.class, () -> capture.clear());
        matcher.reset(List.of("c"));
        unavailable(matcher);
        check(matcher.matches(), "reset new snapshot");
        equal(capture, Arrays.asList("a", null, "b"), "old capture retained across reset");
        expect(NullPointerException.class, () -> matcher.reset(null));
        check(matcher.hasMatch(), "failed reset validation preserves match");
        equal(matcher.group(), List.of("c"), "failed reset preserves input");
        RuntimeException failure = new RuntimeException("identity");
        List<String> failing = new AbstractList<>() {
            @Override public String get(int index) { throw failure; }
            @Override public int size() { return 1; }
            @Override public Object[] toArray() { throw failure; }
        };
        check(expect(RuntimeException.class, () -> matcher.reset(failing)) == failure,
                "snapshot exception identity");
        check(matcher.hasMatch(), "snapshot failure keeps state");
        Object[] shared = {"a"};
        List<String> leaking = new AbstractList<>() {
            @Override public String get(int index) { return (String) shared[index]; }
            @Override public int size() { return 1; }
            @Override public Object[] toArray() { return shared; }
        };
        TokenMatcher<String> isolated = TokenPattern.literal("a").matcher(leaking);
        shared[0] = "b";
        check(isolated.matches(), "malicious shared toArray still detached");
        AtomicBoolean fail = new AtomicBoolean(true);
        TokenMatcher<String> callback = TokenPattern.<String>token(value -> {
            if (fail.get()) throw failure;
            return true;
        }).matcher(List.of("a"));
        for (Runnable operation : List.<Runnable>of(callback::matches, callback::lookingAt,
                callback::find, () -> callback.find(0))) {
            callback.reset();
            check(expect(RuntimeException.class, operation) == failure, "predicate exception identity");
            unavailable(callback);
        }
        fail.set(false);
        check(callback.reset().matches(), "reset after exception");
        Object throwingLiteral = new Object() {
            @Override public boolean equals(Object other) { throw failure; }
            @Override public int hashCode() { return 1; }
        };
        check(expect(RuntimeException.class, () -> TokenPattern.literal(throwingLiteral)
                .matcher(List.of(new Object())).matches()) == failure, "literal equality exception identity");
    }

    private enum Kind { IDENTIFIER, COMMENT, STRING, SPACE }
    private record Lexeme(Kind kind, String spelling, int identity) {
        @Override public String toString() { throw new AssertionError("token text requested"); }
    }

    private static void tokenBoundaries() {
        List<Lexeme> source = List.of(
                new Lexeme(Kind.COMMENT, "class Fake { int x; }", 0),
                new Lexeme(Kind.STRING, "class \"Fake\" /* code */", 1),
                new Lexeme(Kind.IDENTIFIER, "class", 2),
                new Lexeme(Kind.SPACE, "\n\t ", 3),
                new Lexeme(Kind.IDENTIFIER, "Example", 4));
        TokenPattern<Lexeme> keyword = TokenPattern.token(value ->
                value.kind() == Kind.IDENTIFIER && value.spelling().equals("class"));
        TokenPattern<Lexeme> space = TokenPattern.token(value -> value.kind() == Kind.SPACE);
        TokenPattern<Lexeme> name = TokenPattern.token(value -> value.kind() == Kind.IDENTIFIER);
        TokenMatcher<Lexeme> matcher = keyword.then(space.oneOrMore()).then(name.capture("name"))
                .matcher(source);
        check(matcher.find(), "caller lexical kinds");
        equal(matcher.span(), new TokenMatcher.Span(2, 5), "comments and strings excluded");
        equal(matcher.span("name"), new TokenMatcher.Span(4, 5), "name coordinates");
        check(matcher.group("name").getFirst() == source.get(4), "object identity preserved");
        for (String payload : List.of("a><b", "\ud83d\ude00", "\n", "/*x*/", "\"a\"", "")) {
            TokenMatcher<String> one = TokenPattern.literal(payload).matcher(List.of(payload));
            check(one.matches(), "arbitrary payload");
            equal(one.span(), new TokenMatcher.Span(0, 1), "one payload remains one token");
        }
        check(!TokenPattern.literal("a><b").matcher(List.of("a", "b")).matches(),
                "delimiter encoding collision excluded");
        List<Lexeme> wide = new ArrayList<>();
        for (int i = 0; i < 65_537; i++) wide.add(new Lexeme(Kind.IDENTIFIER, "same", i));
        Lexeme last = wide.getLast();
        TokenMatcher<Lexeme> distinct = TokenPattern.<Lexeme>token(value -> value == last).matcher(wide);
        check(distinct.find(), "over 65536 identities");
        equal(distinct.span(), new TokenMatcher.Span(65_536, 65_537), "identity not encoded as UTF16");
        check(!distinct.find(), "wide search exhaustion");
        unavailable(distinct);
        check(TokenPattern.<Lexeme>anyToken().repeat(65_537, 65_537).matcher(wide).matches(),
                "wide deterministic repeat");
    }

    private enum Form { LITERAL, ANY, EMPTY, SEQUENCE, ALTERNATION, REPEAT, CAPTURE }
    private record Expr(Form form, int literal, Expr left, Expr right, int min, int max,
                        TokenPattern.Quantifier policy) {
        TokenPattern<Integer> tokenPattern() {
            return switch (form) {
                case LITERAL -> TokenPattern.literal(literal);
                case ANY -> TokenPattern.anyToken();
                case EMPTY -> TokenPattern.empty();
                case SEQUENCE -> left.tokenPattern().then(right.tokenPattern());
                case ALTERNATION -> left.tokenPattern().or(right.tokenPattern());
                case REPEAT -> left.tokenPattern().repeat(min, max, policy);
                case CAPTURE -> left.tokenPattern().capture();
            };
        }
        String regex() {
            return switch (form) {
                case LITERAL -> String.valueOf((char) ('a' + literal));
                case ANY -> ".";
                case EMPTY -> "(?:)";
                case SEQUENCE -> "(?:" + left.regex() + right.regex() + ")";
                case ALTERNATION -> "(?:" + left.regex() + "|" + right.regex() + ")";
                case REPEAT -> "(?:" + left.regex() + "){" + min + ","
                        + (max == Integer.MAX_VALUE ? "" : max) + "}" + suffix(policy);
                case CAPTURE -> "(" + left.regex() + ")";
            };
        }
        boolean regularLanguage() {
            return (form != Form.REPEAT || policy != TokenPattern.Quantifier.POSSESSIVE)
                    && (left == null || left.regularLanguage())
                    && (right == null || right.regularLanguage());
        }
        Set<Integer> ends(List<Integer> input, int start) {
            return switch (form) {
                case LITERAL -> start < input.size() && input.get(start) == literal
                        ? Set.of(start + 1) : Set.of();
                case ANY -> start < input.size() ? Set.of(start + 1) : Set.of();
                case EMPTY -> Set.of(start);
                case CAPTURE -> left.ends(input, start);
                case ALTERNATION -> {
                    Set<Integer> result = new HashSet<>(left.ends(input, start));
                    result.addAll(right.ends(input, start));
                    yield result;
                }
                case SEQUENCE -> {
                    Set<Integer> result = new HashSet<>();
                    for (int middle : left.ends(input, start)) result.addAll(right.ends(input, middle));
                    yield result;
                }
                case REPEAT -> {
                    Set<Integer> frontier = Set.of(start);
                    Set<Integer> accepted = new HashSet<>();
                    if (min == 0) accepted.add(start);
                    int limit = max == Integer.MAX_VALUE ? input.size() + min + 1 : max;
                    for (int count = 1; count <= limit && !frontier.isEmpty(); count++) {
                        Set<Integer> next = new HashSet<>();
                        for (int position : frontier) next.addAll(left.ends(input, position));
                        if (count >= min) accepted.addAll(next);
                        if (count >= min && frontier.equals(next)) break;
                        frontier = next;
                    }
                    yield accepted;
                }
            };
        }
    }

    private static Expr expression(Random random, int depth) {
        Form form = Form.values()[random.nextInt(depth == 0 ? 3 : Form.values().length)];
        Expr left = form.ordinal() >= Form.SEQUENCE.ordinal() ? expression(random, depth - 1) : null;
        Expr right = form == Form.SEQUENCE || form == Form.ALTERNATION
                ? expression(random, depth - 1) : null;
        int min = random.nextInt(3);
        int max = random.nextInt(4) == 0 ? Integer.MAX_VALUE : min + random.nextInt(3);
        TokenPattern.Quantifier policy = TokenPattern.Quantifier.values()[random.nextInt(3)];
        return new Expr(form, random.nextInt(2), left, right, min, max, policy);
    }

    private static void generatedLanguages() {
        Random random = new Random(0x4d33544f4b454eL);
        for (int iteration = 0; iteration < 120; iteration++) {
            Expr expression = expression(random, 3);
            TokenPattern<Integer> pattern = expression.tokenPattern();
            String regex = expression.regex();
            for (int length = 0; length <= 5; length++) {
                for (int bits = 0; bits < (1 << length); bits++) {
                    List<Integer> input = new ArrayList<>();
                    for (int i = 0; i < length; i++) input.add((bits >>> i) & 1);
                    compare(pattern, regex, input);
                    if (expression.regularLanguage()) {
                        equal(pattern.matcher(input).matches(), expression.ends(input, 0).contains(length),
                                "independent language " + regex);
                        languages++;
                    }
                }
            }
        }
    }

    private static String suffix(TokenPattern.Quantifier policy) {
        return switch (policy) {
            case GREEDY -> "";
            case RELUCTANT -> "?";
            case POSSESSIVE -> "+";
        };
    }

    private static void compare(TokenPattern<Integer> pattern, String regex, List<Integer> input) {
        StringBuilder text = new StringBuilder();
        for (int value : input) text.append((char) ('a' + value));
        TokenMatcher<Integer> actual = pattern.matcher(input);
        Matcher expected = Pattern.compile(regex).matcher(text);
        equal(actual.groupCount(), expected.groupCount(), "group count " + regex);
        equal(actual.namedGroups(), expected.namedGroups(), "named groups " + regex);
        same(actual, expected, actual.matches(), expected.matches(), input);
        actual.reset(); expected.reset();
        same(actual, expected, actual.lookingAt(), expected.lookingAt(), input);
        actual.reset(); expected.reset();
        int count = 0;
        while (true) {
            boolean left = actual.find();
            boolean right = expected.find();
            same(actual, expected, left, right, input);
            if (!left) break;
            check(++count <= input.size() + 1, "finite empty progress " + regex);
        }
        differentials++;
    }

    private static void same(TokenMatcher<Integer> actual, Matcher expected, boolean left, boolean right,
                             List<Integer> input) {
        equal(left, right, "owner result " + input);
        equal(actual.hasMatch(), left, "token visible validity");
        if (!left) {
            expect(IllegalStateException.class, actual::start);
            expect(IllegalStateException.class, actual::group);
            expect(IllegalStateException.class, actual::span);
            return;
        }
        for (int group = 0; group <= expected.groupCount(); group++) {
            int start = expected.start(group);
            int end = expected.end(group);
            equal(actual.start(group), start, "capture start");
            equal(actual.end(group), end, "capture end");
            equal(actual.group(group), start < 0 ? null : input.subList(start, end), "capture list");
            equal(actual.span(group), start < 0 ? null : new TokenMatcher.Span(start, end), "capture span");
        }
        equal(actual.start(), expected.start(), "group zero start");
        equal(actual.end(), expected.end(), "group zero end");
        equal(actual.group(), input.subList(expected.start(), expected.end()), "group zero list");
        equal(actual.span(), new TokenMatcher.Span(expected.start(), expected.end()), "group zero span");
        for (String name : expected.namedGroups().keySet()) {
            int start = expected.start(name);
            int end = expected.end(name);
            equal(actual.start(name), start, "named capture start");
            equal(actual.end(name), end, "named capture end");
            equal(actual.group(name), start < 0 ? null : input.subList(start, end), "named capture list");
            equal(actual.span(name), start < 0 ? null : new TokenMatcher.Span(start, end), "named capture span");
        }
    }

    private static void sharedPatterns() throws Exception {
        TokenPattern<Integer> shared = literal(0).or(literal(1)).oneOrMore().capture("run").then(literal(2));
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Boolean>> jobs = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                jobs.add(() -> {
                    for (int count = 0; count < 100; count++) {
                        TokenMatcher<Integer> matcher = shared.matcher(List.of(0, 1, 0, 2));
                        if (!matcher.matches() || matcher.start("run") != 0 || matcher.end("run") != 3) return false;
                    }
                    return true;
                });
            }
            for (var result : executor.invokeAll(jobs)) check(result.get(), "independent concurrent matcher");
        }
    }

    private static void unchangedCharacterPatterns() {
        Matcher groups = Pattern.compile("(?<a>a+)(b)\\1").matcher("zaabaa");
        check(groups.find(), "original backreference");
        equal(groups.group("a"), "aa", "original capture");
        equal(groups.start(), 1, "original offset");
        check(Pattern.compile("(?<=x)\\p{L}+").matcher("xΩβ").find(), "original lookbehind");
        check(Pattern.compile("a.b", Pattern.DOTALL).matcher("a\nb").matches(), "original flags");
        check(Pattern.compile("\\x{1F600}+").matcher("😀😀").matches(), "original supplementary");
        Matcher empty = Pattern.compile("").matcher("ab");
        int count = 0;
        while (empty.find()) count++;
        equal(count, 3, "original empty progression");
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void equal(Object actual, Object expected, String label) {
        checks++;
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": actual=" + actual + " expected=" + expected);
        }
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return type.cast(failure);
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
