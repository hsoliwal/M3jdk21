/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * A three-argument function.
 *
 * @param <A> first argument type
 * @param <B> second argument type
 * @param <C> third argument type
 * @param <R> result type
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A first, B second, C third);

    /**
     * Applies this function, followed by the non-null mapping function.
     * A failure in this function prevents the second invocation.
     */
    default <V> TriFunction<A, B, C, V> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
