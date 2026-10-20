// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Main output stream with a sub-progress monitor, a byte limit and shadows that move as it is written.
 *
 * <p>Blocks go to the underlying stream unsplit, then to every shadow; shadows finish when the stream
 * closes successfully. A write that would pass {@code maxBytes} fails before any byte of it is written.
 * Cancellation is checked before each block. A failed provider write or shadow callback makes the
 * observation incomplete: later writes fail and close still releases the provider without finishing
 * the shadows. Repeated close never converts an earlier close failure into completion.</p>
 */
public final class M3FileShadowOutputStream extends FilterOutputStream {
    private static final int FAILED = 1;
    private static final int CLOSED = 2;
    private final long maxBytes;
    private final M3FileProgress progress;
    private final M3FileShadow[] shadows;
    private final byte[] single = new byte[1];
    private long count;
    private int state;

    public M3FileShadowOutputStream(OutputStream out, long maxBytes, M3FileProgress progress, M3FileShadow... shadows) {
        super(Objects.requireNonNull(out, "out"));
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        this.maxBytes = maxBytes;
        this.progress = Objects.requireNonNull(progress, "progress");
        this.shadows = shadows.clone();
        for (M3FileShadow shadow : this.shadows) Objects.requireNonNull(shadow, "shadow");
    }

    @Override
    public void write(int b) throws IOException {
        single[0] = (byte) b;
        write(single, 0, 1);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if ((state & CLOSED) != 0) throw new IOException("stream closed");
        if ((state & FAILED) != 0) throw new IOException("stream observation failed");
        progress.checkCanceled();
        if (length > maxBytes - count) throw new M3FileLimitException("resource exceeds configured write limit");
        try {
            out.write(bytes, offset, length);
            count += length;
            for (M3FileShadow shadow : shadows) shadow.update(bytes, offset, length);
            progress.worked(length);
        } catch (IOException | RuntimeException | Error failure) {
            state |= FAILED;
            throw failure;
        }
    }

    @Override
    public void close() throws IOException {
        if ((state & CLOSED) != 0) return;
        // FilterOutputStream closes at most once, including when flush or close throws.
        state |= CLOSED;
        try {
            super.close();
            if ((state & FAILED) == 0) {
                for (M3FileShadow shadow : shadows) shadow.finish();
            }
        } catch (IOException | RuntimeException | Error failure) {
            state |= FAILED;
            throw failure;
        }
    }

    /** Bytes written so far. */
    public long count() {
        return count;
    }
}
