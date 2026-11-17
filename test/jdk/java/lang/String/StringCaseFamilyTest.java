/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String case family (capitalize ... equalsSimpleFold)
 * @run junit StringCaseFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringCaseFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringCaseFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * Every case-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0,
 * commons-text 1.15.0, CPython 3.12 str.casefold and Go strings.EqualFold on every code point and
 * on every text of up to four units over a case alphabet (donor-oracle proof in the string-lane
 * record).
 */
public class StringCaseFamilyTest {
    private static final char HIGH = (char) 0xD801;
    private static final char LOW = (char) 0xDC00;
    private static final char[] UNITS = {'a', 'A', ' ', '-', (char) 0x01C5, (char) 0x00DF, HIGH,
        LOW};
    private static final char[][] DELIMITERS = {{}, {'-'}, {HIGH, LOW}};
    private static final String[] OTHERS = {"", "A", "ss", "" + HIGH + (char) 0xDC28,
        "" + (char) 0x212A};

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
        out.add(s.capitalize());
        out.add(s.uncapitalize());
        out.add(s.swapCase());
        out.add(s.swapCaseWords());
        out.add(s.capitalizeWords());
        out.add(s.capitalizeFully());
        out.add(s.uncapitalizeWords());
        out.add(s.initials());
        out.add(s.toCamelCase(true));
        out.add(s.toCamelCase(false));
        out.add(s.caseFold());
        for (char[] d : DELIMITERS) {
            out.add(s.capitalizeWords(d));
            out.add(s.capitalizeFully(d));
            out.add(s.uncapitalizeWords(d));
            out.add(s.initials(d));
            out.add(s.toCamelCase(false, d));
        }
        for (String other : OTHERS) {
            out.add(s.equalsFoldCase(other));
            out.add(s.equalsSimpleFold(other));
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
    void capitalizeRules() {
        assertEquals("Hello world", "hello world".capitalize());
        assertEquals("\u01C5a", "\u01C6a".capitalize(), "title case, not upper case");
        assertEquals("" + HIGH + LOW + "b", ("" + HIGH + (char) 0xDC28 + "b").capitalize(),
                "a surrogate pair is one character");
        String already = "Abc";
        assertSame(already, already.capitalize());
        assertEquals("hello", "Hello".uncapitalize());
        assertEquals("", "".uncapitalize());
    }

    @Test
    void swapRules() {
        assertEquals("hELLO wORLD", "Hello World".swapCase());
        assertEquals("\u00DF", "\u00DF".swapCase(), "one code point each, no expansion");
        assertEquals("\u01C6", "\u01C5".swapCase(), "title case becomes lower case");
        assertEquals("A \u01C4", "a \u01C6".swapCase());
        assertEquals("A \u01C5", "a \u01C6".swapCaseWords(), "word starts become title case");
        assertEquals("1A", "1a".swapCaseWords(), "only after whitespace or at the start");
    }

    @Test
    void wordRules() {
        assertEquals("Hello WORLD", "hello wORLD".capitalizeWords());
        assertEquals("Hello World", "hello wORLD".capitalizeFully());
        assertEquals("A-b C", "a-b c".capitalizeWords(), "no argument: whitespace");
        assertEquals("A-B c", "a-b c".capitalizeWords('-'));
        assertEquals("A-b c", "a-b c".capitalizeWords(new char[0]), "empty: no delimiter");
        assertEquals("hello world", "Hello World".uncapitalizeWords());
        assertEquals("BJL", "Ben John Lee".initials());
        assertEquals("BJ", "Ben-John".initials('-'));
        assertEquals("", "Ben John".initials(new char[0]));
        assertEquals("toCamelCase", "to-CAMEL case".toCamelCase(false, '-'));
        assertEquals("ToCamelCase", "to-CAMEL case".toCamelCase(true, '-'));
        assertEquals("aB", "  a b".toCamelCase(false), "the space always separates");
        assertThrows(NullPointerException.class, () -> "a".capitalizeWords((char[]) null));
        assertThrows(NullPointerException.class, () -> "a".capitalizeFully((char[]) null));
        assertThrows(NullPointerException.class, () -> "a".uncapitalizeWords((char[]) null));
        assertThrows(NullPointerException.class, () -> "".initials((char[]) null));
        assertThrows(NullPointerException.class, () -> "".toCamelCase(true, (char[]) null));
    }

    @Test
    void foldRules() {
        assertEquals("strasse", "Stra\u00DFe".caseFold());
        assertEquals("i\u0307", "\u0130".caseFold(), "no Turkic folds");
        assertEquals("k", "\u212A".caseFold());
        assertEquals("\u03C3\u03C3", "\u03A3\u03C2".caseFold());
        assertEquals("" + HIGH + (char) 0xDC28, ("" + HIGH + LOW).caseFold());
        assertEquals("" + HIGH, ("" + HIGH).caseFold(), "a lone surrogate is unchanged");
        String folded = "abc";
        assertSame(folded, folded.caseFold());
        assertTrue("STRASSE".equalsFoldCase("stra\u00DFe"));
        assertFalse("STRASSE".equalsSimpleFold("stra\u00DFe"), "simple folding keeps the length");
        assertTrue("k".equalsSimpleFold("\u212A"));
        assertTrue("\u212A".equalsSimpleFold("K"));
        assertTrue("\u017F".equalsSimpleFold("S"));
        assertTrue("\u00DF".equalsSimpleFold("\u1E9E"));
        assertTrue("\u03C2".equalsSimpleFold("\u03A3"));
        assertFalse("\u0130".equalsSimpleFold("i"));
        assertFalse("\u0130".equalsFoldCase("i"));
        assertFalse(("" + HIGH).equalsSimpleFold("" + LOW));
        assertThrows(NullPointerException.class, () -> "a".equalsFoldCase(null));
        assertThrows(NullPointerException.class, () -> "a".equalsSimpleFold(null));
    }

    @Test
    void longTextsAcrossWindows() {
        String pair = "" + HIGH + LOW;
        String lower = "" + HIGH + (char) 0xDC28;
        String text = "a".repeat(511) + pair;
        assertEquals("a".repeat(511) + lower, text.caseFold());
        assertEquals("A".repeat(511) + lower, text.swapCase());
        assertTrue(("x".repeat(511) + pair + "k").equalsSimpleFold("X".repeat(511) + lower
                + (char) 0x212A));
        assertTrue(("x".repeat(1023) + "\u00DF").equalsFoldCase("X".repeat(1023) + "SS"));
        assertEquals("a", (" ".repeat(600) + "ab").initials());
        assertEquals("A" + "b".repeat(600) + " C",
                ("a" + "B".repeat(600) + " c").capitalizeFully());
    }
}
