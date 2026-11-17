/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1.1)
 * @run junit StringBuildFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringBuildFamilyDonorTest {

    private static String asString(CharSequence text) {
        return text.toString();
    }

    /** Runs a donor assertion; an NPE from the String method (null rejected) passes. */
    private static void assertNpeOr(Runnable assertion) {
        try {
            assertion.run();
        } catch (NullPointerException rejected) {
            // null array rejected as documented
        }
    }

    @Test
    void stringUtilsTest_testAppendIfMissing() {
        assertThrows(NullPointerException.class, () -> asString("abc").appendIfMissing(null));
        assertEquals("xyz", asString("").appendIfMissing("xyz"), "appendIfMissing(\"\",xyz)");
        assertEquals("abcxyz", asString("abc").appendIfMissing("xyz"), "appendIfMissing(abc,xyz)");
        assertEquals("abcxyz", asString("abcxyz").appendIfMissing("xyz"),
                "appendIfMissing(abcxyz,xyz)");
        assertEquals("aXYZxyz", asString("aXYZ").appendIfMissing("xyz"),
                "appendIfMissing(aXYZ,xyz)");
        assertThrows(NullPointerException.class, () -> asString("abc").appendIfMissing(null,
                (CharSequence[]) null));
        assertThrows(NullPointerException.class, () -> asString("").appendIfMissing("xyz",
                (CharSequence[]) null));
        assertEquals("abcxyz", asString("abc").appendIfMissing("xyz", (CharSequence) null),
                "appendIfMissing(abc,xyz,{null})");
        assertEquals("abc", asString("abc").appendIfMissing("xyz", ""),
                "appendIfMissing(abc,xyz,\"\")");
        assertEquals("abcxyz", asString("abc").appendIfMissing("xyz", "mno"),
                "appendIfMissing(abc,xyz,mno)");
        assertEquals("abcxyz", asString("abcxyz").appendIfMissing("xyz", "mno"),
                "appendIfMissing(abcxyz,xyz,mno)");
        assertEquals("abcmno", asString("abcmno").appendIfMissing("xyz", "mno"),
                "appendIfMissing(abcmno,xyz,mno)");
        assertEquals("abcXYZxyz", asString("abcXYZ").appendIfMissing("xyz", "mno"),
                "appendIfMissing(abcXYZ,xyz,mno)");
        assertEquals("abcMNOxyz", asString("abcMNO").appendIfMissing("xyz", "mno"),
                "appendIfMissing(abcMNO,xyz,mno)");
    }

    @Test
    void stringUtilsTest_testAppendIfMissingIgnoreCase() {
        assertThrows(NullPointerException.class,
                () -> asString("abc").appendIfMissingIgnoreCase(null));
        assertEquals("xyz", asString("").appendIfMissingIgnoreCase("xyz"),
                "appendIfMissingIgnoreCase(\"\",xyz)");
        assertEquals("abcxyz", asString("abc").appendIfMissingIgnoreCase("xyz"),
                "appendIfMissingIgnoreCase(abc,xyz)");
        assertEquals("abcxyz", asString("abcxyz").appendIfMissingIgnoreCase("xyz"),
                "appendIfMissingIgnoreCase(abcxyz,xyz)");
        assertEquals("abcXYZ", asString("abcXYZ").appendIfMissingIgnoreCase("xyz"),
                "appendIfMissingIgnoreCase(abcXYZ,xyz)");
        assertThrows(NullPointerException.class,
                () -> asString("abc").appendIfMissingIgnoreCase(null, (CharSequence[]) null));
        assertThrows(NullPointerException.class, () -> asString("").appendIfMissingIgnoreCase("xyz",
                (CharSequence[]) null));
        assertEquals("abcxyz", asString("abc").appendIfMissingIgnoreCase("xyz",
                (CharSequence) null), "appendIfMissingIgnoreCase(abc,xyz,{null})");
        assertEquals("abc", asString("abc").appendIfMissingIgnoreCase("xyz", ""),
                "appendIfMissingIgnoreCase(abc,xyz,\"\")");
        assertEquals("abcxyz", asString("abc").appendIfMissingIgnoreCase("xyz", "mno"),
                "appendIfMissingIgnoreCase(abc,xyz,mno)");
        assertEquals("abcxyz", asString("abcxyz").appendIfMissingIgnoreCase("xyz", "mno"),
                "appendIfMissingIgnoreCase(abcxyz,xyz,mno)");
        assertEquals("abcmno", asString("abcmno").appendIfMissingIgnoreCase("xyz", "mno"),
                "appendIfMissingIgnoreCase(abcmno,xyz,mno)");
        assertEquals("abcXYZ", asString("abcXYZ").appendIfMissingIgnoreCase("xyz", "mno"),
                "appendIfMissingIgnoreCase(abcXYZ,xyz,mno)");
        assertEquals("abcMNO", asString("abcMNO").appendIfMissingIgnoreCase("xyz", "mno"),
                "appendIfMissingIgnoreCase(abcMNO,xyz,mno)");
    }

    @Test
    void stringUtilsTest_testCenter_StringInt() {
        assertEquals("    ", asString("").center(4));
        assertEquals("ab", asString("ab").center(0));
        assertEquals("ab", asString("ab").center(-1));
        assertEquals("ab", asString("ab").center(1));
        assertEquals("    ", asString("").center(4));
        assertEquals(" ab ", asString("ab").center(4));
        assertEquals("abcd", asString("abcd").center(2));
        assertEquals(" a  ", asString("a").center(4));
        assertEquals("  a  ", asString("a").center(5));
    }

    @Test
    void stringUtilsTest_testCenter_StringIntChar() {
        assertEquals("    ", asString("").center(4, ' '));
        assertEquals("ab", asString("ab").center(0, ' '));
        assertEquals("ab", asString("ab").center(-1, ' '));
        assertEquals("ab", asString("ab").center(1, ' '));
        assertEquals("    ", asString("").center(4, ' '));
        assertEquals(" ab ", asString("ab").center(4, ' '));
        assertEquals("abcd", asString("abcd").center(2, ' '));
        assertEquals(" a  ", asString("a").center(4, ' '));
        assertEquals("  a  ", asString("a").center(5, ' '));
        assertEquals("xxaxx", asString("a").center(5, 'x'));
    }

    @Test
    void stringUtilsTest_testCenter_StringIntString() {
        assertEquals("    ", asString("").center(4, " "));
        assertEquals("ab", asString("ab").center(0, " "));
        assertEquals("ab", asString("ab").center(-1, " "));
        assertEquals("ab", asString("ab").center(1, " "));
        assertEquals("    ", asString("").center(4, " "));
        assertEquals(" ab ", asString("ab").center(4, " "));
        assertEquals("abcd", asString("abcd").center(2, " "));
        assertEquals(" a  ", asString("a").center(4, " "));
        assertEquals("yayz", asString("a").center(4, "yz"));
        assertEquals("yzyayzy", asString("a").center(7, "yz"));
        assertThrows(NullPointerException.class, () -> asString("abc").center(7, null));
        assertEquals("  abc  ", asString("abc").center(7, ""));
    }

    @Test
    void stringUtilsTest_testCenterSurrogatePair() {
        final String grin = "\uD83D\uDE00";
        assertEquals("a", asString("a").center(2, grin));
        assertEquals("a" + grin, asString("a").center(3, grin));
        assertEquals("a" + grin, asString("a").center(4, grin));
        assertEquals(grin + "a" + grin, asString("a").center(5, grin));
        assertEquals(grin + "a" + grin, asString("a").center(6, grin));
        assertEquals(grin + "a" + grin + grin, asString("a").center(7, grin));
    }

    @Test
    void stringUtilsTest_testLeftPad_StringInt() {
        assertEquals("     ", asString("").leftPad(5));
        assertEquals("  abc", asString("abc").leftPad(5));
        assertEquals("abc", asString("abc").leftPad(2));
        assertEquals("abc", asString("abc").leftPad(Integer.MIN_VALUE));
    }

    @Test
    void stringUtilsTest_testLeftPad_StringIntChar() {
        assertEquals("     ", asString("").leftPad(5, ' '));
        assertEquals("  abc", asString("abc").leftPad(5, ' '));
        assertEquals("xxabc", asString("abc").leftPad(5, 'x'));
        assertEquals("\uffff\uffffabc", asString("abc").leftPad(5, '\uffff'));
        assertEquals("abc", asString("abc").leftPad(2, ' '));
        assertEquals("abc", asString("abc").leftPad(Integer.MIN_VALUE, ' '));
    }

    @Test
    void stringUtilsTest_testLeftPad_StringIntString() {
        assertEquals("     ", asString("").leftPad(5, " "));
        assertEquals("-+-+abc", asString("abc").leftPad(7, "-+"));
        assertEquals("-+~abc", asString("abc").leftPad(6, "-+~"));
        assertEquals("-+abc", asString("abc").leftPad(5, "-+~"));
        assertEquals("abc", asString("abc").leftPad(2, " "));
        assertEquals("abc", asString("abc").leftPad(-1, " "));
        assertThrows(NullPointerException.class, () -> asString("abc").leftPad(5, null));
        assertEquals("  abc", asString("abc").leftPad(5, ""));
        assertEquals("abc", asString("abc").leftPad(Integer.MIN_VALUE, " "));
    }

    @Test
    void stringUtilsTest_testLeftPadSurrogatePair() {
        final String grin = "\uD83D\uDE00";
        assertEquals("a", asString("a").leftPad(2, grin));
        assertEquals(grin + "a", asString("a").leftPad(3, grin));
        assertEquals(grin + "a", asString("a").leftPad(4, grin));
        assertEquals(grin + grin + "a", asString("a").leftPad(5, grin));
    }

    @Test
    void stringUtilsTest_testOverlay_StringStringIntInt() {
        assertThrows(NullPointerException.class, () -> asString("").overlay(null, 0, 0));
        assertEquals("", asString("").overlay("", 0, 0));
        assertEquals("zzzz", asString("").overlay("zzzz", 0, 0));
        assertEquals("zzzz", asString("").overlay("zzzz", 2, 4));
        assertEquals("zzzz", asString("").overlay("zzzz", -2, -4));
        assertThrows(NullPointerException.class, () -> asString("abcdef").overlay(null, 2, 4));
        assertThrows(NullPointerException.class, () -> asString("abcdef").overlay(null, 4, 2));
        assertEquals("abef", asString("abcdef").overlay("", 2, 4));
        assertEquals("abef", asString("abcdef").overlay("", 4, 2));
        assertEquals("abzzzzef", asString("abcdef").overlay("zzzz", 2, 4));
        assertEquals("abzzzzef", asString("abcdef").overlay("zzzz", 4, 2));
        assertEquals("zzzzef", asString("abcdef").overlay("zzzz", -1, 4));
        assertEquals("zzzzef", asString("abcdef").overlay("zzzz", 4, -1));
        assertEquals("zzzzabcdef", asString("abcdef").overlay("zzzz", -2, -1));
        assertEquals("zzzzabcdef", asString("abcdef").overlay("zzzz", -1, -2));
        assertEquals("abcdzzzz", asString("abcdef").overlay("zzzz", 4, 10));
        assertEquals("abcdzzzz", asString("abcdef").overlay("zzzz", 10, 4));
        assertEquals("abcdefzzzz", asString("abcdef").overlay("zzzz", 8, 10));
        assertEquals("abcdefzzzz", asString("abcdef").overlay("zzzz", 10, 8));
    }

    @Test
    void stringUtilsTest_testOverlaySurrogatePair() {
        final String grin = "\ud83d\ude00";
        assertEquals("X", asString(grin).overlay("X", 1, 1));
        assertEquals("aXb", asString("a" + grin + "b").overlay("X", 1, 3));
        assertEquals("aXb", asString("a" + grin + "b").overlay("X", 2, 2));
    }

    @Test
    void stringUtilsTest_testPadMixedSurrogatePair() {
        final String grin = "\uD83D\uDE00";
        final String pad = "a" + grin + "b";
        assertEquals("ax", asString("x").leftPad(3, pad));
        assertEquals("xa", asString("x").rightPad(3, pad));
        assertEquals("a" + grin + "bax", asString("x").leftPad(7, pad));
        assertEquals("xa" + grin + "ba", asString("x").rightPad(7, pad));
        assertEquals("a" + grin + "x", asString("x").leftPad(4, pad));
        assertEquals("xa" + grin + "ba" + grin, asString("x").rightPad(8, pad));
        assertEquals("axa", asString("x").center(4, pad));
        assertEquals("axa" + grin + "b", asString("x").center(6, pad));
        assertEquals("a" + grin + "xa" + grin, asString("x").center(7, pad));
    }

    @Test
    void stringUtilsTest_testPrependIfMissing() {
        assertThrows(NullPointerException.class, () -> asString("abc").prependIfMissing(null));
        assertEquals("xyz", asString("").prependIfMissing("xyz"), "prependIfMissing(\"\",xyz)");
        assertEquals("xyzabc", asString("abc").prependIfMissing("xyz"),
                "prependIfMissing(abc,xyz)");
        assertEquals("xyzabc", asString("xyzabc").prependIfMissing("xyz"),
                "prependIfMissing(xyzabc,xyz)");
        assertEquals("xyzXYZabc", asString("XYZabc").prependIfMissing("xyz"),
                "prependIfMissing(XYZabc,xyz)");
        assertThrows(NullPointerException.class, () -> asString("abc").prependIfMissing(null,
                (CharSequence[]) null));
        assertThrows(NullPointerException.class, () -> asString("").prependIfMissing("xyz",
                (CharSequence[]) null));
        assertEquals("xyzabc", asString("abc").prependIfMissing("xyz", (CharSequence) null),
                "prependIfMissing(abc,xyz,{null})");
        assertEquals("abc", asString("abc").prependIfMissing("xyz", ""),
                "prependIfMissing(abc,xyz,\"\")");
        assertEquals("xyzabc", asString("abc").prependIfMissing("xyz", "mno"),
                "prependIfMissing(abc,xyz,mno)");
        assertEquals("xyzabc", asString("xyzabc").prependIfMissing("xyz", "mno"),
                "prependIfMissing(xyzabc,xyz,mno)");
        assertEquals("mnoabc", asString("mnoabc").prependIfMissing("xyz", "mno"),
                "prependIfMissing(mnoabc,xyz,mno)");
        assertEquals("xyzXYZabc", asString("XYZabc").prependIfMissing("xyz", "mno"),
                "prependIfMissing(XYZabc,xyz,mno)");
        assertEquals("xyzMNOabc", asString("MNOabc").prependIfMissing("xyz", "mno"),
                "prependIfMissing(MNOabc,xyz,mno)");
    }

    @Test
    void stringUtilsTest_testPrependIfMissingIgnoreCase() {
        assertThrows(NullPointerException.class,
                () -> asString("abc").prependIfMissingIgnoreCase(null));
        assertEquals("xyz", asString("").prependIfMissingIgnoreCase("xyz"),
                "prependIfMissingIgnoreCase(\"\",xyz)");
        assertEquals("xyzabc", asString("abc").prependIfMissingIgnoreCase("xyz"),
                "prependIfMissingIgnoreCase(abc,xyz)");
        assertEquals("xyzabc", asString("xyzabc").prependIfMissingIgnoreCase("xyz"),
                "prependIfMissingIgnoreCase(xyzabc,xyz)");
        assertEquals("XYZabc", asString("XYZabc").prependIfMissingIgnoreCase("xyz"),
                "prependIfMissingIgnoreCase(XYZabc,xyz)");
        assertThrows(NullPointerException.class,
                () -> asString("abc").prependIfMissingIgnoreCase(null, (CharSequence[]) null));
        assertThrows(NullPointerException.class,
                () -> asString("").prependIfMissingIgnoreCase("xyz", (CharSequence[]) null));
        assertEquals("xyzabc", asString("abc").prependIfMissingIgnoreCase("xyz",
                (CharSequence) null), "prependIfMissingIgnoreCase(abc,xyz,{null})");
        assertEquals("abc", asString("abc").prependIfMissingIgnoreCase("xyz", ""),
                "prependIfMissingIgnoreCase(abc,xyz,\"\")");
        assertEquals("xyzabc", asString("abc").prependIfMissingIgnoreCase("xyz", "mno"),
                "prependIfMissingIgnoreCase(abc,xyz,mno)");
        assertEquals("xyzabc", asString("xyzabc").prependIfMissingIgnoreCase("xyz", "mno"),
                "prependIfMissingIgnoreCase(xyzabc,xyz,mno)");
        assertEquals("mnoabc", asString("mnoabc").prependIfMissingIgnoreCase("xyz", "mno"),
                "prependIfMissingIgnoreCase(mnoabc,xyz,mno)");
        assertEquals("XYZabc", asString("XYZabc").prependIfMissingIgnoreCase("xyz", "mno"),
                "prependIfMissingIgnoreCase(XYZabc,xyz,mno)");
        assertEquals("MNOabc", asString("MNOabc").prependIfMissingIgnoreCase("xyz", "mno"),
                "prependIfMissingIgnoreCase(MNOabc,xyz,mno)");
    }

    @Test
    void stringUtilsTest_testRepeat_StringStringInt() {
        assertThrows(NullPointerException.class, () -> asString("").repeat(null, 2));
        assertEquals("", asString("ab").repeat("", 0));
        assertEquals("", asString("").repeat("", 2));
        assertEquals("xx", asString("").repeat("x", 3));
        assertEquals("?, ?, ?", asString("?").repeat(", ", 3));
        assertThrows(IllegalArgumentException.class, () -> asString("?").repeat(", ",
                1_073_741_824));
    }

    @Test
    void stringUtilsTest_testReverse_String() {
        assertEquals("", asString("").reverse());
        assertEquals("sdrawkcab", asString("backwards").reverse());
    }

    @Test
    void stringUtilsTest_testReverseDelimited_StringChar() {
        assertEquals("", asString("").reverseDelimited('.'));
        assertEquals("c.b.a", asString("a.b.c").reverseDelimited('.'));
        assertEquals("a b c", asString("a b c").reverseDelimited('.'));
        assertEquals("", asString("").reverseDelimited('.'));
    }

    @Test
    void stringUtilsTest_testRightPad_StringInt() {
        assertEquals("     ", asString("").rightPad(5));
        assertEquals("abc  ", asString("abc").rightPad(5));
        assertEquals("abc", asString("abc").rightPad(2));
        assertEquals("abc", asString("abc").rightPad(-1));
        assertEquals("abc", asString("abc").rightPad(Integer.MIN_VALUE));
    }

    @Test
    void stringUtilsTest_testRightPad_StringIntChar() {
        assertEquals("     ", asString("").rightPad(5, ' '));
        assertEquals("abc  ", asString("abc").rightPad(5, ' '));
        assertEquals("abc", asString("abc").rightPad(2, ' '));
        assertEquals("abc", asString("abc").rightPad(-1, ' '));
        assertEquals("abcxx", asString("abc").rightPad(5, 'x'));
        assertEquals("abc", asString("abc").rightPad(Integer.MIN_VALUE, ' '));
    }

    @Test
    void stringUtilsTest_testRightPad_StringIntString() {
        assertEquals("     ", asString("").rightPad(5, " "));
        assertEquals("abc-+-+", asString("abc").rightPad(7, "-+"));
        assertEquals("abc-+~", asString("abc").rightPad(6, "-+~"));
        assertEquals("abc-+", asString("abc").rightPad(5, "-+~"));
        assertEquals("abc", asString("abc").rightPad(2, " "));
        assertEquals("abc", asString("abc").rightPad(-1, " "));
        assertThrows(NullPointerException.class, () -> asString("abc").rightPad(5, null));
        assertEquals("abc  ", asString("abc").rightPad(5, ""));
        assertEquals("abc", asString("abc").rightPad(Integer.MIN_VALUE, " "));
    }

    @Test
    void stringUtilsTest_testRightPadSurrogatePair() {
        final String grin = "\uD83D\uDE00";
        assertEquals("a", asString("a").rightPad(2, grin));
        assertEquals("a" + grin, asString("a").rightPad(3, grin));
        assertEquals("a" + grin, asString("a").rightPad(4, grin));
        assertEquals("a" + grin + grin, asString("a").rightPad(5, grin));
    }

    @Test
    void stringUtilsTest_testRotate_StringInt() {
        assertEquals("", asString("").rotate(1));
        assertEquals("abcdefg", asString("abcdefg").rotate(0));
        assertEquals("fgabcde", asString("abcdefg").rotate(2));
        assertEquals("cdefgab", asString("abcdefg").rotate(-2));
        assertEquals("abcdefg", asString("abcdefg").rotate(7));
        assertEquals("abcdefg", asString("abcdefg").rotate(-7));
        assertEquals("fgabcde", asString("abcdefg").rotate(9));
        assertEquals("cdefgab", asString("abcdefg").rotate(-9));
        assertEquals("efgabcd", asString("abcdefg").rotate(17));
        assertEquals("defgabc", asString("abcdefg").rotate(-17));
    }

    @Test
    void stringUtilsTest_testWrap_StringChar() {
        assertEquals("xabx", asString("ab").wrap('x'));
        assertEquals("\"ab\"", asString("ab").wrap('\"'));
        assertEquals("\"\"ab\"\"", asString("\"ab\"").wrap('\"'));
        assertEquals("'ab'", asString("ab").wrap('\''));
        assertEquals("''abcd''", asString("'abcd'").wrap('\''));
        assertEquals("'\"abcd\"'", asString("\"abcd\"").wrap('\''));
        assertEquals("\"'abcd'\"", asString("'abcd'").wrap('\"'));
    }

    @Test
    void stringUtilsTest_testWrap_StringString() {
        assertEquals("", asString("").wrap(""));
        assertThrows(NullPointerException.class, () -> asString("ab").wrap(null));
        assertEquals("xabx", asString("ab").wrap("x"));
        assertEquals("\"ab\"", asString("ab").wrap("\""));
        assertEquals("\"\"ab\"\"", asString("\"ab\"").wrap("\""));
        assertEquals("'ab'", asString("ab").wrap("'"));
        assertEquals("''abcd''", asString("'abcd'").wrap("'"));
        assertEquals("'\"abcd\"'", asString("\"abcd\"").wrap("'"));
        assertEquals("\"'abcd'\"", asString("'abcd'").wrap("\""));
    }

    @Test
    void stringUtilsTest_testWrapIfMissing_StringChar() {
        assertEquals("xabx", asString("ab").wrapIfMissing('x'));
        assertEquals("\"ab\"", asString("ab").wrapIfMissing('\"'));
        assertEquals("\"ab\"", asString("\"ab\"").wrapIfMissing('\"'));
        assertEquals("'ab'", asString("ab").wrapIfMissing('\''));
        assertEquals("'abcd'", asString("'abcd'").wrapIfMissing('\''));
        assertEquals("'\"abcd\"'", asString("\"abcd\"").wrapIfMissing('\''));
        assertEquals("\"'abcd'\"", asString("'abcd'").wrapIfMissing('\"'));
        assertEquals("/x/", asString("x").wrapIfMissing('/'));
        assertEquals("/x/y/z/", asString("x/y/z").wrapIfMissing('/'));
        assertEquals("/x/y/z/", asString("/x/y/z").wrapIfMissing('/'));
        assertEquals("/x/y/z/", asString("x/y/z/").wrapIfMissing('/'));
        assertSame("/", asString("/").wrapIfMissing('/'));
        assertSame("/x/", asString("/x/").wrapIfMissing('/'));
    }

    @Test
    void stringUtilsTest_testWrapIfMissing_StringString() {
        assertEquals("", asString("").wrapIfMissing("\0"));
        assertEquals("xabx", asString("ab").wrapIfMissing("x"));
        assertEquals("\"ab\"", asString("ab").wrapIfMissing("\""));
        assertEquals("\"ab\"", asString("\"ab\"").wrapIfMissing("\""));
        assertEquals("'ab'", asString("ab").wrapIfMissing("\'"));
        assertEquals("'abcd'", asString("'abcd'").wrapIfMissing("\'"));
        assertEquals("'\"abcd\"'", asString("\"abcd\"").wrapIfMissing("\'"));
        assertEquals("\"'abcd'\"", asString("'abcd'").wrapIfMissing("\""));
        assertEquals("/x/", asString("x").wrapIfMissing("/"));
        assertEquals("/x/y/z/", asString("x/y/z").wrapIfMissing("/"));
        assertEquals("/x/y/z/", asString("/x/y/z").wrapIfMissing("/"));
        assertEquals("/x/y/z/", asString("x/y/z/").wrapIfMissing("/"));
        assertEquals("/", asString("/").wrapIfMissing("/"));
        assertEquals("ab/ab", asString("/").wrapIfMissing("ab"));
        assertSame("ab/ab", asString("ab/ab").wrapIfMissing("ab"));
        assertSame("//x//", asString("//x//").wrapIfMissing("//"));
    }
}
