/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util.concurrent;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * M3JDK opt-in scoped progress bridge for ordinary Java functions and executors.
 * No task queue, worker threads, retained Java objects, or native ownership.
 */
public final class ProgressMonitors {
    private static final Supplier<? extends ProgressMonitor> NONE = () -> ProgressMonitor.NONE;
    private static final AtomicReference<Supplier<? extends ProgressMonitor>> SYSTEM =
            new AtomicReference<>(NONE);
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private ProgressMonitors() { }

    /**
     * Process fallback. Supplier publication is atomic; it may be called concurrently.
     *
     * @return the supplied monitor, or {@link ProgressMonitor#NONE} if it is null
     */
    public static ProgressMonitor system() {
        ProgressMonitor monitor = SYSTEM.get().get();
        return monitor == null ? ProgressMonitor.NONE : monitor;
    }

    /**
     * Updates the global fallback and returns the previous provider.
     *
     * @param provider the non-null supplier of process fallback monitors
     * @return the previously installed supplier
     * @throws NullPointerException if {@code provider} is null
     */
    public static Supplier<? extends ProgressMonitor> setSystem(
            Supplier<? extends ProgressMonitor> provider) {
        return SYSTEM.getAndSet(Objects.requireNonNull(provider, "provider"));
    }

    /** Restores the process fallback provider to the no-op monitor. */
    public static void resetSystem() { SYSTEM.set(NONE); }

    /**
     * The lexical monitor, or the process fallback when no scope is active.
     *
     * @return the current non-null monitor
     */
    public static ProgressMonitor current() {
        Scope scope = CURRENT.get();
        return scope != null ? scope.monitor : system();
    }

    /**
     * Binds an already-owned monitor: closing this scope does not call done().
     * The scope must be closed on its creating thread in last-in-first-out order.
     *
     * @param monitor the non-null monitor to bind
     * @return the scope restoring the previous binding when closed
     * @throws NullPointerException if {@code monitor} is null
     */
    public static Scope bind(ProgressMonitor monitor) {
        Objects.requireNonNull(monitor, "monitor");
        Scope next = new Scope(CURRENT.get(), monitor);
        CURRENT.set(next);
        return next;
    }

    /**
     * Runs a task in a borrowed monitor scope, checking cancellation before and
     * after execution. Closing the scope does not complete the monitor.
     *
     * @param task the non-null task to run
     * @param monitor the non-null monitor borrowed for the invocation
     * @throws NullPointerException if {@code task} or {@code monitor} is null
     * @throws CancellationException if a cancellation check rejects execution
     */
    public static void run(Runnable task, ProgressMonitor monitor) {
        Objects.requireNonNull(task, "task");
        Scope scope = bind(monitor);
        try (scope) {
            monitor.checkCancelled();
            task.run();
            monitor.checkCancelled();
        }
    }

    /**
     * Calls a task in a borrowed monitor scope, checking cancellation before and
     * after execution. Closing the scope does not complete the monitor.
     *
     * @param <T> the type of the task result
     * @param task the non-null task to call
     * @param monitor the non-null monitor borrowed for the invocation
     * @return the task result
     * @throws Exception if the task cannot compute a result
     * @throws NullPointerException if {@code task} or {@code monitor} is null
     */
    public static <T> T call(Callable<T> task, ProgressMonitor monitor) throws Exception {
        Objects.requireNonNull(task, "task");
        Scope scope = bind(monitor);
        try (scope) {
            monitor.checkCancelled();
            T result = task.call();
            monitor.checkCancelled();
            return result;
        }
    }

    /**
     * Allocates a weighted child monitor without taking ownership of its parent.
     *
     * @param parent the non-null parent monitor
     * @param name the child description, or null for no description
     * @param parentTicks the nonnegative work units allocated in the parent
     * @param totalWork the positive child work units, or
     *        {@link ProgressMonitor#UNKNOWN_WORK}
     * @return the child monitor, or {@link ProgressMonitor#NONE} for a no-op parent
     * @throws NullPointerException if {@code parent} is null
     * @throws IllegalArgumentException if the work range is invalid
     */
    public static ProgressMonitor child(
            ProgressMonitor parent, String name, long parentTicks, long totalWork) {
        Objects.requireNonNull(parent, "parent");
        if (parentTicks < 0 || totalWork == 0 || totalWork < ProgressMonitor.UNKNOWN_WORK) {
            throw new IllegalArgumentException("Invalid child progress range");
        }
        if (parent == ProgressMonitor.NONE) {
            return ProgressMonitor.NONE;
        }
        return new WeightedChild(parent, name, parentTicks, totalWork);
    }

    /** A borrowed monitor is never completed by an executor task. */
    public static final class Scope implements AutoCloseable {
        private final Scope parent;
        private final ProgressMonitor monitor;
        private final Thread owner = Thread.currentThread();
        private boolean closed;

        private Scope(Scope parent, ProgressMonitor monitor) {
            this.parent = parent;
            this.monitor = monitor;
        }

        @Override public void close() {
            if (owner != Thread.currentThread()) {
                throw new IllegalStateException("Progress scope belongs to another thread");
            }
            if (closed) return;
            if (CURRENT.get() != this) {
                throw new IllegalStateException("Progress scopes must close in LIFO order");
            }
            if (parent == null) CURRENT.remove();
            else CURRENT.set(parent);
            closed = true;
        }
    }

    private static final class WeightedChild implements ProgressMonitor {
        private final ProgressMonitor parent;
        private final long parentTicks;
        private final long totalWork;
        private final AtomicLong consumed = new AtomicLong();
        private final AtomicLong credited = new AtomicLong();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        WeightedChild(ProgressMonitor parent, String name, long parentTicks, long totalWork) {
            this.parent = parent;
            String title = Objects.requireNonNullElse(name, "");
            this.parentTicks = parentTicks;
            this.totalWork = totalWork;
            if (!title.isEmpty()) parent.message(title);
        }

        @Override public void worked(long units) {
            if (units < 0) throw new IllegalArgumentException("Negative progress");
            if (closed.get() || units == 0 || totalWork == ProgressMonitor.UNKNOWN_WORK) return;
            long before;
            long after;
            do {
                before = consumed.get();
                after = units > totalWork - before ? totalWork : before + units;
                if (before == after) return;
            } while (!consumed.compareAndSet(before, after));
            credit(scale(after, parentTicks, totalWork));
        }

        private void credit(long target) {
            long before;
            do {
                before = credited.get();
                if (target <= before) return;
            } while (!credited.compareAndSet(before, target));
            parent.worked(target - before);
        }

        private static long scale(long done, long ticks, long total) {
            if (done >= total) return ticks;
            if (done <= 0 || ticks == 0) return 0L;
            if (done <= Long.MAX_VALUE / ticks) return done * ticks / total;
            return BigInteger.valueOf(done).multiply(BigInteger.valueOf(ticks))
                    .divide(BigInteger.valueOf(total)).longValueExact();
        }

        @Override public void message(String text) { parent.message(text); }
        @Override public boolean isCancelled() {
            return cancelled.get() || parent.isCancelled();
        }
        @Override public void cancel() { cancelled.set(true); }
        @Override public void done() {
            if (closed.compareAndSet(false, true)) credit(parentTicks);
        }
    }
}
