/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests one primitive short value without boxing. */
@FunctionalInterface
public interface ShortPredicate {
    boolean test(short value);

    /** Evaluates the other test only if this test succeeds. */
    default ShortPredicate and(ShortPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /** Evaluates the other test only if this test fails. */
    default ShortPredicate or(ShortPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /** Negates this test. */
    default ShortPredicate negate() {
        return value -> !test(value);
    }
}
