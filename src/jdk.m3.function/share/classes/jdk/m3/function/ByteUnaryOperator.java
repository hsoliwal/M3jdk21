/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Transforms one primitive byte into the same primitive type. */
@FunctionalInterface
public interface ByteUnaryOperator {
    byte applyAsByte(byte value);

    /** Invokes the non-null previous operator before this one. */
    default ByteUnaryOperator compose(ByteUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value -> applyAsByte(before.applyAsByte(value));
    }

    /** Invokes the non-null next operator after this one. */
    default ByteUnaryOperator andThen(ByteUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.applyAsByte(applyAsByte(value));
    }

    /** An operation preserving the exact primitive value. */
    static ByteUnaryOperator identity() {
        return value -> value;
    }
}
