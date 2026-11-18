/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3JDK21 Rust delimiter splits and Scala margin removal preserve canonical ranges
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED StringCrossLanguageTest
 */

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public class StringCrossLanguageTest {
    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + show(expected) + " got " + show(actual));
        }
    }

    private static void expectParts(String[] actual, String... expected) {
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError("expected " + Arrays.toString(expected)
                    + " got " + Arrays.toString(actual));
        }
    }

    private static String show(String text) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            out.append(String.format("\\u%04x", (int) text.charAt(index)));
        }
        return out.toString();
    }

    private static void samples() {
        expectParts("cfg=foo=bar".splitOnce("="), "cfg", "foo=bar");
        expectParts("cfg=foo=bar".rsplitOnce("="), "cfg=foo", "bar");
        expectParts("=foo".splitOnce("="), "", "foo");
        expectParts("foo=".rsplitOnce("="), "foo", "");
        expectParts("anything".splitOnce(""), "", "anything");
        expectParts("anything".rsplitOnce(""), "anything", "");
        expectParts("".splitOnce(""), "", "");
        expectParts("".rsplitOnce(""), "", "");
        expectParts("a\uD83D\uDE00b".splitOnce("\uD83D\uDE00"), "a", "b");
        expectParts("a\uD800b".splitOnce("\uD800"), "a", "b");
        if ("abc".splitOnce("=") != null || "abc".rsplitOnce("=") != null
                || "\uD83D\uDE00".splitOnce("\uD83D") != null
                || "\uD83D\uDE00".rsplitOnce("\uDE00") != null) {
            throw new AssertionError("missing delimiter or partial surrogate was matched");
        }
        try {
            "abc".splitOnce(null);
            throw new AssertionError("splitOnce(null) must throw");
        } catch (NullPointerException expected) {
        }
        try {
            "abc".rsplitOnce(null);
            throw new AssertionError("rsplitOnce(null) must throw");
        } catch (NullPointerException expected) {
        }

        expect("one\n two\r\nthree\r", "  |one\n two\r\n\t|three\r".stripMargin());
        expect("aa\r\nbb\ncc\rdd", "  #aa\r\n\t#bb\ncc\r#dd".stripMargin('#'));
        expect("x\n", "\u001F|x\n".stripMargin());
        expect("\u2003|not-a-control", "\u2003|not-a-control".stripMargin());
        expect("\u2028|after-separator", "\u2028|after-separator".stripMargin());
        expect("", "|".stripMargin());
        expect("|\n", "  ||\n".stripMargin());
        String noMargin = new String(new char[] {'n', 'o', '|', 'x'});
        if (noMargin.stripMargin() != noMargin || noMargin.stripMargin('#') != noMargin
                || "".stripMargin() != "") {
            throw new AssertionError("unchanged stripMargin lost identity");
        }
    }

    private static boolean boundary(String text, int at) {
        return at == 0 || at == text.length()
                || !(Character.isHighSurrogate(text.charAt(at - 1))
                        && Character.isLowSurrogate(text.charAt(at)));
    }

    private static String[] splitOracle(String text, String delimiter, boolean reverse) {
        int best = -1;
        int width = delimiter.length();
        for (int at = 0; at <= text.length() - width; at++) {
            if (boundary(text, at) && boundary(text, at + width)
                    && text.regionMatches(at, delimiter, 0, width)) {
                best = at;
                if (!reverse) break;
            }
        }
        return best < 0 ? null : new String[] {
                text.substring(0, best), text.substring(best + width)};
    }

    private static String marginOracle(String text, char margin) {
        StringBuilder out = new StringBuilder();
        for (int start = 0; start < text.length();) {
            int end = start;
            while (end < text.length() && text.charAt(end) != '\n'
                    && text.charAt(end) != '\r') end++;
            int first = start;
            while (first < end && text.charAt(first) <= 0x20) first++;
            out.append(text, first < end && text.charAt(first) == margin ? first + 1 : start,
                    end);
            int next = end;
            if (next < text.length()) {
                if (text.charAt(next++) == '\r' && next < text.length()
                        && text.charAt(next) == '\n') next++;
                out.append(text, end, next);
            }
            start = next;
        }
        return out.toString();
    }

    private static void randomized() {
        char[] alphabet = {'a', 'Z', '#', '|', '=', ' ', '\t', '\r', '\n',
                '\u001F', '\uD83D', '\uDE00', '\uD800', '\uDC00', '\u2003'};
        char[] margins = {'|', '#', ' ', '\u2003'};
        Random random = new Random(0x9a4217bcL);
        for (int trial = 0; trial < 5_000; trial++) {
            int size = random.nextInt(80);
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < size; index++) {
                builder.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String source = builder.toString();
            String delimiter;
            if (random.nextBoolean()) {
                int begin = random.nextInt(size + 1);
                int end = begin + random.nextInt(size - begin + 1);
                delimiter = source.substring(begin, end);
            } else {
                delimiter = new String(new char[] {
                        alphabet[random.nextInt(alphabet.length)]});
            }
            expectParts(source.splitOnce(delimiter), splitOracle(source, delimiter, false));
            expectParts(source.rsplitOnce(delimiter), splitOracle(source, delimiter, true));
            char margin = margins[random.nextInt(margins.length)];
            expect(marginOracle(source, margin), source.stripMargin(margin));
        }
    }

    private static void composite() throws ReflectiveOperationException {
        String source = "  |A".concat("\r\n").concat("\t|B=last");
        String margin = source.stripMargin();
        expect("A\r\nB=last", margin);
        String[] pieces = margin.rsplitOnce("=");
        expectParts(pieces, "A\r\nB", "last");
        Method m3 = String.class.getDeclaredMethod("m3");
        m3.setAccessible(true);
        if (m3.invoke(source) != null && (m3.invoke(margin) == null
                || m3.invoke(pieces[0]) == null)) {
            throw new AssertionError("canonical M3 String ranges were materialized");
        }
    }

    public static void main(String[] args) throws Exception {
        samples();
        randomized();
        composite();
    }
}
