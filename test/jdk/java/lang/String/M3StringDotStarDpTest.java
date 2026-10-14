/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String bounded dot/star full-match DP matches the JDK Pattern oracle
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringDotStarDpTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringDotStarDpTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringDotStarDpTest {
    private static final char[] INPUT_ALPHABET = {
            'a', 'b', '\n', '\r', '\u0085', '\u2028', '\u2029', '\u00e9'
    };
    private static final String[] ATOMS = {
            "a", "b", ".", "a*", "b*", ".*", "A", "A*"
    };
    private static final Method DIRECT;
    private static long checks;

    static {
        try {
            Class<?> helper = Class.forName("java.lang.M3DotStarRegex");
            DIRECT = helper.getDeclaredMethod("tryFullMatch", CharSequence.class, String.class);
            DIRECT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] ignored) throws Exception {
        differential();
        admissionRefusals();
        exceptionAndLinebreakSemantics();
        System.out.println("M3_STRING_DOT_STAR_DP_PASS|checks=" + checks);
    }

    private static void differential() throws Exception {
        List<String> patterns = patterns();
        List<String> texts = texts();
        List<String> composed = new ArrayList<>(texts.size());
        for (String text : texts) composed.add(compose(text));

        for (String regex : patterns) {
            Pattern jdk = Pattern.compile(regex);
            for (String text : composed) {
                boolean expected = jdk.matcher(new StringBuilder(text)).matches();
                boolean actual = text.matches(regex);
                check(expected == actual, "String.matches regex=" + regex
                        + ", textHash=" + Integer.toHexString(text.hashCode()));

                int primitive = direct(text, regex);
                check(primitive >= 0, "eligible dot-star refused");
                check((primitive == 1) == expected, "DP oracle mismatch");
            }
        }
        check(patterns.size() == 585 && texts.size() == 585, "corpus geometry");
    }

    private static void admissionRefusals() throws Exception {
        for (String unsupported : List.of(
                "*", "*a", "a**", "ab***", "[ab]", "a+", "^a$", "\\d", "(?:a)",
                "(?i)a", "a?", "a{2}", "a|b", "\\Qabc\\E")) {
            check(direct("aaa", unsupported) == -1, "unsupported admitted: " + unsupported);
        }
        for (String surrogate : List.of("\ud83d\ude42", "\ud800", "x\ude00")) {
            check(direct(surrogate, ".*") == -1, "surrogate admitted");
        }
        check(direct("a".repeat(100_000), "a".repeat(512)) == -1, "work budget");
        check(direct("aaa", "a".repeat(513)) == -1, "pattern budget");
    }

    private static void exceptionAndLinebreakSemantics() throws Exception {
        String source = compose("a\nb");
        check(!source.matches("a.b"), "default dot must not match LF");
        check(Pattern.compile("a.b", Pattern.DOTALL).matcher(source).matches(),
                "DOTALL remains distinct");

        for (String invalid : List.of("*", "a**", "[", "\\")) {
            try {
                source.matches(invalid);
                throw new AssertionError("invalid regex did not throw: " + invalid);
            } catch (PatternSyntaxException expected) {
                checks++;
            }
        }
        try {
            source.matches(null);
            throw new AssertionError("null regex did not throw");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    private static int direct(CharSequence input, String regex) throws Exception {
        return (Integer) DIRECT.invoke(null, input, regex);
    }

    private static List<String> patterns() {
        List<String> all = new ArrayList<>();
        List<String> depth = List.of("");
        all.add("");
        for (int i = 0; i < 3; i++) {
            List<String> next = new ArrayList<>();
            for (String prefix : depth) {
                for (String atom : ATOMS) next.add(prefix + atom);
            }
            all.addAll(next);
            depth = next;
        }
        return all;
    }

    private static List<String> texts() {
        List<String> all = new ArrayList<>();
        List<String> depth = List.of("");
        all.add("");
        for (int i = 0; i < 3; i++) {
            List<String> next = new ArrayList<>();
            for (String prefix : depth) {
                for (char unit : INPUT_ALPHABET) next.add(prefix + unit);
            }
            all.addAll(next);
            depth = next;
        }
        return all;
    }

    private static String compose(String raw) {
        int p = raw.length() / 3;
        int q = (raw.length() * 2) / 3;
        return String.join("", raw.substring(0, p), raw.substring(p, q), raw.substring(q));
    }

    private static void check(boolean valid, String reason) {
        checks++;
        if (!valid) throw new AssertionError(reason);
    }
}
