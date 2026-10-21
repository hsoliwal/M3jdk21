/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;

/** Immutable, flattened configuration; each provider open owns one stream state. */
final class MFileContentView implements MFileContent {
    private final MFileContent source;
    private final M3FsProgress progress;
    private final MFileShadow shadow;
    private final String layer;
    private final Consumer<? super MFileCapture> captureConsumer;

    private MFileContentView(MFileContent source, M3FsProgress progress,
            MFileShadow shadow, String layer, Consumer<? super MFileCapture> captureConsumer) {
        this.source = source;
        this.progress = progress;
        this.shadow = shadow;
        this.layer = layer;
        this.captureConsumer = captureConsumer;
    }

    private static MFileContentView view(MFileContent content) {
        if (content instanceof MFileContentView configured) return configured;
        return new MFileContentView(content, M3FsProgress.noop(), null, null, null);
    }

    static MFileContent withProgress(MFileContent content, M3FsProgress progress) {
        MFileContentView previous = view(content);
        return new MFileContentView(previous.source, progress, previous.shadow,
                previous.layer, previous.captureConsumer);
    }

    static MFileContent withShadow(MFileContent content, String layer, MFileShadow shadow) {
        MFileContentView previous = view(content);
        return new MFileContentView(previous.source, previous.progress,
                MFileStreams.combineShadows(previous.shadow, shadow),
                layer == null ? previous.layer : layer, previous.captureConsumer);
    }

    static MFileContent withCapture(MFileContent content, Consumer<? super MFileCapture> consumer) {
        MFileContentView previous = view(content);
        Consumer<? super MFileCapture> first = previous.captureConsumer;
        Consumer<? super MFileCapture> combined = first == null ? consumer : capture -> {
            first.accept(capture);
            consumer.accept(capture);
        };
        return new MFileContentView(previous.source, previous.progress,
                previous.shadow, previous.layer, combined);
    }

    @Override public MFileObservation observation() throws IOException { return source.observation(); }

    @Override public MFileStreams.Input openInputStream() throws IOException {
        return openInput(progress, null, inputLayer());
    }

    @Override public MFileStreams.Input openInputStream(M3FsProgress explicitProgress) throws IOException {
        return openInput(Objects.requireNonNull(explicitProgress, "progress"), null, inputLayer());
    }

    @Override public MFileStreams.Input openInputStream(M3FsProgress explicitProgress,
            OutputStream explicitShadow, String explicitLayer) throws IOException {
        return openInput(explicitProgress, explicitShadow, explicitLayer);
    }

    private MFileStreams.Input openInput(M3FsProgress selectedProgress,
            OutputStream explicitShadow, String selectedLayer) throws IOException {
        MFileStreams.validate(selectedProgress, selectedLayer);
        selectedProgress.checkCanceled();
        MFileShadow selectedShadow = shadowsWith(explicitShadow);
        if (captureConsumer != null) {
            MFileObservation sampled = source.observation();
            long capturedAtMillis = System.currentTimeMillis();
            MFileShadow capture = new MFileShadow() {
                @Override public void onBytes(byte[] bytes, int offset, int length) { }
                @Override public void onComplete(MFileStreams.Completion result) throws IOException {
                    captureConsumer.accept(MFileCapture.fromInput(sampled, capturedAtMillis, result));
                }
            };
            selectedShadow = MFileStreams.combineShadows(selectedShadow, capture);
        }
        InputStream input = source.openInputStream();
        try {
            return MFileStreams.observingInput(input, selectedProgress, selectedShadow, selectedLayer);
        } catch (RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(input, failure);
            throw failure;
        }
    }

    @Override public MFileStreams.Output openOutputStream(boolean append) throws IOException {
        return openOutput(append, progress, null, outputLayer());
    }

    @Override public MFileStreams.Output openOutputStream(boolean append,
            M3FsProgress explicitProgress) throws IOException {
        return openOutput(append, Objects.requireNonNull(explicitProgress, "progress"), null, outputLayer());
    }

    @Override public MFileStreams.Output openOutputStream(boolean append,
            M3FsProgress explicitProgress, OutputStream explicitShadow, String explicitLayer) throws IOException {
        return openOutput(append, explicitProgress, explicitShadow, explicitLayer);
    }

    private MFileStreams.Output openOutput(boolean append, M3FsProgress selectedProgress,
            OutputStream explicitShadow, String selectedLayer) throws IOException {
        MFileStreams.validate(selectedProgress, selectedLayer);
        selectedProgress.checkCanceled();
        MFileShadow selectedShadow = shadowsWith(explicitShadow);
        OutputStream output = source.openOutputStream(append);
        try {
            return MFileStreams.observingOutput(output, selectedProgress, selectedShadow, selectedLayer);
        } catch (RuntimeException | Error failure) {
            MFileStreams.closeAfterFailure(output, failure);
            throw failure;
        }
    }

    private String inputLayer() { return layer == null ? "input" : layer; }
    private String outputLayer() { return layer == null ? "output" : layer; }
    private MFileShadow shadowsWith(OutputStream explicitShadow) {
        return MFileStreams.combineShadows(shadow,
                explicitShadow == null ? null : MFileShadow.of(explicitShadow));
    }
}
