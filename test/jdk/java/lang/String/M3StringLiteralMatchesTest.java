/*
 * @test
 * @summary M3 String ordinary-literal matches fast path preserves Pattern.matches semantics
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLiteralMatchesTest
 */

import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringLiteralMatchesTest {
    private static long checks;

    public static void main(String[] args) {
        deterministic();
        randomized();
        fallbackSyntax();
        System.out.println("M3_STRING_LITERAL_MATCHES_PASS|checks=" + checks);
    }

    private static void deterministic() {
        checkLiteral("", "");
        checkLiteral("alpha", "alpha");
        checkLiteral("alpha", "beta");
        checkLiteral("\ud83d\ude42", "\ud83d\ude42");
        checkLiteral("\ud83d", "\ud83d");
        checkLiteral("\ude42", "\ude42");
        checkLiteral("a\ud83d\ude42b", "a\ud83d\ude42b");
        checkLiteral("\u0000\uffff", "\u0000\uffff");

        String composed = String.join("", "alpha", "-", "\ud83d", "\ude42");
        String literal = "alpha-\ud83d\ude42";
        check(composed.matches(literal) == Pattern.matches(literal, new StringBuilder(composed)),
                "composed literal");
    }

    private static void randomized() {
        Random random = new Random(0x4d334d415443484cL);
        char[] alphabet = {
                0, 'a', 'b', 'X', ' ', '-', '_', '\u00ff', '\u0100',
                '\ud83d', '\ude42', '\ud800', '\udc00', '\uffff'
        };
        for (int trial = 0; trial < 2_000; trial++) {
            int sourceLength = random.nextInt(48);
            char[] sourceChars = new char[sourceLength];
            for (int i = 0; i < sourceLength; i++) {
                sourceChars[i] = alphabet[random.nextInt(alphabet.length)];
            }
            String raw = new String(sourceChars);
            int middle = raw.length() >>> 1;
            String source = String.join("", raw.substring(0, middle), raw.substring(middle));

            char[] patternChars;
            if ((trial & 1) == 0) {
                patternChars = sourceChars.clone();
            } else {
                int patternLength = random.nextInt(48);
                patternChars = new char[patternLength];
                for (int i = 0; i < patternLength; i++) {
                    patternChars[i] = alphabet[random.nextInt(alphabet.length)];
                }
            }
            String regex = new String(patternChars);
            boolean expected = Pattern.matches(regex, new StringBuilder(source));
            check(source.matches(regex) == expected, "random literal " + trial);
        }
    }

    private static void fallbackSyntax() {
        String source = String.join("", "alpha", "123", "\ud83d\ude42");
        for (String regex : new String[] {
                "alpha.*",
                "alpha[0-9]+\\ud83d\\ude42",
                "^alpha123.*$",
                "(?:alpha)123.*",
                "alpha123\\x{1F642}",
                "alpha123.?"
        }) {
            boolean expected = Pattern.matches(regex, new StringBuilder(source));
            check(source.matches(regex) == expected, "syntax fallback " + regex);
        }

        for (String invalid : new String[] {"\\", "[", "(", "*a"}) {
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

    private static void checkLiteral(String sourceText, String regex) {
        String source = String.join("", sourceText.substring(0, sourceText.length() >>> 1),
                sourceText.substring(sourceText.length() >>> 1));
        boolean expected = Pattern.matches(regex, new StringBuilder(sourceText));
        check(source.matches(regex) == expected, "literal " + Integer.toHexString(regex.hashCode()));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
