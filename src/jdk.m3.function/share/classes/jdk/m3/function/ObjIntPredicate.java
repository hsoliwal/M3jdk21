/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive int without boxing the latter. */
@FunctionalInterface
public interface ObjIntPredicate<T> {
    boolean test(T object, int value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjIntPredicate<T> and(ObjIntPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjIntPredicate<T> or(ObjIntPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjIntPredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
