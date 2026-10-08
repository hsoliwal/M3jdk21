// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary String.split and splitWithDelimiters on a literal regex (no metacharacter, escape or
 *          surrogate) take the M3 literal lane; results must equal Pattern.split for every literal,
 *          input and limit, in both flag modes
 * @run main M3StringLiteralSplitTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralSplitTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringLiteralSplitTest {
    private static final String[] LITERALS = {
        " ", ", ", "::", "ab", "aa", "aaa", "abc", "a b", "\t", "--", "<->", "=>", "#", "@@",
        "é", "éé", "\u0000", "éa", "xyz", "0", "00", "99", "-", "_", "!!", "~",
        // metacharacters, escapes and surrogates stay on the Pattern path; results must still agree
        ".", "a.b", "\\.", "a|b", "(a)", "[ab]", "a+", "^a", "a$", "{", "}", "]", "",
        "😀", "a😀b", "\uD83D", "\uDE00"
    };
    private static final String ALPHABET = "ab é:,-0";
    private static long checks;

    public static void main(String[] args) {
        deterministic();
        randomized();
        boundaries();
        System.out.println("M3StringLiteralSplitTest checks=" + checks);
    }

    private static void deterministic() {
        for (String literal : LITERALS) {
            for (String input : fixedInputs(literal)) {
                for (int limit = -2; limit <= 4; limit++) {
                    compare(input, literal, limit);
                }
                compare(input, literal, 100);
            }
        }
    }

    private static List<String> fixedInputs(String literal) {
        List<String> inputs = new ArrayList<>();
        inputs.add("");
        inputs.add(literal);
        inputs.add(literal + literal);
        inputs.add(literal + literal + literal);
        inputs.add("x" + literal);
        inputs.add(literal + "x");
        inputs.add("x" + literal + "y");
        inputs.add("x" + literal + literal + "y");
        inputs.add("x" + literal + "y" + literal);
        inputs.add(literal + "x" + literal + literal);
        inputs.add("boo:and:foo");
        inputs.add("0123456789");
        inputs.add(literal.isEmpty() ? "" : literal.substring(0, literal.length() - 1));
        inputs.add("😀" + literal + "😀");
        return inputs;
    }

    private static void randomized() {
        Random random = new Random(0x4d33535054L);
        for (int step = 0; step < 20_000; step++) {
            String literal = LITERALS[random.nextInt(LITERALS.length)];
            int length = random.nextInt(24);
            StringBuilder input = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (!literal.isEmpty() && random.nextInt(4) == 0) {
                    input.append(literal);
                } else {
                    input.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
                }
            }
            compare(input.toString(), literal, random.nextInt(6) - 2);
        }
    }

    private static void boundaries() {
        String source = "0123456789";
        for (int limit = -2; limit < 3; limit++) {
            for (int x = 0; x < 10; x++) {
                String mark = String.valueOf(x);
                compare(source, mark, limit);
                compare(source, mark + mark, limit);
                compare(source.replace(mark, mark + mark), mark + mark, limit);
            }
        }
        String ascii = "ab:cd::ef:::gh::::";
        for (String literal : new String[] {":", "::", ":::", "::::"}) {
            for (int limit = -1; limit <= 6; limit++) {
                compare(ascii, literal, limit);
            }
        }
    }

    private static void compare(String input, String regex, int limit) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException invalid) {
            // An invalid regex must fail identically through String.split.
            try {
                input.split(regex, limit);
                throw new AssertionError("String.split accepted an invalid regex " + show(regex));
            } catch (PatternSyntaxException expected) {
                checks++;
                return;
            }
        }
        check(Arrays.equals(pattern.split(input, limit), input.split(regex, limit)),
                "split", input, regex, limit);
        check(Arrays.equals(pattern.splitWithDelimiters(input, limit), input.splitWithDelimiters(regex, limit)),
                "splitWithDelimiters", input, regex, limit);
        if (limit == 0) {
            check(Arrays.equals(pattern.split(input), input.split(regex)), "split0", input, regex, limit);
        }
    }

    private static void check(boolean ok, String op, String input, String regex, int limit) {
        checks++;
        if (!ok) {
            throw new AssertionError(op + " differs for input=" + show(input) + " regex=" + show(regex)
                    + " limit=" + limit);
        }
    }

    private static String show(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 32 || c > 126) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
