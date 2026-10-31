// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * Line-start byte offsets, built while the main stream moves.
 *
 * <p>Terminators are LF, CR and CRLF, as {@code BufferedReader.readLine}; a final line without a
 * terminator counts, an empty input has no lines. CR and LF never occur inside UTF-8 multi-byte
 * sequences, so the offsets are exact for UTF-8 and every ASCII-compatible encoding.</p>
 */
public final class M3FileLineShadow implements M3FileShadow {
    private long[] starts = new long[64];
    private int count = 1;
    private long position;
    private boolean afterCr;

    M3FileLineShadow() {
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        for (int i = offset; i < offset + length; i++) {
            byte b = bytes[i];
            position++;
            if (b == '\n') {
                if (afterCr) starts[count - 1] = position;
                else add(position);
                afterCr = false;
            } else if (b == '\r') {
                add(position);
                afterCr = true;
            } else {
                afterCr = false;
            }
        }
    }

    /** Bytes seen. */
    public long length() {
        return position;
    }

    public int lineCount() {
        return starts[count - 1] < position ? count : count - 1;
    }

    /** Byte offset where {@code line} starts. */
    public long lineStart(int line) {
        Objects.checkIndex(line, lineCount());
        return starts[line];
    }

    /** Line containing byte {@code offset} in {@code [0, length())}. */
    public int lineOf(long offset) {
        Objects.checkIndex(offset, position);
        int found = Arrays.binarySearch(starts, 0, count, offset);
        return found >= 0 ? found : -found - 2;
    }

    private void add(long start) {
        if (count == starts.length) starts = Arrays.copyOf(starts, count * 2);
        starts[count++] = start;
    }
}
