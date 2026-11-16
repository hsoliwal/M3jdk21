/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of String.countMatches for char values, surrogates and code points
 * @run junit StringCountMatchesTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringCountMatchesTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * countMatches(int) against a count over the char array, for a whole String, a range of a
 * longer String and a concatenation of two halves.
 */
public class StringCountMatchesTest {
    private static final char HIGH = (char) 0xD83D;
    private static final char LOW = (char) 0xDE42;
    private static final int PAIR = Character.toCodePoint(HIGH, LOW);
    private static final List<Integer> WANTED =
            List.of((int) 'x', (int) 'q', 0xE9, 0x100, (int) HIGH, (int) LOW, PAIR, 0x10000);

    static Stream<Arguments> texts() {
        return Stream.of(0, 1, 33, 257, 5_000, 33_000).flatMap(length ->
                IntStream.range(0, 3).mapToObj(kind -> Arguments.of(length, kind)));
    }

    private static char unit(int index, int kind) {
        if (index % 5 == 0) return 'x';
        if (index % 7 == 0) return (char) 0xE9;
        if (kind >= 1 && index % 11 == 0) return (char) 0x100;
        if (kind == 2 && index % 13 == 1) return HIGH;
        if (kind == 2 && index % 13 == 2) return LOW;
        return (char) ('a' + index % 16);
    }

    @ParameterizedTest(name = "{0} chars, kind {1}")
    @MethodSource("texts")
    void countsEveryForm(int length, int kind) {
        char[] units = new char[length];
        IntStream.range(0, length).forEach(index -> units[index] = unit(index, kind));
        String whole = new String(units);
        String range = new String(("<<" + whole + ">>").toCharArray()).substring(2, 2 + length);
        String halves = new String(units, 0, length / 2)
                .concat(new String(units, length / 2, length - length / 2));
        for (int ch : WANTED) {
            int expected = expected(units, ch);
            assertEquals(expected, whole.countMatches(ch), "whole U+" + Integer.toHexString(ch));
            assertEquals(expected, range.countMatches(ch), "range U+" + Integer.toHexString(ch));
            assertEquals(expected, halves.countMatches(ch), "halves U+" + Integer.toHexString(ch));
        }
    }

    @Test
    void invalidCodePointsNeverOccur() {
        assertEquals(0, "abc".countMatches(-1));
        assertEquals(0, "abc".countMatches(Character.MAX_CODE_POINT + 1));
        assertEquals(0, "".countMatches('a'));
    }

    @Test
    void surrogateValuesCountUnitsAndPairsCountOnce() {
        String text = "" + HIGH + LOW + HIGH + HIGH + LOW + LOW;
        assertEquals(3, text.countMatches(HIGH));
        assertEquals(3, text.countMatches(LOW));
        assertEquals(2, text.countMatches(PAIR));
    }

    private static int expected(char[] units, int ch) {
        if (Character.isBmpCodePoint(ch)) {
            return (int) IntStream.range(0, units.length).filter(i -> units[i] == ch).count();
        }
        char high = Character.highSurrogate(ch);
        char low = Character.lowSurrogate(ch);
        return (int) IntStream.range(0, units.length - 1)
                .filter(i -> units[i] == high && units[i + 1] == low).count();
    }
}
