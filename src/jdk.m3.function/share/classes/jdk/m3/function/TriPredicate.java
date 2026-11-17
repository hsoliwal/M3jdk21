/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A boolean-valued test accepting three values. */
@FunctionalInterface
public interface TriPredicate<A, B, C> {
    boolean test(A first, B second, C third);

    /** Combines tests with left-to-right short-circuit evaluation. */
    default TriPredicate<A, B, C> and(
            TriPredicate<? super A, ? super B, ? super C> other) {
        java.util.Objects.requireNonNull(other);
        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /** Combines tests with left-to-right short-circuit evaluation. */
    default TriPredicate<A, B, C> or(
            TriPredicate<? super A, ? super B, ? super C> other) {
        java.util.Objects.requireNonNull(other);
        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }

    /** Returns the logical negation of this test. */
    default TriPredicate<A, B, C> negate() {
        return (a, b, c) -> !test(a, b, c);
    }
}
