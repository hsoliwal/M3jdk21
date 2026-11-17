/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests one primitive char value without boxing. */
@FunctionalInterface
public interface CharPredicate {
    boolean test(char value);

    /** Evaluates the other test only if this test succeeds. */
    default CharPredicate and(CharPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) && other.test(value);
    }

    /** Evaluates the other test only if this test fails. */
    default CharPredicate or(CharPredicate other) {
        java.util.Objects.requireNonNull(other);
        return value -> test(value) || other.test(value);
    }

    /** Negates this test. */
    default CharPredicate negate() {
        return value -> !test(value);
    }
}
