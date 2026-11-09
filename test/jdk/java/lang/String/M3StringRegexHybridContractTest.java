/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Preserve Java 21 regex observable results on M3-backed, composed UTF-16 String text
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Dm3.requireBacking=true M3StringRegexHybridContractTest
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Differential contract oracle; does not substitute or select a regex backend. */
public final class M3StringRegexHybridContractTest {
    private static final String[] EXPRESSIONS = {
        "", "a", "ab", "a*b", "a+?b", "a{2,4}b", "a.c", "^a.*b$",
        "(ab|a)c?", "(a)\\1", "(?<part>ab)c?", "(?=a)a", "(?<=a)b",
        "[a-z&&[^aeiou]]+", "[\\p{L}\\p{N}]*", "\\R", "\\X",
        "\\b\\w+\\b", "(?s).*", "(?U)\\w+", "a|bc", "(a+)+b",
        "[", "a{", "\\k<unknown>"
    };
    private static final int[] FLAGS = {
        0, Pattern.DOTALL, Pattern.MULTILINE,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE,
        Pattern.UNICODE_CHARACTER_CLASS, Pattern.LITERAL
    };
    private static long checks;

    private M3StringRegexHybridContractTest() { }

    public static void main(String[] args) throws Exception {
        String admitted = String.join("", "al", "ph", "a", "b");
        if (Boolean.getBoolean("m3.requireBacking")) requireM3Storage(admitted);
        for (String text : subjects(admitted)) {
            for (String expression : EXPRESSIONS) {
                compareStringMatches(text, expression);
                for (int flags : FLAGS) compareMatchers(text, expression, flags);
            }
        }
        System.out.println("M3_STRING_REGEX_HYBRID_CONTRACT_PASS checks=" + checks);
    }

    private static List<String> subjects(String admitted) {
        List<String> values = new ArrayList<>();
        values.add(admitted);
        values.add("");
        values.add(String.join("", "a", "b", "c"));
        values.add(String.join("", "a", "\n", "b"));
        values.add(String.join("", "\ud83d", "\ude42"));
        values.add(String.join("", "\ud83d", "x", "\ude42"));
        values.add(String.join("", "\u03b2", "eta", "\u0000"));
        values.add(String.join("", "a", "aaa", "b"));
        values.add("abac".substring(1));
        values.add("abc".repeat(4));
        Random random = new Random(0x20261014L);
        String alphabet = "aabBc0\n \u03b2\ud83d\ude42";
        for (int sample = 0; sample < 100; sample++) {
            int length = random.nextInt(18);
            StringBuilder left = new StringBuilder();
            StringBuilder right = new StringBuilder();
            for (int index = 0; index < length; index++) {
                char c = alphabet.charAt(random.nextInt(alphabet.length()));
                (index < length / 2 ? left : right).append(c);
            }
            values.add(String.join("", left, right));
        }
        return values;
    }

    private static void compareStringMatches(String text, String regex) {
        String observed = matchOutcome(() -> text.matches(regex));
        String oracle = matchOutcome(() -> Pattern.matches(regex, text));
        check(Objects.equals(observed, oracle), "String.matches " + regex + " on " + text);
    }

    private static String matchOutcome(BooleanSupplier check) {
        try {
            return "RESULT:" + check.get();
        } catch (PatternSyntaxException invalid) {
            return "INVALID:" + invalid.getIndex() + ':' + invalid.getDescription();
        }
    }

    private static void compareMatchers(String text, String regex, int flags) {
        Pattern compiled;
        try {
            compiled = Pattern.compile(regex, flags);
        } catch (PatternSyntaxException invalid) {
            checks++;
            return;
        }
        String contiguous = new String(text.toCharArray());
        Matcher original = compiled.matcher(text);
        Matcher materialized = compiled.matcher(contiguous);
        while (true) {
            boolean lhs = original.find();
            boolean rhs = materialized.find();
            check(lhs == rhs, "find availability: " + regex);
            check(original.hitEnd() == materialized.hitEnd(), "hitEnd: " + regex);
            check(original.requireEnd() == materialized.requireEnd(), "requireEnd: " + regex);
            if (!lhs) break;
            check(original.start() == materialized.start(), "start: " + regex);
            check(original.end() == materialized.end(), "end: " + regex);
            check(original.groupCount() == materialized.groupCount(), "group count");
            for (int group = 0; group <= original.groupCount(); group++) {
                check(original.start(group) == materialized.start(group), "group start");
                check(original.end(group) == materialized.end(group), "group end");
                check(Objects.equals(original.group(group), materialized.group(group)), "group value");
            }
        }
        int offset = text.length() / 2;
        Matcher region = compiled.matcher(text).region(offset, text.length());
        Matcher oracle = compiled.matcher(contiguous).region(offset, contiguous.length());
        check(region.matches() == oracle.matches(), "region matches");
        check(region.lookingAt() == oracle.lookingAt(), "region lookingAt");
    }

    private static void requireM3Storage(String text) throws ReflectiveOperationException {
        Field field = String.class.getDeclaredField("m3");
        field.setAccessible(true);
        check(field.get(text) != null, "joined String must use canonical M3 storage");
    }

    private static void check(boolean truth, String operation) {
        if (!truth) throw new AssertionError(operation);
        checks++;
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean get();
    }
}
