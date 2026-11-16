/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked consumer with the shape {@code () to void}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>The exception parameter preserves the declared throwable type. Default operations relay the
 * original throwable without wrapping or catching it and do not change thread interruption status.
 *
 * This is a functional interface whose functional method is {@code run}.
 *
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Runnable<E extends Throwable> {
    /**
     * Performs this operation on the supplied arguments.
     *
     * @throws E if the operation fails
     */
    void run() throws E;

    /**
     * Returns an operation that invokes this operation, then {@code after}.
     * <p>No operation runs until the returned operation is invoked. If this operation throws,
     * {@code after} is not invoked. The original throwable is relayed.
     *
     * @param after the operation to invoke second
     * @return the composed operation
     * @throws NullPointerException if {@code after} is null
     */
    default Runnable<E> andThen(Runnable<? extends E> after) {
        java.util.Objects.requireNonNull(after);
        return () -> {
            run();
            after.run();
        };
    }
}
