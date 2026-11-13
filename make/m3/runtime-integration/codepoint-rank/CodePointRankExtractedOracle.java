// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.util.Arrays;
import java.util.Random;

/**
 * Stock-Java oracle against Geometry extracted unmodified from current java.lang
 * target source. It proves a rank-select algorithm in isolation, not VM wiring.
 */
public final class CodePointRankExtractedOracle {
    private static long checks;
    private static final char[] ALPHABET = {
        'x', '\ud83d', '\ude42', '\ud800', '\udc00', '\u0000', '\uffff'
    };

    public static void main(String[] args) {
        for (int length = 0; length <= 6; length++) {
            int count = 1;
            for (int j = 0; j < length; j++) count *= 4;
            for (int seed = 0; seed < count; seed++) {
                char[] units = new char[length];
                int n = seed;
                for (int j = 0; j < length; j++) {
                    units[j] = ALPHABET[n & 3];
                    n >>>= 2;
                }
                check(units);
            }
        }
        Random random = new Random(0x4d33434f4445524cL);
        for (int trial = 0; trial < 384; trial++) {
            char[] units = new char[random.nextInt(320)];
            for (int i = 0; i < units.length; i++) {
                units[i] = ALPHABET[random.nextInt(ALPHABET.length)];
            }
            check(units);
        }
        for (int length : new int[] {32767, 32768, 32769}) {
            char[] units = new char[length];
            for (int i = 0; i < length; i++) {
                units[i] = ALPHABET[i % ALPHABET.length];
            }
            checkBoundaries(units);
        }
        System.out.println("M3_CODEPOINT_RANK_EXTRACTED_JAVA_PASS checks=" + checks);
    }

    private static void check(char[] units) {
        StringBuilder oracle = new StringBuilder(new String(units));
        int[] pairs = new int[units.length >>> 1];
        int found = 0;
        for (int j = 1; j < units.length; j++) {
            if (Character.isHighSurrogate(units[j - 1])
                    && Character.isLowSurrogate(units[j])) {
                pairs[found++] = j;
            }
        }
        RankGeometryUnderTest.Geometry geometry =
                new RankGeometryUnderTest.Geometry(
                        units.length, Arrays.copyOf(pairs, found));
        for (int index = 0; index <= units.length; index++) {
            int before = Character.codePointCount(units, 0, index);
            int after = Character.codePointCount(units, index, units.length - index);
            int[] offsets = {Integer.MIN_VALUE, -before - 1, -before,
                    -before / 2, -1, 0, 1, after / 2, after,
                    after + 1, Integer.MAX_VALUE};
            for (int amount : offsets) compare(oracle, geometry, index, amount);
        }
    }

    private static void checkBoundaries(char[] units) {
        StringBuilder oracle = new StringBuilder(new String(units));
        int[] pairs = new int[units.length >>> 1];
        int found = 0;
        for (int j = 1; j < units.length; j++) {
            if (Character.isHighSurrogate(units[j - 1])
                    && Character.isLowSurrogate(units[j])) {
                pairs[found++] = j;
            }
        }
        RankGeometryUnderTest.Geometry geometry =
                new RankGeometryUnderTest.Geometry(
                        units.length, Arrays.copyOf(pairs, found));
        for (int index : new int[]{0, 1, 2, 3,
                units.length / 2, units.length - 1, units.length}) {
            for (int amount : new int[]{Integer.MIN_VALUE, -1000,
                    -1, 0, 1, 1000, Integer.MAX_VALUE}) {
                compare(oracle, geometry, index, amount);
            }
        }
    }

    private static void compare(StringBuilder oracle,
                                RankGeometryUnderTest.Geometry geometry,
                                int index, int offset) {
        boolean failedExpected = false;
        int expected = -1;
        try {
            expected = Character.offsetByCodePoints(oracle, index, offset);
        } catch (IndexOutOfBoundsException failure) {
            failedExpected = true;
        }
        boolean failedActual = false;
        int actual = -1;
        try {
            actual = geometry.offsetByCodePoints(index, offset);
        } catch (IndexOutOfBoundsException failure) {
            failedActual = true;
        }
        checks++;
        if (failedExpected != failedActual
                || (!failedExpected && expected != actual)) {
            throw new AssertionError(
                    "index=" + index + " offset=" + offset
                    + " expected=" + expected + " actual=" + actual
                    + " expectedFailure=" + failedExpected
                    + " actualFailure=" + failedActual);
        }
    }
}
