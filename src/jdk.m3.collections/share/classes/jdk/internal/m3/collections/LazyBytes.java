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
 * Lock-free lazy byte array holder.
 *
 * <p>
 * Ideal for lazy SHA-256 computation, serialization, etc.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * LazyBytes hash = LazyBytes.of(() -> computeSHA256(data));
 * byte[] h = hash.get();
 * }</pre>
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/varhandle/LazyBytes.java}
 * blob {@code b82d59f4390502ac7b83ce4912f83e32868bcc49} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LazyBytes {
    private static final PlaceholderLogger log = PlaceholderLogger.of(LazyBytes.class);


    private static final VarHandle VALUE_HANDLE;

    static {
        try {
            VALUE_HANDLE = MethodHandles.lookup().findVarHandle(LazyBytes.class, "value", byte[].class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Supplier<byte[]> supplier;

    @SuppressWarnings("unused") // accessed via VarHandle
    private volatile byte[] value;

    private LazyBytes(Supplier<byte[]> supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a lazy byte array.
     *
     * @param supplier
     *            value supplier
     * @return lazy bytes
     */
    public static LazyBytes of(Supplier<byte[]> supplier) {
        return new LazyBytes(supplier);
    }

    /**
     * Creates a pre-initialized lazy byte array.
     *
     * @param value
     *            initial value
     * @return initialized lazy bytes
     */
    public static LazyBytes ofValue(byte[] value) {
        LazyBytes lazy = new LazyBytes(() -> value);
        VALUE_HANDLE.setVolatile(lazy, value);
        return lazy;
    }

    /**
     * Gets the value, computing lazily if needed.
     *
     * @return the byte array
     */
    public byte[] get() {
        byte[] v = (byte[]) VALUE_HANDLE.getVolatile(this);
        if (v == null) {
            v = supplier.get();
            VALUE_HANDLE.setVolatile(this, v);
        }
        return v;
    }

    /**
     * Checks if value has been computed.
     *
     * @return true if computed
     */
    public boolean isComputed() {
        return VALUE_HANDLE.getVolatile(this) != null;
    }

    /** Resets for recomputation. */
    public void reset() {
        VALUE_HANDLE.setVolatile(this, null);
    }
}
