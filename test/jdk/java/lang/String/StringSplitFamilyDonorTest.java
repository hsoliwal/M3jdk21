/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringSplitFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringSplitFamilyDonorTest {
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

    @Test
    void stringUtilsTest_testSplit_String() {
        assertEquals(0, asString("").split().length);

        String str = "a b  .c";
        String[] res = asString(str).split();
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(".c", res[2]);

        str = " a ";
        res = asString(str).split();
        assertEquals(1, res.length);
        assertEquals("a", res[0]);

        str = "a" + WHITESPACE + "b" + NON_WHITESPACE + "c";
        res = asString(str).split();
        assertEquals(2, res.length);
        assertEquals("a", res[0]);
        assertEquals("b" + NON_WHITESPACE + "c", res[1]);
    }

    @Test
    void stringUtilsTest_testSplit_StringChar() {
        assertEquals(0, asString("").split('.').length);

        String str = "a.b.. c";
        String[] res = asString(str).split('.');
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(" c", res[2]);

        str = ".a.";
        res = asString(str).split('.');
        assertEquals(1, res.length);
        assertEquals("a", res[0]);

        str = "a b c";
        res = asString(str).split(' ');
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
    }

    @Test
    void stringUtilsTest_testSplitByCharacterType() {
        assertEquals(0, asString("").splitByCharacterType().length);
        assertArrayEquals(new String[] { "ab", " ", "de", " ", "fg" },
                asString("ab de fg").splitByCharacterType());
        assertArrayEquals(new String[] { "ab", "   ", "de", " ", "fg" },
                asString("ab   de fg").splitByCharacterType());
        assertArrayEquals(new String[] { "ab", ":", "cd", ":", "ef" },
                asString("ab:cd:ef").splitByCharacterType());
        assertArrayEquals(new String[] { "number", "5" },
                asString("number5").splitByCharacterType());
        assertArrayEquals(new String[] { "foo", "B", "ar" },
                asString("fooBar").splitByCharacterType());
        assertArrayEquals(new String[] { "foo", "200", "B", "ar" },
                asString("foo200Bar").splitByCharacterType());
        assertArrayEquals(new String[] { "ASFR", "ules" },
                asString("ASFRules").splitByCharacterType());

        final String boldA = new String(Character.toChars(0x1D400));

        final String boldFive = new String(Character.toChars(0x1D7D3));
        assertArrayEquals(new String[] { "A" + boldA },
                asString("A" + boldA).splitByCharacterType());
        assertArrayEquals(new String[] { "5" + boldFive },
                asString("5" + boldFive).splitByCharacterType());
        assertArrayEquals(new String[] { boldA, "5" + boldFive, "z" },
                asString(boldA + "5" + boldFive + "z").splitByCharacterType());
    }

    @Test
    void stringUtilsTest_testSplitByCharacterTypeCamelCase() {
        assertEquals(0, asString("").splitByCharacterTypeCamelCase().length);
        assertArrayEquals(new String[] { "ab", " ", "de", " ", "fg" },
                asString("ab de fg").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "ab", "   ", "de", " ", "fg" },
                asString("ab   de fg").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "ab", ":", "cd", ":", "ef" },
                asString("ab:cd:ef").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "number", "5" },
                asString("number5").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "foo", "Bar" },
                asString("fooBar").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "foo", "200", "Bar" },
                asString("foo200Bar").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "ASF", "Rules" },
                asString("ASFRules").splitByCharacterTypeCamelCase());

        final String boldA = new String(Character.toChars(0x1D400));
        assertArrayEquals(new String[] { boldA + "bc" },
                asString(boldA + "bc").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "AB", boldA + "cd" },
                asString("AB" + boldA + "cd").splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] { "foo", boldA + "bar" },
                asString("foo" + boldA + "bar").splitByCharacterTypeCamelCase());
    }

    @Test
    void stringUtilsTest_testSplitByWholeSeparatorPreserveAllTokens_StringString() {
        assertEquals(0, asString("").splitByWholeSeparatorPreserveAllTokens(".").length);
    }

    @Test
    void stringUtilsTest_testSplitByWholeSeparatorPreserveAllTokens_StringStringInt() {
        assertEquals(0, asString("").splitByWholeSeparatorPreserveAllTokens(".", -1).length);
    }

    @Test
    void stringUtilsTest_testSplitByWholeString_StringStringBoolean() {
        assertEquals(0, asString("").splitByWholeSeparator(".").length);
    }

    @Test
    void stringUtilsTest_testSplitByWholeString_StringStringBooleanInt() {
        assertEquals(0, asString("").splitByWholeSeparator(".", 3).length);
    }

    @Test
    void stringUtilsTest_testSplitPreserveAllTokens_String() {
        assertEquals(0, asString("").splitPreserveAllTokens().length);

        String str = "abc def";
        String[] res = asString(str).splitPreserveAllTokens();
        assertEquals(2, res.length);
        assertEquals("abc", res[0]);
        assertEquals("def", res[1]);

        str = "abc  def";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(3, res.length);
        assertEquals("abc", res[0]);
        assertEquals("", res[1]);
        assertEquals("def", res[2]);

        str = " abc ";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(3, res.length);
        assertEquals("", res[0]);
        assertEquals("abc", res[1]);
        assertEquals("", res[2]);

        str = "a b .c";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(".c", res[2]);

        str = " a b .c";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("b", res[2]);
        assertEquals(".c", res[3]);

        str = "a  b  .c";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("", res[1]);
        assertEquals("b", res[2]);
        assertEquals("", res[3]);
        assertEquals(".c", res[4]);

        str = " a  ";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);
        assertEquals("", res[3]);

        str = " a  b";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);
        assertEquals("b", res[3]);

        str = "a" + WHITESPACE + "b" + NON_WHITESPACE + "c";
        res = asString(str).splitPreserveAllTokens();
        assertEquals(WHITESPACE.length() + 1, res.length);
        assertEquals("a", res[0]);
        for (int i = 1; i < WHITESPACE.length() - 1; i++) {
            assertEquals("", res[i]);
        }
        assertEquals("b" + NON_WHITESPACE + "c", res[WHITESPACE.length()]);
    }

    @Test
    void stringUtilsTest_testSplitPreserveAllTokens_StringChar() {
        assertEquals(0, asString("").splitPreserveAllTokens('.').length);

        String str = "a.b. c";
        String[] res = asString(str).splitPreserveAllTokens('.');
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(" c", res[2]);

        str = "a.b.. c";
        res = asString(str).splitPreserveAllTokens('.');
        assertEquals(4, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("", res[2]);
        assertEquals(" c", res[3]);

        str = ".a.";
        res = asString(str).splitPreserveAllTokens('.');
        assertEquals(3, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);

        str = ".a..";
        res = asString(str).splitPreserveAllTokens('.');
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("", res[2]);
        assertEquals("", res[3]);

        str = "..a.";
        res = asString(str).splitPreserveAllTokens('.');
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("", res[1]);
        assertEquals("a", res[2]);
        assertEquals("", res[3]);

        str = "..a";
        res = asString(str).splitPreserveAllTokens('.');
        assertEquals(3, res.length);
        assertEquals("", res[0]);
        assertEquals("", res[1]);
        assertEquals("a", res[2]);

        str = "a b c";
        res = asString(str).splitPreserveAllTokens(' ');
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);

        str = "a  b  c";
        res = asString(str).splitPreserveAllTokens(' ');
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("", res[1]);
        assertEquals("b", res[2]);
        assertEquals("", res[3]);
        assertEquals("c", res[4]);

        str = " a b c";
        res = asString(str).splitPreserveAllTokens(' ');
        assertEquals(4, res.length);
        assertEquals("", res[0]);
        assertEquals("a", res[1]);
        assertEquals("b", res[2]);
        assertEquals("c", res[3]);

        str = "  a b c";
        res = asString(str).splitPreserveAllTokens(' ');
        assertEquals(5, res.length);
        assertEquals("", res[0]);
        assertEquals("", res[1]);
        assertEquals("a", res[2]);
        assertEquals("b", res[3]);
        assertEquals("c", res[4]);

        str = "a b c ";
        res = asString(str).splitPreserveAllTokens(' ');
        assertEquals(4, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
        assertEquals("", res[3]);

        str = "a b c  ";
        res = asString(str).splitPreserveAllTokens(' ');
        assertEquals(5, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
        assertEquals("", res[3]);
        assertEquals("", res[4]);

        {
            final String[] results;
            final String[] expectedResults = {"a", "", "b", "c"};
            results = asString("a..b.c").splitPreserveAllTokens('.');
            assertEquals(expectedResults.length, results.length);
            for (int i = 0; i < expectedResults.length; i++) {
                assertEquals(expectedResults[i], results[i]);
            }
        }
    }

    @Test
    void stringUtilsTest_testSplitPreserveAllTokens_StringString_StringStringInt() {
        assertEquals(0, asString("").splitPreserveAllTokens(".").length);
        assertEquals(0, asString("").splitPreserveAllTokens(".", 3).length);
    }
}
