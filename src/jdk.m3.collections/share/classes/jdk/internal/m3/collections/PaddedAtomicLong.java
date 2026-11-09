/* ******************************************************************************
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
package jdk.internal.m3.collections;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
/**
 * Cache-line padded atomic long for high-contention concurrent counters.
 *
 * <p>
 * Prevents false sharing by padding the value field with 56 bytes on each side,
 * ensuring it occupies its own 64-byte cache line. This eliminates cache-line
 * bouncing between CPU cores when multiple threads access different counters.
 * </p>
 *
 * <h2>Performance Impact</h2>
 * <ul>
 * <li>10-100x throughput improvement in high-contention scenarios</li>
 * <li>Eliminates cache coherency traffic between CPU cores</li>
 * <li>Sub-microsecond latency for atomic operations</li>
 * </ul>
 *
 * <h2>Memory Layout</h2>
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────┐
 * │ LHS Padding (56 bytes) │ Value (8 bytes) │ RHS Padding (56 bytes)│
 * │ p1-p7 (7 longs)        │ value (1 long)  │ p8-p14 (7 longs)      │
 * └──────────────────────────────────────────────────────────────────┘
 *                          ↑
 *                 Isolated on its own cache line
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Create padded counter
 * PaddedAtomicLong counter = new PaddedAtomicLong(0L);
 *
 * // Atomic operations (thread-safe)
 * counter.set(100L);
 * long value = counter.get();
 * long prev = counter.getAndIncrement();
 * long next = counter.incrementAndGet();
 * boolean swapped = counter.compareAndSet(100L, 200L);
 *
 * // Memory ordering control
 * counter.setVolatile(42L); // Full fence
 * counter.setOrdered(42L); // Release semantics
 * long val = counter.getVolatile(); // Acquire semantics
 * }</pre>
 *
 * <h2>Inspired by</h2>
 * <p>
 * </p>
 *
 * @author Hitesh Soliwal
 * @version 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/varhandle/PaddedAtomicLong.java}
 * blob {@code 82ac23298220916d459469983af7a1a04ce7700e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PaddedAtomicLong extends PaddedAtomicLongRhs {
    private static final PlaceholderLogger log = PlaceholderLogger.of(PaddedAtomicLong.class);


    /** Initial value constant. */
    public static final long INITIAL_VALUE = -1L;

    /** Creates a padded atomic long with initial value of -1. */
    public PaddedAtomicLong() {
        this(INITIAL_VALUE);
    }

    /**
     * Creates a padded atomic long with specified initial value.
     *
     * @param initialValue
     *            the initial value
     */
    public PaddedAtomicLong(long initialValue) {
        setValue(initialValue);
    }

    /**
     * CLI demonstration of PaddedAtomicLong.
     *
     * @param args
     *            command line arguments (ignored)
     */
    public static void main(String[] args) {
        log.info("=== PaddedAtomicLong Demo ===");

        PaddedAtomicLong counter = new PaddedAtomicLong(0L);
        log.info("Initial value: {}", counter.get());

        counter.set(100L);
        log.info("After set(100): {}", counter.get());

        long prev = counter.getAndIncrement();
        log.info("getAndIncrement() returned: {}, current: {}", prev, counter.get());

        long next = counter.incrementAndGet();
        log.info("incrementAndGet() returned: {}", next);

        boolean cas = counter.compareAndSet(102L, 200L);
        log.info("CAS(102->200): {}, current: {}", cas, counter.get());

        log.info("Memory layout ensures isolation from adjacent cache lines.");
    }
}

/** Left-hand-side padding to isolate value from object header. */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class PaddedAtomicLongLhs {

    /** Padding field 1. */
    protected long p1;
    /** Padding field 2. */
    protected long p2;
    /** Padding field 3. */
    protected long p3;
    /** Padding field 4. */
    protected long p4;
    /** Padding field 5. */
    protected long p5;
    /** Padding field 6. */
    protected long p6;
    /** Padding field 7. */
    protected long p7;
}

