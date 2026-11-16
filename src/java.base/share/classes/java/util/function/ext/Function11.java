/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A function with the shape {@code (reference, reference, reference, reference, reference,
 * reference, reference, reference, reference, reference, reference) to reference}.
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
 * @param <T6> an input type
 * @param <T7> an input type
 * @param <T8> an input type
 * @param <T9> an input type
 * @param <U1> an input type
 * @param <U2> an input type
 * @param <R> the result type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, U1, U2, R> {
    /**
     * Applies this function to the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @param value3 the argument at position 3
     * @param value4 the argument at position 4
     * @param value5 the argument at position 5
     * @param value6 the argument at position 6
     * @param value7 the argument at position 7
     * @param value8 the argument at position 8
     * @param value9 the argument at position 9
     * @param value10 the argument at position 10
     * @param value11 the argument at position 11
     * @return the result
     */
    R apply(
            T1 value1,
            T2 value2,
            T3 value3,
            T4 value4,
            T5 value5,
            T6 value6,
            T7 value7,
            T8 value8,
            T9 value9,
            U1 value10,
            U2 value11);

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
    default <W> Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, U1, U2, W> andThen(
            java.util.function.Function<? super R, ? extends W> after) {
        java.util.Objects.requireNonNull(after);
        return (value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
                value11) -> after.apply(apply(value1, value2, value3, value4, value5, value6,
                value7, value8, value9, value10, value11));
    }
}
