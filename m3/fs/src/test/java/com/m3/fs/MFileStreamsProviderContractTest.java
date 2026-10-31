/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Frozen provider-boundary regressions. Every malformed source allows at most one
 * bulk call before a sentinel failure, so a broken baseline cannot spin a test.
 * The oracle is the InputStream result domain and the existing M3FS observation
 * lifecycle, not the spelling or structure of a candidate implementation.
 */
class MFileStreamsProviderContractTest {
    private static final int[] INVALID_SCALARS = {Integer.MIN_VALUE, -2, 256, Integer.MAX_VALUE};
    private static final int[] INVALID_COUNTS = {Integer.MIN_VALUE, -2, 0, 4, Integer.MAX_VALUE};

    @TestFactory
    Stream<DynamicTest> scalarResultsOutsideByteOrEofDomainFailClosed() {
        return IntStream.of(INVALID_SCALARS).mapToObj(result ->
                DynamicTest.dynamicTest("scalar " + result, () -> {
                    InvalidInput source = new InvalidInput(result, 0);
                    Recorder observed = new Recorder();
                    AtomicLong work = new AtomicLong();
                    MFileStreams.Input input = observe(source, observed, work);
                    try {
                        assertThrows(IOException.class, input::read);
                        assertEquals(1, source.scalarCalls);
                        assertEquals(0, source.bulkCalls);
                        assertRejected(input, source, observed, work, 0);
                    } finally {
                        input.close();
                    }
                    assertEquals(1, source.closes);
                }));
    }

    @TestFactory
    Stream<DynamicTest> nonemptyBulkResultsMustBePositiveBoundedOrExactEof() {
        return IntStream.of(INVALID_COUNTS).mapToObj(result ->
                DynamicTest.dynamicTest("bulk " + result, () -> {
                    InvalidInput source = new InvalidInput(42, result);
                    Recorder observed = new Recorder();
                    AtomicLong work = new AtomicLong();
                    MFileStreams.Input input = observe(source, observed, work);
                    try {
                        // Four is inside the array but exceeds the requested three.
                        assertThrows(IOException.class, () -> input.read(new byte[9], 2, 3));
                        assertEquals(1, source.bulkCalls);
                        assertEquals(0, source.scalarCalls);
                        assertRejected(input, source, observed, work, 0);
                    } finally {
                        input.close();
                    }
                    assertEquals(1, source.closes);
                }));
    }

    @TestFactory
    Stream<DynamicTest> inheritedLoopsRejectMalformedCountsOnTheirFirstProviderCall() {
        List<DynamicTest> cases = new ArrayList<>();
        for (ReadOperation operation : ReadOperation.values()) {
            for (int malformed : new int[] {0, -2, Integer.MAX_VALUE}) {
                cases.add(DynamicTest.dynamicTest(operation + " result " + malformed, () -> {
                    InvalidInput source = new InvalidInput(42, malformed);
                    Recorder observed = new Recorder();
                    AtomicLong work = new AtomicLong();
                    MFileStreams.Input input = observe(source, observed, work);
                    try {
                        assertThrows(IOException.class, () -> operation.read(input));
                        assertEquals(1, source.bulkCalls, "must not retry an invalid provider result");
                        assertEquals(0, source.scalarCalls, "must not conceal an invalid count by another read");
                        assertRejected(input, source, observed, work, 0);
                    } finally {
                        input.close();
                    }
                    assertEquals(1, source.closes);
                }));
            }
        }
        return cases.stream();
    }

