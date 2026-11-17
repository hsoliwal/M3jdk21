/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-text 1.15.0's own StringEscapeUtils tests for this
 *          family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 *          through port_markup_donor.py
 * @run junit StringEscapeMarkupFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-text 1.15.0 (Apache-2.0) test sources by
 * port_markup_donor.py (StringEscapeUtils. read as StringUtils.); skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringEscapeMarkupFamilyDonorTest {

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
    void stringEscapeUtilsTest_testEscapeHtml3_row1() {
        assertEquals("plain text", asString("plain text").escapeHtml4(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row2() {
        assertEquals("plain text", asString("plain text").escapeHtml4(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row3() {
        assertEquals("", asString("").escapeHtml4(), "empty string");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row5() {
        assertEquals("bread &amp; butter", asString("bread & butter").escapeHtml4(), "ampersand");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row6() {
        assertEquals("&quot;bread&quot; &amp; butter", asString("\"bread\" & butter").escapeHtml4(),
                "quotes");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row7() {
        assertEquals("greater than &gt;", asString("greater than >").escapeHtml4(),
                "final character only");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row8() {
        assertEquals("&lt; less than", asString("< less than").escapeHtml4(),
                "first character only");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row9() {
        assertEquals("Huntington's chorea", asString("Huntington's chorea").escapeHtml4(),
                "apostrophe");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row10() {
        assertEquals("English,Fran&ccedil;ais,\u65E5\u672C\u8A9E (" + "nihongo)", asString(
                "English,Fran\u00E7ais,\u65E5\u672C\u8A9E (ni" + "hongo)").escapeHtml4(),
                "languages");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml3_row11() {
        assertEquals("\u0080\u009F", asString("\u0080\u009F").escapeHtml4(),
                "8-bit ascii shouldn't number-escape");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row1() {
        assertEquals("plain text", asString("plain text").escapeHtml4(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row2() {
        assertEquals("plain text", asString("plain text").escapeHtml4(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row3() {
        assertEquals("", asString("").escapeHtml4(), "empty string");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row5() {
        assertEquals("bread &amp; butter", asString("bread & butter").escapeHtml4(), "ampersand");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row6() {
        assertEquals("&quot;bread&quot; &amp; butter", asString("\"bread\" & butter").escapeHtml4(),
                "quotes");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row7() {
        assertEquals("greater than &gt;", asString("greater than >").escapeHtml4(),
                "final character only");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row8() {
        assertEquals("&lt; less than", asString("< less than").escapeHtml4(),
                "first character only");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row9() {
        assertEquals("Huntington's chorea", asString("Huntington's chorea").escapeHtml4(),
                "apostrophe");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row10() {
        assertEquals("English,Fran&ccedil;ais,\u65E5\u672C\u8A9E (" + "nihongo)", asString(
                "English,Fran\u00E7ais,\u65E5\u672C\u8A9E (ni" + "hongo)").escapeHtml4(),
                "languages");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtml4_row11() {
        assertEquals("\u0080\u009F", asString("\u0080\u009F").escapeHtml4(),
                "8-bit ascii shouldn't number-escape");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtmlThree() {
        assertEquals("a", asString("a").escapeHtml3());
        assertEquals("&lt;b&gt;a", asString("<b>a").escapeHtml3());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeHtmlVersions() {
        assertEquals("&Beta;", asString("\u0392").escapeHtml4());
        assertEquals("\u0392", asString("&Beta;").unescapeHtml4());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeXml10() {
        assertEquals("a&lt;b&gt;c&quot;d&apos;e&amp;f", asString("a<b>c\"d'e&f").escapeXml10());
        assertEquals("a\tb\rc\nd", asString("a\tb\rc\nd").escapeXml10(),
                "XML 1.0 should not escape \t \n \r");
        assertEquals("ab", asString("a\u0000\u0001\u0008\u000b\u000c\u000e\u001fb").escapeXml10(),
                "XML 1.0 should omit most #x0-x8 | #xb | #xc " + "| #xe-#x19");
        assertEquals("a\ud7ff  \ue000b", asString("a\ud7ff\ud800 \udfff \ue000b").escapeXml10(),
                "XML 1.0 should omit #xd800-#xdfff");
        assertEquals("a\ufffdb", asString("a\ufffd\ufffe\uffffb").escapeXml10(),
                "XML 1.0 should omit #xfffe | #xffff");
        assertEquals("a\u007e&#127;&#132;\u0085&#134;&#159;\u00a0b", asString(
                "a\u007e\u007f\u0084\u0085\u0086\u009f\u00a0b").escapeXml10(),
                "XML 1.0 should escape #x7f-#x84 | #x86 - #x9" + "f, for XML 1.1 compatibility");
    }

    @Test
    void stringEscapeUtilsTest_testEscapeXml11() {
        assertEquals("a&lt;b&gt;c&quot;d&apos;e&amp;f", asString("a<b>c\"d'e&f").escapeXml11());
        assertEquals("a\tb\rc\nd", asString("a\tb\rc\nd").escapeXml11(),
                "XML 1.1 should not escape \t \n \r");
        assertEquals("ab", asString("a\u0000b").escapeXml11(), "XML 1.1 should omit #x0");
        assertEquals("a&#1;&#8;&#11;&#12;&#14;&#31;b", asString(
                "a\u0001\u0008\u000b\u000c\u000e\u001fb").escapeXml11(),
                "XML 1.1 should escape #x1-x8 | #xb | #xc | #" + "xe-#x19");
        assertEquals("a\u007e&#127;&#132;\u0085&#134;&#159;\u00a0b", asString(
                "a\u007e\u007f\u0084\u0085\u0086\u009f\u00a0b").escapeXml11(),
                "XML 1.1 should escape #x7F-#x84 | #x86-#x9F");
        assertEquals("a\ud7ff  \ue000b", asString("a\ud7ff\ud800 \udfff \ue000b").escapeXml11(),
                "XML 1.1 should omit #xd800-#xdfff");
        assertEquals("a\ufffdb", asString("a\ufffd\ufffe\uffffb").escapeXml11(),
                "XML 1.1 should omit #xfffe | #xffff");
    }

    @Test
    void stringEscapeUtilsTest_testLang313() {
        assertEquals("& &", asString("& &amp;").unescapeHtml4());
    }

    @Test
    void stringEscapeUtilsTest_testStandaloneAmphersand() {
        assertEquals("<P&O>", asString("&lt;P&O&gt;").unescapeHtml4());
        assertEquals("test & <", asString("test & &lt;").unescapeHtml4());
        assertEquals("<P&O>", asString("&lt;P&O&gt;").unescapeXml());
        assertEquals("test & <", asString("test & &lt;").unescapeXml());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHexCharsHtml() {
        assertEquals("\u0080\u009F", asString("&#x80;&#x9F;").unescapeHtml4(),
                "hex number unescape");
        assertEquals("\u0080\u009F", asString("&#X80;&#X9F;").unescapeHtml4(),
                "hex number unescape");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3() {
        assertEquals("Fran\u00E7ais", asString("Fran\u00E7ais").unescapeHtml3(),
                "funny chars pass through OK");
        assertEquals("Hello&;World", asString("Hello&;World").unescapeHtml3());
        assertEquals("Hello&#;World", asString("Hello&#;World").unescapeHtml3());
        assertEquals("Hello&# ;World", asString("Hello&# ;World").unescapeHtml3());
        assertEquals("Hello&##;World", asString("Hello&##;World").unescapeHtml3());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row1() {
        assertEquals("plain text", asString("plain text").unescapeHtml3(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row2() {
        assertEquals("plain text", asString("plain text").unescapeHtml3(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row3() {
        assertEquals("", asString("").unescapeHtml3(), "empty string");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row5() {
        assertEquals("bread & butter", asString("bread &amp; butter").unescapeHtml3(), "ampersand");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row6() {
        assertEquals("\"bread\" & butter", asString("&quot;bread&quot; &amp; butter").unescapeHtml3(
                ), "quotes");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row7() {
        assertEquals("greater than >", asString("greater than &gt;").unescapeHtml3(),
                "final character only");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row8() {
        assertEquals("< less than", asString("&lt; less than").unescapeHtml3(),
                "first character only");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row9() {
        assertEquals("Huntington's chorea", asString("Huntington's chorea").unescapeHtml3(),
                "apostrophe");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row10() {
        assertEquals("English,Fran\u00E7ais,\u65E5\u672C\u8A9E (ni" + "hongo)", asString(
                "English,Fran&ccedil;ais,\u65E5\u672C\u8A9E (" + "nihongo)").unescapeHtml3(),
                "languages");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml3_row11() {
        assertEquals("\u0080\u009F", asString("\u0080\u009F").unescapeHtml3(),
                "8-bit ascii shouldn't number-escape");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4() {
        assertEquals("Fran\u00E7ais", asString("Fran\u00E7ais").unescapeHtml4(),
                "funny chars pass through OK");
        assertEquals("Hello&;World", asString("Hello&;World").unescapeHtml4());
        assertEquals("Hello&#;World", asString("Hello&#;World").unescapeHtml4());
        assertEquals("Hello&# ;World", asString("Hello&# ;World").unescapeHtml4());
        assertEquals("Hello&##;World", asString("Hello&##;World").unescapeHtml4());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row1() {
        assertEquals("plain text", asString("plain text").unescapeHtml4(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row2() {
        assertEquals("plain text", asString("plain text").unescapeHtml4(), "no escaping");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row3() {
        assertEquals("", asString("").unescapeHtml4(), "empty string");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row5() {
        assertEquals("bread & butter", asString("bread &amp; butter").unescapeHtml4(), "ampersand");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row6() {
        assertEquals("\"bread\" & butter", asString("&quot;bread&quot; &amp; butter").unescapeHtml4(
                ), "quotes");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row7() {
        assertEquals("greater than >", asString("greater than &gt;").unescapeHtml4(),
                "final character only");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row8() {
        assertEquals("< less than", asString("&lt; less than").unescapeHtml4(),
                "first character only");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row9() {
        assertEquals("Huntington's chorea", asString("Huntington's chorea").unescapeHtml4(),
                "apostrophe");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row10() {
        assertEquals("English,Fran\u00E7ais,\u65E5\u672C\u8A9E (ni" + "hongo)", asString(
                "English,Fran&ccedil;ais,\u65E5\u672C\u8A9E (" + "nihongo)").unescapeHtml4(),
                "languages");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeHtml4_row11() {
        assertEquals("\u0080\u009F", asString("\u0080\u009F").unescapeHtml4(),
                "8-bit ascii shouldn't number-escape");
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeUnknownEntity() {
        assertEquals("&zzzz;", asString("&zzzz;").unescapeHtml4());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeXmlSupplementaryCharacters() {
        assertEquals("\uD84C\uDFB4", asString("&#144308;").unescapeXml(),
                "Supplementary character must be represented " + "using a single escape");
        assertEquals("a b c \uD84C\uDFB4", asString("a b c &#144308;").unescapeXml(),
                "Supplementary characters mixed with basic ch"
                + "aracters should be decoded correctly");
    }
}
