/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.io;

import java.util.Objects;

/**
 * A forward-only input with bounded, consumption-driven shadow streams.
 * Only reads on this main stream advance the source. Each shadow sees the bytes
 * consumed since its attachment, including main-stream skips. It cannot read
 * ahead. A shadow read with no available bytes throws {@link IOException} until
 * the main stream reaches EOF or closes; only then does it return EOF.
 *
 * <p>The ring is shared by all shadows. A lagging shadow that fills it causes
 * the next main read to fail before reading the source. Drain or close that
 * shadow to resume. A main bulk read may return fewer bytes to respect available
 * ring space. No background reader or unbounded replay storage is created.
 *
 * <p>Main close closes the source once. Shadows can drain already consumed bytes
 * after main close; closing a shadow only detaches that shadow. This class and
 * its shadows do not support mark/reset. Operations synchronize on this owner;
 * callers must not read the original source directly while this owner is active.
 */
public final class ShadowInputStream extends InputStream {
    private final InputStream source;
    private final byte[] ring;
    private final int mask;
    private final long[] positions;
    private final boolean[] active;
    private long end;
    private boolean eof;
    private boolean closed;

    /**
     * Creates a journal without reading the source.
     * @param source owned input, closed by this stream
     * @param capacity power-of-two byte capacity, at least one
     * @param maxShadows positive maximum simultaneously attached shadows
     * @throws IllegalArgumentException for invalid bounds
     * @throws NullPointerException if source is null
     */
    public ShadowInputStream(InputStream source, int capacity, int maxShadows) {
        this.source = Objects.requireNonNull(source, "source");
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0 || maxShadows <= 0) {
            throw new IllegalArgumentException("power-of-two capacity and positive shadow count required");
        }
        ring = new byte[capacity];
        mask = capacity - 1;
        positions = new long[maxShadows];
        active = new boolean[maxShadows];
    }

    /**
     * Attaches a shadow at the current main consumption position.
     * @return a caller-owned shadow; closing it releases its ring claim
     * @throws IOException if main is closed or the shadow bound is reached
     */
    public synchronized InputStream shadow() throws IOException {
        open();
        for (int slot = 0; slot < active.length; slot++) {
            if (!active[slot]) {
                InputStream result = new Shadow(slot);
                positions[slot] = end;
                active[slot] = true;
                return result;
            }
        }
        throw new IOException("shadow count limit reached");
    }

    private void open() throws IOException {
        if (closed) throw new IOException("main stream is closed");
    }

    private int room(int requested) throws IOException {
        long first = end;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) first = Math.min(first, positions[i]);
        }
        int free = ring.length - (int) (end - first);
        if (free == 0) throw new IOException("shadow lag exceeds ring capacity; drain or close a shadow");
        long remaining = Long.MAX_VALUE - end;
        if (remaining == 0) throw new IOException("consumption position overflow");
        return (int) Math.min(Math.min(requested, free), remaining);
    }

    @Override public synchronized int read() throws IOException {
        open();
        if (eof) return -1;
        room(1);
        int value = source.read();
        if (value < 0) { eof = true; return -1; }
        ring[(int) end & mask] = (byte) value;
        end++;
        return value;
    }

    @Override public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        open();
        if (length == 0) return 0;
        if (eof) return -1;
        int limit = room(length);
        int count = source.read(bytes, offset, limit);
        if (count < 0) { eof = true; return -1; }
        if (count == 0) {
            int value = read();
            if (value < 0) return -1;
            bytes[offset] = (byte) value;
            return 1;
        }
        int at = (int) end & mask;
        int first = Math.min(count, ring.length - at);
        System.arraycopy(bytes, offset, ring, at, first);
        System.arraycopy(bytes, offset + first, ring, 0, count - first);
        end += count;
        return count;
    }

    @Override public synchronized long skip(long count) throws IOException {
        open();
        // InputStream's bounded scratch loop routes through this owner's reads.
        return super.skip(count);
    }

    @Override public synchronized int available() throws IOException {
        open();
        if (eof) return 0;
        long first = end;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) first = Math.min(first, positions[i]);
        }
        return (int) Math.min(Math.min(source.available(), ring.length - (end - first)),
                Long.MAX_VALUE - end);
    }

    @Override public synchronized void close() throws IOException {
        if (!closed) { closed = true; source.close(); }
    }

    private final class Shadow extends InputStream {
        private final int slot;
        private boolean detached;
        Shadow(int slot) { this.slot = slot; }
        private int pending() throws IOException {
            if (detached) throw new IOException("shadow is closed");
            int count = (int) (end - positions[slot]);
            if (count == 0 && !eof && !closed) {
                throw new IOException("main stream must advance before this shadow can read");
            }
            return count;
        }
        @Override public int read() throws IOException {
            synchronized (ShadowInputStream.this) {
                if (pending() == 0) return -1;
                return ring[(int) positions[slot]++ & mask] & 255;
            }
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            synchronized (ShadowInputStream.this) {
                if (detached) throw new IOException("shadow is closed");
                if (length == 0) return 0;
                int count = Math.min(length, pending());
                if (count == 0) return -1;
                int at = (int) positions[slot] & mask;
                int first = Math.min(count, ring.length - at);
                System.arraycopy(ring, at, bytes, offset, first);
                System.arraycopy(ring, 0, bytes, offset + first, count - first);
                positions[slot] += count;
                return count;
            }
        }
        @Override public long skip(long count) throws IOException {
            synchronized (ShadowInputStream.this) {
                if (detached) throw new IOException("shadow is closed");
                if (count <= 0) return 0;
                long skipped = Math.min(count, pending());
                positions[slot] += skipped;
                return skipped;
            }
        }
        @Override public int available() throws IOException {
            synchronized (ShadowInputStream.this) {
                if (detached) throw new IOException("shadow is closed");
                return (int) (end - positions[slot]);
            }
        }
        @Override public void close() {
            synchronized (ShadowInputStream.this) {
                if (!detached) { detached = true; active[slot] = false; }
            }
        }
    }
}
