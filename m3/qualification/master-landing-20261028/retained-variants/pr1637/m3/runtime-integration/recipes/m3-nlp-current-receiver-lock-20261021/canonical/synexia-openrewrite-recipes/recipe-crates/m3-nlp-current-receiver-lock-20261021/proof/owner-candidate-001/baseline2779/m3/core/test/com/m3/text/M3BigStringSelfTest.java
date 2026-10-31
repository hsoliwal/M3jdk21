/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Arrays;
import java.util.regex.Pattern;

/** Dependency-free deterministic contract proof for the B-tree/summary implementation. */
public final class M3BigStringSelfTest {
    private static long checks;
    private M3BigStringSelfTest() { }

    public static void main(String[] args) throws Exception {
        compositionAndRanges();
        summariesAndUnicodeBoundaries();
        literalSearchAndRegex();
        randomizedTreeAndRangeParity();
        hugeLongCoordinates();
        invalidInputs();
        System.out.println("M3_BIG_STRING_OK checks=" + checks);
    }

    private static void compositionAndRanges() throws Exception {
        M3BigString a = M3BigString.of("ab", "", "c");
        M3BigString b = M3BigString.of("de", "f");
        M3BigString ab = M3BigString.concat(a, b);
        M3BigString grouped = M3BigString.concat(M3BigString.concat(M3BigString.of("ab"), M3BigString.of("c")), b);
        eq(6L, ab.length(), "length"); eq("abcdef", ab.flatten(), "concat value");
        eq(ab.flatten(), grouped.flatten(), "parenthesization independent content");
        eq('a', ab.charAt(0), "first"); eq('f', ab.charAt(5), "last");
        eq("bcd", ab.subSequence(1, 4).flatten(), "slice");
        eq("abc", ab.subSequence(0, 3).flatten(), "prefix slice");
        eq("def", ab.subSequence(3, 6).flatten(), "suffix slice");
        eq(0L, ab.subSequence(1, 4).indexOf("bc", 0), "range-relative search");
        eq("abcdef", append(ab), "appendTo without flatten path");
        eq("", M3BigString.concat(M3BigString.of(""), M3BigString.of()).flatten(), "empty concat");
    }

    private static void summariesAndUnicodeBoundaries() {
        M3BigString s = M3BigString.of("x\r", "\n", "\uD83D", "\uDE00", "y\n");
        String flat = "x\r\n\uD83D\uDE00y\n";
        M3BigString.Summary summary = s.summary();
        eq(flat, s.flatten(), "summary input");
        eq((long) flat.length(), summary.utf16Length(), "UTF-16 units");
        eq((long) flat.codePointCount(0, flat.length()), summary.codePointCount(), "cross-chunk surrogate summary");
        eq(2L, summary.lineTerminatorCount(), "CRLF counts as one terminator");
        eq(flat.hashCode(), summary.javaStringHash(), "JDK String hash");
        check(!summary.ascii(), "unicode summary");
        eq(summary.codePointCount(), s.subSequence(0, s.length()).summary().codePointCount(), "full range summary");
        eq(1L, s.subSequence(1, 3).summary().lineTerminatorCount(), "CRLF range summary");
        eq(1L, s.subSequence(3, 5).summary().codePointCount(), "surrogate pair range summary");
        M3BigString allAscii = M3BigString.of("abc", "def");
        check(allAscii.summary().ascii(), "ASCII detection");
        eq(0L, allAscii.summary().lineTerminatorCount(), "no line terminators");
    }

    private static void literalSearchAndRegex() {
        M3BigString s = M3BigString.of("prefix ne", "ed", "le suffix", " and needle");
        eq(7L, s.indexOf("needle", 0), "literal across chunks");
        eq(25L, s.indexOf("needle", 8), "second literal occurrence");
        eq(-1L, s.indexOf("absent", 0), "missing literal");
        eq(s.length(), s.indexOf("", s.length()), "empty literal at end");
        check(s.mayContainLiteral("needle"), "bloom positive for present literal");
        check(!s.mayContainLiteral("zzzz"), "bloom negative for absent character");
        check(Pattern.compile("prefix.*suffix.*needle").matcher(s.asCharSequence()).find(), "JDK regex on rope CharSequence");
        check(!Pattern.compile("prefix.*missing").matcher(s.asCharSequence()).find(), "JDK regex negative");
    }

