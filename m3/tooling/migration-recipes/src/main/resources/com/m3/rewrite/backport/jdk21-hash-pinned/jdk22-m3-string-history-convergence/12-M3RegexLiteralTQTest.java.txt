/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Candidate-only M3TQ literal find gate preserves Matcher result and state
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RegexLiteralTQTest
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3RegexLiteralTQTest {
    private static long checks;

    public static void main(String[] args) {
        String source = String.join(
                "",
                "abc".repeat(150),
                "XYZ",
                "abc".repeat(150),
                "\ud83d",
                "\ude42",
                "tail");

        compareFind(Pattern.compile("acb", Pattern.LITERAL), source, 0, source.length());
        compareFind(Pattern.compile("XYZ", Pattern.LITERAL), source, 0, source.length());
        compareFind(Pattern.compile("abcabc", Pattern.LITERAL), source, 0, source.length());
        compareFind(Pattern.compile("\ud83d\ude42t", Pattern.LITERAL), source, 0, source.length());

        int marker = source.indexOf("XYZ");
        compareFind(Pattern.compile("XYZ", Pattern.LITERAL), source, 0, marker);
        compareFind(Pattern.compile("XYZ", Pattern.LITERAL), source, marker, marker + 3);

        // A mandatory compiled Slice prefix may gate ordinary regex find, but the exact engine
        // still owns suffix quantifiers and all match state.
        compareFind(Pattern.compile("abc.*tail"), "zzabc-middle-tailzz", 0, 19);
        compareFind(Pattern.compile("abc.*tail"), "zz-no-required-prefix-tail", 0,
                "zz-no-required-prefix-tail".length());
        compareFind(Pattern.compile("abc(?:def)?"), "zzabczz", 0, 7);

        // These graphs must NOT be treated as requiring their first textual alternative/character.
        compareFind(Pattern.compile("abc|def"), "zzdefzz", 0, 7);
        compareFind(Pattern.compile("abc?"), "zzabzz", 0, 6);
        compareFind(Pattern.compile("(?:abc)?def"), "zzdefzz", 0, 7);

        // Case-insensitive literal matching has different equivalence semantics and must bypass
        // exact TQ gating.
        compareFind(
                Pattern.compile("xyz", Pattern.LITERAL | Pattern.CASE_INSENSITIVE),
                source,
                0,
                source.length());

        // Short literal patterns have no trigram constraint and remain exact-engine only.
        compareFind(Pattern.compile("a", Pattern.LITERAL), source, 0, source.length());
        compareFind(Pattern.compile("ab", Pattern.LITERAL), source, 0, source.length());

        // Anchored operations deliberately bypass the find-only candidate gate.
        compareAnchored(Pattern.compile(source, Pattern.LITERAL), source);
        compareAnchored(Pattern.compile("acb", Pattern.LITERAL), source);

        // Mutable CharSequence inputs bypass the cache/gate completely.
        StringBuilder mutable = new StringBuilder(source);
        Matcher mutableMatcher = Pattern.compile("XYZ", Pattern.LITERAL).matcher(mutable);
        mutable.replace(marker, marker + 3, "QQQ");
        check(!mutableMatcher.find(), "mutable input observes post-construction mutation");

        System.out.println("M3_REGEX_LITERAL_TQ_PASS|checks=" + checks);
    }

    private static void compareFind(Pattern pattern, String source, int from, int to) {
        Matcher candidate = pattern.matcher(source).region(from, to);
        Matcher oracle = pattern.matcher(new StringBuilder(source)).region(from, to);

        for (int iteration = 0; iteration < 8; iteration++) {
            boolean left = candidate.find();
            boolean right = oracle.find();
            check(left == right, "find result " + pattern + " iteration=" + iteration);
            check(candidate.hitEnd() == oracle.hitEnd(),
                    "hitEnd " + pattern + " iteration=" + iteration);
            check(candidate.requireEnd() == oracle.requireEnd(),
                    "requireEnd " + pattern + " iteration=" + iteration);
            if (!left) return;
            check(candidate.start() == oracle.start(),
                    "start " + pattern + " iteration=" + iteration);
            check(candidate.end() == oracle.end(),
                    "end " + pattern + " iteration=" + iteration);
            check(candidate.group().equals(oracle.group()),
                    "group " + pattern + " iteration=" + iteration);
        }
    }

    private static void compareAnchored(Pattern pattern, String source) {
        Matcher candidate = pattern.matcher(source);
        Matcher oracle = pattern.matcher(new StringBuilder(source));

        boolean leftMatches = candidate.matches();
        boolean rightMatches = oracle.matches();
        check(leftMatches == rightMatches, "matches result " + pattern);
        check(candidate.hitEnd() == oracle.hitEnd(), "matches hitEnd " + pattern);
        check(candidate.requireEnd() == oracle.requireEnd(), "matches requireEnd " + pattern);

        candidate.reset();
        oracle.reset();
        boolean leftLooking = candidate.lookingAt();
        boolean rightLooking = oracle.lookingAt();
        check(leftLooking == rightLooking, "lookingAt result " + pattern);
        check(candidate.hitEnd() == oracle.hitEnd(), "lookingAt hitEnd " + pattern);
        check(candidate.requireEnd() == oracle.requireEnd(), "lookingAt requireEnd " + pattern);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
