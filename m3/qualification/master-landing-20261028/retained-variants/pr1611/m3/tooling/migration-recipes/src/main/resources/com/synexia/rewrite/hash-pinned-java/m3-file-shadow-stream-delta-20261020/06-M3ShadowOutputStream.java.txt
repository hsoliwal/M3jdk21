// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Output stream that notifies a shadow after the primary write succeeds.
 *
 * <p>The primary output remains authoritative. If a shadow fails after a successful primary write,
 * the write cannot be rolled back and the observer failure is propagated.</p>
 */
public final class M3ShadowOutputStream extends FilterOutputStream {
    private final M3ProgressMonitor progress;
    private final M3StreamObserver observer;
    private final byte[] one = new byte[1];
    private boolean closed;

    public M3ShadowOutputStream(
            OutputStream output, M3ProgressMonitor progress, M3StreamObserver observer) {
        super(Objects.requireNonNull(output, "output"));
        this.progress = Objects.requireNonNull(progress, "progress");
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    public static OutputStream wrap(
            OutputStream output, M3ProgressMonitor progress, M3StreamObserver observer) {
        return new M3ShadowOutputStream(output, progress, observer);
    }

    @Override public void write(int value) throws IOException {
        progress.checkCanceled();
        super.write(value);
        one[0] = (byte) value;
        observer.onBytes(one, 0, 1);
        progress.worked(1L);
        progress.checkCanceled();
    }

    @Override public void write(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(bytes, "bytes").length);
        if (length == 0) return;
        progress.checkCanceled();
        out.write(bytes, offset, length);
        observer.onBytes(bytes, offset, length);
        progress.worked(length);
        progress.checkCanceled();
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
}
