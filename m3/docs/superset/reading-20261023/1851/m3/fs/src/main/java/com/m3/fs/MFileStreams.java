/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Pull-driven byte observations for provider, decoder and parser input layers.
 * A shadow receives only bytes that its primary stream has actually read, or
 * bytes from a write that returned successfully. There is no independent reader,
 * read-ahead worker, replay buffer, retained payload or parser in this helper.
 *
 * <p>Each wrapper owns its primary stream and borrows its progress callbacks and
 * optional shadow output. It never flushes or closes that shadow. Wrappers are
 * intended for one sequential consumer. Layer labels name independent byte
 * domains; they do not translate offsets across compression, character decoding,
 * parser lookahead or committed token/AST positions.</p>
 */
public final class MFileStreams {
    private MFileStreams() {
    }

    /** The lifecycle boundary that produced a completed byte observation. */
    public enum CompletionKind { INPUT_EOF, OUTPUT_SEQUENCE }

    /**
     * Immutable evidence delivered after successful primary cleanup. Input
     * evidence covers bytes from the supplied stream's initial position through
     * EOF; output evidence covers confirmed successful writes. Neither kind alone
     * asserts full-file coverage, source stability or durable storage.
     */
    public record Completion(String layer, long byteCount, M3ContentId contentHash,
            CompletionKind kind) {
        public Completion {
            Objects.requireNonNull(layer, "layer");
            if (layer.isEmpty()) throw new IllegalArgumentException("layer must not be empty");
            if (byteCount < 0) throw new IllegalArgumentException("byteCount must not be negative");
            Objects.requireNonNull(contentHash, "contentHash");
            Objects.requireNonNull(kind, "kind");
        }
    }

    /** Observes bytes from the current position of an owned input stream. */
    public static Input input(InputStream source, M3FsProgress progress) {
        return input(source, progress, null, "input");
    }

    /**
     * Observes the owned source and synchronously writes each consumed byte to a
     * caller-owned shadow, when supplied. Validation failure leaves source ownership
     * with the caller. Construction does not read, report work or check cancellation;
     * cancellation is checked at the next I/O operation.
     */
    public static Input input(InputStream source, M3FsProgress progress,
            OutputStream shadow, String layer) {
        return observingInput(source, progress, shadow == null ? null : MFileShadow.of(shadow), layer);
    }

    /**
     * Observes an owned existing input with a borrowed synchronous shadow SPI.
     * Construction performs no I/O. The completion hook waits for both EOF and
     * clean primary close; it never claims this input started at file offset zero.
     */
    public static Input observingInput(InputStream source, M3FsProgress progress,
            MFileShadow shadow, String layer) {
        validate(progress, layer);
        return new Input(Objects.requireNonNull(source, "source"), progress, shadow, layer);
    }

    /** Observes confirmed writes to an owned output stream. */
    public static Output output(OutputStream destination, M3FsProgress progress) {
        return output(destination, progress, null, "output");
    }

    /**
     * Observes writes after the owned destination returns successfully. The shadow
     * is caller-owned. A write that throws may have partially changed the primary;
     * it is not added to the confirmed count, hash or shadow, and observation fails.
     */
    public static Output output(OutputStream destination, M3FsProgress progress,
            OutputStream shadow, String layer) {
        validate(progress, layer);
        Objects.requireNonNull(destination, "destination");
        if (destination == shadow) {
            throw new IllegalArgumentException("The output cannot also be its own shadow");
        }
        return observingOutput(destination, progress, shadow == null ? null : MFileShadow.of(shadow), layer);
    }

    /**
     * Observes an owned existing output with a borrowed synchronous shadow SPI.
     * Completion follows successful primary flush and close. Recognized output
     * adapters, including composed adapters, may not point to the primary itself.
     */
    public static Output observingOutput(OutputStream destination, M3FsProgress progress,
            MFileShadow shadow, String layer) {
        validate(progress, layer);
        Objects.requireNonNull(destination, "destination");
        if (usesOutput(shadow, destination)) {
            throw new IllegalArgumentException("The output cannot also be its own shadow");
        }
        return new Output(destination, progress, shadow, layer);
    }

