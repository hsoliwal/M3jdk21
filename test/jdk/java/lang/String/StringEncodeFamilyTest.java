/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String encode family (encodePercent ... rot13)
 * @run junit StringEncodeFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringEncodeFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringEncodeFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every encode-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of CPython 3.12 urllib.parse and
 * codecs, node's URI functions and toWellFormed, Go's url.PathEscape and url.QueryEscape, and
 * Guava 33.7.2's PercentEscaper, UrlEscapers and Utf8 on every code point and on every text of up
 * to four units over an escape alphabet (donor-oracle proof in the string-lane record).
 */
public class StringEncodeFamilyTest {
    private static final char HIGH = (char) 0xD835;
    private static final char LOW = (char) 0xDD1E;
    private static final char[] UNITS = {'a', ' ', '+', '%', '~', '*', '/', 'C', '3',
        (char) 0x00E9, HIGH, LOW};
    private static final String[] SAFES = {"", "/", " +", "!$&'()*+,;=:@"};
    private static final String PAIR = "" + HIGH + LOW;
    private static final String FFFD = "\uFFFD";

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

    private static Object call(Function<String, Object> method, String text) {
        try {
            return method.apply(text);
        } catch (RuntimeException e) {
            return e.getClass();
        }
    }

    private static List<Object> answers(String s) {
        List<Function<String, Object>> methods = new ArrayList<>(List.of(String::encodePercent,
                String::encodePercentPlus, String::decodePercent, String::decodePercentPlus,
                String::encodeUriComponent, String::encodeUri, String::decodeUriComponent,
                String::decodeUri, String::toWellFormed, String::utf8Length, String::rot13));
        for (String safe : SAFES) {
            methods.add(t -> t.encodePercent(safe));
            methods.add(t -> t.encodePercentPlus(safe));
        }
        List<Object> out = new ArrayList<>();
        for (Function<String, Object> method : methods) out.add(call(method, s));
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
    void encodePercentRules() {
        assertEquals("a%20b~%2A-._", "a b~*-._".encodePercent());
        assertEquals("%C3%A9%E2%82%AC", "\u00E9\u20AC".encodePercent());
        assertEquals("%F0%9D%94%9E", PAIR.encodePercent(), "a surrogate pair is one character");
        String unreserved = "AZaz09-._~";
        assertSame(unreserved, unreserved.encodePercent());
        assertEquals("a/b%3F", "a/b?".encodePercent("/"));
        assertEquals("%C3%A9", "\u00E9".encodePercent("\u00E9"), "non-ASCII safe is ignored");
        assertEquals("a+b%2Bc", "a b+c".encodePercentPlus());
        assertEquals("a+b+c", "a b+c".encodePercentPlus("+ "), "a space is always a plus sign");
        assertThrows(IllegalArgumentException.class, () -> ("a" + HIGH).encodePercent());
        assertThrows(IllegalArgumentException.class, () -> ("" + LOW + "a").encodePercentPlus());
        assertThrows(IllegalArgumentException.class, () -> ("" + HIGH + HIGH).encodeUri());
        assertThrows(NullPointerException.class, () -> "a".encodePercent(null));
        assertThrows(NullPointerException.class, () -> "a".encodePercentPlus(null));
    }

    @Test
    void decodePercentRules() {
        assertEquals("AB", "%41%42".decodePercent());
        assertEquals("\u00E9\u00E9", "%C3%A9%c3%a9".decodePercent());
        String plain = "100% %zz %4";
        assertSame(plain, plain.decodePercent(), "a malformed escape is kept");
        assertEquals(FFFD, "%C3".decodePercent());
        assertEquals(FFFD + FFFD + FFFD, "%ED%A0%80".decodePercent(),
                "one replacement per maximal ill-formed subpart");
        assertEquals(FFFD + "\u00E9" + FFFD, "%C3\u00E9%A9".decodePercent());
        assertEquals(FFFD + "A" + FFFD, "%C3A%A9".decodePercent());
        assertEquals(PAIR, "%F0%9D%94%9E".decodePercent());
        assertEquals("a+b", "a+b".decodePercent());
        assertEquals("a b+", "a+b%2B".decodePercentPlus());
        assertEquals("% 1", "%+1".decodePercentPlus());
        assertEquals("" + HIGH + "A", (HIGH + "%41").decodePercent(), "a lone surrogate is kept");
    }

    @Test
    void uriRules() {
        assertEquals("a%20b%3B%2F%3F%23!~*'()", "a b;/?#!~*'()".encodeUriComponent());
        assertEquals("a%20b;/?#!~*'()", "a b;/?#!~*'()".encodeUri());
        assertEquals("%5B%5D%25", "[]%".encodeUri());
        assertEquals(";/A", "%3B%2fA".decodeUriComponent());
        assertEquals("%3B%2fA%", "%3B%2f%41%25".decodeUri(),
                "reserved escapes are kept as written");
        assertEquals(PAIR, "%F0%9D%94%9E".decodeUriComponent());
        assertEquals("" + LOW, ("" + LOW).decodeUriComponent());
        String reserved = "%2F%3f";
        assertSame(reserved, reserved.decodeUri());
        for (String bad : List.of("%", "%4", "%G1", "%C3", "%C3%41", "%C0%80", "%ED%A0%80",
                "%F4%90%80%80", "%80", "%F8%80%80%80%80", "%E0%A0")) {
            assertThrows(IllegalArgumentException.class, () -> bad.decodeUriComponent(), bad);
            assertThrows(IllegalArgumentException.class, () -> bad.decodeUri(), bad);
        }
    }

    @Test
    void wellFormedLengthAndRot13Rules() {
        assertEquals("a" + FFFD + "b" + FFFD, ("a" + HIGH + "b" + LOW).toWellFormed());
        String wellFormed = "a" + PAIR + "\u00E9";
        assertSame(wellFormed, wellFormed.toWellFormed());
        assertEquals(0, "".utf8Length());
        assertEquals(1 + 2 + 3 + 4, ("a\u00E9\u20AC" + PAIR).utf8Length());
        assertEquals("Unpaired surrogate at index 1",
                assertThrows(IllegalArgumentException.class, () -> ("a" + HIGH + HIGH).utf8Length())
                        .getMessage());
        assertEquals("Uryyb, Jbeyq!", "Hello, World!".rot13());
        String noLetter = "123 \u00E9";
        assertSame(noLetter, noLetter.rot13());
        assertEquals("NOPnop\u00E9", "ABCabc\u00E9".rot13());
    }

    @Test
    void longTextsAcrossWindows() {
        String a510 = "a".repeat(510);
        String a511 = "a".repeat(511);
        assertEquals("%20" + a510 + "%F0%9D%94%9E", (" " + a510 + PAIR).encodePercent(),
                "the pair straddles the first window");
        assertEquals("%20" + a511 + "%F0%9D%94%9E", (" " + a511 + PAIR).encodePercent());
        assertEquals(a511 + "%F0%9D%94%9E", (a511 + PAIR).encodePercent());
        assertThrows(IllegalArgumentException.class,
                () -> (" " + a511 + HIGH + "a").encodePercent());
        assertEquals(" " + a510 + PAIR + FFFD, (" " + a510 + PAIR + HIGH).toWellFormed());
        assertEquals(1 + 510 + 4, (" " + a510 + PAIR).utf8Length());
        assertEquals(1 + 511 + 4, (" " + a511 + PAIR).utf8Length());
        assertEquals("\u00E9".repeat(600), "%C3%A9".repeat(600).decodePercent());
        assertEquals("x".repeat(1023) + "\u00E9",
                ("x".repeat(1023) + "%C3%A9").decodeUriComponent());
        assertEquals("n".repeat(1030), "a".repeat(1030).rot13());
    }
}
