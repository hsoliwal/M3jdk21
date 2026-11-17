/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Consumes one primitive float value without boxing. */
@FunctionalInterface
public interface FloatConsumer {
    void accept(float value);

    /** Invokes the second consumer only after the first succeeds. */
    default FloatConsumer andThen(FloatConsumer after) {
        java.util.Objects.requireNonNull(after);
        return value -> {
            accept(value);
            after.accept(value);
        };
    }
}
