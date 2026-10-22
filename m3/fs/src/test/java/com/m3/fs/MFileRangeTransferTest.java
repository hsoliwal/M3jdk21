/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** Provider-neutral selected-region stream contracts, including partial-read ownership. */
final class MFileRangeTransferTest {
    private static final class Source implements MFileContent {
        final byte[] bytes;
        int opens, closes;
        Source(byte[] bytes) { this.bytes = bytes; }
        @Override public MFileObservation observation() {
            return MFileObservation.unknown("memory:range-transfer");
        }
        @Override public InputStream openInputStream() {
            opens++;
            return new ByteArrayInputStream(bytes) {
                @Override public void close() throws IOException {
                    closes++;
                    super.close();
                }
            };
        }
    }

    @Test void orderedSparseWindowsTransferWithOneOpenAndBorrowedOutput() throws Exception {
        byte[] bytes = new byte[200_000];
        new Random(19).nextBytes(bytes);
        Source source = new Source(bytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AtomicLong progress = new AtomicLong();
        long[] offset = {1, 65, 131_072, 190_000};
        long[] length = {2, 5, 10, 0};
        long written = MFileStreams.transferRanges(source, output, offset, length,
                200_000, M3FsProgress.of(() -> false, progress::addAndGet));
        byte[] expected = new byte[17];
        System.arraycopy(bytes, 1, expected, 0, 2);
        System.arraycopy(bytes, 65, expected, 2, 5);
        System.arraycopy(bytes, 131_072, expected, 7, 10);
        assertEquals(17, written);
        assertArrayEquals(expected, output.toByteArray());
        assertEquals(190_000, progress.get()); // Skipped gaps are actual input work.
        assertEquals(1, source.opens);
        assertEquals(1, source.closes);
        output.write(9); // Borrowed destination must stay open.
        offset[0] = Long.MAX_VALUE;
        assertEquals(18, output.size());
    }

    @Test void emptyAndMalformedSelectionsRejectBeforeProviderIO() throws Exception {
        Source source = new Source(new byte[]{1, 2, 3});
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        AtomicInteger cancellation = new AtomicInteger();
        M3FsProgress monitor = M3FsProgress.of(
                () -> { cancellation.incrementAndGet(); return true; }, ignored -> fail("no work"));
        assertEquals(0, MFileStreams.transferRanges(source, target, new long[0],
                new long[0], 0, monitor));
        assertEquals(0, source.opens);
        assertEquals(0, cancellation.get());
        assertThrows(IllegalArgumentException.class, () -> MFileStreams.transferRanges(
                source, target, new long[]{2, 1}, new long[]{0, 0}, 3, monitor));
        assertThrows(IllegalArgumentException.class, () -> MFileStreams.transferRanges(
                source, target, new long[]{Long.MAX_VALUE}, new long[]{1}, Long.MAX_VALUE, monitor));
        assertThrows(IllegalArgumentException.class, () -> MFileStreams.transferRanges(
                source, target, new long[]{0}, new long[0], 3, monitor));
        assertThrows(CancellationException.class, () -> MFileStreams.transferRanges(
                source, target, new long[]{0}, new long[]{1}, 3, monitor));
        assertEquals(0, source.opens);
        assertEquals(0, target.size());
    }

    @Test void truncatedSourceAndOutputFailureNeverCloseBorrowedDestination() {
        Source truncated = new Source(new byte[4]);
        ByteArrayOutputStream borrowed = new ByteArrayOutputStream();
        assertThrows(EOFException.class, () -> MFileStreams.transferRanges(truncated,
                borrowed, new long[]{5}, new long[]{0}, 7, M3FsProgress.noop()));
        assertEquals(1, truncated.opens);
        assertEquals(1, truncated.closes);
        Source failedWrite = new Source(new byte[]{1, 2, 3});
        ByteArrayOutputStream target = new ByteArrayOutputStream() {
            @Override public synchronized void write(byte[] b, int off, int len) {
                throw new IllegalStateException("synthetic destination failure");
            }
        };
        assertThrows(IllegalStateException.class, () -> MFileStreams.transferRanges(
                failedWrite, target, new long[]{1}, new long[]{1}, 3, M3FsProgress.noop()));
        assertEquals(1, failedWrite.closes);
        target.write(5);
        assertEquals(1, target.size());
    }

    @Test void randomizedProviderTransferMatchesIndependentArraySliceOracle() throws Exception {
        Random random = new Random(0x4D334653L);
        for (int trial = 0; trial < 360; trial++) {
            int size = trial < 9
                    ? new int[]{0, 1, 2, 63, 64, 65, 127, 128, 8192}[trial]
                    : random.nextInt(10_000);
            byte[] data = new byte[size];
            random.nextBytes(data);
            Source source = new Source(data);
            int n = random.nextInt(15);
            long[] starts = new long[n], spans = new long[n];
            int position = 0;
            ByteArrayOutputStream reference = new ByteArrayOutputStream();
            for (int i = 0; i < n; i++) {
                position += random.nextInt(size - position + 1);
                int length = random.nextInt(size - position + 1);
                starts[i] = position;
                spans[i] = length;
                reference.write(data, position, length);
                position += length;
            }
            ByteArrayOutputStream observed = new ByteArrayOutputStream();
            AtomicLong progress = new AtomicLong();
            long written = MFileStreams.transferRanges(source, observed, starts, spans, size,
                    M3FsProgress.of(() -> false, progress::addAndGet));
            assertArrayEquals(reference.toByteArray(), observed.toByteArray());
            assertEquals(reference.size(), written);
            assertEquals(n == 0 ? 0 : starts[n - 1] + spans[n - 1], progress.get());
            assertEquals(n == 0 ? 0 : 1, source.opens);
            assertEquals(source.opens, source.closes);
        }
    }

    @Test void readThroughGapsFeedsConfiguredShadowsWithoutInventingEofCapture() throws Exception {
        Source source = new Source(new byte[]{1, 2, 3, 4, 5, 6, 7});
        ByteArrayOutputStream mirrored = new ByteArrayOutputStream();
        AtomicInteger captures = new AtomicInteger();
        MFileContent wrapped = source.shadow(MFileShadow.of(mirrored))
                .captureReads(event -> captures.incrementAndGet());
        // A partial selected range is not a whole-file input EOF capture.
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        assertEquals(2, MFileStreams.transferRanges(wrapped, target,
                new long[]{2}, new long[]{2}, 7, M3FsProgress.noop()));
        assertArrayEquals(new byte[]{3, 4}, target.toByteArray());
        assertArrayEquals(new byte[]{1, 2, 3, 4}, mirrored.toByteArray());
        assertEquals(0, captures.get());
        assertEquals(1, source.closes);
    }
}
