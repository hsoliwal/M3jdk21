/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive float without boxing the latter. */
@FunctionalInterface
public interface ObjFloatPredicate<T> {
    boolean test(T object, float value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjFloatPredicate<T> and(ObjFloatPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjFloatPredicate<T> or(ObjFloatPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjFloatPredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
