// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * Bounded parallel execution over original M3FileTree row IDs and independently
 * owned directory/drive roots. No parallel mutation of a shared tree, no second
 * filesystem model and no O(file-count) task/future array.
 *
 * <p>Directory enumeration and filtered selection stay with M3FileWalk.
 * A caller may pass a lazy Stream of disjoint drive or directory roots to
 * {@link #roots}; each admitted root owns its original M3FileTree and is
 * closed after processing. No OS-wide scan happens implicitly.
 *
 * <p>At most {@code parallelism} virtual tasks are live. Results reach the
 * caller's sink in encounter order; side effects inside worker callbacks
 * may execute in any order. Callback failure cancels pending work but cannot
 * roll back completed external writes.
 */
public final class M3FileParallel {
    private static final int MAX_PARALLELISM = 256;
    private static final long CHECK_MILLIS = 50L;

    private M3FileParallel() { }

    /** Work against an existing physical file row. No file object is retained by the scheduler. */
    @FunctionalInterface
    public interface FileTask<R> {
        R apply(int rowId, M3File file, M3FileProgress monitor) throws IOException;
    }

    /** Serial, original row order receipt of completed file work. */
    @FunctionalInterface
    public interface FileResult<R> {
        void accept(int rowId, R value) throws IOException;
    }

    /** One independent original file tree per root; caller selects which directories expand. */
    @FunctionalInterface
    public interface RootTask<R> {
        R apply(M3FileTree tree, M3FileProgress monitor) throws IOException;
    }

    /** Serial, caller-provided root order receipt of completed root work. */
    @FunctionalInterface
    public interface RootResult<R> {
        void accept(Path root, R value) throws IOException;
    }

    /**
     * Process only regular files admitted by the original filtered walk.
     * Work is parallel but resulting entries are delivered in original
     * encounter order. Only a bounded number of row IDs/Futures are retained.
     *
     * <p>The tree must not be structurally expanded during this operation;
     * its generation is checked before/after each completed task. Callers
     * owning a lazy tree can select and expand their directories before
     * starting this immutable indexed traversal.
     */
    public static <R> long files(M3FileTraversal selected, int parallelism,
            M3FileProgress progress, FileTask<R> work, FileResult<R> sink)
            throws IOException {
        Objects.requireNonNull(selected, "selected original walk");
        validate(parallelism, progress, work, sink);
        M3FileTree tree = selected.tree();
        int generation = tree.generation();
        PrimitiveIterator.OfInt iterator = Spliterators.iterator(selected.idSpliterator());
        long completed = 0;
        progress.begin("M3 selected-file work", 0L);
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("m3-file-", 0).factory())) {
            ArrayDeque<FileFuture<R>> pending = new ArrayDeque<>(parallelism);
            try {
                while (iterator.hasNext()) {
                    check(progress);
                    generation(tree, generation);
                    int row = iterator.nextInt();
                    if (!tree.flag(M3FileFlag.REGULAR, row)) continue;
                    // No original path or facade is materialized before the task.
                    Future<R> future = executor.submit(() ->
                            work.apply(row, tree.file(row), child(progress)));
                    pending.addLast(new FileFuture<>(row, future));
                    if (pending.size() == parallelism) {
                        completed += completeFile(pending.peekFirst(), tree,
                                generation, progress, sink);
                        pending.removeFirst();
                    }
                }
                while (!pending.isEmpty()) {
                    check(progress);
                    completed += completeFile(pending.peekFirst(), tree,
                            generation, progress, sink);
                    pending.removeFirst();
                }
            } catch (IOException | RuntimeException | Error failure) {
                for (FileFuture<R> item : pending) item.future().cancel(true);
                executor.shutdownNow();
                throw failure;
            }
        }
        progress.done();
        return completed;
    }

    /**
     * Independently process caller-selected drives/directories without making
     * the entire filesystem resident. Each root gets a separate original
     * M3FileTree.open(root, spec); lazy(0/1) limits discovery at admission.
     * A root callback may expand that tree on its own thread before returning.
     *
     * <p>The input Stream is consumed once and closed. Its own iterator must
     * remain valid until this method returns. Overlapping paths should be
     * rejected by the caller if duplicated subtree work is unwanted.
     */
    public static <R> long roots(Stream<Path> roots, M3FileScanSpec spec,
            int parallelism, M3FileProgress progress, RootTask<R> work,
            RootResult<R> sink) throws IOException {
        Objects.requireNonNull(roots, "original root stream");
        Objects.requireNonNull(spec, "original scan policy");
        validate(parallelism, progress, work, sink);
        long completed = 0;
        progress.begin("M3 independent drive/directory roots", 0L);
        try (roots;
             ExecutorService executor = Executors.newThreadPerTaskExecutor(
                     Thread.ofVirtual().name("m3-root-", 0).factory())) {
            ArrayDeque<RootFuture<R>> pending = new ArrayDeque<>(parallelism);
            try {
                var iterator = roots.iterator();
                while (iterator.hasNext()) {
                    check(progress);
                    Path root = Objects.requireNonNull(iterator.next(), "root path");
                    Future<R> future = executor.submit(() -> {
                        try (M3FileTree tree = M3FileTree.open(root, spec)) {
                            return work.apply(tree, child(progress));
                        }
                    });
                    pending.addLast(new RootFuture<>(root, future));
                    if (pending.size() == parallelism) {
                        RootFuture<R> item = pending.peekFirst();
                        R result = await(item.future(), progress);
                        sink.accept(item.root(), result);
                        pending.removeFirst();
                        completed++;
                    }
                }
                while (!pending.isEmpty()) {
                    check(progress);
                    RootFuture<R> item = pending.peekFirst();
                    R result = await(item.future(), progress);
                    sink.accept(item.root(), result);
                    pending.removeFirst();
                    completed++;
                }
            } catch (IOException | RuntimeException | Error failure) {
                for (RootFuture<R> item : pending) item.future().cancel(true);
                executor.shutdownNow();
                throw failure;
            }
        }
        progress.done();
        return completed;
    }

    private static <R> int completeFile(FileFuture<R> item, M3FileTree tree,
            int generation, M3FileProgress progress, FileResult<R> sink)
            throws IOException {
        R result = await(item.future(), progress);
        generation(tree, generation);
        sink.accept(item.row(), result);
        generation(tree, generation);
        return 1;
    }

    private static <R> R await(Future<R> future, M3FileProgress monitor)
            throws IOException {
        try {
            for (;;) {
                check(monitor);
                try {
                    return future.get(CHECK_MILLIS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException pending) {
                    // Recheck cancellation without forcing a second stream read.
                }
            }
        } catch (InterruptedException interrupted) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            InterruptedIOException error = new InterruptedIOException("M3 parallel work interrupted");
            error.initCause(interrupted);
            throw error;
        } catch (CancellationException cancelled) {
            throw new InterruptedIOException("M3 parallel work cancelled");
        } catch (ExecutionException completed) {
            Throwable failure = completed.getCause();
            if (failure instanceof IOException error) throw error;
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
            throw new IOException("M3 parallel source work failed", failure);
        }
    }

    private static void generation(M3FileTree owner, int expected) {
        if (owner.generation() != expected) {
            throw new java.util.ConcurrentModificationException(
                    "M3FileTree source generation changed during selected work");
        }
    }

    private static void check(M3FileProgress root) throws InterruptedIOException {
        synchronized (root) { root.checkCanceled(); }
    }

    /**
     * Existing progress defaults remain the protocol. Serialize just parent
     * monitor callbacks, never IO or byte processing. The worker may call
     * split() on this instance to create nested parser/stream submonitor work.
     */
    private static M3FileProgress child(M3FileProgress parent) {
        return new M3FileProgress() {
            @Override public void worked(long units) {
                if (units < 0) throw new IllegalArgumentException("negative progress");
                synchronized (parent) { parent.worked(units); }
            }
            @Override public boolean isCanceled() {
                if (Thread.currentThread().isInterrupted()) return true;
                synchronized (parent) { return parent.isCanceled(); }
            }
        };
    }

    private static void validate(int parallelism, M3FileProgress monitor,
            Object work, Object sink) {
        Objects.requireNonNull(monitor, "original progress");
        Objects.requireNonNull(work, "original worker");
        Objects.requireNonNull(sink, "ordered result sink");
        if (parallelism < 1 || parallelism > MAX_PARALLELISM) {
            throw new IllegalArgumentException("parallelism must be 1..256");
        }
    }

    private record FileFuture<R>(int row, Future<R> future) { }
    private record RootFuture<R>(Path root, Future<R> future) { }
}
