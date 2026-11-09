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

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
/**
 * Lazy value holder using WeakReference.
 *
 * <p>
 * The value will be reclaimed by GC when no strong references exist. Useful for
 * caches that should not prevent garbage collection.
 * </p>
 *
 * @param <T>
 *            the value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/WeakLazy.java}
 * blob {@code b0692c64a97abdc835cd3141bf525ec04ccccb63} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class WeakLazy<T> implements Supplier<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(WeakLazy.class);


    private final Supplier<T> supplier;
    private final AtomicReference<WeakReference<T>> ref = new AtomicReference<>();

    WeakLazy(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Supplier must not be null");
    }

    @Override
    public T get() {
        WeakReference<T> weakRef = ref.get();
        T value = weakRef != null ? weakRef.get() : null;

        if (value == null) {
            synchronized (this) {
                weakRef = ref.get();
                value = weakRef != null ? weakRef.get() : null;

                if (value == null) {
                    value = supplier.get();
                    ref.set(new WeakReference<>(value));
                }
            }
        }
        return value;
    }

    /**
     * Returns the value if present and not GC'd.
     *
     * @return Optional containing value if present
     */
    public Optional<T> getIfPresent() {
        WeakReference<T> weakRef = ref.get();
        return weakRef != null ? Optional.ofNullable(weakRef.get()) : Optional.empty();
    }

    /** Clears the weak reference. */
    public void clear() {
        ref.set(null);
    }
}
