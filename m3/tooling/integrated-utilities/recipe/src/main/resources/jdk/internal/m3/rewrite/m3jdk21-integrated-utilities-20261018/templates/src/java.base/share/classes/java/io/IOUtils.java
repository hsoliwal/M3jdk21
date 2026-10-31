/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * Operations on byte streams and character readers and writers.
 *
 * <p>All resources belong to the caller. These methods never close or flush
 * them. Unless specified otherwise, a null argument causes a
 * {@link NullPointerException}. An I/O failure is propagated unchanged;
 * input may already have been consumed and output may already have been
 * written when a failure occurs. Cancellation, blocking, asynchronous
 * closure, and thread safety follow the supplied resource implementations.
 *
 * <p>Character counts, offsets, and lengths are measured in UTF-16 code
 * units. Byte/character conversion always requires an explicit
 * {@link Charset}. Bulk operations use existing stream owners and bounded
 * scratch arrays; they do not keep a second persistent content store.
 *
 * @since 21
 */
public final class IOUtils {
    private static final int BUFFER_SIZE = 8192;

    private IOUtils() {
        throw new AssertionError("no instances");
    }

    /**
     * Copies the remaining bytes, leaving both streams open and unflushed.
     *
     * @implSpec This method invokes {@link InputStream#transferTo(OutputStream)}
     * on {@code input}, preserving specialized transfer implementations.
     * @param input the input stream
     * @param output the output stream
     * @return the transferred byte count, saturated at {@link Long#MAX_VALUE}
     * @throws IOException if reading or writing fails
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        return input.transferTo(output);
    }

    /**
     * Copies the remaining characters, leaving the reader and writer open
     * and unflushed.
     *
     * @implSpec This method invokes {@link Reader#transferTo(Writer)} on
     * {@code input}, preserving specialized transfer implementations.
     * @param input the reader
     * @param output the writer
     * @return the transferred character count, saturated at {@link Long#MAX_VALUE}
     * @throws IOException if reading or writing fails
     */
    public static long copy(Reader input, Writer output) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        return input.transferTo(output);
    }

    /**
     * Copies at most {@code limit} bytes from the current input position.
     * Stops at the limit or end of stream without reading beyond the limit.
     * A zero limit performs no I/O. Uses at most 8192 scratch bytes.
     *
     * @param input the input stream
     * @param output the output stream
     * @param limit the maximum number of bytes to copy
     * @return the number of bytes copied, possibly less than {@code limit}
     * @throws IllegalArgumentException if {@code limit} is negative
     * @throws IOException if reading or writing fails
     */
    public static long copy(InputStream input, OutputStream output, long limit)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        checkNonNegative(limit, "limit");
        if (limit == 0) return 0;
        byte[] buffer = new byte[(int) Math.min(BUFFER_SIZE, limit)];
        long copied = 0;
        while (copied < limit) {
            int requested = (int) Math.min(buffer.length, limit - copied);
            int count = readChunk(input, buffer, 0, requested);
            if (count < 0) break;
            output.write(buffer, 0, count);
            copied += count;
        }
        return copied;
    }

    /**
     * Copies at most {@code limit} UTF-16 code units from the current reader
     * position. Stops at the limit or end of stream without reading beyond
     * the limit. A zero limit performs no I/O. Uses at most 8192 scratch chars.
     *
     * @param input the reader
     * @param output the writer
     * @param limit the maximum number of characters to copy
     * @return the number of characters copied, possibly less than {@code limit}
     * @throws IllegalArgumentException if {@code limit} is negative
     * @throws IOException if reading or writing fails
     */
    public static long copy(Reader input, Writer output, long limit) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        checkNonNegative(limit, "limit");
        if (limit == 0) return 0;
        char[] buffer = new char[(int) Math.min(BUFFER_SIZE, limit)];
        long copied = 0;
        while (copied < limit) {
            int requested = (int) Math.min(buffer.length, limit - copied);
            int count = readChunk(input, buffer, 0, requested);
            if (count < 0) break;
            output.write(buffer, 0, count);
            copied += count;
        }
        return copied;
    }

    /**
     * Skips exactly {@code offset} bytes from the current input position,
     * then copies at most {@code length} bytes. An offset beyond end of
     * stream throws {@link EOFException}; a short copied range returns the
     * number actually copied. If {@code length} is zero, no input is read
     * or skipped. The offset and length are not added, so their sum need
     * not be representable as a {@code long}.
     *
     * @param input the input stream
     * @param output the output stream
     * @param offset the number of bytes to skip
     * @param length the maximum number of bytes to copy
     * @return the number of bytes copied
     * @throws IllegalArgumentException if {@code offset} or {@code length} is negative
     * @throws EOFException if the nonempty range starts beyond end of stream
     * @throws IOException if skipping, reading, or writing fails
     */
    public static long copyRange(InputStream input, OutputStream output,
                                 long offset, long length) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        checkNonNegative(offset, "offset");
        checkNonNegative(length, "length");
        if (length == 0) return 0;
        skipFully(input, offset);
        return copy(input, output, length);
    }

    /**
     * Skips exactly {@code offset} UTF-16 code units, then copies at most
     * {@code length} code units. An offset beyond end of stream throws
     * {@link EOFException}; a short copied range returns the number copied.
     * A zero length performs no I/O, including no skip. Offsets refer to
     * the current reader position, and need not fall on code point boundaries.
     *
     * @param input the reader
     * @param output the writer
     * @param offset the number of characters to skip
     * @param length the maximum number of characters to copy
     * @return the number of characters copied
     * @throws IllegalArgumentException if {@code offset} or {@code length} is negative
     * @throws EOFException if the nonempty range starts beyond end of stream
     * @throws IOException if skipping, reading, or writing fails
     */
    public static long copyRange(Reader input, Writer output, long offset, long length)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        checkNonNegative(offset, "offset");
        checkNonNegative(length, "length");
        if (length == 0) return 0;
        skipFully(input, offset);
        return copy(input, output, length);
    }

    /**
     * Fills the entire byte array or throws {@link EOFException} after
     * writing the available prefix.
     *
     * @param input the input stream
     * @param target the destination array
     * @throws EOFException if input ends before the array is filled
     * @throws IOException if reading fails
     */
    public static void readFully(InputStream input, byte[] target) throws IOException {
        readFully(input, target, 0, Objects.requireNonNull(target, "target").length);
    }

    /**
     * Fills exactly {@code length} bytes starting at {@code offset}, or
     * throws {@link EOFException} after writing the available prefix. Array
     * elements outside the requested range are unchanged. A zero length
     * performs no I/O. The range is validated before any input is consumed.
     *
     * @param input the input stream
     * @param target the destination array
     * @param offset the first destination index
     * @param length the number of bytes to read
     * @throws IndexOutOfBoundsException if the destination range is invalid
     * @throws EOFException if input ends before the range is filled
     * @throws IOException if reading fails
     */
    public static void readFully(InputStream input, byte[] target, int offset, int length)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.checkFromIndexSize(offset, length, target.length);
        if (readBlock(input, target, offset, length) != length) throw new EOFException();
    }

    /**
     * Fills the entire character array or throws {@link EOFException}
     * after writing the available prefix.
     *
     * @param input the reader
     * @param target the destination array
     * @throws EOFException if input ends before the array is filled
     * @throws IOException if reading fails
     */
    public static void readFully(Reader input, char[] target) throws IOException {
        readFully(input, target, 0, Objects.requireNonNull(target, "target").length);
    }

    /**
     * Fills exactly {@code length} UTF-16 code units starting at
     * {@code offset}, or throws {@link EOFException} after writing the
     * available prefix. Elements outside the range are unchanged. A zero
     * length performs no I/O. The range is validated before input is consumed.
     *
     * @param input the reader
     * @param target the destination array
     * @param offset the first destination index
     * @param length the number of characters to read
     * @throws IndexOutOfBoundsException if the destination range is invalid
     * @throws EOFException if input ends before the range is filled
     * @throws IOException if reading fails
     */
    public static void readFully(Reader input, char[] target, int offset, int length)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.checkFromIndexSize(offset, length, target.length);
        if (readBlock(input, target, offset, length) != length) throw new EOFException();
    }

    /**
     * Skips exactly {@code count} bytes, or throws {@link EOFException}
     * after consuming the available input. A zero count performs no I/O.
     *
     * @implSpec This method validates {@code count}, then invokes
     * {@link InputStream#skipNBytes(long)} for a positive count.
     * @param input the input stream
     * @param count the number of bytes to skip
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws EOFException if fewer than {@code count} bytes remain
     * @throws IOException if skipping or reading fails
     */
    public static void skipFully(InputStream input, long count) throws IOException {
        Objects.requireNonNull(input, "input");
        checkNonNegative(count, "count");
        if (count != 0) input.skipNBytes(count);
    }

    /**
     * Skips exactly {@code count} UTF-16 code units, or throws
     * {@link EOFException} after consuming available input. If a skip
     * makes no progress, a single character is read. An invalid negative
     * or excessive skip result causes {@link IOException}. A zero count
     * performs no I/O.
     *
     * @param input the reader
     * @param count the number of characters to skip
     * @throws IllegalArgumentException if {@code count} is negative
     * @throws EOFException if fewer than {@code count} characters remain
     * @throws IOException if skipping or reading fails, or skip returns an invalid count
     */
    public static void skipFully(Reader input, long count) throws IOException {
        Objects.requireNonNull(input, "input");
        checkNonNegative(count, "count");
        long remaining = count;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0 && skipped <= remaining) {
                remaining -= skipped;
            } else if (skipped == 0) {
                if (input.read() < 0) throw new EOFException();
                remaining--;
            } else {
                throw new IOException("Unable to skip exactly");
            }
        }
    }

    /**
     * Compares the remaining bytes of two streams. Equal streams are read
     * to end of stream. On a mismatch, input may have been read ahead
     * within the scratch buffers; no position after mismatch is promised.
     * Identical stream references return {@code true} without reading.
     * Uses two 8192-byte scratch buffers; differing short-read boundaries
     * do not affect the result. Neither stream is closed.
     *
     * @param first the first input stream
     * @param second the second input stream
     * @return whether the remaining byte sequences are equal
     * @throws IOException if reading fails
     */
    public static boolean contentEquals(InputStream first, InputStream second)
            throws IOException {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second) return true;
        byte[] left = new byte[BUFFER_SIZE];
        byte[] right = new byte[BUFFER_SIZE];
        while (true) {
            int a = readBlock(first, left, 0, left.length);
            int b = readBlock(second, right, 0, right.length);
            if (a != b || Arrays.mismatch(left, 0, a, right, 0, b) >= 0) return false;
            if (a < left.length) return true;
        }
    }

    /**
     * Compares the remaining UTF-16 code units of two readers. Equal
     * readers are consumed to end of stream. A mismatch may read ahead
     * within two fixed 8192-character scratch buffers. Identical reader
     * references return {@code true} without reading. Short-read
     * boundaries do not affect the result, and neither reader is closed.
     *
     * @param first the first reader
     * @param second the second reader
     * @return whether the remaining character sequences are equal
     * @throws IOException if reading fails
     */
    public static boolean contentEquals(Reader first, Reader second) throws IOException {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second) return true;
        char[] left = new char[BUFFER_SIZE];
        char[] right = new char[BUFFER_SIZE];
        while (true) {
            int a = readBlock(first, left, 0, left.length);
            int b = readBlock(second, right, 0, right.length);
            if (a != b || Arrays.mismatch(left, 0, a, right, 0, b) >= 0) return false;
            if (a < left.length) return true;
        }
    }

    /**
     * Reads all remaining bytes and decodes them with the explicit charset.
     * The conversion has the replacement behavior of
     * {@link String#String(byte[], Charset)} for malformed or unmappable
     * input. This materializes all remaining bytes and the resulting String;
     * use reader-based copying for content that should be streamed.
     *
     * @param input the input stream
     * @param charset the decoding charset
     * @return the decoded text
     * @throws IOException if reading fails
     * @throws OutOfMemoryError if the content cannot be materialized
     */
    public static String toString(InputStream input, Charset charset) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(charset, "charset");
        return new String(input.readAllBytes(), charset);
    }

    /**
     * Encodes text using the explicit charset and writes its bytes. Uses
     * {@link String#getBytes(Charset)} replacement behavior for malformed
     * or unmappable characters. The supplied text is materialized by its
     * {@code toString()} method before encoding. Does not flush or close
     * {@code output}.
     *
     * @param text the text to encode
     * @param output the output stream
     * @param charset the encoding charset
     * @throws IOException if writing fails
     * @throws OutOfMemoryError if the encoded text cannot be materialized
     */
    public static void write(CharSequence text, OutputStream output, Charset charset)
            throws IOException {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(charset, "charset");
        output.write(text.toString().getBytes(charset));
    }

    private static void checkNonNegative(long value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " < 0");
    }

    // Byte and char atoms stay separate to avoid boxing or callback allocation.
    private static int readChunk(InputStream input, byte[] buffer, int offset, int length)
            throws IOException {
        int count = input.read(buffer, offset, length);
        if (count != 0) return count;
        int single = input.read();
        if (single < 0) return -1;
        buffer[offset] = (byte) single;
        return 1;
    }

    private static int readChunk(Reader input, char[] buffer, int offset, int length)
            throws IOException {
        int count = input.read(buffer, offset, length);
        if (count != 0) return count;
        int single = input.read();
        if (single < 0) return -1;
        buffer[offset] = (char) single;
        return 1;
    }

    private static int readBlock(InputStream input, byte[] buffer, int offset, int length)
            throws IOException {
        int total = 0;
        while (total < length) {
            int count = readChunk(input, buffer, offset + total, length - total);
            if (count < 0) break;
            total += count;
        }
        return total;
    }

    private static int readBlock(Reader input, char[] buffer, int offset, int length)
            throws IOException {
        int total = 0;
        while (total < length) {
            int count = readChunk(input, buffer, offset + total, length - total);
            if (count < 0) break;
            total += count;
        }
        return total;
    }
}
