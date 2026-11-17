/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive double without boxing the latter. */
@FunctionalInterface
public interface ObjDoublePredicate<T> {
    boolean test(T object, double value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjDoublePredicate<T> and(ObjDoublePredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjDoublePredicate<T> or(ObjDoublePredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjDoublePredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
