/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Differential M3 String search/precompute against independent UTF-16 oracles
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPrecomputeSearchTest
 */

import java.util.Random;

public class M3StringPrecomputeSearchTest {
    private static long checks;

    public static void main(String[] args) {
        deterministic();
        randomizedCompositions();
        System.out.println("M3_STRING_PRECOMPUTE_SEARCH_PASS|checks=" + checks);
    }

    private static void deterministic() {
        String left = "alpha";
        String middle = "\u03b2eta";
        String emoji = "\ud83d\ude42";
        String right = "omega";

        String joined = String.join("|", left, middle, emoji, right);
        char[] oracle = "alpha|\u03b2eta|\ud83d\ude42|omega".toCharArray();

        check(equalChars(joined, oracle), "joined content");
        check(joined.startsWith("alpha"), "startsWith");
        check(joined.startsWith("\u03b2eta", 6), "startsWith offset");
        check(joined.endsWith("omega"), "endsWith");
        check(joined.regionMatches(6, "\u03b2eta", 0, 4), "regionMatches");
        check(!joined.regionMatches(6, "\u03b2eto", 0, 4), "region mismatch");

        check(joined.indexOf('a') == naiveIndexOf(oracle, 'a', 0), "indexOf BMP");
        check(joined.indexOf('\u03b2') == naiveIndexOf(oracle, '\u03b2', 0), "indexOf Unicode BMP");
        check(joined.indexOf(0x1f642) == naiveIndexOfCodePoint(oracle, 0x1f642, 0, oracle.length),
                "indexOf supplementary");
        check(joined.indexOf('z') == -1, "absent code unit");

        check(joined.lastIndexOf('a') == naiveLastIndexOf(oracle, 'a', oracle.length - 1),
                "lastIndexOf BMP");
        check(joined.lastIndexOf(0x1f642)
                        == naiveLastIndexOfCodePoint(oracle, 0x1f642, oracle.length - 1),
                "lastIndexOf supplementary");

        check(joined.indexOf("\u03b2eta") == naiveIndexOf(oracle, "\u03b2eta".toCharArray(), 0),
                "indexOf String");
        check(joined.indexOf("eta", 4) == naiveIndexOf(oracle, "eta".toCharArray(), 4),
                "indexOf String from");
        check(joined.indexOf("eta", 0, joined.length())
                        == naiveIndexOfBounded(oracle, "eta".toCharArray(), 0, oracle.length),
                "indexOf String bounded");
        check(joined.lastIndexOf("omega")
                        == naiveLastIndexOf(oracle, "omega".toCharArray(), oracle.length),
                "lastIndexOf String");
        check(joined.indexOf("not-present") == -1, "absent String");

        // The pair is deliberately split across canonical composition leaves.
        String splitSupplementary = String.join("", "\ud83d", "\ude42");
        check(equalChars(splitSupplementary, "\ud83d\ude42".toCharArray()),
                "split surrogate content");
        check(splitSupplementary.indexOf(0x1f642) == 0,
                "supplementary code point across atom seam");
        check(splitSupplementary.lastIndexOf(0x1f642) == 0,
                "reverse supplementary across atom seam");

        String trimJoined = String.join("", " \t", "alpha", " \n");
        check(trimJoined.trim().equals("alpha"), "trim facts");

        String stripJoined = String.join("", "\u2003", "alpha", "\u2002");
        check(stripJoined.strip().equals("alpha"), "strip Unicode");
        check(stripJoined.stripLeading().equals("alpha\u2002"), "stripLeading Unicode");
        check(stripJoined.stripTrailing().equals("\u2003alpha"), "stripTrailing Unicode");

        String blankJoined = String.join("", " \t", "\u2003", "\n");
        check(blankJoined.isBlank(), "blank facts");
        check(blankJoined.strip().isEmpty(), "strip blank");

        String repeated = joined.repeat(3);
        char[] repeatedOracle =
                "alpha|\u03b2eta|\ud83d\ude42|omega".repeat(3).toCharArray();
        check(repeated.indexOf("\ud83d\ude42|omegaalpha")
                        == naiveIndexOf(
                                repeatedOracle, "\ud83d\ude42|omegaalpha".toCharArray(), 0),
                "cross-tuple search");
        check(repeated.lastIndexOf("\u03b2eta")
                        == naiveLastIndexOf(
                                repeatedOracle, "\u03b2eta".toCharArray(), repeatedOracle.length),
                "reverse repeated search");

        String slice = repeated.substring(3, repeated.length() - 4);
        char[] sliceOracle =
                copyRange(repeatedOracle, 3, repeatedOracle.length - 4);
        check(slice.indexOf("\ud83d\ude42")
                        == naiveIndexOf(sliceOracle, "\ud83d\ude42".toCharArray(), 0),
                "range supplementary search");
        check(slice.lastIndexOf("alpha")
                        == naiveLastIndexOf(sliceOracle, "alpha".toCharArray(), sliceOracle.length),
                "range reverse search");

        for (int from = -2; from <= oracle.length + 2; from++) {
            check(joined.indexOf('a', from) == naiveIndexOf(oracle, 'a', from),
                    "char from " + from);
            check(joined.lastIndexOf('a', from) == naiveLastIndexOf(oracle, 'a', from),
                    "char reverse " + from);
            check(joined.indexOf("a", from) == naiveIndexOf(oracle, new char[] {'a'}, from),
                    "String from " + from);
            check(joined.lastIndexOf("a", from)
                            == naiveLastIndexOf(oracle, new char[] {'a'}, from),
                    "String reverse " + from);
        }
    }

