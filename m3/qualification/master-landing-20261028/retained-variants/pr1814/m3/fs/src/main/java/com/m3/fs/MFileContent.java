/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Provider content boundary with reusable default operations. Implementations own
 * their access and capability policy; these defaults own only the streams they
 * open. Progress is borrowed, including a caller-created child monitor bridged
 * through {@link M3FsProgress}. Closing a stream never marks that child done.
 */
@FunctionalInterface
public interface MFileContent {
    /** Opens a caller-owned input according to the implementing provider's lifetime. */
    InputStream openInputStream() throws IOException;

    /**
     * Opens a caller-owned output when supported. The default is explicitly
     * read-only. Append/truncate behavior belongs to the implementing provider.
     */
    default OutputStream openOutputStream(boolean append) throws IOException {
        throw new IOException("This content provider does not support output");
    }

    /** Opens replacing output, retaining any progress/shadow configuration on this view. */
    default OutputStream openOutputStream() throws IOException {
        return openOutputStream(false);
    }

    /** Opens replacing output with an explicit borrowed monitor. */
    default MFileStreams.Output openOutputStream(M3FsProgress progress) throws IOException {
        return openOutputStream(false, progress);
    }

    /**
     * Samples provider metadata when supported. Implementations that support
     * capture must describe the same selected content layer their input opens at
     * offset zero. This operation does not assert stability during a later read.
     */
    default MFileObservation observation() throws IOException {
        throw new IOException("This content provider does not support metadata observation");
    }

    /**
     * Returns a lazy view that applies borrowed progress to every input/output it
     * opens. Repeated calls replace the bound monitor. An explicit per-open
     * monitor overrides it; neither form completes a caller-owned child monitor.
     */
    default MFileContent progressify(M3FsProgress progress) {
        return MFileContentView.withProgress(this, Objects.requireNonNull(progress, "progress"));
    }

    /** Appends a borrowed synchronous shadow without opening or reading content. */
    default MFileContent shadow(MFileShadow shadow) {
        return MFileContentView.withShadow(this, null, Objects.requireNonNull(shadow, "shadow"));
    }

    /**
     * Appends a borrowed shadow and sets the byte-domain label for future opens.
     * A later explicit per-open label overrides it. Labels do not convert offsets
     * across decoding or prove that an arbitrary stream represents a whole file.
     */
    default MFileContent shadow(String layer, MFileShadow shadow) {
        MFileStreams.validate(M3FsProgress.noop(), layer);
        return MFileContentView.withShadow(this, layer, Objects.requireNonNull(shadow, "shadow"));
    }

    /**
     * Appends a consumer of successfully completed provider input captures. Each
     * input samples {@link #observation()} once immediately before its raw open.
     * The provider must open the selected content at offset zero. The consumer
     * runs after input EOF, clean primary close and all configured/per-open shadow
     * completions. A size conflict is a checked failure and emits no capture.
     * Early close, failed input and output streams never emit captures.
     *
     * <p>Repeated consumers run in declaration order and fail fast. Their side
     * effects are not transactional. Captured metadata and bytes remain separate
     * observations; their association does not establish source stability.</p>
     */
    default MFileContent captureReads(Consumer<? super MFileCapture> consumer) {
        return MFileContentView.withCapture(this, Objects.requireNonNull(consumer, "consumer"));
    }

    /** Opens observed input with borrowed progress, validating before provider I/O. */
    default MFileStreams.Input openInputStream(M3FsProgress progress) throws IOException {
        return openInputStream(progress, null, "input");
    }

    /**
     * Opens observed input and a synchronous caller-owned shadow sink. Each layer
     * counts its own bytes, independently of decoder/parser lookahead or commits.
     */
    default MFileStreams.Input openInputStream(M3FsProgress progress,
            OutputStream shadow, String layer) throws IOException {
        MFileStreams.validate(progress, layer);
        progress.checkCanceled();
        InputStream source = openInputStream();
        try {
            return MFileStreams.input(source, progress, shadow, layer);
        } catch (RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(source, failure);
            throw failure;
        }
    }

    /** Opens observed output with borrowed progress; writing remains explicit. */
    default MFileStreams.Output openOutputStream(boolean append, M3FsProgress progress) throws IOException {
        return openOutputStream(append, progress, null, "output");
    }

    /**
     * Opens observed output with a caller-owned shadow. Its final digest identifies
     * successful writes through this stream, not a full destination file snapshot.
     */
    default MFileStreams.Output openOutputStream(boolean append, M3FsProgress progress,
            OutputStream shadow, String layer) throws IOException {
        MFileStreams.validate(progress, layer);
        progress.checkCanceled();
        OutputStream destination = openOutputStream(append);
        try {
            return MFileStreams.output(destination, progress, shadow, layer);
        } catch (RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(destination, failure);
            throw failure;
        }
    }

