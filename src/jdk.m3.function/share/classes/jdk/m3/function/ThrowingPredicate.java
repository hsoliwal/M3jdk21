/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A boolean-valued test with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingPredicate<T, E extends Exception> {
    boolean test(T value) throws E;

    /** Combines tests with left-to-right short-circuit evaluation. */
    default ThrowingPredicate<T, E> and(ThrowingPredicate<? super T, E> other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /** Combines tests with left-to-right short-circuit evaluation. */
    default ThrowingPredicate<T, E> or(ThrowingPredicate<? super T, E> other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /** Negates the result while preserving checked exceptions. */
    default ThrowingPredicate<T, E> negate() {
        return value -> !test(value);
    }
}
