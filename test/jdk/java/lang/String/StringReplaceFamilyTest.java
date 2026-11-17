/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String replace family (replace ... translate)
 * @run junit StringReplaceFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringReplaceFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringReplaceFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every replace-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0, the
 * Kotlin standard library and CPython 3.12 on every text of up to four units over the alphabet
 * below (donor-oracle proof in the string-lane record).
 */
public class StringReplaceFamilyTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE00;
    private static final String PAIR = "" + HIGH + LOW;
    private static final char[] UNITS = {'a', 'b', 'A', ',', HIGH, LOW};
    private static final String[] SEARCHES = {"", "a", ",", "b,", PAIR, "" + LOW};
    private static final Map<Integer, String> TABLE = new HashMap<>();

    static {
        TABLE.put((int) 'a', "xy");
        TABLE.put((int) 'b', null);
        TABLE.put(0x1F600, "P");
        TABLE.put((int) LOW, "l");
    }

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
        for (String search : SEARCHES) {
            for (int max = -1; max <= 1; max++) {
                out.add(s.replace(search, "Q", max));
                out.add(s.replaceIgnoreCase(search, "", max));
            }
            out.add(s.replaceOnce(search, "aa"));
            out.add(s.replaceIgnoreCase(search, "q"));
            out.add(s.replaceOnceIgnoreCase(search, "A"));
            out.add(s.remove(search));
            out.add(s.removeIgnoreCase(search));
            out.add(s.replaceBefore(search, "<"));
            out.add(s.replaceAfter(search, ">", "#"));
            out.add(s.replaceBeforeLast(search, ""));
            out.add(s.replaceAfterLast(search, "Z"));
        }
        out.add(s.remove('a'));
        out.add(s.remove(HIGH));
        out.add(s.replaceEach(new String[] {"a", "b", null}, new String[] {"b", "a", "x"}));
        out.add(s.replaceEach(new String[] {"ab", "a"}, new String[] {"", "AA"}));
        out.add(s.replaceEachRepeatedly(new String[] {"a", ","}, new String[] {"A", ""}));
        out.add(s.replaceChars("ab,", "ba"));
        out.add(s.replaceChars("" + HIGH, "h"));
        out.add(s.replaceFirstChar(Character::toUpperCase));
        out.add(s.translate(TABLE));
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
    void rules() {
        assertEquals("abc", "abc".replace("", "x", -1), "an empty search occurs nowhere");
        assertEquals("xaxbxcx", "abc".replace("", "x"), "unlike the JDK two-argument replace");
        assertEquals("xxa", "aaa".replace("a", "x", 2));
        assertEquals("xaa", "aaa".replaceOnce("a", "x"));
        assertEquals("aaa", "aaa".replace("a", "x", 0));
        assertEquals("x-x", "A-a".replaceIgnoreCase("a", "x"));
        assertEquals("x-a", "A-a".replaceOnceIgnoreCase("A", "x"));
        assertEquals("-", "A-a".removeIgnoreCase("a"));
        assertEquals("qd", "queued".remove("ue"));
        assertEquals("qeed", "queued".remove('u'));
        assertEquals("bcc", "abc".replaceEach(new String[] {"a", "b"}, new String[] {"b", "c"}),
                "one pass: a replacement is not searched again");
        assertEquals("ccc", "abc".replaceEachRepeatedly(new String[] {"a", "b"},
                new String[] {"b", "c"}));
        assertEquals("abc", "abc".replaceEach(new String[] {null, ""}, new String[] {"x", "y"}),
                "null and empty searches are left out");
        assertThrows(IllegalArgumentException.class,
                () -> "abc".replaceEach(new String[] {"a"}, new String[] {"b", "c"}));
        assertEquals("", "".replaceEach(new String[] {"a"}, new String[] {"b", "c"}),
                "an empty string comes before the length check");
        assertThrows(IllegalStateException.class,
                () -> "a".replaceEachRepeatedly(new String[] {"a"}, new String[] {"a"}),
                "a text replaced by itself never settles");
        assertEquals("jelly", "hello".replaceChars("ho", "jy"));
        assertEquals("ayya", "abcba".replaceChars("bc", "y"), "removed past the replacement");
        assertEquals("Abc", "abc".replaceFirstChar(Character::toUpperCase));
        assertEquals("<:b", "a:b".replaceBefore(":", "<"));
        assertEquals("a:>", "a:b".replaceAfter(":", ">"));
        assertEquals("#", "ab".replaceAfter(":", ">", "#"));
        assertEquals("aX", "ab".replaceAfterLast("", "X"),
                "an empty delimiter is found at the last character");
        assertEquals("", "".replaceBeforeLast("", "X"), "and not at all in an empty string");
        assertEquals("xyP", ("ab" + PAIR).translate(TABLE));
        assertEquals("l" + HIGH, ("" + LOW + HIGH).translate(TABLE), "lone surrogates map alone");
    }

    @Test
    void unchangedGivesThis() {
        String text = new String("a,b".toCharArray());
        assertSame(text, text.replace("a", "a", -1));
        assertSame(text, text.replaceIgnoreCase("A", "a"));
        assertSame(text, text.remove("z"));
        assertSame(text, text.remove('z'));
        String ab = new String("ab".toCharArray());
        assertSame(ab, ab.replaceEach(new String[] {"a", "b"}, new String[] {"", "ab"}),
                "replaced, yet equal to this string");
        assertSame(text, text.replaceChars("ab", "ab"));
        assertSame(text, text.replaceFirstChar(c -> c));
        assertSame(text, text.replaceBefore(",", "a"));
        assertSame(text, text.translate(Map.of((int) 'a', "a")));
    }

    @Test
    void nullArguments() {
        assertThrows(NullPointerException.class, () -> "".replace(null, "x", -1));
        assertThrows(NullPointerException.class, () -> "".replaceIgnoreCase("a", null));
        assertThrows(NullPointerException.class, () -> "".remove((String) null));
        assertThrows(NullPointerException.class, () -> "".replaceEach(null, new String[0]));
        assertThrows(NullPointerException.class, () -> "a".replaceChars("a", null));
        assertThrows(NullPointerException.class, () -> "a".replaceFirstChar(null));
        assertThrows(NullPointerException.class, () -> "a".replaceBefore("a", null));
        assertThrows(NullPointerException.class, () -> "a".translate(null));
    }

    @Test
    void longTextsAcrossWindows() {
        for (int k = 505; k <= 520; k++) {
            String text = "x".repeat(k) + PAIR + "Ab";
            assertEquals("x".repeat(k) + "PA", text.translate(TABLE));
            assertEquals("x".repeat(k) + PAIR + "Qb", text.replaceIgnoreCase("a", "Q"));
            assertEquals("x".repeat(k) + PAIR + "Ba", text.replaceChars("Ab", "Ba"));
        }
    }
}
