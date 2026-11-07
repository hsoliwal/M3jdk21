// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileWcShadow.java blob edcfa21789743527e39820611cf602b68edd60f6 (retained PR #1619 head; parent 0cd0944136c1 blob 648c49fe7a6884be0e5d9504f5b3fe8376a5d6bd https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileWcShadow.java); recipe m3/file/qualification/retained-port-20261107 RESTORE_EXACT

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