    @Test
    void invalidCountAfterValidPrefixRetainsOnlyTheObservedPrefix() throws Exception {
        byte[] prefix = {0, (byte) 255};
        InvalidInput source = new InvalidInput(42, -2) {
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                if (bulkCalls == 0) {
                    bulkCalls++;
                    System.arraycopy(prefix, 0, bytes, offset, prefix.length);
                    return prefix.length;
                }
                if (++bulkCalls > 2) throw new IOException("sentinel: repeated provider read");
                return -2;
            }
        };
        Recorder observed = new Recorder();
        AtomicLong work = new AtomicLong();
        MFileStreams.Input input = observe(source, observed, work);
        assertEquals(2, input.read(new byte[5]));
        assertThrows(IOException.class, () -> input.read(new byte[5]));
        assertArrayEquals(prefix, observed.bytes.toByteArray());
        assertRejected(input, source, observed, work, prefix.length);
        input.close();
        assertEquals(2, source.bulkCalls);
        assertEquals(1, source.closes);
    }

    @Test
    void invalidResultKeepsCleanupFailureSuppressedAndCannotPublishCompletion() {
        IOException closing = new IOException("provider cleanup failed");
        InvalidInput source = new InvalidInput(42, 0) {
            @Override public void close() throws IOException {
                super.close();
                throw closing;
            }
        };
        Recorder observed = new Recorder();
        AtomicLong work = new AtomicLong();
        MFileStreams.Input input = observe(source, observed, work);
        IOException failure = assertThrows(IOException.class, () -> input.read(new byte[1]));
        assertArrayEquals(new Throwable[] {closing}, failure.getSuppressed());
        assertRejected(input, source, observed, work, 0);
        assertDoesNotThrow(input::close);
        assertEquals(1, source.closes);
    }

    @Test
    void invalidEofCannotBecomeAFileCaptureThroughDefaultComposition() throws Exception {
        InvalidInput source = new InvalidInput(42, -2);
        AtomicInteger captures = new AtomicInteger();
        MFileContent provider = new MFileContent() {
            @Override public InputStream openInputStream() { return source; }
            @Override public MFileObservation observation() { return MFileObservation.unknown("memory:invalid"); }
        };
        try (InputStream input = provider.captureReads(ignored -> captures.incrementAndGet()).openInputStream()) {
            assertThrows(IOException.class, input::readAllBytes);
        }
        assertEquals(1, source.bulkCalls);
        assertEquals(1, source.closes);
        assertEquals(0, captures.get());
    }

    @Test
    void emptyRequestsRemainNoopsAndDoNotProbeOrCancelTheProvider() throws Exception {
        InvalidInput source = new InvalidInput(Integer.MIN_VALUE, 0);
        AtomicInteger cancelChecks = new AtomicInteger();
        MFileStreams.Input input = MFileStreams.input(source,
                M3FsProgress.of(() -> { cancelChecks.incrementAndGet(); return true; },
                        ignored -> fail("empty reads cannot report work")));
        assertEquals(0, input.read(new byte[0]));
        assertEquals(0, input.read(new byte[5], 3, 0));
        assertEquals(0, input.readNBytes(0).length);
        assertEquals(0, input.skip(0));
        assertEquals(0, input.skip(-1));
        assertEquals(0, source.bulkCalls);
        assertEquals(0, source.scalarCalls);
        assertEquals(0, cancelChecks.get());
        assertFalse(input.failed());
        assertFalse(input.complete());
        input.close();
        assertEquals(1, source.closes);
        // Current argument validation precedes closed-state validation.
        assertThrows(NullPointerException.class, () -> input.read(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> input.read(new byte[1], 1, 1));
        assertThrows(IOException.class, () -> input.read(new byte[0]));
    }

    @Test
    void everyValidScalarByteAndExactEofKeepTheirOriginalObservation() throws Exception {
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        Recorder observed = new Recorder();
        AtomicLong work = new AtomicLong();
        MFileStreams.Input input = observe(new ByteArrayInputStream(data), observed, work);
        for (int i = 0; i < data.length; i++) assertEquals(i, input.read());
        assertEquals(-1, input.read());
        assertEquals(-1, input.read());
        assertEquals(0, input.read(new byte[0]));
        assertEquals(data.length, input.position());
        assertEquals(data.length, work.get());
        assertArrayEquals(data, observed.bytes.toByteArray());
        assertTrue(input.complete());
        assertFalse(input.failed());
        assertEquals(0, observed.completions.size());
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), input.contentHash().orElseThrow().bytes());
        input.close();
        assertEquals(1, observed.completions.size());
    }

    @Test
    void shortPositiveReadsPreserveOffsetsSkipTransferAndCompletion() throws Exception {
        byte[] data = new byte[521];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 37);
        InputStream source = new ByteArrayInputStream(data) {
            @Override public synchronized int read(byte[] bytes, int offset, int length) {
                return super.read(bytes, offset, Math.min(length, 1 + pos % 3));
            }
        };
        Recorder observed = new Recorder();
        AtomicLong work = new AtomicLong();
        MFileStreams.Input input = observe(source, observed, work);
        byte[] prefix = new byte[19];
        Arrays.fill(prefix, (byte) 91);
        assertEquals(11, input.readNBytes(prefix, 3, 11));
        assertArrayEquals(Arrays.copyOf(data, 11), Arrays.copyOfRange(prefix, 3, 14));
        assertEquals(91, prefix[2]);
        assertEquals(91, prefix[14]);
        assertEquals(17, input.skip(17));
        ByteArrayOutputStream tail = new ByteArrayOutputStream();
        assertEquals(data.length - 28, input.transferTo(tail));
        assertArrayEquals(Arrays.copyOfRange(data, 28, data.length), tail.toByteArray());
        assertEquals(data.length, input.position());
        assertEquals(data.length, work.get());
        assertArrayEquals(data, observed.bytes.toByteArray());
        input.close();
        assertEquals(1, observed.completions.size());
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data),
                observed.completions.getFirst().contentHash().bytes());
    }

    @Test
    void actualFileInputPreservesNativeProviderProgressHashAndShadow() throws Exception {
        Path file = Files.createTempFile("m3fs-provider-contract-", ".bin");
        byte[] data = new byte[8193];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i * 71);
        try {
            try (OutputStream output = new FileOutputStream(file.toFile())) { output.write(data); }
            Recorder observed = new Recorder();
            AtomicLong work = new AtomicLong();
            try (MFileStreams.Input input = observe(new FileInputStream(file.toFile()), observed, work)) {
                assertArrayEquals(Arrays.copyOf(data, 9), input.readNBytes(9));
                assertEquals(23, input.skip(23));
                assertArrayEquals(Arrays.copyOfRange(data, 32, data.length), input.readAllBytes());
                assertTrue(input.complete());
                assertFalse(input.failed());
                assertEquals(data.length, input.position());
                assertEquals(data.length, work.get());
                assertEquals(0, observed.completions.size());
            }
            assertArrayEquals(data, observed.bytes.toByteArray());
            assertEquals(1, observed.completions.size());
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data),
                    observed.completions.getFirst().contentHash().bytes());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static MFileStreams.Input observe(InputStream source, Recorder observed, AtomicLong work) {
        return MFileStreams.observingInput(source, M3FsProgress.of(() -> false, work::addAndGet), observed, "raw");
    }

    private static void assertRejected(MFileStreams.Input input, InvalidInput source,
            Recorder observed, AtomicLong work, long prefix) {
        assertTrue(input.failed());
        assertFalse(input.complete());
        assertTrue(input.contentHash().isEmpty());
        assertEquals(prefix, input.position());
        assertEquals(prefix, work.get());
        assertEquals(prefix, observed.bytes.size());
        assertTrue(observed.completions.isEmpty());
        assertEquals(1, source.closes);
        int scalarCalls = source.scalarCalls, bulkCalls = source.bulkCalls;
        assertThrows(IOException.class, input::read);
        assertThrows(IOException.class, () -> input.read(new byte[1]));
        assertThrows(IOException.class, () -> input.skip(1));
        assertThrows(IOException.class, input::available);
        assertEquals(scalarCalls, source.scalarCalls);
        assertEquals(bulkCalls, source.bulkCalls);
    }

    private enum ReadOperation {
        ALL { @Override void read(InputStream input) throws IOException { input.readAllBytes(); } },
        ARRAY { @Override void read(InputStream input) throws IOException { input.readNBytes(new byte[13], 2, 7); } },
        ALLOCATED { @Override void read(InputStream input) throws IOException { input.readNBytes(7); } },
        TRANSFER { @Override void read(InputStream input) throws IOException { input.transferTo(new ByteArrayOutputStream()); } },
        SKIP { @Override void read(InputStream input) throws IOException { input.skip(7); } },
        EXACT_SKIP { @Override void read(InputStream input) throws IOException { input.skipNBytes(7); } };
        abstract void read(InputStream input) throws IOException;
    }

    private static class InvalidInput extends InputStream {
        private final int scalarResult;
        private final int bulkResult;
        int scalarCalls;
        int bulkCalls;
        int closes;
        InvalidInput(int scalarResult, int bulkResult) {
            this.scalarResult = scalarResult;
            this.bulkResult = bulkResult;
        }
        @Override public int read() throws IOException {
            if (++scalarCalls > 1) throw new IOException("sentinel: repeated scalar provider read");
            return scalarResult;
        }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            if (++bulkCalls > 1) throw new IOException("sentinel: repeated bulk provider read");
            return bulkResult;
        }
        @Override public void close() throws IOException { closes++; }
    }

    private static final class Recorder implements MFileShadow {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final List<MFileStreams.Completion> completions = new ArrayList<>();
        @Override public void onBytes(byte[] data, int offset, int length) { bytes.write(data, offset, length); }
        @Override public void onComplete(MFileStreams.Completion result) { completions.add(result); }
    }
}
