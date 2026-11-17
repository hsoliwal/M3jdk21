/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-text 1.15.0's own StringEscapeUtils tests for this
 *          family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 *          through port_escape_donor.py
 * @run junit StringEscapeCodeFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-text 1.15.0 (Apache-2.0) test sources by
 * port_escape_donor.py (StringEscapeUtils. read as StringUtils.); skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringEscapeCodeFamilyDonorTest {
    private static final String FOO = "foo";

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
    void stringEscapeUtilsTest_testDeleteCharacter() {
        final String deleteString = "Delete: \u007F";
        assertEquals("Delete: \\u007F", asString(deleteString).escapeJson());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeCsvString() {
        assertEquals("foo.bar", asString("foo.bar").escapeCsv());
        assertEquals("\"foo,bar\"", asString("foo,bar").escapeCsv());
        assertEquals("\"foo\nbar\"", asString("foo\nbar").escapeCsv());
        assertEquals("\"foo\rbar\"", asString("foo\rbar").escapeCsv());
        assertEquals("\"foo\"\"bar\"", asString("foo\"bar").escapeCsv());
        assertEquals("foo\uD84C\uDFB4bar", asString("foo\uD84C\uDFB4bar").escapeCsv());
        assertEquals("", asString("").escapeCsv());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeCsvWriter() {
        assertEquals("foo.bar", asString("foo.bar").escapeCsv());
        assertEquals("\"foo,bar\"", asString("foo,bar").escapeCsv());
        assertEquals("\"foo\nbar\"", asString("foo\nbar").escapeCsv());
        assertEquals("\"foo\rbar\"", asString("foo\rbar").escapeCsv());
        assertEquals("\"foo\"\"bar\"", asString("foo\"bar").escapeCsv());
        assertEquals("foo\uD84C\uDFB4bar", asString("foo\uD84C\uDFB4bar").escapeCsv());
        assertEquals("", asString("").escapeCsv());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeEcmaScript() {
        assertEquals("He didn\\'t say, \\\"stop!\\\"", asString(
                "He didn't say, \"stop!\"").escapeEcmaScript());
        assertEquals("document.getElementById(\\\"test\\\").value "
                + "= \\'<script>alert(\\'aaa\\');<\\/script>\\'" + ";", asString(
                "document.getElementById(\"test\").value = '<"
                + "script>alert('aaa');</script>';").escapeEcmaScript());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeJava() {
        assertEquals("", asString("").escapeJava());
        assertEquals(FOO, asString(FOO).escapeJava());
        assertEquals("\\t", asString("\t").escapeJava());
        assertEquals("\\\\", asString("\\").escapeJava());
        assertEquals("'", asString("'").escapeJava());
        assertEquals("\\\\\\b\\t\\r", asString("\\\b\t\r").escapeJava());
        assertEquals("\\u1234", asString("\u1234").escapeJava());
        assertEquals("\\u0234", asString("\u0234").escapeJava());
        assertEquals("\\u00EF", asString("\u00ef").escapeJava());
        assertEquals("\\u0001", asString("\u0001").escapeJava());
        assertEquals("\\uABCD", asString("\uabcd").escapeJava());
        assertEquals("He didn't say, \\\"stop!\\\"", asString(
                "He didn't say, \"stop!\"").escapeJava());
        assertEquals("This space is non-breaking:\\u00A0", asString(
                "This space is non-breaking:\u00a0").escapeJava());
        assertEquals("\\uABCD\\u1234\\u012C", asString("\uABCD\u1234\u012C").escapeJava());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeJson() {
        assertEquals("He didn't say, \\\"stop!\\\"", asString(
                "He didn't say, \"stop!\"").escapeJson());
        final String expected = "\\\"foo\\\" isn't \\\"bar\\\". specials: \\b\\r\\n\\f\\t\\\\\\/";
        final String input = "\"foo\" isn't \"bar\". specials: \b\r\n\f\t\\/";
        assertEquals(expected, asString(input).escapeJson());
    }

    @Test
    void stringEscapeUtilsTest_testEscapeXSI() {
        assertEquals("He\\ didn\\'t\\ say,\\ \\\"Stop!\\\"", asString(
                "He didn't say, \"Stop!\"").escapeXSI());
        assertEquals("\\\\", asString("\\").escapeXSI());
        assertEquals("", asString("\n").escapeXSI());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeCsvString() {
        assertEquals("foo.bar", asString("foo.bar").unescapeCsv());
        assertEquals("foo,bar", asString("\"foo,bar\"").unescapeCsv());
        assertEquals("foo\nbar", asString("\"foo\nbar\"").unescapeCsv());
        assertEquals("foo\rbar", asString("\"foo\rbar\"").unescapeCsv());
        assertEquals("foo\"bar", asString("\"foo\"\"bar\"").unescapeCsv());
        assertEquals("foo\uD84C\uDFB4bar", asString("foo\uD84C\uDFB4bar").unescapeCsv());
        assertEquals("", asString("").unescapeCsv());
        assertEquals("foo.bar", asString("\"foo.bar\"").unescapeCsv());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeCsvWriter() {
        assertEquals("foo.bar", asString("foo.bar").unescapeCsv());
        assertEquals("foo,bar", asString("\"foo,bar\"").unescapeCsv());
        assertEquals("foo\nbar", asString("\"foo\nbar\"").unescapeCsv());
        assertEquals("foo\rbar", asString("\"foo\rbar\"").unescapeCsv());
        assertEquals("foo\"bar", asString("\"foo\"\"bar\"").unescapeCsv());
        assertEquals("foo\uD84C\uDFB4bar", asString("foo\uD84C\uDFB4bar").unescapeCsv());
        assertEquals("", asString("").unescapeCsv());
        assertEquals("foo.bar", asString("\"foo.bar\"").unescapeCsv());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeEcmaScript() {
        assertEquals("8lvc1u+6B#-I", asString("8lvc1u+6B#-I").unescapeEcmaScript());
        assertEquals("<script src=\"build/main.bundle.js\"></scrip" + "t>", asString(
                "<script src=\"build/main.bundle.js\"></scrip" + "t>").unescapeEcmaScript());
        assertEquals("<script src=\"build/main.bundle.js\"></scrip" + "t>>", asString(
                "<script src=\"build/main.bundle.js\"></scrip" + "t>>").unescapeEcmaScript());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeJava() {
        assertThrows(RuntimeException.class, () -> asString("\\u02-3").unescapeJava());
        assertEquals("", asString("").unescapeJava());
        assertEquals("test", asString("test").unescapeJava());
        assertEquals("\ntest\b", asString("\\ntest\\b").unescapeJava());
        assertEquals("\u123425foo\ntest\b", asString("\\u123425foo\\ntest\\b").unescapeJava());
        assertEquals("'\foo\teste\r", asString("\\'\\foo\\teste\\r").unescapeJava());
        assertEquals("", asString("\\").unescapeJava());
        assertEquals("\uABCDx", asString("\\uabcdx").unescapeJava());
        assertEquals("\uABCDx", asString("\\uABCDx").unescapeJava());
        assertEquals("\uABCD", asString("\\uabcd").unescapeJava());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeJson() {
        assertEquals("", asString("").unescapeJson());
        assertEquals(" ", asString(" ").unescapeJson());
        assertEquals("a:b", asString("a:b").unescapeJson());
        final String jsonString = "{\"age\":100,\"name\":\"kyong.com\n\",\"mess"
                + "ages\":[\"msg 1\",\"msg 2\",\"msg 3\"]}";
        assertEquals(jsonString, asString(jsonString).unescapeJson());
    }

    @Test
    void stringEscapeUtilsTest_testUnescapeJsonDoubleQuoteAndForwardSlash() {
        final String escapedJsonString = "double quote: \\\" and a forward slash: \\/";
        final String jsonString = "double quote: \" and a forward slash: /";
        assertEquals(jsonString, asString(escapedJsonString).unescapeJson());
    }

    @Test
    void stringEscapeUtilsTest_testUnscapeXSI() {
        assertEquals("\"", asString("\\\"").unescapeXSI());
        assertEquals("He didn't say, \"Stop!\"", asString(
                "He\\ didn\\'t\\ say,\\ \\\"Stop!\\\"").unescapeXSI());
        assertEquals("\\", asString("\\\\").unescapeXSI());
        assertEquals("", asString("\\").unescapeXSI());
    }
}
