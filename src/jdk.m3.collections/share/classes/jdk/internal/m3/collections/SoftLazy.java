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

import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
/**
 * Lazy value holder using SoftReference.
 *
 * <p>
 * The value may be reclaimed by GC under memory pressure. If reclaimed, it will
 * be recomputed on next access.
 * </p>
 *
 * @param <T>
 *            the value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/SoftLazy.java}
 * blob {@code ec5141ae2be6eefb1d1b3dafcb44c2e074fbf1bd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SoftLazy<T> implements Supplier<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(SoftLazy.class);


    private final Supplier<T> supplier;
    private final AtomicReference<SoftReference<T>> ref = new AtomicReference<>();

    SoftLazy(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Supplier must not be null");
    }

    @Override
    public T get() {
        SoftReference<T> softRef = ref.get();
        T value = softRef != null ? softRef.get() : null;

        if (value == null) {
            synchronized (this) {
                softRef = ref.get();
                value = softRef != null ? softRef.get() : null;

                if (value == null) {
                    value = supplier.get();
                    ref.set(new SoftReference<>(value));
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
        SoftReference<T> softRef = ref.get();
        return softRef != null ? Optional.ofNullable(softRef.get()) : Optional.empty();
    }

    /** Clears the soft reference (next get() will recompute). */
    public void clear() {
        ref.set(null);
    }
}
