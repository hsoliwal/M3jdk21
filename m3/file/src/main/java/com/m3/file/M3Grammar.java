// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;

/**
 * One-read structural grammar observer for the existing M3File character-shadow pipeline.
 *
 * <p>The TEXT_PARAGRAPHS_V1 profile groups nonblank physical lines. A blank line or
 * U+2029 ends a paragraph; CRLF, CR, LF, NEL and U+2028 delimit lines. Offsets are
 * half-open UTF-16 coordinates, not byte positions. Nonblank lines retain their
 * leading/trailing whitespace; the final line terminator is excluded. Whitespace
 * token counts are not linguistic word counts or programming-language syntax.</p>
 *
 * <p>Feed through {@code M3FileShadow.utf8(grammar)} during the original file read.
 * No text, file handle or second parser is retained. One observer belongs to one
 * source-read generation and one thread. The decoder's malformed-input policy
 * remains authoritative. Results are complete only after {@link #finish()}; bind
 * any later reuse to source identity, generation, digest and decoding/profile policy.</p>
 */
public final class M3Grammar implements M3FileCharShadow {
    public static final String PROFILE = "TEXT_PARAGRAPHS_V1";
    private static final long MAX_UNITS = Long.MAX_VALUE - 1;
    private final long maxUnits;
    private final M3Paragraphs.Builder rows;
    private long units;
    private long line = 1;
    private long lineStart;
    private long lineTokens;
    private int lineFlags;
    private boolean lineHasChars;
    private boolean lineNonblank;
    private boolean inToken;
    private boolean afterCr;
    private long start = -1;
    private long end;
    private long firstLine;
    private long lastLine;
    private long tokens;
    private int flags;
    private int betweenFlags;
    private boolean failed;
    private M3Paragraphs result;

    private M3Grammar(int maxParagraphs, long maxUtf16Units) {
        if (maxUtf16Units < 0 || maxUtf16Units > MAX_UNITS) {
            throw new IllegalArgumentException("invalid UTF-16 budget");
        }
        rows = new M3Paragraphs.Builder(maxParagraphs);
        maxUnits = maxUtf16Units;
    }

    /** Creates a single-use observer with an explicit paragraph count budget. */
    public static M3Grammar paragraphs(int maxParagraphs) {
        return paragraphs(maxParagraphs, MAX_UNITS);
    }

    /** Creates a single-use observer with explicit row and UTF-16 input budgets. */
    public static M3Grammar paragraphs(int maxParagraphs, long maxUtf16Units) {
        return new M3Grammar(maxParagraphs, maxUtf16Units);
    }

    @Override
    public void update(char[] chars, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, chars.length);
        requireActive();
        if (length > maxUnits - units) {
            failed = true;
            throw new IllegalStateException("grammar UTF-16 budget exceeded");
        }
        try {
            for (int i = offset; i < offset + length; i++) accept(chars[i]);
        } catch (RuntimeException | Error failure) {
            failed = true;
            throw failure;
        }
    }

    /** Idempotently completes the observed input; call only at actual end-of-input. */
    @Override
    public void finish() {
        if (result != null) return;
        requireActive();
        try {
            if (lineHasChars) acceptLine(units);
            emit();
            result = rows.freeze(units);
        } catch (RuntimeException | Error failure) {
            failed = true;
            throw failure;
        }
    }

    /** Immutable offset/index/mask metadata. No partial result is exposed. */
    public M3Paragraphs details() {
        if (result == null) throw new IllegalStateException("grammar input is incomplete or failed");
        return result;
    }

    private void accept(char value) {
        long position = units++;
        if (afterCr) {
            afterCr = false;
            if (value == '\n') {
                lineStart = units;
                return;
            }
        }
        if (lineBreak(value)) {
            acceptLine(position);
            if (start >= 0 && value > 0x7f) betweenFlags |= M3Paragraphs.NON_ASCII;
            if (value == '\u2029') emit();
            line++;
            lineStart = units;
            lineTokens = 0;
            lineFlags = 0;
            lineHasChars = false;
            lineNonblank = false;
            inToken = false;
            afterCr = value == '\r';
            return;
        }
        lineHasChars = true;
        if (value > 0x7f) lineFlags |= M3Paragraphs.NON_ASCII;
        if (Character.isSurrogate(value)) lineFlags |= M3Paragraphs.SURROGATE;
        if (value == '\t') lineFlags |= M3Paragraphs.TAB;
        boolean space = Character.isWhitespace(value) || Character.isSpaceChar(value);
        if (!space) {
            lineNonblank = true;
            if (!inToken) lineTokens++;
        }
        inToken = !space;
    }

    private void acceptLine(long contentEnd) {
        if (!lineNonblank) {
            emit();
            return;
        }
        if (start < 0) {
            start = lineStart;
            firstLine = line;
        }
        end = contentEnd;
        lastLine = line;
        tokens += lineTokens;
        flags |= lineFlags | betweenFlags;
        betweenFlags = 0;
    }

    private void emit() {
        if (start < 0) return;
        int state = flags | (lastLine > firstLine ? M3Paragraphs.MULTILINE : 0);
        rows.add(start, end, firstLine, lastLine, tokens, state);
        start = -1;
        tokens = 0;
        flags = 0;
        betweenFlags = 0;
    }

    private void requireActive() {
        if (failed || result != null) throw new IllegalStateException("grammar observer is closed or failed");
    }

    private static boolean lineBreak(char value) {
        return value == '\r' || value == '\n' || value == '\u0085'
                || value == '\u2028' || value == '\u2029';
    }
}
