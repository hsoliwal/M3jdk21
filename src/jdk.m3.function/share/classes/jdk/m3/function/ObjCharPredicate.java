/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive char without boxing the latter. */
@FunctionalInterface
public interface ObjCharPredicate<T> {
    boolean test(T object, char value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjCharPredicate<T> and(ObjCharPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjCharPredicate<T> or(ObjCharPredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjCharPredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
