/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** An action accepting three values in their original order. */
@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A first, B second, C third);

    /** Runs the non-null second action only after this action succeeds. */
    default TriConsumer<A, B, C> andThen(
            TriConsumer<? super A, ? super B, ? super C> after) {
        java.util.Objects.requireNonNull(after);
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}
