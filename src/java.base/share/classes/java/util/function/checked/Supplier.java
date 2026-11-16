/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked supplier with the shape {@code () to reference}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>The exception parameter preserves the declared throwable type. Default operations relay the
 * original throwable without wrapping or catching it and do not change thread interruption status.
 *
 * This is a functional interface whose functional method is {@code get}.
 *
 * @param <R> the result type
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Supplier<R, E extends Throwable> {
    /**
     * Obtains a result from this supplier.
     *
     * @return the result
     * @throws E if the operation fails
     */
    R get() throws E;
}
