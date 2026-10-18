/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * Borrowing helpers for byte and character input/output.
 *
 * <p>Every supplied stream, reader and writer remains owned by its caller. These
 * helpers never close or flush a supplied object. Exceptions from the underlying
 * I/O operations propagate to the caller. Copying and exact reads are not
 * transactional: an exception can follow a successfully transferred prefix.
 *
 * <p>Whole-input copies delegate to {@link InputStream#transferTo(OutputStream)}
 * or {@link Reader#transferTo(Writer)}. Bounded copies never request more input
 * than remains in the supplied limit. Their zero-length bulk-read fallback uses
 * a single-element read to make progress. Character counts refer to Java
 * {@code char} units, not Unicode code points. Text decoding requires an explicit
 * non-null {@link Charset}; reader input is already decoded.
 *
 * <p>Blocking, interruption and concurrent access retain the underlying object's
 * contract. This class adds no background work or cancellation callback.
 */
public final class IOUtils {
    private static final int BUFFER_SIZE = 8192;

    private IOUtils() { }

    /**
     * Copies all remaining bytes using the input's transfer implementation.
     * @param input the non-null borrowed input
     * @param output the non-null borrowed output
     * @return the number of bytes transferred
     * @throws IOException if the underlying transfer fails
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        return input.transferTo(output);
    }

    /**
     * Copies all remaining characters using the reader's transfer implementation.
     * @param input the non-null borrowed reader
     * @param output the non-null borrowed writer
     * @return the number of characters transferred
     * @throws IOException if the underlying transfer fails
     */
    public static long copy(Reader input, Writer output) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        return input.transferTo(output);
    }

    /**
     * Copies at most {@code limit} bytes, stopping normally at end of input.
     * @param input the non-null borrowed input
     * @param output the non-null borrowed output
     * @param limit the nonnegative maximum number of bytes to copy
     * @return the number of bytes transferred, possibly less than the limit
     * @throws IllegalArgumentException if the limit is negative
     * @throws IOException if reading or writing fails
     */
    public static long copy(InputStream input, OutputStream output, long limit) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        nonnegative(limit, "limit");
        if (limit == 0) return 0;
        byte[] buffer = new byte[(int) Math.min(BUFFER_SIZE, limit)];
        long copied = 0;
        while (copied < limit) {
            int requested = (int) Math.min(buffer.length, limit - copied);
            int count = readSome(input, buffer, 0, requested);
            if (count < 0) break;
            output.write(buffer, 0, count);
            copied += count;
        }
        return copied;
    }

    /**
     * Copies at most {@code limit} characters, stopping normally at end of input.
     * @param input the non-null borrowed reader
     * @param output the non-null borrowed writer
     * @param limit the nonnegative maximum number of characters to copy
     * @return the number of characters transferred, possibly less than the limit
     * @throws IllegalArgumentException if the limit is negative
     * @throws IOException if reading or writing fails
     */
    public static long copy(Reader input, Writer output, long limit) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        nonnegative(limit, "limit");
        if (limit == 0) return 0;
        char[] buffer = new char[(int) Math.min(BUFFER_SIZE, limit)];
        long copied = 0;
        while (copied < limit) {
            int requested = (int) Math.min(buffer.length, limit - copied);
            int count = readSome(input, buffer, 0, requested);
            if (count < 0) break;
            output.write(buffer, 0, count);
            copied += count;
        }
        return copied;
    }

    /**
     * Fills the byte buffer or throws after preserving any prefix read before EOF.
     * @param input the non-null borrowed input
     * @param buffer the non-null destination
     * @throws EOFException if input ends before the buffer is filled
     * @throws IOException if the underlying read fails
     */
    public static void readFully(InputStream input, byte[] buffer) throws IOException {
        Objects.requireNonNull(buffer, "buffer");
        readFully(input, buffer, 0, buffer.length);
    }

    /**
     * Fills exactly the specified byte range, preserving its outside elements.
     * Argument validation precedes input access. If EOF is reached early, the
     * successfully read prefix remains in the requested destination range.
     * @param input the non-null borrowed input
     * @param buffer the non-null destination
     * @param offset the first destination index
     * @param length the number of bytes required
     * @throws IndexOutOfBoundsException if the destination range is invalid
     * @throws EOFException if input ends before the range is filled
     * @throws IOException if the underlying read fails
     */
    public static void readFully(InputStream input, byte[] buffer, int offset, int length)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(buffer, "buffer");
        Objects.checkFromIndexSize(offset, length, buffer.length);
        int copied = 0;
        while (copied < length) {
            int count = readSome(input, buffer, offset + copied, length - copied);
            if (count < 0) throw new EOFException("Required " + length + " bytes; read " + copied);
            copied += count;
        }
    }

    /**
     * Fills the character buffer or throws after preserving the prefix read before EOF.
     * @param input the non-null borrowed reader
     * @param buffer the non-null destination
     * @throws EOFException if input ends before the buffer is filled
     * @throws IOException if the underlying read fails
     */
    public static void readFully(Reader input, char[] buffer) throws IOException {
        Objects.requireNonNull(buffer, "buffer");
        readFully(input, buffer, 0, buffer.length);
    }

    /**
     * Fills exactly the specified character range, preserving outside elements.
     * Argument validation precedes input access; an early EOF leaves the
     * successfully read prefix in the requested destination range.
     * @param input the non-null borrowed reader
     * @param buffer the non-null destination
     * @param offset the first destination index
     * @param length the number of characters required
     * @throws IndexOutOfBoundsException if the destination range is invalid
     * @throws EOFException if input ends before the range is filled
     * @throws IOException if the underlying read fails
     */
    public static void readFully(Reader input, char[] buffer, int offset, int length)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(buffer, "buffer");
        Objects.checkFromIndexSize(offset, length, buffer.length);
        int copied = 0;
        while (copied < length) {
            int count = readSome(input, buffer, offset + copied, length - copied);
            if (count < 0) throw new EOFException("Required " + length + " characters; read " + copied);
            copied += count;
        }
    }

    /**
     * Skips exactly the requested number of bytes through {@code skipNBytes}.
     * @param input the non-null borrowed input
     * @param count the nonnegative number of bytes to skip
     * @throws IllegalArgumentException if the count is negative
     * @throws EOFException if input ends before the count is reached
     * @throws IOException if the underlying skip or read fails
     */
    public static void skipFully(InputStream input, long count) throws IOException {
        Objects.requireNonNull(input, "input");
        nonnegative(count, "count");
        input.skipNBytes(count);
    }

    /**
     * Skips exactly the requested number of characters, reading one character
     * whenever the reader reports no skip progress.
     * @param input the non-null borrowed reader
     * @param count the nonnegative number of characters to skip
     * @throws IllegalArgumentException if the count is negative
     * @throws EOFException if input ends before the count is reached
     * @throws IOException if skipping or reading fails, or skip reports an invalid count
     */
    public static void skipFully(Reader input, long count) throws IOException {
        Objects.requireNonNull(input, "input");
        nonnegative(count, "count");
        long remaining = count;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped < 0 || skipped > remaining) {
                throw new IOException("Reader.skip returned an invalid count: " + skipped);
            }
            if (skipped == 0) {
                if (input.read() < 0) throw new EOFException("Required " + count + " characters to skip");
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    /**
     * Compares remaining bytes using bounded buffers and {@link Arrays#mismatch(byte[], int, int, byte[], int, int)}.
     * Different bulk-read fragment sizes do not affect the result. Identical
     * input references compare equal without reading. Inputs may be read ahead
     * into the private comparison buffers and are never closed or reset.
     * @param left the first non-null borrowed input
     * @param right the second non-null borrowed input
     * @return whether the remaining byte sequences are equal
     * @throws IOException if either input fails
     */
    public static boolean contentEquals(InputStream left, InputStream right) throws IOException {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left == right) return true;
        byte[] a = new byte[BUFFER_SIZE];
        byte[] b = new byte[BUFFER_SIZE];
        int first = 0, firstEnd = 0, second = 0, secondEnd = 0;
        for (;;) {
            if (first == firstEnd) {
                firstEnd = readSome(left, a, 0, a.length);
                first = 0;
            }
            if (second == secondEnd) {
                secondEnd = readSome(right, b, 0, b.length);
                second = 0;
            }
            if (firstEnd < 0 || secondEnd < 0) return firstEnd == secondEnd;
            int count = Math.min(firstEnd - first, secondEnd - second);
            if (Arrays.mismatch(a, first, first + count, b, second, second + count) >= 0) return false;
            first += count;
            second += count;
        }
    }

    /**
     * Compares remaining character units using bounded buffers and array mismatch.
     * Identical reader references compare equal without reading. Readers may be
     * read ahead into private buffers and are never closed or reset.
     * @param left the first non-null borrowed reader
     * @param right the second non-null borrowed reader
     * @return whether the remaining character sequences are equal
     * @throws IOException if either reader fails
     */
    public static boolean contentEquals(Reader left, Reader right) throws IOException {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left == right) return true;
        char[] a = new char[BUFFER_SIZE];
        char[] b = new char[BUFFER_SIZE];
        int first = 0, firstEnd = 0, second = 0, secondEnd = 0;
        for (;;) {
            if (first == firstEnd) {
                firstEnd = readSome(left, a, 0, a.length);
                first = 0;
            }
            if (second == secondEnd) {
                secondEnd = readSome(right, b, 0, b.length);
                second = 0;
            }
            if (firstEnd < 0 || secondEnd < 0) return firstEnd == secondEnd;
            int count = Math.min(firstEnd - first, secondEnd - second);
            if (Arrays.mismatch(a, first, first + count, b, second, second + count) >= 0) return false;
            first += count;
            second += count;
        }
    }

    /**
     * Reads all remaining bytes through the input's standard implementation.
     * @param input the non-null borrowed input
     * @return the remaining bytes
     * @throws IOException if reading fails
     */
    public static byte[] readAllBytes(InputStream input) throws IOException {
        return Objects.requireNonNull(input, "input").readAllBytes();
    }

    /**
     * Decodes all remaining bytes with an explicitly supplied charset.
     * Decoding uses the replacement behavior of {@link String#String(byte[], Charset)}.
     * @param input the non-null borrowed input
     * @param charset the non-null decoding charset
     * @return the decoded text
     * @throws IOException if reading fails
     */
    public static String toString(InputStream input, Charset charset) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(charset, "charset");
        return new String(input.readAllBytes(), charset);
    }

    /**
     * Collects already decoded characters through the reader's transfer implementation.
     * @param input the non-null borrowed reader
     * @return the remaining text
     * @throws IOException if reading fails
     */
    public static String toString(Reader input) throws IOException {
        Objects.requireNonNull(input, "input");
        StringWriter output = new StringWriter();
        input.transferTo(output);
        return output.toString();
    }

    private static void nonnegative(long count, String name) {
        if (count < 0) throw new IllegalArgumentException(name + " must be nonnegative");
    }

    /** The caller supplies a valid nonempty destination range. */
    private static int readSome(InputStream input, byte[] buffer, int offset, int length)
            throws IOException {
        int count = input.read(buffer, offset, length);
        if (count != 0) return count;
        int value = input.read();
        if (value < 0) return -1;
        buffer[offset] = (byte) value;
        return 1;
    }

    /** The caller supplies a valid nonempty destination range. */
    private static int readSome(Reader input, char[] buffer, int offset, int length)
            throws IOException {
        int count = input.read(buffer, offset, length);
        if (count != 0) return count;
        int value = input.read();
        if (value < 0) return -1;
        buffer[offset] = (char) value;
        return 1;
    }
}
