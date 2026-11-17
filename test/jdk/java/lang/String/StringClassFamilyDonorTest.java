/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringClassFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringClassFamilyDonorTest {
    private static final String HARD_SPACE;
    private static final String NON_TRIMMABLE;
    private static final String NON_WHITESPACE;
    private static final String TRIMMABLE;
    private static final String WHITESPACE;

    static {
            final StringBuilder ws = new StringBuilder();
            final StringBuilder nws = new StringBuilder();
            final String hs = String.valueOf((char) 160);
            final StringBuilder tr = new StringBuilder();
            final StringBuilder ntr = new StringBuilder();
            for (int i = 0; i < Character.MAX_VALUE; i++) {
                if (Character.isWhitespace((char) i)) {
                    ws.append((char) i);
                    if (i > 32) {
                        ntr.append((char) i);
                    }
                } else if (i < 40) {
                    nws.append((char) i);
                }
            }
            for (int i = 0; i <= 32; i++) {
                tr.append((char) i);
            }
            WHITESPACE = ws.toString();
            NON_WHITESPACE = nws.toString();
            HARD_SPACE = hs;
            TRIMMABLE = tr.toString();
            NON_TRIMMABLE = ntr.toString();
        }

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
    void stringUtilsIsTest_testIsAlpha() {
        assertFalse(asString("").isAlpha());
        assertFalse(asString(" ").isAlpha());
        assertTrue(asString("a").isAlpha());
        assertTrue(asString("A").isAlpha());
        assertTrue(asString("kgKgKgKgkgkGkjkjlJlOKLgHdGdHgl").isAlpha());
        assertFalse(asString("ham kso").isAlpha());
        assertFalse(asString("1").isAlpha());
        assertFalse(asString("hkHKHik6iUGHKJgU7tUJgKJGI87GIkug").isAlpha());
        assertFalse(asString("_").isAlpha());
        assertFalse(asString("hkHKHik*khbkuh").isAlpha());
    }

    @Test
    void stringUtilsIsTest_testIsAlphanumeric() {
        assertFalse(asString("").isAlphanumeric());
        assertFalse(asString(" ").isAlphanumeric());
        assertTrue(asString("a").isAlphanumeric());
        assertTrue(asString("A").isAlphanumeric());
        assertTrue(asString("kgKgKgKgkgkGkjkjlJlOKLgHdGdHgl").isAlphanumeric());
        assertFalse(asString("ham kso").isAlphanumeric());
        assertTrue(asString("1").isAlphanumeric());
        assertTrue(asString("hkHKHik6iUGHKJgU7tUJgKJGI87GIkug").isAlphanumeric());
        assertFalse(asString("_").isAlphanumeric());
        assertFalse(asString("hkHKHik*khbkuh").isAlphanumeric());
    }

    @Test
    void stringUtilsIsTest_testIsAlphanumericSpace() {
        assertTrue(asString("").isAlphanumericSpace());
        assertTrue(asString(" ").isAlphanumericSpace());
        assertTrue(asString("a").isAlphanumericSpace());
        assertTrue(asString("A").isAlphanumericSpace());
        assertTrue(asString("kgKgKgKgkgkGkjkjlJlOKLgHdGdHgl").isAlphanumericSpace());
        assertTrue(asString("ham kso").isAlphanumericSpace());
        assertTrue(asString("1").isAlphanumericSpace());
        assertTrue(asString("hkHKHik6iUGHKJgU7tUJgKJGI87GIkug").isAlphanumericSpace());
        assertFalse(asString("_").isAlphanumericSpace());
        assertFalse(asString("hkHKHik*khbkuh").isAlphanumericSpace());
    }

    @Test
    void stringUtilsIsTest_testIsAlphanumericSpaceSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x10400)) + " " + new String(
                Character.toChars(0x1D7CE))).isAlphanumericSpace());
    }

    @Test
    void stringUtilsIsTest_testIsAlphanumericSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x10400))).isAlphanumeric());
        assertTrue(asString(new String(Character.toChars(0x1D7CE))).isAlphanumeric());
    }

    @Test
    void stringUtilsIsTest_testIsAlphaspace() {
        assertTrue(asString("").isAlphaSpace());
        assertTrue(asString(" ").isAlphaSpace());
        assertTrue(asString("a").isAlphaSpace());
        assertTrue(asString("A").isAlphaSpace());
        assertTrue(asString("kgKgKgKgkgkGkjkjlJlOKLgHdGdHgl").isAlphaSpace());
        assertTrue(asString("ham kso").isAlphaSpace());
        assertFalse(asString("1").isAlphaSpace());
        assertFalse(asString("hkHKHik6iUGHKJgU7tUJgKJGI87GIkug").isAlphaSpace());
        assertFalse(asString("_").isAlphaSpace());
        assertFalse(asString("hkHKHik*khbkuh").isAlphaSpace());
    }

    @Test
    void stringUtilsIsTest_testIsAlphaSpaceSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x10400)) + " a").isAlphaSpace());
        assertFalse(asString(new String(Character.toChars(0x1D7CE))).isAlphaSpace());
    }

    @Test
    void stringUtilsIsTest_testIsAlphaSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x10400))).isAlpha());
        assertFalse(asString(new String(Character.toChars(0x1D7CE))).isAlpha());
    }

    @Test
    void stringUtilsIsTest_testIsAsciiPrintable_String() {
        assertTrue(asString("").isAsciiPrintable());
        assertTrue(asString(" ").isAsciiPrintable());
        assertTrue(asString("a").isAsciiPrintable());
        assertTrue(asString("A").isAsciiPrintable());
        assertTrue(asString("1").isAsciiPrintable());
        assertTrue(asString("Ceki").isAsciiPrintable());
        assertTrue(asString("!ab2c~").isAsciiPrintable());
        assertTrue(asString("1000").isAsciiPrintable());
        assertTrue(asString("10 00").isAsciiPrintable());
        assertFalse(asString("10\t00").isAsciiPrintable());
        assertTrue(asString("10.00").isAsciiPrintable());
        assertTrue(asString("10,00").isAsciiPrintable());
        assertTrue(asString("!ab-c~").isAsciiPrintable());
        assertTrue(asString("hkHK=Hik6i?UGH_KJgU7.tUJgKJ*GI87GI,kug").isAsciiPrintable());
        assertTrue(asString("\u0020").isAsciiPrintable());
        assertTrue(asString("\u0021").isAsciiPrintable());
        assertTrue(asString("\u007e").isAsciiPrintable());
        assertFalse(asString("\u007f").isAsciiPrintable());
        assertTrue(asString("G?lc?").isAsciiPrintable());
        assertTrue(asString("=?iso-8859-1?Q?G=FClc=FC?=").isAsciiPrintable());
        assertFalse(asString("G\u00fclc\u00fc").isAsciiPrintable());
    }

    @Test
    void stringUtilsIsTest_testIsNumeric() {
        assertFalse(asString("").isNumeric());
        assertFalse(asString(" ").isNumeric());
        assertFalse(asString("a").isNumeric());
        assertFalse(asString("A").isNumeric());
        assertFalse(asString("kgKgKgKgkgkGkjkjlJlOKLgHdGdHgl").isNumeric());
        assertFalse(asString("ham kso").isNumeric());
        assertTrue(asString("1").isNumeric());
        assertTrue(asString("1000").isNumeric());
        assertTrue(asString("\u0967\u0968\u0969").isNumeric());
        assertFalse(asString("\u0967\u0968 \u0969").isNumeric());
        assertFalse(asString("2.3").isNumeric());
        assertFalse(asString("10 00").isNumeric());
        assertFalse(asString("hkHKHik6iUGHKJgU7tUJgKJGI87GIkug").isNumeric());
        assertFalse(asString("_").isNumeric());
        assertFalse(asString("hkHKHik*khbkuh").isNumeric());
        assertFalse(asString("+123").isNumeric());
        assertFalse(asString("-123").isNumeric());
    }

    @Test
    void stringUtilsIsTest_testIsNumericSpace() {
        assertTrue(asString("").isNumericSpace());
        assertTrue(asString(" ").isNumericSpace());
        assertFalse(asString("a").isNumericSpace());
        assertFalse(asString("A").isNumericSpace());
        assertFalse(asString("kgKgKgKgkgkGkjkjlJlOKLgHdGdHgl").isNumericSpace());
        assertFalse(asString("ham kso").isNumericSpace());
        assertTrue(asString("1").isNumericSpace());
        assertTrue(asString("1000").isNumericSpace());
        assertFalse(asString("2.3").isNumericSpace());
        assertTrue(asString("10 00").isNumericSpace());
        assertTrue(asString("\u0967\u0968\u0969").isNumericSpace());
        assertTrue(asString("\u0967\u0968 \u0969").isNumericSpace());
        assertFalse(asString("hkHKHik6iUGHKJgU7tUJgKJGI87GIkug").isNumericSpace());
        assertFalse(asString("_").isNumericSpace());
        assertFalse(asString("hkHKHik*khbkuh").isNumericSpace());
    }

    @Test
    void stringUtilsIsTest_testIsNumericSpaceSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x1D7CE)) + " " + new String(
                Character.toChars(0x1D7CE))).isNumericSpace());
        assertFalse(asString(new String(Character.toChars(0x10400))).isNumericSpace());
    }

    @Test
    void stringUtilsIsTest_testIsNumericSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x1D7CE))).isNumeric());
        assertFalse(asString(new String(Character.toChars(0x10400))).isNumeric());
    }

    @Test
    void stringUtilsIsTest_testIsWhitespace() {
        assertTrue(asString("").isBlank());
        assertTrue(asString(" ").isBlank());
        assertTrue(asString("\t \n \t").isBlank());
        assertFalse(asString("\t aa\n \t").isBlank());
        assertTrue(asString(" ").isBlank());
        assertFalse(asString(" a ").isBlank());
        assertFalse(asString("a  ").isBlank());
        assertFalse(asString("  a").isBlank());
        assertFalse(asString("aba").isBlank());
        assertFalse(asString("a").isBlank());
        assertTrue(asString(WHITESPACE).isBlank());
        assertFalse(asString(NON_WHITESPACE).isBlank());
    }

    @Test
    void stringUtilsTest_testIsAllLowerCase() {
        assertFalse(asString("").isAllLowerCase());
        assertFalse(asString("  ").isAllLowerCase());
        assertTrue(asString("abc").isAllLowerCase());
        assertFalse(asString("abc ").isAllLowerCase());
        assertFalse(asString("abc\n").isAllLowerCase());
        assertFalse(asString("abC").isAllLowerCase());
        assertFalse(asString("ab c").isAllLowerCase());
        assertFalse(asString("ab1c").isAllLowerCase());
        assertFalse(asString("ab/c").isAllLowerCase());
    }

    @Test
    void stringUtilsTest_testIsAllLowerCaseSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x10428))).isAllLowerCase());
        assertFalse(asString(new String(Character.toChars(0x10400))).isAllLowerCase());
    }

    @Test
    void stringUtilsTest_testIsAllUpperCase() {
        assertFalse(asString("").isAllUpperCase());
        assertFalse(asString("  ").isAllUpperCase());
        assertTrue(asString("ABC").isAllUpperCase());
        assertFalse(asString("ABC ").isAllUpperCase());
        assertFalse(asString("ABC\n").isAllUpperCase());
        assertFalse(asString("aBC").isAllUpperCase());
        assertFalse(asString("A C").isAllUpperCase());
        assertFalse(asString("A1C").isAllUpperCase());
        assertFalse(asString("A/C").isAllUpperCase());
    }

    @Test
    void stringUtilsTest_testIsAllUpperCaseSupplementary() {
        assertTrue(asString(new String(Character.toChars(0x10400))).isAllUpperCase());
        assertFalse(asString(new String(Character.toChars(0x10428))).isAllUpperCase());
    }

    @Test
    void stringUtilsTest_testIsMixedCase() {
        assertFalse(asString("").isMixedCase());
        assertFalse(asString(" ").isMixedCase());
        assertFalse(asString("A").isMixedCase());
        assertFalse(asString("a").isMixedCase());
        assertFalse(asString("/").isMixedCase());
        assertFalse(asString("A/").isMixedCase());
        assertFalse(asString("/b").isMixedCase());
        assertFalse(asString("abc").isMixedCase());
        assertFalse(asString("ABC").isMixedCase());
        assertTrue(asString("aBc").isMixedCase());
        assertTrue(asString("aBc ").isMixedCase());
        assertTrue(asString("A c").isMixedCase());
        assertTrue(asString("aBc\n").isMixedCase());
        assertTrue(asString("A1c").isMixedCase());
        assertTrue(asString("a/C").isMixedCase());
    }

    @Test
    void stringUtilsTest_testIsMixedCaseSupplementary() {
        assertTrue(asString("a" + new String(Character.toChars(0x10400))).isMixedCase());
        assertFalse(asString(new String(Character.toChars(0x10400))).isMixedCase());
    }
}
