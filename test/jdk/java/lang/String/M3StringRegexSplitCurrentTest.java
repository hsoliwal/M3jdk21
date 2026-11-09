/*
 * @test
 * @summary Current Pattern.split path preserves M3 String subsequence representation and JDK semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexSplitCurrentTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class M3StringRegexSplitCurrentTest {
    private static final Field STRING_M3;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministic();
        randomized();
        noMatchIdentity();
        invalidRegex();
        System.out.println("M3_STRING_REGEX_SPLIT_CURRENT_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        String source = compose("boo:::and::foo");
        for (String regex : new String[] {":+", "o", "(?=:)", "::", "[oa]+", "\\b"}) {
            for (int limit : new int[] {-3, -1, 0, 1, 2, 5, 12}) {
                compare(source, regex, limit, false);
                compare(source, regex, limit, true);
            }
        }

        String unicode = compose("a\ud83d\ude42::b\ud83d\ude42:c");
        for (String regex : new String[] {":+", "\ud83d\ude42", "(?=\ud83d\ude42)"}) {
            for (int limit : new int[] {-1, 0, 1, 2, 8}) {
                compare(unicode, regex, limit, false);
                compare(unicode, regex, limit, true);
            }
        }
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d3353504c49544cL);
        String[] regexes = {":", ":+", "a+", "[ab]", "(?=:)", "x", "ab"};
        char[] alphabet = {'a', 'b', ':', 'x', ' ', '\u00e9', '\ud83d', '\ude42'};

        for (int trial = 0; trial < 512; trial++) {
            char[] value = new char[random.nextInt(80)];
            for (int i = 0; i < value.length; i++) {
                value[i] = alphabet[random.nextInt(alphabet.length)];
            }
            String source = compose(new String(value));
            String regex = regexes[random.nextInt(regexes.length)];
            int limit = switch (random.nextInt(6)) {
                case 0 -> -1;
                case 1 -> 0;
                case 2 -> 1;
                default -> 2 + random.nextInt(6);
            };
            compare(source, regex, limit, false);
            compare(source, regex, limit, true);
        }
    }

    private static void noMatchIdentity() throws Exception {
        String source = compose("alpha-beta");
        String[] split = source.split("Z+", 0);
        check(split.length == 1 && split[0] == source, "no-match split preserves original");
        checkM3(split);

        String[] delimited = source.splitWithDelimiters("Z+", 0);
        check(delimited.length == 1 && delimited[0] == source,
                "no-match splitWithDelimiters preserves original");
        checkM3(delimited);
    }

    private static void invalidRegex() {
        String source = compose("abc");
        for (String regex : new String[] {"[", "(", "*a", "\\"}) {
            try {
                source.split(regex, 0);
                throw new AssertionError("invalid split regex did not throw: " + regex);
            } catch (PatternSyntaxException expected) {
                checks++;
            }
            try {
                source.splitWithDelimiters(regex, 0);
                throw new AssertionError("invalid delimiter regex did not throw: " + regex);
            } catch (PatternSyntaxException expected) {
                checks++;
            }
        }
    }

    private static void compare(
            String source, String regex, int limit, boolean withDelimiters) throws Exception {
        Pattern pattern = Pattern.compile(regex);
        CharSequence oracleInput = new StringBuilder(source);
        String[] expected = withDelimiters
                ? pattern.splitWithDelimiters(oracleInput, limit)
                : pattern.split(oracleInput, limit);
        String[] actual = withDelimiters
                ? source.splitWithDelimiters(regex, limit)
                : source.split(regex, limit);

        check(Arrays.equals(actual, expected),
                "split parity regex=" + regex + " limit=" + limit + " delimiters=" + withDelimiters);
        checkM3(actual);
    }

    private static void checkM3(String[] values) throws Exception {
        for (String value : values) {
            if (!value.isEmpty()) {
                check(STRING_M3.get(value) != null, "non-empty split result lost M3 storage");
            }
        }
    }

    private static String compose(String value) {
        int first = value.length() / 3;
        int second = (value.length() * 2) / 3;
        return String.join("", value.substring(0, first), value.substring(first, second),
                value.substring(second));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