    private static void randomizedTreeAndRangeParity() {
        java.util.Random random = new java.util.Random(0x4d334252L);
        for (int trial = 0; trial < 24; trial++) {
            int count = trial == 0 ? 1 : trial == 1 ? 33 : 80 + random.nextInt(250);
            String[] chunks = new String[count];
            StringBuilder expected = new StringBuilder();
            for (int i = 0; i < count; i++) {
                int length = random.nextInt(12);
                StringBuilder chunk = new StringBuilder(length);
                for (int j = 0; j < length; j++) {
                    char c = switch (random.nextInt(12)) {
                        case 0 -> '\r'; case 1 -> '\n'; case 2 -> '\uD83D';
                        case 3 -> '\uDE00'; default -> (char) ('a' + random.nextInt(8));
                    };
                    chunk.append(c);
                }
                chunks[i] = chunk.toString();
                expected.append(chunks[i]);
            }
            M3BigString actual = M3BigString.of(chunks);
            String flat = expected.toString();
            eq((long) flat.length(), actual.length(), "random length trial " + trial);
            eq(flat.hashCode(), actual.summary().javaStringHash(), "random hash trial " + trial);
            eq((long) flat.codePointCount(0, flat.length()), actual.summary().codePointCount(), "random codepoints trial " + trial);
            eq(lineTerminators(flat), actual.summary().lineTerminatorCount(), "random lines trial " + trial);
            for (int probe = 0; probe < 32 && !flat.isEmpty(); probe++) {
                int at = random.nextInt(flat.length());
                eq(flat.charAt(at), actual.charAt(at), "random char trial " + trial);
                int end = at + random.nextInt(flat.length() - at + 1);
                M3BigString slice = actual.subSequence(at, end);
                String expectedSlice = flat.substring(at, end);
                eq(expectedSlice, slice.flatten(), "random slice trial " + trial);
                eq(expectedSlice.hashCode(), slice.summary().javaStringHash(), "random slice hash trial " + trial);
                eq((long) expectedSlice.codePointCount(0, expectedSlice.length()), slice.summary().codePointCount(), "random slice codepoints trial " + trial);
            }
        }
    }

    private static long lineTerminators(String text) {
        long result = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r' || (c == '\n' && (i == 0 || text.charAt(i - 1) != '\r'))) result++;
        }
        return result;
    }

    private static void hugeLongCoordinates() {
        String megabyte = "a".repeat(1_000_000);
        M3BigString unit = M3BigString.of(megabyte);
        M3BigString[] repeats = new M3BigString[2_200];
        Arrays.fill(repeats, unit);
        M3BigString huge = M3BigString.concat(repeats);
        long expected = 2_200_000_000L;
        eq(expected, huge.length(), "length above Integer.MAX_VALUE");
        eq('a', huge.charAt(0), "huge first");
        eq('a', huge.charAt(1_000_000L), "chunk boundary");
        eq('a', huge.charAt(expected - 1), "huge last");
        eq(2_200_000_000L, huge.summary().codePointCount(), "long code point summary");
        eq(-1L, huge.indexOf("zz", expected - 2), "bounded missing search");
        eq(2_100_000_000L, huge.indexOf("a", 2_100_000_000L), "long-index literal search");
        expect(IllegalStateException.class, huge::flatten, "refuse impossible flatten");
        expect(IllegalStateException.class, huge::asCharSequence, "refuse int-indexed regex facade");
        M3BigString view = huge.subSequence(1_999_999_999L, 2_000_000_003L);
        eq("aaaa", view.flatten(), "huge-coordinate range view");
        eq(4L, view.length(), "view length");
    }

    private static void invalidInputs() {
        expect(NullPointerException.class, () -> M3BigString.of((CharSequence) null), "null chunk");
        expect(IllegalArgumentException.class, () -> M3BigString.of(new StringBuilder("mutable")), "mutable chunk");
        expect(IndexOutOfBoundsException.class, () -> M3BigString.of("x").charAt(1), "bad char index");
        expect(IndexOutOfBoundsException.class, () -> M3BigString.of("x").subSequence(2, 2), "bad range");
    }

    private static String append(M3BigString value) throws Exception {
        StringBuilder result = new StringBuilder(); value.appendTo(result); return result.toString();
    }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
    private static void eq(Object expected, Object actual, String message) { check(expected == null ? actual == null : expected.equals(actual), message + ": expected=" + expected + " actual=" + actual); }
    private static void expect(Class<? extends Throwable> type, Runnable action, String message) {
        checks++;
        try { action.run(); } catch (Throwable thrown) { if (type.isInstance(thrown)) return; throw new AssertionError(message + ": wrong exception", thrown); }
        throw new AssertionError(message + ": no exception");
    }
}
