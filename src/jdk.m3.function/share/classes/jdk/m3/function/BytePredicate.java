/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests one primitive byte value without boxing. */
@FunctionalInterface
public interface BytePredicate {
    boolean test(byte value);

    /** Evaluates the other test only if this test succeeds. */
    default BytePredicate and(BytePredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /** Evaluates the other test only if this test fails. */
    default BytePredicate or(BytePredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /** Negates this test. */
    default BytePredicate negate() {
        return value -> !test(value);
    }
}
