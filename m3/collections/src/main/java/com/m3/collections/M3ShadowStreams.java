/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * One physical I/O traversal with synchronous borrowed shadow-output consumers.
 * Observers see bytes ONLY after the original read/write consumes them. There is no
 * read-ahead, independent parser, retained file payload or per-byte record object.
 * A slow observer backpressures the producer. On observer failure, already consumed
 * original bytes cannot be replayed: fail loudly and stop the original stream.
 *
 * All observers and supplied monitors are caller-owned. The primary stream is owned
 * by this wrapper and closed with it. mark/reset is refused on input because it
 * would duplicate shadow observations. skip reads/distributes actual bytes.
 */
public final class M3ShadowStreams {
    private static final int SKIP_BUFFER = 8192;
    private M3ShadowStreams() { }

    public static InputStream input(InputStream primary, M3ProgressMonitor monitor,
            OutputStream... observers) {
        return new ObservedInput(primary, monitor, admit(observers));
    }
    public static OutputStream output(OutputStream primary, M3ProgressMonitor monitor,
            OutputStream... observers) {
        return new ObservedOutput(primary, monitor, admit(observers));
    }
    private static OutputStream[] admit(OutputStream[] observers) {
        OutputStream[] selected = Objects.requireNonNull(observers, "observers").clone();
        if (selected.length > 64) throw new IllegalArgumentException("shadow observer budget exceeded");
        for (OutputStream sink : selected) Objects.requireNonNull(sink, "shadow observer");
        return selected;
    }

    private static IOException shadowFailure(IOException failure) {
        return new IOException("shadow output failed after original I/O advanced; no implicit replay", failure);
    }
    private static final class ObservedInput extends FilterInputStream {
        private final M3ProgressMonitor monitor;
        private final OutputStream[] observers;
        private boolean poisoned;
        private boolean dispatching;
        private boolean closed;
        ObservedInput(InputStream input, M3ProgressMonitor monitor, OutputStream[] observers) {
            super(Objects.requireNonNull(input, "primary input"));
            this.monitor = Objects.requireNonNull(monitor, "monitor");
            this.observers = observers;
        }
        private void ready() throws IOException {
            if (closed) throw new IOException("primary stream already closed");
            if (poisoned || dispatching) throw new IOException("shadow stream cannot replay/reenter observer dispatch");
            monitor.checkCanceled();
        }
        @Override public synchronized int read() throws IOException {
            ready();
            int result;
            try { result = in.read(); }
            catch (IOException problem) { poisoned = true; throw problem; }
            if (result < 0) return -1;
            try { monitor.worked(1L); }
            catch (RuntimeException problem) { poisoned = true; throw problem; }
            try {
                dispatching = true;
                for (OutputStream output : observers) output.write(result);
            } catch (IOException error) { poisoned = true; throw shadowFailure(error); }
            finally { dispatching = false; }
            return result;
        }
        @Override public synchronized int read(byte[] target, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(target, "target").length);
            ready();
            if (length == 0) return 0;
            int actual;
            try { actual = in.read(target, offset, length); }
            catch (IOException problem) { poisoned = true; throw problem; }
            if (actual <= 0) return actual;
            try { monitor.worked(actual); }
            catch (RuntimeException problem) { poisoned = true; throw problem; }
            try {
                dispatching = true;
                for (OutputStream output : observers) output.write(target, offset, actual);
            } catch (IOException error) { poisoned = true; throw shadowFailure(error); }
            finally { dispatching = false; }
            return actual;
        }
        /** Never bypass shadow consumers via the primary delegate's skip implementation. */
        @Override public synchronized long skip(long amount) throws IOException {
            ready();
            if (amount <= 0L) return 0L;
            byte[] scratch = new byte[(int) Math.min(amount, SKIP_BUFFER)];
            long skipped = 0L;
            while (skipped < amount) {
                int actual = read(scratch, 0, (int) Math.min(scratch.length, amount - skipped));
                if (actual < 0) break;
                if (actual == 0) throw new IOException("primary skip read made no progress");
                skipped += actual;
            }
            return skipped;
        }
        @Override public boolean markSupported() { return false; }
        @Override public void mark(int ignored) { /* No buffered replay or shadow rewind. */ }
        @Override public void reset() throws IOException { throw new IOException("shadow input does not support replay/reset"); }
        @Override public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            try { in.close(); } finally { monitor.done(); }
        }
    }

    private static final class ObservedOutput extends FilterOutputStream {
        private final M3ProgressMonitor monitor;
        private final OutputStream[] observers;
        private boolean poisoned;
        private boolean dispatching;
        private boolean closed;
        ObservedOutput(OutputStream output, M3ProgressMonitor monitor, OutputStream[] observers) {
            super(Objects.requireNonNull(output, "primary output"));
            this.monitor = Objects.requireNonNull(monitor, "monitor");
            this.observers = observers;
        }
        private void ready() throws IOException {
            if (closed) throw new IOException("primary stream already closed");
            if (poisoned || dispatching) throw new IOException("shadow stream cannot replay/reenter observer dispatch");
            monitor.checkCanceled();
        }
        @Override public synchronized void write(int value) throws IOException {
            ready();
            try { out.write(value); }
            catch (IOException problem) { poisoned = true; throw problem; }
            try { monitor.worked(1L); }
            catch (RuntimeException problem) { poisoned = true; throw problem; }
            try {
                dispatching = true;
                for (OutputStream output : observers) output.write(value);
            } catch (IOException error) { poisoned = true; throw shadowFailure(error); }
            finally { dispatching = false; }
        }
        /** Avoid FilterOutputStream's byte-by-byte write loop and duplicate progress. */
        @Override public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(bytes, "bytes").length);
            ready();
            if (length == 0) return;
            try { out.write(bytes, offset, length); }
            catch (IOException problem) { poisoned = true; throw problem; }
            try { monitor.worked(length); }
            catch (RuntimeException problem) { poisoned = true; throw problem; }
            try {
                dispatching = true;
                for (OutputStream output : observers) output.write(bytes, offset, length);
            } catch (IOException error) { poisoned = true; throw shadowFailure(error); }
            finally { dispatching = false; }
        }
        @Override public synchronized void flush() throws IOException {
            ready();
            try { out.flush(); }
            catch (IOException problem) { poisoned = true; throw problem; }
            try {
                dispatching = true;
                for (OutputStream output : observers) output.flush();
            } catch (IOException error) { poisoned = true; throw shadowFailure(error); }
            finally { dispatching = false; }
        }
        @Override public synchronized void close() throws IOException {
            if (closed) return;
            closed = true;
            try { out.close(); } finally { monitor.done(); }
        }
    }
}
