/*
 * @test
 * @summary M3 String searches use canonical storage and preserve String search semantics.
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPrecomputeSearchTest
 */

import java.util.Random;

public class M3StringPrecomputeSearchTest {
    public static void main(String[] args) {
        String left = "alpha";
        String middle = "\u03b2eta";
        String emoji = "\ud83d\ude42";
        String right = "omega";

        String joined = String.join("|", left, middle, emoji, right);
        String oracle = "alpha|\u03b2eta|\ud83d\ude42|omega";

        check(joined.equals(oracle), "joined content");
        check(joined.startsWith("alpha"), "startsWith");
        check(joined.startsWith("\u03b2eta", 6), "startsWith offset");
        check(joined.endsWith("omega"), "endsWith");

        check(joined.indexOf('a') == oracle.indexOf('a'), "indexOf BMP");
        check(joined.indexOf('\u03b2') == oracle.indexOf('\u03b2'), "indexOf UTF16 BMP");
        check(joined.indexOf(0x1f642) == oracle.indexOf(0x1f642), "indexOf supplementary");
        check(joined.indexOf('z') == -1, "indexOf absent precompute rejection");

        check(joined.lastIndexOf('a') == oracle.lastIndexOf('a'), "lastIndexOf BMP");
        check(joined.lastIndexOf(0x1f642) == oracle.lastIndexOf(0x1f642),
                "lastIndexOf supplementary");

        check(joined.indexOf("\u03b2eta") == oracle.indexOf("\u03b2eta"), "indexOf String");
        check(joined.indexOf("eta", 4) == oracle.indexOf("eta", 4), "indexOf String from");
        check(joined.indexOf("eta", 0, joined.length()) == oracle.indexOf("eta", 0, oracle.length()),
                "indexOf String bounded");
        check(joined.lastIndexOf("omega") == oracle.lastIndexOf("omega"), "lastIndexOf String");
        check(joined.indexOf("not-present") == -1, "absent String");

        String splitSupplementary = String.join("", "\ud83d", "\ude42");
        check(splitSupplementary.equals("\ud83d\ude42"), "split surrogate content");
        check(splitSupplementary.indexOf(0x1f642) == 0,
                "supplementary code point across canonical atom seam");
        check(splitSupplementary.lastIndexOf(0x1f642) == 0,
                "reverse supplementary code point across canonical atom seam");

        check(joined.regionMatches(6, "\u03b2eta", 0, 4), "regionMatches canonical");
        check(!joined.regionMatches(6, "\u03b2eto", 0, 4), "regionMatches mismatch");

        String trimJoined = String.join("", " \t", "alpha", " \n");
        check(trimJoined.trim().equals("alpha"), "trim canonical boundaries");

        String stripJoined = String.join("", "\u2003", "alpha", "\u2002");
        check(stripJoined.strip().equals("alpha"), "strip Unicode whitespace");
        check(stripJoined.stripLeading().equals("alpha\u2002"), "stripLeading Unicode");
        check(stripJoined.stripTrailing().equals("\u2003alpha"), "stripTrailing Unicode");

        String blankJoined = String.join("", " \t", "\u2003", "\n");
        check(blankJoined.isBlank(), "isBlank canonical facts");
        check(blankJoined.strip().isEmpty(), "strip all whitespace");

        String repeated = joined.repeat(3);
        String repeatedOracle = oracle.repeat(3);
        check(repeated.indexOf("\ud83d\ude42|omegaalpha") ==
                repeatedOracle.indexOf("\ud83d\ude42|omegaalpha"), "cross-segment search");
        check(repeated.lastIndexOf("\u03b2eta") == repeatedOracle.lastIndexOf("\u03b2eta"),
                "reverse repeated search");

        String slice = repeated.substring(3, repeated.length() - 4);
        String sliceOracle = repeatedOracle.substring(3, repeatedOracle.length() - 4);
        check(slice.indexOf("\ud83d\ude42") == sliceOracle.indexOf("\ud83d\ude42"),
                "slice supplementary search");
        check(slice.lastIndexOf("alpha") == sliceOracle.lastIndexOf("alpha"),
                "slice reverse search");

        for (int from = -2; from <= joined.length() + 2; from++) {
            check(joined.indexOf('a', from) == oracle.indexOf('a', from), "char from " + from);
            check(joined.lastIndexOf('a', from) == oracle.lastIndexOf('a', from),
                    "char reverse " + from);
            check(joined.indexOf("a", from) == oracle.indexOf("a", from),
                    "String from " + from);
            check(joined.lastIndexOf("a", from) == oracle.lastIndexOf("a", from),
                    "String reverse " + from);
        }

        randomizedCompositions();
    }

