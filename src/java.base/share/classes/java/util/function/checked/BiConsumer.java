/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked consumer with the shape {@code (reference, reference) to void}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>The exception parameter preserves the declared throwable type. Default operations relay the
 * original throwable without wrapping or catching it and do not change thread interruption status.
 *
 * This is a functional interface whose functional method is {@code accept}.
 *
 * @param <T> an input type
 * @param <U> an input type
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface BiConsumer<T, U, E extends Throwable> {
    /**
     * Performs this operation on the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @throws E if the operation fails
     */
    void accept(T value1, U value2) throws E;

    /**
     * Returns an operation that invokes this operation, then {@code after}.
     * <p>No operation runs until the returned operation is invoked. If this operation throws,
     * {@code after} is not invoked. The original throwable is relayed.
     *
     * @param after the operation to invoke second
     * @return the composed operation
     * @throws NullPointerException if {@code after} is null
     */
    default BiConsumer<T, U, E> andThen(BiConsumer<? super T, ? super U, ? extends E> after) {
        java.util.Objects.requireNonNull(after);
        return (value1, value2) -> {
            accept(value1, value2);
            after.accept(value1, value2);
        };
    }
}
