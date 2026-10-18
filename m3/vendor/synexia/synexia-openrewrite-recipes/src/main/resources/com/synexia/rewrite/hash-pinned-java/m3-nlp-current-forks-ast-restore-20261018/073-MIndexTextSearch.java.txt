// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Precompiled literal search over indexed/composite text and UTF-8.
 *
 * <p>KMP failure tables are frozen once. Repeated contains/indexOf operations are therefore O(n)
 * in the haystack instead of restarting a literal comparison at every candidate position. When
 * the input is {@link MIndexText}, segments are traversed directly and the composite is never
 * materialized.</p>
 */
public final class MIndexTextSearch {
    private final MIndexString needle;
    private final char[] utf16;
    private final int[] utf16Failure;
    private final byte[] utf8;
    private final int[] utf8Failure;

    private MIndexTextSearch(MIndexString needle) {
        this.needle = needle;
        this.utf16 = needle.toString().toCharArray();
        this.utf16Failure = failure(utf16);
        this.utf8 = needle.getBytes(StandardCharsets.UTF_8);
        this.utf8Failure = failure(utf8);
    }

    public static MIndexTextSearch compile(CharSequence needle) {
        return new MIndexTextSearch(
                MIndexString.from(Objects.requireNonNull(needle, "needle")));
    }

    public MIndexString needle() {
        return needle;
    }

    public boolean matches(CharSequence input) {
        Objects.requireNonNull(input, "input");
        if (input instanceof MIndexString indexed) return indexed.equals(needle);
        return input.length() == utf16.length && startsWith(input);
    }

    public boolean startsWith(CharSequence input) {
        Objects.requireNonNull(input, "input");
        if (utf16.length > input.length()) return false;
        if (input instanceof MIndexText text) {
            int wanted = 0;
            for (int segment = 0; segment < text.segmentCount() && wanted < utf16.length; segment++) {
                MIndexString value = text.segment(segment);
                for (int index = 0; index < value.length() && wanted < utf16.length; index++) {
                    if (value.charAt(index) != utf16[wanted++]) return false;
                }
            }
            return wanted == utf16.length;
        }
        for (int index = 0; index < utf16.length; index++) {
            if (input.charAt(index) != utf16[index]) return false;
        }
        return true;
    }

    public boolean endsWith(CharSequence input) {
        Objects.requireNonNull(input, "input");
        if (utf16.length > input.length()) return false;
        if (input instanceof MIndexText text) {
            int wanted = utf16.length - 1;
            for (int segment = text.segmentCount() - 1; segment >= 0 && wanted >= 0; segment--) {
                MIndexString value = text.segment(segment);
                for (int index = value.length() - 1; index >= 0 && wanted >= 0; index--) {
                    if (value.charAt(index) != utf16[wanted--]) return false;
                }
            }
            return wanted < 0;
        }
        int start = input.length() - utf16.length;
        for (int index = 0; index < utf16.length; index++) {
            if (input.charAt(start + index) != utf16[index]) return false;
        }
        return true;
    }

    public boolean contains(CharSequence input) {
        return indexOf(input) >= 0;
    }

    public int indexOf(CharSequence input) {
        Objects.requireNonNull(input, "input");
        if (utf16.length == 0) return 0;
        if (utf16.length > input.length()) return -1;
        if (input instanceof MIndexText text) return indexOf(text);
        int matched = 0;
        for (int index = 0; index < input.length(); index++) {
            matched = advance(matched, input.charAt(index));
            if (matched == utf16.length) return index - utf16.length + 1;
        }
        return -1;
    }

    public boolean matchesUtf8(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (length != utf8.length) return false;
        for (int index = 0; index < utf8.length; index++) {
            if (source[offset + index] != utf8[index]) return false;
        }
        return true;
    }

    public boolean startsWithUtf8(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (utf8.length > length) return false;
        for (int index = 0; index < utf8.length; index++) {
            if (source[offset + index] != utf8[index]) return false;
        }
        return true;
    }

    public boolean endsWithUtf8(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (utf8.length > length) return false;
        int start = offset + length - utf8.length;
        for (int index = 0; index < utf8.length; index++) {
            if (source[start + index] != utf8[index]) return false;
        }
        return true;
    }

    public boolean containsUtf8(byte[] source, int offset, int length) {
        return indexOfUtf8(source, offset, length) >= 0;
    }

    public int indexOfUtf8(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (utf8.length == 0) return 0;
        if (utf8.length > length) return -1;
        int matched = 0;
        int end = offset + length;
        for (int index = offset; index < end; index++) {
            while (matched > 0 && source[index] != utf8[matched]) {
                matched = utf8Failure[matched - 1];
            }
            if (source[index] == utf8[matched]) matched++;
            if (matched == utf8.length) return index - offset - utf8.length + 1;
        }
        return -1;
    }

    private int indexOf(MIndexText input) {
        int matched = 0;
        int global = 0;
        for (int segment = 0; segment < input.segmentCount(); segment++) {
            MIndexString value = input.segment(segment);
            for (int index = 0; index < value.length(); index++, global++) {
                matched = advance(matched, value.charAt(index));
                if (matched == utf16.length) return global - utf16.length + 1;
            }
        }
        return -1;
    }

    private int advance(int matched, char value) {
        while (matched > 0 && value != utf16[matched]) {
            matched = utf16Failure[matched - 1];
        }
        if (value == utf16[matched]) matched++;
        return matched;
    }

    private static int[] failure(char[] pattern) {
        int[] failure = new int[pattern.length];
        int matched = 0;
        for (int index = 1; index < pattern.length; index++) {
            while (matched > 0 && pattern[index] != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (pattern[index] == pattern[matched]) matched++;
            failure[index] = matched;
        }
        return failure;
    }

    private static int[] failure(byte[] pattern) {
        int[] failure = new int[pattern.length];
        int matched = 0;
        for (int index = 1; index < pattern.length; index++) {
            while (matched > 0 && pattern[index] != pattern[matched]) {
                matched = failure[matched - 1];
            }
            if (pattern[index] == pattern[matched]) matched++;
            failure[index] = matched;
        }
        return failure;
    }
}
