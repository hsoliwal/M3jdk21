/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary The assertions of commons-text 1.15.0's own WordUtils and CaseUtils tests for
 *          this family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT
 *          v1) through port_text_donor.py
 * @run junit StringCaseFamilyTextDonorTest
 */

import static org.junit.jupiter.api.Assertions.*;

import java.nio.CharBuffer;
import java.util.Locale;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Ported mechanically from commons-text 1.15.0 (Apache-2.0) test sources by
 * port_text_donor.py (WordUtils. and CaseUtils. read as StringUtils.); skipped statements and
 * reasons are listed in the string-lane record.
 */
public class StringCaseFamilyTextDonorTest {
    private static final String WHITESPACE = IntStream.rangeClosed(Character.MIN_CODE_POINT,
                Character.MAX_CODE_POINT).filter(Character::isWhitespace)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                    StringBuilder::append).toString();

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
    void wordUtilsTest_testCapitalize_String() {
        assertEquals("", asString("").capitalizeWords());
        assertEquals("  ", asString("  ").capitalizeWords());
        assertEquals("I", asString("I").capitalizeWords());
        assertEquals("I", asString("i").capitalizeWords());
        assertEquals("I Am Here 123", asString("i am here 123").capitalizeWords());
        assertEquals("I Am Here 123", asString("I Am Here 123").capitalizeWords());
        assertEquals("I Am HERE 123", asString("i am HERE 123").capitalizeWords());
        assertEquals("I AM HERE 123", asString("I AM HERE 123").capitalizeWords());
    }

    @Test
    void wordUtilsTest_testCapitalizeFully_String() {
        assertEquals("", asString("").capitalizeFully());
        assertEquals("  ", asString("  ").capitalizeFully());
        assertEquals("I", asString("I").capitalizeFully());
        assertEquals("I", asString("i").capitalizeFully());
        assertEquals("I Am Here 123", asString("i am here 123").capitalizeFully());
        assertEquals("I Am Here 123", asString("I Am Here 123").capitalizeFully());
        assertEquals("I Am Here 123", asString("i am HERE 123").capitalizeFully());
        assertEquals("I Am Here 123", asString("I AM HERE 123").capitalizeFully());
        assertEquals("Alphabet", asString("alphabet").capitalizeFully());
        assertEquals("A\tB\nC D", asString("a\tb\nc d").capitalizeFully());
        assertEquals("And \tBut \nCleat  Dome", asString("and \tbut \ncleat  dome").capitalizeFully(
                ));
        assertEquals(WHITESPACE, asString(WHITESPACE).capitalizeFully());
        assertEquals("A" + WHITESPACE + "B", asString("a" + WHITESPACE + "b").capitalizeFully());
    }

    @Test
    void wordUtilsTest_testCapitalizeFully_Text88() {
        assertEquals("I am fine now", asString("i am fine now").capitalizeFully(new char[] {}));
    }

    @Test
    void wordUtilsTest_testCapitalizeFullyWithDelimiters_String() {
        assertEquals("", asString("").capitalizeFully(new char[0]));
        assertEquals("  ", asString("  ").capitalizeFully(new char[0]));
        char[] chars = { '-', '+', ' ', '@' };
        assertEquals("I", asString("I").capitalizeFully(chars));
        assertEquals("I", asString("i").capitalizeFully(chars));
        assertEquals("I-Am Here+123", asString("i-am here+123").capitalizeFully(chars));
        assertEquals("I Am+Here-123", asString("I Am+Here-123").capitalizeFully(chars));
        assertEquals("I+Am-Here 123", asString("i+am-HERE 123").capitalizeFully(chars));
        assertEquals("I-Am Here+123", asString("I-AM HERE+123").capitalizeFully(chars));
    }

    @Test
    void wordUtilsTest_testCapitalizeFullyWithDelimiters_String_1() {
        char[] chars = new char[] { '.' };
        assertEquals("I am.Fine", asString("i aM.fine").capitalizeFully(chars));
        assertThrows(NullPointerException.class, () -> asString("i am.fine").capitalizeFully(null));
        assertThrows(NullPointerException.class, () -> asString("alphabet").capitalizeFully(null));
        assertEquals("Alphabet", asString("alphabet").capitalizeFully(new char[] { '!' }));
    }

    @Test
    void wordUtilsTest_testCapitalizeWithDelimiters_String() {
        assertEquals("", asString("").capitalizeWords(new char[0]));
        assertEquals("  ", asString("  ").capitalizeWords(new char[0]));
        char[] chars = { '-', '+', ' ', '@' };
        assertEquals("I", asString("I").capitalizeWords(chars));
        assertEquals("I", asString("i").capitalizeWords(chars));
        assertEquals("I-Am Here+123", asString("i-am here+123").capitalizeWords(chars));
        assertEquals("I Am+Here-123", asString("I Am+Here-123").capitalizeWords(chars));
        assertEquals("I+Am-HERE 123", asString("i+am-HERE 123").capitalizeWords(chars));
        assertEquals("I-AM HERE+123", asString("I-AM HERE+123").capitalizeWords(chars));
    }

    @Test
    void wordUtilsTest_testCapitalizeWithDelimiters_String_1() {
        char[] chars = new char[] { '.' };
        assertEquals("I aM.Fine", asString("i aM.fine").capitalizeWords(chars));
        assertThrows(NullPointerException.class, () -> asString("i am.fine").capitalizeWords(null));
    }

    @Test
    void wordUtilsTest_testInitials_String() {
        assertEquals("", asString("").initials());
        assertEquals("", asString("  ").initials());
        assertEquals("I", asString("I").initials());
        assertEquals("i", asString("i").initials());
        assertEquals("BJL", asString("Ben John Lee").initials());
        assertEquals("BJL", asString("   Ben \n   John\tLee\t").initials());
        assertEquals("BJ", asString("Ben J.Lee").initials());
        assertEquals("BJ.L", asString(" Ben   John  . Lee").initials());
        assertEquals("iah1", asString("i am here 123").initials());
    }

    @Test
    void wordUtilsTest_testInitials_String_charArray() {
        assertThrows(NullPointerException.class, () -> asString("").initials((char[]) null));
        assertThrows(NullPointerException.class, () -> asString("  ").initials((char[]) null));
        assertThrows(NullPointerException.class, () -> asString("I").initials((char[]) null));
        assertThrows(NullPointerException.class, () -> asString("i").initials((char[]) null));
        assertThrows(NullPointerException.class, () -> asString("SJC").initials((char[]) null));
        assertThrows(NullPointerException.class, () -> asString("Ben John Lee").initials((
                char[]) null));
        assertThrows(NullPointerException.class, () -> asString("   Ben \n   John\tLee\t").initials(
                (char[]) null));
        assertThrows(NullPointerException.class, () -> asString("Ben J.Lee").initials((
                char[]) null));
        assertThrows(NullPointerException.class, () -> asString(" Ben   John  . Lee").initials((
                char[]) null));
        assertThrows(NullPointerException.class, () -> asString("Kay O'Murphy").initials((
                char[]) null));
        assertThrows(NullPointerException.class, () -> asString("i am here 123").initials((
                char[]) null));
    }

    @Test
    void wordUtilsTest_testInitials_String_charArray_1() {
        char[] array = new char[0];
        assertEquals("", asString("").initials(array));
        assertEquals("", asString("  ").initials(array));
        assertEquals("", asString("I").initials(array));
        assertEquals("", asString("i").initials(array));
        assertEquals("", asString("SJC").initials(array));
        assertEquals("", asString("Ben John Lee").initials(array));
        assertEquals("", asString("   Ben \n   John\tLee\t").initials(array));
        assertEquals("", asString("Ben J.Lee").initials(array));
        assertEquals("", asString(" Ben   John  . Lee").initials(array));
        assertEquals("", asString("Kay O'Murphy").initials(array));
        assertEquals("", asString("i am here 123").initials(array));
    }

    @Test
    void wordUtilsTest_testInitials_String_charArray_2() {
        char[] array = " ".toCharArray();
        assertEquals("", asString("").initials(array));
        assertEquals("", asString("  ").initials(array));
        assertEquals("I", asString("I").initials(array));
        assertEquals("i", asString("i").initials(array));
        assertEquals("S", asString("SJC").initials(array));
        assertEquals("BJL", asString("Ben John Lee").initials(array));
        assertEquals("BJ", asString("Ben J.Lee").initials(array));
        assertEquals("B\nJ", asString("   Ben \n   John\tLee\t").initials(array));
        assertEquals("BJ.L", asString(" Ben   John  . Lee").initials(array));
        assertEquals("KO", asString("Kay O'Murphy").initials(array));
        assertEquals("iah1", asString("i am here 123").initials(array));
    }

    @Test
    void wordUtilsTest_testInitials_String_charArray_3() {
        char[] array = " .".toCharArray();
        assertEquals("", asString("").initials(array));
        assertEquals("", asString("  ").initials(array));
        assertEquals("I", asString("I").initials(array));
        assertEquals("i", asString("i").initials(array));
        assertEquals("S", asString("SJC").initials(array));
        assertEquals("BJL", asString("Ben John Lee").initials(array));
        assertEquals("BJL", asString("Ben J.Lee").initials(array));
        assertEquals("BJL", asString(" Ben   John  . Lee").initials(array));
        assertEquals("KO", asString("Kay O'Murphy").initials(array));
        assertEquals("iah1", asString("i am here 123").initials(array));
    }

    @Test
    void wordUtilsTest_testInitials_String_charArray_4() {
        char[] array = " .'".toCharArray();
        assertEquals("", asString("").initials(array));
        assertEquals("", asString("  ").initials(array));
        assertEquals("I", asString("I").initials(array));
        assertEquals("i", asString("i").initials(array));
        assertEquals("S", asString("SJC").initials(array));
        assertEquals("BJL", asString("Ben John Lee").initials(array));
        assertEquals("BJL", asString("Ben J.Lee").initials(array));
        assertEquals("BJL", asString(" Ben   John  . Lee").initials(array));
        assertEquals("KOM", asString("Kay O'Murphy").initials(array));
        assertEquals("iah1", asString("i am here 123").initials(array));
    }

    @Test
    void wordUtilsTest_testInitials_String_charArray_5() {
        char[] array = "SIJo1".toCharArray();
        assertEquals("", asString("").initials(array));
        assertEquals(" ", asString("  ").initials(array));
        assertEquals("", asString("I").initials(array));
        assertEquals("i", asString("i").initials(array));
        assertEquals("C", asString("SJC").initials(array));
        assertEquals("Bh", asString("Ben John Lee").initials(array));
        assertEquals("B.", asString("Ben J.Lee").initials(array));
        assertEquals(" h", asString(" Ben   John  . Lee").initials(array));
        assertEquals("K", asString("Kay O'Murphy").initials(array));
        assertEquals("i2", asString("i am here 123").initials(array));
    }

    @Test
    void wordUtilsTest_testInitialsSurrogatePairs() {
        assertEquals("\uD800\uDF00\uD800\uDF02", asString(
                "\uD800\uDF00\uD800\uDF01 \uD800\uDF02\uD800" + "\uDF03").initials());
        assertThrows(NullPointerException.class, () -> asString(
                "\uD800\uDF00\uD800\uDF01 \uD800\uDF02\uD800" + "\uDF03").initials(null));
        assertThrows(NullPointerException.class, () -> asString(
                "\uD800\uDF00 \uD800\uDF02 ").initials(null));
        assertEquals("\uD800\uDF00\uD800\uDF02", asString(
                "\uD800\uDF00\uD800\uDF01.\uD800\uDF02\uD800" + "\uDF03").initials(
                new char[] { '.' }));
        assertEquals("\uD800\uDF00\uD800\uDF02", asString(
                "\uD800\uDF00\uD800\uDF01A\uD800\uDF02\uD800" + "\uDF03").initials(
                new char[] { 'A' }));
        assertEquals("\uD800\uDF00\uD800\uDF02", asString(
                "\uD800\uDF00\uD800\uDF01\uD800\uDF14\uD800" + "\uDF02\uD800\uDF03").initials(
                new char[] { '\uD800', '\uDF14' }));
        assertEquals("\uD800\uDF00\uD800\uDF02", asString(
                "\uD800\uDF00\uD800\uDF01\uD800\uDF14\uD800"
                + "\uDF18\uD800\uDF02\uD800\uDF03").initials(new char[] { '\uD800', '\uDF14',
                '\uD800', '\uDF18' }));
    }

    @Test
    void wordUtilsTest_testSwapCase_String() {
        assertEquals("", asString("").swapCaseWords());
        assertEquals("  ", asString("  ").swapCaseWords());
        assertEquals("i", asString("I").swapCaseWords());
        assertEquals("I", asString("i").swapCaseWords());
        assertEquals("I AM HERE 123", asString("i am here 123").swapCaseWords());
        assertEquals("i aM hERE 123", asString("I Am Here 123").swapCaseWords());
        assertEquals("I AM here 123", asString("i am HERE 123").swapCaseWords());
        assertEquals("i am here 123", asString("I AM HERE 123").swapCaseWords());
        final String expect = "tHIS sTRING CONTAINS A tITLEcASE CHARACTER: \u01C9";
        final String test = "This String contains a TitleCase character: \u01C8";
        assertEquals(expect, asString(test).swapCaseWords());
    }

    @Test
    void wordUtilsTest_testUncapitalize_String() {
        assertEquals("", asString("").uncapitalizeWords());
        assertEquals("  ", asString("  ").uncapitalizeWords());
        assertEquals("i", asString("I").uncapitalizeWords());
        assertEquals("i", asString("i").uncapitalizeWords());
        assertEquals("i am here 123", asString("i am here 123").uncapitalizeWords());
        assertEquals("i am here 123", asString("I Am Here 123").uncapitalizeWords());
        assertEquals("i am hERE 123", asString("i am HERE 123").uncapitalizeWords());
        assertEquals("i aM hERE 123", asString("I AM HERE 123").uncapitalizeWords());
        assertEquals("a\tb\nc d", asString("A\tB\nC D").uncapitalizeWords());
        assertEquals("and \tbut \ncLEAT  dome", asString(
                "And \tBut \nCLEAT  Dome").uncapitalizeWords());
        assertEquals(WHITESPACE, asString(WHITESPACE).capitalizeFully());
        assertEquals("A" + WHITESPACE + "B", asString("a" + WHITESPACE + "b").capitalizeFully());
    }

    @Test
    void wordUtilsTest_testUnCapitalize_Text88() {
        assertEquals("i am fine now", asString("I am fine now").uncapitalizeWords(new char[] {}));
    }

    @Test
    void wordUtilsTest_testUncapitalizeWithDelimiters_String() {
        assertEquals("", asString("").uncapitalizeWords(new char[0]));
        assertEquals("  ", asString("  ").uncapitalizeWords(new char[0]));
        char[] chars = { '-', '+', ' ', '@' };
        assertEquals("i", asString("I").uncapitalizeWords(chars));
        assertEquals("i", asString("i").uncapitalizeWords(chars));
        assertEquals("i am-here+123", asString("i am-here+123").uncapitalizeWords(chars));
        assertEquals("i+am here-123", asString("I+Am Here-123").uncapitalizeWords(chars));
        assertEquals("i-am+hERE 123", asString("i-am+HERE 123").uncapitalizeWords(chars));
        assertEquals("i aM-hERE+123", asString("I AM-HERE+123").uncapitalizeWords(chars));
    }

    @Test
    void wordUtilsTest_testUncapitalizeWithDelimiters_String_1() {
        char[] chars = new char[] { '.' };
        assertEquals("i AM.fINE", asString("I AM.FINE").uncapitalizeWords(chars));
        assertThrows(NullPointerException.class, () -> asString("I AM.FINE").uncapitalizeWords(
                null));
    }

    @Test
    void caseUtilsTest_testToCamelCase() {
        assertThrows(NullPointerException.class, () -> asString("").toCamelCase(true, null));
        assertThrows(NullPointerException.class, () -> asString("  ").toCamelCase(false, null));
        assertThrows(NullPointerException.class, () -> asString("a  b  c  @def").toCamelCase(false,
                null));
        assertEquals("ABC@def", asString("a b c @def").toCamelCase(true));
        assertEquals("ABC@def", asString("a b c @def").toCamelCase(true, '-'));
        assertEquals("ABC@def", asString("a b c @def").toCamelCase(true, '-'));
        final char[] chars = { '-', '+', ' ', '@' };
        assertEquals("", asString("-+@ ").toCamelCase(true, chars));
        assertEquals("toCamelCase", asString("   to-CAMEL-cASE").toCamelCase(false, chars));
        assertEquals("ToCamelCase", asString("@@@@   to+CAMEL@cASE ").toCamelCase(true, chars));
        assertEquals("ToCaMeLCase", asString("To+CA+ME L@cASE").toCamelCase(true, chars));
        assertEquals("toCamelCase", asString("To.Camel.Case").toCamelCase(false, '.'));
        assertEquals("toCamelCase", asString("To.Camel-Case").toCamelCase(false, '-', '.'));
        assertEquals("toCamelCase", asString(" to @ Camel case").toCamelCase(false, '-', '@'));
        assertEquals("ToCamelCase", asString(" @to @ Camel case").toCamelCase(true, '-', '@'));
        assertThrows(NullPointerException.class, () -> asString("TO CAMEL CASE").toCamelCase(true,
                null));
        assertThrows(NullPointerException.class, () -> asString("TO CAMEL CASE").toCamelCase(false,
                null));
        assertThrows(NullPointerException.class, () -> asString("TO CAMEL CASE").toCamelCase(false,
                null));
        assertThrows(NullPointerException.class, () -> asString("tocamelcase").toCamelCase(false,
                null));
        assertThrows(NullPointerException.class, () -> asString("tocamelcase").toCamelCase(true,
                null));
        assertThrows(NullPointerException.class, () -> asString("Tocamelcase").toCamelCase(false,
                null));
        assertEquals("Tocamelcase", asString("tocamelcase").toCamelCase(true));
        assertEquals("tocamelcase", asString("tocamelcase").toCamelCase(false));
        assertEquals("\uD800\uDF00\uD800\uDF02", asString("\uD800\uDF00 \uD800\uDF02").toCamelCase(
                true));
        assertEquals("\uD800\uDF00\uD800\uDF01\uD800\uDF02\uD800" + "\uDF03", asString(
                "\uD800\uDF00\uD800\uDF01\uD800\uDF14\uD800" + "\uDF02\uD800\uDF03").toCamelCase(
                true, '\uD800', '\uDF14'));
    }
}
