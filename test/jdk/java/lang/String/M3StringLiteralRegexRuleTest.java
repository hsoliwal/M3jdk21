/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary Compile-time map G7: M3String.literalRegex is the one owner of "this regex is a
 *          literal". Over every regex of up to 3 units drawn from the 14 metacharacters, a, b,
 *          '-', '#', space, NUL, U+00E9, U+0101 and both surrogate halves, and every input of up
 *          to 4 units over "a-." and both surrogate halves: its bits are the union of the two
 *          former predicates; a LITERAL_MATCHES regex matches exactly the equal String; a
 *          LITERAL_SEARCH regex finds, splits (with and without delimiters) and replaces
 *          exactly at the String.indexOf occurrences, for flat and M3-backed receivers; String
 *          declares no literal-regex predicate of its own. Refusals that java.util.regex shows
 *          necessary are counted.
 * @modules java.base/java.lang:open
 * @run main M3StringLiteralRegexRuleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringLiteralRegexRuleTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringLiteralRegexRuleTest {
    /** The 14 metacharacters of a java.util.regex pattern outside a class, escape included. */
    private static final String METACHARACTERS = "\\.^$|?*+()[]{}";
    /** a, b, '-', '#', space, NUL, U+00E9, U+0101, a high and a low surrogate. */
    private static final String ORDINARY =
            new String(new char[] {'a', 'b', '-', '#', ' ', 0, 0xe9, 0x101, 0xd800, 0xdc00});
    /** The units of the inputs: a, '-', '.', a high and a low surrogate. */
    private static final String INPUT_UNITS =
            new String(new char[] {'a', '-', '.', 0xd800, 0xdc00});
    /** No '$' and no '\\': String.replaceAll and replaceFirst may take the M3 literal lane. */
    private static final String REPLACEMENT = "<R>";
    private static final int[] LIMITS = {0, -1, 2};
    private static final List<String> FAILURES = new ArrayList<>();
    private static final List<String> NOT_SHOWN = new ArrayList<>();
    private static int literalMatches;
    private static int literalSearch;
    private static Method literalRegex;
    private static Method isLiteralRegex;
    private static Method isLiteralMatchesRegex;
    private static Method internChars;
    private static Method slice;
    private static Constructor<String> fromM3;
    private static Field stringM3;
    private static long checks;
    private static int m3Receivers;
    private static int m3Refused;
    private static int matchesRefused;
    private static int matchesSyntax;
    private static int matchesBehaviour;
    private static int searchRefused;
    private static int searchShown;

    /**
     * Optional arguments: the longest regex and the longest input in units (3 and 4); a slow
     * interpreted run may pass shorter ones.
     */
    public static void main(String[] args) throws Throwable {
        int regexUnits = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int inputUnits = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        if (bind()) {
            List<String> regexes = words(METACHARACTERS + ORDINARY, regexUnits);
            List<String> inputs = words(INPUT_UNITS, inputUnits);
            List<String[]> receivers = receivers(inputs);
            check((int) invoke(literalRegex, null, (Object) null) == 0, "literalRegex(null) is 0");
            for (String regex : regexes) {
                rule(regex, inputs, receivers);
            }
            System.out.println("M3StringLiteralRegexRuleTest regexes=" + regexes.size()
                    + " inputs=" + inputs.size() + " m3Receivers=" + m3Receivers
                    + " m3Refused=" + m3Refused);
            System.out.println("refused LITERAL_MATCHES: " + matchesRefused
                    + " regexes; java.util.regex shows " + (matchesSyntax + matchesBehaviour)
                    + " necessary (" + matchesSyntax + " do not compile, " + matchesBehaviour
                    + " match an input other than their equal), "
                    + (matchesRefused - matchesSyntax - matchesBehaviour) + " not shown");
            System.out.println("refused LITERAL_SEARCH only: " + searchRefused
                    + " regexes; java.util.regex shows " + searchShown
                    + " necessary (find, split or replaceAll differs from indexOf), "
                    + (searchRefused - searchShown) + " not shown");
            System.out.println("not shown, e.g. " + NOT_SHOWN.subList(0,
                    Math.min(12, NOT_SHOWN.size())));
        }
        System.out.println("M3StringLiteralRegexRuleTest checks=" + checks
                + " failures=" + FAILURES.size());
        if (!FAILURES.isEmpty()) {
            Map<String, List<String>> byCheck = new TreeMap<>();
            for (String failure : FAILURES) {
                String check = failure.substring(failure.lastIndexOf(": ") + 2);
                byCheck.computeIfAbsent(check, k -> new ArrayList<>()).add(failure);
            }
            byCheck.forEach((check, all) -> System.out.println(
                    all.size() + " x " + check + ", e.g. " + all.get(0)));
            throw new AssertionError(FAILURES.size() + " failed checks");
        }
    }

    /**
     * Binds the M3 handles and runs the structural checks: String declares no literal-regex
     * predicate (the discriminator of this leaf: the former String.isConservativeLiteralRegex),
     * and M3String declares the owner and its two rules. False when the owner is absent.
     */
    private static boolean bind() throws Exception {
        List<String> own = new ArrayList<>();
        for (Method declared : String.class.getDeclaredMethods()) {
            if (declared.getName().toLowerCase(Locale.ROOT).contains("literalregex")) {
                own.add(declared.getName());
            }
        }
        check(own.isEmpty(), "String declares " + own + ": String declares no literal-regex"
                + " predicate");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        internChars = method(pool, "internChars", char[].class, int.class, int.class);
        slice = method(m3, "slice", int.class, int.class);
        fromM3 = String.class.getDeclaredConstructor(m3);
        fromM3.setAccessible(true);
        stringM3 = String.class.getDeclaredField("m3");
        stringM3.setAccessible(true);
        isLiteralRegex = method(m3, "isLiteralRegex", String.class);
        try {
            literalRegex = method(m3, "literalRegex", String.class);
            isLiteralMatchesRegex = method(m3, "isLiteralMatchesRegex", String.class);
            literalMatches = constant(m3, "LITERAL_MATCHES");
            literalSearch = constant(m3, "LITERAL_SEARCH");
        } catch (NoSuchMethodException | NoSuchFieldException absent) {
            check(false, absent + ": M3String declares the owner literalRegex and its rules");
            return false;
        }
        check(Integer.bitCount(literalMatches) == 1 && Integer.bitCount(literalSearch) == 1
                && literalMatches != literalSearch, "the two rules are distinct bits");
        return true;
    }

    /** One regex: its rules, then the semantics each rule claims, or the refusal's evidence. */
    private static void rule(String regex, List<String> inputs, List<String[]> receivers)
            throws Throwable {
        int rules = (int) invoke(literalRegex, null, regex);
        int former = (formerConservative(regex) ? literalMatches : 0)
                | (formerLiteral(regex) ? literalSearch : 0);
        check(rules == former,
                show(regex) + ": literalRegex is the union of the former predicates");
        boolean matches = (rules & literalMatches) != 0;
        boolean search = (rules & literalSearch) != 0;
        check((boolean) invoke(isLiteralMatchesRegex, null, regex) == matches
                && (boolean) invoke(isLiteralRegex, null, regex) == search,
                show(regex) + ": the readers answer the owner's bits");
        Pattern pattern = compiled(regex);
        if (matches) {
            check(pattern != null, show(regex) + ": a LITERAL_MATCHES regex compiles");
        }
        if (pattern == null) {
            if (!matches) {
                matchesRefused++;
                matchesSyntax++;
            }
            return;
        }
        if (matches) {
            matchesRule(regex, pattern, inputs, receivers);
        } else {
            matchesRefusal(regex, pattern, inputs);
        }
        if (search) {
            searchRule(regex, pattern, inputs, receivers);
        } else if (matches) {
            searchRefusal(regex, pattern, inputs);
        }
    }

    /** LITERAL_MATCHES: Pattern.matches and String.matches are equality with the regex. */
    private static void matchesRule(String regex, Pattern pattern, List<String> inputs,
            List<String[]> receivers) {
        for (int k = 0; k < inputs.size(); k++) {
            String input = inputs.get(k);
            boolean equal = regex.equals(input);
            if (!ok(pattern.matcher(input).matches() == equal)) {
                fail(regex, input, "Pattern.matches of a LITERAL_MATCHES regex is equality");
            }
            for (String receiver : receivers.get(k)) {
                if (receiver != null && !ok(receiver.matches(regex) == equal)) {
                    fail(regex, input, kind(receiver) + " String.matches is equality");
                }
            }
        }
    }

    /**
     * LITERAL_SEARCH: Matcher.find sees exactly the indexOf occurrences; Matcher.replaceAll is
     * String.replace; String.split is Pattern.split for limits 0, -1 and 2; String.replaceAll
     * and replaceFirst of flat and M3-backed receivers replace exactly those occurrences.
     */
    private static void searchRule(String regex, Pattern pattern, List<String> inputs,
            List<String[]> receivers) {
        for (int k = 0; k < inputs.size(); k++) {
            String input = inputs.get(k);
            if (!ok(finds(pattern, input).equals(occurrences(input, regex)))) {
                fail(regex, input, "Matcher.find sees the indexOf occurrences");
            }
            String replaced = input.replace(regex, REPLACEMENT);
            String first = replaceFirstOccurrence(input, regex);
            if (!ok(pattern.matcher(input).replaceAll(REPLACEMENT).equals(replaced))) {
                fail(regex, input, "Matcher.replaceAll is String.replace");
            }
            if (!ok(pattern.matcher(input).replaceFirst(REPLACEMENT).equals(first))) {
                fail(regex, input, "Matcher.replaceFirst replaces the first indexOf occurrence");
            }
            String[][] pieces = new String[LIMITS.length][];
            String[][] delimited = new String[LIMITS.length][];
            for (int l = 0; l < LIMITS.length; l++) {
                pieces[l] = pattern.split(input, LIMITS[l]);
                delimited[l] = pattern.splitWithDelimiters(input, LIMITS[l]);
                if (!ok(Arrays.equals(occurrenceSplit(input, regex, LIMITS[l]), pieces[l]))) {
                    fail(regex, input, "Pattern.split(limit " + LIMITS[l]
                            + ") splits at the indexOf occurrences");
                }
            }
            for (String receiver : receivers.get(k)) {
                if (receiver == null) {
                    continue;
                }
                for (int l = 0; l < LIMITS.length; l++) {
                    if (!ok(Arrays.equals(receiver.split(regex, LIMITS[l]), pieces[l]))) {
                        fail(regex, input, kind(receiver) + " String.split(limit " + LIMITS[l]
                                + ") is Pattern.split");
                    }
                    if (!ok(Arrays.equals(receiver.splitWithDelimiters(regex, LIMITS[l]),
                            delimited[l]))) {
                        fail(regex, input, kind(receiver) + " String.splitWithDelimiters(limit "
                                + LIMITS[l] + ") is Pattern.splitWithDelimiters");
                    }
                }
                if (!ok(receiver.replaceAll(regex, REPLACEMENT).equals(replaced))) {
                    fail(regex, input, kind(receiver) + " String.replaceAll is String.replace");
                }
                if (!ok(receiver.replaceFirst(regex, REPLACEMENT).equals(first))) {
                    fail(regex, input, kind(receiver) + " String.replaceFirst replaces the first"
                            + " indexOf occurrence");
                }
            }
        }
    }

    /**
     * A regex refused LITERAL_MATCHES that compiles: java.util.regex shows the refusal
     * necessary when some input matches it other than by equality.
     */
    private static void matchesRefusal(String regex, Pattern pattern, List<String> inputs) {
        matchesRefused++;
        for (String input : inputs) {
            if (pattern.matcher(input).matches() != regex.equals(input)) {
                matchesBehaviour++;
                return;
            }
        }
        NOT_SHOWN.add(show(regex));
    }

    /**
     * A LITERAL_MATCHES regex refused LITERAL_SEARCH (empty, or holding a surrogate):
     * java.util.regex shows the refusal necessary when find, split or replaceAll differs from
     * the indexOf occurrences on some input.
     */
    private static void searchRefusal(String regex, Pattern pattern, List<String> inputs) {
        searchRefused++;
        for (String input : inputs) {
            boolean differs = !finds(pattern, input).equals(occurrences(input, regex))
                    || !pattern.matcher(input).replaceAll(REPLACEMENT)
                            .equals(input.replace(regex, REPLACEMENT));
            for (int l = 0; l < LIMITS.length && !differs; l++) {
                differs = !Arrays.equals(occurrenceSplit(input, regex, LIMITS[l]),
                        pattern.split(input, LIMITS[l]));
            }
            if (differs) {
                searchShown++;
                return;
            }
        }
        NOT_SHOWN.add(show(regex) + " (search)");
    }

    /** The removed String.isConservativeLiteralRegex: the literal matches lane before S5. */
    private static boolean formerConservative(String regex) {
        for (int index = 0; index < regex.length(); index++) {
            char unit = regex.charAt(index);
            if (unit == '\\' || ".^$|?*+()[]{}".indexOf(unit) >= 0) return false;
        }
        return true;
    }

    /** The former M3String.isLiteralRegex: the literal search lane before S5. */
    private static boolean formerLiteral(String regex) {
        if (regex == null || regex.isEmpty()) return false;
        for (int index = 0; index < regex.length(); index++) {
            char unit = regex.charAt(index);
            if (Character.isSurrogate(unit) || "\\.^$|?*+()[]{}".indexOf(unit) >= 0) return false;
        }
        return true;
    }

    private static Pattern compiled(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException refused) {
            return null;
        }
    }

    private static List<Integer> finds(Pattern pattern, String input) {
        List<Integer> starts = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        return starts;
    }

    /** The non-overlapping String.indexOf occurrences, left to right (every index for ""). */
    private static List<Integer> occurrences(String input, String literal) {
        List<Integer> starts = new ArrayList<>();
        for (int from = 0; from <= input.length(); ) {
            int at = input.indexOf(literal, from);
            if (at < 0) {
                break;
            }
            starts.add(at);
            from = at + Math.max(1, literal.length());
        }
        return starts;
    }

    /**
     * The split a literal lane makes at the indexOf occurrences (the rule of
     * String.splitLiteral; an empty literal occurs at every index): the pieces between
     * occurrences, at most limit pieces when limit is positive, trailing empty pieces dropped
     * when limit is 0.
     */
    private static String[] occurrenceSplit(String input, String literal, int limit) {
        List<Integer> starts = occurrences(input, literal);
        if (starts.isEmpty()) {
            return new String[] {input};
        }
        List<String> pieces = new ArrayList<>();
        int off = 0;
        for (int start : starts) {
            if (limit > 0 && pieces.size() == limit - 1) {
                break;
            }
            pieces.add(input.substring(off, start));
            off = start + literal.length();
        }
        pieces.add(input.substring(off));
        int size = pieces.size();
        while (limit == 0 && size > 0 && pieces.get(size - 1).isEmpty()) {
            size--;
        }
        return pieces.subList(0, size).toArray(new String[0]);
    }

    private static String replaceFirstOccurrence(String input, String literal) {
        int at = input.indexOf(literal);
        return at < 0 ? input
                : input.substring(0, at) + REPLACEMENT + input.substring(at + literal.length());
    }

    /** Every String of 0 to maxUnits units over the alphabet, shortest first. */
    private static List<String> words(String alphabet, int maxUnits) {
        List<String> words = new ArrayList<>(List.of(""));
        int from = 0;
        for (int units = 1; units <= maxUnits; units++) {
            int to = words.size();
            for (int w = from; w < to; w++) {
                for (int a = 0; a < alphabet.length(); a++) {
                    words.add(words.get(w) + alphabet.charAt(a));
                }
            }
            from = to;
        }
        return words;
    }

    /** Per input: a String of the stock path, an M3 atom and an M3 slice (null if refused). */
    private static List<String[]> receivers(List<String> inputs) throws Throwable {
        List<String[]> receivers = new ArrayList<>();
        for (String input : inputs) {
            String[] three = {new String(input.toCharArray()), null, null};
            if (!input.isEmpty()) {
                three[1] = backed(intern(input), input);
                Object wider = intern("<" + input + ">");
                three[2] = wider == null ? null
                        : backed(invoke(slice, wider, 1, input.length() + 1), input);
            }
            receivers.add(three);
        }
        return receivers;
    }

    private static String backed(Object storage, String input) throws Exception {
        if (storage == null) {
            m3Refused++;
            return null;
        }
        String backed = fromM3.newInstance(storage);
        check(backed.equals(input) && stringM3.get(backed) != null,
                show(input) + ": the M3-backed receiver holds the input");
        m3Receivers++;
        return backed;
    }

    private static Object intern(String text) throws Throwable {
        char[] units = text.toCharArray();
        return invoke(internChars, null, units, 0, units.length);
    }

    private static String kind(String s) {
        try {
            return stringM3.get(s) == null ? "flat" : "M3";
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** The units of s in quotes, each outside printable ASCII as a Unicode escape. */
    private static String show(String s) {
        StringBuilder shown = new StringBuilder("\"");
        for (int index = 0; index < s.length(); index++) {
            char unit = s.charAt(index);
            if (unit >= 0x20 && unit < 0x7f) {
                shown.append(unit);
            } else {
                shown.append(String.format("\\u%04x", (int) unit));
            }
        }
        return shown.append('"').toString();
    }

    private static int constant(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static Object invoke(Method method, Object target, Object... arguments)
            throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters)
            throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static boolean ok(boolean condition) {
        checks++;
        return condition;
    }

    private static void fail(String regex, String input, String check) {
        FAILURES.add(show(regex) + " on " + show(input) + ": " + check);
    }

    private static void check(boolean condition, String label) {
        if (!ok(condition)) {
            FAILURES.add(label);
        }
    }
}
