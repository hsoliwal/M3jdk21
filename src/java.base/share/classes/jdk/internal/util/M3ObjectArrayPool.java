/*
 * Copyright (c) 2026, Hitesh Soliwal.
 * SPDX-License-Identifier: Apache-2.0
 *
 * M3 runtime implementation. No Synexia runtime dependency.
 */
package jdk.internal.util;

import java.util.Arrays;

/**
 * Explicit, bounded, thread-confined reuse of temporary reference arrays.
 *
 * <p>This is NOT an allocator replacement. Fresh TLAB allocation may be faster.
 * Use only after profiling a proven, escaping temporary-array hot path.
 * A lease must never escape the owning operation, and callers must not retain
 * its array after close. This contract cannot be enforced for raw Object[].
 *
 * <p>Exact lengths 1..32 are supported, plus 64, 128 and 256.
 * Each thread caches at most one array of each length. No JNI object-address
 * assumptions, no global locks, and no references retained after release.
 */
public final class M3ObjectArrayPool {
    private static final int SMALL_LIMIT = 32;
    private static final int BUCKETS = SMALL_LIMIT + 3;
    private static final ThreadLocal<Object[][]> LOCAL =
            ThreadLocal.withInitial(() -> new Object[BUCKETS][]);

    private M3ObjectArrayPool() {}

    private static int bucket(int length) {
        if (length >= 1 && length <= SMALL_LIMIT) return length - 1;
        if (length == 64) return SMALL_LIMIT;
        if (length == 128) return SMALL_LIMIT + 1;
        if (length == 256) return SMALL_LIMIT + 2;
        return -1;
    }

    /** Acquire a thread-confined lease; unsupported sizes allocate normally. */
    public static Lease acquire(int length) {
        if (length < 0) throw new NegativeArraySizeException();
        int index = bucket(length);
        Object[] array = null;
        if (index >= 0) {
            Object[][] slots = LOCAL.get();
            array = slots[index];
            slots[index] = null;
        }
        if (array == null) array = new Object[length];
        return new Lease(array, index, Thread.currentThread());
    }

    /** Clears the calling thread's cache; useful for long-lived worker threads. */
    public static void clearThreadCache() {
        LOCAL.remove();
    }

    public static final class Lease implements AutoCloseable {
        private Object[] array;
        private final int index;
        private final Thread owner;

        private Lease(Object[] array, int index, Thread owner) {
            this.array = array;
            this.index = index;
            this.owner = owner;
        }

        /** Borrowed array; never retain it beyond close(). */
        public Object[] array() {
            checkOwner();
            if (array == null) throw new IllegalStateException("Lease closed");
            return array;
        }

        private void checkOwner() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("Cross-thread array lease");
            }
        }

        @Override
        public void close() {
            checkOwner();
            Object[] released = array;
            if (released == null) return;
            array = null;
            // Clear even when the cache is already occupied.
            Arrays.fill(released, null);
            if (index >= 0) {
                Object[][] slots = LOCAL.get();
                if (slots[index] == null) slots[index] = released;
            }
        }
    }
}
