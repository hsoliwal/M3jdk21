// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Bounded UTF-16 literal matching over immutable M3 CharSequence views.
 * No String flattening, decoded byte[] copies, JNI object graphs, or regex rewrites.
 * Caller-owned text is observed synchronously and never retained.
 */
final class NativeUtf16Literal {
    private static final int PAGE_UNITS = 8192;
    private static final boolean NATIVE = loadNative();

    private NativeUtf16Literal() { }

    static boolean nativeAvailable() { return NATIVE; }

    /** Immutable compiled literal; prefix/index lanes are reused for every input line. */
    static Compiled compile(CharSequence pattern) {
        Objects.requireNonNull(pattern, "pattern");
        char[] needle = new char[pattern.length()];
        for (int i = 0; i < needle.length; i++) needle[i] = pattern.charAt(i);
        int[] failure = new int[needle.length];
        for (int i = 1, matched = 0; i < needle.length; i++) {
            while (matched > 0 && needle[i] != needle[matched]) matched = failure[matched - 1];
            if (needle[i] == needle[matched]) matched++;
            failure[i] = matched;
        }
        return new Compiled(needle, failure);
    }

    static boolean contains(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        return compile(pattern).contains(text);
    }

    /** Immutable shared tables; the scan state and bounded page are invocation-local. */
    static final class Compiled {
        private final char[] needle;
        private final int[] failure;

        private Compiled(char[] needle, int[] failure) {
            this.needle = needle;
            this.failure = failure;
        }

        boolean contains(CharSequence text) {
            Objects.requireNonNull(text, "text");
            if (needle.length == 0) return true;
            if (needle.length > text.length()) return false;
            // String is immutable; custom CharSequences retain their existing page-read order.
            if (!NATIVE && text instanceof String string) {
                int state = 0;
                for (int i = 0; i < string.length(); i++) {
                    state = feedUnit(string.charAt(i), needle, failure, state);
                    if (state == -1) return true;
                }
                return false;
            }
            char[] page = new char[Math.min(PAGE_UNITS, text.length())];
            int state = 0;
            for (int offset = 0; offset < text.length();) {
                int count = Math.min(page.length, text.length() - offset);
                for (int i = 0; i < count; i++) page[i] = text.charAt(offset + i);
                state = NATIVE ? feed0(page, count, needle, failure, state)
                        : feedJava(page, count, needle, failure, state);
                if (state == -1) return true;
                if (state < 0 || state >= needle.length)
                    throw new IllegalStateException("invalid native literal state");
                offset += count;
            }
            return false;
        }
    }

    private static int feedJava(char[] page, int count, char[] needle, int[] failure, int state) {
        for (int i = 0; i < count; i++) {
            state = feedUnit(page[i], needle, failure, state);
            if (state == -1) return -1;
        }
        return state;
    }

    private static int feedUnit(char unit, char[] needle, int[] failure, int state) {
        while (state > 0 && unit != needle[state]) state = failure[state - 1];
        return unit == needle[state] && ++state == needle.length ? -1 : state;
    }

    private static boolean loadNative() {
        try {
            if (Boolean.getBoolean("m3.text.native.disable")) return false;
            Path bundled = Path.of(System.getProperty("java.home"), "lib",
                    System.mapLibraryName("m3textio"));
            if (Files.isRegularFile(bundled)) System.load(bundled.toAbsolutePath().toString());
            else System.loadLibrary("m3textio");
            return abiVersion0() == 1;
        } catch (UnsatisfiedLinkError | SecurityException ex) {
            return false; // The pure-Java exact UTF-16 path remains authoritative.
        }
    }

    private static native int abiVersion0();
    private static native int feed0(char[] page, int count, char[] needle, int[] failure, int state);
}
