/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.nlpsearch;

import com.m3.runtime.capability.SourceArtifact;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An independently closeable byte cursor over an existing immutable source span.
 * No payload copy or secondary byte owner is created. Output-array reads are the
 * ordinary InputStream materialization boundary; coordinates stay span-relative.
 */
public final class M3SourceInputStream extends InputStream {
    private SourceArtifact.Span span;
    private int position;
    private int markedPosition;

    private M3SourceInputStream(SourceArtifact.Span span) {
        this.span = Objects.requireNonNull(span, "span");
    }

    /** Borrow exactly this span; the initial cursor and mark are both zero. */
    public static M3SourceInputStream open(SourceArtifact.Span span) {
        return new M3SourceInputStream(span);
    }

    @Override
    public synchronized int read() throws IOException {
        ensureOpen();
        return position == span.length() ? -1 : span.byteAt(position++) & 0xff;
    }

    @Override
    public synchronized int read(byte[] target, int offset, int length) throws IOException {
        ensureOpen();
        Objects.checkFromIndexSize(offset, length, target.length);
        if (length == 0) return 0;
        int count = Math.min(length, span.length() - position);
        if (count == 0) return -1;
        for (int index = 0; index < count; index++) {
            target[offset + index] = span.byteAt(position + index);
        }
        position += count;
        return count;
    }

    @Override
    public synchronized long skip(long count) throws IOException {
        ensureOpen();
        if (count <= 0) return 0;
        int skipped = (int) Math.min(count, (long) span.length() - position);
        position += skipped;
        return skipped;
    }

    @Override
    public synchronized int available() throws IOException {
        ensureOpen();
        return span.length() - position;
    }

    /** Immutable backing keeps the mark valid regardless of the advisory read limit. */
    @Override
    public synchronized void mark(int readLimit) {
        if (span == null) throw new IllegalStateException("source cursor closed");
        markedPosition = position;
    }

    @Override
    public synchronized void reset() throws IOException {
        ensureOpen();
        position = markedPosition;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /** Release this cursor's source reference only; other cursors and the owner remain valid. */
    @Override
    public synchronized void close() {
        span = null;
        position = 0;
        markedPosition = 0;
    }

    private void ensureOpen() throws IOException {
        if (span == null) throw new IOException("source cursor closed");
    }
}
