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
 * Lock-free lazy long value holder.
 *
 * <p>
 * Ideal for lazy hash computation, counters, etc.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * LazyLong hash = LazyLong.of(() -> computeHash(data));
 * long h = hash.get();
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/varhandle/LazyLong.java}
 * blob {@code f5d0cd7164c8a257bafaaf031a873c38f13bbfb0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LazyLong {
    private static final PlaceholderLogger log = PlaceholderLogger.of(LazyLong.class);


    private static final VarHandle VALUE_HANDLE;
    private static final VarHandle COMPUTED_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            VALUE_HANDLE = lookup.findVarHandle(LazyLong.class, "value", long.class);
            COMPUTED_HANDLE = lookup.findVarHandle(LazyLong.class, "computed", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final LongSupplier supplier;

    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile long value;

    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile boolean computed;

    private LazyLong(LongSupplier supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a lazy long.
     *
     * @param supplier
     *            value supplier
     * @return lazy long
     */
    public static LazyLong of(LongSupplier supplier) {
        return new LazyLong(supplier);
    }

    /**
     * Creates a pre-initialized lazy long.
     *
     * @param value
     *            initial value
     * @return initialized lazy long
     */
    public static LazyLong ofValue(long value) {
        LazyLong lazy = new LazyLong(() -> value);
        VALUE_HANDLE.setVolatile(lazy, value);
        COMPUTED_HANDLE.setVolatile(lazy, true);
        return lazy;
    }

    /**
     * Gets the value, computing lazily if needed.
     *
     * @return the value
     */
    public long get() {
        if (!(boolean) COMPUTED_HANDLE.getVolatile(this)) {
            long v = supplier.getAsLong();
            VALUE_HANDLE.setVolatile(this, v);
            COMPUTED_HANDLE.setVolatile(this, true);
        }
        return (long) VALUE_HANDLE.getVolatile(this);
    }

    /**
     * Checks if value has been computed.
     *
     * @return true if computed
     */
    public boolean isComputed() {
        return (boolean) COMPUTED_HANDLE.getVolatile(this);
    }

    /** Resets for recomputation. */
    public void reset() {
        COMPUTED_HANDLE.setVolatile(this, false);
    }

    /** Functional interface for long suppliers (avoids boxing). */
    @FunctionalInterface
    public interface LongSupplier {

        /**
         * Gets a long value.
         *
         * @return long value
         */
        long getAsLong();
    }
}
