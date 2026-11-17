/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String compare family (compare ... canonicalEquals)
 * @run junit StringCompareFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringCompareFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringCompareFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every compare-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0, Guava,
 * the Kotlin standard library and CPython 3.12 on every ordered pair of texts of up to three units
 * over the alphabet below (donor-oracle proof in the string-lane record).
 */
public class StringCompareFamilyTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char[] UNITS = {
        'a', 'A', 'e', (char) 0x0301, (char) 0xE9, (char) 0xDF, (char) 0x212A, HIGH, (char) 0xDE00
    };
    private static final String[] OTHERS = {
        "", "a", "A", "ae", "aE", "e" + (char) 0x0301, String.valueOf((char) 0xE9),
        "" + HIGH + (char) 0xDE00, "" + HIGH + (char) 0xDE01, "k"
    };

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
        for (String o : OTHERS) {
            out.add(String.compare(s, o));
            out.add(String.compareIgnoreCase(o, s, false));
            out.add(String.equals(s, o, true));
            out.add(s.difference(o));
            out.add(s.equalsAny(o, null));
            out.add(s.equalsAnyIgnoreCase(o));
            out.add(s.contentEqualsIgnoreCase(new StringBuilder(o)));
            out.add(Integer.signum(s.compareCodePoints(o)));
            out.add(s.equalsIgnoreAsciiCase(o));
            out.add(s.compareToIgnoreAsciiCase(o));
            out.add(s.commonPrefix(o));
            out.add(s.commonPrefix(o, true));
            out.add(s.commonSuffix(o));
            out.add(s.commonSuffix(new StringBuilder(o), true));
            out.add(s.canonicalEquals(o));
            out.add(String.getCommonPrefix(s, o));
        }
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
    void nullSafeStatics() {
        assertEquals(0, String.compare(null, null));
        assertTrue(String.compare(null, "a") < 0);
        assertTrue(String.compare(null, "a", false) > 0);
        assertEquals("abc".compareTo("ab"), String.compare("abc", "ab"));
        assertTrue(String.compareIgnoreCase("a", "B") < 0);
        assertTrue(String.equals(null, null, true));
        assertFalse(String.equals("a", null, false));
        assertTrue(String.equals("aB", "Ab", true));
        assertEquals("", String.getCommonPrefix((String[]) null));
        assertEquals("", String.getCommonPrefix("abc", null));
        assertEquals("abc", String.getCommonPrefix("abc"));
        assertEquals("ab", String.getCommonPrefix("abc", "abd", "ab"));
        String pair = "" + HIGH + (char) 0xDE00;
        assertEquals("", String.getCommonPrefix(pair, "" + HIGH + (char) 0xDE01),
                "never ends inside a shared surrogate pair");
    }

    @Test
    void instanceRules() {
        assertEquals("d", "abc".difference("abd"));
        assertEquals("", "abc".difference("abc"));
        assertEquals("c", "ab".difference("abc"));
        assertTrue("abc".equalsAny("x", null, "abc"));
        assertFalse("abc".equalsAny());
        assertTrue("ABC".equalsAnyIgnoreCase("abc"));
        String sharpS = String.valueOf((char) 0xDF);
        assertTrue(("Stra" + sharpS + "e").contentEqualsIgnoreCase(
                new StringBuilder("STRA" + sharpS + "E")));
        String supplementary = new String(Character.toChars(0x1F600));
        String privateUse = String.valueOf((char) 0xE000);
        assertTrue(supplementary.compareTo(privateUse) < 0, "UTF-16 order");
        assertTrue(supplementary.compareCodePoints(privateUse) > 0, "code point order");
        assertTrue("Ab".equalsIgnoreAsciiCase("aB"));
        assertFalse(String.valueOf((char) 0xE9).equalsIgnoreAsciiCase(String.valueOf((char) 0xC9)),
                "only ASCII letters fold");
        assertEquals("ab", "abc".commonPrefix("abd"));
        assertEquals("AB", "ABc".commonPrefix("abd", true));
        assertEquals("", ("" + HIGH + (char) 0xDE00).commonPrefix("" + HIGH + (char) 0xDE01),
                "never ends inside a surrogate pair");
        assertEquals("AB", "xAB".commonSuffix("yab", true));
        assertTrue(("e" + (char) 0x0301).canonicalEquals(String.valueOf((char) 0xE9)));
        assertFalse(String.valueOf((char) 0xFB01).canonicalEquals("fi"), "NFC, not NFKC");
        assertThrows(NullPointerException.class, () -> "a".difference(null));
        assertThrows(NullPointerException.class, () -> "a".equalsAny((CharSequence[]) null));
        assertThrows(NullPointerException.class, () -> "a".commonPrefix(null));
    }

    @Test
    void longTextsAcrossWindows() {
        String a = "q".repeat(600) + "X";
        String b = "Q".repeat(600) + "x";
        assertTrue(a.equalsIgnoreAsciiCase(b));
        assertEquals(0, a.compareToIgnoreAsciiCase(b));
        assertEquals(a, a.commonPrefix(b, true));
        assertEquals("q".repeat(600), a.commonPrefix("q".repeat(600) + "Y"));
        String tail = "x" + "q".repeat(520);
        assertEquals(tail, ("A" + tail).commonSuffix(tail));
    }
}
