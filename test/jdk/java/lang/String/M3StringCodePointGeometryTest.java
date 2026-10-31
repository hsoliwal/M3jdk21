/*
 * @test
 * @summary Bounded M3 code-point geometry preserves exact JDK UTF-16 range and offset semantics
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCodePointGeometryTest
 */

import java.util.Random;

public class M3StringCodePointGeometryTest {
    private static long checks;

    public static void main(String[] args) {
        deterministic();
        randomized();
        oversizedFallback();
        System.out.println("M3_STRING_CODEPOINT_GEOMETRY_PASS|checks=" + checks);
    }

    private static void deterministic() {
        String[] values = {
                "",
                "abc",
                "\ud83d\ude42",
                "\ud83d",
                "\ude42",
                "a\ud83d\ude42b",
                "\ud83dX\ude42",
                "\ud83d\ude42\ud83d\ude42",
                "\u0000\ud83d\ude42\uffff"
        };
        for (String raw : values) {
            checkValue(compose(raw), raw.toCharArray(), "det");
        }

        String splitPair = String.join("", "\ud83d", "\ude42");
        check(splitPair.codePointCount(0, 2) == 1, "split pair whole");
        check(splitPair.codePointCount(0, 1) == 1, "split pair high range");
        check(splitPair.codePointCount(1, 2) == 1, "split pair low range");
        check(splitPair.offsetByCodePoints(0, 1) == 2, "split pair forward");
        check(splitPair.offsetByCodePoints(2, -1) == 0, "split pair backward");
    }

    private static void randomized() {
        Random random = new Random(0x4d33435047454f4dL);
        char[] alphabet = {
                0, 'a', 'b', 'X', '\u00ff', '\u0100',
                '\ud83d', '\ude42', '\ud800', '\udc00', '\uffff'
        };

        for (int trial = 0; trial < 512; trial++) {
            int length = random.nextInt(128);
            char[] value = new char[length];
            for (int i = 0; i < length; i++) {
                value[i] = alphabet[random.nextInt(alphabet.length)];
            }
            String source = compose(new String(value));
            checkValue(source, value, "random " + trial);
        }
    }

    private static void oversizedFallback() {
        char[] value = new char[33_000];
        for (int i = 0; i < value.length; i++) {
            value[i] = (i % 97 == 0) ? '\ud83d'
                    : (i % 97 == 1) ? '\ude42'
                    : (char) ('a' + (i % 23));
        }
        String source = compose(new String(value));
        check(source.codePointCount(0, source.length())
                        == Character.codePointCount(value, 0, value.length),
                "oversized count fallback");
        int oracle = Character.offsetByCodePoints(new StringBuilder(new String(value)), 0, 1000);
        check(source.offsetByCodePoints(0, 1000) == oracle, "oversized offset fallback");
    }

    private static void checkValue(String source, char[] oracle, String label) {
        for (int begin = 0; begin <= oracle.length; begin++) {
            int endStep = oracle.length <= 24 ? 1 : Math.max(1, oracle.length / 11);
            for (int end = begin; end <= oracle.length; end += endStep) {
                int expected = Character.codePointCount(oracle, begin, end - begin);
                check(source.codePointCount(begin, end) == expected,
                        label + " count " + begin + ":" + end);
            }
            if ((oracle.length - begin) % endStep != 0) {
                int expected = Character.codePointCount(oracle, begin, oracle.length - begin);
                check(source.codePointCount(begin, oracle.length) == expected,
                        label + " count tail " + begin);
            }
        }

        StringBuilder sequence = new StringBuilder(new String(oracle));
        for (int index = 0; index <= oracle.length; index++) {
            int before = Character.codePointCount(oracle, 0, index);
            int after = Character.codePointCount(oracle, index, oracle.length - index);
            int[] offsets = {
                    -before - 1, -before, Math.min(-1, -before),
                    0,
                    Math.max(1, after), after, after + 1
            };
            for (int offset : offsets) {
                compareOffset(source, sequence, index, offset, label);
            }
            if (before > 1) compareOffset(source, sequence, index, -(before / 2), label);
            if (after > 1) compareOffset(source, sequence, index, after / 2, label);
        }
    }

    private static void compareOffset(
            String source, CharSequence oracle, int index, int offset, String label) {
        Integer expected = null;
        Class<? extends Throwable> expectedFailure = null;
        try {
            expected = Character.offsetByCodePoints(oracle, index, offset);
        } catch (IndexOutOfBoundsException failure) {
            expectedFailure = failure.getClass();
        }

        try {
            int actual = source.offsetByCodePoints(index, offset);
            check(expectedFailure == null && actual == expected,
                    label + " offset index=" + index + " delta=" + offset);
        } catch (IndexOutOfBoundsException failure) {
            check(expectedFailure != null,
                    label + " unexpected offset failure index=" + index + " delta=" + offset);
        }
    }

    private static String compose(String value) {
        int middle = value.length() >>> 1;
        return String.join("", value.substring(0, middle), value.substring(middle));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
