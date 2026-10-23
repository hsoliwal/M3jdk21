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
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Process fallback. Supplier publication is atomic; it may be called concurrently.
     *
     * @return the provider's monitor, or {@link ProgressMonitor#NONE} if it returns null
=======
     * Returns the process fallback. The supplier is selected atomically and may be called concurrently.
     *
     * @return the supplier result, or {@link ProgressMonitor#NONE} if that result is null
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
     */
    public static ProgressMonitor system() {
        ProgressMonitor monitor = SYSTEM.get().get();
        return monitor == null ? ProgressMonitor.NONE : monitor;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Update the global fallback and return the previous provider.
     *
     * @param provider the non-null supplier for the process fallback
     * @return the previous fallback provider
=======
     * Atomically replaces the process fallback supplier.
     *
     * @param provider the new fallback supplier
     * @return the previous fallback supplier
     * @throws NullPointerException if provider is null
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
     */
    public static Supplier<? extends ProgressMonitor> setSystem(
            Supplier<? extends ProgressMonitor> provider) {
        return SYSTEM.getAndSet(Objects.requireNonNull(provider, "provider"));
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** Restores the process fallback to the no-op monitor provider. */
    public static void resetSystem() { SYSTEM.set(NONE); }

    /**
     * The lexical monitor, or the process fallback when no scope is active.
     *
     * @return the current thread's bound monitor, or the process fallback
=======
    /**
     * Resets the process fallback supplier to one that returns {@link ProgressMonitor#NONE}.
     */
    public static void resetSystem() { SYSTEM.set(NONE); }

    /**
     * Returns the current lexical monitor, falling back to the process supplier.
     *
     * @return the current monitor, which is never null
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
     */
    public static ProgressMonitor current() {
        Scope scope = CURRENT.get();
        return scope != null ? scope.monitor : system();
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Bind an already-owned monitor: closing this scope does not call done().
     *
     * @param monitor the non-null monitor to bind to the current thread
     * @return a scope to close on this thread in reverse binding order
=======
     * Binds a borrowed monitor to the current thread until the returned scope closes.
     * Closing the scope does not complete the monitor. Scopes must close on their
     * creating thread and in reverse binding order.
     *
     * @param monitor the borrowed monitor
     * @return the lexical scope that restores the previous binding
     * @throws NullPointerException if monitor is null
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
     */
    public static Scope bind(ProgressMonitor monitor) {
        Objects.requireNonNull(monitor, "monitor");
        Scope next = new Scope(CURRENT.get(), monitor);
        CURRENT.set(next);
        return next;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Runs a task with the borrowed monitor bound to the current thread,
     * checking cancellation before and after the task body.
     * Closing the binding does not complete the monitor.
     *
     * @param task the non-null task to run
     * @param monitor the non-null monitor to bind during execution
     */
=======
     * Runs a task under a borrowed monitor without completing the monitor.
     * Cancellation is checked before the task and after its normal completion.
     *
     * @param task the task to run
     * @param monitor the borrowed monitor
     * @throws NullPointerException if either argument is null
     * @throws CancellationException if cancellation is detected
     */
    @SuppressWarnings("try")
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static void run(Runnable task, ProgressMonitor monitor) {
        Objects.requireNonNull(task, "task");
        final Scope scope = bind(monitor);
        try (scope) {
            monitor.checkCancelled();
            task.run();
            monitor.checkCancelled();
        }
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Calls a task with the borrowed monitor bound to the current thread,
     * checking cancellation before and after the task body.
     * Closing the binding does not complete the monitor.
     *
     * @param <T> the task's result type
     * @param task the non-null task to call
     * @param monitor the non-null monitor to bind during execution
     * @return the result computed by the task
     * @throws Exception if the task cannot compute a result
     */
=======
     * Calls a task under a borrowed monitor without completing the monitor.
     * Cancellation is checked before the task and after its normal completion.
     *
     * @param <T> the task result type
     * @param task the task to call
     * @param monitor the borrowed monitor
     * @return the task result
     * @throws Exception if the task fails
     * @throws NullPointerException if either argument is null
     * @throws CancellationException if cancellation is detected
     */
    @SuppressWarnings("try")
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    public static <T> T call(Callable<T> task, ProgressMonitor monitor) throws Exception {
        Objects.requireNonNull(task, "task");
        final Scope scope = bind(monitor);
        try (scope) {
            monitor.checkCancelled();
            T result = task.call();
            monitor.checkCancelled();
            return result;
        }
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Creates a child that credits an allocated portion of its parent's work.
     * A known child total scales reported work; an unknown total credits the
     * allocation only when the child is completed.
     *
     * @param parent the non-null parent monitor
     * @param name the child task label; {@code null} is treated as empty
     * @param parentTicks the non-negative work units allocated in the parent
     * @param totalWork the positive child work total, or {@link ProgressMonitor#UNKNOWN_WORK}
     * @return a child monitor, or {@link ProgressMonitor#NONE} when the parent is {@code NONE}
     * @throws IllegalArgumentException if {@code parentTicks} is negative, or
     *         {@code totalWork} is neither positive nor {@code UNKNOWN_WORK}
=======
     * Creates a weighted child that credits work to the parent without completing it.
     *
     * @param parent the parent monitor
     * @param name the child task name; null is treated as an empty name
     * @param parentTicks the nonnegative number of parent units reserved for the child
     * @param totalWork the positive total child work, or {@link ProgressMonitor#UNKNOWN_WORK}
     * @return the weighted child, or {@link ProgressMonitor#NONE} for a no-op parent
     * @throws NullPointerException if parent is null
     * @throws IllegalArgumentException if parentTicks is negative, or totalWork is zero or below UNKNOWN_WORK
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
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
