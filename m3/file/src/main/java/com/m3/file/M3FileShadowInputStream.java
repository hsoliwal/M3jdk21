// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Main input stream with a sub-progress monitor, a byte limit and shadows that move as it is read.
 *
 * <p>Every byte returned (or skipped: skip reads through) reaches every shadow exactly once, in order;
 * shadows {@link M3FileShadow#finish() finish} at EOF. Mark/reset is unsupported so no layer ever sees
 * a byte twice. Progress units are bytes; cancellation is checked before each transfer and surfaces as
 * {@link java.io.InterruptedIOException}. Reading past {@code maxBytes} fails like
 * {@link M3File#readAllBytes(int)}.</p>
 */
public final class M3FileShadowInputStream extends FilterInputStream {
    private final long maxBytes;
    private final M3FileProgress progress;
    private final M3FileShadow[] shadows;
    private final byte[] single = new byte[1];
    private byte[] scratch;
    private long count;
    private boolean ended;

    public M3FileShadowInputStream(InputStream in, long maxBytes, M3FileProgress progress, M3FileShadow... shadows) {
        super(Objects.requireNonNull(in, "in"));
        if (maxBytes < 0) throw new IllegalArgumentException("negative maxBytes");
        this.maxBytes = maxBytes;
        this.progress = Objects.requireNonNull(progress, "progress");
        this.shadows = shadows.clone();
        for (M3FileShadow shadow : this.shadows) Objects.requireNonNull(shadow, "shadow");
    }

    @Override
    public int read() throws IOException {
        int n;
        do {
            n = read(single, 0, 1);
        } while (n == 0);
        return n < 0 ? -1 : single[0] & 0xff;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if (length == 0) return 0;
        progress.checkCanceled();
        int n = in.read(bytes, offset, length);
        if (n < 0) {
            end();
            return -1;
        }
        observe(bytes, offset, n);
        return n;
    }

    /** Reads through so the shadows see the skipped bytes. */
    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) return 0;
        if (scratch == null) scratch = new byte[8192];
        long skipped = 0;
        while (skipped < n) {
            int read = read(scratch, 0, (int) Math.min(scratch.length, n - skipped));
            if (read < 0) break;
            skipped += read;
        }
        return skipped;
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        if (scratch == null) scratch = new byte[8192];
        long transferred = 0;
        for (int read = read(scratch, 0, scratch.length); read >= 0; read = read(scratch, 0, scratch.length)) {
            out.write(scratch, 0, read);
            transferred += read;
        }
        return transferred;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readLimit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported: shadows see each byte once");
    }

    /** Bytes read so far. */
    public long count() {
        return count;
    }

    /** True once EOF was reached and the shadows finished. */
    public boolean ended() {
        return ended;
    }

    private void observe(byte[] bytes, int offset, int n) throws IOException {
        count += n;
        if (count > maxBytes) throw new M3FileLimitException("resource exceeds configured read limit");
        for (M3FileShadow shadow : shadows) shadow.update(bytes, offset, n);
        progress.worked(n);
    }

    private void end() {
        if (ended) return;
        ended = true;
        for (M3FileShadow shadow : shadows) shadow.finish();
    }
}
