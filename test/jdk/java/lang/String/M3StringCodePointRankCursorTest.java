/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3 UTF-16 paired-low rank/select equals Character offset semantics
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCodePointRankCursorTest
 * @run main/othervm -Xint -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCodePointRankCursorTest
 */

import java.util.Random;

public class M3StringCodePointRankCursorTest {
    private static long checks;

    public static void main(String[] args) {
        exhaustiveSmall();
        randomized();
        seamAndCapacityEdges();
        System.out.println("M3_CODEPOINT_RANK_CURSOR_PASS checks=" + checks);
    }

    private static void exhaustiveSmall() {
        char[] alphabet = {'a', '\ud83d', '\ude42', '\udc00'};
        for (int length = 0; length <= 6; length++) {
            int combinations = 1 << (2 * length);
            for (int seed = 0; seed < combinations; seed++) {
                char[] units = new char[length];
                int value = seed;
                for (int at = 0; at < length; at++) {
                    units[at] = alphabet[value & 3];
                    value >>>= 2;
                }
                compareAll(units);
            }
        }
    }

    private static void randomized() {
        Random random = new Random(0x4d335250434f4445L);
        char[] alphabet = {'a', 'b', 'X', '\u00ff', '\uffff',
                '\ud83d', '\ude42', '\ud800', '\udc00'};
        for (int trial = 0; trial < 400; trial++) {
            int length = random.nextInt(257);
            char[] units = new char[length];
            for (int at = 0; at < length; at++) {
                units[at] = alphabet[random.nextInt(alphabet.length)];
            }
            compareAll(units);
        }
    }

    private static void seamAndCapacityEdges() {
        check(new char[] {'\ud83d', '\ude42', '\ud83d', '\ude42'}, 0, 1);
        check(new char[] {'\ud83d', '\ude42', '\ud83d', '\ude42'}, 2, -1);
        check(new char[] {'\ud83d', '\ude42', '\ud83d', '\ude42'}, 1, 1);
        check(new char[] {'\ud83d', '\ude42', '\ud83d', '\ude42'}, 3, -1);

        for (int length : new int[] {32767, 32768, 32769}) {
            char[] units = new char[length];
            for (int i = 0; i < units.length; i++) {
                units[i] = (i % 3 == 0) ? '\ud83d'
                        : (i % 3 == 1) ? '\ude42' : 'Z';
            }
            for (int at : new int[] {0, 1, 2, 3, length / 2, length - 1, length}) {
                for (int offset : new int[] {Integer.MIN_VALUE, -100, -1, 0, 1,
                        100, Integer.MAX_VALUE}) {
                    check(units, at, offset);
                }
            }
        }
    }

    private static void compareAll(char[] units) {
        for (int index = 0; index <= units.length; index++) {
            int backward = Character.codePointCount(units, 0, index);
            int forward = Character.codePointCount(units, index, units.length - index);
            int[] deltas = {Integer.MIN_VALUE, -backward - 1, -backward,
                    -backward / 2, -1, 0, 1, forward / 2, forward,
                    forward + 1, Integer.MAX_VALUE};
            for (int delta : deltas) {
                check(units, index, delta);
            }
        }
    }

    private static void check(char[] units, int index, int delta) {
        String oracle = new String(units);
        int split = units.length >>> 1;
        // Force a composition that may cross a surrogate pair at the seam.
        String subject = String.join("", oracle.substring(0, split),
                oracle.substring(split));
        int expected = -1;
        boolean expectedFailure = false;
        try {
            expected = Character.offsetByCodePoints(
                    new StringBuilder(oracle), index, delta);
        } catch (IndexOutOfBoundsException invalid) {
            expectedFailure = true;
        }
        boolean actualFailure = false;
        int actual = -1;
        try {
            actual = subject.offsetByCodePoints(index, delta);
        } catch (IndexOutOfBoundsException invalid) {
            actualFailure = true;
        }
        checks++;
        if (expectedFailure != actualFailure || (!expectedFailure && expected != actual)) {
            throw new AssertionError("length=" + units.length + " index=" + index
                    + " delta=" + delta + " expected=" + expected
                    + " actual=" + actual + " expectedFailure=" + expectedFailure
                    + " actualFailure=" + actualFailure);
        }
    }
}
