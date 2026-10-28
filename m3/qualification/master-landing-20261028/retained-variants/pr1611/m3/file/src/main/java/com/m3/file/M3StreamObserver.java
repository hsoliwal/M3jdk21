// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Allocation-free per-chunk shadow of bytes already transferred by a primary stream.
 *
 * <p>An observer never owns the primary stream. Implementations must consume the supplied range
 * during the call and must not retain or mutate the caller's buffer.</p>
 */
@FunctionalInterface
public interface M3StreamObserver {
    void onBytes(byte[] bytes, int offset, int length) throws IOException;

    default void onEof() throws IOException { }

    default void onClose() throws IOException { }

    default M3StreamObserver andThen(M3StreamObserver next) {
        M3StreamObserver first = this;
        M3StreamObserver second = Objects.requireNonNull(next, "next");
        return new M3StreamObserver() {
            @Override public void onBytes(byte[] bytes, int offset, int length) throws IOException {
                first.onBytes(bytes, offset, length);
                second.onBytes(bytes, offset, length);
            }

            @Override public void onEof() throws IOException {
                first.onEof();
                second.onEof();
            }

            @Override public void onClose() throws IOException {
                IOException failure = null;
                try {
                    first.onClose();
                } catch (IOException problem) {
                    failure = problem;
                }
                try {
                    second.onClose();
                } catch (IOException problem) {
                    if (failure == null) failure = problem;
                    else failure.addSuppressed(problem);
                }
                if (failure != null) throw failure;
            }
        };
    }

    /** Observer that writes the exact transferred bytes to another caller-owned stream. */
    static M3StreamObserver to(OutputStream output) {
        OutputStream target = Objects.requireNonNull(output, "output");
        return target::write;
    }
}
