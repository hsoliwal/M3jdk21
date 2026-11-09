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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
/**
 * Thread-safe lazy value holder with memoization.
 *
 * <p>
 * The value is computed exactly once on first access and cached. Uses
 * double-checked locking for thread safety with minimal overhead.
 * </p>
 *
 * @param <T>
 *            the value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/Lazy.java}
 * blob {@code 9db7ef9bb2331f0156d3632a5db600482919c188} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Lazy<T> implements Supplier<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(Lazy.class);


    private final Supplier<T> supplier;
    private volatile T value;
    private volatile boolean initialized;

    Lazy(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Supplier must not be null");
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = supplier.get();
                    initialized = true;
                }
            }
        }
        return value;
    }

    /**
     * Returns the value if initialized, otherwise empty.
     *
     * @return Optional containing value if initialized
     */
    public Optional<T> getIfPresent() {
        return initialized ? Optional.ofNullable(value) : Optional.empty();
    }

    /**
     * Checks if value has been initialized.
     *
     * @return true if get() has been called
     */
    public boolean isInitialized() {
        return initialized;
    }

    /** Resets the lazy to uninitialized state (next get() will recompute). */
    public synchronized void reset() {
        value = null;
        initialized = false;
    }

    /**
     * Maps the lazy value to another type.
     *
     * @param mapper
     *            the mapping function
     * @param <U>
     *            the target type
     * @return a new Lazy with mapped value
     */
    public <U> Lazy<U> map(Function<T, U> mapper) {
        return new Lazy<>(() -> mapper.apply(get()));
    }

    /**
     * FlatMaps the lazy value.
     *
     * @param mapper
     *            the mapping function returning Lazy
     * @param <U>
     *            the target type
     * @return a new Lazy with flatMapped value
     */
    public <U> Lazy<U> flatMap(Function<T, Lazy<U>> mapper) {
        return new Lazy<>(() -> mapper.apply(get()).get());
    }
}