    private static void randomizedCompositions() {
        Random random = new Random(0x4d33535452494e47L);
        String[] atoms = {
                "", "a", "bc", " ", "\t", "\n", "\u2003", "\u00e9",
                "\ud83d", "\ude42", "\ud83d\ude42", "XYZ", "aba", "\0"
        };
        for (int trial = 0; trial < 2_000; trial++) {
            int pieces = 1 + random.nextInt(8);
            String[] selected = new String[pieces];
            StringBuilder oracleBuilder = new StringBuilder();
            for (int index = 0; index < pieces; index++) {
                selected[index] = atoms[random.nextInt(atoms.length)];
                oracleBuilder.append(selected[index]);
            }
            String source = String.join("", selected);
            char[] oracle = oracleBuilder.toString().toCharArray();

            int targetLength = oracle.length == 0 ? 0 : random.nextInt(Math.min(6, oracle.length) + 1);
            int targetStart = targetLength == 0 ? 0 : random.nextInt(oracle.length - targetLength + 1);
            String target = new String(oracle, targetStart, targetLength);
            if ((trial & 3) == 0) {
                target = target + "#";
            }

            int from = random.nextInt(oracle.length + 7) - 3;
            check(source.indexOf(target, from) == naiveIndexOf(oracle, target.toCharArray(), from),
                    "random indexOf trial " + trial);
            check(source.lastIndexOf(target, from)
                            == naiveLastIndexOf(oracle, target.toCharArray(), from),
                    "random lastIndexOf trial " + trial);

            if (oracle.length != 0) {
                char wanted = oracle[random.nextInt(oracle.length)];
                check(source.indexOf(wanted, from) == naiveIndexOf(oracle, wanted, from),
                        "random char indexOf trial " + trial);
                check(source.lastIndexOf(wanted, from) == naiveLastIndexOf(oracle, wanted, from),
                        "random char lastIndexOf trial " + trial);
            }

            check(source.trim().equals(naiveTrim(oracle)), "random trim trial " + trial);
            check(source.strip().equals(naiveStrip(oracle)), "random strip trial " + trial);
            check(source.isBlank() == naiveIsBlank(oracle), "random blank trial " + trial);
        }
    }

    private static int naiveIndexOf(char[] source, char[] target, int fromIndex) {
        int from = Math.max(0, fromIndex);
        if (target.length == 0) {
            return Math.min(from, source.length);
        }
        for (int at = from; at <= source.length - target.length; at++) {
            int index = 0;
            while (index < target.length && source[at + index] == target[index]) {
                index++;
            }
            if (index == target.length) {
                return at;
            }
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char[] target, int fromIndex) {
        int at = Math.min(fromIndex, source.length - target.length);
        if (target.length == 0) {
            return Math.max(-1, Math.min(fromIndex, source.length));
        }
        for (; at >= 0; at--) {
            int index = 0;
            while (index < target.length && source[at + index] == target[index]) {
                index++;
            }
            if (index == target.length) {
                return at;
            }
        }
        return -1;
    }

    private static int naiveIndexOf(char[] source, char target, int fromIndex) {
        for (int index = Math.max(0, fromIndex); index < source.length; index++) {
            if (source[index] == target) {
                return index;
            }
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char target, int fromIndex) {
        for (int index = Math.min(fromIndex, source.length - 1); index >= 0; index--) {
            if (source[index] == target) {
                return index;
            }
        }
        return -1;
    }

    private static String naiveTrim(char[] value) {
        int start = 0;
        int end = value.length;
        while (start < end && value[start] <= 0x20) {
            start++;
        }
        while (start < end && value[end - 1] <= 0x20) {
            end--;
        }
        return new String(value, start, end - start);
    }

    private static String naiveStrip(char[] value) {
        int start = 0;
        while (start < value.length) {
            int codePoint = Character.codePointAt(value, start, value.length);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        int end = value.length;
        while (start < end) {
            int codePoint = Character.codePointBefore(value, end, start);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return new String(value, start, end - start);
    }

    private static boolean naiveIsBlank(char[] value) {
        for (int index = 0; index < value.length; ) {
            int codePoint = Character.codePointAt(value, index, value.length);
            if (!Character.isWhitespace(codePoint)) {
                return false;
            }
            index += Character.charCount(codePoint);
        }
        return true;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
