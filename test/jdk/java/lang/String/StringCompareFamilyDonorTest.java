/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringCompareFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringCompareFamilyDonorTest {
    private static final String BAR = "bar";
    private static final String FOO = "foo";
    private static final String FOOBAR = "foobar";

    private static String s(CharSequence text) {
        return text.toString();
    }

    private static final class CustomCharSequence implements CharSequence {
            private final CharSequence seq;

            CustomCharSequence(final CharSequence seq) {
                this.seq = seq;
            }

            @Override
            public char charAt(final int index) {
                return seq.charAt(index);
            }

            @Override
            public boolean equals(final Object obj) {
                if (!(obj instanceof CustomCharSequence)) {
                    return false;
                }
                final CustomCharSequence other = (CustomCharSequence) obj;
                return seq.equals(other.seq);
            }

            @Override
            public int hashCode() {
                return seq.hashCode();
            }

            @Override
            public int length() {
                return seq.length();
            }

            @Override
            public CharSequence subSequence(final int start, final int end) {
                return new CustomCharSequence(seq.subSequence(start, end));
            }

            @Override
            public String toString() {
                return seq.toString();
            }
        }

    @Test
    void stringUtilsEqualsIndexOfTest_testCompare_StringString() {
        assertEquals(0, String.compare(null, null));
        assertTrue(String.compare(null, "a") < 0);
        assertTrue(String.compare("a", null) > 0);
        assertEquals(0, String.compare("abc", "abc"));
        assertTrue(String.compare("a", "b") < 0);
        assertTrue(String.compare("b", "a") > 0);
        assertTrue(String.compare("a", "B") > 0);
        assertTrue(String.compare("abc", "abd") < 0);
        assertTrue(String.compare("ab", "abc") < 0);
        assertTrue(String.compare("ab", "ab ") < 0);
        assertTrue(String.compare("abc", "ab ") > 0);
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testCompare_StringStringBoolean() {
        assertEquals(0, String.compare(null, null, false));
        assertTrue(String.compare(null, "a", true) < 0);
        assertTrue(String.compare(null, "a", false) > 0);
        assertTrue(String.compare("a", null, true) > 0);
        assertTrue(String.compare("a", null, false) < 0);
        assertEquals(0, String.compare("abc", "abc", false));
        assertTrue(String.compare("a", "b", false) < 0);
        assertTrue(String.compare("b", "a", false) > 0);
        assertTrue(String.compare("a", "B", false) > 0);
        assertTrue(String.compare("abc", "abd", false) < 0);
        assertTrue(String.compare("ab", "abc", false) < 0);
        assertTrue(String.compare("ab", "ab ", false) < 0);
        assertTrue(String.compare("abc", "ab ", false) > 0);
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testCompareIgnoreCase_StringString() {
        assertEquals(0, String.compareIgnoreCase(null, null));
        assertTrue(String.compareIgnoreCase(null, "a") < 0);
        assertTrue(String.compareIgnoreCase("a", null) > 0);
        assertEquals(0, String.compareIgnoreCase("abc", "abc"));
        assertEquals(0, String.compareIgnoreCase("abc", "ABC"));
        assertTrue(String.compareIgnoreCase("a", "b") < 0);
        assertTrue(String.compareIgnoreCase("b", "a") > 0);
        assertTrue(String.compareIgnoreCase("a", "B") < 0);
        assertTrue(String.compareIgnoreCase("A", "b") < 0);
        assertTrue(String.compareIgnoreCase("abc", "ABD") < 0);
        assertTrue(String.compareIgnoreCase("ab", "ABC") < 0);
        assertTrue(String.compareIgnoreCase("ab", "AB ") < 0);
        assertTrue(String.compareIgnoreCase("abc", "AB ") > 0);
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testCompareIgnoreCase_StringStringBoolean() {
        assertEquals(0, String.compareIgnoreCase(null, null, false));
        assertTrue(String.compareIgnoreCase(null, "a", true) < 0);
        assertTrue(String.compareIgnoreCase(null, "a", false) > 0);
        assertTrue(String.compareIgnoreCase("a", null, true) > 0);
        assertTrue(String.compareIgnoreCase("a", null, false) < 0);
        assertEquals(0, String.compareIgnoreCase("abc", "abc", false));
        assertEquals(0, String.compareIgnoreCase("abc", "ABC", false));
        assertTrue(String.compareIgnoreCase("a", "b", false) < 0);
        assertTrue(String.compareIgnoreCase("b", "a", false) > 0);
        assertTrue(String.compareIgnoreCase("a", "B", false) < 0);
        assertTrue(String.compareIgnoreCase("A", "b", false) < 0);
        assertTrue(String.compareIgnoreCase("abc", "ABD", false) < 0);
        assertTrue(String.compareIgnoreCase("ab", "ABC", false) < 0);
        assertTrue(String.compareIgnoreCase("ab", "AB ", false) < 0);
        assertTrue(String.compareIgnoreCase("abc", "AB ", false) > 0);
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testEqualsAny() {
        assertFalse(s(FOO).equalsAny());
        assertFalse(s(FOO).equalsAny(new String[]{}));
        assertTrue(s(FOO).equalsAny(FOO));
        assertTrue(s(FOO).equalsAny(BAR, new String(new char[] { 'f', 'o', 'o' })));
        assertFalse(s(FOO).equalsAny(BAR, new String(new char[] { 'f', 'O', 'O' })));
        assertFalse(s(FOO).equalsAny(BAR));
        assertFalse(s(FOO).equalsAny(BAR, null));
        assertFalse(s(FOO).equalsAny(FOOBAR));
        assertFalse(s(FOOBAR).equalsAny(FOO));
        assertFalse(s(FOO).equalsAny(null, BAR));
        assertTrue(s(FOO).equalsAny(BAR, null, "", FOO, BAR));
        assertFalse(s(FOO).equalsAny(FOO.toUpperCase(Locale.ROOT)));
        assertTrue(s(FOO).equalsAny(new CustomCharSequence("foo")));
        assertTrue(s(FOO).equalsAny(new StringBuilder("foo")));
        assertFalse(s(FOO).equalsAny(new CustomCharSequence("fOo")));
        assertFalse(s(FOO).equalsAny(new StringBuilder("fOo")));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testEqualsAnyIgnoreCase() {
        assertFalse(s(FOO).equalsAnyIgnoreCase());
        assertFalse(s(FOO).equalsAnyIgnoreCase(new String[]{}));
        assertTrue(s(FOO).equalsAnyIgnoreCase(FOO));
        assertTrue(s(FOO).equalsAnyIgnoreCase(FOO.toUpperCase(Locale.ROOT)));
        assertTrue(s(FOO).equalsAnyIgnoreCase(FOO, new String(new char[]{'f', 'o', 'o'})));
        assertTrue(s(FOO).equalsAnyIgnoreCase(BAR, new String(new char[]{'f', 'O', 'O'})));
        assertFalse(s(FOO).equalsAnyIgnoreCase(BAR));
        assertFalse(s(FOO).equalsAnyIgnoreCase(BAR, null));
        assertFalse(s(FOO).equalsAnyIgnoreCase(FOOBAR));
        assertFalse(s(FOOBAR).equalsAnyIgnoreCase(FOO));
        assertFalse(s(FOO).equalsAnyIgnoreCase(null, BAR));
        assertTrue(s(FOO).equalsAnyIgnoreCase(BAR, null, "", FOO.toUpperCase(Locale.ROOT), BAR));
        assertTrue(s(FOO).equalsAnyIgnoreCase(FOO.toUpperCase(Locale.ROOT)));
        assertTrue(s(FOO).equalsAnyIgnoreCase(new CustomCharSequence("fOo")));
        assertTrue(s(FOO).equalsAnyIgnoreCase(new StringBuilder("fOo")));
    }

    @Test
    void stringUtilsTest_testDifference_StringString() {
        assertEquals("", s("").difference(""));
        assertEquals("abc", s("").difference("abc"));
        assertEquals("", s("abc").difference(""));
        assertThrows(NullPointerException.class, () -> s("i am a machine").difference(null));
        assertEquals("robot", s("i am a machine").difference("i am a robot"));
        assertEquals("", s("abc").difference("abc"));
        assertEquals("you are a robot", s("i am a robot").difference("you are a robot"));
        final String cp10400 = new String(Character.toChars(0x10400));
        final String cp10401 = new String(Character.toChars(0x10401));
        assertEquals(cp10401, s(cp10400).difference(cp10401));
        assertEquals("Y", s(cp10400 + "X").difference(cp10400 + "Y"));
    }

    @Test
    void stringUtilsTest_testGetCommonPrefix_StringArray() {
        assertEquals("", String.getCommonPrefix((String[]) null));
        assertEquals("", String.getCommonPrefix());
        assertEquals("abc", String.getCommonPrefix("abc"));
        assertEquals("", String.getCommonPrefix(null, null));
        assertEquals("", String.getCommonPrefix("", ""));
        assertEquals("", String.getCommonPrefix("", null));
        assertEquals("", String.getCommonPrefix("abc", null, null));
        assertEquals("", String.getCommonPrefix(null, null, "abc"));
        assertEquals("", String.getCommonPrefix("", "abc"));
        assertEquals("", String.getCommonPrefix("abc", ""));
        assertEquals("abc", String.getCommonPrefix("abc", "abc"));
        assertEquals("a", String.getCommonPrefix("abc", "a"));
        assertEquals("ab", String.getCommonPrefix("ab", "abxyz"));
        assertEquals("ab", String.getCommonPrefix("abcde", "abxyz"));
        assertEquals("", String.getCommonPrefix("abcde", "xyz"));
        assertEquals("", String.getCommonPrefix("xyz", "abcde"));
        assertEquals("i am a ", String.getCommonPrefix("i am a machine", "i am a robot"));
        final String cp10400 = new String(Character.toChars(0x10400));
        final String cp10401 = new String(Character.toChars(0x10401));
        assertEquals("", String.getCommonPrefix(cp10400, cp10401));
        assertEquals(cp10400, String.getCommonPrefix(cp10400 + "X", cp10400 + "Y"));
    }
}
