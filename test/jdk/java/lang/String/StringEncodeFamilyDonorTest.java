/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of Guava 33.7.2's own Utf8 and UrlEscapers tests for this
 *          family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 *          through port_guava_donor.py
 * @run junit StringEncodeFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from Guava 33.7.2 (Apache-2.0) test sources by port_guava_donor.py
 * (Utf8.encodedLength and the path-segment and fragment escapers read as
 * StringUtils.); skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringEncodeFamilyDonorTest {

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
    void utf8Test_testEncodedLength_validStrings() {
        assertEquals(0, asString("").utf8Length());
        assertEquals(11, asString("Hello world").utf8Length());
        assertEquals(8, asString("R\u00e9sum\u00e9").utf8Length());
        assertEquals( 461, asString("\u5a01\u5ec9\u00b7\u838e\u58eb\u6bd4\u4e9e"
                + "\uff08William Shakespeare\uff0c" + "1564\u5e744\u670826\u865f\u20141616\u5e744"
                + "\u670823\u865f[1]\uff09\u4fc2\u96bb\u82f1"
                + "\u570b\u55f0\u6f14\u54e1\u3001\u5287\u4f5c" + "\u5bb6\u540c\u8a69\u4eba\uff0c"
                + "\u6709\u6642\u9593\u4f62\u7c21\u7a31\u838e"
                + "\u7fc1\uff1b\u4e2d\u570b\u6e05\u672b\u6c11"
                + "\u521d\u54c8\u62d5\u7ffb\u8b6f\u505a\u820c"
                + "\u514b\u65af\u6bd5\u3001\u6c99\u65af\u76ae"
                + "\u8033\u3001\u7b5b\u65af\u6bd4\u8033\u3001"
                + "\u838e\u57fa\u65af\u5e87\u5c14\u3001\u7d22"
                + "\u58eb\u6bd4\u5c14\u3001\u590f\u514b\u601d"
                + "\u8298\u5c14\u3001\u5e0c\u54c0\u82e6\u76ae"
                + "\u963f\u3001\u53f6\u65af\u58c1\u3001\u6c99" + "\u514b\u76ae\u5c14\u3001"
                + "\u72f9\u65af\u4e15\u723e\u3002[2]\u838e"
                + "\u58eb\u6bd4\u4e9e\u7de8\u5beb\u904e\u597d"
                + "\u591a\u4f5c\u54c1\uff0c\u4f62\u55f0\u5287"
                + "\u4f5c\u97ff\u897f\u6d0b\u6587\u5b78\u597d" + "\u6709\u5f71\u97ff\uff0c"
                + "\u54c8\u90fd\u62d5\u4eba\u7ffb\u8b6f\u505a"
                + "\u597d\u591a\u8a71\u3002").utf8Length());
        assertEquals(4, asString(new String(new char[] {Character.MIN_HIGH_SURROGATE,
                Character.MIN_LOW_SURROGATE})).utf8Length());
    }

    @Test
    void utf8Test_testEncodedLength_invalidStrings() {
        assertEquals("Unpaired surrogate at index " + 0, assertThrows(
                IllegalArgumentException.class, () -> asString(new String(
                new char[] {Character.MIN_HIGH_SURROGATE})).utf8Length()).getMessage());
        assertEquals("Unpaired surrogate at index " + 6, assertThrows(
                IllegalArgumentException.class, () -> asString("foobar" + new String(
                new char[] {Character.MIN_HIGH_SURROGATE})).utf8Length()).getMessage());
        assertEquals("Unpaired surrogate at index " + 0, assertThrows(
                IllegalArgumentException.class, () -> asString(new String(
                new char[] {Character.MIN_LOW_SURROGATE})).utf8Length()).getMessage());
        assertEquals("Unpaired surrogate at index " + 6, assertThrows(
                IllegalArgumentException.class, () -> asString("foobar" + new String(
                new char[] {Character.MIN_LOW_SURROGATE})).utf8Length()).getMessage());
        assertEquals("Unpaired surrogate at index " + 0, assertThrows(
                IllegalArgumentException.class, () -> asString(new String(
                new char[] {Character.MIN_HIGH_SURROGATE,
                Character.MIN_HIGH_SURROGATE})).utf8Length()).getMessage());
    }

    @Test
    void urlEscaperTesting_pathSegment_assertBasicUrlEscaperExceptPercent() {
        assertEquals(String.valueOf('a'), asString(String.valueOf('a')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('z'), asString(String.valueOf('z')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('A'), asString(String.valueOf('A')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('Z'), asString(String.valueOf('Z')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('0'), asString(String.valueOf('0')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('9'), asString(String.valueOf('9')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('-'), asString(String.valueOf('-')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('_'), asString(String.valueOf('_')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('.'), asString(String.valueOf('.')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('*'), asString(String.valueOf('*')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals("%00", asString(String.valueOf('\u0000')).encodePercent("!$&'()*+,;=:@"));
        assertEquals("%7F", asString(String.valueOf('\u007f')).encodePercent("!$&'()*+,;=:@"));
        assertEquals("%C2%80", asString(String.valueOf('\u0080')).encodePercent("!$&'()*+,;=:@"));
        assertEquals("%DF%BF", asString(String.valueOf('\u07ff')).encodePercent("!$&'()*+,;=:@"));
        assertEquals("%E0%A0%80", asString(String.valueOf('\u0800')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals("%EF%BF%BF", asString(String.valueOf('\uffff')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals("%F0%90%80%80", asString(new String(new char[] {'\uD800',
                '\uDC00'})).encodePercent("!$&'()*+,;=:@"));
        assertEquals("%F4%8F%BF%BF", asString(new String(new char[] {'\uDBFF',
                '\uDFFF'})).encodePercent("!$&'()*+,;=:@"));
        assertEquals("", asString("").encodePercent("!$&'()*+,;=:@"));
        assertEquals("safestring", asString("safestring").encodePercent("!$&'()*+,;=:@"));
        assertEquals("embedded%00null", asString("embedded\0null").encodePercent("!$&'()*+,;=:@"));
        assertEquals("max%EF%BF%BFchar", asString("max\uffffchar").encodePercent("!$&'()*+,;=:@"));
    }

    @Test
    void urlEscaperTesting_pathSegment_assertBasicUrlEscaper() {
        assertEquals("%25", asString(String.valueOf('%')).encodePercent("!$&'()*+,;=:@"));
    }

    @Test
    void urlEscaperTesting_pathSegment_assertPathEscaper() {
        assertEquals(String.valueOf('!'), asString(String.valueOf('!')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('\''), asString(String.valueOf('\'')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('('), asString(String.valueOf('(')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf(')'), asString(String.valueOf(')')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('~'), asString(String.valueOf('~')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf(':'), asString(String.valueOf(':')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals(String.valueOf('@'), asString(String.valueOf('@')).encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals("%20", asString(String.valueOf(' ')).encodePercent("!$&'()*+,;=:@"));
        assertEquals("safe%20with%20spaces", asString("safe with spaces").encodePercent(
                "!$&'()*+,;=:@"));
        assertEquals("foo@bar.com", asString("foo@bar.com").encodePercent("!$&'()*+,;=:@"));
    }

    @Test
    void urlEscaperTesting_fragment_assertBasicUrlEscaperExceptPercent() {
        assertEquals(String.valueOf('a'), asString(String.valueOf('a')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('z'), asString(String.valueOf('z')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('A'), asString(String.valueOf('A')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('Z'), asString(String.valueOf('Z')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('0'), asString(String.valueOf('0')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('9'), asString(String.valueOf('9')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('-'), asString(String.valueOf('-')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('_'), asString(String.valueOf('_')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('.'), asString(String.valueOf('.')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('*'), asString(String.valueOf('*')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals("%00", asString(String.valueOf('\u0000')).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("%7F", asString(String.valueOf('\u007f')).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("%C2%80", asString(String.valueOf('\u0080')).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("%DF%BF", asString(String.valueOf('\u07ff')).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("%E0%A0%80", asString(String.valueOf('\u0800')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals("%EF%BF%BF", asString(String.valueOf('\uffff')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals("%F0%90%80%80", asString(new String(new char[] {'\uD800',
                '\uDC00'})).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("%F4%8F%BF%BF", asString(new String(new char[] {'\uDBFF',
                '\uDFFF'})).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("", asString("").encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("safestring", asString("safestring").encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("embedded%00null", asString("embedded\0null").encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals("max%EF%BF%BFchar", asString("max\uffffchar").encodePercent(
                "!$&'()*+,;=:@/?"));
    }

    @Test
    void urlEscaperTesting_fragment_assertBasicUrlEscaper() {
        assertEquals("%25", asString(String.valueOf('%')).encodePercent("!$&'()*+,;=:@/?"));
    }

    @Test
    void urlEscaperTesting_fragment_assertPathEscaper() {
        assertEquals(String.valueOf('!'), asString(String.valueOf('!')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('\''), asString(String.valueOf('\'')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('('), asString(String.valueOf('(')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf(')'), asString(String.valueOf(')')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('~'), asString(String.valueOf('~')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf(':'), asString(String.valueOf(':')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('@'), asString(String.valueOf('@')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals("%20", asString(String.valueOf(' ')).encodePercent("!$&'()*+,;=:@/?"));
        assertEquals("safe%20with%20spaces", asString("safe with spaces").encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals("foo@bar.com", asString("foo@bar.com").encodePercent("!$&'()*+,;=:@/?"));
    }

    @Test
    void urlEscapersTest_testUrlPathSegmentEscaper() {
        assertEquals(String.valueOf('+'), asString(String.valueOf('+')).encodePercent(
                "!$&'()*+,;=:@"));
    }

    @Test
    void urlEscapersTest_testUrlFragmentEscaper() {
        assertEquals(String.valueOf('+'), asString(String.valueOf('+')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('/'), asString(String.valueOf('/')).encodePercent(
                "!$&'()*+,;=:@/?"));
        assertEquals(String.valueOf('?'), asString(String.valueOf('?')).encodePercent(
                "!$&'()*+,;=:@/?"));
    }
}
