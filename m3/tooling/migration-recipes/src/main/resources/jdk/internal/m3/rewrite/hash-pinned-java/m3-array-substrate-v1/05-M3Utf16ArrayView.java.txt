// SPDX-License-Identifier: Apache-2.0
package com.m3.arrays;

import java.nio.CharBuffer;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Immutable logical UTF-16 array view. Coordinates are Java UTF-16 code units and may cross
 * physical segment seams.
 */
public interface M3Utf16ArrayView extends CharSequence {
    @Override
    int length();

    @Override
    char charAt(int index);

    @Override
    M3Utf16ArrayView subSequence(int start, int end);

    int segmentCount();

    M3Utf16Facts facts();

    CharBuffer[] asReadOnlyCharBuffers();

    default int javaHashCode() {
        return facts().javaHashCode();
    }

    default int codePointCount() {
        return facts().codePointCount();
    }

    default int codePointAt(int index) {
        Objects.checkIndex(index, length());
        return Character.codePointAt(this, index);
    }

    default int codePointBefore(int index) {
        if (index <= 0 || index > length()) {
            throw new IndexOutOfBoundsException("UTF-16 boundary: " + index);
        }
        return Character.codePointBefore(this, index);
    }

    default int offsetByCodePoints(int index, int codePointOffset) {
        return Character.offsetByCodePoints(this, index, codePointOffset);
    }

    default IntStream chars() {
        return IntStream.range(0, length()).map(this::charAt);
    }

    default IntStream codePoints() {
        IntStream.Builder result = IntStream.builder();
        for (int index = 0; index < length();) {
            char first = charAt(index++);
            if (Character.isHighSurrogate(first) && index < length()) {
                char second = charAt(index);
                if (Character.isLowSurrogate(second)) {
                    result.add(Character.toCodePoint(first, second));
                    index++;
                    continue;
                }
            }
            result.add(first);
        }
        return result.build();
    }

    default boolean contentEquals(M3Utf16ArrayView other) {
        M3Utf16ArrayView checked = Objects.requireNonNull(other, "other");
        if (length() != checked.length() || javaHashCode() != checked.javaHashCode()) return false;
        for (int index = 0; index < length(); index++) {
            if (charAt(index) != checked.charAt(index)) return false;
        }
        return true;
    }

    default int compareTo(M3Utf16ArrayView other) {
        M3Utf16ArrayView checked = Objects.requireNonNull(other, "other");
        int common = Math.min(length(), checked.length());
        for (int index = 0; index < common; index++) {
            int difference = charAt(index) - checked.charAt(index);
            if (difference != 0) return difference;
        }
        return length() - checked.length();
    }

    default int indexOf(CharSequence needle, int fromIndex) {
        CharSequence checked = Objects.requireNonNull(needle, "needle");
        int start = Math.max(0, fromIndex);
        if (checked.length() == 0) return Math.min(start, length());
        int limit = length() - checked.length();
        for (int at = start; at <= limit; at++) {
            int index = 0;
            while (index < checked.length() && charAt(at + index) == checked.charAt(index)) index++;
            if (index == checked.length()) return at;
        }
        return -1;
    }

    default int lastIndexOf(CharSequence needle, int fromIndex) {
        CharSequence checked = Objects.requireNonNull(needle, "needle");
        if (checked.length() == 0) return fromIndex < 0 ? -1 : Math.min(fromIndex, length());
        int start = Math.min(fromIndex, length() - checked.length());
        for (int at = start; at >= 0; at--) {
            int index = 0;
            while (index < checked.length() && charAt(at + index) == checked.charAt(index)) index++;
            if (index == checked.length()) return at;
        }
        return -1;
    }

    default void copyTo(int sourceStart, char[] target, int targetStart, int count) {
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(sourceStart, count, length());
        Objects.checkFromIndexSize(targetStart, count, target.length);
        int remaining = count;
        int skip = sourceStart;
        int destination = targetStart;
        for (CharBuffer original : asReadOnlyCharBuffers()) {
            CharBuffer buffer = Objects.requireNonNull(original, "buffer").asReadOnlyBuffer();
            int available = buffer.remaining();
            if (skip >= available) {
                skip -= available;
                continue;
            }
            int take = Math.min(remaining, available - skip);
            buffer.get(buffer.position() + skip, target, destination, take);
            destination += take;
            remaining -= take;
            skip = 0;
            if (remaining == 0) return;
        }
        if (remaining != 0) throw new IllegalStateException("UTF-16 segments do not cover declared length");
    }

    default char[] copy() {
        char[] result = new char[length()];
        copyTo(0, result, 0, result.length);
        return result;
    }
}
