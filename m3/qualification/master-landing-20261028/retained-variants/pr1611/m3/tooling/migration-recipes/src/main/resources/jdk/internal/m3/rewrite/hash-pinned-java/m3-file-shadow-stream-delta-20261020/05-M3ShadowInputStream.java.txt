// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Input stream whose shadow observer advances only as bytes are actually read from the primary.
 *
 * <p>The primary read happens first. If a shadow fails, already-consumed primary bytes cannot be
 * rolled back and the failure is propagated. {@link #skip(long)} deliberately does not shadow
 * skipped bytes because they were not read by the consumer.</p>
 */
public final class M3ShadowInputStream extends FilterInputStream {
    private final M3ProgressMonitor progress;
    private final M3StreamObserver observer;
    private final byte[] one = new byte[1];
    private boolean eof;
    private boolean closed;

    public M3ShadowInputStream(
            InputStream input, M3ProgressMonitor progress, M3StreamObserver observer) {
        super(Objects.requireNonNull(input, "input"));
        this.progress = Objects.requireNonNull(progress, "progress");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public static InputStream wrap(
            InputStream input, M3ProgressMonitor progress, M3StreamObserver observer) {
        return new M3ShadowInputStream(input, progress, observer);
    }

    @Override public int read() throws IOException {
        progress.checkCanceled();
        int value = super.read();
        if (value < 0) {
            eof();
            return -1;
        }
        one[0] = (byte) value;
        observer.onBytes(one, 0, 1);
        progress.worked(1L);
        progress.checkCanceled();
        return value;
    }

    @Override public int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(bytes, "bytes").length);
        if (length == 0) return 0;
        progress.checkCanceled();
        int count = super.read(bytes, offset, length);
        if (count < 0) {
            eof();
            return -1;
        }
        observer.onBytes(bytes, offset, count);
        progress.worked(count);
        progress.checkCanceled();
        return count;
    }

    @Override public void close() throws IOException {
        if (closed) return;
        closed = true;
        IOException failure = null;
        try {
            super.close();
        } catch (IOException problem) {
            failure = problem;
        }
        try {
            observer.onClose();
        } catch (IOException problem) {
            if (failure == null) failure = problem;
            else failure.addSuppressed(problem);
        }
        progress.done();
        if (failure != null) throw failure;
    }

    private void eof() throws IOException {
        if (eof) return;
        eof = true;
        observer.onEof();
        progress.done();
    }
}
