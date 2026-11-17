/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to
 *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)
 * @run junit StringReplaceFamilyDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringReplaceFamilyDonorTest {

    private static String asString(CharSequence text) {
        return text.toString();
    }

    /**
     * Runs a donor assertion; a NullPointerException from the String method (null rejected) passes.
     */
    private static void assertNpeOr(Runnable assertion) {
        try {
            assertion.run();
        } catch (NullPointerException rejected) {
            // null array rejected as documented
        }
    }

    @Test
    void stringUtilsTest_testLang623() {
        assertEquals("t", asString("\u00DE").replace('\u00DE', 't'));
        assertEquals("t", asString("\u00FE").replace('\u00FE', 't'));
    }

    @Test
    void stringUtilsTest_testRemove_char() {
        assertThrows(NullPointerException.class, () -> asString("").remove(null));
        assertEquals("", asString("").remove('a'));
        assertEquals("qeed", asString("queued").remove('u'));
        assertEquals("queued", asString("queued").remove('z'));
    }

    @Test
    void stringUtilsTest_testRemove_String() {
        assertThrows(NullPointerException.class, () -> asString("").remove(null));
        assertEquals("", asString("").remove(""));
        assertEquals("", asString("").remove("a"));
        assertThrows(NullPointerException.class, () -> asString("").remove(null));
        assertThrows(NullPointerException.class, () -> asString("a").remove(null));
        assertEquals("", asString("").remove(""));
        assertEquals("a", asString("a").remove(""));
        assertEquals("qd", asString("queued").remove("ue"));
        assertEquals("queued", asString("queued").remove("zz"));
    }

    @Test
    void stringUtilsTest_testRemoveIgnoreCase_String() {
        assertThrows(NullPointerException.class, () -> asString("").removeIgnoreCase(null));
        assertEquals("", asString("").removeIgnoreCase(""));
        assertEquals("", asString("").removeIgnoreCase("a"));
        assertThrows(NullPointerException.class, () -> asString("").removeIgnoreCase(null));
        assertThrows(NullPointerException.class, () -> asString("a").removeIgnoreCase(null));
        assertEquals("", asString("").removeIgnoreCase(""));
        assertEquals("a", asString("a").removeIgnoreCase(""));
        assertEquals("qd", asString("queued").removeIgnoreCase("ue"));
        assertEquals("queued", asString("queued").removeIgnoreCase("zz"));
        assertEquals("qd", asString("quEUed").removeIgnoreCase("UE"));
        assertEquals("queued", asString("queued").removeIgnoreCase("zZ"));
        assertEquals("\u0130", asString("\u0130x").removeIgnoreCase("x"));
        asString("\u0130a").removeIgnoreCase("a");
    }

    @Test
    void stringUtilsTest_testReplace_StringStringArrayStringArray() {
        assertEquals(asString("").replaceEach(new String[]{"a"}, new String[]{"b"}), "");
        assertThrows(NullPointerException.class, () -> asString("aba").replaceEach(null, null));
        assertThrows(NullPointerException.class, () -> asString("aba").replaceEach(new String[0],
                null));
        assertThrows(NullPointerException.class, () -> asString("aba").replaceEach(null,
                new String[0]));
        assertThrows(NullPointerException.class,
                () -> asString("aba").replaceEach(new String[]{"a"}, null));
        assertEquals(asString("aba").replaceEach(new String[]{"a"}, new String[]{""}), "b");
        assertEquals(asString("aba").replaceEach(new String[]{null}, new String[]{"a"}), "aba");
        assertEquals(asString("abcde").replaceEach(new String[]{"ab", "d"}, new String[]{"w", "t"}),
                "wcte");
        assertEquals(asString("abcde").replaceEach(new String[]{"ab", "d"}, new String[]{"d", "t"}),
                "dcte");
        assertEquals("bcc", asString("abc").replaceEach(new String[]{"a", "b"}, new String[]{"b",
                "c"}));
        assertEquals("q651.506bera", asString("d216.102oren").replaceEach(new String[]{"a", "b",
                "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
                "s", "t", "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H",
                "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
                "Y", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9"}, new String[]{"n", "o", "p",
                "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "a", "b", "c", "d", "e", "f",
                "g", "h", "i", "j", "k", "l", "m", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
                "W", "X", "Y", "Z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                "M", "5", "6", "7", "8", "9", "1", "2", "3", "4"}));
        assertEquals(asString("aba").replaceEach(new String[]{"a"}, new String[]{null}), "aba");
        assertEquals(asString("aba").replaceEach(new String[]{"a", "b"}, new String[]{"c", null}),
                "cbc");
        assertThrows(IllegalArgumentException.class,
                () -> asString("abba").replaceEach(new String[] { "a" }, new String[] { "b",
                "a" }),
                "asString(String).replaceEach(String[], String[]) expecting " +
                        "IllegalArgumentException");
    }

    @Test
    void stringUtilsTest_testReplace_StringStringArrayStringArrayBoolean() {
        assertEquals("", asString("").replaceEachRepeatedly(new String[]{"a"}, new String[]{"b"}));
        assertThrows(NullPointerException.class, () -> asString("aba").replaceEachRepeatedly(null,
                null));
        assertThrows(NullPointerException.class,
                () -> asString("aba").replaceEachRepeatedly(new String[0], null));
        assertThrows(NullPointerException.class, () -> asString("aba").replaceEachRepeatedly(null,
                new String[0]));
        assertThrows(NullPointerException.class,
                () -> asString("aba").replaceEachRepeatedly(new String[0], null));
        assertEquals("b", asString("aba").replaceEachRepeatedly(new String[]{"a"},
                new String[]{""}));
        assertEquals("aba", asString("aba").replaceEachRepeatedly(new String[]{null},
                new String[]{"a"}));
        assertEquals("wcte", asString("abcde").replaceEachRepeatedly(new String[]{"ab", "d"},
                new String[]{"w", "t"}));
        assertEquals("tcte", asString("abcde").replaceEachRepeatedly(new String[]{"ab", "d"},
                new String[]{"d", "t"}));
        assertEquals("blaan", asString("blllaan").replaceEachRepeatedly(new String[]{"llaan"},
                new String[]{"laan"}));
        assertEquals("blaan", asString("bllllaan").replaceEachRepeatedly(new String[]{"llaan"},
                new String[]{"laan"}));
        assertEquals("a",
                asString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        .replaceEachRepeatedly(new String[]{"aa"},
                        new String[]{"a"}));
        assertThrows(IllegalStateException.class,
                () -> asString("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                        .replaceEachRepeatedly(new String[] { "aa" }, new String[] { "a" }),
                        "Cannot be resolved within the default time-to-live limit");
        assertThrows(IllegalStateException.class,
                () -> asString("aA0aA0aA0").replaceEachRepeatedly(new String[]{"a", "b", "c", "d",
                "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
                "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
                "1", "2", "3", "4", "5", "6", "7", "8", "9"}, new String[]{"b", "c", "d", "e", "f",
                "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2",
                "3", "4", "5", "6", "7", "8", "9", "0"}),
                "Cannot be resolved within the fixed time-to-live limit");
        assertThrows(IllegalStateException.class,
                () -> asString("a").replaceEachRepeatedly(new String[]{"a", "b", "c", "d", "e",
                "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u",
                "v", "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1",
                "2", "3", "4", "5", "6", "7", "8", "9", "0"}, new String[]{"b", "c", "d", "e", "f",
                "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
                "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "1", "2",
                "3", "4", "5", "6", "7", "8", "9", "0", "a"}), "Should be a circular reference");
        assertThrows(IllegalStateException.class,
                () -> asString("%{key1}").replaceEachRepeatedly(new String[] {"%{key1}", "%{key2}",
                "%{key3}"}, new String[] {"Key1 %{key2}", "Key2 %{key3}", "Key3 %{key1}"}),
                "Should be a circular reference");
        assertThrows(IllegalStateException.class,
                () -> asString("abcde").replaceEachRepeatedly(new String[]{"ab", "d"},
                new String[]{"d", "ab"}), "Should be a circular reference");
    }

    @Test
    void stringUtilsTest_testReplace_StringStringString() {
        assertThrows(NullPointerException.class, () -> asString("").replace(null, null, -1));
        assertThrows(NullPointerException.class, () -> asString("").replace(null, "any", -1));
        assertThrows(NullPointerException.class, () -> asString("").replace("any", null, -1));
        assertEquals("", asString("").replace("any", "any", -1));
        assertEquals("FOO", asString("FOO").replace("", "any", -1));
        assertThrows(NullPointerException.class, () -> asString("FOO").replace(null, "any", -1));
        assertThrows(NullPointerException.class, () -> asString("FOO").replace("F", null, -1));
        assertThrows(NullPointerException.class, () -> asString("FOO").replace(null, null, -1));
        assertEquals("", asString("foofoofoo").replace("foo", "", -1));
        assertEquals("barbarbar", asString("foofoofoo").replace("foo", "bar", -1));
        assertEquals("farfarfar", asString("foofoofoo").replace("oo", "ar", -1));
    }

    @Test
    void stringUtilsTest_testReplace_StringStringStringInt() {
        assertThrows(NullPointerException.class, () -> asString("").replace(null, null, 2));
        assertThrows(NullPointerException.class, () -> asString("").replace(null, "any", 2));
        assertThrows(NullPointerException.class, () -> asString("").replace("any", null, 2));
        assertEquals("", asString("").replace("any", "any", 2));
        final String str = new String(new char[]{'o', 'o', 'f', 'o', 'o'});
        assertSame(str, asString(str).replace("x", "", -1));
        assertEquals("f", asString("oofoo").replace("o", "", -1));
        assertEquals("oofoo", asString("oofoo").replace("o", "", 0));
        assertEquals("ofoo", asString("oofoo").replace("o", "", 1));
        assertEquals("foo", asString("oofoo").replace("o", "", 2));
        assertEquals("fo", asString("oofoo").replace("o", "", 3));
        assertEquals("f", asString("oofoo").replace("o", "", 4));
        assertEquals("f", asString("oofoo").replace("o", "", -5));
        assertEquals("f", asString("oofoo").replace("o", "", 1000));
    }

    @Test
    void stringUtilsTest_testReplaceChars_StringCharChar() {
        assertEquals("", asString("").replace('b', 'z'));
        assertEquals("azcza", asString("abcba").replace('b', 'z'));
        assertEquals("abcba", asString("abcba").replace('x', 'z'));
    }

    @Test
    void stringUtilsTest_testReplaceChars_StringStringString() {
        assertThrows(NullPointerException.class, () -> asString("").replaceChars(null, null));
        assertThrows(NullPointerException.class, () -> asString("").replaceChars("", null));
        assertThrows(NullPointerException.class, () -> asString("").replaceChars("a", null));
        assertThrows(NullPointerException.class, () -> asString("").replaceChars(null, ""));
        assertThrows(NullPointerException.class, () -> asString("").replaceChars(null, "x"));
        assertThrows(NullPointerException.class, () -> asString("abc").replaceChars(null, null));
        assertThrows(NullPointerException.class, () -> asString("abc").replaceChars(null, ""));
        assertThrows(NullPointerException.class, () -> asString("abc").replaceChars(null, "x"));
        assertThrows(NullPointerException.class, () -> asString("abc").replaceChars("", null));
        assertEquals("abc", asString("abc").replaceChars("", ""));
        assertEquals("abc", asString("abc").replaceChars("", "x"));
        assertThrows(NullPointerException.class, () -> asString("abc").replaceChars("b", null));
        assertEquals("ac", asString("abc").replaceChars("b", ""));
        assertEquals("axc", asString("abc").replaceChars("b", "x"));
        assertEquals("ayzya", asString("abcba").replaceChars("bc", "yz"));
        assertEquals("ayya", asString("abcba").replaceChars("bc", "y"));
        assertEquals("ayzya", asString("abcba").replaceChars("bc", "yzx"));
        assertEquals("abcba", asString("abcba").replaceChars("z", "w"));
        assertSame("abcba", asString("abcba").replaceChars("z", "w"));
        assertEquals("jelly", asString("hello").replaceChars("ho", "jy"));
        assertEquals("ayzya", asString("abcba").replaceChars("bc", "yz"));
        assertEquals("ayya", asString("abcba").replaceChars("bc", "y"));
        assertEquals("ayzya", asString("abcba").replaceChars("bc", "yzx"));
        assertEquals("bcc", asString("abc").replaceChars("ab", "bc"));
        assertEquals("q651.506bera",
                asString("d216.102oren").replaceChars(
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789",
                        "nopqrstuvwxyzabcdefghijklmNOPQRSTUVWXYZABCDEFGHIJKLM567891234"));
    }

    @Test
    void stringUtilsTest_testReplaceIgnoreCase_StringStringString() {
        assertThrows(NullPointerException.class, () -> asString("").replaceIgnoreCase(null, null));
        assertThrows(NullPointerException.class, () -> asString("").replaceIgnoreCase(null, "any"));
        assertThrows(NullPointerException.class, () -> asString("").replaceIgnoreCase("any", null));
        assertEquals("", asString("").replaceIgnoreCase("any", "any"));
        assertEquals("FOO", asString("FOO").replaceIgnoreCase("", "any"));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceIgnoreCase(null,
                "any"));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceIgnoreCase("F",
                null));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceIgnoreCase(null,
                null));
        assertEquals("", asString("foofoofoo").replaceIgnoreCase("foo", ""));
        assertEquals("barbarbar", asString("foofoofoo").replaceIgnoreCase("foo", "bar"));
        assertEquals("farfarfar", asString("foofoofoo").replaceIgnoreCase("oo", "ar"));
        assertEquals("", asString("foofoofoo").replaceIgnoreCase("FOO", ""));
        assertEquals("barbarbar", asString("fooFOOfoo").replaceIgnoreCase("foo", "bar"));
        assertEquals("farfarfar", asString("foofOOfoo").replaceIgnoreCase("OO", "ar"));
    }

    @Test
    void stringUtilsTest_testReplaceIgnoreCase_StringStringStringInt() {
        assertThrows(NullPointerException.class, () -> asString("").replaceIgnoreCase(null, null,
                2));
        assertThrows(NullPointerException.class, () -> asString("").replaceIgnoreCase(null, "any",
                2));
        assertThrows(NullPointerException.class, () -> asString("").replaceIgnoreCase("any", null,
                2));
        assertEquals("", asString("").replaceIgnoreCase("any", "any", 2));
        final String str = new String(new char[] { 'o', 'o', 'f', 'o', 'o' });
        assertSame(str, asString(str).replaceIgnoreCase("x", "", -1));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("o", "", -1));
        assertEquals("oofoo", asString("oofoo").replaceIgnoreCase("o", "", 0));
        assertEquals("ofoo", asString("oofoo").replaceIgnoreCase("o", "", 1));
        assertEquals("foo", asString("oofoo").replaceIgnoreCase("o", "", 2));
        assertEquals("fo", asString("oofoo").replaceIgnoreCase("o", "", 3));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("o", "", 4));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("o", "", -5));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("o", "", 1000));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("O", "", -1));
        assertEquals("oofoo", asString("oofoo").replaceIgnoreCase("O", "", 0));
        assertEquals("ofoo", asString("oofoo").replaceIgnoreCase("O", "", 1));
        assertEquals("foo", asString("oofoo").replaceIgnoreCase("O", "", 2));
        assertEquals("fo", asString("oofoo").replaceIgnoreCase("O", "", 3));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("O", "", 4));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("O", "", -5));
        assertEquals("f", asString("oofoo").replaceIgnoreCase("O", "", 1000));
    }

    @Test
    void stringUtilsTest_testReplaceOnce_StringStringString() {
        assertThrows(NullPointerException.class, () -> asString("").replaceOnce(null, null));
        assertThrows(NullPointerException.class, () -> asString("").replaceOnce(null, "any"));
        assertThrows(NullPointerException.class, () -> asString("").replaceOnce("any", null));
        assertEquals("", asString("").replaceOnce("any", "any"));
        assertEquals("FOO", asString("FOO").replaceOnce("", "any"));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceOnce(null, "any"));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceOnce("F", null));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceOnce(null, null));
        assertEquals("foofoo", asString("foofoofoo").replaceOnce("foo", ""));
    }

    @Test
    void stringUtilsTest_testReplaceOnceIgnoreCase_StringStringString() {
        assertThrows(NullPointerException.class, () -> asString("").replaceOnceIgnoreCase(null,
                null));
        assertThrows(NullPointerException.class, () -> asString("").replaceOnceIgnoreCase(null,
                "any"));
        assertThrows(NullPointerException.class, () -> asString("").replaceOnceIgnoreCase("any",
                null));
        assertEquals("", asString("").replaceOnceIgnoreCase("any", "any"));
        assertEquals("FOO", asString("FOO").replaceOnceIgnoreCase("", "any"));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceOnceIgnoreCase(null,
                "any"));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceOnceIgnoreCase("F",
                null));
        assertThrows(NullPointerException.class, () -> asString("FOO").replaceOnceIgnoreCase(null,
                null));
        assertEquals("foofoo", asString("foofoofoo").replaceOnceIgnoreCase("foo", ""));
        assertEquals("Foofoo", asString("FoOFoofoo").replaceOnceIgnoreCase("foo", ""));
    }
}
