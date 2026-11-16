/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A predicate with the shape {@code (reference, char) to boolean}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code test}.
 *
 * @param <T> an input type
 * @since M3JDK 21
 */
@FunctionalInterface
public interface ObjCharPredicate<T> {
    /**
     * Evaluates this predicate for the supplied arguments.
     *
     * @param value1 the argument at position 1
     * @param value2 the argument at position 2
     * @return the result
     */
    boolean test(T value1, char value2);

    /**
     * Returns a short-circuiting logical AND of this predicate and {@code other}.
     * <p>This predicate runs first. If its result is {@code false}, or it throws, the other
     * predicate is not invoked. Composition performs no evaluation.
     *
     * @param other the predicate to evaluate second
     * @return the composed predicate
     * @throws NullPointerException if {@code other} is null
     */
    default ObjCharPredicate<T> and(ObjCharPredicate<? super T> other) {
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
    default ObjCharPredicate<T> or(ObjCharPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (value1, value2) -> test(value1, value2) || other.test(value1, value2);
    }

    /**
     * Returns the logical negation of this predicate.
     *
     * @return the negated predicate
     */
    default ObjCharPredicate<T> negate() {
        return (value1, value2) -> !test(value1, value2);
    }
}
