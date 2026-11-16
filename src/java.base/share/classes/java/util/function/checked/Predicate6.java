/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked predicate with the shape {@code (reference, reference, reference, reference, reference,
 * reference) to boolean}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * <p>The exception parameter preserves the declared throwable type. Default operations relay the
 * original throwable without wrapping or catching it and do not change thread interruption status.
 *
 * This is a functional interface whose functional method is {@code test}.
 *
 * @param <T1> an input type
 * @param <T2> an input type
 * @param <T3> an input type
 * @param <T4> an input type
 * @param <T5> an input type
 * @param <T6> an input type
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface Predicate6<T1, T2, T3, T4, T5, T6, E extends Throwable> {
    /**
     * Evaluates this predicate for the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @param value3 the argument at position 3
     * @param value4 the argument at position 4
     * @param value5 the argument at position 5
     * @param value6 the argument at position 6
     * @return the result
     * @throws E if the operation fails
     */
    boolean test(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, T6 value6) throws E;

    /**
     * Returns a short-circuiting logical AND of this predicate and {@code other}.
     * <p>This predicate runs first. If its result is {@code false}, or it throws, the other
     * predicate is not invoked. Composition performs no evaluation.
     *
     * @param other the predicate to evaluate second
     * @return the composed predicate
     * @throws NullPointerException if {@code other} is null
     */
    default Predicate6<T1, T2, T3, T4, T5, T6, E> and(
            Predicate6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ?
                    extends E> other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2, value3, value4, value5, value6) -> test(value1, value2, value3,
                value4, value5, value6) && other.test(value1, value2, value3, value4, value5,
                value6);
    }

    /**
     * Returns a short-circuiting logical OR of this predicate and {@code other}.
     * <p>This predicate runs first. If its result is {@code true}, or it throws, the other
     * predicate is not invoked. Composition performs no evaluation.
     *
     * @param other the predicate to evaluate second
     * @return the composed predicate
     * @throws NullPointerException if {@code other} is null
     */
    default Predicate6<T1, T2, T3, T4, T5, T6, E> or(
            Predicate6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ?
                    extends E> other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2, value3, value4, value5, value6) -> test(value1, value2, value3,
                value4, value5, value6) || other.test(value1, value2, value3, value4, value5,
                value6);
    }

    /**
     * Returns the logical negation of this predicate.
     *
     * @return the negated predicate
     */
    default Predicate6<T1, T2, T3, T4, T5, T6, E> negate() {
        return (value1, value2, value3, value4, value5, value6) -> !test(value1, value2, value3,
                value4, value5, value6);
    }
}
