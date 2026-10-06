/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Differential M3 String search/precompute against independent UTF-16 oracles
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPrecomputeSearchTest
 */

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class M3StringPrecomputeSearchTest {
    private static long checks;

    public static void main(String[] args) throws Exception {
        deterministic();
        randomizedCompositions();
        System.out.println("M3_STRING_PRECOMPUTE_SEARCH_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
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

        String asciiMixed = String.join("", "Al", "PhA", "XYZ");
        check(asciiMixed.equalsIgnoreCase("aLpHaxyz"), "ASCII equalsIgnoreCase");
        check(asciiMixed.regionMatches(true, 0, "xxALPHAXYZyy", 2, asciiMixed.length()),
                "ASCII regionMatches ignoreCase");
        check(!asciiMixed.regionMatches(true, 0, "xxALPHQXYZyy", 2, asciiMixed.length()),
                "ASCII regionMatches ignoreCase negative");
        String unicodeCase = String.join("", "\u03a3", "\u03c3", "\ud83d\ude42");
        check(unicodeCase.regionMatches(true, 0, "\u03c3\u03a3\ud83d\ude42", 0, unicodeCase.length()),
                "Unicode regionMatches ignoreCase");


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

        check(equalChars(joined.replace('a', 'X'), naiveReplace(oracle, 'a', 'X')),
                "replace char canonical");
        check(joined.replace('z', 'X') == joined, "replace char absent identity");
        check(equalChars(
                        joined.replace("\u03b2eta", "Q"),
                        naiveReplace(oracle, "\u03b2eta".toCharArray(), new char[] {'Q'})),
                "replace literal canonical");
        check(equalChars(
                        joined.replace("\ud83d\ude42", ""),
                        naiveReplace(oracle, "\ud83d\ude42".toCharArray(), new char[0])),
                "replace literal deletion");
        String overlap = String.join("", "aa", "aa");
        check(overlap.replace("aa", "b").equals("bb"), "replace non-overlap semantics");
        check(joined.replace("not-present", "x") == joined, "replace literal absent identity");

        Charset[] encodings = {
                StandardCharsets.UTF_8,
                StandardCharsets.US_ASCII,
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_16LE,
                StandardCharsets.UTF_16BE
        };
        for (Charset charset : encodings) {
            check(Arrays.equals(joined.getBytes(charset), encodeOracle(oracle, charset)),
                    "getBytes charset " + charset.name());
            check(Arrays.equals(splitSupplementary.getBytes(charset),
                            encodeOracle("\ud83d\ude42".toCharArray(), charset)),
                    "getBytes split surrogate " + charset.name());
            String unpaired = String.join("", "\ud83d", "x", "\ude42");
            check(Arrays.equals(unpaired.getBytes(charset),
                            encodeOracle("\ud83dx\ude42".toCharArray(), charset)),
                    "getBytes unpaired surrogate " + charset.name());
        }
        check(Arrays.equals(joined.getBytes("UTF-8"), encodeOracle(oracle, StandardCharsets.UTF_8)),
                "named UTF-8 bytes");
        check(Arrays.equals(joined.getBytes(), encodeOracle(oracle, Charset.defaultCharset())),
                "default charset bytes");

        String validAscii = String.join("", "alpha", "XYZ", "123");
        char[] validAsciiOracle = "alphaXYZ123".toCharArray();
        check(Arrays.equals(
                        invokeBytes("getBytesUTF8NoRepl",
                                new Class<?>[] {String.class}, validAscii),
                        encodeOracleNoRepl(validAsciiOracle, StandardCharsets.UTF_8)),
                "internal UTF-8 no-replacement bytes");
        check(Arrays.equals(
                        invokeBytes("getBytesNoRepl",
                                new Class<?>[] {String.class, Charset.class},
                                validAscii, StandardCharsets.US_ASCII),
                        encodeOracleNoRepl(validAsciiOracle, StandardCharsets.US_ASCII)),
                "internal ASCII no-replacement bytes");
        check(Arrays.equals(
                        invokeBytes("getBytesNoRepl",
                                new Class<?>[] {String.class, Charset.class},
                                joined, StandardCharsets.UTF_16LE),
                        encodeOracleNoRepl(oracle, StandardCharsets.UTF_16LE)),
                "internal generic no-replacement bytes");

        String malformedUtf8 = String.join("", "\ud83d", "x");
        try {
            invokeBytes("getBytesUTF8NoRepl", new Class<?>[] {String.class}, malformedUtf8);
            throw new AssertionError("UTF-8 no-replacement accepted unpaired surrogate");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().equals("malformed input off : 0, length : 1"),
                    "UTF-8 no-replacement message");
            check(expected.getCause() instanceof UnmappableCharacterException,
                    "UTF-8 no-replacement cause");
            check(((UnmappableCharacterException) expected.getCause()).getInputLength() == 1,
                    "UTF-8 no-replacement input length");
        }
        try {
            invokeBytes("getBytesNoRepl",
                    new Class<?>[] {String.class, Charset.class},
                    malformedUtf8, StandardCharsets.UTF_8);
            throw new AssertionError("getBytesNoRepl accepted unpaired surrogate");
        } catch (CharacterCodingException expected) {
            check(expected instanceof UnmappableCharacterException,
                    "getBytesNoRepl UTF-8 exception type");
            check(((UnmappableCharacterException) expected).getInputLength() == 1,
                    "getBytesNoRepl UTF-8 input length");
        }
        try {
            invokeBytes("getBytesNoRepl",
                    new Class<?>[] {String.class, Charset.class},
                    joined, StandardCharsets.US_ASCII);
            throw new AssertionError("ASCII no-replacement accepted Unicode input");
        } catch (CharacterCodingException expected) {
            check(true, "ASCII no-replacement rejected Unicode input");
        }

        try {
            invokeBytes("getBytesNoRepl",
                    new Class<?>[] {String.class, Charset.class},
                    splitSupplementary, StandardCharsets.US_ASCII);
            throw new AssertionError("ASCII no-replacement accepted supplementary pair");
        } catch (CharacterCodingException expected) {
            check(expected instanceof UnmappableCharacterException,
                    "ASCII supplementary exception type");
            check(((UnmappableCharacterException) expected).getInputLength() == 2,
                    "ASCII supplementary input length");
        }

        String asciiCase = String.join("", "AbC", "-xYz-123");
        check(asciiCase.toLowerCase(Locale.ROOT).equals("abc-xyz-123"),
                "ROOT ASCII lowercase");
        check(asciiCase.toUpperCase(Locale.ROOT).equals("ABC-XYZ-123"),
                "ROOT ASCII uppercase");
        String alreadyLower = String.join("", "abc", "-123");
        check(alreadyLower.toLowerCase(Locale.ROOT) == alreadyLower,
                "ROOT ASCII lowercase unchanged identity");
        String alreadyUpper = String.join("", "ABC", "-123");
        check(alreadyUpper.toUpperCase(Locale.ROOT) == alreadyUpper,
                "ROOT ASCII uppercase unchanged identity");
        check(String.join("", "I", "X").toLowerCase(Locale.forLanguageTag("tr"))
                        .equals("\u0131x"),
                "Turkish lowercase bypass");
        check(String.join("", "\u03a3", "X").toLowerCase(Locale.ROOT).equals("\u03c3x"),
                "non-ASCII lowercase bypass");

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
            if (isAscii(oracle)) {
                check(equalChars(
                                source.toLowerCase(Locale.ROOT),
                                naiveAsciiCase(oracle, false)),
                        "random ROOT lowercase " + trial);
                check(equalChars(
                                source.toUpperCase(Locale.ROOT),
                                naiveAsciiCase(oracle, true)),
                        "random ROOT uppercase " + trial);
            }
            int regionLength = oracle.length == 0 ? 0 : random.nextInt(oracle.length + 1);
            int leftStart = oracle.length == regionLength
                    ? 0
                    : random.nextInt(oracle.length - regionLength + 1);
            char[] comparison = copyRange(oracle, 0, oracle.length);
            if (comparison.length != 0 && (trial & 1) == 0) {
                int mutate = random.nextInt(comparison.length);
                char unit = comparison[mutate];
                if (unit >= 'a' && unit <= 'z') comparison[mutate] = (char) (unit - 32);
                else if (unit >= 'A' && unit <= 'Z') comparison[mutate] = (char) (unit + 32);
                else if ((trial & 3) == 0) comparison[mutate] ^= 1;
            }
            int rightStart = comparison.length == regionLength
                    ? 0
                    : random.nextInt(comparison.length - regionLength + 1);
            String comparisonString = new String(comparison);
            check(source.regionMatches(leftStart, comparisonString, rightStart, regionLength)
                            == naiveRegionMatches(
                                    oracle, leftStart, comparison, rightStart, regionLength, false),
                    "random exact region " + trial);
            check(source.regionMatches(true, leftStart, comparisonString, rightStart, regionLength)
                            == naiveRegionMatches(
                                    oracle, leftStart, comparison, rightStart, regionLength, true),
                    "random CI region " + trial);
            char oldChar = oracle.length == 0
                    ? 'x'
                    : oracle[random.nextInt(oracle.length)];
            char newChar = (char) random.nextInt(Character.MAX_VALUE + 1);
            check(equalChars(source.replace(oldChar, newChar), naiveReplace(oracle, oldChar, newChar)),
                    "random char replace " + trial);

            char[] replaceTarget;
            if (oracle.length == 0) {
                replaceTarget = new char[] {'x'};
            } else {
                int replaceLength = 1 + random.nextInt(Math.min(4, oracle.length));
                int replaceStart = random.nextInt(oracle.length - replaceLength + 1);
                replaceTarget = copyRange(oracle, replaceStart, replaceStart + replaceLength);
                if ((trial & 7) == 0) {
                    replaceTarget = append(replaceTarget, '#');
                }
            }
            String replacement = atoms[random.nextInt(atoms.length)];
            char[] replacementChars = replacement.toCharArray();
            check(equalChars(
                            source.replace(new String(replaceTarget), replacement),
                            naiveReplace(oracle, replaceTarget, replacementChars)),
                    "random literal replace " + trial);
            if ((trial & 15) == 0) {
                Charset charset = switch ((trial >>> 4) % 5) {
                    case 0 -> StandardCharsets.UTF_8;
                    case 1 -> StandardCharsets.US_ASCII;
                    case 2 -> StandardCharsets.ISO_8859_1;
                    case 3 -> StandardCharsets.UTF_16LE;
                    default -> StandardCharsets.UTF_16BE;
                };
                check(Arrays.equals(source.getBytes(charset), encodeOracle(oracle, charset)),
                        "random getBytes " + charset.name() + " trial " + trial);
            }
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

    private static byte[] invokeBytes(
            String methodName, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Method method = String.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            return (byte[]) method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof CharacterCodingException coding) throw coding;
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private static byte[] encodeOracleNoRepl(char[] value, Charset charset)
            throws CharacterCodingException {
        CharsetEncoder encoder = charset.newEncoder();
        int capacity = (int) (value.length * (double) encoder.maxBytesPerChar());
        ByteBuffer bytes = ByteBuffer.allocate(capacity);
        CoderResult result = encoder.encode(CharBuffer.wrap(value), bytes, true);
        if (!result.isUnderflow()) result.throwException();
        result = encoder.flush(bytes);
        if (!result.isUnderflow()) result.throwException();
        return Arrays.copyOf(bytes.array(), bytes.position());
    }

    private static byte[] encodeOracle(char[] value, Charset charset) {
        CharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        int capacity = (int) (value.length * (double) encoder.maxBytesPerChar());
        ByteBuffer bytes = ByteBuffer.allocate(capacity);
        try {
            CoderResult result = encoder.encode(CharBuffer.wrap(value), bytes, true);
            if (!result.isUnderflow()) result.throwException();
            result = encoder.flush(bytes);
            if (!result.isUnderflow()) result.throwException();
        } catch (CharacterCodingException impossibleWithReplacement) {
            throw new AssertionError(impossibleWithReplacement);
        }
        return Arrays.copyOf(bytes.array(), bytes.position());
    }

    private static boolean isAscii(char[] value) {
        for (char unit : value) {
            if (unit > 0x7f) return false;
        }
        return true;
    }

    private static char[] naiveAsciiCase(char[] value, boolean upper) {
        char[] result = copyRange(value, 0, value.length);
        for (int index = 0; index < result.length; index++) {
            char unit = result[index];
            result[index] = upper
                    ? (unit >= 'a' && unit <= 'z' ? (char) (unit - ('a' - 'A')) : unit)
                    : (unit >= 'A' && unit <= 'Z' ? (char) (unit + ('a' - 'A')) : unit);
        }
        return result;
    }

    private static boolean naiveRegionMatches(
            char[] left,
            int leftOffset,
            char[] right,
            int rightOffset,
            int length,
            boolean ignoreCase) {
        if (leftOffset < 0 || rightOffset < 0 || length < 0
                || leftOffset > left.length - length
                || rightOffset > right.length - length) {
            return false;
        }
        if (!ignoreCase) {
            for (int index = 0; index < length; index++) {
                if (left[leftOffset + index] != right[rightOffset + index]) return false;
            }
            return true;
        }

        int l = leftOffset;
        int r = rightOffset;
        int remaining = length;
        while (remaining > 0) {
            int lc = left[l];
            int rc = right[r];
            int lcp = lc;
            int rcp = rc;
            int lw = 1;
            int rw = 1;
            if (Character.isHighSurrogate((char) lc) && remaining > 1
                    && Character.isLowSurrogate(left[l + 1])) {
                lcp = Character.toCodePoint((char) lc, left[l + 1]);
                lw = 2;
            }
            if (Character.isHighSurrogate((char) rc) && remaining > 1
                    && Character.isLowSurrogate(right[r + 1])) {
                rcp = Character.toCodePoint((char) rc, right[r + 1]);
                rw = 2;
            }
            if (lcp != rcp) {
                int lu = Character.toUpperCase(lcp);
                int ru = Character.toUpperCase(rcp);
                if (lu != ru && Character.toLowerCase(lu) != Character.toLowerCase(ru)) {
                    return false;
                }
            }
            if (lw != rw) return false;
            l += lw;
            r += rw;
            remaining -= lw;
        }
        return true;
    }

    private static char[] naiveReplace(char[] source, char oldChar, char newChar) {
        char[] result = copyRange(source, 0, source.length);
        for (int index = 0; index < result.length; index++) {
            if (result[index] == oldChar) result[index] = newChar;
        }
        return result;
    }

    private static char[] naiveReplace(char[] source, char[] target, char[] replacement) {
        if (target.length == 0) {
            throw new IllegalArgumentException("test oracle requires non-empty literal target");
        }
        int matches = 0;
        for (int index = 0; index <= source.length - target.length; ) {
            if (matchesAt(source, target, index)) {
                matches++;
                index += target.length;
            } else {
                index++;
            }
        }
        long length = (long) source.length
                + (long) matches * (replacement.length - target.length);
        if (length > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("test oracle result too large");
        }
        char[] result = new char[(int) length];
        int sourceIndex = 0;
        int output = 0;
        while (sourceIndex < source.length) {
            if (sourceIndex <= source.length - target.length
                    && matchesAt(source, target, sourceIndex)) {
                System.arraycopy(replacement, 0, result, output, replacement.length);
                output += replacement.length;
                sourceIndex += target.length;
            } else {
                result[output++] = source[sourceIndex++];
            }
        }
        return result;
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
