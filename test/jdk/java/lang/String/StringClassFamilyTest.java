/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String class-test family (isAlpha ... isMixedCase)
 * @run junit StringClassFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringClassFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringClassFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every class-test method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0 on every
 * text of up to four units over the alphabet below, on every BMP unit in four contexts and on
 * every supplementary code point (donor-oracle proof in the string-lane record).
 */
public class StringClassFamilyTest {
    private static final char HIGH = (char) 0xD835;
    private static final char[] UNITS = {
        'a', 'A', '1', ' ', (char) 0xE9, (char) 0x0663, HIGH, (char) 0xDC00, '~', (char) 0x7F
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

    private static List<Boolean> answers(String s) {
        return List.of(s.isAlpha(), s.isAlphaSpace(), s.isAlphanumeric(), s.isAlphanumericSpace(),
                s.isAsciiPrintable(), s.isNumeric(), s.isNumericSpace(), s.isAllLowerCase(),
                s.isAllUpperCase(), s.isMixedCase());
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
        List<Boolean> expected = answers(new String(text.toCharArray()));
        for (Function<String, String> form : forms()) {
            assertEquals(expected, answers(form.apply(text)));
        }
    }

    @Test
    void emptyStringRules() {
        assertFalse("".isAlpha());
        assertTrue("".isAlphaSpace());
        assertFalse("".isAlphanumeric());
        assertTrue("".isAlphanumericSpace());
        assertTrue("".isAsciiPrintable());
        assertFalse("".isNumeric());
        assertTrue("".isNumericSpace());
        assertFalse("".isAllLowerCase());
        assertFalse("".isAllUpperCase());
        assertFalse("".isMixedCase());
    }

    @Test
    void codePointRules() {
        String boldA = new String(Character.toChars(0x1D400));
        String boldSmallA = new String(Character.toChars(0x1D41A));
        assertTrue(boldA.isAlpha(), "a supplementary letter is one letter");
        assertFalse(String.valueOf(HIGH).isAlpha(), "a lone surrogate is not a letter");
        assertTrue(new String(Character.toChars(0x1D7CE)).isNumeric(), "MATHEMATICAL BOLD ZERO");
        assertTrue((boldA + boldSmallA).isMixedCase());
        assertFalse(boldA.isMixedCase());
        assertTrue(("" + (char) 0x0663 + "7").isNumeric(), "any Unicode decimal digit");
        assertFalse("-1".isNumeric());
        assertFalse("1.5".isNumeric());
        assertTrue("a b".isAlphaSpace());
        assertFalse("a\tb".isAlphaSpace(), "only U+0020 counts as space");
        assertTrue(" ~".isAsciiPrintable());
        assertFalse(String.valueOf((char) 0x7F).isAsciiPrintable());
        assertFalse(String.valueOf((char) 0xE9).isAsciiPrintable());
        assertTrue(("" + (char) 0xE9 + (char) 0xDF).isAllLowerCase());
        assertTrue("a1B".isMixedCase(), "other characters do not count");
        assertFalse("a1b".isMixedCase());
    }

    @Test
    void longLatin1Texts() {
        String letters = "abcXYZ".repeat(100) + (char) 0xE9;
        assertTrue(letters.isAlpha());
        assertFalse((letters + "1").isAlpha());
        assertTrue((letters + "1").isAlphanumeric());
        assertTrue("0123456789".repeat(50).isNumeric());
        assertFalse("abc".repeat(100).isMixedCase());
        assertTrue(("abc".repeat(100) + (char) 0xC9).isMixedCase());
    }
}
