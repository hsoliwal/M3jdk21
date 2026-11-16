/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A function with the shape {@code (reference, reference, reference, reference, reference) to
 * reference}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code apply}.
 *
 * @param <T1> an input type
 * @param <T2> an input type
 * @param <T3> an input type
 * @param <T4> an input type
 * @param <T5> an input type
 * @param <R> the result type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Function5<T1, T2, T3, T4, T5, R> {
    /**
     * Applies this function to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @param value3 the argument at position 3
     * @param value4 the argument at position 4
     * @param value5 the argument at position 5
     * @return the result
     */
    R apply(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5);

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
    default <W> Function5<T1, T2, T3, T4, T5, W> andThen(
            java.util.function.Function<? super R, ? extends W> after) {
        java.util.Objects.requireNonNull(after);
        return (value1, value2, value3, value4, value5) -> after.apply(apply(value1, value2, value3,
                value4, value5));
    }
}
