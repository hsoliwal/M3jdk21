/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Tests a reference and a primitive byte without boxing the latter. */
@FunctionalInterface
public interface ObjBytePredicate<T> {
    boolean test(T object, byte value);

    /** Evaluates the other test only if this test succeeds. */
    default ObjBytePredicate<T> and(ObjBytePredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) && other.test(object, value);
    }

    /** Evaluates the other test only if this test fails. */
    default ObjBytePredicate<T> or(ObjBytePredicate<? super T> other) {
        java.util.Objects.requireNonNull(other);
        return (object, value) ->
                test(object, value) || other.test(object, value);
    }

    /** Negates this test. */
    default ObjBytePredicate<T> negate() {
        return (object, value) -> !test(object, value);
    }
}
