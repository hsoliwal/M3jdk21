/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive short without boxing the latter. */
@FunctionalInterface
public interface ObjShortPredicate<T> {
    boolean test(T object, short value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjShortPredicate<T> and(ObjShortPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjShortPredicate<T> or(ObjShortPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjShortPredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
