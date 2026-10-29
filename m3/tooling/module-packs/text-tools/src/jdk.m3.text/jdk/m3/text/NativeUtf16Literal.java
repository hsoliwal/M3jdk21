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

    static boolean contains(CharSequence text, CharSequence pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length() == 0) return true;
        if (pattern.length() > text.length()) return false;
        char[] needle = new char[pattern.length()];
        for (int i = 0; i < needle.length; i++) needle[i] = pattern.charAt(i);
        int[] failure = new int[needle.length];
        for (int i = 1, matched = 0; i < needle.length; i++) {
            while (matched > 0 && needle[i] != needle[matched]) matched = failure[matched - 1];
            if (needle[i] == needle[matched]) matched++;
            failure[i] = matched;
        }
        char[] page = new char[Math.min(PAGE_UNITS, text.length())];
        int state = 0;
        for (int offset = 0; offset < text.length();) {
            int count = Math.min(page.length, text.length() - offset);
            for (int i = 0; i < count; i++) page[i] = text.charAt(offset + i);
            // A negative result is the single documented match sentinel.
            state = NATIVE ? feed0(page, count, needle, failure, state)
                    : feedJava(page, count, needle, failure, state);
            if (state == -1) return true;
            if (state < 0 || state >= needle.length)
                throw new IllegalStateException("invalid native literal state");
            offset += count;
        }
        return false;
    }

    private static int feedJava(char[] page, int count, char[] needle, int[] failure, int state) {
        for (int i = 0; i < count; i++) {
            while (state > 0 && page[i] != needle[state]) state = failure[state - 1];
            if (page[i] == needle[state] && ++state == needle.length) return -1;
        }
        return state;
    }

    private static boolean loadNative() {
        if (Boolean.getBoolean("m3.text.native.disable")) return false;
        try {
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
