/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package jdk.m3.function;

/** Transforms one primitive float into the same primitive type. */
@FunctionalInterface
public interface FloatUnaryOperator {
    float applyAsFloat(float value);

    /** Invokes the non-null previous operator before this one. */
    default FloatUnaryOperator compose(FloatUnaryOperator before) {
        java.util.Objects.requireNonNull(before);
        return value -> applyAsFloat(before.applyAsFloat(value));
    }

    /** Invokes the non-null next operator after this one. */
    default FloatUnaryOperator andThen(FloatUnaryOperator after) {
        java.util.Objects.requireNonNull(after);
        return value -> after.applyAsFloat(applyAsFloat(value));
    }

    /** An operation preserving the exact primitive value. */
    static FloatUnaryOperator identity() {
        return value -> value;
    }
}
