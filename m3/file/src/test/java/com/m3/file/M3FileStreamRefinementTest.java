// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Failure and boundary contracts for the existing progress/shadow stream owners. */
class M3FileStreamRefinementTest {
    @TempDir
    Path root;

    @Test
    void subprogressNeverExceedsItsShareAtLongPrecisionBoundaries() {
        for (long share : new long[] {0, 1, 100, (1L << 53) + 3, Long.MAX_VALUE - 1, Long.MAX_VALUE}) {
            AtomicLong reported = new AtomicLong();
            M3FileProgress child = M3FileProgress.of(reported::addAndGet, () -> false).split(share);
            child.begin("large", Long.MAX_VALUE);
            child.worked(Long.MAX_VALUE - 1);
            assertTrue(reported.get() <= share, () -> reported.get() + " exceeds " + share);
            child.done();
            child.done();
            child.worked(Long.MAX_VALUE);
            assertEquals(share, reported.get());
        }
    }

    @Test
    void boundedReadConsumesAtMostTheRemainingAllowanceAndOneProbe() throws IOException {
        for (int limit : new int[] {0, 1, 3, 4095}) {
            ByteArrayInputStream source = new ByteArrayInputStream(new byte[16_384]);
            try (M3FileShadowInputStream input = new M3FileShadowInputStream(source, limit, M3FileProgress.NONE)) {
                assertThrows(M3FileLimitException.class, () -> input.read(new byte[8192]));
                assertEquals(limit + 1, 16_384 - source.available());
                assertEquals(limit + 1L, input.count());
            }
        }
    }

