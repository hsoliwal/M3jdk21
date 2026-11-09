// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.Random;

/** Dependency-free JDK String.indexOf oracle; also proves no flattening of M3 views. */
public final class NativeUtf16LiteralSelfTest {
    private static int checks;

    private NativeUtf16LiteralSelfTest() { }

    private static CharSequence view(String value) {
        return new CharSequence() {
            @Override public int length() { return value.length(); }
            @Override public char charAt(int index) { return value.charAt(index); }
            @Override public CharSequence subSequence(int start, int end) {
                return view(value.substring(start, end));
            }
            @Override public String toString() {
                throw new AssertionError("M3 view must not flatten");
            }
        };
    }

    private static void check(String text, String pattern) {
        boolean expected = text.contains(pattern);
        boolean actual = NativeUtf16Literal.contains(view(text), view(pattern));
        if (expected != actual) throw new AssertionError("literal mismatch " + checks);
        var compiled = NativeUtf16Literal.compile(view(pattern));
        if (compiled.contains(view(text)) != expected || compiled.contains(view(text)) != expected)
            throw new AssertionError("compiled plan mismatch " + checks);
        checks++;
    }

    public static void main(String[] args) {
        boolean required = args.length > 0 && args[0].equals("native");
        if (required && !NativeUtf16Literal.nativeAvailable())
            throw new AssertionError("JNI library or expected symbol not loaded");
        if (!required && NativeUtf16Literal.nativeAvailable())
            throw new AssertionError("expected Java fallback without the JNI library");
        for (String text : new String[] {"", "\0", "abcabc", "\ud83d\ude00!", "a\r\nb", "a\ud800b", "a\udc00b"}) {
            for (String pattern : new String[] {"", "\0", "abc", "\ud83d", "\ude00", "\ud800", "\udc00", "a", "\r\n", "!"})
                check(text, pattern);
        }
        String crossing = "a".repeat(8191) + "aabaa" + "b".repeat(8193);
        for (String needle : new String[] {"aabaa", "abaa", "aaab", "a".repeat(8200), "b".repeat(8193), "ba"})
            check(crossing, needle);
        Random random = new Random(0x4D334A4E49L);
        char[] alphabet = {0, 1, 127, 255, '\ud800', '\udc00', '\ud83d', '\ude00', 'a', 'b', 'c', '\n', '\r'};
        for (int r = 0; r < 12_000; r++) {
            int size = random.nextInt(64);
            StringBuilder text = new StringBuilder(size);
            for (int i = 0; i < size; i++) text.append(alphabet[random.nextInt(alphabet.length)]);
            String value = text.toString();
            String needle;
            if (random.nextBoolean() && value.length() > 0) {
                int start = random.nextInt(value.length());
                needle = value.substring(start, start + random.nextInt(value.length() - start + 1));
            } else {
                StringBuilder candidate = new StringBuilder();
                int width = random.nextInt(20);
                for (int i = 0; i < width; i++) candidate.append(alphabet[random.nextInt(alphabet.length)]);
                needle = candidate.toString();
            }
            check(value, needle);
        }
        System.out.println("M3_NATIVE_UTF16_LITERAL_PASS checks=" + checks
                + " backend=" + (NativeUtf16Literal.nativeAvailable() ? "jni" : "java"));
    }
}
