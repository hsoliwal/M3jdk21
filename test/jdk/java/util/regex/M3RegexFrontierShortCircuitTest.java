/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test
 * @summary Qualify necessary-length and dead-frontier short-circuit in the original Pattern implementation
 * @modules java.base/java.util.regex
 * @run main/othervm --add-opens java.base/java.util.regex=ALL-UNNAMED M3RegexFrontierShortCircuitTest
 */

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * The authoritative oracle deliberately passes a mutable CharSequence to Matcher,
 * so the String-only Boolean optimization cannot participate in the reference.
 */
public final class M3RegexFrontierShortCircuitTest {
    private static long assertions;

    private M3RegexFrontierShortCircuitTest() { }

    private static void check(boolean value, String context) {
        assertions++;
        if (!value) throw new AssertionError(context);
    }

    private static Object plan(Pattern pattern) throws Exception {
        Field field = Pattern.class.getDeclaredField("m3BooleanPlan");
        field.setAccessible(true);
        return field.get(pattern);
    }

    private static void minimum(String expression, int expected) throws Exception {
        Object compiled = plan(Pattern.compile(expression));
        check(compiled != null, "admitted compiled plan " + expression.length());
        Field field = compiled.getClass().getDeclaredField("minimumLength");
        field.setAccessible(true);
        check(field.getInt(compiled) == expected, "minimum length " + expression.length());
    }

    private static void compare(Pattern pattern, String text) {
        boolean reference = pattern.matcher(new StringBuilder(text)).matches();
        boolean actual = pattern.asMatchPredicate().test(text);
        check(actual == reference, "pattern=" + pattern + " flags=" + pattern.flags()
                + " textLength=" + text.length() + " reference=" + reference + " actual=" + actual);
    }

    private static String randomText(Random random) {
        String[] symbols = {"a", "b", "c", "z", "\n", "\u00e9", "\ud800", "\udc00", "\ud83d\ude00"};
        StringBuilder out = new StringBuilder();
        for (int i = random.nextInt(13); i > 0; i--) {
            out.append(symbols[random.nextInt(symbols.length)]);
        }
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        minimum("a*bc", 2);
        minimum("a*".repeat(63) + "bc", 2);
        minimum("a*".repeat(64) + "bc", 2);
        minimum("a*".repeat(254) + "bc", 2);
        minimum("a*".repeat(256), 0);
        minimum("a*b*c*", 0);

        List<String> samples = List.of("", "a", "b", "bc", "abc", "aaabc", "ab",
                "z", "azbc", "\u00e9", "\ud800", "\udc00", "\ud83d\ude00",
                "a\ud800bc", "a\ud83d\ude00bc", "a\nbc");
        String[] patterns = {"a*bc", "a*b*", "[ab]*c", ".*bc", "a*".repeat(63) + "bc",
                "a*".repeat(64) + "bc", "a*".repeat(254) + "bc", "a*".repeat(256),
                "\\p{L}*z", "a*b", "(?i)a*bc", "^a*bc$", "(?=a)a*"};
        int[] flags = {0, Pattern.CASE_INSENSITIVE, Pattern.UNICODE_CASE,
                Pattern.UNICODE_CHARACTER_CLASS, Pattern.DOTALL, Pattern.MULTILINE,
                Pattern.UNIX_LINES, Pattern.COMMENTS};
        for (String expression : patterns) {
            for (int flag : flags) {
                Pattern pattern = Pattern.compile(expression, flag);
                for (String text : samples) compare(pattern, text);
            }
        }

        Random random = new Random(0x4d33524653434cL);
        String[] atoms = {"a", "b", "c", ".", "a*", "b*", "c*", ".*", "[ab]*", "\\d*"};
        for (int sample = 0; sample < 6_000; sample++) {
            StringBuilder expression = new StringBuilder();
            int n = 1 + random.nextInt(14);
            for (int i = 0; i < n; i++) {
                expression.append(atoms[random.nextInt(atoms.length)]);
            }
            Pattern pattern = Pattern.compile(expression.toString(), flags[random.nextInt(flags.length)]);
            compare(pattern, randomText(random));
        }

        Pattern scalar = Pattern.compile("a*bc");
        Pattern wide = Pattern.compile("a*".repeat(64) + "bc");
        Pattern terminal = Pattern.compile("a*".repeat(254) + "bc");
        String negative = "z".repeat(30_000) + "\ud800";
        for (Pattern pattern : List.of(scalar, wide, terminal)) {
            compare(pattern, "b");
            compare(pattern, negative);
            compare(pattern, "a".repeat(30_000) + "bc");
            compare(pattern, "\ud83d\ude00bc");
        }
        System.out.println("M3RegexFrontierShortCircuitTest assertions=" + assertions);
    }
}
