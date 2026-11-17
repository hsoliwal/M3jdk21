/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String split family (split ... chunked)
 * @run junit StringSplitFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringSplitFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringSplitFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every split-family method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-lang3 3.22.0, CPython
 * 3.12 and the Kotlin standard library on every text of up to four units over the alphabet below
 * (donor-oracle proof in the string-lane record).
 */
public class StringSplitFamilyTest {
    private static final char HIGH = (char) 0xD835;
    private static final char LOW = (char) 0xDC00;
    private static final String PAIR = "" + HIGH + LOW;
    private static final char[] UNITS = {'a', 'B', ' ', ',', '\r', '\n', HIGH, LOW};

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
        out.add(s.split());
        out.add(s.splitPreserveAllTokens());
        for (char c : new char[] {' ', ',', HIGH}) {
            out.add(s.split(c));
            out.add(s.splitPreserveAllTokens(c));
        }
        for (String chars : new String[] {"", " ,", "" + LOW}) {
            out.add(s.splitPreserveAllTokens(chars));
            out.add(s.splitPreserveAllTokens(chars, 2));
        }
        for (String sep : new String[] {"", ",", ", ", PAIR}) {
            out.add(s.splitByWholeSeparator(sep));
            out.add(s.splitByWholeSeparator(sep, 2));
            out.add(s.splitByWholeSeparatorPreserveAllTokens(sep));
            out.add(s.splitByWholeSeparatorPreserveAllTokens(sep, 2));
        }
        out.add(s.splitByCharacterType());
        out.add(s.splitByCharacterTypeCamelCase());
        for (String sep : new String[] {",", "" + HIGH, PAIR, "aB"}) {
            out.add(s.partition(sep));
            out.add(s.rpartition(sep));
            out.add(s.rsplit(sep, -1));
            out.add(s.rsplit(sep, 1));
        }
        out.add(s.splitLines(false));
        out.add(s.splitLines(true));
        for (int size = 1; size <= 3; size++) {
            out.add(s.chunked(size));
        }
        out.replaceAll(answer -> Arrays.asList((String[]) answer));
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
    void commonsRules() {
        assertArrayEquals(new String[] {"a", "b", ".c"}, "a b  .c".split());
        assertArrayEquals(new String[0], "".split());
        assertArrayEquals(new String[] {"a", "b", " c"}, "a.b.. c".split('.'));
        assertArrayEquals(new String[] {"a", "", "b", "c"}, "a..b.c".splitPreserveAllTokens('.'));
        assertArrayEquals(new String[] {"", "a", ""}, " a ".splitPreserveAllTokens());
        assertArrayEquals(new String[] {"ab", "cd:ef"}, "ab:cd:ef".splitPreserveAllTokens(":", 2));
        assertArrayEquals(new String[] {"ab   de fg"}, "ab   de fg".splitPreserveAllTokens(""),
                "empty separator units: no separator");
        assertArrayEquals(new String[] {"ab", "cd"}, "ab-!-cd-!-".splitByWholeSeparator("-!-"));
        assertArrayEquals(new String[] {"ab", "cd", ""},
                "ab-!-cd-!-".splitByWholeSeparatorPreserveAllTokens("-!-"));
        assertArrayEquals(new String[] {"ab", "de", "fg"}, "ab   de fg".splitByWholeSeparator(""),
                "an empty whole separator splits at whitespace");
        String abstemiously = "abstemiouslyaeiouyabstemiouslyaeiouyabstemiously";
        assertArrayEquals(new String[] {"abstemiously", "abstemiouslyaeiouyabstemiously"},
                abstemiously.splitByWholeSeparator("aeiouy", 2));
        assertArrayEquals(new String[] {"foo", "B", "ar"}, "fooBar".splitByCharacterType());
        assertArrayEquals(new String[] {"ASF", "Rules"},
                "ASFRules".splitByCharacterTypeCamelCase());
        assertArrayEquals(new String[] {"AB", PAIR + "cd"},
                ("AB" + PAIR + "cd").splitByCharacterTypeCamelCase(),
                "a supplementary upper-case letter starts the lower-case run");
        assertThrows(NullPointerException.class, () -> "a".splitByWholeSeparator(null));
        assertThrows(NullPointerException.class, () -> "a".splitPreserveAllTokens((String) null));
    }

    @Test
    void pythonAndKotlinRules() {
        assertArrayEquals(new String[] {"a.b", ".", "c"}, "a.b.c".rpartition("."));
        assertArrayEquals(new String[] {"a", ".", "b.c"}, "a.b.c".partition("."));
        assertArrayEquals(new String[] {"abc", "", ""}, "abc".partition("x"));
        assertArrayEquals(new String[] {"", "", "abc"}, "abc".rpartition("x"));
        assertArrayEquals(new String[] {PAIR, "", ""}, PAIR.partition("" + HIGH),
                "half of a surrogate pair is not found, as among code points");
        assertArrayEquals(new String[] {"a,b", "c"}, "a,b,c".rsplit(",", 1));
        assertArrayEquals(new String[] {"a", "", "b"}, "a,,b".rsplit(",", -1));
        assertArrayEquals(new String[] {""}, "".rsplit(",", -1));
        assertArrayEquals(new String[] {"a", ""}, "aaa".rsplit("aa", -1), "found right to left");
        assertArrayEquals(new String[] {"a", "b", "c", "d"},
                "a\r\nb\rc\u2028d\n".splitLines(false));
        assertArrayEquals(new String[] {"a\r\n", "b\r", "c\u2028", "d\n"},
                "a\r\nb\rc\u2028d\n".splitLines(true));
        assertArrayEquals(new String[] {"\n", "x\u0085"}, "\nx\u0085".splitLines(true));
        assertArrayEquals(new String[0], "".splitLines(false));
        assertArrayEquals(new String[] {"ab", "cd", "e"}, "abcde".chunked(2));
        assertArrayEquals(new String[0], "".chunked(2));
        assertThrows(IllegalArgumentException.class, () -> "a".partition(""));
        assertThrows(IllegalArgumentException.class, () -> "a".rsplit("", 1));
        assertThrows(IllegalArgumentException.class, () -> "a".chunked(0));
        assertThrows(NullPointerException.class, () -> "a".rpartition(null));
    }

    @Test
    void longTextsAcrossWindows() {
        String crlf = "a".repeat(511) + "\r\nb";
        assertArrayEquals(new String[] {"a".repeat(511) + "\r\n", "b"}, crlf.splitLines(true));
        assertArrayEquals(new String[] {"a".repeat(511), "b"}, crlf.splitLines(false));
        String spaced = "x".repeat(600) + " y";
        assertArrayEquals(new String[] {"x".repeat(600), "y"}, spaced.split());
        String camel = "x".repeat(511) + PAIR + "b";
        assertArrayEquals(new String[] {"x".repeat(511), PAIR + "b"},
                camel.splitByCharacterTypeCamelCase());
        assertEquals(3, "z".repeat(1025).chunked(512).length);
    }
}
