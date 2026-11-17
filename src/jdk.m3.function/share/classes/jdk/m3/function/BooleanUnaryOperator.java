/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Transforms one primitive boolean into the same primitive type. */
@FunctionalInterface
public interface BooleanUnaryOperator {
    boolean applyAsBoolean(boolean value);

    /** Invokes the non-null previous operator before this one. */
    default BooleanUnaryOperator compose(BooleanUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value -> applyAsBoolean(before.applyAsBoolean(value));
    }

    /** Invokes the non-null next operator after this one. */
    default BooleanUnaryOperator andThen(BooleanUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.applyAsBoolean(applyAsBoolean(value));
    }

    /** An operation preserving the exact primitive value. */
    static BooleanUnaryOperator identity() {
        return value -> value;
    }
}
