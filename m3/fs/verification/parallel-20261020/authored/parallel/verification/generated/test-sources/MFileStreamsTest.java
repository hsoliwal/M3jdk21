/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

/** Behavioral contracts for real pull-driven byte observers and borrowed progress. */
class MFileStreamsTest {
    private static final byte[] DATA = "alpha-βeta-中文-0123456789".getBytes(StandardCharsets.UTF_8);

    @Test
    void mainReadsAloneAdvanceTheShadowAndHashIncludesSkippedBytes() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        TrackingOutput shadow = new TrackingOutput();
        AtomicLong progress = new AtomicLong();
        try (MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(() -> false, progress::addAndGet), shadow, "raw")) {
            assertEquals(0, source.reads);
            assertEquals(0, shadow.bytes.size());
            assertEquals(0, input.position());
            assertTrue(input.contentHash().isEmpty());
            assertEquals(DATA[0] & 0xff, input.read());
            byte[] next = new byte[5];
            assertEquals(5, input.read(next, 1, 3) + input.read(next, 0, 2));
            assertEquals(4, input.skip(4));
            input.readAllBytes();
            assertEquals(DATA.length, input.position());
            assertEquals(DATA.length, progress.get());
            assertArrayEquals(DATA, shadow.bytes.toByteArray());
            assertEquals(M3ContentId.digest(DATA), input.contentHash().orElseThrow());
            assertTrue(input.complete());
            assertFalse(input.failed());
            assertEquals("raw", input.layer());
            assertEquals(-1, input.read());
            assertEquals(DATA.length, progress.get());
        }
        assertEquals(1, source.closes);
        assertEquals(0, shadow.closes);
        assertEquals(0, shadow.flushes);
    }

    @Test
    void zeroLengthReadAndAvailabilityDoNotAdvanceAnything() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        AtomicLong progress = new AtomicLong();
        try (MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(() -> false, progress::addAndGet))) {
            assertEquals(0, input.read(new byte[4], 2, 0));
            assertEquals(DATA.length, input.available());
            assertEquals(0, input.skip(0));
            assertEquals(0, input.skip(-5));
            assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], 1, 1));
            assertEquals(0, input.position());
            assertEquals(0, source.reads);
            assertEquals(0, progress.get());
            assertFalse(input.markSupported());
            input.mark(10);
            assertThrows(IOException.class, input::reset);
            assertEquals(DATA[0] & 0xff, input.read());
        }
    }

    @Test
    void closingEarlyNeverClaimsACompleteHashOrFinishesBorrowedProgress() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        AtomicLong progress = new AtomicLong();
        MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(() -> false, progress::addAndGet));
        assertEquals(DATA[0] & 0xff, input.read());
        input.close();
        input.close();
        assertEquals(1, source.closes);
        assertEquals(1, progress.get());
        assertEquals(1, input.position());
        assertFalse(input.complete());
        assertFalse(input.failed());
        assertTrue(input.contentHash().isEmpty());
        assertThrows(IOException.class, input::read);
    }

    @Test
    void emptyStreamCompletesOnlyWhenEofIsObserved() throws Exception {
        AtomicLong progress = new AtomicLong();
        try (MFileStreams.Input input = MFileStreams.input(new ByteArrayInputStream(new byte[0]),
                M3FsProgress.of(() -> false, progress::addAndGet))) {
            assertFalse(input.complete());
            assertEquals(0, input.read(new byte[0]));
            assertFalse(input.complete());
            assertEquals(-1, input.read());
            assertTrue(input.complete());
            assertEquals(M3ContentId.digest(new byte[0]), input.contentHash().orElseThrow());
            assertEquals(0, progress.get());
        }
    }

    @Test
    void cancellationBeforeAReadClosesPrimaryWithoutAdvancingShadow() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        TrackingOutput shadow = new TrackingOutput();
        MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(() -> true, ignored -> fail("No work after cancellation")), shadow, "raw");
        assertThrows(CancellationException.class, input::read);
        assertEquals(1, source.closes);
        assertEquals(0, source.reads);
        assertEquals(0, shadow.bytes.size());
        assertEquals(0, shadow.closes);
        assertTrue(input.failed());
        assertTrue(input.contentHash().isEmpty());
        input.close();
        assertEquals(1, source.closes);
    }

    @Test
    void cancellationBetweenReadsLeavesOnlyObservedWork() throws Exception {
        AtomicBoolean canceled = new AtomicBoolean();
        AtomicLong progress = new AtomicLong();
        TrackingInput source = new TrackingInput(DATA);
        MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(canceled::get, progress::addAndGet));
        input.readNBytes(4);
        canceled.set(true);
        assertThrows(CancellationException.class, input::read);
        assertEquals(4, progress.get());
        assertEquals(4, input.position());
        assertFalse(input.complete());
        assertEquals(1, source.closes);
    }

    @Test
    void inputFailurePreservesPrimaryCauseAndSuppressesCloseFailure() {
        IOException primary = new IOException("read failed");
        IOException closing = new IOException("close failed");
        InputStream source = new InputStream() {
            @Override public int read() throws IOException { throw primary; }
            @Override public void close() throws IOException { throw closing; }
        };
        MFileStreams.Input input = MFileStreams.input(source, M3FsProgress.noop());
        IOException observed = assertThrows(IOException.class, input::read);
        assertSame(primary, observed);
        assertArrayEquals(new Throwable[] {closing}, observed.getSuppressed());
        assertTrue(input.failed());
        assertFalse(input.complete());
    }

    @Test
    void shadowFailureAfterAReadStopsThePrimaryAndDoesNotReturnAFullHash() {
        TrackingInput source = new TrackingInput(DATA);
        IOException expected = new IOException("shadow failed");
        OutputStream shadow = new OutputStream() {
            @Override public void write(int value) throws IOException { throw expected; }
        };
        MFileStreams.Input input = MFileStreams.input(source, M3FsProgress.noop(), shadow, "raw");
        assertSame(expected, assertThrows(IOException.class, input::read));
        assertEquals(1, source.closes);
        assertEquals(1, input.position());
        assertTrue(input.contentHash().isEmpty());
    }

    @Test
    void progressCallbackFailureAlsoClosesThePrimary() {
        TrackingInput source = new TrackingInput(DATA);
        IllegalStateException expected = new IllegalStateException("progress failed");
        MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(() -> false, ignored -> { throw expected; }));
        assertSame(expected, assertThrows(IllegalStateException.class, input::read));
        assertEquals(1, source.closes);
        assertTrue(input.failed());
    }

    @Test
    void outputShadowsFollowSuccessfulWritesAndFinalizeOnlyOnClose() throws Exception {
        TrackingOutput primary = new TrackingOutput();
        TrackingOutput shadow = new TrackingOutput();
        AtomicLong progress = new AtomicLong();
        MFileStreams.Output output = MFileStreams.output(primary,
                M3FsProgress.of(() -> false, progress::addAndGet), shadow, "encoded");
        assertEquals(0, primary.bytes.size());
        output.write(DATA[0]);
        output.write(DATA, 1, DATA.length - 1);
        output.write(DATA, 0, 0);
        assertArrayEquals(DATA, primary.bytes.toByteArray());
        assertArrayEquals(DATA, shadow.bytes.toByteArray());
        assertEquals(DATA.length, output.position());
        assertEquals(DATA.length, progress.get());
        assertEquals("encoded", output.layer());
        assertFalse(output.complete());
        assertTrue(output.contentHash().isEmpty());
        output.flush();
        assertEquals(1, primary.flushes);
        assertEquals(0, shadow.flushes);
        output.close();
        output.close();
        assertEquals(1, primary.closes);
        assertEquals(0, shadow.closes);
        assertEquals(M3ContentId.digest(DATA), output.contentHash().orElseThrow());
        assertTrue(output.complete());
        assertFalse(output.failed());
        assertThrows(IOException.class, () -> output.write(0));
    }

    @Test
    void partiallyFailedPrimaryWriteIsNeverReportedAsConfirmedShadowData() {
        IOException expected = new IOException("partial write");
        TrackingOutput primary = new TrackingOutput() {
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                super.write(bytes, offset, Math.min(2, length));
                throw expected;
            }
        };
        TrackingOutput shadow = new TrackingOutput();
        AtomicLong progress = new AtomicLong();
        MFileStreams.Output output = MFileStreams.output(primary,
                M3FsProgress.of(() -> false, progress::addAndGet), shadow, "raw-write");
        assertSame(expected, assertThrows(IOException.class, () -> output.write(DATA)));
        assertEquals(2, primary.bytes.size());
        assertEquals(0, shadow.bytes.size());
        assertEquals(0, progress.get());
        assertEquals(0, output.position());
        assertEquals(1, primary.closes);
        assertTrue(output.failed());
        assertTrue(output.contentHash().isEmpty());
    }

    @Test
    void outputShadowFailureClosesPrimaryAfterItsConfirmedWrite() {
        TrackingOutput primary = new TrackingOutput();
        IOException expected = new IOException("shadow write failed");
        OutputStream shadow = new OutputStream() {
            @Override public void write(int value) throws IOException { throw expected; }
        };
        MFileStreams.Output output = MFileStreams.output(primary, M3FsProgress.noop(), shadow, "raw-write");
        assertSame(expected, assertThrows(IOException.class, () -> output.write(7)));
        assertArrayEquals(new byte[] {7}, primary.bytes.toByteArray());
        assertEquals(1, output.position());
        assertEquals(1, primary.closes);
        assertTrue(output.contentHash().isEmpty());
    }

    @Test
    void canceledOutputDoesNotWriteButStillClosesPrimary() {
        TrackingOutput primary = new TrackingOutput();
        MFileStreams.Output output = MFileStreams.output(primary,
                M3FsProgress.of(() -> true, ignored -> fail("No progress after cancellation")));
        assertThrows(CancellationException.class, () -> output.write(1));
        assertEquals(0, primary.bytes.size());
        assertEquals(1, primary.closes);
        assertTrue(output.failed());
    }

    @Test
    void outputCloseAttemptsCloseEvenWhenFlushFails() {
        IOException flush = new IOException("flush failed");
        IllegalStateException close = new IllegalStateException("close failed");
        AtomicInteger closes = new AtomicInteger();
        OutputStream primary = new OutputStream() {
            @Override public void write(int value) { }
            @Override public void flush() throws IOException { throw flush; }
            @Override public void close() { closes.incrementAndGet(); throw close; }
        };
        MFileStreams.Output output = MFileStreams.output(primary, M3FsProgress.noop());
        assertSame(flush, assertThrows(IOException.class, output::close));
        assertArrayEquals(new Throwable[] {close}, flush.getSuppressed());
        assertEquals(1, closes.get());
        assertTrue(output.failed());
        assertFalse(output.complete());
        assertTrue(output.contentHash().isEmpty());
    }

    @Test
    void nestedGzipLayersKeepIndependentOffsetsHashesAndShadows() throws Exception {
        ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressedBytes)) { gzip.write(DATA); }
        byte[] compressed = compressedBytes.toByteArray();
        TrackingInput source = new TrackingInput(compressed);
        TrackingOutput rawShadow = new TrackingOutput();
        TrackingOutput decodedShadow = new TrackingOutput();
        AtomicLong rawWork = new AtomicLong();
        AtomicLong decodedWork = new AtomicLong();
        try (MFileStreams.Input raw = MFileStreams.input(source,
                M3FsProgress.of(() -> false, rawWork::addAndGet), rawShadow, "gzip-bytes");
                MFileStreams.Input decoded = MFileStreams.input(new GZIPInputStream(raw),
                M3FsProgress.of(() -> false, decodedWork::addAndGet), decodedShadow, "decoded-bytes")) {
            assertEquals(0, decoded.position());
            assertTrue(raw.position() > 0, "gzip construction legitimately consumes its own header");
            assertArrayEquals(DATA, decoded.readAllBytes());
            assertEquals(-1, raw.read(), "explicitly confirm the underlying raw EOF");
            assertEquals(compressed.length, raw.position());
            assertEquals(DATA.length, decoded.position());
            assertEquals(compressed.length, rawWork.get());
            assertEquals(DATA.length, decodedWork.get());
            assertArrayEquals(compressed, rawShadow.bytes.toByteArray());
            assertArrayEquals(DATA, decodedShadow.bytes.toByteArray());
            assertEquals(M3ContentId.digest(compressed), raw.contentHash().orElseThrow());
            assertEquals(M3ContentId.digest(DATA), decoded.contentHash().orElseThrow());
            assertNotEquals(raw.contentHash(), decoded.contentHash());
        }
        assertEquals(1, source.closes);
        assertEquals(0, rawShadow.closes);
        assertEquals(0, decodedShadow.closes);
    }

    @Test
    void defaultsValidateAndCheckCancellationBeforeOpeningResources() {
        AtomicInteger opens = new AtomicInteger();
        MFileContent content = () -> { opens.incrementAndGet(); return new ByteArrayInputStream(DATA); };
        assertThrows(NullPointerException.class, () -> content.openInputStream((M3FsProgress) null));
        assertThrows(IllegalArgumentException.class,
                () -> content.openInputStream(M3FsProgress.noop(), null, ""));
        assertThrows(CancellationException.class,
                () -> content.openInputStream(M3FsProgress.of(() -> true, ignored -> { })));
        assertEquals(0, opens.get());
    }

    @Test
    void defaultTransferOwnsInputAndBorrowsOutputAndProgress() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        TrackingOutput destination = new TrackingOutput();
        AtomicLong progress = new AtomicLong();
        MFileContent content = () -> source;
        assertEquals(DATA.length, content.transferTo(destination,
                M3FsProgress.of(() -> false, progress::addAndGet)));
        assertArrayEquals(DATA, destination.bytes.toByteArray());
        assertEquals(DATA.length, progress.get());
        assertEquals(1, source.closes);
        assertEquals(0, destination.closes);
        assertEquals(0, destination.flushes);
    }

    @Test
    void defaultOutputIsExplicitlyUnsupportedAndOverridesReceiveAppendMode() throws Exception {
        MFileContent readOnly = () -> new ByteArrayInputStream(DATA);
        assertThrows(IOException.class, () -> readOnly.openOutputStream(false));
        AtomicBoolean appendMode = new AtomicBoolean();
        TrackingOutput destination = new TrackingOutput();
        MFileContent writable = new MFileContent() {
            @Override public InputStream openInputStream() { return new ByteArrayInputStream(DATA); }
            @Override public OutputStream openOutputStream(boolean append) { appendMode.set(append); return destination; }
        };
        try (MFileStreams.Output out = writable.openOutputStream(true, M3FsProgress.noop())) { out.write(DATA); }
        assertTrue(appendMode.get());
        assertArrayEquals(DATA, destination.bytes.toByteArray());
        assertEquals(1, destination.closes);
    }

    @Test
    void defaultTransferFailureClosesItsInputAndPreservesDestinationOwnership() {
        TrackingInput source = new TrackingInput(DATA);
        IOException failure = new IOException("target failed");
        AtomicInteger closes = new AtomicInteger();
        OutputStream target = new OutputStream() {
            @Override public void write(int value) throws IOException { throw failure; }
            @Override public void close() { closes.incrementAndGet(); }
        };
        MFileContent content = () -> source;
        assertSame(failure, assertThrows(IOException.class, () -> content.transferTo(target, M3FsProgress.noop())));
        assertEquals(1, source.closes);
        assertEquals(0, closes.get());
    }

    @Test
    void wrappingAStreamHashesOnlyTheRemainingInputNotAnInventedWholeFile() throws Exception {
        ByteArrayInputStream primary = new ByteArrayInputStream(DATA);
        assertEquals(5, primary.skip(5));
        try (MFileStreams.Input input = MFileStreams.input(primary, M3FsProgress.noop(), null, "remaining-range")) {
            input.readAllBytes();
            assertEquals(DATA.length - 5, input.position());
            assertEquals(M3ContentId.digest(Arrays.copyOfRange(DATA, 5, DATA.length)), input.contentHash().orElseThrow());
        }
    }

    @Test
    void outputCannotUseTheSameStreamAsItsOwnShadow() {
        TrackingOutput output = new TrackingOutput();
        assertThrows(IllegalArgumentException.class,
                () -> MFileStreams.output(output, M3FsProgress.noop(), output, "write"));
        assertEquals(0, output.closes);
    }

    @Test
    void boundDefaultsAreLazyAndProgressifyEveryOpenedStream() throws Exception {
        AtomicInteger opens = new AtomicInteger();
        AtomicLong work = new AtomicLong();
        AtomicBoolean appendMode = new AtomicBoolean();
        TrackingOutput destination = new TrackingOutput();
        MFileContent provider = new MFileContent() {
            @Override public InputStream openInputStream() {
                opens.incrementAndGet();
                return new ByteArrayInputStream(DATA);
            }
            @Override public OutputStream openOutputStream(boolean append) {
                opens.incrementAndGet();
                appendMode.set(append);
                return destination;
            }
        };
        MFileContent observed = provider.progressify(M3FsProgress.of(() -> false, work::addAndGet));
        assertEquals(0, opens.get());
        for (int i = 0; i < 2; i++) {
            try (InputStream input = observed.openInputStream()) {
                assertInstanceOf(MFileStreams.Input.class, input);
                assertArrayEquals(DATA, input.readAllBytes());
            }
        }
        try (OutputStream output = observed.openOutputStream(true)) {
            assertInstanceOf(MFileStreams.Output.class, output);
            output.write(DATA);
        }
        assertEquals(3, opens.get());
        assertEquals(3L * DATA.length, work.get());
        assertTrue(appendMode.get());
        assertEquals(1, destination.closes);
    }

    @Test
    void repeatedAndPerOpenProgressReplaceTheBoundMonitorWithoutNestedObservation() throws Exception {
        AtomicLong first = new AtomicLong(), bound = new AtomicLong(), perOpen = new AtomicLong();
        TrackingShadow shadow = new TrackingShadow();
        MFileContent provider = () -> new ByteArrayInputStream(DATA);
        MFileContent observed = provider
                .progressify(M3FsProgress.of(() -> false, first::addAndGet))
                .shadow("raw-file", shadow)
                .progressify(M3FsProgress.of(() -> false, bound::addAndGet));
        try (InputStream input = observed.openInputStream()) { input.readAllBytes(); }
        TrackingOutput extra = new TrackingOutput();
        try (MFileStreams.Input input = observed.openInputStream(
                M3FsProgress.of(() -> false, perOpen::addAndGet), extra, "request")) {
            input.readAllBytes();
            assertEquals(DATA.length, input.position());
            assertEquals("request", input.layer());
        }
        assertEquals(0, first.get());
        assertEquals(DATA.length, bound.get());
        assertEquals(DATA.length, perOpen.get());
        assertEquals(2 * DATA.length, shadow.bytes.size());
        assertEquals(2, shadow.completions.size());
        assertEquals("raw-file", shadow.completions.get(0).layer());
        assertArrayEquals(DATA, extra.bytes.toByteArray());
        assertEquals(0, extra.closes);
    }

    @Test
    void perOpenOutputMonitorOverridesBoundProgressAndAppendsItsShadow() throws Exception {
        AtomicLong bound = new AtomicLong(), override = new AtomicLong();
        TrackingOutput destination = new TrackingOutput(), perOpen = new TrackingOutput();
        TrackingShadow configured = new TrackingShadow();
        MFileContent provider = new MFileContent() {
            @Override public InputStream openInputStream() { return new ByteArrayInputStream(DATA); }
            @Override public OutputStream openOutputStream(boolean append) { return destination; }
        };
        MFileContent observed = provider.shadow(configured)
                .progressify(M3FsProgress.of(() -> false, bound::addAndGet));
        try (MFileStreams.Output output = observed.openOutputStream(false,
                M3FsProgress.of(() -> false, override::addAndGet), perOpen, "write-request")) {
            output.write(DATA);
        }
        assertEquals(0, bound.get());
        assertEquals(DATA.length, override.get());
        assertArrayEquals(DATA, configured.bytes.toByteArray());
        assertArrayEquals(DATA, perOpen.bytes.toByteArray());
        assertEquals(1, configured.completions.size());
        assertEquals(MFileStreams.CompletionKind.OUTPUT_SEQUENCE, configured.completions.get(0).kind());
        assertEquals(1, destination.closes);
    }

    @Test
    void completionIsPublishedOnlyAfterInputEofAndSuccessfulPrimaryClose() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        TrackingShadow shadow = new TrackingShadow() {
            @Override public void onComplete(MFileStreams.Completion result) throws IOException {
                assertEquals(1, source.closes);
                super.onComplete(result);
            }
        };
        MFileStreams.Input input = MFileStreams.observingInput(source, M3FsProgress.noop(), shadow, "raw");
        assertEquals(0, source.reads);
        assertArrayEquals(DATA, input.readAllBytes());
        assertTrue(input.complete());
        assertEquals(0, shadow.completions.size(), "EOF alone does not publish cleanup-safe evidence");
        input.close();
        input.close();
        assertEquals(1, shadow.completions.size());
        MFileStreams.Completion result = shadow.completions.get(0);
        assertEquals("raw", result.layer());
        assertEquals(DATA.length, result.byteCount());
        assertEquals(M3ContentId.digest(DATA), result.contentHash());
        assertEquals(MFileStreams.CompletionKind.INPUT_EOF, result.kind());
    }

    @Test
    void earlyCloseAndFailedCleanupDoNotPublishCompletion() throws Exception {
        TrackingShadow partial = new TrackingShadow();
        try (MFileStreams.Input input = MFileStreams.observingInput(
                new ByteArrayInputStream(DATA), M3FsProgress.noop(), partial, "partial")) {
            input.read();
        }
        assertTrue(partial.completions.isEmpty());
        IOException closing = new IOException("close did not finish");
        InputStream source = new ByteArrayInputStream(DATA) {
            @Override public void close() throws IOException { throw closing; }
        };
        TrackingShadow failed = new TrackingShadow();
        MFileStreams.Input input = MFileStreams.observingInput(source, M3FsProgress.noop(), failed, "raw");
        input.readAllBytes();
        assertSame(closing, assertThrows(IOException.class, input::close));
        assertTrue(failed.completions.isEmpty());
        assertTrue(input.failed());
        assertTrue(input.contentHash().isEmpty());
    }

    @Test
    void cancellationDoesNotCompleteAnObserver() throws Exception {
        AtomicBoolean canceled = new AtomicBoolean();
        TrackingInput source = new TrackingInput(DATA);
        TrackingShadow shadow = new TrackingShadow();
        MFileStreams.Input input = MFileStreams.observingInput(source,
                M3FsProgress.of(canceled::get, ignored -> { }), shadow, "raw");
        input.read();
        canceled.set(true);
        assertThrows(CancellationException.class, input::read);
        input.close();
        assertEquals(1, source.closes);
        assertTrue(shadow.completions.isEmpty());
    }

    @Test
    void cancellationAfterEofStillSuppressesCompletionWhileClosingPrimary() throws Exception {
        AtomicBoolean canceled = new AtomicBoolean();
        TrackingInput source = new TrackingInput(DATA);
        TrackingShadow shadow = new TrackingShadow();
        MFileStreams.Input input = MFileStreams.observingInput(source,
                M3FsProgress.of(canceled::get, ignored -> { }), shadow, "raw");
        input.readAllBytes();
        canceled.set(true);
        assertThrows(CancellationException.class, input::close);
        input.close();
        assertEquals(1, source.closes);
        assertTrue(shadow.completions.isEmpty());
        assertTrue(input.failed());
        assertTrue(input.contentHash().isEmpty());
    }

    @Test
    void outputCompletionFollowsSuccessfulFlushAndClose() throws Exception {
        TrackingOutput primary = new TrackingOutput();
        TrackingShadow shadow = new TrackingShadow() {
            @Override public void onComplete(MFileStreams.Completion result) throws IOException {
                assertEquals(1, primary.flushes);
                assertEquals(1, primary.closes);
                super.onComplete(result);
            }
        };
        MFileStreams.Output output = MFileStreams.observingOutput(primary, M3FsProgress.noop(), shadow, "written");
        output.write(DATA);
        assertTrue(shadow.completions.isEmpty());
        output.close();
        output.close();
        assertEquals(1, shadow.completions.size());
        assertEquals(MFileStreams.CompletionKind.OUTPUT_SEQUENCE, shadow.completions.get(0).kind());
        assertEquals(M3ContentId.digest(DATA), shadow.completions.get(0).contentHash());
    }

    @Test
    void completionFailurePropagatesOnceAndDoesNotRetryOrReclose() throws Exception {
        TrackingInput source = new TrackingInput(DATA);
        IOException expected = new IOException("metadata commit failed");
        AtomicInteger calls = new AtomicInteger();
        MFileShadow shadow = new MFileShadow() {
            @Override public void onBytes(byte[] bytes, int offset, int length) { }
            @Override public void onComplete(MFileStreams.Completion result) throws IOException {
                calls.incrementAndGet();
                throw expected;
            }
        };
        MFileStreams.Input input = MFileStreams.observingInput(source, M3FsProgress.noop(), shadow, "raw");
        input.readAllBytes();
        assertSame(expected, assertThrows(IOException.class, input::close));
        input.close();
        assertEquals(1, calls.get());
        assertEquals(1, source.closes);
        assertTrue(input.failed());
        assertFalse(input.complete());
        assertTrue(input.contentHash().isEmpty());
    }

    @Test
    void completionMayCloseTheSameWrapperWithoutReentrantPublication() throws Exception {
        AtomicReference<MFileStreams.Input> reference = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        MFileShadow shadow = new MFileShadow() {
            @Override public void onBytes(byte[] bytes, int offset, int length) { }
            @Override public void onComplete(MFileStreams.Completion result) throws IOException {
                calls.incrementAndGet();
                reference.get().close();
            }
        };
        MFileStreams.Input input = MFileStreams.observingInput(
                new ByteArrayInputStream(DATA), M3FsProgress.noop(), shadow, "raw");
        reference.set(input);
        input.readAllBytes();
        input.close();
        assertEquals(1, calls.get());
    }

    @Test
    void composedShadowsAreOrderedSynchronousAndFailFast() throws Exception {
        List<String> events = new ArrayList<>();
        MFileShadow first = new MFileShadow() {
            @Override public void onBytes(byte[] bytes, int offset, int length) { events.add("first-bytes"); }
            @Override public void onComplete(MFileStreams.Completion result) { events.add("first-complete"); }
        };
        MFileShadow second = new MFileShadow() {
            @Override public void onBytes(byte[] bytes, int offset, int length) { events.add("second-bytes"); }
            @Override public void onComplete(MFileStreams.Completion result) { events.add("second-complete"); }
        };
        MFileContent content = ((MFileContent) () -> new ByteArrayInputStream(new byte[] {42}))
                .shadow(first).shadow(second);
        assertTrue(events.isEmpty());
        try (InputStream input = content.openInputStream()) {
            assertEquals(42, input.read());
            assertEquals(List.of("first-bytes", "second-bytes"), events);
            assertEquals(-1, input.read());
        }
        assertEquals(List.of("first-bytes", "second-bytes", "first-complete", "second-complete"), events);

        IOException failure = new IOException("first observer rejected bytes");
        MFileShadow rejecting = (bytes, offset, length) -> { throw failure; };
        MFileStreams.Input rejected = MFileStreams.observingInput(new ByteArrayInputStream(DATA),
                M3FsProgress.noop(), rejecting.andThen(second), "raw");
        assertSame(failure, assertThrows(IOException.class, rejected::read));
        assertEquals(4, events.size(), "later observer must not see a rejected slice");
    }

    @Test
    void completionCompositionStopsAfterTheFirstFailure() throws Exception {
        IOException expected = new IOException("first completion failed");
        AtomicInteger later = new AtomicInteger();
        MFileShadow first = new MFileShadow() {
            @Override public void onBytes(byte[] bytes, int offset, int length) { }
            @Override public void onComplete(MFileStreams.Completion result) throws IOException { throw expected; }
        };
        MFileShadow second = new MFileShadow() {
            @Override public void onBytes(byte[] bytes, int offset, int length) { }
            @Override public void onComplete(MFileStreams.Completion result) { later.incrementAndGet(); }
        };
        MFileStreams.Input input = MFileStreams.observingInput(
                new ByteArrayInputStream(DATA), M3FsProgress.noop(), first.andThen(second), "raw");
        input.readAllBytes();
        assertSame(expected, assertThrows(IOException.class, input::close));
        assertEquals(0, later.get());
    }

    @Test
    void outputStreamShadowAdaptersRemainBorrowedAndCannotShadowThePrimaryItself() throws Exception {
        TrackingOutput sink = new TrackingOutput();
        try (MFileStreams.Input input = MFileStreams.observingInput(new ByteArrayInputStream(DATA),
                M3FsProgress.noop(), MFileShadow.of(sink), "raw")) {
            input.readAllBytes();
        }
        assertArrayEquals(DATA, sink.bytes.toByteArray());
        assertEquals(0, sink.flushes);
        assertEquals(0, sink.closes);
        assertThrows(IllegalArgumentException.class,
                () -> MFileStreams.observingOutput(sink, M3FsProgress.noop(), MFileShadow.of(sink), "raw"));
        assertEquals(0, sink.closes, "factory validation does not take ownership on failure");

        MFileContent provider = new MFileContent() {
            @Override public InputStream openInputStream() { return new ByteArrayInputStream(DATA); }
            @Override public OutputStream openOutputStream(boolean append) { return sink; }
        };
        MFileContent observed = provider.shadow((bytes, offset, length) -> { });
        assertThrows(IllegalArgumentException.class,
                () -> observed.openOutputStream(false, M3FsProgress.noop(), sink, "raw"));
        assertEquals(1, sink.closes, "a failed default wrapping step closes the output it opened");
    }

    @Test
    void decoratorValidationAndBoundCancellationHappenBeforeProviderIo() {
        AtomicInteger opens = new AtomicInteger();
        MFileContent provider = () -> { opens.incrementAndGet(); return new ByteArrayInputStream(DATA); };
        assertThrows(NullPointerException.class, () -> provider.progressify(null));
        assertThrows(NullPointerException.class, () -> provider.shadow(null));
        assertThrows(IllegalArgumentException.class, () -> provider.shadow("", (bytes, offset, length) -> { }));
        assertThrows(NullPointerException.class, () -> provider.captureReads(null));
        MFileContent canceled = provider.progressify(M3FsProgress.of(() -> true, ignored -> { }));
        assertThrows(CancellationException.class, canceled::openInputStream);
        assertThrows(CancellationException.class, () -> canceled.openOutputStream(false));
        assertThrows(IOException.class, provider::observation);
        assertThrows(IOException.class, () -> provider.shadow((bytes, offset, length) -> { }).openOutputStream(false));
        assertEquals(0, opens.get());
    }

    private static class TrackingShadow implements MFileShadow {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final List<MFileStreams.Completion> completions = new ArrayList<>();
        @Override public void onBytes(byte[] data, int offset, int length) throws IOException {
            bytes.write(data, offset, length);
        }
        @Override public void onComplete(MFileStreams.Completion result) throws IOException { completions.add(result); }
    }

    private static class TrackingInput extends InputStream {
        final ByteArrayInputStream bytes;
        int reads;
        int closes;
        TrackingInput(byte[] data) { bytes = new ByteArrayInputStream(data); }
        @Override public int read() { reads++; return bytes.read(); }
        @Override public int read(byte[] data, int offset, int length) { reads++; return bytes.read(data, offset, length); }
        @Override public int available() { return bytes.available(); }
        @Override public void close() { closes++; }
    }

    private static class TrackingOutput extends OutputStream {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int closes;
        int flushes;
        @Override public void write(int value) throws IOException { bytes.write(value); }
        @Override public void write(byte[] data, int offset, int length) throws IOException { bytes.write(data, offset, length); }
        @Override public void flush() { flushes++; }
        @Override public void close() { closes++; }
    }
}
