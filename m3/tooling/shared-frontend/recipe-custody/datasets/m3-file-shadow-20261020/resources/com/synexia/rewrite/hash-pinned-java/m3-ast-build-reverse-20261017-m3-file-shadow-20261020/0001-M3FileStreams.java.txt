// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Bounded, provider-owned stream observation without eager read-ahead.
 * A shadow advances only when the primary source is read, and never opens
 * another file descriptor. No global file tree, content store or parser.
 */
public final class M3FileStreams {
    private M3FileStreams() { }

    /** Recursive progress can be composed using default methods. */
    @FunctionalInterface
    public interface SubProgressMonitor {
        SubProgressMonitor NONE = units -> { };
        void worked(long units);
        default boolean isCanceled() { return false; }
        default void done() { }

        /** Deterministically transfer child work into a bounded share of parent ticks. */
        default SubProgressMonitor subMonitor(int parentTicks, int childUnits) {
            if (parentTicks < 0 || parentTicks > 1_000_000
                    || childUnits < 1 || childUnits > 1_000_000) {
                throw new IllegalArgumentException("invalid subprogress budget");
            }
            SubProgressMonitor parent = this;
            return new SubProgressMonitor() {
                private int completed;
                @Override public synchronized void worked(long units) {
                    if (units < 0 || units > childUnits - completed) {
                        throw new IllegalArgumentException("subprogress overrun");
                    }
                    int before = completed;
                    completed += (int) units;
                    long previous = (long) before * parentTicks / childUnits;
                    long current = (long) completed * parentTicks / childUnits;
                    if (current != previous) parent.worked(current - previous);
                }
                @Override public boolean isCanceled() { return parent.isCanceled(); }
            };
        }
    }

