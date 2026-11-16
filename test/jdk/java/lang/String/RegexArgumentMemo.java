/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Repeated regex arguments of String methods give the results of a fresh Pattern
 * @run main RegexArgumentMemo
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      RegexArgumentMemo
 */

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * matches, replaceFirst, replaceAll and split keep the compiled form of a regex argument. Each
 * result, and each exception, must be that of a Pattern compiled for the call: on the first use,
 * on later uses, after other regexes took the same slot, for an invalid regex every time, for a
 * regex too long to keep and for a regex that is a new String object with equal content.
 */
public class RegexArgumentMemo {
    private static final List<String> FIXED = List.of(
            "", "a", ",", "\\s+", "[,;]", "a|b", "(\\w+)@(\\w+)", "^x", "x$", "(?i)abc", "a*",
            "(a)(b)?", "(x)\\1", ".", "\\.", "(", "[a", "*a", "\\", "a{2,1}", "y".repeat(300));
    private static final List<String> INPUTS = List.of(
            "", "a", "abc", "ABC", "a,b;c,,d", "x  y\tz", "user@host and me@there", "xx", "aab",
            "xaxbx", "aaaa", "y".repeat(300), "ab ab ab");
    private static final List<String> REPLACEMENTS = List.of("-", "$1", "");
    private static final int GENERATED = 600;
    private static final int ROUNDS = 3;
    private static int checks;

    public static void main(String[] args) {
        List<String> regexes = Stream.concat(FIXED.stream(),
                IntStream.range(0, GENERATED).mapToObj(n -> "x{" + n + "}y?")).toList();
        IntStream.range(0, ROUNDS).forEach(round -> regexes.forEach(regex ->
                INPUTS.forEach(input -> check(new String(regex.toCharArray()), input))));
        System.out.println("PASS checks=" + checks);
    }

    private static void check(String regex, String input) {
        same(() -> input.matches(regex), () -> Pattern.compile(regex).matcher(input).matches());
        REPLACEMENTS.forEach(with -> {
            same(() -> input.replaceFirst(regex, with),
                    () -> Pattern.compile(regex).matcher(input).replaceFirst(with));
            same(() -> input.replaceAll(regex, with),
                    () -> Pattern.compile(regex).matcher(input).replaceAll(with));
        });
        List.of(0, -1, 2).forEach(limit -> same(
                () -> Arrays.toString(input.split(regex, limit)),
                () -> Arrays.toString(Pattern.compile(regex).split(input, limit))));
    }

    private static void same(Supplier<Object> actual, Supplier<Object> expected) {
        checks++;
        String got = outcome(actual);
        String wanted = outcome(expected);
        if (!got.equals(wanted)) {
            throw new AssertionError("expected " + wanted + " but was " + got);
        }
    }

    private static String outcome(Supplier<Object> call) {
        try {
            return String.valueOf(call.get());
        } catch (RuntimeException failure) {
            return failure.getClass().getName() + ": " + failure.getMessage();
        }
    }
}
