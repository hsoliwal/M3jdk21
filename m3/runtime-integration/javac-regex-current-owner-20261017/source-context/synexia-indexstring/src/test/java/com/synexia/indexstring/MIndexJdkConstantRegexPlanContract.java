// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Standalone differential oracle: prepared plan versus actual JDK Pattern. */
public final class MIndexJdkConstantRegexPlanContract {
    private static long checks;

    private MIndexJdkConstantRegexPlanContract() {}

    private static void check(boolean value, String detail) {
        checks++;
        if (!value) throw new AssertionError(detail);
    }

    public static void main(String[] args) {
        List<String> expressions = List.of(
                "", ".", "^$", "\\w+", "[a-z]+", "(ab)+", "(a?)(b*)", "a|b",
                "(?<=a)b", "(?=b)", "\\b", "a.*b", "a{0,3}", "(.)\\1",
                "(?i:abc)", "\\p{L}+", "\\d+", "\\x{1F600}", "[\\uD800-\\uDFFF]");
        int[] flags = {0, Pattern.CASE_INSENSITIVE,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE,
                Pattern.LITERAL, Pattern.MULTILINE, Pattern.DOTALL,
                Pattern.UNICODE_CHARACTER_CLASS};
        Random random = new Random(0x4d33524547455832L);
        char[] alphabet = {'a', 'b', 'c', 'A', 'B', '0', '1', '\n', '\r',
                '$', '\\', '\u00E9', '\u0301', '\uD83D', '\uDE00'};
        for (String expression : expressions) {
            for (int flag : flags) {
                MIndexJdkConstantRegexPlan compiled =
                        MIndexJdkConstantRegexPlan.compile(expression, flag);
                Pattern reference = Pattern.compile(expression, flag);
                check(compiled.javaPattern().pattern().equals(reference.pattern()),
                        "Pattern source preserved");
                check(compiled.flags() == flag, "requested flags preserved");
                check(compiled.javaEffectiveFlags() == reference.flags(),
                        "JDK-inferred effective flags preserved");
                for (int iteration = 0; iteration < 115; iteration++) {
                    int length = random.nextInt(25);
                    StringBuilder builder = new StringBuilder(length);
                    for (int pos = 0; pos < length; pos++) {
                        builder.append(alphabet[random.nextInt(alphabet.length)]);
                    }
                    String input = builder.toString();
                    check(compiled.matches(input) == reference.matcher(input).matches(),
                            "JDK matcher parity expression=" + expression);
                    int limit = random.nextInt(7) - 3;
                    String[] actual = compiled.split(input, limit);
                    String[] expected = reference.split(input, limit);
                    check(java.util.Arrays.equals(actual, expected), "JDK split parity");
                    String replacement = Matcher.quoteReplacement(
                            random.nextBoolean() ? "$value" : "\\x");
                    check(compiled.replaceAll(input, replacement).equals(
                                    reference.matcher(input).replaceAll(replacement)),
                            "JDK replacement groups/escapes");
                    check(compiled.replaceFirst(input, replacement).equals(
                                    reference.matcher(input).replaceFirst(replacement)),
                            "JDK first replacement");
                    if (flag == 0) {
                        check(compiled.matches(input) == input.matches(expression),
                                "String.matches parity");
                        check(java.util.Arrays.equals(compiled.split(input, limit),
                                        input.split(expression, limit)),
                                "String.split parity");
                        check(compiled.replaceAll(input, replacement).equals(
                                        input.replaceAll(expression, replacement)),
                                "String.replaceAll parity");
                        check(compiled.replaceFirst(input, replacement).equals(
                                        input.replaceFirst(expression, replacement)),
                                "String.replaceFirst parity");
                    }
                }
            }
        }
        // Replacements may be null and no match may occur; delegate to the
        // Matcher directly rather than eagerly changing JDK exception timing.
        var single = MIndexJdkConstantRegexPlan.compile("x");
        for (String input : List.of("", "a", "x", "xx")) {
            boolean jdkThrows = false;
            boolean m3Throws = false;
            String expected = null, actual = null;
            try { expected = Pattern.compile("x").matcher(input).replaceAll(null); }
            catch (RuntimeException failure) { jdkThrows = true; }
            try { actual = single.replaceAll(input, null); }
            catch (RuntimeException failure) { m3Throws = true; }
            check(jdkThrows == m3Throws, "null replacement exception timing");
            if (!jdkThrows) check(Objects.equals(expected, actual), "null replacement value");
        }
        boolean invalid = false;
        try { MIndexJdkConstantRegexPlan.compile("["); }
        catch (PatternSyntaxException expected) { invalid = true; }
        check(invalid, "invalid regex retains PatternSyntaxException");
        invalid = false;
        try { MIndexJdkConstantRegexPlan.compile("a", -1); }
        catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid, "invalid flags retain JDK exception");
        invalid = false;
        try { MIndexJdkConstantRegexPlan.compile("a".repeat(8193)); }
        catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid, "bounded compile-time candidate policy");
        System.out.println("MIndexJdkConstantRegexPlanContract checks=" + checks + " PASS");
    }
}
