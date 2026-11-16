/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of String.indexOfDifference in every storage form
 * @run junit StringIndexOfDifferenceTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringIndexOfDifferenceTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringIndexOfDifferenceTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * indexOfDifference against the documented rule applied to the char arrays: one changed unit at
 * a chosen position, a text and its prefix, and equal texts, with each side as a whole String,
 * a range of a longer String and a concatenation of two halves.
 */
public class StringIndexOfDifferenceTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE42;

    static Stream<Arguments> cases() {
        List<Arguments> cases = new ArrayList<>();
        for (int length : new int[] {1, 9, 64, 257, 5_000}) {
            for (int kind = 0; kind < 3; kind++) {
                char[] left = text(length, kind);
                for (int at : new int[] {0, length / 2, length - 1}) {
                    char[] right = left.clone();
                    right[at] = (char) (right[at] + 1);
                    cases.add(Arguments.of(left, right));
                }
                cases.add(Arguments.of(left, left.clone()));
                cases.add(Arguments.of(left, java.util.Arrays.copyOf(left, length / 2)));
            }
        }
        return cases.stream();
    }

    /** Kind 0 is Latin-1, kind 1 has wide chars, kind 2 has surrogate pairs and lone surrogates. */
    private static char[] text(int length, int kind) {
        char[] units = new char[length];
        IntStream.range(0, length).forEach(index -> units[index] =
                kind == 1 && index % 5 == 0 ? (char) 0x3B1
                : kind == 2 && index % 6 == 1 ? HIGH
                : kind == 2 && index % 6 == 2 ? LOW
                : kind == 2 && index % 11 == 4 ? HIGH
                : (char) ('a' + index % 26));
        return units;
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("cases")
    void everyFormOfBothSides(char[] left, char[] right) {
        int expected = expected(left, right);
        for (String a : forms(left)) {
            for (String b : forms(right)) {
                assertEquals(expected, a.indexOfDifference(b));
                assertEquals(expected, b.indexOfDifference(a));
            }
        }
    }

    @Test
    void pairsStayWholeAndPrefixesGiveTheShorterLength() {
        String pair = "" + HIGH + LOW;
        assertEquals(0, (HIGH + "a").indexOfDifference(pair));
        assertEquals(0, pair.indexOfDifference("" + HIGH + (char) (LOW + 1)));
        assertEquals(1, (HIGH + "a").indexOfDifference(HIGH + "b"));
        assertEquals(1, ("" + HIGH).indexOfDifference(pair));
        assertEquals(3, "abc".indexOfDifference("abcde"));
        assertEquals(-1, "".indexOfDifference(""));
        assertEquals(-1, new String("same").indexOfDifference("same"));
        assertThrows(NullPointerException.class, () -> "a".indexOfDifference(null));
    }

    private static List<String> forms(char[] units) {
        String whole = new String(units);
        int middle = units.length / 2;
        return List.of(whole,
                new String(("<<" + whole + ">>").toCharArray()).substring(2, 2 + units.length),
                new String(units, 0, middle)
                        .concat(new String(units, middle, units.length - middle)));
    }

    /** The documented rule, applied to the arrays. */
    private static int expected(char[] a, char[] b) {
        int limit = Math.min(a.length, b.length);
        int same = IntStream.range(0, limit).filter(i -> a[i] != b[i]).findFirst().orElse(limit);
        if (same == limit) return a.length == b.length ? -1 : limit;
        boolean insidePair = same > 0 && Character.isHighSurrogate(a[same - 1])
                && (Character.isLowSurrogate(a[same]) || Character.isLowSurrogate(b[same]));
        return insidePair ? same - 1 : same;
    }
}
