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
import java.util.function.Supplier;
/**
 * Lock-free lazy reference holder.
 *
 * <p>
 * Uses VarHandle for thread-safe lazy initialization without locks. Multiple
 * threads may compute the value, but only one will be stored.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * LazyRef<ExpensiveObject> lazy = LazyRef.of(() -> new ExpensiveObject());
 * ExpensiveObject obj = lazy.get();
 * }</pre>
 *
 * @param <T>
 *            value type
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/varhandle/LazyRef.java}
 * blob {@code cef1238a09509190c297087175d0d7d2f7c8bd73} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LazyRef<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(LazyRef.class);


    private static final VarHandle VALUE_HANDLE;

    static {
        try {
            VALUE_HANDLE = MethodHandles.lookup().findVarHandle(LazyRef.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Supplier<T> supplier;

    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile Object value;

    private LazyRef(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a lazy reference.
     *
     * @param supplier
     *            value supplier
     * @param <T>
     *            value type
     * @return lazy reference
     */
    public static <T> LazyRef<T> of(Supplier<T> supplier) {
        return new LazyRef<>(supplier);
    }

    /**
     * Creates a pre-initialized reference.
     *
     * @param value
     *            initial value
     * @param <T>
     *            value type
     * @return initialized reference
     */
    public static <T> LazyRef<T> ofValue(T value) {
        LazyRef<T> ref = new LazyRef<>(() -> value);
        VALUE_HANDLE.setVolatile(ref, value);
        return ref;
    }

    /**
     * Gets the value, computing lazily if needed.
     *
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public T get() {
        Object v = VALUE_HANDLE.getVolatile(this);
        if (v == null) {
            v = supplier.get();
            VALUE_HANDLE.setVolatile(this, v);
        }
        return (T) v;
    }

    /**
     * Checks if value has been computed.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return VALUE_HANDLE.getVolatile(this) != null;
    }

    /** Resets the lazy reference (allows recomputation). */
    public void reset() {
        VALUE_HANDLE.setVolatile(this, null);
    }

    /**
     * Gets value if initialized, otherwise null.
     *
     * @return value or null
     */
    @SuppressWarnings("unchecked")
    public T getIfPresent() {
        return (T) VALUE_HANDLE.getVolatile(this);
    }
}
