/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A two-argument function with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingBiFunction<A, B, R, E extends Exception> {
    R apply(A first, B second) throws E;

    /** Runs a non-throwing mapping only after this function succeeds. */
    default <V> ThrowingBiFunction<A, B, V, E> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return (a, b) -> after.apply(apply(a, b));
    }
}
