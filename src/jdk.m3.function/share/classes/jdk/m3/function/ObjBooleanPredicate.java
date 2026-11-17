/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive boolean without boxing the latter. */
@FunctionalInterface
public interface ObjBooleanPredicate<T> {
    boolean test(T object, boolean value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjBooleanPredicate<T> and(ObjBooleanPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjBooleanPredicate<T> or(ObjBooleanPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjBooleanPredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
