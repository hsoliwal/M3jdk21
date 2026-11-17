/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A two-argument test with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingBiPredicate<A, B, E extends Exception> {
    boolean test(A first, B second) throws E;

    /** Evaluates the other test only after this one succeeds. */
    default ThrowingBiPredicate<A, B, E> and(
            ThrowingBiPredicate<? super A, ? super B, E> other) {
        java.util.Objects.requireNonNull(other);
        return (a, b) -> test(a, b) && other.test(a, b);
    }

    /** Evaluates the other test only if this one fails. */
    default ThrowingBiPredicate<A, B, E> or(
            ThrowingBiPredicate<? super A, ? super B, E> other) {
        java.util.Objects.requireNonNull(other);
        return (a, b) -> test(a, b) || other.test(a, b);
    }

    /** Negates this result without catching checked exceptions. */
    default ThrowingBiPredicate<A, B, E> negate() {
        return (a, b) -> !test(a, b);
    }
}
