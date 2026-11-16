/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A function with the shape {@code (float, double) to reference}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code apply}.
 *
 * @param <R> the result type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface FloatDoubleFunction<R> {
    /**
     * Applies this function to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @return the result
     */
    R apply(float value1, double value2);

    /**
     * Returns a function that passes this result to {@code after}.
     * <p>This function runs first. If it throws, {@code after} is not invoked. The result,
     * including null, is passed unchanged. Composition invokes neither function.
     *
     * @param <W> the composed result type
     * @param after the function to apply second
     * @return the composed function
     * @throws NullPointerException if {@code after} is null
     */
    default <W> FloatDoubleFunction<W> andThen(
            java.util.function.Function<? super R, ? extends W> after) {
        java.util.Objects.requireNonNull(after);
        return (value1, value2) -> after.apply(apply(value1, value2));
    }
}
