/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 composite String literal search must match independent UTF-16 oracles at tuple and surrogate seams
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCompositeLiteralSearchCursorTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class M3StringCompositeLiteralSearchCursorTest {
    private static final char[] UNITS = {'a', 'b', 'c', '|', '\0', '\ud83d', '\ude42', '\u00e9', '\u03a3', '\ud800', '\udfff'};
    private static long checks;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label + " check=" + checks);
    }
    private static int exactForward(char[] haystack, char[] needle, int from, int end) {
        int start = Math.clamp(from, 0, haystack.length);
        end = Math.min(end, haystack.length);
        if (needle.length == 0) return Math.min(start, end);
        for (int i = start; i <= end - needle.length; i++) {
            int matched = 0;
            while (matched < needle.length && haystack[i + matched] == needle[matched]) matched++;
            if (matched == needle.length) return i;
        }
        return -1;
    }
    private static int exactReverse(char[] haystack, char[] needle, int from) {
        int maximum = Math.min(from, haystack.length - needle.length);
        if (needle.length == 0) return maximum;
        for (int i = maximum; i >= 0; i--) {
            int matched = 0;
            while (matched < needle.length && haystack[i + matched] == needle[matched]) matched++;
            if (matched == needle.length) return i;
        }
        return -1;
    }
    private static String composite(char[] characters, Random random) {
        List<String> slices = new ArrayList<>();
        for (int at = 0; at < characters.length;) {
            int next = Math.min(characters.length, at + 1 + random.nextInt(8));
            String padded = "@" + new String(characters, at, next - at) + "#";
            slices.add(padded.substring(1, padded.length() - 1));
            at = next;
        }
        if (slices.isEmpty()) return String.join("", "", "");
        return String.join("", slices);
    }
    private static String segmented(char[] characters, int segmentWidth) {
        List<String> pieces = new ArrayList<>();
        for (int at = 0; at < characters.length; at += segmentWidth) {
            int end = Math.min(characters.length, at + segmentWidth);
            pieces.add(new String(characters, at, end - at));
        }
        return String.join("", pieces);
    }

    private static void evaluate(String composite, char[] original, String needle,
                                 char[] needleArray, String label) {
        check(composite.length() == original.length, label + " length");
        for (int i = 0; i < original.length; i++) {
            check(composite.charAt(i) == original[i], label + " unit=" + i);
        }
        for (int from = -1; from <= original.length + 1; from += Math.max(1, original.length / 9)) {
            int end = Math.max(0, original.length - Math.max(0, from) / 3);
            int forward = exactForward(original, needleArray, from, original.length);
            int bounded = exactForward(original, needleArray, from, end);
            int reverse = exactReverse(original, needleArray, from);
            check(composite.indexOf(needle, from) == forward, label + " forward from=" + from);
            if (from >= 0 && from <= end) {
                check(composite.indexOf(needle, from, end) == bounded, label + " bounded from=" + from);
            }
            check(composite.lastIndexOf(needle, from) == reverse, label + " reverse from=" + from);
        }
    }
    public static void main(String[] args) {
        Random random = new Random(0x4d33535452494e47L);
        for (int repetition = 0; repetition < 1200; repetition++) {
            int length = random.nextInt(420);
            char[] source = new char[length];
            for (int i = 0; i < source.length; i++) source[i] = UNITS[random.nextInt(UNITS.length)];
            String joined = composite(source, random);
            int begin = random.nextInt(source.length + 1);
            int end = begin + random.nextInt(source.length - begin + 1);
            String viewed = joined.substring(begin, end);
            char[] oracle = java.util.Arrays.copyOfRange(source, begin, end);
            int needleLength = random.nextInt(29);
            char[] needleArray = new char[needleLength];
            for (int i = 0; i < needleLength; i++) needleArray[i] = UNITS[random.nextInt(UNITS.length)];
            if (oracle.length >= needleLength && needleLength > 0 && random.nextBoolean()) {
                int matchAt = random.nextInt(oracle.length - needleLength + 1);
                System.arraycopy(oracle, matchAt, needleArray, 0, needleLength);
            }
            String needle = composite(needleArray, random);
            evaluate(viewed, oracle, needle, needleArray, "random-" + repetition);
        }
        String seam = String.join("", "\ud83d", "\ude42", "--", "ab", "c", "defghi", "\ud800", "z");
        evaluate(seam, "\ud83d\ude42--abcdefghi\ud800z".toCharArray(),
                String.join("", "c", "defghi"), "cdefghi".toCharArray(), "seam-bmh");
        evaluate(seam, "\ud83d\ude42--abcdefghi\ud800z".toCharArray(),
                String.join("", "\ud83d", "\ude42"), "\ud83d\ude42".toCharArray(), "seam-surrogate");
        String big = String.join("", java.util.Collections.nCopies(512, "x"));
        evaluate(big, big.toCharArray(), String.join("", "xxx", "xxxxx"), "xxxxxxxx".toCharArray(), "repeated-bmh");
        longTrigramPrefilterPreservesExactSearch();
        System.out.println("M3_COMPOSITE_LITERAL_CURSOR_TEST_PASS checks=" + checks);
    }

    private static void longTrigramPrefilterPreservesExactSearch() {
        char[] sourceUnits = new char[32_768];
        char[] cycle = {'a', 'a', 'b', 'a', 'a', 'a', '#'};
        for (int index = 0; index < sourceUnits.length; index++) {
            sourceUnits[index] = cycle[index % cycle.length];
        }

        char[] patternUnits = new char[8_192];
        for (int index = 0; index < patternUnits.length - 1; index++) patternUnits[index] = 'a';
        patternUnits[patternUnits.length - 1] = 'b';

        String pattern = segmented(patternUnits, 64);
        String absent = segmented(sourceUnits, 64);
        int absentOracle = exactForward(sourceUnits, patternUnits, 0, sourceUnits.length);
        check(absentOracle == -1, "long trigram-superset exact oracle");
        check(absent.length() == 32_768 && pattern.length() == 8_192,
                "trigram precompute admission boundaries");
        check(absent.indexOf(pattern) == absentOracle,
                "long composite absent despite complete trigram superset");

        int expectedMatch = 16_384;
        char[] presentUnits = sourceUnits.clone();
        System.arraycopy(patternUnits, 0, presentUnits, expectedMatch, patternUnits.length);
        String present = segmented(presentUnits, 64);
        int presentOracle = exactForward(presentUnits, patternUnits, 0, presentUnits.length);
        check(presentOracle == expectedMatch, "long trigram-superset exact match oracle");
        check(present.indexOf(pattern) == presentOracle,
                "long composite match survives trigram prefilter");
    }
    }
}
