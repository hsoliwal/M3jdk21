/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String edge and empty/blank family (chomp ... trimToNull)
 * @run junit StringEdgeFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringEdgeFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringEdgeFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every edge and empty/blank method gives the same answer on every storage form of a text (the
 * whole String, a range of a longer String, a concatenation of two halves) as on the flat String;
 * fixed cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0 on
 * every text of up to four units over the alphabet below and on every BMP character (donor-oracle
 * proof in the string-lane record).
 */
public class StringEdgeFamilyTest {
    private static final char NBSP = (char) 0x00A0;
    private static final char[] UNITS = {
        'a', ' ', '\t', '\r', '\n', NBSP, (char) 0x01, (char) 0xE9
    };

    static Stream<String> texts() {
        List<String> texts = new ArrayList<>();
        for (int length = 0; length <= 3; length++) {
            int total = (int) Math.pow(UNITS.length, length);
            for (int code = 0; code < total; code++) {
                char[] units = new char[length];
                int rest = code;
                for (int i = 0; i < length; i++) {
                    units[i] = UNITS[rest % UNITS.length];
                    rest /= UNITS.length;
                }
                texts.add(new String(units));
            }
        }
        return texts.stream();
    }

    private static List<Object> answers(String s) {
        List<Object> out = new ArrayList<>();
        out.add(s.chomp());
        out.add(s.chop());
        out.add(s.deleteWhitespace());
        out.add(s.normalizeSpace());
        out.add(s.stripAccents());
        out.add(s.isNotBlank());
        out.add(s.isNotEmpty());
        for (String chars : new String[] {"", " ", "a", "\r\n", NBSP + "\t"}) {
            out.add(s.strip(chars));
            out.add(s.stripStart(chars));
            out.add(s.stripEnd(chars));
        }
        out.add(String.stripToEmpty(s));
        out.add(String.stripToNull(s));
        out.add(String.trimToEmpty(s));
        out.add(String.trimToNull(s));
        out.add(String.isAnyBlank(s, "x"));
        out.add(String.isAllBlank(s, " "));
        out.add(String.isAnyEmpty(s, "x"));
        out.add(String.firstNonBlank(" ", s));
        out.add(String.defaultIfBlank(s, "d"));
        out.add(List.of(String.stripAll(s, " a")));
        return out;
    }

    private static List<Function<String, String>> forms() {
        return List.of(
            text -> new String(("<<" + text + ">>").toCharArray()).substring(2, 2 + text.length()),
            text -> new String(text.toCharArray(), 0, text.length() / 2)
                    .concat(new String(text.toCharArray(), text.length() / 2,
                            text.length() - text.length() / 2)));
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("texts")
    void everyFormAnswersAsTheFlatString(String text) {
        List<Object> expected = answers(new String(text.toCharArray()));
        for (Function<String, String> form : forms()) {
            assertEquals(expected, answers(form.apply(text)));
        }
    }

    @Test
    void lineAndCharacterRules() {
        assertEquals("", "\r\n".chomp());
        assertEquals("\n", "\n\r".chomp(), "only \\r is removed, the \\n stays");
        assertEquals("a\r\n", "a\r\n\r\n".chomp());
        assertEquals("a", "a\r\n".chop(), "a final \\r\\n is one");
        assertEquals("", "a".chop());
        assertEquals("ab", " a\t b\n".deleteWhitespace());
        assertEquals("a" + NBSP + "b", ("a" + NBSP + "b").deleteWhitespace(),
                "NBSP is not whitespace");
        assertEquals("a b", " a \t\n b ".normalizeSpace());
        assertEquals("a  b", ("a" + NBSP + NBSP + "b").normalizeSpace(), "NBSP becomes a space");
        assertEquals("", " \t".normalizeSpace());
        String accented = "" + (char) 0xE9 + (char) 0xC5 + (char) 0x0110 + (char) 0x0142;
        assertEquals("eADl", accented.stripAccents());
        assertEquals("fi", String.valueOf((char) 0xFB01).stripAccents(),
                "compatibility decomposition");
        String ascii = "plain ascii";
        assertSame(ascii, ascii.stripAccents());
        assertEquals("b", "aaba".strip("a"));
        assertEquals("ba", "aaba".stripStart("a"));
        assertEquals("aab", "aaba".stripEnd("a"));
        assertEquals("xa", "xa".strip(""));
    }

    @Test
    void nullSafeStatics() {
        assertTrue(String.isNullOrEmpty(null));
        assertFalse(String.isAnyBlank((CharSequence[]) null));
        assertTrue(String.isAllBlank((CharSequence[]) null));
        assertTrue(String.isAllBlank());
        assertTrue(String.isAnyBlank("a", null));
        assertTrue(String.isNoneEmpty("a", "b"));
        assertEquals("x", String.firstNonBlank(null, " ", "x"));
        assertNull(String.firstNonEmpty((String[]) null));
        assertEquals("d", String.defaultIfBlank(" ", "d"));
        assertEquals(" ", String.defaultIfEmpty(" ", "d"));
        assertEquals("", String.defaultString(null));
        assertEquals("n", String.defaultString(null, "n"));
        assertEquals("s", String.getIfBlank("", () -> "s"));
        assertNull(String.getIfEmpty("", null));
        assertNull(String.stripAll((String[]) null));
        assertArrayEquals(new String[] {"a", null, "b"}, String.stripAll(" a ", null, "\tb"));
        assertArrayEquals(new String[] {"b"}, String.stripAll(new String[] {"aba"}, "a"));
        assertNull(String.stripToNull(" "));
        assertNull(String.trimToNull(String.valueOf((char) 0x01)));
        assertEquals("", String.trimToEmpty(null));
    }

    @Test
    void nullCharactersThrow() {
        assertThrows(NullPointerException.class, () -> "a".strip((String) null));
        assertThrows(NullPointerException.class, () -> "a".stripStart(null));
        assertThrows(NullPointerException.class, () -> "a".stripEnd(null));
    }
}
