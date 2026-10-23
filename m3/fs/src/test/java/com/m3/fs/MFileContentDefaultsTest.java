/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MFileContentDefaultsTest {
    @Test void oneReadHookSuppliesBoundedReadsAndText() throws IOException {
        byte[] bytes = "M3 \u00e9 \ud83d\ude80".getBytes(StandardCharsets.UTF_8);
        MFileContent content = () -> new ByteArrayInputStream(bytes);
        assertArrayEquals(bytes, content.readAllBytes(bytes.length));
        assertEquals("M3 \u00e9 \ud83d\ude80", content.readString(bytes.length, StandardCharsets.UTF_8));
        assertArrayEquals(new byte[0], ((MFileContent) InputStream::nullInputStream).readAllBytes(0));
    }

    @Test void boundFailureReadsOnlyOneExtraByteAndCloses() {
        TrackedInput input = new TrackedInput(new byte[] {1, 2, 3, 4, 5});
        MFileContent content = () -> input;
        assertThrows(IOException.class, () -> content.readAllBytes(3));
        assertEquals(4, input.consumed());
        assertEquals(1, input.closes);
    }

    @Test void validationPrecedesReadOrDestructiveOutputOpen() {
        MemoryContent content = new MemoryContent(new byte[] {9});
        assertThrows(IllegalArgumentException.class, () -> content.readAllBytes(-1));
        assertThrows(NullPointerException.class, () -> content.readString(3, null));
        assertThrows(NullPointerException.class, () -> content.readAllBytes(3, null));
        assertThrows(NullPointerException.class, () -> content.write(null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> content.write(new byte[] {1}, 1, 1, false, M3FsProgress.noop()));
        assertThrows(NullPointerException.class, () -> content.transferFrom(null));
        assertEquals(0, content.readOpens);
        assertEquals(0, content.writeOpens);
        assertArrayEquals(new byte[] {9}, content.bytes);
    }

    @Test void noMonitorOverloadsPreserveConfiguredProgressAndShadows() throws IOException {
        MemoryContent content = new MemoryContent(new byte[] {1, 2, 3});
        AtomicLong worked = new AtomicLong();
        ByteArrayOutputStream shadow = new ByteArrayOutputStream();
        MFileContent observed = content.progressify(M3FsProgress.of(() -> false, worked::addAndGet))
                .shadow(MFileShadow.of(shadow));
        assertArrayEquals(new byte[] {1, 2, 3}, observed.readAllBytes(3));
        observed.write(new byte[] {4, 5});
        observed.append(new byte[] {6});
        assertArrayEquals(new byte[] {4, 5, 6}, content.bytes);
        assertEquals(6, worked.get());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6}, shadow.toByteArray());
        assertEquals(2, observed.transferFrom(new ByteArrayInputStream(new byte[] {7, 8})));
        ByteArrayOutputStream copied = new ByteArrayOutputStream();
        assertEquals(2, observed.transferTo(copied));
        assertArrayEquals(new byte[] {7, 8}, copied.toByteArray());
        assertEquals(10, worked.get());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 7, 8}, shadow.toByteArray());
    }

    @Test void explicitMonitorOverridesConfiguredOne() throws IOException {
        MemoryContent content = new MemoryContent(new byte[] {1, 2});
        AtomicLong bound = new AtomicLong();
        AtomicLong selected = new AtomicLong();
        MFileContent observed = content.progressify(M3FsProgress.of(() -> false, bound::addAndGet));
        M3FsProgress progress = M3FsProgress.of(() -> false, selected::addAndGet);
        assertArrayEquals(new byte[] {1, 2}, observed.readAllBytes(2, progress));
        observed.write(new byte[] {3, 4}, progress);
        observed.append(new byte[] {5}, progress);
        assertEquals(0, bound.get());
        assertEquals(5, selected.get());
    }

    @Test void defaultWritesReplaceAppendAndHonorRegions() throws IOException {
        MemoryContent content = new MemoryContent(new byte[] {9, 9, 9});
        content.write(new byte[] {1, 2});
        content.append(new byte[] {3});
        content.write(new byte[] {8, 4, 5, 8}, 1, 2, true, M3FsProgress.noop());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, content.bytes);
        content.write(new byte[0]);
        assertArrayEquals(new byte[0], content.bytes);
        assertEquals(List.of(false, true, true, false), content.appendModes);
        assertEquals(4, content.outputCloses);
    }

    @Test void cancellationBeforeOpenPreservesContent() {
        MemoryContent content = new MemoryContent(new byte[] {9});
        M3FsProgress canceled = M3FsProgress.of(() -> true, ignored -> { });
        assertThrows(CancellationException.class, () -> content.readAllBytes(1, canceled));
        assertThrows(CancellationException.class, () -> content.write(new byte[] {1}, canceled));
        assertThrows(CancellationException.class, () -> content.append(new byte[] {1}, canceled));
        assertThrows(CancellationException.class,
                () -> content.transferFrom(new ByteArrayInputStream(new byte[] {1}), false, canceled));
        assertEquals(0, content.readOpens);
        assertEquals(0, content.writeOpens);
        assertArrayEquals(new byte[] {9}, content.bytes);
    }

    @Test void transfersBorrowCallerStreamsAndCloseProviderStreams() throws IOException {
        MemoryContent content = new MemoryContent(new byte[] {9});
        TrackedInput source = new TrackedInput(new byte[] {1, 2, 3});
        assertEquals(3, content.transferFrom(source));
        assertEquals(0, source.closes);
        assertEquals(1, content.outputCloses);
        AtomicInteger destinationCloses = new AtomicInteger();
        ByteArrayOutputStream destination = new ByteArrayOutputStream() {
            @Override public void close() { destinationCloses.incrementAndGet(); }
        };
        assertEquals(3, content.transferTo(destination));
        assertArrayEquals(new byte[] {1, 2, 3}, destination.toByteArray());
        assertEquals(0, destinationCloses.get());
        assertEquals(1, content.lastInput.closes);
        assertEquals(2, content.transferFrom(new ByteArrayInputStream(new byte[] {4, 5}), true));
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5}, content.bytes);
    }

    @Test void transferProgressCountsConfirmedOutputAndRetainsInputOwnership() throws IOException {
        MemoryContent content = new MemoryContent(new byte[0]);
        TrackedInput source = new TrackedInput(new byte[] {1, 2, 3});
        AtomicLong written = new AtomicLong();
        assertEquals(3, content.transferFrom(source, false,
                M3FsProgress.of(() -> false, written::addAndGet)));
        assertEquals(3, written.get());
        assertEquals(0, source.closes);
    }

    @Test void providerTransferOverrideCannotFlushBorrowedOutput() throws IOException {
        AtomicInteger flushes = new AtomicInteger();
        ByteArrayOutputStream destination = new ByteArrayOutputStream() {
            @Override public void flush() { flushes.incrementAndGet(); }
        };
        MFileContent content = () -> new ByteArrayInputStream(new byte[] {1, 2, 3}) {
            @Override public long transferTo(OutputStream output) throws IOException {
                long count = super.transferTo(output);
                output.flush();
                return count;
            }
        };
        assertEquals(3, content.transferTo(destination));
        assertArrayEquals(new byte[] {1, 2, 3}, destination.toByteArray());
        assertEquals(0, flushes.get());
    }

    @Test void failingInputRemainsPrimaryWhenProviderCloseFails() {
        IOException readFailure = new IOException("read");
        IOException closeFailure = new IOException("close");
        AtomicInteger borrowedCloses = new AtomicInteger();
        InputStream source = new InputStream() {
            @Override public int read() throws IOException { throw readFailure; }
            @Override public void close() { borrowedCloses.incrementAndGet(); }
        };
        MFileContent content = new MFileContent() {
            @Override public InputStream openInputStream() { return InputStream.nullInputStream(); }
            @Override public OutputStream openOutputStream(boolean append) {
                return new OutputStream() {
                    @Override public void write(int value) { }
                    @Override public void close() throws IOException { throw closeFailure; }
                };
            }
        };
        assertSame(readFailure, assertThrows(IOException.class, () -> content.transferFrom(source)));
        assertArrayEquals(new Throwable[] {closeFailure}, readFailure.getSuppressed());
        assertEquals(0, borrowedCloses.get());
    }

    @Test void readOnlyDefaultDoesNotConsumeBorrowedInput() {
        MFileContent readOnly = InputStream::nullInputStream;
        TrackedInput source = new TrackedInput(new byte[] {1});
        assertThrows(IOException.class, () -> readOnly.write(new byte[] {1}));
        assertThrows(IOException.class, () -> readOnly.transferFrom(source));
        assertEquals(0, source.consumed());
        assertEquals(0, source.closes);
    }

    @Test void reusedReadAndCloseFailureRetainsItsIdentity() {
        for (boolean explicit : new boolean[] {false, true}) {
            IOException failure = new IOException("same provider failure");
            AtomicInteger closes = new AtomicInteger();
            MFileContent content = () -> new InputStream() {
                @Override public int read() throws IOException { throw failure; }
                @Override public void close() throws IOException { closes.incrementAndGet(); throw failure; }
            };
            assertSame(failure, assertThrows(IOException.class, () -> {
                if (explicit) content.readAllBytes(1, M3FsProgress.noop());
                else content.readAllBytes(1);
            }));
            assertEquals(1, closes.get());
            assertEquals(0, failure.getSuppressed().length);
        }
    }

    @Test void reusedWriteAndCloseFailureRetainsItsIdentity() {
        for (boolean explicit : new boolean[] {false, true}) {
            IOException failure = new IOException("same provider failure");
            AtomicInteger closes = new AtomicInteger();
            MFileContent content = new MFileContent() {
                @Override public InputStream openInputStream() { return InputStream.nullInputStream(); }
                @Override public OutputStream openOutputStream(boolean append) {
                    return new OutputStream() {
                        @Override public void write(int value) throws IOException { throw failure; }
                        @Override public void close() throws IOException { closes.incrementAndGet(); throw failure; }
                    };
                }
            };
            assertSame(failure, assertThrows(IOException.class, () -> {
                if (explicit) content.write(new byte[] {1}, M3FsProgress.noop());
                else content.write(new byte[] {1});
            }));
            assertEquals(1, closes.get());
            assertEquals(0, failure.getSuppressed().length);
        }
    }

    @Test void borrowedTransferFailureSharedWithOwnedCloseRetainsItsIdentity() {
        for (boolean explicit : new boolean[] {false, true}) {
            IOException failure = new IOException("shared failure");
            AtomicInteger inputCloses = new AtomicInteger();
            AtomicInteger outputCloses = new AtomicInteger();
            MFileContent content = new MFileContent() {
                @Override public InputStream openInputStream() {
                    return new ByteArrayInputStream(new byte[] {1}) {
                        @Override public void close() throws IOException { inputCloses.incrementAndGet(); throw failure; }
                    };
                }
                @Override public OutputStream openOutputStream(boolean append) {
                    return new ByteArrayOutputStream() {
                        @Override public void close() throws IOException { outputCloses.incrementAndGet(); throw failure; }
                    };
                }
            };
            OutputStream borrowedOutput = new OutputStream() {
                @Override public void write(int value) throws IOException { throw failure; }
            };
            InputStream borrowedInput = new InputStream() {
                @Override public int read() throws IOException { throw failure; }
            };
            assertSame(failure, assertThrows(IOException.class, () -> {
                if (explicit) content.transferTo(borrowedOutput, M3FsProgress.noop());
                else content.transferTo(borrowedOutput);
            }));
            assertSame(failure, assertThrows(IOException.class, () -> {
                if (explicit) content.transferFrom(borrowedInput, M3FsProgress.noop());
                else content.transferFrom(borrowedInput);
            }));
            assertEquals(1, inputCloses.get());
            assertEquals(1, outputCloses.get());
            assertEquals(0, failure.getSuppressed().length);
        }
    }

    @Test void boundedReadPreservesSuccessfulCaptureAndRejectsIncompleteCapture() throws IOException {
        MemoryContent content = new MemoryContent(new byte[] {1, 2, 3});
        List<MFileCapture> captures = new ArrayList<>();
        MFileContent observed = content.captureReads(captures::add);
        assertThrows(IOException.class, () -> observed.readAllBytes(2));
        assertTrue(captures.isEmpty());
        assertArrayEquals(new byte[] {1, 2, 3}, observed.readAllBytes(3));
        assertEquals(1, captures.size());
    }

    private static final class TrackedInput extends ByteArrayInputStream {
        int closes;
        TrackedInput(byte[] bytes) { super(bytes); }
        int consumed() { return pos; }
        @Override public void close() { closes++; }
    }

    private static final class MemoryContent implements MFileContent {
        byte[] bytes;
        int readOpens;
        int writeOpens;
        int outputCloses;
        TrackedInput lastInput;
        final List<Boolean> appendModes = new ArrayList<>();
        MemoryContent(byte[] bytes) { this.bytes = bytes.clone(); }
        @Override public InputStream openInputStream() {
            readOpens++;
            return lastInput = new TrackedInput(bytes.clone());
        }
        @Override public OutputStream openOutputStream(boolean append) {
            writeOpens++;
            appendModes.add(append);
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            if (append) data.writeBytes(bytes);
            return new OutputStream() {
                @Override public void write(int value) { data.write(value); }
                @Override public void write(byte[] values, int offset, int length) {
                    data.write(values, offset, length);
                }
                @Override public void close() { outputCloses++; bytes = data.toByteArray(); }
            };
        }
        @Override public MFileObservation observation() {
            return MFileObservation.metadata("test:/content", MFileObservation.EXISTS_KNOWN
                    | MFileObservation.EXISTS | MFileObservation.TYPE_KNOWN
                    | MFileObservation.FILE | MFileObservation.SIZE_KNOWN,
                    bytes.length, 0, MFileObservation.ChangeStamp.unavailable());
        }
    }
}
