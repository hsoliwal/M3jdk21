// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
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
 * roll back completed external writes. Any admitted worker failure is observed
 * while waiting for an earlier task; a failure may stop ordered publication early.
 * Workers must honor interruption for cancellation and close to finish.
 */
public final class M3FileParallel {
    private static final int MAX_PARALLELISM = 256;

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
        try (M3FileTaskWindow<R> pending = new M3FileTaskWindow<>(parallelism, "m3-file-")) {
            while (iterator.hasNext()) {
                check(progress);
                pending.checkFailure();
                generation(tree, generation);
                int row = iterator.nextInt();
                if (!tree.flag(M3FileFlag.REGULAR, row)) continue;
                pending.add(row, () -> work.apply(row, tree.file(row), child(progress)));
                if (pending.full()) {
                    completed += completeFile(pending, tree, generation, progress, sink);
                }
            }
            while (pending.size() != 0) {
                check(progress);
                completed += completeFile(pending, tree, generation, progress, sink);
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
        try (roots;
             M3FileTaskWindow<R> pending = new M3FileTaskWindow<>(parallelism, "m3-root-")) {
            // Ownership is established before an effectful monitor callback.
            progress.begin("M3 independent drive/directory roots", 0L);
            var iterator = roots.iterator();
            while (iterator.hasNext()) {
                check(progress);
                pending.checkFailure();
                Path root = Objects.requireNonNull(iterator.next(), "root path");
                pending.add(root, () -> {
                    try (M3FileTree tree = M3FileTree.open(root, spec)) {
                        return work.apply(tree, child(progress));
                    }
                });
                if (pending.full()) {
                    completeRoot(pending, progress, sink);
                    completed++;
                }
            }
            while (pending.size() != 0) {
                check(progress);
                completeRoot(pending, progress, sink);
                completed++;
            }
        }
        progress.done();
        return completed;
    }

    private static <R> int completeFile(M3FileTaskWindow<R> pending, M3FileTree tree,
            int generation, M3FileProgress progress, FileResult<R> sink)
            throws IOException {
        R result = pending.await(() -> check(progress));
        generation(tree, generation);
        sink.accept(pending.row(), result);
        generation(tree, generation);
        pending.remove();
        return 1;
    }

    private static <R> void completeRoot(M3FileTaskWindow<R> pending,
            M3FileProgress progress, RootResult<R> sink) throws IOException {
        R result = pending.await(() -> check(progress));
        sink.accept(pending.root(), result);
        pending.remove();
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

}
