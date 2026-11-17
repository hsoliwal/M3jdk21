/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/**
 * Consumes a caller-supplied int index and value in that order.
 * The caller defines indexing origin and encounter order.
 */
@FunctionalInterface
public interface IndexedConsumer<T> {
    void accept(int index, T value);

    /** Runs the second action only when the first succeeds. */
    default IndexedConsumer<T> andThen(IndexedConsumer<? super T> after) {
        java.util.Objects.requireNonNull(after);
        return (index, value) -> {
            accept(index, value);
            after.accept(index, value);
        };
    }
}
