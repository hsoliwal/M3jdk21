/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringEdgeFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringEdgeFamilyDonorTest {
    private static final String FOO = "foo";
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
    void stringUtilsTrimTest_testTrimToEmpty() {
        assertEquals(FOO, String.trimToEmpty(FOO + "  "));
        assertEquals(FOO, String.trimToEmpty(" " + FOO + "  "));
        assertEquals(FOO, String.trimToEmpty(" " + FOO));
        assertEquals(FOO, String.trimToEmpty(FOO + ""));
        assertEquals("", String.trimToEmpty(" \t\r\n\b "));
        assertEquals("", String.trimToEmpty(TRIMMABLE));
        assertEquals(NON_TRIMMABLE, String.trimToEmpty(NON_TRIMMABLE));
        assertEquals("", String.trimToEmpty(""));
        assertEquals("", String.trimToEmpty(null));
    }

    @Test
    void stringUtilsTrimTest_testTrimToNull() {
        assertEquals(FOO, String.trimToNull(FOO + "  "));
        assertEquals(FOO, String.trimToNull(" " + FOO + "  "));
        assertEquals(FOO, String.trimToNull(" " + FOO));
        assertEquals(FOO, String.trimToNull(FOO + ""));
        assertNull(String.trimToNull(" \t\r\n\b "));
        assertNull(String.trimToNull(TRIMMABLE));
        assertEquals(NON_TRIMMABLE, String.trimToNull(NON_TRIMMABLE));
        assertNull(String.trimToNull(""));
        assertNull(String.trimToNull(null));
    }

    @Test
    void stringUtilsStripTest_testStripAccents() {
        final String cue = "\u00C7\u00FA\u00EA";
        assertEquals("Cue", asString(cue).stripAccents(), "Failed to strip accents from " + cue);
        final String lots = "\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C7"
                + "\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE" + "\u00CF\u00D1\u00D2\u00D3"
                + "\u00D4\u00D5\u00D6\u00D9\u00DA\u00DB\u00DC" + "\u00DD";
        assertEquals("AAAAAACEEEEIIIINOOOOOUUUUY", asString(lots).stripAccents(),
                "Failed to strip accents from " + lots);
        assertEquals("", asString("").stripAccents(), "Failed empty String");
        assertEquals("control", asString("control").stripAccents(),
                "Failed to handle non-accented text");
        assertEquals("eclair", asString("\u00E9clair").stripAccents(),
                "Failed to handle easy example");
        assertEquals("ALOSZZCND aloszzcnd", asString("\u0104\u0141\u00D3\u015A\u017B\u0179\u0106"
                + "\u0143\u0110 \u0105\u0142\u00F3\u015B\u017C"
                + "\u017A\u0107\u0144\u0111").stripAccents());
        assertEquals("The cafe\u2019s pinata gave me deja vu.", asString(
                "The caf\u00e9\u2019s pi\u00f1ata gave me d" + "\u00e9j\u00e0 vu.").stripAccents(),
                "Failed to handle accented text");
        assertEquals("fluid quest", asString("\ufb02uid que\ufb06").stripAccents(),
                "Failed to handle ligatures");
        assertEquals("a b c 1 2 3", asString(
                "\u1d43 \u1d47 \u1d9c \u00b9 \u00b2 \u00b3").stripAccents(),
                "Failed to handle superscript text");
        assertEquals("math italic", asString("\uD835\uDC5A\uD835\uDC4E\uD835\uDC61\u210E "
                + "\uD835\uDC56\uD835\uDC61\uD835\uDC4E\uD835"
                + "\uDC59\uD835\uDC56\uD835\uDC50").stripAccents(),
                "Failed to handle UTF32 example");
        assertEquals("\uD83D\uDF01 \uD83D\uDF02 \uD83D\uDF03 " + "\uD83D\uDF04", asString(
                "\uD83D\uDF01 \uD83D\uDF02 \uD83D\uDF03 " + "\uD83D\uDF04").stripAccents(),
                "Failed to handle non-accented text");
    }

    @Test
    void stringUtilsStripTest_testStripAccentsBengali() {
        assertEquals("\u09CB", asString("\u09CB").stripAccents());
        assertEquals("\u09CB", asString("\u09C7\u09BE").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsIWithBar() {
        assertEquals("I i I i I", asString("\u0197 \u0268 \u1D7B \u1DA4 \u1DA7").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsJapanese() {
        assertEquals("\u30AC", asString("\u30AC").stripAccents());
        assertEquals("\u30AC", asString("\u30AB\u3099").stripAccents());
        assertEquals("\u30AC", asString("\uFF76\uFF9E").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsKorean() {
        final String input = "\uC78A\uC9C0\uB9C8 \uB10C \uD750\uB9B0 "
                + "\uC5B4\uB460\uC0AC\uC774 \uC67C\uC190\uC73C"
                + "\uB85C \uADF8\uB9B0 \uBCC4 \uD558\uB098";
        assertEquals(input, asString(input).stripAccents(), "Failed to handle Korean text");
    }

    @Test
    void stringUtilsStripTest_testStripAccentsKoreanJamo() {
        assertEquals("\uAC00", asString("\u1100\u1161").stripAccents());
        assertEquals("\uAC01", asString("\u1100\u1161\u11A8").stripAccents());
        assertEquals("\uAC00", asString("\u3131\u314F").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsKoreanWithLatinAccents() {
        final String expected = "\uAC00 \uAC01 cafe deja vu";
        assertEquals(expected, asString("\uAC00 \uAC01 caf\u00E9 d\u00E9j\u00E0 vu").stripAccents(
                ));
        assertEquals(expected, asString("\u1100\u1161 \u1100\u1161\u11A8 cafe\u0301 d"
                + "e\u0301ja\u0300 vu").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsSymbolMath() {
        final String lt = "<";
        assertEquals(lt, asString(lt).stripAccents());
        assertEquals(lt, asString("\uFE64").stripAccents());
        assertEquals(lt, asString("\uFF1C").stripAccents());
        assertEquals(lt, asString("\u226E").stripAccents());
        final String gt = ">";
        assertEquals(gt, asString(gt).stripAccents());
        assertEquals(gt, asString("\uFE65").stripAccents());
        assertEquals(gt, asString("\uFE65").stripAccents());
        assertEquals(gt, asString("\u226F").stripAccents());
        assertEquals("</script>", asString("\uFF1C\uFF0Fscript\uFF1E").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsTWithStroke() {
        assertEquals("T t", asString("\u0166 \u0167").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsUnicodeVulgarFractions() {
        assertEquals("1\u20444", asString("\u00BC").stripAccents());
        assertEquals("1\u20442", asString("\u00BD").stripAccents());
        assertEquals("3\u20444", asString("\u00BE").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAccentsUWithBar() {
        assertEquals("U u U u", asString("\u0244 \u0289 \u1D7E \u1DB6").stripAccents());
    }

    @Test
    void stringUtilsStripTest_testStripAll() {
        assertNull(String.stripAll((String[]) null));
        final String[] empty = {};
        assertArrayEquals(empty, String.stripAll());
        assertArrayEquals(new String[] { null }, String.stripAll((String) null));
        assertArrayEquals(empty, String.stripAll(empty));
        final String[] foo = { FOO, FOO, FOO };
        final String[] fooSpace = { "  " + FOO + "  ", "  " + FOO, FOO + "  " };
        assertArrayEquals(foo, String.stripAll(fooSpace));
        assertNull(String.stripAll(null, null));
        assertArrayEquals(foo, String.stripAll(fooSpace, null));
        final String[] fooDots = { ".." + FOO + "..", ".." + FOO, FOO + ".." };
        assertArrayEquals(foo, String.stripAll(fooDots, "."));
    }

    @Test
    void stringUtilsStripTest_testStripEndStringString() {
        assertThrows(NullPointerException.class, () -> asString("").stripEnd(null));
        assertThrows(NullPointerException.class, () -> asString("        ").stripEnd(null));
        assertThrows(NullPointerException.class, () -> asString("  abc  ").stripEnd(null));
        assertThrows(NullPointerException.class, () -> asString(WHITESPACE + NON_WHITESPACE
                + WHITESPACE).stripEnd(null));
        assertEquals("", asString("").stripEnd(""));
        assertEquals("        ", asString("        ").stripEnd(""));
        assertEquals("  abc  ", asString("  abc  ").stripEnd(""));
        assertEquals(WHITESPACE, asString(WHITESPACE).stripEnd(""));
        assertEquals("", asString("").stripEnd(" "));
        assertEquals("", asString("        ").stripEnd(" "));
        assertEquals("  abc", asString("  abc  ").stripEnd(" "));
        assertEquals("", asString("").stripEnd("ab"));
        assertEquals("        ", asString("        ").stripEnd("ab"));
        assertEquals("  abc  ", asString("  abc  ").stripEnd("ab"));
        assertEquals("abc", asString("abcabab").stripEnd("ab"));
        assertEquals(WHITESPACE, asString(WHITESPACE).stripEnd(""));
    }

    @Test
    void stringUtilsStripTest_testStripStartStringString() {
        assertThrows(NullPointerException.class, () -> asString("").stripStart(null));
        assertThrows(NullPointerException.class, () -> asString("        ").stripStart(null));
        assertThrows(NullPointerException.class, () -> asString("  abc  ").stripStart(null));
        assertThrows(NullPointerException.class, () -> asString(WHITESPACE + NON_WHITESPACE
                + WHITESPACE).stripStart(null));
        assertEquals("", asString("").stripStart(""));
        assertEquals("        ", asString("        ").stripStart(""));
        assertEquals("  abc  ", asString("  abc  ").stripStart(""));
        assertEquals(WHITESPACE, asString(WHITESPACE).stripStart(""));
        assertEquals("", asString("").stripStart(" "));
        assertEquals("", asString("        ").stripStart(" "));
        assertEquals("abc  ", asString("  abc  ").stripStart(" "));
        assertEquals("", asString("").stripStart("ab"));
        assertEquals("        ", asString("        ").stripStart("ab"));
        assertEquals("  abc  ", asString("  abc  ").stripStart("ab"));
        assertEquals("cabab", asString("abcabab").stripStart("ab"));
        assertEquals(WHITESPACE, asString(WHITESPACE).stripStart(""));
    }

    @Test
    void stringUtilsStripTest_testStripString() {
        assertEquals("", asString("").strip());
        assertEquals("", asString("        ").strip());
        assertEquals("abc", asString("  abc  ").strip());
        assertEquals(NON_WHITESPACE, asString(WHITESPACE + NON_WHITESPACE + WHITESPACE).strip());
    }

    @Test
    void stringUtilsStripTest_testStripStringString() {
        assertThrows(NullPointerException.class, () -> asString("").strip(null));
        assertThrows(NullPointerException.class, () -> asString("        ").strip(null));
        assertThrows(NullPointerException.class, () -> asString("  abc  ").strip(null));
        assertThrows(NullPointerException.class, () -> asString(WHITESPACE + NON_WHITESPACE
                + WHITESPACE).strip(null));
        assertEquals("", asString("").strip(""));
        assertEquals("        ", asString("        ").strip(""));
        assertEquals("  abc  ", asString("  abc  ").strip(""));
        assertEquals(WHITESPACE, asString(WHITESPACE).strip(""));
        assertEquals("", asString("").strip(" "));
        assertEquals("", asString("        ").strip(" "));
        assertEquals("abc", asString("  abc  ").strip(" "));
        assertEquals("", asString("").strip("ab"));
        assertEquals("        ", asString("        ").strip("ab"));
        assertEquals("  abc  ", asString("  abc  ").strip("ab"));
        assertEquals("c", asString("abcabab").strip("ab"));
        assertEquals(WHITESPACE, asString(WHITESPACE).strip(""));
    }

    @Test
    void stringUtilsStripTest_testStripSupplementaryCodePoints() {
        final String mathA = "\uD835\uDD1E";
        final String mathB = "\uD835\uDD1F";
        assertEquals(mathA + "abc", asString(mathA + "abc").stripStart(mathB));
        final String clef = "\uD834\uDD1E";
        assertEquals("abc" + clef, asString("abc" + clef).stripEnd(mathA));
        assertEquals(mathA + "abc" + mathA, asString(mathA + "abc" + mathA).strip(mathB));
        assertEquals("abc", asString(mathA + mathA + "abc").stripStart(mathA));
        assertEquals("abc", asString("abc" + mathA + mathA).stripEnd(mathA));
        assertEquals("abc", asString(mathA + "abc" + mathA).strip(mathA));
    }

    @Test
    void stringUtilsStripTest_testStripToEmptyString() {
        assertEquals("", String.stripToEmpty(null));
        assertEquals("", String.stripToEmpty(""));
        assertEquals("", String.stripToEmpty("        "));
        assertEquals("", String.stripToEmpty(WHITESPACE));
        assertEquals("ab c", String.stripToEmpty("  ab c  "));
        assertEquals(NON_WHITESPACE, String.stripToEmpty(WHITESPACE + NON_WHITESPACE + WHITESPACE));
    }

    @Test
    void stringUtilsStripTest_testStripToNullString() {
        assertNull(String.stripToNull(null));
        assertNull(String.stripToNull(""));
        assertNull(String.stripToNull("        "));
        assertNull(String.stripToNull(WHITESPACE));
        assertEquals("ab c", String.stripToNull("  ab c  "));
        assertEquals(NON_WHITESPACE, String.stripToNull(WHITESPACE + NON_WHITESPACE + WHITESPACE));
    }

    @Test
    void stringUtilsEmptyBlankTest_testFirstNonBlank() {
        assertNull(String.firstNonBlank());
        assertNull(String.firstNonBlank((String[]) null));
        assertNull(String.firstNonBlank(null, null, null));
        assertNull(String.firstNonBlank(null, "", " "));
        assertNull(String.firstNonBlank(null, null, " "));
        assertEquals("a", String.firstNonBlank(null, "a"));
        assertEquals("zz", String.firstNonBlank(null, "zz"));
        assertEquals("abc", String.firstNonBlank("abc"));
        assertEquals("xyz", String.firstNonBlank(null, "xyz"));
        assertEquals("xyz", String.firstNonBlank(null, "xyz", "abc"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testFirstNonEmpty() {
        assertNull(String.firstNonEmpty());
        assertNull(String.firstNonEmpty((String[]) null));
        assertNull(String.firstNonEmpty(null, null, null));
        assertEquals(" ", String.firstNonEmpty(null, "", " "));
        assertNull(String.firstNonEmpty(null, null, ""));
        assertEquals("a", String.firstNonEmpty(null, "a"));
        assertEquals("zz", String.firstNonEmpty(null, "zz"));
        assertEquals("abc", String.firstNonEmpty("abc"));
        assertEquals("xyz", String.firstNonEmpty(null, "xyz"));
        assertEquals("xyz", String.firstNonEmpty(null, "xyz", "abc"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsAllBlank() {
        assertTrue(String.isAllBlank((String) null));
        assertTrue(String.isAllBlank((String[]) null));
        assertTrue(String.isAllBlank(null, null));
        assertTrue(String.isAllBlank(null, " "));
        assertFalse(String.isAllBlank(null, "foo"));
        assertFalse(String.isAllBlank(null, "a"));
        assertFalse(String.isAllBlank("", "bar"));
        assertFalse(String.isAllBlank("bob", ""));
        assertFalse(String.isAllBlank("  bob  ", null));
        assertFalse(String.isAllBlank(" ", "bar"));
        assertFalse(String.isAllBlank("foo", "bar"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsAllEmpty() {
        assertTrue(String.isAllEmpty());
        assertTrue(String.isAllEmpty());
        assertTrue(String.isAllEmpty((String) null));
        assertTrue(String.isAllEmpty((String[]) null));
        assertFalse(String.isAllEmpty(null, "foo"));
        assertFalse(String.isAllEmpty(null, "a"));
        assertFalse(String.isAllEmpty("", "bar"));
        assertFalse(String.isAllEmpty("bob", ""));
        assertFalse(String.isAllEmpty("  bob  ", null));
        assertFalse(String.isAllEmpty(" ", "bar"));
        assertFalse(String.isAllEmpty("foo", "bar"));
        assertTrue(String.isAllEmpty("", null));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsAnyBlank() {
        assertTrue(String.isAnyBlank((String) null));
        assertFalse(String.isAnyBlank((String[]) null));
        assertTrue(String.isAnyBlank(null, "foo"));
        assertTrue(String.isAnyBlank(null, null));
        assertTrue(String.isAnyBlank("", "bar"));
        assertTrue(String.isAnyBlank("bob", ""));
        assertTrue(String.isAnyBlank("  bob  ", null));
        assertTrue(String.isAnyBlank(" ", "bar"));
        assertFalse(String.isAnyBlank("foo", "bar"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsAnyEmpty() {
        assertTrue(String.isAnyEmpty((String) null));
        assertFalse(String.isAnyEmpty((String[]) null));
        assertTrue(String.isAnyEmpty(null, "foo"));
        assertTrue(String.isAnyEmpty(null, "a"));
        assertTrue(String.isAnyEmpty("", "bar"));
        assertTrue(String.isAnyEmpty("bob", ""));
        assertTrue(String.isAnyEmpty("  bob  ", null));
        assertFalse(String.isAnyEmpty(" ", "bar"));
        assertFalse(String.isAnyEmpty("foo", "bar"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsNoneBlank() {
        assertFalse(String.isNoneBlank((String) null));
        assertTrue(String.isNoneBlank((String[]) null));
        assertFalse(String.isNoneBlank(null, "a"));
        assertFalse(String.isNoneBlank(null, "foo"));
        assertFalse(String.isNoneBlank(null, null));
        assertFalse(String.isNoneBlank("", "bar"));
        assertFalse(String.isNoneBlank("a", ""));
        assertFalse(String.isNoneBlank("bob", ""));
        assertFalse(String.isNoneBlank("  bob  ", null));
        assertFalse(String.isNoneBlank(" ", "bar"));
        assertTrue(String.isNoneBlank("foo", "bar"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsNoneEmpty() {
        assertFalse(String.isNoneEmpty((String) null));
        assertTrue(String.isNoneEmpty((String[]) null));
        assertFalse(String.isNoneEmpty(null, "a"));
        assertFalse(String.isNoneEmpty(null, "foo"));
        assertFalse(String.isNoneEmpty("", "bar"));
        assertFalse(String.isNoneEmpty("bob", ""));
        assertFalse(String.isNoneEmpty("a", ""));
        assertFalse(String.isNoneEmpty("  bob  ", null));
        assertTrue(String.isNoneEmpty(" ", "bar"));
        assertTrue(String.isNoneEmpty("foo", "bar"));
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsNotBlank() {
        assertFalse(asString("").isNotBlank());
        assertFalse(asString(WHITESPACE).isNotBlank());
        assertTrue(asString("a").isNotBlank());
        assertTrue(asString("foo").isNotBlank());
        assertTrue(asString("  foo  ").isNotBlank());
    }

    @Test
    void stringUtilsEmptyBlankTest_testIsNotEmpty() {
        assertFalse(asString("").isNotEmpty());
        assertTrue(asString(" ").isNotEmpty());
        assertTrue(asString("a").isNotEmpty());
        assertTrue(asString("foo").isNotEmpty());
        assertTrue(asString("  foo  ").isNotEmpty());
    }


    @Test
    void stringUtilsTest_testDefault_String() {
        assertEquals("", String.defaultString(null));
        assertEquals("", String.defaultString(""));
        assertEquals("abc", String.defaultString("abc"));
    }

    @Test
    void stringUtilsTest_testDefault_StringString() {
        assertEquals("NULL", String.defaultString(null, "NULL"));
        assertEquals("", String.defaultString("", "NULL"));
        assertEquals("abc", String.defaultString("abc", "NULL"));
    }

    @Test
    void stringUtilsTest_testDefaultIfBlank_CharBuffers() {
        assertEquals("NULL", String.defaultIfBlank(CharBuffer.wrap(""), CharBuffer.wrap(
                "NULL")).toString());
        assertEquals("NULL", String.defaultIfBlank(CharBuffer.wrap(" "), CharBuffer.wrap(
                "NULL")).toString());
        assertEquals("abc", String.defaultIfBlank(CharBuffer.wrap("abc"), CharBuffer.wrap(
                "NULL")).toString());
        assertNull(String.defaultIfBlank(CharBuffer.wrap(""), (CharBuffer) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfBlank_StringBuffers() {
        assertEquals("NULL", String.defaultIfBlank(new StringBuffer(""), new StringBuffer(
                "NULL")).toString());
        assertEquals("NULL", String.defaultIfBlank(new StringBuffer(" "), new StringBuffer(
                "NULL")).toString());
        assertEquals("abc", String.defaultIfBlank(new StringBuffer("abc"), new StringBuffer(
                "NULL")).toString());
        assertNull(String.defaultIfBlank(new StringBuffer(""), (StringBuffer) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfBlank_StringBuilders() {
        assertEquals("NULL", String.defaultIfBlank(new StringBuilder(""), new StringBuilder(
                "NULL")).toString());
        assertEquals("NULL", String.defaultIfBlank(new StringBuilder(" "), new StringBuilder(
                "NULL")).toString());
        assertEquals("abc", String.defaultIfBlank(new StringBuilder("abc"), new StringBuilder(
                "NULL")).toString());
        assertNull(String.defaultIfBlank(new StringBuilder(""), (StringBuilder) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfBlank_StringString() {
        assertEquals("NULL", String.defaultIfBlank(null, "NULL"));
        assertEquals("NULL", String.defaultIfBlank("", "NULL"));
        assertEquals("NULL", String.defaultIfBlank(" ", "NULL"));
        assertEquals("abc", String.defaultIfBlank("abc", "NULL"));
        assertNull(String.defaultIfBlank("", (String) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfEmpty_CharBuffers() {
        assertEquals("NULL", String.defaultIfEmpty(CharBuffer.wrap(""), CharBuffer.wrap(
                "NULL")).toString());
        assertEquals("abc", String.defaultIfEmpty(CharBuffer.wrap("abc"), CharBuffer.wrap(
                "NULL")).toString());
        assertNull(String.defaultIfEmpty(CharBuffer.wrap(""), (CharBuffer) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfEmpty_StringBuffers() {
        assertEquals("NULL", String.defaultIfEmpty(new StringBuffer(""), new StringBuffer(
                "NULL")).toString());
        assertEquals("abc", String.defaultIfEmpty(new StringBuffer("abc"), new StringBuffer(
                "NULL")).toString());
        assertNull(String.defaultIfEmpty(new StringBuffer(""), (StringBuffer) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfEmpty_StringBuilders() {
        assertEquals("NULL", String.defaultIfEmpty(new StringBuilder(""), new StringBuilder(
                "NULL")).toString());
        assertEquals("abc", String.defaultIfEmpty(new StringBuilder("abc"), new StringBuilder(
                "NULL")).toString());
        assertNull(String.defaultIfEmpty(new StringBuilder(""), (StringBuilder) null));
    }

    @Test
    void stringUtilsTest_testDefaultIfEmpty_StringString() {
        assertEquals("NULL", String.defaultIfEmpty(null, "NULL"));
        assertEquals("NULL", String.defaultIfEmpty("", "NULL"));
        assertEquals("abc", String.defaultIfEmpty("abc", "NULL"));
        assertNull(String.getIfEmpty("", null));
    }

    @Test
    void stringUtilsTest_testDeleteWhitespace_String() {
        assertEquals("", asString("").deleteWhitespace());
        assertEquals("", asString("  \u000C  \t\t\u001F\n\n \u000B  ").deleteWhitespace());
        assertEquals("", asString(WHITESPACE).deleteWhitespace());
        assertEquals(NON_WHITESPACE, asString(NON_WHITESPACE).deleteWhitespace());
        assertEquals("\u00A0\u202F", asString("  \u00A0  \t\t\n\n \u202F  ").deleteWhitespace());
        assertEquals("\u00A0\u202F", asString("\u00A0\u202F").deleteWhitespace());
        assertEquals("test", asString("\u000Bt  \t\n\u0009e\rs\n\n   \tt").deleteWhitespace());
    }

    @Test
    void stringUtilsTest_testGetIfBlank_StringStringSupplier() {
        assertEquals("NULL", String.getIfBlank(null, () -> "NULL"));
        assertEquals("NULL", String.getIfBlank("", () -> "NULL"));
        assertEquals("NULL", String.getIfBlank(" ", () -> "NULL"));
        assertEquals("abc", String.getIfBlank("abc", () -> "NULL"));
        assertNull(String.getIfBlank("", (() -> null)));
        assertNull(String.defaultIfBlank("", (String) null));
    }

    @Test
    void stringUtilsTest_testGetIfEmpty_StringStringSupplier() {
        assertEquals("NULL", String.getIfEmpty((String) null, () -> "NULL"));
        assertEquals("NULL", String.getIfEmpty("", () -> "NULL"));
        assertEquals("abc", String.getIfEmpty("abc", () -> "NULL"));
        assertNull(String.getIfEmpty("", (() -> null)));
        assertNull(String.defaultIfEmpty("", (String) null));
    }

    @Test
    void stringUtilsTest_testLANG666() {
        assertEquals("12", asString("120.00").stripEnd(".0"));
        assertEquals("121", asString("121.00").stripEnd(".0"));
    }

    @Test
    void stringUtilsTest_testNormalizeSpace() {
        assertEquals("", asString("").normalizeSpace());
        assertEquals("", asString(" ").normalizeSpace());
        assertEquals("", asString("\t").normalizeSpace());
        assertEquals("", asString("\n").normalizeSpace());
        assertEquals("", asString("\u0009").normalizeSpace());
        assertEquals("", asString("\u000B").normalizeSpace());
        assertEquals("", asString("\u000C").normalizeSpace());
        assertEquals("", asString("\u001C").normalizeSpace());
        assertEquals("", asString("\u001D").normalizeSpace());
        assertEquals("", asString("\u001E").normalizeSpace());
        assertEquals("", asString("\u001F").normalizeSpace());
        assertEquals("", asString("\f").normalizeSpace());
        assertEquals("", asString("\r").normalizeSpace());
        assertEquals("a", asString("  a  ").normalizeSpace());
        assertEquals("a b c", asString("  a  b   c  ").normalizeSpace());
        assertEquals("a b c", asString("a\t\f\r  b\u000B   c\n").normalizeSpace());
        assertEquals("a   b c", asString("a\t\f\r  " + HARD_SPACE + HARD_SPACE
                + "b\u000B   c\n").normalizeSpace());
        assertEquals("b", asString("\u0000b").normalizeSpace());
        assertEquals("b", asString("b\u0000").normalizeSpace());
    }

    @Test
    void stringUtilsTest_testNormalizeSpaceNonBreakingSpace() {
        assertEquals("", asString("\u00A0").normalizeSpace());
        assertEquals("", asString("\u00A0\u00A0").normalizeSpace());
        assertEquals("a", asString("\u00A0a").normalizeSpace());
        assertEquals("a", asString("a\u00A0").normalizeSpace());
        assertEquals("a", asString(" \u00A0a\u00A0 ").normalizeSpace());
        assertEquals("a b", asString("a\u00A0b").normalizeSpace());
        assertEquals("a  b", asString("a\u00A0\u00A0b").normalizeSpace());
        assertEquals("a  b", asString("a\u00A0 b").normalizeSpace());
        assertEquals("a  b", asString("a \u00A0b").normalizeSpace());
        assertEquals("\u2007a\u2007", asString("\u2007a\u2007").normalizeSpace());
        assertEquals("\u202Fa\u202F", asString("\u202Fa\u202F").normalizeSpace());
    }

    @Test
    void stringUtilsTest_testNormalizeSpaceUnicodeWhitespace() {
        assertEquals("a b", asString("\u2003a\u2003\u2003b\u2003").normalizeSpace());
    }
}
