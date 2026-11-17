/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests one primitive float value without boxing. */
@FunctionalInterface
public interface FloatPredicate {
    boolean test(float value);

    /** Evaluates the other test only if this test succeeds. */
    default FloatPredicate and(FloatPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /** Evaluates the other test only if this test fails. */
    default FloatPredicate or(FloatPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /** Negates this test. */
    default FloatPredicate negate() {
        return value -> !test(value);
    }
}
