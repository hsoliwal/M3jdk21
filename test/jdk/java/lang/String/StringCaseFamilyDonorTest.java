/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringCaseFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringCaseFamilyDonorTest {
    private static final String FOO_CAP = "Foo";
    private static final String FOO_UNCAP = "foo";
    private static final String SENTENCE_CAP = "Foo Bar Baz";
    private static final String SENTENCE_UNCAP = "foo bar baz";

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
    void stringUtilsTest_testCapitalize() {
        assertEquals("", asString("").capitalize(), "capitalize(empty-string) failed");
        assertEquals("X", asString("x").capitalize(), "capitalize(single-char-string) failed");
        assertEquals(FOO_CAP, asString(FOO_CAP).capitalize(), "capitalize(String) failed");
        assertEquals(FOO_CAP, asString(FOO_UNCAP).capitalize(), "capitalize(string) failed");
        assertEquals("\u01C8", asString("\u01C9").capitalize(),
                "capitalize(String) is not using TitleCase");
        assertEquals("", asString("").capitalize());
        assertEquals("Cat", asString("cat").capitalize());
        assertEquals("CAt", asString("cAt").capitalize());
        assertEquals("'cat'", asString("'cat'").capitalize());
    }

    @Test
    void stringUtilsTest_testReCapitalize() {
        assertEquals(SENTENCE_UNCAP, asString(asString(SENTENCE_UNCAP).capitalize()).uncapitalize(),
                "uncapitalize(capitalize(String)) failed");
        assertEquals(SENTENCE_CAP, asString(asString(SENTENCE_CAP).uncapitalize()).capitalize(),
                "capitalize(uncapitalize(String)) failed");
        assertEquals(FOO_UNCAP, asString(asString(FOO_UNCAP).capitalize()).uncapitalize(),
                "uncapitalize(capitalize(String)) failed");
        assertEquals(FOO_CAP, asString(asString(FOO_CAP).uncapitalize()).capitalize(),
                "capitalize(uncapitalize(String)) failed");
    }

    @Test
    void stringUtilsTest_testSwapCase_String() {
        assertEquals("", asString("").swapCase());
        assertEquals("  ", asString("  ").swapCase());
        assertEquals("I AM HERE 123", asString("i am here 123").swapCase());
        assertEquals("i aM hERE 123", asString("I Am Here 123").swapCase());
        assertEquals("I AM here 123", asString("i am HERE 123").swapCase());
        assertEquals("i am here 123", asString("I AM HERE 123").swapCase());
        final String expect = "tHIS sTRING CONTAINS A tITLEcASE CHARACTER: \u01C9";
        final String test = "This String contains a TitleCase character: \u01C8";
        assertEquals(expect, asString(test).swapCase());
    }

    @Test
    void stringUtilsTest_testUnCapitalize() {
        assertEquals(FOO_UNCAP, asString(FOO_CAP).uncapitalize(), "uncapitalize(String) failed");
        assertEquals(FOO_UNCAP, asString(FOO_UNCAP).uncapitalize(), "uncapitalize(string) failed");
        assertEquals("", asString("").uncapitalize(), "uncapitalize(empty-string) failed");
        assertEquals("x", asString("X").uncapitalize(), "uncapitalize(single-char-string) failed");
        assertEquals("cat", asString("cat").uncapitalize());
        assertEquals("cat", asString("Cat").uncapitalize());
        assertEquals("cAT", asString("CAT").uncapitalize());
    }
}
