/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * A function whose declared failure type may be checked.
 * No checked exception is silently wrapped or suppressed.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T value) throws E;

    /** Runs a non-throwing mapping after this function succeeds. */
    default <V> ThrowingFunction<T, V, E> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.apply(apply(value));
    }

    /** Runs a non-throwing mapping before this function. */
    default <V> ThrowingFunction<V, R, E> compose(
            java.util.function.Function<? super V, ? extends T> before) {
        java.util.Objects.requireNonNull(before);
        return value -> apply(before.apply(value));
    }
}
