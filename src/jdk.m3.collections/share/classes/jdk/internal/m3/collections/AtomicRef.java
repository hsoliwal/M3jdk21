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
import java.util.function.UnaryOperator;
/**
 * VarHandle-based atomic reference with update operations.
 *
 * <p>
 * Provides updateAndGet, getAndUpdate, compareAndSet operations.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * AtomicRef<Config> config = AtomicRef.of(initialConfig);
 * config.updateAndGet(c -> c.withTimeout(5000));
 * }</pre>
 *
 * @param <T>
 *            value type
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/varhandle/AtomicRef.java}
 * blob {@code 0ae82d2f2e64a3e008bbf4e2e7b54e779ff9ca88} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AtomicRef<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AtomicRef.class);


    private static final VarHandle VALUE_HANDLE;

    static {
        try {
            VALUE_HANDLE = MethodHandles.lookup().findVarHandle(AtomicRef.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile Object value;

    private AtomicRef(T initialValue) {
        this.value = initialValue;
    }

    /**
     * Creates an atomic reference.
     *
     * @param initialValue
     *            initial value
     * @param <T>
     *            value type
     * @return atomic reference
     */
    public static <T> AtomicRef<T> of(T initialValue) {
        return new AtomicRef<>(initialValue);
    }

    /**
     * Gets the current value.
     *
     * @return current value
     */
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) VALUE_HANDLE.getVolatile(this);
    }

    /**
     * Sets the value.
     *
     * @param newValue
     *            new value
     */
    public void set(T newValue) {
        VALUE_HANDLE.setVolatile(this, newValue);
    }

    /**
     * Atomically updates the value and returns the new value.
     *
     * @param updateFunction
     *            update function
     * @return the new value
     */
    @SuppressWarnings("unchecked")
    public T updateAndGet(UnaryOperator<T> updateFunction) {
        T prev;
        T next;
        do {
            prev = (T) VALUE_HANDLE.getVolatile(this);
            next = updateFunction.apply(prev);
        } while (!VALUE_HANDLE.compareAndSet(this, prev, next));
        return next;
    }

    /**
     * Atomically updates the value and returns the old value.
     *
     * @param updateFunction
     *            update function
     * @return the old value
     */
    @SuppressWarnings("unchecked")
    public T getAndUpdate(UnaryOperator<T> updateFunction) {
        T prev;
        T next;
        do {
            prev = (T) VALUE_HANDLE.getVolatile(this);
            next = updateFunction.apply(prev);
        } while (!VALUE_HANDLE.compareAndSet(this, prev, next));
        return prev;
    }

    /**
     * Atomically sets if current value equals expected.
     *
     * @param expected
     *            expected value
     * @param newValue
     *            new value
     * @return true if successful
     */
    public boolean compareAndSet(T expected, T newValue) {
        return VALUE_HANDLE.compareAndSet(this, expected, newValue);
    }

    /**
     * Gets and sets atomically.
     *
     * @param newValue
     *            new value
     * @return old value
     */
    @SuppressWarnings("unchecked")
    public T getAndSet(T newValue) {
        return (T) VALUE_HANDLE.getAndSet(this, newValue);
    }
}
