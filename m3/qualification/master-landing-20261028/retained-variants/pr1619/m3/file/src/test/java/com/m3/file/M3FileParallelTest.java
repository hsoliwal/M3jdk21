// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Bounded original-row/drive/directory parallelism: no global filesystem materialization. */
final class M3FileParallelTest {
    @TempDir Path root;

    private static final class Progress implements M3FileProgress {
        final AtomicLong bytes = new AtomicLong();
        final AtomicInteger started = new AtomicInteger();
        final AtomicInteger finished = new AtomicInteger();
        final AtomicBoolean canceled = new AtomicBoolean();
        @Override public void worked(long units) {
            if (units < 0) throw new AssertionError("negative progress");
            bytes.addAndGet(units);
        }
        @Override public boolean isCanceled() { return canceled.get(); }
        @Override public void begin(String task, long total) { started.incrementAndGet(); }
        @Override public void done() { finished.incrementAndGet(); }
    }

    private void write(String name, String contents) throws IOException {
        Path path = root.resolve(name);
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents, StandardCharsets.UTF_8);
    }

    private static int countBytes(M3File entry, M3FileProgress progress) throws IOException {
        int read = 0;
        try (InputStream input = entry.openInputStream(progress, M3FileShadow.sha256())) {
            byte[] scratch = new byte[64];
            for (int n; (n = input.read(scratch)) >= 0; ) read += n;
        }
        return read;
    }

    @Test void selectedFilesStayPrimitiveUntilBoundedWorkAndDeliverInOriginalOrder()
            throws Exception {
        write("a/one.txt", "first");
        write("a/two.json", "{\"x\":2}");
        write("a/skip.md", "not a candidate");
        write("b/three.bin", "binary");
        write("b/four.txt", "four");
        try (M3FileTree tree = M3FileTree.open(root)) {
            M3FileWalk walk = tree.walk().types("txt", "json", "bin")
                    .kinds(M3FileKind.FILE);
            int[] expected = walk.ids().toArray();
            assertEquals(4, expected.length);
            Progress progress = new Progress();
            AtomicInteger active = new AtomicInteger();
            AtomicInteger peak = new AtomicInteger();
            List<Integer> delivered = new ArrayList<>();
            AtomicLong sum = new AtomicLong();
            long count = walk.parallelFiles(3, progress, (id, entry, child) -> {
                int now = active.incrementAndGet();
                peak.accumulateAndGet(now, Math::max);
                try {
                    assertEquals(id, entry.id());
                    return countBytes(entry, child);
                } finally {
                    active.decrementAndGet();
                }
            }, (id, byteCount) -> {
                delivered.add(id);
                sum.addAndGet(byteCount);
            });
            assertEquals(expected.length, count);
            assertArrayEquals(expected, delivered.stream().mapToInt(Integer::intValue).toArray());
            assertTrue(peak.get() > 0 && peak.get() <= 3);
            assertEquals(sum.get(), progress.bytes.get());
            assertEquals(1, progress.started.get());
            assertEquals(1, progress.finished.get());
            assertEquals(0, active.get());
            assertEquals(tree.liveCount(), tree.rows());
        }
    }

    @Test void eachDriveOrDirectoryOwnsOnlyItsOriginalLazyRoot() throws Exception {
        write("diskA/a1.txt", "A");
        write("diskA/sub/a2.json", "{}");
        write("diskB/b1.bin", "B");
        write("diskB/hidden.deep/nested.log", "ignored");
        Path driveA = root.resolve("diskA"), driveB = root.resolve("diskB");
        Progress progress = new Progress();
        List<Path> seen = new ArrayList<>();
        AtomicBoolean inputClosed = new AtomicBoolean();
        long completed = M3FileParallel.roots(
                Stream.of(driveA, driveB).onClose(() -> inputClosed.set(true)),
                M3FileScanSpec.lazy(0), 2, progress, (tree, child) -> {
                    assertEquals(1, tree.rows(), "lazy admission indexes the requested root only");
                    int scanned = tree.scanChildren(0);
                    assertTrue(scanned > 0);
                    child.worked(scanned);
                    return tree.rows();
                }, (path, rows) -> {
                    seen.add(path);
                    assertTrue(rows > 1);
                });
        assertEquals(2, completed);
        assertEquals(List.of(driveA, driveB), seen);
        assertTrue(inputClosed.get());
        assertEquals(2, progress.started.get() + progress.finished.get());
        assertTrue(progress.bytes.get() > 0);
    }

    @Test void directoryDefaultSplitsWithoutCopyingTheWholePathList()
            throws Exception {
        write("first/a.txt", "a");
        write("second/b.txt", "b");
        write("third/c.txt", "c");
        try (M3FileTree tree = M3FileTree.open(root, M3FileScanSpec.lazy(1))) {
            M3FileWalk directories = tree.walk().kinds(M3FileKind.DIRECTORY)
                    .minDepth(1).maxDepth(1);
            Progress progress = new Progress();
            List<Path> ordered = new ArrayList<>();
            long count = directories.parallelDirectories(M3FileScanSpec.lazy(0), 3,
                    progress, (separateTree, child) -> {
                        assertEquals(1, separateTree.rows());
                        int children = separateTree.scanChildren(0);
                        child.worked(children);
                        return children;
                    }, (path, children) -> {
                        ordered.add(path);
                        assertEquals(1, children);
                    });
            assertEquals(3, count);
            assertEquals(3, ordered.size());
            assertEquals(3, progress.bytes.get());
            assertTrue(ordered.contains(root.resolve("first")));
            assertTrue(ordered.contains(root.resolve("second")));
            assertTrue(ordered.contains(root.resolve("third")));
        }
    }

    @Test void structuralMutationInSinkRefusesStaleRowGeneration() throws Exception {
        write("A.txt", "A");
        write("unscanned/nested.txt", "nested");
        try (M3FileTree tree = M3FileTree.open(root, M3FileScanSpec.lazy(1))) {
            int expandable = tree.walk().kinds(M3FileKind.DIRECTORY)
                    .minDepth(1).ids().findFirst().orElseThrow();
            assertFalse(tree.flag(M3FileFlag.SCANNED, expandable));
            M3FileWalk files = tree.walk().types("txt").kinds(M3FileKind.FILE);
            assertThrows(ConcurrentModificationException.class,
                    () -> files.parallelFiles(1, M3FileProgress.NONE,
                            (id, entry, child) -> countBytes(entry, child),
                            (id, result) -> {
                                assertEquals(1, tree.scanChildren(expandable));
                            }));
        }
    }

    @Test void cancellationAndWorkerFailureDoNotPretendSuccess() throws Exception {
        write("A.txt", "A");
        write("B.txt", "B");
        try (M3FileTree tree = M3FileTree.open(root)) {
            M3FileWalk walk = tree.walk().types("txt").kinds(M3FileKind.FILE);
            Progress canceled = new Progress();
            canceled.canceled.set(true);
            AtomicInteger calls = new AtomicInteger();
            assertThrows(InterruptedIOException.class, () ->
                    walk.parallelFiles(2, canceled, (id, entry, child) -> {
                        calls.incrementAndGet();
                        return 0;
                    }, (id, result) -> { }));
            assertEquals(0, calls.get());
            assertEquals(0, canceled.finished.get());

            Progress failure = new Progress();
            assertThrows(IOException.class, () ->
                    walk.parallelFiles(2, failure, (id, entry, child) -> {
                        throw new IOException("provider refused");
                    }, (id, result) -> { }));
            assertEquals(0, failure.finished.get());
        }
        assertThrows(IllegalArgumentException.class, () ->
                M3FileParallel.roots(Stream.<Path>empty(),
                        M3FileScanSpec.lazy(0), 0,
                        M3FileProgress.NONE, (tree, child) -> 1, (path, result) -> { }));
    }

    @Test void cancelWhileOldestTaskBlocksInterruptsAllPendingTasks() throws Exception {
        write("first.txt", "first");
        write("second.txt", "second");
        try (M3FileTree tree = M3FileTree.open(root)) {
            M3FileWalk walk = tree.walk().types("txt").kinds(M3FileKind.FILE);
            Progress monitor = new Progress();
            CountDownLatch started = new CountDownLatch(2);
            CountDownLatch neverReleased = new CountDownLatch(1);
            AtomicInteger interrupted = new AtomicInteger();
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread coordinator = Thread.ofVirtual().start(() -> {
                try {
                    walk.parallelFiles(2, monitor, (id, file, child) -> {
                        started.countDown();
                        try {
                            neverReleased.await();
                            return 1;
                        } catch (InterruptedException stop) {
                            interrupted.incrementAndGet();
                            Thread.currentThread().interrupt();
                            throw new InterruptedIOException("task canceled");
                        }
                    }, (id, result) -> { });
                } catch (Throwable failure) {
                    error.set(failure);
                }
            });
            assertTrue(started.await(10, TimeUnit.SECONDS), "both bounded tasks admitted");
            monitor.canceled.set(true);
            coordinator.join(10_000);
            if (coordinator.isAlive()) {
                coordinator.interrupt();
                neverReleased.countDown();
                coordinator.join(10_000);
            }
            assertFalse(coordinator.isAlive(), "cancellation must unblock the coordinator");
            assertTrue(error.get() instanceof InterruptedIOException,
                    "cancellation is not reported as success: " + error.get());
            assertEquals(2, interrupted.get(), "both pending futures were canceled");
            assertEquals(0, monitor.finished.get(), "failed tasks do not claim completion");
        }
    }

    @Test void thousandsOfFilesNeedOnlyBoundedWorkerResidency() throws Exception {
        Path bulk = Files.createDirectories(root.resolve("bulk"));
        for (int i = 0; i < 1000; i++) {
            Files.writeString(bulk.resolve("item" + i + ".txt"), "x");
        }
        try (M3FileTree tree = M3FileTree.open(root, 4)) {
            M3FileWalk selected = tree.walk().types("txt").kinds(M3FileKind.FILE);
            AtomicInteger concurrency = new AtomicInteger();
            AtomicInteger maximum = new AtomicInteger();
            AtomicLong total = new AtomicLong();
            Progress progress = new Progress();
            long files = selected.parallelFiles(8, progress, (id, entry, child) -> {
                int current = concurrency.incrementAndGet();
                maximum.accumulateAndGet(current, Math::max);
                try {
                    return countBytes(entry, child);
                } finally {
                    concurrency.decrementAndGet();
                }
            }, (id, result) -> total.addAndGet(result));
            assertEquals(1000, files);
            assertEquals(1000, total.get());
            assertEquals(1000, progress.bytes.get());
            assertTrue(maximum.get() <= 8);
            assertTrue(tree.residentCount() <= 4);
        }
    }
}