    static void validate(M3FsProgress progress, String layer) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(layer, "layer");
        if (layer.isEmpty()) {
            throw new IllegalArgumentException("layer must not be empty");
        }
    }

    static MFileShadow outputShadow(OutputStream sink) {
        return new OutputShadow(sink);
    }

    static MFileShadow combineShadows(MFileShadow first, MFileShadow second) {
        if (first == null) return second;
        if (second == null) return first;
        List<MFileShadow> observers = new ArrayList<>();
        appendShadows(observers, first);
        appendShadows(observers, second);
        return new CombinedShadow(List.copyOf(observers));
    }

    private static void appendShadows(List<MFileShadow> observers, MFileShadow shadow) {
        if (shadow instanceof CombinedShadow combined) observers.addAll(combined.observers);
        else observers.add(shadow);
    }

    private static boolean usesOutput(MFileShadow shadow, OutputStream primary) {
        if (shadow instanceof OutputShadow output) return output.sink == primary;
        if (shadow instanceof CombinedShadow combined) {
            for (MFileShadow observer : combined.observers) {
                if (usesOutput(observer, primary)) return true;
            }
        }
        return false;
    }

    static void closeAfterFailure(Closeable primary, Throwable failure) {
        if (primary == null) return;
        try {
            primary.close();
        } catch (IOException | RuntimeException | Error closing) {
            if (closing != failure) failure.addSuppressed(closing);
        }
    }

    private static final class OutputShadow implements MFileShadow {
        private final OutputStream sink;
        private OutputShadow(OutputStream sink) { this.sink = sink; }
        @Override public void onBytes(byte[] bytes, int offset, int length) throws IOException {
            sink.write(bytes, offset, length);
        }
    }

    private static final class CombinedShadow implements MFileShadow {
        private final List<MFileShadow> observers;
        private CombinedShadow(List<MFileShadow> observers) { this.observers = observers; }
        @Override public void onBytes(byte[] bytes, int offset, int length) throws IOException {
            for (MFileShadow observer : observers) observer.onBytes(bytes, offset, length);
        }
        @Override public void onComplete(Completion result) throws IOException {
            for (MFileShadow observer : observers) observer.onComplete(result);
        }
    }

    /**
     * Streams selected half-open source byte ranges from any MFileContent
     * provider into a borrowed output. This is the sequential fallback for
     * local, archive, database and remote providers that cannot seek. It uses
     * one input stream and 8 KiB scratch space; no file-wide payload, per-range
     * object or independent background reader is retained.
     *
     * <p>Offsets and lengths must be ordered and non-overlapping; maxEnd is
     * the greatest permitted source coordinate, including unread gaps. Gaps
     * are consumed, not written, and still count as input progress and shadows.
     * This method neither claims a full-content hash nor replays failed writes.
     * A failed borrowed destination may have received a partial prefix.
     * Caller retains destination ownership and must close/flush it.
     *
     * <p>Source arrays are copied before any provider work. If there are no
     * ranges, no provider is opened. Successful return reports transferred
     * bytes, not an atomic destination commit or stable source generation.
     */
    public static long transferRanges(MFileContent source, OutputStream destination,
            long[] offsets, long[] lengths, long maxEnd, M3FsProgress progress)
            throws IOException {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
        return transferRangesChecked(source, destination, offsets, lengths,
                maxEnd, null, progress);
    }

    /**
     * Reuse a caller-owned, exclusive 1..1 MiB scratch array over many serial
     * provider range transfers. Never retained, freed or separately read.
     * Callers must prevent concurrent reuse and buffer mutation by observers.
     * Provider/shadow progress, destination borrowing, and failure contracts
     * remain exactly those of the original transferRanges method.
     */
    public static long transferRanges(MFileContent source, OutputStream destination,
            long[] offsets, long[] lengths, long maxEnd, byte[] scratch,
            M3FsProgress progress) throws IOException {
        Objects.requireNonNull(scratch, "scratch");
        if (scratch.length < 1 || scratch.length > (1 << 20)) {
            throw new IllegalArgumentException("scratch length must be 1..1048576");
        }
        return transferRangesChecked(source, destination, offsets, lengths,
                maxEnd, scratch, progress);
    }

    /** One original range-transfer behavioral atom for both public entrypoints. */
    private static long transferRangesChecked(MFileContent source, OutputStream destination,
            long[] offsets, long[] lengths, long maxEnd, byte[] borrowedScratch,
            M3FsProgress progress) throws IOException {
>>>>>>> e8783daf526a9f4d305aad96ec6c0309d1006820
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        Objects.requireNonNull(progress, "progress");
        if (maxEnd < 0 || offsets.length != lengths.length) {
            throw new IllegalArgumentException("invalid range column geometry");
        }
        long[] selected = offsets.clone();
        long[] extents = lengths.clone();
        long previousEnd = 0;
        for (int row = 0; row < selected.length; row++) {
            long start = selected[row], length = extents[row];
            if (start < previousEnd || length < 0 || start > maxEnd
                    || length > maxEnd - start) {
                throw new IllegalArgumentException("ranges must be ordered, disjoint and bounded");
            }
            previousEnd = start + length;
        }
        if (selected.length == 0) return 0;
        progress.checkCanceled();
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        byte[] scratch = new byte[8192];
=======
        byte[] scratch = borrowedScratch == null ? new byte[8192] : borrowedScratch;
>>>>>>> e8783daf526a9f4d305aad96ec6c0309d1006820
        long sourcePosition = 0;
        long written = 0;
        try (Input input = source.openInputStream(progress)) {
            for (int row = 0; row < selected.length; row++) {
                long gap = selected[row] - sourcePosition;
                while (gap > 0) {
                    int amount = input.read(scratch, 0, (int) Math.min(scratch.length, gap));
                    if (amount < 0) throw new EOFException("source ended before selected range");
                    // Input itself rejects a zero-progress or invalid provider result.
                    sourcePosition += amount;
                    gap -= amount;
                }
                long remaining = extents[row];
                while (remaining > 0) {
                    int amount = input.read(scratch, 0, (int) Math.min(scratch.length, remaining));
                    if (amount < 0) throw new EOFException("source shortened in selected range");
                    destination.write(scratch, 0, amount);
                    sourcePosition += amount;
                    written += amount;
                    remaining -= amount;
                }
            }
        }
        return written;
    }

    /**
     * A primary input with synchronous observation. The count and digest start at
     * the supplied stream's current position, which may be an interior file range.
     * A complete hash therefore does not by itself prove a whole-file snapshot.
     * Reading EOF seals the digest; an early close leaves it incomplete. Skipping
     * reads through the wrapper so skipped bytes reach the shadow and digest.
     * Mark/reset are unsupported because they would replay an observation.
     */
    public static final class Input extends InputStream {
        private final InputStream source;
        private final State state;
        private final byte[] single = new byte[1];
        private byte[] skipBuffer;

        private Input(InputStream source, M3FsProgress progress, MFileShadow shadow, String layer) {
            this.source = source;
            this.state = new State(source, progress, shadow, layer);
        }

        /** The caller's byte-domain label; this is not a parser cursor. */
        public String layer() { return state.layer; }
        /** Bytes consumed from the primary, including read-through skips. */
        public long position() { return state.position; }
        /** Whether EOF was observed without a subsequent observation/close failure. */
        public boolean complete() { return state.complete; }
        /** Whether I/O, cancellation, shadow, progress or cleanup failed. */
        public boolean failed() { return state.failed; }
        /** Exact digest of the observed input, available only after successful EOF. */
        public Optional<M3ContentId> contentHash() { return Optional.ofNullable(state.hash); }

        @Override
        public int read() throws IOException {
            state.requireOpen();
            if (state.complete) {
                return -1;
            }
            try {
                state.progress.checkCanceled();
                int value = source.read();
                if (value < -1 || value > 255) {
                    throw new IOException("Input provider returned invalid byte: " + value);
                }
                if (value < 0) {
                    state.finish();
                } else {
                    single[0] = (byte) value;
                    state.record(single, 0, 1);
                }
                return value;
            } catch (IOException | RuntimeException | Error failure) {
                state.fail(failure);
                throw failure;
            }
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            state.requireOpen();
            if (length == 0) {
                return 0;
            }
            if (state.complete) {
                return -1;
            }
            try {
                state.progress.checkCanceled();
                int amount = source.read(bytes, offset, length);
                if (amount < -1 || amount == 0 || amount > length) {
                    throw new IOException("Input provider returned invalid byte count: " + amount);
                }
                if (amount < 0) {
                    state.finish();
                } else if (amount > 0) {
                    state.record(bytes, offset, amount);
                }
                return amount;
            } catch (IOException | RuntimeException | Error failure) {
                state.fail(failure);
                throw failure;
            }
        }

        @Override
        public long skip(long amount) throws IOException {
            state.requireOpen();
            if (amount <= 0) {
                return 0;
            }
            if (skipBuffer == null) {
                skipBuffer = new byte[8192];
            }
            long skipped = 0;
            while (skipped < amount) {
                int count = read(skipBuffer, 0, (int) Math.min(skipBuffer.length, amount - skipped));
                if (count < 0) {
                    break;
                }
                if (count == 0) {
                    if (read() < 0) {
                        break;
                    }
                    count = 1;
                }
                skipped += count;
            }
            return skipped;
        }

        @Override
        public int available() throws IOException {
            state.requireOpen();
            try {
                state.progress.checkCanceled();
                return state.complete ? 0 : source.available();
            } catch (IOException | RuntimeException | Error failure) {
                state.fail(failure);
                throw failure;
            }
        }

        @Override public boolean markSupported() { return false; }
        @Override public void mark(int readLimit) { }
        @Override public void reset() throws IOException {
            throw new IOException("Observed inputs do not support mark/reset");
        }
        @Override public void close() throws IOException { state.close(false); }
    }

    /**
     * An owned output with synchronous observation of confirmed write calls.
     * Its digest becomes available only after successful flush and close of the
     * primary. It identifies the write sequence, not the destination's full bytes:
     * append mode, previous data, external writes and failed partial writes matter.
     */
    public static final class Output extends OutputStream {
        private final OutputStream destination;
        private final State state;
        private final byte[] single = new byte[1];

        private Output(OutputStream destination, M3FsProgress progress, MFileShadow shadow, String layer) {
            this.destination = destination;
            this.state = new State(destination, progress, shadow, layer);
        }

        /** The caller's write-byte domain label. */
        public String layer() { return state.layer; }
        /** Bytes from primary write calls that returned successfully. */
        public long position() { return state.position; }
        /** Whether the complete write sequence was successfully closed. */
        public boolean complete() { return state.complete; }
        /** Whether I/O, cancellation, shadow, progress or cleanup failed. */
        public boolean failed() { return state.failed; }
        /** Digest of confirmed writes after successful close, not a destination snapshot. */
        public Optional<M3ContentId> contentHash() { return Optional.ofNullable(state.hash); }

        @Override
        public void write(int value) throws IOException {
            state.requireOpen();
            try {
                state.progress.checkCanceled();
                destination.write(value);
                single[0] = (byte) value;
                state.record(single, 0, 1);
            } catch (IOException | RuntimeException | Error failure) {
                state.fail(failure);
                throw failure;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            state.requireOpen();
            if (length == 0) {
                return;
            }
            try {
                state.progress.checkCanceled();
                destination.write(bytes, offset, length);
                state.record(bytes, offset, length);
            } catch (IOException | RuntimeException | Error failure) {
                state.fail(failure);
                throw failure;
            }
        }

        /** Flushes the primary only; the caller retains its shadow's buffering policy. */
        @Override
        public void flush() throws IOException {
            state.requireOpen();
            try {
                state.progress.checkCanceled();
                destination.flush();
            } catch (IOException | RuntimeException | Error failure) {
                state.fail(failure);
                throw failure;
            }
        }

        /** Flushes and closes the primary, without completing borrowed progress. */
        @Override public void close() throws IOException { state.close(true); }
    }

    private static final class State {
        private final Closeable primary;
        private final M3FsProgress progress;
        private final MFileShadow shadow;
        private final String layer;
        private final MessageDigest digest = M3ContentId.newDigest();
        private long position;
        private M3ContentId hash;
        private boolean complete;
        private boolean failed;
        private boolean closed;

        State(Closeable primary, M3FsProgress progress, MFileShadow shadow, String layer) {
            this.primary = primary;
            this.progress = progress;
            this.shadow = shadow;
            this.layer = layer;
        }

        void requireOpen() throws IOException {
            if (closed) {
                throw new IOException("The observed stream is closed");
            }
        }

        void record(byte[] bytes, int offset, int length) throws IOException {
            position = Math.addExact(position, length);
            digest.update(bytes, offset, length);
            if (shadow != null) {
                shadow.onBytes(bytes, offset, length);
            }
            progress.worked(length);
        }

        void finish() {
            if (!complete) {
                hash = M3ContentId.of(digest.digest());
                complete = true;
            }
        }

        void fail(Throwable primaryFailure) {
            failed = true;
            complete = false;
            hash = null;
            if (!closed) {
                closed = true;
                try {
                    primary.close();
                } catch (IOException | RuntimeException | Error closing) {
                    if (closing != primaryFailure) {
                        primaryFailure.addSuppressed(closing);
                    }
                }
            }
        }

        void close(boolean output) throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            Throwable failure = null;
            if (output) {
                try {
                    ((OutputStream) primary).flush();
                } catch (IOException | RuntimeException | Error flushing) {
                    failure = flushing;
                }
            }
            try {
                primary.close();
            } catch (IOException | RuntimeException | Error closing) {
                if (failure == null) {
                    failure = closing;
                } else if (failure != closing) {
                    failure.addSuppressed(closing);
                }
            }
            if (failure != null) {
                failed = true;
                complete = false;
                hash = null;
                if (failure instanceof IOException checked) {
                    throw checked;
                }
                if (failure instanceof RuntimeException unchecked) {
                    throw unchecked;
                }
                throw (Error) failure;
            }
            if (output || complete) {
                try {
                    progress.checkCanceled();
                    if (output) finish();
                    if (shadow != null) {
                        shadow.onComplete(new Completion(layer, position, hash,
                                output ? CompletionKind.OUTPUT_SEQUENCE : CompletionKind.INPUT_EOF));
                    }
                } catch (IOException | RuntimeException | Error completionFailure) {
                    fail(completionFailure);
                    throw completionFailure;
                }
            }
        }
    }
}
