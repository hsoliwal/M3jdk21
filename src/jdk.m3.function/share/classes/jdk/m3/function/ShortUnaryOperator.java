/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Transforms one primitive short into the same primitive type. */
@FunctionalInterface
public interface ShortUnaryOperator {
    short applyAsShort(short value);

    /** Invokes the non-null previous operator before this one. */
    default ShortUnaryOperator compose(ShortUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value -> applyAsShort(before.applyAsShort(value));
    }

    /** Invokes the non-null next operator after this one. */
    default ShortUnaryOperator andThen(ShortUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.applyAsShort(applyAsShort(value));
    }

    /** An operation preserving the exact primitive value. */
    static ShortUnaryOperator identity() {
        return value -> value;
    }
}
