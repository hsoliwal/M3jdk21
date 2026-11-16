/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A consumer with the shape {@code (boolean, boolean) to void}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code accept}.
 *
 * @since M3JDK 21
 */
@FunctionalInterface
public interface BooleanBiConsumer {
    /**
     * Performs this operation on the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     */
    void accept(boolean value1, boolean value2);

    /**
     * Returns an operation that invokes this operation, then {@code after}.
     * <p>No operation runs until the returned operation is invoked. If this operation throws,
     * {@code after} is not invoked. The original throwable is relayed.
     *
     * @param after the operation to invoke second
     * @return the composed operation
     * @throws NullPointerException if {@code after} is null
     */
    default BooleanBiConsumer andThen(BooleanBiConsumer after) {
        java.util.Objects.requireNonNull(after);
        return (value1, value2) -> {
            accept(value1, value2);
            after.accept(value1, value2);
        };
    }
}
