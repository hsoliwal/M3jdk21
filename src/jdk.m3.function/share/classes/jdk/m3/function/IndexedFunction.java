/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * Maps a caller-supplied int index and a value.
 * It does not implicitly retain a collection or cursor.
 */
@FunctionalInterface
public interface IndexedFunction<T, R> {
    R apply(int index, T value);

    /** Applies a reference mapper after this function succeeds. */
    default <V> IndexedFunction<T, V> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return (index, value) -> after.apply(apply(index, value));
    }
}
