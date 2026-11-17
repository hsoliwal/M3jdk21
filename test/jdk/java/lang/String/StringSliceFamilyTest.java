/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String slice family (left ... unwrap)
 * @run junit StringSliceFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringSliceFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringSliceFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every slice-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0 and the
 * Kotlin standard library on every text of up to four units over the alphabet below (donor-oracle
 * proof in the string-lane record).
 */
public class StringSliceFamilyTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE00;
    private static final char[] UNITS = {'a', 'b', 'A', ',', HIGH, LOW};
    private static final String[] SEPARATORS = {"", "a", ",", "b,", "" + HIGH + LOW};

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
        for (int n = -1; n <= 4; n++) {
            out.add(s.left(n));
            out.add(s.right(n));
            out.add(s.mid(n, 2));
            if (n >= 0) {
                out.add(s.truncate(n));
                out.add(s.truncate(1, n));
                out.add(s.drop(n));
                out.add(s.dropLast(n));
            }
        }
        out.add(s.takeWhile(Character::isLetter));
        out.add(s.dropWhile(c -> c == 'a'));
        out.add(s.takeLastWhile(c -> Character.isLowSurrogate((char) c)));
        out.add(s.dropLastWhile(Character::isLetter));
        for (String sep : SEPARATORS) {
            out.add(s.substringBefore(sep));
            out.add(s.substringAfter(sep));
            out.add(s.substringBeforeLast(sep));
            out.add(s.substringAfterLast(sep));
            out.add(s.substringBefore(sep, "#"));
            out.add(s.substringAfterLast(sep, "#"));
            out.add(String.valueOf(s.substringBetween(sep)));
            out.add(String.valueOf(s.substringBetween(sep, ",")));
            out.add(s.removeStart(sep));
            out.add(s.removeEnd(sep));
            out.add(s.removeStartIgnoreCase(sep));
            out.add(s.removeEndIgnoreCase(sep));
            out.add(s.removeSurrounding(sep));
            out.add(s.unwrap(sep));
        }
        out.add(s.substringBefore(0x1F600));
        out.add(s.substringAfter(','));
        out.add(s.substringAfterLast('a'));
        out.add(s.unwrap('a'));
        out.add(s.removeStart('a'));
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
        String pair = "" + HIGH + LOW;
        assertEquals("a", ("a" + pair).left(2), "never splits a surrogate pair");
        assertEquals(pair, ("a" + pair).right(2));
        assertEquals("", "abc".left(-1));
        assertEquals("abc", "abc".mid(-5, 9), "a negative position counts as 0");
        assertEquals("b", "abc".mid(1, 1));
        assertEquals("c", "abc".substringAfter("b"));
        assertEquals("", "abc".substringAfter("z"), "absent: empty");
        assertEquals("abc", "abc".substringAfter("z", "abc"), "absent: the given answer");
        assertEquals("", "abc".substringBefore(""));
        assertEquals("abc", "abc".substringBeforeLast(""));
        assertEquals("ab", "abc".substringBeforeLast("", "#"),
                "Kotlin: searched from the last index");
        assertEquals("b", "[b]".substringBetween("[", "]"));
        assertNull("[b".substringBetween("[", "]"));
        assertEquals("bc", "abc".removeStart("a"));
        assertEquals("abc", "abc".removeEnd(""));
        assertEquals("b", "<b>".removeSurrounding("<", ">"));
        assertEquals("", "aa".removeSurrounding("a"));
        assertEquals("a", "a".removeSurrounding("a"), "needs room for both");
        assertEquals("b", "'b'".unwrap('\''));
        assertEquals("bc", "abc".drop(1));
        assertThrows(IllegalArgumentException.class, () -> "abc".drop(-1));
        assertThrows(IllegalArgumentException.class, () -> "abc".truncate(-1));
        assertThrows(NullPointerException.class, () -> "".removeEnd(null));
        assertThrows(NullPointerException.class, () -> "".substringAfter(null));
    }

    @Test
    void longTextsAcrossWindows() {
        String text = "a".repeat(600) + "b";
        assertEquals("a".repeat(600), text.takeWhile(c -> c == 'a'));
        assertEquals("b", text.dropWhile(c -> c == 'a'));
        assertEquals("", text.takeLastWhile(c -> c == 'a'));
        assertEquals(text, text.dropLastWhile(c -> c == 'a'));
    }
}
