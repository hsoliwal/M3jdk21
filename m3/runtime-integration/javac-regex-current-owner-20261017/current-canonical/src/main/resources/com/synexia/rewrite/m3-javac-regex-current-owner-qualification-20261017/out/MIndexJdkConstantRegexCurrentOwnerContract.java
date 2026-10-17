// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Standalone differential oracle: prepared plan versus actual JDK Pattern. */
public final class MIndexJdkConstantRegexCurrentOwnerContract {
    private static long checks;

    private MIndexJdkConstantRegexCurrentOwnerContract() {}

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
        verifyCurrentCharSequences();
    }

    private static long charSequenceChecks;

    private static void currentCheck(boolean value, String detail) {
        charSequenceChecks++;
        if (!value) throw new AssertionError(detail);
    }

    private record LiteralCase(String expression, int flags, boolean expected) { }

    /** Real JDK views qualify this overload; no synthetic MIndex storage is supplied. */
    private static CharSequence[] currentViews(String value) {
        return new CharSequence[]{
                value,
                new StringBuilder(value),
                java.nio.CharBuffer.wrap("!" + value + "?")
                        .subSequence(1, value.length() + 1).asReadOnlyBuffer()
        };
    }

    private static void verifyCurrentCharSequences() {
        String[] expressions = {
                "", "Alpha_123", "abc", ".", "^$", "\\w+", "[a-z]+",
                "(ab)+", "(a?)(b*)", "a|b", "(?<=a)b", "(?=b)", "\\b",
                "a.*b", "a{0,3}", "(.)\\1", "(?i:abc)", "\\p{L}+",
                "\\x{1F600}", "[\\uD800-\\uDFFF]"
        };
        int[] flagSets = {
                0, Pattern.LITERAL, Pattern.CASE_INSENSITIVE,
                Pattern.LITERAL | Pattern.CASE_INSENSITIVE,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE,
                Pattern.LITERAL | Pattern.UNICODE_CASE,
                Pattern.UNICODE_CHARACTER_CLASS, Pattern.MULTILINE,
                Pattern.DOTALL, Pattern.CANON_EQ
        };
        String[] inputs = {
                "", "Alpha_123", "Alpha_124", "abc", "ABC", "a.b", "ab",
                "aaab", "a\nb", "a\rb", "\u00e9", "e\u0301", "\u0100",
                "\ud800", "\udc00", "\ud83d\ude00", "a\ud800b", "$\\"
        };
        int comparisons = 0;
        for (String expression : expressions) {
            for (int flags : flagSets) {
                var prepared = MIndexJdkConstantRegexPlan.compile(expression, flags);
                Pattern reference = Pattern.compile(expression, flags);
                currentCheck(prepared.expression().equals(reference.pattern()),
                        "current expression identity");
                currentCheck(prepared.flags() == flags, "current requested flags");
                currentCheck(prepared.javaEffectiveFlags() == reference.flags(),
                        "current effective flags");
                for (String input : inputs) {
                    for (CharSequence view : currentViews(input)) {
                        currentCheck(prepared.matches(view) == reference.matcher(view).matches(),
                                "current CharSequence/JDK parity " + expression + "/" + flags);
                        comparisons++;
                    }
                }
            }
        }
        currentCheck(comparisons == expressions.length * flagSets.length * inputs.length * 3,
                "all real JDK String/StringBuilder/ranged CharBuffer comparisons executed");

        LiteralCase[] literalCases = {
                new LiteralCase("", 0, true),
                new LiteralCase("Alpha_123", 0, true),
                new LiteralCase("a.b", 0, false),
                new LiteralCase("\u00e9", 0, false),
                new LiteralCase("\ud83d\ude00", 0, false),
                new LiteralCase("[", Pattern.LITERAL, true),
                new LiteralCase("a.b", Pattern.LITERAL, true),
                new LiteralCase("\ud800", Pattern.LITERAL, true),
                new LiteralCase("\udc00", Pattern.LITERAL, true),
                new LiteralCase("\ud83d\ude00", Pattern.LITERAL, true),
                new LiteralCase("abc", Pattern.CASE_INSENSITIVE, false),
                new LiteralCase("abc", Pattern.LITERAL | Pattern.CASE_INSENSITIVE, false),
                new LiteralCase("abc", Pattern.LITERAL | Pattern.UNICODE_CASE, false),
                new LiteralCase("abc", Pattern.CANON_EQ, false)
        };
        for (LiteralCase fixture : literalCases) {
            var prepared = MIndexJdkConstantRegexPlan.compile(
                    fixture.expression(), fixture.flags());
            currentCheck(prepared.usesExactLiteralMatchFastPath() == fixture.expected(),
                    "explicit literal eligibility fixture");
            Pattern reference = Pattern.compile(fixture.expression(), fixture.flags());
            for (String value : new String[]{
                    fixture.expression(), fixture.expression() + "x", "x" + fixture.expression()}) {
                for (CharSequence view : currentViews(value)) {
                    currentCheck(prepared.matches(view) == reference.matcher(view).matches(),
                            "literal UTF16/length/mismatch parity");
                }
            }
        }

        for (String expression : new String[]{"abc", "a.*"}) {
            var prepared = MIndexJdkConstantRegexPlan.compile(expression);
            boolean sequenceNull = false;
            try { prepared.matches((CharSequence) null); }
            catch (NullPointerException expected) { sequenceNull = true; }
            currentCheck(sequenceNull, "CharSequence null remains NPE");
            boolean stringNull = false;
            try { prepared.matches((String) null); }
            catch (NullPointerException expected) { stringNull = true; }
            currentCheck(stringNull, "String null remains NPE");
        }
        var literal = MIndexJdkConstantRegexPlan.compile("ab");
        var mutable = new StringBuilder("ab");
        currentCheck(literal.matches((CharSequence) mutable), "literal observes current input");
        mutable.setCharAt(1, 'c');
        currentCheck(!literal.matches((CharSequence) mutable), "literal never caches input result");
        mutable.setCharAt(1, 'b');
        currentCheck(literal.matches((CharSequence) mutable), "literal rereads current units");
        var residual = MIndexJdkConstantRegexPlan.compile("a.");
        currentCheck(residual.matches((CharSequence) mutable), "fresh residual Matcher");
        mutable.setLength(1);
        currentCheck(!residual.matches((CharSequence) mutable), "residual never caches input result");
        mutable.append('b');
        currentCheck(residual.matches((CharSequence) mutable), "residual rebuilds Matcher");
        String maximum = "x".repeat(8192);
        var bounded = MIndexJdkConstantRegexPlan.compile(maximum);
        currentCheck(bounded.matches((CharSequence) new StringBuilder(maximum)),
                "8192-unit accepted boundary");
        System.out.println("MIndexJdkConstantRegexCurrentOwnerContract addedChecks="
                + charSequenceChecks + " charSequenceComparisons=" + comparisons
                + " scope=JDK_CHARSEQUENCE_ONLY PASS");
    }
}
