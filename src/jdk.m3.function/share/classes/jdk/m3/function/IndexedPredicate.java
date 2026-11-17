/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a caller-supplied int index and value without retaining either. */
@FunctionalInterface
public interface IndexedPredicate<T> {
    boolean test(int index, T value);

    /** Combines tests with left-to-right short-circuiting. */
    default IndexedPredicate<T> and(IndexedPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (index, value) -> test(index, value) && other.test(index, value);
    }

    /** Combines tests with left-to-right short-circuiting. */
    default IndexedPredicate<T> or(IndexedPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (index, value) -> test(index, value) || other.test(index, value);
    }

    /** Negates the test without changing the index or value. */
    default IndexedPredicate<T> negate() {
        return (index, value) -> !test(index, value);
    }
}
