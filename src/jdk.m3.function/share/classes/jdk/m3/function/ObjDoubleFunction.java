/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps a reference and a primitive double to a reference result. */
@FunctionalInterface
public interface ObjDoubleFunction<T, R> {
    R apply(T object, double value);

    /** Applies a non-null mapping after this operation. */
    default <V> ObjDoubleFunction<T, V> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return (object, value) -> after.apply(apply(object, value));
    }
}
