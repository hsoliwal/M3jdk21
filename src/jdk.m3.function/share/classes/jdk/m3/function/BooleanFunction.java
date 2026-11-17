/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Maps a primitive boolean value to a reference value. */
@FunctionalInterface
public interface BooleanFunction<R> {
    R apply(boolean value);

    /** Applies a reference-value mapper after this mapping. */
    default <V> BooleanFunction<V> andThen(
            java.util.function.Function<? super R, ? extends V> after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.apply(apply(value));
    }
}