    /**
     * Transfers one newly opened input to a borrowed output without flushing or
     * closing that output. Progress reports consumed input bytes, which may exceed
     * confirmed output bytes if the output throws. The input always closes.
     */
    default long transferTo(OutputStream destination, M3FsProgress progress) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(progress, "progress");
        return transferAndCloseInput(openInputStream(progress), destination);
    }

    /**
     * Transfers to a borrowed output, retaining this view's progress and shadows.
     * The newly opened input closes; the supplied output is neither flushed nor closed.
     */
    default long transferTo(OutputStream destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        return transferAndCloseInput(openInputStream(), destination);
    }

    /**
     * Reads complete content within an explicit byte limit. Exceeding the limit
     * throws rather than returning truncated content. At most one byte beyond
     * the limit is consumed to distinguish EOF from excess. The input always
     * closes; configured progress, shadows and successful-read capture remain active.
     * Returned arrays are ordinary caller-owned materializations.
     */
    default byte[] readAllBytes(int maxBytes) throws IOException {
        checkLimit(maxBytes);
        return readBoundedAndClose(openInputStream(), maxBytes);
    }

    /** Reads bounded complete content with an explicit borrowed monitor. */
    default byte[] readAllBytes(int maxBytes, M3FsProgress progress) throws IOException {
        checkLimit(maxBytes);
        Objects.requireNonNull(progress, "progress");
        return readBoundedAndClose(openInputStream(progress), maxBytes);
    }

    /**
     * Decodes bounded complete content using {@link String#String(byte[], Charset)}
     * replacement semantics for malformed input. The limit counts encoded bytes.
     */
    default String readString(int maxBytes, Charset charset) throws IOException {
        Objects.requireNonNull(charset, "charset");
        return new String(readAllBytes(maxBytes), charset);
    }

    /** Decodes bounded content with an explicit borrowed monitor. */
    default String readString(int maxBytes, Charset charset, M3FsProgress progress) throws IOException {
        Objects.requireNonNull(charset, "charset");
        return new String(readAllBytes(maxBytes, progress), charset);
    }

    /** Replaces content with the supplied bytes and closes the newly opened output. */
    default void write(byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        write(bytes, 0, bytes.length, false);
    }

    /** Replaces content using an explicit borrowed monitor. */
    default void write(byte[] bytes, M3FsProgress progress) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        write(bytes, 0, bytes.length, false, progress);
    }

    /** Appends bytes when the provider supports append, retaining configured observations. */
    default void append(byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        write(bytes, 0, bytes.length, true);
    }

    /** Appends bytes using an explicit borrowed monitor. */
    default void append(byte[] bytes, M3FsProgress progress) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        write(bytes, 0, bytes.length, true, progress);
    }

    /**
     * Writes a validated array region through one provider output. Arguments are
     * checked before an output can be opened or truncated. A zero-length replacing
     * write still opens replacing output. Provider effects before a failure are
     * not rolled back. The caller must not modify the region during this call.
     */
    default void write(byte[] bytes, int offset, int length, boolean append) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        writeAndClose(openOutputStream(append), bytes, offset, length);
    }

    /** Writes an array region with explicit progress over successfully written bytes. */
    default void write(byte[] bytes, int offset, int length, boolean append,
            M3FsProgress progress) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        Objects.requireNonNull(progress, "progress");
        writeAndClose(openOutputStream(append, progress), bytes, offset, length);
    }

    /** Replaces content from a borrowed input; the input is never closed. */
    default long transferFrom(InputStream source) throws IOException {
        return transferFrom(source, false);
    }

    /** Replaces content from a borrowed input with explicit output-byte progress. */
    default long transferFrom(InputStream source, M3FsProgress progress) throws IOException {
        return transferFrom(source, false, progress);
    }

    /**
     * Transfers a borrowed input through one newly opened output. The output
     * closes on every exit. Existing view progress/shadows remain active. Failure
     * can leave partial content under the provider's own write contract; this is
     * not a transaction and does not protect against caller-selected source aliases.
     */
    default long transferFrom(InputStream source, boolean append) throws IOException {
        Objects.requireNonNull(source, "source");
        return transferAndCloseOutput(source, openOutputStream(append));
    }

    /**
     * Transfers with progress over confirmed output bytes. Cancellation is
     * cooperative at output boundaries; it cannot interrupt a blocking provider
     * read. The supplied input and monitor remain caller-owned.
     */
    default long transferFrom(InputStream source, boolean append,
            M3FsProgress progress) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(progress, "progress");
        return transferAndCloseOutput(source, openOutputStream(append, progress));
    }

    private static void checkLimit(int maxBytes) {
        if (maxBytes < 0) throw new IllegalArgumentException("maxBytes must not be negative");
    }

    // Providers may reuse one failure for I/O and close. Preserve that primary
    // failure instead of attempting Throwable self-suppression during cleanup.
    private static byte[] readBoundedAndClose(InputStream input, int maxBytes) throws IOException {
        final byte[] bytes;
        try {
            bytes = input.readNBytes(maxBytes);
            if (input.read() != -1) throw new IOException("Content exceeds byte limit: " + maxBytes);
        } catch (IOException | RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(input, failure);
            throw failure;
        }
        input.close();
        return bytes;
    }

    private static void writeAndClose(OutputStream output, byte[] bytes, int offset, int length)
            throws IOException {
        try {
            output.write(bytes, offset, length);
        } catch (IOException | RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(output, failure);
            throw failure;
        }
        output.close();
    }

    private static long transferAndCloseInput(InputStream input, OutputStream destination)
            throws IOException {
        long count = 0;
        try {
            // InputStream overrides may flush the destination. This boundary
            // promises a borrowed output with no flush, so own the copy loop.
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) != -1;) {
                destination.write(buffer, 0, read);
                count += read;
            }
        } catch (IOException | RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(input, failure);
            throw failure;
        }
        input.close();
        return count;
    }

    private static long transferAndCloseOutput(InputStream source, OutputStream output)
            throws IOException {
        final long count;
        try {
            count = source.transferTo(output);
        } catch (IOException | RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(output, failure);
            throw failure;
        }
        output.close();
        return count;
    }

}
