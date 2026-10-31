// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3FileParallelHashTest {
    @TempDir Path root;

    @Test void parallelFilteredSubtreeMatchesSerialOnePassHashes() throws Exception {
        Path dir = Files.createDirectories(root.resolve("selected"));
        Files.writeString(dir.resolve("text.txt"), "a β\nword ".repeat(4000));
        Files.write(dir.resolve("binary.bin"), new byte[] {0, 1, 2, 3});
        Files.writeString(dir.resolve("empty.txt"), "");
        Files.writeString(dir.resolve("ignored.json"), "{}");
        Files.writeString(root.resolve("outside.txt"), "outside");
        M3FileChangeProvider structured = new M3FileChangeProvider() {
            @Override public Optional<? extends M3FileStructHash> structure(M3File file) {
                return Optional.of(new M3FileStructHash() {
                    private long chars;
                    @Override public void update(char[] data, int off, int length) { chars += length; }
                    @Override public long structHash() { return chars; }
                });
            }
        };
        try (M3FileTree serial = M3FileTree.open(root); M3FileTree parallel = M3FileTree.open(root)) {
            int serialDir = id(serial, dir), parallelDir = id(parallel, dir);
            int expected = serial.hash(serial.walk(serialDir).types("txt", "bin"), 1 << 20,
                    M3FileProgress.NONE, structured);
            GuardedProgress progress = new GuardedProgress();
            assertEquals(expected, parallel.hash(parallel.walk(parallelDir).types("txt", "bin"),
                    1 << 20, progress, structured, 3));
            assertEquals(3, expected);
            assertEquals(Files.size(dir.resolve("text.txt")) + 4, progress.units);
            assertEquals(1, progress.finished);
            for (int row : serial.ids().toArray()) {
                int other = id(parallel, serial.path(row));
                for (M3FileFlag flag : new M3FileFlag[] {M3FileFlag.HASHED, M3FileFlag.TOKEN_HASHED,
                        M3FileFlag.STRUCT_HASHED, M3FileFlag.FAILED}) {
                    assertEquals(serial.flag(flag, row), parallel.flag(flag, other));
                }
                for (int word = 0; word < 4; word++) assertEquals(serial.shaWord(row, word), parallel.shaWord(other, word));
                assertEquals(serial.crc(row), parallel.crc(other));
                assertEquals(serial.tokenHash(row), parallel.tokenHash(other));
                assertEquals(serial.structHash(row), parallel.structHash(other));
            }
        }
    }

    @Test void slowFirstFileBoundsTheEntireOutstandingWindow() throws Exception {
        ProbeSource source = new ProbeSource(3, true, -1);
        M3FileTree tree = fixture(source, 127);
        try (var caller = Executors.newSingleThreadExecutor()) {
            var result = caller.submit(() -> tree.hash(tree.walk(), 100, M3FileProgress.NONE,
                    M3FileChangeProvider.ATTRIBUTES, 3));
            try {
                assertTrue(source.started.await(10, TimeUnit.SECONDS), "three concurrent readers start");
                assertEquals(3, source.opens.get(), "slow first row bounds submitted work, not just active threads");
                assertEquals(3, source.maximum.get());
                assertFalse(tree.flag(M3FileFlag.HASHED, 2), "completed later row is not published out of order");
            } finally {
                source.release.countDown();
            }
            assertEquals(127, result.get(20, TimeUnit.SECONDS));
        }
        assertEquals(127, source.opens.get(), "one physical stream per file");
        assertEquals(0, source.active.get());
        assertTrue(source.maximum.get() <= 3);
        for (int row = 1; row <= 127; row++) assertTrue(tree.flag(M3FileFlag.HASHED, row));
    }

    @Test void ordinaryReadFailureCommitsOnlyItsFailureAndContinues() throws Exception {
        ProbeSource source = new ProbeSource(1, false, 5);
        M3FileTree tree = fixture(source, 32);
        assertEquals(31, tree.hash(tree.walk(), 100, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES, 4));
        assertTrue(tree.flag(M3FileFlag.FAILED, 5));
        assertFalse(tree.flag(M3FileFlag.HASHED, 5));
        for (int row = 1; row <= 32; row++) if (row != 5) assertTrue(tree.flag(M3FileFlag.HASHED, row));
        assertEquals(0, source.active.get());
    }

    @Test void cancellationAndLayerFailureJoinReadersBeforeReturning() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            ProbeSource source = new ProbeSource(1, false, -1);
            M3FileTree tree = fixture(source, 128);
            AtomicBoolean canceled = new AtomicBoolean();
            M3FileProgress monitor = M3FileProgress.of(units -> canceled.set(true), canceled::get);
            assertThrows(InterruptedIOException.class,
                    () -> tree.hash(tree.walk(), 100, monitor, M3FileChangeProvider.ATTRIBUTES, 4));
            assertEquals(0, source.active.get());
            IllegalStateException failure = new IllegalStateException("structure failed");
            M3FileChangeProvider provider = new M3FileChangeProvider() {
                @Override public Optional<? extends M3FileStructHash> structure(M3File file) {
                    return Optional.of(new M3FileStructHash() {
                        @Override public void update(char[] chars, int off, int len) { throw failure; }
                        @Override public long structHash() { return 0; }
                    });
                }
            };
            ProbeSource failing = new ProbeSource(1, false, -1);
            M3FileTree other = fixture(failing, 32);
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> other.hash(other.walk(), 100, M3FileProgress.NONE, provider, 3)));
            assertEquals(0, failing.active.get());
            for (int row = 1; row <= 32; row++) assertFalse(other.flag(M3FileFlag.HASHED, row));
        });
    }

    @Test void laterLayerFailureInterruptsBlockedEarlierReader() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            ProbeSource source = new ProbeSource(2, true, -1);
            M3FileTree tree = fixture(source, 20);
            IllegalArgumentException failure = new IllegalArgumentException("later structure failed");
            M3FileChangeProvider provider = new M3FileChangeProvider() {
                @Override public Optional<? extends M3FileStructHash> structure(M3File file) {
                    return file.id() != 2 ? Optional.empty() : Optional.of(new M3FileStructHash() {
                        @Override public void update(char[] chars, int off, int len) { throw failure; }
                        @Override public long structHash() { return 0; }
                    });
                }
            };
            assertSame(failure, assertThrows(IllegalArgumentException.class,
                    () -> tree.hash(tree.walk(), 100, M3FileProgress.NONE, provider, 2)));
            assertEquals(0, source.active.get());
            assertEquals(2, source.opens.get());
            for (int row = 1; row <= 20; row++) assertFalse(tree.flag(M3FileFlag.HASHED, row));
        });
    }

    @Test void callerInterruptionJoinsReadersAndPreservesInterruptStatus() throws Exception {
        ProbeSource source = new ProbeSource(2, true, -1);
        M3FileTree tree = fixture(source, 20);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        Thread caller = Thread.ofPlatform().start(() -> {
            try { tree.hash(tree.walk(), 100, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES, 2); }
            catch (Throwable caught) { failure.set(caught); interruptPreserved.set(Thread.currentThread().isInterrupted()); }
        });
        try {
            assertTrue(source.started.await(5, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(5000);
            assertFalse(caller.isAlive(), "interrupted caller joined its readers");
            assertInstanceOf(InterruptedIOException.class, failure.get());
            assertTrue(interruptPreserved.get());
            assertEquals(0, source.active.get());
        } finally {
            source.release.countDown();
            caller.interrupt();
            caller.join(5000);
        }
    }

    @Test void emptyFilteredAndInvalidCallsDoNotOpenContent() throws Exception {
        ProbeSource source = new ProbeSource(1, false, -1);
        M3FileTree tree = fixture(source, 8);
        assertEquals(0, tree.hash(tree.walk().types("json"), 100, M3FileProgress.NONE,
                M3FileChangeProvider.ATTRIBUTES, 2));
        assertThrows(IllegalArgumentException.class, () -> tree.hash(tree.walk(), 100,
                M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES, 0));
        assertThrows(IllegalArgumentException.class, () -> tree.hash(tree.walk(), -1,
                M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES, 2));
        M3FileTree other = fixture(source, 1);
        assertThrows(IllegalArgumentException.class, () -> tree.hash(other.walk(), 100,
                M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES, 2));
        assertThrows(InterruptedIOException.class, () -> tree.hash(tree.walk(), 100,
                M3FileProgress.of(units -> fail("work after cancellation"), () -> true),
                M3FileChangeProvider.ATTRIBUTES, 2));
        assertEquals(0, source.opens.get());
        assertEquals(8, tree.hash(tree.walk(), 100, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES, 1));
        assertEquals(1, source.maximum.get());
    }

    private static int id(M3FileTree tree, Path path) {
        return tree.ids().filter(row -> tree.path(row).equals(path)).findFirst().orElseThrow();
    }

    private static M3FileTree fixture(M3FileSource source, int files) {
        M3FileTree tree = M3FileTree.create(Path.of("fixture"), 4, source);
        M3FileTreeBuilder builder = new M3FileTreeBuilder(tree);
        builder.openFrame(builder.append("fixture", M3FileKind.DIRECTORY, false, 0, 0));
        for (int i = 0; i < files; i++) builder.append(i + ".txt", M3FileKind.FILE, false, 6, 0);
        builder.closeFrame();
        return tree;
    }

    private static final class GuardedProgress implements M3FileProgress {
        private final AtomicBoolean inside = new AtomicBoolean();
        private long units;
        private int finished;
        @Override public void worked(long amount) {
            assertTrue(inside.compareAndSet(false, true), "concurrent monitor callback");
            try { Thread.yield(); units += amount; } finally { inside.set(false); }
        }
        @Override public void done() { finished++; }
    }

    private static final class ProbeSource implements M3FileSource {
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maximum = new AtomicInteger();
        private final CountDownLatch started;
        private final CountDownLatch release = new CountDownLatch(1);
        private final boolean blockFirst;
        private final int failId;
        ProbeSource(int width, boolean blockFirst, int failId) {
            started = new CountDownLatch(width); this.blockFirst = blockFirst; this.failId = failId;
        }
        @Override public InputStream openContent(M3File file) throws IOException {
            opens.incrementAndGet();
            if (file.id() == failId) throw new IOException("unreadable");
            maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
            started.countDown();
            return new FilterInputStream(new ByteArrayInputStream(new byte[] {'a', ' ', 'b', '\n', 'c', 'd'})) {
                private boolean closed;
                @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                    if (blockFirst) {
                        try {
                            if (!started.await(10, TimeUnit.SECONDS)) throw new IOException("test readers did not start");
                            if (file.id() == 1 && !release.await(10, TimeUnit.SECONDS)) throw new IOException("test reader timed out");
                        }
                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new InterruptedIOException("interrupted"); }
                    }
                    return super.read(bytes, offset, length);
                }
                @Override public void close() throws IOException {
                    if (!closed) { closed = true; active.decrementAndGet(); }
                    super.close();
                }
            };
        }
    }
}
