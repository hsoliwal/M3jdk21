// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Versioned, immutable source-byte image for M3File and parser clients. The
 * original M3File provider remains the sole filesystem observation owner;
 * this class retains neither a file descriptor nor another filesystem tree.
 * Offsets are physical bytes. UTF-16 mapping is validated, sparse and lazy.
 */
public final class M3FileSource {
    private static final int CHECKPOINT_STRIDE = 4096;
    private final byte[] bytes;
    private final int[] lineStarts;
    private final String sha256;
    private volatile Utf8Index utf8;

    private M3FileSource(byte[] owned) {
        bytes = owned;
        sha256 = HexFormat.of().formatHex(shaEngine().digest(owned));
        int[] lines = new int[16];
        lines[0] = 0;
        int used = 1;
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            if (value == '\r' || value == '\n') {
                if (value == '\r' && i + 1 < bytes.length && bytes[i + 1] == '\n') i++;
                if (used == lines.length) lines = Arrays.copyOf(lines, lines.length * 2);
                lines[used++] = i + 1;
            }
        }
        lineStarts = Arrays.copyOf(lines, used);
    }

    public static M3FileSource fromBytes(byte[] source) {
        return new M3FileSource(Objects.requireNonNull(source, "source").clone());
    }

    /**
     * One bounded M3File observation: content is copied out of the ephemeral
     * callback before its scratch page is reused. The original observer checks
     * post-read size, mtime and file key and supplies the already-computed hash.
     * This does not promise an atomic snapshot against undetectable concurrent I/O.
     */
    public static Capture capture(M3File file, int maxBytes) throws IOException {
        Objects.requireNonNull(file, "file");
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        M3File.Observation observation = file.observe(maxBytes, M3StreamProgress.none(),
                (data, offset, length) -> buffer.write(data, offset, length));
        M3FileSource source = new M3FileSource(buffer.toByteArray());
        ByteBuffer hash = ByteBuffer.wrap(HexFormat.of().parseHex(source.sha256));
        M3File.Fingerprint fingerprint = observation.fingerprint();
        if (fingerprint.size() != source.length()
                || fingerprint.digest0() != hash.getLong()
                || fingerprint.digest1() != hash.getLong()
                || fingerprint.digest2() != hash.getLong()
                || fingerprint.digest3() != hash.getLong()) {
            throw new IOException("observed source bytes do not match the admitted fingerprint");
        }
        return new Capture(source, observation);
    }
    public record Capture(M3FileSource source, M3File.Observation observation) {
        public Capture {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(observation, "observation");
        }
    }

    public int length() { return bytes.length; }
    public String sha256() { return sha256; }
    public byte[] bytes() { return bytes.clone(); }
    public byte[] bytes(int start, int end) {
        Objects.checkFromToIndex(start, end, bytes.length);
        return Arrays.copyOfRange(bytes, start, end);
    }
    public int lines() { return lineStarts.length; }
    public int lineStartByte(int zeroBasedLine) {
        return lineStarts[Objects.checkIndex(zeroBasedLine, lineStarts.length)];
    }
    public int lineAtByte(int offset) {
        Objects.checkIndex(offset, bytes.length + 1);
        int found = Arrays.binarySearch(lineStarts, offset);
        return found >= 0 ? found : -found - 2;
    }
    public int columnBytes(int offset) {
        return offset - lineStarts[lineAtByte(offset)];
    }
    public int columnUtf16(int offset) {
        return utf16AtByte(offset) - utf16AtByte(lineStarts[lineAtByte(offset)]);
    }
    /** Rejects mid-codepoint offsets and malformed UTF-8 instead of guessing. */
    public int utf16AtByte(int offset) {
        Objects.checkIndex(offset, bytes.length + 1);
        return utf8().utf16AtByte(offset, bytes);
    }
    /** Rejects boundaries inside a surrogate pair. */
    public int byteAtUtf16(int units) {
        return utf8().byteAtUtf16(units, bytes);
    }
    public int utf16Length() { return utf8().units; }

    /** Copy-on-edit source generation. Old ranges inside replaced text have no mapping. */
    public EditResult replace(int from, int to, byte[] replacement) {
        Objects.checkFromToIndex(from, to, bytes.length);
        Objects.requireNonNull(replacement, "replacement");
        int length = Math.addExact(Math.subtractExact(bytes.length, to - from), replacement.length);
        byte[] updated = new byte[length];
        System.arraycopy(bytes, 0, updated, 0, from);
        System.arraycopy(replacement, 0, updated, from, replacement.length);
        System.arraycopy(bytes, to, updated, from + replacement.length, bytes.length - to);
        return new EditResult(new M3FileSource(updated), new Edit(from, to,
                Math.addExact(from, replacement.length)));
    }
    public record Edit(int startByte, int oldEndByte, int newEndByte) {
        public int oldToNew(int position) {
            if (position < 0) throw new IndexOutOfBoundsException(position);
            if (position < startByte) return position;
            if (position < oldEndByte) return -1;
            return Math.addExact(position, newEndByte - oldEndByte);
        }
    }
    public record EditResult(M3FileSource source, Edit edit) {
        public EditResult {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(edit, "edit");
        }
    }

    private Utf8Index utf8() {
        Utf8Index current = utf8;
        if (current == null) {
            synchronized (this) {
                current = utf8;
                if (current == null) utf8 = current = Utf8Index.create(bytes);
            }
        }
        return current;
    }
    private static final class Utf8Index {
        final int[] byteOffsets, unitsAt;
        final int units;
        private Utf8Index(int[] byteOffsets, int[] unitsAt, int units) {
            this.byteOffsets = byteOffsets; this.unitsAt = unitsAt; this.units = units;
        }
        static Utf8Index create(byte[] data) {
            int[] bp = new int[Math.max(2, data.length / CHECKPOINT_STRIDE + 2)];
            int[] up = new int[bp.length];
            int checkpoints = 1, index = 0, units = 0;
            while (index < data.length) {
                if (index - bp[checkpoints - 1] >= CHECKPOINT_STRIDE) {
                    bp[checkpoints] = index; up[checkpoints] = units; checkpoints++;
                }
                int cp = scalar(data, index);
                index += width(data[index] & 0xff);
                units = Math.addExact(units, cp > 0xffff ? 2 : 1);
            }
            return new Utf8Index(Arrays.copyOf(bp, checkpoints),
                    Arrays.copyOf(up, checkpoints), units);
        }
        int utf16AtByte(int offset, byte[] bytes) {
            int checkpoint = floor(byteOffsets, offset);
            int index = byteOffsets[checkpoint], count = unitsAt[checkpoint];
            while (index < offset) {
                int cp = scalar(bytes, index);
                int next = index + width(bytes[index] & 0xff);
                if (next > offset) throw new IllegalArgumentException("byte offset splits UTF-8 scalar");
                count += cp > 0xffff ? 2 : 1;
                index = next;
            }
            return count;
        }
        int byteAtUtf16(int offset, byte[] bytes) {
            if (offset < 0 || offset > units) throw new IndexOutOfBoundsException(offset);
            int checkpoint = floor(unitsAt, offset);
            int index = byteOffsets[checkpoint], count = unitsAt[checkpoint];
            while (count < offset) {
                int cp = scalar(bytes, index);
                int delta = cp > 0xffff ? 2 : 1;
                if (count + delta > offset)
                    throw new IllegalArgumentException("UTF-16 offset splits surrogate pair");
                index += width(bytes[index] & 0xff);
                count += delta;
            }
            return index;
        }
        private static int floor(int[] array, int key) {
            int lo = 0, hi = array.length;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (array[mid] <= key) lo = mid + 1;
                else hi = mid;
            }
            return lo - 1;
        }
    }

    /** Strict scalar-only UTF-8, including rejection of overlong and surrogate encodings. */
    private static int scalar(byte[] bytes, int i) {
        int first = bytes[i] & 0xff;
        int len = width(first);
        if (len < 0 || len > bytes.length - i)
            throw new IllegalArgumentException("invalid/truncated UTF-8 at byte " + i);
        if (len == 1) return first;
        int cp = first & ((1 << (7 - len)) - 1);
        for (int j = 1; j < len; j++) {
            int next = bytes[i + j] & 0xff;
            if ((next & 0xc0) != 0x80)
                throw new IllegalArgumentException("invalid UTF-8 continuation at byte " + (i + j));
            cp = (cp << 6) | (next & 0x3f);
        }
        if (cp < (len == 2 ? 0x80 : len == 3 ? 0x800 : 0x10000)
                || (cp >= 0xd800 && cp <= 0xdfff) || cp > 0x10ffff)
            throw new IllegalArgumentException("non-scalar UTF-8 at byte " + i);
        return cp;
    }
    private static int width(int first) {
        if (first < 0x80) return 1;
        if (first >= 0xc2 && first <= 0xdf) return 2;
        if (first >= 0xe0 && first <= 0xef) return 3;
        if (first >= 0xf0 && first <= 0xf4) return 4;
        return -1;
    }
    private static MessageDigest shaEngine() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
