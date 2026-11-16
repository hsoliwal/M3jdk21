/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.checked;

/**
 * A checked predicate with the shape {@code (double, double) to boolean}.
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
 * @param <E> the throwable type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface DoubleBiPredicate<E extends Throwable> {
    /**
     * Evaluates this predicate for the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @return the result
     * @throws E if the operation fails
     */
    boolean test(double value1, double value2) throws E;

    /**
     * Returns a short-circuiting logical AND of this predicate and {@code other}.
     * <p>This predicate runs first. If its result is {@code false}, or it throws, the other
     * predicate is not invoked. Composition performs no evaluation.
     *
     * @param other the predicate to evaluate second
     * @return the composed predicate
     * @throws NullPointerException if {@code other} is null
     */
    default DoubleBiPredicate<E> and(DoubleBiPredicate<? extends E> other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2) -> test(value1, value2) && other.test(value1, value2);
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
    default DoubleBiPredicate<E> or(DoubleBiPredicate<? extends E> other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2) -> test(value1, value2) || other.test(value1, value2);
    }

    /**
     * Returns the logical negation of this predicate.
     *
     * @return the negated predicate
     */
    default DoubleBiPredicate<E> negate() {
        return (value1, value2) -> !test(value1, value2);
    }
}