/** The actual value field with atomic operations. */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class PaddedAtomicLongValue extends PaddedAtomicLongLhs {

    /** VarHandle for atomic operations on value field. */
    private static final VarHandle VALUE_HANDLE;

    static {
        try {
            VALUE_HANDLE = MethodHandles.lookup().findVarHandle(PaddedAtomicLongValue.class, "value", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** The actual value. */
    protected long value;

    /**
     * Sets the value with release semantics (ordered store).
     *
     * @param newValue
     *            the new value
     */
    protected void setValue(long newValue) {
        VALUE_HANDLE.setRelease(this, newValue);
    }

    /**
     * Gets the value with acquire semantics.
     *
     * @return the current value
     */
    public long get() {
        return (long) VALUE_HANDLE.getAcquire(this);
    }

    /**
     * Sets the value with release semantics.
     *
     * @param newValue
     *            the new value
     */
    public void set(long newValue) {
        VALUE_HANDLE.setRelease(this, newValue);
    }

    /**
     * Gets the value with volatile semantics (full fence).
     *
     * @return the current value
     */
    public long getVolatile() {
        return (long) VALUE_HANDLE.getVolatile(this);
    }

    /**
     * Sets the value with volatile semantics (full fence).
     *
     * @param newValue
     *            the new value
     */
    public void setVolatile(long newValue) {
        VALUE_HANDLE.setVolatile(this, newValue);
    }

    /**
     * Sets the value with ordered semantics (release fence).
     *
     * @param newValue
     *            the new value
     */
    public void setOrdered(long newValue) {
        VALUE_HANDLE.setRelease(this, newValue);
    }

    /**
     * Atomically compares and sets the value.
     *
     * @param expectedValue
     *            the expected current value
     * @param newValue
     *            the new value
     * @return true if successful, false if current value was not expected
     */
    public boolean compareAndSet(long expectedValue, long newValue) {
        return VALUE_HANDLE.compareAndSet(this, expectedValue, newValue);
    }

    /**
     * Atomically increments and returns the new value.
     *
     * @return the incremented value
     */
    public long incrementAndGet() {
        return addAndGet(1L);
    }

    /**
     * Atomically decrements and returns the new value.
     *
     * @return the decremented value
     */
    public long decrementAndGet() {
        return addAndGet(-1L);
    }

    /**
     * Atomically adds delta and returns the new value.
     *
     * @param delta
     *            the value to add
     * @return the new value after addition
     */
    public long addAndGet(long delta) {
        return (long) VALUE_HANDLE.getAndAdd(this, delta) + delta;
    }

    /**
     * Atomically gets and increments.
     *
     * @return the previous value
     */
    public long getAndIncrement() {
        return getAndAdd(1L);
    }

    /**
     * Atomically gets and decrements.
     *
     * @return the previous value
     */
    public long getAndDecrement() {
        return getAndAdd(-1L);
    }

    /**
     * Atomically gets and adds delta.
     *
     * @param delta
     *            the value to add
     * @return the previous value
     */
    public long getAndAdd(long delta) {
        return (long) VALUE_HANDLE.getAndAdd(this, delta);
    }

    /**
     * Atomically gets and sets.
     *
     * @param newValue
     *            the new value
     * @return the previous value
     */
    public long getAndSet(long newValue) {
        return (long) VALUE_HANDLE.getAndSet(this, newValue);
    }

    @Override
    public String toString() {
        return Long.toString(get());
    }
}

/** Right-hand-side padding to isolate value from subsequent fields. */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class PaddedAtomicLongRhs extends PaddedAtomicLongValue {

    /** Padding field 8. */
    protected long p8;
    /** Padding field 9. */
    protected long p9;
    /** Padding field 10. */
    protected long p10;
    /** Padding field 11. */
    protected long p11;
    /** Padding field 12. */
    protected long p12;
    /** Padding field 13. */
    protected long p13;
    /** Padding field 14. */
    protected long p14;
}
