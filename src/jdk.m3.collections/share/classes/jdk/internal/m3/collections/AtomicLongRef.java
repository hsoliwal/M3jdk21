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
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
/**
 * VarHandle-based atomic long with update operations.
 *
 * <p>
 * Similar to AtomicLong but uses VarHandle directly.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * AtomicLongRef counter = AtomicLongRef.zero();
 * counter.incrementAndGet();
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/varhandle/AtomicLongRef.java}
 * blob {@code bef6a63613c897d96681a8b5023eb18161486919} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AtomicLongRef {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AtomicLongRef.class);


    private static final VarHandle VALUE_HANDLE;

    static {
        try {
            VALUE_HANDLE = MethodHandles.lookup().findVarHandle(AtomicLongRef.class, "value", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile long value;

    private AtomicLongRef(long initialValue) {
        this.value = initialValue;
    }

    /**
     * Creates an atomic long.
     *
     * @param initialValue
     *            initial value
     * @return atomic long
     */
    public static AtomicLongRef of(long initialValue) {
        return new AtomicLongRef(initialValue);
    }

    /**
     * Creates an atomic long starting at 0.
     *
     * @return atomic long
     */
    public static AtomicLongRef zero() {
        return new AtomicLongRef(0L);
    }

    /**
     * Gets the current value.
     *
     * @return current value
     */
    public long get() {
        return (long) VALUE_HANDLE.getVolatile(this);
    }

    /**
     * Sets the value.
     *
     * @param newValue
     *            new value
     */
    public void set(long newValue) {
        VALUE_HANDLE.setVolatile(this, newValue);
    }

    /**
     * Increments and returns the new value.
     *
     * @return new value after increment
     */
    public long incrementAndGet() {
        return (long) VALUE_HANDLE.getAndAdd(this, 1L) + 1L;
    }

    /**
     * Decrements and returns the new value.
     *
     * @return new value after decrement
     */
    public long decrementAndGet() {
        return (long) VALUE_HANDLE.getAndAdd(this, -1L) - 1L;
    }

    /**
     * Returns and increments.
     *
     * @return old value before increment
     */
    public long getAndIncrement() {
        return (long) VALUE_HANDLE.getAndAdd(this, 1L);
    }

    /**
     * Returns and decrements.
     *
     * @return old value before decrement
     */
    public long getAndDecrement() {
        return (long) VALUE_HANDLE.getAndAdd(this, -1L);
    }

    /**
     * Adds delta and returns new value.
     *
     * @param delta
     *            amount to add
     * @return new value
     */
    public long addAndGet(long delta) {
        return (long) VALUE_HANDLE.getAndAdd(this, delta) + delta;
    }

    /**
     * Returns old value and adds delta.
     *
     * @param delta
     *            amount to add
     * @return old value
     */
    public long getAndAdd(long delta) {
        return (long) VALUE_HANDLE.getAndAdd(this, delta);
    }

    /**
     * Updates and returns new value.
     *
     * @param updateFunction
     *            update function
     * @return new value
     */
    public long updateAndGet(LongUnaryOperator updateFunction) {
        long prev;
        long next;
        do {
            prev = (long) VALUE_HANDLE.getVolatile(this);
            next = updateFunction.applyAsLong(prev);
        } while (!VALUE_HANDLE.compareAndSet(this, prev, next));
        return next;
    }

    /**
     * Accumulates value and returns new value.
     *
     * @param x
     *            operand
     * @param accumulatorFunction
     *            accumulator
     * @return new value
     */
    public long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        long prev;
        long next;
        do {
            prev = (long) VALUE_HANDLE.getVolatile(this);
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!VALUE_HANDLE.compareAndSet(this, prev, next));
        return next;
    }

    /**
     * Compare and set.
     *
     * @param expected
     *            expected value
     * @param newValue
     *            new value
     * @return true if successful
     */
    public boolean compareAndSet(long expected, long newValue) {
        return VALUE_HANDLE.compareAndSet(this, expected, newValue);
    }
}
