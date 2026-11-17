/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests one primitive boolean value without boxing. */
@FunctionalInterface
public interface BooleanPredicate {
    boolean test(boolean value);

    /** Evaluates the other test only if this test succeeds. */
    default BooleanPredicate and(BooleanPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /** Evaluates the other test only if this test fails. */
    default BooleanPredicate or(BooleanPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /** Negates this test. */
    default BooleanPredicate negate() {
        return value -> !test(value);
    }
}
