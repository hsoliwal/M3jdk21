/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * M3JDK21 internal composite representation: never copies existing atoms.
 */
package jdk.internal.m3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;

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

    // Prepared literal search: target-owned adaptation of the KMP recurrence in
    // M3StringSearchPrecompute (Hitesh Soliwal, GPLv2 + Classpath exception).
    // BytePatternSearch supplied the bounded/cancellable byte-search comparison;
    // no Apache-licensed source expression or native buffer-copy path is imported.
    public static final int MAX_LITERAL_BYTES = 1 << 20;
    private static final int LITERAL_POLL_WORK = 4096;
    private static final int LITERAL_STREAM_BUFFER_BYTES = 1 << 16;

    /**
     * Snapshots an exact byte literal and prepares its KMP prefix table.
     * An empty literal matches every boundary, including the terminal boundary.
     * Preparation retains five primitive bytes per pattern byte, excluding
     * array and object headers. It never retains source text or a source stream.
     */
    public static PreparedLiteral prepareLiteral(byte[] needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.length > MAX_LITERAL_BYTES) {
            throw new IllegalArgumentException("literal exceeds 1048576 bytes");
        }
        checkLiteralInterrupted();
        return new PreparedLiteral(needle.clone());
    }

    /** Immutable reusable byte-search metadata; no decoding or regex semantics. */
    public static final class PreparedLiteral {
        private final byte[] needle;
        private final int[] prefix;

        private PreparedLiteral(byte[] owned) {
            needle = owned;
            prefix = new int[owned.length];
            int work = LITERAL_POLL_WORK;
            for (int at = 1; at < owned.length; at++) {
                if (--work == 0) {
                    work = LITERAL_POLL_WORK;
                    checkLiteralInterrupted();
                }
                int matched = prefix[at - 1];
                while (matched > 0 && owned[at] != owned[matched]) {
                    matched = prefix[matched - 1];
                    if (--work == 0) {
                        work = LITERAL_POLL_WORK;
                        checkLiteralInterrupted();
                    }
                }
                if (owned[at] == owned[matched]) matched++;
                prefix[at] = matched;
            }
            checkLiteralInterrupted();
        }

        public int byteLength() {
            return needle.length;
        }

        /**
         * Searches a caller-owned stream from its current position, labelled
         * {@code fromIndex}, through EOF or the exclusive {@code toIndex} limit.
         * The caller supplies that coordinate; this method does not seek or skip.
         * Both coordinates must satisfy {@code 0 <= fromIndex <= toIndex}.
         *
         * <p>The returned cursor owns at most 64 KiB of read scratch and never
         * closes the stream. Reads may advance the stream beyond a returned hit;
         * continue through the same cursor to preserve its unread buffered bytes.
         * Each cursor is confined to one caller. Stream reads occur without any
         * composite or cursor monitor held. Interruption is polled between reads
         * and bounded units of scanning work; a blocking read is interruptible
         * only to the extent supported by that stream implementation.
         */
        public StreamMatches streamMatches(InputStream input, long fromIndex,
                                            long toIndex, boolean overlapping) {
            return new StreamMatches(this, input, fromIndex, toIndex, overlapping);
        }
    }

    /** First matching absolute byte offset in {@code [fromIndex, length)}, or -1. */
    public synchronized long indexOf(PreparedLiteral literal, long fromIndex) {
        return indexOf(literal, fromIndex, length);
    }

    /**
     * First matching absolute byte offset in the strict selected range, or -1.
     * An empty literal returns fromIndex. No backing bytes are flattened or copied.
     */
    public synchronized long indexOf(PreparedLiteral literal, long fromIndex, long toIndex) {
        return matches(literal, fromIndex, toIndex, false).next();
    }

    public synchronized LiteralMatches matches(PreparedLiteral literal, long fromIndex,
                                                boolean overlapping) {
        return matches(literal, fromIndex, length, overlapping);
    }

    /**
     * Prepares a single forward traversal of the selected immutable byte range.
     * Empty literals enumerate every boundary from fromIndex through toIndex.
     * All cursor operations use this source's monitor and require it to be open,
     * including after cursor exhaustion. Closing the source invalidates cursors.
     */
    public synchronized LiteralMatches matches(PreparedLiteral literal, long fromIndex,
                                                long toIndex, boolean overlapping) {
        requireOpen();
        Objects.requireNonNull(literal, "literal");
        Objects.checkFromToIndex(fromIndex, toIndex, length);
        checkLiteralInterrupted();
        return new LiteralMatches(this, literal, fromIndex, toIndex, overlapping);
    }

    /** Stateful single-pass hits over borrowed read-only piece views. */
    public static final class LiteralMatches {
        private final M3CompositeBytes source;
        private final LiteralState state;
        private final long toIndex;
        private int piece;
        private int offset;
        private int pieceLimit;
        private ByteBuffer buffer;
        private boolean done;

        private LiteralMatches(M3CompositeBytes source, PreparedLiteral literal,
                               long fromIndex, long toIndex, boolean overlapping) {
            this.source = source;
            this.state = new LiteralState(literal, fromIndex, overlapping);
            this.toIndex = toIndex;
            if (literal.needle.length > toIndex - fromIndex) {
                done = true;
            } else if (literal.needle.length != 0) {
                piece = source.locate(fromIndex);
            }
        }

        /**
         * Returns the next absolute byte offset, or -1 after exhaustion.
         * Cancellation preserves the interrupt flag and a resumable frontier;
         * after the caller clears the flag, next() continues without rescanning.
         * The source must remain open. No user callbacks run under its monitor.
         */
        public long next() {
            synchronized (source) {
                source.requireOpen();
                checkLiteralInterrupted();
                if (done) return -1;
                if (state.literal.needle.length == 0) {
                    long boundary = state.position;
                    if (boundary == toIndex) done = true;
                    else state.position++;
                    return boundary;
                }
                while (state.position < toIndex) {
                    borrowPiece();
                    long hit = state.accept(buffer.get(offset));
                    offset++;
                    if (hit >= 0) return hit;
                }
                done = true;
                buffer = null;
                return -1;
            }
        }

        private void borrowPiece() {
            if (buffer != null && offset < pieceLimit) return;
            buffer = null;
            while (source.ends[piece] <= state.position) {
                piece++;
                state.pollWork();
            }
            long start = piece == 0 ? 0 : source.ends[piece - 1];
            int local = (int) (state.position - start);
            offset = source.byteOffsets[piece] + local;
            int remaining = (int) Math.min(source.byteLengths[piece] - local,
                    toIndex - state.position);
            pieceLimit = offset + remaining;
            buffer = source.pool.asReadOnlyBuffer(source.handles[piece]);
        }
    }

    /**
     * Single-caller stream continuation with bounded scratch and long offsets.
     * Ordinary I/O exceptions propagate with the stream's own consumption
     * semantics. InterruptedIOException preserves its valid transferred bytes
     * in the cursor and restores the interrupt flag before propagation.
     */
    public static final class StreamMatches {
        private final InputStream input;
        private final LiteralState state;
        private final long toIndex;
        private byte[] buffer;
        private int offset;
        private int limit;
        private boolean emptyBoundaryPending;
        private boolean done;

        private StreamMatches(PreparedLiteral literal, InputStream input,
                              long fromIndex, long toIndex, boolean overlapping) {
            this.input = Objects.requireNonNull(input, "input");
            Objects.checkFromToIndex(fromIndex, toIndex, Long.MAX_VALUE);
            checkLiteralInterrupted();
            this.state = new LiteralState(literal, fromIndex, overlapping);
            this.toIndex = toIndex;
            emptyBoundaryPending = literal.needle.length == 0;
            done = literal.needle.length > toIndex - fromIndex;
        }

        /**
         * Returns the next hit or -1 at EOF/range exhaustion. Empty needles emit
         * the initial boundary without reading, then one boundary per byte read.
         * A nonempty read returning zero is refused with IOException, preventing
         * an unbounded retry loop. Positive short reads are handled normally.
         */
        public long next() throws IOException {
            checkLiteralInterrupted();
            if (done) return -1;
            if (emptyBoundaryPending) {
                emptyBoundaryPending = false;
                return state.position;
            }
            while (state.position < toIndex) {
                if (!ensureBuffered()) {
                    done = true;
                    return -1;
                }
                if (state.literal.needle.length == 0) {
                    offset++;
                    return ++state.position;
                }
                long hit = state.accept(buffer[offset]);
                offset++;
                if (hit >= 0) return hit;
            }
            done = true;
            buffer = null;
            return -1;
        }

        private boolean ensureBuffered() throws IOException {
            if (offset < limit) return true;
            checkLiteralInterrupted();
            int requested = (int) Math.min(LITERAL_STREAM_BUFFER_BYTES,
                    toIndex - state.position);
            if (buffer == null) buffer = new byte[requested];
            offset = 0;
            limit = 0;
            final int count;
            try {
                count = input.read(buffer, 0, requested);
            } catch (InterruptedIOException interrupted) {
                if (interrupted.bytesTransferred >= 0
                        && interrupted.bytesTransferred <= requested) {
                    limit = interrupted.bytesTransferred;
                }
                Thread.currentThread().interrupt();
                throw interrupted;
            }
            if (count < -1 || count > requested || count == 0) {
                throw new IOException("invalid progress from input stream read");
            }
            if (count == -1) {
                checkLiteralInterrupted();
                return false;
            }
            limit = count;
            checkLiteralInterrupted();
            return true;
        }
    }

    /** One KMP continuation shared by the resident and bounded-stream routes. */
    private static final class LiteralState {
        private final PreparedLiteral literal;
        private final boolean overlapping;
        private long position;
        private int matched;
        private int remainingWork = LITERAL_POLL_WORK;

        private LiteralState(PreparedLiteral literal, long position, boolean overlapping) {
            this.literal = literal;
            this.position = position;
            this.overlapping = overlapping;
        }

        private long accept(byte value) {
            pollWork();
            while (matched > 0 && value != literal.needle[matched]) {
                matched = literal.prefix[matched - 1];
                // A cancelled fallback leaves this byte unconsumed. Frontiers
                // already disproved by it need not be revisited after resuming.
                pollWork();
            }
            if (value == literal.needle[matched]) matched++;
            position++;
            if (matched == literal.needle.length) {
                long hit = position - literal.needle.length;
                matched = overlapping ? literal.prefix[matched - 1] : 0;
                return hit;
            }
            return -1;
        }

        private void pollWork() {
            if (--remainingWork == 0) {
                remainingWork = LITERAL_POLL_WORK;
                checkLiteralInterrupted();
            }
        }
    }

    private static void checkLiteralInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("M3 literal search interrupted");
        }
    }

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
