/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps a reference and a primitive int to a reference result. */
@FunctionalInterface
public interface ObjIntFunction<T, R> {
    R apply(T object, int value);

    /** Applies a non-null mapping after this operation. */
    default <V> ObjIntFunction<T, V> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return (object, value) -> after.apply(apply(object, value));
    }
}
