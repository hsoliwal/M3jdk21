/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

package java.lang;

import jdk.internal.util.M3StringFacts;

/**
 * Segment-native exact UTF-16 search kernels for ordinary M3JDK Strings.
 *
 * <p>Presence signals are rejection filters only. Every reported result is
 * verified by exact UTF-16 matching. Short needles use Shift-And; long needles
 * use a KMP prefix table. The source String is never flattened.</p>
 */
final class M3StringSearch {
    private static final int MAX_BIT_PATTERN = Long.SIZE;

    private M3StringSearch() {}

    static boolean regionEquals(
            String left, int leftOffset, String right, int rightOffset, int length) {
        for (int index = 0; index < length; index++) {
            if (left.charAt(leftOffset + index) != right.charAt(rightOffset + index)) {
                return false;
            }
        }
        return true;
    }

    static int indexOf(String text, int ch, int fromIndex, int endIndex) {
        int from = Math.max(0, fromIndex);
        int end = Math.min(endIndex, text.length());
        if (from >= end || ch < 0 || ch > Character.MAX_CODE_POINT) {
            return -1;
        }
        if (ch <= Character.MAX_VALUE) {
            char target = (char) ch;
            for (int index = from; index < end; index++) {
                if (text.charAt(index) == target) return index;
            }
            return -1;
        }
        char high = Character.highSurrogate(ch);
        char low = Character.lowSurrogate(ch);
        for (int index = from; index + 1 < end; index++) {
            if (text.charAt(index) == high && text.charAt(index + 1) == low) {
                return index;
            }
        }
        return -1;
    }

    static int lastIndexOf(String text, int ch, int fromIndex) {
        if (ch < 0 || ch > Character.MAX_CODE_POINT || fromIndex < 0) {
            return -1;
        }
        int from = Math.min(fromIndex, text.length() - 1);
        if (ch <= Character.MAX_VALUE) {
            char target = (char) ch;
            for (int index = from; index >= 0; index--) {
                if (text.charAt(index) == target) return index;
            }
            return -1;
        }
        char high = Character.highSurrogate(ch);
        char low = Character.lowSurrogate(ch);
        for (int index = Math.min(from, text.length() - 2); index >= 0; index--) {
            if (text.charAt(index) == high && text.charAt(index + 1) == low) {
                return index;
            }
        }
        return -1;
    }

    static int indexOf(String text, String pattern, int fromIndex, int endIndex) {
        int from = Math.clamp(fromIndex, 0, endIndex);
        int width = pattern.length();
        if (width == 0) return from;
        if (width > endIndex - from) return -1;
        if (!M3StringFacts.mayContain(text.m3BitSignal64(), pattern.m3BitSignal64())) {
            return -1;
        }
        return width <= MAX_BIT_PATTERN
                ? bitParallelIndexOf(text, pattern, from, endIndex)
                : kmpIndexOf(text, pattern, from, endIndex);
    }

    static int lastIndexOf(String text, String pattern, int fromIndex) {
        int width = pattern.length();
        int rightIndex = text.length() - width;
        int maxStart = Math.min(fromIndex, rightIndex);
        if (maxStart < 0) return -1;
        if (width == 0) return maxStart;
        if (!M3StringFacts.mayContain(text.m3BitSignal64(), pattern.m3BitSignal64())) {
            return -1;
        }
        return width <= MAX_BIT_PATTERN
                ? bitParallelLastIndexOf(text, pattern, maxStart)
                : kmpLastIndexOf(text, pattern, maxStart);
    }

    private static int bitParallelIndexOf(
            String text, String pattern, int from, int endExclusive) {
        PatternMasks masks = new PatternMasks(pattern);
        long state = 0L;
        long matchBit = 1L << (pattern.length() - 1);
        for (int index = from; index < endExclusive; index++) {
            state = ((state << 1) | 1L) & masks.forChar(text.charAt(index));
            if ((state & matchBit) != 0L) {
                int start = index - pattern.length() + 1;
                if (start >= from) return start;
            }
        }
        return -1;
    }

    private static int bitParallelLastIndexOf(
            String text, String pattern, int maxStart) {
        PatternMasks masks = new PatternMasks(pattern);
        long state = 0L;
        long matchBit = 1L << (pattern.length() - 1);
        int last = -1;
        int scanEnd = Math.min(text.length(), maxStart + pattern.length());
        for (int index = 0; index < scanEnd; index++) {
            state = ((state << 1) | 1L) & masks.forChar(text.charAt(index));
            if ((state & matchBit) != 0L) {
                int start = index - pattern.length() + 1;
                if (start <= maxStart) last = start;
            }
        }
        return last;
    }

    private static int kmpIndexOf(
            String text, String pattern, int from, int endExclusive) {
        LongPattern compiled = new LongPattern(pattern);
        int matched = 0;
        for (int index = from; index < endExclusive; index++) {
            matched = compiled.advance(matched, text.charAt(index));
            if (matched == compiled.length()) {
                return index - compiled.length() + 1;
            }
        }
        return -1;
    }

    private static int kmpLastIndexOf(String text, String pattern, int maxStart) {
        LongPattern compiled = new LongPattern(pattern);
        int matched = 0;
        int last = -1;
        int scanEnd = Math.min(text.length(), maxStart + compiled.length());
        for (int index = 0; index < scanEnd; index++) {
            matched = compiled.advance(matched, text.charAt(index));
            if (matched == compiled.length()) {
                int start = index - compiled.length() + 1;
                if (start <= maxStart) last = start;
                matched = compiled.fallbackAfterMatch();
            }
        }
        return last;
    }

    private static final class LongPattern {
        private final char[] units;
        private final int[] prefix;

        LongPattern(String pattern) {
            units = pattern.toCharArray();
            prefix = new int[units.length];
            for (int index = 1, matched = 0; index < units.length; index++) {
                while (matched > 0 && units[index] != units[matched]) {
                    matched = prefix[matched - 1];
                }
                if (units[index] == units[matched]) matched++;
                prefix[index] = matched;
            }
        }

        int length() {
            return units.length;
        }

        int advance(int matched, char next) {
            while (matched > 0 && next != units[matched]) {
                matched = prefix[matched - 1];
            }
            if (next == units[matched]) matched++;
            return matched;
        }

        int fallbackAfterMatch() {
            return prefix[units.length - 1];
        }
    }

    private static final class PatternMasks {
        private final char[] keys;
        private final long[] masks;
        private final int slotMask;

        PatternMasks(String pattern) {
            int capacity = 2;
            int required = Math.max(2, pattern.length() << 1);
            while (capacity < required) capacity <<= 1;
            keys = new char[capacity];
            masks = new long[capacity];
            slotMask = capacity - 1;
            for (int index = 0; index < pattern.length(); index++) {
                char key = pattern.charAt(index);
                int slot = slot(key);
                while (masks[slot] != 0L && keys[slot] != key) {
                    slot = (slot + 1) & slotMask;
                }
                keys[slot] = key;
                masks[slot] |= 1L << index;
            }
        }

        long forChar(char key) {
            int slot = slot(key);
            while (masks[slot] != 0L) {
                if (keys[slot] == key) return masks[slot];
                slot = (slot + 1) & slotMask;
            }
            return 0L;
        }

        private int slot(char key) {
            int x = key;
            x ^= x >>> 16;
            x *= 0x7feb_352d;
            x ^= x >>> 15;
            x *= 0x846c_a68b;
            x ^= x >>> 16;
            return x & slotMask;
        }
    }
}
