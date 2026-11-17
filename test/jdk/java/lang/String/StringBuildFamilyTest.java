/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String build family (leftPad ... overlay, zeroFill, padStart)
 * @run junit StringBuildFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringBuildFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringBuildFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every build-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0, CPython
 * 3.12 (zeroFill) and Node.js (padStart, padEnd) on every text of up to four units over the
 * alphabet below (donor-oracle proof in the string-lane record).
 */
public class StringBuildFamilyTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE00;
    private static final String PAIR = "" + HIGH + LOW;
    private static final char[] UNITS = {'a', 'A', ',', '-', HIGH, LOW};
    private static final String[] PADS = {"", "z", "yz", PAIR, "a" + PAIR};
    private static final CharSequence[] MORE = {null, "", "A"};

    static Stream<String> texts() {
        List<String> texts = new ArrayList<>();
        for (int length = 0; length <= 3; length++) {
            int total = (int) Math.pow(UNITS.length, length);
            for (int code = 0; code < total; code++) {
                char[] units = new char[length];
                int rest = code;
                for (int i = 0; i < length; i++) {
                    units[i] = UNITS[rest % UNITS.length];
                    rest /= UNITS.length;
                }
                texts.add(new String(units));
            }
        }
        return texts.stream();
    }

    private static List<Object> answers(String s) {
        List<Object> out = new ArrayList<>();
        for (int n = -1; n <= 5; n++) {
            out.add(s.leftPad(n));
            out.add(s.rightPad(n, HIGH));
            out.add(s.center(n));
            out.add(s.center(n, '*'));
            out.add(s.zeroFill(n));
            out.add(s.rotate(n));
            out.add(s.repeat(",", n));
            for (String pad : PADS) {
                out.add(s.leftPad(n, pad));
                out.add(s.rightPad(n, pad));
                out.add(s.center(n, pad));
                out.add(s.padStart(n, pad));
                out.add(s.padEnd(n, pad));
            }
            for (int m = -1; m <= 4; m++) {
                out.add(s.overlay(PAIR, n, m));
            }
        }
        out.add(s.reverse());
        out.add(s.reverseDelimited(','));
        out.add(s.reverseDelimited(HIGH));
        for (String affix : new String[] {"", "a", "A", PAIR}) {
            out.add(s.appendIfMissing(affix, MORE));
            out.add(s.appendIfMissingIgnoreCase(affix));
            out.add(s.prependIfMissing(affix));
            out.add(s.prependIfMissingIgnoreCase(affix, MORE));
            out.add(s.wrap(affix));
            out.add(s.wrapIfMissing(affix));
        }
        out.add(s.wrap('a'));
        out.add(s.wrapIfMissing(HIGH));
        return out;
    }

    private static List<Function<String, String>> forms() {
        return List.of(
            text -> new String(("<<" + text + ">>").toCharArray()).substring(2, 2 + text.length()),
            text -> new String(text.toCharArray(), 0, text.length() / 2)
                    .concat(new String(text.toCharArray(), text.length() / 2,
                            text.length() - text.length() / 2)));
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("texts")
    void everyFormAnswersAsTheFlatString(String text) {
        List<Object> expected = answers(new String(text.toCharArray()));
        for (Function<String, String> form : forms()) {
            assertEquals(expected, answers(form.apply(text)));
        }
    }

    @Test
    void padRules() {
        assertEquals("  abc", "abc".leftPad(5));
        assertEquals("abc**", "abc".rightPad(5, '*'));
        assertEquals("yzyabc", "abc".leftPad(6, "yz"), "the pad repeats from its start");
        assertEquals("  abc", "abc".leftPad(5, ""), "an empty pad pads with spaces");
        assertEquals("abc", "abc".padStart(5, ""), "JavaScript: an empty pad adds nothing");
        assertEquals("a", "a".leftPad(2, PAIR), "commons never splits a pair of the pad");
        assertEquals(HIGH + "a", "a".padStart(2, PAIR), "JavaScript cuts the pad anywhere");
        assertEquals("a" + HIGH, "a".padEnd(2, PAIR));
        assertEquals("abc", "abc".leftPad(Integer.MIN_VALUE, 'x'));
        assertEquals(" ab  ", "ab".center(5), "the left side gets half, rounded down");
        assertEquals("yzabyzy", "ab".center(7, "yz"));
        assertThrows(NullPointerException.class, () -> "abc".leftPad(1, (String) null));
        assertThrows(NullPointerException.class, () -> "abc".center(-1, (String) null));
        assertThrows(NullPointerException.class, () -> "abc".padEnd(9, null));
    }

    @Test
    void zeroFillRules() {
        assertEquals("-0042", "-42".zeroFill(5), "the sign stays in front");
        assertEquals("+00", "+".zeroFill(3));
        assertEquals("000", "".zeroFill(3));
        assertEquals("0a-", "a-".zeroFill(3));
        assertEquals("0" + PAIR, PAIR.zeroFill(2), "a pair counts as one code point");
        assertEquals("-0" + PAIR, ("-" + PAIR).zeroFill(3));
        String text = "abc";
        assertSame(text, text.zeroFill(3));
    }

    @Test
    void reverseAndRotateRules() {
        assertEquals("cba", "abc".reverse());
        assertEquals(PAIR + "a", ("a" + PAIR).reverse(), "a pair keeps its order");
        assertEquals("" + HIGH + LOW, ("" + LOW + HIGH).reverse(),
                "StringBuilder.reverse turns a low-high run into a pair");
        assertEquals("c.b.a", "a.b.c".reverseDelimited('.'));
        assertEquals("c.b.a", "..a..b.c.".reverseDelimited('.'), "empty parts are dropped");
        assertEquals("", "...".reverseDelimited('.'));
        String text = "abc";
        assertSame(text, text.reverseDelimited('.'));
        assertEquals("cab", "abc".rotate(1));
        assertEquals("bca", "abc".rotate(-1));
        assertSame(text, text.rotate(3));
        assertEquals("" + LOW + "a" + HIGH, ("a" + PAIR).rotate(1), "a rotation may split a pair");
    }

    @Test
    void affixAndWrapRules() {
        assertEquals("abcxyz", "abc".appendIfMissing("xyz"));
        assertEquals("abcXYZ", "abcXYZ".appendIfMissingIgnoreCase("xyz"));
        assertEquals("abc", "abc".appendIfMissing("xyz", ""), "an empty element always matches");
        assertEquals("abcxyz", "abc".appendIfMissing("xyz", (CharSequence) null),
                "null elements are skipped");
        assertEquals("xyzabc", "abc".prependIfMissing("xyz", "mno"));
        assertEquals("mnoabc", "mnoabc".prependIfMissing("xyz", "mno"));
        assertThrows(NullPointerException.class, () -> "abc".appendIfMissing(null));
        assertThrows(NullPointerException.class,
                () -> "abc".prependIfMissing("", (CharSequence[]) null));
        assertEquals("'ab'", "ab".wrap('\''));
        assertEquals("ab", "ab".wrap('\0'));
        assertEquals("xxabxx", "ab".wrap("xx"));
        assertEquals("/a/", "a/".wrapIfMissing('/'));
        assertEquals("/a/", "/a".wrapIfMissing("/"));
        assertEquals("", "".wrap("x"));
        assertThrows(NullPointerException.class, () -> "ab".wrapIfMissing(null));
    }

    @Test
    void repeatAndOverlayRules() {
        assertEquals("?, ?, ?", "?".repeat(", ", 3));
        assertEquals("xx", "".repeat("x", 3));
        assertEquals("", "ab".repeat(",", 0));
        assertThrows(IllegalArgumentException.class, () -> "?".repeat(", ", 1_073_741_824));
        assertThrows(NullPointerException.class, () -> "".repeat(null, 2));
        assertEquals("aZZc", "abc".overlay("ZZ", 1, 2));
        assertEquals("aZZc", "abc".overlay("ZZ", 2, 1), "the ends are swapped");
        assertEquals("abcZZ", "abc".overlay("ZZ", 4, 9), "the ends are clamped");
        assertEquals("aZ", ("a" + PAIR).overlay("Z", 2, 2), "a cut never splits a pair");
        assertThrows(NullPointerException.class, () -> "abc".overlay(null, 0, 1));
    }

    @Test
    void longTextsAcrossWindows() {
        for (int at = 505; at <= 1030; at += (at == 520 ? 498 : 1)) {
            String text = "x".repeat(at) + PAIR + "y".repeat(7);
            String reversed = "y".repeat(7) + PAIR + "x".repeat(at);
            assertEquals(reversed, text.reverse());
            assertEquals(text, reversed.reverse());
            assertEquals("0".repeat(2) + text, text.zeroFill(text.length() + 1));
            assertEquals("y".repeat(7) + "," + "x".repeat(at), ("x".repeat(at) + ",,"
                    + "y".repeat(7)).reverseDelimited(','));
        }
    }
}
