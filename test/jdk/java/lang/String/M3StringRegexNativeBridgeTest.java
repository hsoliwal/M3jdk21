/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Verify JNI UTF-16 String shadows and regex parity over joined and sliced M3-backed text
 * @run main/native/othervm -Xcheck:jni M3StringRegexNativeBridgeTest
 * @run main/native/othervm -Xint -Xcheck:jni M3StringRegexNativeBridgeTest
 * @run main/native/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3StringRegexNativeBridgeTest
 */
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3StringRegexNativeBridgeTest {
    static { System.loadLibrary("M3StringRegexNativeBridgeTest"); }
    private static native long[] digests(String input);
    private static native String roundTrip(String input);
    private static long checks;

    public static void main(String[] args) {
        if (args.length != 0) throw new AssertionError("unexpected args");
        String[] pieces = {"", "a", "b", "abc", "\u0000", "\r\n", "\u0085", "\u2028",
                "\u2029", "\u00e9", "\u4e00", "\ud83d", "\ude42", "\ud83d\ude42"};
        for (String first : pieces) {
            for (String second : pieces) {
                String text = first.concat(second);
                check(text);
                check("!".concat(text).concat("?").substring(1, text.length() + 1));
            }
        }
        Random random = new Random(0x4d33524e49554616L);
        for (int trial = 0; trial < 600; trial++) {
            StringBuilder builder = new StringBuilder();
            int n = random.nextInt(270);
            for (int i = 0; i < n; i++) {
                builder.append(pieces[random.nextInt(pieces.length)]);
            }
            String text = builder.toString();
            check(text);
            check("<".concat(text).concat(">").substring(1, text.length() + 1));
        }
        check("a".repeat(8192));
        check("x".concat("\ud83d").concat("\ude42").repeat(150));
        System.out.println("M3_STRING_REGEX_NATIVE_BRIDGE_PASS checks=" + checks);
    }

    private static void check(String text) {
        long expected = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) {
            expected = (expected ^ text.charAt(i)) * 0x100000001b3L;
        }
        long[] actual = digests(text);
        equal(actual != null && actual.length == 3, "JNI digest shape");
        for (long hash : actual) {
            equal(hash == expected, "JNI UTF16 code-unit digest");
        }
        String copied = roundTrip(text);
        equal(copied != null && copied.length() == text.length() && text.equals(copied),
                "native NewString roundtrip");
        String[] expressions = text.length() > 512
                ? new String[] {"a*b*", "(?s).*", "[\\s\\S]*"}
                : new String[] {"a*b*", "(?s).*", "(a|b)*", "[\\s\\S]*"};
        for (String regex : expressions) {
            Pattern pattern = Pattern.compile(regex);
            boolean oracle = pattern.matcher(new StringBuilder(text)).matches();
            equal(text.matches(regex) == oracle, "original regex parity");
            equal(copied.matches(regex) == oracle, "JNI roundtrip regex parity");
            Matcher fromString = pattern.matcher(copied);
            Matcher fromBuilder = pattern.matcher(new StringBuilder(copied));
            equal(fromString.find() == fromBuilder.find(), "find state");
            equal(fromString.hitEnd() == fromBuilder.hitEnd(), "hitEnd state");
            equal(fromString.requireEnd() == fromBuilder.requireEnd(), "requireEnd state");
        }
    }
    private static void equal(boolean truth, String label) {
        checks++;
        if (!truth) throw new AssertionError(label + " at check " + checks);
    }
}
