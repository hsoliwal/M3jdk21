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
 * Cancellation is checked before each block.</p>
 */
public final class M3FileShadowOutputStream extends FilterOutputStream {
    private final long maxBytes;
    private final M3FileProgress progress;
    private final M3FileShadow[] shadows;
    private final byte[] single = new byte[1];
    private long count;
    private boolean finished;

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
        if (finished) throw new IOException("stream closed");
        progress.checkCanceled();
        if (length > maxBytes - count) throw new M3FileLimitException("resource exceeds configured write limit");
        out.write(bytes, offset, length);
        count += length;
        for (M3FileShadow shadow : shadows) shadow.update(bytes, offset, length);
        progress.worked(length);
    }

    @Override
    public void close() throws IOException {
        if (finished) return;
        super.close();
        finished = true;
        for (M3FileShadow shadow : shadows) shadow.finish();
    }

    /** Bytes written so far. */
    public long count() {
        return count;
    }
}
