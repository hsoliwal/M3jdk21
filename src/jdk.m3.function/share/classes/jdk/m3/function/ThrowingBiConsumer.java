/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** A two-argument action with a declared checked-exception type. */
@FunctionalInterface
public interface ThrowingBiConsumer<A, B, E extends Exception> {
    void accept(A first, B second) throws E;

    /** Invokes the second action only after this action succeeds. */
    default ThrowingBiConsumer<A, B, E> andThen(
            ThrowingBiConsumer<? super A, ? super B, E> after) {
        java.util.Objects.requireNonNull(after);
        return (a, b) -> {
            accept(a, b);
            after.accept(a, b);
        };
    }
}
