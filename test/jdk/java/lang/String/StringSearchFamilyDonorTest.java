/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringSearchFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringSearchFamilyDonorTest {
    private static final String BAR = "bar";
    private static final String FOO = "foo";
    private static final String FOOBAR_EQUALSINDEXOF = "foobar";
    private static final String FOOBAR_STARTSENDSWITH = "FOOBAR";
    private static final String foobar = "foobar";
    private static final String[] FOOBAR_SUB_ARRAY = {"ob", "ba"};
    private static final String CharU20000 = "\uD840\uDC00";
    private static final String CharU20001 = "\uD840\uDC01";
    private static final String CharU24000 = "\uD850\uDC00";
    private static final String CharUSuppCharHigh = "\uD840";
    private static final String CharUSuppCharLow = "\uDC00";

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
    void stringUtilsContainsTest_testContainsAny_StringCharArray() {
        assertNpeOr(() -> assertFalse(asString("").containsAny((char[]) null)));
        assertFalse(asString("").containsAny(new char[0]));
        assertFalse(asString("").containsAny('a', 'b'));
        assertNpeOr(() -> assertFalse(asString("zzabyycdxx").containsAny((char[]) null)));
        assertFalse(asString("zzabyycdxx").containsAny(new char[0]));
        assertTrue(asString("zzabyycdxx").containsAny('z', 'a'));
        assertTrue(asString("zzabyycdxx").containsAny('b', 'y'));
        assertTrue(asString("zzabyycdxx").containsAny('z', 'y'));
        assertFalse(asString("ab").containsAny('z'));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringCharArrayWithBadSupplementaryChars() {
        assertFalse(asString(CharUSuppCharLow).containsAny(CharU20001.toCharArray()));
        assertFalse(asString("abc" + CharUSuppCharLow + "xyz").containsAny(CharU20001.toCharArray(
                )));
        assertFalse(asString(CharUSuppCharHigh).containsAny(CharU20001.toCharArray()));
        assertFalse(asString(CharU20001).containsAny(CharUSuppCharLow.toCharArray()));
        assertTrue(asString(CharU20001).containsAny(CharUSuppCharHigh.toCharArray()));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringCharArrayWithSharedLowSurrogate() {
        assertFalse(asString(CharU20000).containsAny(CharU24000.toCharArray()));
        assertFalse(asString(CharU24000).containsAny(CharU20000.toCharArray()));
        assertFalse(asString("abc" + CharU20000 + "xyz").containsAny(CharU24000.toCharArray()));
        assertTrue(asString("abc" + CharU24000 + "xyz").containsAny(CharU24000.toCharArray()));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringCharArrayWithSupplementaryChars() {
        assertTrue(asString(CharU20000 + CharU20001).containsAny(CharU20000.toCharArray()));
        assertTrue(asString("a" + CharU20000 + CharU20001).containsAny("a".toCharArray()));
        assertTrue(asString(CharU20000 + "a" + CharU20001).containsAny("a".toCharArray()));
        assertTrue(asString(CharU20000 + CharU20001 + "a").containsAny("a".toCharArray()));
        assertTrue(asString(CharU20000 + CharU20001).containsAny(CharU20001.toCharArray()));
        assertTrue(asString(CharU20000).containsAny(CharU20000.toCharArray()));
        assertFalse(asString(CharU20000).containsAny(CharU20001.toCharArray()));
        assertFalse(asString(CharU20001).containsAny(CharU20000.toCharArray()));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringString() {
        assertNpeOr(() -> assertFalse(asString("").containsAny((String) null)));
        assertFalse(asString("").containsAny(""));
        assertFalse(asString("").containsAny("ab"));
        assertNpeOr(() -> assertFalse(asString("zzabyycdxx").containsAny((String) null)));
        assertFalse(asString("zzabyycdxx").containsAny(""));
        assertTrue(asString("zzabyycdxx").containsAny("za"));
        assertTrue(asString("zzabyycdxx").containsAny("by"));
        assertTrue(asString("zzabyycdxx").containsAny("zy"));
        assertFalse(asString("ab").containsAny("z"));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringStringArray() {
        assertNpeOr(() -> assertFalse(asString("").containsAny((String[]) null)));
        assertFalse(asString("").containsAny(new String[0]));
        assertFalse(asString("").containsAny(new String[] { "hello" }));
        assertNpeOr(() -> assertFalse(asString("hello, goodbye").containsAny((String[]) null)));
        assertFalse(asString("hello, goodbye").containsAny(new String[0]));
        assertTrue(asString("hello, goodbye").containsAny(new String[] { "hello", "goodbye" }));
        assertTrue(asString("hello, goodbye").containsAny(new String[] { "hello", "Goodbye" }));
        assertFalse(asString("hello, goodbye").containsAny(new String[] { "Hello", "Goodbye" }));
        assertFalse(asString("hello, goodbye").containsAny(new String[] { "Hello", null }));
        assertFalse(asString("hello, null").containsAny(new String[] { "Hello", null }));
        assertTrue(asString("abcd").containsAny("ab", null));
        assertTrue(asString("abcd").containsAny("ab", "cd"));
        assertTrue(asString("abc").containsAny("d", "abc"));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringWithBadSupplementaryChars() {
        assertFalse(asString(CharUSuppCharLow).containsAny(CharU20001));
        assertFalse(asString(CharUSuppCharHigh).containsAny(CharU20001));
        assertFalse(asString(CharU20001).containsAny(CharUSuppCharLow));
        assertTrue(asString(CharU20001).containsAny(CharUSuppCharHigh));
    }

    @Test
    void stringUtilsContainsTest_testContainsAny_StringWithSupplementaryChars() {
        assertTrue(asString(CharU20000 + CharU20001).containsAny(CharU20000));
        assertTrue(asString(CharU20000 + CharU20001).containsAny(CharU20001));
        assertTrue(asString(CharU20000).containsAny(CharU20000));
        assertFalse(asString(CharU20000).containsAny(CharU20001));
        assertFalse(asString(CharU20001).containsAny(CharU20000));
    }

    @Test
    void stringUtilsContainsTest_testContainsAnyIgnoreCase_StringStringArray() {
        assertNpeOr(() -> assertFalse(asString("").containsAnyIgnoreCase((String[]) null)));
        assertFalse(asString("").containsAnyIgnoreCase(new String[0]));
        assertFalse(asString("").containsAnyIgnoreCase(new String[] { "hello" }));
        assertNpeOr(() -> assertFalse(asString("hello, goodbye").containsAnyIgnoreCase((
                String[]) null)));
        assertFalse(asString("hello, goodbye").containsAnyIgnoreCase(new String[0]));
        assertTrue(asString("hello, goodbye").containsAnyIgnoreCase(new String[] { "hello",
                "goodbye" }));
        assertTrue(asString("hello, goodbye").containsAnyIgnoreCase(new String[] { "hello",
                "Goodbye" }));
        assertTrue(asString("hello, goodbye").containsAnyIgnoreCase(new String[] { "Hello",
                "Goodbye" }));
        assertTrue(asString("hello, goodbye").containsAnyIgnoreCase(new String[] { "Hello",
                null }));
        assertTrue(asString("hello, null").containsAnyIgnoreCase(new String[] { "Hello", null }));
        assertTrue(asString("abcd").containsAnyIgnoreCase("ab", null));
        assertTrue(asString("abcd").containsAnyIgnoreCase("ab", "cd"));
        assertTrue(asString("abc").containsAnyIgnoreCase("d", "abc"));
    }

    @Test
    void stringUtilsContainsTest_testContainsIgnoreCase_StringString() {
        assertThrows(NullPointerException.class, () -> asString("").containsIgnoreCase(null));
        assertThrows(NullPointerException.class, () -> asString("a").containsIgnoreCase(null));
        assertThrows(NullPointerException.class, () -> asString("abc").containsIgnoreCase(null));
        assertTrue(asString("").containsIgnoreCase(""));
        assertTrue(asString("a").containsIgnoreCase(""));
        assertTrue(asString("abc").containsIgnoreCase(""));
        assertFalse(asString("").containsIgnoreCase("a"));
        assertTrue(asString("a").containsIgnoreCase("a"));
        assertTrue(asString("abc").containsIgnoreCase("a"));
        assertFalse(asString("").containsIgnoreCase("A"));
        assertTrue(asString("a").containsIgnoreCase("A"));
        assertTrue(asString("abc").containsIgnoreCase("A"));
        assertFalse(asString("").containsIgnoreCase("abc"));
        assertFalse(asString("a").containsIgnoreCase("abc"));
        assertTrue(asString("xabcz").containsIgnoreCase("abc"));
        assertFalse(asString("").containsIgnoreCase("ABC"));
        assertFalse(asString("a").containsIgnoreCase("ABC"));
        assertTrue(asString("xabcz").containsIgnoreCase("ABC"));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_CharArray() {
        assertThrows(NullPointerException.class, () -> asString("").containsNone((char[]) null));
        final char[] emptyChars = {};
        final String str1 = "a";
        assertTrue(asString(str1).containsNone(emptyChars));
        assertTrue(asString("").containsNone(emptyChars));
        final char[] chars1 = { 'b' };
        assertTrue(asString("").containsNone(chars1));
        assertTrue(asString(str1).containsNone(chars1));
        final char[] chars2 = { '.' };
        assertTrue(asString(str1).containsNone(chars2));
        final char[] chars3 = { 'c', 'd' };
        assertTrue(asString(str1).containsNone(chars3));
        final String str2 = "b";
        assertFalse(asString(str2).containsNone(chars1));
        assertTrue(asString(str2).containsNone(chars2));
        assertTrue(asString(str2).containsNone(chars3));
        final String str3 = "ab.";
        assertFalse(asString(str3).containsNone(chars1));
        assertFalse(asString(str3).containsNone(chars2));
        assertTrue(asString(str3).containsNone(chars3));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_CharArrayWithBadSupplementaryChars() {
        assertTrue(asString(CharUSuppCharLow).containsNone(CharU20001.toCharArray()));
        assertTrue(asString(CharUSuppCharHigh).containsNone(CharU20001.toCharArray()));
        assertTrue(asString(CharU20001).containsNone(CharUSuppCharLow.toCharArray()));
        assertFalse(asString(CharU20001).containsNone(CharUSuppCharHigh.toCharArray()));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_CharArrayWithSharedLowSurrogate() {
        assertTrue(asString(CharU20000).containsNone(CharU24000.toCharArray()));
        assertTrue(asString(CharU24000).containsNone(CharU20000.toCharArray()));
        assertFalse(asString("abc" + CharU24000).containsNone(CharU24000.toCharArray()));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_CharArrayWithSupplementaryChars() {
        assertFalse(asString(CharU20000 + CharU20001).containsNone(CharU20000.toCharArray()));
        assertFalse(asString(CharU20000 + CharU20001).containsNone(CharU20001.toCharArray()));
        assertFalse(asString(CharU20000).containsNone(CharU20000.toCharArray()));
        assertTrue(asString(CharU20000).containsNone(CharU20001.toCharArray()));
        assertTrue(asString(CharU20001).containsNone(CharU20000.toCharArray()));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_String() {
        assertThrows(NullPointerException.class, () -> asString("").containsNone((String) null));
        final String str1 = "a";
        assertTrue(asString(str1).containsNone(""));
        assertTrue(asString("").containsNone(""));
        final String chars1 = "b";
        assertTrue(asString("").containsNone(chars1));
        assertTrue(asString(str1).containsNone(chars1));
        final String chars2 = ".";
        assertTrue(asString(str1).containsNone(chars2));
        final String chars3 = "cd";
        assertTrue(asString(str1).containsNone(chars3));
        final String str2 = "b";
        assertFalse(asString(str2).containsNone(chars1));
        assertTrue(asString(str2).containsNone(chars2));
        assertTrue(asString(str2).containsNone(chars3));
        final String str3 = "ab.";
        assertFalse(asString(str3).containsNone(chars1));
        assertFalse(asString(str3).containsNone(chars2));
        assertTrue(asString(str3).containsNone(chars3));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_StringWithBadSupplementaryChars() {
        assertTrue(asString(CharUSuppCharLow).containsNone(CharU20001));
        assertTrue(asString(CharUSuppCharHigh).containsNone(CharU20001));
        assertTrue(asString(CharU20001).containsNone(CharUSuppCharLow));
        assertFalse(asString(CharU20001).containsNone(CharUSuppCharHigh));
    }

    @Test
    void stringUtilsContainsTest_testContainsNone_StringWithSupplementaryChars() {
        assertFalse(asString(CharU20000 + CharU20001).containsNone(CharU20000));
        assertFalse(asString(CharU20000 + CharU20001).containsNone(CharU20001));
        assertFalse(asString(CharU20000).containsNone(CharU20000));
        assertTrue(asString(CharU20000).containsNone(CharU20001));
        assertTrue(asString(CharU20001).containsNone(CharU20000));
    }

    @Test
    void stringUtilsContainsTest_testContainsOnly_CharArray() {
        assertThrows(NullPointerException.class, () -> asString("").containsOnly((char[]) null));
        final char[] emptyChars = {};
        final String str1 = "a";
        assertFalse(asString(str1).containsOnly(emptyChars));
        assertTrue(asString("").containsOnly(emptyChars));
        final char[] chars1 = { 'b' };
        assertTrue(asString("").containsOnly(chars1));
        assertFalse(asString(str1).containsOnly(chars1));
        final char[] chars2 = { 'a' };
        assertTrue(asString(str1).containsOnly(chars2));
        final char[] chars3 = { 'a', 'b' };
        assertTrue(asString(str1).containsOnly(chars3));
        final String str2 = "b";
        assertTrue(asString(str2).containsOnly(chars1));
        assertFalse(asString(str2).containsOnly(chars2));
        assertTrue(asString(str2).containsOnly(chars3));
        final String str3 = "ab";
        assertFalse(asString(str3).containsOnly(chars1));
        assertFalse(asString(str3).containsOnly(chars2));
        assertTrue(asString(str3).containsOnly(chars3));
    }

    @Test
    void stringUtilsContainsTest_testContainsOnly_String() {
        assertThrows(NullPointerException.class, () -> asString("").containsOnly((String) null));
        final String str1 = "a";
        assertFalse(asString(str1).containsOnly(""));
        assertTrue(asString("").containsOnly(""));
        final String chars1 = "b";
        assertTrue(asString("").containsOnly(chars1));
        assertFalse(asString(str1).containsOnly(chars1));
        final String chars2 = "a";
        assertTrue(asString(str1).containsOnly(chars2));
        final String chars3 = "ab";
        assertTrue(asString(str1).containsOnly(chars3));
        final String str2 = "b";
        assertTrue(asString(str2).containsOnly(chars1));
        assertFalse(asString(str2).containsOnly(chars2));
        assertTrue(asString(str2).containsOnly(chars3));
        final String str3 = "ab";
        assertFalse(asString(str3).containsOnly(chars1));
        assertFalse(asString(str3).containsOnly(chars2));
        assertTrue(asString(str3).containsOnly(chars3));
    }

    @Test
    void stringUtilsContainsTest_testContainsWhitespace() {
        assertFalse(asString("").containsWhitespace());
        assertTrue(asString(" ").containsWhitespace());
        assertFalse(asString("a").containsWhitespace());
        assertTrue(asString("a ").containsWhitespace());
        assertTrue(asString(" a").containsWhitespace());
        assertTrue(asString("a\t").containsWhitespace());
        assertTrue(asString("\n").containsWhitespace());
    }






    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAny_StringCharArray() {
        assertNpeOr(() -> assertEquals(-1, asString("").indexOfAny((char[]) null)));
        assertEquals(-1, asString("").indexOfAny(new char[0]));
        assertEquals(-1, asString("").indexOfAny('a', 'b'));
        assertNpeOr(() -> assertEquals(-1, asString("zzabyycdxx").indexOfAny((char[]) null)));
        assertEquals(-1, asString("zzabyycdxx").indexOfAny(new char[0]));
        assertEquals(0, asString("zzabyycdxx").indexOfAny('z', 'a'));
        assertEquals(3, asString("zzabyycdxx").indexOfAny('b', 'y'));
        assertEquals(-1, asString("ab").indexOfAny('z'));
        assertEquals(0, asString("abcd").indexOfAny('a', 'b', 'c', 'd'));
        assertEquals(0, asString("bcda").indexOfAny('a', 'b', 'c', 'd'));
        assertEquals(0, asString("cbda").indexOfAny('a', 'b', 'c', 'd'));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAny_StringCharArrayWithSharedLowSurrogate() {
        assertEquals(-1, asString(CharU20000).indexOfAny(CharU24000.toCharArray()));
        assertEquals(-1, asString("abc" + CharU20000).indexOfAny(CharU24000.toCharArray()));
        assertEquals(3, asString("abc" + CharU24000).indexOfAny(CharU24000.toCharArray()));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAny_StringCharArrayWithSupplementaryChars() {
        assertEquals(0, asString(CharU20000 + CharU20001).indexOfAny(CharU20000.toCharArray()));
        assertEquals(2, asString(CharU20000 + CharU20001).indexOfAny(CharU20001.toCharArray()));
        assertEquals(0, asString(CharU20000).indexOfAny(CharU20000.toCharArray()));
        assertEquals(-1, asString(CharU20000).indexOfAny(CharU20001.toCharArray()));
        assertEquals(-1, asString(CharUSuppCharHigh).indexOfAny(CharU20001.toCharArray()));
        assertFalse(asString(CharUSuppCharHigh).containsAny(CharU20001.toCharArray()));
        assertEquals(-1, asString("abc" + CharUSuppCharHigh).indexOfAny(CharU20000.toCharArray()));
    }


    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAny_StringString() {
        assertNpeOr(() -> assertEquals(-1, asString("").indexOfAny((String) null)));
        assertEquals(-1, asString("").indexOfAny(""));
        assertEquals(-1, asString("").indexOfAny("ab"));
        assertNpeOr(() -> assertEquals(-1, asString("zzabyycdxx").indexOfAny((String) null)));
        assertEquals(-1, asString("zzabyycdxx").indexOfAny(""));
        assertEquals(0, asString("zzabyycdxx").indexOfAny("za"));
        assertEquals(3, asString("zzabyycdxx").indexOfAny("by"));
        assertEquals(-1, asString("ab").indexOfAny("z"));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAny_StringStringArray() {
        assertNpeOr(() -> assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).indexOfAny((
                String[]) null)));
        assertEquals(2, asString(FOOBAR_EQUALSINDEXOF).indexOfAny(FOOBAR_SUB_ARRAY));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).indexOfAny(new String[0]));
        assertEquals(-1, asString("").indexOfAny(new String[0]));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).indexOfAny(new String[] {"llll"}));
        assertEquals(0, asString(FOOBAR_EQUALSINDEXOF).indexOfAny(new String[] {""}));
        assertEquals(0, asString("").indexOfAny(new String[] {""}));
        assertEquals(-1, asString("").indexOfAny(new String[] {"a"}));
        assertEquals(-1, asString("").indexOfAny(new String[] {null}));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).indexOfAny(new String[] {null}));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAny_StringStringWithSupplementaryChars() {
        assertEquals(0, asString(CharU20000 + CharU20001).indexOfAny(CharU20000));
        assertEquals(2, asString(CharU20000 + CharU20001).indexOfAny(CharU20001));
        assertEquals(0, asString(CharU20000).indexOfAny(CharU20000));
        assertEquals(-1, asString(CharU20000).indexOfAny(CharU20001));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAnyBut_StringCharArray() {
        assertThrows(NullPointerException.class, () -> asString("").indexOfAnyBut((char[]) null));
        assertEquals(-1, asString("").indexOfAnyBut());
        assertEquals(-1, asString("").indexOfAnyBut('a', 'b'));
        assertThrows(NullPointerException.class, () -> asString("zzabyycdxx").indexOfAnyBut((
                char[]) null));
        assertEquals(-1, asString("zzabyycdxx").indexOfAnyBut());
        assertEquals(3, asString("zzabyycdxx").indexOfAnyBut('z', 'a'));
        assertEquals(0, asString("zzabyycdxx").indexOfAnyBut('b', 'y'));
        assertEquals(-1, asString("aba").indexOfAnyBut('a', 'b'));
        assertEquals(0, asString("aba").indexOfAnyBut('z'));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfAnyBut_StringString() {
        assertThrows(NullPointerException.class, () -> asString("").indexOfAnyBut((String) null));
        assertEquals(-1, asString("").indexOfAnyBut(""));
        assertEquals(-1, asString("").indexOfAnyBut("ab"));
        assertThrows(NullPointerException.class, () -> asString("zzabyycdxx").indexOfAnyBut((
                String) null));
        assertEquals(-1, asString("zzabyycdxx").indexOfAnyBut(""));
        assertEquals(3, asString("zzabyycdxx").indexOfAnyBut("za"));
        assertEquals(0, asString("zzabyycdxx").indexOfAnyBut("by"));
        assertEquals(0, asString("ab").indexOfAnyBut("z"));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfIgnoreCase_String() {
        assertThrows(NullPointerException.class, () -> asString("").indexOfIgnoreCase(null));
        assertEquals(0, asString("").indexOfIgnoreCase(""));
        assertEquals(0, asString("aabaabaa").indexOfIgnoreCase("a"));
        assertEquals(0, asString("aabaabaa").indexOfIgnoreCase("A"));
        assertEquals(2, asString("aabaabaa").indexOfIgnoreCase("b"));
        assertEquals(2, asString("aabaabaa").indexOfIgnoreCase("B"));
        assertEquals(1, asString("aabaabaa").indexOfIgnoreCase("ab"));
        assertEquals(1, asString("aabaabaa").indexOfIgnoreCase("AB"));
        assertEquals(0, asString("aabaabaa").indexOfIgnoreCase(""));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testIndexOfIgnoreCase_StringInt() {
        assertEquals(1, asString("aabaabaa").indexOfIgnoreCase("AB", -1));
        assertEquals(1, asString("aabaabaa").indexOfIgnoreCase("AB", 0));
        assertEquals(1, asString("aabaabaa").indexOfIgnoreCase("AB", 1));
        assertEquals(4, asString("aabaabaa").indexOfIgnoreCase("AB", 2));
        assertEquals(4, asString("aabaabaa").indexOfIgnoreCase("AB", 3));
        assertEquals(4, asString("aabaabaa").indexOfIgnoreCase("AB", 4));
        assertEquals(-1, asString("aabaabaa").indexOfIgnoreCase("AB", 5));
        assertEquals(-1, asString("aabaabaa").indexOfIgnoreCase("AB", 6));
        assertEquals(-1, asString("aabaabaa").indexOfIgnoreCase("AB", 7));
        assertEquals(-1, asString("aabaabaa").indexOfIgnoreCase("AB", 8));
        assertEquals(1, asString("aab").indexOfIgnoreCase("AB", 1));
        assertEquals(5, asString("aabaabaa").indexOfIgnoreCase("", 5));
        assertEquals(-1, asString("ab").indexOfIgnoreCase("AAB", 0));
        assertEquals(-1, asString("aab").indexOfIgnoreCase("AAB", 1));
        assertEquals(-1, asString("abc").indexOfIgnoreCase("", 9));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLANG1193() {
        assertEquals(0, asString("abc").ordinalIndexOf("ab", 1));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLANG1241_1() {
        assertEquals(0, asString("abaabaab").ordinalIndexOf("ab", 1));
        assertEquals(3, asString("abaabaab").ordinalIndexOf("ab", 2));
        assertEquals(6, asString("abaabaab").ordinalIndexOf("ab", 3));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLANG1241_2() {
        assertEquals(0, asString("abababa").ordinalIndexOf("aba", 1));
        assertEquals(2, asString("abababa").ordinalIndexOf("aba", 2));
        assertEquals(4, asString("abababa").ordinalIndexOf("aba", 3));
        assertEquals(0, asString("abababab").ordinalIndexOf("abab", 1));
        assertEquals(2, asString("abababab").ordinalIndexOf("abab", 2));
        assertEquals(4, asString("abababab").ordinalIndexOf("abab", 3));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLastIndexOfAny_StringStringArray() {
        assertNpeOr(() -> assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny((
                CharSequence) null)));
        assertNpeOr(() -> assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny((
                CharSequence[]) null)));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny());
        assertEquals(3, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny(FOOBAR_SUB_ARRAY));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny(new String[0]));
        assertEquals(-1, asString("").lastIndexOfAny(new String[0]));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny(new String[] {"llll"}));
        assertEquals(6, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny(new String[] {""}));
        assertEquals(0, asString("").lastIndexOfAny(new String[] {""}));
        assertEquals(-1, asString("").lastIndexOfAny(new String[] {"a"}));
        assertEquals(-1, asString("").lastIndexOfAny(new String[] {null}));
        assertEquals(-1, asString(FOOBAR_EQUALSINDEXOF).lastIndexOfAny(new String[] {null}));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLastIndexOfIgnoreCase_String() {
        assertThrows(NullPointerException.class, () -> asString("").lastIndexOfIgnoreCase(null));
        assertEquals(-1, asString("").lastIndexOfIgnoreCase("a"));
        assertEquals(0, asString("").lastIndexOfIgnoreCase(""));
        assertEquals(8, asString("aabaabaa").lastIndexOfIgnoreCase(""));
        assertEquals(7, asString("aabaabaa").lastIndexOfIgnoreCase("a"));
        assertEquals(7, asString("aabaabaa").lastIndexOfIgnoreCase("A"));
        assertEquals(5, asString("aabaabaa").lastIndexOfIgnoreCase("b"));
        assertEquals(5, asString("aabaabaa").lastIndexOfIgnoreCase("B"));
        assertEquals(4, asString("aabaabaa").lastIndexOfIgnoreCase("ab"));
        assertEquals(4, asString("aabaabaa").lastIndexOfIgnoreCase("AB"));
        assertEquals(-1, asString("ab").lastIndexOfIgnoreCase("AAB"));
        assertEquals(0, asString("aab").lastIndexOfIgnoreCase("AAB"));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLastIndexOfIgnoreCase_StringInt() {
        assertThrows(NullPointerException.class, () -> asString("").lastIndexOfIgnoreCase(null, 0));
        assertThrows(NullPointerException.class, () -> asString("").lastIndexOfIgnoreCase(null,
                -1));
        assertEquals(0, asString("").lastIndexOfIgnoreCase("", 0));
        assertEquals(-1, asString("").lastIndexOfIgnoreCase("", -1));
        assertEquals(0, asString("").lastIndexOfIgnoreCase("", 9));
        assertEquals(0, asString("abc").lastIndexOfIgnoreCase("", 0));
        assertEquals(-1, asString("abc").lastIndexOfIgnoreCase("", -1));
        assertEquals(3, asString("abc").lastIndexOfIgnoreCase("", 9));
        assertEquals(7, asString("aabaabaa").lastIndexOfIgnoreCase("A", 8));
        assertEquals(5, asString("aabaabaa").lastIndexOfIgnoreCase("B", 8));
        assertEquals(4, asString("aabaabaa").lastIndexOfIgnoreCase("AB", 8));
        assertEquals(2, asString("aabaabaa").lastIndexOfIgnoreCase("B", 3));
        assertEquals(5, asString("aabaabaa").lastIndexOfIgnoreCase("B", 9));
        assertEquals(-1, asString("aabaabaa").lastIndexOfIgnoreCase("B", -1));
        assertEquals(-1, asString("aabaabaa").lastIndexOfIgnoreCase("B", 0));
        assertEquals(0, asString("aabaabaa").lastIndexOfIgnoreCase("A", 0));
        assertEquals(1, asString("aab").lastIndexOfIgnoreCase("AB", 1));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testLastOrdinalIndexOf() {
        assertThrows(NullPointerException.class, () -> asString("*").lastOrdinalIndexOf(null, 42));
        assertEquals(0, asString("").lastOrdinalIndexOf("", 42));
        assertEquals(7, asString("aabaabaa").lastOrdinalIndexOf("a", 1));
        assertEquals(6, asString("aabaabaa").lastOrdinalIndexOf("a", 2));
        assertEquals(5, asString("aabaabaa").lastOrdinalIndexOf("b", 1));
        assertEquals(2, asString("aabaabaa").lastOrdinalIndexOf("b", 2));
        assertEquals(4, asString("aabaabaa").lastOrdinalIndexOf("ab", 1));
        assertEquals(1, asString("aabaabaa").lastOrdinalIndexOf("ab", 2));
        assertEquals(8, asString("aabaabaa").lastOrdinalIndexOf("", 1));
        assertEquals(8, asString("aabaabaa").lastOrdinalIndexOf("", 2));
    }

    @Test
    void stringUtilsEqualsIndexOfTest_testOrdinalIndexOf() {
        assertThrows(NullPointerException.class, () -> asString("").ordinalIndexOf(null,
                Integer.MIN_VALUE));
        assertEquals(-1, asString("").ordinalIndexOf("", Integer.MIN_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("a", Integer.MIN_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("b", Integer.MIN_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("ab", Integer.MIN_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("", Integer.MIN_VALUE));
        assertThrows(NullPointerException.class, () -> asString("").ordinalIndexOf(null, -1));
        assertEquals(-1, asString("").ordinalIndexOf("", -1));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("a", -1));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("b", -1));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("ab", -1));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("", -1));
        assertThrows(NullPointerException.class, () -> asString("").ordinalIndexOf(null, 0));
        assertEquals(-1, asString("").ordinalIndexOf("", 0));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("a", 0));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("b", 0));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("ab", 0));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("", 0));
        assertThrows(NullPointerException.class, () -> asString("").ordinalIndexOf(null, 1));
        assertEquals(0, asString("").ordinalIndexOf("", 1));
        assertEquals(0, asString("aabaabaa").ordinalIndexOf("a", 1));
        assertEquals(2, asString("aabaabaa").ordinalIndexOf("b", 1));
        assertEquals(1, asString("aabaabaa").ordinalIndexOf("ab", 1));
        assertEquals(0, asString("aabaabaa").ordinalIndexOf("", 1));
        assertThrows(NullPointerException.class, () -> asString("").ordinalIndexOf(null, 2));
        assertEquals(0, asString("").ordinalIndexOf("", 2));
        assertEquals(1, asString("aabaabaa").ordinalIndexOf("a", 2));
        assertEquals(5, asString("aabaabaa").ordinalIndexOf("b", 2));
        assertEquals(4, asString("aabaabaa").ordinalIndexOf("ab", 2));
        assertEquals(0, asString("aabaabaa").ordinalIndexOf("", 2));
        assertThrows(NullPointerException.class, () -> asString("").ordinalIndexOf(null,
                Integer.MAX_VALUE));
        assertEquals(0, asString("").ordinalIndexOf("", Integer.MAX_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("a", Integer.MAX_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("b", Integer.MAX_VALUE));
        assertEquals(-1, asString("aabaabaa").ordinalIndexOf("ab", Integer.MAX_VALUE));
        assertEquals(0, asString("aabaabaa").ordinalIndexOf("", Integer.MAX_VALUE));
        assertEquals(-1, asString("aaaaaaaaa").ordinalIndexOf("a", 0));
        assertEquals(0, asString("aaaaaaaaa").ordinalIndexOf("a", 1));
        assertEquals(1, asString("aaaaaaaaa").ordinalIndexOf("a", 2));
        assertEquals(2, asString("aaaaaaaaa").ordinalIndexOf("a", 3));
        assertEquals(3, asString("aaaaaaaaa").ordinalIndexOf("a", 4));
        assertEquals(4, asString("aaaaaaaaa").ordinalIndexOf("a", 5));
        assertEquals(5, asString("aaaaaaaaa").ordinalIndexOf("a", 6));
        assertEquals(6, asString("aaaaaaaaa").ordinalIndexOf("a", 7));
        assertEquals(7, asString("aaaaaaaaa").ordinalIndexOf("a", 8));
        assertEquals(8, asString("aaaaaaaaa").ordinalIndexOf("a", 9));
        assertEquals(-1, asString("aaaaaaaaa").ordinalIndexOf("a", 10));
        assertEquals(0, asString("aaaaaa").ordinalIndexOf("aa", 1));
        assertEquals(1, asString("aaaaaa").ordinalIndexOf("aa", 2));
        assertEquals(2, asString("aaaaaa").ordinalIndexOf("aa", 3));
        assertEquals(3, asString("aaaaaa").ordinalIndexOf("aa", 4));
        assertEquals(4, asString("aaaaaa").ordinalIndexOf("aa", 5));
        assertEquals(-1, asString("aaaaaa").ordinalIndexOf("aa", 6));
        assertEquals(0, asString("ababab").ordinalIndexOf("aba", 1));
        assertEquals(2, asString("ababab").ordinalIndexOf("aba", 2));
        assertEquals(-1, asString("ababab").ordinalIndexOf("aba", 3));
        assertEquals(0, asString("abababab").ordinalIndexOf("abab", 1));
        assertEquals(2, asString("abababab").ordinalIndexOf("abab", 2));
        assertEquals(4, asString("abababab").ordinalIndexOf("abab", 3));
        assertEquals(-1, asString("abababab").ordinalIndexOf("abab", 4));
    }

    @Test
    void stringUtilsStartsEndsWithTest_testEndsWithAny() {
        assertNpeOr(() -> assertFalse(asString("abcxyz").endsWithAny((String) null),
                "asString(abcxyz).endsWithAny(null)"));
        assertTrue(asString("abcxyz").endsWithAny(""),
                "asString(abcxyz).endsWithAny(new String[] {\"\"})");
        assertTrue(asString("abcxyz").endsWithAny("xyz"),
                "asString(abcxyz).endsWithAny(new String[] {xyz})");
        assertTrue(asString("abcxyz").endsWithAny(null, "xyz", "abc"),
                "asString(abcxyz).endsWithAny(new String[] {null, xyz, abc})");
        assertFalse(asString("defg").endsWithAny(null, "xyz", "abc"),
                "asString(defg).endsWithAny(new String[] {null, xyz, abc})");
        assertTrue(asString("abcXYZ").endsWithAny("def", "XYZ"));
        assertFalse(asString("abcXYZ").endsWithAny("def", "xyz"));
        assertTrue(asString("abcXYZ").endsWithAny("def", "YZ"));
        assertNpeOr(() -> assertFalse(asString("abcXYZ").endsWithAny((CharSequence) null)));
        assertNpeOr(() -> assertFalse(asString("abcXYZ").endsWithAny((CharSequence[]) null)));
        assertTrue(asString("abcXYZ").endsWithAny(""));
        assertTrue(asString("abcxyz").endsWithAny(new StringBuilder("abc"), new StringBuffer(
                "xyz")), "asString(abcxyz).endsWithAny(StringBuilder(abc), StringBuffer(xyz))");
        assertTrue(asString(new StringBuffer("abcxyz")).endsWithAny(new StringBuilder("abc"),
                new StringBuffer("xyz")),
                "asString(StringBuffer(abcxyz)).endsWithAny(StringBuilder(abc), StringBuffer(xy"
                + "z))");
    }

    @Test
    void stringUtilsStartsEndsWithTest_testEndsWithIgnoreCase() {
        assertThrows(NullPointerException.class, () -> asString(
                FOOBAR_STARTSENDSWITH).endsWithIgnoreCase(null));
        assertTrue(asString(FOOBAR_STARTSENDSWITH).endsWithIgnoreCase(""),
                "endsWithIgnoreCase(FOOBAR, \"\")");
        assertFalse(asString(FOOBAR_STARTSENDSWITH).endsWithIgnoreCase(FOO),
                "endsWithIgnoreCase(FOOBAR, FOO)");
        assertTrue(asString(FOOBAR_STARTSENDSWITH).endsWithIgnoreCase(BAR),
                "endsWithIgnoreCase(FOOBAR, BAR)");
        assertTrue(asString("abcdef").endsWithIgnoreCase("def"));
        assertTrue(asString("ABCDEF").endsWithIgnoreCase("def"));
        assertFalse(asString("ABCDEF").endsWithIgnoreCase("cde"));
        assertTrue(asString("\u03B1\u03B2\u03B3\u03B4").endsWithIgnoreCase("\u0394"),
                "endsWith(\u03B1\u03B2\u03B3\u03B4, \u0394)");
        assertFalse(asString("\u03B1\u03B2\u03B3\u03B4").endsWithIgnoreCase("\u0393"),
                "endsWith(\u03B1\u03B2\u03B3\u03B4, \u0393)");
    }

    @Test
    void stringUtilsStartsEndsWithTest_testStartsWithAny() {
        assertNpeOr(() -> assertFalse(asString("abcxyz").startsWithAny((String[]) null)));
        assertFalse(asString("abcxyz").startsWithAny());
        assertTrue(asString("abcxyz").startsWithAny("abc"));
        assertTrue(asString("abcxyz").startsWithAny(null, "xyz", "abc"));
        assertFalse(asString("abcxyz").startsWithAny(null, "xyz", "abcd"));
        assertTrue(asString("abcxyz").startsWithAny(""));
        assertFalse(asString("abcxyz").startsWithAny(null, "xyz", "ABCX"));
        assertFalse(asString("ABCXYZ").startsWithAny(null, "xyz", "abc"));
        assertTrue(asString("abcxyz").startsWithAny(new StringBuilder("xyz"), new StringBuffer(
                "abc")), "asString(abcxyz).startsWithAny(StringBuilder(xyz), StringBuffer(abc))");
        assertTrue(asString(new StringBuffer("abcxyz")).startsWithAny(new StringBuilder("xyz"),
                new StringBuffer("abc")),
                "asString(StringBuffer(abcxyz)).startsWithAny(StringBuilder(xyz), StringBuffer("
                + "abc))");
    }

    @Test
    void stringUtilsStartsEndsWithTest_testStartsWithIgnoreCase() {
        assertThrows(NullPointerException.class, () -> asString(
                FOOBAR_STARTSENDSWITH).startsWithIgnoreCase(null));
        assertTrue(asString(FOOBAR_STARTSENDSWITH).startsWithIgnoreCase(""),
                "startsWithIgnoreCase(FOOBAR, \"\")");
        assertTrue(asString(FOOBAR_STARTSENDSWITH).startsWithIgnoreCase(FOO),
                "startsWithIgnoreCase(FOOBAR, FOO)");
        assertFalse(asString(FOOBAR_STARTSENDSWITH).startsWithIgnoreCase(BAR),
                "startsWithIgnoreCase(FOOBAR, BAR)");
    }

    @Test
    void stringUtilsTest_testDifferenceAt_StringArray() {
        assertEquals(-1, asString("").indexOfDifference(""));
        assertThrows(NullPointerException.class, () -> asString("").indexOfDifference(null));
        assertEquals(0, asString("").indexOfDifference("abc"));
        assertEquals(0, asString("abc").indexOfDifference(""));
        assertEquals(-1, asString("abc").indexOfDifference("abc"));
        assertEquals(1, asString("abc").indexOfDifference("a"));
        assertEquals(2, asString("ab").indexOfDifference("abxyz"));
        assertEquals(2, asString("abcde").indexOfDifference("abxyz"));
        assertEquals(0, asString("abcde").indexOfDifference("xyz"));
        assertEquals(0, asString("xyz").indexOfDifference("abcde"));
        assertEquals(7, asString("i am a machine").indexOfDifference("i am a robot"));
        final String cp10400 = new String(Character.toChars(0x10400));
        final String cp10401 = new String(Character.toChars(0x10401));
    }

    @Test
    void stringUtilsTest_testDifferenceAt_StringString() {
        assertEquals(-1, asString("").indexOfDifference(""));
        assertEquals(0, asString("").indexOfDifference("abc"));
        assertEquals(0, asString("abc").indexOfDifference(""));
        assertThrows(NullPointerException.class, () -> asString("i am a machine").indexOfDifference(
                null));
        assertEquals(7, asString("i am a machine").indexOfDifference("i am a robot"));
        assertEquals(-1, asString("foo").indexOfDifference("foo"));
        assertEquals(0, asString("i am a robot").indexOfDifference("you are a robot"));
        final String cp10400 = new String(Character.toChars(0x10400));
        final String cp10401 = new String(Character.toChars(0x10401));
        assertEquals(0, asString(cp10400).indexOfDifference(cp10401));
        assertEquals(2, asString(cp10400 + "X").indexOfDifference(cp10400 + "Y"));
    }
}
