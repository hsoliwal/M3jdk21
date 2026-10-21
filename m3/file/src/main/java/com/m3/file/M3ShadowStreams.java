// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * M3JDK21-owned, demand-driven shadow I/O for layered parsing.
 *
 * <p>A SharedInput reads its original source at most once, on demand by its
 * primary or any shadow cursor. Parent-owned 8 KiB dynamic byte pages retain
 * only bytes actually pulled, up to an explicit limit. Shadow cursors are
 * physical offsets into those same pages, not cloned source streams, file
 * handles, parser trees or a filesystem graph. The caller must close the
 * primary; closing an individual shadow does not close its owner.</p>
 */
public final class M3ShadowStreams {
    private static final int PAGE_SHIFT = 13;
    private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    private static final int MAX_ZERO_READS = 16;

    private M3ShadowStreams() { }

    public static SharedInput input(InputStream source, int maxRetainedBytes,
                                    M3StreamProgress sourceProgress) {
        return new SharedInput(source, maxRetainedBytes, sourceProgress);
    }

    /** Unbuffered alternative when no replay/shadow consumer is needed. */
    public static InputStream monitoredInput(InputStream input, M3StreamProgress progress) {
        Objects.requireNonNull(input, "input");
        M3StreamProgress monitor = M3StreamProgress.require(progress);
        return new FilterInputStream(input) {
            @Override public int read() throws IOException {
                monitor.checkCancelled();
                int value = super.read();
                if (value >= 0) monitor.worked(1);
                return value;
            }
            @Override public int read(byte[] bytes, int off, int length) throws IOException {
                Objects.checkFromIndexSize(off, length, Objects.requireNonNull(bytes, "bytes").length);
                if (length == 0) return 0;
                monitor.checkCancelled();
                // Direct delegate call: FilterInputStream's implementation invokes
                // read() repeatedly and would otherwise double-count bytes.
                int read = in.read(bytes, off, length);
                if (read > 0) monitor.worked(read);
                return read;
            }
            @Override public long skip(long length) throws IOException {
                if (length <= 0) return 0;
                monitor.checkCancelled();
                long skipped = in.skip(length);
                if (skipped > 0) monitor.worked(skipped);
                return skipped;
            }
        };
    }

    /**
     * Write through to an original output and optionally mirror into a shadow.
     * An unsuccessful shadow write may leave the original partially written;
     * it poisons further writes, rather than claiming atomic output.
     */
    public static OutputStream output(OutputStream primary, OutputStream shadow,
                                      M3StreamProgress progress) {
        Objects.requireNonNull(primary, "primary");
        if (primary == shadow) throw new IllegalArgumentException("output shadows must be distinct");
        M3StreamProgress monitor = M3StreamProgress.require(progress);
        return new OutputStream() {
            private boolean closed;
            private IOException failed;
            private void requireWritable() throws IOException {
                if (closed) throw new IOException("M3 output closed");
                if (failed != null) throw new IOException("M3 output shadow failed", failed);
                monitor.checkCancelled();
            }
            @Override public void write(int value) throws IOException {
                requireWritable();
                try {
                    primary.write(value);
                    if (shadow != null) shadow.write(value);
                } catch (IOException failure) { failed = failure; throw failure; }
                monitor.worked(1);
            }
            @Override public void write(byte[] bytes, int off, int length) throws IOException {
                Objects.checkFromIndexSize(off, length, Objects.requireNonNull(bytes, "bytes").length);
                requireWritable();
                if (length == 0) return;
                try {
                    primary.write(bytes, off, length);
                    if (shadow != null) shadow.write(bytes, off, length);
                } catch (IOException failure) { failed = failure; throw failure; }
                monitor.worked(length);
            }
            @Override public void flush() throws IOException {
                requireWritable();
                try {
                    primary.flush();
                    if (shadow != null) shadow.flush();
                } catch (IOException failure) { failed = failure; throw failure; }
            }
            @Override public void close() throws IOException {
                if (closed) return;
                closed = true;
                IOException error = null;
                try { primary.close(); } catch (IOException failure) { error = failure; }
                if (shadow != null && shadow != primary) {
                    try { shadow.close(); }
                    catch (IOException failure) {
                        if (error == null) error = failure;
                        else error.addSuppressed(failure);
                    }
                }
                if (error != null) throw error;
            }
        };
    }

    /** A single source owner whose independent cursors read one shared page arena. */
    public static final class SharedInput extends InputStream {
        private final PushbackInputStream source;
        private final int budget;
        private final M3StreamProgress progress;
        private byte[][] pages = new byte[4][];
        private int retained;
        private int position;
        private int mark;
        private boolean eof;
        private boolean closed;
        private final byte[] single = new byte[1];

        private SharedInput(InputStream source, int budget, M3StreamProgress progress) {
            if (budget < 0) throw new IllegalArgumentException("negative retention budget");
            this.source = new PushbackInputStream(Objects.requireNonNull(source, "source"), 1);
            this.budget = budget;
            this.progress = M3StreamProgress.require(progress);
        }

        /** Shadows start at byte zero and can drive the single original read. */
        public synchronized InputStream shadow(M3StreamProgress parserProgress) throws IOException {
            requireOpen();
            return new ShadowCursor(this, 0, M3StreamProgress.require(parserProgress));
        }