    @Test
    void limitFailureCannotFinishAnUnobservedInputSuffix() throws IOException {
        Shadow shadow = new Shadow();
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(
                new ByteArrayInputStream(new byte[] {1}), 0, M3FileProgress.NONE, shadow)) {
            assertThrows(M3FileLimitException.class, input::read);
            assertThrows(IOException.class, input::read);
            assertFalse(input.ended());
            assertEquals(0, shadow.finishes);
        }
    }

    @Test
    void failedInputShadowStopsFurtherReadsAndCompletion() throws IOException {
        IllegalStateException primary = new IllegalStateException("update failure");
        M3FileShadow first = (bytes, offset, length) -> { throw primary; };
        Shadow later = new Shadow();
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[] {1});
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(source, 100,
                M3FileProgress.NONE, first, later)) {
            assertSame(primary, assertThrows(IllegalStateException.class, input::read));
            assertThrows(IOException.class, input::read);
            assertEquals(0, source.available());
            assertEquals(0, later.bytes);
            assertEquals(0, later.finishes);
            assertFalse(input.ended());
        }
    }

    @Test
    void failedInputProviderCannotLaterDeclareCompleteShadows() throws IOException {
        IOException primary = new IOException("read failed after consumption");
        AtomicLong calls = new AtomicLong();
        InputStream source = new InputStream() {
            @Override public int read() throws IOException {
                if (calls.getAndIncrement() == 0) throw primary;
                return -1;
            }
        };
        Shadow shadow = new Shadow();
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(source, 100, M3FileProgress.NONE, shadow)) {
            assertSame(primary, assertThrows(IOException.class, input::read));
            assertThrows(IOException.class, input::read);
            assertEquals(1, calls.get());
            assertEquals(0, shadow.finishes);
        }
    }

    @Test
    void failedInputFinishDoesNotReportEndedOrRetryCallbacks() throws IOException {
        IllegalStateException primary = new IllegalStateException("finish failure");
        AtomicLong finishes = new AtomicLong();
        M3FileShadow failing = new M3FileShadow() {
            @Override public void update(byte[] bytes, int offset, int length) { }
            @Override public void finish() { finishes.incrementAndGet(); throw primary; }
        };
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(
                new ByteArrayInputStream(new byte[0]), 0, M3FileProgress.NONE, failing)) {
            assertSame(primary, assertThrows(IllegalStateException.class, input::read));
            assertFalse(input.ended());
            assertThrows(IOException.class, input::read);
            assertEquals(1, finishes.get());
        }
    }

    @Test
    void inputDoesNotConsumeAfterItsShadowsHaveFinished() throws IOException {
        AtomicLong calls = new AtomicLong();
        InputStream source = new InputStream() {
            @Override public int read() {
                return switch ((int) calls.getAndIncrement()) { case 0 -> 1; case 2 -> 2; default -> -1; };
            }
        };
        Shadow shadow = new Shadow();
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(source, 100, M3FileProgress.NONE, shadow)) {
            assertEquals(1, input.read());
            assertEquals(-1, input.read());
            assertEquals(-1, input.read());
            assertEquals(2, calls.get());
            assertEquals(1, shadow.bytes);
            assertEquals(1, shadow.finishes);
            assertTrue(input.ended());
        }
    }

    @Test
    void zeroLengthReadAndEarlyCloseDoNotFinishAnIncompleteInput() throws IOException {
        Shadow shadow = new Shadow();
        try (M3FileShadowInputStream input = new M3FileShadowInputStream(
                new ByteArrayInputStream(new byte[] {1, 2}), 2, M3FileProgress.NONE, shadow)) {
            assertEquals(0, input.read(new byte[0]));
            assertEquals(1, input.read());
            assertFalse(input.ended());
        }
        assertEquals(1, shadow.bytes);
        assertEquals(0, shadow.finishes);
    }

    @Test
    void retryAfterFailedOutputCloseCannotFinishShadows() throws IOException {
        IOException primary = new IOException("close failure");
        AtomicLong closes = new AtomicLong();
        OutputStream source = new ByteArrayOutputStream() {
            @Override public void close() throws IOException { closes.incrementAndGet(); throw primary; }
        };
        Shadow shadow = new Shadow();
        M3FileShadowOutputStream output = new M3FileShadowOutputStream(source, 100, M3FileProgress.NONE, shadow);
        output.write(1);
        assertSame(primary, assertThrows(IOException.class, output::close));
        output.close();
        assertEquals(1, closes.get());
        assertEquals(0, shadow.finishes);
        assertThrows(IOException.class, () -> output.write(2));
    }

    @Test
    void partialOutputFailureCannotFinalizeOrWriteAgain() throws IOException {
        IOException primary = new IOException("partial write failure");
        ByteArrayOutputStream committed = new ByteArrayOutputStream();
        OutputStream source = new OutputStream() {
            @Override public void write(int value) { committed.write(value); }
            @Override public void write(byte[] bytes, int offset, int length) throws IOException {
                if (length != 0) committed.write(bytes[offset]);
                throw primary;
            }
        };
        Shadow shadow = new Shadow();
        M3FileShadowOutputStream output = new M3FileShadowOutputStream(source, 100, M3FileProgress.NONE, shadow);
        assertSame(primary, assertThrows(IOException.class, () -> output.write(new byte[] {1, 2, 3})));
        assertThrows(IOException.class, () -> output.write(9));
        output.close();
        assertEquals(1, committed.size());
        assertEquals(0, shadow.bytes);
        assertEquals(0, shadow.finishes);
    }

    @Test
    void failedOutputShadowCannotFinalizeLaterLayers() throws IOException {
        IllegalStateException primary = new IllegalStateException("update failure");
        M3FileShadow first = (bytes, offset, length) -> { throw primary; };
        Shadow later = new Shadow();
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        M3FileShadowOutputStream output = new M3FileShadowOutputStream(source, 100, M3FileProgress.NONE, first, later);
        assertSame(primary, assertThrows(IllegalStateException.class, () -> output.write(new byte[] {1, 2})));
        output.close();
        assertEquals(2, source.size());
        assertEquals(0, later.bytes);
        assertEquals(0, later.finishes);
    }

    @Test
    void flushAndCloseFailuresKeepTheExistingJdkPrimaryAndSuppression() {
        IOException flush = new IOException("flush failure");
        IOException close = new IOException("close failure");
        AtomicLong closes = new AtomicLong();
        OutputStream source = new ByteArrayOutputStream() {
            @Override public void flush() throws IOException { throw flush; }
            @Override public void close() throws IOException { closes.incrementAndGet(); throw close; }
        };
        Shadow shadow = new Shadow();
        M3FileShadowOutputStream output = new M3FileShadowOutputStream(source, 100, M3FileProgress.NONE, shadow);
        assertSame(close, assertThrows(IOException.class, output::close));
        assertArrayEquals(new Throwable[] {flush}, close.getSuppressed());
        assertEquals(1, closes.get());
        assertEquals(0, shadow.finishes);
    }

    @Test
    void tryWithResourcesRetainsCallbackFailureAndSuppressesCloseFailure() {
        IllegalStateException primary = new IllegalStateException("update failure");
        IOException close = new IOException("close failure");
        OutputStream source = new ByteArrayOutputStream() {
            @Override public void close() throws IOException { throw close; }
        };
        M3FileShadow shadow = (bytes, offset, length) -> { throw primary; };
        IllegalStateException observed = assertThrows(IllegalStateException.class, () -> {
            try (M3FileShadowOutputStream output = new M3FileShadowOutputStream(source, 100,
                    M3FileProgress.NONE, shadow)) { output.write(1); }
        });
        assertSame(primary, observed);
        assertArrayEquals(new Throwable[] {close}, observed.getSuppressed());
    }

    @Test
    void invalidOutputArgumentsAreRejectedBeforeTruncation() throws IOException {
        Path file = root.resolve("existing.bin");
        byte[] original = {1, 2, 3, 4};
        for (int kind = 0; kind < 3; kind++) {
            Files.write(file, original);
            int argument = kind;
            assertThrows(NullPointerException.class, () -> {
                switch (argument) {
                    case 0 -> M3File.of(file).openOutputStream(null);
                    case 1 -> M3File.of(file).openOutputStream(M3FileProgress.NONE, (M3FileShadow[]) null);
                    default -> M3File.of(file).openOutputStream(M3FileProgress.NONE, (M3FileShadow) null);
                }
            });
            assertArrayEquals(original, Files.readAllBytes(file), "invalid argument kind=" + kind);
        }
    }

    @Test
    void throwingBeginIsCalledOnceBeforeOpeningTheOutput() throws IOException {
        Path file = Files.write(root.resolve("begin.bin"), new byte[] {1, 2, 3});
        AtomicLong begins = new AtomicLong();
        IllegalStateException primary = new IllegalStateException("begin failure");
        M3FileProgress progress = new M3FileProgress() {
            @Override public void worked(long units) { }
            @Override public void begin(String task, long total) { begins.incrementAndGet(); throw primary; }
        };
        assertSame(primary, assertThrows(IllegalStateException.class, () -> M3File.of(file).openOutputStream(progress)));
        assertEquals(1, begins.get());
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file));
    }

    @Test
    void streamOpenCopiesTheShadowArrayBeforeCallingBegin() throws IOException {
        Path file = root.resolve("copied.bin");
        Shadow shadow = new Shadow();
        M3FileShadow[] supplied = {shadow};
        AtomicLong begins = new AtomicLong();
        M3FileProgress progress = new M3FileProgress() {
            @Override public void worked(long units) { }
            @Override public void begin(String task, long total) { begins.incrementAndGet(); supplied[0] = null; }
        };
        try (OutputStream output = M3File.of(file).openOutputStream(progress, supplied)) { output.write(1); }
        assertEquals(1, begins.get());
        assertEquals(1, shadow.bytes);
        assertEquals(1, shadow.finishes);
        assertArrayEquals(new byte[] {1}, Files.readAllBytes(file));
    }

    @Test
    void invalidInputShadowIsRejectedBeforeBeginningProgress() throws IOException {
        Path file = Files.write(root.resolve("input.bin"), new byte[] {1});
        AtomicLong begins = new AtomicLong();
        M3FileProgress progress = new M3FileProgress() {
            @Override public void worked(long units) { }
            @Override public void begin(String task, long total) { begins.incrementAndGet(); }
        };
        assertThrows(NullPointerException.class,
                () -> M3File.of(file).openInputStream(progress, (M3FileShadow) null));
        assertEquals(0, begins.get());
    }

    @Test
    void preflightWriteRefusalLeavesTheStreamUsableWithinItsAllowance() throws IOException {
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        Shadow shadow = new Shadow();
        AtomicLong worked = new AtomicLong();
        try (M3FileShadowOutputStream output = new M3FileShadowOutputStream(source, 2,
                M3FileProgress.of(worked::addAndGet, () -> false), shadow)) {
            assertThrows(M3FileLimitException.class, () -> output.write(new byte[3]));
            output.write(new byte[] {1, 2});
            assertEquals(2, output.count());
        }
        assertArrayEquals(new byte[] {1, 2}, source.toByteArray());
        assertEquals(2, worked.get());
        assertEquals(2, shadow.bytes);
        assertEquals(1, shadow.finishes);
    }

    private static final class Shadow implements M3FileShadow {
        private long bytes;
        private int finishes;
        @Override public void update(byte[] input, int offset, int length) { bytes += length; }
        @Override public void finish() { finishes++; }
    }
}
