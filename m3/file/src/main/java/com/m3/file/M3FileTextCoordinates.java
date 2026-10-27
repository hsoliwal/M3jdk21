// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Strict, encoding-declared source coordinate view of an existing immutable
 * M3FileSource. The source still exclusively owns bytes and SHA-256; this class
 * retains only bounded primitive checkpoints and line offsets, not a second text
 * or filesystem image. Coordinates never silently replace malformed bytes.
 *
 * <p>Line/point columns are PHYSICAL bytes (Tree-sitter convention); Java/JDT
 * UTF-16 coordinates are explicit separate methods. UTF-8 BOM must be requested;
 * UTF-16 LE/BE BOM is optional when the encoding is explicitly declared and a
 * contrary BOM refuses admission. A BOM belongs to the source byte generation
 * but is not a decoded character; byte positions inside it are not addressable.
 */
public final class M3FileTextCoordinates {
    public enum Encoding { UTF8, UTF8_BOM, UTF16_LE, UTF16_BE, LATIN1 }
    public record Point(int row, int byteColumn) {
        public Point {
            if (row < 0 || byteColumn < 0) throw new IllegalArgumentException("negative point");
        }
    }
    private static final int STEP_BYTES = 4096;
    private final M3FileSource source;
    private final Encoding encoding;
    private final int bomBytes, units, scalarCount;
    private final int[] checkpointsByte, checkpointsUtf16;
    private final int[] lineBytes, lineUtf16;

    public M3FileTextCoordinates(M3FileSource source, Encoding encoding) {
        this.source = Objects.requireNonNull(source, "source");
        this.encoding = Objects.requireNonNull(encoding, "encoding");
        bomBytes = bom(source, encoding);
        Ints cpBytes = new Ints(), cpUnits = new Ints();
        Ints lb = new Ints(), lu = new Ints();
        cpBytes.add(bomBytes); cpUnits.add(0);
        lb.add(bomBytes); lu.add(0);
        int cursor = bomBytes, utf16 = 0, scalars = 0;
        int pendingCrEndByte = -1, pendingCrEndUtf16 = -1;
        while (cursor < source.length()) {
            if (cursor - cpBytes.last() >= STEP_BYTES) {
                cpBytes.add(cursor); cpUnits.add(utf16);
            }
            long decoded = decode(source, cursor, encoding);
            int cp = (int) (decoded >>> 32);
            int width = (int) decoded;
            cursor += width;
            utf16 = Math.addExact(utf16, cp > 0xffff ? 2 : 1);
            scalars++;
            if (pendingCrEndByte >= 0) {
                if (cp == '\n') {
                    lb.add(cursor); lu.add(utf16);
                    pendingCrEndByte = -1;
                    continue;
                }
                lb.add(pendingCrEndByte); lu.add(pendingCrEndUtf16);
                pendingCrEndByte = -1;
            }
            if (cp == '\r') {
                pendingCrEndByte = cursor;
                pendingCrEndUtf16 = utf16;
            } else if (cp == '\n') {
                lb.add(cursor); lu.add(utf16);
            }
        }
        if (pendingCrEndByte >= 0) {
            lb.add(pendingCrEndByte); lu.add(pendingCrEndUtf16);
        }
        checkpointsByte = cpBytes.freeze();
        checkpointsUtf16 = cpUnits.freeze();
        lineBytes = lb.freeze();
        lineUtf16 = lu.freeze();
        units = utf16;
        scalarCount = scalars;
    }
    public Encoding encoding() { return encoding; }
    public String sourceSha256() { return source.sha256(); }
    public int contentStartByte() { return bomBytes; }
    public int sourceLengthBytes() { return source.length(); }
    public int utf16Length() { return units; }
    public int unicodeScalars() { return scalarCount; }
    public int lines() { return lineBytes.length; }
    public int lineStartByte(int line) { return lineBytes[Objects.checkIndex(line, lines())]; }
    public int lineStartUtf16(int line) { return lineUtf16[Objects.checkIndex(line, lines())]; }
    public void requireSource(M3FileSource image) {
        if (!source.sha256().equals(Objects.requireNonNull(image, "image").sha256()))
            throw new IllegalStateException("text coordinates belong to another source generation");
    }

