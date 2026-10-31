// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/**
 * {@code wc} shadow with POSIX C-locale semantics: bytes, newline bytes, and words as maximal runs of
 * bytes other than space, tab, LF, VT, FF and CR. Byte-level, so a split block never splits a count.
 */
public final class M3FileWcShadow implements M3FileShadow {
    private long bytes;
    private long lines;
    private long words;
    private boolean inWord;

    M3FileWcShadow() {
    }

    @Override
    public void update(byte[] block, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            byte b = block[i];
            if (b == '\n') lines++;
            boolean space = b == ' ' || (b >= '\t' && b <= '\r');
            if (!space && !inWord) words++;
            inWord = !space;
        }
        bytes += length;
    }

    public long bytes() {
        return bytes;
    }

    public long lines() {
        return lines;
    }

    public long words() {
        return words;
    }
}
