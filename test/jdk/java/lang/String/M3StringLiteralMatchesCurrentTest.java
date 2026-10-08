/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Recover conservative literal matches without changing JDK regex semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralMatchesCurrentTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringLiteralMatchesCurrentTest
 */
import java.lang.reflect.Field;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringLiteralMatchesCurrentTest {
    private static final Field M3;
    private static long checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        String full = new String(new char[] {'a', 'l', 'p', 'h', 'a', '-', '\u03B2'});
        require(M3.get(full) != null, "test must use actual M3-backed source");
        compare(full, "alpha-\u03B2");
        compare(full, "alpha-\u03B3");
        compare(full, "alpha-.*");
        compare(full, "(alpha)-\u03B2");
        compare(full, "^alpha-\u03B2$");
        compare(full, "alpha-[\u03B2\u03B3]");
        compare(full, "");
        compare(full, "alpha-\\u03B2");
        compare(full, "alpha-\\Q\u03B2\\E");

        String surrogate = new String(new char[] {'\uD83D', '\uDE03'});
        compare(surrogate, "\uD83D\uDE03");
        compare(surrogate, "\uD83D");
        compare(surrogate, ".");
        compare(new String(new char[] {'a'}), "");
        compare(new String(new char[0]), "");

        try {
            full.matches("[");
            throw new AssertionError("malformed regex accepted");
        } catch (PatternSyntaxException expected) {
            checks++;
        }

        Random random = new Random(0xB0A7D21L);
        char[] alphabet = {'a', 'b', 'c', '-', '\n', '\u00E9', '\uD83D', '\uDE03', '\0'};
        String[] patterns = {"a", "abc", "a-b", "aa", "", ".*", "a.*",
                "[ab]+", "^a", "a$", "(a|b)+", "\uD83D", "\uD83D\uDE03", "a\\nb"};
        for (int i = 0; i < 2_048; i++) {
            char[] chars = new char[random.nextInt(40) + 1];
            for (int k = 0; k < chars.length; k++) {
                chars[k] = alphabet[random.nextInt(alphabet.length)];
            }
            String subject = new String(chars);
            require(M3.get(subject) != null, "random input must be M3-backed");
            for (String regex : patterns) compare(subject, regex);
        }
        System.out.println("M3_STRING_LITERAL_MATCHES_CURRENT_PASS|checks=" + checks);
    }

    private static void compare(String subject, String regex) {
        boolean expected = Pattern.matches(regex, subject);
        boolean actual = subject.matches(regex);
        require(expected == actual, "matches differential: " + regex);
    }

    private static void require(boolean valid, String description) {
        checks++;
        if (!valid) throw new AssertionError(description);
    }
}