    /** Absolute decoded UTF-16 offset at an exact Unicode scalar byte boundary. */
    public int utf16AtByte(int byteOffset) {
        if (byteOffset < bomBytes || byteOffset > source.length())
            throw new IndexOutOfBoundsException("offset outside decoded source");
        int p = floor(checkpointsByte, byteOffset);
        int cursor = checkpointsByte[p], current = checkpointsUtf16[p];
        while (cursor < byteOffset) {
            long v = decode(source, cursor, encoding);
            int next = cursor + (int) v;
            if (next > byteOffset) throw new IllegalArgumentException("split Unicode scalar byte range");
            current += (v >>> 32) > 0xffff ? 2 : 1;
            cursor = next;
        }
        return current;
    }
    /** Exact UTF-16 source position; midpoint of surrogate pair is invalid. */
    public int byteAtUtf16(int position) {
        if (position < 0 || position > units) throw new IndexOutOfBoundsException(position);
        int p = floor(checkpointsUtf16, position);
        int cursor = checkpointsByte[p], current = checkpointsUtf16[p];
        while (current < position) {
            long v = decode(source, cursor, encoding);
            int delta = (v >>> 32) > 0xffff ? 2 : 1;
            if (current + delta > position)
                throw new IllegalArgumentException("split UTF-16 surrogate pair");
            cursor += (int) v;
            current += delta;
        }
        return cursor;
    }
    public int lineAtByte(int byteOffset) {
        if (byteOffset < bomBytes || byteOffset > source.length())
            throw new IndexOutOfBoundsException(byteOffset);
        return floor(lineBytes, byteOffset);
    }
    public Point pointAtByte(int byteOffset) {
        int line = lineAtByte(byteOffset);
        utf16AtByte(byteOffset); // refuse split scalar or malformed boundary
        return new Point(line, byteOffset - lineBytes[line]);
    }
    public int utf16ColumnAtByte(int byteOffset) {
        int line = lineAtByte(byteOffset);
        return utf16AtByte(byteOffset) - lineUtf16[line];
    }
    /** Refuse points extending into the next line or splitting a scalar. */
    public int byteAtPoint(Point point) {
        Objects.requireNonNull(point, "point");
        int line = Objects.checkIndex(point.row(), lines());
        long byteOffset = (long) lineBytes[line] + point.byteColumn();
        if (byteOffset > source.length()
                || (line + 1 < lines() && byteOffset >= lineBytes[line + 1]))
            throw new IndexOutOfBoundsException("point escapes line");
        int position = Math.toIntExact(byteOffset);
        utf16AtByte(position);
        return position;
    }
    public int byteAtUtf16Point(int row, int columnUtf16) {
        int line = Objects.checkIndex(row, lines());
        if (columnUtf16 < 0) throw new IndexOutOfBoundsException(columnUtf16);
        long index = (long) lineUtf16[line] + columnUtf16;
        if (index > units || (line + 1 < lines() && index >= lineUtf16[line + 1]))
            throw new IndexOutOfBoundsException("UTF-16 position escapes line");
        return byteAtUtf16((int) index);
    }
    /** Verified decoding; never use String's replacement-character fallback. */
    public String decodedText() {
        byte[] value = source.bytes();
        return switch (encoding) {
            case UTF8, UTF8_BOM -> new String(value, bomBytes,
                    value.length - bomBytes, StandardCharsets.UTF_8);
            case UTF16_LE -> new String(value, bomBytes,
                    value.length - bomBytes, StandardCharsets.UTF_16LE);
            case UTF16_BE -> new String(value, bomBytes,
                    value.length - bomBytes, StandardCharsets.UTF_16BE);
            case LATIN1 -> new String(value, StandardCharsets.ISO_8859_1);
        };
    }
    private static int bom(M3FileSource b, Encoding codec) {
        boolean utf8 = b.length() >= 3 && b.byteAt(0) == 0xef
                && b.byteAt(1) == 0xbb && b.byteAt(2) == 0xbf;
        boolean le = b.length() >= 2 && b.byteAt(0) == 0xff && b.byteAt(1) == 0xfe;
        boolean be = b.length() >= 2 && b.byteAt(0) == 0xfe && b.byteAt(1) == 0xff;
        return switch (codec) {
            case UTF8_BOM -> {
                if (!utf8) throw new IllegalArgumentException("UTF-8 BOM required");
                yield 3;
            }
            case UTF16_LE -> {
                if (be) throw new IllegalArgumentException("opposite UTF-16 BOM");
                yield le ? 2 : 0;
            }
            case UTF16_BE -> {
                if (le) throw new IllegalArgumentException("opposite UTF-16 BOM");
                yield be ? 2 : 0;
            }
            case UTF8 -> {
                if (utf8) throw new IllegalArgumentException("explicit UTF8_BOM required");
                yield 0;
            }
            case LATIN1 -> 0;
        };
    }
    private static long decode(M3FileSource b, int offset, Encoding codec) {
        return switch (codec) {
            case UTF8, UTF8_BOM -> decodeUtf8(b, offset);
            case UTF16_LE, UTF16_BE -> decodeUtf16(b, offset, codec == Encoding.UTF16_LE);
            case LATIN1 -> ((long) b.byteAt(offset) << 32) | 1L;
        };
    }
    private static long decodeUtf16(M3FileSource b, int i, boolean littleEndian) {
        if (i + 1 >= b.length()) throw new IllegalArgumentException("odd-length UTF-16 source");
        int first = littleEndian ? (b.byteAt(i) | (b.byteAt(i + 1) << 8))
                : ((b.byteAt(i) << 8) | b.byteAt(i + 1));
        if (first >= 0xdc00 && first <= 0xdfff)
            throw new IllegalArgumentException("orphan UTF-16 low surrogate");
        if (first < 0xd800 || first > 0xdbff) return ((long) first << 32) | 2L;
        if (i + 3 >= b.length()) throw new IllegalArgumentException("truncated UTF-16 pair");
        int second = littleEndian ? (b.byteAt(i + 2) | (b.byteAt(i + 3) << 8))
                : ((b.byteAt(i + 2) << 8) | b.byteAt(i + 3));
        if (second < 0xdc00 || second > 0xdfff)
            throw new IllegalArgumentException("orphan UTF-16 high surrogate");
        int cp = Character.toCodePoint((char) first, (char) second);
        return ((long) cp << 32) | 4L;
    }
    private static long decodeUtf8(M3FileSource b, int i) {
        int first = b.byteAt(i);
        int width = first < 0x80 ? 1 : first >= 0xc2 && first <= 0xdf ? 2
                : first >= 0xe0 && first <= 0xef ? 3
                : first >= 0xf0 && first <= 0xf4 ? 4 : -1;
        if (width < 0 || width > b.length() - i)
            throw new IllegalArgumentException("invalid UTF-8 lead or truncation");
        int cp = width == 1 ? first : first & ((1 << (7 - width)) - 1);
        for (int at = i + 1; at < i + width; at++) {
            int c = b.byteAt(at);
            if ((c & 0xc0) != 0x80) throw new IllegalArgumentException("invalid UTF-8 continuation");
            cp = (cp << 6) | (c & 0x3f);
        }
        if (cp < (width == 1 ? 0 : width == 2 ? 0x80 : width == 3 ? 0x800 : 0x10000)
                || (cp >= 0xd800 && cp <= 0xdfff) || cp > 0x10ffff)
            throw new IllegalArgumentException("non-scalar UTF-8 sequence");
        return ((long) cp << 32) | width;
    }
    private static int floor(int[] offsets, int key) {
        int lo = 0, hi = offsets.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (offsets[mid] <= key) lo = mid + 1;
            else hi = mid;
        }
        return lo - 1;
    }
    private static final class Ints {
        private int[] values = new int[16];
        private int size;
        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = value;
        }
        int last() { return values[size - 1]; }
        int[] freeze() { return Arrays.copyOf(values, size); }
    }
}
