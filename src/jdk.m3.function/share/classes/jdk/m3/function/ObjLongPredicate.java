/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive long without boxing the latter. */
@FunctionalInterface
public interface ObjLongPredicate<T> {
    boolean test(T object, long value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjLongPredicate<T> and(ObjLongPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjLongPredicate<T> or(ObjLongPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjLongPredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
