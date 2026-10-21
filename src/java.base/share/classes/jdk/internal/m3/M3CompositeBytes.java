/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * M3JDK21 internal composite representation: never copies existing atoms.
 */
package jdk.internal.m3;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, possibly very large logical byte sequence composed of M3 atoms.
 *
 * <p>Only long handle arrays, integer slice offsets and lengths, and prefix
 * indexes represent the sequence. Joining and slicing never copy backing bytes.
 * Each live instance holds explicit M3 leases on its referenced descriptors.
 * Close releases those leases. This is not yet java.lang.String integration;
 * preserving UTF-16/JDK String semantics requires separate VM-wide changes.
 */
public final class M3CompositeBytes implements AutoCloseable {
    private final M3ByteSlices pool;
    private final long[] handles;
    private final int[] byteOffsets;
    private final int[] byteLengths;
    private final long[] ends;
    private final long length;
    private boolean closed;

    private M3CompositeBytes(M3ByteSlices pool, long[] handles,
                             int[] offsets, int[] lengths) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.handles = handles.clone();
        this.byteOffsets = offsets.clone();
        this.byteLengths = lengths.clone();
        if (handles.length != offsets.length || handles.length != lengths.length) {
            throw new IllegalArgumentException("piece descriptor lengths differ");
        }
        this.ends = new long[handles.length];
        long total = 0;
        int retained = 0;
        try {
            for (int i = 0; i < handles.length; i++) {
                Objects.checkFromIndexSize(offsets[i], lengths[i], pool.length(handles[i]));
                total = Math.addExact(total, lengths[i]);
                ends[i] = total;
                pool.retain(handles[i]);
                retained++;
            }
        } catch (RuntimeException | Error failure) {
            for (int i = retained - 1; i >= 0; i--) {
                pool.release(handles[i]);
            }
            throw failure;
        }
        this.length = total;
    }

    /** Makes a composite over whole existing atoms; no byte arrays are copied. */
    public static M3CompositeBytes of(M3ByteSlices pool, long... atoms) {
        Objects.requireNonNull(atoms, "atoms");
        Objects.requireNonNull(pool, "pool");
        final int[] starts = new int[atoms.length];
        final int[] sizes = new int[atoms.length];
        for (int i = 0; i < atoms.length; i++) {
            sizes[i] = pool.length(atoms[i]);
        }
        return new M3CompositeBytes(pool, atoms, starts, sizes);
    }

    public synchronized long length() {
        requireOpen();
        return length;
    }

    public synchronized int pieceCount() {
        requireOpen();
        return handles.length;
    }

    public synchronized byte byteAt(long position) {
        requireOpen();
        Objects.checkIndex(position, length);
        final int piece = locate(position);
        final long pieceStart = piece == 0 ? 0 : ends[piece - 1];
        return pool.byteAt(handles[piece],
                byteOffsets[piece] + Math.toIntExact(position - pieceStart));
    }

    /** Returns a composite of descriptor ranges without allocating backing bytes. */
    public synchronized M3CompositeBytes slice(long from, long count) {
        requireOpen();
        Objects.checkFromIndexSize(from, count, length);
        final long to = from + count;
        final long[] newHandles = new long[handles.length];
        final int[] newOffsets = new int[handles.length];
        final int[] newLengths = new int[handles.length];
        int used = 0;
        long currentStart = 0;
        for (int i = 0; i < handles.length; i++) {
            final long first = Math.max(from, currentStart);
            final long last = Math.min(to, ends[i]);
            if (first < last) {
                newHandles[used] = handles[i];
                newOffsets[used] = byteOffsets[i] + Math.toIntExact(first - currentStart);
                newLengths[used] = Math.toIntExact(last - first);
                used++;
            }
            currentStart = ends[i];
        }
        return new M3CompositeBytes(pool, Arrays.copyOf(newHandles, used),
                Arrays.copyOf(newOffsets, used), Arrays.copyOf(newLengths, used));
    }

    /** Concatenates descriptor arrays only; byte payloads remain shared. */
    public M3CompositeBytes concat(M3CompositeBytes other) {
        Objects.requireNonNull(other, "other");
        // Use is confined to a caller-controlled lifecycle; no concurrent close.
        final long[] leftHandles;
        final int[] leftOffsets;
        final int[] leftLengths;
        synchronized (this) {
            requireOpen();
            leftHandles = handles.clone();
            leftOffsets = byteOffsets.clone();
            leftLengths = byteLengths.clone();
        }
        final long[] rightHandles;
        final int[] rightOffsets;
        final int[] rightLengths;
        synchronized (other) {
            other.requireOpen();
            if (pool != other.pool) {
                throw new IllegalArgumentException("cannot concatenate different M3 pools");
            }
            rightHandles = other.handles.clone();
            rightOffsets = other.byteOffsets.clone();
            rightLengths = other.byteLengths.clone();
        }
        return new M3CompositeBytes(pool, append(leftHandles, rightHandles),
                append(leftOffsets, rightOffsets), append(leftLengths, rightLengths));
    }

    /** Deliberate materialization boundary for Java APIs requiring byte[]. */
    public synchronized byte[] materialize() {
        requireOpen();
        if (length > Integer.MAX_VALUE - 8) {
            throw new IllegalStateException("M3 composite is too large for one byte[]");
        }
        final byte[] result = new byte[(int) length];
        int written = 0;
        for (int i = 0; i < handles.length; i++) {
            pool.copyTo(handles[i], byteOffsets[i], result, written, byteLengths[i]);
            written += byteLengths[i];
        }
        return result;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (long handle : handles) {
            pool.release(handle);
        }
    }

    private int locate(long index) {
        // Lower-bound search: the first prefix end strictly greater than index.
        int lo = 0;
        int hi = ends.length;
        while (lo < hi) {
            final int middle = (lo + hi) >>> 1;
            if (ends[middle] <= index) {
                lo = middle + 1;
            } else {
                hi = middle;
            }
        }
        return lo;
    }

    private static long[] append(long[] left, long[] right) {
        final long[] combined = Arrays.copyOf(left, Math.addExact(left.length, right.length));
        System.arraycopy(right, 0, combined, left.length, right.length);
        return combined;
    }

    private static int[] append(int[] left, int[] right) {
        final int[] combined = Arrays.copyOf(left, Math.addExact(left.length, right.length));
        System.arraycopy(right, 0, combined, left.length, right.length);
        return combined;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("closed M3 composite");
        }
    }
}
