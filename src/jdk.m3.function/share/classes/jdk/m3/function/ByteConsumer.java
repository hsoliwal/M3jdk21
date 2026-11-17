/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Consumes one primitive byte value without boxing. */
@FunctionalInterface
public interface ByteConsumer {
    void accept(byte value);

    /** Invokes the second consumer only after the first succeeds. */
    default ByteConsumer andThen(ByteConsumer after) {
        java.util.Objects.requireNonNull(after);
        return value -> {
            accept(value);
            after.accept(value);
        };
    }
}
