/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Qualify current M3 String regex split as range-preserving without whole-source materialization
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexSplitHistoryTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexSplitHistoryTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringRegexSplitHistoryTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static int checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> type = Class.forName("java.lang.M3String");
            M3_OWNER = type.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministic();
        randomizedParity();
        System.out.println("M3_STRING_REGEX_SPLIT_HISTORY_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        String source = String.join("", "aa", "::", "bb", ":", "cc", "::");
        Object sourceOwner = owner(source);

        String[] general = source.split(":+", -1);
        checkArray(general, new String[] {"aa", "bb", "cc", ""}, "general split");
        checkSourceOwner(sourceOwner, general, "general split owner");

        String[] limited = source.split(":+", 2);
        checkArray(limited, new String[] {"aa", "bb:cc::"}, "limited split");
        checkSourceOwner(sourceOwner, limited, "limited split owner");

        String[] zeroLimit = source.split(":+", 0);
        checkArray(zeroLimit, new String[] {"aa", "bb", "cc"}, "zero-limit trailing empty");
        checkSourceOwner(sourceOwner, zeroLimit, "zero-limit owner");

        String[] delimiters = source.splitWithDelimiters(":+", -1);
        checkArray(
                delimiters,
                new String[] {"aa", "::", "bb", ":", "cc", "::", ""},
                "general delimiters");
        checkSourceOwner(sourceOwner, delimiters, "general delimiter owner");

        String[] noMatch = source.split("ZZ+", -1);
        check(noMatch.length == 1 && noMatch[0] == source, "no-match identity");

        String[] limitOne = source.split(":+", 1);
        check(limitOne.length == 1 && limitOne[0] == source, "limit-one identity");

        String zeroWidth = String.join("", "abc", "def");
        String[] atStart = zeroWidth.split("(?=a)", -1);
        check(atStart.length == 1 && atStart[0] == zeroWidth,
                "zero-width beginning produces no leading empty");
        String[] inside = zeroWidth.split("(?=b)", -1);
        checkArray(inside, new String[] {"a", "bcdef"}, "zero-width interior");
        checkSourceOwner(owner(zeroWidth), inside, "zero-width interior owner");

        String positive = String.join("", "aa", "BB");
        String[] positiveStart = positive.splitWithDelimiters("a+", -1);
        checkArray(positiveStart, new String[] {"", "aa", "BB"}, "positive-width beginning");
        checkSourceOwner(owner(positive), positiveStart, "positive-width owner");

        // Single-character String.split uses the JDK fast path. Fields remain M3 substrings;
        // delimiter values may use the canonical one-unit atom rather than the source owner.
        String[] fast = source.split(":", -1);
        checkArray(fast, new String[] {"aa", "", "bb", "cc", "", ""}, "single-char fast split");
        for (String value : fast) {
            if (!value.isEmpty()) check(requireM3(value) != null, "fast split M3 field");
        }

        expectPatternSyntax(() -> source.split("[", 1), "compile before limit-one shortcut");
    }

    private static void randomizedParity() throws Exception {
        Random random = new Random(0x4d3353504c49544cL);
        String[] atoms = {
                "", "a", "b", "x", "y", ":", "::", "-", "\u03b2",
                "\ud83d", "\ude42", "\ud83d\ude42"
        };
        String[] regexes = {
                ":+", "a+", "[xy]+", "(?=b)", "\\Q::\\E", "(?:ab)+"
        };
        int[] limits = {-1, 0, 1, 2, 5};

        for (int trial = 0; trial < 256; trial++) {
            int pieces = 1 + random.nextInt(10);
            String[] selected = new String[pieces];
            for (int index = 0; index < pieces; index++) {
                selected[index] = atoms[random.nextInt(atoms.length)];
            }
            String source = String.join("", selected);
            String regex = regexes[random.nextInt(regexes.length)];
            int limit = limits[random.nextInt(limits.length)];
            Pattern pattern = Pattern.compile(regex);
            StringBuilder oracleInput = new StringBuilder(source);

            checkArray(
                    source.split(regex, limit),
                    pattern.split(oracleInput, limit),
                    "random split " + trial + " regex=" + regex + " limit=" + limit);
            checkArray(
                    source.splitWithDelimiters(regex, limit),
                    pattern.splitWithDelimiters(oracleInput, limit),
                    "random splitWithDelimiters " + trial + " regex=" + regex + " limit=" + limit);
        }
    }

    private static Object requireM3(String value) throws Exception {
        value.length();
        Object m3 = STRING_M3.get(value);
        if (m3 == null) throw new AssertionError("String not M3-backed: " + value);
        return m3;
    }

    private static Object owner(String value) throws Exception {
        return M3_OWNER.get(requireM3(value));
    }

    private static void checkSourceOwner(Object sourceOwner, String[] values, String label)
            throws Exception {
        for (String value : values) {
            if (value.isEmpty()) continue;
            check(M3_OWNER.get(requireM3(value)) == sourceOwner,
                    label + " did not preserve source owner for " + value);
        }
    }

    private static void checkArray(String[] actual, String[] expected, String label) {
        checks++;
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(
                    label + " actual=" + Arrays.toString(actual)
                            + " expected=" + Arrays.toString(expected));
        }
    }

    private static void expectPatternSyntax(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw");
        } catch (PatternSyntaxException expected) {
            checks++;
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
