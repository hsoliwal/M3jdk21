/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Integrated IOUtils range, short-read, EOF, ownership, and charset contracts
 * @run main IOUtilsTest
 */

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class IOUtilsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        copiesAndRanges();
        fullReads();
        skips();
        equality();
        charsets();
        ownershipAndFailures();
        System.out.println("IOUtilsTest checks=" + checks);
    }

    private static void copiesAndRanges() throws Exception {
        Random random = new Random(210018L);
        for (int size : new int[] {0, 1, 7, 8191, 8192, 8193, 16385}) {
            byte[] data = new byte[size];
            random.nextBytes(data);
            char[] text = new char[size];
            for (int i = 0; i < size; i++) text[i] = (char) random.nextInt(65536);
            for (int chunk : new int[] {1, 7, 8192}) {
                for (long limit : new long[] {0, 1, 5, size, (long) size + 1, Long.MAX_VALUE}) {
                    ChunkedInput input = new ChunkedInput(data, chunk, true);
                    CountingOutput output = new CountingOutput();
                    long count = IOUtils.copy(input, output, limit);
                    int expected = (int) Math.min(size, limit);
                    check(count == expected, "bounded byte count");
                    check(Arrays.equals(output.toByteArray(), Arrays.copyOf(data, expected)), "bounded bytes");
                    check(input.position == expected, "no byte read beyond limit");
                    check(input.closed == 0 && output.closed == 0 && output.flushed == 0, "byte ownership");
                    ChunkedReader reader = new ChunkedReader(text, chunk, true);
                    CountingWriter writer = new CountingWriter();
                    count = IOUtils.copy(reader, writer, limit);
                    check(count == expected, "bounded char count");
                    check(writer.toString().equals(new String(text, 0, expected)), "bounded UTF-16");
                    check(reader.position == expected, "no char read beyond limit");
                    check(reader.closed == 0 && writer.closed == 0 && writer.flushed == 0, "char ownership");
                }
            }
        }
        byte[] data = {0, 1, 2, 3, 4, 5, 6};
        char[] text = {'a', '\uD83D', '\uDE00', '\0', '\uD800', 'z', '\u4E2D'};
        for (int offset = 0; offset <= data.length + 1; offset++) {
            for (int length = 0; length <= data.length + 1; length++) {
                ChunkedInput input = new ChunkedInput(data, 2, true);
                CountingOutput output = new CountingOutput();
                ChunkedReader reader = new ChunkedReader(text, 2, true);
                CountingWriter writer = new CountingWriter();
                int at = offset;
                int len = length;
                if (offset > data.length && length > 0) {
                    expect(EOFException.class, () -> IOUtils.copyRange(input, output, at, len));
                    expect(EOFException.class, () -> IOUtils.copyRange(reader, writer, at, len));
                    check(output.size() == 0 && writer.toString().isEmpty(), "offset EOF writes nothing");
                } else {
                    int expected = length == 0 ? 0 : Math.min(length, data.length - offset);
                    check(IOUtils.copyRange(input, output, offset, length) == expected, "byte range count");
                    check(IOUtils.copyRange(reader, writer, offset, length) == expected, "char range count");
                    byte[] wanted = length == 0 ? new byte[0] : Arrays.copyOfRange(data, offset, offset + expected);
                    String chars = length == 0 ? "" : new String(text, offset, expected);
                    check(Arrays.equals(wanted, output.toByteArray()), "byte range result");
                    check(writer.toString().equals(chars), "char range result");
                    check(input.position == (length == 0 ? 0 : offset + expected), "byte range position");
                    check(reader.position == (length == 0 ? 0 : offset + expected), "char range position");
                }
            }
        }
        ChunkedInput input = new ChunkedInput(data, 1, false);
        ChunkedReader reader = new ChunkedReader(text, 1, false);
        check(IOUtils.copyRange(input, new CountingOutput(), Long.MAX_VALUE, 0) == 0, "empty huge byte range");
        check(IOUtils.copyRange(reader, new CountingWriter(), Long.MAX_VALUE, 0) == 0, "empty huge char range");
        expect(IllegalArgumentException.class, () -> IOUtils.copy(input, new CountingOutput(), -1));
        expect(IllegalArgumentException.class, () -> IOUtils.copy(reader, new CountingWriter(), -1));
        expect(IllegalArgumentException.class, () -> IOUtils.copyRange(input, new CountingOutput(), -1, 0));
        expect(IllegalArgumentException.class, () -> IOUtils.copyRange(reader, new CountingWriter(), 0, -1));
        check(input.readCalls == 0 && reader.readCalls == 0, "invalid/empty ranges perform no I/O");
    }

    private static void fullReads() throws Exception {
        byte[] data = {1, 2, 3, 4, 5};
        char[] text = {'x', '\0', '\uD800', '\uDC00', 'y'};
        for (int length = 0; length <= 7; length++) {
            for (int offset = 0; offset <= 3; offset++) {
                byte[] target = new byte[offset + length + 3];
                char[] chars = new char[target.length];
                Arrays.fill(target, (byte) 99);
                Arrays.fill(chars, 'z');
                ChunkedInput input = new ChunkedInput(data, 1, true);
                ChunkedReader reader = new ChunkedReader(text, 2, true);
                int at = offset;
                int len = length;
                if (length <= data.length) {
                    IOUtils.readFully(input, target, offset, length);
                    IOUtils.readFully(reader, chars, offset, length);
                } else {
                    expect(EOFException.class, () -> IOUtils.readFully(input, target, at, len));
                    expect(EOFException.class, () -> IOUtils.readFully(reader, chars, at, len));
                }
                int copied = Math.min(length, data.length);
                for (int i = 0; i < target.length; i++) {
                    byte expected = i >= offset && i < offset + copied ? data[i - offset] : 99;
                    char expectedChar = i >= offset && i < offset + copied ? text[i - offset] : 'z';
                    check(target[i] == expected, "readFully byte prefix/sentinel");
                    check(chars[i] == expectedChar, "readFully char prefix/sentinel");
                }
                check(input.position == copied && reader.position == copied, "full-read input position");
                if (length == 0) check(input.readCalls == 0 && reader.readCalls == 0, "zero readFully no I/O");
            }
        }
        byte[] all = new byte[data.length];
        char[] allChars = new char[text.length];
        IOUtils.readFully(new ByteArrayInputStream(data), all);
        IOUtils.readFully(new StringReader(new String(text)), allChars);
        check(Arrays.equals(data, all) && Arrays.equals(text, allChars), "whole-array overloads");
        for (int[] range : new int[][] {{-1, 1}, {0, -1}, {4, 2}, {Integer.MAX_VALUE, 1}, {1, Integer.MAX_VALUE}}) {
            ChunkedInput input = new ChunkedInput(data, 1, false);
            ChunkedReader reader = new ChunkedReader(text, 1, false);
            expect(IndexOutOfBoundsException.class, () -> IOUtils.readFully(input, new byte[5], range[0], range[1]));
            expect(IndexOutOfBoundsException.class, () -> IOUtils.readFully(reader, new char[5], range[0], range[1]));
            check(input.readCalls == 0 && reader.readCalls == 0, "range checked before input access");
        }
        expect(NullPointerException.class, () -> IOUtils.readFully((InputStream) null, new byte[0]));
        expect(NullPointerException.class, () -> IOUtils.readFully((Reader) null, new char[0]));
        expect(NullPointerException.class, () -> IOUtils.readFully(InputStream.nullInputStream(), (byte[]) null));
        expect(NullPointerException.class, () -> IOUtils.readFully(Reader.nullReader(), (char[]) null));
    }

    private static void skips() throws Exception {
        for (int count = 0; count <= 7; count++) {
            ChunkedInput input = new ChunkedInput(new byte[] {1, 2, 3, 4, 5}, 2, false);
            ChunkedReader reader = new ChunkedReader("abcde".toCharArray(), 2, false);
            int n = count;
            if (count <= 5) {
                IOUtils.skipFully(input, count);
                IOUtils.skipFully(reader, count);
            } else {
                expect(EOFException.class, () -> IOUtils.skipFully(input, n));
                expect(EOFException.class, () -> IOUtils.skipFully(reader, n));
            }
            check(input.position == Math.min(5, count), "byte skip fallback");
            check(reader.position == Math.min(5, count), "char skip fallback");
        }
        expect(IllegalArgumentException.class, () -> IOUtils.skipFully(InputStream.nullInputStream(), -1));
        expect(IllegalArgumentException.class, () -> IOUtils.skipFully(Reader.nullReader(), -1));
        for (long bad : new long[] {-1, 4}) {
            InputStream input = new ByteArrayInputStream(new byte[5]) {
                @Override public long skip(long n) { return bad; }
            };
            Reader reader = new StringReader("abcde") {
                @Override public long skip(long n) { return bad; }
            };
            expect(IOException.class, () -> IOUtils.skipFully(input, 3));
            expect(IOException.class, () -> IOUtils.skipFully(reader, 3));
        }
        IOUtils.skipFully(new ByteArrayInputStream(new byte[0]) {
            @Override public void skipNBytes(long n) { throw new AssertionError("zero skip dispatch"); }
        }, 0);
        checks++;
    }

    private static void equality() throws Exception {
        Random random = new Random(4132);
        for (int size : new int[] {0, 1, 7, 8191, 8192, 8193, 16385}) {
            byte[] data = new byte[size];
            random.nextBytes(data);
            char[] text = new char[size];
            for (int i = 0; i < size; i++) text[i] = (char) random.nextInt(65536);
            check(IOUtils.contentEquals(new ChunkedInput(data, 1, true), new ChunkedInput(data, 7, false)), "different byte chunk boundaries");
            check(IOUtils.contentEquals(new ChunkedReader(text, 2, true), new ChunkedReader(text, 11, false)), "different char chunk boundaries");
            check(!IOUtils.contentEquals(new ByteArrayInputStream(data), new ByteArrayInputStream(Arrays.copyOf(data, size + 1))), "longer second bytes");
            check(!IOUtils.contentEquals(new ByteArrayInputStream(Arrays.copyOf(data, size + 1)), new ByteArrayInputStream(data)), "longer first bytes");
            check(!IOUtils.contentEquals(new StringReader(new String(text)), new StringReader(new String(text) + '\0')), "longer second chars");
            check(!IOUtils.contentEquals(new StringReader(new String(text) + '\0'), new StringReader(new String(text))), "longer first chars");
            if (size > 0) {
                for (int at : new int[] {0, size / 2, size - 1}) {
                    byte[] changed = data.clone();
                    changed[at] ^= 1;
                    char[] changedText = text.clone();
                    changedText[at] ^= 1;
                    check(!IOUtils.contentEquals(new ChunkedInput(data, 3, false), new ChunkedInput(changed, 5, true)), "byte mismatch");
                    check(!IOUtils.contentEquals(new ChunkedReader(text, 5, false), new ChunkedReader(changedText, 3, true)), "char mismatch");
                }
            }
        }
        ChunkedInput sameInput = new ChunkedInput(new byte[] {1}, 1, false);
        ChunkedReader sameReader = new ChunkedReader(new char[] {'x'}, 1, false);
        check(IOUtils.contentEquals(sameInput, sameInput) && sameInput.readCalls == 0, "identical stream is not consumed");
        check(IOUtils.contentEquals(sameReader, sameReader) && sameReader.readCalls == 0, "identical reader is not consumed");
        expect(NullPointerException.class, () -> IOUtils.contentEquals((InputStream) null, null));
        expect(NullPointerException.class, () -> IOUtils.contentEquals((Reader) null, null));
    }

    private static void charsets() throws Exception {
        for (Charset charset : new Charset[] {StandardCharsets.UTF_8, StandardCharsets.UTF_16BE, StandardCharsets.ISO_8859_1}) {
            String value = "A\0\u4E2D\uD83D\uDE00\uD800Z";
            byte[] bytes = value.getBytes(charset);
            ChunkedInput input = new ChunkedInput(bytes, 3, false);
            String result = IOUtils.toString(input, charset);
            check(result.equals(new String(bytes, charset)), "explicit decoding and replacement");
            CountingOutput output = new CountingOutput();
            IOUtils.write(new StringBuilder(value), output, charset);
            check(Arrays.equals(bytes, output.toByteArray()), "explicit encoding and replacement");
            check(input.closed == 0 && output.closed == 0 && output.flushed == 0, "charset resource ownership");
        }
        byte[] invalidUtf8 = {(byte) 0xC3, 0x28};
        check(IOUtils.toString(new ByteArrayInputStream(invalidUtf8), StandardCharsets.UTF_8)
                .equals(new String(invalidUtf8, StandardCharsets.UTF_8)), "malformed decoding parity");
        ChunkedInput input = new ChunkedInput(new byte[] {1}, 1, false);
        CountingOutput output = new CountingOutput();
        expect(NullPointerException.class, () -> IOUtils.toString(input, null));
        expect(NullPointerException.class, () -> IOUtils.write("a", output, null));
        check(input.readCalls == 0 && output.size() == 0, "charset validated before I/O");
    }

    private static void ownershipAndFailures() throws Exception {
        int[] transfers = new int[2];
        InputStream input = new ByteArrayInputStream(new byte[] {1, 2}) {
            @Override public long transferTo(OutputStream out) throws IOException {
                transfers[0]++;
                return super.transferTo(out);
            }
        };
        Reader reader = new StringReader("ab") {
            @Override public long transferTo(Writer out) throws IOException {
                transfers[1]++;
                return super.transferTo(out);
            }
        };
        CountingOutput output = new CountingOutput();
        CountingWriter writer = new CountingWriter();
        check(IOUtils.copy(input, output) == 2 && IOUtils.copy(reader, writer) == 2, "unbounded copies");
        check(Arrays.equals(transfers, new int[] {1, 1}), "canonical transferTo dispatch");
        check(output.closed == 0 && output.flushed == 0 && writer.closed == 0 && writer.flushed == 0, "no close or flush");
        IOException sentinel = new IOException("sentinel");
        InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw sentinel; }
        };
        Reader brokenReader = new Reader() {
            @Override public int read(char[] b, int off, int len) throws IOException { throw sentinel; }
            @Override public void close() { throw new AssertionError("must not close"); }
        };
        check(expect(IOException.class, () -> IOUtils.copy(broken, output, 10)) == sentinel, "byte exception identity");
        check(expect(IOException.class, () -> IOUtils.copy(brokenReader, writer, 10)) == sentinel, "char exception identity");
        OutputStream brokenOutput = new OutputStream() {
            @Override public void write(int b) throws IOException { throw sentinel; }
            @Override public void close() { throw new AssertionError("must not close"); }
        };
        check(expect(IOException.class, () -> IOUtils.copy(new ByteArrayInputStream(new byte[] {1}), brokenOutput, 1)) == sentinel, "write exception identity");
        expect(NullPointerException.class, () -> IOUtils.copy((InputStream) null, output));
        expect(NullPointerException.class, () -> IOUtils.copy((Reader) null, writer));
        expect(NullPointerException.class, () -> IOUtils.copy(input, (OutputStream) null, 0));
        expect(NullPointerException.class, () -> IOUtils.copy(reader, (Writer) null, 0));
    }

    private static final class ChunkedInput extends InputStream {
        final byte[] data;
        final int chunk;
        int position;
        int readCalls;
        int closed;
        boolean zeroNext;
        ChunkedInput(byte[] data, int chunk, boolean zeroNext) {
            this.data = data;
            this.chunk = chunk;
            this.zeroNext = zeroNext;
        }
        @Override public int read() {
            readCalls++;
            return position == data.length ? -1 : data[position++] & 255;
        }
        @Override public int read(byte[] b, int off, int len) {
            readCalls++;
            if (len == 0) return 0;
            if (zeroNext) { zeroNext = false; return 0; }
            if (position == data.length) return -1;
            int n = Math.min(Math.min(chunk, len), data.length - position);
            System.arraycopy(data, position, b, off, n);
            position += n;
            return n;
        }
        @Override public long skip(long count) { return 0; }
        @Override public void close() { closed++; }
    }

    private static final class ChunkedReader extends Reader {
        final char[] data;
        final int chunk;
        int position;
        int readCalls;
        int closed;
        boolean zeroNext;
        ChunkedReader(char[] data, int chunk, boolean zeroNext) {
            this.data = data;
            this.chunk = chunk;
            this.zeroNext = zeroNext;
        }
        @Override public int read() {
            readCalls++;
            return position == data.length ? -1 : data[position++];
        }
        @Override public int read(char[] b, int off, int len) {
            readCalls++;
            if (len == 0) return 0;
            if (zeroNext) { zeroNext = false; return 0; }
            if (position == data.length) return -1;
            int n = Math.min(Math.min(chunk, len), data.length - position);
            System.arraycopy(data, position, b, off, n);
            position += n;
            return n;
        }
        @Override public long skip(long count) { return 0; }
        @Override public void close() { closed++; }
    }

    private static final class CountingOutput extends ByteArrayOutputStream {
        int closed;
        int flushed;
        @Override public void close() { closed++; }
        @Override public void flush() { flushed++; }
    }
    private static final class CountingWriter extends StringWriter {
        int closed;
        int flushed;
        @Override public void close() { closed++; }
        @Override public void flush() { flushed++; }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return type.cast(actual);
            throw new AssertionError("expected " + type + ", got " + actual, actual);
        }
        throw new AssertionError("expected " + type);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}
