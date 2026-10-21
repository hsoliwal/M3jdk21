/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        try (MFileStreams.Input input = openInputStream(progress)) {
            return input.transferTo(destination);
        }
    }

}
