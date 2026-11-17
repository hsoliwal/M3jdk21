/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String search family (containsAny ... containsAllWords)
 * @run junit StringSearchFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringSearchFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringSearchFamilyTest
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
 * Every search-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0 and
 * commons-text 1.15.0 on every text of up to four units over the alphabet below (donor-oracle proof
 * in the string-lane record).
 */
public class StringSearchFamilyTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE42;
    private static final char[] UNITS = {'a', 'A', 'b', ' ', (char) 0xE9, (char) 0xC9, HIGH, LOW};
    private static final String PAIR = "" + HIGH + LOW;

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

    /** The results of every method on one receiver, as one comparable list. */
    private static List<Object> answers(String s) {
        char[][] sets = {
            {}, {'a'}, {'A', ' '}, {HIGH}, {LOW}, {HIGH, LOW}, {HIGH, 'a'}, {(char) 0xE9}
        };
        String[] searches = {"", "a", "A", "ab", " ", PAIR, "" + HIGH, "É"};
        List<Object> out = new ArrayList<>();
        for (char[] set : sets) {
            String chars = new String(set);
            out.add(s.containsAny(set));
            out.add(s.containsAny((CharSequence) chars));
            out.add(s.containsNone(set));
            out.add(s.containsNone(chars));
            out.add(s.containsOnly(set));
            out.add(s.containsOnly(chars));
            out.add(s.indexOfAny(set));
            out.add(s.indexOfAny(chars));
            out.add(s.indexOfAnyBut(set));
            out.add(s.indexOfAnyBut((CharSequence) chars));
        }
        for (String search : searches) {
            out.add(s.containsIgnoreCase(search));
            out.add(s.indexOfIgnoreCase(search));
            out.add(s.lastIndexOfIgnoreCase(search));
            out.add(s.startsWithIgnoreCase(search));
            out.add(s.endsWithIgnoreCase(search));
            for (int number = -1; number <= 4; number++) {
                out.add(s.indexOfIgnoreCase(search, number));
                out.add(s.lastIndexOfIgnoreCase(search, number));
                out.add(s.ordinalIndexOf(search, number));
                out.add(s.lastOrdinalIndexOf(search, number));
            }
            CharSequence[] pair = {search, "b"};
            out.add(s.containsAny(pair));
            out.add(s.containsAnyIgnoreCase(pair));
            out.add(s.indexOfAny(pair));
            out.add(s.lastIndexOfAny(pair));
            out.add(s.startsWithAny(pair));
            out.add(s.endsWithAny(pair));
        }
        out.add(s.containsWhitespace());
        out.add(s.containsAllWords("a"));
        out.add(s.containsAllWords("a", "b"));
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
    void characterSetRules() {
        assertTrue("abc".containsAny('x', 'c'));
        assertFalse("".containsAny('a'));
        assertFalse("abc".containsAny(new char[0]));
        assertTrue(PAIR.containsAny(HIGH, LOW));
        assertFalse(("" + LOW).containsAny(HIGH, LOW),
                "the low half of a pair does not match alone");
        assertTrue(PAIR.containsAny(HIGH), "a high surrogate at the end of the set matches alone");
        assertFalse(("" + HIGH).containsAny(HIGH, 'a'), "a high surrogate needs its follower");
        assertTrue((HIGH + "a").containsAny(HIGH, 'a'));
        assertEquals(1, ("a" + PAIR).indexOfAny(HIGH, LOW));
        assertTrue("abc".containsNone("xyz"));
        assertTrue("".containsOnly());
        assertFalse("a".containsOnly());
        assertTrue("abba".containsOnly("ab"));
        assertEquals(0, PAIR.indexOfAnyBut(HIGH), "a pair is one code point, not its high half");
        assertEquals(-1, PAIR.indexOfAnyBut(HIGH, LOW));
        assertEquals(2, (PAIR + "a").indexOfAnyBut((CharSequence) PAIR));
        assertTrue("a\tb".containsWhitespace());
        assertFalse("ab".containsWhitespace());
    }

    @Test
    void longLatin1Scans() {
        String latin = "a".repeat(300) + (char) 0xE9;
        assertEquals(300, latin.indexOfAny((char) 0xE9, '#'));
        assertEquals(300, latin.indexOfAnyBut('a'));
        assertEquals(-1, latin.indexOfAnyBut('a', (char) 0xE9));
        assertFalse(("x".repeat(300) + (char) 0xA0).containsWhitespace(), "NBSP is not whitespace");
        assertTrue(("x".repeat(300) + (char) 0x1F).containsWhitespace());
    }

    @Test
    void ignoreCaseFirstUnits() {
        assertEquals(1, ("x" + (char) 0x017F + "t").indexOfIgnoreCase("ST"),
                "U+017F matches by its upper case");
        assertEquals(1, ("x" + (char) 0x212A).lastIndexOfIgnoreCase("k"),
                "U+212A matches by the lower case of its upper case");
        assertEquals(0, String.valueOf((char) 0x0131).indexOfIgnoreCase("I"));
        assertEquals(1, ("a" + (char) 0xB5).indexOfIgnoreCase(String.valueOf((char) 0x039C)),
                "a Latin-1 text and a UTF-16 search");
        assertEquals(2, ("ab" + (char) 0x0178).lastIndexOfIgnoreCase(String.valueOf((char) 0xFF)));
        assertEquals(1, ("a" + PAIR + "b").indexOfIgnoreCase(PAIR + "B"), "a pair as first unit");
        assertEquals(-1, ("x".repeat(200) + "ZQ").indexOfIgnoreCase("zqx"));
    }

    @Test
    void sequenceRules() {
        assertTrue("ab".containsAny("ba"), "a single sequence argument is a character set");
        assertFalse("ab".containsAny("ba", "ca"), "two or more are substrings");
        assertTrue("ab".containsAny(null, "b"), "null elements are skipped");
        assertEquals(0, "abc".indexOfAny("", "c"));
        assertEquals(3, "abc".lastIndexOfAny("", "c"));
        assertTrue("Hello".containsIgnoreCase("ELL"));
        assertEquals(1, "aBcB".indexOfIgnoreCase("b"));
        assertEquals(3, "aBcB".lastIndexOfIgnoreCase("b"));
        assertEquals(1, "a".indexOfIgnoreCase("", 1));
        assertEquals(-1, "a".indexOfIgnoreCase("", 2));
        assertEquals(1, "aaaa".ordinalIndexOf("aa", 2), "occurrences may overlap");
        assertEquals(1, "aaaa".lastOrdinalIndexOf("aa", 2));
        assertEquals(-1, "aaaa".ordinalIndexOf("aa", 0));
        assertEquals(4, "aaaa".lastOrdinalIndexOf("", 1));
        assertTrue("prefix".startsWithAny("x", "pre"));
        assertFalse("".startsWithAny(""));
        assertTrue("Prefix".startsWithIgnoreCase("pRE"));
        assertTrue("suffIX".endsWithIgnoreCase("Fix"));
        assertTrue("the quick fox".containsAllWords("fox", "the"));
        assertFalse("the quickfox".containsAllWords("fox"));
        assertFalse("a b".containsAllWords("a", " "));
    }

    @Test
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> "a".containsAny((char[]) null));
        assertThrows(NullPointerException.class, () -> "a".indexOfAny((CharSequence[]) null));
        assertThrows(NullPointerException.class, () -> "a".containsIgnoreCase(null));
        assertThrows(NullPointerException.class, () -> "a".ordinalIndexOf(null, 1));
        assertThrows(NullPointerException.class, () -> "a".containsAllWords("a", null));
    }
}
