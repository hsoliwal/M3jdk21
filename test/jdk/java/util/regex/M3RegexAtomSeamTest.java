/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3 String atom/tuple UTF-16 seam and stateful Pattern/Matcher parity
 * @run main/othervm -Xcheck:jni M3RegexAtomSeamTest
 * @run main/othervm -Xint -Xcheck:jni M3RegexAtomSeamTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3RegexAtomSeamTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Target-only differential jtreg fixture. Public String is candidate; an independently
 * constructed StringBuilder is the JDK CharSequence oracle. Exact UTF-16 positions matter.
 * No internal Synexia runtime class, native pointer or data-copying shadow is retained.
 */
public class M3RegexAtomSeamTest {
    private static long checks;

    public static void main(String[] args) {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        List<String> corpus = corpus();
        List<String> expressions = List.of(
                "", "a", "a*", "a.*b", "^a+$", "(a|ab)b?", "(?<named>a)?b",
                "(a)\\1", "(?<=a)b", "(?=b)b", "\\babc\\b", "\\R", "\\r\\n",
                "[\\s\\S]*", "\\p{L}+", "a*?", "(?:a|b)+", "\\ud83d\\ude42",
                "\\u0085|\\u2028|\\u2029", "$"
        );
        int[] flags = {0, Pattern.MULTILINE, Pattern.DOTALL, Pattern.UNICODE_CASE,
                Pattern.CASE_INSENSITIVE, Pattern.LITERAL};
        for (String text : corpus) {
            CharSequence oracle = new StringBuilder(text);
            for (String expression : expressions) {
                for (int flag : flags) {
                    compare(Pattern.compile(expression, flag), text, oracle);
                }
            }
        }
        System.out.printf("M3_REGEX_ATOM_SEAM_TEST corpus=%d patterns=%d flags=%d checks=%d PASS%n",
                corpus.size(), expressions.size(), flags.length, checks);
    }

    private static List<String> corpus() {
        String[] pieces = {"", "a", "b", "ab", "abc", "a\n", "\r", "\n", "\u0085",
                "\u2028", "\u2029", "\ud83d", "\ude42", "x", "1", "\u00e9",
                "\ud83d\ude42", "a\u0301", "z"};
        List<String> result = new ArrayList<>();
        result.add("");
        for (String left : pieces) {
            for (String right : pieces) {
                // Each source is constructed by concat; then a visible substring range.
                String joined = left.concat(right);
                result.add(joined);
                result.add("!".concat(joined).concat("?").substring(1, joined.length() + 1));
            }
        }
        Random random = new Random(0x4D335365614D3331L);
        for (int sample = 0; sample < 128; sample++) {
            String value = "";
            for (int piece = random.nextInt(8); piece > 0; piece--) {
                value = value.concat(pieces[random.nextInt(pieces.length)]);
            }
            result.add(sample % 4 == 0 ? value.repeat(sample % 3 + 1) : value);
        }
        result.add("a".repeat(256) + "b");
        result.add("\ud83d".concat("\ude42").repeat(48));
        result.add("x\r".concat("\ny"));
        return result;
    }

    private static void compare(Pattern pattern, String text, CharSequence oracleText) {
        Matcher left = pattern.matcher(text);
        Matcher right = pattern.matcher(oracleText);
        same(left.find(), right.find(), left, right);
        same(left.find(), right.find(), left, right);
        left.reset(); right.reset();
        same(left.lookingAt(), right.lookingAt(), left, right);
        left.reset(); right.reset();
        same(left.matches(), right.matches(), left, right);
        left.reset(); right.reset();
        int from = text.length() / 3;
        int to = text.length() - text.length() / 4;
        left.region(from, to); right.region(from, to);
        left.useTransparentBounds(true).useAnchoringBounds(false);
        right.useTransparentBounds(true).useAnchoringBounds(false);
        same(left.find(), right.find(), left, right);
        left.reset(text); right.reset(oracleText);
        same(left.find(0), right.find(0), left, right);
        Pattern next = Pattern.compile("(?<g>a)?b");
        left.usePattern(next); right.usePattern(next);
        left.reset(); right.reset();
        same(left.find(), right.find(), left, right);
        left.reset(); right.reset();
        eq(left.replaceAll("<$0>"), right.replaceAll("<$0>"), "replacement");
        eq(Arrays.equals(pattern.split(text, -1), pattern.split(oracleText, -1)), true, "split -1");
        eq(Arrays.equals(pattern.split(text, 0), pattern.split(oracleText, 0)), true, "split 0");
        eq(pattern.splitAsStream(text).collect(Collectors.toList()),
                pattern.splitAsStream(oracleText).collect(Collectors.toList()), "stream");
    }

    private static void same(boolean answer, boolean expected, Matcher left, Matcher right) {
        eq(answer, expected, "answer");
        eq(left.hitEnd(), right.hitEnd(), "hitEnd");
        eq(left.requireEnd(), right.requireEnd(), "requireEnd");
        eq(left.hasMatch(), right.hasMatch(), "hasMatch");
        eq(left.regionStart(), right.regionStart(), "regionStart");
        eq(left.regionEnd(), right.regionEnd(), "regionEnd");
        if (answer) {
            eq(left.groupCount(), right.groupCount(), "groupCount");
            for (int i = 0; i <= left.groupCount(); i++) {
                eq(left.start(i), right.start(i), "start");
                eq(left.end(i), right.end(i), "end");
                eq(left.group(i), right.group(i), "group");
            }
            MatchResult snapshot = left.toMatchResult();
            eq(snapshot.start(), left.start(), "snapshotStart");
            eq(snapshot.end(), left.end(), "snapshotEnd");
        } else {
            eq(failure(left::start), failure(right::start), "failedMatchException");
        }
    }

    private static String failure(Runnable task) {
        try { task.run(); return "NO_EXCEPTION"; }
        catch (RuntimeException exception) { return exception.getClass().getName(); }
    }

    private static void eq(Object left, Object right, String label) {
        checks++;
        if (!Objects.equals(left, right)) {
            throw new AssertionError(label + " expected=" + right + " actual=" + left);
        }
    }
}
