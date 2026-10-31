/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.file;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Java 21 provider-scoped, hierarchical exclusive access for M3File clients.
 * A source file/provider remains the original authority for I/O, symlinks,
 * permissions and external writes. This coordinator only governs participating
 * threads in this process; it is not an OS file lock or Eclipse JobManager.
 * No JNI callback, queue of element nodes, or second filesystem is created.
 */
public final class M3ResourceLocks {
    /** Preserve the accessible no-argument construction contract explicitly. */
    public M3ResourceLocks() { }
    /**
     * Immutable lexical scope over one provider-owned absolute NIO Path.
     * A subtree scope covers its root and descendants; an exact scope covers
     * only its root. Paths are normalized *lexically*, without any file I/O or
     * symlink resolution. Different FileSystem instances are independent.
     */
    public record Scope(Path path, boolean descendants) {
        public Scope {
            path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        }

        public static Scope exact(Path path) { return new Scope(path, false); }
        public static Scope subtree(Path path) { return new Scope(path, true); }

        public boolean contains(Scope other) {
            if (other == null || path.getFileSystem() != other.path.getFileSystem()) return false;
            if (path.equals(other.path)) return descendants || !other.descendants;
            return descendants && other.path.startsWith(path);
        }

        /** Symmetric, reflexive exclusion. Prefix-looking siblings never collide. */
        public boolean conflicts(Scope other) {
            if (other == null || path.getFileSystem() != other.path.getFileSystem()) return false;
            return path.equals(other.path)
                    || (descendants && other.path.startsWith(path))
                    || (other.descendants && path.startsWith(other.path));
        }
    }

    /** Exclusive caller-owned, thread-confined acquisition handle. */
    public final class Lease implements AutoCloseable {
        private final Thread owner;
        private final Scope scope;
        private final Lease parent;
        private boolean closed;

        private Lease(Thread owner, Scope scope, Lease parent) {
            this.owner = owner;
            this.scope = scope;
            this.parent = parent;
        }

        public Scope scope() { return scope; }
        public boolean isClosed() { return closed; }

        /** Enforce exact acquiring thread and strict LIFO, including nested rules. */
        @Override public void close() {
            if (Thread.currentThread() != owner) throw new IllegalStateException("foreign resource lease owner");
            mutex.lock();
            try {
                if (closed) throw new IllegalStateException("resource lease already closed");
                if (top.get() != this) throw new IllegalStateException("resource leases close in LIFO order");
                int found = -1;
                for (int i = 0; i < used; i++) {
                    if (active[i] == this) { found = i; break; }
                }
                if (found < 0) throw new IllegalStateException("resource lease not registered");
                System.arraycopy(active, found + 1, active, found, used - found - 1);
                active[--used] = null;
                closed = true;
                if (parent == null) top.remove(); else top.set(parent);
                available.signalAll();
            } finally {
                mutex.unlock();
            }
        }
    }

    private final ReentrantLock mutex = new ReentrantLock(true);
    private final Condition available = mutex.newCondition();
    private final ThreadLocal<Lease> top = new ThreadLocal<>();
    private Lease[] active = new M3ResourceLocks.Lease[4];
    private int used;

    /** Current number of held scopes, including nested leases, not queued tasks. */
    public int activeScopes() {
        mutex.lock();
        try { return used; } finally { mutex.unlock(); }
    }

    /**
     * Acquire without holding any unrelated Java monitor while waiting.
     * Interruption refuses acquisition before publication. Same-thread nesting
     * is allowed only when the new scope is contained by the current scope,
     * avoiding implicit lock upgrades or unrelated-resource deadlocks.
     */
    public Lease acquire(Scope scope) throws InterruptedException {
        Objects.requireNonNull(scope, "scope");
        mutex.lockInterruptibly();
        try {
            Lease prior = top.get();
            requireNested(prior, scope);
            while (conflicted(scope, Thread.currentThread())) available.await();
            return admit(scope, prior);
        } finally {
            mutex.unlock();
        }
    }

    /** Returns null rather than blocking on a competing owner. */
    public Lease tryAcquire(Scope scope) {
        Objects.requireNonNull(scope, "scope");
        if (!mutex.tryLock()) return null;
        try {
            Lease prior = top.get();
            requireNested(prior, scope);
            return conflicted(scope, Thread.currentThread()) ? null : admit(scope, prior);
        } finally {
            mutex.unlock();
        }
    }

    /**
     * Wait at most the specified total budget for a participating scope.
     * Timeout returns null without publishing a lease. Interruption aborts
     * without acquiring ownership. The budget includes mutex contention and
     * condition waiting; NIO provider and Eclipse scheduling rules stay owned
     * by their original implementations.
     */
    public Lease tryAcquire(Scope scope, long timeout, TimeUnit unit)
            throws InterruptedException {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(unit, "unit");
        if (timeout < 0L) throw new IllegalArgumentException("negative resource wait");
        long budget = unit.toNanos(timeout);
        long started = System.nanoTime();
        if (!mutex.tryLock(budget, TimeUnit.NANOSECONDS)) return null;
        try {
            Lease prior = top.get();
            requireNested(prior, scope);
            while (conflicted(scope, Thread.currentThread())) {
                long remaining = budget - (System.nanoTime() - started);
                if (remaining <= 0L) return null;
                available.awaitNanos(remaining);
            }
            return admit(scope, prior);
        } finally {
            mutex.unlock();
        }
    }

    /** Execute an ordinary Java Runnable in a release-safe resource scope. */
    @SuppressWarnings("try")
    public void run(Scope scope, Runnable operation) throws InterruptedException {
        Objects.requireNonNull(operation, "operation");
        try (Lease ignored = acquire(scope)) { operation.run(); }
    }

    /** Execute an ordinary Java Callable without changing its result or exception. */
    @SuppressWarnings("try")
    public <T> T call(Scope scope, Callable<T> operation) throws Exception {
        Objects.requireNonNull(operation, "operation");
        try (Lease ignored = acquire(scope)) { return operation.call(); }
    }

    private static void requireNested(Lease previous, Scope requested) {
        if (previous != null && !previous.scope.contains(requested)) {
            throw new IllegalStateException("nested scope not contained in existing resource scope");
        }
    }

    private boolean conflicted(Scope requested, Thread self) {
        for (int i = 0; i < used; i++) {
            Lease current = active[i];
            if (current.owner != self && current.scope.conflicts(requested)) return true;
        }
        return false;
    }

    private Lease admit(Scope scope, Lease previous) {
        if (used == active.length) active = Arrays.copyOf(active, Math.addExact(used, Math.max(4, used >>> 1)));
        Lease acquired = new Lease(Thread.currentThread(), scope, previous);
        active[used++] = acquired;
        top.set(acquired);
        return acquired;
    }
}
