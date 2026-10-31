/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package jdk.internal.m3.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * I/O operations over M3's borrowed array, offset, projection and state-mask planes.
 *
 * <p>This internal utility does not change {@code InputStream}/{@code OutputStream}
 * contracts, replace mutable Java arrays, or own the underlying byte pages. The
 * caller retains the arrays and must not mutate the metadata or page contents while
 * an operation is executing. No row/entry object is created by projected output.
 */
public final class M3IO {
    private M3IO() { }

    /**
     * Copies to an output without allocating a buffer or closing either stream.
     * The scratch array is borrowed for the duration of the call. A stream that
     * returns zero from a nonempty bulk read is advanced with a scalar read,
     * avoiding a CPU-spinning zero-progress loop.
     *
     * @return number of bytes written
     */
    public static long transfer(InputStream source, OutputStream target, byte[] scratch)
            throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(scratch, "scratch");
        if (scratch.length == 0) {
            throw new IllegalArgumentException("scratch buffer must not be empty");
        }
        long total = 0;
        while (true) {
            int count = source.read(scratch, 0, scratch.length);
            if (count < 0) {
                return total;
            }
            if (count == 0) {
                int value = source.read();
                if (value < 0) {
                    return total;
                }
                target.write(value);
                total = Math.addExact(total, 1L);
            } else {
                target.write(scratch, 0, count);
                total = Math.addExact(total, count);
            }
        }
    }

    /**
     * Reads the entire requested range or throws {@link EOFException}. A zero
     * bulk-read result is handled with a scalar read. Neither stream nor array
     * is retained; a zero-length request performs no reads.
     */
    public static void readExactly(InputStream source, byte[] destination, int offset, int length)
            throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.checkFromIndexSize(offset, length, destination.length);
        int done = 0;
        while (done < length) {
            int count = source.read(destination, offset + done, length - done);
            if (count < 0) {
                throw new EOFException("expected " + length + " bytes, received " + done);
            }
            if (count == 0) {
                int value = source.read();
                if (value < 0) {
                    throw new EOFException("expected " + length + " bytes, received " + done);
                }
                destination[offset + done++] = (byte) value;
            } else {
                done += count;
            }
        }
    }

    /**
     * Writes active physical page ranges in physical index order. {@code offsets}
     * and {@code lengths} are parallel to {@code pages}; {@code activeWords}
     * has one bit for each physical page (low bit first). Inactive pages may be
     * null. No per-page entry object is created, and each page remains borrowed.
     *
     * <p>All selected ranges are checked before any output is written. Stream
     * I/O failures can still leave partial output, as with ordinary streams.
     */
    public static long writeMasked(OutputStream target, byte[][] pages, int[] offsets,
            int[] lengths, long[] activeWords) throws IOException {
        Objects.requireNonNull(pages, "pages");
        return write(target, pages, offsets, lengths, null, 0, pages.length, activeWords);
    }

    /**
     * Writes a logical range by following logical-to-physical indices, checking
     * the physical state bit for each selected slot. Repeated physical indices
     * intentionally produce repeated bytes. Inactive slots emit nothing.
     */
    public static long writeProjected(OutputStream target, byte[][] pages, int[] offsets,
            int[] lengths, int[] logicalToPhysical, int from, int to, long[] activeWords)
            throws IOException {
        Objects.requireNonNull(logicalToPhysical, "logicalToPhysical");
        return write(target, pages, offsets, lengths, logicalToPhysical, from, to, activeWords);
    }

    private static long write(OutputStream target, byte[][] pages, int[] offsets,
            int[] lengths, int[] logicalToPhysical, int from, int to, long[] activeWords)
            throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(pages, "pages");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        Objects.requireNonNull(activeWords, "activeWords");
        if (offsets.length != pages.length || lengths.length != pages.length) {
            throw new IllegalArgumentException("offset and length planes must match page count");
        }
        if (activeWords.length < (((long) pages.length + 63L) >>> 6)) {
            throw new IllegalArgumentException("incomplete page state-mask plane");
        }
        int size = logicalToPhysical == null ? pages.length : logicalToPhysical.length;
        Objects.checkFromToIndex(from, to, size);

        // Preflight all selected pages, preserving the output on bad metadata.
        long total = 0;
        for (int logical = from; logical < to; logical++) {
            int physical = logicalToPhysical == null ? logical : logicalToPhysical[logical];
            Objects.checkIndex(physical, pages.length);
            if (active(activeWords, physical)) {
                byte[] page = Objects.requireNonNull(pages[physical], "active page");
                Objects.checkFromIndexSize(offsets[physical], lengths[physical], page.length);
                total = Math.addExact(total, lengths[physical]);
            }
        }
        for (int logical = from; logical < to; logical++) {
            int physical = logicalToPhysical == null ? logical : logicalToPhysical[logical];
            if (active(activeWords, physical) && lengths[physical] != 0) {
                target.write(pages[physical], offsets[physical], lengths[physical]);
            }
        }
        return total;
    }

    private static boolean active(long[] words, int physical) {
        return (words[physical >>> 6] & (1L << (physical & 63))) != 0;
    }
    /**
     * Borrows a monitor (including a caller-allocated child) for byte accounting.
     * Positive reads and skips count; bytes reread after reset count again.
     * Close never completes the monitor. An exception from a monitor callback
     * may follow successful I/O; the delegate operation is not rolled back.
     */
    public static InputStream progress(InputStream source,
            java.util.concurrent.ProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(monitor, "monitor");
        return new InputStream() {
            private boolean closed;
            private void open() throws IOException {
                if (closed) throw new IOException("progress stream is closed");
            }
            @Override public int read() throws IOException {
                open(); monitor.checkCancelled();
                int result = source.read();
                if (result >= 0) monitor.worked(1);
                return result;
            }
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                Objects.checkFromIndexSize(offset, length, bytes.length);
                open();
                if (length == 0) return 0;
                monitor.checkCancelled();
                int count = source.read(bytes, offset, length);
                if (count > 0) monitor.worked(count);
                return count;
            }
            @Override public long skip(long count) throws IOException {
                open();
                if (count <= 0) return 0;
                monitor.checkCancelled();
                long skipped = source.skip(count);
                if (skipped > 0) monitor.worked(skipped);
                return skipped;
            }
            @Override public int available() throws IOException { open(); return source.available(); }
            @Override public boolean markSupported() { return source.markSupported(); }
            @Override public synchronized void mark(int limit) { if (!closed) source.mark(limit); }
            @Override public synchronized void reset() throws IOException { open(); source.reset(); }
            @Override public void close() throws IOException {
                if (!closed) { closed = true; source.close(); }
            }
        };
    }

    /**
     * Counts completed writes once, retaining bulk delegate writes. An IOException
     * may follow an unobservable partial write; no count is invented for that case.
     * Closing the wrapper delegates once and never completes the borrowed monitor.
     */
    public static OutputStream progress(OutputStream target,
            java.util.concurrent.ProgressMonitor monitor) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(monitor, "monitor");
        return new OutputStream() {
            private boolean closed;
            private void open() throws IOException {
                if (closed) throw new IOException("progress stream is closed");
            }
            @Override public void write(int value) throws IOException {
                open(); monitor.checkCancelled(); target.write(value); monitor.worked(1);
            }
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                Objects.checkFromIndexSize(offset, length, bytes.length);
                open();
                if (length == 0) return;
                monitor.checkCancelled();
                target.write(bytes, offset, length);
                monitor.worked(length);
            }
            @Override public void flush() throws IOException { open(); target.flush(); }
            @Override public void close() throws IOException {
                if (!closed) { closed = true; target.close(); }
            }
        };
    }

}