    private static void randomizedCompositions() {
        Random random = new Random(0x4d33535452494e47L);
        String[] atoms = {
                "", "a", "bc", " ", "\t", "\n", "\u2003", "\u2002", "\u00e9",
                "\ud83d", "\ude42", "\ud83d\ude42", "XYZ", "aba", "\u0000"
        };

        for (int trial = 0; trial < 2_000; trial++) {
            int pieceCount = 1 + random.nextInt(8);
            String[] pieces = new String[pieceCount];
            int total = 0;
            for (int index = 0; index < pieces.length; index++) {
                pieces[index] = atoms[random.nextInt(atoms.length)];
                total += pieces[index].length();
            }

            char[] oracle = new char[total];
            int at = 0;
            for (String piece : pieces) {
                piece.getChars(0, piece.length(), oracle, at);
                at += piece.length();
            }
            String source = String.join("", pieces);
            check(equalChars(source, oracle), "random content " + trial);

            int targetLength =
                    oracle.length == 0 ? 0 : random.nextInt(Math.min(6, oracle.length) + 1);
            int targetStart =
                    targetLength == 0 ? 0 : random.nextInt(oracle.length - targetLength + 1);
            char[] targetChars = copyRange(oracle, targetStart, targetStart + targetLength);
            String target = new String(targetChars);
            if ((trial & 3) == 0) {
                target = target + "#";
                targetChars = append(targetChars, '#');
            }

            int from = random.nextInt(oracle.length + 7) - 3;
            check(source.indexOf(target, from) == naiveIndexOf(oracle, targetChars, from),
                    "random indexOf " + trial);
            check(source.lastIndexOf(target, from) == naiveLastIndexOf(oracle, targetChars, from),
                    "random lastIndexOf " + trial);

            int begin = oracle.length == 0 ? 0 : random.nextInt(oracle.length + 1);
            int end = begin + random.nextInt(oracle.length - begin + 1);
            check(source.indexOf(target, begin, end)
                            == naiveIndexOfBounded(oracle, targetChars, begin, end),
                    "random bounded indexOf " + trial);

            if (oracle.length != 0) {
                char wanted = oracle[random.nextInt(oracle.length)];
                check(source.indexOf(wanted, from) == naiveIndexOf(oracle, wanted, from),
                        "random char indexOf " + trial);
                check(source.lastIndexOf(wanted, from) == naiveLastIndexOf(oracle, wanted, from),
                        "random char lastIndexOf " + trial);

                int cpStart = random.nextInt(oracle.length);
                int codePoint = Character.codePointAt(oracle, cpStart, oracle.length);
                check(source.indexOf(codePoint, from)
                                == naiveIndexOfCodePoint(oracle, codePoint, from, oracle.length),
                        "random codePoint indexOf " + trial);
                check(source.lastIndexOf(codePoint, from)
                                == naiveLastIndexOfCodePoint(oracle, codePoint, from),
                        "random codePoint lastIndexOf " + trial);
            }

            check(equalChars(source.trim(), naiveTrim(oracle).toCharArray()),
                    "random trim " + trial);
            check(equalChars(source.strip(), naiveStrip(oracle).toCharArray()),
                    "random strip " + trial);
            check(source.isBlank() == naiveIsBlank(oracle), "random blank " + trial);
        }
    }

