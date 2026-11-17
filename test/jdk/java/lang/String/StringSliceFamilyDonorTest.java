/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringSliceFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringSliceFamilyDonorTest {
    private static final String BAR = "bar";
    private static final String FOO = "foo";
    private static final String FOOBAR = "foobar";

    private static String asString(CharSequence text) {
        return text.toString();
    }

    /** Runs a donor assertion; a NullPointerException (null rejected) passes too. */
    private static void assertNpeOr(Runnable assertion) {
        try {
            assertion.run();
        } catch (NullPointerException rejected) {
            // null array rejected as documented
        }
    }

    @Test
    void stringUtilsSubstringTest_testLeft_String() {
        assertEquals("", asString("").left(-1));
        assertEquals("", asString("").left(0));
        assertEquals("", asString("").left(2));
        assertEquals("", asString(FOOBAR).left(-1));
        assertEquals("", asString(FOOBAR).left(0));
        assertEquals(FOO, asString(FOOBAR).left(3));
        assertSame(FOOBAR, asString(FOOBAR).left(80));
    }

    @Test
    void stringUtilsSubstringTest_testLeftSurrogatePair() {
        final String grin = "\ud83d\ude00";
        assertEquals("", asString(grin).left(0));
        assertEquals("", asString(grin).left(1));
        assertEquals(grin, asString(grin).left(2));
        assertEquals(grin, asString(grin).left(3));
        assertEquals("a", asString("a" + grin).left(1));
        assertEquals("a", asString("a" + grin).left(2));
        assertEquals("a" + grin, asString("a" + grin).left(3));
    }

    @Test
    void stringUtilsSubstringTest_testMid_String() {
        assertEquals("", asString("").mid(0, -1));
        assertEquals("", asString("").mid(0, 0));
        assertEquals("", asString("").mid(0, 2));
        assertEquals("", asString(FOOBAR).mid(3, -1));
        assertEquals("", asString(FOOBAR).mid(3, 0));
        assertEquals("b", asString(FOOBAR).mid(3, 1));
        assertEquals(FOO, asString(FOOBAR).mid(0, 3));
        assertEquals(BAR, asString(FOOBAR).mid(3, 3));
        assertEquals(FOOBAR, asString(FOOBAR).mid(0, 80));
        assertEquals(BAR, asString(FOOBAR).mid(3, 80));
        assertEquals("", asString(FOOBAR).mid(9, 3));
        assertEquals(FOO, asString(FOOBAR).mid(-1, 3));
        assertEquals(BAR, asString(FOOBAR).mid(3, Integer.MAX_VALUE));
        assertEquals(FOOBAR, asString(FOOBAR).mid(0, Integer.MAX_VALUE));
    }

    @Test
    void stringUtilsSubstringTest_testMidSurrogatePair() {
        final String grin = "\ud83d\ude00";
        assertEquals("", asString(grin).mid(0, 0));
        assertEquals("", asString(grin).mid(0, 1));
        assertEquals(grin, asString(grin).mid(0, 2));
        assertEquals(grin, asString(grin).mid(0, 3));
        assertEquals("", asString(grin).mid(1, 1));
        assertEquals("", asString(grin).mid(1, 2));
        assertEquals("a", asString("a" + grin + "b").mid(0, 2));
        assertEquals(grin, asString("a" + grin + "b").mid(1, 2));
        assertEquals("b", asString("a" + grin + "b").mid(2, 2));
    }

    @Test
    void stringUtilsSubstringTest_testRight_String() {
        assertEquals("", asString("").right(-1));
        assertEquals("", asString("").right(0));
        assertEquals("", asString("").right(2));
        assertEquals("", asString(FOOBAR).right(-1));
        assertEquals("", asString(FOOBAR).right(0));
        assertEquals(BAR, asString(FOOBAR).right(3));
        assertSame(FOOBAR, asString(FOOBAR).right(80));
    }

    @Test
    void stringUtilsSubstringTest_testRightSurrogatePair() {
        final String grin = "\ud83d\ude00";
        assertEquals("", asString(grin).right(0));
        assertEquals("", asString(grin).right(1));
        assertEquals(grin, asString(grin).right(2));
        assertEquals(grin, asString(grin).right(3));
        assertEquals("a", asString(grin + "a").right(1));
        assertEquals("a", asString(grin + "a").right(2));
        assertEquals(grin + "a", asString(grin + "a").right(3));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringAfter_StringInt() {
        assertEquals("", asString("").substringAfter(0));
        assertEquals("", asString("").substringAfter('X'));
        assertEquals("", asString("foo").substringAfter(0));
        assertEquals("ot", asString("foot").substringAfter('o'));
        assertEquals("bc", asString("abc").substringAfter('a'));
        assertEquals("cba", asString("abcba").substringAfter('b'));
        assertEquals("", asString("abc").substringAfter('c'));
        assertEquals("", asString("abc").substringAfter('d'));
        final int grin = 0x1F600;
        final String s = new String(Character.toChars(grin));
        assertEquals("world", asString("hello" + s + "world").substringAfter(grin));
        assertEquals("", asString("hello" + s).substringAfter(grin));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringAfter_StringString() {
        assertEquals("barXXbaz", asString("fooXXbarXXbaz").substringAfter("XX"));
        assertThrows(NullPointerException.class, () -> asString("").substringAfter(null));
        assertEquals("", asString("").substringAfter(""));
        assertEquals("", asString("").substringAfter("XX"));
        assertThrows(NullPointerException.class, () -> asString("foo").substringAfter(null));
        assertEquals("ot", asString("foot").substringAfter("o"));
        assertEquals("bc", asString("abc").substringAfter("a"));
        assertEquals("cba", asString("abcba").substringAfter("b"));
        assertEquals("", asString("abc").substringAfter("c"));
        assertEquals("abc", asString("abc").substringAfter(""));
        assertEquals("", asString("abc").substringAfter("d"));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringAfterLast_StringInt() {
        assertEquals("", asString("").substringAfterLast(0));
        assertEquals("", asString("").substringAfterLast('a'));
        assertEquals("", asString("foo").substringAfterLast(0));
        assertEquals("", asString("foo").substringAfterLast('b'));
        assertEquals("t", asString("foot").substringAfterLast('o'));
        assertEquals("bc", asString("abc").substringAfterLast('a'));
        assertEquals("a", asString("abcba").substringAfterLast('b'));
        assertEquals("", asString("abc").substringAfterLast('c'));
        assertEquals("", asString("").substringAfterLast('d'));
        final int grin = 0x1F600;
        final String s = new String(Character.toChars(grin));
        assertEquals("", asString("hello" + s).substringAfterLast(grin));
        assertEquals("x", asString(s + "a" + s + "x").substringAfterLast(grin));
        assertEquals("world", asString("a" + s + "world").substringAfterLast(grin));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringAfterLast_StringString() {
        assertEquals("baz", asString("fooXXbarXXbaz").substringAfterLast("XX"));
        assertThrows(NullPointerException.class, () -> asString("").substringAfterLast(null));
        assertEquals("", asString("").substringAfterLast(""));
        assertEquals("", asString("").substringAfterLast("a"));
        assertThrows(NullPointerException.class, () -> asString("foo").substringAfterLast(null));
        assertEquals("", asString("foo").substringAfterLast("b"));
        assertEquals("t", asString("foot").substringAfterLast("o"));
        assertEquals("bc", asString("abc").substringAfterLast("a"));
        assertEquals("a", asString("abcba").substringAfterLast("b"));
        assertEquals("", asString("abc").substringAfterLast("c"));
        assertEquals("", asString("").substringAfterLast("d"));
        assertEquals("", asString("abc").substringAfterLast(""));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringBefore_StringInt() {
        assertEquals("foo", asString("fooXXbarXXbaz").substringBefore('X'));
        assertEquals("", asString("").substringBefore(0));
        assertEquals("", asString("").substringBefore('X'));
        assertEquals("foo", asString("foo").substringBefore(0));
        assertEquals("foo", asString("foo").substringBefore('b'));
        assertEquals("f", asString("foot").substringBefore('o'));
        assertEquals("", asString("abc").substringBefore('a'));
        assertEquals("a", asString("abcba").substringBefore('b'));
        assertEquals("ab", asString("abc").substringBefore('c'));
        assertEquals("abc", asString("abc").substringBefore(0));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringBefore_StringString() {
        assertEquals("foo", asString("fooXXbarXXbaz").substringBefore("XX"));
        assertThrows(NullPointerException.class, () -> asString("").substringBefore(null));
        assertEquals("", asString("").substringBefore(""));
        assertEquals("", asString("").substringBefore("XX"));
        assertThrows(NullPointerException.class, () -> asString("foo").substringBefore(null));
        assertEquals("foo", asString("foo").substringBefore("b"));
        assertEquals("f", asString("foot").substringBefore("o"));
        assertEquals("", asString("abc").substringBefore("a"));
        assertEquals("a", asString("abcba").substringBefore("b"));
        assertEquals("ab", asString("abc").substringBefore("c"));
        assertEquals("", asString("abc").substringBefore(""));
        assertEquals("abc", asString("abc").substringBefore("X"));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringBeforeLast_StringString() {
        assertEquals("fooXXbar", asString("fooXXbarXXbaz").substringBeforeLast("XX"));
        assertThrows(NullPointerException.class, () -> asString("").substringBeforeLast(null));
        assertEquals("", asString("").substringBeforeLast(""));
        assertEquals("", asString("").substringBeforeLast("XX"));
        assertThrows(NullPointerException.class, () -> asString("foo").substringBeforeLast(null));
        assertEquals("foo", asString("foo").substringBeforeLast("b"));
        assertEquals("fo", asString("foo").substringBeforeLast("o"));
        assertEquals("abc\r\n", asString("abc\r\n").substringBeforeLast("d"));
        assertEquals("abc", asString("abcdabc").substringBeforeLast("d"));
        assertEquals("abcdabc", asString("abcdabcd").substringBeforeLast("d"));
        assertEquals("a", asString("abc").substringBeforeLast("b"));
        assertEquals("abc ", asString("abc \n").substringBeforeLast("\n"));
        assertThrows(NullPointerException.class, () -> asString("a").substringBeforeLast(null));
        assertEquals("a", asString("a").substringBeforeLast(""));
        assertEquals("", asString("a").substringBeforeLast("a"));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringBetween_StringString() {
        assertEquals("", asString("").substringBetween(""));
        assertNull(asString("").substringBetween("abc"));
        assertEquals("", asString("    ").substringBetween(" "));
        assertThrows(NullPointerException.class, () -> asString("abc").substringBetween(null));
        assertEquals("", asString("abc").substringBetween(""));
        assertNull(asString("abc").substringBetween("a"));
        assertEquals("bc", asString("abca").substringBetween("a"));
        assertEquals("bc", asString("abcabca").substringBetween("a"));
        assertEquals("bar", asString("\nbar\n").substringBetween("\n"));
    }

    @Test
    void stringUtilsSubstringTest_testSubstringBetween_StringStringString() {
        assertThrows(NullPointerException.class, () -> asString("").substringBetween(null, ""));
        assertThrows(NullPointerException.class, () -> asString("").substringBetween("", null));
        assertEquals("", asString("").substringBetween("", ""));
        assertEquals("", asString("foo").substringBetween("", ""));
        assertNull(asString("foo").substringBetween("", "]"));
        assertNull(asString("foo").substringBetween("[", "]"));
        assertEquals("", asString("    ").substringBetween(" ", "  "));
        assertEquals("bar", asString("<foo>bar</foo>").substringBetween("<foo>", "</foo>"));
        assertEquals("abc", asString("yabczyabcz").substringBetween("y", "z"));
    }

    @Test
    void stringUtilsTest_testRemoveEnd() {
        assertThrows(NullPointerException.class, () -> asString("").removeEnd(null));
        assertEquals(asString("").removeEnd(""), "");
        assertEquals(asString("").removeEnd("a"), "");
        assertEquals(asString("www.domain.com.").removeEnd(".com"), "www.domain.com.");
        assertEquals(asString("www.domain.com").removeEnd(".com"), "www.domain");
        assertEquals(asString("www.domain").removeEnd(".com"), "www.domain");
        assertEquals(asString("domain.com").removeEnd(""), "domain.com");
        assertThrows(NullPointerException.class, () -> asString("domain.com").removeEnd(null));
    }

    @Test
    void stringUtilsTest_testRemoveEndIgnoreCase() {
        assertThrows(NullPointerException.class, () -> asString("").removeEndIgnoreCase(null));
        assertEquals(asString("").removeEndIgnoreCase(""), "", "removeEndIgnoreCase(\"\", \"\")");
        assertEquals(asString("").removeEndIgnoreCase("a"), "", "removeEndIgnoreCase(\"\", \"a\")");
        assertEquals(asString("www.domain.com.").removeEndIgnoreCase(".com"), "www.domain.com.",
                "removeEndIgnoreCase(\"www.domain.com.\", \".com\")");
        assertEquals(asString("www.domain.com").removeEndIgnoreCase(".com"), "www.domain",
                "removeEndIgnoreCase(\"www.domain.com\", \".com\")");
        assertEquals(asString("www.domain").removeEndIgnoreCase(".com"), "www.domain",
                "removeEndIgnoreCase(\"www.domain\", \".com\")");
        assertEquals(asString("domain.com").removeEndIgnoreCase(""), "domain.com",
                "removeEndIgnoreCase(\"domain.com\", \"\")");
        assertThrows(NullPointerException.class, () -> asString("domain.com").removeEndIgnoreCase(
                null));
        assertEquals(asString("www.domain.com").removeEndIgnoreCase(".COM"), "www.domain",
                "removeEndIgnoreCase(\"www.domain.com\", \".COM\")");
        assertEquals(asString("www.domain.COM").removeEndIgnoreCase(".com"), "www.domain",
                "removeEndIgnoreCase(\"www.domain.COM\", \".com\")");
    }

    @Test
    void stringUtilsTest_testRemoveStartChar() {
        assertEquals(asString("").removeStart('\0'), "");
        assertEquals(asString("").removeStart('a'), "");
        assertEquals(asString("/path").removeStart('/'), "path");
        assertEquals(asString("path").removeStart('/'), "path");
        assertEquals(asString("path").removeStart('\0'), "path");
    }

    @Test
    void stringUtilsTest_testRemoveStartIgnoreCase() {
        assertThrows(NullPointerException.class, () -> asString("").removeStartIgnoreCase(null));
        assertEquals(asString("").removeStartIgnoreCase(""), "",
                "removeStartIgnoreCase(\"\", \"\")");
        assertEquals(asString("").removeStartIgnoreCase("a"), "",
                "removeStartIgnoreCase(\"\", \"a\")");
        assertEquals(asString("www.domain.com").removeStartIgnoreCase("www."), "domain.com",
                "removeStartIgnoreCase(\"www.domain.com\", \"www.\")");
        assertEquals(asString("domain.com").removeStartIgnoreCase("www."), "domain.com",
                "removeStartIgnoreCase(\"domain.com\", \"www.\")");
        assertEquals(asString("domain.com").removeStartIgnoreCase(""), "domain.com",
                "removeStartIgnoreCase(\"domain.com\", \"\")");
        assertThrows(NullPointerException.class, () -> asString("domain.com").removeStartIgnoreCase(
                null));
        assertEquals(asString("www.domain.com").removeStartIgnoreCase("WWW."), "domain.com",
                "removeStartIgnoreCase(\"www.domain.com\", \"WWW.\")");
    }

    @Test
    void stringUtilsTest_testRemoveStartString() {
        assertThrows(NullPointerException.class, () -> asString("").removeStart(null));
        assertEquals(asString("").removeStart(""), "");
        assertEquals(asString("").removeStart("a"), "");
        assertEquals(asString("www.domain.com").removeStart("www."), "domain.com");
        assertEquals(asString("domain.com").removeStart("www."), "domain.com");
        assertEquals(asString("domain.com").removeStart(""), "domain.com");
        assertThrows(NullPointerException.class, () -> asString("domain.com").removeStart(null));
    }

    @Test
    void stringUtilsTest_testTruncate_StringInt() {
        assertEquals("", asString("").truncate(10));
        assertEquals("abc", asString("abcdefghij").truncate(3));
        assertEquals("abcdef", asString("abcdefghij").truncate(6));
        assertEquals("", asString("abcdefghij").truncate(0));
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-1),
                "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-100),
                "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(
                Integer.MIN_VALUE), "maxWidth cannot be negative");
        assertEquals("abcdefghij", asString("abcdefghijklmno").truncate(10));
        assertEquals("abcdefghijklmno", asString("abcdefghijklmno").truncate(Integer.MAX_VALUE));
        assertEquals("abcde", asString("abcdefghijklmno").truncate(5));
        assertEquals("abc", asString("abcdefghijklmno").truncate(3));
    }

    @Test
    void stringUtilsTest_testTruncate_StringIntInt() {
        assertEquals("", asString("").truncate(0, 10));
        assertEquals("", asString("").truncate(2, 10));
        assertEquals("abc", asString("abcdefghij").truncate(0, 3));
        assertEquals("fghij", asString("abcdefghij").truncate(5, 6));
        assertEquals("", asString("abcdefghij").truncate(0, 0));
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(0, -1),
                "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(0, -10),
                "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(0, -100),
                "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(1, -100),
                "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(0,
                Integer.MIN_VALUE), "maxWidth cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-1, 0),
                "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-10, 0),
                "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-100, 1),
                "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(
                Integer.MIN_VALUE, 0), "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-1, -1),
                "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-10,
                -10), "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(-100,
                -100), "offset cannot be negative");
        assertThrows(IllegalArgumentException.class, () -> asString("abcdefghij").truncate(
                Integer.MIN_VALUE, Integer.MIN_VALUE), "offset cannot be negative");
        final String raspberry = "raspberry peach";
        assertEquals("peach", asString(raspberry).truncate(10, 15));
        assertEquals("abcdefghij", asString("abcdefghijklmno").truncate(0, 10));
        assertEquals("abcdefghijklmno", asString("abcdefghijklmno").truncate(0, Integer.MAX_VALUE));
        assertEquals("bcdefghijk", asString("abcdefghijklmno").truncate(1, 10));
        assertEquals("cdefghijkl", asString("abcdefghijklmno").truncate(2, 10));
        assertEquals("defghijklm", asString("abcdefghijklmno").truncate(3, 10));
        assertEquals("efghijklmn", asString("abcdefghijklmno").truncate(4, 10));
        assertEquals("fghijklmno", asString("abcdefghijklmno").truncate(5, 10));
        assertEquals("fghij", asString("abcdefghijklmno").truncate(5, 5));
        assertEquals("fgh", asString("abcdefghijklmno").truncate(5, 3));
        assertEquals("klm", asString("abcdefghijklmno").truncate(10, 3));
        assertEquals("klmno", asString("abcdefghijklmno").truncate(10, Integer.MAX_VALUE));
        assertEquals("n", asString("abcdefghijklmno").truncate(13, 1));
        assertEquals("no", asString("abcdefghijklmno").truncate(13, Integer.MAX_VALUE));
        assertEquals("o", asString("abcdefghijklmno").truncate(14, 1));
        assertEquals("o", asString("abcdefghijklmno").truncate(14, Integer.MAX_VALUE));
        assertEquals("", asString("abcdefghijklmno").truncate(15, 1));
        assertEquals("", asString("abcdefghijklmno").truncate(15, Integer.MAX_VALUE));
        assertEquals("", asString("abcdefghijklmno").truncate(Integer.MAX_VALUE,
                Integer.MAX_VALUE));
    }

    @Test
    void stringUtilsTest_testTruncate_StringIntInt_surrogatePair() {
        final String grin = "\ud83d\ude00";
        assertEquals("a", asString("a" + grin + "b").truncate(0, 2));
        assertEquals(grin, asString("a" + grin + "b").truncate(1, 2));
        assertEquals("ab", asString("ab" + grin).truncate(0, 3));
        assertEquals("a", asString(grin + "ab").truncate(1, 2));
        assertEquals(grin, asString(grin).truncate(0, 2));
        assertEquals(grin, asString(grin).truncate(0, 3));
        assertEquals("", asString(grin).truncate(0, 0));
        assertEquals("", asString(grin).truncate(0, 1));
        assertEquals("", asString(grin).truncate(1, 1));
        assertEquals("", asString(grin).truncate(1, 2));
        assertEquals("", asString(grin).truncate(2, 2));
    }

    @Test
    void stringUtilsTest_testUnwrap_StringChar() {
        assertThrows(NullPointerException.class, () -> asString("abc").unwrap(null));
        assertEquals("a", asString("a").unwrap("a"));
        assertEquals("", asString("aa").unwrap("a"));
        assertEquals("abc", asString("\'abc\'").unwrap('\''));
        assertEquals("abc", asString("AabcA").unwrap('A'));
        assertEquals("AabcA", asString("AAabcAA").unwrap('A'));
        assertEquals("abc", asString("abc").unwrap('b'));
        assertEquals("#A", asString("#A").unwrap('#'));
        assertEquals("A#", asString("A#").unwrap('#'));
        assertEquals("ABA", asString("AABAA").unwrap('A'));
    }

    @Test
    void stringUtilsTest_testUnwrap_StringString() {
        assertThrows(NullPointerException.class, () -> asString("abc").unwrap(null));
        assertEquals("abc", asString("abc").unwrap(""));
        assertEquals("a", asString("a").unwrap("a"));
        assertEquals("ababa", asString("ababa").unwrap("aba"));
        assertEquals("", asString("aa").unwrap("a"));
        assertEquals("abc", asString("\'abc\'").unwrap("\'"));
        assertEquals("abc", asString("\"abc\"").unwrap("\""));
        assertEquals("abc\"xyz", asString("\"abc\"xyz\"").unwrap("\""));
        assertEquals("abc\"xyz\"", asString("\"abc\"xyz\"\"").unwrap("\""));
        assertEquals("abc\'xyz\'", asString("\"abc\'xyz\'\"").unwrap("\""));
        assertEquals("\"abc\'xyz\'\"", asString("AA\"abc\'xyz\'\"AA").unwrap("AA"));
        assertEquals("\"abc\'xyz\'\"", asString("123\"abc\'xyz\'\"123").unwrap("123"));
        assertEquals("AA\"abc\'xyz\'\"", asString("AA\"abc\'xyz\'\"").unwrap("AA"));
        assertEquals("AA\"abc\'xyz\'\"AA", asString("AAA\"abc\'xyz\'\"AAA").unwrap("A"));
        assertEquals("\"abc\'xyz\'\"AA", asString("\"abc\'xyz\'\"AA").unwrap("AA"));
    }
}
