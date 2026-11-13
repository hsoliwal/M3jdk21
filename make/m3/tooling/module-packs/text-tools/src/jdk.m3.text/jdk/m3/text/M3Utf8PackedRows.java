// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.List;
import java.util.Objects;

/**
 * One exact-length UTF-8 byte lane plus primitive row offsets for immutable M3 text.
 * This is a Java-side packing boundary, not a second M3 String owner or JNI heap.
 * Every row is validated before allocating the packed payload. No per-line byte[]
 * or ByteArrayOutputStream grows during construction.
 *
 * <p>Callers must not concurrently modify their input List while packing.
 * The bytes and offsets are private to their single NativeLineBatch owner.</p>
 */
final class M3Utf8PackedRows {
    record Packed(byte[] bytes, int[] offsets) { }

    private M3Utf8PackedRows() { }

    static Packed pack(List<String> rows) {
        Objects.requireNonNull(rows, "rows");
        int n = rows.size();
        int[] offsets = new int[Math.addExact(n, 1)];
        long bytes = 0;
        for (int i = 0; i < n; i++) {
            String row = Objects.requireNonNull(rows.get(i), "row");
            bytes = Math.addExact(bytes, byteLength(row));
            if (bytes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("UTF-8 line window exceeds Java array limit");
            }
            offsets[i + 1] = (int) bytes;
        }
        byte[] packed = new byte[(int) bytes];
        for (int i = 0; i < n; i++) {
            int cursor = encode(Objects.requireNonNull(rows.get(i), "row"),
                    packed, offsets[i], offsets[i + 1]);
            if (cursor != offsets[i + 1]) {
                throw new IllegalArgumentException("UTF-8 row changed during preparation");
            }
        }
        return new Packed(packed, offsets);
    }

    private static long byteLength(String row) {
        long bytes = 0;
        for (int i = 0; i < row.length(); i++) {
            char code = row.charAt(i);
            if (code <= 0x7f) bytes++;
            else if (code <= 0x7ff) bytes += 2;
            else if (Character.isHighSurrogate(code)) {
                if (++i >= row.length() || !Character.isLowSurrogate(row.charAt(i))) {
                    throw new IllegalArgumentException("unpaired high surrogate");
                }
                bytes += 4;
            } else if (Character.isLowSurrogate(code)) {
                throw new IllegalArgumentException("unpaired low surrogate");
            } else bytes += 3;
        }
        return bytes;
    }

    private static int encode(String row, byte[] out, int start, int end) {
        int cursor = start;
        for (int i = 0; i < row.length(); i++) {
            int code = row.charAt(i);
            if (Character.isHighSurrogate((char) code)) {
                if (i + 1 >= row.length() || !Character.isLowSurrogate(row.charAt(i + 1))) {
                    throw new IllegalArgumentException("unpaired high surrogate");
                }
                code = Character.toCodePoint((char) code, row.charAt(++i));
            } else if (Character.isLowSurrogate((char) code)) {
                throw new IllegalArgumentException("unpaired low surrogate");
            }
            int width = code <= 0x7f ? 1 : code <= 0x7ff ? 2 : code <= 0xffff ? 3 : 4;
            if (cursor > end - width) {
                throw new IllegalArgumentException("UTF-8 row changed during preparation");
            }
            switch (width) {
                case 1 -> out[cursor++] = (byte) code;
                case 2 -> {
                    out[cursor++] = (byte) (0xc0 | (code >>> 6));
                    out[cursor++] = (byte) (0x80 | (code & 0x3f));
                }
                case 3 -> {
                    out[cursor++] = (byte) (0xe0 | (code >>> 12));
                    out[cursor++] = (byte) (0x80 | ((code >>> 6) & 0x3f));
                    out[cursor++] = (byte) (0x80 | (code & 0x3f));
                }
                case 4 -> {
                    out[cursor++] = (byte) (0xf0 | (code >>> 18));
                    out[cursor++] = (byte) (0x80 | ((code >>> 12) & 0x3f));
                    out[cursor++] = (byte) (0x80 | ((code >>> 6) & 0x3f));
                    out[cursor++] = (byte) (0x80 | (code & 0x3f));
                }
                default -> throw new AssertionError(width);
            }
        }
        return cursor;
    }
}