    private static int naiveIndexOf(char[] source, char[] target, int fromIndex) {
        int from = Math.max(0, fromIndex);
        if (target.length == 0) return Math.min(from, source.length);
        for (int start = from; start <= source.length - target.length; start++) {
            if (matchesAt(source, target, start)) return start;
        }
        return -1;
    }

    private static int naiveIndexOfBounded(
            char[] source, char[] target, int beginIndex, int endIndex) {
        if (target.length == 0) return beginIndex;
        for (int start = beginIndex; start <= endIndex - target.length; start++) {
            if (matchesAt(source, target, start)) return start;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char[] target, int fromIndex) {
        int start = Math.min(fromIndex, source.length - target.length);
        if (target.length == 0) return start < 0 ? -1 : start;
        for (; start >= 0; start--) {
            if (matchesAt(source, target, start)) return start;
        }
        return -1;
    }

    private static boolean matchesAt(char[] source, char[] target, int start) {
        if (start < 0 || start > source.length - target.length) return false;
        for (int index = 0; index < target.length; index++) {
            if (source[start + index] != target[index]) return false;
        }
        return true;
    }

    private static int naiveIndexOf(char[] source, char target, int fromIndex) {
        for (int index = Math.max(0, fromIndex); index < source.length; index++) {
            if (source[index] == target) return index;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char target, int fromIndex) {
        for (int index = Math.min(fromIndex, source.length - 1); index >= 0; index--) {
            if (source[index] == target) return index;
        }
        return -1;
    }

    private static int naiveIndexOfCodePoint(
            char[] source, int codePoint, int fromIndex, int endIndex) {
        int from = Math.max(0, fromIndex);
        if (!Character.isValidCodePoint(codePoint)) return -1;
        if (Character.isBmpCodePoint(codePoint)) {
            return naiveIndexOf(source, (char) codePoint, from);
        }
        char high = Character.highSurrogate(codePoint);
        char low = Character.lowSurrogate(codePoint);
        for (int index = from; index + 1 < endIndex; index++) {
            if (source[index] == high && source[index + 1] == low) return index;
        }
        return -1;
    }

    private static int naiveLastIndexOfCodePoint(
            char[] source, int codePoint, int fromIndex) {
        if (!Character.isValidCodePoint(codePoint)) return -1;
        if (Character.isBmpCodePoint(codePoint)) {
            return naiveLastIndexOf(source, (char) codePoint, fromIndex);
        }
        char high = Character.highSurrogate(codePoint);
        char low = Character.lowSurrogate(codePoint);
        for (int index = Math.min(fromIndex, source.length - 2); index >= 0; index--) {
            if (source[index] == high && source[index + 1] == low) return index;
        }
        return -1;
    }

    private static String naiveTrim(char[] value) {
        int start = 0;
        int end = value.length;
        while (start < end && value[start] <= 0x20) start++;
        while (start < end && value[end - 1] <= 0x20) end--;
        return new String(copyRange(value, start, end));
    }

    private static String naiveStrip(char[] value) {
        int start = 0;
        while (start < value.length) {
            int codePoint = Character.codePointAt(value, start, value.length);
            if (!Character.isWhitespace(codePoint)) break;
            start += Character.charCount(codePoint);
        }
        int end = value.length;
        while (start < end) {
            int codePoint = Character.codePointBefore(value, end, start);
            if (!Character.isWhitespace(codePoint)) break;
            end -= Character.charCount(codePoint);
        }
        return new String(copyRange(value, start, end));
    }

    private static boolean naiveIsBlank(char[] value) {
        for (int index = 0; index < value.length; ) {
            int codePoint = Character.codePointAt(value, index, value.length);
            if (!Character.isWhitespace(codePoint)) return false;
            index += Character.charCount(codePoint);
        }
        return true;
    }

    private static char[] copyRange(char[] source, int begin, int end) {
        char[] result = new char[end - begin];
        System.arraycopy(source, begin, result, 0, result.length);
        return result;
    }

    private static char[] append(char[] source, char value) {
        char[] result = new char[source.length + 1];
        System.arraycopy(source, 0, result, 0, source.length);
        result[source.length] = value;
        return result;
    }

    private static boolean equalChars(String value, char[] expected) {
        checks++;
        if (value.length() != expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (value.charAt(index) != expected[index]) return false;
        }
        return true;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
