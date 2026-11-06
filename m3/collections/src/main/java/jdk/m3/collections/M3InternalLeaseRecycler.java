/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/**
 * Bounded, exclusive reuse of compiler-qualified non-escaping internal scratch objects.
 * Never pass publicly exposed facades, JNI handles, listeners, synchronized identity
 * owners, or objects with observable constructor/disposal behavior to this recycler.
 *
 * <p>Completion of a callback does not prove absence of escaped references. The
 * caller must establish that fact independently; a stale captured reference could
 * otherwise observe a later rebinding. No GC callback, resurrection, finalizer,
 * per-borrow lease wrapper, or alternative cache/pressure sampler is used.</p>
 *
 * <p>Checkout removes an idle object before executing user code. Nested and
 * concurrent borrows cannot receive the same instance. Clearing executes on
 * every exit, including callback failure; failed clearing drops the object.
 * Existing {@link M3RuntimeHeap.Pressure} observations control optional idle
 * retention at explicit checkpoints, never the correctness of a live borrow.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/M3InternalLeaseRecycler.java}
 * blob {@code 132707c53896011d461afe800ea6ace09d474498} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3InternalLeaseRecycler<T> implements AutoCloseable {
    public record Stats(int idle, int active, long created, long reused,
                        long resets, long discarded) { }

    private final Supplier<? extends T> factory;
    private final ObjLongConsumer<? super T> rebind;
    private final Consumer<? super T> reset;
    /** No per-instance spare array until an internal object actually returns. */
    private static final Object[] EMPTY_IDLE = new Object[0];
    private final int maxIdle;
    private Object[] idle = EMPTY_IDLE;
    private int idleSize;
    private int retentionLimit;
    private int active;
    private long created;
    private long reused;
    private long resets;
    private long discarded;
    private boolean closed;

    public M3InternalLeaseRecycler(Supplier<? extends T> factory,
            ObjLongConsumer<? super T> rebind, Consumer<? super T> reset,
            int maxIdle) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.rebind = Objects.requireNonNull(rebind, "rebind");
        this.reset = Objects.requireNonNull(reset, "reset");
        if (maxIdle < 0 || maxIdle > 65536) {
            throw new IllegalArgumentException("maxIdle out of bounds");
        }
        this.maxIdle = maxIdle;
        this.retentionLimit = maxIdle;
    }

    /**
     * A lexical, exclusive borrow. The callback MUST NOT retain the object,
     * hand it to another thread, or publish it to any observable Java API.
     */
    public void withBorrowed(long logicalId, Consumer<? super T> action) {
        if (logicalId < 0) throw new IllegalArgumentException("negative logicalId");
        Objects.requireNonNull(action, "action");
        T value;
        synchronized (this) {
            requireOpen();
            active = Math.addExact(active, 1);
            if (idleSize != 0) {
                value = idleValue(--idleSize);
                idle[idleSize] = null;
                reused++;
            } else value = null;
        }
        if (value == null) {
            try {
                value = Objects.requireNonNull(factory.get(), "factory result");
                synchronized (this) { created++; }
            } catch (RuntimeException | Error failure) {
                synchronized (this) { active--; }
                throw failure;
            }
        }

        Throwable primary = null;
        try {
            // The factory may close the owner before returning a new object.
            // The owner lock makes that transition visible before any rebind.
            synchronized (this) { requireOpen(); }
            rebind.accept(value, logicalId);
            // A user-supplied rebind callback may itself reenter close().
            // A closed owner must never invoke a subsequent borrower action.
            synchronized (this) { requireOpen(); }
            action.accept(value);
        } catch (RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            release(value, primary);
        }
    }

    /** Never drop a live borrow; release only optional idle references. */
    public synchronized int trimIdle(int target) {
        if (target < 0) throw new IllegalArgumentException("negative target");
        int removed = 0;
        while (idleSize > target) {
            idle[--idleSize] = null;
            removed++;
        }
        discarded += removed;
        // High pressure, close and explicit trim-to-zero release spare backing too.
        // No new allocation, JNI call or GC invocation at this boundary.
        if (target == 0 && idleSize == 0) idle = EMPTY_IDLE;
        return removed;
    }

    /** Reuse the existing heap-pressure plane: no per-borrow JVM heap sampling. */
    public synchronized int onPressure(M3RuntimeHeap.Pressure pressure) {
        Objects.requireNonNull(pressure, "pressure");
        retentionLimit = switch (pressure) {
            case NORMAL -> maxIdle;
            case WARM -> maxIdle / 4;
            case HIGH -> 0;
        };
        return trimIdle(retentionLimit);
    }

    public synchronized Stats stats() {
        return new Stats(idleSize, active, created, reused, resets, discarded);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        retentionLimit = 0;
        trimIdle(0);
    }

    private void release(T value, Throwable primary) {
        boolean sanitized = false;
        try {
            reset.accept(value);
            sanitized = true;
        } catch (RuntimeException | Error failure) {
            if (primary == null) throw failure;
            if (primary != failure) primary.addSuppressed(failure);
        } finally {
            synchronized (this) {
                active--;
                if (sanitized) {
                    resets++;
                    if (!closed && idleSize < retentionLimit) {
                        // Grow only at check-in boundaries, never on a borrow hit.
                        // A failed optional growth may not replace a callback failure.
                        try {
                            ensureIdleCapacity(idleSize + 1);
                            idle[idleSize++] = value;
                        } catch (RuntimeException | Error failure) {
                            discarded++;
                            if (primary == null) throw failure;
                            if (primary != failure) primary.addSuppressed(failure);
                        }
                    } else discarded++;
                } else discarded++;
            }
        }
    }

    private void ensureIdleCapacity(int required) {
        if (required <= idle.length) return;
        // Capacity is bounded by the original maxIdle contract and the current
        // pressure policy. Geometric growth makes repeated nested check-ins
        // amortized O(1), without an eagerly reserved 65,536-reference array.
        int expanded = idle.length + Math.max(4, idle.length >>> 1);
        int capacity = Math.min(retentionLimit, Math.max(required, expanded));
        idle = Arrays.copyOf(idle, capacity);
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("closed recycler");
    }

    @SuppressWarnings("unchecked")
    private T idleValue(int index) { return (T) idle[index]; }
}