    /** Open the original M3File provider only once. Caller closes the result. */
    public static ShadowInput input(M3File file, int windowBytes,
            SubProgressMonitor primary, SubProgressMonitor observer) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(primary, "primary progress");
        Objects.requireNonNull(observer, "observer progress");
        if (windowBytes < 1 || windowBytes > (1 << 20)) {
            throw new IllegalArgumentException("shadow window must be 1..1048576 bytes");
        }
        return input(file.openInputStream(), windowBytes, primary, observer);
    }

    /** Caller transfers responsibility for closing the source to the returned owner. */
    public static ShadowInput input(InputStream source, int windowBytes,
            SubProgressMonitor primary, SubProgressMonitor observer) {
        return new ShadowInput(source, windowBytes, primary, observer);
    }

    /** Mirror each successfully emitted output chunk, without retaining any bytes. */
    public static OutputStream output(OutputStream primary, OutputStream observer,
            SubProgressMonitor primaryProgress, SubProgressMonitor observerProgress) {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(observer, "observer");
        if (primary == observer) throw new IllegalArgumentException("distinct output streams required");
        SubProgressMonitor first = Objects.requireNonNull(primaryProgress, "primary progress");
        SubProgressMonitor second = Objects.requireNonNull(observerProgress, "observer progress");
        return new OutputStream() {
            private boolean closed;
            private void ready() throws IOException {
                if (closed) throw new IOException("shadow output closed");
                if (first.isCanceled() || second.isCanceled()) {
                    throw new InterruptedIOException("shadow output canceled");
                }
            }
            @Override public void write(int value) throws IOException {
                ready();
                primary.write(value);
                first.worked(1);
                observer.write(value);
                second.worked(1);
            }
            @Override public void write(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                ready();
                if (len == 0) return;
                primary.write(b, off, len);
                first.worked(len);
                observer.write(b, off, len);
                second.worked(len);
            }
            @Override public void flush() throws IOException {
                ready();
                primary.flush();
                observer.flush();
            }
            @Override public void close() throws IOException {
                if (closed) return;
                closed = true;
                IOException failure = null;
                try { primary.close(); }
                catch (IOException error) { failure = error; }
                try { observer.close(); }
                catch (IOException error) {
                    if (failure == null) failure = error;
                    else failure.addSuppressed(error);
                }
                first.done();
                second.done();
                if (failure != null) throw failure;
            }
        };
    }

    /**
     * One original source, one primary reader, one lagging observer.
     * Neither side retains payload beyond the fixed-size byte ring. If the
     * observer falls behind, primary reads refuse BEFORE reading more bytes.
     * A shadow read may block until the primary advances or closes; consuming
     * the shadow before the primary on the same thread is unsupported.
     */
    public static final class ShadowInput implements AutoCloseable {
        private static final int MAX_WINDOW = 1 << 20;
        private final InputStream source;
        private final byte[] ring;
        private final SubProgressMonitor primaryProgress, observerProgress;
        private final Object lock = new Object();
        private long produced, consumed;
        private boolean primaryClosed, observerClosed, eof;
        private final InputStream primaryStream = new Primary();
        private final InputStream observerStream = new Observer();

        private ShadowInput(InputStream source, int windowBytes,
                SubProgressMonitor primary, SubProgressMonitor observer) {
            this.source = Objects.requireNonNull(source, "source");
            if (windowBytes < 1 || windowBytes > MAX_WINDOW) {
                throw new IllegalArgumentException("shadow window must be 1..1048576 bytes");
            }
            ring = new byte[windowBytes];
            primaryProgress = Objects.requireNonNull(primary, "primary progress");
            observerProgress = Objects.requireNonNull(observer, "observer progress");
        }

        public InputStream primary() { return primaryStream; }
        public InputStream shadow() { return observerStream; }
        public int capacity() { return ring.length; }

        private void checkPrimary() throws IOException {
            if (primaryClosed) throw new IOException("primary stream closed");
            if (primaryProgress.isCanceled()) {
                throw new InterruptedIOException("primary stream canceled");
            }
            if (!observerClosed && produced - consumed == ring.length) {
                throw new IOException("shadow window exhausted; observer must consume first");
            }
        }

        private void publish(byte[] data, int off, int size) {
            if (observerClosed || size == 0) return;
            int firstOffset = (int) (produced % ring.length);
            int first = Math.min(size, ring.length - firstOffset);
            System.arraycopy(data, off, ring, firstOffset, first);
            if (size > first) System.arraycopy(data, off + first, ring, 0, size - first);
            produced = Math.addExact(produced, size);
            lock.notifyAll();
        }

        private final class Primary extends InputStream {
            @Override public int read() throws IOException {
                int result;
                synchronized (lock) {
                    if (primaryClosed) throw new IOException("primary stream closed");
                    if (eof) return -1;
                    checkPrimary();
                    result = source.read();
                    if (result < 0) { eof = true; lock.notifyAll(); return -1; }
                    if (!observerClosed) {
                        ring[(int) (produced % ring.length)] = (byte) result;
                        produced = Math.addExact(produced, 1L);
                        lock.notifyAll();
                    }
                }
                primaryProgress.worked(1L);
                return result;
            }
            @Override public int read(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (len == 0) return 0;
                int result;
                synchronized (lock) {
                    if (primaryClosed) throw new IOException("primary stream closed");
                    if (eof) return -1;
                    checkPrimary();
                    int writable = observerClosed ? len : (int) Math.min(len, ring.length - (produced - consumed));
                    result = source.read(b, off, writable);
                    if (result < 0) { eof = true; lock.notifyAll(); return -1; }
                    publish(b, off, result);
                }
                if (result > 0) primaryProgress.worked(result);
                return result;
            }
            @Override public long skip(long count) throws IOException {
                if (count <= 0) return 0;
                byte[] scratch = new byte[(int) Math.min(8192, count)];
                long passed = 0;
                while (passed < count) {
                    int n = read(scratch, 0, (int) Math.min(scratch.length, count - passed));
                    if (n <= 0) break;
                    passed += n;
                }
                return passed;
            }
            @Override public int available() throws IOException {
                synchronized (lock) {
                    if (primaryClosed) throw new IOException("primary stream closed");
                    return source.available();
                }
            }
            @Override public void close() throws IOException {
                synchronized (lock) {
                    if (primaryClosed) return;
                    primaryClosed = true;
                    eof = true;
                    lock.notifyAll();
                }
                try { source.close(); }
                finally { primaryProgress.done(); }
            }
            @Override public boolean markSupported() { return false; }
            @Override public synchronized void reset() throws IOException {
                throw new IOException("shadow stream cannot replay source bytes");
            }
        }

        private final class Observer extends InputStream {
            private void await() throws IOException {
                synchronized (lock) {
                    if (observerClosed) throw new IOException("shadow stream closed");
                    while (produced == consumed && !eof) {
                        if (observerProgress.isCanceled()) {
                            throw new InterruptedIOException("shadow observer canceled");
                        }
                        try { lock.wait(); }
                        catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedIOException("shadow observer interrupted");
                        }
                    }
                    if (observerClosed) throw new IOException("shadow stream closed");
                }
            }
            @Override public int read() throws IOException {
                await();
                int result;
                synchronized (lock) {
                    if (produced == consumed) return -1;
                    result = ring[(int) (consumed++ % ring.length)] & 255;
                    lock.notifyAll();
                }
                observerProgress.worked(1);
                return result;
            }
            @Override public int read(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (len == 0) return 0;
                await();
                int n;
                synchronized (lock) {
                    if (produced == consumed) return -1;
                    n = (int) Math.min(len, produced - consumed);
                    int start = (int) (consumed % ring.length);
                    int first = Math.min(n, ring.length - start);
                    System.arraycopy(ring, start, b, off, first);
                    if (n > first) System.arraycopy(ring, 0, b, off + first, n - first);
                    consumed += n;
                    lock.notifyAll();
                }
                observerProgress.worked(n);
                return n;
            }
            @Override public int available() throws IOException {
                synchronized (lock) {
                    if (observerClosed) throw new IOException("shadow stream closed");
                    return (int) (produced - consumed);
                }
            }
            @Override public void close() {
                synchronized (lock) {
                    if (observerClosed) return;
                    observerClosed = true;
                    consumed = produced;
                    lock.notifyAll();
                }
                observerProgress.done();
            }
            @Override public boolean markSupported() { return false; }
            @Override public synchronized void reset() throws IOException {
                throw new IOException("shadow observer cannot rewind original source");
            }
        }

        @Override public void close() throws IOException {
            IOException failure = null;
            try { primaryStream.close(); }
            catch (IOException problem) { failure = problem; }
            observerStream.close();
            if (failure != null) throw failure;
        }
    }
}
