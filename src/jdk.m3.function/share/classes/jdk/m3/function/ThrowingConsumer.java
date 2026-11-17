/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** An action with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T value) throws E;

    /** Invokes the second action only after this action succeeds. */
    default ThrowingConsumer<T, E> andThen(
            ThrowingConsumer<? super T, E> after) {
        java.util.Objects.requireNonNull(after);
        return value -> {
            accept(value);
            after.accept(value);
        };
    }
}
