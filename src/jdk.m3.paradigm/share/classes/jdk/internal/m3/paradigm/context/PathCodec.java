/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Bounded text/primitive-word codec used by the existing ContextPath owner.
 * This does not resolve contexts, generate identities, or grant authority.
 * Caller-owned array overloads allocate no internal objects on valid inputs.
 * Arrays must not be mutated concurrently while read or written by this codec.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextPathCodec.java}
 * blob {@code 6b300c8df582701be6f6ac5a1565f78f03f6bbea} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PathCodec {
    public static final int MAX_DEPTH = 256;
    public static final int MAX_INPUT_CHARS = 65_536;
    public static final int CHARS_PER_SEGMENT = 37;
    private static final String HEX = "0123456789abcdef";
    private PathCodec() { }

    public static long[] parse(String text) {
        long range = validate(text);
        int start = (int) (range >>> 32), length = (int) range;
        long[] words = new long[length == 1 ? 0 : length / CHARS_PER_SEGMENT * 2];
        decode(text, start, words, 0, words.length / 2);
        return words;
    }

    /**
     * Validates the complete immutable input and destination before writing.
     * Invalid text/capacity leaves the destination unchanged. Returns UID depth.
     * Offset and capacity are in long words, not UID segments or bytes.
     */
    public static int parseInto(String text, long[] target, int offset) {
        Objects.requireNonNull(target, "target");
        long range = validate(text);
        int start = (int) (range >>> 32), length = (int) range;
        int depth = length == 1 ? 0 : length / CHARS_PER_SEGMENT;
        Objects.checkFromIndexSize(offset, depth * 2, target.length);
        decode(text, start, target, offset, depth);
        return depth;
    }

    public static String format(long[] words) {
        int length = renderedLength(words);
        if (words.length == 0) return "/";
        char[] text = new char[length];
        formatInto(words, text, 0);
        return new String(text);
    }

    /** Writes canonical lower-case UID text into a caller-owned character buffer. */
    public static int formatInto(long[] words, char[] target, int offset) {
        int length = renderedLength(words);
        Objects.requireNonNull(target, "target");
        Objects.checkFromIndexSize(offset, length, target.length);
        if (words.length == 0) { target[offset] = '/'; return 1; }
        int out = offset;
        for (int index = 0; index < words.length; index += 2) {
            target[out++] = '/';
            for (int digit = 0; digit < 32; digit++) {
                if (digit == 8 || digit == 12 || digit == 16 || digit == 20) target[out++] = '-';
                long word = words[index + digit / 16];
                target[out++] = HEX.charAt((int) (word >>> ((15 - digit % 16) * 4)) & 15);
            }
        }
        return length;
    }

    private static int renderedLength(long[] words) {
        Objects.requireNonNull(words, "words");
        if ((words.length & 1) != 0 || words.length > MAX_DEPTH * 2) {
            throw new IllegalArgumentException("Path requires at most 256 high/low UID word pairs");
        }
        return words.length == 0 ? 1 : words.length / 2 * CHARS_PER_SEGMENT;
    }

    /** Packs start/length into one primitive return value; no range helper allocation. */
    private static long validate(String text) {
        Objects.requireNonNull(text, "path");
        if (text.length() > MAX_INPUT_CHARS) throw new IllegalArgumentException("Path input exceeds character bound");
        int start = 0, end = text.length();
        while (start < end && Character.isWhitespace(text.codePointAt(start))) {
            start += Character.charCount(text.codePointAt(start));
        }
        while (end > start && Character.isWhitespace(text.codePointBefore(end))) {
            end -= Character.charCount(text.codePointBefore(end));
        }
        int length = end - start;
        if (length == 1 && text.charAt(start) == '/') return ((long) start << 32) | 1;
        if (length == 0 || length % CHARS_PER_SEGMENT != 0 || length > MAX_DEPTH * CHARS_PER_SEGMENT) {
            throw new IllegalArgumentException("Invalid UID path length or depth");
        }
        for (int segment = start; segment < end; segment += CHARS_PER_SEGMENT) {
            if (text.charAt(segment) != '/') throw new IllegalArgumentException("Expected UID path separator");
            for (int i = 0; i < 36; i++) {
                char c = text.charAt(segment + 1 + i);
                boolean hyphen = i == 8 || i == 13 || i == 18 || i == 23;
                if (hyphen ? c != '-' : nibble(c) < 0) {
                    throw new IllegalArgumentException("Invalid canonical UID path segment");
                }
            }
        }
        return ((long) start << 32) | length;
    }

    private static void decode(String text, int start, long[] target, int offset, int depth) {
        for (int segment = 0; segment < depth; segment++) {
            long high = 0, low = 0;
            int digits = 0, from = start + segment * CHARS_PER_SEGMENT + 1;
            for (int i = 0; i < 36; i++) {
                char c = text.charAt(from + i);
                if (c == '-') continue;
                if (digits++ < 16) high = (high << 4) | nibble(c);
                else low = (low << 4) | nibble(c);
            }
            target[offset + segment * 2] = high;
            target[offset + segment * 2 + 1] = low;
        }
    }
    private static int nibble(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