        /** Shadows may also start at the current primary logical offset. */
        public synchronized InputStream shadowAtCurrent(M3StreamProgress parserProgress) throws IOException {
            requireOpen();
            return new ShadowCursor(this, position, M3StreamProgress.require(parserProgress));
        }

        public synchronized int consumedSourceBytes() { return retained; }
        public int retentionBudget() { return budget; }

        private void requireOpen() throws IOException {
            if (closed) throw new IOException("M3 shared input closed");
        }

        /** Pull at most one page, respecting the explicit budget before I/O. */
        private void pull() throws IOException {
            if (eof) return;
            progress.checkCancelled();
            if (retained == budget) {
                int probe = source.read();
                if (probe < 0) { eof = true; return; }
                source.unread(probe);
                throw new IOException("M3 shadow retention budget exceeded");
            }
            int pageIndex = retained >>> PAGE_SHIFT;
            int inPage = retained & (PAGE_SIZE - 1);
            if (pageIndex == pages.length) pages = Arrays.copyOf(pages, pages.length * 2);
            if (pages[pageIndex] == null) pages[pageIndex] = new byte[PAGE_SIZE];
            int capacity = Math.min(PAGE_SIZE - inPage, budget - retained);
            int zeros = 0;
            int count;
            do {
                count = source.read(pages[pageIndex], inPage, capacity);
                if (count == 0 && ++zeros > MAX_ZERO_READS) {
                    throw new IOException("M3 input source made no progress");
                }
            } while (count == 0);
            if (count < 0) { eof = true; return; }
            retained = Math.addExact(retained, count);
            progress.worked(count);
        }

        /** All readers share this exact physical byte coordinate space. */
        private synchronized int readAt(int cursor, byte[] target, int offset, int length)
                throws IOException {
            Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(target, "target").length);
            requireOpen();
            if (length == 0) return 0;
            if (cursor >= retained && !eof) pull();
            if (cursor >= retained) return -1;
            int count = Math.min(length, retained - cursor);
            int consumed = 0;
            while (consumed < count) {
                int at = cursor + consumed;
                int within = at & (PAGE_SIZE - 1);
                int piece = Math.min(count - consumed, PAGE_SIZE - within);
                System.arraycopy(pages[at >>> PAGE_SHIFT], within, target, offset + consumed, piece);
                consumed += piece;
            }
            return count;
        }

        @Override public synchronized int read() throws IOException {
            int n = read(single, 0, 1);
            return n < 0 ? -1 : Byte.toUnsignedInt(single[0]);
        }
        @Override public synchronized int read(byte[] bytes, int off, int length) throws IOException {
            int n = readAt(position, bytes, off, length);
            if (n > 0) position += n;
            return n;
        }
        @Override public synchronized long skip(long count) throws IOException {
            requireOpen();
            if (count <= 0) return 0;
            long skipped = 0;
            while (skipped < count) {
                if (position >= retained && !eof) pull();
                if (position >= retained) break;
                int delta = (int) Math.min(count - skipped, retained - position);
                position += delta;
                skipped += delta;
            }
            return skipped;
        }
        @Override public synchronized int available() throws IOException {
            requireOpen();
            return Math.max(0, retained - position);
        }
        @Override public boolean markSupported() { return true; }
        @Override public synchronized void mark(int readlimit) { mark = position; }
        @Override public synchronized void reset() throws IOException { requireOpen(); position = mark; }
        @Override public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            Arrays.fill(pages, null);
            source.close();
        }
    }

    private static final class ShadowCursor extends InputStream {
        private final SharedInput owner;
        private final M3StreamProgress monitor;
        private int position;
        private int mark;
        private boolean closed;
        private final byte[] single = new byte[1];

        ShadowCursor(SharedInput owner, int start, M3StreamProgress monitor) {
            this.owner = owner;
            this.monitor = monitor;
            this.position = this.mark = start;
        }
        private void requireOpen() throws IOException {
            if (closed) throw new IOException("M3 shadow cursor closed");
        }
        @Override public int read() throws IOException {
            return read(single, 0, 1) < 0 ? -1 : Byte.toUnsignedInt(single[0]);
        }
        @Override public int read(byte[] bytes, int off, int length) throws IOException {
            Objects.checkFromIndexSize(off, length, Objects.requireNonNull(bytes, "bytes").length);
            requireOpen();
            if (length == 0) return 0;
            monitor.checkCancelled();
            int n = owner.readAt(position, bytes, off, length);
            if (n > 0) { position += n; monitor.worked(n); }
            return n;
        }
        @Override public long skip(long length) throws IOException {
            requireOpen();
            if (length <= 0) return 0;
            monitor.checkCancelled();
            long skipped = 0;
            while (skipped < length) {
                int available;
                synchronized (owner) {
                    owner.requireOpen();
                    if (position >= owner.retained && !owner.eof) owner.pull();
                    available = owner.retained - position;
                }
                if (available <= 0) break;
                int delta = (int) Math.min(length - skipped, available);
                position += delta;
                skipped += delta;
            }
            if (skipped > 0) monitor.worked(skipped);
            return skipped;
        }
        @Override public int available() throws IOException {
            requireOpen();
            synchronized (owner) {
                owner.requireOpen();
                return owner.retained - position;
            }
        }
        @Override public boolean markSupported() { return true; }
        @Override public void mark(int readlimit) { mark = position; }
        @Override public void reset() throws IOException {
            requireOpen();
            position = mark;
        }
        @Override public void close() { closed = true; }
    }
}
